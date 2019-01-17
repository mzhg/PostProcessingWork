package jet.opengl.demos.gpupro.glitter;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;

final class BlurBuffer {
    static final int mWidth = 1280;
    static final int mHeight = 720;

    //frame buffer
    private int FBO;
    //output texture
    private int bufferBlurColor;

    private GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
    BlurBuffer() {
        FBO = gl.glGenFramebuffer();
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, FBO);
        bufferBlurColor = gl.glGenTexture();
        gl.glBindTexture(GLenum.GL_TEXTURE_2D, bufferBlurColor);
        gl.glTexImage2D(GLenum.GL_TEXTURE_2D, 0, GLenum.GL_RGB8, mWidth, mHeight, 0, GLenum.GL_RGB, GLenum.GL_FLOAT, null);
        gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_NEAREST);
        gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_NEAREST);
        gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_COLOR_ATTACHMENT0, GLenum.GL_TEXTURE_2D, bufferBlurColor, 0);
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
    }

    void BindFramebuffer() {
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, FBO);
    }


    void BindBuffersLighting(GLSLProgram lightingShader, GBuffer gbuffer) {
        //bind necessary textures from gbuffer
        gbuffer.BindBuffersLighting(lightingShader);
        //as well as output from SSAO shader
        gl.glActiveTexture(GLenum.GL_TEXTURE6);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D, bufferBlurColor);
        gl.glUniform1i(gl.glGetUniformLocation(lightingShader.getProgram(), "bufferOcclusion"), 6);
    }

    void BindBuffersRadiosity(GLSLProgram blurRadiosityShader) {
        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D, bufferBlurColor);
        gl.glUniform1i(gl.glGetUniformLocation(blurRadiosityShader.getProgram(), "bufferRadiosity"), 0);
    }
}
