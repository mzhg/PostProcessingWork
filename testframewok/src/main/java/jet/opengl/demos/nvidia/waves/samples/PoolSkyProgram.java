package jet.opengl.demos.nvidia.waves.samples;

import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Matrix4f;

import jet.util.buffer.GLUtil;
import jet.util.opengl.shader.GLSLProgram;
import jet.util.opengl.shader.libs.SimpleProgram;

/**
 * Created by mazhen'gui on 2017/3/21.
 */

public class PoolSkyProgram extends SimpleProgram{
    private int mvpIndex = -1;

    public PoolSkyProgram(String shaderPath){
    	compile(shaderPath + "poolsky.vert", shaderPath +  "poolsky.frag", COpenGLRenderer.POS_BINDER);
    	mvpIndex =GL20.glGetUniformLocation(programId, "g_mvp");
    	
    	enable();
    	GL20.glUniform1i(GLSLProgram.getUniformLocation(programId, "PoolSkyCubeMap"), 0);
    	disable();
    }

    public void setMVP(Matrix4f mvpMat){
        GL20.glUniformMatrix4fv(mvpIndex, false, GLUtil.wrap(mvpMat));
    }

    public final int getAttribPosition() { return 0;}
}
