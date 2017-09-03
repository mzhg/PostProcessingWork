package assimp.importer.raw;

import java.util.ArrayList;

final class GroupInformation {

	final String name;
	final ArrayList<MeshInformation> meshes = new ArrayList<>(10);
	public GroupInformation(String name) {
		this.name = name;
	}
}
