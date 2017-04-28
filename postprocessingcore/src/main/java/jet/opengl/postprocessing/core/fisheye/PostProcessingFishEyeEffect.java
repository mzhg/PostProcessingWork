package jet.opengl.postprocessing.core.fisheye;

import jet.opengl.postprocessing.core.PostProcessing;
import jet.opengl.postprocessing.core.PostProcessingEffect;
import jet.opengl.postprocessing.core.PostProcessingRenderPass;

/**
 * Created by mazhen'gui on 2017/4/24.
 */

public class PostProcessingFishEyeEffect extends PostProcessingEffect {

    @Override
    protected void fillRenderPass(PostProcessing context, PostProcessingRenderPass sceneColorTexture, PostProcessingRenderPass sceneDepthTexture) {
        PostProcessingRenderPass lastPass = getLastRenderPass();

        PostProcessingFishEyePass fishEyePass = new PostProcessingFishEyePass();

        if(lastPass == null){
            fishEyePass.setDependency(0, sceneColorTexture, 0);
        }else{
            fishEyePass.setDependency(0, lastPass, 0);
        }

        context.appendRenderPass(getEffectName(), fishEyePass);
    }

    @Override
    public String getEffectName() {
        return PostProcessing.FISH_EYE;
    }

    @Override
    public int getPriority() {
        return PostProcessing.FISH_EYE_PRIPORTY;
    }
}
