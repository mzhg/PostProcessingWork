package assimp.importer.blender;

final class MDeformWeight extends ElemBase{

	int def_nr;
	float weight;
	
	public MDeformWeight() {}
	
	public MDeformWeight(MDeformWeight o) {
		set(o);
	}
	
	public void set(MDeformWeight o){
		def_nr = o.def_nr;
		weight = o.weight;
	}
}
