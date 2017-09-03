package assimp.common;

/** Describes the animation of a single node. The name specifies the 
 *  bone/node which is affected by this animation channel. The keyframes
 *  are given in three separate series of values, one each for position, 
 *  rotation and scaling. The transformation matrix computed from these
 *  values replaces the node's original transformation matrix at a
 *  specific time.<p>
 *  This means all keys are absolute and not relative to the bone default pose.
 *  The order in which the transformations are applied is
 *  - as usual - scaling, rotation, translation.<p>
 *
 *  <b>note</b> All keys are returned in their correct, chronological order.
 *  Duplicate keys don't pass the validation step. Most likely there
 *  will be no negative time values, but they are not forbidden also ( so 
 *  implementations need to cope with them! ) */
public class NodeAnim implements Copyable<NodeAnim>{

	/** The name of the node affected by this animation. The node 
	 *  must exist and it must be unique.*/
	public String mNodeName;

	/** The position keys of this animation channel. Positions are 
	 * specified as 3D vector. The array is mNumPositionKeys in size.
	 *
	 * If there are position keys, there will also be at least one
	 * scaling and one rotation key.*/
	public VectorKey[] mPositionKeys;

	/** The rotation keys of this animation channel. Rotations are 
	 *  given as quaternions,  which are 4D vectors. The array is 
	 *  mNumRotationKeys in size.<p>
	 *
	 * If there are rotation keys, there will also be at least one
	 * scaling and one position key. */
	public QuatKey[] mRotationKeys;

	/** The scaling keys of this animation channel. Scalings are 
	 *  specified as 3D vector. The array is mNumScalingKeys in size.
	 *
	 * If there are scaling keys, there will also be at least one
	 * position and one rotation key.*/
	public VectorKey[] mScalingKeys;


	/** Defines how the animation behaves before the first
	 *  key is encountered.<p>
	 *
	 *  The default value is aiAnimBehaviour_DEFAULT (the original
	 *  transformation matrix of the affected node is used).*/
	public AnimBehaviour mPreState = AnimBehaviour.aiAnimBehaviour_DEFAULT;

	/** Defines how the animation behaves after the last 
	 *  key was processed.<p>
	 *
	 *  The default value is aiAnimBehaviour_DEFAULT (the original
	 *  transformation matrix of the affected node is taken).*/
	public AnimBehaviour mPostState = AnimBehaviour.aiAnimBehaviour_DEFAULT;
	
	/**Return the number of position keys */
	public int getNumPositionKeys(){ return mPositionKeys != null ? mPositionKeys.length : 0;}
	
	/**Return the number of rotation keys */
	public int getNumRotationKeys(){ return mRotationKeys != null ? mRotationKeys.length : 0;}
	
	/** Return the number of scaling keys */
	public int getNumScalingKeys() { return mScalingKeys != null ? mScalingKeys.length : 0;}

	@Override
	public NodeAnim copy() {
		NodeAnim anim = new NodeAnim();
		anim.mNodeName = mNodeName;
		anim.mPositionKeys = AssUtil.copyOf(mPositionKeys);
		anim.mPostState = mPostState;
		anim.mPreState = mPreState;
		anim.mRotationKeys = AssUtil.copyOf(mRotationKeys);
		anim.mScalingKeys = AssUtil.copyOf(mScalingKeys);
		return anim;
	}
}
