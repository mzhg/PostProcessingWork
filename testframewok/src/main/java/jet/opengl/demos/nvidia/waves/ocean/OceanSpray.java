package jet.opengl.demos.nvidia.waves.ocean;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import jet.opengl.demos.intel.cput.D3D11_INPUT_ELEMENT_DESC;
import jet.opengl.demos.intel.cput.ID3D11InputLayout;
import jet.opengl.demos.nvidia.waves.wavework.GFSDK_WaveWorks_Simulation;
import jet.opengl.demos.scene.CameraData;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.Numeric;

final class OceanSpray implements OceanConst, Disposeable {
    static final int NUMSPRAYPARTICLES = 50000;
    static final int NUMSPRAYGENERATORS = OceanHullSensors.MaxNumSensors;
    static final int NUMRIMSPRAYGENERATORS = OceanHullSensors.MaxNumRimSensors;

    private static final float kParticleKillDepth = 3.f;
    private static final float kParticleInitialScale = 0.75f;
    private static final float kParticleJitter = 0.5f;
    private static final float kBowGenThreshold = 0.5f;

    static boolean ENABLE_GPU_SIMULATION = true;
    static boolean SPRAY_PARTICLE_SORTING = true;

//    ID3DX11Effect* m_pFX;
    private GLSLProgram m_pRenderSprayToSceneTechTechnique;
    private GLSLProgram m_pRenderSprayToPSMTechnique;
    private GLSLProgram m_pRenderSprayToFoamTechnique;
    private GLSLProgram m_pInitSortTechnique;
    private GLSLProgram m_pBitonicSortTechnique;
    private GLSLProgram m_pMatrixTransposeTechnique;
    private GLSLProgram m_pSensorVisualizationTechnique;
    private GLSLProgram m_pAudioVisualizationTechnique;

    /*ID3DX11EffectShaderResourceVariable* m_pTexSplashVariable;
    ID3DX11EffectShaderResourceVariable* m_pTexDepthVariable;
    ID3DX11EffectShaderResourceVariable* m_pRenderInstanceDataVariable;
    ID3DX11EffectShaderResourceVariable* m_pRenderVelocityAndTimeDataVariable;
    ID3DX11EffectShaderResourceVariable* m_pRenderOrientationAndDecimationDataVariable;
    ID3DX11EffectMatrixVariable* m_pMatProjVariable;
    ID3DX11EffectMatrixVariable* m_pMatViewVariable;
    ID3DX11EffectMatrixVariable* m_pMatProjInvVariable;
    ID3DX11EffectVectorVariable* m_pLightDirectionVariable;
    ID3DX11EffectVectorVariable* m_pLightColorVariable;
    ID3DX11EffectVectorVariable* m_pAmbientColorVariable;
    ID3DX11EffectScalarVariable* m_pFogExponentVariable;
    ID3DX11EffectScalarVariable* m_pInvParticleLifeTimeVariable;
    ID3DX11EffectScalarVariable* m_pSimpleParticlesVariable;

    ID3DX11EffectVectorVariable* m_pLightningPositionVariable;
    ID3DX11EffectVectorVariable* m_pLightningColorVariable;

    ID3DX11EffectScalarVariable* m_pSpotlightNumVariable;
    ID3DX11EffectVectorVariable* m_pRenderSurfaceSpotlightPositionVariable;
    ID3DX11EffectVectorVariable* m_pRenderSurfaceSpotLightAxisAndCosAngleVariable;
    ID3DX11EffectVectorVariable* m_pRenderSurfaceSpotlightColorVariable;
    ID3DX11EffectMatrixVariable* m_pSpotlightShadowMatrixVar;
    ID3DX11EffectShaderResourceVariable* m_pSpotlightShadowResourceVar;

    // Depth sort params
    ID3DX11EffectScalarVariable* m_piDepthSortLevelVariable;
    ID3DX11EffectScalarVariable* m_piDepthSortLevelMaskVariable;
    ID3DX11EffectScalarVariable* m_piDepthSortWidthVariable;
    ID3DX11EffectScalarVariable* m_piDepthSortHeightVariable;
    ID3DX11EffectUnorderedAccessViewVariable* m_pParticleDepthSortUAVVariable;
    ID3DX11EffectShaderResourceVariable* m_pParticleDepthSortSRVVariable;

    // PSM params
    ID3DX11EffectMatrixVariable* m_pMatViewToPSMVariable;
    OceanPSMParams* m_pPSMParams;*/

    // D3D objects
//    ID3D11Device* m_pd3dDevice;
    private Texture2D          m_pSplashTextureSRV;
    private BufferGL           m_pRenderInstanceDataBuffer;
    private BufferGL           m_pRenderInstanceDataSRV;
    private BufferGL           m_pRenderVelocityDataBuffer;
    private BufferGL           m_pRenderVelocityDataSRV;
    private BufferGL           m_pRenderOrientationsAndDecimationsBuffer;
    private BufferGL           m_pRenderOrientationsAndDecimationsSRV;

    private BufferGL           m_pDepthSort1SRV;
    private BufferGL           m_pDepthSort1UAV;
    private BufferGL           m_pDepthSort2SRV;
    private BufferGL           m_pDepthSort2UAV;

    private OceanHullSensors   m_pHullSensors;
    private final Vector3f[]   m_SprayGeneratorShipSpaceSprayDir = new Vector3f[NUMSPRAYGENERATORS];
    private final Vector3f[]   m_SprayGeneratorWorldSpaceSprayDir = new Vector3f[NUMSPRAYGENERATORS];
    private final Vector4f[]   m_displacementsOld = new Vector4f[NUMSPRAYGENERATORS];
    private final Vector3f[]   m_displacementsSpeed = new Vector3f[NUMSPRAYGENERATORS];
    private final Vector3f[]   m_WorldSensorPositionsOld = new Vector3f[NUMSPRAYGENERATORS];

    private BufferGL	       m_pSensorsVisualizationVertexBuffer;
    private BufferGL	       m_pSensorsVisualizationIndexBuffer;
    private ID3D11InputLayout  m_pSensorVisualizationLayout;

    private final int[]		   m_sprayAmount = new int[NUMSPRAYGENERATORS];
    private final float[]	   m_sprayVizualisationFade = new float[NUMSPRAYGENERATORS];
    private final Vector3f[]   m_spraySpeed = new Vector3f[NUMSPRAYGENERATORS];
    private final Vector3f[]   m_sprayOffset = new Vector3f[NUMSPRAYGENERATORS];

    private final Vector3f[]   m_hullSpeed = new Vector3f[NUMSPRAYGENERATORS];

    private final Vector3f[]   m_WorldRimSensorPositionsOld = new Vector3f[NUMRIMSPRAYGENERATORS];
    private final Vector3f[]   m_RimSprayGeneratorShipSpaceSprayDir = new Vector3f[NUMRIMSPRAYGENERATORS];
    private final Vector3f[]   m_RimSprayGeneratorWorldSpaceSprayDir = new Vector3f[NUMRIMSPRAYGENERATORS];
    private final int[]		   m_rimSprayAmount = new int[NUMRIMSPRAYGENERATORS];
    private final float[]	   m_rimSprayVizualisationFade = new float[NUMRIMSPRAYGENERATORS];

    private final Vector3f[]   m_particlePosition = new Vector3f[NUMSPRAYPARTICLES];
    private final Vector3f[]   m_particleSpeed = new Vector3f[NUMSPRAYPARTICLES];
    private final float[]	   m_particleTime = new float[NUMSPRAYPARTICLES];
    private final float[]	   m_particleScale = new float[NUMSPRAYPARTICLES];
    private final short[]	   m_particleOrientations = new short[2*NUMSPRAYPARTICLES];
    private int		           m_numParticlesAlive;

    private boolean			   m_firstUpdate;

    // GPU simulation variables
    private final BufferGL[]	m_pParticlesBuffer = new BufferGL[2];
    private final BufferGL[]	m_pParticlesBufferUAV = new BufferGL[2];
    private final BufferGL[]	m_pParticlesBufferSRV = new BufferGL[2];

    private int 			    m_ParticleWriteBuffer;

    private BufferGL			m_pDrawParticlesCB;
    private BufferGL			m_pDrawParticlesBuffer;
    private BufferGL			m_pDrawParticlesBufferUAV;
    private BufferGL			m_pDrawParticlesBufferStaging;

    private GLSLProgram		    m_pInitSprayParticlesTechnique;
    private GLSLProgram		    m_pSimulateSprayParticlesTechnique;
    private GLSLProgram		    m_pDispatchArgumentsTechnique;

    /*ID3DX11EffectScalarVariable* m_pParticlesNum;
    ID3DX11EffectScalarVariable* m_pSimulationTime;
    ID3DX11EffectVectorVariable* m_pWindSpeed;
    ID3DX11EffectShaderResourceVariable* m_pParticlesBufferVariable;

    ID3DX11EffectMatrixVariable* m_pWorldToVesselVariable;
    ID3DX11EffectMatrixVariable* m_pVesselToWorldVariable;

    ID3DX11EffectVectorVariable* m_worldToHeightLookupScaleVariable;
    ID3DX11EffectVectorVariable* m_worldToHeightLookupRotVariable;
    ID3DX11EffectVectorVariable* m_worldToHeightLookupOffsetVariable;
    ID3DX11EffectShaderResourceVariable* m_texHeightLookupVariable;

    ID3DX11EffectMatrixVariable* m_pMatWorldToFoamVariable;

    ID3DX11EffectVectorVariable* m_pAudioVisualizationRectVariable;
    ID3DX11EffectVectorVariable* m_pAudioVisualizationMarginVariable;
    ID3DX11EffectScalarVariable* m_pAudioVisualizationLevelVariable;*/

    private float m_SplashPowerFromLastUpdate;

    void init(OceanHullSensors pHullSensors){
        /*ID3DXBuffer* pEffectBuffer = NULL;
        V_RETURN(LoadFile(TEXT(".\\Media\\ocean_spray_d3d11.fxo"), &pEffectBuffer));
        V_RETURN(D3DX11CreateEffectFromMemory(pEffectBuffer->GetBufferPointer(), pEffectBuffer->GetBufferSize(), 0, m_pd3dDevice, &m_pFX));
        pEffectBuffer->Release();*/

        m_pRenderSprayToSceneTechTechnique = m_pFX->GetTechniqueByName("RenderSprayToSceneTech");
        m_pRenderSprayToPSMTechnique = m_pFX->GetTechniqueByName("RenderSprayToPSMTech");
        m_pRenderSprayToFoamTechnique = m_pFX->GetTechniqueByName("RenderSprayToFoamTech");
        m_pInitSortTechnique = m_pFX->GetTechniqueByName("InitSortTech");
        m_pBitonicSortTechnique = m_pFX->GetTechniqueByName("BitonicSortTech");
        m_pMatrixTransposeTechnique = m_pFX->GetTechniqueByName("MatrixTransposeTech");
        m_pAudioVisualizationTechnique = m_pFX->GetTechniqueByName("AudioVisualizationTech");

        /*m_pTexSplashVariable = m_pFX->GetVariableByName("g_texSplash")->AsShaderResource();
        m_pTexDepthVariable = m_pFX->GetVariableByName("g_texDepth")->AsShaderResource();
        m_pRenderInstanceDataVariable = m_pFX->GetVariableByName("g_RenderInstanceData")->AsShaderResource();
        m_pRenderVelocityAndTimeDataVariable = m_pFX->GetVariableByName("g_RenderVelocityAndTimeData")->AsShaderResource();
        m_pRenderOrientationAndDecimationDataVariable = m_pFX->GetVariableByName("g_RenderOrientationAndDecimationData")->AsShaderResource();
        m_pMatProjVariable = m_pFX->GetVariableByName("g_matProj")->AsMatrix();
        m_pMatProjInvVariable = m_pFX->GetVariableByName("g_matProjInv")->AsMatrix();
        m_pMatViewVariable = m_pFX->GetVariableByName("g_matView")->AsMatrix();
        m_pLightDirectionVariable = m_pFX->GetVariableByName("g_LightDirection")->AsVector();
        m_pLightColorVariable = m_pFX->GetVariableByName("g_LightColor")->AsVector();
        m_pAmbientColorVariable = m_pFX->GetVariableByName("g_AmbientColor")->AsVector();
        m_pFogExponentVariable = m_pFX->GetVariableByName("g_FogExponent")->AsScalar();
        m_pInvParticleLifeTimeVariable = m_pFX->GetVariableByName("g_InvParticleLifeTime")->AsScalar();
        m_pSimpleParticlesVariable = m_pFX->GetVariableByName("g_SimpleParticles")->AsScalar();

        m_pLightningPositionVariable = m_pFX->GetVariableByName("g_LightningPosition")->AsVector();
        m_pLightningColorVariable = m_pFX->GetVariableByName("g_LightningColor")->AsVector();

        m_pSpotlightNumVariable = m_pFX->GetVariableByName("g_LightsNum")->AsScalar();
        m_pRenderSurfaceSpotlightPositionVariable = m_pFX->GetVariableByName("g_SpotlightPosition")->AsVector();
        m_pRenderSurfaceSpotLightAxisAndCosAngleVariable = m_pFX->GetVariableByName("g_SpotLightAxisAndCosAngle")->AsVector();
        m_pRenderSurfaceSpotlightColorVariable = m_pFX->GetVariableByName("g_SpotlightColor")->AsVector();
        m_pSpotlightShadowMatrixVar = m_pFX->GetVariableByName("g_SpotlightMatrix")->AsMatrix();
        m_pSpotlightShadowResourceVar = m_pFX->GetVariableByName("g_SpotlightResource")->AsShaderResource();

        m_pMatViewToPSMVariable = m_pFX->GetVariableByName("g_matViewToPSM")->AsMatrix();
        m_pPSMParams = new OceanPSMParams(m_pFX);

        // Fire-and-forgets
        m_pFX->GetVariableByName("g_PSMOpacityMultiplier")->AsScalar()->SetFloat(kPSMOpacityMultiplier);

        m_piDepthSortLevelVariable = m_pFX->GetVariableByName("g_iDepthSortLevel")->AsScalar();
        m_piDepthSortLevelMaskVariable = m_pFX->GetVariableByName("g_iDepthSortLevelMask")->AsScalar();
        m_piDepthSortWidthVariable = m_pFX->GetVariableByName("g_iDepthSortWidth")->AsScalar();
        m_piDepthSortHeightVariable = m_pFX->GetVariableByName("g_iDepthSortHeight")->AsScalar();
        m_pParticleDepthSortUAVVariable = m_pFX->GetVariableByName("g_ParticleDepthSortUAV")->AsUnorderedAccessView();
        m_pParticleDepthSortSRVVariable = m_pFX->GetVariableByName("g_ParticleDepthSortSRV")->AsShaderResource();

        m_pAudioVisualizationRectVariable = m_pFX->GetVariableByName("g_AudioVisualizationRect")->AsVector();
        m_pAudioVisualizationMarginVariable = m_pFX->GetVariableByName("g_AudioVisualizationMargin")->AsVector();
        m_pAudioVisualizationLevelVariable = m_pFX->GetVariableByName("g_AudioVisualizationLevel")->AsScalar();*/

        if(null == m_pSplashTextureSRV)
        {
            /*ID3D11Resource* pD3D11Resource = NULL;
            V_RETURN(D3DX11CreateTextureFromFile(m_pd3dDevice, TEXT(".\\media\\splash.dds"), NULL, NULL, &pD3D11Resource, NULL));
            V_RETURN(m_pd3dDevice->CreateShaderResourceView(pD3D11Resource, NULL, &m_pSplashTextureSRV));
            SAFE_RELEASE(pD3D11Resource);*/

            m_pSplashTextureSRV = OceanConst.CreateTexture2DFromFile(".\\media\\splash.dds");
        }

        {
            /*D3D11_BUFFER_DESC buffer_desc;
            buffer_desc.ByteWidth = NUMSPRAYPARTICLES * sizeof(D3DXVECTOR4);
            buffer_desc.Usage = D3D11_USAGE_DYNAMIC; *//*D3D11_USAGE_DEFAULT*//*
            buffer_desc.BindFlags = D3D11_BIND_SHADER_RESOURCE *//*| D3D11_BIND_UNORDERED_ACCESS*//*;
            buffer_desc.CPUAccessFlags = D3D11_CPU_ACCESS_WRITE; *//*0*//*
            buffer_desc.MiscFlags = 0;
            buffer_desc.StructureByteStride = 0;

            D3D11_SHADER_RESOURCE_VIEW_DESC srv_desc;
            srv_desc.Format = DXGI_FORMAT_R32G32B32A32_FLOAT;
            srv_desc.ViewDimension = D3D11_SRV_DIMENSION_BUFFER;
            srv_desc.Buffer.FirstElement = 0;
            srv_desc.Buffer.NumElements = NUMSPRAYPARTICLES;

            V_RETURN(m_pd3dDevice->CreateBuffer(&buffer_desc, NULL, &m_pRenderInstanceDataBuffer));
            V_RETURN(m_pd3dDevice->CreateShaderResourceView(m_pRenderInstanceDataBuffer, &srv_desc, &m_pRenderInstanceDataSRV));*/

            m_pRenderInstanceDataBuffer = new BufferGL();
            m_pRenderInstanceDataBuffer.initlize(GLenum.GL_SHADER_STORAGE_BUFFER, NUMSPRAYPARTICLES*Vector4f.SIZE, null, GLenum.GL_DYNAMIC_COPY);
            m_pRenderInstanceDataSRV = m_pRenderInstanceDataBuffer;

            /*V_RETURN(m_pd3dDevice->CreateBuffer(&buffer_desc, NULL, &m_pRenderVelocityDataBuffer));
            V_RETURN(m_pd3dDevice->CreateShaderResourceView(m_pRenderVelocityDataBuffer, &srv_desc, &m_pRenderVelocityDataSRV));*/
            // V_RETURN(m_pd3dDevice->CreateUnorderedAccessView(pBuffer, &uav_desc, &m_pSimulationInstanceDataUAV));

            m_pRenderVelocityDataBuffer = new BufferGL();
            m_pRenderVelocityDataBuffer.initlize(GLenum.GL_SHADER_STORAGE_BUFFER, NUMSPRAYPARTICLES*Vector4f.SIZE, null, GLenum.GL_DYNAMIC_COPY);
            m_pRenderVelocityDataSRV = m_pRenderVelocityDataBuffer;
        }

        {
            /*D3D11_BUFFER_DESC buffer_desc;
            buffer_desc.ByteWidth = 4 * NUMSPRAYPARTICLES * sizeof(WORD);
            buffer_desc.Usage = D3D11_USAGE_DYNAMIC; *//*D3D11_USAGE_DEFAULT*//*
            buffer_desc.BindFlags = D3D11_BIND_SHADER_RESOURCE *//*| D3D11_BIND_UNORDERED_ACCESS*//*;
            buffer_desc.CPUAccessFlags = D3D11_CPU_ACCESS_WRITE; *//*0*//*
            buffer_desc.MiscFlags = 0;
            buffer_desc.StructureByteStride = 0;

            D3D11_SHADER_RESOURCE_VIEW_DESC srv_desc;
            srv_desc.Format = DXGI_FORMAT_R16G16B16A16_UNORM;
            srv_desc.ViewDimension = D3D11_SRV_DIMENSION_BUFFER;
            srv_desc.Buffer.FirstElement = 0;
            srv_desc.Buffer.NumElements = NUMSPRAYPARTICLES;

            V_RETURN(m_pd3dDevice->CreateBuffer(&buffer_desc, NULL, &m_pRenderOrientationsAndDecimationsBuffer));
            V_RETURN(m_pd3dDevice->CreateShaderResourceView(m_pRenderOrientationsAndDecimationsBuffer, &srv_desc, &m_pRenderOrientationsAndDecimationsSRV));*/
            // V_RETURN(m_pd3dDevice->CreateUnorderedAccessView(pBuffer, &uav_desc, &m_pSimulationInstanceDataUAV));
            m_pRenderOrientationsAndDecimationsBuffer = new BufferGL();
            m_pRenderOrientationsAndDecimationsBuffer.initlize(GLenum.GL_SHADER_STORAGE_BUFFER, 4 * NUMSPRAYPARTICLES*2, null, GLenum.GL_DYNAMIC_COPY);
            m_pRenderOrientationsAndDecimationsSRV = m_pRenderOrientationsAndDecimationsBuffer;
        }

        initializeGenerators(pHullSensors);

        if(ENABLE_GPU_SIMULATION) {
            int elementSize = (/*sizeof(float)*/4 * 4) * 2;

            for (int i = 0; i < 2; ++i) {
            /*CD3D11_BUFFER_DESC bufDesc(SPRAY_PARTICLE_COUNT * elementSize, D3D11_BIND_SHADER_RESOURCE | D3D11_BIND_UNORDERED_ACCESS, D3D11_USAGE_DEFAULT, 0, D3D11_RESOURCE_MISC_BUFFER_STRUCTURED, elementSize);
            V_RETURN(m_pd3dDevice->CreateBuffer(&bufDesc, NULL, &m_pParticlesBuffer[i]));*/

                m_pParticlesBuffer[i] = new BufferGL();
                m_pParticlesBuffer[i].initlize(GLenum.GL_SHADER_STORAGE_BUFFER, SPRAY_PARTICLE_COUNT * elementSize, null, GLenum.GL_DYNAMIC_COPY);

            /*CD3D11_UNORDERED_ACCESS_VIEW_DESC descUAV(m_pParticlesBuffer[i], DXGI_FORMAT_UNKNOWN, 0, SPRAY_PARTICLE_COUNT);
            descUAV.Buffer.Flags = D3D11_BUFFER_UAV_FLAG_APPEND;
            V_RETURN(m_pd3dDevice->CreateUnorderedAccessView(m_pParticlesBuffer[i], &descUAV, &m_pParticlesBufferUAV[i]));*/

                m_pParticlesBufferUAV[i] = m_pParticlesBuffer[i];

            /*CD3D11_SHADER_RESOURCE_VIEW_DESC descSRV(m_pParticlesBuffer[i], DXGI_FORMAT_UNKNOWN, 0, SPRAY_PARTICLE_COUNT);
            V_RETURN(m_pd3dDevice->CreateShaderResourceView(m_pParticlesBuffer[i], &descSRV, &m_pParticlesBufferSRV[i]));*/

                m_pParticlesBufferSRV[i] = m_pParticlesBuffer[i];
            }

            {
            /*CD3D11_BUFFER_DESC bufDesc(sizeof(int) * 4, D3D11_BIND_UNORDERED_ACCESS, D3D11_USAGE_DEFAULT, 0, D3D11_RESOURCE_MISC_DRAWINDIRECT_ARGS);
            D3D11_SUBRESOURCE_DATA subresourceData;
            int drawArguments[4] = {0, 1, 1, 0};
            subresourceData.pSysMem = drawArguments;
            V_RETURN(m_pd3dDevice->CreateBuffer(&bufDesc, &subresourceData, &m_pDrawParticlesBuffer));*/

                IntBuffer drawArguments = CacheBuffer.wrap(0, 1, 1, 0);
                m_pDrawParticlesBuffer = new BufferGL();
                m_pDrawParticlesBuffer.initlize(GLenum.GL_DRAW_INDIRECT_BUFFER, drawArguments.remaining() * 4, drawArguments, GLenum.GL_DYNAMIC_DRAW);

            /*CD3D11_UNORDERED_ACCESS_VIEW_DESC descUAV(m_pDrawParticlesBuffer, DXGI_FORMAT_R32G32B32A32_UINT, 0, 1);
            V_RETURN(m_pd3dDevice->CreateUnorderedAccessView(m_pDrawParticlesBuffer, &descUAV, &m_pDrawParticlesBufferUAV));*/

                m_pDrawParticlesBufferUAV = m_pDrawParticlesBuffer;

            /*CD3D11_BUFFER_DESC descCB(sizeof(int) * 4, D3D11_BIND_CONSTANT_BUFFER);
            V_RETURN(m_pd3dDevice->CreateBuffer(&descCB, NULL, &m_pDrawParticlesCB));*/

                m_pDrawParticlesCB = new BufferGL();

            }

            {
            /*CD3D11_BUFFER_DESC bufDesc(sizeof(int) * 4, 0, D3D11_USAGE_STAGING, D3D11_CPU_ACCESS_READ);
            V_RETURN(m_pd3dDevice->CreateBuffer(&bufDesc, NULL, &m_pDrawParticlesBufferStaging));*/

                m_pDrawParticlesBufferStaging = m_pDrawParticlesBuffer;
            }

            m_pInitSprayParticlesTechnique = m_pFX -> GetTechniqueByName("InitSprayParticles");
            m_pSimulateSprayParticlesTechnique = m_pFX -> GetTechniqueByName("SimulateSprayParticles");
            m_pDispatchArgumentsTechnique = m_pFX -> GetTechniqueByName("PrepareDispatchArguments");

        /*m_pParticlesNum = m_pFX->GetVariableByName("g_ParticlesNum")->AsScalar();
        m_pSimulationTime = m_pFX->GetVariableByName("g_SimulationTime")->AsScalar();
        m_pWindSpeed = m_pFX->GetVariableByName("g_WindSpeed")->AsVector();
        m_pParticlesBufferVariable = m_pFX->GetVariableByName("g_SprayParticleDataSRV")->AsShaderResource();

        m_pWorldToVesselVariable = m_pFX->GetVariableByName("g_worldToVessel")->AsMatrix();
        m_pVesselToWorldVariable = m_pFX->GetVariableByName("g_vesselToWorld")->AsMatrix();

        m_worldToHeightLookupScaleVariable = m_pFX->GetVariableByName("g_worldToHeightLookupScale")->AsVector();
        m_worldToHeightLookupRotVariable = m_pFX->GetVariableByName("g_worldToHeightLookupRot")->AsVector();
        m_worldToHeightLookupOffsetVariable = m_pFX->GetVariableByName("g_worldToHeightLookupOffset")->AsVector();
        m_texHeightLookupVariable = m_pFX->GetVariableByName("g_texHeightLookup")->AsShaderResource();

        m_pMatWorldToFoamVariable = m_pFX->GetVariableByName("g_matWorldToFoam")->AsMatrix();*/
        }

        if(SPRAY_PARTICLE_SORTING) {
            // Create depth sort buffers
            {
            /*D3D11_BUFFER_DESC buffer_desc;
            buffer_desc.ByteWidth = 2 * SPRAY_PARTICLE_COUNT * sizeof(float);
            buffer_desc.Usage = D3D11_USAGE_DEFAULT;
            buffer_desc.BindFlags = D3D11_BIND_SHADER_RESOURCE | D3D11_BIND_UNORDERED_ACCESS;
            buffer_desc.CPUAccessFlags = 0;
            buffer_desc.MiscFlags = D3D11_RESOURCE_MISC_BUFFER_STRUCTURED;
            buffer_desc.StructureByteStride = 2 * sizeof(float);

            D3D11_SHADER_RESOURCE_VIEW_DESC srv_desc;
            srv_desc.Format = DXGI_FORMAT_UNKNOWN;
            srv_desc.ViewDimension = D3D11_SRV_DIMENSION_BUFFER;
            srv_desc.Buffer.FirstElement = 0;
            srv_desc.Buffer.NumElements = SPRAY_PARTICLE_COUNT;

            D3D11_UNORDERED_ACCESS_VIEW_DESC uav_desc;
            uav_desc.Format = DXGI_FORMAT_UNKNOWN;
            uav_desc.ViewDimension = D3D11_UAV_DIMENSION_BUFFER;
            uav_desc.Buffer.FirstElement = 0;
            uav_desc.Buffer.NumElements = SPRAY_PARTICLE_COUNT;
            uav_desc.Buffer.Flags = D3D11_BUFFER_UAV_FLAG_COUNTER;

            ID3D11Buffer* pBuffer = NULL;
            V_RETURN(m_pd3dDevice->CreateBuffer(&buffer_desc, NULL, &pBuffer));
            V_RETURN(m_pd3dDevice->CreateShaderResourceView(pBuffer, &srv_desc, &m_pDepthSort1SRV));
            V_RETURN(m_pd3dDevice->CreateUnorderedAccessView(pBuffer, &uav_desc, &m_pDepthSort1UAV));
            SAFE_RELEASE(pBuffer);*/

                m_pDepthSort1SRV = new BufferGL();
                m_pDepthSort1SRV.initlize(GLenum.GL_SHADER_STORAGE_BUFFER, 2 * SPRAY_PARTICLE_COUNT * /*sizeof(float)*/4, null, GLenum.GL_DYNAMIC_COPY);
                m_pDepthSort1UAV = m_pDepthSort1SRV;

            /*V_RETURN(m_pd3dDevice->CreateBuffer(&buffer_desc, NULL, &pBuffer));
            V_RETURN(m_pd3dDevice->CreateShaderResourceView(pBuffer, &srv_desc, &m_pDepthSort2SRV));
            V_RETURN(m_pd3dDevice->CreateUnorderedAccessView(pBuffer, &uav_desc, &m_pDepthSort2UAV));
            SAFE_RELEASE(pBuffer);*/

                m_pDepthSort2SRV = new BufferGL();
                m_pDepthSort2SRV.initlize(GLenum.GL_SHADER_STORAGE_BUFFER, 2 * SPRAY_PARTICLE_COUNT * /*sizeof(float)*/4, null, GLenum.GL_DYNAMIC_COPY);
                m_pDepthSort2UAV = m_pDepthSort2SRV;
            }
        }

        // sensor visualization D3D objects
        {
            final int D3D11_INPUT_PER_VERTEX_DATA = 0;
		    final D3D11_INPUT_ELEMENT_DESC svis_layout[] = {
                new D3D11_INPUT_ELEMENT_DESC( "POSITION", 0, DXGI_FORMAT_R32G32B32_FLOAT, 0, 0, D3D11_INPUT_PER_VERTEX_DATA, 0 ),
                new D3D11_INPUT_ELEMENT_DESC( "INTENSITY", 0, DXGI_FORMAT_R32_FLOAT, 0, 12, D3D11_INPUT_PER_VERTEX_DATA, 0 ),
                new D3D11_INPUT_ELEMENT_DESC( "TEXCOORD", 0, DXGI_FORMAT_R32G32_FLOAT, 0, 16, D3D11_INPUT_PER_VERTEX_DATA, 0 )};

//		    const UINT num_layout_elements = sizeof(svis_layout)/sizeof(svis_layout[0]);

            m_pSensorVisualizationTechnique = m_pFX->GetTechniqueByName("SensorVisualizationTech");

            /*D3DX11_PASS_DESC PassDesc;
            V_RETURN(m_pSensorVisualizationTechnique->GetPassByIndex(0)->GetDesc(&PassDesc));

            V_RETURN(m_pd3dDevice->CreateInputLayout(	svis_layout, num_layout_elements,
                    PassDesc.pIAInputSignature, PassDesc.IAInputSignatureSize,
                    &m_pSensorVisualizationLayout
            ));*/

            m_pSensorVisualizationLayout = ID3D11InputLayout.createInputLayoutFrom(svis_layout);

            /*D3D11_BUFFER_DESC buffer_desc;
            buffer_desc.ByteWidth = 4 * (NUMSPRAYGENERATORS + NUMRIMSPRAYGENERATORS) * sizeof(VisualizationVertex);
            buffer_desc.Usage = D3D11_USAGE_DYNAMIC;
            buffer_desc.BindFlags = D3D11_BIND_VERTEX_BUFFER;
            buffer_desc.CPUAccessFlags = D3D11_CPU_ACCESS_WRITE;
            buffer_desc.MiscFlags = 0;
            V_RETURN(m_pd3dDevice->CreateBuffer(&buffer_desc, NULL, &m_pSensorsVisualizationVertexBuffer));*/

            m_pSensorsVisualizationVertexBuffer = new BufferGL();
            m_pSensorsVisualizationVertexBuffer.initlize(GLenum.GL_ARRAY_BUFFER, 4 * (NUMSPRAYGENERATORS + NUMRIMSPRAYGENERATORS)*VisualizationVertex.SIZE, null, GLenum.GL_DYNAMIC_COPY);
        }

        {
		    final int max_num_generators = NUMSPRAYGENERATORS + NUMRIMSPRAYGENERATORS;
            IntBuffer indices = CacheBuffer.getCachedIntBuffer(max_num_generators*6);
            int vertex_ix = 0;
//            DWORD* pCurrIx = indices;
            for(int i = 0; i != max_num_generators; ++i)
            {
                indices.put(vertex_ix + 0);
                indices.put(vertex_ix + 2);
                indices.put(vertex_ix + 1);
                indices.put(vertex_ix + 0);
                indices.put(vertex_ix + 3);
                indices.put(vertex_ix + 2);

                vertex_ix += 4;
            }
            indices.flip();

            /*D3D11_BUFFER_DESC buffer_desc;
            buffer_desc.ByteWidth = 6 * max_num_generators * sizeof(DWORD);
            buffer_desc.Usage = D3D11_USAGE_IMMUTABLE;
            buffer_desc.BindFlags = D3D11_BIND_INDEX_BUFFER;
            buffer_desc.CPUAccessFlags = 0;
            buffer_desc.MiscFlags = 0;

            D3D11_SUBRESOURCE_DATA srd;
            srd.pSysMem = indices;
            srd.SysMemPitch = sizeof(indices);
            srd.SysMemSlicePitch = sizeof(indices);

            V_RETURN(m_pd3dDevice->CreateBuffer(&buffer_desc, &srd, &m_pSensorsVisualizationIndexBuffer));*/

            m_pSensorsVisualizationIndexBuffer = new BufferGL();
            m_pSensorsVisualizationIndexBuffer.initlize(GLenum.GL_ELEMENT_ARRAY_BUFFER, 4*indices.remaining(),indices, GLenum.GL_STATIC_DRAW);
        }
    }

    void updateSprayGenerators(GFSDK_WaveWorks_Simulation hOceanSimulation, OceanVessel pOceanVessel, float deltaTime, float kWaterScale, Vector2f wind_speed, float spray_mult){
        Vector3f wind_speed_3d = new Vector3f(wind_speed.x,wind_speed.y,0.f);

        // Re-zero splash power
        m_SplashPowerFromLastUpdate = 0.f;

        // get sensor data
	    final OceanHullSensors pSensors = pOceanVessel.getHullSensors();
	    final Vector3f[] pLocalSensorPositions = pSensors. getSensorPositions();
	    final Vector3f[] pWorldSensorPositions = pSensors. getWorldSensorPositions();
	    final Vector3f[] pWorldSensorNormals = pSensors. getWorldSensorNormals();
        final Vector4f[] pSensorDisplacements = pSensors. getDisplacements();
        final Vector2f[] pSensorReadbackCoords = pSensors. getReadbackCoords();

	    final Vector3f[] pWorldRimSensorPositions = pSensors. getWorldRimSensorPositions();
	    final Vector3f[] pWorldRimSensorNormals = pSensors. getWorldRimSensorNormals();
        final Vector4f[] pRimSensorDisplacements = pSensors. getRimDisplacements();

        // we load the dice in favour of bow events for hull spray
        final float sensors_min_z = pSensors. getBoundsMinCorner().z;
        final float sensors_max_z = pSensors. getBoundsMaxCorner().z;

        // local variables
        Vector3f hypothetical_spray_dir = CacheBuffer.getCachedVec3();
        Vector3f displacementSpeedAtHull = CacheBuffer.getCachedVec3();
        Vector3f rv = CacheBuffer.getCachedVec3();
        Vector3f relative_velocity_of_wave = CacheBuffer.getCachedVec3();
        Vector3f water_normal = CacheBuffer.getCachedVec3();

        // getting hull speed
        for(int i = 0; i < m_pHullSensors.getNumSensors(); i++)
        {
            m_hullSpeed[i].x = (pWorldSensorPositions[i].x - m_WorldSensorPositionsOld[i].x)/deltaTime;
            m_hullSpeed[i].y = (pWorldSensorPositions[i].y - m_WorldSensorPositionsOld[i].y)/deltaTime;
            m_hullSpeed[i].z = (pWorldSensorPositions[i].z - m_WorldSensorPositionsOld[i].z)/deltaTime;
        }

        // getting wave speed
        for(int i = 0; i < m_pHullSensors.getNumSensors(); i++)
        {
            m_displacementsSpeed[i].x = (pSensorDisplacements[i].x - m_displacementsOld[i].x)/deltaTime;
            m_displacementsSpeed[i].y = (pSensorDisplacements[i].y - m_displacementsOld[i].y)/deltaTime;
            m_displacementsSpeed[i].z = (pSensorDisplacements[i].z - m_displacementsOld[i].z)/deltaTime;
        }

        if(m_firstUpdate) {
            m_firstUpdate = false;
        } else {

            // Emit hull particles when -
            //	- the waterline is nearby
            //	- the relative velocity between hull and boat has a strong component toward the hull
            // We do this stochastically, and we are careful to use a poisson CDF in order to get the probabilities right
            // for a given time interval

		    final float power_nrm = 1.f/((m_pHullSensors.getNumSensors())*deltaTime);
            for(int i = 0; i < m_pHullSensors.getNumSensors(); i++)
            {
                float length_param = (pLocalSensorPositions[i].z-sensors_min_z)/(sensors_max_z-sensors_min_z);
                if(length_param < kBowGenThreshold) {
                    m_sprayAmount[i] = 0;
                    continue;
                }

                float bow_mult = (length_param - kBowGenThreshold)/(1.f-kBowGenThreshold);

                float water_line_proximity = pSensorDisplacements[i].z - pWorldSensorPositions[i].z;
//                D3DXVECTOR3 relative_velocity_of_wave = m_displacementsSpeed[i] - m_hullSpeed[i];
                Vector3f.sub(m_displacementsSpeed[i], m_hullSpeed[i], relative_velocity_of_wave);
//                hull_normal.set(pWorldSensorNormals[i].x,pWorldSensorNormals[i].y,pWorldSensorNormals[i].z);
                ReadableVector3f hull_normal = pWorldSensorNormals[i];
			    final float hull_perp_water_speed = -Vector3f.dot(relative_velocity_of_wave, hull_normal);

                float mean_spray_gen = spray_mult * CalcMeanSprayGen(hull_perp_water_speed,water_line_proximity,deltaTime,m_pHullSensors.getMeanSensorRadius());

                // Load the dice towards bow-spray
                mean_spray_gen *= bow_mult;

                m_sprayAmount[i] = PoissonOutcome(mean_spray_gen);

                if(m_sprayAmount[i] != 0) {

                    // We will need a water normal
                    Vector2f water_gradient = new Vector2f();
                    pOceanVessel.getSurfaceHeights().getGradient(pSensorReadbackCoords[i], water_gradient);
                    water_normal.set(water_gradient.x,water_gradient.y,1.f);
//                    D3DXVec3Normalize(&water_normal,&water_normal);
                    water_normal.normalise();

                    // We use the contact direction to figure out how 'squirty' this spray event is
                    Vector3f contact_line_dir = hypothetical_spray_dir;
//                    D3DXVec3Cross(&contact_line_dir, &water_normal, &hull_normal);
//                    D3DXVec3Normalize(&contact_line_dir,&contact_line_dir);
                    Vector3f.cross(water_normal, hull_normal, contact_line_dir);
                    contact_line_dir.normalise();

                    // Calculate a hypothetical spray direction based on the water line
//                    D3DXVECTOR3 hypothetical_spray_dir;
//                    D3DXVec3Cross(&hypothetical_spray_dir, &hull_normal, &contact_line_dir);
                    // No need to normalize, already perp
                    Vector3f.cross(hull_normal, contact_line_dir, hypothetical_spray_dir);

//                    D3DXVec3TransformNormal(&rv,&m_SprayGeneratorShipSpaceSprayDir[i], pOceanVessel->getWorldXform());
                    Matrix4f.transformNormal(pOceanVessel.getWorldXform(), m_SprayGeneratorShipSpaceSprayDir[i], rv);
                    m_SprayGeneratorWorldSpaceSprayDir[i].x = rv.x;
                    m_SprayGeneratorWorldSpaceSprayDir[i].y = rv.z;
                    m_SprayGeneratorWorldSpaceSprayDir[i].z = rv.y;

                    // Squirtiness is how well actual and hypothetical line up
				    final float squirtiness = Math.max(0,Vector3f.dot(hypothetical_spray_dir,m_SprayGeneratorWorldSpaceSprayDir[i]));

                    final float normals_dp = Math.max(0.f,-Vector3f.dot(water_normal, hull_normal));
                    final float water_perp_water_speed = Vector3f.dot(relative_velocity_of_wave, water_normal);
                    final float speed_of_contact_line_relative_to_hull = (float) Math.max(0.f,water_perp_water_speed/Math.sqrt(1.f-normals_dp*normals_dp));

                    float hull_perp_water_speed_mult = min_speed_mult + (max_speed_mult-min_speed_mult) * /*((float)rand()/(float)RAND_MAX)*/Numeric.random();
                    float speed = squirtiness * speed_of_contact_line_relative_to_hull + hull_perp_water_speed_mult * hull_perp_water_speed;
                    if(speed > max_speed)
                        speed = max_speed;

                    // Emit along a random direction between the hull spray direction and the water surface
                    ReadableVector3f hull_spray_direction = m_SprayGeneratorWorldSpaceSprayDir[i];
//                    D3DXVECTOR3 water_spray_direction = hull_spray_direction + D3DXVec3Dot(&hull_spray_direction,&water_normal) * water_normal;
                    Vector3f water_spray_direction = Vector3f.linear(hull_spray_direction, water_normal, Vector3f.dot(hull_spray_direction,water_normal), displacementSpeedAtHull);
//                    D3DXVec3Normalize(&water_spray_direction,&water_spray_direction);
                    water_spray_direction.normalise();

                    float spray_rand = /*((float)rand()/(float)RAND_MAX)*/Numeric.random();
                    {
                        /*D3DXVECTOR3 spray_direction = (1.f-spray_rand) * hull_spray_direction + spray_rand * water_spray_direction;
                        D3DXVec3Normalize(&spray_direction,&spray_direction);

                        m_spraySpeed[i] = speed * spray_direction;*/

                        Vector3f spray_direction = m_spraySpeed[i];
                        Vector3f.mix(hull_spray_direction, water_spray_direction, spray_rand, spray_direction);
                        spray_direction.normalise();
                        spray_direction.scale(speed);
                    }

                    // Add the hull speed back in for front-facing surfaces
                    Vector3f hull_speed_dir = displacementSpeedAtHull;
                    hull_speed_dir.set( m_hullSpeed[i]);
//                    D3DXVec3Normalize(&hull_speed_dir,&hull_speed_dir);
                    hull_speed_dir.normalise();
                    float hull_perp_hull_speed_dir = -Vector3f.dot(hull_speed_dir,hull_normal);
                    if(hull_perp_hull_speed_dir > 0.f) {
//                        m_spraySpeed[i] += hull_perp_hull_speed_dir * m_hullSpeed[i];
                        Vector3f.linear(m_spraySpeed[i],m_hullSpeed[i], hull_perp_hull_speed_dir,m_spraySpeed[i]);
                    }

                    // Offset so that particles emerge from water
                    {
                        /*D3DXVECTOR3 offset_vec = hull_normal + normals_dp * water_normal;
                        D3DXVec3Normalize(&offset_vec,&offset_vec);
                        offset_vec -= water_normal;
                        m_sprayOffset[i] = 0.5f * offset_vec * kParticleInitialScale;*/

                        Vector3f offset_vec = displacementSpeedAtHull;
                        Vector3f.linear(hull_normal, water_normal, normals_dp, offset_vec);
                        offset_vec.normalise();
                        Vector3f.sub(offset_vec, water_normal, offset_vec);
                        offset_vec.scale(0.5f * kParticleInitialScale);
                        m_sprayOffset[i].set(offset_vec);
                    }

                    // Add the KE to the tracker
                    m_SplashPowerFromLastUpdate += m_sprayAmount[i]*Vector3f.dot(m_spraySpeed[i],m_spraySpeed[i])*power_nrm;
                }
            }

            // Emit rim particles when the rim is under-water
            for(int i = 0; i < m_pHullSensors.getNumRimSensors() && m_numParticlesAlive < NUMSPRAYPARTICLES; i++)
            {
                float water_height = pRimSensorDisplacements[i].z - pWorldRimSensorPositions[i].z;
                m_rimSprayAmount[i] = 0;
                if(water_height > 0.f)
                {
                    float mean_rim_spray_gen = spray_mult * CalcMeanRimSprayGen(water_height, deltaTime);
                    int spray_amount = PoissonOutcome(mean_rim_spray_gen);
                    m_rimSprayAmount[i] = spray_amount;
                    if((m_numParticlesAlive + spray_amount) > NUMSPRAYPARTICLES)
                        spray_amount = NUMSPRAYPARTICLES - m_numParticlesAlive;
                    for(int emit_ix = 0; emit_ix != spray_amount; ++emit_ix)
                    {
                        float lerp = /*(float)rand()/(float)RAND_MAX*/Numeric.random();
//                        D3DXVECTOR3 lerped_gen_position = (1.f-lerp) * pWorldRimSensorPositions[i] + lerp * m_WorldRimSensorPositionsOld[i];
                        Vector3f lerped_gen_position = Vector3f.mix(pWorldRimSensorPositions[i], m_WorldRimSensorPositionsOld[i], lerp, displacementSpeedAtHull);

//                        D3DXVec3TransformNormal(&rv,&m_RimSprayGeneratorShipSpaceSprayDir[i], pOceanVessel->getWorldXform());
                        Matrix4f.transformNormal(pOceanVessel.getWorldXform(), m_RimSprayGeneratorShipSpaceSprayDir[i], rv);
                        Vector3f deck_up = hypothetical_spray_dir;
                        deck_up.x = rv.x;
                        deck_up.y = rv.z;
                        deck_up.z = rv.y;

//                        D3DXVECTOR3 water_out = -pWorldRimSensorNormals[i];
                        Vector3f water_out = relative_velocity_of_wave;
                        water_out.set(pWorldRimSensorNormals[i]);
                        water_out.negate();

                        // Spawn at random location along water height
//                        lerped_gen_position += water_height * /*(float)rand()/(float)RAND_MAX*/Numeric.random() * deck_up;
                        Vector3f.linear(lerped_gen_position, deck_up, water_height * /*(float)rand()/(float)RAND_MAX*/Numeric.random(), lerped_gen_position);

                        // Spawn in the water
//                        lerped_gen_position -= 0.5f * water_out * kParticleInitialScale;
                        Vector3f.linear(lerped_gen_position, water_out, -0.5f * kParticleInitialScale,lerped_gen_position);

                        m_particlePosition[m_numParticlesAlive].x = lerped_gen_position.x;
                        m_particlePosition[m_numParticlesAlive].y = lerped_gen_position.y;
                        m_particlePosition[m_numParticlesAlive].z = lerped_gen_position.z;

                        // Ensure particles follow movements of hull
                        float random_up_speed = rim_up_speed_min + (rim_up_speed_max-rim_up_speed_min)*/*(float)rand()/(float)RAND_MAX*/Numeric.random();
                        float random_out_speed = rim_out_speed_min + (rim_out_speed_max-rim_out_speed_min)*/*(float)rand()/(float)RAND_MAX*/Numeric.random();
//                        D3DXVECTOR3 hull_speed = (pWorldRimSensorPositions[i] - m_WorldRimSensorPositionsOld[i])/deltaTime;
//                        D3DXVECTOR3 emit_speed = random_up_speed * deck_up + random_out_speed * water_out;
                        Vector3f hull_speed = Vector3f.sub(pWorldRimSensorPositions[i], m_WorldRimSensorPositionsOld[i], displacementSpeedAtHull);
                        hull_speed.scale(1f/deltaTime);
                        Vector3f emit_speed = Vector3f.linear(deck_up, random_up_speed, water_out, random_out_speed, water_out);

//                        m_particleSpeed[m_numParticlesAlive] = hull_speed + emit_speed;
                        Vector3f.add(hull_speed, emit_speed, m_particleSpeed[m_numParticlesAlive]);
                        m_particleTime[m_numParticlesAlive]		= 0.f;

                        float speedVariation = /*(float)rand() / (float)RAND_MAX*/Numeric.random();
                        m_particleScale[m_numParticlesAlive]	= kParticleInitialScale / (speedVariation * 2.0f + 0.25f);

                        // Randomize orientation
					    final float orientation = 2.f * Numeric.PI * /*(float)rand()/(float)RAND_MAX*/Numeric.random();
                        final float c = (float)Math.cos(orientation);
                        final float s = (float)Math.sin(orientation);
                        m_particleOrientations[2*m_numParticlesAlive+0] = (short)Math.floor(0.5f + c * 32767.f);
                        m_particleOrientations[2*m_numParticlesAlive+1] = (short)Math.floor(0.5f + s * 32767.f);

                        ++m_numParticlesAlive;
                    }
                }
            }

            //generating particles
            for(int i = 0; i < m_pHullSensors.getNumSensors() && m_numParticlesAlive < NUMSPRAYPARTICLES; i++)
            {
                for(int emit_ix = 0; emit_ix != m_sprayAmount[i] && m_numParticlesAlive < NUMSPRAYPARTICLES; ++emit_ix)
                {
                    // Interpolate back to previous location to avoid burstiness
                    float lerp = /*(float)rand()/(float)RAND_MAX*/Numeric.random();
                    {
                        /*D3DXVECTOR3 lerped_gen_position = (1.f-lerp) * pWorldSensorPositions[i] + lerp * m_WorldSensorPositionsOld[i];
                        lerped_gen_position += m_sprayOffset[i];	// Keep particles from intersecting hull
                        m_particlePosition[m_numParticlesAlive].x = lerped_gen_position.x;
                        m_particlePosition[m_numParticlesAlive].y = lerped_gen_position.y;
                        m_particlePosition[m_numParticlesAlive].z = lerped_gen_position.z;*/

                        Vector3f lerped_gen_position = m_particlePosition[m_numParticlesAlive];
                        Vector3f.mix(pWorldSensorPositions[i], m_WorldSensorPositionsOld[i], lerp,lerped_gen_position);
                        Vector3f.add(lerped_gen_position, m_sprayOffset[i], lerped_gen_position);
                    }

                    // Jitter the emit position - note that this is intentionally biased towards the centre
                    Vector3f spray_tangent = displacementSpeedAtHull;
                    Vector3f.cross(m_SprayGeneratorWorldSpaceSprayDir[i],pWorldSensorNormals[i], spray_tangent);
                    float r = kParticleJitter * /*(float)rand()/(float)RAND_MAX*/Numeric.random();
                    float theta = Numeric.PI * /*(float)rand()/(float)RAND_MAX*/Numeric.random();
//                    m_particlePosition[m_numParticlesAlive] += r * cosf(theta) * spray_tangent;
//                    m_particlePosition[m_numParticlesAlive] += r * sinf(theta) * m_SprayGeneratorWorldSpaceSprayDir[i];
                    Vector3f.linear(m_particlePosition[m_numParticlesAlive], spray_tangent, (float)(r * Math.cos(theta)), m_particlePosition[m_numParticlesAlive]);
                    Vector3f.linear(m_particlePosition[m_numParticlesAlive], m_SprayGeneratorWorldSpaceSprayDir[i], (float)(r * Math.sin(theta)), m_particlePosition[m_numParticlesAlive]);

                    float speedVariation = /*(float)rand() / (float)RAND_MAX*/Numeric.random();

                    m_particleSpeed[m_numParticlesAlive].x	= m_spraySpeed[i].x;
                    m_particleSpeed[m_numParticlesAlive].y	= m_spraySpeed[i].y;
                    m_particleSpeed[m_numParticlesAlive].z	= m_spraySpeed[i].z;
                    m_particleTime[m_numParticlesAlive]		= 0;
                    m_particleScale[m_numParticlesAlive]	= kParticleInitialScale / (speedVariation * 2.0f + 0.25f);

                    // Randomize orientation
				    final float orientation = 2.f * Numeric.PI * /*(float)rand()/(float)RAND_MAX*/Numeric.random();
                    final double c = Math.cos(orientation);
                    final double s = Math.sin(orientation);
                    m_particleOrientations[2*m_numParticlesAlive+0] = (short)Math.floor(0.5 + c * 32767.);
                    m_particleOrientations[2*m_numParticlesAlive+1] = (short)Math.floor(0.5 + s * 32767.);

//#if !ENABLE_GPU_SIMULATION
                    if(!ENABLE_GPU_SIMULATION){
                    // Pre-roll sim to avoid bunching
                    float preRollDeltaTime = lerp * deltaTime;
                    simulateParticles(m_numParticlesAlive,m_numParticlesAlive+1,preRollDeltaTime,wind_speed_3d);
                    }
//#endif
                        ++m_numParticlesAlive;
                }
            }
        }

        // passing displacements to old global variables
        for(int i = 0; i < m_pHullSensors.getNumSensors(); i++)
        {
            m_displacementsOld[i].x = pSensorDisplacements[i].x;
            m_displacementsOld[i].y = pSensorDisplacements[i].y;
            m_displacementsOld[i].z = pSensorDisplacements[i].z;
        }

        // passing hull points to old global variables
        for(int i = 0; i < m_pHullSensors.getNumSensors(); i++)
        {
            m_WorldSensorPositionsOld[i].x = pWorldSensorPositions[i].x;
            m_WorldSensorPositionsOld[i].y = pWorldSensorPositions[i].y;
            m_WorldSensorPositionsOld[i].z = pWorldSensorPositions[i].z;
        }

        // same for rim points
        for(int i = 0; i < m_pHullSensors.getNumRimSensors(); i++)
        {
            m_WorldRimSensorPositionsOld[i].x = pWorldRimSensorPositions[i].x;
            m_WorldRimSensorPositionsOld[i].y = pWorldRimSensorPositions[i].y;
            m_WorldRimSensorPositionsOld[i].z = pWorldRimSensorPositions[i].z;
        }

        CacheBuffer.free(hypothetical_spray_dir);
        CacheBuffer.free(displacementSpeedAtHull);
        CacheBuffer.free(rv);
        CacheBuffer.free(relative_velocity_of_wave);
        CacheBuffer.free(water_normal);
    }

    void updateSprayParticles(/*ID3D11DeviceContext* pDC,*/ OceanVessel pOceanVessel, float deltaTime, float kWaterScale, Vector2f wind_speed){
        if(ENABLE_GPU_SIMULATION){
            final Vector3f wind_speed_3d = new Vector3f(wind_speed.y,0.0f,wind_speed.y);

            if (m_numParticlesAlive > 0) // New particles to simulate
            {
                /*D3D11_MAPPED_SUBRESOURCE msr;
                pDC->Map(m_pRenderInstanceDataBuffer, 0, D3D11_MAP_WRITE_DISCARD, 0, &msr);
                float* pCurrRenderData = (float*)msr.pData;*/
                ByteBuffer pCurrRenderData = m_pRenderInstanceDataBuffer.map(GLenum.GL_WRITE_ONLY);

                /*pDC->Map(m_pRenderVelocityDataBuffer, 0, D3D11_MAP_WRITE_DISCARD, 0, &msr);
                float* pCurrVelocityData = (float*)msr.pData;*/
                ByteBuffer pCurrVelocityData = m_pRenderVelocityDataBuffer.map(GLenum.GL_WRITE_ONLY);


                for(int src_ix=0;src_ix<m_numParticlesAlive; src_ix++)
                {
                    /*pCurrRenderData[0] = m_particlePosition[src_ix].x;
                    pCurrRenderData[1] = m_particlePosition[src_ix].z;
                    pCurrRenderData[2] = m_particlePosition[src_ix].y;
                    pCurrRenderData[3] = m_particleScale[src_ix];
                    pCurrRenderData += 4;*/

                    m_particlePosition[src_ix].store(pCurrRenderData);
                    pCurrRenderData.putFloat(m_particleScale[src_ix]);

                    /*pCurrVelocityData[0] = m_particleSpeed[src_ix].x;
                    pCurrVelocityData[1] = m_particleSpeed[src_ix].z;
                    pCurrVelocityData[2] = m_particleSpeed[src_ix].y;
                    pCurrVelocityData += 4;*/

                    m_particleSpeed[src_ix].store(pCurrVelocityData);
                }
                /*pDC->Unmap(m_pRenderInstanceDataBuffer, 0);
                pDC->Unmap(m_pRenderVelocityDataBuffer, 0);*/

                m_pRenderInstanceDataBuffer.unmap();
                m_pRenderVelocityDataBuffer.unmap();

                m_pParticlesNum->SetInt(m_numParticlesAlive);
                m_pRenderInstanceDataVariable->SetResource(m_pRenderInstanceDataSRV);
                m_pRenderVelocityAndTimeDataVariable->SetResource(m_pRenderVelocityDataSRV);
                m_pInitSprayParticlesTechnique->GetPassByIndex(0)->Apply(0, pDC);

                GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
                m_pInitSprayParticlesTechnique.enable();

//                UINT count = (UINT)-1;
//                pDC->CSSetUnorderedAccessViews(1, 1, &m_pParticlesBufferUAV[m_ParticleWriteBuffer], &count);
                gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 0, m_pParticlesBufferUAV[m_ParticleWriteBuffer].getBuffer());

                int blocksNum = (m_numParticlesAlive + SimulateSprayParticlesCSBlocksSize - 1) / SimulateSprayParticlesCSBlocksSize;
                gl.glDispatchCompute(blocksNum, 1, 1);

                m_numParticlesAlive = 0;
            }

            { // Prepare DispatchIndirect arguments for simulation
                pDC->CopyStructureCount(m_pDrawParticlesCB, 0, m_pParticlesBufferUAV[m_ParticleWriteBuffer]);

                m_pDispatchArgumentsTechnique->GetPassByIndex(0)->Apply(0, pDC);

                pDC->CSSetConstantBuffers(2, 1, &m_pDrawParticlesCB);
                pDC->CSSetUnorderedAccessViews(0, 1, &m_pDrawParticlesBufferUAV, NULL);

                pDC->Dispatch(1, 1, 1);

                ID3D11UnorderedAccessView* pNullUAV = NULL;
                pDC->CSSetUnorderedAccessViews(0, 1, &pNullUAV, NULL);
            }

            { // Simulate particles

                m_ParticleWriteBuffer = 1 - m_ParticleWriteBuffer;

                m_pSimulationTime->SetFloat(deltaTime);

                m_pWindSpeed->SetFloatVector((FLOAT*)&wind_speed_3d);

                m_pVesselToWorldVariable->SetMatrix((float*)pOceanVessel->getWorldXform());

                D3DXMATRIX matWorldToVessel;
                D3DXMatrixInverse(&matWorldToVessel,NULL,pOceanVessel->getWorldXform());
                m_pWorldToVesselVariable->SetMatrix((float*)&matWorldToVessel);

                gfsdk_float2 worldToUVScale;
                gfsdk_float2 worldToUVOffset;
                gfsdk_float2 worldToUVRot;
                pOceanVessel->getSurfaceHeights()->getGPUWorldToUVTransform(worldToUVOffset, worldToUVRot, worldToUVScale);

                m_worldToHeightLookupScaleVariable->SetFloatVector((float*)&worldToUVScale);
                m_worldToHeightLookupRotVariable->SetFloatVector((float*)&worldToUVRot);
                m_worldToHeightLookupOffsetVariable->SetFloatVector((float*)&worldToUVOffset);

                m_texHeightLookupVariable->SetResource(pOceanVessel->getSurfaceHeights()->getGPULookupSRV());

                m_pParticlesBufferVariable->SetResource(m_pParticlesBufferSRV[1-m_ParticleWriteBuffer]);
                m_pSimulateSprayParticlesTechnique->GetPassByIndex(0)->Apply(0, pDC);

                pDC->CSSetConstantBuffers(2, 1, &m_pDrawParticlesCB);

                UINT count = 0;
                pDC->CSSetUnorderedAccessViews(1, 1, &m_pParticlesBufferUAV[m_ParticleWriteBuffer], &count);

                pDC->DispatchIndirect(m_pDrawParticlesBuffer, 0);

                ID3D11UnorderedAccessView* pNullUAV = NULL;
                pDC->CSSetUnorderedAccessViews(1, 1, &pNullUAV, NULL);

                pDC->CopyStructureCount(m_pDrawParticlesBuffer, 0, m_pParticlesBufferUAV[m_ParticleWriteBuffer]);
                pDC->CopyStructureCount(m_pDrawParticlesCB, 0, m_pParticlesBufferUAV[m_ParticleWriteBuffer]);

                // Release refs
                m_texHeightLookupVariable->SetResource(NULL);
                m_pSimulateSprayParticlesTechnique->GetPassByIndex(0)->Apply(0, pDC);
            }
        }else{
            // CPU simualtes the Particle animations.
            Vector3f wind_speed_3d = new Vector3f(wind_speed.y,wind_speed.y,0.f);
            simulateParticles(0,m_numParticlesAlive,deltaTime,wind_speed_3d);

            Matrix4f worldToVessel = CacheBuffer.getCachedMatrix();
//            D3DXMatrixInverse(&worldToVessel,NULL,pOceanVessel->getWorldXform());
            Matrix4f.invert(pOceanVessel.getWorldXform(), worldToVessel);

            /*D3D11_MAPPED_SUBRESOURCE msr;
            pDC->Map(m_pRenderInstanceDataBuffer, 0, D3D11_MAP_WRITE_DISCARD, 0, &msr);
            float* pCurrRenderData = (float*)msr.pData;*/
            ByteBuffer pCurrRenderData = m_pRenderInstanceDataBuffer.map(GLenum.GL_MAP_WRITE_BIT);

            /*pDC->Map(m_pRenderOrientationsAndDecimationsBuffer, 0, D3D11_MAP_WRITE_DISCARD, 0, &msr);
            WORD* pCurrOrientationAndDecimationData = (WORD*)msr.pData;*/
            ByteBuffer pCurrOrientationAndDecimationData = m_pRenderOrientationsAndDecimationsBuffer.map(GLenum.GL_MAP_WRITE_BIT);

            // compact out particles that are below water surface, and update our render buffer at the same time
            int dst_ix = 0;
            for(int src_ix=0;src_ix<m_numParticlesAlive; src_ix++)
            {
//                world_pos.x = m_particlePosition[src_ix].x;
                float world_pos_y = m_particlePosition[src_ix].z;
//                world_pos.z = m_particlePosition[src_ix].y;

                boolean kill_this_particle = false;
                if(world_pos_y < -kParticleKillDepth)
                {
                    kill_this_particle=true;
                }
                if(m_particleTime[src_ix] > kParticleTTL)
                {
                    kill_this_particle=true;
                }

                if(!kill_this_particle) {
                    if(src_ix != dst_ix) {
                        // Keep this particle
                        m_particlePosition[dst_ix] = m_particlePosition[src_ix];
                        m_particleSpeed[dst_ix] = m_particleSpeed[src_ix];
                        m_particleTime[dst_ix] = m_particleTime[src_ix];
                        m_particleScale[dst_ix] = m_particleScale[src_ix];
                        m_particleOrientations[2*dst_ix+0] = m_particleOrientations[2*src_ix+0];
                        m_particleOrientations[2*dst_ix+1] = m_particleOrientations[2*src_ix+1];
                    }

                    /*pCurrRenderData[0] = m_particlePosition[dst_ix].x;
                    pCurrRenderData[1] = m_particlePosition[dst_ix].z;
                    pCurrRenderData[2] = m_particlePosition[dst_ix].y;
                    pCurrRenderData[3] = m_particleScale[dst_ix];
                    pCurrRenderData += 4;*/
                    m_particlePosition[dst_ix].store(pCurrRenderData);
                    pCurrRenderData.putFloat(m_particleScale[dst_ix]);

                    /*pCurrOrientationAndDecimationData[0] = m_particleOrientations[2*dst_ix+0];
                    pCurrOrientationAndDecimationData[1] = m_particleOrientations[2*dst_ix+1];
                    pCurrOrientationAndDecimationData += 4;*/

                    pCurrOrientationAndDecimationData.putShort(m_particleOrientations[2*dst_ix+0]);
                    pCurrOrientationAndDecimationData.putShort(m_particleOrientations[2*dst_ix+1]);

			        final float decimation = Math.min(kFadeInTime,m_particleTime[dst_ix])/kFadeInTime;
//                    pCurrOrientationAndDecimationData[2] = (WORD)floorf(0.5f + decimation * 32767.f);
                    pCurrOrientationAndDecimationData.putShort((short)Math.floor(0.5f + decimation * 32767.f));

                    ++dst_ix;
                }
            }

//            pDC->Unmap(m_pRenderInstanceDataBuffer,0);
//            pDC->Unmap(m_pRenderOrientationsAndDecimationsBuffer,0);

            m_pRenderInstanceDataBuffer.unmap();
            m_pRenderOrientationsAndDecimationsBuffer.unmap();
            m_numParticlesAlive = dst_ix;
        }
    }

    float getSplashPowerFromLastUpdate() { return m_SplashPowerFromLastUpdate; }

    void renderToScene(    //ID3D11DeviceContext* pDC,
                           OceanVessel pOceanVessel, CameraData camera, OceanEnvironment ocean_env,
                           TextureGL pDepthSRV, boolean simple
    ){
        // Matrices
        D3DXMATRIX matView = *camera.GetViewMatrix();
        D3DXMATRIX matProj = *camera.GetProjMatrix();

        D3DXMATRIX matInvView;
        D3DXMatrixInverse(&matInvView, NULL, &matView);

        D3DXMATRIX matInvProj;
        D3DXMatrixInverse(&matInvProj, NULL, &matProj);

        // View-proj
        m_pMatProjVariable->SetMatrix((FLOAT*)&matProj);
        m_pMatProjInvVariable->SetMatrix((FLOAT*)&matInvProj);
        m_pMatViewVariable->SetMatrix((FLOAT*)&matView);

#if SPRAY_PARTICLE_SORTING
        depthSortParticles(pDC);
#endif

        // Simple particles
        m_pSimpleParticlesVariable->SetFloat(simple?1.0f:0.0f);

        // Global lighting
        m_pLightDirectionVariable->SetFloatVector((FLOAT*)&ocean_env.main_light_direction);
        m_pLightColorVariable->SetFloatVector((FLOAT*)&ocean_env.main_light_color);
        m_pAmbientColorVariable->SetFloatVector((FLOAT*)&ocean_env.sky_color);

        // Lightnings
        m_pLightningColorVariable->SetFloatVector((FLOAT*)&ocean_env.lightning_light_intensity);
        m_pLightningPositionVariable->SetFloatVector((FLOAT*)&ocean_env.lightning_light_position);

        // Fog
        m_pFogExponentVariable->SetFloat(ocean_env.fog_exponent);

        D3DXMATRIX matWorldToVessel;
        D3DXMatrixInverse(&matWorldToVessel,NULL,pOceanVessel->getWorldXform());
        D3DXMATRIX matViewToVessel = matInvView * matWorldToVessel;
        m_pWorldToVesselVariable->SetMatrix((float*)&matViewToVessel);

        int lightsNum = 0;

        // Spot lights - transform to view space
        D3DXMATRIX matSpotlightsToView = ocean_env.spotlights_to_world_matrix * matView;
        D3DXMATRIX matViewToSpotlights;
        D3DXMatrixInverse(&matViewToSpotlights,NULL,&matSpotlightsToView);

        D3DXVECTOR4 spotlight_position[MaxNumSpotlights];
        D3DXVECTOR4 spotlight_axis_and_cos_angle[MaxNumSpotlights];
        D3DXVECTOR4 spotlight_color[MaxNumSpotlights];

        D3DXVec4TransformArray(spotlight_position,sizeof(spotlight_position[0]),ocean_env.spotlight_position,sizeof(ocean_env.spotlight_position[0]),&matSpotlightsToView,MaxNumSpotlights);
        D3DXVec3TransformNormalArray((D3DXVECTOR3*)spotlight_axis_and_cos_angle,sizeof(spotlight_axis_and_cos_angle[0]),(D3DXVECTOR3*)ocean_env.spotlight_axis_and_cos_angle,sizeof(ocean_env.spotlight_axis_and_cos_angle[0]),&matSpotlightsToView,MaxNumSpotlights);

        for(int i=0; i!=ocean_env.activeLightsNum; ++i) {

            if (ocean_env.lightFilter != -1 && ocean_env.objectID[i] != ocean_env.lightFilter) continue;

            spotlight_position[lightsNum] = spotlight_position[i];
            spotlight_axis_and_cos_angle[lightsNum] = spotlight_axis_and_cos_angle[i];
            spotlight_color[lightsNum] = ocean_env.spotlight_color[i];
            spotlight_axis_and_cos_angle[lightsNum].w = ocean_env.spotlight_axis_and_cos_angle[i].w;

            if(ENABLE_SHADOWS) {
                D3DXMATRIX spotlight_shadow_matrix = matViewToSpotlights * ocean_env.spotlight_shadow_matrix[i];
                m_pSpotlightShadowMatrixVar -> SetMatrixArray(( float*)&
                spotlight_shadow_matrix, lightsNum, 1);
                m_pSpotlightShadowResourceVar -> SetResourceArray((ID3D11ShaderResourceView * *) & ocean_env.spotlight_shadow_resource[i], lightsNum, 1);
            }

                    ++lightsNum;
        }

        m_pSpotlightNumVariable->SetInt(lightsNum);
        m_pRenderSurfaceSpotlightPositionVariable->SetFloatVectorArray((FLOAT*)spotlight_position,0,lightsNum);
        m_pRenderSurfaceSpotLightAxisAndCosAngleVariable->SetFloatVectorArray((FLOAT*)spotlight_axis_and_cos_angle,0,lightsNum);
        m_pRenderSurfaceSpotlightColorVariable->SetFloatVectorArray((FLOAT*)spotlight_color,0,lightsNum);

        // PSM
        D3DXVECTOR3 no_tint(1,1,1); // For now
        pOceanVessel->getPSM()->setReadParams(*m_pPSMParams,no_tint);

        D3DXMATRIX matViewToPSM = matInvView * *pOceanVessel->getPSM()->getWorldToPSMUV();
        m_pMatViewToPSMVariable->SetMatrix((FLOAT*)&matViewToPSM);

        renderParticles(pDC,m_pRenderSprayToSceneTechTechnique, pDepthSRV, D3D11_PRIMITIVE_TOPOLOGY_1_CONTROL_POINT_PATCHLIST);

        pOceanVessel->getPSM()->clearReadParams(*m_pPSMParams);
        m_pRenderSprayToSceneTechTechnique->GetPassByIndex(0)->Apply(0,pDC);
    }

    void renderToPSM(//	ID3D11DeviceContext* pDC,
                         OceanPSM pPSM, OceanEnvironment ocean_env){
        m_pSimpleParticlesVariable->SetFloat(0.0f);
        m_pMatViewVariable->SetMatrix((FLOAT*)pPSM->getPSMView());
        m_pMatProjVariable->SetMatrix((FLOAT*)pPSM->getPSMProj());
        pPSM->setWriteParams(*m_pPSMParams);

        renderParticles(pDC,m_pRenderSprayToPSMTechnique, NULL, D3D11_PRIMITIVE_TOPOLOGY_POINTLIST);
    }

    void renderToFoam(	//ID3D11DeviceContext* pDC,
						Matrix4f matWorldToFoam, OceanVessel pOceanVessel, float deltaTime){
        // Just swap y and z in view matrix
        /*D3DXMATRIX matView = D3DXMATRIX(1,0,0,0,0,0,1,0,0,1,0,0,0,0,0,1);
        m_pMatViewVariable->SetMatrix((float*)&matView);

        m_pMatWorldToFoamVariable->SetMatrix((float*)&matWorldToFoam);

        gfsdk_float2 worldToUVScale;
        gfsdk_float2 worldToUVOffset;
        gfsdk_float2 worldToUVRot;
        pOceanVessel->getSurfaceHeights()->getGPUWorldToUVTransform(worldToUVOffset, worldToUVRot, worldToUVScale);

        m_worldToHeightLookupScaleVariable->SetFloatVector((float*)&worldToUVScale);
        m_worldToHeightLookupRotVariable->SetFloatVector((float*)&worldToUVRot);
        m_worldToHeightLookupOffsetVariable->SetFloatVector((float*)&worldToUVOffset);

        m_texHeightLookupVariable->SetResource(pOceanVessel->getSurfaceHeights()->getGPULookupSRV());

        m_pSimulationTime->SetFloat(deltaTime);
        m_pSimpleParticlesVariable->SetFloat(0.0f);

        renderParticles(pDC,m_pRenderSprayToFoamTechnique, NULL, D3D11_PRIMITIVE_TOPOLOGY_POINTLIST);

        // Release refs
        m_texHeightLookupVariable->SetResource(NULL);
        m_pRenderSprayToFoamTechnique->GetPassByIndex(0)->Apply(0,pDC);*/

        throw new UnsupportedOperationException();
    }

    int GetParticleBudget() {
        if (ENABLE_GPU_SIMULATION)
            return SPRAY_PARTICLE_COUNT;
        else
            return NUMSPRAYPARTICLES;
    }

    int GetParticleCount(/*ID3D11DeviceContext* pDC*/){
        int particleCount = m_numParticlesAlive;

        if (ENABLE_GPU_SIMULATION) {
            /*pDC -> CopyResource(m_pDrawParticlesBufferStaging, m_pDrawParticlesBuffer);

            D3D11_MAPPED_SUBRESOURCE mappedSubresource;
            pDC -> Map(m_pDrawParticlesBufferStaging, 0, D3D11_MAP_READ, 0, & mappedSubresource);
            particleCount = *((UINT *) mappedSubresource.pData);
            pDC -> Unmap(m_pDrawParticlesBufferStaging, 0);*/
            throw new UnsupportedOperationException();
        }

        return particleCount;
    }

    private final VisualizationVertex[] tempData = new VisualizationVertex[4];

    void UpdateSensorsVisualizationVertexBuffer(/*ID3D11DeviceContext* pDC,*/ OceanVessel pOceanVessel,float deltaTime){
        final OceanHullSensors pSensors = pOceanVessel.getHullSensors();
        final Vector3f[] pSensorPositions = pSensors.getSensorPositions();
        final Vector3f[] pSensorNormals = pSensors.getSensorNormals();
        final Vector3f[] pRimSensorPositions = pSensors.getRimSensorPositions();
        final Vector3f[] pRimSensorNormals = pSensors.getRimSensorNormals();

        /*D3D11_MAPPED_SUBRESOURCE subresource;
        pDC->Map(m_pSensorsVisualizationVertexBuffer, 0, D3D11_MAP_WRITE_DISCARD, 0, &subresource);
        VisualizationVertex* sensorsVisualizationVertexData = (VisualizationVertex*)subresource.pData;*/
        ByteBuffer sensorsVisualizationVertexData = m_pSensorsVisualizationVertexBuffer.map(GLenum.GL_MAP_WRITE_BIT);

        final float sensor_min_size = 0.075f;
        final float sensor_max_size = 0.4f;
        final float spray_amount_fade_rate = 1.f;

        Vector3f tangent = CacheBuffer.getCachedVec3();
        Vector3f binormal = CacheBuffer.getCachedVec3();

        if(tempData[0] == null){
            for(int i = 0; i < tempData.length;i++)
                tempData[0] = new VisualizationVertex();
        }

        for(int i=0;i<pSensors.getNumSensors(); i++)
        {
            if(m_sprayAmount[i] > 0)
                m_sprayVizualisationFade[i] = 1.f;
            final float intensity = (float)m_sprayVizualisationFade[i] + 0.001f;
            final float sensor_size = m_sprayVizualisationFade[i] * sensor_max_size + (1.f-m_sprayVizualisationFade[i]) * sensor_min_size;
            if(m_sprayVizualisationFade[i] > 0.f)
                m_sprayVizualisationFade[i] -= spray_amount_fade_rate * deltaTime;
            if(m_sprayVizualisationFade[i] < 0.f)
                m_sprayVizualisationFade[i] = 0.f;

            CalcTangentAndBinormal(tangent, binormal, pSensorNormals[i]);
            tangent .scale(sensor_size);
            binormal.scale(sensor_size);

            tempData[0].position_and_intensity.set(pSensorPositions[i].x + tangent.x + binormal.x,pSensorPositions[i].y + tangent.y + binormal.y,pSensorPositions[i].z + tangent.z + binormal.z, intensity);
            tempData[1].position_and_intensity.set(pSensorPositions[i].x + tangent.x - binormal.x,pSensorPositions[i].y + tangent.y - binormal.y,pSensorPositions[i].z + tangent.z - binormal.z, intensity);
            tempData[2].position_and_intensity.set(pSensorPositions[i].x - tangent.x - binormal.x,pSensorPositions[i].y - tangent.y - binormal.y,pSensorPositions[i].z - tangent.z - binormal.z, intensity);
            tempData[3].position_and_intensity.set(pSensorPositions[i].x - tangent.x + binormal.x,pSensorPositions[i].y - tangent.y + binormal.y,pSensorPositions[i].z - tangent.z + binormal.z, intensity);

            tempData[0].uv.set(0.f,0.f);
            tempData[1].uv.set(1.f,0.f);
            tempData[2].uv.set(1.f,1.f);
            tempData[3].uv.set(0.f,1.f);

//            sensorsVisualizationVertexData += 4;
            for(int j = 0; j < tempData.length; j++){
                tempData[j].store(sensorsVisualizationVertexData);
            }
        }
        for(int i=0;i<pSensors.getNumRimSensors(); i++)
        {
            if(m_rimSprayAmount[i] > 0)
                m_rimSprayVizualisationFade[i] = 1.f;
		    final float intensity = -m_rimSprayVizualisationFade[i] - 0.001f; // making it negative to differentiate rim sensors from regular sensors
            final float sensor_size = m_rimSprayVizualisationFade[i] * sensor_max_size + (1.f-m_rimSprayVizualisationFade[i]) * sensor_min_size;
            if(m_rimSprayVizualisationFade[i] > 0.f)
                m_rimSprayVizualisationFade[i] -= spray_amount_fade_rate * deltaTime;
            if(m_rimSprayVizualisationFade[i] < 0.f)
                m_rimSprayVizualisationFade[i] = 0.f;

            CalcTangentAndBinormal(tangent, binormal, pRimSensorNormals[i]);
            tangent.scale(sensor_size);
            binormal.scale(sensor_size);

            /*sensorsVisualizationVertexData[0].position_and_intensity = D3DXVECTOR4(pRimSensorPositions[i] + tangent + binormal, intensity);
            sensorsVisualizationVertexData[1].position_and_intensity = D3DXVECTOR4(pRimSensorPositions[i] + tangent - binormal, intensity);
            sensorsVisualizationVertexData[2].position_and_intensity = D3DXVECTOR4(pRimSensorPositions[i] - tangent - binormal, intensity);
            sensorsVisualizationVertexData[3].position_and_intensity = D3DXVECTOR4(pRimSensorPositions[i] - tangent + binormal, intensity);

            sensorsVisualizationVertexData[0].uv = D3DXVECTOR2(0.f,0.f);
            sensorsVisualizationVertexData[1].uv = D3DXVECTOR2(1.f,0.f);
            sensorsVisualizationVertexData[2].uv = D3DXVECTOR2(1.f,1.f);
            sensorsVisualizationVertexData[3].uv = D3DXVECTOR2(0.f,1.f);

            sensorsVisualizationVertexData += 4;*/

            tempData[0].position_and_intensity.set(pRimSensorPositions[i].x + tangent.x + binormal.x,pRimSensorPositions[i].y + tangent.y + binormal.y,pRimSensorPositions[i].z + tangent.z + binormal.z, intensity);
            tempData[1].position_and_intensity.set(pRimSensorPositions[i].x + tangent.x - binormal.x,pRimSensorPositions[i].y + tangent.y - binormal.y,pRimSensorPositions[i].z + tangent.z - binormal.z, intensity);
            tempData[2].position_and_intensity.set(pRimSensorPositions[i].x - tangent.x - binormal.x,pRimSensorPositions[i].y - tangent.y - binormal.y,pRimSensorPositions[i].z - tangent.z - binormal.z, intensity);
            tempData[3].position_and_intensity.set(pRimSensorPositions[i].x - tangent.x + binormal.x,pRimSensorPositions[i].y - tangent.y + binormal.y,pRimSensorPositions[i].z - tangent.z + binormal.z, intensity);

            tempData[0].uv.set(0.f,0.f);
            tempData[1].uv.set(1.f,0.f);
            tempData[2].uv.set(1.f,1.f);
            tempData[3].uv.set(0.f,1.f);

//            sensorsVisualizationVertexData += 4;
            for(int j = 0; j < tempData.length; j++){
                tempData[j].store(sensorsVisualizationVertexData);
            }
        }

//        pDC->Unmap(m_pSensorsVisualizationVertexBuffer, 0);
        m_pSensorsVisualizationVertexBuffer.unmap();

        CacheBuffer.free(tangent);
        CacheBuffer.free(binormal);
    }

    void RenderSensors(/*ID3D11DeviceContext* pDC, const*/ CameraData camera, OceanVessel pOceanVessel){
        final OceanHullSensors pSensors = pOceanVessel.getHullSensors();
	    final int num_sensors = pSensors.getNumSensors() + pSensors.getNumRimSensors();

//        D3DXMATRIX matWorldView = *pOceanVessel->getWorldXform() * *camera.GetViewMatrix();
        Matrix4f matWorldView = CacheBuffer.getCachedMatrix();
        Matrix4f.mul(camera.getViewMatrix(), pOceanVessel.getWorldXform(), matWorldView);

        m_pMatViewVariable->SetMatrix(matWorldView);
        m_pMatProjVariable->SetMatrix((float*)camera.GetProjMatrix());
//        m_pSensorVisualizationTechnique->GetPassByIndex(0)->Apply(0, pDC);
        m_pSensorVisualizationTechnique.enable();

	    final int vertexStride = VisualizationVertex.SIZE;
	    final int vbOffset = 0;

        /*pDC->IASetInputLayout(m_pSensorVisualizationLayout);
        pDC->IASetVertexBuffers(0, 1, &m_pSensorsVisualizationVertexBuffer, &vertexStride, &vbOffset);
        pDC->IASetIndexBuffer(m_pSensorsVisualizationIndexBuffer, DXGI_FORMAT_R32_UINT, 0);
        pDC->IASetPrimitiveTopology(D3D11_PRIMITIVE_TOPOLOGY_TRIANGLELIST);
        pDC->DrawIndexed(6*num_sensors,0,0);*/

        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        gl.glBindVertexArray(0);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, m_pSensorsVisualizationIndexBuffer.getBuffer());
        m_pSensorVisualizationLayout.bind();
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, m_pSensorsVisualizationIndexBuffer.getBuffer());
        gl.glDrawElements(GLenum.GL_TRIANGLES, 6*num_sensors, GLenum.GL_UNSIGNED_INT, 0);

        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0);
        m_pSensorVisualizationLayout.unbind();

        CacheBuffer.free(matWorldView);
    }

    void renderAudioVisualization(/*ID3D11DeviceContext* pDC,*/ float l, float t, float r, float b, float h_margin, float v_margin, float level){
        FLOAT litterbug[4] = {l,t,r,b};
        m_pAudioVisualizationRectVariable->SetFloatVector(litterbug);

        FLOAT margin[4] = {h_margin, v_margin, 0.f, 0.f};
        m_pAudioVisualizationMarginVariable->SetFloatVector(margin);

        m_pAudioVisualizationLevelVariable->SetFloat(level);

//        m_pAudioVisualizationTechnique->GetPassByIndex(0)->Apply(0, pDC);
        m_pAudioVisualizationTechnique.enable();

        /*pDC->IASetPrimitiveTopology(D3D11_PRIMITIVE_TOPOLOGY_TRIANGLELIST);
        pDC->IASetInputLayout(NULL);
        pDC->Draw(12,0);*/

        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0);

        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 12);
    }

    private void initializeGenerators(OceanHullSensors pHullSensors){
        m_pHullSensors = pHullSensors;

	    final Vector3f[] pSensorNormals = pHullSensors.getSensorNormals();
        for(int i = 0; i != pHullSensors.getNumSensors(); ++i) {

            ReadableVector3f up = Vector3f.Y_AXIS;
            Vector3f ideal_contact_dir = m_SprayGeneratorShipSpaceSprayDir[i];
//            D3DXVec3Cross(&ideal_contact_dir,&pSensorNormals[i],&up);
//            D3DXVec3Normalize(&ideal_contact_dir,&ideal_contact_dir);

//            D3DXVec3Cross(&m_SprayGeneratorShipSpaceSprayDir[i],&ideal_contact_dir,&pSensorNormals[i]);
            Vector3f.cross(pSensorNormals[i], up, ideal_contact_dir);
            ideal_contact_dir.normalise();

            Vector3f.cross(ideal_contact_dir, pSensorNormals[i], m_SprayGeneratorShipSpaceSprayDir[i]);
        }

	    final Vector3f[] pRimSensorNormals = pHullSensors.getRimSensorNormals();
        for(int i = 0; i != pHullSensors.getNumRimSensors(); ++i) {

            ReadableVector3f up = Vector3f.Y_AXIS;
            Vector3f ideal_contact_dir = m_RimSprayGeneratorShipSpaceSprayDir[i];
//            D3DXVec3Cross(&ideal_contact_dir,&pRimSensorNormals[i],&up);
//            D3DXVec3Normalize(&ideal_contact_dir,&ideal_contact_dir);

//            D3DXVec3Cross(&m_RimSprayGeneratorShipSpaceSprayDir[i],&ideal_contact_dir,&pRimSensorNormals[i]);

            Vector3f.cross(pRimSensorNormals[i],up, ideal_contact_dir);
            ideal_contact_dir.normalise();

            Vector3f.cross(ideal_contact_dir, pRimSensorNormals[i], m_RimSprayGeneratorShipSpaceSprayDir[i]);
        }

        m_firstUpdate = true;
    }

    private void renderParticles(/*ID3D11DeviceContext* pDC,*/ GLSLProgram pTech, TextureGL pSRV, int topology){
        // Particle
        /*m_pTexSplashVariable->SetResource(m_pSplashTextureSRV);
        m_pTexDepthVariable->SetResource(pDepthSRV);
        m_pInvParticleLifeTimeVariable->SetFloat(1.f/kParticleTTL);

        pDC->IASetInputLayout(NULL);
        pDC->IASetPrimitiveTopology(topology);

#if ENABLE_GPU_SIMULATION
        m_pParticleDepthSortSRVVariable->SetResource(m_pDepthSort1SRV);
        m_pParticlesBufferVariable->SetResource(m_pParticlesBufferSRV[m_ParticleWriteBuffer]);
        pTech->GetPassByIndex(0)->Apply(0, pDC);

        pDC->DrawInstancedIndirect(m_pDrawParticlesBuffer, 0);

        // Clear resource usage
        m_pParticlesBufferVariable->SetResource(NULL);
        m_pParticleDepthSortSRVVariable->SetResource(NULL);
#else
        m_pRenderInstanceDataVariable->SetResource(m_pRenderInstanceDataSRV);
        m_pRenderOrientationAndDecimationDataVariable->SetResource(m_pRenderOrientationsAndDecimationsSRV);
        pTech->GetPassByIndex(0)->Apply(0, pDC);

        pDC->Draw(m_numParticlesAlive,0);

        // Clear resource usage
        m_pRenderInstanceDataVariable->SetResource(NULL);
        m_pRenderOrientationAndDecimationDataVariable->SetResource(NULL);
#endif

        m_pTexDepthVariable->SetResource(NULL);
        pTech->GetPassByIndex(0)->Apply(0, pDC);*/

        throw new UnsupportedOperationException();
    }

    private void simulateParticles(int begin_ix, int end_ix, float deltaTime, Vector3f wind_speed_3d){
        int num_steps = (int) Math.ceil(deltaTime/kMaxSimulationTimeStep);
        float time_step = deltaTime/(float)(num_steps);

        for(int step = 0; step != num_steps; ++step) {

            // updating spray particles positions
            for(int i=begin_ix;i<end_ix; i++)
            {
                float x_delta = m_particleSpeed[i].x*time_step;
                float y_delta = m_particleSpeed[i].y*time_step;
                float z_delta = m_particleSpeed[i].z*time_step;

                m_particlePosition[i].x += x_delta;
                m_particlePosition[i].y += y_delta;
                m_particlePosition[i].z += z_delta;

                float distance_moved = (float) Math.sqrt(x_delta*x_delta + y_delta*y_delta + z_delta*z_delta);
                m_particleScale[i] += distance_moved * kParticleScaleRate;
            }

            // updating spray particles speeds
            for(int i=begin_ix;i<end_ix; i++)
            {
                float accel_x = -kParticleDrag * (m_particleSpeed[i].x - wind_speed_3d.x);
                float accel_y = -kParticleDrag * (m_particleSpeed[i].y - wind_speed_3d.y);
                float accel_z = -kParticleDrag * (m_particleSpeed[i].z - wind_speed_3d.z);
                accel_z -= kGravity;
                m_particleSpeed[i].x += accel_x*time_step;
                m_particleSpeed[i].y += accel_y*time_step;
                m_particleSpeed[i].z += accel_z*time_step;
            }

            // updating particle times
            for(int i=begin_ix;i<end_ix; i++)
            {
                m_particleTime[i]+=time_step;
            }
        }
    }

    private void depthSortParticles(/*ID3D11DeviceContext* pDC*/){
        if(!ENABLE_GPU_SIMULATION)
            return;

        /*int num_elements = SPRAY_PARTICLE_COUNT;

        { // Init sorting data
            m_pParticlesBufferVariable->SetResource(m_pParticlesBufferSRV[m_ParticleWriteBuffer]);
            m_pInitSortTechnique->GetPassByIndex(0)->Apply(0, pDC);

            pDC->CSSetConstantBuffers(2, 1, &m_pDrawParticlesCB);

            int count = 0;
            pDC->CSSetUnorderedAccessViews(0, 1, &m_pDepthSort1UAV, &count);

            pDC->Dispatch(num_elements/SprayParticlesCSBlocksSize, 1, 1);
        }

	    const UINT matrix_width = BitonicSortCSBlockSize;
	    const UINT matrix_height = num_elements / BitonicSortCSBlockSize;

        // First sort the rows for the levels <= to the block size
        for( UINT level = 2 ; level <= BitonicSortCSBlockSize ; level = level * 2 )
        {
            setDepthSortConstants( level, level, matrix_height, matrix_width );

            // Sort the row data
            m_pParticleDepthSortUAVVariable->SetUnorderedAccessView(m_pDepthSort1UAV);
            m_pBitonicSortTechnique->GetPassByIndex(0)->Apply(0, pDC);
            pDC->Dispatch( num_elements / BitonicSortCSBlockSize, 1, 1 );
        }

        // Then sort the rows and columns for the levels > than the block size
        // Transpose. Sort the Columns. Transpose. Sort the Rows.
        for( int level = (BitonicSortCSBlockSize * 2) ; level <= num_elements ; level = level * 2 )
        {
            setDepthSortConstants( (level / BitonicSortCSBlockSize), (level & ~num_elements) / BitonicSortCSBlockSize, matrix_width, matrix_height );

            // Transpose the data from buffer 1 into buffer 2
            m_pParticleDepthSortUAVVariable->SetUnorderedAccessView(m_pDepthSort2UAV);
            m_pParticleDepthSortSRVVariable->SetResource(m_pDepthSort1SRV);
            m_pMatrixTransposeTechnique->GetPassByIndex(0)->Apply(0, pDC);
            pDC->Dispatch( matrix_width / TransposeCSBlockSize, matrix_height / TransposeCSBlockSize, 1 );
            m_pParticleDepthSortSRVVariable->SetResource(NULL);
            m_pMatrixTransposeTechnique->GetPassByIndex(0)->Apply(0, pDC);

            // Sort the transposed column data
            m_pBitonicSortTechnique->GetPassByIndex(0)->Apply(0, pDC);
            pDC->Dispatch( num_elements / BitonicSortCSBlockSize, 1, 1 );

            setDepthSortConstants( BitonicSortCSBlockSize, level, matrix_height, matrix_width );

            // Transpose the data from buffer 2 back into buffer 1
            m_pParticleDepthSortUAVVariable->SetUnorderedAccessView(m_pDepthSort1UAV);
            m_pParticleDepthSortSRVVariable->SetResource(m_pDepthSort2SRV);
            m_pMatrixTransposeTechnique->GetPassByIndex(0)->Apply(0, pDC);
            pDC->Dispatch( matrix_height / TransposeCSBlockSize, matrix_width / TransposeCSBlockSize, 1 );
            m_pParticleDepthSortSRVVariable->SetResource(NULL);
            m_pMatrixTransposeTechnique->GetPassByIndex(0)->Apply(0, pDC);

            // Sort the row data
            m_pBitonicSortTechnique->GetPassByIndex(0)->Apply(0, pDC);
            pDC->Dispatch( num_elements / BitonicSortCSBlockSize, 1, 1 );
        }

        // Release outputs
        m_pParticleDepthSortUAVVariable->SetUnorderedAccessView(NULL);
        m_pMatrixTransposeTechnique->GetPassByIndex(0)->Apply(0, pDC);
        m_pBitonicSortTechnique->GetPassByIndex(0)->Apply(0, pDC);*/

        throw new UnsupportedOperationException();
    }
    private void setDepthSortConstants( int iLevel, int iLevelMask, int iWidth, int iHeight ){
       /* m_piDepthSortLevelVariable->SetInt(iLevel);
        m_piDepthSortLevelMaskVariable->SetInt(iLevelMask);
        m_piDepthSortWidthVariable->SetInt(iWidth);
        m_piDepthSortHeightVariable->SetInt(iHeight);*/

        throw new UnsupportedOperationException();
    }

    private static int PoissonOutcomeKnuth(float lambda) {

        // Adapted to log space to avoid exp() overflow for large lambda
        int k = 0;
        float p = 0.f;
        do {
            k = k+1;
            p = (float) (p - Math.log(/*(float)rand()/(float)(RAND_MAX+1)*/ Math.random()));
        }
        while (p < lambda);

        return k-1;
    }

    private static int PoissonOutcome(float lambda) {
        return PoissonOutcomeKnuth(lambda);
    }

    @Override
    public void dispose() {

    }

    private final static class VisualizationVertex implements Readable {
        static final int SIZE = Vector4f.SIZE+Vector2f.SIZE;
        final Vector4f position_and_intensity = new Vector4f();
        final Vector2f uv = new Vector2f();

        @Override
        public ByteBuffer store(ByteBuffer buf) {
            position_and_intensity.store(buf);
            uv.store(buf);
            return buf;
        }
    }

    private static final float normal_speed_min = 0.5f;
    private static final float normal_speed_max = 10.f;
    private static final float min_speed_mult = 2.0f;
    private static final float max_speed_mult = 2.5f;
    private static final float max_speed = 12.f;
    private static final float rim_up_speed_min = 4.f;
    private static final float rim_up_speed_max = 5.f;
    private static final float rim_out_speed_min = -5.f;
    private static final float rim_out_speed_max = 8.f;

    // This is the time density we would apply if we wanted to run all our generators at maximum
    // rate without ever running out of particle slots
    private static final float nominal_time_density = (float)(OceanSpray.NUMSPRAYPARTICLES)/((NUMSPRAYGENERATORS+NUMRIMSPRAYGENERATORS)*(kParticleTTL));

    // But in reality, not every generator is emitting all of the time, so we can run at a much higher
    // rate
    private static final float time_density = 7000.f * nominal_time_density;
    private static final float rim_time_density = 150.f * nominal_time_density;

    float CalcMeanSprayGen(float normal_speed, float water_line_proximity, float time_interval, float waterline_detection_radius)
    {
        final float water_line_max =  waterline_detection_radius;
        final float water_line_min = -waterline_detection_radius;
        final float water_line_mid = 0.5f * (water_line_min + water_line_max);
        final float normal_speed_sqr = normal_speed * Math.abs(normal_speed);

        float result = 0.f;
        if(water_line_proximity > water_line_min && water_line_proximity < water_line_max) {
            if(normal_speed_sqr > normal_speed_min) {
                // Triangular CDF for water line
                if(water_line_proximity < water_line_mid) {
                    result = (water_line_proximity - water_line_min)/(water_line_mid - water_line_min);
                } else {
                    result = (water_line_proximity - water_line_max)/(water_line_mid - water_line_max);
                }

                if(normal_speed_sqr < normal_speed_max) {
                    float velocity_multiplier = Math.abs(normal_speed_sqr-normal_speed_min)/(normal_speed_max-normal_speed_min);
                    result *= velocity_multiplier;
                }
            }
        }

        result *= time_density * time_interval;

        return result;
    }

    float CalcMeanRimSprayGen(float water_height, float time_interval)
    {
        return rim_time_density * time_interval * water_height;
    }

    private static void CalcTangentAndBinormal(Vector3f tangent, Vector3f binormal, Vector3f normal)
    {
        Vector3f perp = CacheBuffer.getCachedVec3();
        if(Math.abs(normal.x) < Math.abs(normal.y) && Math.abs(normal.x) < Math.abs(normal.z)) {
            perp.set(1.f,0.f,0.f);
        }
        else if(Math.abs(normal.y) < Math.abs(normal.z)) {
            perp.set(0.f,1.f,0.f);
        }
        else {
            perp.set(0.f,0.f,1.f);
        }

        /*D3DXVec3Cross(&tangent,&perp,&normal);
        D3DXVec3Normalize(&tangent,&tangent);
        D3DXVec3Cross(&binormal,&tangent,&normal);*/

        Vector3f.cross(perp, normal, tangent);
        tangent.normalise();
        Vector3f.cross(tangent, normal, binormal);

        CacheBuffer.free(perp);
    }
}
