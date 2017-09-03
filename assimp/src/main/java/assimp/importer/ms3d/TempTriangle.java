package assimp.importer.ms3d;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import assimp.common.AssUtil;

final class TempTriangle {

	final int[] indices = new int[3];
	final Vector3f[] normals = new Vector3f[3];
	final Vector2f[] uv = new Vector2f[3];

	int sg, group;
	
	public TempTriangle() {
		AssUtil.initArray(normals);
		AssUtil.initArray(uv);
	}
}
