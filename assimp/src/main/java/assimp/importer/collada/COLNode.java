package assimp.importer.collada;

import java.util.ArrayList;

/** A node in a scene hierarchy */
final class COLNode {

	String mName;
	String mID;
	String mSID;
	COLNode mParent;
	final ArrayList<COLNode> mChildren = new ArrayList<>();

	/** Operations in order to calculate the resulting transformation to parent. */
	final ArrayList<Transform> mTransforms = new ArrayList<>();

	/** Meshes at this node */
	final ArrayList<MeshInstance> mMeshes = new ArrayList<>();    

	/** Lights at this node */
	final ArrayList<LightInstance> mLights = new ArrayList<>();  

	/** Cameras at this node */
	final ArrayList<CameraInstance> mCameras = new ArrayList<>(); 

	/** Node instances at this node */
	final ArrayList<NodeInstance> mNodeInstances = new ArrayList<>();

	/** Rootnodes: Name of primary camera, if any */
	String mPrimaryCamera;
}
