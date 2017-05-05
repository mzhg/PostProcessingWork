package jet.opengl.postprocessing.core.light;

import java.io.IOException;

import jet.opengl.postprocessing.shader.GLSLProgram;

/**
 * Created by mazhen'gui on 2017/5/5.
 */

final class Downsample4xProgram extends GLSLProgram {

    private int centerIndex = -1;

    Downsample4xProgram() throws IOException {
        setSourceFromFiles("shader_libs/downSample4x.vert", "shader_libs/downSample4x.frag");
        enable();
        int iChannel0Loc = getUniformLocation("sampler");
        gl.glUniform1i(iChannel0Loc, 0);  // set the texture0 location.
        centerIndex = getUniformLocation("twoTexelSize");
    }

    public void setTwoTexelSize(float x, float y){
        if(centerIndex >= 0)
            gl.glUniform2f(centerIndex, x, y);
    }
}
