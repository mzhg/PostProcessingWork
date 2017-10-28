package jet.opengl.demos.gpupro.fire;

import com.nvidia.developer.opengl.app.NvCameraMotionType;
import com.nvidia.developer.opengl.app.NvSampleApp;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2017/10/28.
 */

public class FireDemo extends NvSampleApp {
    static final String file_dir = "gpupro/Fire/models/";
    // angle parameter for gluPerspective applied during the scene rendering:
    float vertical_viewing_angle = 45.0f;

    // argument for object "rotator";
    // it defines step of angle for rotation performed with a mouse while
    // its left or right buttons are pressed
    float modelRotationStep = 0.05f;

    // time interval between iterations through the fire
    // sequence when keys "a" or "z" are pressed:
    float framing_interval = 0.15f;

    // length of the interval for collecting fps statistics
    float fps_display_interval = 0.05f;

    // collision distance between the camera and the burner
    float contact_distance = 2.0f;

    // radius of S1 (see Fig. 1 in the VFA document)
    float radius = 1.0f;

    // starting parameters for gluLookAt applied during the scene rendering
    final Vector3f starting_eye = new Vector3f(0, 1, 0);
    final Vector3f starting_center = new Vector3f(0, 1, -1);
    final Vector3f starting_up = new Vector3f(0, 1, 0);

    // position of the fire within the scene
    final Vector3f r_f = new Vector3f(0, 0.871f, -5);
    final Vector3f h_f = new Vector3f(0.0f, 1.2f, -5.0f);

    final HeatHazeOGL my_heathaze = new HeatHazeOGL();
    final AftereffectBuffers my_fxbuffers = new AftereffectBuffers();

    float dx,dz;

    // variables which keep the state of buttons:
    boolean upPressed,downPressed,leftPressed,rightPressed,a_pressed,z_pressed;
    boolean s_pressed = false;
    boolean x_pressed = false;
    boolean mouseLeft, mouseRight;

    boolean navigation;   // navigation mode on/off (see "FireDemo-README" file)
    boolean run = true;  // automatic fire animation on/off

    // definiton of the object responsible for production of the texture T1 (see VFA document)
    // and placing it in the scene:
    final VolumeRenderer my_renderer = new VolumeRenderer();

    // the object below loads the fire sequence and holds it:
    final EffectSequence my_effectSequence = new EffectSequence();

    // iterators utilized to proceed through the frames of the fire sequence:
    final EffectIter my_effIter1 = new EffectIter(my_effectSequence);
    final EffectIter my_effIter2 = new EffectIter(my_effectSequence);

    // config reads FireDemo configuration parameters (from "Configuration" file)
    // and holds them:
    final Configuration config = new Configuration();

    // manager which controls fps calculation and display:
    final FramerateDisplayer my_framerate = new FramerateDisplayer();

    // rotator performs rotations made with a mouse when its
    // left or right buttons are pressed
    final ObjectRotation rotator = new ObjectRotation(0);
    final Texture2D[] LandscapeTextures = new Texture2D[6];

    Runnable firePit;        // reference to the display list of burner used to produce depth mask (see VFA document)
    Runnable firePit2;       // reference to the display list of the original burner
    Runnable firePitWall;    // reference to the display list of O1 (see VFA document)

    // definition of light illuminating the burner
    float ambient[] = {0.2f, 0.2f, 0.2f, 1};
    float diffuse[] = {0.8f, 0.8f, 0.8f,1};
    float diff_pos[] = {-1,1,5,1};

    float[] identityMatrix = {
            1, 0, 0 ,0,
            0, 1, 0, 0,
            0, 0, 1, 0,
            0, 0, 0, 1
    };

    private final Matrix4f m_view = new Matrix4f();
    private final Matrix4f m_proj = new Matrix4f();

    private GLFuncProvider gl;

    @Override
    protected void initRendering() {
        gl = GLFuncProviderFactory.getGLFuncProvider();
        // loading configuration data for FireDemo
        if( !config.load(file_dir + "Configuration") ){
            System.err.println("class Configuration: Loading from the file: \"Configuration\" FAILED !!!");
        }

        /*IntBuffer textures = GLUtil.getCachedIntBuffer(6);
        GL11.glGenTextures(textures);
        textures.get(LandscapeTextures);*/
        // loading textures
        /*if(
                !createTexture(LandscapeTextures[0],file_dir + "Terrain.tga",GL11.GL_REPEAT)
                        || !createTexture(LandscapeTextures[1],file_dir + "Top.tga",GL12.GL_CLAMP_TO_EDGE)
                        || !createTexture(LandscapeTextures[2],file_dir + "Back.tga",GL12.GL_CLAMP_TO_EDGE)
                        || !createTexture(LandscapeTextures[3],file_dir + "Front.tga",GL12.GL_CLAMP_TO_EDGE)
                        || !createTexture(LandscapeTextures[4],file_dir + "Left.tga",GL12.GL_CLAMP_TO_EDGE)
                        || !createTexture(LandscapeTextures[5],file_dir + "Right.tga",GL12.GL_CLAMP_TO_EDGE)
                ) exit();*/
        LandscapeTextures[0] = createTexture(file_dir + "Terrain.tga",GLenum.GL_REPEAT);
        LandscapeTextures[1] = createTexture(file_dir + "Top.tga",GLenum.GL_CLAMP_TO_EDGE);
        LandscapeTextures[2] = createTexture(file_dir + "Back.tga",GLenum.GL_CLAMP_TO_EDGE);
        LandscapeTextures[3] = createTexture(file_dir + "Front.tga",GLenum.GL_CLAMP_TO_EDGE);
        LandscapeTextures[4] = createTexture(file_dir + "Left.tga",GLenum.GL_CLAMP_TO_EDGE);
        LandscapeTextures[5] = createTexture(file_dir + "Right.tga",GLenum.GL_CLAMP_TO_EDGE);

		/**/
        // creation of a light source to illuminate the burner
        /*gl.glEnable(GL11.GL_COLOR_MATERIAL);
        gl.glLightModel(GL11.GL_LIGHT_MODEL_AMBIENT,wrap4(ambient));
        gl.glEnable(GL11.GL_LIGHT0);
        gl.glLight(GL11.GL_LIGHT0, GL11.GL_DIFFUSE,wrap4(diffuse));
        gl.glLight(GL11.GL_LIGHT0, GL11.GL_POSITION,wrap4(diff_pos));*/

        // creation of needed burner models:
        firePit = GL11.glGenLists(1);
        firePit2 = GL11.glGenLists(1);
        firePitWall = GL11.glGenLists(1);
        createFirePit(firePit, 0.5f, 1.19f, 0.4f, 0.1f, 0.4f, 0.2f, 0.53f, -0.87f, 0.0f, 0.0f, 1.0f, 1.0f, false);
        createFirePit(firePit2, 0.5f, 1.19f, 0.42f, 0.105f, 0.4f, 0.2f, 0.53f, -0.87f, 0.0f, 0.0f, 1.0f, 1.0f, false);
        createFirePit(firePitWall, 0.5f, 1.19f, 0.42f, 0.105f, 0.4f, 0.2f, 0.53f, -0.87f, 0.0f, 0.0f, 0.0f, 0.0f, true);
		/**/
        // initializations:

        rotator.setRotationSpeed( modelRotationStep );

        /*camera.setCameraSpeed( config.cameraSpeed );
        camera.positionCamera( starting_eye.x, starting_eye.y, starting_eye.z,
                starting_center.x, starting_center.y, starting_center.z,
                starting_up.x, starting_up.y, starting_up.z);*/
        m_transformer.setTranslation(-starting_eye.x, -starting_eye.y, -starting_eye.z);
        m_transformer.setMotionMode(NvCameraMotionType.FIRST_PERSON);

        Vector3f camera_position = new Vector3f();
        Vector3f camera_lookat = new Vector3f();
        Vector3f camera_up = new Vector3f();

        m_transformer.getModelViewMat(m_view);
        Matrix4f.decompseRigidMatrix(m_view, camera_position, null, camera_up, camera_lookat);
        camera_lookat.scale(-1);

        final int window_size_x = getGLContext().width();
        final int window_size_y = getGLContext().height();

//        Vector3f forward = Vector3f.sub(camera.view, camera.position, null);
        boolean init = my_effectSequence.init(file_dir + "Data")
                &&	my_effIter1.init( config.sequence1shift,
                (int)config.angIncrSeq1,
                (int)config.initAngSeq1 )
                &&	my_effIter2.init( config.sequence2shift,
                (int)config.angIncrSeq2,
                (int)config.initAngSeq2 )
                && 	my_renderer.init( my_effectSequence.get_layers_no(),
                my_effectSequence.get_slices_no_alongX(),
                my_effectSequence.getFirstNonEmptySlice(),
                my_effectSequence.getLastNonEmptySlice(),
                radius, window_size_y, vertical_viewing_angle, r_f,
                camera_position, camera_lookat, camera_up );
//        if(!init) exit();

        my_fxbuffers.init( window_size_x, window_size_y, 100, 76, window_size_x/2, window_size_y/2 );

        my_heathaze.init(
                h_f, camera_position, camera_lookat, camera_up,
                config.heatHazeSpeed,
                config.timeGapClouds,
                config.halfSizeInit,
                config.halfSizeAmpl,
                config.initHorizSpiralRadius,
                config.spiralParameter,
                config.spiralSpeedAmpl,
                config.vertSpeedInit,
                config.vertSpeedAmpl,
                config.turbulenceSpeed,
                config.lifetimeAmpl,
                100,
                15,
                config.maxDistort,
                4.0f,
                0.12f,
                8.0f,
                config.rescaleDistTex,
                my_fxbuffers.getTextureNo(0),
                my_fxbuffers.getTextureNo(2),
                my_fxbuffers.getTextureNo(1));

//        my_framerate.init(fps_display_interval,Glut.BITMAP_HELVETICA_18,1.0f,0.0f,0.0f,0.05f,0.95f);
    }

    Texture2D createTexture(String file, int type){
        /*Pixels tgaImage = Glut.loadTGA(file);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
        tgaImage.uploadTexture2D(true);
        */

        try {
            Texture2D texture = TextureUtils.createTexture2DFromFile(file, true);
            gl.glBindTexture(texture.getTarget(), texture.getTexture());
            gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_LINEAR_MIPMAP_LINEAR);
            gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_LINEAR);
            gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_WRAP_S, type);
            gl.glTexParameteri(GLenum.GL_TEXTURE_2D, GLenum.GL_TEXTURE_WRAP_T, type);

            return texture;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    void createSkyBox(float x, float y, float z, float width, float height, float length,
                      int back_id, int front_id, int bottom_id, int top_id, int left_id, int right_id){
//        gl.glEnable(GL11.GL_TEXTURE_2D);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D, back_id);

        x = x - width  / 2;
        y = y - height / 2;
        z = z - length / 2;

        final int stride = 5 * 4;
        final int position_loc = 0, pos_offset = 2;
        final int texture0_loc = 1, tex0_offset = 0;
        gl.glEnableVertexAttribArray(position_loc);
        gl.glEnableVertexAttribArray(texture0_loc);

        /*GL11.glBegin(GL11.GL_QUADS);
        {
            GL11.glTexCoord2f(0.0f, 0.0f); GL11.glVertex3f(x,            y,            z);
            GL11.glTexCoord2f(0.0f, 1.0f); GL11.glVertex3f(x,            y + height, z);
            GL11.glTexCoord2f(1.0f, 1.0f); GL11.glVertex3f(x + width, y + height, z);
            GL11.glTexCoord2f(1.0f, 0.0f); GL11.glVertex3f(x + width, y,            z);
        }
        GL11.glEnd();*/

        {
            FloatBuffer vertex_buffer = CacheBuffer.getCachedFloatBuffer(5 * 4);
            vertex_buffer.put(0.0f).put(0.0f);
            vertex_buffer.put(x).put(y).put(z);

            vertex_buffer.put(0.0f).put(1.0f);
            vertex_buffer.put(x).put(y+height).put(z);

            vertex_buffer.put(1.0f).put(1.0f);
            vertex_buffer.put(x + width).put(y + height).put(z);

            vertex_buffer.put(1.0f).put(0.0f);
            vertex_buffer.put(x + width).put(y).put(z);
            vertex_buffer.flip();

            gl.glVertexAttribPointer(position_loc, 3, GLenum.GL_FLOAT, false, stride, vertex_buffer.position(pos_offset));
            gl.glVertexAttribPointer(texture0_loc, 2, GLenum.GL_FLOAT, false, stride, vertex_buffer.position(tex0_offset));

            gl.glDrawArrays(GLenum.GL_TRIANGLE_FAN, 0, 4);
        }



        gl.glBindTexture(GLenum.GL_TEXTURE_2D, front_id);

        /*GL11.glBegin(GL11.GL_QUADS);
        {
            GL11.glTexCoord2f(1.0f, 0.0f); GL11.glVertex3f(x,            y,            z + length);
            GL11.glTexCoord2f(1.0f, 1.0f); GL11.glVertex3f(x,            y + height, z + length);
            GL11.glTexCoord2f(0.0f, 1.0f); GL11.glVertex3f(x + width, y + height, z + length);
            GL11.glTexCoord2f(0.0f, 0.0f); GL11.glVertex3f(x + width, y,            z + length);
        }
        GL11.glEnd();*/
        {
            FloatBuffer vertex_buffer = CacheBuffer.getCachedFloatBuffer(5 * 4);
            vertex_buffer.put(1.0f).put(0.0f);
            vertex_buffer.put(x).put(y).put(z + length);

            vertex_buffer.put(1.0f).put(1.0f);
            vertex_buffer.put(x).put(y+height).put(z + length);

            vertex_buffer.put(0.0f).put(1.0f);
            vertex_buffer.put(x + width).put(y + height).put(z + length);

            vertex_buffer.put(0.0f).put(0.0f);
            vertex_buffer.put(x + width).put(y).put(z + length);
            vertex_buffer.flip();

            gl.glVertexAttribPointer(position_loc, 3, GLenum.GL_FLOAT, false, stride, vertex_buffer.position(pos_offset));
            gl.glVertexAttribPointer(texture0_loc, 2, GLenum.GL_FLOAT, false, stride, vertex_buffer.position(tex0_offset));


            gl.glDrawArrays(GLenum.GL_TRIANGLE_FAN, 0, 4);
        }

        gl.glBindTexture(GLenum.GL_TEXTURE_2D, top_id);

        /*GL11.glBegin(GL11.GL_QUADS);
        {
            GL11.glTexCoord2f(0.0f, 1.0f); GL11.glVertex3f(x,            y + height,    z);
            GL11.glTexCoord2f(1.0f, 1.0f); GL11.glVertex3f(x,            y + height,    z + length);
            GL11.glTexCoord2f(1.0f, 0.0f); GL11.glVertex3f(x + width, y + height, z + length);
            GL11.glTexCoord2f(0.0f, 0.0f); GL11.glVertex3f(x + width, y + height, z);
        }
        GL11.glEnd();*/
        {
            FloatBuffer vertex_buffer = CacheBuffer.getCachedFloatBuffer(5 * 4);
            vertex_buffer.put(0.0f).put(1.0f);
            vertex_buffer.put(x).put(y + height).put(z);

            vertex_buffer.put(1.0f).put(1.0f);
            vertex_buffer.put(x).put(y+height).put(z + length);

            vertex_buffer.put(1.0f).put(0.0f);
            vertex_buffer.put(x + width).put(y + height).put(z + length);

            vertex_buffer.put(0.0f).put(0.0f);
            vertex_buffer.put(x + width).put(y + height).put(z);
            vertex_buffer.flip();

            gl.glVertexAttribPointer(position_loc, 3, GLenum.GL_FLOAT, false, stride, vertex_buffer.position(pos_offset));
            gl.glVertexAttribPointer(texture0_loc, 2, GLenum.GL_FLOAT, false, stride, vertex_buffer.position(tex0_offset));


            gl.glDrawArrays(GLenum.GL_TRIANGLE_FAN, 0, 4);
        }

        gl.glBindTexture(GLenum.GL_TEXTURE_2D, left_id);

        /*GL11.glBegin(GL11.GL_QUADS);
        {
            GL11.glTexCoord2f(1.0f, 0.0f); GL11.glVertex3f(x,            y,            z);
            GL11.glTexCoord2f(0.0f, 0.0f); GL11.glVertex3f(x,            y,            z + length);
            GL11.glTexCoord2f(0.0f, 1.0f); GL11.glVertex3f(x,            y + height,    z + length);
            GL11.glTexCoord2f(1.0f, 1.0f); GL11.glVertex3f(x,            y + height,    z);
        }
        GL11.glEnd();*/
        {
            FloatBuffer vertex_buffer = CacheBuffer.getCachedFloatBuffer(5 * 4);
            vertex_buffer.put(1.0f).put(0.0f);
            vertex_buffer.put(x).put(y).put(z);

            vertex_buffer.put(0.0f).put(0.0f);
            vertex_buffer.put(x).put(y).put(z + length);

            vertex_buffer.put(0.0f).put(1.0f);
            vertex_buffer.put(x).put(y + height).put(z + length);

            vertex_buffer.put(1.0f).put(1.0f);
            vertex_buffer.put(x).put(y + height).put(z);
            vertex_buffer.flip();

            gl.glVertexAttribPointer(position_loc, 3, GLenum.GL_FLOAT, false, stride, vertex_buffer.position(pos_offset));
            gl.glVertexAttribPointer(texture0_loc, 2, GLenum.GL_FLOAT, false, stride, vertex_buffer.position(tex0_offset));

            gl.glDrawArrays(GLenum.GL_TRIANGLE_FAN, 0, 4);
        }

        gl.glBindTexture(GLenum.GL_TEXTURE_2D, right_id);

        /*GL11.glBegin(GL11.GL_QUADS);
        {
            GL11.glTexCoord2f(0.0f, 0.0f); GL11.glVertex3f(x + width, y,            z);
            GL11.glTexCoord2f(1.0f, 0.0f); GL11.glVertex3f(x + width, y,            z + length);
            GL11.glTexCoord2f(1.0f, 1.0f); GL11.glVertex3f(x + width, y + height,    z + length);
            GL11.glTexCoord2f(0.0f, 1.0f); GL11.glVertex3f(x + width, y + height,    z);
        }
        GL11.glEnd();*/
        {
            FloatBuffer vertex_buffer = CacheBuffer.getCachedFloatBuffer(5 * 4);
            vertex_buffer.put(0.0f).put(0.0f);
            vertex_buffer.put(x + width).put(y).put(z);

            vertex_buffer.put(1.0f).put(0.0f);
            vertex_buffer.put(x + width).put(y).put(z + length);

            vertex_buffer.put(1.0f).put(1.0f);
            vertex_buffer.put(x + width).put(y + height).put(z + length);

            vertex_buffer.put(0.0f).put(1.0f);
            vertex_buffer.put(x + width).put(y + height).put(z);
            vertex_buffer.flip();

            gl.glVertexAttribPointer(position_loc, 3, GLenum.GL_FLOAT, false, stride, vertex_buffer.position(pos_offset));
            gl.glVertexAttribPointer(texture0_loc, 2, GLenum.GL_FLOAT, false, stride, vertex_buffer.position(tex0_offset));

            gl.glDrawArrays(GLenum.GL_TRIANGLE_FAN, 0, 4);
        }

//        GL11.glDisable(GL11.GL_TEXTURE_2D);
        gl.glDisableVertexAttribArray(position_loc);
        gl.glDisableVertexAttribArray(tex0_offset);
    }
}
