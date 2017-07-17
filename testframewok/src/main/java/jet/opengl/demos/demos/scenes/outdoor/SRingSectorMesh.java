package jet.opengl.demos.demos.scenes.outdoor;

import com.nvidia.developer.opengl.utils.BoundingBox;

final class SRingSectorMesh {

	int pIndBuff;
	int uiNumIndices;
	final BoundingBox boundBox = new BoundingBox();
	@Override
	public String toString() {
		return "SRingSectorMesh [pIndBuff=" + pIndBuff + ", uiNumIndices=" + uiNumIndices + ", boundBox=" + boundBox
				+ "]";
	}
}
