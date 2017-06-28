package com.nvidia.developer.opengl.models.sdkmesh;

import jet.opengl.postprocessing.util.Numeric;

public final class SDKMeshSubset {

	static final int SIZE = 144;
	
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
    	name = SDKmesh.getString(data, position, SDKmesh.MAX_SUBSET_NAME);
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

	public void toString(StringBuilder out, int index){
		out.append("SDKMeshSubset").append(index).append(":------------------------------\n");
		out.append("name = ").append(name).append('\n');
		out.append("materialID = ").append(materialID).append('\n');
		out.append("primitiveType = ").append(primitiveType).append('\n');

		out.append("indexStart = ").append(indexStart).append('\n');
		out.append("indexCount = ").append(indexCount).append('\n');
		out.append("vertexStart = ").append(vertexStart).append('\n');
		out.append("vertexCount = ").append(vertexCount).append('\n');
		out.append("--------------------------------\n");
	}

	@Override
	public String toString() {
		return "SDKMeshSubset [name=" + name + ",\n materialID=" + materialID + ", primitiveType=" + primitiveType
				+ ",\n indexStart=" + indexStart + ", indexCount=" + indexCount + ",\n vertexStart=" + vertexStart
				+ ", vertexCount=" + vertexCount + "]";
	}
}
