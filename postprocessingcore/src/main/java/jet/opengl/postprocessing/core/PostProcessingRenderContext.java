package jet.opengl.postprocessing.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jet.opengl.postprocessing.buffer.AttribDesc;
import jet.opengl.postprocessing.buffer.BufferBinding;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.buffer.VertexArrayObject;
import jet.opengl.postprocessing.common.BlendState;
import jet.opengl.postprocessing.common.DepthStencilState;
import jet.opengl.postprocessing.common.GLAPIVersion;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLStateTracker;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.common.RasterizerState;
import jet.opengl.postprocessing.shader.FullscreenProgram;
import jet.opengl.postprocessing.shader.GLSLProgramPipeline;
import jet.opengl.postprocessing.texture.AttachType;
import jet.opengl.postprocessing.texture.RenderTargets;
import jet.opengl.postprocessing.texture.RenderTexturePool;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.Texture3D;
import jet.opengl.postprocessing.texture.TextureAttachDesc;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.Recti;

/**
 * Created by mazhen'gui on 2017/4/17.
 */

public final class PostProcessingRenderContext {

    private static final Texture2DDummy g_DummyTex = new Texture2DDummy();

    private final List<PostProcessingRenderPass> m_RenderPassList = new ArrayList<>();
    private PostProcessingRenderPass m_PrevPass;
    private PostProcessingRenderPass m_CurrentPass;

    private RenderTargets m_RenderTargets;

    private VertexArrayObject m_CurrentVAO;
    private VertexArrayObject m_DefaultVAO;
    private BufferGL          m_BufferQuad;
    private GLStateTracker    m_StateTracker;

//    private OpenGLProgram m_CurrentProgram;
//    private GLSLProgram m_DefaultProgram;

    PostProcessingParameters m_Parameters;
    PostProcessingFrameAttribs m_FrameAttribs;

    private TextureAttachDesc[] m_AttachDescs =new TextureAttachDesc[8];  // Default attchment description
    private GLFuncProvider gl;
    private String m_PassName;  // For debugging
    private boolean m_Initlized = false;
    private FullscreenProgram m_ScreenQuadProgram;
    private boolean m_bForceTextureBinding = true;

    public PostProcessingRenderContext(){
        for(int i = 0; i < m_AttachDescs.length; i++){
            m_AttachDescs[i] = new TextureAttachDesc(i, AttachType.TEXTURE, 0, 0);
        }
    }

    void initlizeGL(int width, int height){
        if(m_Initlized)
            return;

        m_StateTracker = GLStateTracker.getInstance();
        gl = GLFuncProviderFactory.getGLFuncProvider();
        GLAPIVersion version = gl.getGLAPIVersion();
        m_RenderTargets = new RenderTargets();
        m_RenderTargets.initlize();

        // Initlize the VAO
        m_DefaultVAO = new VertexArrayObject();
        if(version.major >= 3){
            m_DefaultVAO.initlize(null, null);
        }else{
            // triangle strip
            float[] screenPos = {
              -1,-1,  +1,-1,
              -1,+1,  +1,+1,
            };

            m_BufferQuad = new BufferGL();
            m_BufferQuad.initlize(GLenum.GL_ARRAY_BUFFER, screenPos.length * 4, CacheBuffer.wrap(screenPos), GLenum.GL_STATIC_DRAW);
            AttribDesc desc = new AttribDesc();
            desc.index = 0;
            desc.size = 2;
            desc.divisor = 0;
            desc.normalized = false;
            desc.offset = 0;
            desc.stride= 0;
            desc.type = GLenum.GL_FLOAT;

            m_DefaultVAO.initlize(new BufferBinding[]{new BufferBinding(m_BufferQuad, desc)}, null);
        }

        m_ScreenQuadProgram = new FullscreenProgram();
    }

    public void setViewport(int x, int y, int width, int height){
        m_StateTracker.setViewport(x, y, width, height);
    }

    public void setRenderTarget(Texture2D tex){
        if(tex != g_DummyTex) {
            m_StateTracker.setFramebuffer(m_RenderTargets.getFramebuffer());
            if(tex.getArraySize() > 1)
                m_AttachDescs[0].type = AttachType.TEXTURE;
            else
                m_AttachDescs[0].type = AttachType.TEXTURE_2D;

            m_RenderTargets.setRenderTexture(tex, m_AttachDescs[0]);
        }else{ // tex == g_DummyTex
            m_StateTracker.setFramebuffer(0);
        }
    }

    public void setRenderTargetLayer(Texture3D tex, int slice){
        m_StateTracker.setFramebuffer(m_RenderTargets.getFramebuffer());
        m_AttachDescs[0].type = AttachType.TEXTURE_LAYER;
        m_AttachDescs[0].layer = slice;
        m_AttachDescs[0].level = 0;
        m_RenderTargets.setRenderTexture(tex, m_AttachDescs[0]);
    }

    public void setRenderTargets(Texture2D[] texs){
        m_StateTracker.setFramebuffer(m_RenderTargets.getFramebuffer());
        m_RenderTargets.setRenderTextures(texs, m_AttachDescs);
    }

    public void setProgram(OpenGLProgram program){
//        if(program == null){
//            m_StateTracker.setProgram(m_DefaultProgram);
//        }else{
//
//            m_CurrentProgram = program;
//        }

        m_StateTracker.setProgram(program);
    }

    public void bindTexture(TextureGL textureGL, int unit, int sampler){
        if(m_bForceTextureBinding){
            if(textureGL != null) {
                gl.glActiveTexture(GLenum.GL_TEXTURE0 + unit);
                gl.glBindTexture(textureGL.getTarget(), textureGL.getTexture());
            }else{
                gl.glBindTextureUnit(unit, 0);
            }

            gl.glBindSampler(unit, sampler);
        }else {
            m_StateTracker.bindTexture(textureGL, unit, sampler);
        }
    }

    public void bindTextures(TextureGL[] textures, int[] units, int[] samplers) {
        if(m_bForceTextureBinding){
            for(int i = 0; i < textures.length; i++){
                TextureGL textureGL = textures[i];
                int unit = (units != null ? units[i] : i);
                int sampler = (samplers != null ? samplers[i] : 0);

                if(textureGL != null) {
                    gl.glActiveTexture(GLenum.GL_TEXTURE0 + unit);
                    gl.glBindTexture(textureGL.getTarget(), textureGL.getTexture());
                }else{
                    gl.glBindTextureUnit(unit, 0);
                }

                gl.glBindSampler(unit, sampler);
            }
        }else {
            m_StateTracker.bindTextures(textures, units, samplers);
        }
    }

    public void setBlendState(BlendState state) {m_StateTracker.setBlendState(state);}
    public void setDepthStencilState(DepthStencilState state) {m_StateTracker.setDepthStencilState(state);}
    public void setRasterizerState(RasterizerState state) {m_StateTracker.setRasterizerState(state);}

    public void setProgramPipeline(GLSLProgramPipeline pipeline){
        m_StateTracker.setProgramPipeline(pipeline);
    }

    public void setVAO(VertexArrayObject vao){
        // TODO: need setup defualt vao if the specified vao is null.

        m_StateTracker.setVAO(vao);
    }

    private void flush(){
        if(GLCheck.CHECK)
            m_StateTracker.checkFlags("", true);
    }

    public void drawArrays(int mode, int offset, int count){
        flush();

        gl.glDrawArrays(mode, offset, count);
    }

    public void drawArrays(int mode, int offset, int count, int instanceCount){
        flush();
        gl.glDrawArraysInstanced(mode, offset, count, instanceCount);
    }

    public void drawElements(int mode, int count, int type, long offset){
        flush();

        gl.glDrawElements(mode, count, type, offset);
    }

    public void drawFullscreenQuad(){
        drawArrays(GLenum.GL_TRIANGLES, 0, 3);
    }

    void setRenderPasses(Collection<PostProcessingRenderPass> inRenderPasses){
        for(PostProcessingRenderPass renderPass : m_RenderPassList){
            renderPass.dispose();
        }
        m_RenderPassList.clear();

        m_RenderPassList.addAll(inRenderPasses);
    }

    void renderTo(Texture2D src, Texture2D dst, Recti viewport){
        GLCheck.checkError("renderTo: begin");
        m_StateTracker.clearFlags(false);

        setViewport(viewport.x, viewport.y, viewport.width, viewport.height);
        if(dst != null) {
            setRenderTarget(dst);
        }else{
            m_StateTracker.setFramebuffer(0);
        }

        m_StateTracker.bindTexture(src, 0, 0);
        m_StateTracker.setVAO(null);
        m_StateTracker.setProgram(m_ScreenQuadProgram);
        m_StateTracker.setBlendState(null);
        m_StateTracker.setDepthStencilState(null);
        m_StateTracker.setRasterizerState(null);
        drawFullscreenQuad();

        // Use the current state settinmg
//        gl.glViewport(0, 0, viewport.width, viewport.height);
//        gl.glActiveTexture(GLenum.GL_TEXTURE0);
//        gl.glBindTexture(GLenum.GL_TEXTURE_2D, src.getTexture());
//        gl.glDrawArrays(GLenum.GL_TRIANGLE_STRIP, 0, 4);

        if(GLCheck.CHECK)
            GLCheck.checkError("renderTo: end");
    }

    private final Texture2DDesc outputDesc = new Texture2DDesc();
    private final List<PostProcessingRenderPass> currentDependencyPasses = new ArrayList<>();
    private final List<TextureGL> inputTextures = new ArrayList<>();

    void performancePostProcessing(Texture2D output, Recti viewport){
        if(m_RenderPassList.isEmpty())
            return;

        for(PostProcessingRenderPass pass : m_RenderPassList){
            pass.reset();
        }

//        PostProcessingRenderPass lastPass = m_RenderPassList.get(m_RenderPassList.size() - 1);
        for(PostProcessingRenderPass it : m_RenderPassList){
            m_CurrentPass = it;

            if(m_CurrentPass.m_bProcessed){
                continue;
            }

            currentDependencyPasses.clear();
            inputTextures.clear();

            int inputCount = m_CurrentPass.getInputCount();
            for(int i = 0; i < inputCount; i++){
                InputDesc inputDesc = m_CurrentPass.getInputDesc(i);
                PostProcessingRenderPass inputPass = inputDesc.dependencyPass;
                if(inputPass != null){
                    currentDependencyPasses.add(inputPass);
                    inputPass.markOutputSlot(inputDesc.slot);
                    inputTextures.add(inputPass.getOutputTexture(inputDesc.slot));
                }else{
                    inputTextures.add(null);
                }
            }

            m_CurrentPass.setInputTextures(inputTextures);
            for(int i = 0; i < m_CurrentPass.getOutputCount(); i++){
                m_CurrentPass.computeOutDesc(i, outputDesc);

                PostProcessingRenderPassOutputTarget outputTarget = m_CurrentPass.getOutputTarget();
                switch (outputTarget){
                    case DEFAULT:
                        Texture2D temp = RenderTexturePool.getInstance().findFreeElement(outputDesc);
                        m_CurrentPass.setOutputRenderTexture(i, temp);
                        break;
                    case INTERNAL:
                        // nothing need to do.
                        break;
                    case SCREEN:
                        if(output != null){
                            m_CurrentPass.setOutputRenderTexture(i, output);
                        }else {
                            g_DummyTex._width = viewport.width;
                            g_DummyTex._height = viewport.height;
                            m_CurrentPass.setOutputRenderTexture(i, g_DummyTex);
                        }
                        break;
                    case SOURCE_COLOR:
                        m_CurrentPass.setOutputRenderTexture(i, m_FrameAttribs.sceneColorTexture);
                        break;
                }
            }

            m_CurrentPass._process(this, m_Parameters);
            resolveDependencies(currentDependencyPasses);
        }
    }

    public PostProcessingFrameAttribs getFrameAttribs() {return m_FrameAttribs;}

    void resolveDependencies(List<PostProcessingRenderPass> currentDependencyPasses)
    {
        for (PostProcessingRenderPass it : currentDependencyPasses)
        {
            it.resolveDependencies();
        }
    }

    public void finish() {
        if (!m_RenderPassList.isEmpty())
        {
//            std::vector<PostProcessingRenderPass*> lastPass(1);
//            lastPass[0] = m_RenderPassList.back().get();
            PostProcessingRenderPass lastPass = m_RenderPassList.get(m_RenderPassList.size() - 1);
            lastPass.markOutputSlot(0);
            currentDependencyPasses.clear();
            currentDependencyPasses.add(lastPass);
            resolveDependencies(currentDependencyPasses);
        }
    }

    private final static class Texture2DDummy extends Texture2D{
        int _width, _height;

        Texture2DDummy(){super("DummyTex2D");}
        @Override
        public int getWidth() {
            return _width;
        }

        @Override
        public int getHeight() {
            return _height;
        }
    }
}
