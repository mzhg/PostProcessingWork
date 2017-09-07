package com.nvidia.developer.opengl.models.obj;

import org.lwjgl.util.vector.Writable;

import java.nio.ByteBuffer;
import java.util.Arrays;

final class NvModelExtFileHeader implements Writable{

	static final int SIZE = 4 + 4 * 4 + 4 * 3 * 3;
	
	final byte[] _magic= new byte[4];
	int _headerSize; // includes magic
	int _version;
	int _subMeshCount;
	int _matCount;
	final float[] _boundingBoxMin = new float[3];
	final float[] _boundingBoxMax = new float[3];
	final float[] _boundingBoxCenter = new float[3];
	@Override
	public NvModelExtFileHeader load(ByteBuffer buf) {
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
		return this;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("NvModelExtFileHeader:\n");
		sb.append("_magic = ").append(_magic).append('\n');
		sb.append("_headerSize = ").append(_headerSize).append('\n');
		sb.append("_version = ").append(_version).append('\n');
		sb.append("_subMeshCount = ").append(_subMeshCount).append('\n');
		sb.append("_matCount = ").append(_matCount).append('\n');
		sb.append("_boundingBoxMin = ").append(Arrays.toString(_boundingBoxMin)).append('\n');
		sb.append("_boundingBoxMax = ").append(Arrays.toString(_boundingBoxMax)).append('\n');
		sb.append("_boundingBoxCenter = ").append(Arrays.toString(_boundingBoxCenter)).append('\n');
		return sb.toString();
	}
}
