package com.nvidia.developer.opengl.models.obj;

import org.lwjgl.util.vector.Writable;

import java.nio.ByteBuffer;

final class NvModelMaterialHeader_v1 implements Writable{
	final float[] _ambient = new float[3];
	final float[] _diffuse = new float[3];
	final float[] _emissive = new float[3];
	float _alpha;
	final float[] _specular = new float[3];
	float _shininess;
	float _opticalDensity;
	final float[] _transmissionFilter = new float[3];
	int _ambientTexture;
	int _diffuseTexture;
	int _specularTexture;
	int _bumpMapTexture;
	int _reflectionTexture;
	int _displacementMapTexture;
	int _specularPowerTexture;
	int _alphaMapTexture;
	int _decalTexture;
	int _illumModel;
	
	@Override
	public NvModelMaterialHeader_v1 load(ByteBuffer buf) {
		for(int i = 0; i < _ambient.length; i++)
			_ambient[i] = buf.getFloat();
		for(int i = 0; i < _diffuse.length; i++)
			_diffuse[i] = buf.getFloat();
		for(int i = 0; i < _emissive.length; i++)
			_emissive[i] = buf.getFloat();
		_alpha = buf.getFloat();
		for(int i = 0; i < _specular.length; i++)
			_specular[i] = buf.getFloat();
		_shininess = buf.getFloat();
		_opticalDensity = buf.getFloat();
		for(int i = 0; i < _transmissionFilter.length; i++)
			_transmissionFilter[i] = buf.getFloat();
		_ambientTexture = buf.getInt();
		_diffuseTexture = buf.getInt();
		_specularTexture = buf.getInt();
		_bumpMapTexture = buf.getInt();
		_reflectionTexture = buf.getInt();
		_displacementMapTexture = buf.getInt();
		_specularPowerTexture = buf.getInt();
		_alphaMapTexture = buf.getInt();
		_decalTexture = buf.getInt();
		_illumModel = buf.getInt();

		return this;
	}
}
