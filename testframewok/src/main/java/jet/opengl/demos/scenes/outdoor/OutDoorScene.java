package jet.opengl.demos.scenes.outdoor;

import com.nvidia.developer.opengl.app.NvCameraMotionType;
import com.nvidia.developer.opengl.app.NvGLAppContext;
import com.nvidia.developer.opengl.app.NvInputTransformer;
import com.nvidia.developer.opengl.app.NvSampleApp;
import com.nvidia.developer.opengl.utils.ShadowmapGenerateProgram;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.CascadePassMode;
import jet.opengl.postprocessing.core.CascadeShadowMapAttribs;
import jet.opengl.postprocessing.shader.FullscreenProgram;
import jet.opengl.postprocessing.shader.GLSLProgramPipeline;
import jet.opengl.postprocessing.shader.VisualDepthTextureProgram;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.Numeric;
import jet.opengl.render.debug.FrustumeRender;

/**
 * Created by mazhen'gui on 2017/6/1.
 */

public class OutDoorScene {
    private static final int m_uiShadowMapResolution = 1024;

    private static final int EN_STATIC_SCENE_TEST = 0x1;
    private static final int EN_LIGHT_SHATT = 2;
    private static final int EN_AUTO_EXPOSE = 4;
    private static final int EN_EPILOR_FORM = 8;
    private static final int EN_CUSTOM_SDM = 16;  // COSTUME SHADOWMAP

    boolean m_bAnimateSun = true;
    private float m_fScatteringScale = 0.5f;

    //    std::vector< CComPtr<ID3D11DepthStencilView> > m_pShadowMapDSVs;
//    CComPtr<ID3D11ShaderResourceView> m_pShadowMapSRV;
//    private final List<Texture2D> m_pShadowMapDSVs = new ArrayList<>();
    private Texture2D m_pShadowMapSRV;
    private int 	  m_pShadowMapFBO;

    private Texture2D  m_pOffscreenRenderTarget;
    private Texture2D  m_pOffscreenDepth;
    private int 	   m_pOffscreenFBO;
    private Texture2D  mpBackBufferRTV;
    private FullscreenProgram m_QuadProgram;
    private RenderSunProgram m_RenderSunTech;
    private GLSLProgramPipeline m_pipeline;
    private ShadowmapGenerateProgram m_SMProgram;

    final SRenderingParams m_TerrainRenderParams = new SRenderingParams();
    String m_strRawDEMDataFile;
    String m_strMtrlMaskFile;
    String m_strTileTexPaths[] = new String[CEarthHemsiphere.NUM_TILE_TEXTURES];
    String m_strNormalMapTexPaths[]= new String[CEarthHemsiphere.NUM_TILE_TEXTURES];

    CElevationDataSource m_pElevDataSource;

    CEarthHemsiphere m_EarthHemisphere;

    final Matrix4f m_ProjMatrix = new Matrix4f();
    final Matrix4f m_ViewMatrix = new Matrix4f();
    final Matrix4f  m_ViewProj   = new Matrix4f();
    final Vector3f m_CameraPos = new Vector3f();
    final Vector2f m_NearFarPlane = new Vector2f();

    // Temp variable
    final Vector3f vLightSpaceX = new Vector3f();
    final Vector3f vLightSpaceY = new Vector3f();
    final Vector3f vLightSpaceZ = new Vector3f();
    final Vector3f vLightPos = new Vector3f();
    final Vector3f tmpVec3      = new Vector3f();
    final Matrix4f cascadeFrustumProjMatrix = new Matrix4f();
    final Matrix4f[] m_WorldToLightProjMats = new Matrix4f[4];

    final SLightAttribs m_LightAttribs = new SLightAttribs();
    final SAirScatteringAttribs m_ScatteringAttribs = new SAirScatteringAttribs();

    final Vector3f m_LightPos = new Vector3f();
    final Vector3f m_lightAxis = new Vector3f();
    float m_lightAngle;

    //    int m_pcbLightAttribs;
    int m_uiBackBufferWidth, m_uiBackBufferHeight;

    //    CPUTDropdown* m_pSelectPanelDropDowns[3];
    int m_uiSelectedPanelInd;

    float m_fMinElevation, m_fMaxElevation;
    int m_MediaScatteringBuffer;

    private VisualDepthTextureProgram m_visTexShader;
    private int m_DummyVAO;

    private final FrustumeRender.Params m_frustumeParams = new FrustumeRender.Params();
    private FrustumeRender m_frustumeRender;
    private final Matrix4f m_initViewMat = new Matrix4f();
    private boolean m_bFirstLoop = true;
    private int m_iFirstCascade;
    private float m_fCameraNear;
    private float m_fCameraFar;

    private NvGLAppContext nvApp;
    private NvInputTransformer m_transformer;
    private GLFuncProvider gl;

    public OutDoorScene(NvSampleApp app){
        nvApp =app.getGLContext();
        m_transformer = app.getInputTransformer();

        for(int i = 0; i < m_WorldToLightProjMats.length; i++){
            m_WorldToLightProjMats[i] = new Matrix4f();
        }
    }

    public void onCreate() {
        gl = GLFuncProviderFactory.getGLFuncProvider();
        nvApp.setSwapInterval(0);
        m_frustumeRender = new FrustumeRender();
//
        m_transformer.setMotionMode(NvCameraMotionType.FIRST_PERSON);
        m_transformer.setTranslation(-0.000000f, -8000.615234f, 0.000000f);
        m_transformer.setRotationVec(new Vector3f(0.1f, Numeric.PI * 0.7f, 0));
        m_transformer.setMaxTranslationVel(1000);

        m_LightPos.set(10000, 10000, 10000);
        m_lightAxis.set(0.5f, 0.3f, 0.0f);
        m_lightAxis.normalise();

        // Load configs
        Config.parseConfigurationFile(this, false);

        m_pElevDataSource = new CElevationDataSource(m_strRawDEMDataFile) ;
        m_pElevDataSource.setOffsets(m_TerrainRenderParams.m_iColOffset, m_TerrainRenderParams.m_iRowOffset);
        System.out.println("Min Elevation: " + m_pElevDataSource.getGlobalMinElevation());
        System.out.println("Max Elevation: " + m_pElevDataSource.getGlobalMaxElevation());
        m_fMinElevation = m_pElevDataSource.getGlobalMinElevation() * m_TerrainRenderParams.m_TerrainAttribs.m_fElevationScale;
        m_fMaxElevation = m_pElevDataSource.getGlobalMaxElevation() * m_TerrainRenderParams.m_TerrainAttribs.m_fElevationScale;

        System.out.println("m_fMinElevation = " + m_fMinElevation);
        System.out.println("m_fMaxElevation = " + m_fMaxElevation);

        m_EarthHemisphere = new CEarthHemsiphere();
        m_EarthHemisphere.onD3D11CreateDevice(m_pElevDataSource, m_TerrainRenderParams, /*mpD3dDevice, mpContext,*/
                m_strRawDEMDataFile, m_strMtrlMaskFile, m_strTileTexPaths, m_strNormalMapTexPaths,
                getShaderResourcePath() );

        m_QuadProgram = new FullscreenProgram();
        m_pipeline    = new GLSLProgramPipeline();
        m_DummyVAO    = gl.glGenVertexArray();
        m_pShadowMapFBO = gl.glGenFramebuffer();
        GLCheck.checkError();

//        initTestData();
    }

    public void setMaxElevation( float maxElevation){m_fMaxElevation = Math.max(m_fMaxElevation, maxElevation); }

    public String getShaderResourcePath(){return "Scenes/Outdoor/shaders/";}

    public void draw(float dt) {
        update(dt);
        render(dt);
    }

    private void render(float deltaSeconds){
        renderNormalScene(deltaSeconds);
    }

    private void renderNormalScene(float deltaSeconds){
        gl.glClearColor(0.350f,  0.350f,  0.350f, 1.0f);
        gl.glClearDepthf(1.0f);
        gl.glEnable(GLenum.GL_DEPTH_TEST);
        gl.glDepthFunc(GLenum.GL_LESS);
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT | GLenum.GL_DEPTH_BUFFER_BIT);

        Matrix4f.mul(m_ProjMatrix, m_ViewMatrix, m_ViewProj);
        m_LightAttribs.f4DirOnLight.set(0.094241f, 0.104686f, -0.990030f, 0);
        m_LightAttribs.f4DirOnLight.normalise();
        m_LightAttribs.f4ExtraterrestrialSunColor.set(10 * m_fScatteringScale,10 * m_fScatteringScale,10 * m_fScatteringScale,10 * m_fScatteringScale);

        RenderShadowMapOrigin(m_LightAttribs);

//        LightAttribs.ShadowAttribs.bVisualizeCascades = ((CPUTCheckbox*)pGUI->GetControl(ID_SHOW_CASCADES_CHECK))->GetCheckboxState() == CPUT_CHECKBOX_CHECKED;

        m_LightAttribs.shadowAttribs.bVisualizeCascades = false;
        // Calculate location of the sun on the screen
        Vector4f f4LightPosPS = m_LightAttribs.f4LightScreenPos;
        Matrix4f.transform(m_ViewProj, m_LightAttribs.f4DirOnLight, f4LightPosPS);
        f4LightPosPS.x /= f4LightPosPS.w;
        f4LightPosPS.y /= f4LightPosPS.w;
        f4LightPosPS.z /= f4LightPosPS.w;

//        System.out.println(f4LightPosPS);

        float fDistToLightOnScreen = Vector2f.length(f4LightPosPS);
        float fMaxDist = 100;
        if( fDistToLightOnScreen > fMaxDist ){
            float scale = fMaxDist/fDistToLightOnScreen;
            f4LightPosPS.x *= scale;
            f4LightPosPS.y *= scale;
        }

        // Note that in fact the outermost visible screen pixels do not lie exactly on the boundary (+1 or -1), but are biased by
        // 0.5 screen pixel size inwards. Using these adjusted boundaries improves precision and results in
        // smaller number of pixels which require inscattering correction
        m_LightAttribs.bIsLightOnScreen = Math.abs(f4LightPosPS.x) <= 1.f - 1.f/(float)m_uiBackBufferWidth &&
                Math.abs(f4LightPosPS.y) <= 1.f - 1.f/(float)m_uiBackBufferHeight;
//        								  &&Math.abs(f4LightPosPS.z) <= 1.0f;

        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, m_pOffscreenFBO);
        gl.glViewport(0, 0, m_uiBackBufferWidth, m_uiBackBufferHeight);
        gl.glClearColor(0, 0, 0, 0);
        gl.glClearDepthf(1.0f);
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT | GLenum.GL_DEPTH_BUFFER_BIT);

        // Render terrain
//        ID3D11Buffer *pcMediaScatteringParams = m_pLightSctrPP->GetMediaAttribsCB();
//        int pcMediaScatteringParams = m_pLightSctrPP.getMediaAttribsCB();
        Texture2D pAmbientSkyLightSRV = null;//m_ScatteringTable.getAmbientSkyLight();
        Texture2D pPrecomputedNetDensitySRV = null;//m_ScatteringTable.getOccludedNetDensityToAtmTop();

//        m_FrameAttribs.cameraAttribs.fNearPlaneZ = m_shadowMapInput.nearPlane;
//        m_FrameAttribs.cameraAttribs.fFarPlaneZ = m_shadowMapInput.farPlane;
        gl.glDisable(GLenum.GL_CULL_FACE);
        m_EarthHemisphere.render( /*mpContext,*/ m_CameraPos, m_ViewProj, m_LightAttribs, m_ScatteringAttribs,
                m_fCameraNear, m_fCameraFar,
                m_pShadowMapSRV, pPrecomputedNetDensitySRV, pAmbientSkyLightSRV, false);
//        GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);

        renderSun();
        gl.glDisable(GLenum.GL_DEPTH_TEST);
        gl.glDepthMask(true);
        gl.glDepthFunc(GLenum.GL_LESS);
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
    }

    public void renderSun(){
        if(m_LightAttribs.f4LightScreenPos.w <= 0)
            return;

        if(m_RenderSunTech == null){
            m_RenderSunTech = new RenderSunProgram(getShaderResourcePath());
        }

        gl.glDisable(GLenum.GL_CULL_FACE);
        enableDepthCmpEqDS();

        m_RenderSunTech.enable();
        m_RenderSunTech.setUniform(m_ProjMatrix.m00, m_ProjMatrix.m11, m_LightAttribs.f4LightScreenPos.x, m_LightAttribs.f4LightScreenPos.y);
        gl.glDrawArrays(GLenum.GL_TRIANGLE_STRIP, 0, 4);
        m_RenderSunTech.disable();
    }

    void enableDepthCmpEqDS(){
        gl.glEnable(GLenum.GL_DEPTH_TEST);
        gl.glDepthMask(false);
        gl.glDepthFunc(GLenum.GL_EQUAL);
    }

    public Texture2D getSceneColor() { return m_pOffscreenRenderTarget;}
    public Texture2D getSceneDepth() { return m_pOffscreenDepth;}
    public Texture2D getShadowMap()  { return m_pShadowMapSRV;}
    public float     getSceneNearPlane() { return m_fCameraNear;}
    public float     getSceneFarPlane() {  return m_fCameraFar;}
    public Matrix4f  getViewMat()       { return m_ViewMatrix;}
    public Matrix4f  getProjMat()       { return m_ProjMatrix;}
    public float     getFovInRadian()   { return (float)Math.toRadians(45);}
    public Matrix4f[] getWorldToLightProjMats() { return m_WorldToLightProjMats;}
    public Vector4f[] getViewFrustumPlanes(){ return m_EarthHemisphere.m_viewFrustum.array;}
    public Vector3f  getLightDirection(){
        Vector3f dir = new Vector3f(m_LightAttribs.f4DirOnLight);
//        dir.scale(-1);
        return dir;
    }


    public boolean handleCharacterInput(char c) {
        switch (c) {
            case 'p':
            case 'P':
                System.out.println("lightPosition: " + m_LightAttribs.f4LightScreenPos);
                System.out.println("CameraPosition: " + m_CameraPos);
                System.out.println("(Near, Far) = (" + m_fCameraNear + ", " + m_fCameraFar + ")");
                m_bAnimateSun = !m_bAnimateSun;
                return true;
            default:
                return false;
        }
    }

    public void getCascadeShadowMapInformations(CascadeShadowMapAttribs attribs) {
        attribs.mode = CascadePassMode.SINGLE;
        attribs.numCascades = 4;
        attribs.worldToLightView = m_LightAttribs.shadowAttribs.mWorldToLightViewT;

        final Matrix4f[] worldToLgihtView = m_LightAttribs.shadowAttribs.mWorldToShadowMapUVDepthT;
        for(int i = 0; i < attribs.numCascades; i++){
            attribs.worldToShadowMapUVDepth[i].load(worldToLgihtView[i]);
            attribs.startEndZ[i].set(m_LightAttribs.shadowAttribs.cascades[i].f4StartEndZ);
        }
    }

    private boolean print_log;
    private void RenderShadowMapOrigin(SLightAttribs m_LightAttribs2) {
        if(m_SMProgram == null)
            m_SMProgram = new ShadowmapGenerateProgram();
        float m_fCascadePartitioningFactor = 0.95f;
        // Declare a world to light space transformation matrix
        // Initialize to an identity matrix
        final Matrix4f worldToLgihtView = m_LightAttribs2.shadowAttribs.mWorldToLightViewT;
        Matrix4f.lookAt(0, 0, 0, -m_LightAttribs2.f4DirOnLight.x, -m_LightAttribs2.f4DirOnLight.y, -m_LightAttribs2.f4DirOnLight.z,
                0, 1, 0, worldToLgihtView);

        float f = Numeric.EPSILON;
        Vector3f f3CameraPosInLightSpace = Matrix4f.transformCoord(worldToLgihtView, m_CameraPos, tmpVec3); // TODO Camera Position?
        final float fMainCamNearPlane = m_NearFarPlane.y;
        final float fMainCamFarPlane = m_NearFarPlane.x;

        for(int i=0; i < 8; ++i)
            m_LightAttribs2.shadowAttribs.fCascadeCamSpaceZEnd[i] = +Float.MAX_VALUE;

        if(!print_log){
            System.err.println("NumShadowCascade = " + m_TerrainRenderParams.m_iNumShadowCascades);
            System.err.println("fMainCamNearPlane = " + fMainCamNearPlane);
            System.err.println("fMainCamFarPlane = " + fMainCamFarPlane);
        }

        m_SMProgram.enable();
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, m_pShadowMapFBO);
        gl.glColorMask(false, false, false, false);
        // Render cascades
        for(int iCascade = 0; iCascade < m_TerrainRenderParams.m_iNumShadowCascades; ++iCascade){
            SCascadeAttribs currCascade = m_LightAttribs2.shadowAttribs.cascades[iCascade];

//      	float fCascadeNearZ = ShadowMapAttribs.fCascadeCamSpaceZEnd[iCascade];
            float fCascadeFarZ = (iCascade == 0) ? fMainCamFarPlane : m_LightAttribs2.shadowAttribs.fCascadeCamSpaceZEnd[iCascade-1];
            float fCascadeNearZ = fMainCamNearPlane;

            if (iCascade < m_TerrainRenderParams.m_iNumShadowCascades-1)
            {
                float ratio = fMainCamNearPlane/fMainCamFarPlane;
                float power = (float)(iCascade+1) / (float)m_TerrainRenderParams.m_iNumShadowCascades;
                float logZ = (float) (fMainCamFarPlane * Math.pow(ratio, power));

                float range = fMainCamNearPlane - fMainCamFarPlane;
                float uniformZ = fMainCamFarPlane + range * power;

                fCascadeNearZ = m_fCascadePartitioningFactor * (logZ - uniformZ) + uniformZ;
                m_LightAttribs2.shadowAttribs.fCascadeCamSpaceZEnd[iCascade] = fCascadeNearZ;
            }

            float fMaxLightShaftsDist = 3e+5f;
            currCascade.f4StartEndZ.x = (iCascade == m_iFirstCascade) ? 0 : Math.min(fCascadeFarZ, fMaxLightShaftsDist);
            currCascade.f4StartEndZ.y = Math.min(fCascadeNearZ, fMaxLightShaftsDist);
            Matrix4f.perspective(45, (float)nvApp.width()/nvApp.height(), fCascadeFarZ, fCascadeNearZ, cascadeFrustumProjMatrix);
            if(!print_log){
                System.err.println("iCascade: " + iCascade + ", fCascadeNearZ = " + fCascadeNearZ + ", fCascadeFarZ = " + fCascadeFarZ);
                System.err.println("currCascade.f4StartEndZ: " + currCascade.f4StartEndZ.x + ", " + currCascade.f4StartEndZ.y);
            }

//          D3DXMATRIX CascadeFrustumViewProjMatr = m_CameraViewMatrix * CascadeFrustumProjMatrix;
//          D3DXMATRIX CascadeFrustumProjSpaceToWorldSpace;
//          D3DXMatrixInverse(&CascadeFrustumProjSpaceToWorldSpace, nullptr, &CascadeFrustumViewProjMatr);
//          D3DXMATRIX CascadeFrustumProjSpaceToLightSpace = CascadeFrustumProjSpaceToWorldSpace * WorldToLightViewSpaceMatr;
            Matrix4f CascadeFrustumViewProjMatr = Matrix4f.mul(cascadeFrustumProjMatrix, m_ViewMatrix, cascadeFrustumProjMatrix);
            Matrix4f CascadeFrustumProjSpaceToWorldSpace = Matrix4f.invert(CascadeFrustumViewProjMatr, CascadeFrustumViewProjMatr);
            Matrix4f CascadeFrustumProjSpaceToLightSpace = Matrix4f.mul(worldToLgihtView, CascadeFrustumProjSpaceToWorldSpace, CascadeFrustumProjSpaceToWorldSpace);

            // Set reference minimums and maximums for each coordinate
//          D3DXVECTOR3 f3MinXYZ(f3CameraPosInLightSpace), f3MaxXYZ(f3CameraPosInLightSpace);
            float f3MinXYZ_x = f3CameraPosInLightSpace.x, f3MaxXYZ_x = f3CameraPosInLightSpace.x;
            float f3MinXYZ_y = f3CameraPosInLightSpace.y, f3MaxXYZ_y = f3CameraPosInLightSpace.y;
            float f3MinXYZ_z = f3CameraPosInLightSpace.z, f3MaxXYZ_z = f3CameraPosInLightSpace.z;

            // First cascade used for ray marching must contain camera within it
            if( iCascade != m_iFirstCascade )
            {
//              f3MinXYZ = D3DXVECTOR3(+FLT_MAX, +FLT_MAX, +FLT_MAX);
//              f3MaxXYZ = D3DXVECTOR3(-FLT_MAX, -FLT_MAX, -FLT_MAX);
                f3MinXYZ_x = f3MinXYZ_y = f3MinXYZ_z = Float.MAX_VALUE;
                f3MaxXYZ_x = f3MaxXYZ_y = f3MaxXYZ_z = -Float.MAX_VALUE;
            }

            for(int iClipPlaneCorner=0; iClipPlaneCorner < 8; ++iClipPlaneCorner)
            {
                Vector3f f3PlaneCornerProjSpace = tmpVec3;
                f3PlaneCornerProjSpace.set         ((iClipPlaneCorner & 0x01)!=0 ? +1.f : - 1.f,
                        (iClipPlaneCorner & 0x02)!=0 ? +1.f : - 1.f,
                        // Since we use complimentary depth buffering,
                        // far plane has depth 0
                        (iClipPlaneCorner & 0x04)!=0 ? 1.f : -1.f);
//              D3DXVECTOR3 f3PlaneCornerLightSpace;
//              D3DXVec3TransformCoord(&f3PlaneCornerLightSpace, &f3PlaneCornerProjSpace, &CascadeFrustumProjSpaceToLightSpace);
//              D3DXVec3Minimize(&f3MinXYZ, &f3MinXYZ, &f3PlaneCornerLightSpace);
//              D3DXVec3Maximize(&f3MaxXYZ, &f3MaxXYZ, &f3PlaneCornerLightSpace);
                Vector3f f3PlaneCornerLightSpace = Matrix4f.transformCoord(CascadeFrustumProjSpaceToLightSpace, f3PlaneCornerProjSpace, tmpVec3);
//              f3PlaneCornerLightSpace.z = -f3PlaneCornerLightSpace.z;

                f3MinXYZ_x = Math.min(f3MinXYZ_x, f3PlaneCornerLightSpace.x);
                f3MinXYZ_y = Math.min(f3MinXYZ_y, f3PlaneCornerLightSpace.y);
                f3MinXYZ_z = Math.min(f3MinXYZ_z, f3PlaneCornerLightSpace.z);

                f3MaxXYZ_x = Math.max(f3MaxXYZ_x, f3PlaneCornerLightSpace.x);
                f3MaxXYZ_y = Math.max(f3MaxXYZ_y, f3PlaneCornerLightSpace.y);
                f3MaxXYZ_z = Math.max(f3MaxXYZ_z, f3PlaneCornerLightSpace.z);
            }

            // It is necessary to ensure that shadow-casting patches, which are not visible
            // in the frustum, are still rendered into the shadow map
            f3MaxXYZ_z += 6360000.f * Math.sqrt(2.f);
//          f3MinXYZ_z -= 6360000.f * Math.sqrt(2.f);

            // Align cascade extent to the closest power of two
            float fShadowMapDim = (float)m_uiShadowMapResolution;
            float fCascadeXExt = (f3MaxXYZ_x - f3MinXYZ_x) * (1 + 1.f/fShadowMapDim);
            float fCascadeYExt = (f3MaxXYZ_y - f3MinXYZ_y) * (1 + 1.f/fShadowMapDim);
            final float fExtStep = 2.f;
            fCascadeXExt = (float) Math.pow( fExtStep, Math.ceil( Math.log(fCascadeXExt)/Math.log(fExtStep) ) );
            fCascadeYExt = (float) Math.pow( fExtStep, Math.ceil( Math.log(fCascadeYExt)/Math.log(fExtStep) ) );
            // Align cascade center with the shadow map texels to alleviate temporal aliasing
            float fCascadeXCenter = (f3MaxXYZ_x + f3MinXYZ_x)/2.f;
            float fCascadeYCenter = (f3MaxXYZ_y + f3MinXYZ_y)/2.f;
            float fTexelXSize = fCascadeXExt / fShadowMapDim;
            float fTexelYSize = fCascadeXExt / fShadowMapDim;
            fCascadeXCenter = (float) (Math.floor(fCascadeXCenter/fTexelXSize) * fTexelXSize);
            fCascadeYCenter = (float) (Math.floor(fCascadeYCenter/fTexelYSize) * fTexelYSize);
            // Compute new cascade min/max xy coords
            f3MaxXYZ_x = fCascadeXCenter + fCascadeXExt/2.f;
            f3MinXYZ_x = fCascadeXCenter - fCascadeXExt/2.f;
            f3MaxXYZ_y = fCascadeYCenter + fCascadeYExt/2.f;
            f3MinXYZ_y = fCascadeYCenter - fCascadeYExt/2.f;

//          if(f3MinXYZ_z < 0){
//        	  f3MinXYZ_z = -f3MinXYZ_z;
//          }else{
//        	  f3MinXYZ_z = 0;
//          }
//
//          if(f3MaxXYZ_z < 0){
//        	  f3MaxXYZ_z = -f3MaxXYZ_z;
//          }else{
//        	  f3MaxXYZ_z = 0;
//          }

//          f3MinXYZ_z = Math.abs(Math.min(0, f3MinXYZ_z));
//          f3MaxXYZ_z = Math.abs(Math.min(0, f3MaxXYZ_z));
//          System.err.println("f3MinXYZ = (" + f3MinXYZ_x + ", " + f3MinXYZ_y + ", " + f3MinXYZ_z + ")");
//        	System.err.println("f3MaxXYZ = (" + f3MaxXYZ_x + ", " + f3MaxXYZ_y + ", " + f3MaxXYZ_z + ")");

            currCascade.f4LightSpaceScale.x =  2.f / (f3MaxXYZ_x - f3MinXYZ_x);
            currCascade.f4LightSpaceScale.y =  2.f / (f3MaxXYZ_y - f3MinXYZ_y);
            currCascade.f4LightSpaceScale.z =  2.f / (f3MaxXYZ_z - f3MinXYZ_z);  //-1.f / (f3MaxXYZ_z - f3MinXYZ_z);
            // Apply bias to shift the extent to [-1,1]x[-1,1]x[1,0]
            currCascade.f4LightSpaceScaledBias.x = -f3MinXYZ_x * currCascade.f4LightSpaceScale.x - 1.f;
            currCascade.f4LightSpaceScaledBias.y = -f3MinXYZ_y * currCascade.f4LightSpaceScale.y - 1.f;
            currCascade.f4LightSpaceScaledBias.z = -f3MinXYZ_z * currCascade.f4LightSpaceScale.z - 1.f;     //+ 0.f;

//          D3DXMATRIX ScaleMatrix;
//          D3DXMatrixScaling(&ScaleMatrix, CurrCascade.f4LightSpaceScale.x, CurrCascade.f4LightSpaceScale.y, CurrCascade.f4LightSpaceScale.z);
//          D3DXMATRIX ScaledBiasMatrix;
//          D3DXMatrixTranslation(&ScaledBiasMatrix, CurrCascade.f4LightSpaceScaledBias.x, CurrCascade.f4LightSpaceScaledBias.y, CurrCascade.f4LightSpaceScaledBias.z);
//
//          // Note: bias is applied after scaling!
//          D3DXMATRIX CascadeProjMatr = ScaleMatrix * ScaledBiasMatrix;
            Matrix4f CascadeProjMatr = cascadeFrustumProjMatrix;
            CascadeProjMatr.set
                    (
                            currCascade.f4LightSpaceScale.x, 0, 0, 0,
                            0, currCascade.f4LightSpaceScale.y, 0, 0,
                            0, 0, currCascade.f4LightSpaceScale.z, 0,
                            currCascade.f4LightSpaceScaledBias.x,currCascade.f4LightSpaceScaledBias.y,currCascade.f4LightSpaceScaledBias.z,1
                    );
//          Matrix4f.ortho(f3MinXYZ_x, f3MaxXYZ_x, f3MinXYZ_y, f3MaxXYZ_y, f3MinXYZ_z, f3MaxXYZ_z, CascadeProjMatr);
            Matrix4f.ortho(f3MinXYZ_x, f3MaxXYZ_x, f3MinXYZ_y, f3MaxXYZ_y, -f3MaxXYZ_z, -f3MinXYZ_z, CascadeProjMatr);
            if(!print_log){
                System.err.println("f3MinXYZ = (" + f3MinXYZ_x + ", " + f3MinXYZ_y + ", " + f3MinXYZ_z + ")");
                System.err.println("f3MaxXYZ = (" + f3MaxXYZ_x + ", " + f3MaxXYZ_y + ", " + f3MaxXYZ_z + ")");
            }

//          Matrix4f.ortho(f3MinXYZ_x, f3MaxXYZ_x, f3MinXYZ_y, f3MaxXYZ_y, 0, Math.abs(f3MaxXYZ_z), CascadeProjMatr);

            // Adjust the world to light space transformation matrix
//          D3DXMATRIX WorldToLightProjSpaceMatr = WorldToLightViewSpaceMatr * CascadeProjMatr;
//          D3DXMATRIX ProjToUVScale, ProjToUVBias;
//          D3DXMatrixScaling( &ProjToUVScale, 0.5f, -0.5f, 1.f);
//          D3DXMatrixTranslation( &ProjToUVBias, 0.5f, 0.5f, 0.f);
//          D3DXMATRIX WorldToShadowMapUVDepthMatr = WorldToLightProjSpaceMatr * ProjToUVScale * ProjToUVBias;
//          D3DXMatrixTranspose( &ShadowMapAttribs.mWorldToShadowMapUVDepthT[iCascade], &WorldToShadowMapUVDepthMatr );
            Matrix4f WorldToLightProjSpaceMatr = Matrix4f.mul(CascadeProjMatr, worldToLgihtView, CascadeProjMatr);
            Matrix4f ScaledBiasMat = m_LightAttribs2.shadowAttribs.mWorldToShadowMapUVDepthT[iCascade];
            ScaledBiasMat.set
                    (
                            0.5f, 0, 0, 0,
                            0, 0.5f, 0, 0,
                            0, 0, 0.5f, 0,
                            0.5f,0.5f,0.5f,1
                    );
            Matrix4f.mul(ScaledBiasMat, WorldToLightProjSpaceMatr, ScaledBiasMat);

            gl.glViewport(0, 0, m_uiShadowMapResolution, m_uiShadowMapResolution);
//          rtManager.setTexture2DRenderTargets(0, m_pShadowMapDSVs.get(iCascade).getTexture());
//          GLError.checkError();
//          rtManager.clearDepthTarget(1.0f);
            gl.glFramebufferTextureLayer(GLenum.GL_FRAMEBUFFER, GLenum.GL_DEPTH_ATTACHMENT, m_pShadowMapSRV.getTexture(), 0, iCascade);
            gl.glDrawBuffers(GLenum.GL_NONE);
            gl.glClearBufferfv(GLenum.GL_DEPTH, 0, CacheBuffer.wrap(1f));
            m_SMProgram.applyMVPMat(WorldToLightProjSpaceMatr);
            // Render terrain to shadow map
//          m_EarthHemisphere.Render(mpContext, m_CameraPos, WorldToLightProjSpaceMatr, nullptr, nullptr, nullptr, nullptr, nullptr, true);
//          m_EarthHemisphere.render(m_CameraPos, WorldToLightProjSpaceMatr, 0, 0, null, null, null, true);
            m_EarthHemisphere.drawMesh(WorldToLightProjSpaceMatr);
            m_WorldToLightProjMats[iCascade].load(WorldToLightProjSpaceMatr);
        }

        gl.glUseProgram(0);
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
        gl.glViewport(0, 0, nvApp.width(), nvApp.height());
        gl.glColorMask(true, true, true, true);

        print_log = true;
    }

    static float getRaySphereIntersection(ReadableVector3f rayOrigin,
                                          Vector3f f3RayDirection, Vector3f f3SphereCenter,
                                          float fSphereRadius){
        // http://wiki.cgsociety.org/index.php/Ray_Sphere_Intersection
//	    f3RayOrigin -= f3SphereCenter;
        Vector3f f3RayOrigin = Vector3f.sub(rayOrigin, f3SphereCenter, null);
        float A = Vector3f.dot(f3RayDirection, f3RayDirection);
        float B = 2 * Vector3f.dot(f3RayOrigin, f3RayDirection);
        float C = Vector3f.dot(f3RayOrigin, f3RayOrigin) - fSphereRadius*fSphereRadius;
        float D = B*B - 4*A*C;
        // If discriminant is negative, there are no real roots hence the ray misses the
        // sphere
        if( D<0 )
        {
            return -1;
        }
        else
        {
            D = (float) Math.sqrt(D);
//	        f2Intersections = D3DXVECTOR2(-B - D, -B + D) / (2*A); // A must be positive here!!
            float x = (-B - D)/(2 * A);
            float y = (-B + D)/(2 * A);

            return x > 0.0f ? x : y;
        }
    }

    void ComputeApproximateNearFarPlaneDist(ReadableVector3f CameraPos,
                                            Matrix4f ViewMatr,
                                            Matrix4f ProjMatr,
                                            Vector3f EarthCenter,
                                            float fEarthRadius,
                                            float fMinRadius,
                                            float fMaxRadius,
                                            Vector2f nearFarPlane){
//    	D3DXMATRIX ViewProjMatr = ViewMatr * ProjMatr;
//        D3DXMATRIX ViewProjInv;
//        D3DXMatrixInverse(&ViewProjInv, nullptr, &ViewProjMatr);
        Matrix4f ViewProjMatr = Matrix4f.mul(ProjMatr, ViewMatr, cascadeFrustumProjMatrix);
        Matrix4f ViewProjInv = Matrix4f.invert(ViewProjMatr, ViewProjMatr);

        // Compute maximum view distance for the current camera altitude
//        D3DXVECTOR3 f3CameraGlobalPos = CameraPos - EarthCenter;
        Vector3f f3CameraGlobalPos = Vector3f.sub(CameraPos, EarthCenter, vLightSpaceX);

        float fCameraElevationSqr = Vector3f.dot(f3CameraGlobalPos, f3CameraGlobalPos);
        float fMaxViewDistance = (float) (Math.sqrt(fCameraElevationSqr -  fEarthRadius*fEarthRadius ) +
                Math.sqrt(fMaxRadius*fMaxRadius -fEarthRadius*fEarthRadius ));
        float fCameraElev = (float) Math.sqrt(fCameraElevationSqr);

        float fNearPlaneZ = 50.f;
        if( fCameraElev > fMaxRadius )
        {
            // Adjust near clipping plane
            fNearPlaneZ = (float) ((fCameraElev - fMaxRadius) / Math.sqrt( 1 + 1.f/(ProjMatr.m00*ProjMatr.m00) + 1.f/(ProjMatr.m11*ProjMatr.m11) ));
        }

        fNearPlaneZ = Math.max(fNearPlaneZ, 50);
        float fFarPlaneZ = 1000;

        final Vector3f PosPS = vLightSpaceX;
        final Vector3f PosWS = vLightSpaceY;
        final Vector3f DirFromCamera = vLightSpaceZ;
        final int iNumTestDirections = 5;
        for(int i=0; i<iNumTestDirections; ++i)
            for(int j=0; j<iNumTestDirections; ++j)
            {
//                D3DXVECTOR3 PosPS, PosWS, DirFromCamera;
                PosPS.x = (float)i / (float)(iNumTestDirections-1) * 2.f - 1.f;
                PosPS.y = (float)j / (float)(iNumTestDirections-1) * 2.f - 1.f;
                PosPS.z = 1; // Far plane is at 0 in complimentary depth buffer
//                D3DXVec3TransformCoord(&PosWS, &PosPS, &ViewProjInv);
                Matrix4f.transformCoord(ViewProjInv, PosPS, PosWS);

//                DirFromCamera = PosWS - CameraPos;
//                D3DXVec3Normalize(&DirFromCamera, &DirFromCamera);
                Vector3f.sub(PosWS, CameraPos, DirFromCamera);
                DirFromCamera.normalise();

//                getRaySphereIntersection(CameraPos, DirFromCamera, EarthCenter, fMinRadius, IsecsWithBottomBoundSphere);
//                float fNearIsecWithBottomSphere = IsecsWithBottomBoundSphere.x > 0 ? IsecsWithBottomBoundSphere.x : IsecsWithBottomBoundSphere.y;
                float fNearIsecWithBottomSphere = getRaySphereIntersection(CameraPos, DirFromCamera, EarthCenter, fMinRadius);
                if( fNearIsecWithBottomSphere > 0 )
                {
                    // The ray hits the Earth. Use hit point to compute camera space Z
                    Vector3f HitPointWS = Vector3f.linear(CameraPos, DirFromCamera, fNearIsecWithBottomSphere, vLightSpaceX);
                    Vector3f HitPointCamSpace = Matrix4f.transformVector(ViewMatr, HitPointWS, vLightSpaceY);
                    fFarPlaneZ = Math.max(fFarPlaneZ, Math.abs(HitPointCamSpace.z));
                }
                else
                {
                    // The ray misses the Earth. In that case the whole earth could be seen
                    fFarPlaneZ = fMaxViewDistance;
                }
            }

        nearFarPlane.set(fNearPlaneZ, fFarPlaneZ);
    }

    private float m_totalTime;
    private void update(float deltaSeconds){
        m_bAnimateSun = true;
        if( m_bAnimateSun )
        {
//            auto &LightOrientationMatrix = m_pDirLightOrienationCamera.getParentMatrix();
//            float3 RotationAxis( 0.5f, 0.3f, 0.0f );
//            float3 LightDir = m_pDirLightOrienationCamera->GetLook() * -1;
//            float fRotationScaler = ( LightDir.y > +0.2f ) ? 50.f : 1.f;
//            float4x4 RotationMatrix = float4x4RotationAxis(RotationAxis, 0.02f * (float)deltaSeconds * fRotationScaler);
//            LightOrientationMatrix = LightOrientationMatrix * RotationMatrix;
//            m_pDirLightOrienationCamera->SetParentMatrix(LightOrientationMatrix);

            Matrix4f rotation = cascadeFrustumProjMatrix;
            rotation.setIdentity();

//    		if(m_lightAngle > 88){
//    			m_lightAngle = -88;
//    		}

            rotation.rotate((float)Math.toRadians(m_lightAngle), m_lightAxis);
            Matrix4f.transformNormal(rotation, m_LightPos, vLightPos);
            m_lightAngle += deltaSeconds * 180f/10;

            boolean reset = false;
            if(m_totalTime > 1.5f){
                m_totalTime = 0.0f;
                reset = true;
            }

            if(!reset){
                m_totalTime += deltaSeconds;
            }
        }

//        if( mpCameraController )
        {
//            float fSpeedScale = max( (m_CameraPos.y-5000)/20, 200.f );
//            mpCameraController->SetMoveSpeed(fSpeedScale);
//
//            mpCameraController->Update( static_cast<float>(deltaSeconds) );
        }

//        mpCamera->GetPosition(&m_CameraPos.x, &m_CameraPos.y, &m_CameraPos.z);
        m_CameraPos.set(m_transformer.getTranslationVec());
        m_CameraPos.scale(-1);

        float fTerrainHeightUnderCamera =
                m_pElevDataSource.getInterpolatedHeight(m_CameraPos.x/m_TerrainRenderParams.m_TerrainAttribs.m_fElevationSamplingInterval,
                        m_CameraPos.z/m_TerrainRenderParams.m_TerrainAttribs.m_fElevationSamplingInterval)
                        * m_TerrainRenderParams.m_TerrainAttribs.m_fElevationScale;
        fTerrainHeightUnderCamera += 100.f;
        float fXZMoveRadius = 512 * m_TerrainRenderParams.m_TerrainAttribs.m_fElevationSamplingInterval;
        boolean bUpdateCamPos = false;
//        float fDistFromCenter = (float) Math.sqrt(m_CameraPos.x*m_CameraPos.x + m_CameraPos.z*m_CameraPos.z);
//        if( fDistFromCenter > fXZMoveRadius )
//        {
//            m_CameraPos.x *= fXZMoveRadius/fDistFromCenter;
//            m_CameraPos.z *= fXZMoveRadius/fDistFromCenter;
//            bUpdateCamPos = true;
//        }
        if( m_CameraPos.y < fTerrainHeightUnderCamera )
        {
            m_CameraPos.y = fTerrainHeightUnderCamera;
            bUpdateCamPos = true;
        }
//        float fMaxCameraAltitude = /*SAirScatteringAttribs().fAtmTopHeight*/80000.f * 10;
//        if( m_CameraPos.y > fMaxCameraAltitude )
//        {
//            m_CameraPos.y = fMaxCameraAltitude;
//            bUpdateCamPos = true;
//        }
        if( bUpdateCamPos )
        {
//            mpCamera->SetPosition(m_CameraPos.x, m_CameraPos.y, m_CameraPos.z);
//            mpCamera->Update();
            m_transformer.setTranslation(-m_CameraPos.x, -m_CameraPos.y, -m_CameraPos.z);
        }

//        m_CameraViewMatrix = (D3DXMATRIX&)*mpCamera->GetViewMatrix();
//        D3DXMATRIX mProj = (D3DXMATRIX &)*mpCamera->GetProjectionMatrix();
        final Matrix4f m_CameraViewMatrix = m_ViewMatrix;
        m_transformer.getModelViewMat(m_CameraViewMatrix);
        float fEarthRadius = 6360000.f/*SAirScatteringAttribs().fEarthRadius*/;
//        D3DXVECTOR3 EarthCenter(0, -fEarthRadius, 0);
        Vector3f EarthCenter = tmpVec3;
        EarthCenter.set(0, -fEarthRadius,0);
        float fNearPlaneZ, fFarPlaneZ;

        ComputeApproximateNearFarPlaneDist(m_CameraPos,
                m_CameraViewMatrix,
                m_ProjMatrix,
                EarthCenter,
                fEarthRadius,
                fEarthRadius + m_fMinElevation,
                fEarthRadius + m_fMaxElevation,
//                                           fNearPlaneZ,
//                                           fFarPlaneZ,
                m_NearFarPlane);
        fNearPlaneZ = m_NearFarPlane.x;
        fFarPlaneZ = m_NearFarPlane.y;

        fNearPlaneZ = Math.max(fNearPlaneZ, 0.1f);
        fFarPlaneZ  = Math.max(fFarPlaneZ, fNearPlaneZ+100);
        fFarPlaneZ  = Math.max(fFarPlaneZ, 1000);

        m_NearFarPlane.x = fNearPlaneZ;
        m_NearFarPlane.y = fFarPlaneZ;

//        mpCamera->SetNearPlaneDistance( fNearPlaneZ );
//        mpCamera->SetFarPlaneDistance( fFarPlaneZ );
//        mpCamera->Update();
        Matrix4f.perspective(45, (float)nvApp.width()/nvApp.height(), fNearPlaneZ, fFarPlaneZ, m_ProjMatrix);
//        System.err.println("fNearPlaneZ = " + fNearPlaneZ);
//        System.err.println("fFarPlaneZ = " + fFarPlaneZ);

        m_fCameraFar = fFarPlaneZ;
        m_fCameraNear = fNearPlaneZ;

        if(m_bFirstLoop){
            m_initViewMat.load(m_CameraViewMatrix);
//        	m_bFirstLoop = false;

        }
    }

    public void onResize(int width, int height) {
        if( width <= 0 || height <= 0 )
            return;
        m_uiBackBufferWidth = width;
        m_uiBackBufferHeight = height;

        releaseTmpBackBuffAndDepthBuff();
        Texture2DDesc screenRenderTargetDesc = new Texture2DDesc
                (
                        width,
                        height,
                        1,
                        1,
                        GLenum.GL_RGBA8,
                        1
                );

        m_pOffscreenRenderTarget = TextureUtils.createTexture2D(screenRenderTargetDesc, null);
        m_pOffscreenRenderTarget.setMagFilter(GLenum.GL_LINEAR);
        m_pOffscreenRenderTarget.setMinFilter(GLenum.GL_LINEAR);
        m_pOffscreenRenderTarget.setWrapS(GLenum.GL_CLAMP_TO_EDGE);
        m_pOffscreenRenderTarget.setWrapT(GLenum.GL_CLAMP_TO_EDGE);

        screenRenderTargetDesc.format = GLenum.GL_DEPTH_COMPONENT32F;
        m_pOffscreenDepth = TextureUtils.createTexture2D(screenRenderTargetDesc, null);
        screenRenderTargetDesc.width = m_uiShadowMapResolution;
        screenRenderTargetDesc.height = m_uiShadowMapResolution;
        screenRenderTargetDesc.arraySize = 4;

        m_pShadowMapSRV = TextureUtils.createTexture2D(screenRenderTargetDesc, null);

//        m_shadowMapInput.fov = 45.0f;
//        m_shadowMapInput.ratio = (float)width/height;
//        m_shadowMapInput.nearPlane = 50;
//        m_shadowMapInput.farPlane = 1e+7f;
        Matrix4f.perspective(45.0f, (float)width/height, 50, 1e+7f, m_ProjMatrix);

        if(m_pOffscreenFBO == 0){
            m_pOffscreenFBO = gl.glGenFramebuffer();
        }

        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, m_pOffscreenFBO);
        {
            gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_COLOR_ATTACHMENT0, m_pOffscreenRenderTarget.getTarget(), m_pOffscreenRenderTarget.getTexture(), 0);
            gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_DEPTH_ATTACHMENT, m_pOffscreenDepth.getTarget(), m_pOffscreenDepth.getTexture(), 0);
        }
        GLCheck.checkFramebufferStatus();
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
    }

    public void onDestroy() {
        m_EarthHemisphere.onD3D11DestroyDevice();

        releaseTmpBackBuffAndDepthBuff();

        if(m_QuadProgram != null){
            m_QuadProgram.dispose();
            m_QuadProgram = null;
        }

        if(m_RenderSunTech != null){
            m_RenderSunTech.dispose();
            m_RenderSunTech = null;
        }

    }

    private void releaseTmpBackBuffAndDepthBuff(){
        if(m_pOffscreenRenderTarget != null){
            m_pOffscreenRenderTarget.dispose();
            m_pOffscreenRenderTarget = null;
        }

        if(m_pOffscreenDepth != null){
            m_pOffscreenDepth.dispose();
            m_pOffscreenDepth = null;
        }
    }

}
