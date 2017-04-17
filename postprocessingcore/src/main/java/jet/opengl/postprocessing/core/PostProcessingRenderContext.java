package jet.opengl.postprocessing.core;

import java.util.ArrayList;
import java.util.List;

import jet.opengl.postprocessing.buffer.AttribDesc;
import jet.opengl.postprocessing.buffer.BufferBinding;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.buffer.VertexArrayObject;
import jet.opengl.postprocessing.common.GLAPIVersion;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.RenderTargets;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.Recti;

/**
 * Created by mazhen'gui on 2017/4/17.
 */

public final class PostProcessingRenderContext {

    private static final int PROGRAM_BITS = 1;
    private static final int VAO_BITS = 2;
    private static final int RENDERTARGET_BITS = 4;
    private static final int VIEWPORT_BITS = 8;


    private final List<PostProcessingRenderPass> m_RenderPassList = new ArrayList<>();
    private PostProcessingRenderPass m_PrevPass;
    private PostProcessingRenderPass m_CurrentPass;

    private RenderTargets m_RenderTargets;

    private VertexArrayObject m_CurrentVAO;
    private VertexArrayObject m_DefaultVAO;
    private BufferGL          m_BufferQuad;

    private GLSLProgram m_CurrentProgram;
    private GLSLProgram m_DefaultProgram;

    PostProcessingParameters m_Parameters;

    private int m_FlagBits;
    private final Recti m_Viewport = new Recti();

    private boolean m_Initlized = false;

    void initlizeGL(int width, int height){
        if(m_Initlized)
            return;

        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        GLAPIVersion version = gl.getGLAPIVersion();

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

        // Initlize the program
        // TODO Add it later.

        //
    }

    public void setViewport(int x, int y, int width, int height){
        m_Viewport.x = x;
        m_Viewport.y = y;
        m_Viewport.width = width;
        m_Viewport.height = height;

        m_FlagBits |= VIEWPORT_BITS;
    }

    public void setRenderTarget(Texture2D tex){


        m_FlagBits |= RENDERTARGET_BITS;
    }

    public void setRenderTargets(Texture2D[] texs){


        m_FlagBits |= RENDERTARGET_BITS;
    }

    public void drawArrays(int mode, int offset, int count){

    }

    public void drawElements(int mode, int offset, int count){

    }

    public void drawFullscreenQuad(){

    }

    void setRenderPasses(List<PostProcessingRenderPass> inRenderPasses){
        m_RenderPassList.clear();
        m_RenderPassList.addAll(inRenderPasses);
    }

    void renderTo(Texture2D src, Texture2D dst, Recti viewport){

    }

    private final Texture2DDesc outputDesc = new Texture2DDesc();
    private final List<PostProcessingRenderPass> currentDependencyPasses = new ArrayList<>();
    private final List<Texture2D> inputTextures = new ArrayList<>();

    void performancePostProcessing(){
        for(PostProcessingRenderPass pass : m_RenderPassList){
            pass.reset();
        }

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
                }
            }

            m_CurrentPass.setInputTextures(inputTextures);
            for(int i = 0; i < m_CurrentPass.getOutputCount(); i++){
                m_CurrentPass.computeOutDesc(i, outputDesc);
                Texture2D temp = RenderTexturePool.getInstance().findFreeElement(outputDesc);
                m_CurrentPass.setOutputRenderTexture(i, temp);
            }

            m_CurrentPass._process(this, m_Parameters);
            resolveDependencies(currentDependencyPasses);
        }
    }

    void resolveDependencies(List<PostProcessingRenderPass> currentDependencyPasses)
    {
        for (PostProcessingRenderPass it : currentDependencyPasses)
        {
            it.resolveDependencies();
        }
    }

    private final boolean isViewportSet() {return (m_FlagBits & VIEWPORT_BITS) != 0;}
    private final boolean isProgramSet() {return (m_FlagBits & PROGRAM_BITS) != 0;}
    private final boolean isVAOSet() {return (m_FlagBits & VAO_BITS) != 0;}
    private final boolean isRenderTargetSet() {return (m_FlagBits & RENDERTARGET_BITS) != 0;}

    public void finish() {
    }
}
