package jet.opengl.postprocessing.core.grayscreen;

import jet.opengl.postprocessing.core.PostProcessing;
import jet.opengl.postprocessing.core.PostProcessingEffect;
import jet.opengl.postprocessing.core.PostProcessingRenderPass;

/**
 * Created by mazhen'gui on 2017/4/24.
 */
public class PostProcessingGrayScreenEffect extends PostProcessingEffect {

    @Override
    protected void fillRenderPass(PostProcessing context, PostProcessingRenderPass sceneColorTexture, PostProcessingRenderPass sceneDepthTexture) {
        PostProcessingRenderPass lastPass = getLastRenderPass();

        PostProcessingGrayScreenPass grayScreenPass = new PostProcessingGrayScreenPass();

        if(lastPass == null){
            grayScreenPass.setDependency(0, sceneColorTexture, 0);
        }else{
            grayScreenPass.setDependency(0, lastPass, 0);
        }

        context.appendRenderPass(getEffectName(), grayScreenPass);
    }

    @Override
    public String getEffectName() {
        return PostProcessing.GRAY_SCREEN;
    }

    @Override
    public int getPriority() {
        return PostProcessing.FISH_EYE_PRIPORTY+1;
    }
}
