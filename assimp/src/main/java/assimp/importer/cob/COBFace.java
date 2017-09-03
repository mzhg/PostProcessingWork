package assimp.importer.cob;

import java.util.ArrayList;
import java.util.List;

/** COB Face data structure */
final class COBFace {

	// intentionally uninitialized
	int material, flags;
	final List<VertexIndex> indices = new ArrayList<>();
}
