package com.nvidia.developer.opengl.models.obj;

import java.nio.ByteBuffer;

final class NvModelSkeletonDataBlockHeader {
	static final int SIZE = 8;
	
	int _boneCount;
    int _skeletonBlockSize;
    
    void load(ByteBuffer data){
    	_boneCount = data.getInt();
    	_skeletonBlockSize = data.getInt();
    }

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("NvModelSkeletonDataBlockHeader:\n_boneCount=");
		builder.append(_boneCount);
		builder.append(", _skeletonBlockSize=");
		builder.append(_skeletonBlockSize);
		builder.append("\n");
		return builder.toString();
	}
}
