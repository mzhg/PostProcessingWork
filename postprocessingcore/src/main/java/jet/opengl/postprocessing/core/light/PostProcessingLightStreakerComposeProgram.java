package jet.opengl.postprocessing.core.light;

import java.io.IOException;

import jet.opengl.postprocessing.shader.GLSLProgram;

/**
 * Created by mazhen'gui on 2017/5/3.
 */

final class PostProcessingLightStreakerComposeProgram extends GLSLProgram{

    PostProcessingLightStreakerComposeProgram() throws IOException {
        setSourceFromFiles("shader_libs/PostProcessingDefaultScreenSpaceVS.vert", "shader_libs/PostProcessingStarStreakCompose.frag");
        enable();
        int iChannel0Loc = getUniformLocation("g_Texture1");
        gl.glUniform1i(iChannel0Loc, 0);  // set the texture0 location.

        int iChannel1Loc = getUniformLocation("g_Texture2");
        gl.glUniform1i(iChannel1Loc, 1);  // set the texture0 location.

        int iChannel2Loc = getUniformLocation("g_Texture3");
        gl.glUniform1i(iChannel2Loc, 2);  // set the texture0 location.

        int iChannel3Loc = getUniformLocation("g_Texture4");
        gl.glUniform1i(iChannel3Loc, 3);  // set the texture0 location.
    }
}
