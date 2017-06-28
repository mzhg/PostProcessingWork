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

	void resolveElementSize(){
		for(int i = 0; i < decl.length; i++){
			VertexElement9 curr = decl[i];
			if(curr.stream < 0 || curr.stream >= decl.length)
				return;

			if(i == decl.length - 1){
				int diff_offset = (int)(strideBytes - curr.offset);
				curr.size = (byte) (diff_offset /4);  // Float
			}else{
				VertexElement9 next = decl[i+1];
				int diff_offset = 0;
				if(next.stream >=0 && next.stream < decl.length){
					diff_offset = next.offset - curr.offset;
				}else{
					diff_offset = (int)(strideBytes - curr.offset);
				}

				curr.size = (byte) (diff_offset /4);  // Float
			}

			curr.stream = (short)i;
		}
	}

	public void toString(StringBuilder out, int index){
		out.append("SDKMeshVertexBufferHeader").append(index).append(":----------------------------\n");
		out.append("numVertices = ").append(numVertices).append('\n');
		out.append("sizeBytes = ").append(sizeBytes).append('\n');
		out.append("strideBytes = ").append(strideBytes).append('\n');
		out.append("dataOffset = ").append(dataOffset).append('\n');

		for(VertexElement9 vertex : decl){
			if(vertex.stream < 0 || vertex.stream >= SDKmesh.MAX_VERTEX_STREAMS)
				break;

			out.append(vertex).append('\n');
		}
		out.append("------------------------------------\n");
	}

	@Override
	public String toString() {
		return "SDKMeshVertexBufferHeader [numVertices=" + numVertices + ",\n sizeBytes=" + sizeBytes + ",\n strideBytes="
				+ strideBytes + ",\n decl=" + Arrays.toString(decl) + ",\n dataOffset=" + dataOffset + "]";
	}
}
