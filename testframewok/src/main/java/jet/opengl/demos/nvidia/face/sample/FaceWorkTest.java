package jet.opengl.demos.nvidia.face.sample;

import com.nvidia.developer.opengl.app.NvSampleApp;

import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeoutException;

import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.OpenGLProgram;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.FramebufferGL;
import jet.opengl.postprocessing.texture.SamplerDesc;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureAttachDesc;
import jet.opengl.postprocessing.texture.TextureCube;
import jet.opengl.postprocessing.texture.TextureDataDesc;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.DebugTools;

public class FaceWorkTest extends NvSampleApp {

    private Texture2D g_NormalTex;
    private Texture2D g_UVTex;
    private Texture2D g_TangentCur;
    private Texture2D g_Position;
    private Texture2D g_ShadowMap;
    private Texture2D g_vsm;

    private TextureCube g_texCubeDiffuse;
    private TextureCube g_texCubeSpec;

    private Texture2D g_texDiffuse;
    private Texture2D g_texNormal;
    private Texture2D g_texSpec;

    private GbufferProgram m_prog;
    private FramebufferGL m_fbo;

    private GLFuncProvider gl;

    int	m_pSsPointClamp;
    int	m_pSsBilinearClamp;
    int	m_pSsTrilinearRepeat;
    int	m_pSsTrilinearRepeatAniso;
    int	m_pSsPCF;

    @Override
    protected void initRendering() {
        final int width = 1280;
        final int height = 720;

        gl = GLFuncProviderFactory.getGLFuncProvider();
        g_NormalTex = loadTextureBinary("normal.dat", width, height, GLenum.GL_RGBA32F, GLenum.GL_RGBA, GLenum.GL_FLOAT);
        g_UVTex = loadTextureBinary("uv.dat", width, height, GLenum.GL_RG32F, GLenum.GL_RG, GLenum.GL_FLOAT);
        g_TangentCur = loadTextureBinary("TangentCur.dat", width, height, GLenum.GL_RGBA32F, GLenum.GL_RGBA, GLenum.GL_FLOAT);
        g_Position = loadTextureBinary("Position.dat", width, height, GLenum.GL_RGBA32F, GLenum.GL_RGBA, GLenum.GL_FLOAT);
        g_ShadowMap = loadTextureBinary("shadowmap.dat", 1024, 1024, GLenum.GL_R32F, GLenum.GL_RED, GLenum.GL_FLOAT);
        g_vsm = loadTextureBinary("vsm.dat", 1024, 1024, GLenum.GL_RG32F, GLenum.GL_RG, GLenum.GL_FLOAT);

        m_prog = new GbufferProgram();
        m_prog.init();

        createSamplers();

        g_texCubeSpec = g_texCubeDiffuse = CScene.loadCubeTexture("HDREnvironments\\black_cube.dds");
        g_texDiffuse = CScene.loadTexture("DigitalIra\\00_diffuse_albedo.bmp");
        g_texNormal = CScene.loadTexture("DigitalIra\\00_specular_normal_tangent.bmp", true, false);
        g_texSpec = CScene.loadTexture("DigitalIra\\00_specular_albedo.bmp");

        m_fbo = new FramebufferGL();
        m_fbo.bind();
        m_fbo.addTexture2D(new Texture2DDesc(width, height, GLenum.GL_RGBA8), new TextureAttachDesc());
        gl.glViewport(0, 0, width, height);
        m_prog.enable();
        m_prog.setCameraPos(new Vector3f(-0.211217f, 0.473577f, 59.091206f));
        m_prog.setVecDirectionalLight(new Vector3f(0.593364f, 0.389418f, 0.704466f));
        m_prog.setRgbDirectionalLight(new Vector3f(0.984000f, 1.000000f, 0.912000f));
        m_prog.setVSMMinVariance(0.0001f);
        m_prog.setShadowSharpening(10.f);
        m_prog.setTessScale(21.771534f);
        m_prog.setDeepScatterIntensity(0.5f);
        m_prog.setDeepScatterNormalOffset(1.f);
        m_prog.setExposure(1);
        Matrix4f mat = new Matrix4f(0.025980f, 0.005954f, -0.014676f, 0,
                0, -0.021859f, -0.009631f, 0,
                -0.021882f, 0.007068f, -0.017424f, 0,
                0.485602f, 0.518033f, 0.485627f, 1);
        mat.transpose();
        m_prog.setMatWorldToUvzwShadow(mat);
        m_prog.setNormalStrength(1.0f);
        m_prog.setGloss(0.35f);
        m_prog.printPrograminfo();

        gl.glDisable(GLenum.GL_CULL_FACE);
        gl.glDisable(GLenum.GL_DEPTH_TEST);
        gl.glClearColor(0,0,0,0);
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT);

        // binding the textures
        bindTexture(8, g_NormalTex, m_pSsPointClamp);
        bindTexture(9, g_UVTex, m_pSsPointClamp);
        bindTexture(11, g_TangentCur, m_pSsPointClamp);
        bindTexture(12, g_Position, m_pSsPointClamp);

        bindTexture(0, g_texCubeDiffuse, m_pSsTrilinearRepeat);
        bindTexture(1, g_texCubeSpec, m_pSsTrilinearRepeat);
        bindTexture(4, g_texDiffuse, m_pSsTrilinearRepeat);
        bindTexture(6, g_texNormal, m_pSsTrilinearRepeat);
        bindTexture(7, g_texSpec, m_pSsTrilinearRepeat);
        bindTexture(3, g_vsm, m_pSsBilinearClamp);

        gl.glBindVertexArray(0);
        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);

        GLCheck.checkError();
//        getGLContext().requestExit();

        m_fbo.unbind();
    }

    @Override
    public void display() {
        gl.glBindFramebuffer(GLenum.GL_READ_FRAMEBUFFER, m_fbo.getFramebuffer());
        gl.glBindFramebuffer(GLenum.GL_DRAW_FRAMEBUFFER, 0);
        gl.glReadBuffer(GLenum.GL_COLOR_ATTACHMENT0);
        gl.glBlitFramebuffer(0, 0, 1280, 720, 0, 0, 1280, 720, GLenum.GL_COLOR_BUFFER_BIT, GLenum.GL_NEAREST);
    }

    private void bindTexture(int unit, TextureGL tex, int sampler){
        gl.glActiveTexture(GLenum.GL_TEXTURE0+unit);
        gl.glBindTexture(tex.getTarget(), tex.getTexture());
        gl.glBindSampler(unit, sampler);
    }

    private void createSamplers(){
        SamplerDesc sampDesc = new SamplerDesc();
        sampDesc.minFilter = GLenum.GL_NEAREST_MIPMAP_NEAREST;
        sampDesc.magFilter = GLenum.GL_NEAREST;
        m_pSsPointClamp = SamplerUtils.createSampler(sampDesc);

        sampDesc.minFilter = GLenum.GL_LINEAR_MIPMAP_NEAREST;
        sampDesc.magFilter = GLenum.GL_LINEAR;
        m_pSsBilinearClamp = SamplerUtils.createSampler(sampDesc);

        sampDesc.minFilter = GLenum.GL_LINEAR_MIPMAP_LINEAR;
        sampDesc.magFilter = GLenum.GL_LINEAR;
        sampDesc.wrapR = sampDesc.wrapS = sampDesc.wrapT = GLenum.GL_REPEAT;
        m_pSsTrilinearRepeat = SamplerUtils.createSampler(sampDesc);

        sampDesc.anisotropic = 16;
        m_pSsTrilinearRepeatAniso = SamplerUtils.createSampler(sampDesc);

        sampDesc.minFilter = GLenum.GL_LINEAR_MIPMAP_NEAREST;
        sampDesc.magFilter = GLenum.GL_LINEAR;
        sampDesc.anisotropic =0;
        sampDesc.compareFunc = GLenum.GL_LESS;
        sampDesc.compareMode = GLenum.GL_COMPARE_REF_TO_TEXTURE;
        sampDesc.wrapR = sampDesc.wrapS = sampDesc.wrapT = GLenum.GL_CLAMP_TO_BORDER;
        sampDesc.borderColor = 0xFFFFFFFF;
        m_pSsPCF = SamplerUtils.createSampler(sampDesc);
    }

    private static Texture2D loadTextureBinary(String filename, int width, int height, int internalformat, int format, int type){
        final String root = "E:\\textures\\Subsurface\\";
        ByteBuffer binary = DebugTools.loadBinary(root+filename);
        Texture2DDesc desc = new Texture2DDesc(width, height, internalformat);
        TextureDataDesc data = new TextureDataDesc( format, type, binary);

        return TextureUtils.createTexture2D(desc, data);
    }

    private final class GbufferProgram implements OpenGLProgram{

        int m_program;
        int g_posCameraLoc = -1;
        int g_matWorldToUvzShadowNormalLoc = -1;
        int g_tessScaleLoc = -1;
        int g_debugSlider2Loc = -1;
        int g_deepScatterIntensityLoc = -1;
        int g_debugSlider3Loc = -1;
        int g_debugSlider0Loc = -1;
        int g_debugSlider1Loc = -1;
        int g_rgbDirectionalLightLoc = -1;
        int g_vsmMinVarianceLoc = -1;
        int g_matWorldToClipLoc = -1;
        int g_shadowSharpeningLoc = -1;
        int g_matWorldToUvzwShadowLoc = -1;
        int g_deepScatterNormalOffsetLoc = -1;
        int g_debugLoc = -1;
        int g_vecDirectionalLightLoc = -1;
        int g_exposureLoc = -1;

        int g_normalStrength = -1;
        int g_gloss = -1;
        int g_faceworksData = -1;

        public void init(){
            try {
                GLSLProgram program = GLSLProgram.createFromFiles("shader_libs/PostProcessingDefaultScreenSpaceVS.vert",
                        "nvidia/FaceWorks/shaders/gbuffer_renderPS.frag");
                m_program = program.getProgram();
            } catch (IOException e) {
                e.printStackTrace();
            }

            g_posCameraLoc = gl.glGetUniformLocation(m_program, "g_posCamera");
            g_matWorldToUvzShadowNormalLoc = gl.glGetUniformLocation(m_program, "g_matWorldToUvzShadowNormal");
            g_tessScaleLoc = gl.glGetUniformLocation(m_program, "g_tessScale");
            g_debugSlider2Loc = gl.glGetUniformLocation(m_program, "g_debugSlider2");
            g_deepScatterIntensityLoc = gl.glGetUniformLocation(m_program, "g_deepScatterIntensity");
            g_debugSlider3Loc = gl.glGetUniformLocation(m_program, "g_debugSlider3");
            g_debugSlider0Loc = gl.glGetUniformLocation(m_program, "g_debugSlider0");
            g_debugSlider1Loc = gl.glGetUniformLocation(m_program, "g_debugSlider1");
            g_rgbDirectionalLightLoc = gl.glGetUniformLocation(m_program, "g_rgbDirectionalLight");
            g_vsmMinVarianceLoc = gl.glGetUniformLocation(m_program, "g_vsmMinVariance");
            g_matWorldToClipLoc = gl.glGetUniformLocation(m_program, "g_matWorldToClip");
            g_shadowSharpeningLoc = gl.glGetUniformLocation(m_program, "g_shadowSharpening");
            g_matWorldToUvzwShadowLoc = gl.glGetUniformLocation(m_program, "g_matWorldToUvzwShadow");
            g_deepScatterNormalOffsetLoc = gl.glGetUniformLocation(m_program, "g_deepScatterNormalOffset");
            g_debugLoc = gl.glGetUniformLocation(m_program, "g_debug");
            g_vecDirectionalLightLoc = gl.glGetUniformLocation(m_program, "g_vecDirectionalLight");
            g_exposureLoc = gl.glGetUniformLocation(m_program, "g_exposure");
            g_normalStrength = gl.glGetUniformLocation(m_program, "g_normalStrength");
            g_gloss = gl.glGetUniformLocation(m_program, "g_gloss");
            g_faceworksData = gl.glGetUniformLocation(m_program, "g_faceworksData.data");
        }

        public void setCameraPos(Vector3f v) { if(g_posCameraLoc >=0)gl.glUniform3f(g_posCameraLoc, v.x, v.y, v.z);}
        public void setWorldToUvzShadowNormal(Matrix3f mat) { if(g_matWorldToUvzShadowNormalLoc >=0)gl.glUniformMatrix3fv(g_matWorldToUvzShadowNormalLoc, false, CacheBuffer.wrap(mat));}
        public void setTessScale(float f) { if(g_tessScaleLoc >=0)gl.glUniform1f(g_tessScaleLoc, f);}
        public void setDdebugSlider2(float f) { if(g_debugSlider2Loc >=0)gl.glUniform1f(g_debugSlider2Loc, f);}
        public void setDeepScatterIntensity(float f) { if(g_deepScatterIntensityLoc >=0)gl.glUniform1f(g_deepScatterIntensityLoc, f);}
        public void setDebugSlider3(float f) { if(g_debugSlider3Loc >=0)gl.glUniform1f(g_debugSlider3Loc, f);}
        public void setebugSlider0(float f) { if(g_debugSlider0Loc >=0)gl.glUniform1f(g_debugSlider0Loc, f);}
        public void setDebugSlider1(float f) { if(g_debugSlider1Loc >=0)gl.glUniform1f(g_debugSlider1Loc, f);}
        public void setRgbDirectionalLight(Vector3f v) { if(g_rgbDirectionalLightLoc >=0)gl.glUniform3f(g_rgbDirectionalLightLoc, v.x, v.y, v.z);}
        public void setVSMMinVariance(float f) { if(g_vsmMinVarianceLoc >=0)gl.glUniform1f(g_vsmMinVarianceLoc, f);}

        public void setMatWorldToClip(Matrix4f mat) { if(g_matWorldToClipLoc >=0)gl.glUniformMatrix4fv(g_matWorldToClipLoc, false, CacheBuffer.wrap(mat));}
        public void setShadowSharpening(float f) { if(g_shadowSharpeningLoc >=0)gl.glUniform1f(g_shadowSharpeningLoc, f);}
        public void setMatWorldToUvzwShadow(Matrix4f mat) { if(g_matWorldToUvzwShadowLoc >=0)gl.glUniformMatrix4fv(g_matWorldToUvzwShadowLoc, false, CacheBuffer.wrap(mat));}
        public void setDeepScatterNormalOffset(float f) { if(g_deepScatterNormalOffsetLoc >=0)gl.glUniform1f(g_deepScatterNormalOffsetLoc, f);}
        public void setDebug(float f) { if(g_debugLoc >=0)gl.glUniform1f(g_debugLoc, f);}
        public void setVecDirectionalLight(Vector3f v) { if(g_vecDirectionalLightLoc >=0)gl.glUniform3f(g_vecDirectionalLightLoc, v.x, v.y, v.z);}
        public void setExposure(float f) { if(g_exposureLoc >=0)gl.glUniform1f(g_exposureLoc, f);}
        public void setNormalStrength(float f){ if(g_normalStrength >=0) gl.glUniform1f(g_normalStrength, f); }
        public void setGloss(float f) {if(g_gloss >= 0) gl.glUniform1f(g_gloss, f);}
        public void setFaceworksData(float[] constants){if(g_faceworksData>=0) gl.glUniform4fv(g_faceworksData, CacheBuffer.wrap(constants));}

        @Override
        public int getProgram() {
            return m_program;
        }

        public void enable(){ gl.glUseProgram(m_program); }

        @Override
        public void setName(String name) { }

        @Override
        public void dispose() {
            gl.glDeleteProgram(m_program);
        }
    }
}
