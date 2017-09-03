package assimp.importer.blender;

/** Import statistics, i.e. number of file blocks read*/
class Statistics {

	/** total number of fields we read */
	int fields_read;

	/** total number of resolved pointers */
	int pointers_resolved;

	/** number of pointers resolved from the cache */
	int cache_hits;

	/* number of blocks (from  FileDatabase::entries) 
	  we did actually read from. */
	// int blocks_read;

	/** objects in FileData::cache */
	int cached_objects;
}
