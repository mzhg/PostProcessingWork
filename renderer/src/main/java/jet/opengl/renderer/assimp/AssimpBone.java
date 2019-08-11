package jet.opengl.renderer.assimp;

import org.lwjgl.util.vector.Matrix4f;

import jet.opengl.postprocessing.util.StackInt;

public class AssimpBone {
    int mName;
    final Matrix4f mOffset = new Matrix4f();
    final Matrix4f mTransform = new Matrix4f();

    int mParent;
    final StackInt mChildren = new StackInt();

    public AssimpBone(int name){
        mName = name;
        mParent = -1;
    }
}
