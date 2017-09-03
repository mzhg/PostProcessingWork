package assimp.importer.ac;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

/** Represents an AC3D surface */
final class ACObject {

	// Type description
//	enum Type
//	{
	static final int
		World = 0x0,
		Poly  = 0x1,
		Group = 0x2,
		Light = 0x4;
//	} type;
	
	int type;

	// name of the object
	String name = "";

	// object children
	final List<ACObject> children = new ArrayList<ACObject>();

	// texture to be assigned to all surfaces of the object
	String texture = "";

	// texture repat factors (scaling for all coordinates)
	final Vector2f texRepeat = new Vector2f(1.f, 1.f);
	final Vector2f texOffset = new Vector2f(0.0f, 0.0f);

	// rotation matrix
	final Matrix3f rotation = new Matrix3f();

	// translation vector
	final Vector3f translation = new Vector3f();

	// vertices
	final List<Vector3f> vertices = new ArrayList<Vector3f>();

	// surfaces
	final List<Surface> surfaces = new ArrayList<Surface>();

	// number of indices (= num verts in verbose format)
	int numRefs;

	// number of subdivisions to be performed on the 
	// imported data
	int subDiv;

	// max angle limit for smoothing
	float crease;
}
