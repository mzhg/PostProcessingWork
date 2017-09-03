package assimp.common;

/** A single influence of a bone on a vertex. */
public class VertexWeight implements Copyable<VertexWeight>{
	
	/** The memory size in bytes of the <code>VertexWeight</code> */
	public static final int SIZE = 8;

	/** Index of the vertex which is influenced by the bone. */
	public int mVertexId;
	/**
	 * The strength of the influence in the range (0...1).<br>
	 * The influence from all bones at one vertex amounts to 1.
	 */
	public float mWeight;
	
	public VertexWeight() {
	}

	public VertexWeight(int vertexId, float weight) {
		this.mVertexId = vertexId;
		this.mWeight = weight;
	}

	@Override
	public VertexWeight copy() { return new VertexWeight(mVertexId, mWeight);}
	
	
}
