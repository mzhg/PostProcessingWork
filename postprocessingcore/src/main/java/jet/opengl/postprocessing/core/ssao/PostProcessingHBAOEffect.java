package jet.opengl.postprocessing.core.ssao;

import jet.opengl.postprocessing.common.GLAPI;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.core.PostProcessing;
import jet.opengl.postprocessing.core.PostProcessingCombinePass;
import jet.opengl.postprocessing.core.PostProcessingDownsamplePass;
import jet.opengl.postprocessing.core.PostProcessingEffect;
import jet.opengl.postprocessing.core.PostProcessingGaussionBlurPass;
import jet.opengl.postprocessing.core.PostProcessingRenderPass;
import jet.opengl.postprocessing.texture.Texture2D;

/**
 * Created by mazhen'gui on 2017/4/24.
 */

public class PostProcessingHBAOEffect extends PostProcessingEffect {

    @Override
    protected void fillRenderPass(PostProcessing context, PostProcessingRenderPass sceneColorTexture, PostProcessingRenderPass sceneDepthTexture) {
        final PostProcessingRenderPass lastPass = getLastRenderPass();

        Texture2D texture = sceneColorTexture.getOutputTexture(0);
        final int sampleCount = texture.getSampleCount();

        for(int i = 0; i < sampleCount; i++){

        }

        PostProcessingDownsamplePass downsamplePass = new PostProcessingDownsamplePass();
        { // dowmsample the scene
            if(lastPass == null){
                downsamplePass.setDependency(0, sceneColorTexture, 0);
            }else{
                downsamplePass.setDependency(0, lastPass, 0);
            }

            int dowmsampleIndex = 0;
            context.appendRenderPass("Downsample2x2_" + dowmsampleIndex, downsamplePass);
            dowmsampleIndex++;

            if(GLFuncProviderFactory.getGLFuncProvider().getHostAPI() == GLAPI.ANDROID){
                int count = 3;
                int width = sceneColorTexture.getInput(0).getWidth();
                int height = sceneColorTexture.getInput(0).getHeight();

                PostProcessingDownsamplePass lastdownsamplePass = downsamplePass;
                while (count > 0 && width >= 256 && height >= 256){
                    downsamplePass = new PostProcessingDownsamplePass();
                    downsamplePass.setDependency(0, lastdownsamplePass, 0);

                    context.appendRenderPass("Downsample2x2_" + dowmsampleIndex, downsamplePass);
                    dowmsampleIndex++;

                    lastdownsamplePass = downsamplePass;

                    width /= 2;
                    height /= 2;
                    count--;
                }
            }
        }

        // Bloom Setup
        PostProcessingBloomSetupPass bloomSetupPass =new PostProcessingBloomSetupPass();
        bloomSetupPass.setDependency(0, downsamplePass, 0);
        context.appendRenderPass("BloomSetup", bloomSetupPass);

        // Gaussion Blur
        PostProcessingGaussionBlurPass gaussionBlurPass = new PostProcessingGaussionBlurPass(7);
        gaussionBlurPass.setDependency(0, bloomSetupPass, 0);
        context.appendRenderPass("Bloom::GaussionBlur", gaussionBlurPass);

        // Combine the source image and the blured image.
        PostProcessingCombinePass combinePass = new PostProcessingCombinePass();
        combinePass.setDependency(0, sceneColorTexture, 0);
        combinePass.setDependency(1, gaussionBlurPass, 0);
        context.appendRenderPass("Bloom::CombinePass", combinePass);
    }

    @Override
    public String getEffectName() {
        return PostProcessing.BLOOM;
    }

    @Override
    public int getPriority() {
        return PostProcessing.BLOOM_PRIPORTY;
    }
}
