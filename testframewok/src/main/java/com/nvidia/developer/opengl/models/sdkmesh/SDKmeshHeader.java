package com.nvidia.developer.opengl.models.sdkmesh;

import jet.opengl.postprocessing.util.Numeric;

final class SDKmeshHeader {

	//Basic Info and sizes
    int version;		                    // 4
    boolean isBigEndian;					// 8
    long headerSize;						// 16
    long nonBufferDataSize;					// 24
    long bufferDataSize;					// 32

    //Stats
    int numVertexBuffers;					// 36
    int numIndexBuffers;				    // 40
    int numMeshes;							// 44
    int numTotalSubsets;					// 48
    int numFrames;							// 52
    int numMaterials;						// 56

    //Offsets to Data
    long vertexStreamHeadersOffset;			// 64
    long indexStreamHeadersOffset;			// 72
    long meshDataOffset;					// 80
    long subsetDataOffset;					// 88
    long frameDataOffset;					// 96
    long materialDataOffset;				// 104
    
    int load(byte[] data, int offset){
    	version = Numeric.getInt(data, offset);  offset += 4;
    	isBigEndian = data[offset] != 0;  offset += 4;
    	headerSize = Numeric.getLong(data, offset);  offset += 8;
    	nonBufferDataSize = Numeric.getLong(data, offset);  offset += 8;
    	bufferDataSize = Numeric.getLong(data, offset);  offset += 8;
    	
    	numVertexBuffers = Numeric.getInt(data, offset);  offset += 4;
    	numIndexBuffers = Numeric.getInt(data, offset);  offset += 4;
    	numMeshes = Numeric.getInt(data, offset);  offset += 4;
    	numTotalSubsets = Numeric.getInt(data, offset);  offset += 4;
    	numFrames = Numeric.getInt(data, offset);  offset += 4;
    	numMaterials = Numeric.getInt(data, offset);  offset += 4;
    	
    	vertexStreamHeadersOffset = Numeric.getLong(data, offset);  offset += 8;
    	indexStreamHeadersOffset = Numeric.getLong(data, offset);  offset += 8;
    	meshDataOffset = Numeric.getLong(data, offset);  offset += 8;
    	subsetDataOffset = Numeric.getLong(data, offset);  offset += 8;
    	frameDataOffset = Numeric.getLong(data, offset);  offset += 8;
    	materialDataOffset = Numeric.getLong(data, offset);  offset += 8;
    	
    	return offset;
    }

	@Override
	public String toString() {
		return "SDKmeshHeader [version=" + version + ", isBigEndian=" + isBigEndian + ", headerSize=" + headerSize
				+ ",\n nonBufferDataSize=" + nonBufferDataSize + ", bufferDataSize=" + bufferDataSize
				+ ", numVertexBuffers=" + numVertexBuffers + ",\n numIndexBuffers=" + numIndexBuffers + ", numMeshes="
				+ numMeshes + ", numTotalSubsets=" + numTotalSubsets + ",\n numFrames=" + numFrames + ", numMaterials="
				+ numMaterials + ",\n vertexStreamHeadersOffset=" + vertexStreamHeadersOffset
				+ ", indexStreamHeadersOffset=" + indexStreamHeadersOffset + ",\n meshDataOffset=" + meshDataOffset
				+ ", subsetDataOffset=" + subsetDataOffset + ",\n frameDataOffset=" + frameDataOffset
				+ ", materialDataOffset=" + materialDataOffset + "]";
	}
}
