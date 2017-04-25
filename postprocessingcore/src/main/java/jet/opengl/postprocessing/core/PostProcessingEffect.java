package jet.opengl.postprocessing.core;

/**
 * Created by mazhen'gui on 2017/4/17.
 */

public abstract class PostProcessingEffect {
    PostProcessingRenderPass m_LastRenderPass;
    Object initValue;
    Object uniformValue;

    protected abstract void fillRenderPass(PostProcessing context, PostProcessingRenderPass sceneColorTexture, PostProcessingRenderPass sceneDepthTexture);

    protected PostProcessingRenderPass getLastRenderPass(){

        return m_LastRenderPass;
    }

    public abstract String getEffectName();
    public abstract int getPriority();

    protected Object getInitValue() {return initValue;}
    protected Object getUniformValue() {return uniformValue;}
}
