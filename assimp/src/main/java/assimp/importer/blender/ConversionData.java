package assimp.importer.blender;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import assimp.common.Camera;
import assimp.common.Light;
import assimp.common.Material;
import assimp.common.Mesh;
import assimp.common.Texture;
import assimp.common.TextureType;

/** ConversionData acts as intermediate storage location for
 *  the various ConvertXXX routines in BlenderImporter.*/
final class ConversionData {

	// set of all materials referenced by at least one mesh in the scene
	final ArrayList<BLEMaterial> materials_raw = new ArrayList<>();
	final Set<BLEObject> objects = new HashSet<>();
	
	// counter to name sentinel textures inserted as substitutes for procedural textures.
	int sentinel_cnt;

	// next texture ID for each texture type, respectively
	final int[] next_texture = new int[TextureType.aiTextureType_UNKNOWN.ordinal() + 1];

	// original file data
	final FileDatabase db;
	
	final ArrayList<Mesh> meshes = new ArrayList<>();
	final ArrayList<Camera> cameras = new ArrayList<>();
	final ArrayList<Light> lights = new ArrayList<>();
	final ArrayList<Material> materials = new ArrayList<>();
	final ArrayList<Texture> textures = new ArrayList<>();
	
	public ConversionData(FileDatabase db) {
		this.db = db;
	}
}
