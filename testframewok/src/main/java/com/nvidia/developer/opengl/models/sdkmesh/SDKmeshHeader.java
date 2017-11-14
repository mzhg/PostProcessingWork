package com.nvidia.developer.opengl.models.sdkmesh;

import jet.opengl.postprocessing.util.Numeric;

public final class SDKmeshHeader {
	
	static final int SIZE = 104;

	//Basic Info and sizes
    public int version;		                    // 4
	public boolean isBigEndian;					// 8
	public long headerSize;						// 16
	public long nonBufferDataSize;					// 24
	public long bufferDataSize;					// 32

    //Stats
	public int numVertexBuffers;					// 36
	public int numIndexBuffers;				    // 40
	public int numMeshes;							// 44
	public int numTotalSubsets;					// 48
	public int numFrames;							// 52
	public int numMaterials;						// 56

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

	public void toString(StringBuilder out){
		out.append("SDKmeshHeader:----------------\n");
		out.append("version = ").append(version).append('\n');
		out.append("isBigEndian = ").append(isBigEndian).append('\n');
		out.append("headerSize = ").append(headerSize).append('\n');
		out.append("nonBufferDataSize = ").append(nonBufferDataSize).append('\n');
		out.append("bufferDataSize = ").append(bufferDataSize).append('\n');
		out.append("numVertexBuffers = ").append(numVertexBuffers).append('\n');
		out.append("numIndexBuffers = ").append(numIndexBuffers).append('\n');
		out.append("numMeshes = ").append(numMeshes).append('\n');
		out.append("numTotalSubsets = ").append(numTotalSubsets).append('\n');
		out.append("numFrames = ").append(numFrames).append('\n');
		out.append("numMaterials = ").append(numMaterials).append('\n');
		out.append("vertexStreamHeadersOffset = ").append(vertexStreamHeadersOffset).append('\n');
		out.append("indexStreamHeadersOffset = ").append(indexStreamHeadersOffset).append('\n');
		out.append("meshDataOffset = ").append(meshDataOffset).append('\n');
		out.append("subsetDataOffset = ").append(subsetDataOffset).append('\n');
		out.append("frameDataOffset = ").append(frameDataOffset).append('\n');
		out.append("materialDataOffset = ").append(materialDataOffset).append('\n');
		out.append("-----------------------------------\n");
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
