package jet.opengl.postprocessing.core.light;

import java.io.IOException;

import jet.opengl.postprocessing.shader.GLSLProgram;

/**
 * Created by mazhen'gui on 2017/5/3.
 */

final class PostProcessingExtractHighLightProgram extends GLSLProgram {
    private int centerIndex = -1;

    PostProcessingExtractHighLightProgram() throws IOException {
        setSourceFromFiles("shader_libs/PostProcessingDefaultScreenSpaceVS.vert", "shader_libs/PostProcessingExtractHighLight.frag");
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
