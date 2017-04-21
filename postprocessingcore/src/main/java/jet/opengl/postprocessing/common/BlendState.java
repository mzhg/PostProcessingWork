package jet.opengl.postprocessing.common;

/**
 * Created by mazhen'gui on 2017/4/20.
 */

public class BlendState {

    /**Readonly value, don't modify it. */
    public static final BlendState g_DefaultBlendState = new BlendState();

    public boolean blendEnable = false;
    public int srcBlend = GLenum.GL_ONE;
    public int destBlend = GLenum.GL_ZERO;
    public int   blendOp = GLenum.GL_FUNC_ADD;
    public int srcBlendAlpha = GLenum.GL_ONE;
    public int destBlendAlpha = GLenum.GL_ZERO;
    public int blendOpAlpha = GLenum.GL_FUNC_ADD;

    public boolean equals(BlendState that){
        if (blendEnable != that.blendEnable) return false;
        if (srcBlend != that.srcBlend) return false;
        if (destBlend != that.destBlend) return false;
        if (blendOp != that.blendOp) return false;
        if (srcBlendAlpha != that.srcBlendAlpha) return false;
        if (destBlendAlpha != that.destBlendAlpha) return false;
        return blendOpAlpha == that.blendOpAlpha;
    }

    public void set(BlendState o){
        blendEnable = o.blendEnable;
        srcBlend = o.srcBlend;
        destBlend = o.destBlend;
        blendOp = o.blendOp;
        srcBlendAlpha = o.srcBlendAlpha;
        destBlendAlpha = o.destBlendAlpha;
        blendOpAlpha = o.blendOpAlpha;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BlendState that = (BlendState) o;

        return equals(that);
    }

    @Override
    public int hashCode() {
        int result = (blendEnable ? 1 : 0);
        result = 31 * result + srcBlend;
        result = 31 * result + destBlend;
        result = 31 * result + blendOp;
        result = 31 * result + srcBlendAlpha;
        result = 31 * result + destBlendAlpha;
        result = 31 * result + blendOpAlpha;
        return result;
    }
}
