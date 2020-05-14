package nv.samples.culling;

import java.nio.ByteBuffer;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
    This class wraps several operations to aid implementing scalable occlusion culling.
    Traditional techniques using "conditional rendering" or classic "occlusion queries",
    often suffered from performance issues when applied to many thousands of objects.
    See readme of "https://github.com/nvpro-samples/gl_occlusion_culling"

    In this system here we do the occlusion test of many bounding boxes with a single drawcall.
    The results for all of those boxes are stored in buffers that are packed
    into bit buffers (one bit per tested object). The result of the occlusion test
    can then either be read back or kept on the GPU to build draw indirect drawcalls.

    The system does not make any allocations, except for a small UBO used to pass
    uniforms to the shaders used.

    As user you provide all necessary data as buffers in the "Job" class.
    You can derive from this class to implement your own result handling,
    although a few basic implementations are provided already.
  */

final class CullingSystem {

    private static final int GL_REPRESENTATIVE_FRAGMENT_TEST_NV  = 1;  // todo
    static boolean DEBUG_VISIBLEBOXES = false;

    private  Programs  m_programs;

    private BufferGL m_ubo;
    private int    m_fbo;
    private int[]  m_tbo = new int[2];
    private boolean      m_dualindex;
    private boolean      m_useRepesentativeTest;

    private GLFuncProvider gl;

    @Deprecated
    static int minDivide( int val,  int alignment)
    {
        return (val+alignment-1)/alignment;
    }

    // provide the programs using your own loading mechanism
    // internally tbo, fbo, ubos are generated
    // dualindex - means shaders were built in dual index mode.
    //             The app provides two indices per proxy bounding box,
    //             one is the matrix index, the other is the bounding box index.

    void init(  Programs programs, boolean dualindex ){
        gl = GLFuncProviderFactory.getGLFuncProvider();

        update(programs,dualindex);
        m_fbo = gl.glGenFramebuffer();
//        glCreateTextures(GL_TEXTURE_BUFFER,2,m_tbo);
        m_tbo[0] = gl.glCreateTextures(GLenum.GL_TEXTURE_BUFFER);
        m_tbo[1] = gl.glCreateTextures(GLenum.GL_TEXTURE_BUFFER);
        /*glGenBuffers(1, &m_ubo);
        glBindBuffer(GL_UNIFORM_BUFFER, m_ubo);
        glBufferData(GL_UNIFORM_BUFFER, sizeof(View), nullptr, GL_DYNAMIC_DRAW);
        glBindBuffer(GL_UNIFORM_BUFFER, 0);*/

        m_ubo = new BufferGL();
        m_ubo.initlize(GLenum.GL_UNIFORM_BUFFER, View.SIZE, null, GLenum.GL_DYNAMIC_DRAW);
    }

    void deinit(){
        gl.glDeleteFramebuffer(m_fbo);
        gl.glDeleteTextures(m_tbo);
    }

    void update( Programs programs, boolean dualindex ){
        m_programs = programs;
        m_dualindex = dualindex;

        m_useRepesentativeTest = gl.isSupportExt("GL_NV_representative_fragment_test");
    }

    // helper function for HiZ method, leaves fbo bound to 0
    // uses internal fbo, naive non-optimized implementation
    void buildDepthMipmaps(Texture2D textureDepth, int width, int height){
        int level = 0;
        int dim = width > height ? width : height;
        int twidth  = width;
        int theight = height;
        int wasEven = 0;

        gl.glBindFramebuffer(GLenum. GL_FRAMEBUFFER,m_fbo);
        gl.glDepthFunc(GLenum.GL_ALWAYS);
//        glUseProgram(m_programs.depth_mips);
        m_programs.depth_mips.enable();
        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D, textureDepth.getTexture());

        while (dim > 0){
            if (level > 0){
                twidth  = twidth < 1 ? 1 : twidth;
                theight = theight < 1 ? 1 : theight;
                gl.glViewport(0,0,twidth,theight);
                gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER,GLenum.GL_DEPTH_STENCIL_ATTACHMENT,GLenum.GL_TEXTURE_2D, textureDepth.getTexture(), level);
                gl.glUniform1i(0, level-1);
                gl.glUniform1i(1, wasEven);

                gl.glDrawArrays(GLenum.GL_TRIANGLES,0,3);
            }

            wasEven = ((twidth % 2 == 0) && (theight % 2 == 0)) ? 1 : 0;

            dim       /=  2;
            twidth    /=  2;
            theight   /=  2;
            level++;
        }

        gl.glUseProgram(0);
        gl.glViewport(0,0,width,height);
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER,0);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D, 0);
        gl.glDepthFunc(GLenum.GL_LEQUAL);
        gl.glViewport(0,0,width,height);
    }

    // perform occlusion test for all bounding boxes provided in the job
    private void testBboxes( Job job, boolean raster){
        // send the scene's bboxes as points stream
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, job.m_bufferObjectBbox.buffer);
        if (m_dualindex){
            gl.glVertexAttribIPointer(0, 1, GLenum.GL_INT, job.m_bufferObjectBbox.stride, job.m_bufferObjectBbox.offset);
            gl.glVertexAttribDivisor(0, 0);
            gl.glEnableVertexAttribArray(0);
        }
        else{
            int stride = job.m_bufferObjectBbox.stride > 0 ? job.m_bufferObjectBbox.stride : /*sizeof(float)*/4*4*2;
            gl.glVertexAttribPointer(0, 4, GLenum.GL_FLOAT, false, stride, job.m_bufferObjectBbox.offset);
            gl.glVertexAttribDivisor(0, 0);
            gl.glEnableVertexAttribArray(0);
            gl.glVertexAttribPointer(1, 4, GLenum.GL_FLOAT, false, stride, (/*sizeof(float)*/4*4 + job.m_bufferObjectBbox.offset));
            gl.glVertexAttribDivisor(1, 0);
            gl.glEnableVertexAttribArray(1);
        }

        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, job.m_bufferObjectMatrix.buffer);
        gl.glVertexAttribIPointer(2, 1, GLenum.GL_INT, job.m_bufferObjectMatrix.stride, job.m_bufferObjectMatrix.offset);
        gl.glVertexAttribDivisor(2, 0);
        gl.glEnableVertexAttribArray(2);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);

        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glBindTexture(GLenum.GL_TEXTURE_BUFFER, m_tbo[0]);
        job.m_bufferMatrices.TexBuffer(GLenum.GL_TEXTURE_BUFFER,GLenum.GL_RGBA32F);

        if (m_dualindex){
            gl.glActiveTexture(GLenum.GL_TEXTURE1);
            gl.glBindTexture(GLenum.GL_TEXTURE_BUFFER, m_tbo[1]);
            job.m_bufferBboxes.TexBuffer(GLenum.GL_TEXTURE_BUFFER,GLenum.GL_RGBA32F);
        }

        if (raster){
            if (m_useRepesentativeTest) {
                gl.glEnable( GL_REPRESENTATIVE_FRAGMENT_TEST_NV );
            }
            if(!DEBUG_VISIBLEBOXES) {
                gl.glDepthMask(false);
                gl.glColorMask(false, false, false, false);
            }
        }
        else {
            gl.glEnable(GLenum.GL_RASTERIZER_DISCARD);
            job.m_bufferVisOutput.BindBufferRange(GLenum.GL_SHADER_STORAGE_BUFFER,0);
        }

        gl.glDrawArrays(GLenum.GL_POINTS,0,job.m_numObjects);

        if (raster){
            if (m_useRepesentativeTest) {
                gl.glDisable( GL_REPRESENTATIVE_FRAGMENT_TEST_NV );
            }
            if(!DEBUG_VISIBLEBOXES) {
                gl.glDepthMask(true);
                gl.glColorMask(true, true, true, true);
            }
        }
        else {
            gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER,0,0);
            gl.glDisable(GLenum.GL_RASTERIZER_DISCARD);
        }

        if (m_dualindex){
            gl.glBindTexture(GLenum.GL_TEXTURE_BUFFER, 0);
            gl.glActiveTexture(GLenum.GL_TEXTURE0);
        }
        gl.glBindTexture(GLenum.GL_TEXTURE_BUFFER, 0);

        gl.glDisableVertexAttribArray(0);
        gl.glDisableVertexAttribArray(1);
        gl.glDisableVertexAttribArray(2);
    }

    // computes occlusion test for all bboxes provided in the job
    // updates job.m_bufferVisOutput
    // assumes appropriate fbo bound for raster method as it assumes intact depthbuffer

    void buildOutput( MethodType  method, Job job, View view ){
        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 0, m_ubo.getBuffer());
        ByteBuffer bytes = CacheBuffer.getCachedByteBuffer(View.SIZE);
        view.store(bytes);  bytes.flip();
        gl.glBufferSubData(GLenum.GL_UNIFORM_BUFFER, 0, bytes);

        switch(method){
            case METHOD_FRUSTUM:
            {
                gl.glUseProgram(m_programs.object_frustum.getProgram());

                testBboxes(job,false);
            }
            break;
            case METHOD_HIZ:
            {
                gl.glUseProgram(m_programs.object_hiz.getProgram());
                gl.glActiveTexture(GLenum.GL_TEXTURE2);
                gl.glBindTexture(GLenum.GL_TEXTURE_2D,job.m_textureDepthWithMipmaps.getTexture());

                testBboxes(job,false);

                gl.glActiveTexture(GLenum.GL_TEXTURE2);
                gl.glBindTexture(GLenum.GL_TEXTURE_2D,0);
                gl.glActiveTexture(GLenum.GL_TEXTURE0);
            }
            break;
            case METHOD_RASTER:
            {
                // clear visibles
                job.m_bufferVisOutput.BindBufferRange(GLenum.GL_SHADER_STORAGE_BUFFER,0);
                gl.glClearBufferData(GLenum.GL_SHADER_STORAGE_BUFFER, GLenum.GL_R32UI,GLenum.GL_RED_INTEGER,GLenum.GL_UNSIGNED_INT,null);

                gl.glUseProgram(m_programs.object_raster.getProgram());

                gl.glEnable( GLenum.GL_POLYGON_OFFSET_FILL );
                gl.glPolygonOffset(-1,-1);
                testBboxes(job,true);
                gl.glPolygonOffset(0,0);
                gl.glDisable( GLenum.GL_POLYGON_OFFSET_FILL );

                gl.glMemoryBarrier(GLenum.GL_SHADER_STORAGE_BARRIER_BIT);

                gl.glBindBufferBase (GLenum.GL_SHADER_STORAGE_BUFFER,0,0);
            }
            break;
        }

        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 0, 0);
    }

    // updates job.m_bufferVisBitsCurrent
    // from output buffer (job.m_bufferVisOutput), filled in "buildOutput" as well as potentially
    // using job.m_bufferVisBitsLast, depending on BitType.
    void bitsFromOutput ( Job job, BitType type ){
        // for GL 3.3 compatibility we use xfb
        // in GL 4.3 SSBO is used
        //
        // using compute instead of "invisible" point drawing
        // would be better if we had really huge thread counts

        gl.glEnable(GLenum.GL_RASTERIZER_DISCARD);

        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, job.m_bufferVisOutput.buffer);
        for (int i = 0; i < 8; i++){
            gl.glVertexAttribIPointer(i, 4, GLenum.GL_UNSIGNED_INT, /*sizeof(int)*/4*32, (i*/*sizeof(int)*/4*4 + job.m_bufferVisOutput.offset));
            gl.glVertexAttribDivisor(i, 0);
            gl.glEnableVertexAttribArray(i);
        }

        if (type == BitType.BITS_CURRENT){
            gl.glUseProgram(m_programs.bit_regular.getProgram());
        }
        else{
            gl.glUseProgram(type == BitType.BITS_CURRENT_AND_LAST ? m_programs.bit_temporallast.getProgram() : m_programs.bit_temporalnew.getProgram());

            gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, job.m_bufferVisBitsLast.buffer);
            gl.glVertexAttribIPointer(9, 1, GLenum.GL_UNSIGNED_INT, /*sizeof(int)*/4, job.m_bufferVisBitsLast.offset);
            gl.glEnableVertexAttribArray(9);
        }

        job.m_bufferVisBitsCurrent.BindBufferRange(GLenum.GL_SHADER_STORAGE_BUFFER,0);
        gl.glMemoryBarrier(GLenum.GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT);

        gl.glDrawArrays(GLenum.GL_POINTS,0, minDivide(job.m_numObjects,32));

        gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 0, 0);
        gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 1, 0);

        gl.glDisableVertexAttribArray(9);
        for (int i = 0; i < 8; i++){
            gl.glDisableVertexAttribArray(i);
        }

        gl.glDisable(GLenum.GL_RASTERIZER_DISCARD);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
    }

    // result handling is implemented in the interface provided by the job.
    // for example you could be building MDI commands
    void resultFromBits ( Job job ){
        job.resultFromBits(job.m_bufferVisBitsCurrent);
    }

    // result handling on the client is implemented in the interface provided by the job
    // for example waiting for readbacks, or nothing
    void resultClient   ( Job job ){
        job.resultClient();
    }

    // swaps the Current/Last bit array (for temporal coherent techniques)
    void swapBits       ( Job job ){
        BufferValue temp = job.m_bufferVisBitsCurrent;
        job.m_bufferVisBitsCurrent = job.m_bufferVisBitsLast;
        job.m_bufferVisBitsLast = temp;
    }
}
