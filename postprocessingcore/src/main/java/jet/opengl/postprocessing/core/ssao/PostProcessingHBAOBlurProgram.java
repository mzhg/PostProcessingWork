package jet.opengl.postprocessing.core.ssao;

import java.io.IOException;

import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.util.CachaRes;

/**
 * Created by mazhen'gui on 2017/4/18.
 */

final class PostProcessingHBAOBlurProgram extends GLSLProgram{

    private int centerIndex = -1;

    public PostProcessingHBAOBlurProgram() throws IOException {
        setSourceFromFiles("shader_libs/PostProcessingDefaultScreenSpaceVS.vert", "shader_libs/PostProcessingHBAOBlurPS.frag");

        enable();
        int iChannel0Loc = getUniformLocation("g_TexSource");
        gl.glUniform1i(iChannel0Loc, 0);  // set the texture0 location.
        centerIndex = getUniformLocation("g_Uniforms");
    }

    @CachaRes
    public void setUVAndResolution(float invWidth, float invHeight, float sharpness){
        gl.glUniform4f(centerIndex, invWidth, invHeight, sharpness, 0);
    }
}
