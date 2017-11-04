package jet.opengl.demos.nvidia.shadows;

import com.nvidia.developer.opengl.utils.BoundingBox;
import com.nvidia.developer.opengl.utils.ShadowmapGenerateProgram;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.nio.IntBuffer;

import jet.opengl.demos.scene.BaseScene;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.FramebufferGL;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2017/11/3.
 */
 public abstract class ShadowScene extends BaseScene{
    private static final int DIRTY_SHADOWMAP_TEXTURE = 1;   // It means that we need reconstruct the shadow map texture.
    private static final int DIRTY_SHADOW_CONSTRUCTION = 2;  // It means the shadow frame needs reconstructing which caused by the light animation or shadow casters animation.
    private static final int DIRTY_LIGHT_TYPE = 4;
    private static final int DIRTY_SHADOW_TYPE = 8;

    private final ShadowConfig shadowConfig = new ShadowConfig();
    private int mDirtyFlags;  // 0 means no dirty.

    private final BoundingBox mPreviousShadowCasterBoudingBox = new BoundingBox();
    private final BoundingBox mCurrentShadowCasterBoudingBox = new BoundingBox();
    private int m_ShadowFBO;
    private Texture2D m_ShadowMap;
    private ShadowmapGenerateProgram mShadowmapGenerateProgram;

    private int m_ResolvedShadowFBO;  // Used when the source shadow has multi-samples.
    private Texture2D m_ResolvedShadowMap;
    private final ShadowMapParams m_ShadowMapParams = new ShadowMapParams();
    private boolean mOnlyClearShadowMap;
    private int mCascadeCount;
    private boolean mCubeShadowMap;
    private final Matrix4f m_tempMat0 = new Matrix4f();

    @Override
    protected final void onRender(boolean clearFBO) {
//        getShadowCasterBoundingBox(mCurrentShadowCasterBoudingBox);

        prepareShadowMap();
        onSceneRender(clearFBO);
    }

    private void prepareShadowMap(){
        mOnlyClearShadowMap = false;
        mCascadeCount = 0;
        /*if(!mCurrentShadowCasterBoudingBox.valid()){
            // no objects cast the shadow
            mOnlyClearShadowMap = true;
//            mDirtyFlags |= DIRTY_SHADOW_CONSTRUCTION;
        }else if (!mPreviousShadowCasterBoudingBox.valid() || !mPreviousShadowCasterBoudingBox.equals(mCurrentShadowCasterBoudingBox)){
            mDirtyFlags |= DIRTY_SHADOW_CONSTRUCTION;
        }

        mPreviousShadowCasterBoudingBox.set(mCurrentShadowCasterBoudingBox);

        if(!mOnlyClearShadowMap){
            if((mDirtyFlags & DIRTY_SHADOW_CONSTRUCTION) != 0){
                buildShadowMapFrame();
            }
        }*/

        if(isShdowMapFrameDirty()){
            buildShadowMapFrame();
        }

        renderShadowMap();

        mDirtyFlags = 0;  // clear the flags.
    }

    private void renderShadowMap(){
        // 1, Saved the current FBO
        int vx = -1;
        int vy = -1;
        int vw = -1;
        int vh = -1;
        int old_fbo = 0;
        IntBuffer viewport = CacheBuffer.getCachedIntBuffer(4);
        gl.glGetIntegerv(GLenum.GL_VIEWPORT, viewport);

        vx = viewport.get();
        vy = viewport.get();
        vw = viewport.get();
        vh = viewport.get();
        old_fbo = gl.glGetInteger(GLenum.GL_FRAMEBUFFER_BINDING);

        // 1, We need update the shadow map if config changed.
        if((mDirtyFlags & DIRTY_SHADOWMAP_TEXTURE) !=0){
            buildShadowMaps();
        }

        // 2, Render the Shadow Map
        if(mShadowmapGenerateProgram == null){
            mShadowmapGenerateProgram = new ShadowmapGenerateProgram();
        }

        final int attachment = FramebufferGL.measureTextureAttachment(m_ShadowMap, 0);
        mShadowmapGenerateProgram.enable();
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, m_ShadowFBO);
        gl.glEnable(GLenum.GL_DEPTH_TEST);
        gl.glDepthFunc(GLenum.GL_LESS);
        gl.glDepthMask(true);
        gl.glClearDepthf(1.0f);
        for(int i =0; i < mCascadeCount; i++){
            if(mCascadeCount == 1) {
                gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, attachment, m_ShadowMap.getTarget(), m_ShadowMap.getTexture(), 0);
            }else{
                gl.glFramebufferTextureLayer(GLenum.GL_FRAMEBUFFER, attachment, m_ShadowMap.getTexture(), 0, i);
            }

            gl.glClear(GLenum.GL_DEPTH_BUFFER_BIT);
            if(mOnlyClearShadowMap)
                onShadowRender(m_ShadowMapParams, mShadowmapGenerateProgram, i);
        }

        // 3, Store the framebuffer and viewport
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, old_fbo);
        gl.glViewport(vx, vy, vw, vh);

        //4, Resovle the multi-samples
        if(mCascadeCount > 1){
            gl.glBindFramebuffer(GLenum.GL_READ_FRAMEBUFFER, m_ShadowFBO);
            // dettach the previouse binding
            gl.glFramebufferTextureLayer(GLenum.GL_READ_FRAMEBUFFER, attachment, 0, 0, 0);
            gl.glFramebufferTexture(GLenum.GL_READ_FRAMEBUFFER, attachment, m_ShadowMap.getTexture(), 0);

            gl.glBindFramebuffer(GLenum.GL_DRAW_FRAMEBUFFER, m_ResolvedShadowFBO);
            int shadowMapSize = shadowConfig.shadowMapSize;
            gl.glBlitFramebuffer(0,0, shadowMapSize, shadowMapSize,
                    0,0,shadowMapSize,shadowMapSize,
                    GLenum.GL_NEAREST, GLenum.GL_DEPTH_BUFFER_BIT);


            gl.glBindFramebuffer(GLenum.GL_READ_FRAMEBUFFER, 0);
            gl.glBindFramebuffer(GLenum.GL_DRAW_FRAMEBUFFER, 0);
        }

    }

    private void buildShadowMaps(){
        releaseShadowMapResources();

        Texture2DDesc shadowmapDesc = new Texture2DDesc(shadowConfig.shadowMapSize, shadowConfig.shadowMapSize, shadowConfig.shadowMapFormat);
        shadowmapDesc.sampleCount = shadowConfig.shadowMapSampleCount;
        shadowmapDesc.arraySize = mCascadeCount;
        {
            m_ShadowMap = TextureUtils.createTexture2D(shadowmapDesc, null);
            m_ShadowFBO = gl.glGenFramebuffer();

            gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, m_ShadowFBO);
            gl.glDrawBuffers(GLenum.GL_NONE);
        }

        {
            if(shadowConfig.shadowMapSampleCount > 1){
                shadowmapDesc.sampleCount = 1;
                m_ResolvedShadowMap = TextureUtils.createTexture2D(shadowmapDesc, null);
                m_ResolvedShadowFBO = gl.glGenFramebuffer();

                gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, m_ResolvedShadowFBO);
                gl.glFramebufferTexture(GLenum.GL_FRAMEBUFFER, FramebufferGL.measureTextureAttachment(m_ResolvedShadowMap, 0),m_ResolvedShadowMap.getTexture(), 0);
                gl.glDrawBuffers(GLenum.GL_NONE);
            }
        }
    }

    private void buildShadowMapFrame(){
        if(shadowConfig.lightType == LightType.SPOT){
            buildSpotLightShadowMapFrame();
        }else if(shadowConfig.lightType == LightType.DIRECTION){
            buildDirectionLightShadowMapFrame();
        }else if(shadowConfig.lightType == LightType.POINT){
            buildPointLightShadowMapFrame();
        }
    }

    private void  buildSpotLightShadowMapFrame(){
        if(shadowConfig.shadowMapSplitting == ShadowMapSplitting.NONE){
            Matrix4f.perspective(shadowConfig.spotHalfAngle * 2, 1.0f, shadowConfig.lightNear, shadowConfig.lightFar, m_ShadowMapParams.m_LightProj);
            Vector3f lightTarget = Vector3f.sub(shadowConfig.lightPos, shadowConfig.lightDir, null);
            Matrix4f.lookAt(shadowConfig.lightPos, lightTarget, Vector3f.Y_AXIS, m_ShadowMapParams.m_LightView);  // TODO
            Matrix4f.mul(m_ShadowMapParams.m_LightProj, m_ShadowMapParams.m_LightView, m_ShadowMapParams.m_LightViewProj);

            m_ShadowMapParams.m_LightFar = shadowConfig.lightFar;
            m_ShadowMapParams.m_LightNear = shadowConfig.lightNear;

            mCascadeCount = 1;
            mCubeShadowMap = false;
            mOnlyClearShadowMap = !checkCameraFrustumeVisible(m_ShadowMapParams.m_LightViewProj);
        }
    }

    private void buildDirectionLightShadowMapFrame(){
        if(shadowConfig.shadowMapSplitting == ShadowMapSplitting.NONE){
            calculateCameraViewBoundingBox(mCurrentShadowCasterBoudingBox);
            mPreviousShadowCasterBoudingBox.init();
            int count = getShadowCasterCount();
            for(int i = 0; i < count; i++){
                addShadowCasterBoundingBox(i, mPreviousShadowCasterBoudingBox);
            }

            BoundingBox.intersect(mPreviousShadowCasterBoudingBox, mCurrentShadowCasterBoudingBox, mCurrentShadowCasterBoudingBox);
            mOnlyClearShadowMap = !mCurrentShadowCasterBoudingBox.valid();
            if(mOnlyClearShadowMap){
                return;
            }

            Matrix4f.lookAt(Vector3f.ZERO, shadowConfig.lightDir, Vector3f.Y_AXIS, m_ShadowMapParams.m_LightView);  // TODO
            // construct the projection matrix.
            calculateLightViewBoundingBox(mCurrentShadowCasterBoudingBox, m_ShadowMapParams.m_LightView, mCurrentShadowCasterBoudingBox);

            float near = Math.min(-mCurrentShadowCasterBoudingBox._max.z, shadowConfig.lightNear);
            float far = Math.max(-mCurrentShadowCasterBoudingBox._min.z, shadowConfig.lightFar);
            Matrix4f.ortho(mCurrentShadowCasterBoudingBox._min.x, mCurrentShadowCasterBoudingBox._max.x,
                    mCurrentShadowCasterBoudingBox._min.y, mCurrentShadowCasterBoudingBox._max.y,
                    near, far, m_ShadowMapParams.m_LightProj);

            Matrix4f.mul(m_ShadowMapParams.m_LightProj, m_ShadowMapParams.m_LightView, m_ShadowMapParams.m_LightViewProj);

            m_ShadowMapParams.m_LightFar = /*shadowConfig.lightFar*/far;
            m_ShadowMapParams.m_LightNear = /*shadowConfig.lightNear*/near;

            mCascadeCount = 1;
            mCubeShadowMap = false;

        }
    }

    private void buildPointLightShadowMapFrame(){
        if(shadowConfig.shadowMapSplitting == ShadowMapSplitting.NONE){
//            Matrix4f.(shadowConfig.spotHalfAngle * 2, 1.0f, shadowConfig.lightNear, shadowConfig.lightFar, m_ShadowMapParams.m_LightProj);  TODO
            Vector3f lightTarget = Vector3f.sub(shadowConfig.lightPos, shadowConfig.lightDir, null);
            Matrix4f.lookAt(shadowConfig.lightPos, lightTarget, Vector3f.Y_AXIS, m_ShadowMapParams.m_LightView);
            Matrix4f.mul(m_ShadowMapParams.m_LightProj, m_ShadowMapParams.m_LightView, m_ShadowMapParams.m_LightViewProj);

            m_ShadowMapParams.m_LightFar = shadowConfig.lightFar;
            m_ShadowMapParams.m_LightNear = shadowConfig.lightNear;

            mCascadeCount = 1;
            mCubeShadowMap = false;
            mOnlyClearShadowMap = !checkCameraFrustumeVisible(m_ShadowMapParams.m_LightViewProj);
        }
    }

    /**
     * Check the frustume of the view camera whether can be seen by the light camera. Return false means the light camera can't
     * see anything in the final image, then the shadowmap render-system will auto ingore the "Shadow Render" step and just clear the
     * entry shadowmap to 1.0<p>
     *     Subclass could override this method to get more precise cheching.
     * @param lightViewProj
     * @return True means the light camera can see the view camera frustume.
     */
    protected boolean checkCameraFrustumeVisible(Matrix4f lightViewProj){
        if(shadowConfig.checkCameraFrustumeVisible){
            Matrix4f.invert(getSceneData().getViewProjMatrix(), m_tempMat0);
            return checkFrustumeCollisions(m_tempMat0, lightViewProj);
        }

        return true;
    }

    protected final void calculateCameraViewBoundingBox(BoundingBox out){
        out.init();

        Vector3f f3PlaneCornerProjSpace = new Vector3f();
        Matrix4f cameraProjToWorld = Matrix4f.invert(getSceneData().getViewProjMatrix(), m_tempMat0);

        for(int iClipPlaneCorner=0; iClipPlaneCorner < 8; ++iClipPlaneCorner) {
            f3PlaneCornerProjSpace.set((iClipPlaneCorner & 0x01) != 0 ? +1.f : -1.f,
                    (iClipPlaneCorner & 0x02) != 0 ? +1.f : -1.f,
                    // Since we use complimentary depth buffering,
                    // far plane has depth 0
                    (iClipPlaneCorner & 0x04) != 0 ? 1.f : -1.f);

            Matrix4f.transformCoord(cameraProjToWorld, f3PlaneCornerProjSpace, f3PlaneCornerProjSpace);  // Transform the position from projection to world space

            out.expandBy(f3PlaneCornerProjSpace);
        }
    }

    protected final void calculateLightViewBoundingBox(BoundingBox worldPos, Matrix4f lightView, BoundingBox out){
        if(m_corners[0] == null){
            for(int i = 0; i < m_corners.length; i++){
                m_corners[i] = new Vector4f();
            }
        }

        m_corners[0].set(worldPos._min.x, worldPos._min.y, worldPos._min.z, 1.0f);
        m_corners[1].set(worldPos._max.x, worldPos._min.y, worldPos._min.z, 1.0f);
        m_corners[2].set(worldPos._max.x, worldPos._max.y, worldPos._min.z, 1.0f);
        m_corners[3].set(worldPos._min.x, worldPos._max.y, worldPos._min.z, 1.0f);
        m_corners[4].set(worldPos._min.x, worldPos._min.y, worldPos._max.z, 1.0f);
        m_corners[5].set(worldPos._max.x, worldPos._min.y, worldPos._max.z, 1.0f);
        m_corners[6].set(worldPos._max.x, worldPos._max.y, worldPos._max.z, 1.0f);
        m_corners[7].set(worldPos._min.x, worldPos._max.y, worldPos._max.z, 1.0f);

        out.init();
        for(int i = 0; i < 8; i++){
            Vector4f pos = m_corners[i];
            Matrix4f.transform(lightView, pos, pos);

            out.expandBy(pos);
        }
    }

    private final Vector4f[] m_corners = new Vector4f[8];
    protected final boolean checkFrustumeCollisions(Matrix4f cameraProjToWorld, Matrix4f viewProj1){
        if(m_corners[0] == null){
            for(int i = 0; i < m_corners.length; i++){
                m_corners[i] = new Vector4f();
            }
        }

        for(int iClipPlaneCorner=0; iClipPlaneCorner < 8; ++iClipPlaneCorner) {
            Vector4f f3PlaneCornerProjSpace = m_corners[iClipPlaneCorner];
            f3PlaneCornerProjSpace.set((iClipPlaneCorner & 0x01) != 0 ? +1.f : -1.f,
                    (iClipPlaneCorner & 0x02) != 0 ? +1.f : -1.f,
                    // Since we use complimentary depth buffering,
                    // far plane has depth 0
                    (iClipPlaneCorner & 0x04) != 0 ? 1.f : -1.f, 1.0f);

            Matrix4f.transformCoord(cameraProjToWorld, f3PlaneCornerProjSpace, f3PlaneCornerProjSpace);  // Transform the position from projection to world space
            Matrix4f.transform(viewProj1, f3PlaneCornerProjSpace, f3PlaneCornerProjSpace);               // Transform the position from world space to another projection space.
        }

        if (m_corners[0].x < -m_corners[0].w && m_corners[1].x < -m_corners[1].w && m_corners[2].x < -m_corners[2].w && m_corners[3].x < -m_corners[3].w &&
                m_corners[4].x < -m_corners[4].w && m_corners[5].x < -m_corners[5].w && m_corners[6].x < -m_corners[6].w && m_corners[7].x < -m_corners[7].w)
            return false;

        if (m_corners[0].x > m_corners[0].w && m_corners[1].x > m_corners[1].w && m_corners[2].x > m_corners[2].w && m_corners[3].x > m_corners[3].w &&
                m_corners[4].x > m_corners[4].w && m_corners[5].x > m_corners[5].w && m_corners[6].x > m_corners[6].w && m_corners[7].x > m_corners[7].w)
            return false;

        if (m_corners[0].y < -m_corners[0].w && m_corners[1].y < -m_corners[1].w && m_corners[2].y < -m_corners[2].w && m_corners[3].y < -m_corners[3].w &&
                m_corners[4].y < -m_corners[4].w && m_corners[5].y < -m_corners[5].w && m_corners[6].y < -m_corners[6].w && m_corners[7].y < -m_corners[7].w)
            return false;

        if (m_corners[0].y > m_corners[0].w && m_corners[1].y > m_corners[1].w && m_corners[2].y > m_corners[2].w && m_corners[3].y > m_corners[3].w &&
                m_corners[4].y > m_corners[4].w && m_corners[5].y > m_corners[5].w && m_corners[6].y > m_corners[6].w && m_corners[7].y > m_corners[7].w)
            return false;

        if (m_corners[0].z < -m_corners[0].w && m_corners[1].z < -m_corners[1].w && m_corners[2].z < -m_corners[2].w && m_corners[3].z < -m_corners[3].w &&
                m_corners[4].z < -m_corners[4].w && m_corners[5].z < -m_corners[5].w && m_corners[6].z < -m_corners[6].w && m_corners[7].z < -m_corners[7].w)
            return false;

        if (m_corners[0].z > m_corners[0].w && m_corners[1].z > m_corners[1].w && m_corners[2].z > m_corners[2].w && m_corners[3].z > m_corners[3].w &&
                m_corners[4].z > m_corners[4].w && m_corners[5].z > m_corners[5].w && m_corners[6].z > m_corners[6].w && m_corners[7].z > m_corners[7].w)
            return false;

        return true;
    }

    public final void invalidShadowCasters(){
        mDirtyFlags |= DIRTY_SHADOW_CONSTRUCTION;
    }

    private final boolean isShdowMapFrameDirty() { return (mDirtyFlags & DIRTY_SHADOW_CONSTRUCTION) != 0;}

    public void setShadowConfig(ShadowConfig config){
        mDirtyFlags = 0;

        if(config.shadowType != ShadowType.NONE){
            if(!equals(config.shadowType, shadowConfig.shadowType)){
                mDirtyFlags |= DIRTY_SHADOW_TYPE;
            }

            if(!equals(config.lightType, shadowConfig.lightType)){
                mDirtyFlags |= DIRTY_LIGHT_TYPE;
            }

            if(!config.lightDir.equals(shadowConfig.lightDir)){
                invalidShadowCasters();
            }

            if(!config.lightPos.equals(shadowConfig.lightPos)){
                invalidShadowCasters();
            }

            if(!equals(config.shadowMapSplitting, shadowConfig.shadowMapSplitting)){
                invalidShadowCasters();
            }

            if(config.lightType == LightType.SPOT &&
                    (config.spotHalfAngle != shadowConfig.spotHalfAngle || config.lightFar != shadowConfig.lightFar || config.lightNear != shadowConfig.lightNear)){
                invalidShadowCasters();
            }

            if(config.lightType == LightType.POINT &&
                    (config.lightFar != shadowConfig.lightFar || config.lightNear != shadowConfig.lightNear)){
                invalidShadowCasters();
            }

            int cascadCount;
            if(equals(config.shadowMapSplitting, ShadowMapSplitting.CSM) && config.cascadCount != shadowConfig.cascadCount){
                invalidShadowCasters();
                cascadCount = config.cascadCount;
            }else{
                cascadCount = 1;
            }

            if(m_ShadowMap == null){
                mDirtyFlags |= DIRTY_SHADOWMAP_TEXTURE;
            }else{ // m_ShadowMap != null
                if(m_ShadowMap.getWidth() != config.shadowMapSize || m_ShadowMap.getSampleCount() != config.shadowMapSampleCount ||
                        m_ShadowMap.getFormat() != config.shadowMapFormat || m_ShadowMap.getArraySize() != cascadCount){
                    mDirtyFlags |= DIRTY_SHADOWMAP_TEXTURE;
                }
            }
        }

        shadowConfig.set(config); // copy the data
    }

    private static<T> boolean equals(Enum<? super T> a, Enum<? super T> b){
        int aValue = (a != null) ? a.ordinal() : 0;
        int bValue = (b != null) ? b.ordinal() : 0;

        return aValue == bValue;
    }

    protected abstract void onShadowRender(ShadowMapParams shadowMapParams, ShadowmapGenerateProgram program, int cascade);
    protected abstract void onSceneRender(boolean clearFBO);
    public Texture2D getShadowMap(){
        return m_ShadowMap.getSampleCount() > 1 ? m_ResolvedShadowMap : m_ShadowMap;
    }

    @Override
    protected void onDestroy() {
        releaseShadowMapResources();
    }

    private void releaseShadowMapResources(){
        if(m_ShadowFBO != 0){
            gl.glDeleteFramebuffer(m_ShadowFBO);
            m_ShadowFBO = 0;
        }

        if(m_ShadowMap != null){
            m_ShadowMap.dispose();
            m_ShadowMap = null;
        }
    }

    /*protected abstract void getShadowCasterBoundingBox( BoundingBox boundingBox);*/
    protected abstract void addShadowCasterBoundingBox(int index, BoundingBox boundingBox);
    protected abstract int getShadowCasterCount();

    public enum LightType{
        POINT,
        SPOT,
        DIRECTION
    }

    public enum ShadowType{
        NONE,
        SHADOW_MAPPING,
        SHADOW_VOLUME
    }

    public enum ShadowMapSplitting{
        NONE,
        PSSM,  // Parallel Split shadow mapping
        CSM,   // Cascaded shadow mapping
    }

    public enum ShadowMapWarping{
        NONE,
        LiSPSM, // Light Space Perspective
        TSM,    // Trapezoid
        PSM,    // Perspective
        CSSM,   // Camera Space
    }

    public enum ShadowMapFiltering{
        NONE,
        PCF,   // Percentage Closer Filtering  TODO Smoothing
        ESM,   // Exponential
        CSM,   // Convolution
        VSM,   // Variance
        SAVSM, // Summed Area Variance
        SMSR,  // Shadow Map Silhouette Revectorization
        PCSS,  // Percentage Closer   TODO Soft Shadows
        SSSS,  // Screen space soft shadows  TODO Soft Shadows
        FIV,   // Fullsphere Irradiance Vector
    }
}
