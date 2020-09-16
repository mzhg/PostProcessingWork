package jet.opengl.demos.labs.atmosphere;

import com.nvidia.developer.opengl.app.NvCameraMotionType;
import com.nvidia.developer.opengl.app.NvSampleApp;
import com.nvidia.developer.opengl.models.GLVAO;

import org.lwjgl.util.vector.Vector2f;

import java.io.IOException;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.TextureUtils;

public class VolumetricClouds extends NvSampleApp {

    int mQuadVAO;
    GLSLProgram mProgram;

    Texture2D mLowfreqTexture;
    Texture2D mHighFreqTexture;
    Texture2D mWeatherTexture;
    Texture2D mCurlNoiseTexture;

    private GLFuncProvider gl;

    @Override
    protected void initRendering() {
        final String shaderPath = "labs/VolumetricClouds/shaders/";
        final String kVertexShader = "shader_libs/PostProcessingDefaultScreenSpaceVS.vert";
        mProgram = GLSLProgram.createProgram(kVertexShader, shaderPath+"RayMarching.frag", null);

        final String resourcePath = "labs/VolumetricClouds/textures/";
        try {
            mLowfreqTexture = TextureUtils.createTexture2DFromFile(resourcePath + "LowFrequency3DTexture.tga", false);
            mHighFreqTexture = TextureUtils.createTexture2DFromFile(resourcePath + "HighFrequency3DTexture.tga", false);
            mWeatherTexture = TextureUtils.createTexture2DFromFile(resourcePath + "weathermap.png", false);
            mCurlNoiseTexture = TextureUtils.createTexture2DFromFile(resourcePath + "curlNoise.png", false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        m_transformer.setMotionMode(NvCameraMotionType.FIRST_PERSON);
        m_transformer.setTranslation(0, -4, 0);

        gl = GLFuncProviderFactory.getGLFuncProvider();
        mQuadVAO = gl.glGenVertexArray();
    }

    @Override
    public void display() {
        gl.glViewport(0,0, getGLContext().width(), getGLContext().height());

        gl.glBindFramebuffer(GLenum.GL_DRAW_FRAMEBUFFER, 0);
        gl.glClearDepthf(1.0f);
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT|GLenum.GL_DEPTH_BUFFER_BIT);
        gl.glDisable(GLenum.GL_DEPTH_TEST);

        mProgram.enable();
        //todo binding shader resources.

        gl.glBindVertexArray(0);
        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
        gl.glBindVertexArray(0);

        mProgram.printOnce();
    }

    /*
     * @ Halton computing
     */
    float HaltonSequenceAt(int index, int base)
    {
        float f = 1.0f;
        float r = 0.0f;

        while (index > 0)
        {
            f = f / base;
            r += f*(index%base);
            index = (int)(Math.floor(index / base));
        }

        return r;
    }

    void ComputeHaltonVectors(Vector2f haltonSeq){
        haltonSeq.x = HaltonSequenceAt(1, 3);
        haltonSeq.y = HaltonSequenceAt(5, 3);
    }
}
