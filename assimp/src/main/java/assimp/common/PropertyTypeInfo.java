package assimp.common;

/** A very primitive RTTI system for the contents of material 
 *  properties.
 */
public enum PropertyTypeInfo {

	/** Array of single-precision (32 Bit) floats<p>
	 *
	 *  It is possible to use aiGetMaterialInteger[Array]() (or the C++-API 
	 *  aiMaterial::Get()) to query properties stored in floating-point format. 
	 *  The material system performs the type conversion automatically.
   */
   aiPTI_Float   /*= 0x1*/,

   /** The material property is an aiString.<p>
	 *
	 *  Arrays of strings aren't possible, aiGetMaterialString() (or the 
	 *  C++-API aiMaterial::Get()) *must* be used to query a string property.
   */
   aiPTI_String  /*= 0x3*/,

   /** Array of (32 Bit) integers<p>
	 *
	 *  It is possible to use aiGetMaterialFloat[Array]() (or the C++-API 
	 *  aiMaterial::Get()) to query properties stored in integer format. 
	 *  The material system performs the type conversion automatically.
   */
   aiPTI_Integer /*= 0x4*/,


   /** Simple binary buffer, content undefined. Not convertible to anything.
   */
   aiPTI_Buffer  /*= 0x5*/,
}
