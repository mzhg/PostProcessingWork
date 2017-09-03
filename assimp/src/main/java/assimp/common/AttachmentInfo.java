package assimp.common;

/**
 * Helper data structure for SceneCombiner.<p>
 * Describes to which node a scene must be attached to.
 */
public class AttachmentInfo {

	public Scene scene;
	public Node attachToNode;
	
	public AttachmentInfo() {
	}

	public AttachmentInfo(Scene scene, Node attachToNode) {
		this.scene = scene;
		this.attachToNode = attachToNode;
	}
	
}
