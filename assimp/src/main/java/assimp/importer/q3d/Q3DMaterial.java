package assimp.importer.q3d;

import org.lwjgl.util.vector.Vector3f;

final class Q3DMaterial {

	String name;
	final Vector3f ambient = new Vector3f();
	final Vector3f diffuse = new Vector3f(0.6f, 0.6f, 0.6f);
	final Vector3f specular = new Vector3f();
	float transparency;
	int texIdx;
}
