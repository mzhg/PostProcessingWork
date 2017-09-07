package com.nvidia.developer.opengl.models.obj;

import org.lwjgl.util.vector.Writable;

import java.nio.ByteBuffer;
import java.util.Arrays;

final class NvModelMaterialHeader implements Writable{
	static final int SIZE = /*Util.sizeof(new NvModelMaterialHeader())*/152;
	
	int _materialBlockSize; // includes header + TextureDesc array
    final float[] _ambient = new float[3];
    final float[] _diffuse = new float[3];
    final float[] _emissive = new float[3];
	float _alpha;
	final float[] _specular = new float[3];
	float _shininess;
	float _opticalDensity;
	final float[] _transmissionFilter = new float[3];
	int _ambientTextureOffset;
    int _ambientTextureCount;
    int _diffuseTextureOffset;
    int _diffuseTextureCount;
    int _specularTextureOffset;
    int _specularTextureCount;
    int _bumpMapTextureOffset;
    int _bumpMapTextureCount;
    int _reflectionTextureOffset;
    int _reflectionTextureCount;
    int _displacementMapTextureOffset;
    int _displacementMapTextureCount;
    int _specularPowerTextureOffset;
    int _specularPowerTextureCount;
    int _alphaMapTextureOffset;
    int _alphaMapTextureCount;
    int _decalTextureOffset;
    int _decalTextureCount;
    int _illumModel;
    
	@Override
	public NvModelMaterialHeader load(ByteBuffer buf) {
		_materialBlockSize = buf.getInt();
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
		_ambientTextureOffset = buf.getInt();
		_ambientTextureCount = buf.getInt();
		_diffuseTextureOffset = buf.getInt();
		_diffuseTextureCount = buf.getInt();
		_specularTextureOffset = buf.getInt();
		_specularTextureCount = buf.getInt();
		_bumpMapTextureOffset = buf.getInt();
		_bumpMapTextureCount = buf.getInt();
		_reflectionTextureOffset = buf.getInt();
		_reflectionTextureCount = buf.getInt();
		_displacementMapTextureOffset = buf.getInt();
		_displacementMapTextureCount = buf.getInt();
		_specularPowerTextureOffset = buf.getInt();
		_specularPowerTextureCount = buf.getInt();
		_alphaMapTextureOffset = buf.getInt();
		_alphaMapTextureCount = buf.getInt();
		_decalTextureOffset = buf.getInt();
		_decalTextureCount = buf.getInt();
		_illumModel = buf.getInt();
		return this;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("NvModelMaterialHeader:\n");
		sb.append("_materialBlockSize = ").append(_materialBlockSize).append('\n');
		sb.append("_ambient = ").append(Arrays.toString(_ambient)).append('\n');
		sb.append("_diffuse = ").append(Arrays.toString(_diffuse)).append('\n');
		sb.append("_emissive = ").append(Arrays.toString(_emissive)).append('\n');
		sb.append("_alpha = ").append(_alpha).append('\n');
		sb.append("_specular = ").append(Arrays.toString(_specular)).append('\n');
		sb.append("_shininess = ").append(_shininess).append('\n');
		sb.append("_opticalDensity = ").append(_opticalDensity).append('\n');
		sb.append("_transmissionFilter = ").append(Arrays.toString(_transmissionFilter)).append('\n');
		sb.append("_ambientTextureOffset = ").append(_ambientTextureOffset).append('\n');
		sb.append("_ambientTextureCount = ").append(_ambientTextureCount).append('\n');
		sb.append("_diffuseTextureOffset = ").append(_diffuseTextureOffset).append('\n');
		sb.append("_diffuseTextureCount = ").append(_diffuseTextureCount).append('\n');
		sb.append("_specularTextureOffset = ").append(_specularTextureOffset).append('\n');
		sb.append("_specularTextureCount = ").append(_specularTextureCount).append('\n');
		sb.append("_bumpMapTextureOffset = ").append(_bumpMapTextureOffset).append('\n');
		sb.append("_bumpMapTextureCount = ").append(_bumpMapTextureCount).append('\n');
		sb.append("_reflectionTextureOffset = ").append(_reflectionTextureOffset).append('\n');
		sb.append("_reflectionTextureCount = ").append(_reflectionTextureCount).append('\n');
		sb.append("_displacementMapTextureOffset = ").append(_displacementMapTextureOffset).append('\n');
		sb.append("_displacementMapTextureCount = ").append(_displacementMapTextureCount).append('\n');
		sb.append("_specularPowerTextureOffset = ").append(_specularPowerTextureOffset).append('\n');
		sb.append("_specularPowerTextureCount = ").append(_specularPowerTextureCount).append('\n');
		sb.append("_alphaMapTextureOffset = ").append(_alphaMapTextureOffset).append('\n');
		sb.append("_alphaMapTextureCount = ").append(_alphaMapTextureCount).append('\n');
		sb.append("_decalTextureOffset = ").append(_decalTextureOffset).append('\n');
		sb.append("_decalTextureCount = ").append(_decalTextureCount).append('\n');
		sb.append("_illumModel = ").append(_illumModel).append('\n');
		return sb.toString();
	}
	
}
