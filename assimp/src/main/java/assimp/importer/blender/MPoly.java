package assimp.importer.blender;

final class MPoly extends ElemBase{

	int loopstart;
	int totloop;
	short mat_nr;
	byte flag;
	
	public MPoly() {}
	
	public MPoly(MPoly o) {
		set(o);
	}
	
	public void set(MPoly o){
		loopstart = o.loopstart;
		totloop = o.totloop;
		mat_nr = o.mat_nr;
		flag = o.flag;
	}

}
