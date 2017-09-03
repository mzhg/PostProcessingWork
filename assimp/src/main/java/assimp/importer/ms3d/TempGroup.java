package assimp.importer.ms3d;

import it.unimi.dsi.fastutil.ints.IntArrayList;

final class TempGroup extends TempComment{

	final byte[] name = new byte[32]; // +0
	final IntArrayList triangles = new IntArrayList();
	int mat; // 0xff is no material
}
