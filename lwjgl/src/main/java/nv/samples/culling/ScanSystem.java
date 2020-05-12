package nv.samples.culling;

import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL45;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.Numeric;

final class ScanSystem {
    final static int GROUPSIZE = 512;
    final static int BATCH_ELEMENTS = GROUPSIZE*4;

    private Programs    programs;

    private int      maxGrpsPrefix;
    private int      maxGrpsOffsets;
    private int      maxGrpsCombine;

    static class Buffer {
        int      buffer;
        int      offset;
        int      size;

        private GLFuncProvider gl;

        Buffer(int buffer) { this(buffer, 0);}
        Buffer(int buffer, int sizei /*= 0*/)
      /*: buffer(buffer)
                , offset(0)
                , stride(0)*/
        {
            gl = GLFuncProviderFactory.getGLFuncProvider();
            this.buffer = buffer;
            offset = 0;
            gl.glBindBuffer(GLenum.GL_COPY_READ_BUFFER, buffer);
            size = gl.glGetBufferParameteri(GLenum.GL_COPY_READ_BUFFER, GLenum.GL_BUFFER_SIZE);
            gl.glBindBuffer(GLenum.GL_COPY_READ_BUFFER, 0);
        }

        Buffer(){}

        void BindBufferRange(int target, int index) {
            gl.glBindBufferRange(target, index, buffer, offset, size);
        }
        void BindBufferRange(int target, int index, int offseta, int sizea) {
            gl.glBindBufferRange(target, index, buffer, offset+offseta, size+sizea);
        }

        void GetNamedBufferSubData(ByteBuffer data){
            GL45.glGetNamedBufferSubData(buffer,offset,/*size,*/data);
        }

    };

    private GLFuncProvider gl;

    void init(Programs progs){
        update(progs);
    }

    void update(Programs progs){
//        GLuint    maxGroups[3];
        int[] maxGroups = new int[3];
        GL30.glGetIntegeri_v(GLenum.GL_MAX_COMPUTE_WORK_GROUP_COUNT,0,maxGroups);

        //GLuint    groupSize[3];
        //glGetProgramiv(progs.combine,    GL_COMPUTE_WORK_GROUP_SIZE, (GLint*)groupSize);
        maxGrpsCombine = maxGroups[0];
        //glGetProgramiv(progs.offsets,    GL_COMPUTE_WORK_GROUP_SIZE, (GLint*)groupSize);
        maxGrpsOffsets = maxGroups[0];
        //glGetProgramiv(progs.prefixsum,    GL_COMPUTE_WORK_GROUP_SIZE, (GLint*)groupSize);
        maxGrpsPrefix = maxGroups[0];

        programs = progs;
    }

    void test(){
        int[] scanbuffers = new int[3];
        GL45.glCreateBuffers(scanbuffers);

        int low  = BATCH_ELEMENTS/2;
        int mid  = BATCH_ELEMENTS*BATCH_ELEMENTS;
        int high = BATCH_ELEMENTS*BATCH_ELEMENTS*2;
        int offsize = getOffsetSize(high);

        IntBuffer data = CacheBuffer.getCachedIntBuffer(high);
        for (int i = 0; i < high; i++){
//            data[i] = 1;
            data.put(1);
        }
        data.flip();

        gl.glNamedBufferStorage(scanbuffers[0], /*high * sizeof(GLuint),*/ data, 0 );
        gl.glNamedBufferStorage(scanbuffers[1], high * /*sizeof(GLuint)*/4,GLenum.GL_MAP_READ_BIT );
        gl.glNamedBufferStorage(scanbuffers[2], offsize,GLenum.GL_MAP_READ_BIT);

        int result;
        boolean needcombine;

        /*// low   todo
        needcombine = scanData(low, scanbuffers[0], scanbuffers[1], scanbuffers[2]);
        assert(needcombine == false);
        result = 0;
        glGetNamedBufferSubData(scanbuffers[1],sizeof(GLuint) * (low-1), sizeof(GLuint), &result);
        assert(result == low);

        // med
        needcombine = scanData(mid, scanbuffers[0], scanbuffers[1], scanbuffers[2]);
        assert(needcombine == true);
        result = 0;
        glGetNamedBufferSubData(scanbuffers[2],sizeof(GLuint) * (ScanSystem::BATCH_ELEMENTS-1), sizeof(GLuint), &result);
        assert(result == mid);

        combineWithOffsets(mid, scanbuffers[1], scanbuffers[2]);
        result = 0;
        glGetNamedBufferSubData(scanbuffers[1],sizeof(GLuint) * (mid-1), sizeof(GLuint), &result);
        assert(result == mid);

        // high
        needcombine = scanData(high, scanbuffers[0], scanbuffers[1], scanbuffers[2]);
        assert(needcombine == true);
        combineWithOffsets(high, scanbuffers[1], scanbuffers[2]);
        result = 0;
        glGetNamedBufferSubData(scanbuffers[1],sizeof(GLuint) * (high-1), sizeof(GLuint), &result);
        assert(result == high);

        glDeleteBuffers(3,scanbuffers);*/
    }

    // returns true if offsets are needed
    // the offset value needs to be added using the BATCH_ELEMENTS
    boolean scanData(int elements, Buffer input, Buffer output, Buffer offsets){
        assert( (elements % 4) == 0 );
        assert( elements < BATCH_ELEMENTS*BATCH_ELEMENTS*BATCH_ELEMENTS);
        assert( elements * /*sizeof(GLuint)*/4 <= input.size );
        assert( input.size <= output.size );

//         glUseProgram(programs.prefixsum);
        programs.prefixsum.enable();
        gl.glUniform1ui(0,elements);

        input.BindBufferRange(GLenum.GL_SHADER_STORAGE_BUFFER,1);
        output.BindBufferRange(GLenum.GL_SHADER_STORAGE_BUFFER,0);

        gl.glMemoryBarrier(GLenum.GL_SHADER_STORAGE_BARRIER_BIT);

        int groups = Numeric.divideAndRoundUp(elements,BATCH_ELEMENTS);

        assert(groups <= maxGrpsPrefix);
        gl.glDispatchCompute(groups,1,1);

        if (groups > 1){

            int groupcombines = Numeric.divideAndRoundUp(groups,BATCH_ELEMENTS);

            assert( groupcombines <= BATCH_ELEMENTS );
            assert( getOffsetSize(elements) <= offsets.size);

//            gl.glUseProgram(programs.offsets);
            programs.offsets.enable();
            gl.glUniform1ui(0,elements);

            output.BindBufferRange(GLenum.GL_SHADER_STORAGE_BUFFER,  1);
            offsets.BindBufferRange(GLenum.GL_SHADER_STORAGE_BUFFER, 0);

            gl.glMemoryBarrier(GLenum.GL_SHADER_STORAGE_BARRIER_BIT);

            assert(groupcombines <= maxGrpsOffsets);
            gl.glDispatchCompute(groupcombines,1,1);

            if (groupcombines > 1){
                gl.glUniform1ui(0,groupcombines*BATCH_ELEMENTS);

                Buffer additionaloffsets = offsets; // derive from offsets
                int required = groupcombines*BATCH_ELEMENTS*/*sizeof(GLuint)*/4;;

                additionaloffsets.offset += required;
                additionaloffsets.size = offsets.size - required;

                offsets.BindBufferRange(GLenum.GL_SHADER_STORAGE_BUFFER,1);
                additionaloffsets.BindBufferRange(GLenum.GL_SHADER_STORAGE_BUFFER,0);

                gl.glMemoryBarrier(GLenum.GL_SHADER_STORAGE_BARRIER_BIT);

                gl.glDispatchCompute(1,1,1);

                combineWithOffsets(groupcombines*BATCH_ELEMENTS, offsets, additionaloffsets);
            }
        }

        gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER,0,0);
        gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER,0,1);

        return groups > 1;
    }

    void combineWithOffsets(int elements, Buffer output, Buffer offsets){
        //assert((elements % 4) == 0);
        assert(elements * /*sizeof(GLuint)*/4 <= output.size);

//        glUseProgram(programs.combine);
        programs.combine.enable();
        gl.glUniform1ui(0,elements);

        offsets.BindBufferRange(GLenum.GL_SHADER_STORAGE_BUFFER, 1);
        output.BindBufferRange(GLenum.GL_SHADER_STORAGE_BUFFER, 0);

        gl.glMemoryBarrier(GLenum.GL_SHADER_STORAGE_BARRIER_BIT);

        int groups = Numeric.divideAndRoundUp(elements,GROUPSIZE);
        assert(groups < maxGrpsCombine);
        gl.glDispatchCompute(groups,1,1);
    }

    static int getOffsetSize(int elements){
        int groups = CullingSystem.minDivide(elements,BATCH_ELEMENTS);

        if (groups == 1) return 0;

        int groupcombines = CullingSystem.minDivide(groups,BATCH_ELEMENTS);
        int size = groupcombines*BATCH_ELEMENTS*/*sizeof(GLuint)*/4;

        if (groupcombines > 1){
            // add another layer
            int combines = Numeric.divideAndRoundUp(groupcombines,BATCH_ELEMENTS);
            size += combines*BATCH_ELEMENTS*/*sizeof(GLuint)*/4;
        }

        return (size);
    }


    static final class Programs {
        GLSLProgram prefixsum;
        GLSLProgram offsets;
        GLSLProgram combine;
    };
}
