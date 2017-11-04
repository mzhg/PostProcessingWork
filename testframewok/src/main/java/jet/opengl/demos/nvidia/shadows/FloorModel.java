package jet.opengl.demos.nvidia.shadows;

final class FloorModel {

	static final int numVertices = 4;

	static final int numIndices = 6;
	
	static final float[][] vertices = {
		{-1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 0.0f},
		{1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 0.0f},
		{-1.0f, 0.0f, -1.0f, 1.0f, 0.0f, 1.0f, 0.0f},
		{1.0f, 0.0f, -1.0f, 1.0f, 0.0f, 1.0f, 0.0f}	
	};
	
	static final short[] indices = {
			0, 1, 2, 2, 1, 3,
	};
}
