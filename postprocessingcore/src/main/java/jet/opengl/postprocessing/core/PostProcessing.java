package jet.opengl.postprocessing.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLAPI;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLStateTracker;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.bloom.PostProcessingBloomEffect;
import jet.opengl.postprocessing.core.dof.PostProcessingDOFBokehEffect;
import jet.opengl.postprocessing.core.eyeAdaption.PostProcessingEyeAdaptationEffect;
import jet.opengl.postprocessing.core.fisheye.PostProcessingFishEyeEffect;
import jet.opengl.postprocessing.core.fxaa.PostProcessingFXAAEffect;
import jet.opengl.postprocessing.core.light.PostProcessingLightEffect;
import jet.opengl.postprocessing.core.radialblur.PostProcessingRadialBlurEffect;
import jet.opengl.postprocessing.core.toon.PostProcessingToonEffect;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureDataDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CommonUtil;
import jet.opengl.postprocessing.util.FileUtils;
import jet.opengl.postprocessing.util.LogUtil;
import jet.opengl.postprocessing.util.Numeric;

/**
 * Created by mazhen'gui on 2017/4/17.
 */

public class PostProcessing implements Disposeable{
    public static final String BLOOM = "BLOOM";
    public static final String FXAA = "FXAA";
    public static final String CLIP_TEXTURE = "CLIP_TEXTURE";
    public static final String GUASSION_BLUR = "GUASSION_BLUR";
    public static final String RADIAL_BLUR = "RADIAL_BLUR";
    public static final String TOON = "TOON";
    public static final String FISH_EYE = "FISH_EYE";
    public static final String LIGHT_EFFECT = "LIGHT_EFFECT";
    public static final String EYE_ADAPATION = "EYE_ADAPATION";
    public static final String DOF_BOKEH = "DOF_BOKEH";

    private static final int NUM_TAG_CACHE = 32;

    public static final int RADIAL_BLUR_PRIPORTY = 0;
    public static final int TOON_PRIPORTY = 100;
    public static final int FISH_EYE_PRIPORTY = 50;
    public static final int BLOOM_PRIPORTY = 200;
    public static final int FXAA_PRIPORTY = 1000;
    public static final int LIGHT_EFFECT_PRIPORTY = 2000;
    public static final int DOF_BOKEH_PRIPORTY = 3000;
    public static final int EYE_ADAPATION_PRIPORTY = -100;

    private PostProcessingRenderContext m_RenderContext;

    private final List<EffectTag> m_CurrentEffects = new ArrayList<>();
    private final List<EffectTag> m_PrevEffects    = new ArrayList<>();
    private final LinkedHashMap<String, PostProcessingRenderPass> m_AddedRenderPasses = new LinkedHashMap<>();
    private final PostProcessingParameters m_Parameters;
    private final Map<String, PostProcessingEffect> m_RegisteredEffects = new HashMap<>();
    private final EffectTag[] m_TagCaches = new EffectTag[NUM_TAG_CACHE];
    private int m_TagCount = 0;

    private PostProcessingRenderPass m_LastAddedPass;

    private boolean m_bEnablePostProcessing = true;
    private boolean m_SplitScreenDebug;
    private boolean m_bUsePortionTex;
    private Texture2D m_DefaultLensMask;

    public PostProcessing(){
        m_Parameters = new PostProcessingParameters(this);
        registerEffect(new PostProcessingRadialBlurEffect());
        registerEffect(new PostProcessingBloomEffect());
        registerEffect(new PostProcessingFishEyeEffect());
        registerEffect(new PostProcessingToonEffect());
        registerEffect(new PostProcessingFXAAEffect());
        registerEffect(new PostProcessingLightEffect());
        registerEffect(new PostProcessingEyeAdaptationEffect());
        registerEffect(new PostProcessingDOFBokehEffect());
    }

    public void registerEffect(PostProcessingEffect effect){
        if(GLCheck.CHECK){
            if(m_RegisteredEffects.containsKey(effect.getEffectName())){
                LogUtil.e(LogUtil.LogType.DEFAULT, "Dumplicate Effect: name = " + effect.getEffectName());
            }
        }

        m_RegisteredEffects.put(effect.getEffectName(), effect);
    }

    public void addEffect(String name, Object initParams, Object uniformParams){
        PostProcessingEffect effect = m_RegisteredEffects.get(name);
        if(effect == null)
            throw new NullPointerException("No found the Effect by name: " + name);
        effect.initValue = initParams;
        effect.uniformValue = uniformParams;
        m_CurrentEffects.add(obtain(name, effect.getPriority(), initParams, uniformParams));
    }

    /*
    Object getEffectInitValue(String name){
        EffectTag value = customeEffectData.get(name);
        if(value == null)
            return null;
        else
            return value.initValue;
    }

    Object getEffectUniformValue(String name){
        EffectTag value = customeEffectData.get(name);
        if(value == null)
            return null;
        else
            return value.uniformValue;
    }
*/
    public void performancePostProcessing(PostProcessingFrameAttribs frameAttribs){
        prepare(frameAttribs);

        GLStateTracker.getInstance().saveStates();
        try{
            if(!m_bEnablePostProcessing){
                if(frameAttribs.sceneColorTexture != null) {
                    m_RenderContext.renderTo(frameAttribs.sceneColorTexture, frameAttribs.outputTexture, frameAttribs.viewport);
                }
                return;
            }

            m_RenderContext.performancePostProcessing(frameAttribs.outputTexture, frameAttribs.viewport);
            //      checkGLError();
            if (!m_AddedRenderPasses.isEmpty()) {
//                int size = m_AddedRenderPasses.size();
//                Texture2D src;
//                if(size > 2) {
//                    src = m_LastAddedPass.getOutputTexture(0);
//                }else{
//                    src = frameAttribs.sceneColorTexture;
//                }
//                if (m_bUsePortionTex) {
//                    // TODO The two step can combine in one pass.
//                    m_RenderContext.renderTo(frameAttribs.sceneColorTexture, frameAttribs.outputTexture, frameAttribs.viewport);
//                    m_RenderContext.renderTo(src, frameAttribs.outputTexture, frameAttribs.clipRect);
//                }
//                else {
//                    m_RenderContext.renderTo(src, frameAttribs.outputTexture, frameAttribs.viewport);
//                }
//                m_RenderContext.finish();
            }else /*if(m_OutputToScreen || frameAttribs.SceneColorBuffer != nullptr)*/{
                m_RenderContext.renderTo(frameAttribs.sceneColorTexture, frameAttribs.outputTexture, m_bUsePortionTex? frameAttribs.clipRect: frameAttribs.viewport);
            }

        }finally {
            GLStateTracker.getInstance().restoreStates();
            GLStateTracker.getInstance().reset();
        }
    }

    private EffectTag obtain(String name,int priority){
        return obtain(name, priority, null, null);
    }

    private EffectTag obtain(String name,int priority,Object initValue, Object uniformValue){
        if(m_TagCount > 0){
            m_TagCount--;
            EffectTag tag = m_TagCaches[m_TagCount];
            tag.name = name;
            tag.initValue = initValue;
            tag.uniformValue = uniformValue;
            tag.priority = priority;
            return tag;
        }else{
            return new EffectTag(name, priority, initValue, uniformValue);
        }
    }

    private void releaseTags(List<EffectTag> tags){
        final int offset=m_TagCount;
        final int end = Math.min(NUM_TAG_CACHE, m_TagCount + tags.size());

        for(; m_TagCount < end; m_TagCount ++){
            m_TagCaches[m_TagCount] = tags.get(m_TagCount - offset);
        }
        tags.clear();
    }

    private void initlizeContext(){
        if(m_RenderContext == null){
            m_RenderContext = new PostProcessingRenderContext();
            m_RenderContext.initlizeGL(0,0);
            m_RenderContext.m_Parameters = m_Parameters;
        }
    }

    public boolean isHDREnabled(){
        // TODO  return true for debug only!!!
        return true;
    }

    private void prepare(PostProcessingFrameAttribs frameAttribs){
        initlizeContext();

        m_RenderContext.m_FrameAttribs = frameAttribs;
        if(!frameAttribs.viewport.isValid()){
            frameAttribs.viewport.set(0,0, frameAttribs.sceneColorTexture.getWidth(), frameAttribs.sceneColorTexture.getHeight());
        }

        if(m_CurrentEffects.isEmpty() && m_PrevEffects.isEmpty()){
            return;
        }

        m_CurrentEffects.sort(null);
        if(m_CurrentEffects.size() != m_PrevEffects.size() || !m_CurrentEffects.equals(m_PrevEffects)){
            m_AddedRenderPasses.clear();
            m_LastAddedPass = null;

            PostProcessingRenderPassInput colorInputPass = new PostProcessingRenderPassInput("SceneColor", frameAttribs.sceneColorTexture);
            PostProcessingRenderPassInput depthInputPass = new PostProcessingRenderPassInput("SceneDepth", frameAttribs.sceneDepthTexture);

//            m_AddedRenderPasses.put("SceneColor", colorInputPass);  TODO maybe cause problems
//            m_AddedRenderPasses.put("SceneDepth", depthInputPass);
            EffectTag lastTag = null;
            if(m_CurrentEffects.size() > 0)
                lastTag = m_CurrentEffects.get(m_CurrentEffects.size() - 1);

            for(EffectTag effectTag : m_CurrentEffects){
                PostProcessingEffect effect = m_RegisteredEffects.get(effectTag.name);

                effect.isLastEffect = (lastTag == effectTag);
                effect.m_LastRenderPass = m_LastAddedPass;
                effect.initValue = effectTag.initValue;
                effect.uniformValue = effectTag.uniformValue;
                effect.m_Parameters = m_Parameters;

                effect.fillRenderPass(this, colorInputPass, depthInputPass);
            }

            if(m_LastAddedPass != null) {
                m_LastAddedPass.setDependencies(0, 1);
            }

            m_RenderContext.setRenderPasses(m_AddedRenderPasses.values());
        }

        releaseTags(m_PrevEffects);
        m_PrevEffects.addAll(m_CurrentEffects);
        m_CurrentEffects.clear();
    }

    public void appendRenderPass(String name, PostProcessingRenderPass renderPass){
        if(renderPass == null){
            throw new NullPointerException("renderPass is null");
        }

        m_AddedRenderPasses.put(name, renderPass);
        m_LastAddedPass = renderPass;
    }

    /*
    public PostProcessingRenderPass getLastPass(){
        return m_LastAddedPass;
    }*/

    public PostProcessingRenderPass findPass(String name){
        return m_AddedRenderPasses.get(name);
    }

    public void addDOFBokeh(float focalDepth, float focalRange, float fstop){
        m_Parameters.focalDepth = focalDepth;
        m_Parameters.focalLength = focalRange;
        m_Parameters.fstop = fstop;

        PostProcessingEffect effect = m_RegisteredEffects.get(DOF_BOKEH);
        m_CurrentEffects.add(obtain(effect.getEffectName(), effect.getPriority(), null, null));
    }

    public void addEyeAdaptation(){
        PostProcessingEffect effect = m_RegisteredEffects.get(EYE_ADAPATION);
        m_CurrentEffects.add(obtain(effect.getEffectName(), effect.getPriority(), null, null));
    }

    public void addLightEffect(boolean enableLightStreaker, boolean enableLensFlare, float elpsedTime){
        m_Parameters.enableLensFlare = enableLensFlare;
        m_Parameters.enableLightStreaker = enableLightStreaker;
        m_Parameters.elapsedTime = elpsedTime;

        PostProcessingEffect effect = m_RegisteredEffects.get(LIGHT_EFFECT);
        m_CurrentEffects.add(obtain(effect.getEffectName(), effect.getPriority(), null, null));
    }

    public void addRadialBlur(float centerX, float centerY){
        int samples = GLFuncProviderFactory.getGLFuncProvider().getHostAPI() == GLAPI.ANDROID ? 12 : 24;
        addRadialBlur(centerX, centerY, samples);
    }

    public void addRadialBlur(float centerX, float centerY, int samples){
        m_Parameters.radialBlurCenterX =centerX;
        m_Parameters.radialBlurCenterY = centerY;
        m_Parameters.radialBlurSamples = samples;

        PostProcessingEffect effect = m_RegisteredEffects.get(RADIAL_BLUR);
        m_CurrentEffects.add(obtain(effect.getEffectName(), effect.getPriority(), null, null));
    }

    public void addToon(){
        addToon(0.2f, 5.0f);
    }

    public void addToon(float edgeThreshold, float edgeThreshold2){
        m_Parameters.edgeThreshold = edgeThreshold;
        m_Parameters.edgeThreshold2 = edgeThreshold2;

        PostProcessingEffect effect = m_RegisteredEffects.get(TOON);
        m_CurrentEffects.add(obtain(effect.getEffectName(), effect.getPriority(), null, null));
    }

    public void addFishEye(float factor){
        m_Parameters.fishEyeFactor = factor;

        PostProcessingEffect effect = m_RegisteredEffects.get(FISH_EYE);
        m_CurrentEffects.add(obtain(effect.getEffectName(), effect.getPriority(), null, null));
    }

    public void addBloom(){
        addBloom(0.25f, 1.02f, 1.12f);
    }

    public void addBloom(float bloomThreshold, float exposureScale, float bloomIntensity){
        m_Parameters.bloomThreshold = Math.max(bloomThreshold, 0.25f);
        m_Parameters.exposureScale = exposureScale;
        m_Parameters.bloomIntensity = Math.max(bloomIntensity, 0.01f);

        PostProcessingEffect effect = m_RegisteredEffects.get(BLOOM);
        m_CurrentEffects.add(obtain(effect.getEffectName(), effect.getPriority(), null, null));
    }

    /**
     * Add the FXAA post-processing.
     * @param quality The FXAA qyality, ranged [0, 5]
     */
    public void addFXAA(int quality){
        m_Parameters.fxaaQuality = Numeric.clamp(quality, 0, 5);

        PostProcessingEffect effect = m_RegisteredEffects.get(FXAA);
        m_CurrentEffects.add(obtain(effect.getEffectName(), effect.getPriority(), m_Parameters.fxaaQuality, null));
    }

    Texture2D getOrCreateLensMask(){
        if(m_DefaultLensMask == null){
            try {
                byte[] data = FileUtils.loadBytes("shader_libs/PostProcessingDefaultLensMask.data");
                Texture2DDesc desc = new Texture2DDesc(128,128, GLenum.GL_RGBA8);
                TextureDataDesc dataDesc = new TextureDataDesc(GLenum.GL_RGBA, GLenum.GL_UNSIGNED_BYTE, data);
                m_DefaultLensMask = TextureUtils.createTexture2D(desc, dataDesc);
                GLStateTracker.getInstance().bindTexture(m_DefaultLensMask, 0,0 );

                m_DefaultLensMask.setMagFilter(GLenum.GL_LINEAR);
                m_DefaultLensMask.setMinFilter(GLenum.GL_LINEAR);
                m_DefaultLensMask.setWrapS(GLenum.GL_CLAMP_TO_EDGE);
                m_DefaultLensMask.setWrapT(GLenum.GL_CLAMP_TO_EDGE);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return  m_DefaultLensMask;
    }

    @Override
    public void dispose() {
        PostProcessingRenderPass.releaseResources();

        if(m_DefaultLensMask != null){
            m_DefaultLensMask.dispose();
            m_DefaultLensMask = null;
        }
    }

    private static final class EffectTag implements Comparable<EffectTag>{
        String name;
        int priority;

        Object initValue;
        Object uniformValue;

        public EffectTag(String name, int priority, Object initValue, Object uniformValue) {
            this.name = name;
            this.priority = priority;
            this.initValue = initValue;
            this.uniformValue = uniformValue;
        }

        public EffectTag(String name, int priority) {
            this.name = name;
            this.priority = priority;
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

            return CommonUtil.equals(initValue, effectTag.initValue);

            // TODO igore the uniformValue, it dynamic
        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + priority;
            result = 31 * result + (initValue != null ? initValue.hashCode(): 0);
            return result;
        }

        @Override
        public int compareTo(EffectTag effectTag) {
            return priority - effectTag.priority;
        }
    }
}
