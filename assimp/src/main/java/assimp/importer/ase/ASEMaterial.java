package assimp.importer.ase;

import java.util.ArrayList;
import java.util.List;

import assimp.common.Material;
import assimp.importer.d3ds.D3DSMaterial;

/** Helper structure representing an ASE material */
final class ASEMaterial extends D3DSMaterial{

	/** Contains all sub materials of this material */
	final List<ASEMaterial> avSubMaterials = new ArrayList<ASEMaterial>();
	
	Material pcInstance = null;
	/** Can we remove this material? */
	boolean bNeed = false;
}
