package jet.opengl.postprocessing.core.tonemapping;

import java.io.IOException;

import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.Macro;

/**
 * Created by mazhen'gui on 2017/4/18.
 */

final class PostProcessingTonemappingProgram extends GLSLProgram{

    private int centerIndex = -1;

    PostProcessingTonemappingProgram(boolean eyeAdaption) throws IOException {
        setSourceFromFiles("shader_libs/PostProcessingDefaultScreenSpaceVS.vert", "shader_libs/PostProcessingTonemappingPS.frag",
                new Macro("EYE_ADAPATION", eyeAdaption));

        enable();
        centerIndex = getUniformLocation("g_Uniforms");
        int iChannel0Loc = getUniformLocation("g_SceneTex");
        gl.glUniform1i(iChannel0Loc, 0);  // set the SceneTex location.
        iChannel0Loc = getUniformLocation("g_BlurTex");
        gl.glUniform1i(iChannel0Loc, 1);  // set the BlurTex location.

        if(!eyeAdaption)
            return;

        iChannel0Loc = getUniformLocation("g_LumTex");
        gl.glUniform1i(iChannel0Loc, 2);  // set the LumTex location.
    }

    public void setUniforms(float blurAmout, float exposure, float gamma){
        if(centerIndex >= 0)
            gl.glUniform4f(centerIndex, blurAmout, exposure, gamma, 0);
    }
}
