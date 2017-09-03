package assimp.importer.fbx;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import assimp.common.Pair;

/** FBX data entity that consists of a 'scope', a collection
 *  of not necessarily unique #Element instances.<p>
 *
 *  Example:
 *  <pre>
 *    GlobalSettings:  {
 *        Version: 1000
 *        Properties70: 
 *        [...]
 *    }
 *  </pre>  */
final class Scope {

	private Map<String, LinkedList<Element>> elements;
	
	public Scope(Parser parser, boolean topLevel) {
	}
	
	Element get(String index){
		LinkedList<Element> list = elements != null ? elements.get(index) : null;
		return list != null ? list.peekFirst() : null;
	}
	
	LinkedList<Element> getCollection(String index){
		return elements.get(index);
	}
	
//	Pair<K, V>
	
	Map<String, LinkedList<Element>> elements() { return elements;}
}
