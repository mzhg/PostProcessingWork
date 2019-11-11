package jet.opengl.demos.nvidia.waves.crest;

import org.lwjgl.util.vector.Vector3f;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayDeque;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.texture.TextureUtils;

public class AsyncGPUReadbackRequest {

    private static final int RE_DONE = 1;
    private static final int RE_ERROR = 2;

    public interface AsyncGPUReadbackFinished{
        void onAsyncGPUReadbackFinish(AsyncGPUReadbackRequest request);
    }

    private enum SourceType{
        None,
        Buffer,
        Texture,
    }

    private BufferGL m_bufSource;
    private TextureGL m_texSource;
    private SourceType m_sourceType = SourceType.None;
    private int m_texMiplevel;

    private int m_stagingBuf;
    private ByteBuffer m_ReadbackData;
    private int m_ReadbackSize;
    private int m_fbo;

    private GLFuncProvider gl;
    private int m_result = -1;

    private final ArrayDeque<GPUReadbackSubRequst>  m_requests = new ArrayDeque<>();

    private interface GPUReadbackSubRequst{

         boolean isFinished();

        void update();

        int getResult();
    }

    private int  waitFenceSync(long fence){
        int results = 0;
        switch (gl.glClientWaitSync(fence, GLenum.GL_SYNC_FLUSH_COMMANDS_BIT, 1_000_00)){  // 0.1ms
            case GLenum.GL_ALREADY_SIGNALED:
            case GLenum.GL_CONDITION_SATISFIED:
            {
                results = RE_DONE;
                break;
            }
            case GLenum.GL_TIMEOUT_EXPIRED:
            {
                // continue to wait.
                break;
            }
            case GLenum.GL_WAIT_FAILED:
            {
                // An error occured.
                results = RE_ERROR;
                break;
            }
        }

        return results;
    }

    private final class BufferReadbackSubRequest implements GPUReadbackSubRequst{
        private long m_fence;
        private int m_results = 0;

        BufferReadbackSubRequest(){
            gl.glCopyNamedBufferSubData(m_bufSource.getBuffer(), m_stagingBuf, 0,0, m_bufSource.getBufferSize());
            m_fence = gl.glFenceSync();

            m_results = waitFenceSync(m_fence);
            if(m_results != 0){
                gl.glDeleteSync(m_fence);
            }
        }

        @Override
        public int getResult() {
            return m_results;
        }
        @Override
        public boolean isFinished() {
            return m_results != 0;
        }

        public void update(){
            if(!isFinished()){
                m_results = waitFenceSync(m_fence);
                if(m_results != 0){
                    gl.glDeleteSync(m_fence);
                }
            }
        }
    }

    private final class TextureReadbackSubRequest implements GPUReadbackSubRequst{
        private long m_fence;
        private int m_results = 0;
        private int m_slice;
        private int m_offset;
        private boolean m_inited = false;

        TextureReadbackSubRequest(int slice, int offset){
            m_slice = slice;
            m_offset = offset;

            if(m_slice != 0){
                return;
            }

            start();
        }

        private void start(){
            if(m_inited)
                return;

            Texture2D tex2D = (Texture2D)m_texSource;
            if(tex2D  == null)
                throw new UnsupportedOperationException("Only supprt the Texture2D");

            gl.glBindFramebuffer(GLenum.GL_READ_FRAMEBUFFER, m_fbo);  // todo The GL_READ_FRAMEBUFFER need checking.
            if(tex2D.getArraySize() > 1 || tex2D.getTarget() == GLenum.GL_TEXTURE_2D_ARRAY){
                gl.glFramebufferTextureLayer(GLenum.GL_READ_FRAMEBUFFER, GLenum.GL_COLOR_ATTACHMENT0, tex2D.getTexture(), m_texMiplevel, m_slice);
                gl.glReadBuffer(GLenum.GL_COLOR_ATTACHMENT0);
            }

            // todo assum the scssor_test disabled.
            gl.glBindBuffer(GLenum.GL_PIXEL_PACK_BUFFER, m_stagingBuf);
            gl.glReadPixels(0,0, tex2D.getWidth(), tex2D.getHeight(), TextureUtils.measureFormat(tex2D.getFormat()), TextureUtils.measureDataType(tex2D.getFormat()), m_offset);

            m_fence = gl.glFenceSync();

            m_results = waitFenceSync(m_fence);
            if(m_results != 0){
                gl.glDeleteSync(m_fence);
                gl.glBindBuffer(GLenum.GL_PIXEL_PACK_BUFFER, 0);
            }

            m_inited = true;
        }

        @Override
        public boolean isFinished() {
            return m_results != 0;
        }

        public void update(){
            if(!isFinished()){
                start();

                m_results = waitFenceSync(m_fence);
                if(m_results != 0){
                    gl.glDeleteSync(m_fence);
                    gl.glBindBuffer(GLenum.GL_PIXEL_PACK_BUFFER, 0);
                }
            }
        }

        @Override
        public int getResult() {
            return m_results;
        }
    }

    public static AsyncGPUReadbackRequest create(Wave_Simulation simulation, TextureGL source, int mipLevel){
        AsyncGPUReadbackRequest request = new AsyncGPUReadbackRequest();
        request.m_texSource = source;
        request.m_texMiplevel = mipLevel;
        request.m_sourceType = SourceType.Texture;

        simulation.addGPUReadback(request);

        request.init();

        return  request;
    }

    public static AsyncGPUReadbackRequest create(Wave_Simulation simulation, BufferGL source){
        AsyncGPUReadbackRequest request = new AsyncGPUReadbackRequest();
        request.m_bufSource = source;
        request.m_sourceType = SourceType.Buffer;

        simulation.addGPUReadback(request);

        request.init();

        return  request;
    }

    private void init(){
        gl = GLFuncProviderFactory.getGLFuncProvider();

        int memSize = 0;

        if(m_sourceType == SourceType.Texture){
            m_fbo = gl.glGenFramebuffer();

            int target = GLenum.GL_TEXTURE_2D_ARRAY;
            int currentBindingTex = gl.glGetInteger(GLenum.GL_TEXTURE_BINDING_2D_ARRAY);
            if(currentBindingTex == 0) {
                currentBindingTex = gl.glGetInteger(GLenum.GL_TEXTURE_BINDING_2D);
                if(currentBindingTex != 0){
                    target = GLenum.GL_TEXTURE_2D;
                }else{
                    // todo assum there is no texture binging.
                }
            }

            memSize = (int)TextureUtils.getTextureMemorySize(m_texSource.getTarget(), m_texSource.getTexture(), m_texMiplevel, 1);

            if(currentBindingTex != 0)
                gl.glBindTexture(target, currentBindingTex);  // restore the texture bingding.
        }else{
            memSize = m_bufSource.getBufferSize();
        }

        m_ReadbackSize = memSize;

        m_stagingBuf = gl.glCreateBuffer();
        gl.glNamedBufferStorage(m_stagingBuf, memSize, GLenum.GL_MAP_WRITE_BIT | GLenum.GL_MAP_PERSISTENT_BIT | GLenum.GL_MAP_COHERENT_BIT);
        GLCheck.checkError();

        if(m_sourceType == SourceType.Texture) {
            Texture2D tex2D = (Texture2D)m_texSource;
            if(tex2D  == null)
                throw new UnsupportedOperationException("Only supprt the Texture2D");

            int offset = 0;
            for(int i = 0; i < tex2D.getArraySize(); i++){
                m_requests.add(new TextureReadbackSubRequest(i, offset));
                offset += memSize / tex2D.getArraySize();
            }
        }else if(m_sourceType == SourceType.Buffer){
            m_requests.add(new BufferReadbackSubRequest());
        }
    }

    boolean update(){
        if(!m_requests.isEmpty()){
            GPUReadbackSubRequst requst = m_requests.peekFirst();
            requst.update();
            if(requst.isFinished()){
                m_result = requst.getResult();
                if(hasError()){
                    m_requests.clear();

                    return true;
                }
            }

            m_requests.pollFirst();
            return m_requests.isEmpty();
        }

        return true;
    }

    void release(){
        if(m_fbo != 0){
            gl.glDeleteFramebuffer(m_fbo);
            m_fbo = 0;

            gl.glDeleteBuffer(m_stagingBuf);
            m_stagingBuf = 0;
        }
    }

    public boolean hasError() {
        if(m_result == -1)
            return false;

        return (m_result & RE_ERROR) != 0;
    }

    public boolean done() {
        if(m_result == -1)
            return false;

        return(m_result & RE_DONE) != 0;
    }

    private void getReadbackData(){
        if(m_ReadbackData != null) return;

        if(!hasError() && done()){
            m_ReadbackData =  gl.glMapNamedBufferRange(m_stagingBuf, 0, m_ReadbackSize, GLenum.GL_MAP_READ_BIT | GLenum.GL_MAP_PERSISTENT_BIT);
            GLCheck.checkError();
        }else{
            throw new IllegalStateException("The Read back request has't done or occur errors!");
        }
    }

    public boolean GetData(ByteBuffer dest){  return true;}
    public boolean GetData(ShortBuffer dest){  return true;}

    public boolean GetData(Vector3f[] dest){
        getReadbackData();

        for(int i = 0; i < dest.length;i++){
            int offset = i * Vector3f.SIZE;
            float x = m_ReadbackData.getFloat(offset + 0);
            float y = m_ReadbackData.getFloat(offset + 4);
            float z = m_ReadbackData.getFloat(offset + 8);

            dest[i].set(x,y,z);
        }
        return true;
    }
    public boolean GetData(short[] dest){
        getReadbackData();
        for(int i = 0; i < dest.length;i++){
            int offset = i * 2;
            dest[i] = m_ReadbackData.getShort(offset);
        }

        return true;

    }
}
