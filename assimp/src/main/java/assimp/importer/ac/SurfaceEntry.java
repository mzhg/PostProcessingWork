package assimp.importer.ac;

import org.lwjgl.util.vector.Vector2f;

final class SurfaceEntry {

	int first;
	float x;
	float y;
	
	public SurfaceEntry(int first, Vector2f second) {
		this.first = first;
		x = second.x;
		y = second.y;
	}
	
	public SurfaceEntry() {}
}
