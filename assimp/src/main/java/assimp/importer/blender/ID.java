package assimp.importer.blender;

final class ID extends ElemBase {

	final byte[] name = new byte[24];
	short flag;
	
	public ID() {}
	
	public ID(ID o) {
		set(o);
	}
	
	public void set(ID o){
		System.arraycopy(o.name, 0, name, 0, o.name.length);
		flag = o.flag;
	}
}
