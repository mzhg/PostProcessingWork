package jet.opengl.postprocessing.core.bloom;

import jet.opengl.postprocessing.core.PostProcessing;
import jet.opengl.postprocessing.core.PostProcessingDownsamplePass;
import jet.opengl.postprocessing.core.PostProcessingEffect;
import jet.opengl.postprocessing.core.PostProcessingRenderPass;

/**
 * Created by mazhen'gui on 2017/4/24.
 */

public class PostProcessingBloomEffect extends PostProcessingEffect {

    @Override
    protected void fillRenderPass(PostProcessing context, PostProcessingRenderPass sceneColorTexture, PostProcessingRenderPass sceneDepthTexture) {
        PostProcessingRenderPass lastPass = getLastRenderPass();

        PostProcessingDownsamplePass downsamplePass = new PostProcessingDownsamplePass();
        {
            if(lastPass == null){
                downsamplePass.setDependency(0, sceneColorTexture, 0);
            }else{
                downsamplePass.setDependency(0, lastPass, 0);
            }


        }


        PostProcessingBloomSetupPass radialBlurPass = new PostProcessingBloomSetupPass();

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
