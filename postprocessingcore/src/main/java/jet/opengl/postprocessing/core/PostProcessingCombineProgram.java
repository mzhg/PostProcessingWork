package jet.opengl.postprocessing.core;

import java.io.IOException;

import jet.opengl.postprocessing.shader.GLSLProgram;

/**
 * Created by mazhen'gui on 2017/4/18.
 */

final class PostProcessingCombineProgram extends GLSLProgram{

    private int centerIndex = -1;

    PostProcessingCombineProgram() throws IOException {
        setSourceFromFiles("shader_libs/PostProcessingDefaultScreenSpaceVS.vert", "shader_libs/PostProcessingCombinePS.frag");
        enable();
        int iChannel0Loc = getUniformLocation("g_Texture0");
        gl.glUniform1i(iChannel0Loc, 0);  // set the texture0 location.
        int iChannel1Loc = getUniformLocation("g_Texture1");
        gl.glUniform1i(iChannel1Loc, 1);  // set the texture1 location.
        centerIndex = getUniformLocation("g_f2Intensity");
    }

    public void setIntensity(float tex0Factor, float tex1Factor){
        gl.glUniform2f(centerIndex, tex0Factor, tex1Factor);
    }
}
