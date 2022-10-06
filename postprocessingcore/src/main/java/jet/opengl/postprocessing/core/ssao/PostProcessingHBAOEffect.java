package jet.opengl.postprocessing.core.ssao;

import jet.opengl.postprocessing.core.PostProcessing;
import jet.opengl.postprocessing.core.PostProcessingEffect;
import jet.opengl.postprocessing.core.PostProcessingReconstructCameraZPass;
import jet.opengl.postprocessing.core.PostProcessingRenderPass;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.TextureUtils;

/**
 * Created by mazhen'gui on 2017-05-12 13:52:50.
 */

public class PostProcessingHBAOEffect extends PostProcessingEffect {
    static final boolean USE_FP32 = true;

    @Override
    protected void fillRenderPass(PostProcessing context, PostProcessingRenderPass sceneColorTexture, PostProcessingRenderPass sceneDepthTexture) {
        final PostProcessingRenderPass lastPass = getLastRenderPass();

        Texture2D texture = sceneColorTexture.getOutputTexture(0);
        final int sampleCount = texture.getSampleCount();
//        final int width = sceneDepthTexture.getOutputTexture(0).getWidth();
//        final int height = sceneDepthTexture.getOutputTexture(0).getHeight();
//        final int quarterWidth  = ((width +3)/4);
//        final int quarterHeight = ((height+3)/4);

        final boolean enableMSAA = sampleCount > 1;
        for(int sampleIdx = 0; sampleIdx < sampleCount; sampleIdx++){
            final String linearDepthPassName = "ReconstructCameraZ" + (enableMSAA ? ("MSAA" + sampleIdx):"");
            PostProcessingReconstructCameraZPass linearDepthPass = (PostProcessingReconstructCameraZPass) context.findPass(linearDepthPassName);

            if(linearDepthPass == null) {
                linearDepthPass = new PostProcessingReconstructCameraZPass(enableMSAA, sampleIdx, USE_FP32);
                linearDepthPass.setDependency(0, sceneDepthTexture, 0);

                context.appendRenderPass(linearDepthPassName, linearDepthPass);
            }

            PostProcessingReconstructNormalPass  viewNormalPass = new PostProcessingReconstructNormalPass();
            viewNormalPass.setDependency(0, linearDepthPass, 0);
            context.appendRenderPass("ReconstructNormal" + sampleIdx, viewNormalPass);

            PostProcessingDeinterleavePass deinterleavePass = new PostProcessingDeinterleavePass(USE_FP32);
            deinterleavePass.setDependency(0, linearDepthPass, 0);
            context.appendRenderPass("Deinterleave" + sampleIdx, deinterleavePass);

            PostProcessingHBAOCalculatePass hbaoCalculatePass = new PostProcessingHBAOCalculatePass();
            hbaoCalculatePass.setDependency(0, deinterleavePass, 0);
            hbaoCalculatePass.setDependency(1, viewNormalPass, 0);
            context.appendRenderPass("HBAOCalculate" + sampleIdx, hbaoCalculatePass);

            PostProcessingReinterleavePass reinterleavePass = new PostProcessingReinterleavePass();
            reinterleavePass.setDependency(0, hbaoCalculatePass, 0);
            context.appendRenderPass("Reinterleave" + sampleIdx, reinterleavePass);

            PostProcessingHBAOBlurPass hbaoBlurPass = new PostProcessingHBAOBlurPass(enableMSAA, sampleIdx);
            hbaoBlurPass.setDependency(0, reinterleavePass, 0);
            context.appendRenderPass("HBAOBlur" + sampleIdx, hbaoBlurPass);
        }
    }

    @Override
    public String getEffectName() {
        return PostProcessing.HBAO;
    }

    @Override
    public int getPriority() {
        return PostProcessing.HBAO_PRIPORTY;
    }
}
