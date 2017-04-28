package jet.opengl.postprocessing.core.toon;

import java.io.IOException;

import jet.opengl.postprocessing.shader.GLSLProgram;

/**
 * Created by mazhen'gui on 2017/4/18.
 */

final class PostProcessingToonProgram extends GLSLProgram{

    private int centerIndex = -1;

    PostProcessingToonProgram() throws IOException {
        setSourceFromFiles("shader_libs/PostProcessingDefaultScreenSpaceVS.vert", "shader_libs/PostProcessingToonPS.frag");
        enable();
        int iChannel0Loc = getUniformLocation("g_Texture");
        gl.glUniform1i(iChannel0Loc, 0);  // set the texture0 location.
        centerIndex = getUniformLocation("g_Uniforms");
    }

    public void setUniforms(float edge_thres, float edge_thres2, float texelSizeX, float texelSizeY){
        if(centerIndex >= 0)
            gl.glUniform4f(centerIndex, edge_thres, edge_thres2, texelSizeX, texelSizeY);
    }
}
