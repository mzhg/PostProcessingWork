package assimp.importer.ase;

/** Helper structure to represent an ASE camera */
final class ASECamera extends BaseNode{

//	enum CameraType
//	{
	static final int
		FREE = 0,
		TARGET = 1;
//	};
	
	float mFOV = 0.75f;  // in radians
	float mNear = 0.1f, mFar = 1000.f;
	byte mCameraType;
	
	public ASECamera() {
		super(CAMERA);
	}
}
