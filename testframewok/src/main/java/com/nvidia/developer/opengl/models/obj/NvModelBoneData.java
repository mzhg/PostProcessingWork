package com.nvidia.developer.opengl.models.obj;

import org.lwjgl.util.vector.Writable;

import java.nio.ByteBuffer;

final class NvModelBoneData implements Writable{
	static final int SIZE = (4 + 16) * 4;
	
	int _parentIndex;
    final float[] _parentRelTransform = new float[16];
    int _nameLength;
    int _numChildren;
    int _numMeshes;
    
	@Override
	public Writable load(ByteBuffer buf) {
		_parentIndex = buf.getInt();
		for(int i = 0; i < _parentRelTransform.length; i++)
			_parentRelTransform[i] = buf.getFloat();
		_nameLength = buf.getInt();
		_numChildren = buf.getInt();
		_numMeshes = buf.getInt();
		return this;
	}
}
