package com.nvidia.developer.opengl.models.sdkmesh;

import jet.opengl.postprocessing.util.Numeric;

public final class VertexElement9 {
	
	static final int SIZE = 8;

	public short    stream;     // Stream index
	public short    offset;     // Offset in the stream in bytes
	public byte     type;       // Data type
	public byte     method;     // Processing method
	public byte     usage;      // Semantics
	public byte     usageIndex; // Semantic index
	public byte     size;       // The element count
	 
	 int load(byte[] data, int offset){
		 stream = Numeric.getShort(data, offset); offset +=2;
		 this.offset = Numeric.getShort(data, offset); offset +=2;
		 
		 type = data[offset++];
		 method = data[offset++];
		 usage = data[offset++];
		 usageIndex = data[offset++];
		 return offset;
	 }

	public void toString(StringBuilder out){
		if(stream < 0 || stream >= SDKmesh.MAX_VERTEX_STREAMS)
			return;

		out.append(toString());
	}

	@Override
	public String toString() {
		return "VertexElement9 [stream=" + stream + ", offset=" + offset + ",\n type=" + type + ", method=" + method
				+ ", usage=" + usage + ", usageIndex=" + usageIndex + "]";
	}
}
