package jet.opengl.demos.scene;

import jet.opengl.postprocessing.common.GLenum;

/**
 * Created by mazhen'gui on 2017/10/9.
 */

public class SceneConfig {
    public int downsampleScale = 1;
    public int colorFormat = GLenum.GL_RGBA8;
    public int depthStencilFormat = GLenum.GL_DEPTH_COMPONENT24;
    public int sampleCount = 1;
    public boolean noFBO = false;

    public void set(SceneConfig other){
        downsampleScale = other.downsampleScale;
        colorFormat = other.colorFormat;
        depthStencilFormat = other.depthStencilFormat;
        sampleCount = other.sampleCount;
        noFBO = other.noFBO;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SceneConfig that = (SceneConfig) o;

        if (downsampleScale != that.downsampleScale) return false;
        if (colorFormat != that.colorFormat) return false;
        if (depthStencilFormat != that.depthStencilFormat) return false;
        if (sampleCount != that.sampleCount) return false;
        return noFBO == that.noFBO;

    }

    @Override
    public int hashCode() {
        int result = downsampleScale;
        result = 31 * result + colorFormat;
        result = 31 * result + depthStencilFormat;
        result = 31 * result + sampleCount;
        result = 31 * result + (noFBO ? 1 : 0);
        return result;
    }
}
