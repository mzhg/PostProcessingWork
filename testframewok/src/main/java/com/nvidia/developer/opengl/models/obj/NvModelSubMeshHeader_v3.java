package com.nvidia.developer.opengl.models.obj;

import org.lwjgl.util.vector.Writable;

import java.nio.ByteBuffer;

final class NvModelSubMeshHeader_v3 implements Writable{
	static final int SIZE = 76/*Util.sizeof(NvModelSubMeshHeader_v3.class)*/;
	
	int _vertexCount;
    int _indexCount;
    int _vertexSize; // size of each vert IN BYTES!
    int _indexSize; // size of each index IN BYTES!
    int _pOffset;
    int _nOffset;
    int _tcOffset;
    int _sTanOffset;
    int _cOffset;
    int _boneIndexOffset;
    int _boneWeightOffset;
    int _posSize;
    int _tcSize;
    int _colorCount;
    int _bonesPerVertex;
    int _boneMapCount;

    // offset in bytes from the start of the block
    // (in version 1, this was the offset in bytes 
    //  from the start of the file)
    int _vertArrayBase; 

    // offset in bytes from the start of the block
    // (in version 1, this was the offset in bytes 
    //  from the start of the file)
    int _indexArrayBase;
    int _matIndex;
    
	@Override
	public Writable load(ByteBuffer buf) {
		_vertexCount = buf.getInt();
		_indexCount = buf.getInt();
		_vertexSize = buf.getInt();
		_indexSize = buf.getInt();
		_pOffset = buf.getInt();
		_nOffset = buf.getInt();
		_tcOffset = buf.getInt();
		_sTanOffset = buf.getInt();
		_cOffset = buf.getInt();
		_boneIndexOffset = buf.getInt();
		_boneWeightOffset = buf.getInt();
		_posSize = buf.getInt();
		_tcSize = buf.getInt();
		_colorCount = buf.getInt();
		_bonesPerVertex = buf.getInt();
		_boneMapCount = buf.getInt();
		_vertArrayBase = buf.getInt();
		_indexArrayBase = buf.getInt();
		_matIndex = buf.getInt();
		return null;
	}
}
