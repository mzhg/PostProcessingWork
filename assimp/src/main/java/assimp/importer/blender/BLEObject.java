package assimp.importer.blender;

final class BLEObject extends ElemBase{

	final ID id = new ID();
	
//	enum Type {
	static final int
		 Type_EMPTY		=	0
		,Type_MESH		=	1
		,Type_CURVE		=	2
		,Type_SURF		=   3
		,Type_FONT		=   4
		,Type_MBALL		=	5

		,Type_LAMP		=	10
		,Type_CAMERA	=   11

		,Type_WAVE		=   21
		,Type_LATTICE	=   22;
//	};

	int type;
	float[][] obmat = new float[4][4];
	float[][] parentinv = new float[4][4];
	byte[] parsubstr =  new byte[32];
	
	BLEObject parent;
	BLEObject track;

	BLEObject proxy,proxy_from,proxy_group;
	Group dup_group;
	ElemBase data;

	final ListBase modifiers = new ListBase();
}
