package jet.opengl.postprocessing.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.util.CommonUtil;
import jet.opengl.postprocessing.util.LogUtil;

/**
 * Created by mazhen'gui on 2017/4/17.
 */

public class PostProcessing {
    public static final String BLOOM = "BLOOM";
    public static final String FXAA = "FXAA";
    public static final String CLIP_TEXTURE = "CLIP_TEXTURE";
    public static final String GUASSION_BLUR = "GUASSION_BLUR";
    public static final String RADIAL_BLUR = "RADIAL_BLUR";
    public static final String FISH_EYE = "FISH_EYE";

    private PostProcessingRenderContext m_RenderContext;

    private final List<EffectTag> m_CurrentEffects = new ArrayList<>();
    private final List<EffectTag> m_PrevEffects    = new ArrayList<>();
    private final LinkedHashMap<String, PostProcessingRenderPass> m_AddedRenderPasses = new LinkedHashMap<>();
    private final PostProcessingParameters m_Parameters = new PostProcessingParameters();
    private final Map<String, PostProcessingEffect> m_RegisteredEffects = new HashMap<>();
    private final Map<String, Object> m_CustomeEffectUniformData = new HashMap<>();

    private PostProcessingRenderPass m_LastAddedPass;

    private boolean m_bEnablePostProcessing;
    private boolean m_SplitScreenDebug;
    private boolean m_bUsePortionTex;

    public void registerEffect(PostProcessingEffect effect){
        if(GLCheck.CHECK){
            if(m_RegisteredEffects.containsKey(effect.getEffectName())){
                LogUtil.e(LogUtil.LogType.DEFAULT, "Dumplicate Effect: name = " + effect.getEffectName());
            }
        }

        m_RegisteredEffects.put(effect.getEffectName(), effect);
    }

    public void addEffect(String name, Object uniformParams, Object initParams){
        PostProcessingEffect effect = m_RegisteredEffects.get(name);
        if(effect == null)
            throw new NullPointerException("No found the Effect by name: " + name);
        m_CurrentEffects.add(new EffectTag(name, effect.getPriority(), initParams));
        m_CustomeEffectUniformData.put(name, uniformParams);
    }

    public void performancePostProcessing(PostProcessingFrameAttribs frameAttribs){
        prepare(frameAttribs);

        if(!m_bEnablePostProcessing){
            if(frameAttribs.sceneColorTexture != null) {
                m_RenderContext.renderTo(frameAttribs.sceneColorTexture, frameAttribs.outputTexture, frameAttribs.viewport);
            }

            return;
        }

        m_RenderContext.performancePostProcessing(/* TODO: don't forget the parameters */);
        //      checkGLError();

        if (!m_AddedRenderPasses.isEmpty()) {
            int size = m_AddedRenderPasses.size();
            Texture2D src = m_AddedRenderPasses.get(size-1).getOutputTexture(0);
            if (m_bUsePortionTex) {
                // TODO The two step can combine in one pass.
                m_RenderContext.renderTo(frameAttribs.sceneColorTexture, frameAttribs.outputTexture, frameAttribs.viewport);
                m_RenderContext.renderTo(src, frameAttribs.outputTexture, frameAttribs.clipRect);
            }
            else {
                m_RenderContext.renderTo(src, frameAttribs.outputTexture, frameAttribs.viewport);
            }

            m_RenderContext.finish();
        }else /*if(m_OutputToScreen || frameAttribs.SceneColorBuffer != nullptr)*/{
            m_RenderContext.renderTo(frameAttribs.sceneColorTexture, frameAttribs.outputTexture, m_bUsePortionTex? frameAttribs.clipRect: frameAttribs.viewport);
        }
    }

    private void initlizeContext(){
        if(m_RenderContext == null){
            m_RenderContext = new PostProcessingRenderContext();
            m_RenderContext.initlizeGL(0,0);
            m_RenderContext.m_Parameters = m_Parameters;
        }
    }

    private void prepare(PostProcessingFrameAttribs frameAttribs){
        initlizeContext();

        if(!m_bEnablePostProcessing){
            return;
        }


        m_CurrentEffects.sort(null);
        if(m_CurrentEffects.size() != m_PrevEffects.size() ||  !m_CurrentEffects.equals(m_PrevEffects)){
            m_AddedRenderPasses.clear();
            m_LastAddedPass = null;

            for(EffectTag effectTag : m_CurrentEffects){
                PostProcessingEffect effect = m_RegisteredEffects.get(effectTag.name);
                // TODO
                effect.fillRenderPass(this, frameAttribs.sceneColorTexture, frameAttribs.sceneDepthTexture);
            }

            m_LastAddedPass.setDependencies(0,1);
        }

    }

    public void appendRenderPass(String name, PostProcessingRenderPass renderPass){
        if(renderPass == null){
            throw new NullPointerException("renderPass is null");
        }

        m_AddedRenderPasses.put(name, renderPass);
        m_LastAddedPass = renderPass;
    }

    public PostProcessingRenderPass getLastPass(){
        return m_LastAddedPass;
    }

    public PostProcessingRenderPass findPass(String name){
        return m_AddedRenderPasses.get(name);
    }

    private static final class EffectTag implements Comparable<EffectTag>{
        String name;
        Object value;
        int priority;

        public EffectTag(String name, int priority, Object value) {
            this.name = name;
            this.priority = priority;
            this.value = value;
        }

        public EffectTag(String name, int priority) {
            this.name = name;
            this.priority = priority;
            this.value = null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            EffectTag effectTag = (EffectTag) o;

            if(priority != effectTag.priority)
                return false;

            if(!name.equals(effectTag.name))
                return false;

            return CommonUtil.equals(value, effectTag.value);
        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + priority;
            result = 31 * result + (value != null ? value.hashCode(): 0);
            return result;
        }

        @Override
        public int compareTo(EffectTag effectTag) {
            return priority - effectTag.priority;
        }
    }
}
