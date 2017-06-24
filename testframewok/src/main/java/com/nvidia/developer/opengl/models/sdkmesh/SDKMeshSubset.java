package com.nvidia.developer.opengl.models.sdkmesh;

import jet.opengl.postprocessing.util.Numeric;

public final class SDKMeshSubset {

//	char Name[MAX_SUBSET_NAME];
	public String name;        // 100
	public int materialID;     // 104
	public int primitiveType;  // 108
    					// 112 pad
	public long indexStart;    // 120
	public long indexCount;    // 128
	public long vertexStart;   // 136
	public long vertexCount;   // 144
    
    int load(byte[] data, int position){
    	name = new String(data, position, SDKmesh.MAX_SUBSET_NAME).trim();
    	position += SDKmesh.MAX_SUBSET_NAME;
    	
    	materialID = Numeric.getInt(data, position); position += 4;
    	primitiveType = Numeric.getInt(data, position); position += 4;
    	
    	position += 4;
    	indexStart = Numeric.getLong(data, position); position += 8;
    	indexCount = Numeric.getLong(data, position); position += 8;
    	vertexStart = Numeric.getLong(data, position); position += 8;
    	vertexCount = Numeric.getLong(data, position); position += 8;
    	return position;
    }

	@Override
	public String toString() {
		return "SDKMeshSubset [name=" + name + ",\n materialID=" + materialID + ", primitiveType=" + primitiveType
				+ ",\n indexStart=" + indexStart + ", indexCount=" + indexCount + ",\n vertexStart=" + vertexStart
				+ ", vertexCount=" + vertexCount + "]";
	}
}
