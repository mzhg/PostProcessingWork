package com.nvidia.developer.opengl.models.sdkmesh;

import java.io.DataInputStream;
import java.io.IOException;

import jet.opengl.postprocessing.util.Numeric;

final class SDKAnimationFileHeader {

	static final int SIZE = 6 * 4 + 2 * 8;
	
	int version;
    boolean isBigEndian;
    int frameTransformType;
    int numFrames;
    int numAnimationKeys;
    int animationFPS;
    long animationDataSize;
    long animationDataOffset;
    
    int load(byte[] data, int position){
    	version = Numeric.getInt(data, position); position += 4;
    	isBigEndian = Numeric.getInt(data, position) != 0; position += 4;
    	frameTransformType = Numeric.getInt(data, position); position += 4;
    	numFrames = Numeric.getInt(data, position); position += 4;
    	numAnimationKeys = Numeric.getInt(data, position); position += 4;
    	animationFPS = Numeric.getInt(data, position); position += 4;
    	
    	animationDataSize = Numeric.getLong(data, position); position += 8;
    	animationDataOffset = Numeric.getLong(data, position); position += 8;
    	
    	return position;
    }
    
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
