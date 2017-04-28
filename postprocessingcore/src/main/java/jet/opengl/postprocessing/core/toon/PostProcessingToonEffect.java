package jet.opengl.postprocessing.core.toon;

import jet.opengl.postprocessing.core.PostProcessing;
import jet.opengl.postprocessing.core.PostProcessingEffect;
import jet.opengl.postprocessing.core.PostProcessingRenderPass;

/**
 * Created by mazhen'gui on 2017/4/24.
 */

public class PostProcessingToonEffect extends PostProcessingEffect {

    @Override
    protected void fillRenderPass(PostProcessing context, PostProcessingRenderPass sceneColorTexture, PostProcessingRenderPass sceneDepthTexture) {
        PostProcessingRenderPass lastPass = getLastRenderPass();

        PostProcessingToonPass radialBlurPass = new PostProcessingToonPass();

        if(lastPass == null){
            radialBlurPass.setDependency(0, sceneColorTexture, 0);
        }else{
            radialBlurPass.setDependency(0, lastPass, 0);
        }

        context.appendRenderPass(getEffectName(), radialBlurPass);
    }

    @Override
    public String getEffectName() {
        return PostProcessing.TOON;
    }

    @Override
    public int getPriority() {
        return PostProcessing.TOON_PRIPORTY;
    }
}
