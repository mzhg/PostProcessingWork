package jet.opengl.demos.nvidia.waves.ocean;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;

import java.nio.IntBuffer;

import jet.opengl.demos.intel.cput.D3D11_INPUT_ELEMENT_DESC;
import jet.opengl.demos.intel.cput.ID3D11InputLayout;
import jet.opengl.demos.nvidia.waves.wavework.GFSDK_WaveWorks;
import jet.opengl.demos.nvidia.waves.wavework.GFSDK_WaveWorks_Quadtree;
import jet.opengl.demos.nvidia.waves.wavework.GFSDK_WaveWorks_Quadtree_Params;
import jet.opengl.demos.nvidia.waves.wavework.GFSDK_WaveWorks_Quadtree_Stats;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.CommonUtil;

final class OceanSurface implements OceanConst, Disposeable {

    private final OceanSurfaceParameters m_params = new OceanSurfaceParameters();

    // D3D objects
//    ID3D11Device* m_pd3dDevice;
    ID3D11InputLayout m_pQuadLayout;

    // Color look up 1D texture
    private Texture2D m_pBicolorMap;			// (RGBA8)
    private Texture2D m_pFoamIntensityMap;
    private Texture2D m_pFoamDiffuseMap;
    private Texture2D m_pWakeMap;
    private Texture2D m_pShipFoamMap;
    private Texture2D m_pGustMap;


    private GLSLProgram m_pShiftFadeBlurLocalFoamTechnique;
    private GLSLProgram		  m_pShiftFadeBlurLocalFoamShadedPass;
//    ID3DX11EffectVectorVariable* m_pShiftFadeBlurLocalFoamUVOffsetBlurVariable;
//    ID3DX11EffectScalarVariable* m_pShiftFadeBlurLocalFoamFadeAmountVariable;
//    ID3DX11EffectShaderResourceVariable* m_pShiftFadeBlurLocalFoamTextureVariable;
    private Texture2D		  m_pLocalFoamMapReceiver;
    private Texture2D m_pLocalFoamMapReceiverSRV;
    private Texture2D   m_pLocalFoamMapReceiverRTV;
    private Texture2D		  m_pLocalFoamMapFader;
    private Texture2D m_pLocalFoamMapFaderSRV;
    private Texture2D   m_pLocalFoamMapFaderRTV;
//    ID3DX11EffectShaderResourceVariable* m_pRenderSurfaceLocalFoamMapVariable;


//    ID3DX11Effect* m_pOceanFX;
    private GLSLProgram m_pRenderSurfaceTechnique;
    private GLSLProgram m_pRenderSurfaceShadedPass;
    private GLSLProgram m_pRenderSurfaceWireframePass;
    /*ID3DX11EffectMatrixVariable* m_pRenderSurfaceMatViewProjVariable;
    ID3DX11EffectMatrixVariable* m_pRenderSurfaceMatViewVariable;
    ID3DX11EffectVectorVariable* m_pRenderSurfaceSkyColorVariable;
    ID3DX11EffectVectorVariable* m_pRenderSurfaceLightDirectionVariable;
    ID3DX11EffectVectorVariable* m_pRenderSurfaceLightColorVariable;
    ID3DX11EffectVectorVariable* m_pRenderSurfaceWaterColorVariable;
    ID3DX11EffectVectorVariable* m_pRenderSurfacePatchColorVariable;
    ID3DX11EffectShaderResourceVariable* m_pRenderSurfaceColorMapVariable;
    ID3DX11EffectShaderResourceVariable* m_pRenderSurfaceCubeMap0Variable;
    ID3DX11EffectShaderResourceVariable* m_pRenderSurfaceCubeMap1Variable;
    ID3DX11EffectScalarVariable* m_pRenderSurfaceCubeBlendVariable;
    ID3DX11EffectVectorVariable* m_pRenderSurfaceCube0RotateSinCosVariable;
    ID3DX11EffectVectorVariable* m_pRenderSurfaceCube1RotateSinCosVariable;
    ID3DX11EffectVectorVariable* m_pRenderSurfaceCubeMultVariable;

    ID3DX11EffectScalarVariable* m_pSpotlightNumVariable;
    ID3DX11EffectVectorVariable* m_pRenderSurfaceSpotlightPositionVariable;
    ID3DX11EffectVectorVariable* m_pRenderSurfaceSpotLightAxisAndCosAngleVariable;
    ID3DX11EffectVectorVariable* m_pRenderSurfaceSpotlightColorVariable;
    ID3DX11EffectMatrixVariable* m_pSpotlightShadowMatrixVar;
    ID3DX11EffectShaderResourceVariable* m_pSpotlightShadowResourceVar;

    ID3DX11EffectScalarVariable* m_pRenderSurfaceFogExponentVariable;

    ID3DX11EffectShaderResourceVariable* m_pRenderSurfaceFoamIntensityMapVariable;
    ID3DX11EffectShaderResourceVariable* m_pRenderSurfaceFoamDiffuseMapVariable;
    ID3DX11EffectScalarVariable* m_pGlobalFoamFadeVariable;

    ID3DX11EffectVectorVariable* m_pRenderSurfaceHullProfileCoordOffsetAndScaleVariable;
    ID3DX11EffectVectorVariable* m_pRenderSurfaceHullProfileHeightOffsetAndHeightScaleAndTexelSizeVariable;
    ID3DX11EffectShaderResourceVariable* m_pRenderSurfaceHullProfileMapVariable;

    ID3DX11EffectShaderResourceVariable* m_pRenderSurfaceWakeMapVariable;
    ID3DX11EffectShaderResourceVariable* m_pRenderSurfaceShipFoamMapVariable;
    ID3DX11EffectMatrixVariable*		 m_pRenderSurfaceWorldToShipVariable;
    ID3DX11EffectShaderResourceVariable* m_pRenderSurfaceGustMapVariable;
    ID3DX11EffectVectorVariable*		 m_pRenderSurfaceGustUVVariable;

    ID3DX11EffectShaderResourceVariable* m_pRenderSurfaceReflectionTextureVariable;
    ID3DX11EffectShaderResourceVariable* m_pRenderSurfaceReflectionDepthTextureVariable;
    ID3DX11EffectShaderResourceVariable* m_pRenderSurfaceReflectionPosTextureVariable;
    ID3DX11EffectVectorVariable*		 m_pRenderSurfaceScreenSizeInvVariable;

    ID3DX11EffectMatrixVariable*		 m_pRenderSurfaceSceneToShadowMapVariable;
    ID3DX11EffectVectorVariable*		 m_pRenderSurfaceLightningPositionVariable;
    ID3DX11EffectVectorVariable*		 m_pRenderSurfaceLightningColorVariable;

    ID3DX11EffectScalarVariable*		 m_pRenderSurfaceCloudFactorVariable;
    ID3DX11EffectScalarVariable*		 m_pRenderSurfaceShowSpraySimVariable;
    ID3DX11EffectScalarVariable*		 m_pRenderSurfaceShowFoamSimVariable;

    ID3DX11EffectScalarVariable*		 m_pRenderSurfaceGustsEnabledVariable;
    ID3DX11EffectScalarVariable*		 m_pRenderSurfaceWakeEnabledVariable;*/

    // --------------------------------- Surface geometry -----------------------------------
    GFSDK_WaveWorks_Quadtree m_hOceanQuadTree;

    /*UINT* m_pQuadTreeShaderInputMappings_Shaded;
    UINT* m_pQuadTreeShaderInputMappings_Wireframe;

    UINT* m_pSimulationShaderInputMappings_Shaded;
    UINT* m_pSimulationShaderInputMappings_Wireframe;*/

    private OceanSurfaceState m_pSurfaceState;

    private final Matrix4f m_matWorldToShip = new Matrix4f();

    private final BufferGL[]    m_pParticlesBuffer = new BufferGL[2];
    private final BufferGL[]    m_pParticlesBufferUAV = new BufferGL[2];
    private final BufferGL[]    m_pParticlesBufferSRV = new BufferGL[2];

    private int                 m_ParticleWriteBuffer;

    private BufferGL            m_pDrawParticlesCB;
    private BufferGL            m_pDrawParticlesBuffer;
    private BufferGL            m_pDrawParticlesBufferUAV;

    private GLSLProgram         m_pRenderSprayParticlesTechnique;
    private GLSLProgram         m_pSimulateSprayParticlesTechnique;
    private GLSLProgram         m_pDispatchArgumentsTechnique;

    /*ID3DX11EffectVectorVariable* m_pViewRightVariable;
    ID3DX11EffectVectorVariable* m_pViewUpVariable;
    ID3DX11EffectVectorVariable* m_pViewForwardVariable;
    ID3DX11EffectScalarVariable* m_pTimeStep;

    ID3DX11EffectShaderResourceVariable* m_pParticlesBufferVariable;*/

    private GLFuncProvider gl;

    public OceanSurface(OceanSurfaceState pSurfaceState){
        m_pSurfaceState = pSurfaceState;
    }

    void initQuadTree(GFSDK_WaveWorks_Quadtree_Params params)
    {
        if(null == m_hOceanQuadTree)
        {
            m_hOceanQuadTree = GFSDK_WaveWorks.GFSDK_WaveWorks_Quadtree_CreateD3D11(params);
        }
        else
        {
            GFSDK_WaveWorks.GFSDK_WaveWorks_Quadtree_UpdateParams(m_hOceanQuadTree, params);
        }
    }

    void init(OceanSurfaceParameters params)
    {
        if(null == m_pRenderSurfaceTechnique)
        {
            /*ID3DXBuffer* pEffectBuffer = NULL;
            V_RETURN(LoadFile(TEXT(".\\Media\\ocean_surface_d3d11.fxo"), &pEffectBuffer));
            V_RETURN(D3DX11CreateEffectFromMemory(pEffectBuffer->GetBufferPointer(), pEffectBuffer->GetBufferSize(), 0, m_pd3dDevice, &m_pOceanFX));
            pEffectBuffer->Release();*/

            // Hook up the shader mappings
//            m_pRenderSurfaceTechnique = m_pOceanFX->GetTechniqueByName("RenderOceanSurfTech");
            m_pRenderSurfaceShadedPass = ShaderManager.getInstance().getProgram("Pass_PatchSolid");
            m_pRenderSurfaceWireframePass = ShaderManager.getInstance().getProgram("Pass_PatchWireframe");

            /*m_pRenderSurfaceMatViewProjVariable = m_pOceanFX->GetVariableByName("g_matViewProj")->AsMatrix();
            m_pRenderSurfaceMatViewVariable = m_pOceanFX->GetVariableByName("g_matView")->AsMatrix();
            m_pRenderSurfaceSkyColorVariable = m_pOceanFX->GetVariableByName("g_SkyColor")->AsVector();
            m_pRenderSurfaceLightDirectionVariable = m_pOceanFX->GetVariableByName("g_LightDir")->AsVector();
            m_pRenderSurfaceLightColorVariable = m_pOceanFX->GetVariableByName("g_LightColor")->AsVector();
            m_pRenderSurfaceWaterColorVariable = m_pOceanFX->GetVariableByName("g_DeepColor")->AsVector();
            m_pRenderSurfacePatchColorVariable = m_pOceanFX->GetVariableByName("g_PatchColor")->AsVector();
            m_pRenderSurfaceColorMapVariable = m_pOceanFX->GetVariableByName("g_texColorMap")->AsShaderResource();
            m_pRenderSurfaceCubeMap0Variable = m_pOceanFX->GetVariableByName("g_texCubeMap0")->AsShaderResource();
            m_pRenderSurfaceCubeMap1Variable = m_pOceanFX->GetVariableByName("g_texCubeMap1")->AsShaderResource();
            m_pRenderSurfaceCubeBlendVariable = m_pOceanFX->GetVariableByName("g_CubeBlend")->AsScalar();
            m_pRenderSurfaceCube0RotateSinCosVariable = m_pOceanFX->GetVariableByName("g_SkyCube0RotateSinCos")->AsVector();
            m_pRenderSurfaceCube1RotateSinCosVariable = m_pOceanFX->GetVariableByName("g_SkyCube1RotateSinCos")->AsVector();
            m_pRenderSurfaceCubeMultVariable = m_pOceanFX->GetVariableByName("g_SkyCubeMult")->AsVector();

            m_pSpotlightNumVariable = m_pOceanFX->GetVariableByName("g_LightsNum")->AsScalar();
            m_pRenderSurfaceSpotlightPositionVariable = m_pOceanFX->GetVariableByName("g_SpotlightPosition")->AsVector();
            m_pRenderSurfaceSpotLightAxisAndCosAngleVariable = m_pOceanFX->GetVariableByName("g_SpotLightAxisAndCosAngle")->AsVector();
            m_pRenderSurfaceSpotlightColorVariable = m_pOceanFX->GetVariableByName("g_SpotlightColor")->AsVector();
            m_pSpotlightShadowMatrixVar = m_pOceanFX->GetVariableByName("g_SpotlightMatrix")->AsMatrix();
            m_pSpotlightShadowResourceVar = m_pOceanFX->GetVariableByName("g_SpotlightResource")->AsShaderResource();

            m_pRenderSurfaceFogExponentVariable = m_pOceanFX->GetVariableByName("g_FogExponent")->AsScalar();

            m_pRenderSurfaceFoamIntensityMapVariable = m_pOceanFX->GetVariableByName("g_texFoamIntensityMap")->AsShaderResource();
            m_pRenderSurfaceFoamDiffuseMapVariable = m_pOceanFX->GetVariableByName("g_texFoamDiffuseMap")->AsShaderResource();
            m_pGlobalFoamFadeVariable = m_pOceanFX->GetVariableByName("g_GlobalFoamFade")->AsScalar();

            m_pRenderSurfaceHullProfileCoordOffsetAndScaleVariable = m_pOceanFX->GetVariableByName("g_HullProfileCoordOffsetAndScale")->AsVector();
            m_pRenderSurfaceHullProfileHeightOffsetAndHeightScaleAndTexelSizeVariable = m_pOceanFX->GetVariableByName("g_HullProfileHeightOffsetAndHeightScaleAndTexelSize")->AsVector();
            m_pRenderSurfaceHullProfileMapVariable = m_pOceanFX->GetVariableByName("g_texHullProfileMap")->AsShaderResource();

            m_pRenderSurfaceGustMapVariable = m_pOceanFX->GetVariableByName("g_texGustMap")->AsShaderResource();
            m_pRenderSurfaceGustUVVariable = m_pOceanFX->GetVariableByName("g_GustUV")->AsVector();

            m_pRenderSurfaceWakeMapVariable = m_pOceanFX->GetVariableByName("g_texWakeMap")->AsShaderResource();
            m_pRenderSurfaceShipFoamMapVariable = m_pOceanFX->GetVariableByName("g_texShipFoamMap")->AsShaderResource();
            m_pRenderSurfaceWorldToShipVariable = m_pOceanFX->GetVariableByName("g_matWorldToShip")->AsMatrix();

            m_pRenderSurfaceReflectionTextureVariable = m_pOceanFX->GetVariableByName("g_texReflection")->AsShaderResource();
            m_pRenderSurfaceReflectionPosTextureVariable = m_pOceanFX->GetVariableByName("g_texReflectionPos")->AsShaderResource();
            m_pRenderSurfaceScreenSizeInvVariable = m_pOceanFX->GetVariableByName("g_ScreenSizeInv")->AsVector();

            m_pRenderSurfaceScreenSizeInvVariable = m_pOceanFX->GetVariableByName("g_ScreenSizeInv")->AsVector();

            m_pRenderSurfaceSceneToShadowMapVariable = m_pOceanFX->GetVariableByName("g_matSceneToShadowMap")->AsMatrix();
            m_pRenderSurfaceLightningPositionVariable = m_pOceanFX->GetVariableByName("g_LightningPosition")->AsVector();
            m_pRenderSurfaceLightningColorVariable = m_pOceanFX->GetVariableByName("g_LightningColor")->AsVector();
            m_pRenderSurfaceCloudFactorVariable = m_pOceanFX->GetVariableByName("g_CloudFactor")->AsScalar();
            m_pRenderSurfaceLocalFoamMapVariable = m_pOceanFX->GetVariableByName("g_texLocalFoamMap")->AsShaderResource();*/

            m_pShiftFadeBlurLocalFoamTechnique = ShaderManager.getInstance().getProgram("LocalFoamMapTech");
            m_pShiftFadeBlurLocalFoamShadedPass = ShaderManager.getInstance().getProgram("Pass_Solid");
            /*m_pShiftFadeBlurLocalFoamTextureVariable = m_pOceanFX->GetVariableByName("g_texLocalFoamSource")->AsShaderResource();
            m_pShiftFadeBlurLocalFoamUVOffsetBlurVariable = m_pOceanFX->GetVariableByName("g_UVOffsetBlur")->AsVector();
            m_pShiftFadeBlurLocalFoamFadeAmountVariable = m_pOceanFX->GetVariableByName("g_FadeAmount")->AsScalar();

            m_pRenderSurfaceShowSpraySimVariable = m_pOceanFX->GetVariableByName("g_ShowSpraySim")->AsScalar();
            m_pRenderSurfaceShowFoamSimVariable = m_pOceanFX->GetVariableByName("g_ShowFoamSim")->AsScalar();

            m_pRenderSurfaceGustsEnabledVariable = m_pOceanFX->GetVariableByName("g_bGustsEnabled")->AsScalar();
            m_pRenderSurfaceWakeEnabledVariable = m_pOceanFX->GetVariableByName("g_bWakeEnabled")->AsScalar();

            D3DX11_PASS_SHADER_DESC passShaderDesc;

            V_RETURN(m_pRenderSurfaceShadedPass->GetVertexShaderDesc(&passShaderDesc));
            ID3D11ShaderReflection* pShadedReflectionVS = GetReflection(passShaderDesc);

            V_RETURN(m_pRenderSurfaceShadedPass->GetHullShaderDesc(&passShaderDesc));
            ID3D11ShaderReflection* pShadedReflectionHS = GetReflection(passShaderDesc);

            V_RETURN(m_pRenderSurfaceShadedPass->GetDomainShaderDesc(&passShaderDesc));
            ID3D11ShaderReflection* pShadedReflectionDS = GetReflection(passShaderDesc);

            V_RETURN(m_pRenderSurfaceShadedPass->GetPixelShaderDesc(&passShaderDesc));
            ID3D11ShaderReflection* pShadedReflectionPS = GetReflection(passShaderDesc);

            V_RETURN(m_pRenderSurfaceWireframePass->GetVertexShaderDesc(&passShaderDesc));
            ID3D11ShaderReflection* pWireframeReflectionVS = GetReflection(passShaderDesc);

            V_RETURN(m_pRenderSurfaceWireframePass->GetHullShaderDesc(&passShaderDesc));
            ID3D11ShaderReflection* pWireframeReflectionHS = GetReflection(passShaderDesc);

            V_RETURN(m_pRenderSurfaceWireframePass->GetDomainShaderDesc(&passShaderDesc));
            ID3D11ShaderReflection* pWireframeReflectionDS = GetReflection(passShaderDesc);

            V_RETURN(m_pRenderSurfaceWireframePass->GetPixelShaderDesc(&passShaderDesc));
            ID3D11ShaderReflection* pWireframeReflectionPS = GetReflection(passShaderDesc);

            UINT NumQuadtreeShaderInputs = GFSDK_WaveWorks_Quadtree_GetShaderInputCountD3D11();
            UINT NumSimulationShaderInputs = GFSDK_WaveWorks_Simulation_GetShaderInputCountD3D11();

            for(UINT i = 0; i != NumQuadtreeShaderInputs; ++i)
            {
                GFSDK_WaveWorks_ShaderInput_Desc inputDesc;
                GFSDK_WaveWorks_Quadtree_GetShaderInputDescD3D11(i, &inputDesc);

                m_pQuadTreeShaderInputMappings_Shaded[i] = GetShaderInputRegisterMapping(pShadedReflectionVS, pShadedReflectionHS, pShadedReflectionDS, pShadedReflectionPS, inputDesc);
                m_pQuadTreeShaderInputMappings_Wireframe[i] = GetShaderInputRegisterMapping(pWireframeReflectionVS, pWireframeReflectionHS, pWireframeReflectionDS, pWireframeReflectionPS, inputDesc);
            }

            for(UINT i = 0; i != NumSimulationShaderInputs; ++i)
            {
                GFSDK_WaveWorks_ShaderInput_Desc inputDesc;
                GFSDK_WaveWorks_Simulation_GetShaderInputDescD3D11(i, &inputDesc);

                m_pSimulationShaderInputMappings_Shaded[i] = GetShaderInputRegisterMapping(pShadedReflectionVS, pShadedReflectionHS, pShadedReflectionDS, pShadedReflectionPS, inputDesc);
                m_pSimulationShaderInputMappings_Wireframe[i] = GetShaderInputRegisterMapping(pWireframeReflectionVS, pWireframeReflectionHS, pWireframeReflectionDS, pWireframeReflectionPS, inputDesc);
            }

            pShadedReflectionVS->Release();
            pWireframeReflectionVS->Release();
            pShadedReflectionPS->Release();
            pWireframeReflectionPS->Release();
            pShadedReflectionHS->Release();
            pWireframeReflectionHS->Release();
            pShadedReflectionDS->Release();
            pWireframeReflectionDS->Release();*/
        }

        if(null == m_pQuadLayout)
        {
            int D3D11_INPUT_PER_VERTEX_DATA = 0;
		    final D3D11_INPUT_ELEMENT_DESC quad_layout[] = {
                new D3D11_INPUT_ELEMENT_DESC( "POSITION", 0, DXGI_FORMAT_R32G32B32_FLOAT, 0, 0, D3D11_INPUT_PER_VERTEX_DATA, 0 ),
                new D3D11_INPUT_ELEMENT_DESC( "TEXCOORD", 0, DXGI_FORMAT_R32G32_FLOAT, 0, 12, D3D11_INPUT_PER_VERTEX_DATA, 0 ),
            };
		    /*const UINT num_layout_elements = sizeof(quad_layout)/sizeof(quad_layout[0]);

            ID3DX11EffectTechnique* pDisplayBufferTech = m_pOceanFX->GetTechniqueByName("DisplayBufferTech");

            D3DX11_PASS_DESC PassDesc;
            V_RETURN(pDisplayBufferTech->GetPassByIndex(0)->GetDesc(&PassDesc));

            V_RETURN(m_pd3dDevice->CreateInputLayout(	quad_layout, num_layout_elements,
                    PassDesc.pIAInputSignature, PassDesc.IAInputSignatureSize,
                    &m_pQuadLayout
            ));*/

            m_pQuadLayout = ID3D11InputLayout.createInputLayoutFrom(quad_layout);
        }

        // Textures
        if(null == m_pFoamIntensityMap)
        {
            /*ID3D11Resource* pD3D11Resource = NULL;
            V_RETURN(D3DX11CreateTextureFromFile(m_pd3dDevice, TEXT(".\\media\\foam_intensity_perlin2.dds"), NULL, NULL, &pD3D11Resource, NULL));
            V_RETURN(m_pd3dDevice->CreateShaderResourceView(pD3D11Resource, NULL, &m_pFoamIntensityMap));
            SAFE_RELEASE(pD3D11Resource);*/

            m_pFoamIntensityMap = OceanConst.CreateTexture2DFromFile(".\\media\\foam_intensity_perlin2.dds");
        }

        if(null == m_pFoamDiffuseMap)
        {
            /*ID3D11Resource* pD3D11Resource = NULL;
            V_RETURN(D3DX11CreateTextureFromFile(m_pd3dDevice, TEXT(".\\media\\foam.dds"), NULL, NULL, &pD3D11Resource, NULL));
            V_RETURN(m_pd3dDevice->CreateShaderResourceView(pD3D11Resource, NULL, &m_pFoamDiffuseMap));
            SAFE_RELEASE(pD3D11Resource);*/
            m_pFoamDiffuseMap = OceanConst.CreateTexture2DFromFile(".\\media\\foam.dds");
        }

        if(null == m_pWakeMap)
        {
            /*ID3D11Resource* pD3D11Resource = NULL;
            V_RETURN(D3DX11CreateTextureFromFile(m_pd3dDevice, TEXT(".\\media\\wake_map.dds"), NULL, NULL, &pD3D11Resource, NULL));
            V_RETURN(m_pd3dDevice->CreateShaderResourceView(pD3D11Resource, NULL, &m_pWakeMap));
            SAFE_RELEASE(pD3D11Resource);*/
            m_pWakeMap = OceanConst.CreateTexture2DFromFile(".\\media\\wake_map.dds");
        }

        if(null == m_pShipFoamMap)
        {
            /*ID3D11Resource* pD3D11Resource = NULL;
            V_RETURN(D3DX11CreateTextureFromFile(m_pd3dDevice, TEXT(".\\media\\ship_foam_map.dds"), NULL, NULL, &pD3D11Resource, NULL));
            V_RETURN(m_pd3dDevice->CreateShaderResourceView(pD3D11Resource, NULL, &m_pShipFoamMap));
            SAFE_RELEASE(pD3D11Resource);*/
            m_pShipFoamMap = OceanConst.CreateTexture2DFromFile(".\\media\\ship_foam_map.dds");
        }

        if(null == m_pGustMap)
        {
            /*ID3D11Resource* pD3D11Resource = NULL;
            V_RETURN(D3DX11CreateTextureFromFile(m_pd3dDevice, TEXT(".\\media\\gust_ripples_map.dds"), NULL, NULL, &pD3D11Resource, NULL));
            V_RETURN(m_pd3dDevice->CreateShaderResourceView(pD3D11Resource, NULL, &m_pGustMap));
            SAFE_RELEASE(pD3D11Resource);*/
            m_pGustMap = OceanConst.CreateTexture2DFromFile(".\\media\\gust_ripples_map.dds");
        }

        boolean recreateFresnelMap = false;
        if(params.sky_blending != m_params.sky_blending)
        {
            recreateFresnelMap = true;
        }

        m_params.set(params);

        if(recreateFresnelMap)
            createFresnelMap();

        createLocalFoamMaps();

        if (ENABLE_SPRAY_PARTICLES) {
            int elementSize = (/*sizeof( float)*/4 *4) *2;

            for (int i = 0; i < 2; ++i) {
                /*CD3D11_BUFFER_DESC bufDesc
                (SPRAY_PARTICLE_COUNT * elementSize, D3D11_BIND_SHADER_RESOURCE | D3D11_BIND_UNORDERED_ACCESS, D3D11_USAGE_DEFAULT, 0, D3D11_RESOURCE_MISC_BUFFER_STRUCTURED, elementSize)
                ;
                V_RETURN(m_pd3dDevice -> CreateBuffer( & bufDesc, NULL, & m_pParticlesBuffer[i]));*/

                m_pParticlesBuffer[i] = new BufferGL();
                m_pParticlesBuffer[i].initlize(GLenum.GL_SHADER_STORAGE_BUFFER, SPRAY_PARTICLE_COUNT * elementSize, null, GLenum.GL_DYNAMIC_COPY);

                m_pParticlesBufferSRV[i] = m_pParticlesBufferUAV[i] =m_pParticlesBuffer[i];
                /*CD3D11_UNORDERED_ACCESS_VIEW_DESC descUAV
                (m_pParticlesBuffer[i], DXGI_FORMAT_UNKNOWN, 0, SPRAY_PARTICLE_COUNT);
                descUAV.Buffer.Flags = D3D11_BUFFER_UAV_FLAG_APPEND;
                V_RETURN(m_pd3dDevice -> CreateUnorderedAccessView(m_pParticlesBuffer[i], & descUAV, & m_pParticlesBufferUAV[i]))
                ;

                CD3D11_SHADER_RESOURCE_VIEW_DESC descSRV
                (m_pParticlesBuffer[i], DXGI_FORMAT_UNKNOWN, 0, SPRAY_PARTICLE_COUNT);
                V_RETURN(m_pd3dDevice -> CreateShaderResourceView(m_pParticlesBuffer[i], & descSRV, & m_pParticlesBufferSRV[i]))
                ;*/
            }

            {
                /*CD3D11_BUFFER_DESC bufDesc (sizeof( int) *
                4, D3D11_BIND_UNORDERED_ACCESS, D3D11_USAGE_DEFAULT, 0, D3D11_RESOURCE_MISC_DRAWINDIRECT_ARGS);
                D3D11_SUBRESOURCE_DATA subresourceData;
                subresourceData.pSysMem = drawArguments;
                V_RETURN(m_pd3dDevice -> CreateBuffer( & bufDesc, & subresourceData, &m_pDrawParticlesBuffer));*/

                IntBuffer drawArguments = CacheBuffer.wrap(0, 1, 1, 0);
                m_pDrawParticlesBuffer = new BufferGL();
                m_pDrawParticlesBuffer.initlize(GLenum.GL_DRAW_INDIRECT_BUFFER, 4*drawArguments.remaining(),drawArguments, GLenum.GL_DYNAMIC_COPY);

                /*CD3D11_UNORDERED_ACCESS_VIEW_DESC descUAV
                (m_pDrawParticlesBuffer, DXGI_FORMAT_R32G32B32A32_UINT, 0, 1);
                V_RETURN(m_pd3dDevice -> CreateUnorderedAccessView(m_pDrawParticlesBuffer, & descUAV, & m_pDrawParticlesBufferUAV));*/
                m_pDrawParticlesBufferUAV = m_pDrawParticlesBuffer;

                /*CD3D11_BUFFER_DESC descCB (sizeof( int) *4, D3D11_BIND_CONSTANT_BUFFER);
                V_RETURN(m_pd3dDevice -> CreateBuffer( & descCB, NULL, & m_pDrawParticlesCB));*/
                m_pDrawParticlesCB  = new BufferGL();
                m_pDrawParticlesCB.initlize(GLenum.GL_UNIFORM_BUFFER, 16, null, GLenum.GL_DYNAMIC_COPY);
            }

            m_pRenderSprayParticlesTechnique = ShaderManager.getInstance().getProgram("RenderSprayParticles");
            m_pSimulateSprayParticlesTechnique = ShaderManager.getInstance().getProgram("SimulateSprayParticles");
            m_pDispatchArgumentsTechnique = ShaderManager.getInstance().getProgram("PrepareDispatchArguments");

            /*m_pViewRightVariable = m_pOceanFX -> GetVariableByName("g_ViewRight")->AsVector();
            m_pViewUpVariable = m_pOceanFX -> GetVariableByName("g_ViewUp")->AsVector();
            m_pViewForwardVariable = m_pOceanFX -> GetVariableByName("g_ViewForward")->AsVector();
            m_pTimeStep = m_pOceanFX -> GetVariableByName("g_TimeStep")->AsScalar();
            m_pParticlesBufferVariable = m_pOceanFX -> GetVariableByName("g_SprayParticleDataSRV")->
            AsShaderResource();*/
        }

//            m_pViewForwardVariable = m_pOceanFX->GetVariableByName("g_ViewForward")->AsVector();
    }

    void beginRenderToLocalFoamMap(/*ID3D11DeviceContext* pDC,*/ Matrix4f matWorldToFoam)
    {
        matWorldToFoam.load(m_matWorldToShip);	// Same mapping as wakes

        pDC->OMSetRenderTargets(1, &m_pLocalFoamMapReceiverRTV, NULL);

        CD3D11_VIEWPORT viewport(0.0f, 0.0f, (float)LOCAL_FOAMMAP_TEX_SIZE, (float)LOCAL_FOAMMAP_TEX_SIZE);
        pDC->RSSetViewports(1, &viewport);
    }

//    private static boolean first_time = true;
    void endRenderToLocalFoamMap(/*ID3D11DeviceContext* pDC,*/ Vector2f shift_amount, float blur_amount, float fade_amount)
    {

        D3DXVECTOR4 ShiftBlurUV;
        ID3D11RenderTargetView* pRTVs[1];

        D3D11_VIEWPORT original_viewports[D3D11_VIEWPORT_AND_SCISSORRECT_OBJECT_COUNT_PER_PIPELINE];
        UINT num_original_viewports = sizeof(original_viewports)/sizeof(original_viewports[0]);
        pDC->RSGetViewports( &num_original_viewports, original_viewports);

        CD3D11_VIEWPORT viewport(0.0f, 0.0f, (float)LOCAL_FOAMMAP_TEX_SIZE, (float)LOCAL_FOAMMAP_TEX_SIZE);
        pDC->RSSetViewports(1, &viewport);

        // Blurring horizontally/shifting/fading from local foam map receiver to local foam map fader
        pRTVs[0] = m_pLocalFoamMapFaderRTV;
        pDC->OMSetRenderTargets(1, pRTVs, NULL);
        ShiftBlurUV.x = shift_amount.x; // xy must be set according to vessel speed and elapsed time
        ShiftBlurUV.y = shift_amount.y;
        ShiftBlurUV.z = 0;
        ShiftBlurUV.w = blur_amount; // must be set according to elapsed time
        m_pShiftFadeBlurLocalFoamUVOffsetBlurVariable->SetFloatVector((FLOAT*)&ShiftBlurUV);
        m_pShiftFadeBlurLocalFoamFadeAmountVariable->SetFloat(fade_amount); // must be set according to elapsed time
        // REMOVE ME, IT'S JUST A CHECK!
	/*
	if(first_time)
	{
		m_pShiftFadeBlurLocalFoamTextureVariable->SetResource(m_pGustMap);
		first_time=0;
	}
	else
	*/
        {
            m_pShiftFadeBlurLocalFoamTextureVariable->SetResource(m_pLocalFoamMapReceiverSRV);
        }
        pDC->IASetPrimitiveTopology(D3D11_PRIMITIVE_TOPOLOGY_TRIANGLESTRIP);
        m_pShiftFadeBlurLocalFoamShadedPass->Apply(0,pDC);
        pDC->Draw(4, 0);
        m_pShiftFadeBlurLocalFoamTextureVariable->SetResource(NULL);
        m_pShiftFadeBlurLocalFoamShadedPass->Apply(0,pDC);

        // Blurring vertically from local foam map fader to local foam map receiver
        pRTVs[0] = m_pLocalFoamMapReceiverRTV;
        pDC->OMSetRenderTargets(1, pRTVs, NULL);

        ShiftBlurUV.x = 0;
        ShiftBlurUV.y = 0;
        ShiftBlurUV.z = blur_amount; // must be set according to elapsed time
        ShiftBlurUV.w = 0;
        m_pShiftFadeBlurLocalFoamUVOffsetBlurVariable->SetFloatVector((FLOAT*)&ShiftBlurUV);
        m_pShiftFadeBlurLocalFoamFadeAmountVariable->SetFloat(1.0f);
        m_pShiftFadeBlurLocalFoamTextureVariable->SetResource(m_pLocalFoamMapFaderSRV);
        m_pShiftFadeBlurLocalFoamShadedPass->Apply(0,pDC);
        pDC->IASetPrimitiveTopology(D3D11_PRIMITIVE_TOPOLOGY_TRIANGLESTRIP);
        pDC->Draw(4, 0);
        m_pShiftFadeBlurLocalFoamTextureVariable->SetResource(NULL);

        pDC->RSSetViewports(num_original_viewports, original_viewports);
        pDC->OMSetRenderTargets(0, NULL, NULL);
    }

    void createLocalFoamMaps()
    {
        CommonUtil.safeRelease(m_pLocalFoamMapReceiver);
        CommonUtil.safeRelease(m_pLocalFoamMapReceiverSRV);
        CommonUtil.safeRelease(m_pLocalFoamMapReceiverRTV);
        CommonUtil.safeRelease(m_pLocalFoamMapFader);
        CommonUtil.safeRelease(m_pLocalFoamMapFaderSRV);
        CommonUtil.safeRelease(m_pLocalFoamMapFaderRTV);

        D3D11_TEXTURE2D_DESC tex_desc;
        D3D11_SHADER_RESOURCE_VIEW_DESC textureSRV_desc;

        ZeroMemory(&textureSRV_desc,sizeof(textureSRV_desc));
        ZeroMemory(&tex_desc,sizeof(tex_desc));

        tex_desc.Width              = (UINT)(LOCAL_FOAMMAP_TEX_SIZE);
        tex_desc.Height             = (UINT)(LOCAL_FOAMMAP_TEX_SIZE);
        tex_desc.MipLevels          = (UINT)max(1,log(max((float)tex_desc.Width,(float)tex_desc.Height))/(float)log(2.0f));
        tex_desc.ArraySize          = 1;
        tex_desc.Format             = DXGI_FORMAT_R16_FLOAT;
        tex_desc.SampleDesc.Count   = 1;
        tex_desc.SampleDesc.Quality = 0;
        tex_desc.Usage              = D3D11_USAGE_DEFAULT;
        tex_desc.BindFlags          = D3D11_BIND_SHADER_RESOURCE | D3D11_BIND_RENDER_TARGET;
        tex_desc.CPUAccessFlags     = 0;
        tex_desc.MiscFlags          = D3D11_RESOURCE_MISC_GENERATE_MIPS;

        textureSRV_desc.Format                    = DXGI_FORMAT_R16_FLOAT;
        textureSRV_desc.ViewDimension             = D3D11_SRV_DIMENSION_TEXTURE2D;
        textureSRV_desc.Texture2D.MipLevels = tex_desc.MipLevels;
        textureSRV_desc.Texture2D.MostDetailedMip = 0;

        m_pd3dDevice->CreateTexture2D(&tex_desc, NULL, &m_pLocalFoamMapReceiver);
        m_pd3dDevice->CreateShaderResourceView(m_pLocalFoamMapReceiver, &textureSRV_desc, &m_pLocalFoamMapReceiverSRV);
        m_pd3dDevice->CreateRenderTargetView(m_pLocalFoamMapReceiver, NULL, &m_pLocalFoamMapReceiverRTV);
        m_pd3dDevice->CreateTexture2D(&tex_desc, NULL, &m_pLocalFoamMapFader);
        m_pd3dDevice->CreateShaderResourceView(m_pLocalFoamMapFader, &textureSRV_desc, &m_pLocalFoamMapFaderSRV);
        m_pd3dDevice->CreateRenderTargetView(m_pLocalFoamMapFader, NULL, &m_pLocalFoamMapFaderRTV);
    }

    void createFresnelMap()
    {
        SAFE_RELEASE(m_pBicolorMap);

        DWORD buffer[BICOLOR_TEX_SIZE];
        for (int i = 0; i < BICOLOR_TEX_SIZE; i++)
        {
            // R channel: fresnel term
            // G channel: sky/envmap blend term
            // B channel:
            // A channel:

            float cos_a = i / (FLOAT)BICOLOR_TEX_SIZE;
            float fresnel = D3DXFresnelTerm(cos_a, 1.33f);
            DWORD weight = (DWORD)(fresnel * 255);

            fresnel = powf(1 / (1 + cos_a), m_params.sky_blending);
            DWORD refl_fresnel = (DWORD)(fresnel * 255);

            buffer[i] = (weight) | (refl_fresnel << 8);
        }

        D3D11_TEXTURE1D_DESC texDesc;
        texDesc.Width = BICOLOR_TEX_SIZE;
        texDesc.MipLevels = 1;
        texDesc.ArraySize = 1;
        texDesc.Format = DXGI_FORMAT_R8G8B8A8_UNORM_SRGB;
        texDesc.Usage = D3D11_USAGE_IMMUTABLE;
        texDesc.BindFlags = D3D11_BIND_SHADER_RESOURCE;
        texDesc.CPUAccessFlags = 0;
        texDesc.MiscFlags = 0;

        D3D11_SUBRESOURCE_DATA srd;
        srd.pSysMem = buffer;
        srd.SysMemPitch = 0;
        srd.SysMemSlicePitch = 0;

        ID3D11Texture1D* pTextureResource = NULL;
        m_pd3dDevice->CreateTexture1D(&texDesc, &srd, &pTextureResource);
        m_pd3dDevice->CreateShaderResourceView(pTextureResource, NULL, &m_pBicolorMap);
        SAFE_RELEASE(pTextureResource);
    }

    void setHullProfiles(OceanHullProfile hullProfiles, int NumHullProfiles)
    {
        assert(NumHullProfiles <= MaxNumVessels);

        D3DXVECTOR4 vHullProfileCoordOffsetAndScales[MaxNumVessels];
        D3DXVECTOR4 vHullProfileHeightOffsetAndHeightScaleAndTexelSizes[MaxNumVessels];
        ID3D11ShaderResourceView* pHullProfileSRVs[MaxNumVessels];
        for(UINT i = 0; i != NumHullProfiles; ++i) {
            vHullProfileCoordOffsetAndScales[i] = D3DXVECTOR4(	hullProfiles[i].m_WorldToProfileCoordsOffset.x,
                    hullProfiles[i].m_WorldToProfileCoordsOffset.y,
                    hullProfiles[i].m_WorldToProfileCoordsScale.x,
                    hullProfiles[i].m_WorldToProfileCoordsScale.y);
            vHullProfileHeightOffsetAndHeightScaleAndTexelSizes[i] = D3DXVECTOR4(hullProfiles[i].m_ProfileToWorldHeightOffset, hullProfiles[i].m_ProfileToWorldHeightScale, hullProfiles[i].m_TexelSizeInWorldSpace, 0.f);
            pHullProfileSRVs[i] = hullProfiles[i].GetSRV();
        }

        m_pRenderSurfaceHullProfileMapVariable->SetResourceArray(pHullProfileSRVs,0,NumHullProfiles);
        m_pRenderSurfaceHullProfileCoordOffsetAndScaleVariable->SetFloatVectorArray((FLOAT*)&vHullProfileCoordOffsetAndScales,0,NumHullProfiles);
        m_pRenderSurfaceHullProfileHeightOffsetAndHeightScaleAndTexelSizeVariable->SetFloatVectorArray((FLOAT*)&vHullProfileHeightOffsetAndHeightScaleAndTexelSizes,0,NumHullProfiles);
    }

    void OceanSurface::renderWireframe(	ID3D11DeviceContext* pDC,
									const D3DXMATRIX& matView, const D3DXMATRIX& matProj,
                                           GFSDK_WaveWorks_SimulationHandle hSim, GFSDK_WaveWorks_SavestateHandle hSavestate,
                                           BOOL freeze_cam
    )
    {
        // Matrices
        D3DXMATRIX matVP =  matView * matProj;
        m_pRenderSurfaceMatViewProjVariable->SetMatrix((FLOAT*)&matVP);
        m_pRenderSurfaceMatViewVariable->SetMatrix((FLOAT*)&matView);
        m_pRenderSurfaceWorldToShipVariable->SetMatrix((FLOAT*)&m_matWorldToShip);

        // Wireframe color
        D3DXVECTOR4 patch_color(0.2f, 0.2f, 0.2f, 1.f);
        m_pRenderSurfacePatchColorVariable->SetFloatVector((FLOAT*)&patch_color);

        D3D11_VIEWPORT vp;
        UINT NumViewports = 1;
        pDC->RSGetViewports(&NumViewports,&vp);

        if(!freeze_cam) {
            m_pSurfaceState->m_matView = matView;
            m_pSurfaceState->m_matProj = matProj;
        }

        m_pRenderSurfaceWireframePass->Apply(0,pDC);
        GFSDK_WaveWorks_Simulation_SetRenderStateD3D11(hSim, pDC, NvFromDX(m_pSurfaceState->m_matView), m_pSimulationShaderInputMappings_Wireframe, hSavestate);
        GFSDK_WaveWorks_Quadtree_DrawD3D11(m_hOceanQuadTree, pDC, NvFromDX(m_pSurfaceState->m_matView), NvFromDX(m_pSurfaceState->m_matProj), m_pQuadTreeShaderInputMappings_Wireframe, hSavestate);
        GFSDK_WaveWorks_Savestate_RestoreD3D11(hSavestate, pDC);

        // Release ref to hull profile
        m_pRenderSurfaceHullProfileMapVariable->SetResource(0);
        m_pRenderSurfaceShadedPass->Apply(0,pDC);
    }

    void renderShaded(ID3D11DeviceContext* pDC,
								const D3DXMATRIX& matCameraView, const D3DXMATRIX& matView, const D3DXMATRIX& matProj,
                                    GFSDK_WaveWorks_SimulationHandle hSim, GFSDK_WaveWorks_SavestateHandle hSavestate,
								const OceanEnvironment& ocean_env,
                                    BOOL freeze_cam,
                                    float foam_fade,
                                    bool show_spray_sim,
                                    bool show_foam_sim,
                                    bool gusts_enabled,
                                    bool wake_enabled
    )
    {
        m_pRenderSurfaceColorMapVariable->SetResource(m_pBicolorMap);
        m_pRenderSurfaceCubeMap0Variable->SetResource(ocean_env.pSky0->m_pReflectionSRV);
        m_pRenderSurfaceCubeMap1Variable->SetResource(ocean_env.pSky1->m_pReflectionSRV);
        m_pRenderSurfaceCubeBlendVariable->SetFloat(ocean_env.sky_interp);
        m_pRenderSurfaceFoamIntensityMapVariable->SetResource(m_pFoamIntensityMap);
        m_pRenderSurfaceFoamDiffuseMapVariable->SetResource(m_pFoamDiffuseMap);
        m_pRenderSurfaceWakeMapVariable->SetResource(m_pWakeMap);
        m_pRenderSurfaceShipFoamMapVariable->SetResource(m_pShipFoamMap);
        m_pRenderSurfaceGustMapVariable->SetResource(m_pGustMap);
        m_pRenderSurfaceReflectionTextureVariable->SetResource(ocean_env.pPlanarReflectionSRV);
        m_pRenderSurfaceReflectionPosTextureVariable->SetResource(ocean_env.pPlanarReflectionPosSRV);
        m_pRenderSurfaceLocalFoamMapVariable->SetResource(m_pLocalFoamMapReceiverSRV);

        m_pRenderSurfaceGustUVVariable->SetFloatVector((FLOAT*)&ocean_env.gust_UV);
        m_pRenderSurfaceLightningColorVariable->SetFloatVector((FLOAT*)&ocean_env.lightning_light_intensity);
        m_pRenderSurfaceLightningPositionVariable->SetFloatVector((FLOAT*)&ocean_env.lightning_light_position);

        m_pGlobalFoamFadeVariable->SetFloat(foam_fade);

        D3D11_VIEWPORT viewport;
        UINT numviewports = 1;
        pDC->RSGetViewports(&numviewports,&viewport);
        D3DXVECTOR4 ScreenSizeInv = D3DXVECTOR4(1.0f/(float)viewport.Width,1.0f/(float)viewport.Height,0,0);
        m_pRenderSurfaceScreenSizeInvVariable->SetFloatVector((FLOAT*)&ScreenSizeInv);

        D3DXVECTOR4 sincos0;
        sincos0.x = sinf(ocean_env.pSky0->m_Orientation);
        sincos0.y = cosf(ocean_env.pSky0->m_Orientation);
        m_pRenderSurfaceCube0RotateSinCosVariable->SetFloatVector((FLOAT*)&sincos0);

        D3DXVECTOR4 sincos1;
        sincos1.x = sinf(ocean_env.pSky1->m_Orientation);
        sincos1.y = cosf(ocean_env.pSky1->m_Orientation);
        m_pRenderSurfaceCube1RotateSinCosVariable->SetFloatVector((FLOAT*)&sincos1);

        m_pRenderSurfaceCubeMultVariable->SetFloatVector((FLOAT*)&ocean_env.sky_map_color_mult);

        // Colors
        m_pRenderSurfaceSkyColorVariable->SetFloatVector((FLOAT*)&ocean_env.sky_color);
        m_pRenderSurfaceWaterColorVariable->SetFloatVector((FLOAT*)&m_params.waterbody_color);

        // Lighting
        m_pRenderSurfaceLightDirectionVariable->SetFloatVector((FLOAT*)&ocean_env.main_light_direction);
        m_pRenderSurfaceLightColorVariable->SetFloatVector((FLOAT*)&ocean_env.main_light_color);

        // Spot lights - transform to view space
        D3DXMATRIX matSpotlightsToView = ocean_env.spotlights_to_world_matrix * matCameraView;
        D3DXMATRIX matViewToSpotlights;
        D3DXMatrixInverse(&matViewToSpotlights,NULL,&matSpotlightsToView);

        D3DXVECTOR4 spotlight_position[MaxNumSpotlights];
        D3DXVECTOR4 spotlight_axis_and_cos_angle[MaxNumSpotlights];

        D3DXVec4TransformArray(spotlight_position,sizeof(spotlight_position[0]),ocean_env.spotlight_position,sizeof(ocean_env.spotlight_position[0]),&matSpotlightsToView,MaxNumSpotlights);
        D3DXVec3TransformNormalArray((D3DXVECTOR3*)spotlight_axis_and_cos_angle,sizeof(spotlight_axis_and_cos_angle[0]),(D3DXVECTOR3*)ocean_env.spotlight_axis_and_cos_angle,sizeof(ocean_env.spotlight_axis_and_cos_angle[0]),&matSpotlightsToView,MaxNumSpotlights);

        for(int i=0; i!=MaxNumSpotlights; ++i) {

            spotlight_axis_and_cos_angle[i].w = ocean_env.spotlight_axis_and_cos_angle[i].w;

#if ENABLE_SHADOWS
            D3DXMATRIX spotlight_shadow_matrix = matViewToSpotlights * ocean_env.spotlight_shadow_matrix[i];
            m_pSpotlightShadowMatrixVar->SetMatrixArray((float*)&spotlight_shadow_matrix, i, 1);
            m_pSpotlightShadowResourceVar->SetResourceArray((ID3D11ShaderResourceView**)&ocean_env.spotlight_shadow_resource[i], i, 1);
#endif
        }

        // Spot lights
        m_pSpotlightNumVariable->SetInt(ocean_env.activeLightsNum);
        m_pRenderSurfaceSpotlightPositionVariable->SetFloatVectorArray((FLOAT*)spotlight_position,0,MaxNumSpotlights);
        m_pRenderSurfaceSpotLightAxisAndCosAngleVariable->SetFloatVectorArray((FLOAT*)spotlight_axis_and_cos_angle,0,MaxNumSpotlights);
        m_pRenderSurfaceSpotlightColorVariable->SetFloatVectorArray((FLOAT*)ocean_env.spotlight_color,0,MaxNumSpotlights);

        // Fog
        m_pRenderSurfaceFogExponentVariable->SetFloat(ocean_env.fog_exponent);

        // Cloud Factor
        m_pRenderSurfaceCloudFactorVariable->SetFloat(ocean_env.cloud_factor);

        // Show spray and foam rendering variables
        m_pRenderSurfaceShowSpraySimVariable->SetFloat(show_spray_sim?1.0f:0.0f);
        m_pRenderSurfaceShowFoamSimVariable->SetFloat(show_foam_sim?1.0f:0.0f);

        // Gusts/wakes
        m_pRenderSurfaceGustsEnabledVariable->SetBool(gusts_enabled);
        m_pRenderSurfaceWakeEnabledVariable->SetBool(wake_enabled);

        // Matrices
        D3DXMATRIX matVP =  matView * matProj;
        m_pRenderSurfaceMatViewProjVariable->SetMatrix((FLOAT*)&matVP);
        m_pRenderSurfaceMatViewVariable->SetMatrix((FLOAT*)&matView);
        m_pRenderSurfaceWorldToShipVariable->SetMatrix((FLOAT*)&m_matWorldToShip);

        D3DXMATRIX matViewInv;
        D3DXMatrixInverse(&matViewInv, NULL, &matView);
        m_pViewForwardVariable->SetFloatVector((float*)&matViewInv._31);

        D3D11_VIEWPORT vp;
        UINT NumViewports = 1;
        pDC->RSGetViewports(&NumViewports,&vp);

        if(!freeze_cam) {
            m_pSurfaceState->m_matView = matView;
            m_pSurfaceState->m_matProj = matProj;
        }

        m_pRenderSurfaceShadedPass->Apply(0,pDC);

#if ENABLE_SPRAY_PARTICLES
        UINT count = (UINT)-1;
        pDC->OMSetRenderTargetsAndUnorderedAccessViews(D3D11_KEEP_RENDER_TARGETS_AND_DEPTH_STENCIL, NULL, NULL, 1, 1, &m_pParticlesBufferUAV[m_ParticleWriteBuffer], &count);
#endif

        GFSDK_WaveWorks_Simulation_SetRenderStateD3D11(hSim, pDC, NvFromDX(m_pSurfaceState->m_matView), m_pSimulationShaderInputMappings_Shaded, hSavestate);
        GFSDK_WaveWorks_Quadtree_DrawD3D11(m_hOceanQuadTree, pDC, NvFromDX(m_pSurfaceState->m_matView), NvFromDX(m_pSurfaceState->m_matProj), m_pQuadTreeShaderInputMappings_Shaded, hSavestate);
        GFSDK_WaveWorks_Savestate_RestoreD3D11(hSavestate, pDC);

        // Release refs to inputs
        ID3D11ShaderResourceView* pNullSRVs[max(MaxNumVessels,MaxNumSpotlights)];
        memset(pNullSRVs,0,sizeof(pNullSRVs));
        m_pRenderSurfaceHullProfileMapVariable->SetResourceArray(pNullSRVs,0,MaxNumVessels);
        m_pRenderSurfaceReflectionTextureVariable->SetResource(NULL);
        m_pRenderSurfaceReflectionPosTextureVariable->SetResource(NULL);
        m_pSpotlightShadowResourceVar->SetResourceArray(pNullSRVs,0,MaxNumSpotlights);
        m_pRenderSurfaceLocalFoamMapVariable->SetResource(NULL);
        m_pRenderSurfaceShadedPass->Apply(0,pDC);

#if ENABLE_SPRAY_PARTICLES
        ID3D11UnorderedAccessView* pNullUAV = NULL;
        pDC->OMSetRenderTargetsAndUnorderedAccessViews(D3D11_KEEP_RENDER_TARGETS_AND_DEPTH_STENCIL, NULL, NULL, 1, 1, &pNullUAV, NULL);

        pDC->CopyStructureCount(m_pDrawParticlesCB, 0, m_pParticlesBufferUAV[m_ParticleWriteBuffer]);
#endif
    }
    void getQuadTreeStats(GFSDK_WaveWorks_Quadtree_Stats stats)
    {
        GFSDK_WaveWorks.GFSDK_WaveWorks_Quadtree_GetStats(m_hOceanQuadTree, stats);
    }

    void setWorldToShipMatrix(Matrix4f matShipToWorld)
    {
//        D3DXMatrixInverse(&m_matWorldToShip,NULL,&matShipToWorld);
        Matrix4f.invert(matShipToWorld, m_matWorldToShip);
    }

    @Override
    public void dispose() {

    }
}
