package assimp.importer.d3ds;


/** Float key - quite similar to aiVectorKey and aiQuatKey. Both are in the
C-API, so it would be difficult to make them a template. */
final class FloatKey implements Comparable<FloatKey>{

	double mTime;
	float mValue;
	
	@Override
	public int compareTo(FloatKey o) {
		if(mTime < o.mTime)
			return -1;
		else if(mTime > o.mTime)
			return 1;
		else
			return 0;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(mTime);
		result = prime * result + (int) (temp ^ (temp >>> 32));
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
		FloatKey other = (FloatKey) obj;
		if (Double.doubleToLongBits(mTime) != Double
				.doubleToLongBits(other.mTime))
			return false;
		return true;
	}
}
