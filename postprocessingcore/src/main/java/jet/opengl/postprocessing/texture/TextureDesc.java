package jet.opengl.postprocessing.texture;

import java.util.Arrays;

/**
 * Texture parameters control how the texel array is treated when specified or changed, and when 
 * applied to a fragment.
 */
public class TextureDesc {
	
	TextureDesc() {
	}

	/**
	 * <code>target</code> is the texture target, and must be one of <b>TEXTURE_1D, TEXTURE_2D,
	 * TEXTURE_3D, TEXTURE_1D_ARRAY, TEXTURE_2D_ARRAY. TEXTURE_RECTANGLE,
	 * TEXTURE_CUBE_MAP, TEXTURE_CUBE_MAP_ARRAY, TEXTURE_2D_MULTISAMPLE,</b> or
	 * <b>TEXTURE_2D_MULTISAMPLE_ARRAY</b>.
	 */
	public int target;
	
	/**
	 * The depth texture mode. Legal Values are <b>RED, LUMINANCE, INTENSITY, ALPHA</b>.<p><b>NOTE:<b> The field has deprecated in the OpenGL core profile.
	 */
	public int depthTextureMode;
	
	/**Legal Values are <b>DEPTH_COMPONENT, STENCIL_INDEX</b> */
	public int depthStencilTextureMode;
	
	public int baseLevel;
	
	public final float[] borderColor = new float[4];
	
	/**  */
	public int compareMode;
	
	public int compareFunc;
	
	public float lodBias;
	
	public int magFilter;
	public int minFilter;
	
	public int maxLevel;
	public float maxLod;
	public float minLod;
	
	public int swizzleR;
	public int swizzleG;
	public int swizzleB;
	public int swizzleA;
	public int wrapS;
	public int wrapT;
	public int wrapR;
	
	public int textureViewMinLevel;
	public int textureViewNumLevels;
	public int textureViewMinLayer;
	public int textureViewNumLayers;
	
	public int immutableLayer;
	public int imageFormatCompatibilityType;
	
	public boolean immutableFormat;
	public TextureLevelDesc[] levelDescs;
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(256);
		sb.append("target: ").append(TextureUtils.getTextureTargetName(target)).append('\n');
		sb.append("depthTextureMode: ").append(TextureUtils.getDepthTextureModeName(depthStencilTextureMode)).append('\n');
		sb.append("baseLevel: ").append(baseLevel).append('\n');
		sb.append("borderColor: ").append(Arrays.toString(borderColor)).append('\n');
		sb.append("compareMode: ");
		if(compareMode != 0)
			sb.append("GL_COMPARE_REF_TO_TEXTURE").append('\n');
		else
			sb.append("GL_NONE").append('\n');
		sb.append("compareFunc: ").append(TextureUtils.getCompareModeName(compareFunc)).append('\n');
		sb.append("lodBias: ").append(lodBias).append('\n');
		
		sb.append("magFilter: ").append(TextureUtils.getTextureFilterName(magFilter)).append('\n');
		sb.append("minFilter: ").append(TextureUtils.getTextureFilterName(minFilter)).append('\n');
		
		sb.append("maxLevel: ").append(maxLevel).append('\n');
		sb.append("maxLod: ").append(maxLod).append('\n');
		sb.append("minLod: ").append(minLod).append('\n');
		
		sb.append("swizzleR: ").append(TextureUtils.getTextureSwizzleName(swizzleR)).append('\n');
		sb.append("swizzleG: ").append(TextureUtils.getTextureSwizzleName(swizzleG)).append('\n');
		sb.append("swizzleB: ").append(TextureUtils.getTextureSwizzleName(swizzleB)).append('\n');
		sb.append("swizzleA: ").append(TextureUtils.getTextureSwizzleName(swizzleA)).append('\n');
		
		sb.append("wrapS: ").append(TextureUtils.getTextureWrapName(wrapS)).append('\n');
		sb.append("wrapT: ").append(TextureUtils.getTextureWrapName(wrapT)).append('\n');
		sb.append("wrapR: ").append(TextureUtils.getTextureWrapName(wrapR)).append('\n');
		
		sb.append("textureViewMinLevel: ").append(textureViewMinLevel).append('\n');
		sb.append("textureViewNumLevels: ").append(textureViewNumLevels).append('\n');
		sb.append("textureViewMinLayer: ").append(textureViewMinLayer).append('\n');
		sb.append("textureViewNumLayers: ").append(textureViewNumLayers).append('\n');
		sb.append("immutableLayer: ").append(immutableLayer).append('\n');
		sb.append("imageFormatCompatibilityType: ").append(imageFormatCompatibilityType).append('\n');
		sb.append("immutableFormat: ").append(immutableFormat).append('\n');
		
		if(levelDescs != null){
			int idx = 0;
			sb.append("mipLevels: ").append(levelDescs.length).append('\n').append('\n');
			for(TextureLevelDesc levelDesc : levelDescs){
				sb.append("--------------------------------------Texture Level").append(idx).append(" informations:---------------------------------------\n");
				sb.append(levelDesc.toString()).append('\n');
				idx++;
			}
		}
		
		return sb.toString();
	}
}
