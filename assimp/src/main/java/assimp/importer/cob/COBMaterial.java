package assimp.importer.cob;

/** COB Material data structure */
final class COBMaterial extends ChunkInfo{

//	enum Shader {
	static final int
		FLAT = 0,PHONG = 1,METAL = 2;
//	};

//	enum AutoFacet {
	static final int
		FACETED = 0,AUTOFACETED = 1,SMOOTH = 2;
//	};
	
	String type;

//	aiColor3D rgb;
	float r,g,b;
	float alpha, exp, ior,ka,ks = 1.f;

	int matnum = -1;
	int shader = FLAT; 

	int autofacet = FACETED;
	float autofacet_angle;

	COBTexture tex_env,tex_bump,tex_color;
}
