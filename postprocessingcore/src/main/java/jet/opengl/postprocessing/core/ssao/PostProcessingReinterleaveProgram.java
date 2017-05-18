package jet.opengl.postprocessing.core.ssao;

import java.io.IOException;

import jet.opengl.postprocessing.shader.GLSLProgram;

/**
 * Created by mazhen'gui on 2017-05-11 15:29:12.
 */

final class PostProcessingReinterleaveProgram extends GLSLProgram{
    public PostProcessingReinterleaveProgram() throws IOException {
        setSourceFromFiles("shader_libs/PostProcessingDefaultScreenSpaceVS.vert", "shader_libs/HBAO/PostProcessingHBAOReinterleavePS.frag");

        enable();
        int iChannel0Loc = getUniformLocation("texResultsArray");
        gl.glUniform1i(iChannel0Loc, 0);  // set the texture0 location.
    }

}
