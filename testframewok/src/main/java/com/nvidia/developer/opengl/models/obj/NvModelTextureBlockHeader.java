package com.nvidia.developer.opengl.models.obj;

import org.lwjgl.util.vector.Writable;

import java.nio.ByteBuffer;

final class NvModelTextureBlockHeader implements Writable{
	static final int SIZE = 8;
	
	int _textureCount;
	int _textureBlockSize;  //
	@Override
	
	public Writable load(ByteBuffer buf) {
		_textureCount = buf.getInt();
		_textureBlockSize = buf.getInt();
		return this;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("NvModelTextureBlockHeader :\n_textureCount=");
		builder.append(_textureCount);
		builder.append(", _textureBlockSize=");
		builder.append(_textureBlockSize);
		builder.append("\n");
		return builder.toString();
	}
}
