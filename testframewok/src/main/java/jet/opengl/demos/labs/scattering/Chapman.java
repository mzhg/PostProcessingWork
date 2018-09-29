package jet.opengl.demos.labs.scattering;

import com.nvidia.developer.opengl.app.NvCameraMotionType;
import com.nvidia.developer.opengl.app.NvSampleApp;
import com.nvidia.developer.opengl.models.DrawMode;
import com.nvidia.developer.opengl.models.GLVAO;
import com.nvidia.developer.opengl.models.QuadricBuilder;
import com.nvidia.developer.opengl.models.QuadricMesh;
import com.nvidia.developer.opengl.models.QuadricSphere;

import org.lwjgl.util.vector.Matrix;
import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.io.IOException;

import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CachaRes;
import jet.opengl.postprocessing.util.CacheBuffer;

public class Chapman extends NvSampleApp {

    private GLVAO mSphere;
    private Texture2D mEarthColor;
    private Texture2D mOceanMask;

    private GLSLProgram mEarthProgram;
    private GLSLProgram mChapmam;

    private final Matrix4f mModel = new Matrix4f();
    private final Matrix4f mView = new Matrix4f();
    private final Matrix4f mProj = new Matrix4f();
    private final Matrix4f mMVP = new Matrix4f();
    private final Matrix3f mNormalMat = new Matrix3f();

    private GLFuncProvider gl;

    @Override
    protected void initRendering() {
        m_transformer.setMotionMode(NvCameraMotionType.FIRST_PERSON);
        m_transformer.setTranslation(0, 0, -3);

        gl = GLFuncProviderFactory.getGLFuncProvider();

        QuadricBuilder builder = new QuadricBuilder();
        builder.setXSteps(30).setYSteps(30);
        builder.setDrawMode(DrawMode.FILL);
        builder.setCenterToOrigin(true);
        builder.setPostionLocation(0);
        builder.setNormalLocation(1);
        builder.setTexCoordLocation(2);

        mSphere = new QuadricMesh(builder, new QuadricSphere(1)).getModel().genVAO();

        String root = "labs\\Chapman\\";
        try {
            mEarthColor = TextureUtils.createTexture2DFromFile(root + "textures\\earthcolor.jpg",false);
            mOceanMask = TextureUtils.createTexture2DFromFile(root + "textures\\oceanmask.jpg",false);

            mEarthProgram = GLSLProgram.createFromFiles(root + "shaders\\EarthVS.vert", root + "shaders\\EarthPS.frag");
            mChapmam = GLSLProgram.createFromFiles(root + "shaders\\chapman_vs.vert", root + "shaders\\chapman_ps.frag");

            mEarthProgram.enable();
            int textureIndex = mEarthProgram.getUniformLocation("g_Texture");
            gl.glUniform1i(textureIndex, 0);

        } catch (IOException e) {
            e.printStackTrace();
        }

        mModel.scale(1.1f);
    }

    @Override
    public void display() {
        gl.glClearColor(0,0,0,0);
        gl.glClearDepthf(1.f);
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT|GLenum.GL_DEPTH_BUFFER_BIT);

        gl.glViewport(0, 0, getGLContext().width(), getGLContext().height());
        gl.glEnable(GLenum.GL_DEPTH_TEST);

        m_transformer.getModelViewMat(mView);
        Matrix4f.mul(mProj, mView, mMVP);
        Matrix4f.mul(mMVP, mModel, mMVP);

//        mEarthProgram.enable();
//        int mvpIndex = mEarthProgram.getUniformLocation("g_ModelViewProj");
//        gl.glUniformMatrix4fv(mvpIndex, false, CacheBuffer.wrap(mMVP));
        mChapmam.enable();
        int mvpIndex = mChapmam.getUniformLocation("g_ModelViewProj");
        gl.glUniformMatrix4fv(mvpIndex, false, CacheBuffer.wrap(mMVP));
        int modeIndex = mChapmam.getUniformLocation("g_Model");
        gl.glUniformMatrix4fv(modeIndex, false, CacheBuffer.wrap(mModel));

//        Matrix4f.mul(mView, mModel, mView);
        Vector3f pos = new Vector3f();
        Vector3f look = new Vector3f();
        Matrix4f.decompseRigidMatrix(mView, pos, null, null, look);
        look.scale(-1);

        int posIndex = mChapmam.getUniformLocation("g_CameraPos");
        gl.glUniform3f(posIndex, pos.x, pos.y,pos.z);
        int viewIndex = mChapmam.getUniformLocation("g_CameraView");
        gl.glUniform3f(viewIndex, look.x, look.y,look.z);

        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glBindTexture(mEarthColor.getTarget(), mEarthColor.getTexture());
        gl.glActiveTexture(GLenum.GL_TEXTURE1);
        gl.glBindTexture(mOceanMask.getTarget(), mOceanMask.getTexture());

        mSphere.bind();
        mSphere.draw(GLenum.GL_TRIANGLES);
        mSphere.unbind();

        GLCheck.checkError();
    }

    @Override
    protected void reshape(int width, int height) {
        if(width <= 0 || height <=0)
            return;

        Matrix4f.perspective(60, (float)width/height, 0.1f, 10.f, mProj);
    }
}
