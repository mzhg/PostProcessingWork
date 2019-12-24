package jet.opengl.demos.gpupro.culling;

import com.nvidia.developer.opengl.models.GLVAO;

import java.util.ArrayList;
import java.util.List;

import jet.opengl.postprocessing.util.Pool;

final class InstanceMesh implements Pool.Poolable {

    GLVAO mMesh;
    MeshType mType;

    boolean mCoraseRendered;
    boolean mFineRendered;

    final List<InstanceData> mData = new ArrayList<>();

    @Override
    public void reset() {
        mMesh = null;
        mCoraseRendered = false;
        mFineRendered = false;

        Scene.gTransfomCache.freeAll(mData);
        mData.clear();
    }
}
