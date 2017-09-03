package assimp.common;

/** An animation consists of keyframe data for a number of nodes. For 
 *  each node affected by the animation a separate series of data is given.*/
public class Animation implements Copyable<Animation>{

	/** The name of the animation. If the modeling package this data was 
	 *  exported from does support only a single animation channel, this 
	 *  name is usually empty (length is zero). */
	public String mName;

	/** Duration of the animation in ticks.  */
	public double mDuration = -1;

	/** Ticks per second. 0 if not specified in the imported file */
	public double mTicksPerSecond;

	/* The number of bone animation channels. Each channel affects
	 *  a single node. */
//	public int mNumChannels;

	/** The node animation channels. Each channel affects a single node. 
	 *  The array is mNumChannels in size. */
	public NodeAnim[] mChannels;

	/* The number of mesh animation channels. Each channel affects
	 *  a single mesh and defines vertex-based animation. */
//	public int mNumMeshChannels;

	/** The mesh animation channels. Each channel affects a single mesh. 
	 *  The array is mNumMeshChannels in size. */
	public MeshAnim[] mMeshChannels;
	
	/** The number of bone animation channels. Each channel affects
	 *  a single node. */
	public int getNumChannels() {return mChannels != null ? mChannels.length : 0;}
	
	/** The number of mesh animation channels. Each channel affects
	 *  a single mesh and defines vertex-based animation. */
	public int getNumMeshChannels() { return mMeshChannels != null ? mMeshChannels.length : 0;}

	@Override
	public Animation copy() {
		Animation anim = new Animation();
		anim.mChannels = AssUtil.copyOf(mChannels);
		anim.mDuration = mDuration;
		anim.mMeshChannels = AssUtil.copyOf(mMeshChannels);
		anim.mName = mName;
		anim.mTicksPerSecond = mTicksPerSecond;
		return anim;
	}
}
