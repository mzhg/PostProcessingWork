package jet.opengl.renderer.assimp;

import org.lwjgl.assimp.AIBone;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.util.List;
import java.util.Map;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.util.BoundingBox;
import jet.opengl.postprocessing.util.Pair;
import jet.opengl.postprocessing.util.StackInt;

public class AssimpMesh {
    final StackInt mBones = new StackInt();
    int mMaterilIndex;
    private BufferGL mVertexBuffer;
    private BufferGL mIndexBuffer;
    private Runnable mInputLayout;
    private BufferGL mBlendShapeBuffer;
    private int mPrimitive;
    private int mIndiceFormat;
    private int mVertexStride;
    private int mIndiceCount;
    private int mVertexCount;
    private int mWeightCount;

    private float mUVScale;  // for the skin rendering.
    final Matrix4f mNodeTransform = new Matrix4f();
    private AssimpAsset mAsset;
    private List<BlendShapeDeformer> mBlendShape;

    private int mNumIndicesConvertOriginal;  //
    private int[] mIndicesConvertOriginal;
    private Vector3f[] mVerticesPosData;  // The transformed vertex informations, only support the fbx files.
    private final Vector3f mMeshCenter = new Vector3f();  // for the BlendShape, only support the fbx files.

    private AssimpMesh mMeshParent;
    private List<AssimpMesh> mMeshChildren;

    private final BoundingBox mBoundingBox = new BoundingBox();

    private boolean mEnabled = true;
    String mName;

    private final MeshFormat mMeshFormat = new MeshFormat();

    public AssimpMesh(AssimpAsset asset){
        mAsset = asset;
    }

    public void createMesh(AIMesh mesh, Map<String, Pair<AIBone, Integer>> paresedBones, BoundingBox boundingBox, boolean generateCurvature){

    }

    public void createMesh(List<AIMesh> mesh, Map<String, Pair<AIBone, Integer>> paresedBones, BoundingBox boundingBox, boolean generateCurvature){

    }
}
