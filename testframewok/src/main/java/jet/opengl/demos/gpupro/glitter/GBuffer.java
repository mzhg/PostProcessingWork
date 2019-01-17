package jet.opengl.demos.gpupro.glitter;

import java.nio.IntBuffer;

import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.util.CacheBuffer;

//G-Buffer for deferred shading
final class GBuffer {
    static final int mWidth = 1280;
    static final int mHeight = 720;

    static final int GBUFFER_LAYERS = 2;

    //frame buffer object to render geometry to
    private int FBO;
    //buffer attachment textures where data is stored
    private int bufferPosition;
    private int bufferNormal;
    private int bufferColor;
    //GLuint bufferLambertian;
    //GLuint bufferSpecular;
    private int bufferDepth;

    //previous frame's first layer depth buffer
    private int bufferDepthCompare;

    private GLFuncProvider gl;

    GBuffer() {
        gl = GLFuncProviderFactory.getGLFuncProvider();
        //create framebuffer
        FBO = gl.glGenFramebuffer();
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, FBO);

        //create textures for each layer
        //position buffer - a "color" buffer
        bufferPosition = gl.glGenTexture();
        gl.glBindTexture(GLenum.GL_TEXTURE_2D_ARRAY, bufferPosition);
        gl.glTexImage3D(GLenum.GL_TEXTURE_2D_ARRAY, 0, GLenum.GL_RGB16F, mWidth, mHeight, GBUFFER_LAYERS, 0, GLenum.GL_RGB, GLenum.GL_FLOAT, null);
        gl.glTexParameteri(GLenum.GL_TEXTURE_2D_ARRAY, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_NEAREST);
        gl.glTexParameteri(GLenum.GL_TEXTURE_2D_ARRAY, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_NEAREST);
        gl.glTexParameteri(GLenum.GL_TEXTURE_2D_ARRAY, GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GLenum.GL_TEXTURE_2D_ARRAY, GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_CLAMP_TO_EDGE);
        gl.glFramebufferTexture(GLenum.GL_FRAMEBUFFER, GLenum.GL_COLOR_ATTACHMENT0, bufferPosition, 0);

        //normal buffer - a "color" buffer, use rgb for xyz
        bufferNormal = gl.glGenTexture();
        gl.glBindTexture(GLenum.GL_TEXTURE_2D_ARRAY, bufferNormal);
        gl.glTexImage3D(GLenum.GL_TEXTURE_2D_ARRAY, 0, GLenum.GL_RGB16F, mWidth, mHeight, GBUFFER_LAYERS, 0, GLenum.GL_RGB, GLenum.GL_FLOAT, null);
        gl.glTexParameteri(GLenum.GL_TEXTURE_2D_ARRAY, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_NEAREST);
        gl.glTexParameteri(GLenum.GL_TEXTURE_2D_ARRAY, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_NEAREST);
        gl.glFramebufferTexture(GLenum.GL_FRAMEBUFFER, GLenum.GL_COLOR_ATTACHMENT1, bufferNormal, 0);

        //color buffer - put diffuse in rgb, specular in a
        bufferColor = gl.glGenTexture();
        gl.glBindTexture(GLenum.GL_TEXTURE_2D_ARRAY, bufferColor);
        gl.glTexImage3D(GLenum.GL_TEXTURE_2D_ARRAY, 0, GLenum.GL_RGBA8, mWidth, mHeight, GBUFFER_LAYERS, 0, GLenum.GL_RGBA, GLenum.GL_UNSIGNED_BYTE, null);
        gl.glTexParameteri(GLenum.GL_TEXTURE_2D_ARRAY, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_NEAREST);
        gl.glTexParameteri(GLenum.GL_TEXTURE_2D_ARRAY, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_NEAREST);
        gl.glFramebufferTexture(GLenum.GL_FRAMEBUFFER, GLenum.GL_COLOR_ATTACHMENT2, bufferColor, 0);

        //set as attachments to FBO
//        GLuint attachments[3] = { GLenum.GL_COLOR_ATTACHMENT0, GLenum.GL_COLOR_ATTACHMENT1, GLenum.GL_COLOR_ATTACHMENT2 };
        IntBuffer attachments = CacheBuffer.getCachedIntBuffer(3);
        attachments.put(GLenum.GL_COLOR_ATTACHMENT0);
        attachments.put(GLenum.GL_COLOR_ATTACHMENT1);
        attachments.put(GLenum.GL_COLOR_ATTACHMENT2);
        attachments.flip();
        gl.glDrawBuffers(attachments);

        //add depth buffer
        //depth buffer has an extra layer, at bufferDepth[GBUFFER_LAYERS] which is used for comparison
        bufferDepth = gl.glGenTexture();
        gl.glBindTexture(GLenum.GL_TEXTURE_2D_ARRAY, bufferDepth);
        //glTexStorage3D(GL_TEXTURE_2D_ARRAY, 0, GL_DEPTH_COMPONENT24, mWidth, mHeight, GBUFFER_LAYERS + 1);
        //glTexSubImage3D(GL_TEXTURE_2D_ARRAY, 0, 0, 0, 0, mWidth, mHeight, GBUFFER_LAYERS + 1, GL_DEPTH_COMPONENT, GL_FLOAT, NULL);
        gl.glTexImage3D(GLenum.GL_TEXTURE_2D_ARRAY, 0, GLenum.GL_DEPTH_COMPONENT24, mWidth, mHeight, GBUFFER_LAYERS, 0, GLenum.GL_DEPTH_COMPONENT, GLenum.GL_FLOAT, null);
        gl.glTexParameteri(GLenum.GL_TEXTURE_2D_ARRAY, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_LINEAR);
        gl.glTexParameteri(GLenum.GL_TEXTURE_2D_ARRAY, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_LINEAR);
        gl.glTexParameteri(GLenum.GL_TEXTURE_2D_ARRAY, GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GLenum.GL_TEXTURE_2D_ARRAY, GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_CLAMP_TO_EDGE);
        //glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_COMPARE_FUNC, GL_LEQUAL);
        gl.glFramebufferTexture(GLenum.GL_FRAMEBUFFER, GLenum.GL_DEPTH_ATTACHMENT, bufferDepth, 0);

        //make sure framebuffer was built successfully
//        if (gl.glCheckFramebufferStatus(GLenum.GL_FRAMEBUFFER) != GLenum.GL_FRAMEBUFFER_COMPLETE)
//            std::cout << "Error setting up G-Buffer " << glGetError() << std::endl;

        GLCheck.checkFramebufferStatus();

        //unbind FBO
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);

        //create comparison depth buffer
        bufferDepthCompare = gl.glGenTexture();
        gl.glBindTexture(GLenum.GL_TEXTURE_2D_ARRAY, bufferDepthCompare);
        gl.glTexImage3D(GLenum.GL_TEXTURE_2D_ARRAY, 0, GLenum.GL_DEPTH_COMPONENT24, mWidth, mHeight, GBUFFER_LAYERS - 1, 0, GLenum.GL_DEPTH_COMPONENT, GLenum.GL_FLOAT, null);
        gl.glTexParameteri(GLenum.GL_TEXTURE_2D_ARRAY, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_LINEAR);
        gl.glTexParameteri(GLenum.GL_TEXTURE_2D_ARRAY, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_LINEAR);
        gl.glTexParameteri(GLenum.GL_TEXTURE_2D_ARRAY, GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GLenum.GL_TEXTURE_2D_ARRAY, GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_CLAMP_TO_EDGE);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D_ARRAY, 0);
    }

    void BindFramebuffer() {
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, FBO);
    }

    void CopyAndBindDepthCompareLayer(GLSLProgram geometryShader) {
        //copy first layer of last frame's depth buffer into comparison texture and bind it
        gl.glCopyImageSubData(bufferDepth, GLenum.GL_TEXTURE_2D_ARRAY, 0, 0, 0, 0, bufferDepthCompare, GLenum.GL_TEXTURE_2D_ARRAY, 0, 0, 0, 0, mWidth, mHeight, GBUFFER_LAYERS - 1);

        //and bind to texture unit 0
        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D_ARRAY, bufferDepthCompare);
        gl.glUniform1i(gl.glGetUniformLocation(geometryShader.getProgram(), "bufferDepthCompare"), 0);
    }

    void BindBuffersSSAO(GLSLProgram ssaoShader) {
        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D_ARRAY, bufferPosition);
        gl.glActiveTexture(GLenum.GL_TEXTURE1);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D_ARRAY, bufferNormal);

        gl.glUniform1i(gl.glGetUniformLocation(ssaoShader.getProgram(), "bufferPosition"), 0);
        gl.glUniform1i(gl.glGetUniformLocation(ssaoShader.getProgram(), "bufferNormal"), 1);
    }

    void BindBuffersLighting(GLSLProgram lightingShader) {
        gl.glActiveTexture(GLenum.GL_TEXTURE3);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D_ARRAY, bufferPosition);
        gl.glActiveTexture(GLenum.GL_TEXTURE4);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D_ARRAY, bufferNormal);
        gl.glActiveTexture(GLenum.GL_TEXTURE5);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D_ARRAY, bufferColor);

        gl.glUniform1i(gl.glGetUniformLocation(lightingShader.getProgram(), "bufferPosition"), 3);
        gl.glUniform1i(gl.glGetUniformLocation(lightingShader.getProgram(), "bufferNormal"), 4);
        gl.glUniform1i(gl.glGetUniformLocation(lightingShader.getProgram(), "bufferColor"), 5);
    }

    void BindBuffersRadiosity(GLSLProgram radiosityShader) {
        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D_ARRAY, bufferPosition);
        gl.glActiveTexture(GLenum.GL_TEXTURE1);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D_ARRAY, bufferNormal);
        //glActiveTexture(GL_TEXTURE3);
        //glBindTexture(GL_TEXTURE_2D_ARRAY, bufferColor);

        gl.glUniform1i(gl.glGetUniformLocation(radiosityShader.getProgram(), "bufferPosition"), 0);
        gl.glUniform1i(gl.glGetUniformLocation(radiosityShader.getProgram(), "bufferNormal"), 1);
        //glUniform1i(glGetUniformLocation(radiosityShader.Program, "bufferRadiosity"), 3);
    }

    // Copies depth buffer from G-Buffer to standard framebuffer 0
    void CopyDepthBuffer() {
        gl.glBindFramebuffer(GLenum.GL_READ_FRAMEBUFFER, FBO);
        gl.glBindFramebuffer(GLenum.GL_DRAW_FRAMEBUFFER, 0);
        gl.glBlitFramebuffer(0, 0, mWidth, mHeight, 0, 0, mWidth, mHeight, GLenum.GL_DEPTH_BUFFER_BIT, GLenum.GL_NEAREST);
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
    }
}
