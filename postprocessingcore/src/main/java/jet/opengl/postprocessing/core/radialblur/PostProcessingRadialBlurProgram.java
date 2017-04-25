package jet.opengl.postprocessing.core.radialblur;

import java.io.IOException;

import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.shader.GLSLProgram;

/**
 * Created by mazhen'gui on 2017/4/18.
 */

public final class PostProcessingRadialBlurProgram extends GLSLProgram{

    private int centerIndex = -1;

    PostProcessingRadialBlurProgram() throws IOException {
        setSourceFromFiles("shader_libs/PostProcessingDefaultScreenSpaceVS.vert", "shader_libs/PostProcessingRadialBlurPS.frag");
        enable();
        int iChannel0Loc = getUniformLocation("iChannel0");
        centerIndex = getUniformLocation("g_UniformValue");
        GLFuncProviderFactory.getGLFuncProvider().glUniform1i(iChannel0Loc, 0);  // set the texture location.
    }

    public void setUniformValue(float centerX, float centerY, float gloablTime, float samples){
        gl.glUniform4f(centerIndex, centerX,centerY, gloablTime, samples);
    }
}
