package assimp.importer.blender;


final class MTFace extends ElemBase{

	final float[][] uv = new float[4][2];
	byte flag;
	short mode;
	short tile;
	short unwrap;
	
	public MTFace() {}
	
	public MTFace(MTFace o) {
		set(o);
	}
	
	public void set(MTFace o){
		for(int i = 0; i < o.uv.length; i++)
			System.arraycopy(o.uv[i], 0, uv[i], 0, o.uv[i].length);
		flag = o.flag;
		mode = o.mode;
		tile = o.tile;
		unwrap = o.unwrap;

	}
}
