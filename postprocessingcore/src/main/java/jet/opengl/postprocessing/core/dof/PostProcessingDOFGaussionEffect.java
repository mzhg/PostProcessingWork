package jet.opengl.postprocessing.core.dof;

import jet.opengl.postprocessing.core.PostProcessing;
import jet.opengl.postprocessing.core.PostProcessingEffect;
import jet.opengl.postprocessing.core.PostProcessingGaussionBlurPass2;
import jet.opengl.postprocessing.core.PostProcessingRenderPass;
import jet.opengl.postprocessing.util.Numeric;

/**
 * Created by mazhen'gui on 2017/4/24.
 */

public class PostProcessingDOFGaussionEffect extends PostProcessingEffect {

    @Override
    protected void fillRenderPass(PostProcessing context, PostProcessingRenderPass sceneColorTexture, PostProcessingRenderPass sceneDepthTexture) {
        PostProcessingRenderPass lastPass = getLastRenderPass();
        int initValue = (Integer)getInitValue();
        boolean bInNearBlur = Numeric.decodeFirst(initValue) != 0;
        boolean bInFarBlur = Numeric.decodeSecond(initValue) != 0;

        PostProcessingDOFSetupPass setupPass = new PostProcessingDOFSetupPass(bInFarBlur, bInNearBlur);

        if(lastPass == null){
            setupPass.setDependency(0, sceneColorTexture, 0);
        }else{
            setupPass.setDependency(0, lastPass, 0);
        }
        setupPass.setDependency(1, sceneDepthTexture, 0);

        context.appendRenderPass("DOF_Setup", setupPass);

        PostProcessingGaussionBlurPass2 nearBlurPass = null;
        PostProcessingGaussionBlurPass2 farBlurPass = null;

        if(bInFarBlur){
            farBlurPass = new PostProcessingGaussionBlurPass2(13);
            farBlurPass.setDependency(0, setupPass, 0);

            context.appendRenderPass("DOF_FarBlur", farBlurPass);
        }

        if(bInNearBlur){
            nearBlurPass = new PostProcessingGaussionBlurPass2(11);
            nearBlurPass.setDependency(0, setupPass, bInFarBlur ? 1: 0);

            context.appendRenderPass("DOF_NearBlur", nearBlurPass);
        }

        PostProcessingDOFRecombinePass recombinePass = new PostProcessingDOFRecombinePass(bInFarBlur, bInNearBlur);
        if(lastPass == null){
            recombinePass.setDependency(0, sceneColorTexture, 0);
        }else{
            recombinePass.setDependency(0, lastPass, 0);
        }
        recombinePass.setDependency(1, sceneDepthTexture, 0);
        int slot = 2;
        if(farBlurPass != null){
            recombinePass.setDependency(slot++, farBlurPass, 0);
        }

        if(nearBlurPass != null){
            recombinePass.setDependency(slot++, nearBlurPass, 0);
        }

        context.appendRenderPass("DOF_Recombine", recombinePass);
    }

    @Override
    public String getEffectName() {
        return PostProcessing.DOF_GAUSSION;
    }
    @Override
    public int getPriority() {
        return PostProcessing.DOF_GAUSSION_PRIPORTY;
    }
}
