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

final class ParticleRenderProgram extends GLSLProgram{
    final int proj_index;
    final int mv_index;
    final int render_index;

    public ParticleRenderProgram() {
        CharSequence vert = null;
        CharSequence gemo = null;
        CharSequence frag = null;

        try {
            vert = ShaderLoader.loadShaderFile("flight404/shaders/particle_update404.vert", false);
            gemo = ShaderLoader.loadShaderFile("flight404/shaders/particle_render404.gemo", false);
            frag = ShaderLoader.loadShaderFile("flight404/shaders/particles_render.frag", false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        setSourceFromStrings(new ShaderSourceItem[]{
                new ShaderSourceItem(vert, ShaderType.VERTEX),
                new ShaderSourceItem(gemo, ShaderType.GEOMETRY),
                new ShaderSourceItem(frag, ShaderType.FRAGMENT),
        });

        proj_index = getUniformLocation("projection");
        mv_index   = getUniformLocation("modelView");
        render_index   = getUniformLocation("render_particle");

        enable();
        setTextureUniform("sprite_texture", 0);
    }

    public void applyProjection(Matrix4f mat){ gl.glUniformMatrix4fv(proj_index, false, CacheBuffer.wrap(mat));}
    public void applyModelView(Matrix4f mat) { gl.glUniformMatrix4fv(mv_index,   false, CacheBuffer.wrap(mat));}
    public void applyRenderType(boolean particle) { gl.glUniform1i(render_index, particle ? 1 : 0);}
}
