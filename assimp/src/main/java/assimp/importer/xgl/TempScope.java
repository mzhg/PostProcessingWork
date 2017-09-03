package assimp.importer.xgl;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.ArrayList;
import java.util.List;

import assimp.common.Light;
import assimp.common.Material;
import assimp.common.Mesh;

final class TempScope {

	final Int2ObjectMap<List<Mesh>> meshes = new Int2ObjectOpenHashMap<List<Mesh>>();
	final Int2ObjectMap<Material> materials = new Int2ObjectOpenHashMap<Material>();
	
	final ArrayList<Mesh> meshes_linear = new ArrayList<Mesh>();
	final ArrayList<Material> materials_linear = new ArrayList<Material>();
	
	Light light;
	
	void dismiss() {
		light = null;
		meshes_linear.clear();
		materials_linear.clear();
		meshes.clear();
		materials.clear();
	}
}
