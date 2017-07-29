package jet.opengl.demos.nvidia.waves.samples;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector2f;

import jet.opengl.demos.nvidia.waves.GFSDK_WaveWorks;
import jet.opengl.demos.nvidia.waves.GFSDK_WaveWorks_Quadtree;
import jet.opengl.demos.nvidia.waves.GFSDK_WaveWorks_Quadtree_Params;
import jet.opengl.demos.nvidia.waves.GFSDK_WaveWorks_Quadtree_Stats;
import jet.opengl.demos.nvidia.waves.GFSDK_WaveWorks_Savestate;
import jet.opengl.demos.nvidia.waves.GFSDK_WaveWorks_ShaderInput_Desc;
import jet.opengl.demos.nvidia.waves.GFSDK_WaveWorks_Simulation;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.buffer.VertexArrayObject;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.util.CommonUtil;

/**
 * Created by mazhen'gui on 2017/7/27.
 */

final class OceanSurface implements Disposeable{

    GFSDK_WaveWorks_Quadtree m_hOceanQuadTree;

    // D3D objects
//    ID3D11Device*			m_pd3dDevice;

//    ID3DX11Effect*			m_pOceanFX;
    GLSLProgram m_pRenderSurfaceTechnique;
    GLSLProgram		m_pRenderSurfaceShadedWithShorelinePass;
//	ID3DX11EffectPass*		m_pRenderSurfaceWireframeWithShorelinePass;

    VertexArrayObject m_pQuadLayout;
    VertexArrayObject		m_pRayContactLayout;
    GLSLProgram m_pRenderRayContactTechnique;
    BufferGL m_pContactVB;
    BufferGL			m_pContactIB;

    final int[] m_pQuadTreeShaderInputMappings_Shore;
    final int[] m_pSimulationShaderInputMappings_Shore;

    public OceanSurface(){
        int NumQuadtreeShaderInputs = GFSDK_WaveWorks.GFSDK_WaveWorks_Quadtree_GetShaderInputCountD3D11();
        int NumSimulationShaderInputs = GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_GetShaderInputCountD3D11();
        m_pQuadTreeShaderInputMappings_Shore = new int [NumQuadtreeShaderInputs];
        m_pSimulationShaderInputMappings_Shore = new int [NumSimulationShaderInputs];
    }

    private DistanceField pDistanceFieldModule; // Not owned!
    void AttachDistanceFieldModule( DistanceField pDistanceField ) { pDistanceFieldModule = pDistanceField; }

    public void init() {
        TCHAR path[MAX_PATH];

        V_RETURN(DXUTFindDXSDKMediaFileCch(path, MAX_PATH, TEXT("ocean_surface_d3d11.fxo")));
        V_RETURN(D3DX11CreateEffectFromFile(path, 0, m_pd3dDevice, &m_pOceanFX));


        // Hook up the shader mappings
        m_pRenderSurfaceTechnique = m_pOceanFX->GetTechniqueByName("RenderOceanSurfTech");
        m_pRenderSurfaceShadedWithShorelinePass = m_pRenderSurfaceTechnique->GetPassByName("Pass_Solid_WithShoreline");

        D3DX11_PASS_SHADER_DESC passShaderDesc;

        V_RETURN(m_pRenderSurfaceShadedWithShorelinePass->GetVertexShaderDesc(&passShaderDesc));
        ID3D11ShaderReflection* pShadedShoreReflectionVS = GetReflection(passShaderDesc);

        V_RETURN(m_pRenderSurfaceShadedWithShorelinePass->GetHullShaderDesc(&passShaderDesc));
        ID3D11ShaderReflection* pShadedShoreReflectionHS = GetReflection(passShaderDesc);

        V_RETURN(m_pRenderSurfaceShadedWithShorelinePass->GetDomainShaderDesc(&passShaderDesc));
        ID3D11ShaderReflection* pShadedShoreReflectionDS = GetReflection(passShaderDesc);

        V_RETURN(m_pRenderSurfaceShadedWithShorelinePass->GetPixelShaderDesc(&passShaderDesc));
        ID3D11ShaderReflection* pShadedShoreReflectionPS = GetReflection(passShaderDesc);

        UINT NumQuadtreeShaderInputs = GFSDK_WaveWorks_Quadtree_GetShaderInputCountD3D11();
        UINT NumSimulationShaderInputs = GFSDK_WaveWorks_Simulation_GetShaderInputCountD3D11();

        for(UINT i = 0; i != NumQuadtreeShaderInputs; ++i)
        {
            GFSDK_WaveWorks_ShaderInput_Desc inputDesc;
            GFSDK_WaveWorks_Quadtree_GetShaderInputDescD3D11(i, &inputDesc);
            m_pQuadTreeShaderInputMappings_Shore[i] = GetShaderInputRegisterMapping(pShadedShoreReflectionVS, pShadedShoreReflectionHS, pShadedShoreReflectionDS, pShadedShoreReflectionPS, inputDesc);
        }

        for(UINT i = 0; i != NumSimulationShaderInputs; ++i)
        {
            GFSDK_WaveWorks_ShaderInput_Desc inputDesc;
            GFSDK_WaveWorks_Simulation_GetShaderInputDescD3D11(i, &inputDesc);
            m_pSimulationShaderInputMappings_Shore[i] = GetShaderInputRegisterMapping(pShadedShoreReflectionVS, pShadedShoreReflectionHS, pShadedShoreReflectionDS, pShadedShoreReflectionPS, inputDesc);
        }

        pShadedShoreReflectionVS->Release();
        pShadedShoreReflectionPS->Release();
        pShadedShoreReflectionHS->Release();
        pShadedShoreReflectionDS->Release();
    }

    public void initQuadTree(GFSDK_WaveWorks_Quadtree_Params params) {
        if(null == m_hOceanQuadTree)
        {
            m_hOceanQuadTree = GFSDK_WaveWorks.GFSDK_WaveWorks_Quadtree_CreateD3D11(params /*m_pd3dDevice,*/ );
        }
        else
        {
            GFSDK_WaveWorks.GFSDK_WaveWorks_Quadtree_UpdateParams(m_hOceanQuadTree, params);
        }
    }
    // --------------------------------- Rendering routines -----------------------------------
    // Rendering
    void renderShaded(//		ID3D11DeviceContext* pDC,
                              Matrix4f matView,
                              Matrix4f matProj,
                              GFSDK_WaveWorks_Simulation hSim,
                              GFSDK_WaveWorks_Savestate hSavestate,
                              final ReadableVector2f windDir,
                              float steepness,
                               float amplitude,
                               float wavelength,
                               float speed,
                               float parallelness,
                               float totalTime){

    }

    void getQuadTreeStats(GFSDK_WaveWorks_Quadtree_Stats stats){

    }


    @Override
    public void dispose() {
        CommonUtil.safeRelease(m_pRenderSurfaceTechnique);
        CommonUtil.safeRelease(m_pRenderSurfaceShadedWithShorelinePass);
        CommonUtil.safeRelease(m_pQuadLayout);
        CommonUtil.safeRelease(m_pRayContactLayout);
        CommonUtil.safeRelease(m_pRenderRayContactTechnique);
        CommonUtil.safeRelease(m_pContactVB);
        CommonUtil.safeRelease(m_pContactIB);
    }
}
