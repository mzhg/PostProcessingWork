package jet.opengl.postprocessing.core.ssao;

import org.lwjgl.util.vector.Matrix4f;

import java.io.IOException;

import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.PostProcessingFrameAttribs;
import jet.opengl.postprocessing.core.PostProcessingParameters;
import jet.opengl.postprocessing.core.PostProcessingRenderContext;
import jet.opengl.postprocessing.core.PostProcessingRenderPass;
import jet.opengl.postprocessing.texture.SamplerDesc;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.util.LogUtil;

/**
 * Created by mazhen'gui on 2017-05-11 16:17:38.
 */

public class PostProcessingReconstructNormalPass extends PostProcessingRenderPass {

    private static PostProcessingReconstructNormalProgram g_ReconstructNormalProgram = null;
    private int m_SamplerPointClamp;
    public PostProcessingReconstructNormalPass() {
        super("ReconstructNormalPass");

        set(1,1);
    }

    @Override
    public void process(PostProcessingRenderContext context, PostProcessingParameters parameters) {
        if(g_ReconstructNormalProgram == null){
            try {
                g_ReconstructNormalProgram = new PostProcessingReconstructNormalProgram();
                addDisposedResource(g_ReconstructNormalProgram);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if(m_SamplerPointClamp == 0){
            SamplerDesc desc = new SamplerDesc();
            desc.minFilter = desc.magFilter = GLenum.GL_NEAREST;
            m_SamplerPointClamp = SamplerUtils.createSampler(desc);
        }

        Texture2D input0 = getInput(0);
        Texture2D output = getOutputTexture(0);
        output.setName("ReconstructNormal");
        if(input0 == null){
            LogUtil.e(LogUtil.LogType.DEFAULT, "ReconstructNormalPass:: Missing input texture!");
            return;
        }

        context.setViewport(0,0, output.getWidth(), output.getHeight());
        context.setVAO(null);
        context.setProgram(g_ReconstructNormalProgram);
        PostProcessingFrameAttribs frameAttribs = context.getFrameAttribs();
        g_ReconstructNormalProgram.setCameraMatrixs(frameAttribs.projMat, frameAttribs.getProjInvertMatrix());
        g_ReconstructNormalProgram.setTexelSize(1.0f/input0.getWidth(), 1.0f/input0.getHeight());

        Matrix4f projMat = frameAttribs.projMat;
        float x = 2.0f/projMat.m00,     // (x) * (R - L)/N
              y = 2.0f/projMat.m11,     // (y) * (T - B)/N
              z = -( 1.0f - projMat.m20) / projMat.m00, // L/N
              w = -( 1.0f + projMat.m21) / projMat.m11;  // B/N
        g_ReconstructNormalProgram.setProjInfo(x, y, z, w);
        g_ReconstructNormalProgram.setProjOrtho(0);

        context.bindTexture(input0, 0, m_SamplerPointClamp);
        context.setBlendState(null);
        context.setDepthStencilState(null);
        context.setRasterizerState(null);
        context.setRenderTarget(output);

        context.drawFullscreenQuad();
        context.bindTexture(input0, 0, 0);  // unbind sampler.

        if(GLCheck.CHECK)
            GLCheck.checkError("ReconstructNormalPass");
    }

    @Override
    public void computeOutDesc(int index, Texture2DDesc out) {
        Texture2D input = getInput(0);
        if(input != null){
            input.getDesc(out);
            out.format = GLenum.GL_RGB8;
        }

        super.computeOutDesc(index, out);
    }
}
