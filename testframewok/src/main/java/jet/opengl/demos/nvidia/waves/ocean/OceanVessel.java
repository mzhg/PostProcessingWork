package jet.opengl.demos.nvidia.waves.ocean;

import com.nvidia.developer.opengl.models.sdkmesh.SDKMeshSubset;
import com.nvidia.developer.opengl.models.sdkmesh.SDKmeshMaterial;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector2f;
import org.lwjgl.util.vector.Vector;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.io.IOException;
import java.nio.IntBuffer;
import java.util.ArrayList;

import jet.opengl.demos.intel.cput.ID3D11InputLayout;
import jet.opengl.demos.nvidia.waves.wavework.GFSDK_WaveWorks;
import jet.opengl.demos.nvidia.waves.wavework.GFSDK_WaveWorks_Simulation;
import jet.opengl.demos.scene.CameraData;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureDataDesc;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.BoundingBox;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.CommonUtil;
import jet.opengl.postprocessing.util.Numeric;
import jet.opengl.postprocessing.util.NvImage;
import jet.opengl.postprocessing.util.StringUtils;

final class OceanVessel implements OceanConst{

    private static final float kAccelerationDueToGravity = 9.81f;
    private static final float kDensityOfWater = 1000.f;
    private static final float kMaximumFractionalCornerError = 0.0001f; // 100ppm
    private static final float kMaxSimulationTimeStep = 0.02f;

    private static final float kSpotlightClipNear = 0.1f;
    private static final float kSpotlightClipFar = 200.0f;

    // ---------------------------------- GPU shading data ------------------------------------

    // D3D objects
//    ID3D11Device* m_pd3dDevice;

    private BoatMesh m_pMesh;
    private float m_meshRenderScale;
    private float m_DiffuseGamma;
    private ID3D11InputLayout m_pLayout;

//    ID3DX11Effect* m_pFX;
    private GLSLProgram m_pRenderToSceneTechnique;
    private GLSLProgram m_pRenderToShadowMapTechnique;
    private GLSLProgram m_pRenderToHullProfileTechnique;
    private GLSLProgram m_pRenderQuadToUITechnique;
    private GLSLProgram m_pRenderQuadToCrackFixTechnique;
    private GLSLProgram m_pWireframeOverrideTechnique;
    /*ID3DX11EffectMatrixVariable* m_pMatWorldViewProjVariable;
    ID3DX11EffectMatrixVariable* m_pMatWorldViewVariable;
    ID3DX11EffectMatrixVariable* m_pMatWorldVariable;
    ID3DX11EffectShaderResourceVariable *m_pTexDiffuseVariable;
    ID3DX11EffectShaderResourceVariable *m_pTexRustMapVariable;
    ID3DX11EffectShaderResourceVariable *m_pTexRustVariable;
    ID3DX11EffectShaderResourceVariable *m_pTexBumpVariable;
    ID3DX11EffectVectorVariable* m_pDiffuseColorVariable;
    ID3DX11EffectVectorVariable* m_pLightDirectionVariable;
    ID3DX11EffectVectorVariable* m_pLightColorVariable;
    ID3DX11EffectVectorVariable* m_pAmbientColorVariable;

    ID3DX11EffectScalarVariable* m_pSpotlightNumVariable;
    ID3DX11EffectVectorVariable* m_pSpotlightPositionVariable;
    ID3DX11EffectVectorVariable* m_pSpotLightAxisAndCosAngleVariable;
    ID3DX11EffectVectorVariable* m_pSpotlightColorVariable;
    ID3DX11EffectMatrixVariable* m_pSpotlightShadowMatrixVar;
    ID3DX11EffectShaderResourceVariable* m_pSpotlightShadowResourceVar;

    ID3DX11EffectVectorVariable* m_pLightningPositionVariable;
    ID3DX11EffectVectorVariable* m_pLightningColorVariable;

    ID3DX11EffectScalarVariable* m_pFogExponentVariable;*/

    private TextureGL m_pWhiteTextureSRV;
    private TextureGL m_pRustMapSRV;
    private TextureGL m_pRustSRV;
    private TextureGL m_pBumpSRV;

    private final TextureGL[] m_pHullProfileSRV = new TextureGL[2];
    private final TextureGL[] m_pHullProfileRTV = new TextureGL[2];
    private TextureGL m_pHullProfileDSV;

    private float m_Draft;
    private float m_Length;
    private float m_Beam;
    private float m_MeshHeight;
    private float m_BuoyantArea;
    private float m_Mass;
    private float m_PitchInertia;
    private float m_RollInertia;
    private float m_MetacentricHeight;
    private float m_LongitudinalCOM;
    private float m_MassMult;
    private float m_PitchInertiaMult;
    private float m_RollInertiaMult;

    private float m_InitialPitch;
    private float m_InitialHeight;

    private final Vector3f m_bbCentre = new Vector3f();
    private final Vector3f m_bbExtents = new Vector3f();

    private OceanVesselDynamicState m_pDynamicState;

    private float m_HeightDrag;
    private float m_PitchDrag;
    private float m_YawDrag;
    private float m_YawCoefficient;
    private float m_RollDrag;

    private float m_CameraHeightAboveWater;
    private float m_CameraLongitudinalOffset;

    private int m_HullProfileTextureWH;

    private String m_MeshFileName;

    private final Matrix4f m_MeshToLocal = new Matrix4f();
    private final Matrix4f m_CameraToLocal = new Matrix4f();

    private final static class Spotlight{
        final Vector3f position = new Vector3f();
        final Vector3f axis = new Vector3f();
        float beam_angle;
        final Vector4f color = new Vector4f();
    };

    private final ArrayList<Spotlight> m_Spotlights = new ArrayList<>();

    private static final class SpotlightShadow
    {
        final Matrix4f              m_ViewProjMatrix = new Matrix4f();
        boolean                     m_Dirty;

        Texture2D                   m_pResource;
        Texture2D                   m_pDSV;
        Texture2D                   m_pSRV;
    };

    private final ArrayList<SpotlightShadow> m_SpotlightsShadows = new ArrayList<>();

    // Smoke sim
    private float m_FunnelHeightAboveWater;
    private float m_FunnelLongitudinalOffset;
    private final Vector2f m_FunnelMouthSize = new Vector2f();
    private final Matrix4f m_FunnelMouthToLocal = new Matrix4f();

    private String m_SmokeTextureFileName;
    private int m_NumSmokeParticles;
    private float m_SmokeParticleEmitRate;
    private float m_SmokeParticleBeginSize;
    private float m_SmokeParticleEndSize;
    private float m_SmokeParticleEmitMinVelocity;
    private float m_SmokeParticleEmitMaxVelocity;
    private float m_SmokeParticleEmitSpread;
    private float m_SmokeParticleMinBuoyancy;
    private float m_SmokeParticleMaxBuoyancy;
    private float m_SmokeParticleCoolingRate;
    private float m_SmokeWindDrag;
    private float m_SmokePSMBoundsFadeMargin;
    private float m_SmokeWindNoiseLevel;
    private float m_SmokeWindNoiseSpatialScale;
    private float m_SmokeWindNoiseTimeScale;
    private final Vector3f m_SmokeTint = new Vector3f();
    private float m_SmokeShadowOpacity;
    private OceanSmoke m_pSmoke;

    private int m_PSMRes;
    private final Vector3f m_PSMMinCorner = new Vector3f();
    private final Vector3f m_PSMMaxCorner = new Vector3f();
    private OceanPSM m_pPSM;

    private int m_NumSurfaceHeightSamples;
    private OceanSurfaceHeights m_pSurfaceHeights;

    private OceanHullSensors m_pHullSensors;
    private boolean m_bFirstSensorUpdate;

    private void parseConfig(String cfg_string);
    private Spotlight processGlobalConfigLine(String line);
    private Spotlight processSpotlightConfigLine(String line, Spotlight pSpot);

    OceanVessel(OceanVesselDynamicState pDynamicState){
        m_pDynamicState = pDynamicState;
        m_HeightDrag = 1.f;
        m_PitchDrag = 2.f;
        m_YawDrag = 1.f;
        m_YawCoefficient = 1.f;
        m_RollDrag = 1.f;

        m_Length = 30.f;
        m_CameraHeightAboveWater = 6.f;
        m_CameraLongitudinalOffset = 5.f;
        m_MetacentricHeight = 1.f;
        m_LongitudinalCOM = -7.f;
        m_MassMult = 0.5f;
        m_PitchInertiaMult = 0.2f;
        m_RollInertiaMult = 0.3f;

        m_InitialPitch = 0.0438f;
        m_InitialHeight = -0.75f;

        m_HullProfileTextureWH = 512;

        m_DiffuseGamma = 3.f;

        m_FunnelLongitudinalOffset = 0.f;
        m_FunnelHeightAboveWater = 6.f;
        m_FunnelMouthSize.set(1.f,1.f);

        m_NumSmokeParticles = 4096;
        m_SmokeParticleEmitRate = (m_NumSmokeParticles)/10.f;
        m_SmokeParticleEmitMinVelocity = 1.f;
        m_SmokeParticleEmitMaxVelocity = 1.f;
        m_SmokeParticleMinBuoyancy = 0.f;
        m_SmokeParticleMaxBuoyancy = 1.f;
        m_SmokeParticleCoolingRate = 0.f;
        m_SmokeParticleEmitSpread = 0.f;
        m_SmokeParticleBeginSize = 1.f;
        m_SmokeParticleEndSize = 1.f;
        m_SmokeWindDrag = 1.f;
        m_SmokePSMBoundsFadeMargin = 0.1f;
        m_SmokeWindNoiseLevel = 2.f;
        m_SmokeWindNoiseSpatialScale = 1.f;
        m_SmokeWindNoiseTimeScale = 1.f;
        m_SmokeTint.set( 1.f, 1.f, 1.f);
        m_SmokeShadowOpacity = 1.f;
        m_SmokeTextureFileName = null;
        m_pSmoke = null;

        m_PSMRes = 512;
        m_PSMMinCorner.set(-1.f,-1.f,-1.f);
        m_PSMMaxCorner.set( 1.f, 1.f, 1.f);
        m_pPSM = null;

        m_NumSurfaceHeightSamples = 1000;
    }

    void init(String cfg_string, boolean allow_smoke){
        // Parse the cfg file
        parseConfig(cfg_string);

        // Load the mesh
        m_pMesh = new BoatMesh();
        try {
            m_pMesh.create(m_MeshFileName);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Get the bounding box and figure out the scale needed to achieve the desired length,
        // then figure out the other dims
        int num_meshes = m_pMesh.getNumMeshes();
        if(0 == num_meshes)
            throw new IllegalStateException();

        m_bbExtents.set(m_pMesh.getMeshBBoxExtents(0));
        m_bbCentre.set(m_pMesh.getMeshBBoxCenter(0));
        float meshRenderScale = 0.5f * m_Length/m_bbExtents.z;

        /*D3DXMATRIX matMeshScale;
        D3DXMatrixScaling(&matMeshScale, meshRenderScale, meshRenderScale, meshRenderScale);

        D3DXMATRIX matMeshOrient;
        D3DXMatrixRotationY(&matMeshOrient, D3DX_PI);

        D3DXMATRIX matMeshOffset;
        D3DXMatrixTranslation(&matMeshOffset, -m_bbCentre.x, 0.f, -m_bbCentre.z);

        m_MeshToLocal = matMeshOffset * matMeshScale * matMeshOrient;*/
        m_MeshToLocal.setIdentity();
        m_MeshToLocal.rotate(Numeric.PI, Vector3f.Y_AXIS);
        m_MeshToLocal.scale(meshRenderScale, meshRenderScale, meshRenderScale);
        m_MeshToLocal.translate(-m_bbCentre.x, 0.f, -m_bbCentre.z);

        /*D3DXMatrixIdentity(&m_CameraToLocal);
        m_CameraToLocal._42 = m_CameraHeightAboveWater;
        m_CameraToLocal._43 = m_CameraLongitudinalOffset;*/
        m_CameraToLocal.setIdentity();
        m_CameraToLocal.m31 = m_CameraHeightAboveWater;
        m_CameraToLocal.m32 = m_CameraLongitudinalOffset;

        /*D3DXMatrixRotationX(&m_FunnelMouthToLocal,-3.14f*0.3f);
        m_FunnelMouthToLocal._42 = m_FunnelHeightAboveWater;
        m_FunnelMouthToLocal._43 = m_FunnelLongitudinalOffset;*/
        m_FunnelMouthToLocal.setIdentity();
        m_FunnelMouthToLocal.rotate(-3.14f*0.3f, Vector3f.X_AXIS);
        m_FunnelMouthToLocal.m31 = m_FunnelHeightAboveWater;
        m_FunnelMouthToLocal.m32 = m_FunnelLongitudinalOffset;

        m_Draft = (m_bbExtents.y-m_bbCentre.y) * meshRenderScale;								// Assumes mesh was modelled with the MWL at y = 0
        m_Beam = 2.f * m_bbExtents.x * meshRenderScale;
        m_MeshHeight = 2.f * m_bbExtents.y * meshRenderScale;
        m_BuoyantArea = m_Length * m_Beam;
        m_Mass = m_BuoyantArea * m_Draft * kDensityOfWater;									// At equilibrium, the displaced water is equal to the mass of the ship
        m_Mass *= 0.25f*Numeric.PI;															// We approximate the hull profile with an ellipse, it is important to
        // match this in the mass calc so that the ship sits at the right height
        // in the water at rest
        m_Mass *= m_MassMult;
        m_PitchInertia = m_Mass * (m_Draft * m_Draft + m_Length * m_Length)/12.f;	// Use the inertia of the displaced water
        m_RollInertia = m_Mass * (m_Draft * m_Draft + m_Beam * m_Beam)/12.f;

        m_PitchInertia *= m_PitchInertiaMult;
        m_RollInertia *= m_RollInertiaMult;

        /*SAFE_RELEASE(m_pFX);
        ID3DXBuffer* pEffectBuffer = NULL;
        V_RETURN(LoadFile(TEXT(".\\Media\\ocean_vessel_d3d11.fxo"), &pEffectBuffer));
        V_RETURN(D3DX11CreateEffectFromMemory(pEffectBuffer->GetBufferPointer(), pEffectBuffer->GetBufferSize(), 0, m_pd3dDevice, &m_pFX));
        pEffectBuffer->Release();*/

        m_pRenderToSceneTechnique = m_pFX->GetTechniqueByName("RenderVesselToSceneTech");
        m_pRenderToShadowMapTechnique = m_pFX->GetTechniqueByName("RenderVesselToShadowMapTech");
        m_pRenderToHullProfileTechnique = m_pFX->GetTechniqueByName("RenderVesselToHullProfileTech");
        m_pRenderQuadToUITechnique = m_pFX->GetTechniqueByName("RenderQuadToUITech");
        m_pRenderQuadToCrackFixTechnique = m_pFX->GetTechniqueByName("RenderQuadToCrackFixTech");
        m_pWireframeOverrideTechnique = m_pFX->GetTechniqueByName("WireframeOverrideTech");
        /*m_pMatWorldViewProjVariable = m_pFX->GetVariableByName("g_matWorldViewProj")->AsMatrix();
        m_pMatWorldVariable = m_pFX->GetVariableByName("g_matWorld")->AsMatrix();
        m_pMatWorldViewVariable = m_pFX->GetVariableByName("g_matWorldView")->AsMatrix();
        m_pTexDiffuseVariable = m_pFX->GetVariableByName("g_texDiffuse")->AsShaderResource();
        m_pTexRustMapVariable = m_pFX->GetVariableByName("g_texRustMap")->AsShaderResource();
        m_pTexRustVariable = m_pFX->GetVariableByName("g_texRust")->AsShaderResource();
        m_pTexBumpVariable = m_pFX->GetVariableByName("g_texBump")->AsShaderResource();
        m_pDiffuseColorVariable = m_pFX->GetVariableByName("g_DiffuseColor")->AsVector();
        m_pLightDirectionVariable = m_pFX->GetVariableByName("g_LightDirection")->AsVector();
        m_pLightColorVariable = m_pFX->GetVariableByName("g_LightColor")->AsVector();
        m_pAmbientColorVariable = m_pFX->GetVariableByName("g_AmbientColor")->AsVector();

        m_pSpotlightNumVariable = m_pFX->GetVariableByName("g_LightsNum")->AsScalar();
        m_pSpotlightPositionVariable = m_pFX->GetVariableByName("g_SpotlightPosition")->AsVector();
        m_pSpotLightAxisAndCosAngleVariable = m_pFX->GetVariableByName("g_SpotLightAxisAndCosAngle")->AsVector();
        m_pSpotlightColorVariable = m_pFX->GetVariableByName("g_SpotlightColor")->AsVector();
        m_pSpotlightShadowMatrixVar = m_pFX->GetVariableByName("g_SpotlightMatrix")->AsMatrix();
        m_pSpotlightShadowResourceVar = m_pFX->GetVariableByName("g_SpotlightResource")->AsShaderResource();

        m_pLightningPositionVariable = m_pFX->GetVariableByName("g_LightningPosition")->AsVector();
        m_pLightningColorVariable = m_pFX->GetVariableByName("g_LightningColor")->AsVector();

        m_pFogExponentVariable = m_pFX->GetVariableByName("g_FogExponent")->AsScalar();

        D3DX11_PASS_DESC PassDesc;
        V_RETURN(m_pRenderToSceneTechnique->GetPassByIndex(0)->GetDesc(&PassDesc));

        SAFE_RELEASE(m_pLayout);*/
        m_pLayout = m_pMesh.CreateInputLayout(/*m_pd3dDevice, */0, 0, null, num_meshes);

        // Set up an all-white texture SRV to use when a mesh subset has no associated texture
        {
            CommonUtil.safeRelease(m_pWhiteTextureSRV);

            final int WhiteTextureWH = 4;
            Texture2DDesc tex_desc = new Texture2DDesc();
            tex_desc.width = WhiteTextureWH;
            tex_desc.height = WhiteTextureWH;
            tex_desc.arraySize = 1;
//            tex_desc.BindFlags = D3D11_BIND_SHADER_RESOURCE;
//            tex_desc.CPUAccessFlags = 0;
            tex_desc.format = DXGI_FORMAT_R8G8B8A8_UNORM;
            tex_desc.mipLevels = 1;
//            tex_desc.MiscFlags = 0;
//            tex_desc.SampleDesc.Count = 1;
//            tex_desc.SampleDesc.Quality = 0;
//            tex_desc.Usage = D3D11_USAGE_IMMUTABLE;

            IntBuffer tex_data = CacheBuffer.getCachedIntBuffer(WhiteTextureWH * WhiteTextureWH);
            for(int i = 0; i != WhiteTextureWH * WhiteTextureWH; ++i) {
                tex_data.put(0xFFFFFFFF);
            }
            tex_data.flip();

            TextureDataDesc tex_srd = new TextureDataDesc();
            tex_srd.data = tex_data;
            tex_srd.format = GLenum.GL_RGBA; // WhiteTextureWH * sizeof(tex_data[0]);
            tex_srd.type = GLenum.GL_UNSIGNED_BYTE; // sizeof(tex_data);

            /*ID3D11Texture2D* pWhiteTetxure = NULL;
            V_RETURN(m_pd3dDevice->CreateTexture2D(&tex_desc,&tex_srd,&pWhiteTetxure));
            V_RETURN(m_pd3dDevice->CreateShaderResourceView(pWhiteTetxure,NULL,&m_pWhiteTextureSRV));
            SAFE_RELEASE(pWhiteTetxure);*/

            m_pWhiteTextureSRV = TextureUtils.createTexture2D(tex_desc, tex_srd);
        }

        {
            CommonUtil.safeRelease(m_pHullProfileSRV[0]);
            CommonUtil.safeRelease(m_pHullProfileSRV[1]);
            CommonUtil.safeRelease(m_pHullProfileRTV[0]);
            CommonUtil.safeRelease(m_pHullProfileRTV[1]);
            CommonUtil.safeRelease(m_pHullProfileDSV);
//            ID3D11Texture2D* pTexture = NULL;

            // Set up textures for rendering hull profile
            Texture2DDesc tex_desc = new Texture2DDesc();
            tex_desc.width = m_HullProfileTextureWH;
            tex_desc.height = m_HullProfileTextureWH;
            tex_desc.arraySize = 1;
//            tex_desc.BindFlags = D3D11_BIND_SHADER_RESOURCE | D3D11_BIND_RENDER_TARGET;
//            tex_desc.CPUAccessFlags = 0;
            tex_desc.format = DXGI_FORMAT_R16G16_UNORM;
            tex_desc.mipLevels = 1;
//            tex_desc.MiscFlags = 0;
//            tex_desc.SampleDesc.Count = 1;
//            tex_desc.SampleDesc.Quality = 0;
//            tex_desc.Usage = D3D11_USAGE_DEFAULT;

            /*V_RETURN(m_pd3dDevice->CreateTexture2D(&tex_desc,NULL,&pTexture));
            V_RETURN(m_pd3dDevice->CreateShaderResourceView(pTexture,NULL,&m_pHullProfileSRV[0]));
            V_RETURN(m_pd3dDevice->CreateRenderTargetView(pTexture,NULL,&m_pHullProfileRTV[0]));
            SAFE_RELEASE(pTexture);*/

            m_pHullProfileRTV[0] = m_pHullProfileSRV[0] = TextureUtils.createTexture2D(tex_desc, null);

            tex_desc.mipLevels = Numeric.calculateMipLevels(m_HullProfileTextureWH);
//            tex_desc.MiscFlags = D3D11_RESOURCE_MISC_GENERATE_MIPS;
            /*V_RETURN(m_pd3dDevice->CreateTexture2D(&tex_desc,NULL,&pTexture));
            V_RETURN(m_pd3dDevice->CreateShaderResourceView(pTexture,NULL,&m_pHullProfileSRV[1]));
            V_RETURN(m_pd3dDevice->CreateRenderTargetView(pTexture,NULL,&m_pHullProfileRTV[1]));
            SAFE_RELEASE(pTexture);*/

            m_pHullProfileRTV[1] = m_pHullProfileSRV[1] = TextureUtils.createTexture2D(tex_desc, null);

            Texture2DDesc depth_tex_desc = new Texture2DDesc();
            depth_tex_desc.width = m_HullProfileTextureWH;
            depth_tex_desc.height = m_HullProfileTextureWH;
            depth_tex_desc.arraySize = 1;
//            depth_tex_desc.BindFlags = D3D11_BIND_DEPTH_STENCIL;
//            depth_tex_desc.CPUAccessFlags = 0;
            depth_tex_desc.format = DXGI_FORMAT_D24_UNORM_S8_UINT;
            depth_tex_desc.mipLevels = 1;
//            depth_tex_desc.MiscFlags = 0;
//            depth_tex_desc.SampleDesc.Count = 1;
//            depth_tex_desc.SampleDesc.Quality = 0;
//            depth_tex_desc.Usage = D3D11_USAGE_DEFAULT;

            /*V_RETURN(m_pd3dDevice->CreateTexture2D(&depth_tex_desc,NULL,&pTexture));
            V_RETURN(m_pd3dDevice->CreateDepthStencilView(pTexture,NULL,&m_pHullProfileDSV));
            SAFE_RELEASE(pTexture);*/
            m_pHullProfileDSV = TextureUtils.createTexture2D(depth_tex_desc, null);
        }

//#if ENABLE_SHADOWS
        if (!m_Spotlights.isEmpty())
        {
            int lightsNum = m_Spotlights.size();
            m_SpotlightsShadows.ensureCapacity(lightsNum);

            for (int i=0; i<lightsNum; ++i)
            {
                SpotlightShadow shadow = new SpotlightShadow();

                /*CD3D11_TEXTURE2D_DESC desc(DXGI_FORMAT_R24G8_TYPELESS, (UINT)kSpotlightShadowResolution, (UINT)kSpotlightShadowResolution, 1, 1, D3D11_BIND_SHADER_RESOURCE | D3D11_BIND_DEPTH_STENCIL);
                V_RETURN(m_pd3dDevice->CreateTexture2D(&desc, NULL, &shadow.m_pResource));

                CD3D11_DEPTH_STENCIL_VIEW_DESC descDSV(D3D11_DSV_DIMENSION_TEXTURE2D, DXGI_FORMAT_D24_UNORM_S8_UINT);
                V_RETURN(m_pd3dDevice->CreateDepthStencilView(shadow.m_pResource, &descDSV, &m_SpotlightsShadows[i].m_pDSV));

                CD3D11_SHADER_RESOURCE_VIEW_DESC descSRV(D3D11_SRV_DIMENSION_TEXTURE2D, DXGI_FORMAT_R24_UNORM_X8_TYPELESS);
                V_RETURN(m_pd3dDevice->CreateShaderResourceView(shadow.m_pResource, &descSRV, &m_SpotlightsShadows[i].m_pSRV));*/

                Texture2DDesc desc = new Texture2DDesc((int)kSpotlightShadowResolution, (int)kSpotlightShadowResolution, DXGI_FORMAT_D24_UNORM_S8_UINT);
                shadow.m_pDSV = shadow.m_pResource = shadow.m_pSRV = TextureUtils.createTexture2D(desc, null);

                m_SpotlightsShadows.add(shadow);
            }
        }
//#endif

        if(!StringUtils.isEmpty(m_SmokeTextureFileName) && allow_smoke) {
            /*ID3D11Resource* pD3D11Resource = NULL;
            V_RETURN(CreateTextureFromFileSRGB(m_pd3dDevice, m_SmokeTextureFileName, &pD3D11Resource));
            ID3D11ShaderResourceView* pSmokeTextureSRV;
            V_RETURN(m_pd3dDevice->CreateShaderResourceView(pD3D11Resource, NULL, &pSmokeTextureSRV));
            SAFE_RELEASE(pD3D11Resource);*/

            NvImage.loadAsSRGB(true);
            Texture2D pSmokeTextureSRV = OceanConst.CreateTexture2DFromFileSRGB(m_SmokeTextureFileName);

            m_pSmoke = new OceanSmoke();
            m_pSmoke.init(	pSmokeTextureSRV,
                    m_NumSmokeParticles,
                    m_SmokeParticleEmitRate,
                    m_SmokeParticleBeginSize,
                    m_SmokeParticleEndSize,
                    m_SmokeParticleEmitMinVelocity,
                    m_SmokeParticleEmitMaxVelocity,
                    m_SmokeParticleEmitSpread,
                    m_SmokeWindDrag,
                    m_SmokeParticleMinBuoyancy,
                    m_SmokeParticleMaxBuoyancy,
                    m_SmokeParticleCoolingRate,
                    m_FunnelMouthSize,
                    m_SmokePSMBoundsFadeMargin,
                    m_SmokeShadowOpacity,
                    m_SmokeTint,
                    m_SmokeWindNoiseSpatialScale,
                    m_SmokeWindNoiseTimeScale
            );
//            SAFE_RELEASE(pSmokeTextureSRV);
        }

        m_pPSM = new OceanPSM();
        m_pPSM.init(m_PSMMinCorner,m_PSMMaxCorner,m_PSMRes);

        if(null == m_pRustMapSRV)
        {
            /*ID3D11Resource* pD3D11Resource = NULL;
            V_RETURN(D3DX11CreateTextureFromFile(m_pd3dDevice, TEXT(".\\media\\rustmap.dds"), NULL, NULL, &pD3D11Resource, NULL));
            V_RETURN(m_pd3dDevice->CreateShaderResourceView(pD3D11Resource, NULL, &m_pRustMapSRV));
            SAFE_RELEASE(pD3D11Resource);*/

            m_pRustMapSRV = OceanConst.CreateTexture2DFromFileSRGB(SHADER_PATH + "\\media\\rustmap.dds");
        }

        if(null == m_pRustSRV)
        {
            /*ID3D11Resource* pD3D11Resource = NULL;
            V_RETURN(D3DX11CreateTextureFromFile(m_pd3dDevice, TEXT(".\\media\\rust.dds"), NULL, NULL, &pD3D11Resource, NULL));
            V_RETURN(m_pd3dDevice->CreateShaderResourceView(pD3D11Resource, NULL, &m_pRustSRV));
            SAFE_RELEASE(pD3D11Resource);*/

            m_pRustSRV = OceanConst.CreateTexture2DFromFileSRGB(SHADER_PATH + "\\media\\rust.dds");
        }

        if(null == m_pBumpSRV)
        {
            /*ID3D11Resource* pD3D11Resource = NULL;
            V_RETURN(D3DX11CreateTextureFromFile(m_pd3dDevice, TEXT(".\\media\\foam_intensity_perlin2.dds"), NULL, NULL, &pD3D11Resource, NULL));
            V_RETURN(m_pd3dDevice->CreateShaderResourceView(pD3D11Resource, NULL, &m_pBumpSRV));
            SAFE_RELEASE(pD3D11Resource);*/

            m_pBumpSRV = OceanConst.CreateTexture2DFromFileSRGB(SHADER_PATH + "\\media\\foam_intensity_perlin2.dds");
        }

        Vector2f UVToWorldScale = new Vector2f();
        UVToWorldScale.x = (float) (2.f * Math.sqrt(m_Beam*m_Beam+m_MeshHeight*m_MeshHeight));	// Use double-diagonal, to be conservative
        UVToWorldScale.x +=	m_Length;												// Then add another vessel length, to make sure we catch big bow sprays
        UVToWorldScale.y = m_Length + 2.f * m_MeshHeight;							// Add height, to be conservative

        m_pSurfaceHeights = new OceanSurfaceHeights(m_NumSurfaceHeightSamples,UVToWorldScale);
        m_pSurfaceHeights.init();

        m_pHullSensors = new OceanHullSensors();
        m_pHullSensors.init(m_pMesh, getMeshToLocalXform());
        m_bFirstSensorUpdate = true;
    }

    private void renderVessel(	//ID3D11DeviceContext* pDC,
                                  GLSLProgram pTechnique,
                                  OceanVesselSubset pSubsetOverride,
                                  boolean wireframe,
                                  boolean depthOnly){
        if(null == m_pMesh)
            return;

        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        // Iterate over mesh subsets to draw
        BoatMesh mesh = m_pMesh;
//        UINT Strides[1];
//        UINT Offsets[1];
//        ID3D11Buffer* pVB[1];
        int pVB = mesh.getVB10(0,0);
        int stride = mesh.getVertexStride(0,0);
//        Offsets[0] = 0;
//        pDC->IASetVertexBuffers( 0, 1, pVB, Strides, Offsets );
//        pDC->IASetInputLayout(m_pLayout);

        gl.glBindVertexArray(0);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, pVB);
        m_pLayout.bind();


        m_pTexRustMapVariable.SetResource(m_pRustMapSRV);
        m_pTexRustVariable.SetResource(m_pRustSRV);
        m_pTexBumpVariable.SetResource(m_pBumpSRV);
        if(pSubsetOverride != null) {
//            pDC->IASetPrimitiveTopology( D3D11_PRIMITIVE_TOPOLOGY_POINTLIST );
//            pDC->IASetIndexBuffer( pSubsetOverride->pIB, pSubsetOverride->ib_format, 0);

            gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, pSubsetOverride.pIB.getBuffer());

            m_pTexDiffuseVariable.SetResource(m_pWhiteTextureSRV);
            D3DXVECTOR4 diffuse = D3DXVECTOR4(100,100,100,100);
            m_pDiffuseColorVariable->SetFloatVector((FLOAT*)&diffuse);
//            pTechnique->GetPassByIndex(0)->Apply(0, pDC);
            pTechnique.enable();
            if(wireframe)
                m_pWireframeOverrideTechnique->GetPassByIndex(0)->Apply(0,pDC);
//            pDC->DrawIndexed(pSubsetOverride->index_count, 0, 0);
            gl.glDrawElements(GLenum.GL_POINTS, pSubsetOverride.index_count, pSubsetOverride.ib_format, 0);
        } else {
//            pDC->IASetPrimitiveTopology( D3D11_PRIMITIVE_TOPOLOGY_TRIANGLELIST );
//            pDC->IASetIndexBuffer( mesh.GetIB11(0), mesh.GetIBFormat11(0), 0 );
            gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, mesh.getIB10(0));

//            pTechnique->GetPassByIndex(0)->Apply(0, pDC);
            pTechnique.enable();
            if(wireframe)
                m_pWireframeOverrideTechnique->GetPassByIndex(0)->Apply(0,pDC);
            for (int subset = 0; subset < mesh.getNumSubsets(0); ++subset)
            {
                SDKMeshSubset pSubset = mesh.getSubset( 0, subset );

                if (depthOnly == false) // TODO: Currently don't support alpha-tested materials
                {
                    SDKmeshMaterial  pMat = mesh.getMaterial(pSubset.materialID);

                    m_pTexDiffuseVariable.SetResource(pMat.pDiffuseRV11);// ? pMat->pDiffuseRV11 : m_pWhiteTextureSRV);

                    Vector4f diffuse = pMat.diffuse;
                    float diffuseX = (float)Math.pow(diffuse.x,m_DiffuseGamma);//*1.9f;
                    float diffuseY = (float)Math.pow(diffuse.y,m_DiffuseGamma);//*1.9f;
                    float diffuseZ = (float)Math.pow(diffuse.z,m_DiffuseGamma);//*1.9f;
                    // de-saturating the diffuse color a bit
//                    D3DXVECTOR4  LuminanceWeights = D3DXVECTOR4(0.299f,0.587f,0.114f, 0.0f);
                    float Luminance = //D3DXVec4Dot(&diffuse,&LuminanceWeights);
                            diffuseX * 0.299f + diffuseY * 0.587f + diffuseZ * 0.114f;
//                    D3DXVECTOR4 LuminanceVec = D3DXVECTOR4(Luminance,Luminance,Luminance,1.0f);
//                    D3DXVec4Lerp(&diffuse,&diffuse,&LuminanceVec,0.7f);
                    diffuseX = Numeric.mix(diffuseX, Luminance, 0.7f);
                    diffuseY = Numeric.mix(diffuseY, Luminance, 0.7f);
                    diffuseZ = Numeric.mix(diffuseZ, Luminance, 0.7f);

                    m_pDiffuseColorVariable->SetFloatVector((FLOAT*)&diffuse);

                    // HACK to render the hull with no backface culling but the rest with backface
                    if(pSubset.materialID != 0)
                        pTechnique->GetPassByIndex(0)->Apply(0, pDC);
				    else
                        pTechnique->GetPassByIndex(1)->Apply(0, pDC);

                    if(wireframe)
                        m_pWireframeOverrideTechnique->GetPassByIndex(0)->Apply(0,pDC);
                }

//                pDC->DrawIndexed( (UINT)pSubset->IndexCount, (UINT)pSubset->IndexStart, (UINT)pSubset->VertexStart );

                if(pSubset.vertexStart != 0)
                    throw new IllegalStateException();
                gl.glDrawElements(GLenum.GL_TRIANGLES, (int)pSubset.indexCount, mesh.getIBFormat10(0), pSubset.indexStart);
            }
        }

        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0);
        m_pLayout.unbind();
    }

    void updateVesselMotion(/*ID3D11DeviceContext* pDC,*/ GFSDK_WaveWorks_Simulation hSim, float sea_level, float time_delta, float water_scale){
        m_pDynamicState.m_Position.x += time_delta * m_pDynamicState.m_Speed * m_pDynamicState.m_NominalHeading.x;
        m_pDynamicState.m_Position.y += time_delta * m_pDynamicState.m_Speed * m_pDynamicState.m_NominalHeading.y;

	    final double actual_heading_angle = Math.atan2(m_pDynamicState.m_NominalHeading.x, m_pDynamicState.m_NominalHeading.y) + m_pDynamicState.m_Yaw;
        float actual_headingx = (float)Math.sin(actual_heading_angle);
        float actual_headingy = (float)Math.cos(actual_heading_angle);
	    final Vector2f heading_perp = new Vector2f(actual_headingy, -actual_headingx);

        // Use the displacement of our current position for establishing a footprint
        Vector4f[] nominal_displacement = {new Vector4f()};
        GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_GetDisplacements(hSim, new Vector2f[]{m_pDynamicState.m_Position}, nominal_displacement, 1);
        Vector2f UVToWorldRotation = new Vector2f();
        UVToWorldRotation.x = actual_headingy;
        UVToWorldRotation.y = actual_headingx;
        Vector2f worldCentroid = new Vector2f();
        worldCentroid.x = m_pDynamicState.m_Position.x + nominal_displacement[0].x;
        worldCentroid.y = m_pDynamicState.m_Position.y + nominal_displacement[0].y;
        m_pSurfaceHeights.updateHeights(/*pDC,*/hSim,UVToWorldRotation,worldCentroid);

        // Force a sensor update on first update (for subsequent updates, we will re-use the trailing update next frame)
        if(m_bFirstSensorUpdate) {
            if(m_pDynamicState.m_bFirstUpdate) {

                // m_pDynamicState.m_LocalToWorld has yet to be updated so lookup the displacement of the origin and use a reasonable estimate

                Vector2f lookup_coord = new Vector2f();
                lookup_coord.x = m_pDynamicState.m_Position.x;
                lookup_coord.y = m_pDynamicState.m_Position.y;

                m_pSurfaceHeights.getDisplacements(new Vector2f[]{lookup_coord},nominal_displacement,1);

                final float heading = (float)Math.atan2(m_pDynamicState.m_NominalHeading.x, m_pDynamicState.m_NominalHeading.y);
                /*D3DXMATRIX mat_roll;
                D3DXMatrixRotationZ(&mat_roll, m_pDynamicState.m_Roll);

                D3DXMATRIX mat_pitch;
                D3DXMatrixRotationX(&mat_pitch, -m_pDynamicState.m_Pitch);

                D3DXMATRIX mat_heading;
                D3DXMatrixRotationY(&mat_heading, heading + m_pDynamicState.m_Yaw);*/

//                m_pDynamicState.m_LocalToWorld = mat_roll * mat_pitch * mat_heading;
                m_pDynamicState.m_LocalToWorld.setIdentity();
                m_pDynamicState.m_LocalToWorld.rotate(heading + m_pDynamicState.m_Yaw, Vector3f.Y_AXIS);
                m_pDynamicState.m_LocalToWorld.rotate(-m_pDynamicState.m_Pitch, Vector3f.X_AXIS);
                m_pDynamicState.m_LocalToWorld.rotate(m_pDynamicState.m_Roll, Vector3f.Z_AXIS);

                m_pDynamicState.m_LocalToWorld.m30 = m_pDynamicState.m_Position.x + nominal_displacement[0].x;
                m_pDynamicState.m_LocalToWorld.m31 = sea_level + m_pDynamicState.m_Height;
                m_pDynamicState.m_LocalToWorld.m32 = m_pDynamicState.m_Position.y + nominal_displacement[1].y;

                m_pHullSensors.update(m_pSurfaceHeights,m_pDynamicState.m_LocalToWorld);
                m_pDynamicState.m_bFirstUpdate = false;

            } else {

                // m_pDynamicState.m_LocalToWorld is valid
                m_pHullSensors.update(m_pSurfaceHeights,m_pDynamicState.m_LocalToWorld);
            }
            m_bFirstSensorUpdate = false;
        }

        // Caculate the means
        Vector4f mean_displacement = new Vector4f(0.f,0.f,0.f,0.f);
        float mean_displaced_head = 0.f;
        float mean_pitch_moment = 0.f;
        float mean_roll_moment = 0.f;
	    final int num_samples = m_pHullSensors.getNumSensors();
        float immersed_ratio = 0.f;
        {
            final Vector4f[] pDisplacements = m_pHullSensors.getDisplacements();
            final Vector3f[] pWorldSensorPositions = m_pHullSensors.getWorldSensorPositions();
            final Vector3f[] pWorldSensorNormals = m_pHullSensors.getWorldSensorNormals();
            final Vector3f[] pSensorPositions = m_pHullSensors.getSensorPositions();
            for(int sample_ix = 0; sample_ix != num_samples; ++sample_ix)
            {
//                mean_displacement += pDisplacements[sample_ix];
                Vector4f.add(mean_displacement, pDisplacements[sample_ix], mean_displacement);
                // Only immersed areas of the hull contribute to physics
                float water_depth_at_sample = pDisplacements[sample_ix].z - pWorldSensorPositions[sample_ix].z;
                if(water_depth_at_sample > 0.f)
                {
                    float upward_head = water_depth_at_sample * -pWorldSensorNormals[sample_ix].z;	// Assume that non-upward pressure cancel out
                    mean_displaced_head += upward_head;
                    mean_pitch_moment += upward_head * pSensorPositions[sample_ix].z;
                    mean_roll_moment += upward_head * pSensorPositions[sample_ix].x;
                    immersed_ratio += 1.f;
                }
            }
        }
        mean_displacement.scale(water_scale/(float)(num_samples));
        mean_displaced_head *= water_scale/(float)(num_samples);
        mean_pitch_moment *= (water_scale*water_scale)/(float)(num_samples);
        mean_roll_moment *= (water_scale*water_scale)/(float)(num_samples);
        immersed_ratio /= (float)(num_samples);

        // Directly sample displacement at bow and stern
        Vector3f bowPos = new Vector3f();
        bowPos.x = 0.f;
        bowPos.y = 0.f;
        bowPos.z = 0.5f * m_Length;
        Vector3f sternPos =bowPos.negate(new Vector3f());
//        D3DXVec3TransformCoord(&bowPos,&bowPos,&m_pDynamicState.m_LocalToWorld);
//        D3DXVec3TransformCoord(&sternPos,&sternPos,&m_pDynamicState.m_LocalToWorld);
        Matrix4f.transformVector(m_pDynamicState.m_LocalToWorld, bowPos, bowPos);
        Matrix4f.transformVector(m_pDynamicState.m_LocalToWorld, sternPos, sternPos);

        Vector2f[] lookup_coords = {new Vector2f(), new Vector2f()};
        lookup_coords[0].x = bowPos.x;
        lookup_coords[0].y = bowPos.z;
        lookup_coords[1].x = sternPos.x;
        lookup_coords[1].y = sternPos.z;

        Vector2f projected_stern_to_bow = new Vector2f();
        projected_stern_to_bow.x = bowPos.x - sternPos.x;
        projected_stern_to_bow.y = bowPos.y - sternPos.y;
	    final float projected_length = (float) Math.sqrt(projected_stern_to_bow.x*projected_stern_to_bow.x + projected_stern_to_bow.y*projected_stern_to_bow.y);

        Vector4f bow_stern_disps[] = {new Vector4f(), new Vector4f()};
        m_pSurfaceHeights.getDisplacements(lookup_coords,bow_stern_disps,bow_stern_disps.length);

        Vector4f sea_yaw_vector = Vector4f.sub(bow_stern_disps[0], bow_stern_disps[1], bow_stern_disps[1]);
        float sea_yaw_angle = Vector2f.dot(heading_perp,sea_yaw_vector)/projected_length;
        float sea_yaw_rate = (sea_yaw_angle - m_pDynamicState.m_PrevSeaYaw)/time_delta;
        m_pDynamicState.m_PrevSeaYaw = sea_yaw_angle;

        if(m_pDynamicState.m_DynamicsCountdown > 0) {
            // Snap to surface on first few updates
            m_pDynamicState.m_Height = m_InitialHeight;
            m_pDynamicState.m_Pitch = m_InitialPitch;
            m_pDynamicState.m_Roll = 0.f;
            --m_pDynamicState.m_DynamicsCountdown;
        } else {
            // Run pseudo-dynamics
		    final int num_steps = (int)Math.ceil(time_delta/kMaxSimulationTimeStep);
		    final float time_step = time_delta/(float)(num_steps);
            for(int i = 0; i != num_steps; ++i)
            {
                // Height
                final float buoyancy_accel = (m_BuoyantArea * mean_displaced_head * kDensityOfWater * kAccelerationDueToGravity)/m_Mass;
                final float drag_accel = (m_HeightDrag * m_pDynamicState.m_HeightRate * immersed_ratio);
                final float height_accel = (buoyancy_accel - kAccelerationDueToGravity - drag_accel);
                m_pDynamicState.m_HeightRate += height_accel * time_step;
                m_pDynamicState.m_Height += m_pDynamicState.m_HeightRate * time_step;

                // Pitch
                final float pitch_COM_accel = (float) (m_LongitudinalCOM * Math.cos(m_pDynamicState.m_Pitch) * m_Mass * kAccelerationDueToGravity/m_PitchInertia);
                final float pitch_buoyancy_accel = (m_BuoyantArea * mean_pitch_moment * kDensityOfWater * kAccelerationDueToGravity)/m_PitchInertia;
                final float pitch_drag_accel = (m_PitchDrag * m_pDynamicState.m_PitchRate * immersed_ratio);
                final float pitch_accel = (pitch_buoyancy_accel - pitch_drag_accel - pitch_COM_accel);
                m_pDynamicState.m_PitchRate += pitch_accel * time_step;
                m_pDynamicState.m_Pitch += m_pDynamicState.m_PitchRate * time_step;

                // Roll
                final float roll_buoyancy_accel = (m_BuoyantArea * mean_roll_moment * kDensityOfWater * kAccelerationDueToGravity)/m_RollInertia;
                final float roll_righting_accel = 2.f * (float)Math.sin(m_pDynamicState.m_Roll) * m_MetacentricHeight * m_Mass * kAccelerationDueToGravity/m_RollInertia;
                final float roll_drag_accel = (m_RollDrag * m_pDynamicState.m_RollRate * immersed_ratio);
                final float roll_accel = (roll_buoyancy_accel - roll_drag_accel - roll_righting_accel);
                m_pDynamicState.m_RollRate += roll_accel * time_step;
                m_pDynamicState.m_Roll += m_pDynamicState.m_RollRate * time_step;

                // Yaw
                final float yaw_accel = immersed_ratio * (m_YawDrag * (sea_yaw_rate - m_pDynamicState.m_YawRate) + m_YawCoefficient * (sea_yaw_angle - m_pDynamicState.m_Yaw));
                m_pDynamicState.m_YawRate += yaw_accel * time_step;
                m_pDynamicState.m_Yaw += m_pDynamicState.m_YawRate * time_step;

                // Clamp pitch to {-pi/2,pi/2}
                if(m_pDynamicState.m_Pitch > 0.5f * Numeric.PI)
                    m_pDynamicState.m_Pitch = 0.5f * Numeric.PI;
                if(m_pDynamicState.m_Pitch < -0.5f * Numeric.PI)
                    m_pDynamicState.m_Pitch = -0.5f * Numeric.PI;
            }
        }

        final float heading = (float)Math.atan2(m_pDynamicState.m_NominalHeading.x, m_pDynamicState.m_NominalHeading.y);
        /*D3DXMATRIX mat_roll;
        D3DXMatrixRotationZ(&mat_roll, m_pDynamicState.m_Roll);

        D3DXMATRIX mat_pitch;
        D3DXMatrixRotationX(&mat_pitch, -m_pDynamicState.m_Pitch);

        D3DXMATRIX mat_heading;
	    const FLOAT heading = atan2f(m_pDynamicState.m_NominalHeading.x, m_pDynamicState.m_NominalHeading.y);
        D3DXMatrixRotationY(&mat_heading, heading + m_pDynamicState.m_Yaw);
        m_pDynamicState.m_LocalToWorld = mat_roll * mat_pitch * mat_heading;*/

        m_pDynamicState.m_LocalToWorld.setIdentity();
        m_pDynamicState.m_LocalToWorld.rotate(heading + m_pDynamicState.m_Yaw, Vector3f.Y_AXIS);
        m_pDynamicState.m_LocalToWorld.rotate(-m_pDynamicState.m_Pitch, Vector3f.X_AXIS);
        m_pDynamicState.m_LocalToWorld.rotate(m_pDynamicState.m_Roll, Vector3f.Z_AXIS);

        m_pDynamicState.m_LocalToWorld.m30 = m_pDynamicState.m_Position.x + mean_displacement.x;
        m_pDynamicState.m_LocalToWorld.m31 = sea_level + m_pDynamicState.m_Height;
        m_pDynamicState.m_LocalToWorld.m32 = m_pDynamicState.m_Position.y + mean_displacement.y;

        // We want damped movement for camera
        /*D3DXMATRIX mat_local_to_world_damped;
        D3DXMatrixRotationZ(&mat_roll, m_pDynamicState.m_Roll*0.5f);
        D3DXMatrixRotationX(&mat_pitch, -m_pDynamicState.m_Pitch*0.5f);
        D3DXMatrixRotationY(&mat_heading, heading + m_pDynamicState.m_Yaw*0.5f);
        mat_local_to_world_damped = mat_roll * mat_pitch * mat_heading;
        mat_local_to_world_damped._41 = m_pDynamicState.m_Position.x + mean_displacement.x;
        mat_local_to_world_damped._42 = sea_level + m_pDynamicState.m_Height;
        mat_local_to_world_damped._43 = m_pDynamicState.m_Position.y + mean_displacement.y;
        m_pDynamicState.m_CameraToWorld = m_CameraToLocal*mat_local_to_world_damped;*/

        m_pDynamicState.m_CameraToWorld.setIdentity();
        m_pDynamicState.m_CameraToWorld.rotate(heading + m_pDynamicState.m_Yaw*0.5f, Vector3f.Y_AXIS);
        m_pDynamicState.m_CameraToWorld.rotate(-m_pDynamicState.m_Pitch*0.5f, Vector3f.X_AXIS);
        m_pDynamicState.m_CameraToWorld.rotate(m_pDynamicState.m_Roll*0.5f, Vector3f.Z_AXIS);
        m_pDynamicState.m_CameraToWorld.m30 = m_pDynamicState.m_Position.x + mean_displacement.x;
        m_pDynamicState.m_CameraToWorld.m31 = sea_level + m_pDynamicState.m_Height;
        m_pDynamicState.m_CameraToWorld.m32 = m_pDynamicState.m_Position.y + mean_displacement.y;

        Matrix4f.mul(m_pDynamicState.m_CameraToWorld, m_CameraToLocal, m_pDynamicState.m_CameraToWorld);

        // We do not want the wake to yaw around, hence
//        D3DXMatrixRotationY(&m_pDynamicState.m_WakeToWorld, heading + 3.141592f*1.5f);

        m_pDynamicState.m_WakeToWorld.setIdentity();
        m_pDynamicState.m_WakeToWorld.rotate(heading + 3.141592f*1.5f, Vector3f.Y_AXIS);
        m_pDynamicState.m_WakeToWorld.m30 = m_pDynamicState.m_Position.x + mean_displacement.x;
        m_pDynamicState.m_WakeToWorld.m31 = sea_level + m_pDynamicState.m_Height;
        m_pDynamicState.m_WakeToWorld.m32 = m_pDynamicState.m_Position.y + mean_displacement.y;


        // Ensure sensors are bang-up-to-date
        m_pHullSensors.update(m_pSurfaceHeights,m_pDynamicState.m_LocalToWorld);
    }

    void renderVesselToScene(	//ID3D11DeviceContext* pDC,
								Matrix4f matView,
								Matrix4f matProj,
								OceanEnvironment ocean_env,
								OceanVesselSubset pSubsetOverride,
                                boolean  wireframe
    ){
        D3DXMATRIX matLocalToView = m_pDynamicState.m_LocalToWorld * matView;

        // View-proj
        D3DXMATRIX matW = m_MeshToLocal * m_pDynamicState.m_LocalToWorld;
        D3DXMATRIX matWV = m_MeshToLocal * matLocalToView;
        D3DXMATRIX matWVP = matWV * matProj;
        m_pMatWorldViewProjVariable->SetMatrix((FLOAT*)&matWVP);
        m_pMatWorldVariable->SetMatrix((FLOAT*)&matW);
        m_pMatWorldViewVariable->SetMatrix((FLOAT*)&matWV);

        // Global lighting
        m_pLightDirectionVariable->SetFloatVector((FLOAT*)&ocean_env.main_light_direction);
        m_pLightColorVariable->SetFloatVector((FLOAT*)&ocean_env.main_light_color);
        m_pAmbientColorVariable->SetFloatVector((FLOAT*)&ocean_env.sky_color);

        // Spot lights - transform to view space
        D3DXMATRIX matSpotlightsToView = ocean_env.spotlights_to_world_matrix * matView;
        D3DXMATRIX matViewToSpotlights;
        D3DXMatrixInverse(&matViewToSpotlights,NULL,&matSpotlightsToView);
        D3DXVECTOR4 spotlight_position[MaxNumSpotlights];
        D3DXVECTOR4 spotlight_axis_and_cos_angle[MaxNumSpotlights];
        D3DXVECTOR4 spotlight_color[MaxNumSpotlights];
        int lightsNum = 0;

        D3DXVec4TransformArray(spotlight_position,sizeof(spotlight_position[0]),ocean_env.spotlight_position,sizeof(ocean_env.spotlight_position[0]),&matSpotlightsToView,MaxNumSpotlights);
        D3DXVec3TransformNormalArray((D3DXVECTOR3*)spotlight_axis_and_cos_angle,sizeof(spotlight_axis_and_cos_angle[0]),(D3DXVECTOR3*)ocean_env.spotlight_axis_and_cos_angle,sizeof(ocean_env.spotlight_axis_and_cos_angle[0]),&matSpotlightsToView,MaxNumSpotlights);

        for(int i=0; i!=ocean_env.activeLightsNum; ++i) {

            if (ocean_env.lightFilter != -1 && ocean_env.objectID[i] != ocean_env.lightFilter) continue;

            spotlight_position[lightsNum] = spotlight_position[i];
            spotlight_axis_and_cos_angle[lightsNum] = spotlight_axis_and_cos_angle[i];
            spotlight_color[lightsNum] = ocean_env.spotlight_color[i];
            spotlight_axis_and_cos_angle[lightsNum].w = ocean_env.spotlight_axis_and_cos_angle[i].w;

            D3DXMATRIX spotlight_shadow_matrix = matViewToSpotlights * ocean_env.spotlight_shadow_matrix[i];
            m_pSpotlightShadowMatrixVar->SetMatrixArray((float*)&spotlight_shadow_matrix, lightsNum, 1);
            m_pSpotlightShadowResourceVar->SetResourceArray((ID3D11ShaderResourceView**)&ocean_env.spotlight_shadow_resource[i], lightsNum, 1);

            ++lightsNum;
        }

        m_pSpotlightNumVariable->SetInt(lightsNum);
        m_pSpotlightPositionVariable->SetFloatVectorArray((FLOAT*)spotlight_position,0,lightsNum);
        m_pSpotLightAxisAndCosAngleVariable->SetFloatVectorArray((FLOAT*)spotlight_axis_and_cos_angle,0,lightsNum);
        m_pSpotlightColorVariable->SetFloatVectorArray((FLOAT*)spotlight_color,0,lightsNum);

        // Lightnings
        m_pLightningColorVariable->SetFloatVector((FLOAT*)&ocean_env.lightning_light_intensity);
        m_pLightningPositionVariable->SetFloatVector((FLOAT*)&ocean_env.lightning_light_position);

        // Fog
        m_pFogExponentVariable->SetFloat(ocean_env.fog_exponent*ocean_env.cloud_factor);

        renderVessel(pDC, m_pRenderToSceneTechnique, pSubsetOverride, wireframe, false);

        // Release input refs
        ID3D11ShaderResourceView* pNullSRVs[MaxNumSpotlights];
        memset(pNullSRVs,0,sizeof(pNullSRVs));
        m_pSpotlightShadowResourceVar->SetResourceArray(pNullSRVs,0,MaxNumSpotlights);
        m_pRenderToSceneTechnique->GetPassByIndex(0)->Apply(0,pDC);
    }

    void renderReflectedVesselToScene(	//ID3D11DeviceContext* pDC,
										CameraData camera,
										Vector4f world_reflection_plane,
										OceanEnvironment ocean_env
    ){
        D3DXMATRIX matView = *camera.GetViewMatrix();
        D3DXMATRIX matProj = *camera.GetProjMatrix();

        D3DXMATRIX matReflection;
        D3DXMatrixReflect(&matReflection, &world_reflection_plane);

        matView = matReflection*matView;

        renderVesselToScene(pDC, matView, matProj, ocean_env, NULL, false);
    }

    void updateVesselShadows(/*ID3D11DeviceContext* pDC*/){
        D3D11_VIEWPORT original_viewports[D3D11_VIEWPORT_AND_SCISSORRECT_OBJECT_COUNT_PER_PIPELINE];
        UINT num_original_viewports = sizeof(original_viewports)/sizeof(original_viewports[0]);
        pDC->RSGetViewports( &num_original_viewports, original_viewports);

        CD3D11_VIEWPORT viewport(0.0f, 0.0f, (float)kSpotlightShadowResolution, (float)kSpotlightShadowResolution);
        pDC->RSSetViewports(1, &viewport);

        size_t lightsNum = m_Spotlights.size();
        for (size_t i=0; i<lightsNum; ++i)
        {
            if (m_SpotlightsShadows[i].m_Dirty == false)
            {
                continue;
            }

            Spotlight sl = m_Spotlights.get(i);

            D3DXMATRIX matView;
            D3DXMATRIX matProj;

            D3DXVECTOR3 lightPosWorldSpace;
            D3DXVec3TransformCoord(&lightPosWorldSpace, (D3DXVECTOR3*)&sl.position, &m_pDynamicState.m_LocalToWorld);

            D3DXVECTOR3 lightAxisWorldSpace;
            D3DXVec3TransformNormal(&lightAxisWorldSpace, (D3DXVECTOR3*)&sl.axis, &m_pDynamicState.m_LocalToWorld);

            D3DXVECTOR3 lookAt = (D3DXVECTOR3&)sl.position + (D3DXVECTOR3&)sl.axis;
            D3DXVECTOR3 up(1.0f, 0.0, 1.0f);
            D3DXMatrixLookAtLH(&matView, (D3DXVECTOR3*)&sl.position, &lookAt, &up);

            D3DXMatrixPerspectiveFovLH(&matProj, m_Spotlights[i].beam_angle, 1.0f, kSpotlightClipNear, kSpotlightClipFar);

            D3DXMATRIX matW = m_MeshToLocal;
            D3DXMATRIX matWV = matW * matView;
            D3DXMATRIX matWVP = matWV * matProj;
            m_pMatWorldViewProjVariable->SetMatrix((FLOAT*)&matWVP);

            m_SpotlightsShadows[i].m_ViewProjMatrix = matView * matProj;

            pDC->ClearDepthStencilView(m_SpotlightsShadows[i].m_pDSV, D3D11_CLEAR_DEPTH, 1.0f, 0);
            pDC->OMSetRenderTargets(0, NULL, m_SpotlightsShadows[i].m_pDSV);

            renderVessel(pDC, m_pRenderToShadowMapTechnique, NULL, false, true);

            m_SpotlightsShadows[i].m_Dirty = false;
        }

        pDC->RSSetViewports(num_original_viewports, original_viewports);

        pDC->OMSetRenderTargets(0, NULL, NULL);
    }

    void updateVesselLightsInEnv(OceanEnvironment env, Matrix4f matView, float lighting_mult, int objectID){
        final int num_lights_to_update = Math.min(m_Spotlights.size(), MaxNumSpotlights - env.activeLightsNum);
        for(int i = 0; i != num_lights_to_update; ++i) {
            Spotlight sl = m_Spotlights.get(i);
            env.spotlight_position[env.activeLightsNum].set(sl.position,1.f);

		    final float cosHalfBeamAngle = (float) Math.cos(0.5f * sl.beam_angle);
            env.spotlight_axis_and_cos_angle[env.activeLightsNum].set(sl.axis,cosHalfBeamAngle);

            env.spotlight_color[env.activeLightsNum].set(sl.color.x * lighting_mult, sl.color.y*lighting_mult, sl.color.z*lighting_mult,1.f);
//#if ENABLE_SHADOWS
            env.spotlight_shadow_matrix[env.activeLightsNum].load(m_SpotlightsShadows.get(i).m_ViewProjMatrix);
            env.spotlight_shadow_resource[env.activeLightsNum] = m_SpotlightsShadows.get(i).m_pSRV;
//#endif

            env.objectID[env.activeLightsNum] = objectID;
            ++env.activeLightsNum;
        }

        env.spotlights_to_world_matrix.load(m_pDynamicState.m_LocalToWorld);
    }

    void renderVesselToHullProfile(/*ID3D11DeviceContext* pDC,*/ OceanHullProfile profile){
        OceanHullProfile result = new OceanHullProfile(m_pHullProfileSRV[1]);

        // Calculate transforms etc. - we render from below, world-aligned

        // Transform calc - 1/ transform the bounds based on current mesh->world
        /*Vector3f xEdge = new Vector3f(2.f*m_bbExtents.x,0.f,0.f);
        Vector3f yEdge = new Vector3f(0.f,2.f*m_bbExtents.y,0.f);
        Vector3f zEdge = new Vector3f(0.f,0.f,2.f*m_bbExtents.z);
        Vector3f[] bbCorners = new Vector3f[8];
        bbCorners[0] = m_bbCentre - m_bbExtents;
        bbCorners[1] = bbCorners[0] + xEdge;
        bbCorners[2] = bbCorners[0] + yEdge;
        bbCorners[3] = bbCorners[1] + yEdge;
        bbCorners[4] = bbCorners[0] + zEdge;
        bbCorners[5] = bbCorners[1] + zEdge;
        bbCorners[6] = bbCorners[2] + zEdge;
        bbCorners[7] = bbCorners[3] + zEdge;

        D3DXMATRIX matW = m_MeshToLocal * m_pDynamicState->m_LocalToWorld;
        D3DXVec3TransformCoordArray(bbCorners, sizeof(bbCorners[0]), bbCorners, sizeof(bbCorners[0]), &matW, sizeof(bbCorners)/sizeof(bbCorners[0]));

        // Transform calc - 2/ calculate the world bounds
        D3DXVECTOR3 minCorner = bbCorners[0];
        D3DXVECTOR3 maxCorner = minCorner;
        for(int i = 1; i != sizeof(bbCorners)/sizeof(bbCorners[0]); ++i) {
            minCorner = D3DXVec3Min(minCorner,bbCorners[i]);
            maxCorner = D3DXVec3Max(maxCorner,bbCorners[i]);
        }*/

        BoundingBox aabb = new BoundingBox();
        aabb.setFromExtent(m_bbCentre, m_bbExtents);
        Matrix4f matW = Matrix4f.mul(m_pDynamicState.m_LocalToWorld, m_MeshToLocal, null);
        BoundingBox.transform(matW, aabb, aabb);
        Vector3f maxCorner = aabb._max;
        Vector3f minCorner = aabb._min;

        // Transform calc - 3/inflate the world bounds so that the x and y footprints are equal
        float w = maxCorner.x - minCorner.x;
        float l = maxCorner.z - minCorner.z;
        if(w > l) {
            minCorner.z -= 0.5f*(w-l);
            maxCorner.z += 0.5f*(w-l);
            l = w;
        } else {
            minCorner.x -= 0.5f*(l-w);
            maxCorner.x += 0.5f*(l-w);
            w = l;
        }

        // Transform calc - 4/ calculate hull profile transforms
        result.m_ProfileToWorldHeightScale = maxCorner.y - minCorner.y;
        result.m_ProfileToWorldHeightOffset = minCorner.y;
        result.m_WorldToProfileCoordsScale .set(1.f/(maxCorner.x-minCorner.x),1.f/(maxCorner.z-minCorner.z));
        result.m_WorldToProfileCoordsOffset.set(result.m_WorldToProfileCoordsScale.x * -minCorner.x, result.m_WorldToProfileCoordsScale.y * -minCorner.z);
        result.m_TexelSizeInWorldSpace = w/m_HullProfileTextureWH;

        // Transform calc - 5/ set up view-proj for rendering into the hull profile
        Matrix4f matVP = new Matrix4f();
//        memset(matVP,0,sizeof(matVP));
        matVP.m00 = 2.f/(maxCorner.x-minCorner.x);	// x out from x
        matVP.m21 = 2.f/(minCorner.z-maxCorner.z);	// y out from z
        matVP.m12 = 1.f/(maxCorner.y-minCorner.y);	// z out from y
        matVP.m33 = 1.f;
        matVP.m30 =  1.f - matVP.m00 * maxCorner.x;
        matVP.m31 = -1.f - matVP.m21 * maxCorner.z;
        matVP.m32 =  1.f - matVP.m12 * maxCorner.y;

        // Set up matrices for rendering
        D3DXMATRIX matWVP = matW* matVP;
        m_pMatWorldViewProjVariable->SetMatrix((FLOAT*)&matWVP);
        m_pMatWorldVariable->SetMatrix((FLOAT*)&matW);

        // Save rt setup to restore shortly...
        D3D11_VIEWPORT original_viewports[D3D11_VIEWPORT_AND_SCISSORRECT_OBJECT_COUNT_PER_PIPELINE];
        UINT num_original_viewports = sizeof(original_viewports)/sizeof(original_viewports[0]);
        pDC->RSGetViewports( &num_original_viewports, original_viewports);
        ID3D11RenderTargetView* original_rtvs[D3D11_SIMULTANEOUS_RENDER_TARGET_COUNT];
        ID3D11DepthStencilView* original_dsv = NULL;
        UINT num_original_rtvs = sizeof(original_rtvs)/sizeof(original_rtvs[0]);
        pDC->OMGetRenderTargets( num_original_rtvs, original_rtvs, &original_dsv );

        // Do the rendering
        pDC->ClearDepthStencilView(m_pHullProfileDSV, D3D11_CLEAR_DEPTH, 1.f, 0);
	    const FLOAT rtvClearColor[4] = { 1.f, 0.f, 0.f, 0.f };
        pDC->ClearRenderTargetView(m_pHullProfileRTV[0], rtvClearColor);
        pDC->OMSetRenderTargets( 1, &m_pHullProfileRTV[0], m_pHullProfileDSV);

        D3D11_VIEWPORT vp;
        vp.TopLeftX = vp.TopLeftY = 0.f;
        vp.Height = vp.Width = FLOAT(m_HullProfileTextureWH);
        vp.MinDepth = 0.f;
        vp.MaxDepth = 1.f;
        pDC->RSSetViewports(1, &vp);

        renderVessel(pDC, m_pRenderToHullProfileTechnique, NULL, false, false);

        // Fix up cracks
        pDC->OMSetRenderTargets( 1, &m_pHullProfileRTV[1], NULL);
        m_pTexDiffuseVariable->SetResource(m_pHullProfileSRV[0]);
        m_pRenderQuadToCrackFixTechnique->GetPassByIndex(0)->Apply(0, pDC);
        pDC->IASetPrimitiveTopology(D3D11_PRIMITIVE_TOPOLOGY_TRIANGLESTRIP);
        pDC->IASetInputLayout(NULL);
        pDC->Draw(4,0);
        m_pTexDiffuseVariable->SetResource(NULL);
        m_pRenderQuadToCrackFixTechnique->GetPassByIndex(0)->Apply(0, pDC);

        // Restore original state
        pDC->OMSetRenderTargets(num_original_rtvs, original_rtvs, original_dsv);
        pDC->RSSetViewports(num_original_viewports, original_viewports);
        SAFE_RELEASE(original_dsv);
        for(UINT i = 0; i != num_original_rtvs; ++i) {
            SAFE_RELEASE(original_rtvs[i]);
        }

        // Generate mips
        pDC->GenerateMips(m_pHullProfileSRV[1]);

        // Set result
        profile = result;
    }

//    void renderHullProfileInUI(ID3D11DeviceContext* pDC);

    void updateSmokeSimulation(/*ID3D11DeviceContext* pDC,*/CameraData camera, float time_delta,Vector2f wind_dir, float wind_speed, float emit_rate_scale){
        if(m_pSmoke != null)
        {
            Vector3f vWindVector = new Vector3f();
            vWindVector.x = wind_dir.x * wind_speed;
            vWindVector.y = 0.f;
            vWindVector.z = wind_dir.y * wind_speed;
            Matrix4f matEmitter = CacheBuffer.getCachedMatrix();
//            D3DXMATRIX matEmitter = m_FunnelMouthToLocal * m_pDynamicState->m_LocalToWorld;
            Matrix4f.mul(m_pDynamicState.m_LocalToWorld,m_FunnelMouthToLocal, matEmitter);
            m_pSmoke.updateSimulation(/*pDC,*/ camera, time_delta, matEmitter, vWindVector, m_SmokeWindNoiseLevel * wind_speed, emit_rate_scale);
            CacheBuffer.free(matEmitter);
        }
    }

    void renderSmokeToScene(	//ID3D11DeviceContext* pDC,
                                CameraData camera,
                                OceanEnvironment ocean_env
    ){
        if(m_pSmoke != null)
        {
            m_pSmoke.renderToScene(/*pDC,*/ camera, m_pPSM, ocean_env);
        }
    }

    void beginRenderToPSM(/*ID3D11DeviceContext* pDC, const*/ ReadableVector2f wind_dir){
        Matrix4f matEmitter = CacheBuffer.getCachedMatrix();
//        const D3DXMATRIX matEmitter = m_FunnelMouthToLocal * m_pDynamicState->m_LocalToWorld;
        Matrix4f.mul(m_pDynamicState.m_LocalToWorld,m_FunnelMouthToLocal, matEmitter);

        // Get the emitter position from the emitter matrix and set up a wind-aligned local
        // space with the emitter at the origin
        Vector3f emitter_pos = CacheBuffer.getCachedVec3();
//        D3DXVec3TransformCoord(&emitter_pos, &emitter_pos, &matEmitter);
        emitter_pos.set(0,0,0);
        Matrix4f.transformCoord(matEmitter, emitter_pos, emitter_pos);

        // Local y is wind-aligned in the plane
        D3DXVECTOR3 local_y = -D3DXVECTOR3(wind_dir.x,0,wind_dir.y);
        local_y.y = 0.f;
        D3DXVec3Normalize(&local_y, &local_y);

        // Local z is world down (effective light dir - to put shadows on bottom of smoke plume)
        D3DXVECTOR3 local_z = D3DXVECTOR3(0.f,-1.f,0.f);

        // Local x is implied by y and z
        D3DXVECTOR3 local_x;
        D3DXVec3Cross(&local_x,&local_y,&local_z);

        D3DXMATRIX matPSM;
        D3DXMatrixTranslation(&matPSM,emitter_pos.x,emitter_pos.y,emitter_pos.z);

        matPSM._11 = local_x.x;
        matPSM._12 = local_x.y;
        matPSM._13 = local_x.z;

        matPSM._21 = local_y.x;
        matPSM._22 = local_y.y;
        matPSM._23 = local_y.z;

        matPSM._31 = local_z.x;
        matPSM._32 = local_z.y;
        matPSM._33 = local_z.z;


        m_pPSM->beginRenderToPSM(matPSM,pDC);

        CacheBuffer.free(matEmitter);
    }
    void endRenderToPSM(/*ID3D11DeviceContext* pDC*/){
        m_pPSM.endRenderToPSM(/*pDC*/);
    }
    void renderSmokeToPSM(	//ID3D11DeviceContext* pDC,
							OceanEnvironment ocean_env){
        if(m_pSmoke != null)
        {
            m_pSmoke.renderToPSM(/*pDC,*/ m_pPSM, ocean_env);
        }
    }

//    void renderTextureToUI(/*ID3D11DeviceContext* pDC,*/ ID3D11ShaderResourceView* pSRV);

	Matrix4f getCameraXform() { return m_pDynamicState.m_CameraToWorld; }
    Matrix4f getWorldXform() { return m_pDynamicState.m_LocalToWorld; }
    Matrix4f getWakeToWorldXform() { return m_pDynamicState.m_WakeToWorld; }
    Matrix4f getMeshToLocalXform() { return m_MeshToLocal; }

    float getLength() { return m_Length; }

    BoatMesh getMesh() {return m_pMesh;}

    OceanSurfaceHeights getSurfaceHeights() { return m_pSurfaceHeights; }

    OceanPSM getPSM() { return m_pPSM; }

    OceanHullSensors getHullSensors() { return m_pHullSensors; }

    float getYaw() { return m_pDynamicState.m_Yaw; }
    float getYawRate() { return m_pDynamicState.m_YawRate; }
}
