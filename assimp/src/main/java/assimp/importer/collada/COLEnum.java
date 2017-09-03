package assimp.importer.collada;

interface COLEnum {

	/** Collada file versions which evolved during the years ... */
//	enum FormatVersion
//	{
	static final int 
		FV_1_5_n = 0,
		FV_1_4_n = 1,
		FV_1_3_n = 2;
//	};


	/** Transformation types that can be applied to a node */
//	enum TransformType
//	{
	static final int 
		TF_LOOKAT = 0,
		TF_ROTATE = 1,
		TF_TRANSLATE = 2,
		TF_SCALE = 3,
		TF_SKEW = 4,
		TF_MATRIX = 5;
//	};

	/** Different types of input data to a vertex or face */
//	enum InputType
//	{
	static final int
		IT_Invalid = 0,
		IT_Vertex  = 1,  // special type for per-index data referring to the <vertices> element carrying the per-vertex data.
		IT_Position= 2,
		IT_Normal  = 3,
		IT_Texcoord= 4,
		IT_Color   = 5,
		IT_Tangent = 6,
		IT_Bitangent = 7;
//	};
	
	static final int aiLightSource_AMBIENT = 0xdeaddead;
	static final float ASSIMP_COLLADA_LIGHT_ANGLE_NOT_SET = 1e9f;
	
	/** Which type of primitives the ReadPrimitives() function is going to read */
//	enum PrimitiveType
//	{
	static final int
		Prim_Invalid = 0,
		Prim_Lines = 1,
		Prim_LineStrip = 2,
		Prim_Triangles = 3,
		Prim_TriStrips = 4,
		Prim_TriFans = 5,
		Prim_Polylist = 6,
		Prim_Polygon = 7;
//	};
	
	/** Type of the effect param */
//	enum ParamType
//	{
	static final int
		Param_Sampler = 0,
		Param_Surface = 1;
//	};
	
	/** Shading type supported by the standard effect spec of Collada */
//	enum ShadeType
//	{
	static final int
		Shade_Invalid = 0,
		Shade_Constant = 1,
		Shade_Lambert = 2,
		Shade_Phong = 3,
		Shade_Blinn = 4;
//	};
}
