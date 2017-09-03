package assimp.common;

import org.lwjgl.util.vector.Matrix4f;

/** A single bone of a mesh. <p>
 * A bone has a name by which it can be found in the frame hierarchy and by
 *  which it can be addressed by animations. In addition it has a number of 
 */
public class Bone implements Copyable<Bone>{

	/** The name of the bone. */ 
	public String mName;

	/* The number of vertices affected by this bone<p>
	 The maximum value for this member is #AI_MAX_BONE_WEIGHTS.*/
//	public int mNumWeights;

	/** The vertices affected by this bone */
	public VertexWeight[] mWeights;

	/** Matrix that transforms from mesh space to bone space in bind pose */
	public final Matrix4f mOffsetMatrix =  new Matrix4f();
	
	public Bone() {
	}

	public Bone(String name, VertexWeight[] weights, Matrix4f offsetMatrix) {
		this.mName = name;
		this.mWeights = weights;
		this.mOffsetMatrix.load(offsetMatrix);
	}
	
	/** The number of vertices affected by this bone<p>
	 The maximum value for this member is {@link ObjMesh.AI_MAX_BONE_WEIGHTS}.*/
	public int getNumWeights(){
		return mWeights != null ? mWeights.length : 0;
	}

	@Override
	public Bone copy() {
		return new Bone(mName, AssUtil.copyOf(mWeights), mOffsetMatrix);
	}
	
	
}
