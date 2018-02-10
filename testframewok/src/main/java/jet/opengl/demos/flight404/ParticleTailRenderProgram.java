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

final class ParticleTailRenderProgram extends GLSLProgram{
    final int proj_index;
    final int mv_index;
    final int radius_index;

    ParticleTailRenderProgram() {
        CharSequence vert = null;
        CharSequence gemo = null;
        CharSequence frag = null;

        try {
            vert = ShaderLoader.loadShaderFile("flight404/shaders/particle_update404.vert", false);
            gemo = ShaderLoader.loadShaderFile("flight404/shaders/particle_tail_render404.gemo", false);
            frag = ShaderLoader.loadShaderFile("flight404/shaders/particle_tail_render404.frag", false);
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
        radius_index   = getUniformLocation("radius");
    }

    public void applyProjection(Matrix4f mat){ gl.glUniformMatrix4fv(proj_index, false, CacheBuffer.wrap(mat));}
    public void applyModelView(Matrix4f mat) { gl.glUniformMatrix4fv(mv_index,   false, CacheBuffer.wrap(mat));}
    public void applyRadius(float radius)    { if(radius_index >=0) gl.glUniform1f(radius_index, radius);}
}
