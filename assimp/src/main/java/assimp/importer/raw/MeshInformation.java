package assimp.importer.raw;

import java.nio.FloatBuffer;

import assimp.common.AssimpConfig;
import assimp.common.MemoryUtil;

final class MeshInformation {

	String name;
	FloatBuffer vertices;
	FloatBuffer colors;
	
	public MeshInformation(String name) {
		this.name = name;
		
		vertices = MemoryUtil.createFloatBuffer(100 * 3, AssimpConfig.MESH_USE_NATIVE_MEMORY);
		colors   = MemoryUtil.createFloatBuffer(100 * 4, AssimpConfig.MESH_USE_NATIVE_MEMORY);
	}
}
