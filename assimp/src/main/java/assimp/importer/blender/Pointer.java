package assimp.importer.blender;

/** Represents a generic pointer to a memory location, which can be either 32
 *  or 64 bits. These pointers are loaded from the BLEND file and finally
 *  fixed to point to the real, converted representation of the objects 
 *  they used to point to.*/
final class Pointer {

	long val;

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (val ^ (val >>> 32));
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
//		if (obj == null)
//			return false;
//		if (getClass() != obj.getClass())
//			return false;
		Pointer other = (Pointer) obj;
		if (val != other.val)
			return false;
		return true;
	}
	
	
}
