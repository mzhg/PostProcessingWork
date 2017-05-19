package jet.opengl.postprocessing.core;

/**
 * Created by mazhen'gui on 2017/4/17.
 */

public abstract class PostProcessingEffect {
    PostProcessingRenderPass m_LastRenderPass;
    PostProcessingParameters m_Parameters;
    PostProcessingFrameAttribs m_FrameAttribs;

    Object initValue;
    Object uniformValue;
    boolean isLastEffect;

    protected void fillRenderPass(PostProcessing context, PostProcessingCommonData commonData){
        fillRenderPass(context, commonData.sceneColorTexture, commonData.sceneDepthTexture);
    }

    @Deprecated
    protected abstract void fillRenderPass(PostProcessing context, PostProcessingRenderPass sceneColorTexture, PostProcessingRenderPass sceneDepthTexture);

    protected PostProcessingRenderPass getLastRenderPass(){

        return m_LastRenderPass;
    }

    public abstract String getEffectName();
    public abstract int getPriority();

    protected Object getInitValue() {return initValue;}
    protected Object getUniformValue() {return uniformValue;}
    protected boolean isLastEffect() { return isLastEffect;}
    protected PostProcessingParameters getParameters() {return m_Parameters;}
    protected PostProcessingFrameAttribs getFrameAttribs() {return m_FrameAttribs;}
}
