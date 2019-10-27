package jet.opengl.demos.nvidia.waves.ocean;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector2f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import jet.opengl.demos.scene.CameraData;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureDataDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.Numeric;

final class OceanSmoke implements OceanConst{

    private static final float kMaxSimulationTimeStep = 0.06f;

    // noise permutation table
    private final static short kNoisePerms[] = {	151,160,137,91,90,15,
            131,13,201,95,96,53,194,233,7,225,140,36,103,30,69,142,8,99,37,240,21,10,23,
            190, 6,148,247,120,234,75,0,26,197,62,94,252,219,203,117,35,11,32,57,177,33,
            88,237,149,56,87,174,20,125,136,171,168, 68,175,74,165,71,134,139,48,27,166,
            77,146,158,231,83,111,229,122,60,211,133,230,220,105,92,41,55,46,245,40,244,
            102,143,54, 65,25,63,161, 1,216,80,73,209,76,132,187,208, 89,18,169,200,196,
            135,130,116,188,159,86,164,100,109,198,173,186, 3,64,52,217,226,250,124,123,
            5,202,38,147,118,126,255,82,85,212,207,206,59,227,47,16,58,17,182,189,28,42,
            223,183,170,213,119,248,152, 2,44,154,163, 70,221,153,101,155,167, 43,172,9,
            129,22,39,253, 19,98,108,110,79,113,224,232,178,185, 112,104,218,246,97,228,
            251,34,242,193,238,210,144,12,191,179,162,241, 81,51,145,235,249,14,239,107,
            49,192,214, 31,181,199,106,157,184, 84,204,176,115,121,50,45,127, 4,150,254,
            138,236,205,93,222,114,67,29,24,72,243,141,128,195,78,66,215,61,156,180
    };

    private static ByteBuffer wrapNoisePerms(){
        ByteBuffer bytes = CacheBuffer.getCachedByteBuffer(kNoisePerms.length);
        for(int i = 0; i < kNoisePerms.length; i++){
            bytes.put((byte)kNoisePerms[i]);
        }

        bytes.flip();
        return bytes;
    }

    private static final int kNoiseG4[] = {
            0, -1, -1, -1,
            0, -1, -1, 1,
            0, -1, 1, -1,
            0, -1, 1, 1,
            0, 1, -1, -1,
            0, 1, -1, 1,
            0, 1, 1, -1,
            0, 1, 1, 1,
            -1, -1, 0, -1,
            -1, 1, 0, -1,
            1, -1, 0, -1,
            1, 1, 0, -1,
            -1, -1, 0, 1,
            -1, 1, 0, 1,
            1, -1, 0, 1,
            1, 1, 0, 1,

            -1, 0, -1, -1,
            1, 0, -1, -1,
            -1, 0, -1, 1,
            1, 0, -1, 1,
            -1, 0, 1, -1,
            1, 0, 1, -1,
            -1, 0, 1, 1,
            1, 0, 1, 1,
            0, -1, -1, 0,
            0, -1, -1, 0,
            0, -1, 1, 0,
            0, -1, 1, 0,
            0, 1, -1, 0,
            0, 1, -1, 0,
            0, 1, 1, 0,
            0, 1, 1, 0,
    };

    private static int encode_noiseg4(int val) {
        switch(val)
        {
            case -1: return 0x80;
            case 1: return 0x7F;
            default: return 0;
        }
    }

    private static final int kPSMLayersPerSlice = 2;

//    ID3DX11Effect* m_pFX;
    private OceanSmokeTechnique m_pRenderToSceneTechnique;
    private OceanSmokeTechnique m_pRenderToPSMTechnique;
    private OceanSmokeTechnique m_pEmitParticlesTechnique;
    private OceanSmokeTechnique m_pSimulateParticlesTechnique;
    private OceanSmokeTechnique m_pBitonicSortTechnique;
    private OceanSmokeTechnique m_pMatrixTransposeTechnique;

    private final OceanPSMParams m_pPSMParams = new OceanPSMParams();
    private final OceanSmokeParams m_TechParams = new OceanSmokeParams();

    // D3D resources
    private Texture2D m_pSmokeTextureSRV;
    private BufferGL m_pRandomUVSRV;
    private BufferGL m_pRenderInstanceDataSRV;
    private BufferGL m_pSimulationInstanceDataUAV;
    private BufferGL m_pSimulationVelocitiesUAV;
    private Texture2D m_ppermTextureSRV;
    private Texture2D m_pgradTexture4dSRV;
    private BufferGL m_pDepthSort1SRV;
    private BufferGL m_pDepthSort1UAV;
    private BufferGL m_pDepthSort2SRV;
    private BufferGL m_pDepthSort2UAV;

    int m_MaxNumParticles;
    int m_ActiveNumParticles;
    int m_NumDepthSortParticles;
    int m_EmitParticleIndex;
    int m_EmitParticleRandomOffset;

    float m_EmitMinVelocity;
    float m_EmitMaxVelocity;
    float m_EmitSpread = 1;
    float m_ParticleBeginSize = 1;
    float m_ParticleEndSize = 1;
    float m_NominalEmitRate;
    float m_ActualEmitRate;
    float m_TimeToNextEmit;
    float m_WindDrag = 1;
    float m_MinBuoyancy;
    float m_MaxBuoyancy = 1;
    float m_CoolingRate = 0.5f;
    float m_NoiseTime;
    final Vector2f m_EmitAreaScale = new Vector2f(1,1);

    final Vector3f m_TintColor = new Vector3f();

    final Matrix4f  m_EmitterMatrix = new Matrix4f();
    boolean m_EmitterMatrixIsValid;

    final Vector3f m_WindVector = new Vector3f();
    boolean m_WindVectorIsValid;

    private GLFuncProvider gl;

    void init(	Texture2D pSmokeTexture,
                     int num_particles,
                     float emit_rate,
                     float particle_begin_size,
                     float particle_end_size,
                     float emit_min_velocity,
                     float emit_max_velocity,
                     float emit_spread,
                     float wind_drag,
                     float min_buoyancy,
                     float max_buoyancy,
                     float cooling_rate,
					 ReadableVector2f emitAreaScale,
                     float psm_bounds_fade_margin,
                     float shadow_opacity_multiplier,
					 ReadableVector3f tint_color,
                     float wind_noise_spatial_scale,
                     float wind_noise_time_scale
    ){
        m_MaxNumParticles = num_particles;
        m_ActiveNumParticles = 0;
        m_EmitParticleIndex = 0;
        m_EmitParticleRandomOffset = 0;
        m_ParticleBeginSize = particle_begin_size;
        m_ParticleEndSize = particle_end_size;
        m_EmitMinVelocity = emit_min_velocity;
        m_EmitMaxVelocity = emit_max_velocity;
        m_EmitSpread = emit_spread;
        m_NominalEmitRate = emit_rate;
        m_ActualEmitRate = 0.f;
        m_TimeToNextEmit = 0.f;
        m_EmitterMatrixIsValid = false;
        m_WindVectorIsValid = false;
        m_WindDrag = wind_drag;
        m_MinBuoyancy = min_buoyancy;
        m_MaxBuoyancy = max_buoyancy;
        m_CoolingRate = cooling_rate;
        m_NoiseTime = 0.f;
        m_EmitAreaScale.set(emitAreaScale);
        m_TintColor.set(tint_color);

        m_TechParams.m_pPSMParams = m_pPSMParams;
        // Depth sort works to power of two, so...
        m_NumDepthSortParticles = 1024;
        while(m_NumDepthSortParticles < m_MaxNumParticles)
            m_NumDepthSortParticles *= 2;

//        SAFE_RELEASE(m_pSmokeTextureSRV);
        m_pSmokeTextureSRV = pSmokeTexture;
//        m_pSmokeTextureSRV->AddRef();

        /*SAFE_RELEASE(m_pFX);
        ID3DXBuffer* pEffectBuffer = NULL;
        V_RETURN(LoadFile(TEXT(".\\Media\\ocean_smoke_d3d11.fxo"), &pEffectBuffer));
        V_RETURN(D3DX11CreateEffectFromMemory(pEffectBuffer->GetBufferPointer(), pEffectBuffer->GetBufferSize(), 0, m_pd3dDevice, &m_pFX));
        pEffectBuffer->Release();*/

        gl = GLFuncProviderFactory.getGLFuncProvider();
        m_pRenderToSceneTechnique = ShaderManager.getInstance().getProgram("RenderSmokeToSceneTech");
        m_pRenderToPSMTechnique = ShaderManager.getInstance().getProgram("RenderSmokeToPSMTech");
        m_pEmitParticlesTechnique = ShaderManager.getInstance().getProgram("EmitParticlesTech");
        m_pSimulateParticlesTechnique = ShaderManager.getInstance().getProgram("SimulateParticlesTech");
        m_pBitonicSortTechnique = ShaderManager.getInstance().getProgram("BitonicSortTech");
        m_pMatrixTransposeTechnique = ShaderManager.getInstance().getProgram("MatrixTransposeTech");

        /*m_pParticleIndexOffsetVariable = m_pFX->GetVariableByName("g_ParticleIndexOffset")->AsScalar();
        m_pParticleCountVariable = m_pFX->GetVariableByName("g_ParticleCount")->AsScalar();
        m_pTimeStepVariable = m_pFX->GetVariableByName("g_TimeStep")->AsScalar();
        m_pPreRollEndTimeVariable = m_pFX->GetVariableByName("g_PreRollEndTime")->AsScalar();
        m_pBuoyancyParamsVariable = m_pFX->GetVariableByName("g_BuoyancyParams")->AsVector();
        m_pNoiseTimeVariable = m_pFX->GetVariableByName("g_NoiseTime")->AsScalar();
        m_pParticleDepthSortUAVVariable = m_pFX->GetVariableByName("g_ParticleDepthSortUAV")->AsUnorderedAccessView();
        m_pParticleDepthSortSRVVariable = m_pFX->GetVariableByName("g_ParticleDepthSortSRV")->AsShaderResource();

        m_pSimulationInstanceDataVariable = m_pFX->GetVariableByName("g_SimulationInstanceData")->AsUnorderedAccessView();
        m_pSimulationVelocitiesVariable = m_pFX->GetVariableByName("g_SimulationVelocities")->AsUnorderedAccessView();
        m_pRandomUVVariable = m_pFX->GetVariableByName("g_RandomUV")->AsShaderResource();
        m_pRandomOffsetVariable = m_pFX->GetVariableByName("g_RandomOffset")->AsScalar();
        m_pCurrEmitterMatrixVariable = m_pFX->GetVariableByName("g_CurrEmitterMatrix")->AsMatrix();
        m_pPrevEmitterMatrixVariable = m_pFX->GetVariableByName("g_PrevEmitterMatrix")->AsMatrix();
        m_pEmitInterpScaleAndOffsetVariable = m_pFX->GetVariableByName("g_EmitInterpScaleAndOffset")->AsVector();
        m_pEmitMinMaxVelocityAndSpreadVariable = m_pFX->GetVariableByName("g_EmitMinMaxVelocityAndSpread")->AsVector();
        m_pEmitAreaScaleVariable = m_pFX->GetVariableByName("g_EmitAreaScale")->AsVector();

        m_pWindDragVariable =  m_pFX->GetVariableByName("g_WindDrag")->AsScalar();
        m_pWindVectorAndNoiseMultVariable =  m_pFX->GetVariableByName("g_WindVectorAndNoiseMult")->AsVector();

        m_pTexDiffuseVariable = m_pFX->GetVariableByName("g_texDiffuse")->AsShaderResource();
        m_pRenderInstanceDataVariable = m_pFX->GetVariableByName("g_RenderInstanceData")->AsShaderResource();
        m_pMatProjVariable = m_pFX->GetVariableByName("g_matProj")->AsMatrix();
        m_pMatViewVariable = m_pFX->GetVariableByName("g_matView")->AsMatrix();
        m_pLightDirectionVariable = m_pFX->GetVariableByName("g_LightDirection")->AsVector();
        m_pLightColorVariable = m_pFX->GetVariableByName("g_LightColor")->AsVector();
        m_pAmbientColorVariable = m_pFX->GetVariableByName("g_AmbientColor")->AsVector();
        m_pFogExponentVariable = m_pFX->GetVariableByName("g_FogExponent")->AsScalar();
        m_pParticleBeginEndScaleVariable = m_pFX->GetVariableByName("g_ParticleBeginEndScale")->AsVector();
        m_pInvParticleLifeTimeVariable = m_pFX->GetVariableByName("g_InvParticleLifeTime")->AsScalar();

        m_pLightningPositionVariable = m_pFX->GetVariableByName("g_LightningPosition")->AsVector();
        m_pLightningColorVariable = m_pFX->GetVariableByName("g_LightningColor")->AsVector();

        m_ppermTextureVariable = m_pFX->GetVariableByName("permTexture")->AsShaderResource();
        m_pgradTexture4dVariable = m_pFX->GetVariableByName("gradTexture4d")->AsShaderResource();

        m_pMatViewToPSMVariable = m_pFX->GetVariableByName("g_matViewToPSM")->AsMatrix();
        m_pPSMParams = new OceanPSMParams(m_pFX);

        m_piDepthSortLevelVariable = m_pFX->GetVariableByName("g_iDepthSortLevel")->AsScalar();
        m_piDepthSortLevelMaskVariable = m_pFX->GetVariableByName("g_iDepthSortLevelMask")->AsScalar();
        m_piDepthSortWidthVariable = m_pFX->GetVariableByName("g_iDepthSortWidth")->AsScalar();
        m_piDepthSortHeightVariable = m_pFX->GetVariableByName("g_iDepthSortHeight")->AsScalar();*/

        // Fire-and-forgets
        /*m_pFX->GetVariableByName("g_NoiseSpatialScale")->AsScalar()->SetFloat(wind_noise_spatial_scale);
        m_pFX->GetVariableByName("g_NoiseTimeScale")->AsScalar()->SetFloat(wind_noise_time_scale);
        m_pFX->GetVariableByName("g_PSMOpacityMultiplier")->AsScalar()->SetFloat(shadow_opacity_multiplier);
        m_pFX->GetVariableByName("g_PSMFadeMargin")->AsScalar()->SetFloat(psm_bounds_fade_margin);*/

        m_TechParams.g_NoiseSpatialScale = wind_noise_spatial_scale;
        m_TechParams.g_NoiseTimeScale = wind_noise_time_scale;
        m_TechParams.g_PSMOpacityMultiplier = shadow_opacity_multiplier;
        m_TechParams.g_PSMFadeMargin = psm_bounds_fade_margin;

        // Create buffer for random unit circle
        {
            Numeric.setRandomSeed(0);
            final int NumRandomUV = Numeric.RAND_MAX/2;
            FloatBuffer randomUV = CacheBuffer.getCachedFloatBuffer(NumRandomUV*2);
            for(int i = 0; i != NumRandomUV; ++i) {
//                randomUV[2*i+0] = (FLOAT(rand())/RAND_MAX);
//                randomUV[2*i+1] = (FLOAT(rand())/RAND_MAX);
                randomUV.put(Numeric.random());
            }
            randomUV.flip();

            /*D3D11_BUFFER_DESC buffer_desc;
            buffer_desc.ByteWidth = sizeof(randomUV);
            buffer_desc.Usage = D3D11_USAGE_IMMUTABLE;
            buffer_desc.BindFlags = D3D11_BIND_SHADER_RESOURCE;
            buffer_desc.CPUAccessFlags = 0;
            buffer_desc.MiscFlags = 0;
            buffer_desc.StructureByteStride = 0;

            D3D11_SUBRESOURCE_DATA srd;
            srd.pSysMem = randomUV;
            srd.SysMemPitch = 0;
            srd.SysMemSlicePitch = 0;

            ID3D11Buffer* pBuffer = NULL;
            V_RETURN(m_pd3dDevice->CreateBuffer(&buffer_desc, &srd, &pBuffer));

            D3D11_SHADER_RESOURCE_VIEW_DESC srv_desc;
            srv_desc.Format = DXGI_FORMAT_R32G32_FLOAT;
            srv_desc.ViewDimension = D3D11_SRV_DIMENSION_BUFFER;
            srv_desc.Buffer.FirstElement = 0;
            srv_desc.Buffer.NumElements = NumRandomUV;
            V_RETURN(m_pd3dDevice->CreateShaderResourceView(pBuffer, &srv_desc, &m_pRandomUVSRV));

            SAFE_RELEASE(pBuffer);*/

            m_pRandomUVSRV = new BufferGL();
            m_pRandomUVSRV.initlize(GLenum.GL_UNIFORM_BUFFER, randomUV.remaining() * 4, randomUV, GLenum.GL_STATIC_DRAW);
        }

        // Create noise buffers
        {
            Texture2DDesc tex_desc = new Texture2DDesc();
            tex_desc.width = kNoisePerms.length;
            tex_desc.height = 1;
            tex_desc.mipLevels = 1;
            tex_desc.arraySize = 1;
            tex_desc.format = GLenum.GL_R8;
            /*tex_desc.Usage = D3D11_USAGE_IMMUTABLE;
            tex_desc.BindFlags = D3D11_BIND_SHADER_RESOURCE;
            tex_desc.CPUAccessFlags = 0;
            tex_desc.MiscFlags = 0;*/

            TextureDataDesc srd = new TextureDataDesc();
            srd.data = wrapNoisePerms();
            srd.format = GLenum.GL_RED;
            srd.type = GLenum.GL_UNSIGNED_BYTE;

            /*ID3D11Texture1D* pTex = NULL;
            V_RETURN(m_pd3dDevice->CreateTexture1D(&tex_desc, &srd, &pTex));
            V_RETURN(m_pd3dDevice->CreateShaderResourceView(pTex, NULL, &));

            SAFE_RELEASE(pTex);*/
            m_ppermTextureSRV = TextureUtils.createTexture2D(tex_desc, srd);
        }

        {
            final int DXGI_FORMAT_R8G8B8A8_SNORM = GLenum.GL_RGBA8_SNORM;
            final int NumNoiseG4s = kNoiseG4.length/4;
            Texture2DDesc tex_desc = new Texture2DDesc();
            tex_desc.width = NumNoiseG4s;
            tex_desc.height = 1;
            tex_desc.mipLevels = 1;
            tex_desc.arraySize = 1;
            tex_desc.format = DXGI_FORMAT_R8G8B8A8_SNORM;
            /*tex_desc.Usage = D3D11_USAGE_IMMUTABLE;
            tex_desc.BindFlags = D3D11_BIND_SHADER_RESOURCE;
            tex_desc.CPUAccessFlags = 0;
            tex_desc.MiscFlags = 0;*/

            IntBuffer tex_data = CacheBuffer.getCachedIntBuffer(NumNoiseG4s);
            for(int i = 0; i != NumNoiseG4s; ++i)
            {
                int r = encode_noiseg4(kNoiseG4[4*i+0]);
                int g = encode_noiseg4(kNoiseG4[4*i+1]);
                int b = encode_noiseg4(kNoiseG4[4*i+2]);
                int a = encode_noiseg4(kNoiseG4[4*i+3]);
                tex_data.put((r << 24) | (g << 16) | (b << 8) | a);
            }
            tex_data.flip();

            TextureDataDesc srd = new TextureDataDesc();
            srd.data = tex_data;
            srd.format = GLenum.GL_RGBA;
            srd.type = GLenum.GL_BYTE;

            /*ID3D11Texture1D* pTex = NULL;
            V_RETURN(m_pd3dDevice->CreateTexture1D(&tex_desc, &srd, &pTex));
            V_RETURN(m_pd3dDevice->CreateShaderResourceView(pTex, NULL, &m_pgradTexture4dSRV));

            SAFE_RELEASE(pTex);*/
            m_pgradTexture4dSRV = TextureUtils.createTexture2D(tex_desc, srd);
        }

        // Create simulation buffers
        {
            /*D3D11_BUFFER_DESC buffer_desc;
            buffer_desc.ByteWidth = m_MaxNumParticles * sizeof(D3DXVECTOR4);
            buffer_desc.Usage = D3D11_USAGE_DEFAULT;
            buffer_desc.BindFlags = D3D11_BIND_SHADER_RESOURCE | D3D11_BIND_UNORDERED_ACCESS;
            buffer_desc.CPUAccessFlags = 0;
            buffer_desc.MiscFlags = 0;
            buffer_desc.StructureByteStride = 0;

            D3D11_SHADER_RESOURCE_VIEW_DESC srv_desc;
            srv_desc.Format = DXGI_FORMAT_R32G32B32A32_FLOAT;
            srv_desc.ViewDimension = D3D11_SRV_DIMENSION_BUFFER;
            srv_desc.Buffer.FirstElement = 0;
            srv_desc.Buffer.NumElements = m_MaxNumParticles;

            D3D11_UNORDERED_ACCESS_VIEW_DESC uav_desc;
            uav_desc.Format = DXGI_FORMAT_R32_FLOAT;
            uav_desc.ViewDimension = D3D11_UAV_DIMENSION_BUFFER;
            uav_desc.Buffer.FirstElement = 0;
            uav_desc.Buffer.NumElements = m_MaxNumParticles * 4;
            uav_desc.Buffer.Flags = 0;

            ID3D11Buffer* pBuffer = NULL;
            V_RETURN(m_pd3dDevice->CreateBuffer(&buffer_desc, NULL, &pBuffer));
            V_RETURN(m_pd3dDevice->CreateShaderResourceView(pBuffer, &srv_desc, &m_pRenderInstanceDataSRV));
            V_RETURN(m_pd3dDevice->CreateUnorderedAccessView(pBuffer, &uav_desc, &m_pSimulationInstanceDataUAV));
            SAFE_RELEASE(pBuffer);

            V_RETURN(m_pd3dDevice->CreateBuffer(&buffer_desc, NULL, &pBuffer));
            V_RETURN(m_pd3dDevice->CreateUnorderedAccessView(pBuffer, &uav_desc, &m_pSimulationVelocitiesUAV));
            SAFE_RELEASE(pBuffer);*/

            m_pRenderInstanceDataSRV = new BufferGL();
            m_pRenderInstanceDataSRV.initlize(GLenum.GL_SHADER_STORAGE_BUFFER, m_MaxNumParticles * Vector4f.SIZE, null, GLenum.GL_DYNAMIC_READ);
            m_pSimulationInstanceDataUAV = m_pRenderInstanceDataSRV;

            m_pSimulationVelocitiesUAV = new BufferGL();
            m_pSimulationVelocitiesUAV.initlize(GLenum.GL_SHADER_STORAGE_BUFFER, m_MaxNumParticles * Vector4f.SIZE, null, GLenum.GL_DYNAMIC_READ);
        }

        // Create depth sort buffers
        {
            /*D3D11_BUFFER_DESC buffer_desc;
            buffer_desc.ByteWidth = 2 * m_NumDepthSortParticles * sizeof(float);
            buffer_desc.Usage = D3D11_USAGE_DEFAULT;
            buffer_desc.BindFlags = D3D11_BIND_SHADER_RESOURCE | D3D11_BIND_UNORDERED_ACCESS;
            buffer_desc.CPUAccessFlags = 0;
            buffer_desc.MiscFlags = D3D11_RESOURCE_MISC_BUFFER_STRUCTURED;
            buffer_desc.StructureByteStride = 2 * sizeof(float);

            D3D11_SHADER_RESOURCE_VIEW_DESC srv_desc;
            srv_desc.Format = DXGI_FORMAT_UNKNOWN;
            srv_desc.ViewDimension = D3D11_SRV_DIMENSION_BUFFER;
            srv_desc.Buffer.FirstElement = 0;
            srv_desc.Buffer.NumElements = m_NumDepthSortParticles;

            D3D11_UNORDERED_ACCESS_VIEW_DESC uav_desc;
            uav_desc.Format = DXGI_FORMAT_UNKNOWN;
            uav_desc.ViewDimension = D3D11_UAV_DIMENSION_BUFFER;
            uav_desc.Buffer.FirstElement = 0;
            uav_desc.Buffer.NumElements = m_NumDepthSortParticles;
            uav_desc.Buffer.Flags = 0;

            ID3D11Buffer* pBuffer = NULL;
            V_RETURN(m_pd3dDevice->CreateBuffer(&buffer_desc, NULL, &pBuffer));
            V_RETURN(m_pd3dDevice->CreateShaderResourceView(pBuffer, &srv_desc, &m_pDepthSort1SRV));
            V_RETURN(m_pd3dDevice->CreateUnorderedAccessView(pBuffer, &uav_desc, &m_pDepthSort1UAV));
            SAFE_RELEASE(pBuffer);*/

            m_pDepthSort1SRV = new BufferGL();
            m_pDepthSort1SRV.initlize(GLenum.GL_UNIFORM_BUFFER, 2 * m_NumDepthSortParticles *4, null, GLenum.GL_DYNAMIC_READ);
            m_pDepthSort1UAV = m_pDepthSort1SRV;

            /*V_RETURN(m_pd3dDevice->CreateBuffer(&buffer_desc, NULL, &pBuffer));
            V_RETURN(m_pd3dDevice->CreateShaderResourceView(pBuffer, &srv_desc, &m_pDepthSort2SRV));
            V_RETURN(m_pd3dDevice->CreateUnorderedAccessView(pBuffer, &uav_desc, &m_pDepthSort2UAV));
            SAFE_RELEASE(pBuffer);*/

            m_pDepthSort2SRV = new BufferGL();
            m_pDepthSort2SRV.initlize(GLenum.GL_UNIFORM_BUFFER, 2 * m_NumDepthSortParticles *4, null, GLenum.GL_DYNAMIC_READ);
            m_pDepthSort2UAV = m_pDepthSort2SRV;
        }
    }

    // Particles are emitted over a unit x-y disk centred on the origin
    void updateSimulation(	//ID3D11DeviceContext* pDC,
							CameraData camera,
                              float time_delta,
							Matrix4f emitterMatrix,
							ReadableVector3f wind_vector,
							float wind_noise,
                            float emit_rate_scale
    ){
        Matrix4f prevEmitterMatrix = m_EmitterMatrix;
	    final boolean prevEmitterMatrixIsValid = m_EmitterMatrixIsValid;

        m_ActualEmitRate = m_NominalEmitRate * emit_rate_scale;

        // Clear the depth sort (this ensures that pow-2 pad entries are set to -1.f view-space,
        // which will force them to sort to end)
	    FloatBuffer depthSortClearValues = CacheBuffer.wrap(-1.f, -1.f, -1.f, -1.f);
//        pDC->ClearUnorderedAccessViewUint(m_pDepthSort1UAV,(UINT*)depthSortClearValues);
        gl.glClearNamedBufferData(m_pDepthSort1UAV.getBuffer(), GLenum.GL_RG32F, GLenum.GL_RG, GLenum.GL_FLOAT, depthSortClearValues);

        // Set common params
//        Vector4f vWindVector = new Vector4f(wind_vector.getX(),wind_vector.getY(),wind_vector.getZ(),wind_noise);
//        m_pWindVectorAndNoiseMultVariable->SetFloatVector((FLOAT*)&vWindVector);
        m_TechParams.g_WindVectorAndNoiseMult.set(wind_vector.getX(),wind_vector.getY(),wind_vector.getZ(),wind_noise);
//        m_pWindDragVariable->SetFloat(m_WindDrag);
        m_TechParams.g_WindDrag = m_WindDrag;

//        D3DXVECTOR4 vBuoyancyParams(m_MinBuoyancy, m_MaxBuoyancy, -m_CoolingRate, 0.f);
//        m_pBuoyancyParamsVariable->SetFloatVector((FLOAT*)&vBuoyancyParams);
        m_TechParams.g_BuoyancyParams.set(m_MinBuoyancy, m_MaxBuoyancy, -m_CoolingRate);

//        m_ppermTextureVariable->SetResource(m_ppermTextureSRV);
        m_TechParams.permTexture =m_ppermTextureSRV;
//        m_pgradTexture4dVariable->SetResource(m_pgradTexture4dSRV);
        m_TechParams.gradTexture4d = m_pgradTexture4dSRV;

//        D3DXMATRIX matView = *camera.GetViewMatrix();
//        m_pMatViewVariable->SetMatrix((FLOAT*)&matView);
        m_TechParams.g_matView = camera.getViewMatrix();

        int num_steps = (int)Math.ceil(time_delta/kMaxSimulationTimeStep);
        float time_step = time_delta/(float)num_steps;
        float step_interp_scale = 1.f/num_steps;
        if(0 == num_steps) {
            // Force through at least one sim step, in order to update depth sort
            num_steps = 1;
            time_step = 0.f;
            step_interp_scale = 1.f;
        }
        for(int step = 0; step != num_steps; ++step)
        {
		    final float step_interp_offset = step * step_interp_scale;
//            m_pNoiseTimeVariable->SetFloat(m_NoiseTime);
            m_TechParams.g_NoiseTime = m_NoiseTime;

            // Can/should emit?
            int num_particles_emitted = 0;
            if(prevEmitterMatrixIsValid) {

                if(time_step < m_TimeToNextEmit) {

                    // Time slice too short to emit
                    m_TimeToNextEmit -= time_step;

                } else {

                    // Emit particles
				    final float emit_interval = 1.f/m_ActualEmitRate;
                    final int num_particles_to_emit = (int)Math.ceil((time_step - m_TimeToNextEmit)/emit_interval);
                    if(num_particles_to_emit!=0) {

                        /*m_pTimeStepVariable->SetFloat(time_delta);*/
                        m_TechParams.g_TimeStep = time_delta;  // NB: We set the entire time slice when emitting, so that we can
                        // interpolate emit positions etc. in a consistent way

                        // But this means that we need to subtract the remainder of the timeslice when calculating pre-roll
					    final float pre_roll_end_time = time_step * (float)(num_steps - step - 1);
//                        m_pPreRollEndTimeVariable->SetFloat(pre_roll_end_time);
                        m_TechParams.g_PreRollEndTime = pre_roll_end_time;

//                        m_pParticleIndexOffsetVariable->SetInt(m_EmitParticleIndex);
                        m_TechParams.g_ParticleIndexOffset = m_EmitParticleIndex;
//                        m_pParticleCountVariable->SetInt(num_particles_to_emit);
                        m_TechParams.g_ParticleCount = num_particles_to_emit;

//                        m_pSimulationInstanceDataVariable->SetUnorderedAccessView(m_pSimulationInstanceDataUAV);
                        m_TechParams.g_SimulationInstanceData = m_pSimulationInstanceDataUAV;
//                        m_pSimulationVelocitiesVariable->SetUnorderedAccessView(m_pSimulationVelocitiesUAV);
                        m_TechParams.g_SimulationVelocities = m_pSimulationVelocitiesUAV;
//                        m_pParticleDepthSortUAVVariable->SetUnorderedAccessView(m_pDepthSort1UAV);
                        m_TechParams.g_ParticleDepthSortUAV = m_pDepthSort1UAV;
//                        m_pRandomUVVariable->SetResource(m_pRandomUVSRV);
                        m_TechParams.g_RandomUV = m_pRandomUVSRV;

//                        m_pRandomOffsetVariable->SetInt(m_EmitParticleRandomOffset);
                        m_TechParams.g_RandomOffset = m_EmitParticleRandomOffset;
//                        m_pCurrEmitterMatrixVariable->SetMatrix((FLOAT*)&emitterMatrix);
                        m_TechParams.g_CurrEmitterMatrix = emitterMatrix;
//                        m_pPrevEmitterMatrixVariable->SetMatrix((FLOAT*)&prevEmitterMatrix);
                        m_TechParams.g_PrevEmitterMatrix = prevEmitterMatrix;

                        /*D3DXVECTOR4 vEmitInterpScaleAndOffset;
                        vEmitInterpScaleAndOffset.y = step_interp_offset + step_interp_scale * m_TimeToNextEmit/time_step;
                        vEmitInterpScaleAndOffset.x = step_interp_scale * emit_interval/time_step;
                        vEmitInterpScaleAndOffset.z = vEmitInterpScaleAndOffset.w = 0.f;*/
//                        m_pEmitInterpScaleAndOffsetVariable->SetFloatVector((FLOAT*)&vEmitInterpScaleAndOffset);
                        m_TechParams.g_EmitInterpScaleAndOffset.set(step_interp_offset + step_interp_scale * m_TimeToNextEmit/time_step, step_interp_scale * emit_interval/time_step);

                        /*D3DXVECTOR4 vEmitVelocityMinMaxAndSpread;
                        vEmitVelocityMinMaxAndSpread.x = m_EmitMinVelocity;
                        vEmitVelocityMinMaxAndSpread.y = m_EmitMaxVelocity;
                        vEmitVelocityMinMaxAndSpread.z = m_EmitSpread;
                        vEmitVelocityMinMaxAndSpread.w = 0.f;*/
//                        m_pEmitMinMaxVelocityAndSpreadVariable->SetFloatVector((FLOAT*)&vEmitVelocityMinMaxAndSpread);
                        m_TechParams.g_EmitMinMaxVelocityAndSpread.set(m_EmitMinVelocity, m_EmitMaxVelocity, m_EmitSpread);

                        /*D3DXVECTOR4 vEmitAreaScale;
                        vEmitAreaScale.x = m_EmitAreaScale.x;
                        vEmitAreaScale.y = m_EmitAreaScale.y;
                        vEmitAreaScale.z = 0.f;
                        vEmitAreaScale.w = 0.f;*/
//                        m_pEmitAreaScaleVariable->SetFloatVector((FLOAT*)&vEmitAreaScale);
                        m_TechParams.g_EmitAreaScale.set(m_EmitAreaScale.x, m_EmitAreaScale.y);

					    final int num_groups = 1+ (num_particles_to_emit-1)/EmitParticlesCSBlocksSize;
//                        m_pEmitParticlesTechnique->GetPassByIndex(0)->Apply(0, pDC);
                        m_pEmitParticlesTechnique.enable(m_TechParams);
//                        pDC->Dispatch(num_groups,1,1);
                        gl.glDispatchCompute(num_groups,1,1);

                        // Clear resource usage
//                        m_pSimulationInstanceDataVariable->SetUnorderedAccessView(NULL);
//                        m_pParticleDepthSortUAVVariable->SetUnorderedAccessView(NULL);
//                        m_pEmitParticlesTechnique->GetPassByIndex(0)->Apply(0, pDC);

                        // Update particle counts/indices
                        num_particles_emitted = num_particles_to_emit;
                        m_EmitParticleIndex = (m_EmitParticleIndex + num_particles_to_emit) % m_MaxNumParticles;
                        m_ActiveNumParticles = Math.min(m_MaxNumParticles,m_ActiveNumParticles+num_particles_to_emit);
                        m_EmitParticleRandomOffset = (m_EmitParticleRandomOffset + 2 * num_particles_to_emit) % (Numeric.RAND_MAX/2);
                        m_TimeToNextEmit = m_TimeToNextEmit + (float)(num_particles_to_emit) * emit_interval - time_step;
                    }
                }
            }

            // Do simulation (exclude just-emitted particles, which will have had pre-roll)
		    final int num_particles_to_simulate = m_ActiveNumParticles - num_particles_emitted;
            if(num_particles_to_simulate > 0) {

                int simulate_start_index = 0;
                if(m_ActiveNumParticles == m_MaxNumParticles) {
                    simulate_start_index = m_EmitParticleIndex;
                }

//                m_pTimeStepVariable->SetFloat(time_step);
                m_TechParams.g_TimeStep = time_step;
//                m_pParticleIndexOffsetVariable->SetInt(simulate_start_index);
                m_TechParams.g_ParticleIndexOffset = simulate_start_index;
//                m_pParticleCountVariable->SetInt(num_particles_to_simulate);
                m_TechParams.g_ParticleCount = num_particles_to_simulate;

//                m_pSimulationInstanceDataVariable->SetUnorderedAccessView(m_pSimulationInstanceDataUAV);
                m_TechParams.g_SimulationInstanceData = m_pSimulationInstanceDataUAV;
//                m_pSimulationVelocitiesVariable->SetUnorderedAccessView(m_pSimulationVelocitiesUAV);
                m_TechParams.g_SimulationVelocities = m_pSimulationVelocitiesUAV;
//                m_pParticleDepthSortUAVVariable->SetUnorderedAccessView(m_pDepthSort1UAV);
                m_TechParams.g_ParticleDepthSortUAV = m_pDepthSort1UAV;

			    final int num_groups = 1+ (num_particles_to_simulate-1)/SimulateParticlesCSBlocksSize;
//                m_pSimulateParticlesTechnique->GetPassByIndex(0)->Apply(0, pDC);
//                pDC->Dispatch(num_groups,1,1);
                m_pSimulateParticlesTechnique.enable(m_TechParams);
                gl.glDispatchCompute(num_groups,1,1);

                // Clear resource usage
//                m_pSimulationInstanceDataVariable->SetUnorderedAccessView(NULL);
//                m_pParticleDepthSortUAVVariable->SetUnorderedAccessView(NULL);
//                m_pSimulateParticlesTechnique->GetPassByIndex(0)->Apply(0, pDC);
            }

            // Evolve procedural noise
            m_NoiseTime += time_step;
        }

        // Update emitter matrix
        m_EmitterMatrix.load(emitterMatrix);
        m_EmitterMatrixIsValid = true;

        // Update wind
        m_WindVector.set(wind_vector);
        m_WindVectorIsValid = true;

        // Depth-sort ready for rendering
        if(m_ActiveNumParticles != 0) {
            depthSortParticles(/*pDC*/);
        }
    }

    void renderToScene(	/*ID3D11DeviceContext* pDC,*/
						CameraData camera,
                           OceanPSM pPSM,
						OceanEnvironment ocean_env){
        if(0==m_ActiveNumParticles)
            return;

        // Matrices
        Matrix4f matView = camera.getViewMatrix();
        Matrix4f matProj = camera.getProjMatrix();

        // View-proj
//        m_pMatProjVariable->SetMatrix((FLOAT*)&matProj);
//        m_pMatViewVariable->SetMatrix((FLOAT*)&matView);
        m_TechParams.g_matProj = matProj;
        m_TechParams.g_matView = matView;

        // Global lighting
//        m_pLightDirectionVariable->SetFloatVector((FLOAT*)&ocean_env.main_light_direction);
        m_TechParams.g_LightDirection = ocean_env.main_light_direction;
//        m_pLightColorVariable->SetFloatVector((FLOAT*)&ocean_env.main_light_color);
        m_TechParams.g_LightColor = ocean_env.main_light_color;
//        m_pAmbientColorVariable->SetFloatVector((FLOAT*)&ocean_env.sky_color);
        m_TechParams.g_AmbientColor =ocean_env.sky_color;

        // Lightnings
//        m_pLightningColorVariable->SetFloatVector((FLOAT*)&ocean_env.lightning_light_intensity);
        m_TechParams.g_LightningColor = ocean_env.lightning_light_intensity;
//        m_pLightningPositionVariable->SetFloatVector((FLOAT*)&ocean_env.lightning_light_position);
        m_TechParams.g_LightningPosition = ocean_env.lightning_light_position;

        // Fog
//        m_pFogExponentVariable->SetFloat(ocean_env.fog_exponent);
        m_TechParams.g_FogExponent=ocean_env.fog_exponent;

        // PSM
        pPSM.setReadParams(m_pPSMParams,m_TintColor);

        /*D3DXMATRIX matInvView;
        D3DXMatrixInverse(&matInvView, NULL, &matView);
        D3DXMATRIX matViewToPSM = matInvView * *pPSM.getWorldToPSMUV();
        m_pMatViewToPSMVariable->SetMatrix((FLOAT*)&matViewToPSM);*/
        Matrix4f result = m_TechParams.g_matViewToPSM;
        Matrix4f.invert(matView, result);
        Matrix4f.mul(pPSM.getWorldToPSMUV(), result, result);

        renderParticles(/*pDC,*/m_pRenderToSceneTechnique);

        pPSM.clearReadParams(m_pPSMParams);
//        m_pRenderToSceneTechnique->GetPassByIndex(0)->Apply(0,pDC);
        m_pRenderToSceneTechnique.enable(m_TechParams);
    }

    void renderToPSM(	//ID3D11DeviceContext* pDC,
                         OceanPSM pPSM,
						 OceanEnvironment ocean_env){
        if(0==m_ActiveNumParticles)
            return;

//        m_pMatViewVariable->SetMatrix((FLOAT*)pPSM->getPSMView());
//        m_pMatProjVariable->SetMatrix((FLOAT*)pPSM->getPSMProj());
        m_TechParams.g_matView = pPSM.getPSMView();
        m_TechParams.g_matProj = pPSM.getPSMProj();
        pPSM.setWriteParams(m_pPSMParams);

        renderParticles(/*pDC,*/m_pRenderToPSMTechnique);
    }


    private void renderParticles(Technique pTech){
        // Particle
//        D3DXVECTOR4 vParticleBeginEndScale(m_ParticleBeginSize, m_ParticleEndSize, 0.f, 0.f);
//        m_pParticleBeginEndScaleVariable->SetFloatVector((FLOAT*)&vParticleBeginEndScale);
        m_TechParams.g_ParticleBeginEndScale.set(m_ParticleBeginSize, m_ParticleEndSize);
//        m_pTexDiffuseVariable->SetResource(m_pSmokeTextureSRV);
        m_TechParams.g_texDiffuse = m_pSmokeTextureSRV;
//        m_pRenderInstanceDataVariable->SetResource(m_pRenderInstanceDataSRV);
        m_TechParams.g_RenderInstanceData = m_pRenderInstanceDataSRV;
//        m_pParticleDepthSortSRVVariable->SetResource(m_pDepthSort1SRV);
        m_TechParams.g_ParticleDepthSortSRV = m_pDepthSort1SRV;
//        m_pInvParticleLifeTimeVariable->SetFloat(m_ActualEmitRate/float(m_MaxNumParticles));
        m_TechParams.g_InvParticleLifeTime = m_ActualEmitRate/(float)m_MaxNumParticles;

	    final int num_particles_to_render = m_ActiveNumParticles;
//        pTech->GetPassByIndex(0)->Apply(0, pDC);
        pTech.enable(m_TechParams);
//        pDC->IASetInputLayout(NULL);
//        pDC->IASetPrimitiveTopology(D3D11_PRIMITIVE_TOPOLOGY_POINTLIST);
//        pDC->Draw(num_particles_to_render,0);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0);
        gl.glDrawArrays(GLenum.GL_POINTS, 0, num_particles_to_render);

        // Clear resource usage
        /*m_pRenderInstanceDataVariable->SetResource(NULL);
        m_pParticleDepthSortSRVVariable->SetResource(NULL);
        pTech->GetPassByIndex(0)->Apply(0, pDC);*/
    }

    private void depthSortParticles(/*ID3D11DeviceContext* pDC*/){
        int num_elements = 1;
        while(num_elements < m_ActiveNumParticles)
        {
            num_elements *= 2;
        }

	    final int matrix_width = BitonicSortCSBlockSize;
        final int matrix_height = num_elements / BitonicSortCSBlockSize;

        // First sort the rows for the levels <= to the block size
        for( int level = 2 ; level <= BitonicSortCSBlockSize ; level = level * 2 )
        {
            setDepthSortConstants( level, level, matrix_height, matrix_width );

            // Sort the row data
//            m_pParticleDepthSortUAVVariable->SetUnorderedAccessView(m_pDepthSort1UAV);
            m_TechParams.g_ParticleDepthSortUAV = m_pDepthSort1UAV;
//            m_pBitonicSortTechnique->GetPassByIndex(0)->Apply(0, pDC);
            m_pBitonicSortTechnique.enable(m_TechParams);
//            pDC->Dispatch( num_elements / BitonicSortCSBlockSize, 1, 1 );
            gl.glDispatchCompute(num_elements / BitonicSortCSBlockSize, 1, 1);
        }

        // Then sort the rows and columns for the levels > than the block size
        // Transpose. Sort the Columns. Transpose. Sort the Rows.
        for( int level = (BitonicSortCSBlockSize * 2) ; level <= num_elements ; level = level * 2 )
        {
            setDepthSortConstants( (level / BitonicSortCSBlockSize), (level & ~num_elements) / BitonicSortCSBlockSize, matrix_width, matrix_height );

            // Transpose the data from buffer 1 into buffer 2
//            m_pParticleDepthSortUAVVariable->SetUnorderedAccessView(m_pDepthSort2UAV);
            m_TechParams.g_ParticleDepthSortUAV = m_pDepthSort2UAV;
//            m_pParticleDepthSortSRVVariable->SetResource(m_pDepthSort1SRV);
            m_TechParams.g_ParticleDepthSortSRV = m_pDepthSort1SRV;
//            m_pMatrixTransposeTechnique->GetPassByIndex(0)->Apply(0, pDC);
            m_pMatrixTransposeTechnique.enable(m_TechParams);
//            pDC->Dispatch( matrix_width / TransposeCSBlockSize, matrix_height / TransposeCSBlockSize, 1 );
            gl.glDispatchCompute(matrix_width / TransposeCSBlockSize, matrix_height / TransposeCSBlockSize, 1);
//            m_pParticleDepthSortSRVVariable->SetResource(NULL);
            m_TechParams.g_ParticleDepthSortSRV = null;
//            m_pMatrixTransposeTechnique->GetPassByIndex(0)->Apply(0, pDC);

            // Sort the transposed column data
//            m_pBitonicSortTechnique->GetPassByIndex(0)->Apply(0, pDC);
            m_pBitonicSortTechnique.enable(m_TechParams);
//            pDC->Dispatch( num_elements / BitonicSortCSBlockSize, 1, 1 );
            gl.glDispatchCompute(num_elements / BitonicSortCSBlockSize, 1, 1);

            setDepthSortConstants( BitonicSortCSBlockSize, level, matrix_height, matrix_width );

            // Transpose the data from buffer 2 back into buffer 1
//            m_pParticleDepthSortUAVVariable->SetUnorderedAccessView(m_pDepthSort1UAV);
            m_TechParams.g_ParticleDepthSortUAV = m_pDepthSort1UAV;
//            m_pParticleDepthSortSRVVariable->SetResource(m_pDepthSort2SRV);
            m_TechParams.g_ParticleDepthSortSRV = m_pDepthSort2SRV;
//            m_pMatrixTransposeTechnique->GetPassByIndex(0)->Apply(0, pDC);
            m_pMatrixTransposeTechnique.enable(m_TechParams);
            gl.glDispatchCompute( matrix_height / TransposeCSBlockSize, matrix_width / TransposeCSBlockSize, 1 );
//            m_pParticleDepthSortSRVVariable->SetResource(NULL);
            m_TechParams.g_ParticleDepthSortSRV = null;
//            m_pMatrixTransposeTechnique->GetPassByIndex(0)->Apply(0, pDC);

            // Sort the row data
//            m_pBitonicSortTechnique->GetPassByIndex(0)->Apply(0, pDC);
            m_pBitonicSortTechnique.enable(m_TechParams);
            gl.glDispatchCompute( num_elements / BitonicSortCSBlockSize, 1, 1 );
        }

        // Release outputs
//        m_pParticleDepthSortUAVVariable->SetUnorderedAccessView(NULL);
//        m_pMatrixTransposeTechnique->GetPassByIndex(0)->Apply(0, pDC);
//        m_pBitonicSortTechnique->GetPassByIndex(0)->Apply(0, pDC);
    }

    private void setDepthSortConstants( int iLevel, int iLevelMask, int iWidth, int iHeight ){
        /*m_piDepthSortLevelVariable->SetInt(iLevel);
        m_piDepthSortLevelMaskVariable->SetInt(iLevelMask);
        m_piDepthSortWidthVariable->SetInt(iWidth);
        m_piDepthSortHeightVariable->SetInt(iHeight);*/

        m_TechParams.g_iDepthSortLevel = iLevel;
        m_TechParams.g_iDepthSortLevelMask = iLevelMask;
        m_TechParams.g_iDepthSortWidth = iWidth;
        m_TechParams.g_iDepthSortHeight = iHeight;
    }
}
