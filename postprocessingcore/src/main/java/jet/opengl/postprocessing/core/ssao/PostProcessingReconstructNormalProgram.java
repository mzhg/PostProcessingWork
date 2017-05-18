package jet.opengl.postprocessing.core.ssao;

import org.lwjgl.util.vector.Matrix4f;

import java.io.IOException;
import java.nio.FloatBuffer;

import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.util.CachaRes;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2017/4/18.
 */

public class PostProcessingReconstructNormalProgram extends GLSLProgram{

    private int centerIndex = -1;
    private int texelIndex = -1;

    private int projInfoIndex;
    private int projOrthoIndex;

    public PostProcessingReconstructNormalProgram() throws IOException {
        setSourceFromFiles("shader_libs/PostProcessingDefaultScreenSpaceVS.vert", "shader_libs/HBAO/PostProcessingReconstructNormalPS.frag");

        enable();
        int iChannel0Loc = getUniformLocation("g_LinearDepthTex");
        gl.glUniform1i(iChannel0Loc, 0);  // set the texture0 location.
        centerIndex = getUniformLocation("g_Uniforms");
        texelIndex = getUniformLocation("g_InvFullResolution");

        projInfoIndex = getUniformLocation("projInfo");
        projOrthoIndex = getUniformLocation("projOrtho");
    }

    @CachaRes
    public void setCameraMatrixs(Matrix4f proj, Matrix4f invert){
        if(centerIndex < 0)
            return;

        FloatBuffer buffer = CacheBuffer.getCachedFloatBuffer(16 * 2);
        proj.store(buffer);
        invert.store(buffer);
        buffer.flip();
        gl.glUniformMatrix4fv(centerIndex, false, buffer);
        GLCheck.checkError("ReconstructNormalProgram::setCameraMatrixs");
    }

    public void setTexelSize(float texelSizeX, float texelSizeY){
        gl.glUniform2f(texelIndex, texelSizeX, texelSizeY);
    }

    void setProjInfo(float x, float y, float z, float w){
        if(projInfoIndex >= 0)
            gl.glUniform4f(projInfoIndex, x, y, z, w);
    }

    void setProjOrtho(int x){
        if(projOrthoIndex >= 0)
            gl.glUniform1i(projOrthoIndex, x);
    }
}
