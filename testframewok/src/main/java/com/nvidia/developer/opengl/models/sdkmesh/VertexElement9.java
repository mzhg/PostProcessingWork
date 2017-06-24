package com.nvidia.developer.opengl.models.sdkmesh;

import jet.opengl.postprocessing.util.Numeric;

final class VertexElement9 {

	 short    stream;     // Stream index
	 short    offset;     // Offset in the stream in bytes
	 byte     type;       // Data type
	 byte     method;     // Processing method
	 byte     usage;      // Semantics
	 byte     usageIndex; // Semantic index
	 
	 int load(byte[] data, int offset){
		 stream = Numeric.getShort(data, offset); offset +=2;
		 this.offset = Numeric.getShort(data, offset); offset +=2;
		 
		 type = data[offset++];
		 method = data[offset++];
		 usage = data[offset++];
		 usageIndex = data[offset++];
		 return offset;
	 }

	@Override
	public String toString() {
		return "VertexElement9 [stream=" + stream + ", offset=" + offset + ",\n type=" + type + ", method=" + method
				+ ", usage=" + usage + ", usageIndex=" + usageIndex + "]";
	}
}
