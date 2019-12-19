package jet.opengl.demos.gpupro.culling;

import com.nvidia.developer.opengl.app.NvInputTransformer;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.util.ArrayList;
import java.util.List;

import jet.opengl.postprocessing.util.StackBool;
import jet.opengl.postprocessing.util.StackInt;

final class Scene {

    // The model sources.
    final List<Model> mModels = new ArrayList<>();

    // processed attributes for fast rendering.
    final List<Mesh> mExpandMeshes = new ArrayList<>();
    final StackBool mExpandMeshVisible = new StackBool();
    final List<Material> mMaterials = new ArrayList<>();
    final StackInt mMeshMaterials = new StackInt();  // The material id for the expand meshes.

    // Camera attributes
    final Matrix4f mView = new Matrix4f();
    final Matrix4f mProj = new Matrix4f();
    final Vector3f mEye = new Vector3f();
//    final Recti mViewport = new Recti();

    void updateCamera(NvInputTransformer camera){
        camera.getModelViewMat(mView);
        Matrix4f.decompseRigidMatrix(mView, mEye, null, null);
    }

    void onResize(int width, int height){
        Matrix4f.perspective(60, (float)width/height, 0.1f, 1000, mProj);
    }
}
