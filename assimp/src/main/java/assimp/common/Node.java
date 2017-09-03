package assimp.common;

import java.util.Arrays;

import org.lwjgl.util.vector.Matrix4f;

/** A node in the imported hierarchy. <p>
*
* Each node has name, a parent node (except for the root node), 
* a transformation relative to its parent and possibly several child nodes.
* Simple file formats don't support hierarchical structures - for these formats 
* the imported scene does consist of only a single root node without children.
*/
public class Node implements Copyable<Node>{

	/** The name of the node. <p>
	 *
	 * The name might be empty (length of zero) but all nodes which 
	 * need to be referenced by either bones or animations are named.
	 * Multiple nodes may have the same name, except for nodes which are referenced
	 * by bones (see #aiBone and #aiMesh::mBones). Their names *must* be unique.<p>
	 * 
	 * Cameras and lights reference a specific node by name - if there
	 * are multiple nodes with this name, they are assigned to each of them.<p>
	 * <br>
	 * There are no limitations with regard to the characters contained in
	 * the name string as it is usually taken directly from the source file. <p>
	 * 
	 * Implementations should be able to handle tokens such as whitespace, tabs,
	 * line feeds, quotation marks, ampersands etc.<p>
	 *
	 * Sometimes assimp introduces new nodes not present in the source file
	 * into the hierarchy (usually out of necessity because sometimes the
	 * source hierarchy format is simply not compatible). Their names are
	 * surrounded by <> e.g.
	 */
	public String mName = "";

	/** The transformation relative to the node's parent. */
	public final Matrix4f mTransformation = new Matrix4f();

	/** Parent node. null if this node is the root node. */
	public Node mParent;

	/** The child nodes of this node. NULL if mNumChildren is 0. */
	public Node[] mChildren;

	/** The meshes of this node. Each entry is an index into the mesh */
	public int[] mMeshes;

	/** Metadata associated with this node or NULL if there is no metadata.
	  *  Whether any metadata is generated depends on the source file format. See the
	  * @link importer_notes @endlink page for more information on every source file
	  * format. Importers that don't document any metadata don't write any. 
	  */
	public Metadata mMetaData;
	
	public Node() {}
	
	public Node(String name){
		mName = name;
	}
	
	/** The number of child nodes of this node. */
	public int getNumChildren() { return mChildren != null ? mChildren.length : 0;}
	
	/** The number of meshes of this node. */
	public int getNumMeshes() { return mMeshes != null ? mMeshes.length : 0;}
	
	/** Searches for a node with a specific name, beginning at this
	 *  nodes. Normally you will call this method on the root node
	 *  of the scene.
	 * 
	 *  @param name Name to search for
	 *  @return null or a valid Node if the search was successful.
	 */
	public Node findNode(String name)
	{
		if(mName.equals(name)) return this;
		
		int numChildren = getNumChildren();
		for(int i = 0; i < numChildren; i++){
			Node p = mChildren[i].findNode(name);
			if(p != null)
				return p;
		}
		
		// there is definitely no sub-node with this name
		return null;
	}

	@Override
	public Node copy() {
		Node node = new Node();
		node.mChildren = AssUtil.copyOf(mChildren);
		if(mMeshes != null)
			node.mMeshes = Arrays.copyOf(mMeshes, mMeshes.length);
		node.mMetaData = mMetaData;
		node.mName = mName;
		node.mParent = mParent;
		node.mTransformation.load(mTransformation);
		return node;
	}
}
