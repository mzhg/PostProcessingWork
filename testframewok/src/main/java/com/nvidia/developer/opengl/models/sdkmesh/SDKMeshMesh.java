package com.nvidia.developer.opengl.models.sdkmesh;

import org.lwjgl.util.vector.Vector3f;

import java.nio.charset.Charset;

import jet.opengl.postprocessing.util.Numeric;

final class SDKMeshMesh {

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
    
    int load(byte[] data, int offset){
    	name = new String(data, offset, SDKmesh.MAX_MESH_NAME, Charset.defaultCharset()).trim();
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
		return "SDKMeshMesh [name=" + name + ", numVertexBuffers=" + numVertexBuffers + ", indexBuffer=" + indexBuffer
				+ ", numSubsets=" + numSubsets + ", numFrameInfluences=" + numFrameInfluences + ", subsetOffset="
				+ subsetOffset + ", frameInfluenceOffset=" + frameInfluenceOffset + "]";
	}
    
    
}
