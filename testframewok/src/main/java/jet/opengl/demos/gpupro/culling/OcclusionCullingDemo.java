package jet.opengl.demos.gpupro.culling;

import com.nvidia.developer.opengl.app.NvCameraMotionType;
import com.nvidia.developer.opengl.app.NvSampleApp;
import com.nvidia.developer.opengl.models.DrawMode;
import com.nvidia.developer.opengl.models.GLVAO;
import com.nvidia.developer.opengl.models.ModelGenerator;
import com.nvidia.developer.opengl.models.QuadricBuilder;
import com.nvidia.developer.opengl.models.QuadricMesh;
import com.nvidia.developer.opengl.models.QuadricSphere;
import com.nvidia.developer.opengl.ui.NvUIText;

import org.lwjgl.util.vector.Vector3f;

import java.io.IOException;

import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.VisualDepthTextureProgram;
import jet.opengl.postprocessing.texture.SamplerDesc;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.util.BoundingBox;
import jet.opengl.postprocessing.util.Numeric;

public final class OcclusionCullingDemo extends NvSampleApp {
    private Renderer mRenderer;
    private Scene mScene;
    private  OcclusionTester mCulling;

    private GLVAO mSphereVao;
    private GLVAO mCubeVao;

    // reflection variables.
    private boolean mOcclusionEnabled;
    private boolean mFineOcclusion;
    private boolean mShowHZBuffer;
    private int mHZBSlice;

    private NvUIText mCullingInfo;
    private VisualDepthTextureProgram mVisualTexture;
    private int m_PointSampler;
    @Override
    public void initUI() {
        mTweakBar.addValue("Occlusion Enabled", createControl("mOcclusionEnabled"));
        mTweakBar.addValue("Fine Occlusion", createControl("mFineOcclusion"));
        mTweakBar.addValue("Visual HZB", createControl("mShowHZBuffer"));
        mTweakBar.addValue("HZB Slice", createControl("mHZBSlice"), 0, 9);

    }

    @Override
    protected void initRendering() {
        mScene = new Scene();
        buildBaseMesh();
        buildModles();
        mScene.buildMeshInformations();

        mRenderer = new ForwardRenderer();
        mRenderer.onCreate();

        m_transformer.setMotionMode(NvCameraMotionType.FIRST_PERSON);
        m_transformer.setTranslation(1,1,1);

//        mCulling = new HZBOcclusionTester();
        mCulling = new DawnCulling();

        try {
            mVisualTexture = new VisualDepthTextureProgram(false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(m_PointSampler == 0){
            SamplerDesc desc = new SamplerDesc();
            desc.minFilter = desc.magFilter = GLenum.GL_NEAREST;
            m_PointSampler = SamplerUtils.createSampler(desc);
            desc.minFilter = GLenum.GL_NEAREST_MIPMAP_NEAREST;
//            m_PointMipmapSampler = SamplerUtils.createSampler(desc);
        }
        getGLContext().setSwapInterval(0);
    }

    @Override
    protected void reshape(int width, int height) {
        if(width <=0 || height <=0)
            return;
        mScene.onResize(width, height);
        mRenderer.onResize(width, height);
    }

    @Override
    public void display() {
        mScene.updateCamera(m_transformer);

        mRenderer.mFrameNumber = getFrameCount();
        if(mOcclusionEnabled){
            mCulling.newFrame(getFrameCount());
            mCulling.cullingCoarse(mRenderer, mScene);
            mRenderer.render(mScene, true);

            if(mFineOcclusion){
                mCulling.cullingFine(mRenderer, mScene);
                mRenderer.render(mScene, false);
            }
        }else{
            mRenderer.render(mScene, true);
        }

        mRenderer.present();
        GLCheck.checkError();

        if(mShowHZBuffer){
            visualTexture(((HZBOcclusionTester)mCulling).getHZBSlice(mHZBSlice));
        }
    }

    private void visualTexture(Texture2D tex){
        if(tex == null || !mOcclusionEnabled)
            return;

        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
        gl.glDisable(GLenum.GL_DEPTH_TEST);
        gl.glDisable(GLenum.GL_CULL_FACE);
        gl.glDisable(GLenum.GL_BLEND);
        gl.glViewport(getGLContext().width()/2, getGLContext().height()/2, getGLContext().width()/2, getGLContext().height()/2);

        gl.glBindTextureUnit(0, tex.getTexture());
        gl.glBindSampler(0, m_PointSampler);
        mVisualTexture.enable();
        mVisualTexture.setUniforms(0.1f, 100, 0, 1);
        gl.glBindVertexArray(0);
        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
        gl.glBindTextureUnit(0, 0);

        gl.glViewport(0,0, getGLContext().width(), getGLContext().height());
        gl.glBindSampler(0, 0);
    }

    private void buildBaseMesh(){
        QuadricBuilder builder = new QuadricBuilder();
        builder.setXSteps(100).setYSteps(100);
        builder.setPostionLocation(0);
        builder.setNormalLocation(1);
        builder.setTexCoordLocation(2);
        builder.setDrawMode(DrawMode.FILL);
        builder.setCenterToOrigin(true);
        mSphereVao = new QuadricMesh(builder, new QuadricSphere(1)).getModel().genVAO();
        mCubeVao = ModelGenerator.genCube(2, true, true, false).genVAO();
    }

    private void buildModles(){
        Model model = new Model();
        model.mMaterial = new Material();
        model.mMaterial.mColor.set(0.7f, 0.8f, 0.6f, 1);

        for (int i = 0; i < 1000; i++){
            float rnd = Numeric.random();
            MeshType type  = rnd < 0.5 ? MeshType.Cube : MeshType.Sphere;
            Mesh mesh = new Mesh();
            mesh.mVao = type == MeshType.Cube ? mCubeVao : mSphereVao;
            mesh.mType = type;
            mesh.count = 1;
            mesh.mAABB.set(new Vector3f(-1,-1,-1),new Vector3f(1,1,1));
            mesh.mWorld.translate(Numeric.random(-10,10),Numeric.random(-10,10),Numeric.random(-10,10));
            BoundingBox.transform(mesh.mWorld, mesh.mAABB, mesh.mAABB);

            model.mMeshes.add(mesh );
        }

        mScene.mModels.add(model);
    }
}
