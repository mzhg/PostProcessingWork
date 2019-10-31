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

    // multi-sample
    public boolean sampleMask;
    public int sampleMaskValue = ~0;

    public void reset(){
        blendEnable = false;
        srcBlend = GLenum.GL_ONE;
        destBlend = GLenum.GL_ZERO;
        blendOp = GLenum.GL_FUNC_ADD;
        srcBlendAlpha = GLenum.GL_ONE;
        destBlendAlpha = GLenum.GL_ZERO;
        blendOpAlpha = GLenum.GL_FUNC_ADD;

        // multi-sample
        sampleMask = false;
        sampleMaskValue = ~0;
    }

    public boolean equals(BlendState that){
        if (blendEnable != that.blendEnable) return false;
        if (srcBlend != that.srcBlend) return false;
        if (destBlend != that.destBlend) return false;
        if (blendOp != that.blendOp) return false;
        if (srcBlendAlpha != that.srcBlendAlpha) return false;
        if (destBlendAlpha != that.destBlendAlpha) return false;
        if (sampleMask != that.sampleMask) return false;
        if (sampleMaskValue != that.sampleMaskValue) return false;
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
        sampleMask = o.sampleMask;
        sampleMaskValue = o.sampleMaskValue;
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
        result = 31 * result + sampleMaskValue;
        result = 31 * result + (sampleMask ? 1: 0);
        return result;
    }

    @Override
    public String toString() {
        return "BlendState{" +
                "blendEnable=" + blendEnable +
                ", srcBlend=" + getBlendFuncName(srcBlend) +
                ", destBlend=" + getBlendFuncName(destBlend) +
                ", blendOp=" + getBlendEquationName(blendOp) +
                ", srcBlendAlpha=" + getBlendFuncName(srcBlendAlpha) +
                ", destBlendAlpha=" + getBlendFuncName(destBlendAlpha) +
                ", blendOpAlpha=" + getBlendEquationName(blendOpAlpha) +
                ", sampleMask=" + sampleMask +
                ", sampleMaskValue=" + sampleMaskValue +
                '}';
    }

    public static String getBlendFuncName(int blend){
        switch(blend){
            case GLenum.GL_ZERO: return "GL_ZERO";
            case GLenum.GL_ONE: return "GL_ONE";
            case GLenum.GL_SRC_COLOR: return "GL_SRC_COLOR";
            case GLenum.GL_ONE_MINUS_SRC_COLOR: return "GL_ONE_MINUS_SRC_COLOR";
            case GLenum.GL_DST_COLOR: return "GL_DST_COLOR";
            case GLenum.GL_ONE_MINUS_DST_COLOR: return "GL_ONE_MINUS_DST_COLOR";
            case GLenum.GL_SRC_ALPHA: return "GL_SRC_ALPHA";
            case GLenum.GL_ONE_MINUS_SRC_ALPHA: return "GL_ONE_MINUS_SRC_ALPHA";
            case GLenum.GL_DST_ALPHA: return "GL_DST_ALPHA";
            case GLenum.GL_ONE_MINUS_DST_ALPHA: return "GL_ONE_MINUS_DST_ALPHA";
            case GLenum.GL_CONSTANT_COLOR: return "GL_CONSTANT_COLOR";
            case GLenum.GL_ONE_MINUS_CONSTANT_COLOR: return "GL_ONE_MINUS_CONSTANT_COLOR";
            case GLenum.GL_CONSTANT_ALPHA: return "GL_CONSTANT_ALPHA";
            case GLenum.GL_ONE_MINUS_CONSTANT_ALPHA: return "GL_ONE_MINUS_CONSTANT_ALPHA";
            default: return "Unkonw(" + Integer.toHexString(blend) + ')';
        }
    }

    public static String getBlendEquationName(int blendEquation){
        switch(blendEquation){
            case GLenum.GL_FUNC_ADD: return "GL_FUNC_ADD";
            case GLenum.GL_FUNC_SUBTRACT: return "GL_FUNC_SUBTRACT";
            case GLenum.GL_FUNC_REVERSE_SUBTRACT: return "GL_FUNC_REVERSE_SUBTRACT";
            case GLenum.GL_MIN: return "GL_MIN";
            case GLenum.GL_MAX: return "GL_MAX";
            default: return "Unkonw(" + Integer.toHexString(blendEquation) + ')';
        }
    }
}
