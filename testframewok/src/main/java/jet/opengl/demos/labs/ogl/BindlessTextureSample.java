package jet.opengl.demos.labs.ogl;

import com.nvidia.developer.opengl.app.NvSampleApp;

import java.io.IOException;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.GLSLUtil;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.TextureUtils;

public class BindlessTextureSample extends NvSampleApp {

    private GLFuncProvider gl;
    private GLSLProgram mBlitProg;
    private Texture2D mImage;
    private Texture2D mImage1;

    @Override
    protected void initRendering() {
        gl = GLFuncProviderFactory.getGLFuncProvider();

        try {
            mImage = TextureUtils.createTexture2DFromFile("labs/AtmosphereTest/textures/earthmap1k.jpg", false);
            mImage1 = TextureUtils.createTexture2DFromFile("labs/Chapman/textures/oceanmask.jpg", false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mBlitProg = GLSLProgram.createProgram("shader_libs/PostProcessingDefaultScreenSpaceVS.vert", "labs/bindless/shaders/bindlessTexture.frag", null);
    }

    @Override
    public void display() {
        gl.glDisable(GLenum.GL_DEPTH_TEST);
        gl.glClearColor(0,0,0,0);
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT);

        gl.glBindTextureUnit(0, mImage.getTexture());
        gl.glBindTextureUnit(1, mImage1.getTexture());

        mBlitProg.enable();
        GLSLUtil.setInt(mBlitProg, "texture[0].sampler", 0);
        GLSLUtil.setInt(mBlitProg, "texture[1].sampler", 1);

        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
    }

    @Override
    protected void reshape(int width, int height) {
        if(width <= 0 || height <=0)
            return;

        gl.glViewport(0,0,width, height);
    }
}
