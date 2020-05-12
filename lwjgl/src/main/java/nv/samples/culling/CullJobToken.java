package nv.samples.culling;

import java.util.ArrayList;
import java.util.List;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import nv.samples.cmdlist.NvToken;

final class CullJobToken extends Job {
    int      program_sizes;
    int      program_cmds;

    int                numTokens;
    final List<Sequence> sequences = new ArrayList<>();

    // input buffers
    ScanSystem.Buffer    tokenOrig;
    // for each command
    // #cmds rounded to multiple of 4
    ScanSystem.Buffer    tokenSizes;   // 
    ScanSystem.Buffer    tokenOffsets; // 
    ScanSystem.Buffer    tokenObjects; // -1 if no drawcall, otherwise object

    // outputs
    ScanSystem.Buffer    tokenOut;
    ScanSystem.Buffer    tokenOutSizes;
    ScanSystem.Buffer    tokenOutScan;
    ScanSystem.Buffer    tokenOutScanOffset;

    @Override
    void resultFromBits(BufferValue bufferVisBitsCurrent) {
        final GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        // First we compute sizes based on culling result
        // it generates an output stream where size[token] is either 0 or original size
        // depending on which object the token belonged to.
        gl.glUseProgram(program_sizes);

        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, tokenSizes.buffer);
        gl.glVertexAttribIPointer(0,1,GLenum.GL_UNSIGNED_INT,0,tokenSizes.offset);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, tokenObjects.buffer);
        gl.glVertexAttribIPointer(1,1,GLenum.GL_INT,0,tokenObjects.offset);

        gl.glEnableVertexAttribArray(0);
        gl.glEnableVertexAttribArray(1);

        tokenOutSizes.BindBufferRange(GLenum.GL_SHADER_STORAGE_BUFFER,0);
        bufferVisBitsCurrent.BindBufferRange(GLenum.GL_SHADER_STORAGE_BUFFER,1);

        gl.glMemoryBarrier(GLenum.GL_SHADER_STORAGE_BARRIER_BIT | GLenum.GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT);

        gl.glEnable(GLenum.GL_RASTERIZER_DISCARD);
        gl.glDrawArrays(GLenum.GL_POINTS,0,numTokens);

        gl.glDisableVertexAttribArray(0);
        gl.glDisableVertexAttribArray(1);

        // now let the scan system compute the running offsets for the visible tokens
        // that way we get a compact token stream with the original ordering back

        OcclusionCulling.s_scanSys.scanData(((numTokens+3)/4)*4,tokenOutSizes,tokenOutScan,tokenOutScanOffset);

        // finally we build the actual culled tokenbuffer, using those offsets

        gl.glUseProgram(program_cmds);

        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, tokenOffsets.buffer);
        gl.glVertexAttribIPointer(0,1,GLenum.GL_UNSIGNED_INT,0,tokenOffsets.offset);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, tokenOutSizes.buffer);
        gl.glVertexAttribIPointer(1,1,GLenum.GL_UNSIGNED_INT,0,tokenOutSizes.offset);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, tokenOutScan.buffer);
        gl.glVertexAttribIPointer(2,1,GLenum.GL_UNSIGNED_INT,0,tokenOutScan.offset);

        gl.glEnableVertexAttribArray(0);
        gl.glEnableVertexAttribArray(1);
        gl.glEnableVertexAttribArray(2);

        tokenOut.BindBufferRange(GLenum.GL_SHADER_STORAGE_BUFFER,0);
        tokenOrig.BindBufferRange(GLenum.GL_SHADER_STORAGE_BUFFER,1);
        tokenOutSizes.BindBufferRange(GLenum.GL_SHADER_STORAGE_BUFFER,2);
        tokenOutScan.BindBufferRange(GLenum.GL_SHADER_STORAGE_BUFFER,3);
        tokenOutScanOffset.BindBufferRange(GLenum.GL_SHADER_STORAGE_BUFFER,4);

        gl.glMemoryBarrier(GLenum.GL_SHADER_STORAGE_BARRIER_BIT | GLenum.GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT);

        for (int i = 0; i < sequences.size(); i++){
            Sequence sequence = sequences.get(i);

            gl.glUniform1ui(gl.glGetUniformLocation(program_cmds,"startOffset"),sequence.offset);
            gl.glUniform1i (gl.glGetUniformLocation(program_cmds,"startID"),    sequence.first);
            gl.glUniform1ui(gl.glGetUniformLocation(program_cmds,"endOffset"),  sequence.endoffset);
            gl.glUniform1i (gl.glGetUniformLocation(program_cmds,"endID"),      sequence.first + sequence.num - 1);
            gl.glUniform1ui(gl.glGetUniformLocation(program_cmds,"terminateCmd"), NvToken.s_nvcmdlist_header[GLenum.GL_TERMINATE_SEQUENCE_COMMAND_NV]);
            gl.glDrawArrays(GLenum.GL_POINTS,sequence.first,sequence.num);
        }


        gl.glDisableVertexAttribArray(0);
        gl.glDisableVertexAttribArray(1);
        gl.glDisableVertexAttribArray(2);

        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER,0);

        for (int i = 0; i < 5; i++){
            gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER,i,0);
        }

        gl.glDisable(GLenum.GL_RASTERIZER_DISCARD);
    }

    static final class Sequence {
        int       offset;
        int       endoffset;
        int       first;
        int       num;
    };
}
