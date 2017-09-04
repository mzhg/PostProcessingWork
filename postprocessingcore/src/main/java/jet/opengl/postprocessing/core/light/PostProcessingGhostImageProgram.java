package jet.opengl.postprocessing.core.light;

import java.io.IOException;

import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2017/5/3.
 */

final class PostProcessingGhostImageProgram extends GLSLProgram{
    private int uniformIndex = -1;
    private int colorCoeffIndex = -1;

    PostProcessingGhostImageProgram() throws IOException {
        setSourceFromFiles("shader_libs/PostProcessingGhostImageVS.vert", "shader_libs/PostProcessingGhostImagePS.frag");
        enable();
        int iChannel0Loc = getUniformLocation("g_Texture1");
        gl.glUniform1i(iChannel0Loc, 0);  // set the texture0 location.

        iChannel0Loc = getUniformLocation("g_Texture2");
        gl.glUniform1i(iChannel0Loc, 1);  // set the texture1 location.

        iChannel0Loc = getUniformLocation("g_Texture3");
        gl.glUniform1i(iChannel0Loc, 2);  // set the texture2 location.

        iChannel0Loc = getUniformLocation("g_Texture4");
        gl.glUniform1i(iChannel0Loc, 3);  // set the texture3 location.

        uniformIndex = getUniformLocation("g_Scalar");
        colorCoeffIndex = getUniformLocation("g_ColorCoeff");
    }

    public void setScale(float x, float y, float z, float w){
        gl.glUniform4f(uniformIndex, x, y, z, w);
    }

    public void setColorCoeff(float[] coeff){
        gl.glUniform4fv(colorCoeffIndex, CacheBuffer.wrap(coeff));
    }
}
