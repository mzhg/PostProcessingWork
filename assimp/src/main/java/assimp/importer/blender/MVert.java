package assimp.importer.blender;

final class MVert extends ElemBase{

//	 float co0, co1, co2;
//	 float no0, no1, no2;
	 final float[] co = new float[3];
	 final float[] no = new float[3];
	 byte flag;
	 int mat_nr;
	 int bweight;
	 
	public MVert() {}
	
	public MVert(MVert o) {
		set(o);
	}
	
	public void set(MVert o){
		System.arraycopy(o.co, 0, co, 0, o.co.length);
		System.arraycopy(o.no, 0, no, 0, o.no.length);
		flag = o.flag;
		mat_nr = o.mat_nr;
		bweight = o.bweight;

	}
}
