package jet.opengl.postprocessing.core.light;

import java.io.IOException;

import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2017/5/3.
 */

final class PostProcessingLightStreakerProgram extends GLSLProgram{
    private int uniformIndex = -1;
    private int colorCoeffIndex = -1;


    PostProcessingLightStreakerProgram() throws IOException {
        setSourceFromFiles("shader_libs/PostProcessingStarStreakVS.vert", "shader_libs/PostProcessingStarStreakPS.frag");
        enable();
        int iChannel0Loc = getUniformLocation("g_Texture");
        gl.glUniform1i(iChannel0Loc, 0);  // set the texture0 location.
        uniformIndex = getUniformLocation("g_Uniforms");
        colorCoeffIndex = getUniformLocation("g_ColorCoeff");
    }

    public void setUniforms(float stepSizeX, float stepSizeY, float stride){
        gl.glUniform4f(uniformIndex, stepSizeX, stepSizeY, stride, 0);
    }

    public void setColorCoeff(float[] coeff){
        gl.glUniform4fv(colorCoeffIndex, CacheBuffer.wrap(coeff));
    }
}
