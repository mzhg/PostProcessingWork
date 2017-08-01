package jet.opengl.demos.nvidia.waves.samples;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector2f;

import jet.opengl.demos.nvidia.waves.GFSDK_WaveWorks;
import jet.opengl.demos.nvidia.waves.GFSDK_WaveWorks_Quadtree;
import jet.opengl.demos.nvidia.waves.GFSDK_WaveWorks_Quadtree_Params;
import jet.opengl.demos.nvidia.waves.GFSDK_WaveWorks_Quadtree_Stats;
import jet.opengl.demos.nvidia.waves.GFSDK_WaveWorks_Savestate;
import jet.opengl.demos.nvidia.waves.GFSDK_WaveWorks_Simulation;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.buffer.VertexArrayObject;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.util.CacheBuffer;
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
    private GLFuncProvider gl;
    private final Matrix4f topDownMatrix = new Matrix4f();

    public OceanSurface(){
        int NumQuadtreeShaderInputs = GFSDK_WaveWorks.GFSDK_WaveWorks_Quadtree_GetShaderInputCountD3D11();
        int NumSimulationShaderInputs = GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_GetShaderInputCountD3D11();
        m_pQuadTreeShaderInputMappings_Shore = new int [NumQuadtreeShaderInputs];
        m_pSimulationShaderInputMappings_Shore = new int [NumSimulationShaderInputs];
    }

    DistanceField pDistanceFieldModule; // Not owned!
    void AttachDistanceFieldModule( DistanceField pDistanceField ) { pDistanceFieldModule = pDistanceField; }

    public void init() {
        gl = GLFuncProviderFactory.getGLFuncProvider();
//        TCHAR path[MAX_PATH];
//
//        V_RETURN(DXUTFindDXSDKMediaFileCch(path, MAX_PATH, TEXT("ocean_surface_d3d11.fxo")));
//        V_RETURN(D3DX11CreateEffectFromFile(path, 0, m_pd3dDevice, &m_pOceanFX));
//
//
//        // Hook up the shader mappings
//        m_pRenderSurfaceTechnique = m_pOceanFX->GetTechniqueByName("RenderOceanSurfTech");
//        m_pRenderSurfaceShadedWithShorelinePass = m_pRenderSurfaceTechnique->GetPassByName("Pass_Solid_WithShoreline");
        final String shader_path = "nvidia/WaveWorks/shaders/";
        m_pRenderSurfaceShadedWithShorelinePass =GLSLProgram.createProgram(shader_path + "OceanWaveVS.vert", shader_path + "OceanWaveHS.gltc",
                shader_path+"OceanWaveDS.glte", shader_path+"SolidWireGS.gemo", shader_path+"OceanWaveShorePS.frag", null);

//        D3DX11_PASS_SHADER_DESC passShaderDesc;
//
//        V_RETURN(m_pRenderSurfaceShadedWithShorelinePass->GetVertexShaderDesc(&passShaderDesc));
//        ID3D11ShaderReflection* pShadedShoreReflectionVS = GetReflection(passShaderDesc);
//
//        V_RETURN(m_pRenderSurfaceShadedWithShorelinePass->GetHullShaderDesc(&passShaderDesc));
//        ID3D11ShaderReflection* pShadedShoreReflectionHS = GetReflection(passShaderDesc);
//
//        V_RETURN(m_pRenderSurfaceShadedWithShorelinePass->GetDomainShaderDesc(&passShaderDesc));
//        ID3D11ShaderReflection* pShadedShoreReflectionDS = GetReflection(passShaderDesc);
//
//        V_RETURN(m_pRenderSurfaceShadedWithShorelinePass->GetPixelShaderDesc(&passShaderDesc));
//        ID3D11ShaderReflection* pShadedShoreReflectionPS = GetReflection(passShaderDesc);

        int NumQuadtreeShaderInputs = GFSDK_WaveWorks.GFSDK_WaveWorks_Quadtree_GetShaderInputCountD3D11();
        int NumSimulationShaderInputs = GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_GetShaderInputCountD3D11();

        for(int i = 0; i != NumQuadtreeShaderInputs; ++i)
        {
//            GFSDK_WaveWorks_ShaderInput_Desc inputDesc;  TODO
//            GFSDK_WaveWorks_Quadtree_GetShaderInputDescD3D11(i, &inputDesc);
//            m_pQuadTreeShaderInputMappings_Shore[i] = GetShaderInputRegisterMapping(pShadedShoreReflectionVS, pShadedShoreReflectionHS, pShadedShoreReflectionDS, pShadedShoreReflectionPS, inputDesc);
        }

        for(int i = 0; i != NumSimulationShaderInputs; ++i)
        {
//            GFSDK_WaveWorks_ShaderInput_Desc inputDesc;  TODO
//            GFSDK_WaveWorks_Simulation_GetShaderInputDescD3D11(i, &inputDesc);
//            m_pSimulationShaderInputMappings_Shore[i] = GetShaderInputRegisterMapping(pShadedShoreReflectionVS, pShadedShoreReflectionHS, pShadedShoreReflectionDS, pShadedShoreReflectionPS, inputDesc);
        }

//        pShadedShoreReflectionVS->Release();
//        pShadedShoreReflectionPS->Release();
//        pShadedShoreReflectionHS->Release();
//        pShadedShoreReflectionDS->Release();

        {

            float vertex_data[/*5*4*/] =
            {0, 0, 0, 1,
                    1, 1, 0, 0,
                    0, 1, 1, 0,
                    -1, 1, 0, 0,
                    0, 1,-1, 0};
//            D3D11_BUFFER_DESC vBufferDesc;
//            vBufferDesc.ByteWidth = 5 * sizeof(XMFLOAT4);
//            vBufferDesc.Usage = D3D11_USAGE_DEFAULT;
//            vBufferDesc.BindFlags = D3D11_BIND_VERTEX_BUFFER;
//            vBufferDesc.CPUAccessFlags = 0;
//            vBufferDesc.MiscFlags = 0;
//            D3D11_SUBRESOURCE_DATA vBufferData;
//            vBufferData.pSysMem = vertex_data;
//            vBufferData.SysMemPitch = 0;
//            vBufferData.SysMemSlicePitch = 0;
//            V_RETURN(m_pd3dDevice->CreateBuffer(&vBufferDesc, &vBufferData, &m_pContactVB));
            m_pContactVB = new BufferGL();
            m_pContactVB.initlize(GLenum.GL_ARRAY_BUFFER, vertex_data.length*4, CacheBuffer.wrap(vertex_data), GLenum.GL_STATIC_DRAW);


            final byte indices[] = {0,1,2, 0,2,3, 0,3,4, 0,4,1};
//            D3D11_BUFFER_DESC iBufferDesc;
//            iBufferDesc.ByteWidth = sizeof(indices);
//            iBufferDesc.Usage = D3D11_USAGE_IMMUTABLE;
//            iBufferDesc.BindFlags = D3D11_BIND_INDEX_BUFFER;
//            iBufferDesc.CPUAccessFlags = 0;
//            iBufferDesc.MiscFlags = 0;
//            D3D11_SUBRESOURCE_DATA iBufferData;
//            iBufferData.pSysMem = indices;
//            iBufferData.SysMemPitch = 0;
//            iBufferData.SysMemSlicePitch = 0;
//            V_RETURN(m_pd3dDevice->CreateBuffer(&iBufferDesc, &iBufferData, &m_pContactIB));
            m_pContactIB = new BufferGL();
            m_pContactIB.initlize(GLenum.GL_ELEMENT_ARRAY_BUFFER, indices.length, CacheBuffer.wrap(indices), GLenum.GL_STATIC_DRAW);
        }
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
        if( pDistanceFieldModule != null)
        {
            // Apply data tex SRV
            pDistanceFieldModule.GetWorldToTopDownTextureMatrix( topDownMatrix );

//            XMFLOAT4X4 tdmStore;
//            XMStoreFloat4x4(&tdmStore, topDownMatrix);

            m_pRenderSurfaceShadedWithShorelinePass.enable();
//            m_pOceanFX->GetVariableByName("g_WorldToTopDownTextureMatrix")->AsMatrix()->SetMatrix( (FLOAT*)&tdmStore );
            int index = m_pRenderSurfaceShadedWithShorelinePass.getUniformLocation("g_WorldToTopDownTextureMatrix");
            if(index >=0){
                gl.glUniformMatrix4fv(index, false, CacheBuffer.wrap(topDownMatrix));
            }
//            m_pOceanFX->GetVariableByName("g_GerstnerSteepness")->AsScalar()->SetFloat( steepness );
            index = m_pRenderSurfaceShadedWithShorelinePass.getUniformLocation("g_GerstnerSteepness");
            if(index>=0){
                gl.glUniform1f(index, steepness);
            }
//            m_pOceanFX->GetVariableByName("g_BaseGerstnerAmplitude")->AsScalar()->SetFloat( amplitude );
            index = m_pRenderSurfaceShadedWithShorelinePass.getUniformLocation("g_BaseGerstnerAmplitude");
            if(index>=0){
                gl.glUniform1f(index, amplitude);
            }

//            m_pOceanFX->GetVariableByName("g_BaseGerstnerWavelength")->AsScalar()->SetFloat( wavelength );
            index = m_pRenderSurfaceShadedWithShorelinePass.getUniformLocation("g_BaseGerstnerWavelength");
            if(index>=0){
                gl.glUniform1f(index, wavelength);
            }

//            m_pOceanFX->GetVariableByName("g_BaseGerstnerSpeed")->AsScalar()->SetFloat( speed );
            index = m_pRenderSurfaceShadedWithShorelinePass.getUniformLocation("g_BaseGerstnerSpeed");
            if(index>=0){
                gl.glUniform1f(index, speed);
            }
//            m_pOceanFX->GetVariableByName("g_BaseGerstnerParallelness")->AsScalar()->SetFloat( parallelness );
            index = m_pRenderSurfaceShadedWithShorelinePass.getUniformLocation("g_BaseGerstnerParallelness");
            if(index>=0){
                gl.glUniform1f(index, parallelness);
            }

//            m_pOceanFX->GetVariableByName("g_WindDirection")->AsVector()->SetFloatVector( (FLOAT*) &windDir );
            index = m_pRenderSurfaceShadedWithShorelinePass.getUniformLocation("g_WindDirection");
            if(index>=0){
                gl.glUniform2f(index, windDir.getX(), windDir.getY());
            }

//            m_pOceanFX->GetVariableByName("g_DataTexture")->AsShaderResource()->SetResource( pDistanceFieldModule->GetDataTextureSRV() ); TODO
//            m_pOceanFX->GetVariableByName("g_Time")->AsScalar()->SetFloat( totalTime );
            index = m_pRenderSurfaceShadedWithShorelinePass.getUniformLocation("g_Time");
            if(index>=0){
                gl.glUniform1f(index, totalTime);
            }

//            m_pRenderSurfaceShadedWithShorelinePass->Apply( 0, pDC );
            GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_SetRenderStateD3D11(hSim, /*pDC,*/ matView, m_pSimulationShaderInputMappings_Shore, hSavestate);
            GFSDK_WaveWorks.GFSDK_WaveWorks_Quadtree_DrawD3D11(m_hOceanQuadTree, /*pDC,*/ matView, matProj, m_pQuadTreeShaderInputMappings_Shore, hSavestate);

//            m_pOceanFX->GetVariableByName("g_DataTexture")->AsShaderResource()->SetResource( NULL );
        }
//        GFSDK_WaveWorks.GFSDK_WaveWorks_Savestate_RestoreD3D11(hSavestate, pDC);
    }

    void getQuadTreeStats(GFSDK_WaveWorks_Quadtree_Stats stats){
        GFSDK_WaveWorks.GFSDK_WaveWorks_Quadtree_GetStats(m_hOceanQuadTree, stats);
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