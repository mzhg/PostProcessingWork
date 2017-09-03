package assimp.common;

/** Defines how an animation channel behaves outside the defined time
 *  range. This corresponds to aiNodeAnim::mPreState and 
 *  aiNodeAnim::mPostState.*/
public enum AnimBehaviour {

	/** The value from the default node transformation is taken*/
	aiAnimBehaviour_DEFAULT,  

	/** The nearest key value is used without interpolation */
	aiAnimBehaviour_CONSTANT,

	/** The value of the nearest two keys is linearly
	 *  extrapolated for the current time value.*/
	aiAnimBehaviour_LINEAR,

	/** The animation is repeated.
	 *
	 *  If the animation key go from n to m and the current
	 *  time is t, use the value at (t-n) % (|m-n|).*/
	aiAnimBehaviour_REPEAT/*   = 0x3*/,
}
