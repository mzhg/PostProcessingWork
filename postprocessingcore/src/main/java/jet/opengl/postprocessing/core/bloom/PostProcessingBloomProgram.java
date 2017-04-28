package jet.opengl.postprocessing.core.bloom;

import java.io.IOException;

import jet.opengl.postprocessing.shader.GLSLProgram;

/**
 * Created by mazhen'gui on 2017/4/18.
 */

final class PostProcessingBloomProgram extends GLSLProgram{

    private int centerIndex = -1;

    PostProcessingBloomProgram() throws IOException {
        setSourceFromFiles("shader_libs/PostProcessingDefaultScreenSpaceVS.vert", "shader_libs/PostProcessingBloomSetup.frag");
        enable();
        int iChannel0Loc = getUniformLocation("g_Texture");
        gl.glUniform1i(iChannel0Loc, 0);  // set the texture0 location.
        centerIndex = getUniformLocation("f2BloomThreshold");
    }

    public void setUniforms(float bloomThreshold, float exposureScale){
        if(centerIndex >= 0)
            gl.glUniform2f(centerIndex, bloomThreshold, exposureScale);
    }
}
