package com.nvidia.developer.opengl.models.obj;

import org.lwjgl.util.vector.Writable;

import java.nio.ByteBuffer;

final class NvModelTextureDesc implements Writable{

	static final int SIZE = 24;
	
	static final int 
			MapMode_Wrap = 0x0,
		    MapMode_Clamp = 0x1,
		    MapMode_Mirror = 0x2,
		    MapMode_Force32Bit = 0x7FFFFFFF;
	
	static final int 
			FilterMode_Nearest = 0x0,
		    FilterMode_Linear = 0x1,
		    FilterMode_Force32Bit = 0x7FFFFFFF;
	
	int _textureIndex;
	int _UVIndex;
	int _mapModeS;
	int _mapModeT;
	int _mapModeU;
	int _minFilter;
	@Override
	public Writable load(ByteBuffer buf) {
		_textureIndex = buf.getInt();
		_UVIndex = buf.getInt();
		_mapModeS = buf.getInt();
		_mapModeT = buf.getInt();
		_mapModeU = buf.getInt();
		_minFilter = buf.getInt();
		return null;
	}
	
}
