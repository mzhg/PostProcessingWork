package assimp.importer.blender;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import assimp.common.AssimpConfig;

/** The object cache - all objects addressed by pointers are added here. This
 *  avoids circular references and avoids object duplication. */
final class ObjectCache<TOUT> {

	final List<Map<Pointer, TOUT>> caches = new ArrayList<>(64);
	final FileDatabase db;
	
	public ObjectCache(FileDatabase db) {
		this.db = db;
	}
	
	// --------------------------------------------------------
	/** Check whether a specific item is in the cache.
	 *  @param s Data type of the item
	 *  @param out Output pointer. Unchanged if the
	 *   cache doens't know the item yet.
	 *  @param ptr Item address to look for. */
	TOUT get (Structure s, /*TOUT out, */Pointer ptr){
		if(s.cache_idx == /*static_cast<size_t>*/(-1)) {
			s.cache_idx = db.next_cache_idx++;
//			caches.resize(db.next_cache_idx);
			for(int i = db.next_cache_idx - caches.size(); i > 0; i--){
				caches.add(new HashMap<Pointer, TOUT>());
			}
			return null;
		}

//		typename StructureCache::const_iterator it = caches[s.cache_idx].find(ptr);
//		if (it != caches[s.cache_idx].end()) {
//			out = boost::static_pointer_cast<T>( (*it).second );
//
//	#ifndef ASSIMP_BUILD_BLENDER_NO_STATS
//			++db.stats().cache_hits;
//	#endif
		
		TOUT out = caches.get(s.cache_idx).get(ptr);
		if(out != null){
			if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS)
				++db.stats().cache_hits;
		}
		
		return out;
	}

	// --------------------------------------------------------
	/** Add an item to the cache after the item has 
	 * been fully read. Do not insert anything that
	 * may be faulty or might cause the loading
	 * to abort. 
	 *  @param s Data type of the item
	 *  @param out Item to insert into the cache
	 *  @param ptr address (cache key) of the item. */
	void set (Structure s, TOUT out, Pointer ptr){
		if(s.cache_idx == /*static_cast<size_t>*/(-1)) {
			s.cache_idx = db.next_cache_idx++;
//			caches.resize(db.next_cache_idx);
			for(int i = db.next_cache_idx - caches.size(); i > 0; i--){
				caches.add(new HashMap<Pointer, TOUT>());
			}
		}
//		caches[s.cache_idx][ptr] = boost::static_pointer_cast<ElemBase>( out ); 
		caches.get(s.cache_idx).put(ptr, out);

//	#ifndef ASSIMP_BUILD_BLENDER_NO_STATS
//		++db.stats().cached_objects;
//	#endif
		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS)
			++db.stats().cache_hits;
	}
	
}
