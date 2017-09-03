package assimp.importer.collada;

import java.util.ArrayList;

/** An animation. Container for 0-x animation channels or 0-x animations */
final class COLAnimation {

	/** Anim name */
	String mName;

	/** the animation channels, if any */
	ArrayList<AnimationChannel> mChannels;

	/** the sub-animations, if any */
	ArrayList<COLAnimation> mSubAnims;
}
