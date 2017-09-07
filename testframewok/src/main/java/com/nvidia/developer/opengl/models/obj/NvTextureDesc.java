package com.nvidia.developer.opengl.models.obj;

/** Structure to hold a texture index with associated sampling and addressing parameters */
public class NvTextureDesc {

	public static final int 
					MapMode_Wrap = 0x0, /// Wrapping
				    MapMode_Clamp = 0x1, /// Clamp addressing
				    MapMode_Mirror = 0x2, /// Mirror-wrapped addressing
				    MapMode_Force32Bit = 0x7FFFFFFF;
	
	public static final int
					FilterMode_Nearest = 0x0, /// Nearest filtering
				    FilterMode_Linear = 0x1, /// Lerp filtering
				    FilterMode_Force32Bit = 0x7FFFFFFF;
	
	public int m_textureIndex;
	public int m_UVIndex;
	public int[] m_mapModes = new int[3];
	public int m_minFilter;
}
