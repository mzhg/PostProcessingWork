package jet.opengl.postprocessing.core.light;

import java.io.IOException;

import jet.opengl.postprocessing.shader.GLSLProgram;

/**
 * Created by mazhen'gui on 2017/5/3.
 */

// TODO This program can be replaced by PostProcessingCombineProgram
final class PostProcessingGlareComposeProgram extends GLSLProgram{
    private int uniformIndex = -1;

    PostProcessingGlareComposeProgram() throws IOException {
        setSourceFromFiles("shader_libs/PostProcessingDefaultScreenSpaceVS.vert", "shader_libs/PostProcessingGlareComposePS.frag");
        enable();
        int iChannel0Loc = getUniformLocation("g_Texture1");
        gl.glUniform1i(iChannel0Loc, 0);  // set the texture0 location.

        iChannel0Loc = getUniformLocation("g_Texture2");
        gl.glUniform1i(iChannel0Loc, 1);  // set the texture0 location.

        iChannel0Loc = getUniformLocation("g_Texture3");
        gl.glUniform1i(iChannel0Loc, 2);  // set the texture0 location.

        iChannel0Loc = getUniformLocation("g_Texture4");
        gl.glUniform1i(iChannel0Loc, 3);  // set the texture0 location.

        uniformIndex = getUniformLocation("g_MixCoeff");
    }

    public void setMixCoeff(float x, float y, float z, float w){
        gl.glUniform4f(uniformIndex, x, y, z, w);
    }

}
