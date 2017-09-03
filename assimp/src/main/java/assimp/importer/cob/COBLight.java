package assimp.importer.cob;

/** COB Light data structure */
final class COBLight extends COBNode{

//	enum LightType {
	static final int
		SPOT = 0,LOCAL = 1,INFINITE = 2;
//	};
	
	float r,g,b;
	float angle,inner_angle;

	int ltype = SPOT;
	
	COBLight() {
		super(TYPE_LIGHT);
	}

}
