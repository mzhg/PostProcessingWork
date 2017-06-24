package com.nvidia.developer.opengl.models.sdkmesh;

import java.io.IOException;

final class SDKMeshTest {

	public static void main(String[] args) {
		String root = "D:\\Program Files (x86)\\Microsoft DirectX SDK (June 2010)\\Samples\\Media\\DeferredParticles\\";
		String file = "wallchunk0.sdkmesh";
		
		SDKmesh mesh = new SDKmesh();
		try {
			mesh.create(root + file, false, null);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
//		int k = 12;
//		System.out.println(1 + k << 2);
//		System.out.println(1 + k * 4);
	}
}
