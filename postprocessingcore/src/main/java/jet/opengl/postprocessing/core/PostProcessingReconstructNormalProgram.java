package jet.opengl.postprocessing.core;

import org.lwjgl.util.vector.Matrix4f;

import java.io.IOException;
import java.nio.FloatBuffer;

import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.util.CachaRes;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2017/4/18.
 */

public class PostProcessingReconstructNormalProgram extends GLSLProgram{

    private int centerIndex = -1;

    public PostProcessingReconstructNormalProgram() throws IOException {
        setSourceFromFiles("shader_libs/PostProcessingDefaultScreenSpaceVS.vert", "shader_libs/PostProcessingReconstructCamSpaceZPS.frag");

        enable();
        int iChannel0Loc = getUniformLocation("g_LinearDepthTex");
        gl.glUniform1i(iChannel0Loc, 0);  // set the texture0 location.
        centerIndex = getUniformLocation("g_Uniforms");
    }

    @CachaRes
    public void setCameraMatrixs(Matrix4f proj, Matrix4f invert){
        FloatBuffer buffer = CacheBuffer.getCachedFloatBuffer(16 * 2);
        proj.store(buffer);
        invert.store(buffer);
        buffer.flip();
        gl.glUniformMatrix4fv(centerIndex, false, buffer);
    }
}
