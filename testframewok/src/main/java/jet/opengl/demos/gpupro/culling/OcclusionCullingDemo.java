package jet.opengl.demos.gpupro.culling;

import com.nvidia.developer.opengl.app.NvCameraMotionType;
import com.nvidia.developer.opengl.app.NvSampleApp;
import com.nvidia.developer.opengl.models.DrawMode;
import com.nvidia.developer.opengl.models.GLVAO;
import com.nvidia.developer.opengl.models.ModelGenerator;
import com.nvidia.developer.opengl.models.QuadricBuilder;
import com.nvidia.developer.opengl.models.QuadricMesh;
import com.nvidia.developer.opengl.models.QuadricSphere;

import org.lwjgl.util.vector.Vector3f;

import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.util.BoundingBox;
import jet.opengl.postprocessing.util.Numeric;

public final class OcclusionCullingDemo extends NvSampleApp {
    private Renderer mRenderer;
    private Scene mScene;

    private GLVAO mSphereVao;
    private GLVAO mCubeVao;
    @Override
    protected void initRendering() {
        mScene = new Scene();
        buildBaseMesh();
        buildModles();

        mRenderer = new ForwardRenderer();
        mRenderer.onCreate();

        m_transformer.setMotionMode(NvCameraMotionType.FIRST_PERSON);
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

        mRenderer.render(mScene);
        mRenderer.present();
        GLCheck.checkError();
    }

    private void buildBaseMesh(){
        QuadricBuilder builder = new QuadricBuilder();
        builder.setXSteps(50).setYSteps(50);
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
        model.mMaterial.mColor.set(0.7f, 0.8f, 0.6f);

        for (int i = 0; i < 10; i++){
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
