package assimp.importer.blender;

//Note that red and blue are not swapped, as with MCol
final class MLoopCol extends ElemBase{

	byte r,g,b,a;
	
	public MLoopCol() {}
	
	public MLoopCol(MLoopCol o) {
		set(o);
	}
	
	public void set(MLoopCol o){
		r = o.r;
		g = o.g;
		b = o.b;
		a = o.a;
	}
}
