package jet.opengl.demos.gpupro.ibl;

import com.nvidia.developer.opengl.models.GLVAO;
import com.nvidia.developer.opengl.models.ModelGenerator;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.io.IOException;
import java.nio.IntBuffer;

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
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.TextureAttachDesc;
import jet.opengl.postprocessing.texture.TextureCube;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.LogUtil;

public class FrostbiteIBL implements Disposeable {

    private GLSLProgram mDiffuseLDProg;
    private GLSLProgram mSpecularLDProg;
    private GLSLProgram mDFGProg;
    private GLVAO mCubeVAO;
    private RenderTargets mFbo;
    private int mInputSampler;
    private final TextureAttachDesc mAttachDesc = new TextureAttachDesc();
    private final Matrix4f[] mViews = new Matrix4f[6];
    private final Matrix4f mProj = new Matrix4f();
    private GLFuncProvider gl;
    private boolean mInitlized;

    public void generate(TextureCube source, TextureCube specularLD, TextureCube diffuseLD, Texture2D dfg){
        intlizeResources();
        generateDiffuseLD(source, diffuseLD);
        generateSpecularLD(source, specularLD);
        generateDFG(dfg);
    }

    private void generateDiffuseLD(TextureCube source, TextureCube diffuseLD){
        GLCheck.checkError();
        IntBuffer viewports = CacheBuffer.getCachedIntBuffer(4);
        gl.glGetIntegerv(GLenum.GL_VIEWPORT, viewports);

        final int VP_X = viewports.get(0);
        final int VP_Y = viewports.get(0);
        final int VP_W = viewports.get(0);
        final int VP_H = viewports.get(0);

        final boolean isDepthTest = gl.glIsEnabled(GLenum.GL_DEPTH_TEST);

        gl.glViewport(0,0, diffuseLD.getWidth(), diffuseLD.getWidth());
        gl.glDisable(GLenum.GL_DEPTH_TEST);
        mFbo.bind();
        mDiffuseLDProg.enable();

        int view = gl.glGetUniformLocation(mDiffuseLDProg.getProgram(), "g_View");
        int proj = gl.glGetUniformLocation(mDiffuseLDProg.getProgram(), "g_Proj");
        gl.glUniformMatrix4fv(proj,false, CacheBuffer.wrap(mProj));

        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glBindTexture(source.getTarget(), source.getTexture());
        gl.glBindSampler(0, mInputSampler);

        for(int i = 0; i < 6; i++){
            mAttachDesc.type = AttachType.TEXTURE_2D;
            mAttachDesc.layer = i;
            mAttachDesc.index = 0;
            mAttachDesc.level = 0;
            mFbo.setRenderTexture(diffuseLD, mAttachDesc);

            gl.glUniformMatrix4fv(view,false, CacheBuffer.wrap(mViews[i]));

            mCubeVAO.bind();
            mCubeVAO.draw(GLenum.GL_TRIANGLES);
            mCubeVAO.unbind();
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

    private void generateSpecularLD(TextureCube source, TextureCube destion){
        GLCheck.checkError();
        IntBuffer viewports = CacheBuffer.getCachedIntBuffer(4);
        gl.glGetIntegerv(GLenum.GL_VIEWPORT, viewports);

        final int VP_X = viewports.get(0);
        final int VP_Y = viewports.get(0);
        final int VP_W = viewports.get(0);
        final int VP_H = viewports.get(0);

        final boolean isDepthTest = gl.glIsEnabled(GLenum.GL_DEPTH_TEST);

        gl.glViewport(0,0, destion.getWidth(), destion.getWidth());
        gl.glDisable(GLenum.GL_DEPTH_TEST);
        mFbo.bind();
        mSpecularLDProg.enable();

        int view = gl.glGetUniformLocation(mSpecularLDProg.getProgram(), "g_View");
        int proj = gl.glGetUniformLocation(mSpecularLDProg.getProgram(), "g_Proj");
        int roughness = gl.glGetUniformLocation(mSpecularLDProg.getProgram(), "g_Roughness");
        gl.glUniformMatrix4fv(proj,false, CacheBuffer.wrap(mProj));

        if(roughness < 0)
            throw new IllegalArgumentException();

        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glBindTexture(source.getTarget(), source.getTexture());
        gl.glBindSampler(0, mInputSampler);
        mCubeVAO.bind();

        if(destion.getMipLevels() == 1){
            throw new IllegalArgumentException();
        }

        int size = destion.getWidth();
        for(int level = 0; level < destion.getMipLevels(); level++){
            float roughnessValue = (float)level/(destion.getMipLevels() - 1);
            roughnessValue *= roughnessValue;
            gl.glUniform1f(roughness, roughnessValue);

            gl.glViewport(0,0, size, size);
            size = Math.max(1, size/2);

            for(int i = 0; i < 6; i++){
                mAttachDesc.type = AttachType.TEXTURE_2D;
                mAttachDesc.layer = i;
                mAttachDesc.index = 0;
                mAttachDesc.level = level;
                mFbo.setRenderTexture(destion, mAttachDesc);

                gl.glUniformMatrix4fv(view,false, CacheBuffer.wrap(mViews[i]));

                mCubeVAO.draw(GLenum.GL_TRIANGLES);
                LogUtil.i(LogUtil.LogType.DEFAULT, "Generate the Face(" + i+") done!");
            }
        }

        mCubeVAO.unbind();
        gl.glBindTexture(GLenum.GL_TEXTURE_2D, 0);
        gl.glBindSampler(0, 0);

        gl.glViewport(VP_X, VP_Y, VP_W, VP_H);
        if(isDepthTest)
            gl.glEnable(GLenum.GL_DEPTH_TEST);
        else
            gl.glDisable(GLenum.GL_DEPTH_TEST);

        GLCheck.checkError();
    }

    private void generateDFG(Texture2D dfg){
        GLCheck.checkError();
        IntBuffer viewports = CacheBuffer.getCachedIntBuffer(4);
        gl.glGetIntegerv(GLenum.GL_VIEWPORT, viewports);

        final int VP_X = viewports.get(0);
        final int VP_Y = viewports.get(0);
        final int VP_W = viewports.get(0);
        final int VP_H = viewports.get(0);

        final boolean isDepthTest = gl.glIsEnabled(GLenum.GL_DEPTH_TEST);

        gl.glViewport(0,0, dfg.getWidth(), dfg.getHeight());
        mAttachDesc.type = AttachType.TEXTURE_2D;
        mAttachDesc.layer = 0;
        mAttachDesc.index = 0;
        mAttachDesc.level = 0;
        mFbo.bind();
        mFbo.setRenderTexture(dfg, mAttachDesc);

        gl.glDisable(GLenum.GL_DEPTH_TEST);
        mFbo.bind();
        mDFGProg.enable();

        int viewport = gl.glGetUniformLocation(mDFGProg.getProgram(), "g_Viewport");
        gl.glUniform2f(viewport, dfg.getWidth(), dfg.getHeight());
        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);

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
            mDiffuseLDProg = GLSLProgram.createFromFiles(root + "IrradianceCubeMapVS.vert", root + "FrostbiteDiffuseLD.frag");
            mSpecularLDProg = GLSLProgram.createFromFiles(root + "IrradianceCubeMapVS.vert", root + "FrostbiteSpecularLD.frag");
            mDFGProg = GLSLProgram.createFromFiles("shader_libs/PostProcessingDefaultScreenSpaceVS.vert", root + "FrostbiteDFG.frag");
        } catch (IOException e) {
            e.printStackTrace();
        }

        mCubeVAO = ModelGenerator.genCube(1, false, false, false).genVAO();
        mFbo = new RenderTargets();

        SamplerDesc desc = new SamplerDesc();
        desc.minFilter = desc.magFilter = GLenum.GL_NEAREST;
        desc.wrapR = desc.wrapS = desc.wrapT = GLenum.GL_CLAMP_TO_EDGE;
        mInputSampler = 0; // SamplerUtils.createSampler(desc);

        ShadowMapGenerator.buildCubeShadowMatrices(new Vector3f(0,0,0), 0.1f, 10.0f, mProj,mViews);
        mInitlized = true;
    }

    @Override
    public void dispose() {

    }
}
