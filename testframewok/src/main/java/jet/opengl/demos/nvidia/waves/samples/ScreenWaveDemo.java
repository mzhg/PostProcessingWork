package jet.opengl.demos.nvidia.waves.samples;

import com.nvidia.developer.opengl.app.NvCameraMotionType;
import com.nvidia.developer.opengl.app.NvInputDeviceType;
import com.nvidia.developer.opengl.app.NvPointerActionType;
import com.nvidia.developer.opengl.app.NvPointerEvent;
import com.nvidia.developer.opengl.app.NvSampleApp;
import com.nvidia.developer.opengl.models.DrawMode;
import com.nvidia.developer.opengl.models.GLVAO;
import com.nvidia.developer.opengl.models.Model;
import com.nvidia.developer.opengl.models.QuadricBuilder;
import com.nvidia.developer.opengl.models.QuadricMesh;
import com.nvidia.developer.opengl.models.QuadricPlane;
import com.nvidia.developer.opengl.utils.NvImage;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.io.IOException;

import jet.opengl.demos.nvidia.waves.SimpleWaveSimulator;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.Numeric;

public class ScreenWaveDemo extends NvSampleApp {
    private SimpleWaveSimulator m_WaveSimulator2;
    private SimpleWaveSimulator m_WaveSimulator;
    private GLSLProgram m_WaterRender;
    private GLSLProgram m_WaterRender2;
    private GLVAO m_plane2;
    private GLVAO m_plane;
    private Texture2D m_image;
    private int m_cube_map;

    private int m_frequency = 5;
    private int m_count = 0;

    private final Matrix4f m_MVP = new Matrix4f();
    private final Matrix4f m_View = new Matrix4f();
    private final Matrix4f m_Proj = new Matrix4f();

    private final SimpleWaveSimulator.Params m_Params = new SimpleWaveSimulator.Params();
    private GLFuncProvider gl;

    private boolean m_IsTouchDwon;

    @Override
    protected void initRendering() {
//        getGLContext().setSwapInterval(0);
        gl = GLFuncProviderFactory.getGLFuncProvider();
        m_WaveSimulator2 = new SimpleWaveSimulator();
        m_WaveSimulator = new SimpleWaveSimulator();

        final String root = "nvidia\\ScreenWaveDemo\\shaders\\";
        try {
            m_WaterRender = GLSLProgram.createFromFiles(root + "WaveRenderVS.vert", root + "WaveRenderPS.frag");
            m_WaterRender2 = GLSLProgram.createFromFiles(root + "water.vert", root + "water.frag");
            m_image = TextureUtils.createTexture2DFromFile("nvidia\\ScreenWaveDemo\\textures\\demo_image.jpg",true);

            NvImage image = new NvImage();
            image.loadImageFromFile("nvidia/WaveWorks/textures/sky_cube.dds");

            if(!image.isCubeMap()){
                throw new IllegalArgumentException();
            }
            m_cube_map = image.updaloadTexture();
        } catch (IOException e) {
            e.printStackTrace();
        }

        m_Params.timeScale = 1;
        m_Params.nicest = true;
        m_Params.damping = 0.99f;

        m_WaveSimulator2.setTextureSize(256, 256);
        QuadricBuilder builder = new QuadricBuilder();
        builder.setXSteps(100).setYSteps(100);
        builder.setPostionLocation(0);
        builder.setTexCoordLocation(1);
        builder.setDrawMode(DrawMode.FILL);
        builder.setGenNormal(false);
        builder.setAutoGenNormal(false);
        builder.setCenterToOrigin(false);
        Model plane = new QuadricMesh(builder, new QuadricPlane(256, 256)).getModel();
        m_plane2 = plane.genVAO();

        m_transformer.setMotionMode(NvCameraMotionType.FIRST_PERSON);
        m_transformer.setTranslation(-128, -50, -128);  // the plane center
        m_transformer.setMaxTranslationVel(20);
    }

    @Override
    public void display() {
        m_transformer.getModelViewMat(m_View);
        Matrix4f.mul(m_Proj, m_View, m_MVP);

        m_count ++;
        if(m_count > m_frequency){
            m_WaveSimulator2.addDrop(Numeric.random(0, 1), Numeric.random(0,1),
                    Numeric.random(4f/256, 4f/128), 20);

            m_count = 0;
        }

        renderWater1();
//        renderWater2();
    }

    private void renderWater1(){
        m_WaveSimulator.simulate(m_Params, getFrameDeltaTime());

        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
        gl.glViewport(0,0,getGLContext().width(), getGLContext().height());
        gl.glClearColor(0,0,0,0);
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT);
        gl.glDisable(GLenum.GL_DEPTH_TEST);

        m_WaterRender.enable();

        gl.glBindTextureUnit(0, m_WaveSimulator.getHeightMap().getTexture());
        gl.glBindTextureUnit(1, m_WaveSimulator.getGradientMap().getTexture());
        gl.glBindTextureUnit(2, m_image.getTexture());

        Matrix4f.ortho(0, m_WaveSimulator.getTextureWidth(), 0, m_WaveSimulator.getTextureHeight(), -1, 1, m_MVP);
        int mvpIndex = m_WaterRender.getUniformLocation("g_MVP");
        gl.glUniformMatrix4fv(mvpIndex, false, CacheBuffer.wrap(m_MVP));

        m_plane.bind();
        m_plane.draw(GLenum.GL_TRIANGLES);
        m_plane.unbind();
    }

    private void renderWater2(){
        m_WaveSimulator2.simulate(m_Params, getFrameDeltaTime());

        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
        gl.glViewport(0,0,getGLContext().width(), getGLContext().height());
        gl.glClearColor(0,0,0,0);
        gl.glClearDepthf(1.f);
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT|GLenum.GL_DEPTH_BUFFER_BIT);
        gl.glEnable(GLenum.GL_DEPTH_TEST);

        m_WaterRender2.enable();

        gl.glBindTextureUnit(0, m_WaveSimulator2.getHeightMap().getTexture());
        gl.glBindTextureUnit(1, m_WaveSimulator2.getGradientMap().getTexture());
        gl.glBindTextureUnit(2, m_cube_map);

//        uniform mat4 ModelViewMatrix;
//        uniform mat4 ProjectionMatrix;
//        uniform vec4 eyePositionWorld;

        int mvpIndex = m_WaterRender2.getUniformLocation("ModelViewMatrix");
        gl.glUniformMatrix4fv(mvpIndex, false, CacheBuffer.wrap(m_View));
        mvpIndex = m_WaterRender2.getUniformLocation("ProjectionMatrix");
        gl.glUniformMatrix4fv(mvpIndex, false, CacheBuffer.wrap(m_Proj));

        Vector3f eyePos = m_transformer.getTranslationVec();
        mvpIndex = m_WaterRender2.getUniformLocation("eyePositionWorld");
        gl.glUniform4f(mvpIndex, eyePos.x, eyePos.y, eyePos.z, 1);

        m_plane2.bind();
        m_plane2.draw(GLenum.GL_TRIANGLES);
        m_plane2.unbind();
    }

    @Override
    public boolean handlePointerInput(NvInputDeviceType device, int action, int modifiers, int count, NvPointerEvent[] points) {
        float x = points[0].m_x;
        float y = points[0].m_y;

        if(action == NvPointerActionType.DOWN){
            m_IsTouchDwon = true;
        }else if(action == NvPointerActionType.UP){
            m_IsTouchDwon  = false;
        }

        if(m_IsTouchDwon)
            m_WaveSimulator.addDrop(x/getGLContext().width(), 1-y/getGLContext().height(),
                    Numeric.random(4f/256, 4f/128), Numeric.random(15,25));

        return super.handlePointerInput(device, action, modifiers, count, points);
    }

    @Override
    protected void reshape(int width, int height) {
        if(width <=0 || height <= 0)
            return;

//        Matrix4f.ortho(-(float)width/height, (float)width/height, -1, 1, 0, 10, m_MVP);
//        Matrix4f.ortho(0, (float)width/4, 0, height/4, -100, 100, m_MVP);
        Matrix4f.perspective(60, (float)width/height, 0.1f, 1000.f, m_Proj);

        m_WaveSimulator.setTextureSize(width/4, height/4);
        QuadricBuilder builder = new QuadricBuilder();
        builder.setXSteps(100).setYSteps(100);
        builder.setPostionLocation(0);
        builder.setTexCoordLocation(1);
        builder.setDrawMode(DrawMode.FILL);
        builder.setGenNormal(false);
        builder.setAutoGenNormal(false);
        builder.setCenterToOrigin(false);
        Model plane = new QuadricMesh(builder, new QuadricPlane(width/4, height/4)).getModel();
        m_plane = plane.genVAO();
    }
}
