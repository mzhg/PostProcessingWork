package jet.opengl.demos.flight404;

import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;

import java.io.IOException;

import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.ShaderLoader;
import jet.opengl.postprocessing.shader.ShaderSourceItem;
import jet.opengl.postprocessing.shader.ShaderType;

/**
 * Created by mazhen'gui on 2018/2/10.
 */

final class ParticleUpdateProgram extends GLSLProgram{
    final int perlin_index;
    final int gravity_index;
    final int floor_index;

    final int time_index;
    final int gra_index;
    final int counter_index;
    final int position_index;
    final int type_index;
    final int eye_index;
    final int seed_index;
    final int record_index;

    boolean allow_perlin = true;
    boolean allow_gravity = true;
    boolean allow_floor = true;

    ParticleUpdateProgram() {
        CharSequence vert = null;
        CharSequence gemo = null;

        try {
            vert = ShaderLoader.loadShaderFile("flight404/shaders/particle_update404.vert", false);
            gemo = ShaderLoader.loadShaderFile("flight404/shaders/particle_update404.gemo", false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        addLinkTask(this::bindFeedback);
        ShaderSourceItem vert_item = new ShaderSourceItem(vert, ShaderType.VERTEX);
        ShaderSourceItem gemo_item = new ShaderSourceItem(gemo, ShaderType.GEOMETRY);
        setSourceFromStrings(new ShaderSourceItem[]{vert_item, gemo_item});

//			uniform bool allow_perlin = true;
//			uniform bool allow_gravity = true;
//			uniform bool allow_floor = true;
//
//			uniform float timeAmout = 0.16;
//			uniform vec3 gravity = vec3(0, -.5, 0f);

        perlin_index = getUniformLocation("allow_perlin");
        gravity_index = getUniformLocation("allow_gravity");
        floor_index = getUniformLocation("allow_floor");
        time_index = getUniformLocation("timeAmout");
        gra_index = getUniformLocation("gravity");
        counter_index = getUniformLocation("counter");
        type_index = getUniformLocation("u_type");
        position_index = getUniformLocation("position");
        eye_index = getUniformLocation("eye_loc");
        seed_index = getUniformLocation("u_seed");
        record_index = getUniformLocation("record_tail");

        enable();
        setTextureUniform("random_texture", 0);
    }

    private void bindFeedback(int programid){

//		out vec3 loc;
//		out vec3 vel;
//		out float radius;
//		out float age;
//		out float lifeSpan;
//		out float gen;
//		out float bounceAge;

//		layout (stream = 1) out vec3 n_loc;
//		layout (stream = 1) out vec3 n_vel;
//		layout (stream = 1) out float n_radius;
//		layout (stream = 1) out float n_age;
//		layout (stream = 1) out float n_lifeSpan;
//		layout (stream = 1) out float is_grounded;   // boolean variable
//		layout (stream = 1) out float n_scale;
//		layout (stream = 1) out uint n_color;       // a packed uint color.
        final String[] varyings =
                {
                        "loc", "vel","radius", "age", "lifeSpan", "gen", "bounceAge","type", "tail0", "tail1", "tail2", "tail3",
                        "gl_NextBuffer",
                        "n_loc", "n_vel", "n_radius", "n_age", "n_lifeSpan", "is_grounded", "n_scale", "n_color"
                };

        gl.glTransformFeedbackVaryings(programid, varyings, GLenum.GL_INTERLEAVED_ATTRIBS);
    }

    public void applyCounter(int counter){ gl.glUniform1i(counter_index, counter);}
    public void applyPosition(Vector3f pos) { gl.glUniform3f(position_index, pos.x, pos.y, pos.z);}
    public void applyType(int type) {gl.glUniform1i(type_index, type); }
    public void applyPerlin(boolean flag){
        if(allow_perlin != flag){
            gl.glUniform1i(perlin_index, flag ? 1 : 0);
            allow_perlin = flag;
        }
    }

    public void applyGravity(boolean flag){
        if(allow_gravity != flag){
            gl.glUniform1i(gravity_index, flag ? 1 : 0);
            allow_gravity = flag;
        }
    }

    public void applyFloor(boolean flag){
        if(allow_floor != flag){
            gl.glUniform1i(floor_index, flag ? 1 : 0);
            allow_floor = flag;
        }
    }

    public void applyGravity(Vector3f gravity){	gl.glUniform3f(gra_index, gravity.x, gravity.y, gravity.z);}

    public void applyTime(float time){ gl.glUniform1f(time_index, time);}
    public void applySeed(float seed){ gl.glUniform1f(seed_index, seed);}
    public void applyRecord(boolean record){ gl.glUniform1i(record_index, record ? 1 : 0);}

    public void applyEyePos(ReadableVector3f eyePos) { gl.glUniform3f(eye_index, eyePos.getX(), eyePos.getY(), eyePos.getZ());}
}
