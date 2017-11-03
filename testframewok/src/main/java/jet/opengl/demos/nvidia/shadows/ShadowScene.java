package jet.opengl.demos.nvidia.shadows;

import com.nvidia.developer.opengl.utils.BoundingBox;

import org.lwjgl.util.vector.Matrix4f;

import jet.opengl.demos.scene.BaseScene;
import jet.opengl.postprocessing.texture.Texture2D;

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

    private int m_ResolvedShadowFBO;  // Used when the source shadow has multi-samples.
    private Texture2D m_ResolvedShadowMap;
    private final ShadowMapParams m_ShadowMapParams = new ShadowMapParams();

    @Override
    protected final void onRender(boolean clearFBO) {
        getShadowCasterBoundingBox(mCurrentShadowCasterBoudingBox);

        boolean onlyClearShadowMap = false;
        if(!mCurrentShadowCasterBoudingBox.valid()){
            // no objects cast the shadow
            onlyClearShadowMap = true;
//            mDirtyFlags |= DIRTY_SHADOW_CONSTRUCTION;
        }else if (!mPreviousShadowCasterBoudingBox.valid() || !mPreviousShadowCasterBoudingBox.equals(mCurrentShadowCasterBoudingBox)){
            mDirtyFlags |= DIRTY_SHADOW_CONSTRUCTION;
        }

        mPreviousShadowCasterBoudingBox.set(mCurrentShadowCasterBoudingBox);

        if(onlyClearShadowMap){
        }else if((mDirtyFlags & DIRTY_SHADOW_CONSTRUCTION) != 0){

        }
    }

    private void clearShadowMap(){

    }

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
                mDirtyFlags |= DIRTY_SHADOW_CONSTRUCTION;
            }

            if(!config.lightPos.equals(shadowConfig.lightPos)){
                mDirtyFlags |= DIRTY_SHADOW_CONSTRUCTION;
            }

            if(!equals(config.shadowMapSplitting, shadowConfig.shadowMapSplitting)){
                mDirtyFlags |= DIRTY_SHADOW_CONSTRUCTION;
            }

            int cascadCount;
            if(equals(config.shadowMapSplitting, ShadowMapSplitting.CSM) && config.cascadCount != shadowConfig.cascadCount){
                mDirtyFlags |= DIRTY_SHADOW_CONSTRUCTION;

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

    protected abstract void onShadowRender(Matrix4f lightViewProj);
    protected abstract void onSceneRender(boolean clearFBO);

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


    protected abstract void getShadowCasterBoundingBox(BoundingBox boundingBox);

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
