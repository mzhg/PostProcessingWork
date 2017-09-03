package assimp.importer.xfile;

import java.util.ArrayList;
import java.util.List;

import assimp.common.QuatKey;
import assimp.common.VectorKey;

/** Helper structure representing a single animated bone in a XFile */
public class XAnimBone {
	String mBoneName;
	List<VectorKey> mPosKeys;  // either three separate key sequences for position, rotation, scaling
	List<QuatKey>   mRotKeys;
	List<VectorKey> mScaleKeys;
	final List<XMatrixKey> mTrafoKeys = new ArrayList<XMatrixKey>(); // or a combined key sequence of transformation matrices.
	
	@Override
	public String toString() {
		return "XAnimBone [mBoneName=" + mBoneName + ", mPosKeys=" + mPosKeys
				+ ", mRotKeys=" + mRotKeys + ", mScaleKeys=" + mScaleKeys
				+ ", mTrafoKeys=" + mTrafoKeys + "]";
	}
	
}
