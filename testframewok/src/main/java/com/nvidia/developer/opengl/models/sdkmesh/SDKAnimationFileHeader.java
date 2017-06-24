package com.nvidia.developer.opengl.models.sdkmesh;

import java.io.DataInputStream;
import java.io.IOException;

final class SDKAnimationFileHeader {

	static final int SIZE_BYTES = 20 + 1 + 8;
	
	int version;
    boolean isBigEndian;
    int frameTransformType;
    int numFrames;
    int numAnimationKeys;
    int animationFPS;
    long animationDataSize;
    long animationDataOffset;
    
    void read(DataInputStream in) throws IOException {
    	version = Integer.reverseBytes(in.readInt());
    	isBigEndian = in.readBoolean();  in.skip(3);
    	frameTransformType = Integer.reverseBytes(in.readInt());
    	numFrames = Integer.reverseBytes(in.readInt());
    	numAnimationKeys = Integer.reverseBytes(in.readInt());
    	animationFPS = Integer.reverseBytes(in.readInt());
    	animationDataSize = Long.reverseBytes(in.readLong());
    	animationDataOffset = Long.reverseBytes(in.readLong());
    }

	@Override
	public String toString() {
		return "SDKAnimationFileHeader [version=" + version + ", isBigEndian=" + isBigEndian + ", frameTransformType="
				+ frameTransformType + ",\n numFrames=" + numFrames + ", numAnimationKeys=" + numAnimationKeys
				+ ", animationFPS=" + animationFPS + ",\n animationDataSize=" + animationDataSize
				+ ", animationDataOffset=" + animationDataOffset + "]";
	}
}
