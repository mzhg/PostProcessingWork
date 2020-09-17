package jet.opengl.demos.labs.atmosphere;

import com.nvidia.developer.opengl.app.NvCameraMotionType;
import com.nvidia.developer.opengl.app.NvSampleApp;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.io.IOException;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.GLSLUtil;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.TextureUtils;

public final class AtmosphereOriginDemo extends NvSampleApp {

    private float sun_zenith_angle_radians_ = 1.3f;
    private float sun_azimuth_angle_radians_ = 2.9f;
    private int full_screen_quad_vao_;

    private ModelOrigin model_;
    private Texture2D reflectanceTexture;
    private GLSLProgram program_;

    private GLFuncProvider gl;

    private final Matrix4f mProj = new Matrix4f();
    private final Matrix4f mView = new Matrix4f();
    private final Matrix4f mTemp = new Matrix4f();
    private final Vector3f camera = new Vector3f();
    private final Vector3f sun_direction = new Vector3f();

    @Override
    protected void initRendering() {
        gl = GLFuncProviderFactory.getGLFuncProvider();
        model_ = new ModelOrigin();
        model_.Init();

        m_transformer.setMotionMode(NvCameraMotionType.FIRST_PERSON);
        m_transformer.setTranslation(0, -4, 0);

        full_screen_quad_vao_ = gl.glGenVertexArray();

        try {
            reflectanceTexture = TextureUtils.createTexture2DFromFile("labs\\Chapman\\textures\\", false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        final String shaderPath = "labs/Atmosphere/shaders/";
        final String kVertexShader = "shader_libs/PostProcessingDefaultScreenSpaceVS.vert";
        program_ = GLSLProgram.createProgram(kVertexShader, shaderPath+"earth.glsl", null);
    }

    @Override
    public void display() {

        program_.enable();

        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
        gl.glViewport(0,0, getGLContext().width(), getGLContext().height());
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT|GLenum.GL_DEPTH_BUFFER_BIT);

        m_transformer.getModelViewMat(mView);

        Matrix4f.decompseRigidMatrix(mView, camera, null, null);

        Matrix4f.invertRigid(mView, mTemp);
        GLSLUtil.setMat4(program_, "viewInverse", mTemp);

        Matrix4f.invert(mProj, mTemp);
        GLSLUtil.setMat4(program_, "projInverse", mTemp);

        sun_direction.x = (float)(Math.cos(sun_azimuth_angle_radians_) * Math.sin(sun_zenith_angle_radians_));
        sun_direction.y = (float) (Math.sin(sun_azimuth_angle_radians_) * Math.sin(sun_zenith_angle_radians_));
        sun_direction.z = (float)Math.cos(sun_zenith_angle_radians_);

        GLSLUtil.setFloat3(program_, "s", sun_direction);
        GLSLUtil.setFloat3(program_, "c", camera);
        GLSLUtil.setFloat(program_, "exposure", 0.4f);

//        model_.bindRenderingResources();

        gl.glBindVertexArray(full_screen_quad_vao_);
        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
        gl.glBindVertexArray(0);

//        model_.unbindRenderingResources();

        program_.setName("Demo Program");
        program_.printOnce();
    }

    @Override
    protected void reshape(int width, int height) {
        if(width <= 0 || height <=0)
            return;

        Matrix4f.perspective(50, (float)width/height, 0.1f, 1000.f, mProj);
    }
}
