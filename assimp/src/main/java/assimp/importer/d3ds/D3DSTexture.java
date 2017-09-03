package assimp.importer.d3ds;

import assimp.common.TextureMapMode;

/** Helper structure representing a texture */
public  class D3DSTexture {

	/** Specifies the blend factor for the texture*/
	public float mTextureBlend;

	/** Specifies the filename of the texture*/
	public String mMapName;

	/** Specifies texture coordinate offsets/scaling/rotations*/
	public float mOffsetU;
	public float mOffsetV;
	public float mScaleU = 1.0f;
	public float mScaleV = 1.0f;
	public float mRotation;

	/** Specifies the mapping mode to be used for the texture*/
	public TextureMapMode mMapMode = TextureMapMode.aiTextureMapMode_Wrap;

	/** Used internally*/
	public boolean bPrivate;
	public int iUVSrc;
}
