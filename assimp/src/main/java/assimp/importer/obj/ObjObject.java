package assimp.importer.obj;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.util.vector.Matrix4f;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

/*public*/ final class ObjObject {

//	enum ObjectType
//	{
	static final int
		ObjType = 0,
		GroupType = 1;
//	};
	
	//!	Object name
	String m_strObjName;
	//!	Transformation matrix, stored in OpenGL format
	final Matrix4f m_Transformation = new Matrix4f();
	//!	All sub-objects referenced by this object
	final List<ObjObject> m_SubObjects = new ArrayList<ObjObject>();
	///	Assigned meshes
	final IntList m_Meshes = new IntArrayList(10);
}
