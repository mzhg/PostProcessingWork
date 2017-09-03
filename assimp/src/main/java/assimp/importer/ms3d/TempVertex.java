package assimp.importer.ms3d;

import org.lwjgl.util.vector.Vector3f;

final class TempVertex {

	final Vector3f pos = new Vector3f();
	final int[] bone_id = new int[4];
	int ref_cnt;
	float[] weights = new float[4];
}
