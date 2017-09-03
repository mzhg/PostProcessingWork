package assimp.common;

public class NodeAttachmentInfo {

	public Node node;
	public Node attachToNode;
	public boolean resolved;
	public int src_idx = -1;
	
	public NodeAttachmentInfo() {
	}

	public NodeAttachmentInfo(Node node, Node attachToNode, int src_idx) {
		this.node = node;
		this.attachToNode = attachToNode;
		this.src_idx = src_idx;
	}
}


