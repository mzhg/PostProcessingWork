package jet.opengl.postprocessing.core.radialblur;

import java.io.IOException;

import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.shader.GLSLProgram;

/**
 * Created by mazhen'gui on 2017/4/18.
 */

final class PostProcessingRadialBlurProgram extends GLSLProgram{

    PostProcessingRadialBlurProgram() throws IOException {
        setSourceFromFiles("shader_libs/PostProcessingDefaultScreenSpaceVS.vert", "shader_libs/PostProcessingRadialBlurPS.frag");
        enable();
        int iChannel0Loc = getUniformLocation("iChannel0");
        GLFuncProviderFactory.getGLFuncProvider().glUniform1i(iChannel0Loc, 0);  // set the texture location.
    }

}
