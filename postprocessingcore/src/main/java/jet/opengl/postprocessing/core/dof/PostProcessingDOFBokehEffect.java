package jet.opengl.postprocessing.core.dof;

import jet.opengl.postprocessing.core.PostProcessing;
import jet.opengl.postprocessing.core.PostProcessingEffect;
import jet.opengl.postprocessing.core.PostProcessingRenderPass;

/**
 * Created by mazhen'gui on 2017/4/24.
 */

public class PostProcessingDOFBokehEffect extends PostProcessingEffect {

    @Override
    protected void fillRenderPass(PostProcessing context, PostProcessingRenderPass sceneColorTexture, PostProcessingRenderPass sceneDepthTexture) {
        PostProcessingRenderPass lastPass = getLastRenderPass();

        PostProcessingDOFBokehPass dofBokehPass = new PostProcessingDOFBokehPass();

        if(lastPass == null){
            dofBokehPass.setDependency(0, sceneColorTexture, 0);
        }else{
            dofBokehPass.setDependency(0, lastPass, 0);
        }
        dofBokehPass.setDependency(1, sceneDepthTexture, 0);

        context.appendRenderPass("DOF_BOKEH", dofBokehPass);
    }

    @Override
    public String getEffectName() {
        return PostProcessing.DOF_BOKEH;
    }
    @Override
    public int getPriority() {
        return PostProcessing.DOF_BOKEH_PRIPORTY;
    }
}
