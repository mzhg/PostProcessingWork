package assimp.importer.blender;

final class SubsurfModifierData extends ElemBase{

	static final int
		TYPE_CatmullClarke = 0x0,
		TYPE_Simple = 0x1;
	static final int FLAGS_SubsurfUV = 1 << 3;
	
	final ModifierData modifier = new ModifierData();
	short subdivType;
	short levels;
	short renderLevels ;
	short flags;
}
