package jet.opengl.demos.gpupro.ibl;

import com.nvidia.developer.opengl.app.NvSampleApp;
import com.nvidia.developer.opengl.models.obj.NvGLModel;
import com.nvidia.developer.opengl.utils.NvImage;

import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.io.IOException;

import jet.opengl.demos.nvidia.waves.samples.SkyBoxRender;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.TextureCube;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.FileUtils;

public class IndirectLighting extends NvSampleApp {
    private SkyBoxRender mSkyBox;
    private IrradianceCubeMap mIrraMap;
    private TextureCube mOriginEnvMap;

    private GLSLProgram mShadingProg;

    private GLFuncProvider gl;
    private final Matrix4f mProj = new Matrix4f();
    private final Matrix4f mView = new Matrix4f();
    private final Matrix4f mMVP = new Matrix4f();
    private final Matrix3f mNormalMat = new Matrix3f();

    private final int[] object_ids = new int[3];
    private final int[] triangles_count = new int[3];

    private static final String model_file[]={"venus","teapot","knot"};

    private boolean mShowIrraMap;

    @Override
    public void initUI() {
        mTweakBar.addValue("Show IrradianceCubeMap", createControl("mShowIrraMap"));
    }

    @Override
    protected void initRendering() {
        gl = GLFuncProviderFactory.getGLFuncProvider();

        mSkyBox = new SkyBoxRender();
        try {
            NvImage image = new NvImage();
            image.loadImageFromFile("nvidia/WaveWorks/textures/sky_cube.dds");

            if(!image.isCubeMap()){
                throw new IllegalArgumentException("The nvidia/WaveWorks/textures/sky_cube.dds doesn't a cube map file.");
            }

            int cubeMap = image.updaloadTexture();
            mOriginEnvMap = TextureUtils.createTextureCube(GLenum.GL_TEXTURE_CUBE_MAP, cubeMap);

            final String root = "gpupro\\IBL\\shaders\\";
            mShadingProg = GLSLProgram.createFromFiles(root + "MatteObject.vert", root + "MatteObject.frag");
        } catch (IOException e) {
            e.printStackTrace();
        }

        IrradianceCubeMap.Desc desc = new IrradianceCubeMap.Desc();
        desc.outputToInternalCubeMap = true;
        desc.outputSize = 64;
        desc.sourceFromFile = false;
        desc.sourceEnvMap  = mOriginEnvMap;

        mIrraMap = new IrradianceCubeMap();
        mIrraMap.generateCubeMap(desc);

        loadModels();

        m_transformer.setTranslationVec(new Vector3f(0.0f, 0.0f, -200.2f));
        m_transformer.setRotationVec(new Vector3f(-0.2f, -0.3f, 0));
    }

    @Override
    public void display() {
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
        gl.glViewport(0,0, getGLContext().width(), getGLContext().height());
        gl.glClearColor(0,0,0,0);
        gl.glClearDepthf(1);
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT|GLenum.GL_DEPTH_BUFFER_BIT);

        m_transformer.getModelViewMat(mView);
        Matrix4f.getNormalMatrix(mView, mNormalMat);
        Matrix4f.mul(mProj, mView, mMVP);

        if(mShowIrraMap){
            mSkyBox.setCubemap(mIrraMap.getOutput().getTexture());
        }else{
            mSkyBox.setCubemap(mOriginEnvMap.getTexture());
        }

        mSkyBox.setRotateMatrix(mView);
        mSkyBox.setProjectionMatrix(mProj);
        mSkyBox.draw();
        GLCheck.checkError();

        // Render the model
        gl.glEnable(GLenum.GL_DEPTH_TEST);
        mShadingProg.enable();
        int normalMat = mShadingProg.getUniformLocation("g_NormalMat", true);
        int mvpMat = mShadingProg.getUniformLocation("g_MVP");

        if(normalMat >=0) gl.glUniformMatrix3fv(normalMat, false, CacheBuffer.wrap(mNormalMat));
        if(mvpMat >= 0) gl.glUniformMatrix4fv(mvpMat, false, CacheBuffer.wrap(mMVP));
        gl.glBindTexture(mIrraMap.getOutput().getTarget(), mIrraMap.getOutput().getTexture());

        drawModel(0, 0, 1);
    }

    @Override
    protected void reshape(int width, int height) {
        if(width <= 0 || height <= 0)
            return;

        Matrix4f.perspective(60, (float)width/height, 0.1f, 1000.f, mProj);
    }

    private void loadModels (){
        for(int i = 0; i < 3; i++){
            byte[] data = null;
            try {
                data = FileUtils.loadBytes("HDR/models/" + model_file[i]);
            } catch (IOException e) {
                e.printStackTrace();
            }
            int bufID;
            bufID = gl.glGenBuffer();
            gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, bufID);
            gl.glBufferData(GLenum.GL_ARRAY_BUFFER, CacheBuffer.wrap(data), GLenum.GL_STATIC_DRAW);
            gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);

            object_ids[i] = bufID;
            triangles_count[i] = data.length/32;
        }

        GLCheck.checkError("loadModels done!");
    }

    private void drawModel(int index, int position, int normal){
        gl.glDisable(GLenum.GL_CULL_FACE);
        gl.glBindVertexArray(0);

        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, object_ids[index]);
        gl.glEnableVertexAttribArray(position);
        gl.glEnableVertexAttribArray(normal);
        gl.glVertexAttribPointer(position, 3, GLenum.GL_FLOAT, false, 32, 0);
        gl.glVertexAttribPointer(normal, 3, GLenum.GL_FLOAT, false, 32, 12);
        gl.glDrawArrays( GLenum.GL_TRIANGLES, 0, triangles_count[index]);
        gl.glDisableVertexAttribArray(position);
        gl.glDisableVertexAttribArray(normal);

        GLCheck.checkError();
    }
}
