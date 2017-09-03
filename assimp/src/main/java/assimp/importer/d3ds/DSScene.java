package assimp.importer.d3ds;

import java.util.ArrayList;
import java.util.List;

import assimp.common.Camera;
import assimp.common.Light;

/** Helper structure analogue to aiScene */
final class DSScene {

	//! List of all materials loaded
	//! NOTE: 3ds references materials globally
	final List<D3DSMaterial> mMaterials = new ArrayList<D3DSMaterial>();

	//! List of all meshes loaded
	final List<D3DSMesh> mMeshes = new ArrayList<D3DSMesh>();

	//! List of all cameras loaded
	final List<Camera> mCameras = new ArrayList<>();

	//! List of all lights loaded
	final List<Light> mLights = new ArrayList<Light>();
}
