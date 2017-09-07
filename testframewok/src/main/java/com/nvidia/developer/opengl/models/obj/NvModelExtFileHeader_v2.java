package com.nvidia.developer.opengl.models.obj;

import org.lwjgl.util.vector.Writable;

import java.nio.ByteBuffer;

final class NvModelExtFileHeader_v2 implements Writable{

	final byte[] _magic = new byte[4];
    int _headerSize; // includes magic
    int _version;
    int _subMeshCount;
    int _matCount;
    final float[] _boundingBoxMin = new float[3];
    final float[] _boundingBoxMax = new float[3];
    final float[] _boundingBoxCenter = new float[3];
    int _textureCount;
    int _textureBlockSize;
    
	@Override
	public NvModelExtFileHeader_v2 load(ByteBuffer buf) {
		buf.get(_magic);
		_headerSize = buf.getInt();
		_version = buf.getInt();
		_subMeshCount = buf.getInt();
		_matCount = buf.getInt();
		for(int i = 0; i < _boundingBoxMin.length; i++)
			_boundingBoxMin[i] = buf.getFloat();
		for(int i = 0; i < _boundingBoxMax.length; i++)
			_boundingBoxMax[i] = buf.getFloat();
		for(int i = 0; i < _boundingBoxCenter.length; i++)
			_boundingBoxCenter[i] = buf.getFloat();
		_textureCount = buf.getInt();
		_textureBlockSize = buf.getInt();
		return this;
	}
}
