package assimp.importer.blender;

import java.util.ArrayList;

final class MDeformVert extends ElemBase{

	final ArrayList<MDeformWeight> dw = new ArrayList<>();
	int totweight;
	
	public MDeformVert() {}
	
	public MDeformVert(MDeformVert o) {
		set(o);
	}
	
	public void set(MDeformVert o){
		dw.clear();
		for(int i = 0; i < o.dw.size(); i++)
			dw.add(new assimp.importer.blender.MDeformWeight(o.dw.get(i)));
		totweight = o.totweight;
	}


}
