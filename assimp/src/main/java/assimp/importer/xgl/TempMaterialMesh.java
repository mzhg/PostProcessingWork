package assimp.importer.xgl;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.nio.FloatBuffer;

final class TempMaterialMesh {

	FloatBuffer positions,normals;  //3d
	FloatBuffer uvs; 				//2d
	
	IntArrayList vcounts;
	int pflags;
	int matid;
}
