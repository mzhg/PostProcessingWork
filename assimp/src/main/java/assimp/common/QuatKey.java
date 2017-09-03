package assimp.common;

import org.lwjgl.util.vector.Quaternion;

/** A time-value pair specifying a rotation for the given time. 
 *  Rotations are expressed with quaternions. */
public class QuatKey implements Comparable<QuatKey>, Copyable<QuatKey>{
	
	public static final int SIZE = 20;

	/** The time of this key */
	public float mTime;
	
	/** The value of this key */
	public final Quaternion mValue = new Quaternion();
	
	public QuatKey() {
	}

	public QuatKey(float time, Quaternion value) {
		this.mTime = time;
		this.mValue.set(value);
	}

	@Override
	public int compareTo(QuatKey o) {
		if(mTime < o.mTime)
			return -1;
		else if(mTime > o.mTime)
			return 1;
		else
			return 0;
	}

	@Override
	public QuatKey copy() { return new QuatKey(mTime, mValue);}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Float.floatToIntBits(mTime);
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		QuatKey other = (QuatKey) obj;
		if (Float.floatToIntBits(mTime) != Float.floatToIntBits(other.mTime))
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "QuatKey [mTime=" + mTime + ", mValue=" + mValue + "]";
	}
}
