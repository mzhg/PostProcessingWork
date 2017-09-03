package assimp.importer.dxf;

import org.lwjgl.util.vector.Vector3f;

//reference to a BLOCK. Specifies its own coordinate system.
final class InsertBlock {

	final Vector3f pos = new Vector3f();
	final Vector3f scale = new Vector3f(1,1,1);
	float angle;
	String name;
}
