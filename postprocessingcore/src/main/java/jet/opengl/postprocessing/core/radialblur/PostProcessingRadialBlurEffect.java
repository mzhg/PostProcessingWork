package jet.opengl.postprocessing.core.radialblur;

import jet.opengl.postprocessing.core.PostProcessing;
import jet.opengl.postprocessing.core.PostProcessingEffect;
import jet.opengl.postprocessing.core.PostProcessingRenderPass;

/**
 * Created by mazhen'gui on 2017/4/24.
 */

public class PostProcessingRadialBlurEffect extends PostProcessingEffect {

    @Override
    protected void fillRenderPass(PostProcessing context, PostProcessingRenderPass sceneColorTexture, PostProcessingRenderPass sceneDepthTexture) {
        PostProcessingRenderPass lastPass = getLastRenderPass();

        PostProcessingRadialBlurPass radialBlurPass = null;

        if(lastPass == null){
            radialBlurPass = new PostProcessingRadialBlurPass();
            radialBlurPass.setDependency(0, sceneColorTexture, 0);
        }else{
            radialBlurPass = new PostProcessingRadialBlurPass();
            radialBlurPass.setDependency(0, lastPass, 0);
        }

        context.appendRenderPass(getEffectName(), radialBlurPass);
    }

    @Override
    public String getEffectName() {
        return PostProcessing.RADIAL_BLUR;
    }

    @Override
    public int getPriority() {
        return PostProcessing.GUASSION_BLUR_PRIPORTY;
    }
}
