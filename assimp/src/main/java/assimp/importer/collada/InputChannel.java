package assimp.importer.collada;

/** An input channel for mesh data, referring to a single accessor */
final class InputChannel {

	/** Type of the data */
	int mType;
	/** Optional index, if multiple sets of the same data type are given */
	int mIndex;
	/** Index offset in the indices array of per-face indices. Don't ask, can't explain that any better. */
	int mOffset;
	/** ID of the accessor where to read the actual values from. */
	String mAccessor;
	/** Pointer to the accessor, if resolved. NULL else */
	Accessor mResolved;
}
