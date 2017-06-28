package com.nvidia.developer.opengl.models.sdkmesh;

import jet.opengl.postprocessing.util.Numeric;

final class SDKMeshIndexBufferHeader {

	static final int SIZE = 32;
	
	long numVertices;  // 8
	long sizeBytes;    // 16
	int indexType;     // 20
					   // 24  pad
	long dataOffset;   // 32 (This also forces the union to 64bits)
	int buffer;
	
	int load(byte[] data, int offset){
		numVertices = Numeric.getLong(data, offset); offset += 8;
		sizeBytes = Numeric.getLong(data, offset); offset += 8;
		indexType = Numeric.getInt(data, offset); offset += 8;  // TODO
		dataOffset = Numeric.getLong(data, offset); offset += 8;
		buffer = (int)dataOffset;
		return offset;
	}

	public void toString(StringBuilder out, int index){
		out.append("SDKMeshIndexBufferHeader").append(index).append(":----------------------------\n");
		out.append("numVertices = ").append(numVertices).append('\n');
		out.append("sizeBytes = ").append(sizeBytes).append('\n');
		out.append("indexType = ").append(indexType).append('\n');
		out.append("dataOffset = ").append(dataOffset).append('\n');
		out.append("------------------------------------\n");
	}

	@Override
	public String toString() {
		return "SDKMeshIndexBufferHeader [numVertices=" + numVertices + ", sizeBytes=" + sizeBytes + ", indexType="
				+ indexType + ",\n dataOffset=" + dataOffset + ", buffer=" + buffer + "]";
	}
	
}
