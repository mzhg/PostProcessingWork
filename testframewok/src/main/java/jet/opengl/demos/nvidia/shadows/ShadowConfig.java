package jet.opengl.demos.nvidia.shadows;

import org.lwjgl.util.vector.Vector3f;

import jet.opengl.postprocessing.common.GLenum;

/**
 * Created by mazhen'gui on 2017/11/3.
 */

public class ShadowConfig {
    public int shadowMapFormat = GLenum.GL_DEPTH_COMPONENT16;
    public int shadowMapSize = 1024; // a Square
    public int shadowMapSampleCount = 1;
    public ShadowScene.LightType lightType = ShadowScene.LightType.DIRECTION;
    public final Vector3f lightPos = new Vector3f();
    public final Vector3f lightDir = new Vector3f();
    public ShadowScene.ShadowType shadowType = ShadowScene.ShadowType.SHADOW_MAPPING;
    public ShadowScene.ShadowMapSplitting shadowMapSplitting = ShadowScene.ShadowMapSplitting.NONE;
    public ShadowScene.ShadowMapWarping shadowMapWarping = ShadowScene.ShadowMapWarping.NONE;
    public ShadowScene.ShadowMapFiltering shadowMapFiltering = ShadowScene.ShadowMapFiltering.NONE;
    public int cascadCount = 1;

    public void set(ShadowConfig ohs){
        shadowMapFormat = ohs.shadowMapFormat;
        shadowMapSize = ohs.shadowMapSize;
        shadowMapSampleCount = ohs.shadowMapSampleCount;
        lightType = ohs.lightType;
        shadowType = ohs.shadowType;
        shadowMapSplitting = ohs.shadowMapSplitting;
        shadowMapWarping = ohs.shadowMapWarping;
        shadowMapFiltering = ohs.shadowMapFiltering;
        cascadCount = ohs.cascadCount;

        lightPos.set(ohs.lightPos);
        lightDir.set(ohs.lightDir);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ShadowConfig that = (ShadowConfig) o;

        if (shadowMapFormat != that.shadowMapFormat) return false;
        if (shadowMapSize != that.shadowMapSize) return false;
        if (shadowMapSampleCount != that.shadowMapSampleCount) return false;
        if (cascadCount != that.cascadCount) return false;
        if (lightType != that.lightType) return false;
        if (!lightPos.equals(that.lightPos))
            return false;
        if (!lightDir.equals(that.lightDir))
            return false;
        if (shadowType != that.shadowType) return false;
        if (shadowMapSplitting != that.shadowMapSplitting) return false;
        if (shadowMapWarping != that.shadowMapWarping) return false;
        return shadowMapFiltering == that.shadowMapFiltering;
    }

    @Override
    public int hashCode() {
        int result = shadowMapFormat;
        result = 31 * result + shadowMapSize;
        result = 31 * result + shadowMapSampleCount;
        result = 31 * result + (lightType != null ? lightType.hashCode() : 0);
        result = 31 * result + (lightPos != null ? lightPos.hashCode() : 0);
        result = 31 * result + (lightDir != null ? lightDir.hashCode() : 0);
        result = 31 * result + (shadowType != null ? shadowType.hashCode() : 0);
        result = 31 * result + (shadowMapSplitting != null ? shadowMapSplitting.hashCode() : 0);
        result = 31 * result + (shadowMapWarping != null ? shadowMapWarping.hashCode() : 0);
        result = 31 * result + (shadowMapFiltering != null ? shadowMapFiltering.hashCode() : 0);
        result = 31 * result + cascadCount;
        return result;
    }
}
