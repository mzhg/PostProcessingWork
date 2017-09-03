package assimp.importer.blender;

final class MirrorModifierData extends ElemBase{

	static final int
		Flags_CLIPPING      =1<<0,
		Flags_MIRROR_U      =1<<1,
		Flags_MIRROR_V      =1<<2,
		Flags_AXIS_X        =1<<3,
		Flags_AXIS_Y        =1<<4,
		Flags_AXIS_Z        =1<<5,
		Flags_VGROUP        =1<<6;
	
	final ModifierData modifier =new ModifierData();

	short axis, flag;
	float tolerance;
	BLEObject mirror_ob;
}
