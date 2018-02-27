package jet.opengl.demos.flight404;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.io.IOException;

import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.ShaderLoader;
import jet.opengl.postprocessing.shader.ShaderSourceItem;
import jet.opengl.postprocessing.shader.ShaderType;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2018/2/10.
 */

final class EmitterReflctProgram extends GLSLProgram{
    final int u_right;
    final int u_mvp;
    final int u_up;

    public EmitterReflctProgram() {
        CharSequence vert = null;
        CharSequence gemo = null;
        CharSequence frag = null;

        try {
            vert = ShaderLoader.loadShaderFile("flight404/shaders/emitter_reflect404.vert", false);
            gemo = ShaderLoader.loadShaderFile("flight404/shaders/emitter_reflect404.gemo", false);
            frag = ShaderLoader.loadShaderFile("flight404/shaders/emitter_reflect404.frag", false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        setSourceFromStrings(new ShaderSourceItem[]{
                new ShaderSourceItem(vert, ShaderType.VERTEX),
                new ShaderSourceItem(gemo, ShaderType.GEOMETRY),
                new ShaderSourceItem(frag, ShaderType.FRAGMENT),
        });

        u_right = getUniformLocation("u_right");
        u_mvp   = getUniformLocation("u_mvp");
        u_up    = getUniformLocation("u_up");
        GLCheck.checkError();

        enable();
        GLCheck.checkError();
        setTextureUniform("sprite_texture", 0);
        GLCheck.checkError();
    }

    public void applyUp   (Vector3f up) { gl.glUniform3f(u_up, up.x, up.y, up.z);}
    public void applyRight(Vector3f right) { gl.glUniform3f(u_right, right.x, right.y, right.z);}
    public void applyMVP  (Matrix4f mat)   { gl.glUniformMatrix4fv(u_mvp, false, CacheBuffer.wrap(mat));}
}
