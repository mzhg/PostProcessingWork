package assimp.importer.xgl;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

final class TempFace {

	final Vector3f pos = new Vector3f();
	final Vector3f normal = new Vector3f();
	final Vector2f uv = new Vector2f();
	
	boolean has_normal;
	boolean has_uv;
	
	boolean has_uv() { return has_uv;}
	boolean has_normal() { return has_normal;}
}
