package assimp.importer.blender;

import assimp.common.DefaultLogger;
import assimp.common.Node;

/** Dummy base class for all blender modifiers. Modifiers are reused between imports, so
 *  they should be stateless and not try to cache model data. */
class BlenderModifier {

	/** Check if *this* modifier is active, given a {@link ModifierData} block.*/
	boolean isActive(ModifierData md){ return false;}
	
	void doIt(Node out, ConversionData conv_data, ElemBase orig_modifier, BLEScene in, BLEObject orig_object){
		if(DefaultLogger.LOG_OUT)
			DefaultLogger.warn("This modifier is not supported, skipping: " + orig_modifier.dna_type);
	}
	
}
