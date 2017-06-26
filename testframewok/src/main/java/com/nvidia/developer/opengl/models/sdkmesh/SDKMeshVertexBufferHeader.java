package com.nvidia.developer.opengl.models.sdkmesh;

import java.util.Arrays;

import jet.opengl.postprocessing.util.Numeric;

final class SDKMeshVertexBufferHeader {

	static final int SIZE = 288;
	
	long numVertices;  // 8
	long sizeBytes;    // 16
	long strideBytes;  // 24
	final VertexElement9[] decl = new VertexElement9[SDKmesh.MAX_VERTEX_ELEMENTS]; // 24 + 32 * 8 = 280
	
	long dataOffset; //(This also forces the union to 64bits) 288
	int buffer;  // 292
				 // 296
	
	public SDKMeshVertexBufferHeader() {
		for(int i = 0; i < decl.length; i++)
			decl[i] = new VertexElement9();
	}
	
	int load(byte[] data, int offset){
		numVertices = Numeric.getLong(data, offset); offset += 8;
		sizeBytes = Numeric.getLong(data, offset); offset += 8;
		strideBytes = Numeric.getLong(data, offset); offset += 8;
		
		for(int i = 0; i < decl.length; i++)
			offset = decl[i].load(data, offset);
		
		dataOffset = Numeric.getLong(data, offset); offset += 8;
		buffer = (int) dataOffset;
		
//		offset += 4;
		return offset;
	}

	@Override
	public String toString() {
		return "SDKMeshVertexBufferHeader [numVertices=" + numVertices + ",\n sizeBytes=" + sizeBytes + ",\n strideBytes="
				+ strideBytes + ",\n decl=" + Arrays.toString(decl) + ",\n dataOffset=" + dataOffset + "]";
	}
}
