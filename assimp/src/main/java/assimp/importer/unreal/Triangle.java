package assimp.importer.unreal;

final class Triangle {

	final short[] mVertex = new short[3]; // Vertex indices
	byte mType;                           // James' Mesh Type
	byte mColor;                          // Color for flat and Gourand Shaded
	final byte[][] mTex = new byte[3][2]; // Texture UV coordinates
	byte mTextureNum;                     // Source texture offset
	byte mFlags;						  // Unreal Mesh Flags (unused)
	
	int matIndex;
}
