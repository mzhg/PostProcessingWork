package assimp.importer.collada;


/** Contains all data for one of the different transformation types */
final class Transform {

	/** SID of the transform step, by which anim channels address their target node */
	String mID;
	int mType;
	/** Interpretation of data depends on the type of the transformation */
	final float[] f = new float[16];
	
	public Transform() {}
	
	public Transform(Transform o) {
		set(o);
	}
	
	public void set(Transform o){
		mID = o.mID;
		mType = o.mType;
		System.arraycopy(o.f, 0, f, 0, o.f.length);
	}
}
