package assimp.importer.blender;


final class MLoopUV extends ElemBase{

	final float[] uv= new float[2];// uv0, uv1;
	int flag;
	
	public MLoopUV() {}
	
	public MLoopUV(MLoopUV o) {
		set(o);
	}
	
	public void set(MLoopUV o){
		System.arraycopy(o.uv, 0, uv, 0, o.uv.length);
		flag = o.flag;
	}


}
