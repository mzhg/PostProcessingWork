package jet.opengl.postprocessing.core;

import java.io.IOException;

import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.util.LogUtil;

/**
 * Created by mazhen'gui on 2017-05-11 16:04:08.
 */

public class PostProcessingReconstructCameraZPass extends PostProcessingRenderPass {

    private static PostProcessingReconstructCameraZProgram g_ReconstructCameraZProgram = null;
    private final boolean m_bMSAA;
    private final boolean m_bUse32FP;
    private int m_DefualtSampler;
    private final int m_sampleIdx;

    public PostProcessingReconstructCameraZPass(boolean enableMSAA, int sampleIdx, boolean use32FP) {
        super("ReconstructCameraZ" + (enableMSAA ? ("MSAA" + sampleIdx):""));
        m_bMSAA = enableMSAA;
        m_bUse32FP = use32FP;
        m_sampleIdx  =sampleIdx;

        set(1,1);
    }

    @Override
    public void process(PostProcessingRenderContext context, PostProcessingParameters parameters) {
        if(g_ReconstructCameraZProgram == null){
            try {
                g_ReconstructCameraZProgram = new PostProcessingReconstructCameraZProgram(m_bMSAA);
                addDisposedResource(g_ReconstructCameraZProgram);

                m_DefualtSampler = SamplerUtils.getDefaultSampler();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Texture2D input0 = getInput(0);
        Texture2D output = getOutputTexture(0);
        if(input0 == null){
            LogUtil.e(LogUtil.LogType.DEFAULT, "ReconstructCameraZPass:: Missing depth texture!");
            return;
        }


        context.setViewport(0,0, output.getWidth(), output.getHeight());
//        context.setViewport(0,0, 1280, 720);
        context.setVAO(null);
        context.setProgram(g_ReconstructCameraZProgram);
        PostProcessingFrameAttribs frameAttribs = context.getFrameAttribs();
        g_ReconstructCameraZProgram.setCameraRange(frameAttribs.cameraNear, frameAttribs.cameraFar);

        context.bindTexture(input0, 0, m_DefualtSampler);
//        context.bindTexture(input0, 0, 0);
//        context.bindTexture(input1, 1, 0);
        context.setBlendState(null);
        context.setDepthStencilState(null);
        context.setRasterizerState(null);
        context.setRenderTarget(output);

        context.drawFullscreenQuad();
        context.bindTexture(input0, 0, 0);  // unbind sampler.
    }

    @Override
    public void computeOutDesc(int index, Texture2DDesc out) {
        Texture2D input = getInput(0);
        if(input != null){
            input.getDesc(out);
            out.format = m_bUse32FP?GLenum.GL_R32F:GLenum.GL_R16F;
            out.sampleCount = 1;
        }

        super.computeOutDesc(index, out);
    }
}
