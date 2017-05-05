package jet.opengl.postprocessing.core.eyeAdaption;

import jet.opengl.postprocessing.common.GLAPI;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.core.PostProcessing;
import jet.opengl.postprocessing.core.PostProcessingDownsamplePass;
import jet.opengl.postprocessing.core.PostProcessingEffect;
import jet.opengl.postprocessing.core.PostProcessingRenderPass;

/**
 * Created by mazhen'gui on 2017/5/3.
 */

public class PostProcessingEyeAdaptationEffect extends PostProcessingEffect {

    @Override
    protected void fillRenderPass(PostProcessing context, PostProcessingRenderPass sceneColorTexture, PostProcessingRenderPass sceneDepthTexture) {
        PostProcessingRenderPass downsample4xPass0 = null;
        PostProcessingRenderPass downsample4xPass1 = null;
        PostProcessingRenderPass downsample4xPass2 = null;

        final int downsampleMethod = GLFuncProviderFactory.getGLFuncProvider().getHostAPI() == GLAPI.ANDROID ?
                PostProcessingDownsamplePass.DOWMSAMPLE_FASTEST:
                PostProcessingDownsamplePass.DOWMSAMPLE_NORMAL;

        // search the dowmsample pass from context
        downsample4xPass0 = context.findPass("DownsampleScene4x_0");
        if(downsample4xPass0 == null){
            downsample4xPass0 = context.findPass("Downsample2x2_1");
        }

        if(downsample4xPass0 == null){
            downsample4xPass0 = new PostProcessingDownsamplePass(4, downsampleMethod);
//            downsample4xPass0 = new Downsample4xPass();
            downsample4xPass0.setOutputFixSize(0, 256, 256);
            downsample4xPass0.setDependency(0, sceneColorTexture, 0);
            context.appendRenderPass("DownsampleScene4x_0", downsample4xPass0);
        }

        downsample4xPass1 = context.findPass("DownsampleScene4x_1");
        if(downsample4xPass1 == null) {
            downsample4xPass1 = context.findPass("DownsampleScene2x2_3");
        }

        if(downsample4xPass1 == null) {
            downsample4xPass1 = new PostProcessingDownsamplePass(4, downsampleMethod);
//            downsample4xPass1 = new Downsample4xPass();
            downsample4xPass1.setOutputFixSize(0, 64, 64);
            downsample4xPass1.setDependency(0, downsample4xPass0, 0);
            context.appendRenderPass("DownsampleScene4x_1", downsample4xPass1);
        }

        downsample4xPass2 = new PostProcessingDownsamplePass(4, downsampleMethod);
//        downsample4xPass2 = new Downsample4xPass();
        downsample4xPass2.setOutputFixSize(0, 16, 16);
        downsample4xPass2.setDependency(0, downsample4xPass1, 0);
        context.appendRenderPass("DownsampleScene4x_2", downsample4xPass2);

//         Calculate the scene lumiance.
        PostProcessingCalculateLuminancePass calculateLuminancePass = new PostProcessingCalculateLuminancePass();
        calculateLuminancePass.setDependency(0, downsample4xPass2, 0);
        context.appendRenderPass("EyeAdaptation", calculateLuminancePass);
    }

    @Override
    public String getEffectName() {
        return PostProcessing.EYE_ADAPATION;
    }

    @Override
    public int getPriority() {
        return PostProcessing.EYE_ADAPATION_PRIPORTY;
    }
}
