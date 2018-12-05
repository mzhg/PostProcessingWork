package jet.opengl.demos.nvidia.volumelight;

import com.nvidia.developer.opengl.app.NvKeyActionType;
import com.nvidia.developer.opengl.app.NvSampleApp;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;

import jet.opengl.demos.scenes.Cube16;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.volumetricLighting.LightType;
import jet.opengl.postprocessing.shader.FullscreenProgram;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureDataDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.DebugTools;

public class VolumetricLightingDemo extends NvSampleApp {
    private static final int RENDER_TYPE_NONE = 0;
    private static final int RENDER_TYPE_ORIGIN = 1;
    private static final int RENDER_TYPE_CPU_TESSLATION = 2;
    private static final int RENDER_TYPE_RAY_MARCHING = 3;

    private Cube16 m_Scene;
    private GLFuncProvider gl;
    private FullscreenProgram fullscreenProgram;
    private int m_DummyVAO;

    private VolumeLightProcess m_volumeLight;
    private final VolumeLightParams m_volumeParams = new VolumeLightParams();

    final ContextDesc contextDesc_ = new ContextDesc();
    final PostprocessDesc postprocessDesc_ = new PostprocessDesc();
    final ViewerDesc      viewerDesc_      = new ViewerDesc();
    final MediumDesc      mediumDesc_      = new MediumDesc();
    final LightDesc       lightDesc_       = new LightDesc();
    final ShadowMapDesc   shadowMapDesc_   = new ShadowMapDesc();
    final VolumeDesc      volumeDesc_      = new VolumeDesc();
    ContextImp_Common gwvlctx_;
    boolean isCtxValid_;
    boolean isPaused_;

    DebugFlags debugMode_;
    int mediumType_;

    private int m_RenderType = RENDER_TYPE_ORIGIN;
    private GLSLProgram m_tonemap;

    private  Texture2D m_StaticColor;
    private  Texture2D m_StaticDepth;
    private  Texture2D m_StaticShadow;

    private boolean useStaticData = false;
    private boolean runOnce;

    @Override
    protected void initRendering() {
        getGLContext().setSwapInterval(0);
        m_Scene = new Cube16(this);

        m_Scene.onCreate();
        fullscreenProgram = new FullscreenProgram();
        gl = GLFuncProviderFactory.getGLFuncProvider();
        m_DummyVAO = gl.glGenVertexArray();

        m_volumeLight = new VolumeLightProcess();
        m_volumeLight.initlizeGL(m_Scene.getShadowMapResolution());


        m_StaticColor = loadTextureFromBinaryFile("E:/textures/VolumetricLighting/scene.dat", 1280, 720, GLenum.GL_RGBA16F);
        m_StaticDepth = loadTextureFromBinaryFile("E:/textures/VolumetricLighting/sceneDepth.dat", 1280, 720, GLenum.GL_DEPTH24_STENCIL8);
        m_StaticShadow = loadTextureFromBinaryFile("E:/textures/VolumetricLighting/shadownMap.dat", 1024, 1024, GLenum.GL_DEPTH24_STENCIL8);

        initOriginParams();
    }

    @Override
    public void initUI() {
        m_Scene.initUI(mTweakBar);
    }

    @Override
    protected void reshape(int width, int height) {
        if(width <= 0 || height <= 0)
            return;

        m_Scene.onResize(width, height);
        m_volumeLight.onResize(width, height);

        contextDesc_.uWidth = width;
        contextDesc_.uHeight = height;
        contextDesc_.uSamples = 1;
    }

    @Override
    public void display() {
        m_Scene.draw(m_RenderType == RENDER_TYPE_NONE);

        if(useStaticData&& runOnce) {
            tonemapping();
            return;
        }

        runOnce = true;
        switch (m_RenderType){
            case RENDER_TYPE_NONE:
                m_volumeParams.sceneColor = m_Scene.getSceneColor();
                m_volumeParams.sceneDepth = m_Scene.getSceneDepth();
                m_volumeParams.cameraNear = m_Scene.getSceneNearPlane();
                m_volumeParams.cameraFar =  m_Scene.getSceneFarPlane();
                m_volumeParams.cameraView.load(m_Scene.getViewMat());
                m_volumeParams.cameraProj.load(m_Scene.getProjMat());
                m_volumeParams.lightNear = m_Scene.getLightNearPlane();
                m_volumeParams.lightFar = m_Scene.getLightFarlane();
                m_volumeParams.lightView.load(m_Scene.getLightViewMat());
                m_volumeParams.lightProj.load(m_Scene.getLightProjMat());
                m_volumeParams.shadowMap = m_Scene.getShadowMap();

                m_volumeLight.renderVolumeLight(m_volumeParams);
                m_Scene.resolve();
                break;
            case RENDER_TYPE_ORIGIN:
                beginAccumulation(useStaticData ? m_StaticDepth.getTexture(): m_Scene.getSceneDepth().getTexture());
                renderVolume(useStaticData ? m_StaticShadow.getTexture() :  m_Scene.getShadowMap().getTexture());
                endAccumulation();
                applyLighting(useStaticData ? m_StaticColor.getTexture(): m_Scene.getSceneColor().getTexture(), useStaticData ? m_StaticDepth.getTexture(): m_Scene.getSceneDepth().getTexture());
                tonemapping();
                break;
        }

    }

    private void beginAccumulation(int sceneDepth){
        Objects.requireNonNull(gwvlctx_);
        Status gwvl_status = gwvlctx_.beginAccumulation(sceneDepth, getViewerDesc(), getMediumDesc(), debugMode_);
        if(gwvl_status != Status.OK){
            System.err.println("GWVL Error: " + gwvl_status);
        }
    }

    ViewerDesc getViewerDesc(){
        ViewerDesc viewerDesc = viewerDesc_;
        final Matrix4f mViewProj = viewerDesc.mViewProj;
        final Matrix4f mProj     = viewerDesc.mProj;

        Matrix4f.decompseRigidMatrix(m_Scene.getViewMat(), viewerDesc.vEyePosition, null, null, null);
        Matrix4f.mul(m_Scene.getProjMat(), m_Scene.getViewMat(), mViewProj);
        mProj.load(m_Scene.getProjMat());

        viewerDesc.uViewportWidth = contextDesc_.uWidth;
        viewerDesc.uViewportHeight = contextDesc_.uHeight;
        viewerDesc.fZNear = m_Scene.getSceneNearPlane();
        viewerDesc.fZFar  = m_Scene.getSceneFarPlane();

        /*Matrix4f.lookAt(vEyePos, vOrigin, VIEWPOINT_UP[viewpoint_], mViewProj);
        Matrix4f.perspective((float)Math.toDegrees(SPOTLIGHT_FALLOFF_ANGLE * 2.0f), (float)contextDesc_.uWidth/contextDesc_.uHeight,
                viewerDesc.fZNear, viewerDesc.fZFar, mProj);
        Matrix4f.mul(mProj, mViewProj, mViewProj);*/


        return viewerDesc;
    }

    final Vector3f tempVec3 = new Vector3f();
    void renderVolume(int shadowmap){
        final Vector3f vLightPos = tempVec3;
        final Matrix4f mLightViewProj = shadowMapDesc_.elements[0].mViewProj;
        getLightViewpoint(vLightPos, mLightViewProj);

        ShadowMapDesc shadowmapDesc = shadowMapDesc_;
        {
            shadowmapDesc.eType = (m_Scene.getLightMode() == LightType.POINT) ? ShadowMapLayout.PARABOLOID : ShadowMapLayout.SIMPLE;
            shadowmapDesc.uWidth = Cube16.SHADOWMAP_RESOLUTION;
            shadowmapDesc.uHeight = Cube16.SHADOWMAP_RESOLUTION;
            shadowmapDesc.uElementCount = 1;
            shadowmapDesc.elements[0].uOffsetX = 0;
            shadowmapDesc.elements[0].uOffsetY = 0;
            shadowmapDesc.elements[0].uWidth = shadowmapDesc.uWidth;
            shadowmapDesc.elements[0].uHeight = shadowmapDesc.uHeight;
//            shadowmapDesc.elements[0].mViewProj = NVtoNVC(mLightViewProj);
            shadowmapDesc.elements[0].mArrayIndex = 0;
            if (m_Scene.getLightMode() == LightType.POINT)
            {
                shadowmapDesc.uElementCount = 2;
                shadowmapDesc.elements[1].uOffsetX = 0;
                shadowmapDesc.elements[1].uOffsetY = 0;
                shadowmapDesc.elements[1].uWidth = shadowmapDesc.uWidth;
                shadowmapDesc.elements[1].uHeight = shadowmapDesc.uHeight;
                shadowmapDesc.elements[1].mViewProj.load(mLightViewProj);
                shadowmapDesc.elements[1].mArrayIndex = 1;
            }
        }

        VolumeDesc volumeDesc = volumeDesc_;
        {
            volumeDesc.fTargetRayResolution = 12.0f;
            volumeDesc.uMaxMeshResolution = Cube16.SHADOWMAP_RESOLUTION;
            volumeDesc.fDepthBias = 0.0f;
            volumeDesc.eTessQuality = TessellationQuality.HIGH;
        }

        Status gwvl_status;
//        gwvl_status = Nv::Vl::RenderVolume(gwvlctx_, ctx, shadowmap, &shadowmapDesc, getLightDesc(), &volumeDesc);
        gwvl_status = gwvlctx_.renderVolume(shadowmap, shadowmapDesc, getLightDesc(), volumeDesc);
        if(gwvl_status != Status.OK){
            System.err.println("GWVL Error: " + gwvl_status);
        }
    }

    void endAccumulation(){
        Status gwvl_status = gwvlctx_.endAccumulation();
        if(gwvl_status != Status.OK){
            System.err.println("GWVL Error: " + gwvl_status);
        }
    }

    void applyLighting(int sceneRT, int sceneDepth){
        Status gwvl_status = gwvlctx_.applyLighting(sceneRT, sceneDepth, getPostprocessDesc());
        if(gwvl_status != Status.OK){
            System.err.println("GWVL Error: " + gwvl_status);
        }
    }

    void tonemapping(){
        gl.glDisable(GLenum.GL_DEPTH_TEST);
        gl.glDisable(GLenum.GL_STENCIL_TEST);
        gl.glDisable(GLenum.GL_CULL_FACE);
        gl.glColorMask(true, true, true, true);
        gl.glBindTextureUnit(0, useStaticData ? m_StaticColor.getTexture():  m_Scene.getSceneColor().getTexture());
        gl.glBindSampler(0,0);

//        	NV_PERFEVENT(ctx, "Postprocess");
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
        gl.glViewport(0, 0, getGLContext().width(), getGLContext().height());
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT);

        m_tonemap.enable();
        gl.glBindVertexArray(m_DummyVAO);
        gl.glDrawArrays(GLenum.GL_TRIANGLES,0,3);
        gl.glBindVertexArray(0);
        m_tonemap.disable();
        GLCheck.checkError();
    }

    MediumDesc getMediumDesc(){
        MediumDesc mediumDesc = mediumDesc_;
        final float SCATTER_PARAM_SCALE = 0.0001f;
        mediumDesc.uNumPhaseTerms = 0;

        int t = 0;

        mediumDesc.phaseTerms[t].ePhaseFunc = PhaseFunctionType.RAYLEIGH;
//        mediumDesc.phaseTerms[t].vDensity = NVtoNVC(10.00f * SCATTER_PARAM_SCALE * Nv::NvVec3(0.596f, 1.324f, 3.310f));
        Vector3f vDensity = mediumDesc.phaseTerms[t].vDensity;
        vDensity.set(0.596f, 1.324f, 3.310f);
        vDensity.scale(10.00f * SCATTER_PARAM_SCALE);
        t++;

        switch (mediumType_)
        {
            default:
            case 0:
            {
                mediumDesc.phaseTerms[t].ePhaseFunc = PhaseFunctionType.HENYEYGREENSTEIN;
//            mediumDesc.phaseTerms[t].vDensity = NVtoNVC(10.00f * SCATTER_PARAM_SCALE * Nv::NvVec3(1.00f, 1.00f, 1.00f));
                float density = 10.00f * SCATTER_PARAM_SCALE;
                mediumDesc.phaseTerms[t].vDensity.set(density, density, density);
                mediumDesc.phaseTerms[t].fEccentricity = 0.85f;
                t++;
//            mediumDesc.vAbsorption = NVtoNVC(5.0f * SCATTER_PARAM_SCALE * Nv::NvVec3(1, 1, 1));
                float absorption = 5.0f * SCATTER_PARAM_SCALE;
                mediumDesc.vAbsorption.set(absorption, absorption, absorption);
                break;
            }
            case 1:
            {
                mediumDesc.phaseTerms[t].ePhaseFunc = PhaseFunctionType.HENYEYGREENSTEIN;
//            mediumDesc.phaseTerms[t].vDensity = NVtoNVC(15.00f * SCATTER_PARAM_SCALE * Nv::NvVec3(1.00f, 1.00f, 1.00f));
                float density = 15.00f * SCATTER_PARAM_SCALE;
                mediumDesc.phaseTerms[t].vDensity.set(density, density, density);
                mediumDesc.phaseTerms[t].fEccentricity = 0.60f;
                t++;
//            mediumDesc.vAbsorption = NVtoNVC(25.0f * SCATTER_PARAM_SCALE * Nv::NvVec3(1, 1, 1));
                float absorption = 25.0f * SCATTER_PARAM_SCALE;
                mediumDesc.vAbsorption.set(absorption, absorption, absorption);
                break;
            }
            case 2:
            {
                mediumDesc.phaseTerms[t].ePhaseFunc = PhaseFunctionType.MIE_HAZY;
//            mediumDesc.phaseTerms[t].vDensity = NVtoNVC(20.00f * SCATTER_PARAM_SCALE * Nv::NvVec3(1.00f, 1.00f, 1.00f));
                float density = 20.00f * SCATTER_PARAM_SCALE;
                mediumDesc.phaseTerms[t].vDensity.set(density, density, density);
                t++;
//            mediumDesc.vAbsorption = NVtoNVC(25.0f * SCATTER_PARAM_SCALE * Nv::NvVec3(1, 1, 1));
                float absorption = 25.0f * SCATTER_PARAM_SCALE;
                mediumDesc.vAbsorption.set(absorption, absorption, absorption);
                break;
            }
            case 3:
            {
                mediumDesc.phaseTerms[t].ePhaseFunc = PhaseFunctionType.MIE_MURKY;
//            mediumDesc.phaseTerms[t].vDensity = NVtoNVC(30.00f * SCATTER_PARAM_SCALE * Nv::NvVec3(1.00f, 1.00f, 1.00f));
                float density = 30.00f * SCATTER_PARAM_SCALE;
                mediumDesc.phaseTerms[t].vDensity.set(density, density, density);
                t++;
//            mediumDesc.vAbsorption = NVtoNVC(50.0f * SCATTER_PARAM_SCALE * Nv::NvVec3(1, 1, 1));
                float absorption = 50.0f * SCATTER_PARAM_SCALE;
                mediumDesc.vAbsorption.set(absorption, absorption, absorption);
                break;
            }
        }

        mediumDesc.uNumPhaseTerms = t;
        return mediumDesc;
    }

    LightDesc getLightDesc(){
        LightDesc lightDesc = lightDesc_;

        final Vector3f vLightPos = lightDesc.vPosition;
        final Matrix4f mLightViewProj = lightDesc.mLightToWorld;
        getLightViewpoint(vLightPos, mLightViewProj);
//        Nv::NvMat44 mLightViewProj_Inv = Inverse(mLightViewProj);

//        lightDesc.vIntensity = NVtoNVC(getLightIntensity());
        m_Scene.getLightIntensity(lightDesc.vIntensity);
//        lightDesc.mLightToWorld = NVtoNVC(mLightViewProj_Inv);
        mLightViewProj.invert();

        switch (m_Scene.getLightMode())
        {
            case POINT:
            {
                lightDesc.eType = LightType.POINT;
                lightDesc.fZNear = m_Scene.getLightNearPlane();
                lightDesc.fZFar = m_Scene.getLightFarlane();
//                lightDesc.vPosition = NVtoNVC(vLightPos);
                lightDesc.eFalloffMode = SpotlightFalloffMode.NONE;
                lightDesc.eAttenuationMode = AttenuationMode.INV_POLYNOMIAL;
                final float LIGHT_SOURCE_RADIUS = 0.5f; // virtual radius of a spheroid light source
                lightDesc.fAttenuationFactors[0] = 1.0f;
                lightDesc.fAttenuationFactors[1] = 2.0f / LIGHT_SOURCE_RADIUS;
                lightDesc.fAttenuationFactors[2] = 1.0f / (LIGHT_SOURCE_RADIUS*LIGHT_SOURCE_RADIUS);
                lightDesc.fAttenuationFactors[3] = 0.0f;
            }
            break;

            case SPOT:
            {
//                Nv::NvVec3 vLightDirection = -vLightPos;
                final Vector3f vLightDirection = Vector3f.scale(vLightPos, -1, lightDesc.vDirection);
                vLightDirection.normalise();
                lightDesc.eType = LightType.SPOT;
                lightDesc.fZNear = m_Scene.getLightNearPlane();
                lightDesc.fZFar = m_Scene.getLightFarlane();
                lightDesc.eFalloffMode = SpotlightFalloffMode.FIXED;
                lightDesc.fFalloff_Power = Cube16.SPOTLIGHT_FALLOFF_POWER;
                lightDesc.fFalloff_CosTheta = (float) Math.cos(Cube16.SPOTLIGHT_FALLOFF_ANGLE);
//                lightDesc.vDirection = NVtoNVC(vLightDirection);
//                lightDesc.vPosition = NVtoNVC(vLightPos);
                lightDesc.eAttenuationMode = AttenuationMode.INV_POLYNOMIAL;
                final float LIGHT_SOURCE_RADIUS = 1.0f;  // virtual radius of a spheroid light source
                lightDesc.fAttenuationFactors[0] = 1.0f;
                lightDesc.fAttenuationFactors[1] = 2.0f / LIGHT_SOURCE_RADIUS;
                lightDesc.fAttenuationFactors[2] = 1.0f / (LIGHT_SOURCE_RADIUS*LIGHT_SOURCE_RADIUS);
                lightDesc.fAttenuationFactors[3] = 0.0f;
            }
            break;

            default:
            case DIRECTIONAL:
            {
//                Nv::NvVec3 vLightDirection = -vLightPos;
                final Vector3f vLightDirection = Vector3f.scale(vLightPos, -1, lightDesc.vDirection);
                vLightDirection.normalise();
                lightDesc.eType = LightType.DIRECTIONAL;
//                lightDesc.Directional.vDirection = NVtoNVC(vLightDirection);
            }
        }

        return lightDesc;
    }

    void getLightViewpoint(Vector3f vPos, Matrix4f mViewProj){
        Matrix4f.decompseRigidMatrix(m_Scene.getLightViewMat(), vPos, null, null, null);
        Matrix4f.mul(m_Scene.getLightProjMat(), m_Scene.getLightViewMat(), mViewProj);
    }

    PostprocessDesc getPostprocessDesc(){
        m_Scene.getLightIntensity(postprocessDesc_.vFogLight);
        postprocessDesc_.fMultiscatter = 0.000002f;
        return postprocessDesc_;
    }

    private void initOriginParams(){
        isCtxValid_ = false;
        debugMode_ = DebugFlags.NONE;
        isPaused_ = false;

        mediumType_ = 0;

        // Default context settings
        contextDesc_.uWidth = getGLContext().width();
        contextDesc_.uHeight = getGLContext().height();
        contextDesc_.uSamples = 1;
        contextDesc_.eDownsampleMode = DownsampleMode.FULL;
        contextDesc_.eInternalSampleMode = MultisampleMode.SINGLE;
        contextDesc_.eFilterMode = FilterMode.NONE;
        contextDesc_.bUseTesslation = false;

        //--------------------------------------------------------------------------
        // Default post-process settings
        postprocessDesc_.bDoFog = true;
        postprocessDesc_.bIgnoreSkyFog = false;
        postprocessDesc_.eUpsampleQuality = UpsampleQuality.BILINEAR;
        postprocessDesc_.fBlendfactor = 1.0f;
        postprocessDesc_.fTemporalFactor = 0.95f;
        postprocessDesc_.fFilterThreshold = 0.20f;

        gwvlctx_ = ContextImp_Common.create(contextDesc_);

        String root = "nvidia\\NvVolumetricLighting\\shaders\\";
        try {
            m_tonemap = GLSLProgram.createFromFiles(root + "Quad_VS.vert", root + "Post_PS.frag");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        m_Scene.onDestroy();
        fullscreenProgram.dispose();
        gl.glDeleteVertexArray(m_DummyVAO);

        if(gwvlctx_ != null){
            gwvlctx_.dispose();
            gwvlctx_ = null;
            isCtxValid_ = false;
        }
    }

    @Override
    public boolean handleKeyInput(int code, NvKeyActionType action) {
        return m_Scene.handleKeyInput(code, action);
    }

    void toggleDownsampleMode(){
        switch (contextDesc_.eDownsampleMode)
        {
            case FULL:
                contextDesc_.eDownsampleMode = DownsampleMode.HALF;
                break;

            case HALF:
                contextDesc_.eDownsampleMode = DownsampleMode.QUARTER;
                break;

            case QUARTER:
                contextDesc_.eDownsampleMode = DownsampleMode.FULL;
                break;
            default:
                break;
        }
        invalidateCtx();
    }

    void toggleMsaaMode(){
        if (contextDesc_.eDownsampleMode == DownsampleMode.FULL)
            return;

        switch (contextDesc_.eInternalSampleMode)
        {
            case SINGLE:
                contextDesc_.eInternalSampleMode = MultisampleMode.MSAA2;
                break;

            case MSAA2:
                contextDesc_.eInternalSampleMode = MultisampleMode.MSAA4;
                break;

            case MSAA4:
                contextDesc_.eInternalSampleMode = MultisampleMode.SINGLE;
                break;
            default:
                break;
        }
        invalidateCtx();
    }

    void toggleFiltering(){
        if (contextDesc_.eDownsampleMode == DownsampleMode.FULL)
            return;

        contextDesc_.eFilterMode = (contextDesc_.eFilterMode == FilterMode.TEMPORAL) ? FilterMode.NONE : FilterMode.TEMPORAL;
        invalidateCtx();
    }

    void toggleUpsampleMode(){
        if (contextDesc_.eDownsampleMode == DownsampleMode.FULL)
            return;

        switch (postprocessDesc_.eUpsampleQuality)
        {
            case POINT:
                postprocessDesc_.eUpsampleQuality = UpsampleQuality.BILINEAR;
                break;

            case BILINEAR:
                if (contextDesc_.eFilterMode == FilterMode.TEMPORAL)
                    postprocessDesc_.eUpsampleQuality = UpsampleQuality.BILATERAL;
                else
                    postprocessDesc_.eUpsampleQuality = UpsampleQuality.POINT;
                break;

            case BILATERAL:
                postprocessDesc_.eUpsampleQuality = UpsampleQuality.POINT;
                break;
            default:
                break;
        }
    }

    void toggleFog(){
        postprocessDesc_.bDoFog = !postprocessDesc_.bDoFog;
    }

    void toggleMediumType(){
        mediumType_ = (mediumType_+1)%4;
    }


    boolean isCtxValid() { return isCtxValid_;}
    void invalidateCtx() { isCtxValid_ = false;}

    static Texture2D loadTextureFromBinaryFile(String filename, int width, int height, int format){
        boolean flip = false;
        Texture2DDesc desc  = new Texture2DDesc(width, height, format);
        ByteBuffer bytes = DebugTools.loadBinary(filename);

        if(flip){
            TextureUtils.flipY(bytes, height);
        }
        int type = TextureUtils.measureDataType(format);
        format = TextureUtils.measureFormat(format);
        TextureDataDesc data = new TextureDataDesc(format, type, bytes);

        Texture2D texture = TextureUtils.createTexture2D(desc, data);
        return texture;

    }
}
