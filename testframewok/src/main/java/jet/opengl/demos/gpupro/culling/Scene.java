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
    final StackInt mMeshModels = new StackInt();

    // Camera attributes
    final Matrix4f mView = new Matrix4f();
    final Matrix4f mProj = new Matrix4f();
    final Vector3f mEye = new Vector3f();

    final Matrix4f mPrevView = new Matrix4f();
//    final Matrix4f mPrevProj = new Matrix4f();
//    final Recti mViewport = new Recti();

    int mViewWidth, mViewHeight;

    void update(){
        mExpandMeshes.clear();
        mExpandMeshVisible.clear();
        mMaterials.clear();
        mMeshMaterials.clear();
        mMeshModels.clear();

        int modelIndex = 0;
        for(Model model : mModels){
            int index = addMaterial(model.mMaterial);
            for(Mesh mesh : model.mMeshes){
                mExpandMeshes.add(mesh);
                mMeshMaterials.push(index);
                mMeshModels.push(modelIndex);
            }

            modelIndex++;
        }

        mExpandMeshVisible.resize(mExpandMeshes.size());
        mExpandMeshVisible.fill(0, mExpandMeshVisible.size(), true);
    }

    private final int addMaterial(Material material){
        int idx = mMaterials.indexOf(material);
        if(idx < 0){
            mMaterials.add(material);
            idx = mMaterials.size() - 1;
        }

        return idx;
    }

    void updateCamera(NvInputTransformer camera){
        mPrevView.load(mView);

        camera.getModelViewMat(mView);
        Matrix4f.decompseRigidMatrix(mView, mEye, null, null);

        mExpandMeshVisible.fill(0, mExpandMeshVisible.size(), true);

        // camera culling

    }

    void onResize(int width, int height){
        Matrix4f.perspective(60, (float)width/height, 0.1f, 1000, mProj);

        mViewWidth = width;
        mViewHeight = height;
    }
}
