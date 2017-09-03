package assimp.importer.bvh;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import assimp.common.Node;

/** Collected list of node. Will be bones of the dummy mesh some day, addressed by their array index */
final class BVHNode {
	
	/** Possible animation channels for which the motion data holds the values */
//	enum ChannelType
//	{
	static final int
		Channel_PositionX = 0,
		Channel_PositionY = 1,
		Channel_PositionZ = 2,
		Channel_RotationX = 3,
		Channel_RotationY = 4,
		Channel_RotationZ = 5;

	Node mNode;
	final IntArrayList mChannels = new IntArrayList(10);
	// motion data values for that node. Of size NumChannels * NumFrames
	final FloatArrayList mChannelValues = new FloatArrayList(10);
	
	public BVHNode() {
	}
	
	public BVHNode(Node node) {
		mNode = node;
	}
}
