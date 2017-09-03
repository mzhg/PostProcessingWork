package assimp.common;

/** Binds a anim mesh to a specific point in time. */
public class MeshKey implements Comparable<MeshKey>, Copyable<MeshKey>{

	/** The time of this key */
	public double mTime;
	
	/** Index into the {@link Mesh#mAnimMeshes} array of the 
	 *  mesh coresponding to the {@link MeshAnim} hosting this
	 *  key frame. The referenced anim mesh is evaluated
	 *  according to the rules defined in the docs for {@link AnimMesh}.*/
	public int mValue;
	
	public MeshKey() {
	}
	
	public MeshKey(double time, int value) {
		mTime = time;
		mValue = value;
	}
	
	@Override
	public int compareTo(MeshKey o) {
		if(mTime < o.mTime)
			return -1;
		else if(mTime > o.mTime)
			return 1;
		else
			return 0;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + mValue;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MeshKey other = (MeshKey) obj;
		if (mValue != other.mValue)
			return false;
		return true;
	}

	@Override
	public MeshKey copy() {
		return new MeshKey(mTime, mValue);
	}
	
}
