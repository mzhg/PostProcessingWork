package jet.opengl.demos.nvidia.waves.samples;

import org.lwjgl.util.vector.Matrix4f;

import java.io.IOException;

import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2017/3/21.
 */

public class PoolSkyProgram extends GLSLProgram{
    private int mvpIndex = -1;

    public PoolSkyProgram(String shaderPath){
//    	compile(shaderPath + "poolsky.vert", shaderPath +  "poolsky.frag", COpenGLRenderer.POS_BINDER);
        try {
            setSourceFromFiles(shaderPath + "poolsky.vert", shaderPath +  "poolsky.frag");
        } catch (IOException e) {
            e.printStackTrace();
        }
        mvpIndex =gl.glGetUniformLocation(getProgram(), "g_mvp");
        enable();
        setTextureUniform("PoolSkyCubeMap", 0);
    }

    public void setMVP(Matrix4f mvpMat){
        gl.glUniformMatrix4fv(mvpIndex, false, CacheBuffer.wrap(mvpMat));
    }

    public final int getAttribPosition() { return 0;}
}
