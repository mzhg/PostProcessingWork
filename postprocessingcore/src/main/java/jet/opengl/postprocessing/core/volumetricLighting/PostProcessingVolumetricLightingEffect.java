package jet.opengl.postprocessing.core.volumetricLighting;

import jet.opengl.postprocessing.core.PostProcessing;
import jet.opengl.postprocessing.core.PostProcessingEffect;
import jet.opengl.postprocessing.core.PostProcessingRenderPass;

/**
 * Created by mazhen'gui on 2017/5/17.
 */

public class PostProcessingVolumetricLightingEffect extends PostProcessingEffect {

    @Override
    protected void fillRenderPass(PostProcessing context, PostProcessingRenderPass sceneColorTexture, PostProcessingRenderPass sceneDepthTexture) {

    }

    @Override
    public String getEffectName() {
        return PostProcessing.VOLUMETRIC_LIGHTING;
    }

    @Override
    public int getPriority() {
        return PostProcessing.VOLUMETRIC_LIGHTING_PRIPORTY;
    }
}
