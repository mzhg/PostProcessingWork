package jet.opengl.postprocessing.core.light;

import jet.opengl.postprocessing.common.GLAPI;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.core.PostProcessing;
import jet.opengl.postprocessing.core.PostProcessingCombinePass;
import jet.opengl.postprocessing.core.PostProcessingDownsamplePass;
import jet.opengl.postprocessing.core.PostProcessingEffect;
import jet.opengl.postprocessing.core.PostProcessingParameters;
import jet.opengl.postprocessing.core.PostProcessingRenderPass;
import jet.opengl.postprocessing.core.tonemapping.PostProcessingTonemappingPass;

/**
 * Created by mazhen'gui on 2017/5/4.
 */

public class PostProcessingLightEffect extends PostProcessingEffect {

    @Override
    protected void fillRenderPass(PostProcessing context, PostProcessingRenderPass sceneColorTexture, PostProcessingRenderPass sceneDepthTexture) {
        PostProcessingParameters parameters = getParameters();
        if(!parameters.isLensFlareEnable() && !parameters.isLightStreakerEnabled()){
            return;
        }

        PostProcessingRenderPass downsample4xPass0 = null;

        final int downsampleMethod = GLFuncProviderFactory.getGLFuncProvider().getHostAPI() == GLAPI.ANDROID ? PostProcessingDownsamplePass.DOWMSAMPLE_FASTEST:
                PostProcessingDownsamplePass.DOWMSAMPLE_NORMAL;

        // search the dowmsample pass from context
        downsample4xPass0 = context.findPass("DownsampleScene4x_0");
        if(downsample4xPass0 == null){
            downsample4xPass0 = context.findPass("Downsample2x2_1");
        }

        if(downsample4xPass0 == null){
            downsample4xPass0 = new PostProcessingDownsamplePass(4, downsampleMethod);
            downsample4xPass0.setOutputFixSize(0, 256, 256);
            downsample4xPass0.setDependency(0, sceneColorTexture, 0);
            context.appendRenderPass("DownsampleScene4x_0", downsample4xPass0);
        }

        PostProcessingExtractHighLightPass extractHighLightPass = new PostProcessingExtractHighLightPass();
        extractHighLightPass.setOutputFixSize(0, 256, 256);
        extractHighLightPass.setDependency(0, downsample4xPass0, 0);
        context.appendRenderPass("ExtractHighLight", extractHighLightPass);

        PostProcessingRenderPass lastPass = null;
        int lensFlareInputCount = 1;
        PostProcessingLightStreakerPass lightStreakerPass = null;

        if(parameters.isLightStreakerEnabled()){
            lightStreakerPass = new PostProcessingLightStreakerPass();
            lightStreakerPass.setDependency(0, extractHighLightPass, 0);
            context.appendRenderPass("LightStreaker", lightStreakerPass);

            lensFlareInputCount++;

            lastPass = lightStreakerPass;
        }

        PostProcessingLensFlareComposePass lensFlareComposePass = null;
        if(parameters.isLensFlareEnable()){
            lensFlareComposePass = new PostProcessingLensFlareComposePass(lensFlareInputCount);
            lensFlareComposePass.setDependency(0, extractHighLightPass, 0);
            if(lensFlareInputCount > 1){
                lensFlareComposePass.setDependency(1, lightStreakerPass, 0);
            }

            context.appendRenderPass("LensFlareCompose", lightStreakerPass);
            lastPass = lensFlareComposePass;
        }


        if(context.isHDREnabled() && isLastEffect()){
            // Compose the light effect and scene color, and apply tone-map on it.
            PostProcessingRenderPass eyeAdaptationPass = context.findPass("EyeAdaptation");
            PostProcessingTonemappingPass tonemappingPass = new PostProcessingTonemappingPass(eyeAdaptationPass != null);
            tonemappingPass.setDependency(0, sceneColorTexture, 0);
            tonemappingPass.setDependency(1, lastPass, 0);
            if(eyeAdaptationPass != null)
                tonemappingPass.setDependency(2, eyeAdaptationPass, 0);
            context.appendRenderPass("LightEffectTonemapping", tonemappingPass);
        }else{// Compose the light effect and scene color.
            PostProcessingCombinePass lightEffectCombine = new PostProcessingCombinePass();  // TODO: blend factor
            lightEffectCombine.setDependency(0, sceneColorTexture, 0);
            lightEffectCombine.setDependency(1, lastPass, 0);
            context.appendRenderPass("LightEffectCombine", lightEffectCombine);
        }
    }

    @Override
    public String getEffectName() {
        return PostProcessing.LIGHT_EFFECT;
    }

    @Override
    public int getPriority() {
        return PostProcessing.LIGHT_EFFECT_PRIPORTY;
    }
}
