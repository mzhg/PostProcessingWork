package assimp.importer.unreal;

/** temporary representation for a material */
final class TempMat {
	// type of mesh
	int type;

	// index of texture
	int tex;

	// number of faces using us
	int numFaces;
	
	public TempMat() {
	}
	
	public TempMat(Triangle in) {
		type = in.mType;
		tex  = in.mTextureNum;
	}
	
	@Override
	public boolean equals(Object obj) {
		TempMat o = (TempMat)obj;
		return type == o.type;
	}
}
