package assimp.importer.blender;

/** The only purpose of this structure is to feed a virtual dtor into its
 *  descendents. It serves as base class for all data structure fields. */
class ElemBase {

	/** Type name of the element. The type <p>
	 * string points is the `c_str` of the `name` attribute of the 
	 * corresponding `Structure`, that is, it is only valid as long 
	 * as the DNA is not modified. The dna_type is only set if the
	 * data type is not static, i.e. a boost::shared_ptr<ElemBase>
	 * in the scene description would have its type resolved 
	 * at runtime, so this member is always set. */
	String dna_type;
	
	void zero(){}
}
