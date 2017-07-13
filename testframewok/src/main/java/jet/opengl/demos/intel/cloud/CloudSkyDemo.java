package jet.opengl.demos.intel.cloud;

import com.nvidia.developer.opengl.app.NvSampleApp;
import com.nvidia.developer.opengl.utils.FieldControl;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.io.IOException;

import jet.opengl.demos.scenes.outdoor.OutDoorScene;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.CascadeShadowMapAttribs;
import jet.opengl.postprocessing.core.PostProcessing;
import jet.opengl.postprocessing.core.PostProcessingFrameAttribs;
import jet.opengl.postprocessing.core.outdoorLighting.OutdoorLightScatteringFrameAttribs;
import jet.opengl.postprocessing.core.outdoorLighting.OutdoorLightScatteringInitAttribs;
import jet.opengl.postprocessing.shader.FullscreenProgram;
import jet.opengl.postprocessing.shader.VisualDepthTextureProgram;
import jet.opengl.postprocessing.texture.AttachType;
import jet.opengl.postprocessing.texture.RenderTargets;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureAttachDesc;
import jet.opengl.postprocessing.texture.TextureUtils;

/**
 * Created by mazhen'gui on 2017/7/11.
 */

public class CloudSkyDemo extends NvSampleApp {
    private OutDoorScene m_Scene;
    private GLFuncProvider gl;
    private FullscreenProgram fullscreenProgram;
    private VisualDepthTextureProgram m_visTexShader;
    private int m_DummyVAO;

    private PostProcessing m_PostProcessing;
    private PostProcessingFrameAttribs m_frameAttribs;
    private OutdoorLightScatteringInitAttribs m_InitAttribs;
    private OutdoorLightScatteringFrameAttribs m_RuntimeAttribs;

    private Texture2D m_pLiSpCloudTransparencyRTVs;
    private Texture2D m_pLiSpCloudMinMaxDepthRTVs;

    private CCloudsController m_pCloudsController;

    private boolean m_bEnableLightScattering;
    private boolean m_bEnableClouds = true;
    private boolean m_bVisualShadownMap;
    private int m_slice = 0;
    private int count = 0;
    private RenderTargets m_RenderTarget;
    private final TextureAttachDesc[] m_AttachDescs = new TextureAttachDesc[2];
    private Texture2D[] m_RenderTexs = new Texture2D[2];
    private Texture2D[] m_pShadowMapDSVs = new Texture2D[4];

    private int m_uiCloudDensityMapResolution = 512;
    private SRenderAttribs m_RenderAttribs = new SRenderAttribs();
    private SGlobalCloudAttribs m_CloudAttribs = new SGlobalCloudAttribs();
    private final Matrix4f m_ViewProj = new Matrix4f();
    private float m_fCloudTime;

    private final Vector3f m_CameraPos = new Vector3f();

    @Override
    public void initUI() {
        mTweakBar.addValue("Visualize Depth", new FieldControl(this, "m_bVisualShadownMap"));
        mTweakBar.addValue("Slice of Depth Texture", new FieldControl(this, "m_slice"), 0, 4);
        mTweakBar.syncValues();
    }

    @Override
    protected void initRendering() {
        getGLContext().setSwapInterval(0);
        m_Scene = new OutDoorScene(this);
        m_Scene.onCreate();
        fullscreenProgram = new FullscreenProgram();
        gl = GLFuncProviderFactory.getGLFuncProvider();
        m_DummyVAO = gl.glGenVertexArray();

        m_PostProcessing = new PostProcessing();
        m_frameAttribs = new PostProcessingFrameAttribs();
        m_frameAttribs.cascadeShadowMapAttribs = new CascadeShadowMapAttribs();
        m_frameAttribs.outputCurrentFrameLog = false;

        m_InitAttribs = new OutdoorLightScatteringInitAttribs();
        m_InitAttribs.m_bEnableEpipolarSampling = true;
        m_InitAttribs.m_bEnableLightShafts = true;
        m_InitAttribs.m_bAutoExposure = true;
        m_InitAttribs.m_bCorrectScatteringAtDepthBreaks = false;
        m_InitAttribs.m_bOptimizeSampleLocations = true;
        m_RuntimeAttribs = new OutdoorLightScatteringFrameAttribs();
        m_RuntimeAttribs.f4ExtraterrestrialSunColor.set(5,5,5,5);

        m_pCloudsController = new CCloudsController();
        m_pCloudsController.OnCreateDevice();

        createCloudDensityMap();
        for(int i = 0; i < 2; i++){
            m_AttachDescs[i] = new TextureAttachDesc();
            m_AttachDescs[i].type = AttachType.TEXTURE_LAYER;
            m_AttachDescs[i].index = i;
            m_AttachDescs[i].level = 0;
        }
    }

    @Override
    protected void reshape(int width, int height) {
        m_Scene.onResize(width, height);

        m_InitAttribs.m_uiBackBufferWidth = width;
        m_InitAttribs.m_uiBackBufferHeight = height;
        m_CloudAttribs.uiDownscaledBackBufferWidth = width;
        m_CloudAttribs.uiDownscaledBackBufferHeight = height;
        m_pCloudsController.OnResize(width, height);
    }

    @Override
    public void display() {
        m_fCloudTime = getFrameDeltaTime();

        if(m_bEnableClouds){
            SGlobalCloudAttribs CloudAttribs = m_pCloudsController.GetCloudAttribs();
            m_Scene.setMaxElevation(CloudAttribs.fCloudAltitude + CloudAttribs.fCloudThickness/2.f);
        }

        if( m_bEnableClouds )
        {
            int m_pcbCameraAttribs = 0;
            int m_pcbLightAttribs = 0;
            int pcMediaScatteringParams = 0;
            Matrix4f.decompseRigidMatrix(m_Scene.getViewMat(), m_RenderAttribs.f3CameraPos, null, null, m_RenderAttribs.f3ViewDir);
            m_RenderAttribs.f3ViewDir.scale(-1);
            m_CloudAttribs.uiNumCascades = 4; //m_TerrainRenderParams.m_iNumShadowCascades;
            m_pCloudsController.Update(m_CloudAttribs, m_RenderAttribs.f3CameraPos, Vector3f.Y_AXIS_NEG, /*mpD3dDevice, mpContext,*/ m_pcbCameraAttribs, m_pcbLightAttribs, pcMediaScatteringParams);
        }
        m_Scene.draw(getFrameDeltaTime());
        renderCloudDensity();

        if(m_bVisualShadownMap){
            gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
            showShadownMap();
            return;
        }

        if( m_bEnableClouds )
        {
//            m_RenderAttribs.pDevice = mpD3dDevice;
//            m_RenderAttribs.pDeviceContext = mpContext;
            Matrix4f.mul(m_Scene.getProjMat(), m_Scene.getViewMat(), m_ViewProj);
            m_RenderAttribs.ViewProjMatr = m_ViewProj;
            m_RenderAttribs.pcbCameraAttribs = 0; //m_pcbCameraAttribs;
            m_RenderAttribs.pcbLightAttribs = 0;//m_pcbLightAttribs;
            m_RenderAttribs.pcMediaScatteringParams = 0;//pcMediaScatteringParams;
            m_RenderAttribs.pPrecomputedNetDensitySRV = null; //pPrecomputedNetDensitySRV;
            m_RenderAttribs.pAmbientSkylightSRV = null;// pAmbientSkyLightSRV;
            m_RenderAttribs.pDepthBufferSRV = m_Scene.getSceneDepth(); // m_pOffscreenDepth->GetDepthResourceView();
            m_RenderAttribs.pLiSpCloudTransparencySRV = m_pLiSpCloudTransparencyRTVs;
            m_RenderAttribs.pLiSpCloudMinMaxDepthSRV = m_pLiSpCloudMinMaxDepthRTVs;
            m_RenderAttribs.fCurrTime = m_fCloudTime;
            m_RenderAttribs.f4DirOnLight = m_Scene.getLightDirection();
            m_RenderAttribs.f4ViewFrustumPlanes = m_Scene.getViewFrustumPlanes();  // Need to check the viewfrustum validation.
//            m_RenderAttribs.f3CameraPos = m_CameraPos;
//            m_RenderAttribs.f3ViewDir = (D3DXVECTOR3&)mpCamera->GetLook();
//            m_RenderAttribs.m_pCameraAttribs = &CameraAttribs;
//            m_RenderAttribs.m_pSMAttribs = &LightAttribs.ShadowAttribs;
            m_pCloudsController.RenderScreenSpaceDensityAndColor( m_RenderAttribs);
        }

        if(m_bEnableLightScattering){
            gl.glClear(GLenum.GL_COLOR_BUFFER_BIT);
            gl.glDisable(GLenum.GL_DEPTH_TEST);
            // Apply the DOF Bokeh and render result to scene_rt2
            m_frameAttribs.sceneColorTexture = m_Scene.getSceneColor();
            m_frameAttribs.sceneDepthTexture = m_Scene.getSceneDepth();
            m_frameAttribs.shadowMapTexture  = m_Scene.getShadowMap();
            m_frameAttribs.elapsedTime       = getFrameDeltaTime();
            m_frameAttribs.cameraNear = m_Scene.getSceneNearPlane();
            m_frameAttribs.cameraFar =  m_Scene.getSceneFarPlane();
            m_frameAttribs.lightDirection    = m_Scene.getLightDirection();
            m_Scene.getCascadeShadowMapInformations(m_frameAttribs.cascadeShadowMapAttribs);
            m_frameAttribs.outputTexture = null;
            m_frameAttribs.viewport.set(0,0, getGLContext().width(), getGLContext().height());
            m_frameAttribs.viewMat = m_Scene.getViewMat();
            m_frameAttribs.projMat = m_Scene.getProjMat();
            m_frameAttribs.fov =     m_Scene.getFovInRadian();

            if( m_bEnableClouds )
            {
//                if( m_frameAttribs.m_uiShaftsFromCloudsMode == SHAFTS_FROM_CLOUDS_SHADOW_MAP )
                {
                    // Merge cloud density into the shadow map to create shafts from clouds
//                    m_RenderAttribs.pDevice = mpD3dDevice;
//                    m_RenderAttribs.pDeviceContext = mpContext;
                    m_RenderAttribs.pLiSpCloudTransparencySRV = m_pLiSpCloudTransparencyRTVs;
                    m_RenderAttribs.pLiSpCloudMinMaxDepthSRV = m_pLiSpCloudMinMaxDepthRTVs;

                    for(int iCscd=m_InitAttribs.m_iFirstCascade; iCscd < 4 /*m_PPAttribs.m_iNumCascades*/; ++iCscd)
                    {
                        if(m_pShadowMapDSVs[iCscd] == null){
                            m_pShadowMapDSVs[iCscd] = TextureUtils.createTextureView(m_Scene.getShadowMap(), GLenum.GL_TEXTURE_2D, 0, 1, iCscd, 1);
                        }
                        m_RenderAttribs.pShadowMapDSV = m_pShadowMapDSVs[iCscd];
                        m_RenderAttribs.iCascadeIndex = iCscd;
                        m_pCloudsController.MergeLiSpDensityWithShadowMap(m_RenderAttribs);
                    }
                }
//                FrameAttribs.ptex2DScrSpaceCloudColorSRV = m_pCloudsController->GetScrSpaceCloudColor();
//                FrameAttribs.ptex2DScrSpaceCloudTransparencySRV = m_pCloudsController->GetScrSpaceCloudTransparency();
//                FrameAttribs.ptex2DScrSpaceCloudMinMaxDistSRV = m_pCloudsController->GetScrSpaceCloudMinMaxDist();
//                FrameAttribs.ptex2DLiSpCloudTransparencySRV = m_pLiSpCloudTransparencySRV;
//                FrameAttribs.ptex2DLiSpCloudMinMaxDepthSRV = m_pLiSpCloudMinMaxDepthSRV;
            }

            m_PostProcessing.addOutdoorLight(m_InitAttribs, m_RuntimeAttribs);
            m_PostProcessing.performancePostProcessing(m_frameAttribs);
        }else {

//        m_Scene.resoveMultisampleTexture();
            gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
            gl.glViewport(0, 0, getGLContext().width(), getGLContext().height());
            gl.glClear(GLenum.GL_COLOR_BUFFER_BIT | GLenum.GL_DEPTH_BUFFER_BIT);
            gl.glDisable(GLenum.GL_DEPTH_TEST);

            fullscreenProgram.enable();
            gl.glBindVertexArray(m_DummyVAO);
//            scene_color_tex2.bind(0);
            gl.glActiveTexture(GLenum.GL_TEXTURE0);
            gl.glBindTexture(m_Scene.getSceneColor().getTarget(), m_Scene.getSceneColor().getTexture());
            gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
            gl.glBindTexture(GLenum.GL_TEXTURE_2D, 0);
            gl.glUseProgram(0);
            gl.glBindVertexArray(0);
        }

        if( m_bEnableClouds && !m_bEnableLightScattering )
        {
            m_pCloudsController.CombineWithBackBuffer( /*mpD3dDevice, mpContext,*/ m_Scene.getSceneDepth(), m_Scene.getSceneColor());
        }

        m_pCloudsController.closeInfo();
        GLCheck.checkError();
    }

    void renderCloudDensity(){
        if( m_bEnableClouds ) {
            if(m_RenderTarget == null)
                m_RenderTarget = new RenderTargets();

            Matrix4f.mul(m_Scene.getProjMat(), m_Scene.getViewMat(), m_RenderAttribs.viewProjInv);
            m_RenderAttribs.viewProjInv.invert();

            m_RenderTarget.bind();
            Matrix4f[] WorldToLightProjSpaceMatrs = m_Scene.getWorldToLightProjMats();
            for (int iCascade = 0; iCascade < 4; ++iCascade) {

//                    NewViewPort.Width = static_cast <float>(m_uiCloudDensityMapResolution);
//                    NewViewPort.Height = static_cast <float>(m_uiCloudDensityMapResolution);
//                    // Set the viewport
//                    pContext -> RSSetViewports(1, & NewViewPort);
//                    ID3D11RenderTargetView * pRTVs[]={
//                    m_pLiSpCloudTransparencyRTVs[iCascade], m_pLiSpCloudMinMaxDepthRTVs[iCascade]};
//                    pContext -> OMSetRenderTargets(_countof(pRTVs), pRTVs, nullptr);

                m_AttachDescs[0].layer = iCascade;
                m_AttachDescs[1].layer = iCascade;
                m_RenderTarget.setRenderTextures(m_RenderTexs, m_AttachDescs);

//                m_RenderAttribs.pDevice = mpD3dDevice;
//                m_RenderAttribs.pDeviceContext = mpContext;
                m_RenderAttribs.ViewProjMatr = WorldToLightProjSpaceMatrs[iCascade];
//                m_RenderAttribs.pcbCameraAttribs = m_pcbCameraAttribs;
//                m_RenderAttribs.pcMediaScatteringParams = pcMediaScatteringParams;
                m_RenderAttribs.pShadowMapDSV = m_Scene.getShadowMap();
                m_RenderAttribs.iCascadeIndex = iCascade;
                m_RenderAttribs.fCurrTime = m_fCloudTime;
                m_RenderAttribs.uiLiSpCloudDensityDim = m_uiCloudDensityMapResolution;
                m_pCloudsController.RenderLightSpaceDensity(m_RenderAttribs);
//                    ID3D11Buffer * pcMediaScatteringParams = m_pLightSctrPP -> GetMediaAttribsCB();
//                    if (bCascadesValid) {
//
//                    }
            }

            m_RenderTarget.unbind();
        }
    }

    void createCloudDensityMap()
    {
        //ShadowMap
        Texture2DDesc LiSpCloudTransparencyMapDesc = new Texture2DDesc
                (
                        m_uiCloudDensityMapResolution,
                        m_uiCloudDensityMapResolution,
                        6,
                        4, //m_TerrainRenderParams.m_iNumShadowCascades,
                        GLenum.GL_R8,
                        1
//                        D3D11_USAGE_DEFAULT,
//                        D3D11_BIND_SHADER_RESOURCE|D3D11_BIND_RENDER_TARGET,
//                        0,
//                        D3D11_RESOURCE_MISC_GENERATE_MIPS
                );

        {
//            CComPtr<ID3D11Texture2D> ptex2DCloudTransparency;
//            V_RETURN(pd3dDevice->CreateTexture2D(&LiSpCloudTransparencyMapDesc, NULL, &ptex2DCloudTransparency));
//            V_RETURN(pd3dDevice->CreateShaderResourceView(ptex2DCloudTransparency, nullptr, &m_pLiSpCloudTransparencySRV));
//
//            D3D11_RENDER_TARGET_VIEW_DESC CloudTransparencyMapRTVDesc;
//            ZeroMemory( &CloudTransparencyMapRTVDesc, sizeof(CloudTransparencyMapRTVDesc) );
//            CloudTransparencyMapRTVDesc.Format = LiSpCloudTransparencyMapDesc.Format;
//            CloudTransparencyMapRTVDesc.ViewDimension = D3D11_RTV_DIMENSION_TEXTURE2DARRAY;
//            CloudTransparencyMapRTVDesc.Texture2DArray.MipSlice = 0;
//            CloudTransparencyMapRTVDesc.Texture2DArray.ArraySize = 1;
//            m_pLiSpCloudTransparencyRTVs.resize(LiSpCloudTransparencyMapDesc.ArraySize);
//            for(UINT iArrSlice=0; iArrSlice < LiSpCloudTransparencyMapDesc.ArraySize; iArrSlice++)
//            {
//                CloudTransparencyMapRTVDesc.Texture2DArray.FirstArraySlice = iArrSlice;
//                V_RETURN(pd3dDevice->CreateRenderTargetView(ptex2DCloudTransparency, &CloudTransparencyMapRTVDesc, &m_pLiSpCloudTransparencyRTVs[iArrSlice]));
//            }

            m_pLiSpCloudTransparencyRTVs = TextureUtils.createTexture2D(LiSpCloudTransparencyMapDesc, null);
        }

        {
            Texture2DDesc LiSpCloudMinMaxDepthDesc = LiSpCloudTransparencyMapDesc;
            LiSpCloudMinMaxDepthDesc.mipLevels = 1;
            LiSpCloudMinMaxDepthDesc.format = GLenum.GL_RG16;
//            CComPtr<ID3D11Texture2D> ptex2DCloudMinMaxDepth;
//            V_RETURN(pd3dDevice->CreateTexture2D(&LiSpCloudMinMaxDepthDesc, NULL, &ptex2DCloudMinMaxDepth));
//            V_RETURN(pd3dDevice->CreateShaderResourceView(ptex2DCloudMinMaxDepth, nullptr, &m_pLiSpCloudMinMaxDepthSRV));
//
//            D3D11_RENDER_TARGET_VIEW_DESC CloudMinMaxDepthRTV;
//            ZeroMemory( &CloudMinMaxDepthRTV, sizeof(CloudMinMaxDepthRTV) );
//            CloudMinMaxDepthRTV.Format = LiSpCloudMinMaxDepthDesc.Format;
//            CloudMinMaxDepthRTV.ViewDimension = D3D11_RTV_DIMENSION_TEXTURE2DARRAY;
//            CloudMinMaxDepthRTV.Texture2DArray.MipSlice = 0;
//            CloudMinMaxDepthRTV.Texture2DArray.ArraySize = 1;
//            m_pLiSpCloudMinMaxDepthRTVs.resize(LiSpCloudMinMaxDepthDesc.ArraySize);
//            for(UINT iArrSlice=0; iArrSlice < LiSpCloudMinMaxDepthDesc.ArraySize; iArrSlice++)
//            {
//                CloudMinMaxDepthRTV.Texture2DArray.FirstArraySlice = iArrSlice;
//                V_RETURN(pd3dDevice->CreateRenderTargetView(ptex2DCloudMinMaxDepth, &CloudMinMaxDepthRTV, &m_pLiSpCloudMinMaxDepthRTVs[iArrSlice]));
//            }
            m_pLiSpCloudMinMaxDepthRTVs = TextureUtils.createTexture2D(LiSpCloudMinMaxDepthDesc, null);
        }

        m_RenderTexs[0] = m_pLiSpCloudTransparencyRTVs;
        m_RenderTexs[1] = m_pLiSpCloudMinMaxDepthRTVs;
        GLCheck.checkError();
    }

    private void showShadownMap() {
        if(m_visTexShader == null)
            try {
                m_visTexShader = new VisualDepthTextureProgram(true);
            } catch (IOException e) {
                e.printStackTrace();
            }

        gl.glViewport(0, 0, getGLContext().width(), getGLContext().height());
        gl.glDisable(GLenum.GL_DEPTH_TEST);
        gl.glDisable(GLenum.GL_CULL_FACE);
        gl.glClearColor(0, 0, 0, 0);
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT);

        m_visTexShader.enable();
        m_visTexShader.setUniforms(m_Scene.getSceneNearPlane(), m_Scene.getSceneFarPlane(), m_slice, 1.0f);
//        m_visTexShader.setSlice(m_slice);
//        m_visTexShader.setLightZFar(m_shadowMapInput.farPlane);
//        m_visTexShader.setLightZNear(m_shadowMapInput.nearPlane);
        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D_ARRAY, m_Scene.getShadowMap().getTexture());
        gl.glTexParameteri(GLenum.GL_TEXTURE_2D_ARRAY, GLenum.GL_TEXTURE_COMPARE_MODE, GLenum.GL_NONE);
        GLCheck.checkError();

        gl.glDisable(GLenum.GL_DEPTH_TEST);
        gl.glDisable(GLenum.GL_CULL_FACE);

        gl.glBindVertexArray(m_DummyVAO);
        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
        gl.glBindVertexArray(0);

        m_visTexShader.disable();
        gl.glBindTexture(GLenum.GL_TEXTURE_2D_ARRAY, 0);
    }

    @Override
    public void onDestroy() {
        m_PostProcessing.dispose();
        m_Scene.onDestroy();
        fullscreenProgram.dispose();
        gl.glDeleteVertexArray(m_DummyVAO);
    }
}
