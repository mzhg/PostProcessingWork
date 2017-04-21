package jet.opengl.postprocessing.common;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import jet.opengl.postprocessing.buffer.VertexArrayObject;
import jet.opengl.postprocessing.core.OpenGLProgram;
import jet.opengl.postprocessing.shader.GLSLProgramPipeline;
import jet.opengl.postprocessing.util.CachaRes;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.LogUtil;
import jet.opengl.postprocessing.util.Recti;

/**
 * Created by mazhen'gui on 2017/4/1.
 */

public class GLStateTracker {

    private static final int MASK_VAO = 1;
    private static final int MASK_BLEND_STATE = 2;
    private static final int MASK_DEPTH_STENCIL_STATE = 4;
    private static final int MASK_RASTERIZER_STATE = 8;
    private static final int MASK_VIEWPORT = 16;
    private static final int MASK_PROGRAM = 32;
    private static final int MASK_PROGRAM_PIPELINE = 64;
    private static final int MASK_FRAMEBUFFER = 128;

    private static final int MASK_ALL = MASK_VAO | MASK_BLEND_STATE | MASK_DEPTH_STENCIL_STATE |
                             MASK_RASTERIZER_STATE | MASK_VIEWPORT | MASK_PROGRAM |
                             MASK_PROGRAM_PIPELINE|MASK_FRAMEBUFFER;

    private static final int TAG_BLEND_DEFAULT = 256;
    private static final int TAG_DEPTH_STENCIL_DEFAULT = 512;
    private static final int TAG_RASTERIZER_DEFAULT = 1024;

    private ImageBinding[] imageBindings;
    private final SavedStates m_SavedStates = new SavedStates();
    private final SavedStates m_CurrentStates = new SavedStates();
    private final GLFuncProvider gl;
    private int m_flags;

    private static GLStateTracker instance;
    private GLStateTracker(){
        gl = GLFuncProviderFactory.getGLFuncProvider();
    }

    public static GLStateTracker getInstance(){
        if(instance == null){
            instance = new GLStateTracker();
        }

        return instance;
    }

    /**
     * Save the current GL states.
     */
    @CachaRes
    public void saveStates(){
        m_SavedStates.framebuffer = gl.glGetInteger(GLenum.GL_DRAW_FRAMEBUFFER_BINDING);
        m_SavedStates.program  = gl.glGetInteger(GLenum.GL_CURRENT_PROGRAM);
        IntBuffer ints = CacheBuffer.getCachedIntBuffer(4);
        gl.glGetIntegerv(GLenum.GL_VIEWPORT, ints);
        m_SavedStates.viewport.x = ints.get();
        m_SavedStates.viewport.y = ints.get();
        m_SavedStates.viewport.width = ints.get();
        m_SavedStates.viewport.height = ints.get();

        savedState(m_SavedStates.blendState);
        savedState(m_SavedStates.dsState);
        savedState(m_SavedStates.rsState);

        GLAPIVersion version = gl.getGLAPIVersion();
        if(version.major >= 4 && version.minor >= 1)
            m_SavedStates.programPipeline = gl.glGetInteger(GLenum.GL_PROGRAM_PIPELINE_BINDING);
        if(version.major >= 3)
            m_SavedStates.vao = gl.glGetInteger(GLenum.GL_VERTEX_ARRAY_BINDING);

        m_CurrentStates.set(m_SavedStates);
    }

    public void setVAO(VertexArrayObject vao){
        m_flags |= MASK_VAO;

        if(VertexArrayObject.isSupportVAO()){
            int vaoid = (vao != null ? vao.getVAO() : 0);
            if(m_CurrentStates.vao != vaoid){
                m_CurrentStates.vao = vaoid;
                gl.glBindVertexArray(vaoid);
            }
        }else{
            vao.bind();
        }
    }

    public void setFramebuffer(int framebuffer){
        m_flags |= MASK_FRAMEBUFFER;
        if(m_CurrentStates.framebuffer != framebuffer){
            m_CurrentStates.framebuffer = framebuffer;
            gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, framebuffer);
        }
    }

    public void setProgram(OpenGLProgram program){
        m_flags |= MASK_PROGRAM;

        if(m_CurrentStates.program != program.getProgram()){
            m_CurrentStates.program = program.getProgram();
            program.enable();
        }
    }

    public void setProgramPipeline(GLSLProgramPipeline pipeline){
        m_flags |= MASK_PROGRAM_PIPELINE;

        if(m_CurrentStates.programPipeline != pipeline.getProgram()){
            m_CurrentStates.programPipeline = pipeline.getProgram();
            pipeline.enable();
        }
    }

    public void setViewport(int x, int y, int width, int height){
        m_flags |= MASK_VIEWPORT;

        Recti viewport = m_CurrentStates.viewport;
        if(viewport.x != x || viewport.y != y || viewport.width != width || viewport.height != height){
            viewport.set(x, y, width, height);

            gl.glViewport(x, y, width, height);
        }
    }

    public void setBlendState(BlendState state){
        m_flags |= MASK_BLEND_STATE;

        if(state == null && (m_flags & TAG_BLEND_DEFAULT) != 0)
            return;

        if(state == null){
            m_flags |= TAG_BLEND_DEFAULT;
            setBSState(BlendState.g_DefaultBlendState, m_CurrentStates.blendState, false);
        }else{
            m_flags &= (~TAG_BLEND_DEFAULT);
            setBSState(state, m_CurrentStates.blendState, false);
        }
    }

    public void setDepthStencilState(DepthStencilState state){
        m_flags |= MASK_DEPTH_STENCIL_STATE;

        if(state == null && (m_flags & TAG_DEPTH_STENCIL_DEFAULT) != 0)
            return;

        if(state == null){
            m_flags |= TAG_DEPTH_STENCIL_DEFAULT;
            setDSState(DepthStencilState.g_DefaultDSState, m_CurrentStates.dsState, false);
        }else{
            m_flags &= (~TAG_DEPTH_STENCIL_DEFAULT);
            setDSState(state, m_CurrentStates.dsState, false);
        }
    }

    public void setRasterizerState(RasterizerState state){
        m_flags |= MASK_RASTERIZER_STATE;

        if(state == null && (m_flags & TAG_RASTERIZER_DEFAULT) != 0)
            return;

        if(state == null){
            m_flags |= TAG_RASTERIZER_DEFAULT;
            setRSState(RasterizerState.g_DefaultRSState, m_CurrentStates.rsState, false);
        }else{
            m_flags &= (~TAG_RASTERIZER_DEFAULT);
            setRSState(state, m_CurrentStates.rsState, false);
        }
    }

    public void clearFlags(boolean defualtTag){
        if(defualtTag){
            m_flags = 0;
        }else{
            m_flags &= (~MASK_ALL);
        }
    }

    private static void check(String msg, boolean throwExp){
        if(throwExp){
            throw new IllegalStateException(msg);
        }else{
            LogUtil.e(LogUtil.LogType.DEFAULT, msg);
        }
    }

    public void checkFlags(String name, boolean throwExp){
        if((m_flags & MASK_BLEND_STATE) == 0){
            check(name + " Missing Blend State Setting!", throwExp);
        }

        if((m_flags & MASK_DEPTH_STENCIL_STATE) == 0){
            check(name + " Missing depth stencil State Setting!", throwExp);
        }

        if((m_flags & MASK_RASTERIZER_STATE) == 0){
            check(name + " Missing rasterizer State Setting!", throwExp);
        }

        if((m_flags & MASK_VIEWPORT) == 0){
            check(name + " Missing viewport Setting!", throwExp);
        }

        if((m_flags & MASK_VIEWPORT) == 0){
            check(name + " Missing viewport Setting!", throwExp);
        }

        if((m_flags & MASK_VAO) == 0){
            check(name + " Missing vao Setting!", throwExp);
        }

        if((m_flags & MASK_FRAMEBUFFER) == 0){
            check(name + " Missing framebuffer Setting!", throwExp);
        }

        final int program_mask = MASK_PROGRAM | MASK_PROGRAM_PIPELINE;
        int mask = m_flags & program_mask;
        if(mask == 0){
            check(name + " Missing program Setting!", throwExp);
        }else if(mask == program_mask && gl.glIsProgram(m_CurrentStates.program) && gl.glIsProgramPipeline(m_CurrentStates.programPipeline) ){
            check(name + " Both program and programPipeline has Setted!", throwExp);
        }
    }

    /**
     * Reset the GL states with the previous saved by {@link #saveStates()}
     */
    public void restoreStates(){
        if(m_SavedStates.framebuffer != m_CurrentStates.framebuffer){
            gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, m_SavedStates.framebuffer);
        }

        if(m_SavedStates.vao != m_CurrentStates.vao){
            gl.glBindVertexArray(m_SavedStates.vao);
        }

        if(m_SavedStates.program != m_CurrentStates.program){
            gl.glUseProgram(m_SavedStates.program);
        }

        if(m_SavedStates.programPipeline != m_CurrentStates.programPipeline){
            gl.glBindProgramPipeline(m_SavedStates.programPipeline);
        }

        setBSState(m_SavedStates.blendState, m_CurrentStates.blendState, true);
        setDSState(m_SavedStates.dsState, m_CurrentStates.dsState, true);
        setRSState(m_SavedStates.rsState, m_CurrentStates.rsState, true);
        setViewport(m_SavedStates.viewport, m_CurrentStates.viewport);
//        (m_SavedStates.blendState, m_CurrentStates.blendState, true);
    }

    private void savedState(BlendState bsstate){
        bsstate.blendOp =gl.glGetInteger(GLenum.GL_BLEND_EQUATION_RGB);
        bsstate.blendOpAlpha = gl.glGetInteger(GLenum.GL_BLEND_EQUATION_ALPHA);

        bsstate.srcBlend = gl.glGetInteger(GLenum.GL_BLEND_SRC_RGB);
        bsstate.srcBlendAlpha = gl.glGetInteger(GLenum.GL_BLEND_SRC_ALPHA);
        bsstate.destBlend = gl.glGetInteger(GLenum.GL_BLEND_DST_RGB);
        bsstate.destBlendAlpha = gl.glGetInteger(GLenum.GL_BLEND_DST_ALPHA);
        bsstate.blendEnable = gl.glIsEnabled(GLenum.GL_BLEND);
    }

    /**
     * Set the blend states
     * @param dst The new comming states[READ ONLY]
     * @param src The old states[READ WRITE]
     * @param force Whether igore the blend details setting if the blend disabled, this can be improving performance.
     */
    private void setBSState(BlendState dst, BlendState src, boolean force){
        if (dst.blendEnable != src.blendEnable)
        {
            src.blendEnable = dst.blendEnable;
            if (src.blendEnable)
            {
                gl.glEnable(GLenum.GL_BLEND);
            }
            else
            {
                gl.glDisable(GLenum.GL_BLEND);
            }
        }

        if(!force && !dst.blendEnable){
            return;
        }

        if (dst.srcBlend != src.srcBlend ||
                dst.srcBlendAlpha != src.srcBlendAlpha ||
                dst.destBlend != src.destBlend ||
                dst.destBlendAlpha != src.destBlendAlpha)
        {
            gl.glBlendFuncSeparate(src.srcBlend,src.destBlend,src.srcBlendAlpha,src.destBlendAlpha);

            src.srcBlend = dst.srcBlend;
            src.srcBlendAlpha = dst.srcBlendAlpha;
            src.destBlend = dst.destBlend;
            src.destBlendAlpha = dst.destBlendAlpha;
        }

        if (dst.blendOp != src.blendOp || dst.blendOpAlpha != src.blendOpAlpha)
        {
            gl.glBlendEquationSeparate(src.blendOp, src.blendOpAlpha);

            src.blendOp = dst.blendOp;
            src.blendOpAlpha = dst.blendOpAlpha;
        }
    }

    /**
     * Set the depth stencil states
     * @param ds The new comming states[READ ONLY]
     * @param src The old states[READ WRITE]
     * @param force Whether igore the blend details setting if the blend disabled, this can be improving performance.
     */
    private void setDSState(DepthStencilState ds, DepthStencilState src, boolean force){
        if (src.depthWriteMask != ds.depthWriteMask)
        {
            src.depthWriteMask = ds.depthWriteMask;
            if (ds.depthWriteMask)
            {
                gl.glDepthMask(true);
            }
            else
            {
                gl.glDepthMask(false);
            }
        }

        if (src.depthEnable != ds.depthEnable)
        {
            if (ds.depthEnable)
            {
                gl.glEnable(GLenum.GL_DEPTH_TEST);

                if (src.depthFunc != ds.depthFunc)
                {
                    gl.glDepthFunc(ds.depthFunc);
                    src.depthFunc = ds.depthFunc;
                }
            }
            else
            {
                gl.glDisable(GLenum.GL_DEPTH_TEST);
            }

            src.depthEnable = ds.depthEnable;
        }else if (ds.depthEnable)
        {
            if (src.depthFunc != ds.depthFunc)
            {
                gl.glDepthFunc(ds.depthFunc);
                src.depthFunc = ds.depthFunc;
            }
        }

        if (src.stencilEnable != ds.stencilEnable)
        {
            src.stencilEnable = ds.stencilEnable;
            if (ds.stencilEnable)
            {
                gl.glEnable(GLenum.GL_STENCIL_TEST);
            }
            else
            {
                gl.glDisable(GLenum.GL_STENCIL_TEST);
            }
        }

        if(!force && !src.stencilEnable){
            return;
        }

        if (src.frontFace.stencilWriteMask != ds.frontFace.stencilWriteMask ||
                src.backFace.stencilWriteMask  != ds.backFace.stencilWriteMask)
        {
            if (ds.frontFace.stencilWriteMask != ds.backFace.stencilWriteMask)
            {
                gl.glStencilMaskSeparate(GLenum.GL_FRONT, ds.frontFace.stencilWriteMask);
                gl.glStencilMaskSeparate(GLenum.GL_BACK,  ds.backFace.stencilWriteMask);
            }
            else
            {
                gl.glStencilMask(ds.frontFace.stencilWriteMask);
            }

            src.frontFace.stencilWriteMask = ds.frontFace.stencilWriteMask;
            src.backFace.stencilWriteMask = ds.backFace.stencilWriteMask;
        }

        if (    src.frontFace.stencilFunc != ds.frontFace.stencilFunc ||
                src.frontFace.stencilMask != ds.frontFace.stencilMask ||
                src.frontFace.stencilRef  != ds.frontFace.stencilRef ||
                src.backFace.stencilFunc  != ds.backFace.stencilFunc ||
                src.backFace.stencilMask  != ds.backFace.stencilMask ||
                src.backFace.stencilRef   != ds.backFace.stencilRef )
        {
            if (ds.frontFace.stencilFunc != ds.backFace.stencilFunc ||
                    ds.frontFace.stencilMask != ds.backFace.stencilMask ||
                    ds.frontFace.stencilRef != ds.backFace.stencilRef)
            {
                gl.glStencilFuncSeparate(GLenum.GL_FRONT, ds.frontFace.stencilFunc, ds.frontFace.stencilRef, ds.frontFace.stencilMask);
                gl.glStencilFuncSeparate(GLenum.GL_BACK,  ds.backFace.stencilFunc, ds.backFace.stencilRef, ds.backFace.stencilMask);
            }
            else
            {
                gl.glStencilFunc(ds.frontFace.stencilFunc, ds.frontFace.stencilRef, ds.frontFace.stencilMask);
            }

            src.frontFace.stencilFunc = ds.frontFace.stencilFunc;
            src.frontFace.stencilMask = ds.frontFace.stencilMask;
            src.frontFace.stencilRef = ds.frontFace.stencilRef;
            src.backFace.stencilFunc = ds.backFace.stencilFunc;
            src.backFace.stencilMask = ds.backFace.stencilMask;
            src.backFace.stencilRef = ds.backFace.stencilRef;
        }

        if (src.frontFace.stencilFailOp != ds.frontFace.stencilFailOp ||
                src.frontFace.stencilDepthFailOp != ds.frontFace.stencilDepthFailOp ||
                src.frontFace.stencilPassOp != ds.frontFace.stencilPassOp ||

                src.backFace.stencilFailOp != ds.backFace.stencilFailOp ||
                src.backFace.stencilDepthFailOp != ds.backFace.stencilDepthFailOp ||
                src.backFace.stencilPassOp != ds.backFace.stencilPassOp)
        {

            if (ds.frontFace.stencilFailOp != ds.backFace.stencilFailOp ||
                    ds.frontFace.stencilDepthFailOp != ds.backFace.stencilDepthFailOp ||
                    ds.frontFace.stencilPassOp != ds.backFace.stencilPassOp)
            {
                gl.glStencilOpSeparate(GLenum.GL_FRONT, ds.frontFace.stencilFailOp, ds.frontFace.stencilDepthFailOp, ds.frontFace.stencilPassOp);
                gl.glStencilOpSeparate(GLenum.GL_BACK,  ds.backFace.stencilFailOp, ds.backFace.stencilDepthFailOp, ds.backFace.stencilPassOp);
            }
            else
            {
                gl.glStencilOp(ds.frontFace.stencilFailOp, ds.frontFace.stencilDepthFailOp, ds.frontFace.stencilPassOp);
            }

            src.frontFace.stencilFailOp = ds.frontFace.stencilFailOp;
            src.frontFace.stencilDepthFailOp = ds.frontFace.stencilDepthFailOp;
            src.frontFace.stencilPassOp = ds.frontFace.stencilPassOp;
            src.backFace.stencilFailOp = ds.backFace.stencilFailOp;
            src.backFace.stencilDepthFailOp = ds.backFace.stencilDepthFailOp;
            src.backFace.stencilPassOp = ds.backFace.stencilPassOp;
        }
    }

    private void savedState(DepthStencilState dsstate){
        dsstate.depthEnable = gl.glIsEnabled(GLenum.GL_DEPTH_TEST);
        dsstate.depthWriteMask = gl.glGetBoolean(GLenum.GL_DEPTH_WRITEMASK);
        dsstate.depthFunc = gl.glGetInteger(GLenum.GL_DEPTH_FUNC);

        dsstate.stencilEnable = gl.glIsEnabled(GLenum.GL_STENCIL_TEST);

        dsstate.frontFace.stencilFailOp = gl.glGetInteger(GLenum.GL_STENCIL_FAIL);
        dsstate.frontFace.stencilPassOp = gl.glGetInteger(GLenum.GL_STENCIL_PASS_DEPTH_PASS);
        dsstate.frontFace.stencilDepthFailOp = gl.glGetInteger(GLenum.GL_STENCIL_PASS_DEPTH_FAIL);
        dsstate.frontFace.stencilFunc = gl.glGetInteger(GLenum.GL_STENCIL_FUNC);
        dsstate.frontFace.stencilMask = gl.glGetInteger(GLenum.GL_STENCIL_VALUE_MASK);
        dsstate.frontFace.stencilRef = gl.glGetInteger(GLenum.GL_STENCIL_REF);
        dsstate.frontFace.stencilWriteMask = gl.glGetInteger(GLenum.GL_STENCIL_WRITEMASK);

        dsstate.backFace.stencilFailOp = gl.glGetInteger(GLenum.GL_STENCIL_BACK_FAIL);
        dsstate.backFace.stencilPassOp = gl.glGetInteger(GLenum.GL_STENCIL_BACK_PASS_DEPTH_PASS);
        dsstate.backFace.stencilDepthFailOp = gl.glGetInteger(GLenum.GL_STENCIL_BACK_PASS_DEPTH_FAIL);
        dsstate.backFace.stencilFunc = gl.glGetInteger(GLenum.GL_STENCIL_BACK_FUNC);
        dsstate.backFace.stencilMask = gl.glGetInteger(GLenum.GL_STENCIL_BACK_VALUE_MASK);
        dsstate.backFace.stencilRef = gl.glGetInteger(GLenum.GL_STENCIL_BACK_REF);
        dsstate.backFace.stencilWriteMask = gl.glGetInteger(GLenum.GL_STENCIL_BACK_WRITEMASK);
    }

    /**
     * Set the viewport
     * @param dst
     * @param src
     */
    private void setViewport(Recti dst, Recti src){
        if(!src.equals(dst)){
            gl.glViewport(dst.x, dst.y, dst.width, dst.height);
            src.set(dst);
        }
    }

    /**
     * Set the rasterzer states
     * @param dst The new comming states[READ ONLY]
     * @param src The old states[READ WRITE]
     * @param force Whether igore the blend details setting if the blend disabled, this can be improving performance.
     */
    private void setRSState(RasterizerState dst, RasterizerState src, boolean force){
        if(src.fillMode != dst.fillMode){
            src.fillMode = dst.fillMode;
            gl.glPolygonMode(GLenum.GL_FRONT_AND_BACK, src.fillMode);
        }

        if(src.frontCounterClockwise != dst.frontCounterClockwise){
            src.frontCounterClockwise = dst.frontCounterClockwise;
            if(src.frontCounterClockwise){
                gl.glFrontFace(GLenum.GL_CW);
            }else{
                gl.glFrontFace(GLenum.GL_CCW);
            }
        }

        if(src.cullFaceEnable != dst.cullFaceEnable){
            src.cullFaceEnable = dst.cullFaceEnable;
            if(src.cullFaceEnable){
                gl.glEnable(GLenum.GL_CULL_FACE);
            }else{
                gl.glDisable(GLenum.GL_CULL_FACE);
            }
        }

        if(force || src.cullFaceEnable)
        {
            if (src.cullMode != dst.cullMode)
            {
                gl.glCullFace(dst.cullMode);
                src.cullMode = dst.cullMode;
            }
        }

        if(src.rasterizedDiscardEnable != dst.rasterizedDiscardEnable){
            src.rasterizedDiscardEnable = dst.rasterizedDiscardEnable;
            if(src.cullFaceEnable){
                gl.glEnable(GLenum.GL_RASTERIZER_DISCARD);
            }else{
                gl.glDisable(GLenum.GL_RASTERIZER_DISCARD);
            }
        }

        if(src.colorWriteMask != dst.colorWriteMask){
            src.colorWriteMask = dst.colorWriteMask;
            boolean red = (src.colorWriteMask & RasterizerState.MASK_RED) != 0;
            boolean green = (src.colorWriteMask & RasterizerState.MASK_GREEN) != 0;
            boolean blue = (src.colorWriteMask & RasterizerState.MASK_BLUE) != 0;
            boolean alpha = (src.colorWriteMask & RasterizerState.MASK_ALPHA) != 0;

            gl.glColorMask(red, green, blue, alpha);
        }
    }

    @CachaRes
    private void savedState(RasterizerState rsstate){
        rsstate.cullMode = gl.glGetInteger(GLenum.GL_CULL_FACE_MODE);
        rsstate.frontCounterClockwise = gl.glGetInteger(GLenum.GL_FRONT_FACE) == GLenum.GL_CW;
        rsstate.cullFaceEnable = gl.glIsEnabled(GLenum.GL_CULL_FACE);
        rsstate.rasterizedDiscardEnable = gl.glIsEnabled(GLenum.GL_RASTERIZER_DISCARD);
        ByteBuffer bytes = CacheBuffer.getCachedByteBuffer(4);
        gl.glGetBooleanv(GLenum.GL_COLOR_WRITEMASK, bytes);
        rsstate.colorWriteMask = 0;
        for(int i = 0; i < 4; i++){
            rsstate.colorWriteMask |= (bytes.get() != 0 ? (1 << i) : 0);
        }
    }

    public void bindImage(int unit, int textureID, int level, boolean layered, int layer,int  access,int format){

    }

    public void bindTexture(int unit, int target, int textureID, int sampler){

    }

    @SuppressWarnings({"NoteSafe"})
    public void bindBuffer(int target, int buffer){

    }

    // bind the texture to current units
    public void bindTexture(int target, int textureID, int sampler){

    }

    private static final class StateDesc{

    }

    private static final class ImageBinding{
        int textureID;
        int access;
        int unit;
    }

    private static class SavedStates{
        int framebuffer;
        int program;
        int programPipeline;
        int vao;
        final Recti viewport = new Recti();
        final BlendState blendState = new BlendState();
        final DepthStencilState dsState = new DepthStencilState();
        final RasterizerState rsState = new RasterizerState();

        void set(SavedStates other){
            framebuffer = other.framebuffer;
            program = other.program;
            programPipeline = other.programPipeline;
            vao = other.vao;
            blendState.set(other.blendState);
            dsState.set(other.dsState);
            rsState.set(other.rsState);
        }
    }
}
