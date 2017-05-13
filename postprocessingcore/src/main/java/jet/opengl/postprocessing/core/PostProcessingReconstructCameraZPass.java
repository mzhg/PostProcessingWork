package jet.opengl.postprocessing.core;

import java.io.IOException;

import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.SamplerDesc;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.util.LogUtil;

/**
 * Created by mazhen'gui on 2017-05-11 16:04:08.
 */

public class PostProcessingReconstructCameraZPass extends PostProcessingRenderPass {

    private static PostProcessingReconstructCameraZProgram g_ReconstructCameraZProgram = null;
    private static PostProcessingReconstructCameraZProgram g_ReconstructCameraZProgramMSAA = null;
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

    private static PostProcessingReconstructCameraZProgram getReconstructCameraZProgram(boolean msaa){
        if(msaa){
            if(g_ReconstructCameraZProgramMSAA == null){
                try {
                    g_ReconstructCameraZProgramMSAA = new PostProcessingReconstructCameraZProgram(true);
                    addDisposedResource(g_ReconstructCameraZProgramMSAA);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return g_ReconstructCameraZProgramMSAA;
        }else{
            if(g_ReconstructCameraZProgram == null){
                try {
                    g_ReconstructCameraZProgram = new PostProcessingReconstructCameraZProgram(false);
                    addDisposedResource(g_ReconstructCameraZProgram);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return g_ReconstructCameraZProgram;
        }
    }

    @Override
    public void process(PostProcessingRenderContext context, PostProcessingParameters parameters) {
        Texture2D input0 = getInput(0);
        Texture2D output = getOutputTexture(0);
        output.setName("ReconstructCameraZ");
        if(input0 == null){
            LogUtil.e(LogUtil.LogType.DEFAULT, "ReconstructCameraZPass:: Missing depth texture!");
            return;
        }

        PostProcessingReconstructCameraZProgram reconstructCameraZProgram = getReconstructCameraZProgram(m_bMSAA);

        if(m_DefualtSampler == 0){
            SamplerDesc desc = new SamplerDesc();
            desc.minFilter = GLenum.GL_NEAREST;
            desc.magFilter = GLenum.GL_NEAREST;

            m_DefualtSampler = SamplerUtils.createSampler(desc);
        }

        context.setViewport(0,0, output.getWidth(), output.getHeight());
//        context.setViewport(0,0, 1280, 720);
        context.setVAO(null);
        context.setProgram(reconstructCameraZProgram);
        PostProcessingFrameAttribs frameAttribs = context.getFrameAttribs();
        reconstructCameraZProgram.setCameraRange(frameAttribs.cameraNear, frameAttribs.cameraFar);
        reconstructCameraZProgram.setSampleIndex(m_sampleIdx);

        context.bindTexture(input0, 0, m_DefualtSampler);
//        context.bindTexture(input0, 0, 0);
//        context.bindTexture(input1, 1, 0);
        context.setBlendState(null);
        context.setDepthStencilState(null);
        context.setRasterizerState(null);
        context.setRenderTarget(output);

        context.drawFullscreenQuad();
        context.bindTexture(input0, 0, 0);  // unbind sampler.

        if(GLCheck.CHECK)
            GLCheck.checkError("ReconstructCameraZPass");
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
