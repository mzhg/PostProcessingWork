package com.nvidia.developer.opengl.models.sdkmesh;

import org.lwjgl.util.vector.Matrix4f;

import jet.opengl.postprocessing.util.Numeric;

final class SDKMeshFrame {

//	char Name[MAX_FRAME_NAME];
	String name;       // 100
    int mesh;          // 104
    int parentFrame;   // 108
    int childFrame;    // 112
    int siblingFrame;  // 116
    final Matrix4f matrix = new Matrix4f();  // 180
    int animationDataIndex;		// 184 Used to index which set of keyframes transforms this frame
    
    int load(byte[] data, int position){
    	name = new String(data, position, SDKmesh.MAX_FRAME_NAME).trim(); position+=SDKmesh.MAX_FRAME_NAME;
    	
    	mesh = Numeric.getInt(data, position); position += 4;
    	parentFrame = Numeric.getInt(data, position); position += 4;
    	childFrame = Numeric.getInt(data, position); position += 4;
    	siblingFrame = Numeric.getInt(data, position); position += 4;
    	
//    	matrix.m00 = Numeric.getFloat(data, position); position += 4;
    	for(int i = 0; i < 16; i++){
    		matrix.set(i, Numeric.getFloat(data, position), false);
    		position += 4;
    	}
    	
    	animationDataIndex = Numeric.getInt(data, position); position += 4;
    	return position;
    }

	@Override
	public String toString() {
		return "SDKMeshFrame [name=" + name + ", mesh=" + mesh + ", parentFrame=" + parentFrame + ", childFrame="
				+ childFrame + ", siblingFrame=" + siblingFrame + ", matrix=" + matrix + ", animationDataIndex="
				+ animationDataIndex + "]";
	}
}
