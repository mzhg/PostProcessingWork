package assimp.importer.cob;

import java.util.ArrayList;
import java.util.List;

/** Represents a master COB scene, even if we loaded just a single COB file */
final class COBScene {

	final List<COBNode> nodes = new ArrayList<COBNode>();
	final List<COBMaterial> materials = new ArrayList<COBMaterial>();
	// becomes *0 later
	COBBitmap thumbnail;
}
