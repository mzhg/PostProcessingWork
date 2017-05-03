package jet.opengl.postprocessing.core.eyeAdaption;

import java.io.IOException;

import jet.opengl.postprocessing.shader.GLSLProgram;

/**
 * Created by mazhen'gui on 2017/5/3.
 */

public class PostProcessingCalculateLuminanceProgram extends GLSLProgram {
    private int centerIndex = -1;

    PostProcessingCalculateLuminanceProgram() throws IOException {
        setSourceFromFiles("shader_libs/PostProcessingDefaultScreenSpaceVS.vert", "shader_libs/PostProcessingCalculateLuminance.frag");
        enable();
        int iChannel0Loc = getUniformLocation("g_InputImage");
        gl.glUniform1i(iChannel0Loc, 0);  // set the texture0 location.
        iChannel0Loc = getUniformLocation("g_LastLumTex");
        gl.glUniform1i(iChannel0Loc, 1);  // set the texture0 location.

        centerIndex = getUniformLocation("g_ElapsedTime");
    }

    public void setElapsedTime(float elapsedTime){
        gl.glUniform1f(centerIndex, elapsedTime);
    }
}
