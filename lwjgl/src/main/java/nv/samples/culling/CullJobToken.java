package nv.samples.culling;

import java.util.ArrayList;
import java.util.List;

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
        // First we compute sizes based on culling result
        // it generates an output stream where size[token] is either 0 or original size
        // depending on which object the token belonged to.
        glUseProgram(program_sizes);

        glBindBuffer(GL_ARRAY_BUFFER, tokenSizes.buffer);
        glVertexAttribIPointer(0,1,GL_UNSIGNED_INT,0,(const void*)tokenSizes.offset);
        glBindBuffer(GL_ARRAY_BUFFER, tokenObjects.buffer);
        glVertexAttribIPointer(1,1,GL_INT,0,(const void*)tokenObjects.offset);

        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);

        tokenOutSizes.BindBufferRange(GL_SHADER_STORAGE_BUFFER,0);
        bufferVisBitsCurrent.BindBufferRange(GL_SHADER_STORAGE_BUFFER,1);

        glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT | GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT);

        glEnable(GL_RASTERIZER_DISCARD);
        glDrawArrays(GL_POINTS,0,numTokens);

        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);

        // now let the scan system compute the running offsets for the visible tokens
        // that way we get a compact token stream with the original ordering back

        s_scanSys.scanData(((numTokens+3)/4)*4,tokenOutSizes,tokenOutScan,tokenOutScanOffset);

        // finally we build the actual culled tokenbuffer, using those offsets

        glUseProgram(program_cmds);

        glBindBuffer(GL_ARRAY_BUFFER, tokenOffsets.buffer);
        glVertexAttribIPointer(0,1,GL_UNSIGNED_INT,0,(const void*)tokenOffsets.offset);
        glBindBuffer(GL_ARRAY_BUFFER, tokenOutSizes.buffer);
        glVertexAttribIPointer(1,1,GL_UNSIGNED_INT,0,(const void*)tokenOutSizes.offset);
        glBindBuffer(GL_ARRAY_BUFFER, tokenOutScan.buffer);
        glVertexAttribIPointer(2,1,GL_UNSIGNED_INT,0,(const void*)tokenOutScan.offset);

        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        glEnableVertexAttribArray(2);

        tokenOut.BindBufferRange(GL_SHADER_STORAGE_BUFFER,0);
        tokenOrig.BindBufferRange(GL_SHADER_STORAGE_BUFFER,1);
        tokenOutSizes.BindBufferRange(GL_SHADER_STORAGE_BUFFER,2);
        tokenOutScan.BindBufferRange(GL_SHADER_STORAGE_BUFFER,3);
        tokenOutScanOffset.BindBufferRange(GL_SHADER_STORAGE_BUFFER,4);

        glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT | GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT);

        for (size_t i = 0; i < sequences.size(); i++){
      const Sequence& sequence = sequences[i];

            glUniform1ui(glGetUniformLocation(program_cmds,"startOffset"),sequence.offset);
            glUniform1i (glGetUniformLocation(program_cmds,"startID"),    sequence.first);
            glUniform1ui(glGetUniformLocation(program_cmds,"endOffset"),  sequence.endoffset);
            glUniform1i (glGetUniformLocation(program_cmds,"endID"),      sequence.first + sequence.num - 1);
            glUniform1ui(glGetUniformLocation(program_cmds,"terminateCmd"),nvtoken::s_nvcmdlist_header[GL_TERMINATE_SEQUENCE_COMMAND_NV]);
            glDrawArrays(GL_POINTS,sequence.first,sequence.num);
        }


        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glDisableVertexAttribArray(2);

        glBindBuffer(GL_ARRAY_BUFFER,0);

        for (GLuint i = 0; i < 5; i++){
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER,i,0);
        }

        glDisable(GL_RASTERIZER_DISCARD);
    }

    static final class Sequence {
        int       offset;
        int       endoffset;
        int       first;
        int       num;
    };
}
