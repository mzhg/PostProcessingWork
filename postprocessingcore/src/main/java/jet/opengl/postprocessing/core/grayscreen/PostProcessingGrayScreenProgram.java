package jet.opengl.postprocessing.core.grayscreen;

import java.io.IOException;

import jet.opengl.postprocessing.shader.GLSLProgram;

/**
 * Created by mazhen'gui on 2021/8/2.
 */

final class PostProcessingGrayScreenProgram extends GLSLProgram{

    private int centerIndex = -1;

    PostProcessingGrayScreenProgram() throws IOException {
        setSourceFromFiles("shader_libs/PostProcessingDefaultScreenSpaceVS.vert", "shader_libs/GrayScreenPS.frag");
        enable();
        int iChannel0Loc = getUniformLocation("iChannel0");
        gl.glUniform1i(iChannel0Loc, 0);  // set the texture0 location.

        int iPaperLoc = getUniformLocation("iPaper");
        gl.glUniform1i(iPaperLoc, 1);  // set the texture0 location.
        centerIndex = getUniformLocation("g_Uniforms");
    }

    public void setUniforms(float resolutionX, float resolutionY, float rectBorder){
        if(centerIndex >= 0)
            gl.glUniform4f(centerIndex, resolutionX, resolutionY, rectBorder, 0.15f);
    }
}
