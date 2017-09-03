package assimp.importer.blender;

final class BLECamera extends ElemBase{

	static final int Type_PERSP = 0;
	static final int Type_ORTHO = 1;
	
	final ID id = new ID();
	
	int type,flag;
	float angle;
}
