package assimp.importer.blender;

final class ModifierData extends ElemBase{

//	enum ModifierType {
	static final int 
	      eModifierType_None = 0,
	      eModifierType_Subsurf =1,
	      eModifierType_Lattice =2,
	      eModifierType_Curve = 3,
	      eModifierType_Build = 4,
	      eModifierType_Mirror = 5,
	      eModifierType_Decimate = 6,
	      eModifierType_Wave = 7,
	      eModifierType_Armature = 8,
	      eModifierType_Hook =9,
	      eModifierType_Softbody =10,
	      eModifierType_Boolean =11,
	      eModifierType_Array = 12,
	      eModifierType_EdgeSplit =13,
	      eModifierType_Displace =14,
	      eModifierType_UVProject = 15,
	      eModifierType_Smooth =16,
	      eModifierType_Cast = 17,
	      eModifierType_MeshDeform =18,
	      eModifierType_ParticleSystem =19,
	      eModifierType_ParticleInstance =20,
	      eModifierType_Explode =21,
	      eModifierType_Cloth =22,
	      eModifierType_Collision = 23,
	      eModifierType_Bevel = 24,
	      eModifierType_Shrinkwrap = 25,
	      eModifierType_Fluidsim = 26,
	      eModifierType_Mask =27,
	      eModifierType_SimpleDeform =28,
	      eModifierType_Multires =29,
	      eModifierType_Surface = 30,
	      eModifierType_Smoke =31,
	      eModifierType_ShapeKey =32;
//		};
	
	ElemBase next, prev;
	int type, mode;
	final byte[] name = new byte[32];
}
