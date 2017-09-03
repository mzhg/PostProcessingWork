package assimp.importer.blender;

final class BLEImage extends ElemBase{

	final ID id = new ID();
	final byte[] name = new byte[240];
	
	short ok, flag;
	short source, type, pad, pad1;
	int lastframe;

	short tpageflag, totbind;
	short xrep, yrep;
	short twsta, twend;
	//unsigned int bindcode;  
	//unsigned int *repbind; 

	PackedFile packedfile;
	//struct PreviewImage * preview;

	float lastupdate;
	int lastused;
	short animspeed;

	short gen_x, gen_y, gen_type; 
}
