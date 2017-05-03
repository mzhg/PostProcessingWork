package jet.opengl.postprocessing.core;

import java.io.IOException;

import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.Macro;

/**
 * Created by mazhen'gui on 2017/4/18.
 */

public class PostProcessingDownsampleProgram extends GLSLProgram{

    private int centerIndex = -1;

    public PostProcessingDownsampleProgram(int method) throws IOException {
        setSourceFromFiles("shader_libs/PostProcessingDefaultScreenSpaceVS.vert", "shader_libs/PostProcessingDownsamplePS.frag",
                new Macro("METHOD", method));
        enable();
        int iChannel0Loc = getUniformLocation("g_Texture");
        gl.glUniform1i(iChannel0Loc, 0);  // set the texture0 location.
        centerIndex = getUniformLocation("g_TexelSize");
    }

    public void setTexelSize(float texelSizeX, float texelSizeY){
        if(centerIndex >= 0)
            gl.glUniform2f(centerIndex, texelSizeX, texelSizeY);
    }
}
