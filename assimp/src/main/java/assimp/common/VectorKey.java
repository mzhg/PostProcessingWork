package assimp.common;

import org.lwjgl.util.vector.Vector3f;

/** A time-value pair specifying a certain 3D vector for the given time. */
public class VectorKey implements Comparable<VectorKey>, Copyable<VectorKey>{
	
	public static final int SIZE = 16;

	/** The time of this key */
	public float mTime;
	
	/** The value of this key */
	public final Vector3f mValue = new Vector3f();
	
	public VectorKey() {
	}

	public VectorKey(float mTime, Vector3f value) {
		this.mTime = mTime;
		this.mValue.set(value);
	}

	@Override
	public int compareTo(VectorKey o) {
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
		result = prime * result + Float.floatToIntBits(mTime);
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
		VectorKey other = (VectorKey) obj;
		if (Float.floatToIntBits(mTime) != Float.floatToIntBits(other.mTime))
			return false;
		return true;
	}

	@Override
	public VectorKey copy() {
		return new VectorKey(mTime, mValue);
	}

	@Override
	public String toString() {
		return "VectorKey [mTime=" + mTime + ", mValue=" + mValue + "]";
	}
	
}
