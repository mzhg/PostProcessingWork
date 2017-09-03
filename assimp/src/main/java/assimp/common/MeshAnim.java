package assimp.common;

/** Describes vertex-based animations for a single mesh or a group of
 *  meshes. Meshes carry the animation data for each frame in their
 *  aiMesh::mAnimMeshes array. The purpose of aiMeshAnim is to 
 *  define keyframes linking each mesh attachment to a particular
 *  point in time. */
public class MeshAnim implements Copyable<MeshAnim>{

	/** Name of the mesh to be animated. An empty string is not allowed,
	 *  animated meshes need to be named (not necessarily uniquely,
	 *  the name can basically serve as wildcard to select a group
	 *  of meshes with similar animation setup)*/
	public String mName;

	/** Key frames of the animation. May not be null. */
	public MeshKey[] mKeys;
	
	/** Size of the {@link #mKeys} array. Must be 1, at least. */
	public int getNumKeys() { return mKeys != null ? mKeys.length : 0;}

	@Override
	public MeshAnim copy() {
		MeshAnim anim = new MeshAnim();
		anim.mKeys = AssUtil.copyOf(mKeys);
		anim.mName = mName;
		return anim;
	}
}
