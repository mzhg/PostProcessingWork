package com.nvidia.developer.opengl.models.sdkmesh;

import org.lwjgl.util.vector.Vector3f;

import java.util.Arrays;

import jet.opengl.postprocessing.util.Numeric;

final class SDKMeshMesh {
	
	static final int SIZE = 104 + 16 * 4 + 4 * (3 + 6) + 4 + 2*8;

	String  name;  		// char name[MAX_MESH_NAME];  100
    byte numVertexBuffers;  					   // 104
    final int[] vertexBuffers = new int[SDKmesh.MAX_VERTEX_STREAMS];  // 168
    int indexBuffer;							   // 172
    int numSubsets;								   // 176
    int numFrameInfluences; //aka bones            // 180

    final Vector3f boundingBoxCenter = new Vector3f();  // 192
    final Vector3f boundingBoxExtents= new Vector3f();  // 204
    													// 208 pad
    // union{
    long subsetOffset;  // Offset to list of subsets (This also forces the union to 64bits) 216
//    int[] pSubsets;     // /Pointer to list of subsets
    int[] pSubsets;
    //};
    
    // union{
    long frameInfluenceOffset; //Offset to list of frame influences (This also forces the union to 64bits) 224
//    int[] pFrameInfluences;  // Pointer to list of frame influences
    int[] pFrameInfluences;
    //};

	int vao;
    
    int load(byte[] data, int offset){
    	name = SDKmesh.getString(data, offset, SDKmesh.MAX_MESH_NAME);
    	offset+= SDKmesh.MAX_MESH_NAME;
    	numVertexBuffers = data[offset]; offset += 4;
    	for(int i = 0; i < vertexBuffers.length; i++){
    		vertexBuffers[i] = Numeric.getInt(data, offset);
    		offset+= 4;
    	}
    	
    	indexBuffer = Numeric.getInt(data, offset);offset+= 4;
    	numSubsets = Numeric.getInt(data, offset);offset+= 4;
    	numFrameInfluences = Numeric.getInt(data, offset);offset+= 4;
    	
    	boundingBoxCenter.x = Numeric.getFloat(data, offset); offset+=4;
    	boundingBoxCenter.y = Numeric.getFloat(data, offset); offset+=4;
    	boundingBoxCenter.z = Numeric.getFloat(data, offset); offset+=4;
    	
    	boundingBoxExtents.x = Numeric.getFloat(data, offset); offset+=4;
    	boundingBoxExtents.y = Numeric.getFloat(data, offset); offset+=4;
    	boundingBoxExtents.z = Numeric.getFloat(data, offset); offset+=4;
    	
    	offset += 4;  // pad
    	subsetOffset = Numeric.getLong(data, offset); offset += 8;
    	frameInfluenceOffset = Numeric.getLong(data, offset); offset += 8;
//    	pSubsets = new int[2];
//    	pSubsets[0] = Numeric.getInt(data, offset);offset+= 4;
//    	pSubsets[1] = Numeric.getInt(data, offset);offset+= 4;
//    	
//    	pFrameInfluences = new int[2];
//    	pFrameInfluences[0] = Numeric.getInt(data, offset);offset+= 4;
//    	pFrameInfluences[1] = Numeric.getInt(data, offset);offset+= 4;
    	
    	return offset;
    }

	@Override
	public String toString() {
		final int maxLen = 10;
		StringBuilder builder = new StringBuilder();
		builder.append("SDKMeshMesh [name=");
		builder.append(name);
		builder.append(", numVertexBuffers=");
		builder.append(numVertexBuffers);
		builder.append(", vertexBuffers=");
		builder.append(vertexBuffers != null
				? Arrays.toString(Arrays.copyOf(vertexBuffers, Math.min(vertexBuffers.length, maxLen))) : null);
		builder.append(", indexBuffer=");
		builder.append(indexBuffer);
		builder.append(", numSubsets=");
		builder.append(numSubsets);
		builder.append(", numFrameInfluences=");
		builder.append(numFrameInfluences);
		builder.append(", boundingBoxCenter=");
		builder.append(boundingBoxCenter);
		builder.append(", boundingBoxExtents=");
		builder.append(boundingBoxExtents);
		builder.append(", subsetOffset=");
		builder.append(subsetOffset);
		builder.append(", pSubsets=");
		builder.append(
				pSubsets != null ? Arrays.toString(Arrays.copyOf(pSubsets, Math.min(pSubsets.length, maxLen))) : null);
		builder.append(", frameInfluenceOffset=");
		builder.append(frameInfluenceOffset);
		builder.append(", pFrameInfluences=");
		builder.append(pFrameInfluences != null
				? Arrays.toString(Arrays.copyOf(pFrameInfluences, Math.min(pFrameInfluences.length, maxLen))) : null);
		builder.append("]");
		return builder.toString();
	}
    
}
