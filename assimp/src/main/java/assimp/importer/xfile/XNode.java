package assimp.importer.xfile;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.util.vector.Matrix4f;

/** Helper structure to represent a XFile frame */
public class XNode {

	String mName;
	final Matrix4f mTrafoMatrix = new Matrix4f();
	XNode mParent;
	final List<XNode> mChildren = new ArrayList<XNode>();
	final List<XMesh> mMeshes = new ArrayList<XMesh>();
	
	public XNode() {}
	
	public XNode(XNode parent) {
		mParent = parent;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "XNode [mName=" + mName + ", mTrafoMatrix=" + mTrafoMatrix
				+ ", mChildren=" + mChildren + ", mMeshes=" + mMeshes + "]";
	}
}
