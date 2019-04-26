package jet.opengl.demos.gpupro.ibl;

import com.nvidia.developer.opengl.models.GLVAO;
import com.nvidia.developer.opengl.models.ModelGenerator;
import com.nvidia.developer.opengl.utils.NvImage;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.io.IOException;
import java.nio.IntBuffer;

import javax.naming.InitialContext;

import jet.opengl.demos.nvidia.shadows.ShadowMapGenerator;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.AttachType;
import jet.opengl.postprocessing.texture.RenderTargets;
import jet.opengl.postprocessing.texture.SamplerDesc;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.texture.TextureAttachDesc;
import jet.opengl.postprocessing.texture.TextureCube;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.LogUtil;

public class IrradianceCubeMap implements Disposeable {

    public static class Desc{
        public boolean sourceFromFile = false;
        public String sourceFilename;
        public TextureCube sourceEnvMap;

        public boolean outputToInternalCubeMap = true;
        public int outputSize = 128;
        public TextureCube outputEnvMap;
    }

    private TextureCube mInput;
    private TextureCube mOutput;

    private GLSLProgram mProgram;
    private GLVAO mCubeVAO;
    private RenderTargets mFbo;
    private int mInputSampler;
    private final TextureAttachDesc mAttachDesc = new TextureAttachDesc();
    private final Matrix4f[] mViews = new Matrix4f[6];
    private final Matrix4f mProj = new Matrix4f();
    private GLFuncProvider gl;
    private boolean mInitlized;

    private final Desc mDesc = new Desc();

    public void generateCubeMap(Desc desc){
        intlizeResources();
        updateInputs(desc);
        updateOutput(desc);
        renderCubeMap();
    }

    private void renderCubeMap(){
        GLCheck.checkError();
        IntBuffer viewports = CacheBuffer.getCachedIntBuffer(4);
        gl.glGetIntegerv(GLenum.GL_VIEWPORT, viewports);

        final int VP_X = viewports.get(0);
        final int VP_Y = viewports.get(0);
        final int VP_W = viewports.get(0);
        final int VP_H = viewports.get(0);

        final boolean isDepthTest = gl.glIsEnabled(GLenum.GL_DEPTH_TEST);

        gl.glViewport(0,0, mOutput.getWidth(), mOutput.getWidth());
        gl.glDisable(GLenum.GL_DEPTH_TEST);
        mFbo.bind();
        mProgram.enable();

        int view = gl.glGetUniformLocation(mProgram.getProgram(), "g_View");
        int proj = gl.glGetUniformLocation(mProgram.getProgram(), "g_Proj");
        gl.glUniformMatrix4fv(proj,false, CacheBuffer.wrap(mProj));

        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glBindTexture(mInput.getTarget(), mInput.getTexture());
        gl.glBindSampler(0, mInputSampler);

        for(int i = 0; i < 6; i++){
            mAttachDesc.type = AttachType.TEXTURE_2D;
            mAttachDesc.layer = i;
            mAttachDesc.index = 0;
            mAttachDesc.level = 0;
            mFbo.setRenderTexture(mOutput, mAttachDesc);

            gl.glUniformMatrix4fv(view,false, CacheBuffer.wrap(mViews[i]));

            mCubeVAO.bind();
            mCubeVAO.draw(GLenum.GL_TRIANGLES);
            mCubeVAO.unbind();
            LogUtil.i(LogUtil.LogType.DEFAULT, "Generate the Face(" + i+") done!");
        }

        gl.glBindTexture(GLenum.GL_TEXTURE_2D, 0);
        gl.glBindSampler(0, 0);

        gl.glViewport(VP_X, VP_Y, VP_W, VP_H);
        if(isDepthTest)
            gl.glEnable(GLenum.GL_DEPTH_TEST);
        else
            gl.glDisable(GLenum.GL_DEPTH_TEST);

        GLCheck.checkError();
    }

    private void intlizeResources(){
        if(mInitlized)
            return;

        gl = GLFuncProviderFactory.getGLFuncProvider();
        final String root = "gpupro\\IBL\\shaders\\";
        try {
            mProgram = GLSLProgram.createFromFiles(root + "IrradianceCubeMapVS.vert", root + "IrradianceCubeMapPS.frag");
        } catch (IOException e) {
            e.printStackTrace();
        }

        mCubeVAO = ModelGenerator.genCube(1, false, false, false).genVAO();
        mFbo = new RenderTargets();

        SamplerDesc desc = new SamplerDesc();
        desc.minFilter = desc.magFilter = GLenum.GL_NEAREST;
        desc.wrapR = desc.wrapS = desc.wrapT = GLenum.GL_CLAMP_TO_EDGE;
        mInputSampler = SamplerUtils.createSampler(desc);

        ShadowMapGenerator.buildCubeShadowMatrices(new Vector3f(0,0,0), 0.1f, 10.0f, mProj,mViews);
        mInitlized = true;
    }

    private void updateInputs(Desc desc){
        if(desc.sourceFromFile){
            if(!mDesc.sourceFromFile || !mDesc.sourceFilename.equals(desc.sourceFilename)){
                SAFE_RELEASE(mInput);

                // Assume it is a dds file.
                NvImage image = null;
                try {
                    image = NvImage.createFromDDSFile(desc.sourceFilename);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if(image != null){
                    image.convertCrossToCubemap();
                    int textureID = image.updaloadTexture();

                    mInput = TextureUtils.createTextureCube(GLenum.GL_TEXTURE_CUBE_MAP, textureID);
                }
            }

            mDesc.sourceFromFile = true;
            mDesc.sourceFilename = desc.sourceFilename;
        }else{
            mDesc.sourceFromFile = false;
            mInput = desc.sourceEnvMap;

            if(mInput == null)
                throw new NullPointerException("The input Evnmap is null");
        }
    }

    private void updateOutput(Desc desc){
        if(desc.outputToInternalCubeMap){
            if(desc.outputSize < 0)
                throw new IllegalArgumentException("Invalid cube map size");

            if(!mDesc.outputToInternalCubeMap || mOutput == null || mOutput.getWidth() != desc.outputSize){
                if(mDesc.outputToInternalCubeMap){
                    SAFE_RELEASE(mOutput);
                }

                int cubeMap = gl.glGenTexture();
                gl.glBindTexture(GLenum.GL_TEXTURE_CUBE_MAP, cubeMap);
                gl.glTexStorage2D(GLenum.GL_TEXTURE_CUBE_MAP, 1, GLenum.GL_RGBA16F, desc.outputSize, desc.outputSize);
                gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_LINEAR);
                gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_LINEAR);
                gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_WRAP_R, GLenum.GL_CLAMP_TO_EDGE);
                gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_CLAMP_TO_EDGE);
                gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_CLAMP_TO_EDGE);

                mOutput = TextureUtils.createTextureCube(GLenum.GL_TEXTURE_CUBE_MAP, cubeMap);
                GLCheck.checkError();
            }

            mDesc.outputToInternalCubeMap = true;
        }else{
            if(mDesc.outputToInternalCubeMap){
                SAFE_RELEASE(mOutput);
            }

            mDesc.outputToInternalCubeMap = false;
            mOutput = desc.outputEnvMap;

            if(mOutput == null)
                throw new NullPointerException("The output Evnmap is null");
        }
    }

    public TextureCube getOutput(){ return mOutput;}

    @Override
    public void dispose() {

    }

}
