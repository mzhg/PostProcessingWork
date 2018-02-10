package jet.opengl.demos.flight404;

import org.lwjgl.util.vector.Matrix4f;

import java.io.IOException;

import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2018/2/10.
 */

final class BillBoardProgram extends GLSLProgram{
    final int mvp_index;
    final int size_index;

    public BillBoardProgram()  {


        try {
            setSourceFromFiles("flight404/shaders/emiter_debug.vert", "flight404/shaders/emiter_debug.frag");
        } catch (IOException e) {
            e.printStackTrace();
        }

        mvp_index = getUniformLocation("uMVP");
        size_index = getUniformLocation("pointSize");

        enable();
        setTextureUniform("sprite_texture", 0);
    }

    public void applyMVP(Matrix4f mat){ gl.glUniformMatrix4fv(mvp_index, false, CacheBuffer.wrap(mat));}
    public void applyPointSize(float size) { gl.glUniform1f(size_index, size);}
}
