package jet.opengl.postprocessing.core;

import jet.opengl.postprocessing.texture.Texture2D;

/**
 * Created by mazhen'gui on 2017/4/17.
 */

public abstract class PostProcessingEffect {
    PostProcessingRenderPass m_LastRenderPass;

    protected abstract void fillRenderPass(PostProcessing context, Texture2D sceneColorTexture, Texture2D sceneDepthTexture);

    protected PostProcessingRenderPass getLastRenderPass(){

        return m_LastRenderPass;
    }

    public abstract String getEffectName();
    public abstract int getPriority();
}
