package jet.opengl.postprocessing.core;

import java.io.IOException;

import jet.opengl.postprocessing.shader.GLSLProgram;

/**
 * Created by mazhen'gui on 2017/4/18.
 */

public class PostProcessingDefaultProgram extends GLSLProgram{
    public PostProcessingDefaultProgram() throws IOException {
        setSourceFromFiles("shader_libs/PostProcessingDefaultScreenSpaceVS.vert", "shader_libs/PostProcessingDefaultScreenSpacePS.frag");
        enable();
        int iChannel0Loc = getUniformLocation("g_InputTex");
        gl.glUniform1i(iChannel0Loc, 0);  // set the texture0 location.
    }
}
