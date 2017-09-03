package assimp.importer.cob;

/** COB chunk header information */
class ChunkInfo {

	static final int NO_SIZE = -1;
	// Id of this chunk, unique within file
	int id;

	// and the corresponding parent
	int parent_id;

	// version. v1.23 becomes 123
	int version;

	// chunk size in bytes, only relevant for binary files
	// NO_SIZE is also valid.
	int size = NO_SIZE;
	
	void set(ChunkInfo info){
		id = info.id;
		parent_id = info.parent_id;
		version = info.version;
		size = info.size;
	}
}
