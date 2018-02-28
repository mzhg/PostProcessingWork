package jet.opengl.demos.flight404;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import java.io.IOException;
import java.nio.FloatBuffer;

import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2018/2/10.
 */

final class Emitter {
    final Vector3f emiter_pos = new Vector3f();
    final Vector3f emiter_dir = new Vector3f();
    final Vector2f mouse_prev = new Vector2f();
    final Vector2f mouse_curr = new Vector2f();
    final Vector2f mouse_vel = new Vector2f();
    final Vector3f right = new Vector3f();
    final Vector3f up = new Vector3f();
    private final float emiter_diam = 5;

    private BillBoardProgram	 render_program;
    private EmitterReflctProgram reflect_program;
    int emitter_sprite;
    int reflect_sprite;

    private GLFuncProvider gl;
    Flight404 context;

    public Emitter(Flight404 context) {
        this.context = context;
        gl = GLFuncProviderFactory.getGLFuncProvider();
        GLCheck.checkError();
        reflect_program = new EmitterReflctProgram();
        render_program = new BillBoardProgram();

        GLCheck.checkError();
        emitter_sprite = loadTexture("emitter.png");
        reflect_sprite = loadTexture("reflection.png");
    }

    static int loadTexture(String filename){
        try {
            return TextureUtils.createTexture2DFromFile("Flight404/textures/" + filename, false).getTexture();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return 0;
    }

    void update(FrameData frameData, float dt){
        ReadableVector3f eyePos = frameData.cameraPos;
        mouse_curr.set(frameData.mouseX, frameData.mouseY);
        Vector2f.sub(mouse_curr, mouse_prev, mouse_vel);
        mouse_vel.scale(dt * 5);
        Vector2f.add(mouse_prev, mouse_vel,mouse_prev);

        float half_height = frameData.screenHeight * 0.5f;
        if(mouse_prev.y > half_height){
            mouse_prev.y = half_height;
        }

//			System.out.println("mouseX = " + sx + ", mouseY = " + sy);
        // Compute picking ray in view space.
        Vector3f dir = grap(frameData, mouse_prev.x, mouse_prev.y, emiter_dir);

        Vector3f plane_normal = right;
        plane_normal.set(eyePos.getX(), 0, eyePos.getZ());
        plane_normal.normalise();

        float t = -Vector3f.dot(plane_normal, eyePos)/Vector3f.dot(plane_normal, dir);
        Vector3f.linear(eyePos, dir, t, emiter_pos);
        Vector3f.cross(Vector3f.Y_AXIS, plane_normal, up);
    }

    void draw(FrameData frameData){
        // draw the emitter
        render_program.enable();
        render_program.applyPointSize(50);
        render_program.applyMVP(frameData.viewProj);
        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D, emitter_sprite);
        FloatBuffer buf = CacheBuffer.getCachedFloatBuffer(4);
        emiter_pos.store(buf);
        buf.flip();

        gl.glBindVertexArray(0);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
        gl.glVertexAttribPointer(0, 3, GLenum.GL_FLOAT, false, 0, buf);
        gl.glEnableVertexAttribArray(0);
        context.enablePointSprite();
        gl.glDrawArrays(GLenum.GL_POINTS, 0, 1);
        gl.glDisableVertexAttribArray(0);
        context.disablePointSprite();

        if(Flight404.printOnce){
            render_program.setName("Emitter Render");
            render_program.printPrograminfo();
        }

        // draw the reflect.
        reflect_program.enable();
        reflect_program.applyMVP(frameData.viewProj);
//        reflect_program.applyRight(right);
//        reflect_program.applyUp(up);
        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D, reflect_sprite);

        buf = CacheBuffer.getCachedFloatBuffer(4);
        emiter_pos.store(buf);
        buf.put(emiter_diam * 0.3f).flip();
        gl.glVertexAttribPointer(0, 4, GLenum.GL_FLOAT, false, 0, buf);
//        buf.position(3);
//        gl.glVertexAttribPointer(1, 1, GLenum.GL_FLOAT, false, 16, buf);
        gl.glEnableVertexAttribArray(0);
//        gl.glEnableVertexAttribArray(1);

        gl.glDrawArrays(GLenum.GL_POINTS, 0, 1);
        gl.glDisableVertexAttribArray(0);
        gl.glDisableVertexAttribArray(1);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D, 0);

        if(Flight404.printOnce){
            reflect_program.setName("Emitter Reflect Render");
            reflect_program.printPrograminfo();
        }
    }

    private Vector3f grap(FrameData frameData, float screenX, float screenY, Vector3f dir){
        if(frameData.screenWidth <= 0 || frameData.screenHeight <= 0)
            return dir;

        final Matrix4f projection =frameData.proj;
        final Matrix4f view = frameData.view;
        float vx = (float) ((+2.0*screenX/frameData.screenWidth  - 1.0)/ projection.m00);
        float vy = (float) ((-2.0*screenY/frameData.screenHeight + 1.0)/ projection.m11);

        final Matrix4f tempMat = CacheBuffer.getCachedMatrix();
        Matrix4f.invertRigid(view, tempMat);

        dir.set(vx, vy, -1f);
        Matrix4f.transformNormal(tempMat, dir, dir);
        CacheBuffer.free(tempMat);

        return dir;
    }
}
