package assimp.importer.ase;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

/** Represents an ASE file node. Base class for mesh, light and cameras */
class BaseNode {

	static int iCnt = 0;
	static final int LIGHT = 0, CAMERA = 1, MESH = 2, DUMMY = 3;
	
	final int mType;
	
	//! Name of the mesh
	String mName;

	//! Name of the parent of the node
	//! "" if there is no parent ...
	String mParent;

	//! Transformation matrix of the node
	final Matrix4f mTransform = new Matrix4f();

	//! Target position (target lights and cameras)
	final Vector3f mTargetPosition = new Vector3f();

	//! Specifies which axes transformations a node inherits
	//! from its parent ...
	final InheritanceInfo inherit = new InheritanceInfo();

	//! Animation channels for the node
	final ASEAnimation mAnim = new ASEAnimation();

	//! Needed for lights and cameras: target animation channel
	//! Should contain position keys only.
	final ASEAnimation mTargetAnim = new ASEAnimation();

	boolean mProcessed;
	
	public BaseNode(int type) {
		mType = type;
		mName = String.format("UNNAMED_%i",iCnt++);
		mTargetPosition.x = Float.NaN;
	}
}
