package assimp.importer.ase;

import java.util.ArrayList;
import java.util.List;

import assimp.common.QuatKey;
import assimp.common.VectorKey;

/** Helper structure to represent an ASE file animation */
final class ASEAnimation {

//	enum Type
//	{
	static final int
		TRACK   = 0x0,
		BEZIER  = 0x1,
		TCB		= 0x2;
//	} ;

//	Animation()
//		:	mRotationType	(TRACK)
//		,	mScalingType	(TRACK)
//		,	mPositionType	(TRACK)
//	{}
	
	int mRotationType, mScalingType, mPositionType;

	//! List of track rotation keyframes
	final List<QuatKey> akeyRotations = new ArrayList<QuatKey>();

	//! List of track position keyframes
	final List<VectorKey> akeyPositions = new ArrayList<>();

	//! List of track scaling keyframes
	final List<VectorKey> akeyScaling = new ArrayList<VectorKey>();
}
