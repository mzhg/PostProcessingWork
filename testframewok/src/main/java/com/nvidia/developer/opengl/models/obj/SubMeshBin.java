package com.nvidia.developer.opengl.models.obj;

/** Used to define a mesh that uses a single material and an array of faces */
final class SubMeshBin extends SubMesh{

	@Override
	public boolean hasNormals() { return getNormalOffset() > 0;}

	@Override
	public boolean hasTexCoords() { return getTexCoordOffset() > 0;}

	@Override
	public boolean hasTangents() { return getTangentOffset() > 0;}

	@Override
	public boolean hasColors() { return getColorOffset() > 0;}

	@Override
	public boolean hasBoneWeights() { return getBoneIndexOffset() > 0 ;}

}
