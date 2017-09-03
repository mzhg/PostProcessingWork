package assimp.importer.ac;

import org.lwjgl.util.vector.Vector3f;

/** Represents an AC3D material */
final class ACMaterial {

	// base color of the material
	final Vector3f rgb = new Vector3f(0.6f, 0.6f, 0.6f);

	// ambient color of the material
	final Vector3f amb = new Vector3f();

	// emissive color of the material
	final Vector3f emis = new Vector3f();

	// specular color of the material
	final Vector3f spec = new Vector3f(1.f, 1.f, 1.f);

	// shininess exponent
	float shin = 0;

	// transparency. 0 == opaque
	float trans = 0;

	// name of the material. optional.
	String name = "";
}
