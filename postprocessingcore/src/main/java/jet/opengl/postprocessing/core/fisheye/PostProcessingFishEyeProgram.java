package jet.opengl.postprocessing.core.fisheye;

import java.io.IOException;

import jet.opengl.postprocessing.shader.GLSLProgram;

/**
 * Created by mazhen'gui on 2017/4/18.
 */

final class PostProcessingFishEyeProgram extends GLSLProgram{

    private int centerIndex = -1;

    PostProcessingFishEyeProgram() throws IOException {
        setSourceFromFiles("shader_libs/PostProcessingDefaultScreenSpaceVS.vert", "shader_libs/PostProcessingFisheyePS.frag");
        enable();
        int iChannel0Loc = getUniformLocation("iChannel0");
        gl.glUniform1i(iChannel0Loc, 0);  // set the texture0 location.
        centerIndex = getUniformLocation("g_Uniforms");
    }

    public void setUniforms(float resolutionX, float resolutionY, float factor){
        if(centerIndex >= 0)
            gl.glUniform4f(centerIndex, resolutionX, resolutionY, factor, 0);
    }
}
