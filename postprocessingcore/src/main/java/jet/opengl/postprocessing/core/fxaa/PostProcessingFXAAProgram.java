package jet.opengl.postprocessing.core.fxaa;

import java.io.IOException;

import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.Macro;

/**
 * Created by mazhen'gui on 2017/5/2.
 */

final class PostProcessingFXAAProgram extends GLSLProgram{

    private int centerIndex = -1;

    // The construct for android platform
    PostProcessingFXAAProgram() throws IOException {
        setSourceFromFiles("shader_libs/PostProcessingDefaultScreenSpaceVS.vert", "shader_libs/PostProcessingFXAA_ES.frag");

        enable();
        int iChannel0Loc = getUniformLocation("g_Texture");
        gl.glUniform1i(iChannel0Loc, 0);  // set the texture0 location.
        centerIndex = getUniformLocation("g_f2FrameRcpFrame");
    }

    // The construct for desktop
    PostProcessingFXAAProgram(int quality) throws IOException {
        setSourceFromFiles("shader_libs/PostProcessingFXAA.vert", "shader_libs/PostProcessingFXAA.frag", new Macro("FXAA_PRESET", quality));

        enable();
        int iChannel0Loc = getUniformLocation("g_Texture");
        gl.glUniform1i(iChannel0Loc, 0);  // set the texture0 location.
        centerIndex = getUniformLocation("g_f2FrameRcpFrame");
    }

    public void setTexelSize(float texelSizeX, float texelSizeY){
        gl.glUniform2f(centerIndex, texelSizeX, texelSizeY);
    }
}
