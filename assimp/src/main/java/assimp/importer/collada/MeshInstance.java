package assimp.importer.collada;

import java.util.HashMap;
import java.util.Map;

/** A reference to a mesh inside a node, including materials assigned to the various subgroups.
 * The ID refers to either a mesh or a controller which specifies the mesh
 */
final class MeshInstance {

	/** ID of the mesh or controller to be instanced */
	String mMeshOrController;
	/** Map of materials by the subgroup ID they're applied to */
	Map<String, SemanticMappingTable> mMaterials = new HashMap<>();
}
