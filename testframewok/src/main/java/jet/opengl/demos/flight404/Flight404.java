package jet.opengl.demos.flight404;

import com.nvidia.developer.opengl.app.NvInputDeviceType;
import com.nvidia.developer.opengl.app.NvInputTransformer;
import com.nvidia.developer.opengl.app.NvPointerActionType;
import com.nvidia.developer.opengl.app.NvPointerEvent;
import com.nvidia.developer.opengl.app.NvSampleApp;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import java.io.IOException;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.util.FileLoader;
import jet.opengl.postprocessing.util.FileUtils;
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
    static boolean printOnce = true;

    private GLFuncProvider gl;
    Emitter emitter;
    ParticleSystem particleSystem;
    private boolean point_sprite_switcher;

    private final FrameData frameData = new FrameData();
    private boolean mRightButtonDown = false;

    @Override
    protected void initRendering() {
        gl = GLFuncProviderFactory.getGLFuncProvider();
//        getGLContext().setSwapInterval(0);

        /*String filename = "E:\\workspace\\VSProjects\\GraphicsWork\\Media\\DefaultVS.vert";
        CharSequence source = DebugTools.loadText(filename);
        try {
            FileLoader old = FileUtils.g_IntenalFileLoader;
            FileUtils.setIntenalFileLoader(FileLoader.g_DefaultFileLoader);
            GLSLProgram.createShaderProgramFromFile(filename, ShaderType.VERTEX);
            FileUtils.setIntenalFileLoader(old);
        } catch (IOException e) {
            e.printStackTrace();
        }*/

        try {
            FileLoader old = FileUtils.g_IntenalFileLoader;
            FileUtils.setIntenalFileLoader(FileLoader.g_DefaultFileLoader);
            String path = "E:\\workspace\\VSProjects\\GraphicsWork\\Media\\Effects\\";
            GLSLProgram.createFromFiles(path + "SkyBoxVS.vert", path + "SkyBoxPS.frag");
            FileUtils.setIntenalFileLoader(old);
        } catch (IOException e) {
            e.printStackTrace();
        }

        float mTheta =Numeric.PI/4;
        float mPhi = Numeric.PI/3;
        float mRadius = (float)Math.sqrt(100 * 100 + 1500 * 1500);
        float x = (float) (mRadius*Math.sin(mPhi)*Math.cos(mTheta));
        float z = (float) (mRadius*Math.sin(mPhi)*Math.sin(mTheta));
        float y = (float) (mRadius*Math.cos(mPhi));

        initCamera(0, new Vector3f(x,y,z), Vector3f.ZERO);

        emitter = new Emitter(this);
        particleSystem = new ParticleSystem(emitter);

        frameData.mouseX = getGLContext().width()/2;
        frameData.mouseY = getGLContext().height() * 3/4;
    }

    public void initCamera(int index, ReadableVector3f eye, ReadableVector3f at) {
        // Construct the look matrix
//	    	    Matrix4f look;
//	    	    lookAt(look, eye, at, nv.vec3f(0.0f, 1.0f, 0.0f));
        Matrix4f look = Matrix4f.lookAt(eye, at, Vector3f.Y_AXIS, null);

        // Decompose the look matrix to get the yaw and pitch.
        float pitch = (float) Math.atan2(-look.m21, /*_32*/ look.m22/*_33*/);
        float yaw = (float) Math.atan2(look.m20/*_31*/, Vector2f.length(-look.m21/*_32*/, look.m22/*_33*/));

        // Initialize the camera view.
        NvInputTransformer m_camera = getInputTransformer();
        m_camera.setRotationVec(new Vector3f(pitch, yaw, 0.0f), index);
        m_camera.setTranslationVec(new Vector3f(look.m30/*_41*/, look.m31/*_42*/, look.m32/*_43*/), index);
        m_camera.update(0.0f);
    }

    @Override
    public void display() {
        // update the camera data
        m_transformer.getModelViewMat(frameData.view);
        Matrix4f.mul(frameData.proj, frameData.view, frameData.viewProj);
        Matrix4f.decompseRigidMatrix(frameData.view, frameData.cameraPos, null, null);

//        enablePointSprite();
        gl.glClearColor(0,0,0,0);
        gl.glClearDepthf(1.0f);
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT|GLenum.GL_DEPTH_BUFFER_BIT);
        gl.glEnable(GLenum.GL_BLEND);
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

//        disablePointSprite();

        printOnce = false;
    }

    @Override
    public void reshape(int width, int height) {
        if(width <= 0 || height <= 0)
            return;

        gl.glViewport(0, 0, width, height);
        Matrix4f.perspective(Numeric.PI*2/3, (float)width/height, 0.1f, 3000f, frameData.proj);
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
                System.out.println("Right Button touched down，mouse = (" + frameData.mouseX + ", " + frameData.mouseY + ')');
            }else if(action == NvPointerActionType.UP){
                mRightButtonDown = false;
                System.out.println("Right Button released");
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
