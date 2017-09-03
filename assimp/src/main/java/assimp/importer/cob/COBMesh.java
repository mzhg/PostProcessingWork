package assimp.importer.cob;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.nio.FloatBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/** COB Mesh data structure */
final class COBMesh extends COBNode{

//	enum DrawFlags {
	static final int
		SOLID = 0x1,
		TRANS = 0x2,
		WIRED = 0x4,
		BBOX  = 0x8,
		HIDE  = 0x10;
	
	// vertex elements
	FloatBuffer texture_coords;  //2d
	FloatBuffer vertex_positions; //3d

	// face data
	final List<COBFace> faces = new ArrayList<COBFace>();

	// misc. drawing flags
	int draw_flags;

	// used during resolving
//	typedef std::deque<Face*> FaceRefList;
//	typedef std::map< unsigned int,FaceRefList > TempMap;
//	TempMap temp_map;
	final Int2ObjectMap<ArrayDeque<COBFace>> temp_map = new Int2ObjectOpenHashMap<ArrayDeque<COBFace>>();
	
	int getVertexCount() { return vertex_positions != null ? vertex_positions.remaining()/3 : 0;}
	int getTexCoordCount() { return texture_coords != null ? texture_coords.remaining()/2 : 0;}
	public COBMesh() {
		super(TYPE_MESH);
		draw_flags = SOLID;
	}
	
}
