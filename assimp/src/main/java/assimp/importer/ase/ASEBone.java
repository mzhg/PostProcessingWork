package assimp.importer.ase;

/** Helper structure to represent an ASE file bone */
final class ASEBone {

	static int iCnt = 0;
	
	//! Name of the bone
	String mName;
	
	public ASEBone() {
		mName = String.format("UNNAMED_%i",iCnt++);
	}
	
	public ASEBone(String name) {
		mName = name;
	}
}
