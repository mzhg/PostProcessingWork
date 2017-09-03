package assimp.importer.collada;

/** Description of a collada animation channel which has been determined to affect the current node */
final class ChannelEntry {

	AnimationChannel mChannel; ///> the source channel
	String mTransformId;   // the ID of the transformation step of the node which is influenced
	int mTransformIndex; // Index into the node's transform chain to apply the channel to
	int mSubElement; // starting index inside the transform data

	// resolved data references
	Accessor mTimeAccessor; ///> Collada accessor to the time values
	Data mTimeData; ///> Source data array for the time values
	Accessor mValueAccessor; ///> Collada accessor to the key value values
	Data mValueData; ///> Source datat array for the key value values
}
