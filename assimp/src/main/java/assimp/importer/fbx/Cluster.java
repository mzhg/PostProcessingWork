package assimp.importer.fbx;

import org.lwjgl.util.vector.Matrix4f;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

final class Cluster extends Deformer{

	private FloatArrayList weights;
	private IntArrayList   indices;
	private Matrix4f transform;
	private Matrix4f transformLink;
	private Model	 node;
	
	public Cluster(long id, Element element, Document doc, String name) {
		super(id, element, doc, name);
	}

	/** get the list of deformer weights associated with this cluster.
	 *  Use #GetIndices() to get the associated vertices. Both arrays
	 *  have the same size (and may also be null). */
	FloatArrayList getWeights() {
		return weights;
	}

	/** get indices into the vertex data of the geometry associated
	 *  with this cluster. Use #GetWeights() to get the associated weights.
	 *  Both arrays have the same size (and may also be null). */
	IntArrayList getIndices() {	return indices;}

	Matrix4f transform() { return transform;}

	Matrix4f transformLink() {	return transformLink;}

	Model targetNode()  {	return node;}
}
