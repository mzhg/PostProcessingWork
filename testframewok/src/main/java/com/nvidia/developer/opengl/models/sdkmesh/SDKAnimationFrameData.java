package com.nvidia.developer.opengl.models.sdkmesh;

import java.util.Arrays;

import jet.opengl.postprocessing.util.Numeric;

final class SDKAnimationFrameData {

	static final int SIZE = 104+8;
	
//	char FrameName[MAX_FRAME_NAME];
	String frameName;  // 100
	SDKAnimationData[] pAnimationData;
	long dataOffset;
	
	int load(byte[] data, int position){
		frameName = SDKmesh.getString(data, position, SDKmesh.MAX_FRAME_NAME);
		position += SDKmesh.MAX_FRAME_NAME;   // TODO Need +4 ?
		
		position += 4; // padding
		dataOffset = Numeric.getLong(data, position); position += 8;
		return position;
	}

	@Override
	public String toString() {
		return "SDKAnimationFrameData [frameName=" + frameName + ", pAnimationData=" + Arrays.toString(pAnimationData)
				+ ", dataOffset=" + dataOffset + "]";
	}
	
}
