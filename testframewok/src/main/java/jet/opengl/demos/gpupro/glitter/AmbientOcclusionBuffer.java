package jet.opengl.demos.gpupro.glitter;

import org.lwjgl.util.vector.Vector3f;

import java.nio.FloatBuffer;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.Numeric;

final class AmbientOcclusionBuffer {
    static final int mWidth = 1280;
    static final int mHeight = 720;

    static final float mNear = 0.1f;
    static final float mFar = 1000.f;
    static final int SSAO_NUM_SAMPLES = 32;
    //for SSAO
    private Vector3f[] samples = new Vector3f[SSAO_NUM_SAMPLES];
    private int noiseTexture;
    private int FBO;
    private int bufferSSAO;
    private GLFuncProvider gl;

    AmbientOcclusionBuffer() {
        gl = GLFuncProviderFactory.getGLFuncProvider();
        //create samples
        for (int i = 0; i < SSAO_NUM_SAMPLES; i++) {
            //direction
            //x & y go from -1 to 1
            float x = Numeric.random(-1, 1);
            float y = Numeric.random(-1, 1);
            //z goes from 0 to 1 (hemisphere)
            float z = Numeric.random(0, 1);

            //normalize to clamp to edge of hemisphere
            Vector3f sample = new Vector3f(x, y, z);
            sample.normalise();
            //scale from 0 to 1(magnitude)
            sample.scale(Numeric.random());

            //scale to be more distrubuted around the origin
            float scale = i / (float)SSAO_NUM_SAMPLES;
            scale = 0.1f + (scale * scale) * (1.0f - 0.1f);
            sample.scale(scale);
            samples[i] = sample;
        }

        //create noise
        FloatBuffer noise = CacheBuffer.getCachedFloatBuffer(16 * 2);
        for (int i = 0; i < 16; i++) {
//            glm::vec3 n((rand() / (float)RAND_MAX) * 2.0 - 1.0, (rand() / (float)RAND_MAX) * 2.0 - 1.0, 0.0f);
            float x = Numeric.random(-1, 1);
            float y = Numeric.random(-1, 1);
            noise.put(x).put(y);
        }
        noise.flip();
        //put into 4x4 texture
        noiseTexture = gl.glGenTexture();
        gl.glBindTexture(GLenum.GL_TEXTURE_2D, noiseTexture);
        gl.glTexImage2D(GLenum.GL_TEXTURE_2D, 0, GLenum.GL_RG16F, 4, 4, 0, GLenum.GL_RG, GLenum.GL_FLOAT, noise);
        gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_NEAREST);
        gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_NEAREST);
        gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_REPEAT);
        gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_REPEAT);

        //create fbo to store results of ssao stage
        FBO = gl.glGenFramebuffer();
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, FBO);
        bufferSSAO = gl.glGenTexture();
        gl.glBindTexture(GLenum.GL_TEXTURE_2D, bufferSSAO);
        gl.glTexImage2D(GLenum.GL_TEXTURE_2D, 0, GLenum.GL_RGB8, mWidth, mHeight, 0, GLenum.GL_RGB, GLenum.GL_FLOAT, null);
        gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_NEAREST);
        gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_NEAREST);
        gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_COLOR_ATTACHMENT0, GLenum.GL_TEXTURE_2D, bufferSSAO, 0);
    }

    void BindFramebuffer() {
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, FBO);
    }

    void BindBuffersSSAO(GLSLProgram ssaoShader, GBuffer gbuffer) {
        //bind necessary textures from gbuffer
        gbuffer.BindBuffersSSAO(ssaoShader);
        //as well as noise texture
        gl.glActiveTexture(GLenum.GL_TEXTURE2);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D, noiseTexture);
        gl.glUniform1i(gl.glGetUniformLocation(ssaoShader.getProgram(), "texNoise"), 2);
    }

    void BindBuffersBlur(GLSLProgram blurShader) {
        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D, bufferSSAO);
        gl.glUniform1i(gl.glGetUniformLocation(blurShader.getProgram(), "bufferInput"), 0);
    }

    void SetUniforms(GLSLProgram ssaoShader) {
        //samples
//        for (int i = 0; i < SSAO_NUM_SAMPLES; i++)
//            gl.glUniform3f(glGetUniformLocation(ssaoShader.Program, ("samples[" + std::to_string(i) + "]").c_str()), 1, &samples[i][0]);

        int loc = gl.glGetUniformLocation(ssaoShader.getProgram(), "samples");
        gl.glUniform3fv(loc, CacheBuffer.wrap(samples));
    }
}
