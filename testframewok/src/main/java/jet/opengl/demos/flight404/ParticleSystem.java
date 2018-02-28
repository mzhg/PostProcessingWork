package jet.opengl.demos.flight404;

import java.nio.ByteBuffer;

import jet.opengl.demos.nvidia.rain.TransformFeedbackObject;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2018/2/10.
 */

final class ParticleSystem {
    private static final int PARTICLE_SIZE = 48;

    private static final int PAR_LOC_OFFSET = 0;
    private static final int PAR_VEL_OFFSET = 12;
    private static final int PAR_RADIUS_OFFSET = 24;
    private static final int PAR_AGE_OFFSET = 28;
    private static final int PAR_LIFE_SPAN_OFFSET = 32;
    private static final int PAR_GEN_OFFSET = 36;
    private static final int PAR_BOUNCE_AGE_OFFSET = 40;
    private static final int PAR_TYPE_OFFSET = 44;

    private static final int MAX_PARTICLE_COUNT = 100_000;
    private static final int MAX_EMITTER_COUNT = 16;
    private static final int MAX_PARTICLE_TAIL_COUNT = 4;

    private static final int TYPE_BORN = 0;      // particles born
    private static final int TYPE_UPDATE = 1;    // update the particles
    private static final int TYPE_NEBULA = 2;    // update the nebulas
    private static final int TYPE_NEBORN = 3;    // born the emitter nebulas

    TransformFeedbackObject[] particle_chains = new TransformFeedbackObject[2];
    int emitter_count = MAX_EMITTER_COUNT/2;
    int emitter_source_buffer;
    ByteBuffer emitter_buffer;
    long last_update_time;

    int current_chain;
    private boolean first_loop;

    int count = 0;
    int random_texture_1d;
    int particle_sprite;
    int nebula_sprite;

    //		ParticleBornProgram particle_born;
    ParticleUpdateProgram particle_update;
    ParticleRenderProgram particle_render;
    ParticleReflctProgram particle_reflect;
    ParticleTailRenderProgram tail_render;
    private Emitter emitter;

    private GLFuncProvider gl;

    public ParticleSystem(Emitter emitter) {
        this.emitter = emitter;
        gl = GLFuncProviderFactory.getGLFuncProvider();
//			particle_born = new ParticleBornProgram();
        particle_update = new ParticleUpdateProgram();
        particle_render = new ParticleRenderProgram();
        particle_reflect= new ParticleReflctProgram();
        tail_render     = new ParticleTailRenderProgram();

        initEmitterSource();
        particleChans();

        random_texture_1d = createRandomTexture1D(256, 3);
        particle_sprite = Emitter.loadTexture("particle.png");
        nebula_sprite   = Emitter.loadTexture("corona.png");
    }

    // update the particles.
    void update(FrameData frameData, boolean rightButtonDown, float dt){
        // 1. update the emitter source.
        boolean needAddNewParticle = false;
        long current_time = System.currentTimeMillis();
        if(rightButtonDown){
            if(current_time - last_update_time > 50){
                gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, emitter_source_buffer);
                if(emitter_buffer != null){
                    emitter_buffer.clear();
                }

                /*ByteBuffer data = emitter_buffer = gl.glMapBufferRange(GLenum.GL_ARRAY_BUFFER,0, 8 * MAX_EMITTER_COUNT,GLenum.GL_WRITE_ONLY, emitter_buffer);
                if(data != null) {
                    data.position(0);
                }else{
                    data = emitter_buffer;
                }*/
                ByteBuffer data = CacheBuffer.getCachedByteBuffer(8 * MAX_EMITTER_COUNT);
                for(int i = 0; i < MAX_EMITTER_COUNT; i++){
                    float seed = (float)Math.random();
                    data.putFloat(seed);
                    data.putInt(0);
                }
                data.flip();
                gl.glBufferSubData(GLenum.GL_ARRAY_BUFFER, 0, data);

//                gl.glUnmapBuffer(GLenum.GL_ARRAY_BUFFER);
                last_update_time = System.currentTimeMillis();
                needAddNewParticle = true;
            }
        }

        // 2. perapre the transform feedback to record the particle data.
        particle_update.enable(); // we must bind the program first
        particle_update.applyPosition(emitter.emiter_pos);
        particle_update.applyCounter(count);
        particle_update.applyTime(dt);
        particle_update.applyEyePos(frameData.cameraPos);
        particle_update.applySeed((float)System.currentTimeMillis()/100000);

        particle_chains[current_chain].beginRecord(GLenum.GL_POINTS);
        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glBindTexture(GLenum.GL_TEXTURE_1D, random_texture_1d);
        {
            particle_update.applyType(TYPE_NEBORN);
            gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, emitter_source_buffer);
            gl.glVertexAttribPointer(0, 1, GLenum.GL_FLOAT, false, 8, 0);
            gl.glVertexAttribPointer(7, 1, GLenum.GL_UNSIGNED_INT, false, 8, 4);
            gl.glEnableVertexAttribArray(0);
            gl.glEnableVertexAttribArray(7);

            gl.glDrawArrays(GLenum.GL_POINTS, 0, 1);
            gl.glDisableVertexAttribArray(0);
            gl.glDisableVertexAttribArray(7);
            gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
        }

        if(needAddNewParticle){
            particle_update.applyType(TYPE_BORN);
            // 3. bind the emitter source.
            gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, emitter_source_buffer);
            gl.glVertexAttribPointer(0, 1, GLenum.GL_FLOAT, false, 8, 0);
            gl.glVertexAttribPointer(7, 1, GLenum.GL_UNSIGNED_INT, false, 8, 4);
            gl.glEnableVertexAttribArray(0);
            gl.glEnableVertexAttribArray(7);

            gl.glDrawArrays(GLenum.GL_POINTS, 0, MAX_EMITTER_COUNT);
            gl.glDisableVertexAttribArray(0);
            gl.glDisableVertexAttribArray(7);
            gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
        }

        //4. draw the previous transform feedback to current.
        if(first_loop){
            particle_update.applyType(TYPE_UPDATE);
            particle_update.applyRecord(count % 5 == 0);
            particle_chains[1-current_chain].drawStream(0);

            particle_update.applyType(TYPE_NEBULA);
            particle_chains[1-current_chain].drawStream(1);
        }

        // 5. done.
        particle_chains[current_chain].endRecord();
        first_loop = true;
        count ++;
        gl.glBindTexture(GLenum.GL_TEXTURE_1D, 0);

        if(Flight404.printOnce){
            particle_update.setName("Particle Update");
            particle_update.printPrograminfo();
        }

        GLCheck.checkError();
    }

    /** draw the particles. */
    void draw(FrameData frameData){
        //1, draw the particles.
        particle_render.enable();
        particle_render.applyModelView(frameData.view);
        particle_render.applyProjection(frameData.proj);
        particle_render.applyRenderType(true);
        emitter.context.enablePointSprite();
        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D, particle_sprite);
        particle_chains[current_chain].drawStream(0);

        // 1.1 draw the Nebulas.
        gl.glBindTexture(GLenum.GL_TEXTURE_2D, nebula_sprite);
        particle_render.applyRenderType(false);
        particle_chains[current_chain].drawStream(1);
        emitter.context.disablePointSprite();
        if(Flight404.printOnce){
            particle_render.setName("Particle Render");
            particle_render.printPrograminfo();
        }

        // 2, draw the particle reflects.
        particle_reflect.enable();
        particle_reflect.applyMVP(frameData.viewProj);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D, particle_sprite);
        particle_chains[current_chain].drawStream(0);
        if(Flight404.printOnce){
            particle_reflect.setName("Particle Reflect");
            particle_reflect.printPrograminfo();
        }

        // 3, draw the particle tail.
        tail_render.enable();
        tail_render.applyModelView(frameData.view);
        tail_render.applyProjection(frameData.proj);
        tail_render.applyRadius(0.5f);
        gl.glDisable(GLenum.GL_CULL_FACE);
        particle_chains[current_chain].drawStream(0);
        if(Flight404.printOnce){
            tail_render.setName("Tail Render");
            tail_render.printPrograminfo();
        }

        GLCheck.checkError();
        current_chain = 1 - current_chain;
    }

    void particleChans(){
        int size = PARTICLE_SIZE * MAX_PARTICLE_COUNT;
        int[] sizes = {size + MAX_PARTICLE_TAIL_COUNT * 12, size};
        Runnable[] bindings = {this::vertexTailBinding, this::vertexBinding};
        particle_chains[0] = new TransformFeedbackObject(sizes, bindings);
        particle_chains[1] = new TransformFeedbackObject(sizes, bindings);
    }

    void initEmitterSource(){
        emitter_source_buffer = gl.glGenBuffer();
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, emitter_source_buffer);
        gl.glBufferData(GLenum.GL_ARRAY_BUFFER, 8 * MAX_EMITTER_COUNT, GLenum.GL_STATIC_DRAW);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
    }

    private final void vertexBinding(){
        gl.glVertexAttribPointer(0, 3, GLenum.GL_FLOAT, false, PARTICLE_SIZE, PAR_LOC_OFFSET);
        gl.glVertexAttribPointer(1, 3, GLenum.GL_FLOAT, false, PARTICLE_SIZE, PAR_VEL_OFFSET);
        gl.glVertexAttribPointer(2, 1, GLenum.GL_FLOAT, false, PARTICLE_SIZE, PAR_RADIUS_OFFSET);
        gl.glVertexAttribPointer(3, 1, GLenum.GL_FLOAT, false, PARTICLE_SIZE, PAR_AGE_OFFSET);
        gl.glVertexAttribPointer(4, 1, GLenum.GL_FLOAT, false, PARTICLE_SIZE, PAR_LIFE_SPAN_OFFSET);
        gl.glVertexAttribPointer(5, 1, GLenum.GL_FLOAT, false, PARTICLE_SIZE, PAR_GEN_OFFSET);
        gl.glVertexAttribPointer(6, 1, GLenum.GL_FLOAT, false, PARTICLE_SIZE, PAR_BOUNCE_AGE_OFFSET);
        gl.glVertexAttribPointer(7, 1, GLenum.GL_UNSIGNED_INT, false, PARTICLE_SIZE, PAR_TYPE_OFFSET);

        for(int k = 0; k < 8; k++)
            gl.glEnableVertexAttribArray(k);
    }

    private final void vertexTailBinding(){
        final int stride = PARTICLE_SIZE + MAX_PARTICLE_TAIL_COUNT * 12;
        gl.glVertexAttribPointer(0, 3, GLenum.GL_FLOAT, false, stride, PAR_LOC_OFFSET);
        gl.glVertexAttribPointer(1, 3, GLenum.GL_FLOAT, false, stride, PAR_VEL_OFFSET);
        gl.glVertexAttribPointer(2, 1, GLenum.GL_FLOAT, false, stride, PAR_RADIUS_OFFSET);
        gl.glVertexAttribPointer(3, 1, GLenum.GL_FLOAT, false, stride, PAR_AGE_OFFSET);
        gl.glVertexAttribPointer(4, 1, GLenum.GL_FLOAT, false, stride, PAR_LIFE_SPAN_OFFSET);
        gl.glVertexAttribPointer(5, 1, GLenum.GL_FLOAT, false, stride, PAR_GEN_OFFSET);
        gl.glVertexAttribPointer(6, 1, GLenum.GL_FLOAT, false, stride, PAR_BOUNCE_AGE_OFFSET);
        gl.glVertexAttribPointer(7, 1, GLenum.GL_UNSIGNED_INT, false, stride, PAR_TYPE_OFFSET);

        for(int i = 0;i < MAX_PARTICLE_TAIL_COUNT; i++){
            gl.glVertexAttribPointer(8 + i, 3, GLenum.GL_FLOAT, false, stride, PAR_TYPE_OFFSET + 4 + i * 12);
        }

        for(int k = 0; k < 8 + MAX_PARTICLE_TAIL_COUNT; k++)
            gl.glEnableVertexAttribArray(k);
    }

    private static int measureInterformat(int req_comp){
        switch (req_comp) {
            case 1:  	return GLenum.GL_R8;
            case 2:		return GLenum.GL_RG8;
            case 3:     return GLenum.GL_RGB8;
            case 4:     return GLenum.GL_RGBA8;
            default:
                throw new IllegalArgumentException("req_comp = " + req_comp);
        }
    }

    private int createRandomTexture1D(int width, int req_comp){
        int internalFormat = measureInterformat(req_comp);

        ByteBuffer buf = CacheBuffer.getCachedByteBuffer(width * req_comp);
        for(int i = 0; i < width; i++){
            for(int j = 0;j < req_comp; j++)
                buf.put((byte)(Math.random() * 255));
        }

        buf.flip();

        int texture = gl.glGenTexture();
        gl.glBindTexture(GLenum.GL_TEXTURE_1D, texture);
        gl.glTexImage1D(GLenum.GL_TEXTURE_1D, 0, internalFormat, width, 0, TextureUtils.measureFormat(internalFormat), GLenum.GL_UNSIGNED_BYTE, buf);
        gl.glTexParameteri(GLenum.GL_TEXTURE_1D, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_LINEAR);
        gl.glTexParameteri(GLenum.GL_TEXTURE_1D, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_LINEAR);
        gl.glTexParameteri(GLenum.GL_TEXTURE_1D, GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_REPEAT);
        gl.glBindTexture(GLenum.GL_TEXTURE_1D, 0);

        return texture;
    }
}
