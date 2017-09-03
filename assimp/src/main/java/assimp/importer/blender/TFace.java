package assimp.importer.blender;

import assimp.common.AssUtil;

final class TFace extends ElemBase{
	final float[][] uv = new float[4][2];
	final int[] col = new int[4];
	byte flag;
	short mode;
	short tile;
	short unwrap;
	
	public TFace() {}
	
	public TFace(TFace o) {
		set(o);
	}
	
	public void set(TFace o){
		for(int i = 0; i < o.uv.length; i++)
			System.arraycopy(o.uv[i], 0, uv[i], 0, o.uv[i].length);
		System.arraycopy(o.col, 0, col, 0, o.col.length);
		flag = o.flag;
		mode = o.mode;
		tile = o.tile;
		unwrap = o.unwrap;
	}
}
