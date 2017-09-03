package assimp.importer.blender;

final class MFace extends ElemBase{

	int v1,v2,v3,v4;
	int mat_nr;
	byte flag;
	
	public MFace() {
	}
	
	public MFace(MFace o) {
		set(o);
	}
	
	public void set(MFace o){
		v1 = o.v1;
		v2 = o.v2;
		v3 = o.v4;
		mat_nr = o.mat_nr;
		flag = o.flag;
	}
}
