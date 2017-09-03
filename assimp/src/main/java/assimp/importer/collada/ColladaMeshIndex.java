package assimp.importer.collada;

final class ColladaMeshIndex {

	String mMeshID;
	int mSubMesh;
	String mMaterial;
	
	public ColladaMeshIndex(String pMeshID, int pSubMesh, String pMaterial) {
		mMeshID = pMeshID;
		mSubMesh = pSubMesh;
		mMaterial = pMaterial;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((mMaterial == null) ? 0 : mMaterial.hashCode());
		result = prime * result + ((mMeshID == null) ? 0 : mMeshID.hashCode());
		result = prime * result + mSubMesh;
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
		ColladaMeshIndex other = (ColladaMeshIndex) obj;
		if (mMaterial == null) {
			if (other.mMaterial != null)
				return false;
		} else if (!mMaterial.equals(other.mMaterial))
			return false;
		if (mMeshID == null) {
			if (other.mMeshID != null)
				return false;
		} else if (!mMeshID.equals(other.mMeshID))
			return false;
		if (mSubMesh != other.mSubMesh)
			return false;
		return true;
	}
}
