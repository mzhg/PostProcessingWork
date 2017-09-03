package assimp.importer.blender;


class MEdge extends ElemBase{

	int v1, v2;
    byte crease, bweight;
    short flag;
    
    public MEdge() {}
	
	public MEdge(MEdge o) {
		set(o);
	}
	
	public void set(MEdge o){
		v1 = o.v1;
		v2 = o.v2;
		crease = o.crease;
		bweight = o.bweight;
		flag = o.flag;
	}
}
