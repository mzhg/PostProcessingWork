package jet.opengl.demos.labs.atmosphere;

import com.nvidia.developer.opengl.app.NvCameraMotionType;
import com.nvidia.developer.opengl.app.NvSampleApp;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.io.IOException;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.GLSLUtil;
import jet.opengl.postprocessing.texture.ImageData;
import jet.opengl.postprocessing.texture.NativeAPI;
import jet.opengl.postprocessing.texture.SamplerDesc;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture3D;
import jet.opengl.postprocessing.texture.Texture3DDesc;
import jet.opengl.postprocessing.texture.TextureDataDesc;
import jet.opengl.postprocessing.texture.TextureUtils;

public class VolumetricClouds extends NvSampleApp {

    int mQuadVAO;
    GLSLProgram mProgram;

    Texture3D mLowfreqTexture;
    Texture3D mHighFreqTexture;
    Texture2D mWeatherTexture;
    Texture2D mCurlNoiseTexture;

    private int linearSampler;
    private GLFuncProvider gl;

    private final Matrix4f mView = new Matrix4f();
    private final Vector3f cameraFront = new Vector3f();
    private final Vector3f cameraPosition = new Vector3f();
    private final Vector3f cameraRight = new Vector3f();
    private final Vector3f cameraUp = new Vector3f();
    private final Vector2f HaltonSequence = new Vector2f();
    private final Vector3f earthCenter = new Vector3f();

    @Override
    protected void initRendering() {
        gl = GLFuncProviderFactory.getGLFuncProvider();

        final String shaderPath = "labs/VolumetricClouds/shaders/";
        final String kVertexShader = "shader_libs/PostProcessingDefaultScreenSpaceVS.vert";
        mProgram = GLSLProgram.createProgram(kVertexShader, shaderPath+"RayMarching.frag", null);

        final String resourcePath = "labs/VolumetricClouds/textures/";
        try {
            mLowfreqTexture = loadTexture3D(resourcePath + "LowFrequency3DTexture.tga");
            mHighFreqTexture = loadTexture3D(resourcePath + "HighFrequency3DTexture.tga");
            mWeatherTexture = TextureUtils.createTexture2DFromFile(resourcePath + "weathermap.png", false);
            mCurlNoiseTexture = TextureUtils.createTexture2DFromFile(resourcePath + "curlNoise.png", false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        m_transformer.setMotionMode(NvCameraMotionType.FIRST_PERSON);
        m_transformer.setTranslation(0, -4, 0);

        mQuadVAO = gl.glGenVertexArray();
        SamplerDesc samplerDesc = new SamplerDesc();
        samplerDesc.minFilter = samplerDesc.magFilter = GLenum.GL_LINEAR;
        samplerDesc.wrapS = samplerDesc.wrapT = samplerDesc.wrapR = GLenum.GL_REPEAT;
        linearSampler = SamplerUtils.createSampler(samplerDesc);
    }

    @Override
    public void display() {
        gl.glViewport(0,0, getGLContext().width(), getGLContext().height());

        gl.glBindFramebuffer(GLenum.GL_DRAW_FRAMEBUFFER, 0);
        gl.glClearDepthf(1.0f);
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT|GLenum.GL_DEPTH_BUFFER_BIT);
        gl.glDisable(GLenum.GL_DEPTH_TEST);

        m_transformer.getModelViewMat(mView);
        Matrix4f.decompseRigidMatrix(mView, cameraPosition, cameraRight, cameraUp, cameraFront);
        cameraFront.scale(-1);

        earthCenter.set(0.0f, 6378000.0f, 0.0f);
        Vector3f.sub(cameraPosition, earthCenter, earthCenter);

        ComputeHaltonVectors(HaltonSequence);

        mProgram.enable();

        GLSLUtil.setFloat(mProgram, "screenWidth", getGLContext().width());
        GLSLUtil.setFloat(mProgram, "screenHeight", getGLContext().height());
        GLSLUtil.setFloat(mProgram, "Time", getTotalTime());
        GLSLUtil.setFloat3(mProgram, "cameraPosition", cameraPosition);
        GLSLUtil.setFloat3(mProgram, "cameraRight", cameraRight);
        GLSLUtil.setFloat3(mProgram, "cameraUp", cameraUp);
        GLSLUtil.setFloat3(mProgram, "cameraFront", cameraFront);
        GLSLUtil.setFloat3(mProgram, "EarthCenter", earthCenter);
        GLSLUtil.setFloat2(mProgram, "HaltonSequence", HaltonSequence);

//        layout(binding = 0) uniform sampler3D lowFrequencyTexture;
//        layout(binding = 1) uniform sampler3D highFrequencyTexture;
//        layout(binding = 2) uniform sampler2D WeatherTexture;
//        layout(binding = 3) uniform sampler2D CurlNoiseTexture;
        gl.glBindTextureUnit(0, mLowfreqTexture.getTexture());
        gl.glBindTextureUnit(1, mHighFreqTexture.getTexture());
        gl.glBindTextureUnit(2, mWeatherTexture.getTexture());
        gl.glBindTextureUnit(3, mCurlNoiseTexture.getTexture());

        gl.glBindSampler(0, linearSampler);
        gl.glBindSampler(1, linearSampler);
        gl.glBindSampler(2, linearSampler);
        gl.glBindSampler(3, linearSampler);

        gl.glBindVertexArray(mQuadVAO);
        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
        gl.glBindVertexArray(0);

        mProgram.printOnce();

        gl.glBindTextureUnit(0, 0);
        gl.glBindTextureUnit(1, 0);
        gl.glBindTextureUnit(2, 0);
        gl.glBindTextureUnit(3, 0);

        gl.glBindSampler(0, 0);
        gl.glBindSampler(1, 0);
        gl.glBindSampler(2, 0);
        gl.glBindSampler(3, 0);
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

    private Texture3D loadTexture3D(String filename){
        NativeAPI loader = gl.getNativeAPI();
        ImageData data = null;
        try {
            data = loader.load(filename, false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Texture3DDesc desc = new Texture3DDesc(data.width, data.width, data.width, 1, data.internalFormat);
        TextureDataDesc initData = new TextureDataDesc(TextureUtils.measureFormat(data.internalFormat), TextureUtils.measureDataType(data.internalFormat),
                data.pixels);

        return TextureUtils.createTexture3D(desc, initData);
    }
}
