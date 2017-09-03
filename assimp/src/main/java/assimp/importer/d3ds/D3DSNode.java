package assimp.importer.d3ds;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.util.vector.Vector3f;

import assimp.common.QuatKey;
import assimp.common.VectorKey;

/** Helper structure to represent a 3ds file node */
final class D3DSNode {

	private static int iCnt = 0;
	//! Pointer to the parent node
	D3DSNode mParent;

	//! Holds all child nodes
	final List<D3DSNode> mChildren = new ArrayList<D3DSNode>();

	//! Name of the node
	String mName;

	//! InstanceNumber of the node
	int mInstanceNumber;

	//! Dummy nodes: real name to be combined with the $$$DUMMY 
	String mDummyName;

	//! Position of the node in the hierarchy (tree depth)
	short mHierarchyPos;

	//! Index of the node
	short mHierarchyIndex;

	//! Rotation keys loaded from the file
	final List<QuatKey> aRotationKeys = new ArrayList<QuatKey>(20);

	//! Position keys loaded from the file
	final List<VectorKey> aPositionKeys = new ArrayList<>(20);

	//! Scaling keys loaded from the file
	final List<VectorKey> aScalingKeys = new ArrayList<VectorKey>(20);

	// For target lights (spot lights and directional lights):
	// The position of the target
	final List<VectorKey> aTargetPositionKeys = new ArrayList<VectorKey>();

	// For cameras: the camera roll angle
	final List<FloatKey> aCameraRollKeys = new ArrayList<FloatKey>();

	//! Pivot position loaded from the file
	final Vector3f vPivot = new Vector3f();

	//instance count, will be kept only for the first node
	int mInstanceCount = 1;
	
	public D3DSNode() {
		mName = String.format("UNNAMED_%i",iCnt++);
	}
	
	//! Add a child node, setup the right parent node for it
	//! \param pc Node to be 'adopted'
	D3DSNode add(D3DSNode pc)
	{
		mChildren.add(pc);
		pc.mParent = this;
		return this;
	}
}
