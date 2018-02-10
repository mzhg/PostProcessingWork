package jet.opengl.demos.flight404;

import org.lwjgl.util.vector.Matrix4f;

import java.io.IOException;

import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.ShaderLoader;
import jet.opengl.postprocessing.shader.ShaderSourceItem;
import jet.opengl.postprocessing.shader.ShaderType;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2018/2/10.
 */

final class ParticleReflctProgram extends GLSLProgram{
    final int u_mvp;

    public ParticleReflctProgram() {
        CharSequence vert = null;
        CharSequence gemo = null;
        CharSequence frag = null;

        try {
            vert = ShaderLoader.loadShaderFile("flight404/shaders/particle_update404.vert", false);
            gemo = ShaderLoader.loadShaderFile("flight404/shaders/particle_reflect404.gemo", false);
            frag = ShaderLoader.loadShaderFile("flight404/shaders/emitter_reflect404.frag", false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        setSourceFromStrings(new ShaderSourceItem[]{
                new ShaderSourceItem(vert, ShaderType.VERTEX),
                new ShaderSourceItem(gemo, ShaderType.GEOMETRY),
                new ShaderSourceItem(frag, ShaderType.FRAGMENT),
        });

        u_mvp   = getUniformLocation("u_mvp");

        enable();
        setTextureUniform("sprite_texture", 0);
    }

    public void applyMVP  (Matrix4f mat)   { gl.glUniformMatrix4fv(u_mvp, false, CacheBuffer.wrap(mat));}
}
