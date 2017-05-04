package jet.opengl.postprocessing.core.tonemapping;

import jet.opengl.postprocessing.core.PostProcessing;
import jet.opengl.postprocessing.core.PostProcessingEffect;
import jet.opengl.postprocessing.core.PostProcessingRenderPass;

/**
 * Created by mazhen'gui on 2017/4/24.
 */

public class PostProcessingTonemappingEffect extends PostProcessingEffect {

    @Override
    protected void fillRenderPass(PostProcessing context, PostProcessingRenderPass sceneColorTexture, PostProcessingRenderPass sceneDepthTexture) {
        final PostProcessingRenderPass lastPass = getLastRenderPass();
        PostProcessingRenderPass eyeAdaptationPass = context.findPass("EyeAdaptation");

        PostProcessingTonemappingPass tonemappingPass = new PostProcessingTonemappingPass(eyeAdaptationPass != null);
        if(lastPass != null){

        }
    }

    @Override
    public String getEffectName() {
        return PostProcessing.BLOOM;
    }

    @Override
    public int getPriority() {
        return PostProcessing.BLOOM_PRIPORTY;
    }
}
