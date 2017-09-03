package assimp.importer.blender;

import java.util.ArrayList;
import java.util.List;

import assimp.common.AssimpConfig;
import assimp.common.StreamReader;

/** Memory representation of a full BLEND file and all its dependencies. The
 *  output aiScene is constructed from an instance of this data structure. */
final class FileDatabase {

	// publicly accessible fields
	boolean i64bit;
	boolean little;

	final DNA dna = new DNA();
	StreamReader reader;
	final ArrayList<FileBlockHead> entries = new ArrayList<FileBlockHead>();
	final Statistics _stats;
	
	final ObjectCache<List<ElemBase>> _cacheArrays;
	final ObjectCache<ElemBase> _cache;
	int next_cache_idx;
	
	public FileDatabase() {
		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS)
			_stats = new Statistics();
		else
			_stats = null;
		
		_cacheArrays = new ObjectCache<>(this);
		_cache = new ObjectCache<>(this);
	}
	
	ObjectCache<ElemBase> cache(){
		return _cache;
	}
	
	ObjectCache<List<ElemBase>> cacheArray(){
		return _cacheArrays;
	}
	
	Statistics stats() {
		return _stats;
	}
}
