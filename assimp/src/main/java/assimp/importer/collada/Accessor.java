package assimp.importer.collada;

import java.util.ArrayList;

/** Accessor to a data array */
final class Accessor {

	/** in number of objects */
	int mCount;
	/** size of an object, in elements (floats or strings, mostly 1) */
	int mSize;
	/** in number of values */
	int mOffset;
	/** Stride in number of values */
	int mStride;
	/** names of the data streams in the accessors. Empty string tells to ignore. */
	final ArrayList<String> mParams = new ArrayList<>();
	/** Suboffset inside the object for the common 4 elements. For a vector, thats XYZ, for a color RGBA and so on.
	 * For example, SubOffset[0] denotes which of the values inside the object is the vector X component.*/
	final int[] mSubOffset = new int[4];
	/** URL of the source array */
	String mSource;
	/** Pointer to the source array, if resolved. null else */
	Data mData;
}
