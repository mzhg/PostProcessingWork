package assimp.importer.ase;

import java.util.ArrayList;
import java.util.List;

import assimp.common.IntFloatPair;

/** Helper structure to represent an ASE file bone vertex */
final class BoneVertex {

	//! Bone and corresponding vertex weight.
		//! -1 for unrequired bones ....
	final List<IntFloatPair> mBoneWeights = new ArrayList<IntFloatPair>();
}
