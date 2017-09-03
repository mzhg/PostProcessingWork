package assimp.importer.blender;

class MLoop extends ElemBase{

	int v, e;
	
	public MLoop() {}
	
	public MLoop(MLoop o) {
		set(o);
	}
	
	public void set(MLoop o){
		v = o.v;
		e = o.e;
	}
}
