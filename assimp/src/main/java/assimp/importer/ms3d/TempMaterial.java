package assimp.importer.ms3d;

import org.lwjgl.util.vector.Vector4f;

final class TempMaterial extends TempComment{

	// again, add an extra 0 character to all strings -
	final byte[] name = new byte[32];
	final byte[] texture = new byte[128];
	final byte[] alphamap = new byte[128];

//	aiColor4D diffuse,specular,ambient,emissive;
	final Vector4f diffuse = new Vector4f();
	final Vector4f specular = new Vector4f();
	final Vector4f ambient = new Vector4f();
	final Vector4f emissive = new Vector4f();
	float shininess,transparency;
}
