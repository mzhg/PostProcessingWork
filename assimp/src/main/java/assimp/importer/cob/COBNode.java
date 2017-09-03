package assimp.importer.cob;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.util.vector.Matrix4f;

/** A node in the scenegraph */
class COBNode extends ChunkInfo{

//	enum Type {
	static final int 
		TYPE_MESH = 0,TYPE_GROUP = 1,TYPE_LIGHT = 2,TYPE_CAMERA = 3,TYPE_BONE = 4;
//	};
	
	int type;

	// used during resolving
//	typedef std::deque<const Node*> ChildList;
//	mutable ChildList temp_children;
	final List<COBNode> temp_children = new ArrayList<COBNode>();
	// unique name
	String name;

	// local mesh transformation, null equals identity.
	Matrix4f transform;

	// scaling for this node to get to the metric system
	float unit_scale;

	COBNode(int type) { 
		this.type = (type);
		unit_scale = (1.f);
	}
	
}
