package com.nvidia.developer.opengl.models.sdkmesh;

import java.util.Arrays;

import jet.opengl.postprocessing.util.Numeric;

final class SDKAnimationFrameData {

//	char FrameName[MAX_FRAME_NAME];
	String frameName;
	SDKAnimationData[] pAnimationData;
	long dataOffset;
	
	int load(byte[] data, int position){
		frameName = new String(data, position, SDKmesh.MAX_FRAME_NAME).trim();
		position += SDKmesh.MAX_FRAME_NAME;   // TODO Need +4 ?
		
		dataOffset = Numeric.getLong(data, position); position += 8;
		return position;
	}

	@Override
	public String toString() {
		return "SDKAnimationFrameData [frameName=" + frameName + ", pAnimationData=" + Arrays.toString(pAnimationData)
				+ ", dataOffset=" + dataOffset + "]";
	}
	
}
