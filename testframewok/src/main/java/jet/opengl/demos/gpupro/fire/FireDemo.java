package jet.opengl.demos.gpupro.fire;

import com.nvidia.developer.opengl.app.NvCameraMotionType;
import com.nvidia.developer.opengl.app.NvEGLConfiguration;
import com.nvidia.developer.opengl.app.NvKey;
import com.nvidia.developer.opengl.app.NvKeyActionType;
import com.nvidia.developer.opengl.app.NvSampleApp;
import com.nvidia.developer.opengl.models.DrawMode;
import com.nvidia.developer.opengl.models.GLVAO;
import com.nvidia.developer.opengl.models.QuadricBuilder;
import com.nvidia.developer.opengl.models.QuadricCylinder;
import com.nvidia.developer.opengl.models.QuadricDisk;
import com.nvidia.developer.opengl.models.QuadricMesh;
import com.nvidia.developer.opengl.models.QuadricSphere;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.HashMap;

import jet.opengl.demos.nvidia.sparkles.SimpleLightProgram;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2017/10/28.
 */

public final class FireDemo extends NvSampleApp {
    static final int POSITION_LOC = 0;
    static final int NORMAL_LOC = 2;
    static final int TEXTURE_LOC = 1;

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
    private final HashMap<Object, GLVAO> m_ModelCaches = new HashMap<>();

    float[] identityMatrix = {
            1, 0, 0 ,0,
            0, 1, 0, 0,
            0, 0, 1, 0,
            0, 0, 0, 1
    };

    private final Matrix4f m_view = new Matrix4f();
    private final Matrix4f m_proj = new Matrix4f();
    private final ModelViewStack m_mat_stack = new ModelViewStack();
    private SimpleLightProgram m_lightProgram;
    private GLSLProgram m_textureProgram;

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

        // definition of light illuminating the burner
        final Vector3f ambient = new Vector3f(0.2f, 0.2f, 0.2f);
        final Vector3f diffuse = new Vector3f(0.8f, 0.8f, 0.8f);
        final Vector4f diff_pos = new Vector4f(-1,1,5,1);
        final Vector3f white = new Vector3f(1,1,1);
        try{
            m_lightProgram = new SimpleLightProgram(true);
            m_lightProgram.enable();
            m_lightProgram.setLightAmbient(ambient);
            m_lightProgram.setLightDiffuse(diffuse);
            m_lightProgram.setLightPos(diff_pos);
            m_lightProgram.setLightSpecular(white);

            m_textureProgram = GLSLProgram.createFromFiles("gpupro/Fire/shaders/SimpleRectTextureVS.vert", "gpupro/Fire/shaders/SimpleTexturePS.frag");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // creation of needed burner models:
//        firePit = GL11.glGenLists(1);
//        firePit2 = GL11.glGenLists(1);
//        firePitWall = GL11.glGenLists(1);
        firePit = createFirePit(/*firePit,*/ 0.5f, 1.19f, 0.4f, 0.1f, 0.4f, 0.2f, 0.53f, -0.87f, 0.0f, 0.0f, 1.0f, 1.0f, false);
        firePit2= createFirePit(/*firePit2,*/ 0.5f, 1.19f, 0.42f, 0.105f, 0.4f, 0.2f, 0.53f, -0.87f, 0.0f, 0.0f, 1.0f, 1.0f, false);
        firePitWall = createFirePit(/*firePitWall,*/ 0.5f, 1.19f, 0.42f, 0.105f, 0.4f, 0.2f, 0.53f, -0.87f, 0.0f, 0.0f, 0.0f, 0.0f, true);
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
        GLCheck.checkError();
    }

    @Override
    public void configurationCallback(NvEGLConfiguration config) {
        config.stencilBits = 8;  // enable the stencil test
    }

    private void setColorMaterial(float r, float g, float b, float shiness){
        Vector4f material = new Vector4f(r,g,b,shiness);
        m_lightProgram.setMaterialAmbient(material);
        m_lightProgram.setMaterialDiffuse(material);
        m_lightProgram.setMaterialSpecular(material);
    }

    private float rest =0.0f;
    @Override
    public void display() {
        vol_rend();
//        float interval = getRunningTime() - lastTime;
        float interval = getFrameDeltaTime();
        if( a_pressed || z_pressed ){
            if( interval > framing_interval ){
//				globalTime.SetReference();
//                lastTime = getRunningTime();
                rest = 0.0f;
                if( a_pressed ){
                    my_effIter1.incr();
                    my_effIter2.incr();
                    my_heathaze.proceed(framing_interval);
                }

                if( z_pressed ){
                    my_effIter1.decr();
                    my_effIter2.decr();
                }
            }
        } else {
            int fire_frames_passed = (int) ((interval+rest)/config.mainTimetick);
            if( run && fire_frames_passed != 0){
                rest = interval+rest-fire_frames_passed*config.mainTimetick;
//				globalTime.SetReference();
//                lastTime = getRunningTime();
                while(fire_frames_passed != 0){
                    my_effIter1.incr();
                    my_effIter2.incr();
                    --fire_frames_passed;
                }
                my_heathaze.proceed(interval);
            }
        }

        _draw();

//        my_framerate.anotherFrameExecuted(getFPS());

        {
            // TODO update the render and heathaze...
//            Vector3f forward = Vector3f.sub(camera.view, camera.position, null);
//            // update of my_renderer internal state
//            my_renderer.update( r_f, camera.position, forward, camera.up );
//            my_heathaze.update( h_f, camera.position, forward, camera.up);
        }
    }

    void _draw(){
//		updateCamera();
        create_heat();
        new_scene();
        minimize_scene();

        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER_EXT,0);
        gl.glViewport(0,0,getGLContext().width(),getGLContext().height());

        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT | GLenum.GL_DEPTH_BUFFER_BIT);
        gl.glDisable(GLenum.GL_DEPTH_TEST);

        /*GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GLU.gluOrtho2D( -1.0f, 1.0f, -1.0f, 1.0f);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        {
            GL11.glBegin(GL11.GL_QUADS);
            {
                GL11.glTexCoord2f(0,0);
                GL11.glVertex3f(-1,-1, 0.0f );

                GL11.glTexCoord2f(1,0);
                GL11.glVertex3f(1,-1, 0.0f );

                GL11.glTexCoord2f(1,1);
                GL11.glVertex3f(1,1, 0.0f );

                GL11.glTexCoord2f(0,1);
                GL11.glVertex3f(-1,1, 0.0f );
            }
            GL11.glEnd();
        }*/
        my_heathaze.attachHeatHazeObjects();
        {
            FloatBuffer vertex_buffer = CacheBuffer.getCachedFloatBuffer(4 * 4);
            vertex_buffer.put(0.0f).put(0.0f);
            vertex_buffer.put(-1).put(-1);

            vertex_buffer.put(1.0f).put(.0f);
            vertex_buffer.put(1).put(-1);

            vertex_buffer.put(1.0f).put(1.0f);
            vertex_buffer.put(1).put(1);

            vertex_buffer.put(.0f).put(1.0f);
            vertex_buffer.put(-1).put(1);
            vertex_buffer.flip();

            final int stride = 4 * 4;
            final int position_loc = 0, pos_offset = 2;
            final int texture0_loc = 1, tex0_offset = 0;
            gl.glEnableVertexAttribArray(position_loc);
            gl.glEnableVertexAttribArray(texture0_loc);

            gl.glVertexAttribPointer(position_loc, 2, GLenum.GL_FLOAT, false, stride, vertex_buffer.position(pos_offset));
            gl.glVertexAttribPointer(texture0_loc, 2, GLenum.GL_FLOAT, false, stride, vertex_buffer.position(tex0_offset));

            gl.glDrawArrays(GLenum.GL_TRIANGLE_FAN, 0, 4);

            gl.glDisableVertexAttribArray(position_loc);
            gl.glDisableVertexAttribArray(texture0_loc);
        }
        my_heathaze.detachHeatHazeObjects();


        /*******************************************************************************/
        /*GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GLU.gluPerspective(vertical_viewing_angle,ratio, .5f ,500.0f);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GLU.gluLookAt(camera.position.x,camera.position.y,camera.position.z,
                camera.view.x, camera.view.y, camera.view.z,
                camera.up.x,camera.up.y,camera.up.z);*/
        Matrix4f.perspective(vertical_viewing_angle,(float)getGLContext().width()/getGLContext().height(), .5f ,500.0f, m_proj);
        m_transformer.getModelViewMat(m_view);
        Matrix4f.mul(m_proj, m_view, m_view);

        gl.glEnable(GLenum.GL_STENCIL_TEST);

        m_lightProgram.enable();
        m_lightProgram.setEnableLighting(false);
        my_renderer.beginStencilMask();
        gl.glColorMask(false, false, false, false);
//        GL11.glPushMatrix();
        m_mat_stack.pushMatrix();
        {
            my_renderer.placeObstacles(m_mat_stack);
//            GL11.glCallList(firePitWall);
            // TODO Don't forget to apply the combimed matrtix to the program.
            m_proj.load(m_view);   // load the viewProj to m_proj
            firePitWall.run();
        }
//        GL11.glPopMatrix();
        m_mat_stack.popMatrix();
        gl.glColorMask(true, true, true, true);
        my_renderer.endStencilMask();

        my_renderer.stencilMaskOn();
        my_renderer.blendInEffectBillboard(m_proj);
//		my_renderer.stencilMaskOff();

        gl.glDisable(GLenum.GL_STENCIL_TEST);
        gl.glDisable(GLenum.GL_DEPTH_TEST);
//        GL11.glPopMatrix();
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

    void vol_rend()
    {
        {
            //  between my_renderer.beginEffectData()
//		and my_renderer.endEffectData() there is code responsible
            //  for generation of the flat 3d texture
            my_renderer.beginEffectData(m_proj);
//        GL11.glPushMatrix();
            m_mat_stack.pushMatrix();
            my_renderer.placeEffectData(m_mat_stack);

            my_effectSequence.enableDatVec();
//        GL11.glPushMatrix();
            m_mat_stack.pushMatrix();

            // extra rotation for sequence 1 - example depicted in Fig.5 (see VFA document)
//        GL11.glRotatef(my_effIter1.getAngle(),0,1,0);
            m_mat_stack.rotate(my_effIter1.getAngle(),0,1,0);

            Matrix4f modelView = m_mat_stack.getTotalMatrix();
            my_renderer.applyEffectData(m_proj, modelView);
            my_effIter1.passData();
//        GL11.glPopMatrix();
            m_mat_stack.popMatrix();

//        GL11.glPushMatrix();
            m_mat_stack.pushMatrix();

            // extra rotation for sequence 2 - example depicted in Fig.5 (see VFA document)
//        GL11.glRotatef(my_effIter2.getAngle(),0,1,0);
            m_mat_stack.rotate(my_effIter2.getAngle(),0,1,0);

            modelView = m_mat_stack.getTotalMatrix();
            my_renderer.applyEffectData(m_proj, modelView);
            my_effIter2.passData();

            //        GL11.glPopMatrix();
            m_mat_stack.popMatrix();
            my_effectSequence.disableDatVec();

//        GL11.glPopMatrix();
            m_mat_stack.popMatrix();
            my_renderer.endEffectData();
        }

        //  between my_renderer.beginObstacles()
//		and my_renderer.endObstacles() there is code responsible
        //  for production of T1 (see VFA)

        {
            gl.glActiveTexture(GLenum.GL_TEXTURE0);
            gl.glBindTexture(GLenum.GL_TEXTURE_2D, 0);
            my_renderer.beginObstacles(m_proj);
//        GL11.glPushMatrix();
            m_mat_stack.pushMatrix();
            my_renderer.placeObstaclesMask(m_mat_stack);

//        GL11.glCallList(firePit);
            m_lightProgram.enable();
            m_lightProgram.setEnableLighting(false);
            firePit.run();

//        GL11.glPopMatrix();
            m_mat_stack.popMatrix();
            my_renderer.endObstacles(m_mat_stack, m_view);
        }

        // blurring of T1 (when needed)
        my_renderer.createEffectBillboard();
    }

    void create_heat(){
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER_EXT,my_fxbuffers.getFramebufferNo(1));

        gl.glViewport(0,0,my_fxbuffers.getDimX(1),my_fxbuffers.getDimY(1));

        gl.glClearColor(0.0f , 0.0f , 0.0f , 1.0f);
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT | GLenum.GL_DEPTH_BUFFER_BIT);

        gl.glEnable(GLenum.GL_DEPTH_TEST);
        gl.glDepthMask(false);

        /*GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GLU.gluPerspective(vertical_viewing_angle,ratio, .5f ,500.0f);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GLU.gluLookAt(camera.position.x,camera.position.y,camera.position.z,
                camera.view.x, camera.view.y, camera.view.z,
                camera.up.x,camera.up.y,camera.up.z);
        GL11.glPushMatrix();*/
        float ratio = (float)getGLContext().width()/getGLContext().height();
        Matrix4f.perspective(vertical_viewing_angle,ratio, .5f ,500.0f, m_proj);
        m_transformer.getModelViewMat(m_view);
        Matrix4f.mul(m_proj, m_view, m_view);
        my_heathaze.placeHeatHaze();
//				glMultMatrixf(rotator.get_rotation());

        my_heathaze.makeHeatHazeDistTex(Matrix4f.IDENTITY, m_view);
//				my_heathaze.makeHeatHazeDistTex(rotator.get_rotation());
//        GL11.glPopMatrix();
//        GL11.glPopMatrix();
        gl.glDepthMask(true);
    }

    void new_scene(){
        // setup before the scene rendering
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER_EXT,my_fxbuffers.getFramebufferNo(0));

        gl.glViewport(0,0,my_fxbuffers.getDimX(0),my_fxbuffers.getDimY(0));
        /*GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GLU.gluPerspective(vertical_viewing_angle,ratio, .5f ,500.0f);
        // the scene rendering:
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GLU.gluLookAt(camera.position.x,camera.position.y,camera.position.z,
                camera.view.x, camera.view.y, camera.view.z,
                camera.up.x,camera.up.y,camera.up.z);*/
        float ratio = (float)getGLContext().width()/getGLContext().height();
        Matrix4f.perspective(vertical_viewing_angle,ratio, .5f ,500.0f, m_proj);
        m_transformer.getModelViewMat(m_view);
        Matrix4f.mul(m_proj, m_view, m_view);

        m_textureProgram.enable();
        int mvpLoc = m_textureProgram.getUniformLocation("g_MVP");
        gl.glUniformMatrix4fv(mvpLoc, false, CacheBuffer.wrap(m_view));

        gl.glEnable(GLenum.GL_DEPTH_TEST);
        gl.glClearColor(1.0f , 1.0f , 1.0f , 1.0f);
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT | GLenum.GL_DEPTH_BUFFER_BIT);

//        GL11.glEnable(GL11.GL_TEXTURE_2D);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D,LandscapeTextures[0].getTexture());

        /*GL11.glBegin(GL11.GL_QUADS);
        {
            GL11.glTexCoord2f(0,0);
            GL11.glVertex3f(-100,0,-100);
            GL11.glTexCoord2f(10,0);
            GL11.glVertex3f(100,0,-100);
            GL11.glTexCoord2f(10,10);
            GL11.glVertex3f(100,0,100);
            GL11.glTexCoord2f(0,10);
            GL11.glVertex3f(-100,0,100);
        }
        GL11.glEnd();
        GL11.glDisable(GL11.GL_TEXTURE_2D);*/

        {
            FloatBuffer vertex_buffer = CacheBuffer.getCachedFloatBuffer(5 * 4);
            vertex_buffer.put(0.0f).put(0.0f);
            vertex_buffer.put(-100).put(0).put(-100);

            vertex_buffer.put(10.0f).put(.0f);
            vertex_buffer.put(100).put(0).put(-100);

            vertex_buffer.put(10.0f).put(10.0f);
            vertex_buffer.put(100).put(0).put(100);

            vertex_buffer.put(.0f).put(10.0f);
            vertex_buffer.put(-100).put(0).put(100);
            vertex_buffer.flip();

            final int stride = 5 * 4;
            final int position_loc = 0, pos_offset = 2;
            final int texture0_loc = 1, tex0_offset = 0;
            gl.glEnableVertexAttribArray(position_loc);
            gl.glEnableVertexAttribArray(texture0_loc);

            gl.glVertexAttribPointer(position_loc, 3, GLenum.GL_FLOAT, false, stride, vertex_buffer.position(pos_offset));
            gl.glVertexAttribPointer(texture0_loc, 2, GLenum.GL_FLOAT, false, stride, vertex_buffer.position(tex0_offset));

            gl.glDrawArrays(GLenum.GL_TRIANGLE_FAN, 0, 4);

            gl.glDisableVertexAttribArray(position_loc);
            gl.glDisableVertexAttribArray(texture0_loc);
        }

        createSkyBox(0, 20, 0, 400, 200, 400,
                LandscapeTextures[2].getTexture(),LandscapeTextures[3].getTexture(),LandscapeTextures[0].getTexture(),
                LandscapeTextures[1].getTexture(),LandscapeTextures[4].getTexture(),LandscapeTextures[5].getTexture());

        /*GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glPushMatrix();
        my_renderer.placeObstacles();
        GL11.glCallList(firePit2);
        GL11.glPopMatrix();
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glPopMatrix();*/

        m_lightProgram.enable();
        m_lightProgram.setEnableLighting(true);
        m_mat_stack.pushMatrix();
        my_renderer.placeObstacles(m_mat_stack);
        m_proj.load(m_view);  // save the viewProj to m_proj
        firePit2.run();
        m_mat_stack.popMatrix();
        m_lightProgram.setEnableLighting(false);
        my_framerate.displayFramerate();
    }

    void minimize_scene(){
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER_EXT,my_fxbuffers.getFramebufferNo(2));

        gl.glViewport(0,0,my_fxbuffers.getDimX(2),my_fxbuffers.getDimY(2));
        /*GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GLU.gluOrtho2D( -1.0f, 1.0f, -1.0f, 1.0f);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);*/

        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT | GLenum.GL_DEPTH_BUFFER_BIT);
        gl.glDisable(GLenum.GL_DEPTH_TEST);
//        gl.glEnable(GL11.GL_TEXTURE_2D);

        gl.glBindTexture(GLenum.GL_TEXTURE_2D,my_fxbuffers.getTextureNo(0));
//		GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
        gl.glGenerateMipmap(GLenum.GL_TEXTURE_2D);

        /*GL11.glBegin(GL11.GL_QUADS);
        {
            GL11.glTexCoord2f( 0, 0);
            GL11.glVertex3f(-1,-1, 0.0f );

            GL11.glTexCoord2f( 1.0f, 0);
            GL11.glVertex3f(1,-1, 0.0f );

            GL11.glTexCoord2f( 1.0f, 1.0f);
            GL11.glVertex3f(1,1, 0.0f );

            GL11.glTexCoord2f( 0, 1.0f);
            GL11.glVertex3f(-1,1, 0.0f );
        }
        GL11.glEnd();
        GL11.glDisable(GL11.GL_TEXTURE_2D);*/
        m_textureProgram.enable();
        int mvpLoc = m_textureProgram.getUniformLocation("g_MVP");
        gl.glUniformMatrix4fv(mvpLoc, false, CacheBuffer.wrap(Matrix4f.IDENTITY));  // no transform
        {
            FloatBuffer vertex_buffer = CacheBuffer.getCachedFloatBuffer(4 * 4);
            vertex_buffer.put(0.0f).put(0.0f);
            vertex_buffer.put(-1).put(-1);

            vertex_buffer.put(1.0f).put(.0f);
            vertex_buffer.put(1).put(-1);

            vertex_buffer.put(1.0f).put(1.0f);
            vertex_buffer.put(1).put(1);

            vertex_buffer.put(.0f).put(1.0f);
            vertex_buffer.put(-1).put(1);
            vertex_buffer.flip();

            final int stride = 4 * 4;
            final int position_loc = 0, pos_offset = 2;
            final int texture0_loc = 1, tex0_offset = 0;
            gl.glEnableVertexAttribArray(position_loc);
            gl.glEnableVertexAttribArray(texture0_loc);

            gl.glVertexAttribPointer(position_loc, 2, GLenum.GL_FLOAT, false, stride, vertex_buffer.position(pos_offset));
            gl.glVertexAttribPointer(texture0_loc, 2, GLenum.GL_FLOAT, false, stride, vertex_buffer.position(tex0_offset));

            gl.glDrawArrays(GLenum.GL_TRIANGLE_FAN, 0, 4);

            gl.glDisableVertexAttribArray(position_loc);
            gl.glDisableVertexAttribArray(texture0_loc);
        }

    }

    Runnable createFirePit(//int firePitDisplayListNo,
                       float rBaseBottom, float rBaseWallTilt, float baseHeight,
                       float rFeather, float featherPartHeight, float featherUpShift, float featherSideShift,
                       float firePitBasePosY,
                       float red, float green, float blue, float alpha, final boolean onlyBase){

//        Cylinder cylinder = new Cylinder();
//        Sphere sphere = new Sphere();
//        Disk disk = new Disk();
        float rBaseTop = (float) (rBaseBottom + baseHeight/ Math.tan(rBaseWallTilt));
        float featherHeight = (float) Math.sqrt( featherPartHeight*featherPartHeight + featherSideShift*featherSideShift );
        float featherTiltAngle = (float) (90.0 - 180*Math.atan(featherPartHeight/featherSideShift)/3.141592654f);
        float feathersCrownUpShift = featherPartHeight + featherUpShift;

        final GLVAO cylinder0 = getCylinder(rBaseBottom, rBaseTop, baseHeight);
        final GLVAO cylinder1 = getCylinder(rFeather, rFeather, featherHeight);
        final GLVAO sphere = getSphere(rFeather);
        final GLVAO disk = getDisk(rBaseBottom);

//        GL11.glNewList(firePitDisplayListNo, GL11.GL_COMPILE);

        return ()->
        {
            /******************************************************************************/
            if(onlyBase) gl.glEnable(GLenum.GL_CULL_FACE);

//        GL11.glColor4f(red,green,blue,alpha);
            setColorMaterial(red,green,blue, 128.0f);

            /*GL11.glPushMatrix();
            GL11.glTranslatef(0,firePitBasePosY,0);
            GL11.glRotatef(-90.0f,1,0,0);*/
            m_mat_stack.pushMatrix();
            m_mat_stack.translate(0,firePitBasePosY,0);
            m_mat_stack.rotate(-90.0f,1,0,0);

//	        gluCylinder(quadric_p, rBaseBottom, rBaseTop, baseHeight,10,1);
//        cylinder.draw(rBaseBottom, rBaseTop, baseHeight,10,1);
            cylinder0.bind();
            cylinder0.draw(GLenum.GL_TRIANGLES);
            cylinder0.unbind();

            if(onlyBase) gl.glDisable(GLenum.GL_CULL_FACE);

            if(!onlyBase){
                for(int i=0;i<4;i++){
                    /*GL11.glPushMatrix();
                    GL11.glRotatef(90.0f*i,0,0,1);
                    GL11.glTranslatef(-featherSideShift,0,featherUpShift);
                    GL11.glRotatef(featherTiltAngle,0,1,0);*/
                    m_mat_stack.pushMatrix();
                    m_mat_stack.rotate(90.0f*i,0,0,1);
                    m_mat_stack.translate(-featherSideShift,0,featherUpShift);
                    m_mat_stack.rotate(featherTiltAngle,0,1,0);
//			gluSphere(quadric_p,rFeather,10,10);
//                    sphere.draw(rFeather,10,10);

                    Matrix4f modelView = m_mat_stack.getTotalMatrix();
                    m_lightProgram.setModel(modelView);
                    Matrix4f.mul(m_proj, modelView, m_view);
                    m_lightProgram.setModelViewProj(m_view);
                    sphere.bind();
                    sphere.draw(GLenum.GL_TRIANGLES);
                    sphere.unbind();

//			gluCylinder(quadric_p, rFeather, rFeather, featherHeight,10,1);
//                cylinder.draw(rFeather, rFeather, featherHeight,10,1);
                    cylinder1.bind();
                    cylinder1.draw(GLenum.GL_TRIANGLES);
                    cylinder1.unbind();
//                    GL11.glPopMatrix();
                    m_mat_stack.popMatrix();
                }

//                GL11.glRotatef(180.0f,1,0,0);
                m_mat_stack.rotate(180.0f,1,0,0);
//		gluDisk(quadric_p, 0.0, rBaseBottom, 10, 1);
//            disk.draw(0.0f, rBaseBottom, 10, 1);
                Matrix4f modelView = m_mat_stack.getTotalMatrix();
                m_lightProgram.setModel(modelView);
                Matrix4f.mul(m_proj, modelView, m_view);
                m_lightProgram.setModelViewProj(m_view);
                disk.bind();
                disk.draw(GLenum.GL_TRIANGLES);
                disk.unbind();

//                GL11.glTranslatef(0.0f, 0.0f, -feathersCrownUpShift);
                m_mat_stack.translate(0.0f, 0.0f, -feathersCrownUpShift);
//		gluSphere(quadric_p,rFeather,10,10);
//            sphere.draw(rFeather,10,10);
                modelView = m_mat_stack.getTotalMatrix();
                m_lightProgram.setModel(modelView);
                Matrix4f.mul(m_proj, modelView, m_view);
                m_lightProgram.setModelViewProj(m_view);
                sphere.bind();
                sphere.draw(GLenum.GL_TRIANGLES);
                sphere.unbind();
            }
//            GL11.glPopMatrix();
            m_mat_stack.popMatrix();

            /*************************************************************************************/
//            GL11.glEndList();
//	    gluDeleteQuadric(quadric_p);
        };
    }

    GLVAO getCylinder(float baseRadius, float topRadius, float height){
        CylinderParams params = new CylinderParams(baseRadius, topRadius, height);
        GLVAO vao = m_ModelCaches.get(params);
        if(vao != null){
            return vao;
        }else{
            QuadricBuilder builder = new QuadricBuilder();
            builder.setXSteps(10).setYSteps(10);
            builder.setDrawMode(DrawMode.FILL);
            builder.setPostionLocation(POSITION_LOC);
            builder.setNormalLocation(NORMAL_LOC);
            builder.setTexCoordLocation(TEXTURE_LOC);
            vao = new QuadricMesh(builder, new QuadricCylinder(baseRadius, topRadius, height)).getModel().genVAO();
            m_ModelCaches.put(params, vao);
            return vao;
        }
    }

    GLVAO getSphere(float radius){
        SphereParams params = new SphereParams(radius);
        GLVAO vao = m_ModelCaches.get(params);
        if(vao != null){
            return vao;
        }else{
            QuadricBuilder builder = new QuadricBuilder();
            builder.setXSteps(10).setYSteps(1);
            builder.setDrawMode(DrawMode.FILL);
//            builder.setCenterToOrigin(true);
            builder.setPostionLocation(POSITION_LOC);
            builder.setNormalLocation(NORMAL_LOC);
            builder.setTexCoordLocation(TEXTURE_LOC);
            vao = new QuadricMesh(builder, new QuadricSphere(radius)).getModel().genVAO();
            m_ModelCaches.put(params, vao);
            return vao;
        }
    }

    GLVAO getDisk(float baseRadius){
        DiskParams params = new DiskParams(baseRadius);
        GLVAO vao = m_ModelCaches.get(params);
        if(vao != null){
            return vao;
        }else{
            QuadricBuilder builder = new QuadricBuilder();
            builder.setXSteps(10).setYSteps(1);
            builder.setDrawMode(DrawMode.FILL);
//            builder.setCenterToOrigin(true);
            builder.setPostionLocation(POSITION_LOC);
            builder.setNormalLocation(NORMAL_LOC);
            builder.setTexCoordLocation(TEXTURE_LOC);
            vao = new QuadricMesh(builder, new QuadricDisk(0, baseRadius)).getModel().genVAO();
            m_ModelCaches.put(params, vao);
            return vao;
        }
    }

    private static final class CylinderParams{
        float baseRadius;
        float topRadius;
        float height;

        public CylinderParams(float baseRadius, float topRadius, float height) {
            this.baseRadius = baseRadius;
            this.topRadius = topRadius;
            this.height = height;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CylinderParams that = (CylinderParams) o;

            if (Float.compare(that.baseRadius, baseRadius) != 0) return false;
            if (Float.compare(that.topRadius, topRadius) != 0) return false;
            return Float.compare(that.height, height) == 0;

        }

        @Override
        public int hashCode() {
            int result = (baseRadius != +0.0f ? Float.floatToIntBits(baseRadius) : 0);
            result = 31 * result + (topRadius != +0.0f ? Float.floatToIntBits(topRadius) : 0);
            result = 31 * result + (height != +0.0f ? Float.floatToIntBits(height) : 0);
            return result;
        }
    }

    private static final class DiskParams{
        float bottom;

        public DiskParams(float bottom) {
            this.bottom = bottom;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            DiskParams that = (DiskParams) o;

            return Float.compare(that.bottom, bottom) == 0;

        }

        @Override
        public int hashCode() {
            return (bottom != +0.0f ? Float.floatToIntBits(bottom) : 0);
        }
    }

    private static final class SphereParams{
        float radius;

        public SphereParams(float radius) {
            this.radius = radius;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SphereParams that = (SphereParams) o;

            return Float.compare(that.radius, radius) == 0;

        }

        @Override
        public int hashCode() {
            return (radius != +0.0f ? Float.floatToIntBits(radius) : 0);
        }
    }

    @Override
    public boolean handleKeyInput(int code, NvKeyActionType action) {
        switch (action){
            case DOWN:
                if(code == NvKey.K_X){
                    a_pressed = true;
                    return true;
                }else if(code == K_Z){
                    z_pressed =true;
                    return true;
                }

            case UP:
                if(code == NvKey.K_X){
                    a_pressed = false;
                    return true;
                }else if(code == K_Z){
                    z_pressed =false;
                    return true;
                }
        }

        return false;
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
