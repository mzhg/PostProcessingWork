package assimp.importer.collada;

/** An animation channel. */
final class AnimationChannel {

	/** URL of the data to animate. Could be about anything, but we support only the 
	 * "NodeID/TransformID.SubElement" notation 
	 */
	String mTarget;

	/** Source URL of the time values. Collada calls them "input". Meh. */
	String mSourceTimes;
	/** Source URL of the value values. Collada calls them "output". */
	String mSourceValues;
}
