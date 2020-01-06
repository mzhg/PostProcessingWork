package jet.opengl.demos.gpupro.culling;

import org.lwjgl.util.vector.Vector4f;

import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.GLSLUtil;
import jet.opengl.postprocessing.texture.Texture2D;

final class Material {
    Vector4f mColor = new Vector4f();
    Texture2D mDiffuse;

    boolean mTransparency = false;

    void apply(GLSLProgram prog){
        if(mDiffuse != null){
            GLFuncProviderFactory.getGLFuncProvider().glBindTextureUnit(0, mDiffuse.getTexture());
            mColor.w = 0;
        }

        GLSLUtil.setFloat4(prog, "gColor", mColor);
    }
}
