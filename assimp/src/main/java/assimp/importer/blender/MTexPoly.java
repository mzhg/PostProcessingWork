package assimp.importer.blender;


final class MTexPoly extends ElemBase{

	BLEImage tpage;
	byte flag, transp;
	short mode, tile, pad;
	
	public MTexPoly() {}
	
	public MTexPoly(MTexPoly o) {
		set(o);
	}
	
	public void set(MTexPoly o){
		tpage = o.tpage;
		flag = o.flag;
		transp = o.transp;
		mode = o.mode;
		tile = o.tile;
		pad = o.pad;
	}
}
