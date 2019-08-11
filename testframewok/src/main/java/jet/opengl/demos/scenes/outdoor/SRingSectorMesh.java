package jet.opengl.demos.scenes.outdoor;

import jet.opengl.postprocessing.util.BoundingBox;

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
