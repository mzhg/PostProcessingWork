package assimp.importer.collada;

import java.util.ArrayList;

import assimp.common.IntPair;
import it.unimi.dsi.fastutil.ints.IntArrayList;

/** A skeleton controller to deform a mesh with the use of joints */
final class Controller {

	/** the URL of the mesh deformed by the controller. */
	String mMeshId; 

	/** accessor URL of the joint names */
	String mJointNameSource;

    /** The bind shape matrix, as array of floats. I'm not sure what this matrix actually describes, but it can't be ignored in all cases*/
    final float[] mBindShapeMatrix = new float[16];

	/** accessor URL of the joint inverse bind matrices */
	String mJointOffsetMatrixSource;

	/** input channel: joint names. */
	InputChannel mWeightInputJoints;
	/** input channel: joint weights */
	InputChannel mWeightInputWeights;

	/** Number of weights per vertex.*/
	IntArrayList mWeightCounts;

	/** JointIndex-WeightIndex pairs for all vertices*/
//	std::vector< std::pair<size_t, size_t> > mWeights;
	ArrayList<IntPair> mWeights;
}
