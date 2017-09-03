package assimp.importer.collada;

/** A collada camera. */
final class COLCamera {

	// Name of camera
	String mName;

	// True if it is an orthografic camera
	boolean mOrtho;

	//! Horizontal field of view in degrees
	float mHorFov = 10e10f;

	//! Vertical field of view in degrees
	float mVerFov = 10e10f;

	//! Screen aspect
	float mAspect = 10e10f;

	//! Near& far z
	float mZNear = 0.1f, mZFar = 1000.0f;
}
