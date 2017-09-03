package assimp.importer.q3d;

import java.nio.FloatBuffer;
import java.util.ArrayList;

final class Q3DMesh {

	FloatBuffer verts;
	FloatBuffer normals;
	FloatBuffer uv;
	ArrayList<Q3DFace> faces;
	
	int prevUVIdx;
}
