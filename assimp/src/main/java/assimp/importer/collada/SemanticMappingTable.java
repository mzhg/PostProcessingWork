package assimp.importer.collada;

import java.util.HashMap;
import java.util.Map;

/** Table to map from effect to vertex input semantics */
final class SemanticMappingTable {

	/** Name of material */
	String mMatName;
	
	final Map<String, InputSemanticMapEntry> mMap = new HashMap<>();
}
