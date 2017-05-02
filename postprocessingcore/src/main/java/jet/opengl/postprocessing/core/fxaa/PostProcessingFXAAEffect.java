package jet.opengl.postprocessing.core.fxaa;

import jet.opengl.postprocessing.core.PostProcessing;
import jet.opengl.postprocessing.core.PostProcessingEffect;
import jet.opengl.postprocessing.core.PostProcessingRenderPass;

/**
 * Created by mazhen'gui on 2017/5/2.
 */

public class PostProcessingFXAAEffect  extends PostProcessingEffect {
    @Override
    protected void fillRenderPass(PostProcessing context, PostProcessingRenderPass sceneColorTexture, PostProcessingRenderPass sceneDepthTexture) {
        PostProcessingRenderPass lastPass = getLastRenderPass();

        PostProcessingFXAAPass fishEyePass = new PostProcessingFXAAPass();

        if(lastPass == null){
            fishEyePass.setDependency(0, sceneColorTexture, 0);
        }else{
            fishEyePass.setDependency(0, lastPass, 0);
        }

        context.appendRenderPass(getEffectName(), fishEyePass);
    }

    @Override
    public String getEffectName() {
        return PostProcessing.FXAA;
    }

    @Override
    public int getPriority() {
        return PostProcessing.FXAA_PRIPORTY;
    }
}
