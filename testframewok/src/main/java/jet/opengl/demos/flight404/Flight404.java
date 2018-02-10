package jet.opengl.demos.flight404;

import com.nvidia.developer.opengl.app.NvInputDeviceType;
import com.nvidia.developer.opengl.app.NvPointerActionType;
import com.nvidia.developer.opengl.app.NvPointerEvent;
import com.nvidia.developer.opengl.app.NvSampleApp;

import org.lwjgl.util.vector.Matrix4f;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.Numeric;

/**
 * Created by mazhen'gui on 2018/2/10.
 */

public final class Flight404 extends NvSampleApp {
    private static final int MAX_PARTICLE_COUNT = 100_000;
    private static final int MAX_EMITTER_COUNT = 16;
    private static final int MAX_PARTICLE_TAIL_COUNT = 4;

    private static final int TYPE_BORN = 0;      // particles born
    private static final int TYPE_UPDATE = 1;    // update the particles
    private static final int TYPE_NEBULA = 2;    // update the nebulas
    private static final int TYPE_NEBORN = 3;    // born the emitter nebulas

    private static boolean debug = true;

    private GLFuncProvider gl;
    Emitter emitter;
    ParticleSystem particleSystem;
    boolean point_sprite_switcher;

    private final FrameData frameData = new FrameData();
    private boolean mRightButtonDown = false;

    // the properties of particle system


    @Override
    protected void initRendering() {
        /*camera = new CircleformCamera3D(nvApp.getWindow());
        float distance = (float)Math.sqrt(100 * 100 + 1500 * 1500);
        camera.setCameraRange(0.1f, 10000);
        camera.setCamera(Numeric.PI/4, Numeric.PI/3, distance);
        camera.update();
        nvApp.registerGLEventListener(camera);
        nvApp.registerGLFWListener(camera);*/
//	    nvApp.setSwapInterval(1);

        gl = GLFuncProviderFactory.getGLFuncProvider();
        getGLContext().setSwapInterval(0);

        float mTheta =Numeric.PI/4;
        float mPhi = Numeric.PI/3;
        float mRadius = (float)Math.sqrt(100 * 100 + 1500 * 1500);
        float x = (float) (mRadius*Math.sin(mPhi)*Math.cos(mTheta));
        float z = (float) (mRadius*Math.sin(mPhi)*Math.sin(mTheta));
        float y = (float) (mRadius*Math.cos(mPhi));

        m_transformer.setTranslation(x,y,z);

        emitter = new Emitter(m_transformer);
        particleSystem = new ParticleSystem(emitter);
    }

    @Override
    public void display() {
        // update the camera data
        m_transformer.getModelViewMat(frameData.view);
        Matrix4f.mul(frameData.proj, frameData.view, frameData.viewProj);
        Matrix4f.decompseRigidMatrix(frameData.view, frameData.cameraPos, null, null);

        enablePointSprite();
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT|GLenum.GL_DEPTH_BUFFER_BIT);
        gl.glEnable(GLenum.GL_BLEND);
//		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        gl.glBlendFuncSeparate(GLenum.GL_SRC_ALPHA, GLenum.GL_ONE, GLenum.GL_ZERO, GLenum.GL_ZERO);

        gl.glEnable(GLenum.GL_DEPTH_TEST);
        gl.glDepthFunc(GLenum.GL_LEQUAL);
        gl.glDepthMask(false);
        gl.glDisable(GLenum.GL_CULL_FACE);

        particleSystem.update(frameData, mRightButtonDown, getFrameDeltaTime());
        particleSystem.draw(frameData);

        emitter.update(frameData, getFrameDeltaTime());
        emitter.draw(frameData);

        gl.glDepthMask(true);

        disablePointSprite();
    }

    @Override
    public void reshape(int width, int height) {
        if(width <= 0 || height <= 0)
            return;

        gl.glViewport(0, 0, width, height);
        Matrix4f.perspective(Numeric.PI*2/3, (float)width/height, 0.1f, 5000f, frameData.proj);
        frameData.screenWidth = width;
        frameData.screenHeight = height;
    }

    @Override
    public boolean handlePointerInput(NvInputDeviceType device, int action, int modifiers, int count, NvPointerEvent[] points) {
        frameData.mouseX = points[0].m_x;
        frameData.mouseY = points[0].m_y;

        if((points[0].m_id & 2) != 0) {
            if (action == NvPointerActionType.DOWN ) {
                mRightButtonDown = true;
            }else if(action == NvPointerActionType.UP){
                mRightButtonDown = false;
            }
        }

        return super.handlePointerInput(device, action, modifiers, count, points);
    }

    void enablePointSprite(){
        if(!point_sprite_switcher){
            gl.glEnable(GLenum.GL_POINT_SPRITE);
            gl.glEnable(GLenum.GL_PROGRAM_POINT_SIZE);
            point_sprite_switcher = true;
        }
    }

    void disablePointSprite(){
        if(point_sprite_switcher){
            gl.glDisable(GLenum.GL_POINT_SPRITE);
            gl.glDisable(GLenum.GL_PROGRAM_POINT_SIZE);
            point_sprite_switcher = false;
        }
    }
}
