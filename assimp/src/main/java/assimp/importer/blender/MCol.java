package assimp.importer.blender;

final class MCol extends ElemBase{

	byte r,g,b,a;
	
	public MCol() {}
	
	public MCol(MCol o) {
		set(o);
	}
	
	public void set(MCol o){
		r = o.r;
		g = o.g;
		b = o.b;
		a = o.a;
	}
}
