package jet.opengl.demos.gpupro.glitter;

import org.lwjgl.util.vector.Vector3f;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.Numeric;

final class RadiosityBuffer {
    static final int RADIOSITY_NUM_SAMPLES = 32;

    //frame buffer
    private int FBO;
    //inputs
    private int bufferRadiosity;
    private int bufferColor;
    //outputs
    //GLuint bufferBounceRadiosityOut;

    //samples
    private Vector3f[] samples = new Vector3f[RADIOSITY_NUM_SAMPLES];
    private int noiseTexture;

    private final GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();

    RadiosityBuffer() {
        //create samples
        for (int i = 0; i < RADIOSITY_NUM_SAMPLES; i++) {
            //direction
            //x & y go from -1 to 1
            float x = Numeric.random(-1,1);
            float y = Numeric.random(-1,1);
            //z goes from 0 to 1 (hemisphere)
            float z = Numeric.random();

            //normalize to clamp to edge of hemisphere
            Vector3f sample = new Vector3f(x, y, z);
            sample.normalise();
            //scale from 0 to 1(magnitude)
//            sample *= (rand() / (float)RAND_MAX);
            sample.scale(Numeric.random());

            //scale to be more distrubuted around the origin
            float scale = i / (float)RADIOSITY_NUM_SAMPLES;
            scale = 0.1f + (scale * scale) * (1.0f - 0.1f);
            sample.scale(scale);
            samples[i]=sample;
        }

        //create noise texture
        /*vector<glm::vec3> noise;
        for (GLuint i = 0; i < 16; i++) {
            glm::vec3 n((rand() / (float)RAND_MAX) * 2.0 - 1.0, (rand() / (float)RAND_MAX) * 2.0 - 1.0, 0.0f);
            noise.push_back(n);
        }*/

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

        //create FBO
        FBO = gl.glGenFramebuffer();
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, FBO);
        //1st output, Lambertian(diffuse) goes into radiosity algorithm
        bufferRadiosity = gl.glGenTexture();
        gl.glBindTexture(GLenum.GL_TEXTURE_2D_ARRAY, bufferRadiosity);
        gl.glTexImage3D(GLenum.GL_TEXTURE_2D_ARRAY, 0, GLenum.GL_RGBA8, GBuffer.mWidth, GBuffer.mHeight, GBuffer.GBUFFER_LAYERS, 0, GLenum.GL_RGBA, GLenum.GL_UNSIGNED_BYTE, null);
        gl.glTexParameteri(GLenum.GL_TEXTURE_2D_ARRAY, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_NEAREST);
        gl.glTexParameteri(GLenum.GL_TEXTURE_2D_ARRAY, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_NEAREST);
        gl.glFramebufferTexture(GLenum.GL_FRAMEBUFFER, GLenum.GL_COLOR_ATTACHMENT1, bufferRadiosity, 0);
        //2nd output, ambient + specular, gets added back later
        bufferColor = gl.glGenTexture();
        gl.glBindTexture(GLenum.GL_TEXTURE_2D_ARRAY, bufferColor);
        gl.glTexImage3D(GLenum.GL_TEXTURE_2D_ARRAY, 0, GLenum.GL_RGBA8, GBuffer.mWidth, GBuffer.mHeight, GBuffer.GBUFFER_LAYERS, 0, GLenum.GL_RGBA, GLenum.GL_UNSIGNED_BYTE, null);
        gl.glTexParameteri(GLenum.GL_TEXTURE_2D_ARRAY, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_NEAREST);
        gl.glTexParameteri(GLenum.GL_TEXTURE_2D_ARRAY, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_NEAREST);
        gl.glFramebufferTexture(GLenum.GL_FRAMEBUFFER, GLenum.GL_COLOR_ATTACHMENT0, bufferColor, 0);

//        GLuint attachments[2] = { GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1 };
        IntBuffer attachments = CacheBuffer.getCachedIntBuffer(2);
        attachments.put(GLenum.GL_COLOR_ATTACHMENT0);
        attachments.put(GLenum.GL_COLOR_ATTACHMENT1);
        attachments.flip();
        gl.glDrawBuffers(attachments);

        //make sure framebuffer was built successfully
        /*if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
            std::cout << "Error setting up Radiosity Buffer " << glGetError() << std::endl;*/
        GLCheck.checkFramebufferStatus();
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
    }

    void BindFramebuffer() {
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, FBO);
    }

    void BindBuffersRadiosity(GLSLProgram radiosityShader, GBuffer gbuffer) {
        gbuffer.BindBuffersRadiosity(radiosityShader);
        gl.glActiveTexture(GLenum.GL_TEXTURE2);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D, noiseTexture);
        gl.glUniform1i(gl.glGetUniformLocation(radiosityShader.getProgram(), "texNoise"), 2);
        gl.glActiveTexture(GLenum.GL_TEXTURE3);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D_ARRAY, bufferRadiosity);
        gl.glUniform1i(gl.glGetUniformLocation(radiosityShader.getProgram(), "bufferRadiosity"), 3);
        //glActiveTexture(GL_TEXTURE4);
        //glBindTexture(GL_TEXTURE_2D_ARRAY, bufferColor);
        //glUniform1i(glGetUniformLocation(radiosityShader.Program, "bufferColor"), 4);
    }

    void BindBuffersBlur(GLSLProgram blurRadiosityShader) {
        gl.glActiveTexture(GLenum.GL_TEXTURE1);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D_ARRAY, bufferColor);
        gl.glUniform1i(gl.glGetUniformLocation(blurRadiosityShader.getProgram(), "bufferColor"), 1);
    }

    void SetUniforms(GLSLProgram ssaoShader) {
        //samples
        /*for (GLuint i = 0; i < RADIOSITY_NUM_SAMPLES; i++)
            glUniform3fv(glGetUniformLocation(ssaoShader.Program, ("samples[" + std::to_string(i) + "]").c_str()), 1, &samples[i][0]);*/

        int loc = gl.glGetUniformLocation(ssaoShader.getProgram(), "samples");
        gl.glUniform3fv(loc, CacheBuffer.wrap(samples));
    }
}
