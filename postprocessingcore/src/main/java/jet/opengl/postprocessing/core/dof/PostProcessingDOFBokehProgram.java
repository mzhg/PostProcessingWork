package jet.opengl.postprocessing.core.dof;

import java.io.IOException;
import java.nio.FloatBuffer;

import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2017/5/8.
 */

final class PostProcessingDOFBokehProgram  extends GLSLProgram {

    private int centerIndex = -1;

    PostProcessingDOFBokehProgram() throws IOException {
        setSourceFromFiles("shader_libs/PostProcessingDefaultScreenSpaceVS.vert", "shader_libs/PostProcessingDOFBokeh.frag");
        enable();
        int iChannel0Loc = getUniformLocation("g_RenderedTexture");
        if(iChannel0Loc >= 0)
        gl.glUniform1i(iChannel0Loc, 0);  // set the RenderedTexture location.
        iChannel0Loc = getUniformLocation("g_DepthTexture");
        if(iChannel0Loc >= 0)
        gl.glUniform1i(iChannel0Loc, 1);  // set the DepthTexture location.
        centerIndex = getUniformLocation("g_Uniforms");
    }

    public void setUniform(float textureWidth, float textureHeight, float znear, float zfar,
                           float focalDepth, float focalLength, float fstop){
        if(centerIndex >=0) {
            FloatBuffer buffer = CacheBuffer.getCachedFloatBuffer(8);
            buffer.put(textureWidth).put(textureHeight);
            buffer.put(znear).put(zfar);
            buffer.put(focalDepth).put(focalLength).put(fstop);
            buffer.put(0).flip();

            gl.glUniform4fv(centerIndex, buffer);
        }
    }
}
