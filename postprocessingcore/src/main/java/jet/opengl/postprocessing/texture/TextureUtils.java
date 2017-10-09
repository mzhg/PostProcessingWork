package jet.opengl.postprocessing.texture;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jet.opengl.postprocessing.common.GLAPIVersion;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLStateTracker;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.BufferUtils;
import jet.opengl.postprocessing.util.CachaRes;
import jet.opengl.postprocessing.util.CacheBuffer;

public final class TextureUtils {
	private static final int RED = GLenum.GL_RED;
	private static final int RG  = GLenum.GL_RG;
	private static final int RGB = GLenum.GL_RGB;
	private static final int RGBA= GLenum.GL_RGBA;
	
	private static final int RED_INTEGER = GLenum.GL_RED_INTEGER;
	private static final int RG_INTEGER = GLenum.GL_RG_INTEGER;
	private static final int RGB_INTEGER = GLenum.GL_RGB_INTEGER;
	private static final int RGBA_INTEGER = GLenum.GL_RGBA_INTEGER;
	
	private static final int[] compressed_formats = {
			0x8225,  // COMPRESSED_RED
			0x8226,  // COMPRESSED_RG
			0x84ed,  // COMPRESSED_RGB
			0x84ee,  // COMPRESSED_RGBA
			0x8c48,  // COMPRESSED_SRGB
			0x8c49,  // COMPRESSED_SRGB_ALPHA
			0x8dbb,  // COMPRESSED_RED_RGTC1
			0x8dbc,  // COMPRESSED_SIGNED_RED_RGTC1
			0x8dbd,  // COMPRESSED_RG_RGTC2
			0x8dbe,  // COMPRESSED_SIGNED_RG_RGTC2
			0x8e8c,  // COMPRESSED_RGBA_BPTC_UNORM
			0x8e8d,  // COMPRESSED_SRGB_ALPHA_BPTC_UNORM
			0x8e8e,  // COMPRESSED_RGB_BPTC_SIGNED_FLOAT
			0x8e8f,  // GL_COMPRESSED_RGB_BPTC_UNSIGNED_FLOAT
			0x9274,  // GL_COMPRESSED_RGB8_ETC2
			0x9275,  // GL_COMPRESSED_SRGB8_ETC2
			0x9276,  // GL_COMPRESSED_RGB8_PUNCHTHROUGH_ALPHA1_ETC2
			0x9277,  // GL_COMPRESSED_SRGB8_PUNCHTHROUGH_ALPHA1_ETC2
			0x9278,  // GL_COMPRESSED_RGBA8_ETC2_EAC
			0x9279,  // GL_COMPRESSED_SRGB8_ALPHA8_ETC2_EAC
			0x9270,  // GL_COMPRESSED_R11_EAC
			0x9271,  // GL_COMPRESSED_SIGNED_R11_EAC
			0x9272,  // GL_COMPRESSED_RG11_EAC
			0x9273,  // GL_COMPRESSED_SIGNED_RG11_EAC
	};
	
	static {
		Arrays.sort(compressed_formats);
	}
	
	private TextureUtils(){}
	
	/** Get the texture image Data. Call this method would change the texture binding.*/
	public static ByteBuffer getTextureData(int target, int textureID, int level, boolean fromCache){
		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
		gl.glBindBuffer(GLenum.GL_PIXEL_PACK_BUFFER, 0);
		gl.glBindTexture(target, textureID);
		GLCheck.checkError();
		int width = gl.glGetTexLevelParameteri(target, level, GLenum.GL_TEXTURE_WIDTH);
		int height = gl.glGetTexLevelParameteri(target, level, GLenum.GL_TEXTURE_HEIGHT);
		int depth = gl.glGetTexLevelParameteri(target, level, GLenum.GL_TEXTURE_DEPTH);
		if(width == 0 && height == 0 && depth == 0)
			return fromCache ? CacheBuffer.getCachedByteBuffer(0) : BufferUtils.createByteBuffer(0);
		
		int red_bits = gl.glGetTexLevelParameteri(target, level, GLenum.GL_TEXTURE_RED_SIZE);
		int green_bits = gl.glGetTexLevelParameteri(target, level, GLenum.GL_TEXTURE_GREEN_SIZE);
		int blue_bits = gl.glGetTexLevelParameteri(target, level, GLenum.GL_TEXTURE_BLUE_SIZE);
		int alpha_bits = gl.glGetTexLevelParameteri(target, level, GLenum.GL_TEXTURE_ALPHA_SIZE);
		int depth_bits = gl.glGetTexLevelParameteri(target, level, GLenum.GL_TEXTURE_DEPTH_SIZE);
		int stencil_bits = gl.glGetTexLevelParameteri(target, level, GLenum.GL_TEXTURE_STENCIL_SIZE);
		int internalFormat = gl.glGetTexLevelParameteri(target, level, GLenum.GL_TEXTURE_INTERNAL_FORMAT);
		
//		System.out.println("red_bits = " + red_bits);
//		System.out.println("green_bits = " + green_bits);
//		System.out.println("blue_bits = " + blue_bits);
//		System.out.println("alpha_bits = " + alpha_bits);
		
		GLCheck.checkError();
		
		int totalBytes = Math.max(width, 1) * Math.max(height, 1) * Math.max(depth, 1) * 
				(red_bits + green_bits + blue_bits + alpha_bits + depth_bits + stencil_bits) /8;
//		totalBytes = Math.min(2048 * 2048 * 2, totalBytes);
		
		ByteBuffer result = fromCache ?  CacheBuffer.getCachedByteBuffer(totalBytes) : BufferUtils.createByteBuffer(totalBytes);
		int type = measureDataType(internalFormat);
		int format = measureFormat(internalFormat);
//		System.out.println("internalFormat = " + internalFormat);
//		System.out.println("format = " + getFormatName(format));
//		System.out.println("type = " + getTypeName(type));
//		System.out.println("totalBytes = " + totalBytes);
		gl.glGetTexImage(target, level, format, type, result);
//		System.out.println("totalBytes = " + totalBytes);
		GLCheck.checkError();
		return result;
	}

	@CachaRes
	public static ImageData getFramebufferColorData(int internalFormat, boolean fromCache){
		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();

		int fbo = gl.glGetInteger(0x8CA6);
		if(fbo != 0){
			gl.glReadBuffer(GLenum.GL_COLOR_ATTACHMENT0);
		}
		
		IntBuffer params = CacheBuffer.getCachedIntBuffer(4);
		gl.glGetIntegerv(GLenum.GL_VIEWPORT, params);
		
		@SuppressWarnings("unused")
		int x = params.get();
		@SuppressWarnings("unused")
		int y = params.get();
		int w = params.get();
		int h = params.get();
		
		int format = measureFormat(internalFormat);
		int type   = measureDataType(internalFormat);
		
		int totalBytes = (int) (w * h * measureSizePerPixel(internalFormat));  // RGB color
		ByteBuffer result = fromCache ? CacheBuffer.getCachedByteBuffer(totalBytes) : BufferUtils.createByteBuffer(totalBytes);
		gl.glReadPixels(0, 0, w, h, format, type, result);
		
		ImageData data = new ImageData();
		data.width = w;
		data.height = h;
		data.internalFormat = internalFormat;
		data.pixels = result;
		return data;
	}

	public static Texture2D createTexture2DFromFile(String filename, boolean flip,Texture2D out) throws IOException{
		return createTexture2DFromFile(filename, flip, false, out);
	}

	public static Texture2D createTexture2DFromFile(String filename, boolean flip) throws IOException {
		return createTexture2DFromFile(filename, flip, false, null);
	}

	public static Texture2D createTexture2DFromFile(String filename, boolean flip, boolean genmipmap, Texture2D out) throws IOException{
		ImageLoader loader = GLFuncProviderFactory.getGLFuncProvider().getImageLoader();
		ImageData data = loader.load(filename, flip);

		Texture2DDesc desc = new Texture2DDesc(data.width, data.height, data.internalFormat);
		if(genmipmap){
			desc.mipLevels = (int) (Math.log(Math.max(data.width, data.height))/Math.log(2));
		}

		return createTexture2D(desc, new TextureDataDesc(measureFormat(data.internalFormat), GLenum.GL_UNSIGNED_BYTE, data.pixels), out);
	}

	public static Texture2D createTexture2DFromFile(String filename, boolean flip, boolean genmipmap) throws IOException {
		return createTexture2DFromFile(filename, flip, genmipmap, null);
	}

	public static void flipY(ByteBuffer bytes, int height){
		// flip the image.
		final int widthBytes = bytes.remaining()/ height;
		byte[] firstRow = new byte[widthBytes];
		byte[] secondRow = new byte[widthBytes];
		
		for(int i = 0; i < height/2; i++){
			// Read the first row
			bytes.position(i * widthBytes);
			bytes.get(firstRow);
			
			// Read the second row
			bytes.position((height -1 - i) * widthBytes);
			bytes.get(secondRow);
			
			// Put the secondRow in the location of the first row.
			bytes.position(i * widthBytes);
			bytes.put(secondRow);
			
			// Put the firstRow in the location of the second row.
			bytes.position((height -1 - i) * widthBytes);
			bytes.put(firstRow);
		}
		
		// reset the position.
		bytes.position(0);
	}
	
	public static Texture2D createTextureView(TextureGL source, int target, int minlevel, int numlevels, int minlayer, int numlayers){
		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
		int texture = gl.glGenTexture();
		gl.glTextureView(texture, target, source.getTexture(), source.getFormat(), minlevel, numlevels, minlayer, numlayers);
		GLCheck.checkError();
		
		Texture2D result = new Texture2D();
		result.arraySize = numlayers;
		result.format = source.format;
		result.height = source.getHeight();
		result.width  = source.getWidth();
		result.target = target;
		result.textureID = texture;
		result.mipLevels = numlayers;
		result.samples = source.getSampleCount();
		return result;
	}
	
	public static Texture3D createTexture3DView(TextureGL source, int minlevel, int numlevels, int minlayer, int numlayers){
		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
		int texture = gl.glGenTexture();
		gl.glTextureView(texture, GLenum.GL_TEXTURE_3D, source.getTexture(), source.getFormat(), minlevel, numlevels, minlayer, numlayers);
		GLCheck.checkError();
		
		Texture3D result = new Texture3D();
		result.depth = numlayers;
		result.format = source.format;
		result.height = source.getHeight();
		result.width  = source.getWidth();
		result.target = GLenum.GL_TEXTURE_3D;
		result.textureID = texture;
		result.mipLevels = numlevels;
		return result;
	}
	
	public static Texture2D createTexture2D(int target, int textureID){
		return createTexture2D(target, textureID, null);
	}
	
	public static Texture2D createTexture2D(int target, int textureID, Texture2D out){
		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();

		if(!gl.glIsTexture(textureID))
			return null;
		
		if(target != GLenum.GL_TEXTURE_2D && target != GLenum.GL_TEXTURE_2D_ARRAY &&
		   target != GLenum.GL_TEXTURE_2D_MULTISAMPLE && target != GLenum.GL_TEXTURE_2D_MULTISAMPLE_ARRAY)
			throw new IllegalArgumentException("Invalid target: " + getTextureTargetName(target));

		gl.glBindTexture(target, textureID);
		Texture2D result = out != null ? out : new Texture2D();
		result.width  = gl.glGetTexLevelParameteri(target, 0, GLenum.GL_TEXTURE_WIDTH);
		result.height = gl.glGetTexLevelParameteri(target, 0, GLenum.GL_TEXTURE_HEIGHT);
		result.format = gl.glGetTexLevelParameteri(target, 0, GLenum.GL_TEXTURE_INTERNAL_FORMAT);
		result.samples= gl.glGetTexLevelParameteri(target, 0, GLenum.GL_TEXTURE_SAMPLES);
		result.target = target;
		result.textureID = textureID;
		
	    boolean immutableFormat         = gl.glGetTexParameteri(target, GLenum.GL_TEXTURE_IMMUTABLE_FORMAT) != 0;
	    if(immutableFormat){
	    	result.mipLevels    = gl.glGetTexParameteri(target, GLenum.GL_TEXTURE_VIEW_NUM_LEVELS);
	    	result.arraySize    = gl.glGetTexParameteri(target, GLenum.GL_TEXTURE_VIEW_NUM_LAYERS);
	    	if(result.mipLevels == 0){
	    		result.mipLevels = gl.glGetTexParameteri(target, GLenum.GL_TEXTURE_IMMUTABLE_LEVELS);
	    	}
	    }else{
	    	result.arraySize    = gl.glGetTexLevelParameteri(target, 0, GLenum.GL_TEXTURE_DEPTH);
	    	if(result.width > 0){
		    	int level = 1;
		    	while(true){
		    		int width = gl.glGetTexLevelParameteri(target, level, GLenum.GL_TEXTURE_WIDTH);
		    		if(width == 0){
		    			break;
		    		}
		    		
		    		level++;
		    	}
		    	
		    	result.mipLevels = level;
	    	}
	    }
		return result;
	}

	public static Texture3D createTexture3D(int target, int textureID){
		return createTexture3D(target, textureID, null);
	}

	public static Texture3D createTexture3D(int target, int textureID, Texture3D out){
		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();

		if(!gl.glIsTexture(textureID))
			return null;

		if(target != GLenum.GL_TEXTURE_3D)
			throw new IllegalArgumentException("Invalid target: " + getTextureTargetName(target));

		gl.glBindTexture(target, textureID);
		Texture3D result = out != null ? out : new Texture3D();
		result.width  = gl.glGetTexLevelParameteri(target, 0, GLenum.GL_TEXTURE_WIDTH);
		result.height = gl.glGetTexLevelParameteri(target, 0, GLenum.GL_TEXTURE_HEIGHT);
		result.depth    = gl.glGetTexLevelParameteri(target, 0, GLenum.GL_TEXTURE_DEPTH);
		result.format = gl.glGetTexLevelParameteri(target, 0, GLenum.GL_TEXTURE_INTERNAL_FORMAT);
		result.target = target;
		result.textureID = textureID;

		boolean immutableFormat         = gl.glGetTexParameteri(target, GLenum.GL_TEXTURE_IMMUTABLE_FORMAT) != 0;
		if(immutableFormat){
			result.mipLevels    = gl.glGetTexParameteri(target, GLenum.GL_TEXTURE_VIEW_NUM_LEVELS);
			if(result.mipLevels == 0){
				result.mipLevels = gl.glGetTexParameteri(target, GLenum.GL_TEXTURE_IMMUTABLE_LEVELS);
			}
		}else{

			if(result.width > 0){
				int level = 1;
				while(true){
					int width = gl.glGetTexLevelParameteri(target, level, GLenum.GL_TEXTURE_WIDTH);
					if(width == 0){
						break;
					}

					level++;
				}

				result.mipLevels = level;
			}
		}
		return result;
	}
	
	public static boolean isTexture2D(int target){
		if(target == GLenum.GL_TEXTURE_2D || target == GLenum.GL_TEXTURE_2D_MULTISAMPLE)
			return true;
		
		return false;
	}
	
	public static boolean isTextureLayered(int target){
		if(target == GLenum.GL_TEXTURE_3D || target == GLenum.GL_TEXTURE_CUBE_MAP || target == GLenum.GL_TEXTURE_2D_ARRAY
				||target == GLenum.GL_TEXTURE_1D_ARRAY || target == GLenum.GL_TEXTURE_2D_MULTISAMPLE_ARRAY)
			return true;
		return false;
	}
	
	public static Texture3D createTexture3D(Texture3DDesc textureDesc, TextureDataDesc dataDesc){
		return createTexture3D(textureDesc, dataDesc, null);
	}

	public static boolean isCompressedFormat(int format){
		return Arrays.binarySearch(compressed_formats, format) >= 0;
	}
	
	@SuppressWarnings("unchecked")
	public static Texture3D createTexture3D(Texture3DDesc textureDesc, TextureDataDesc dataDesc, Texture3D out){
		int textureID;
		int target = GLenum.GL_TEXTURE_3D;
		int format;
		boolean isCompressed = false;
		boolean isDSA; // = GL.getCapabilities().GL_ARB_direct_state_access;
		int mipLevels = Math.max(1, textureDesc.mipLevels);

		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
		GLAPIVersion version = gl.getGLAPIVersion();
		isDSA = version.major >= 4 && version.minor >= 5; /*gl.isSupportExt("GL_ARB_direct_state_access")*/;  // We only use the standrad profile.
		
		// measure texture internal format
		if(dataDesc != null){
			isCompressed = Arrays.binarySearch(compressed_formats, dataDesc.format) >= 0;
			if(isCompressed){
				format = dataDesc.format;
			}else{
				format = textureDesc.format;
			}
		}else{
			format = textureDesc.format;
		}
		
		if(isDSA && !isCompressed){
			// 1. Generate texture ID
			textureID = gl.glCreateTextures(target);
			if(valid_texture2D) GLCheck.checkError();
			
			// 2. Allocate storage for Texture Object
			gl.glTextureStorage3D(textureID, mipLevels, format, textureDesc.width, textureDesc.height, textureDesc.depth);
			gl.glTextureParameteri(textureID, GLenum.GL_TEXTURE_WRAP_R, GLenum.GL_CLAMP_TO_EDGE);
			gl.glTextureParameteri(textureID, GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_CLAMP_TO_EDGE);
			gl.glTextureParameteri(textureID, GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_CLAMP_TO_EDGE);
			gl.glTextureParameteri(textureID, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_LINEAR);

			// 3. Fill the texture Data
			if(dataDesc != null){
				enablePixelStore(dataDesc);
				
				int width = textureDesc.width;
				int height = textureDesc.height;
				int depth = textureDesc.depth;
				
				if(mipLevels > 1){
					List<Object> mipData = (List<Object>)dataDesc.data;
					int loop = Math.min(mipData.size(), mipLevels);
					for(int i = 0; i < loop; i++){
						subTexImage3DDAS(textureID, width, height, depth, i, dataDesc.format, dataDesc.type, mipData.get(i));
						
						width = Math.max(1, width >> 1);
						height = Math.max(1, height >> 1);
						depth = Math.max(1, depth >> 1);
					}
					gl.glTextureParameteri(textureID, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_LINEAR_MIPMAP_LINEAR);
				}else{
					subTexImage3DDAS(textureID, width, height, depth, 0, dataDesc.format, dataDesc.type, dataDesc.data);
					gl.glTextureParameteri(textureID, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_LINEAR);
				}
				
				disablePixelStore(dataDesc);
			}
		}else{
			boolean allocateStorage = false;
			
			// 1. Generate texture ID
			textureID = gl.glGenTexture();
			
			// 2. Allocate storage for Texture Object
//			final GLCapabilities cap = GL.getCapabilities();
			final boolean textureStorage = version.ES && version.major >= 3 || gl.isSupportExt("GL_ARB_texture_storage");
			gl.glBindTexture(target, textureID);
			if(!isCompressed && textureStorage){
				gl.glTexStorage3D(GLenum.GL_TEXTURE_2D_ARRAY, mipLevels, format, textureDesc.width, textureDesc.height, textureDesc.depth);
				allocateStorage = true;
			}

			gl.glTexParameteri(target, GLenum.GL_TEXTURE_WRAP_R, GLenum.GL_CLAMP_TO_EDGE);
			gl.glTexParameteri(target, GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_CLAMP_TO_EDGE);
			gl.glTexParameteri(target, GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_CLAMP_TO_EDGE);
			gl.glTexParameteri(target, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_LINEAR);
			gl.glTexParameteri(target, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_LINEAR);

			// 3. Fill the texture Data�� Ignore the multisample texture.
			if(dataDesc != null){
				int width = textureDesc.width;
				int height = textureDesc.height;
				int depth = textureDesc.depth;
				enablePixelStore(dataDesc);
				
				int dataFormat = measureFormat(format);
				int type = GLenum.GL_UNSIGNED_BYTE;
				Object pixelData = null;
				if(dataDesc != null){
					dataFormat = dataDesc.format;
					type = dataDesc.type;
					pixelData = dataDesc.data;
				}
				
				if(mipLevels > 1){
					int loop = mipLevels;
					List<Object> mipData = null;
					if(dataDesc != null){
						mipData = (List<Object>)dataDesc.data;
						loop = Math.min(mipData.size(), mipLevels);
					}
					
					for(int i = 0; i < loop; i++){
						Object mipmapData = null;
						if(mipData != null){
							mipmapData = mipData.get(i);
						}
						
						if(isCompressed){
							compressedTexImage3D(target, width, height, depth, i, dataFormat, dataDesc.type, dataDesc.imageSize, mipmapData);
						}else{
							if(allocateStorage){
								subTexImage3D(target, width, height, depth, i, dataFormat, type, mipmapData);
							}else{
								texImage3D(target, format, width, height, depth, i, dataFormat, type, mipmapData);
							}
						}
						
						width = Math.max(1, width >> 1);
						height = Math.max(1, height >> 1);
						depth = Math.max(1, depth >> 1);
					}

					gl.glTexParameteri(target, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_LINEAR_MIPMAP_LINEAR);
				}else{
					if(isCompressed){
						compressedTexImage3D(target, width, height, depth, 0, dataDesc.format, dataDesc.type, dataDesc.imageSize, dataDesc.data);
					}else{
						if(allocateStorage){
							subTexImage3D(target, width, height, depth, 0, dataFormat, type, pixelData);
						}else{
							texImage3D(target, format, width, height, depth, 0, dataFormat, type, pixelData);
						}
					}
				}
				
				disablePixelStore(dataDesc);
			}
			
			
			gl.glBindTexture(target, 0);  // unbind Texture
		}		
		
		GLCheck.checkError();
		Texture3D texture = out!=null ? out : new Texture3D();
		texture.format = format;
		texture.height = textureDesc.height;
		texture.width  = textureDesc.width;
		texture.depth  = textureDesc.depth;
		texture.target = target;
		texture.textureID = textureID;
		texture.mipLevels = mipLevels;
		return texture;
	}
	
	static boolean valid_texture2D = true;

	private static void check(Texture2DDesc desc){
		if(desc.width == 0 )
			throw new IllegalArgumentException("width can't be 0.");
		if(desc.height == 0 )
			throw new IllegalArgumentException("height can't be 0.");
	}
	
	public static Texture2D createTexture2D(Texture2DDesc textureDesc, TextureDataDesc dataDesc){
		return createTexture2D(textureDesc, dataDesc, null);
	}
	
	@SuppressWarnings("unchecked")
	public static Texture2D createTexture2D(Texture2DDesc textureDesc, TextureDataDesc dataDesc, Texture2D out){
		int textureID;
		int target;
		int format;
		boolean isCompressed = false;
		boolean isDSA; // = GL.getCapabilities().GL_ARB_direct_state_access;
		boolean multiSample; // = GL.getCapabilities().OpenGL32 && textureDesc.sampleDesc.count > 1;
		int mipLevels = Math.max(1, textureDesc.mipLevels);
		final boolean isSupportMSAA;

		check(textureDesc);

		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
		GLAPIVersion version = gl.getGLAPIVersion();
		isDSA = version.major >= 4 && version.minor >= 5;
		isSupportMSAA = version.major >= 3 && ((version.ES && version.minor >= 1) || version.minor >= 2);
		multiSample = isSupportMSAA && textureDesc.sampleCount > 1;
		
		// measure texture target.
		if(textureDesc.arraySize > 1){
			target = multiSample ? GLenum.GL_TEXTURE_2D_MULTISAMPLE_ARRAY  : GLenum.GL_TEXTURE_2D_ARRAY;
		}else{
			target = multiSample ? GLenum.GL_TEXTURE_2D_MULTISAMPLE : GLenum.GL_TEXTURE_2D;
		}
		
		// measure texture internal format
		if(dataDesc != null){
			isCompressed = Arrays.binarySearch(compressed_formats, dataDesc.format) >= 0;
			if(isCompressed){
				format = dataDesc.format;
			}else{
				format = textureDesc.format;
			}
		}else{
			format = textureDesc.format;
		}
		
		if(isDSA && !isCompressed){
			// 1. Generate texture ID
			textureID = gl.glCreateTextures(target);
			if(valid_texture2D) GLCheck.checkError();
			
			// 2. Allocate storage for Texture Object
			switch (target) {
			case GLenum.GL_TEXTURE_2D_MULTISAMPLE:
				gl.glTextureStorage2DMultisample(textureID, textureDesc.sampleCount, format, textureDesc.width, textureDesc.height, false);
				mipLevels = 1;  // multisample_texture doesn't support mipmaps.
				if(valid_texture2D) GLCheck.checkError();
				break;
			case GLenum.GL_TEXTURE_2D_MULTISAMPLE_ARRAY:
				gl.glTextureStorage3DMultisample(textureID, textureDesc.sampleCount, format, textureDesc.width, textureDesc.height, textureDesc.arraySize, false);
				mipLevels = 1; // multisample_texture doesn't support mipmaps.
				if(valid_texture2D) GLCheck.checkError();
				break;
			case GLenum.GL_TEXTURE_2D_ARRAY:
				gl.glTextureStorage3D(textureID, mipLevels, format, textureDesc.width, textureDesc.height, textureDesc.arraySize);
				if(valid_texture2D) GLCheck.checkError();
				break;
			case GLenum.GL_TEXTURE_2D:
				gl.glTextureStorage2D(textureID, mipLevels, format, textureDesc.width, textureDesc.height);
				if(valid_texture2D) GLCheck.checkError();
				break;
			default:
				break;
			}

			if(!multiSample) {
				// setup the defualt properties.
				gl.glTextureParameteri(textureID, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_LINEAR);
				gl.glTextureParameteri(textureID, GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_CLAMP_TO_EDGE);
				gl.glTextureParameteri(textureID, GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_CLAMP_TO_EDGE);
			}

			// 3. Fill the texture Data
			if(dataDesc != null && dataDesc.data != null && target != GLenum.GL_TEXTURE_2D_MULTISAMPLE_ARRAY && target != GLenum.GL_TEXTURE_2D_MULTISAMPLE){
				enablePixelStore(dataDesc);
				
				int width = textureDesc.width;
				int height = textureDesc.height;
				int depth = textureDesc.arraySize;
				
				if(mipLevels > 1){
					gl.glTextureParameteri(textureID, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_LINEAR_MIPMAP_LINEAR);
					List<Object> mipData = null;
					if(dataDesc.data instanceof  List<?>){
						mipData = (List<Object>)dataDesc.data;
					}else if(dataDesc.data.getClass().isArray()){
						mipData = Arrays.asList((Object[])dataDesc.data);
					}else{
						mipData = Arrays.asList(dataDesc.data);
					}

					int loop = Math.min(mipData.size(), mipLevels);
					for(int i = 0; i < loop; i++){
						if(target == GLenum.GL_TEXTURE_2D_ARRAY){
							subTexImage3DDAS(textureID, width, height, depth, i, dataDesc.format, dataDesc.type, mipData.get(i));
						}else if(target == GLenum.GL_TEXTURE_2D){
							subTexImage2DDAS(textureID, width, height, i, dataDesc.format, dataDesc.type, mipData.get(i));
						}
						
						width = Math.max(1, width >> 1);
						height = Math.max(1, height >> 1);
						depth = Math.max(1, depth >> 1);
					}

					if(mipData.size() < mipLevels){
						gl.glGenerateTextureMipmap(textureID);
					}
				}else{
					if(!multiSample) {
						gl.glTextureParameteri(textureID, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_LINEAR);
					}
					if(target == GLenum.GL_TEXTURE_2D_ARRAY){
						subTexImage3DDAS(textureID, width, height, depth, 0, dataDesc.format, dataDesc.type, dataDesc.data);
					}else if(target == GLenum.GL_TEXTURE_2D){
						subTexImage2DDAS(textureID, width, height, 0, dataDesc.format, dataDesc.type,dataDesc.data);
					}
				}
				
				disablePixelStore(dataDesc);
			}else{
				if(!multiSample) {
					gl.glTextureParameteri(textureID, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_LINEAR);
				}
			}
		}else{
			boolean allocateStorage = false;
			
			// 1. Generate texture ID
			textureID = gl.glGenTexture();
			
			// 2. Allocate storage for Texture Object
//			final GLCapabilities cap = GL.getCapabilities();
			final boolean textureStorage = version.ES && version.major >= 3 || gl.isSupportExt("GL_ARB_texture_storage");
			final boolean textureStorageMSAA = (version.ES && version.major >= 2 && version.minor >= 1) || gl.isSupportExt("GL_ARB_texture_storage_multisample");
			if(!isCompressed){
				gl.glBindTexture(target, textureID);
				switch (target) {
				case GLenum.GL_TEXTURE_2D_MULTISAMPLE:
					if(textureStorageMSAA){
						gl.glTexStorage2DMultisample(target, textureDesc.sampleCount, format, textureDesc.width, textureDesc.height, false);
					}else{
						gl.glTexImage2DMultisample(target, textureDesc.sampleCount, format, textureDesc.width, textureDesc.height, false);
					}
					mipLevels = 1;  // multisample_texture doesn't support mipmaps.
					break;
				case GLenum.GL_TEXTURE_2D_MULTISAMPLE_ARRAY:
					if(textureStorageMSAA){
						gl.glTexStorage3DMultisample(target, textureDesc.sampleCount, format, textureDesc.width, textureDesc.height, textureDesc.arraySize, false);
					}else{
						gl.glTexImage3DMultisample(target, textureDesc.sampleCount, format, textureDesc.width, textureDesc.height, textureDesc.arraySize, false);
					}
					mipLevels = 1;  // multisample_texture doesn't support mipmaps.
					break;
				case GLenum.GL_TEXTURE_2D_ARRAY:
				case GLenum.GL_TEXTURE_2D:
					if(textureStorage){
						allocateStorage = true;
						if(target == GLenum.GL_TEXTURE_2D){
							gl.glTexStorage2D(GLenum.GL_TEXTURE_2D, mipLevels, format, textureDesc.width, textureDesc.height);
						}else{
							gl.glTexStorage3D(GLenum.GL_TEXTURE_2D_ARRAY, mipLevels, format, textureDesc.width, textureDesc.height, textureDesc.arraySize);
						}
					}
					
					break;
				
				default:
					break;
				}
			}else{ 
				// remove multisample symbol
				if(target == GLenum.GL_TEXTURE_2D_MULTISAMPLE_ARRAY){
					target = GLenum.GL_TEXTURE_2D_ARRAY;
					multiSample = false;
				}
				
				if(target == GLenum.GL_TEXTURE_2D_MULTISAMPLE){
					target = GLenum.GL_TEXTURE_2D;
					multiSample = false;
				}

				gl.glBindTexture(target, textureID);
			}

			if(!multiSample) {
				// setup the defualt parameters.
				gl.glTexParameteri(target, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_LINEAR);
				gl.glTexParameteri(target, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_LINEAR);
				gl.glTexParameteri(target, GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_CLAMP_TO_EDGE);
				gl.glTexParameteri(target, GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_CLAMP_TO_EDGE);
			}
			
			// 3. Fill the texture Data�� Ignore the multisample texture.
			if(target != GLenum.GL_TEXTURE_2D_MULTISAMPLE_ARRAY && target != GLenum.GL_TEXTURE_2D_MULTISAMPLE){
				int width = textureDesc.width;
				int height = textureDesc.height;
				int depth = textureDesc.arraySize;
				enablePixelStore(dataDesc);
				
				int dataFormat = measureFormat(format);
				int type = GLenum.GL_UNSIGNED_BYTE;
				Object pixelData = null;
				if(dataDesc != null){
					dataFormat = dataDesc.format;
					type = dataDesc.type;
					pixelData = dataDesc.data;
				}

				if(mipLevels > 1){
					int loop;
					List<Object> mipData = null;
					if(dataDesc.data instanceof  List<?>){
						mipData = (List<Object>)dataDesc.data;
					}else if(dataDesc.data.getClass().isArray()){
						mipData = Arrays.asList((Object[])dataDesc.data);
					}else{
						mipData = Arrays.asList(dataDesc.data);
					}

					loop = Math.min(mipData.size(), mipLevels);
					
					for(int i = 0; i < loop; i++){
						Object mipmapData = null;
						if(mipData != null){
							mipmapData = mipData.get(i);
						}
						
						if(isCompressed){
							if(target == GLenum.GL_TEXTURE_2D_ARRAY){
								compressedTexImage3D(target, width, height, depth, i, dataFormat, dataDesc.type, dataDesc.imageSize, mipmapData);
							}else{
								compressedTexImage2D(target, width, height, i, dataFormat, dataDesc.type, dataDesc.imageSize, mipmapData);
							}
						}else if(target == GLenum.GL_TEXTURE_2D_ARRAY){
							if(allocateStorage){
								subTexImage3D(target, width, height, depth, i, dataFormat, type, mipmapData);
							}else{
								texImage3D(target, format, width, height, depth, i, dataFormat, type, mipmapData);
							}
						}else if(target == GLenum.GL_TEXTURE_2D){
							if(allocateStorage){
								subTexImage2D(target, width, height, i, dataFormat, type, mipmapData);
							}else{
								texImage2D(target, format, width, height, i, dataFormat, type, mipmapData);
							}
						}
						
						width = Math.max(1, width >> 1);
						height = Math.max(1, height >> 1);
						depth = Math.max(1, depth >> 1);
					}

					gl.glTexParameteri(target, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_LINEAR_MIPMAP_LINEAR);
					if(mipData.size() < mipLevels) {
						gl.glGenerateMipmap(target);
					}
				}else{
					if(isCompressed){
						compressedTexImage3D(target, width, height, depth, 0, dataDesc.format, dataDesc.type, dataDesc.imageSize, dataDesc.data);
					}else if(target == GLenum.GL_TEXTURE_2D_ARRAY){
						if(allocateStorage){
							subTexImage3D(target, width, height, depth, 0, dataFormat, type, pixelData);
						}else{
							texImage3D(target, format, width, height, depth, 0, dataFormat, type, pixelData);
						}
					}else if(target == GLenum.GL_TEXTURE_2D){
						if(allocateStorage){
							subTexImage2D(target, width, height, 0, dataFormat, type, pixelData);
						}else{
							texImage2D(target, format, width, height, 0, dataFormat, type, pixelData);
						}
					}
				}
				
				disablePixelStore(dataDesc);
			}
			
			
			gl.glBindTexture(target, 0);  // unbind Texture
		}		
		
		GLCheck.checkError();
		Texture2D texture = (out != null ? out : new Texture2D());
		texture.arraySize = textureDesc.arraySize;
		texture.format = format;
		texture.height = textureDesc.height;
		texture.width  = textureDesc.width;
		texture.target = target;
		texture.textureID = textureID;
		texture.mipLevels = mipLevels;
		texture.samples = textureDesc.sampleCount;
		return texture;
	}

	@CachaRes
	private static void subTexImage3DDAS(int textureID, int width, int height, int depth, int level, int format, int type, Object data){
		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
		if(data == null){
			gl.glTextureSubImage3D(textureID, level, 0, 0, 0, width, height, depth, format, type, (ByteBuffer)null);
		}else if(data instanceof  Buffer){
			gl.glTextureSubImage3D(textureID, level, 0, 0, 0, width, height, depth, format, type, (Buffer)data);
		}else if(data instanceof Number){
			gl.glTextureSubImage3D(textureID, level, 0, 0, 0, width, height, depth, format, type, ((Number)data).longValue());
		}else{
			gl.glTextureSubImage3D(textureID, level, 0, 0, 0, width, height, depth, format, type, CacheBuffer.wrapPrimitiveArray(data));
		}

		if(valid_texture2D) {
//			System.out.println("Target = " + getTextureTargetName(textureID));
//			System.out.println("format = " + getFormatName(format));
//			System.out.println("Type = " + getTypeName(type));
//			
//			System.out.println("level = " + level);
//			System.out.println("width = " + width);
//			System.out.println("height = " + height);
//			System.out.println("depth = " + depth);
			GLCheck.checkError();
		}
	}

	@CachaRes
	private static void subTexImage3D(int target, int width, int height, int depth, int level, int format, int type, Object data){
		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
		if(data == null){
			gl.glTexSubImage3D(target, level, 0, 0, 0, width, height, depth, format, type, (ByteBuffer)null);
		}else if(data instanceof  Buffer){
			gl.glTexSubImage3D(target, level, 0, 0, 0, width, height, depth, format, type, (Buffer)null);
		}else if(data instanceof Number){
			gl.glTexSubImage3D(target, level, 0, 0, 0, width, height, depth, format, type, ((Number)data).longValue());
		}else{
			gl.glTexSubImage3D(target, level, 0, 0, 0, width, height, depth, format, type, CacheBuffer.wrapPrimitiveArray(data));
		}

		if(valid_texture2D) {
//			System.out.println("Target = " + getTextureTargetName(target));
//			System.out.println("format = " + getFormatName(format));
//			System.out.println("Type = " + getTypeName(type));
//			
//			System.out.println("level = " + level);
//			System.out.println("width = " + width);
//			System.out.println("height = " + height);
//			System.out.println("depth = " + depth);
			GLCheck.checkError();
		}
	}

	@CachaRes
	private static void subTexImage2DDAS(int texture, int width, int height, int level, int format, int type,  Object data){
		if(valid_texture2D) GLCheck.checkError();
		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();

//		System.out.println("width = " + width);
//		System.out.println("height = " + height);
//		System.out.println("level = " + level);
//		System.out.println("format = " + ((format == GL11.GL_RED) ? "GL_RED": "KKKK"));
//		System.out.println(data);
		if(!gl.glIsTexture(texture))
			throw new IllegalArgumentException();

		if(data == null){
			gl.glTextureSubImage2D(texture, level, 0, 0, width, height, format, type, (ByteBuffer)null);
		}else if(data instanceof  Buffer){
			gl.glTextureSubImage2D(texture, level, 0, 0, width, height, format, type, (Buffer)data);
		}else if(data instanceof Number){
			gl.glTextureSubImage2D(texture, level, 0, 0, width, height, format, type, ((Number)data).longValue());
		}else{
			gl.glTextureSubImage2D(texture, level, 0, 0, width, height, format, type, CacheBuffer.wrapPrimitiveArray(data));
		}
		
		if(valid_texture2D) GLCheck.checkError();
	}

	@CachaRes
	private static void subTexImage2D(int target, int width, int height, int level, int format, int type,  Object data){
		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
		if(data == null){
			gl.glTexSubImage2D(target, level, 0, 0, width, height, format, type, (Buffer)null);
		}else if(data instanceof  Buffer){
			gl.glTexSubImage2D(target, level, 0, 0, width, height, format, type, (Buffer)data);
		}else if(data instanceof Number){
			gl.glTexSubImage2D(target, level, 0, 0, width, height, format, type, ((Number)data).longValue());
		}else{
			gl.glTexSubImage2D(target, level, 0, 0, width, height, format, type, CacheBuffer.wrapPrimitiveArray(data));
		}

		if(valid_texture2D) GLCheck.checkError();
	}

	@CachaRes
	private static void texImage3D(int target, int internalformat, int width, int height, int depth, int level, int format, int type, Object data){
		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
		if(data == null){
			gl.glTexImage3D(target, level, internalformat, width, height, depth);
		}else if(data instanceof  Buffer){
			gl.glTexImage3D(target, level, internalformat, width, height, depth, 0, format, type, (Buffer)data);
		}else if(data instanceof Number){
			gl.glTexImage3D(target, level, internalformat, width, height, depth, 0, format, type, ((Number)data).longValue());
		}else{
			gl.glTexImage3D(target, level, internalformat, width, height, depth, 0, format, type, CacheBuffer.wrapPrimitiveArray(data));
		}

		if(valid_texture2D) GLCheck.checkError();
	}

	@CachaRes
	private static void texImage2D(int target, int internalformat, int width, int height, int level, int format, int type, Object data){
		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
		if(data == null){
			gl.glTexImage2D(target, level, internalformat, width, height);
		}else if(data instanceof  Buffer){
			gl.glTexImage2D(target, level, internalformat, width, height, 0, format, type, (Buffer)data);
		}else if(data instanceof Number){
			gl.glTexImage2D(target, level, internalformat, width, height, 0, format, type, ((Number)data).longValue());
		}else{
			gl.glTexImage2D(target, level, internalformat, width, height, 0, format, type, CacheBuffer.wrapPrimitiveArray(data));
		}

		if(valid_texture2D) GLCheck.checkError();
	}

	@CachaRes
	private static void compressedTexImage3D(int target, int width, int height, int depth, int level, int internalformat, int type, int imageSize,Object data){
		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
		if(data == null){
			gl.glCompressedTexImage3D(target, level, internalformat, width, height, depth, 0, imageSize, 0L);
		}else if(data instanceof  ByteBuffer){
			gl.glCompressedTexImage3D(target, level, internalformat, width, height, depth, 0, (ByteBuffer) data);
		}else if(data instanceof Number){
			gl.glCompressedTexImage3D(target, level, internalformat, width, height, depth, 0, imageSize, ((Number)data).longValue());
		}else{
			gl.glCompressedTexImage3D(target, level, internalformat, width, height, depth, 0, CacheBuffer.wrapPrimitiveArray(data));
		}
		
		if(valid_texture2D) GLCheck.checkError();
	}

	@CachaRes
	private static void compressedTexImage2D(int target, int width, int height, int level, int internalformat, int type, int imageSize,Object data){
		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
		if(data == null){
			gl.glCompressedTexImage2D(target, level, internalformat, width, height, 0, imageSize, 0L);
		}else if(data instanceof  ByteBuffer){
			gl.glCompressedTexImage2D(target, level, internalformat, width, height, 0, (ByteBuffer)data);
		}else if(data instanceof Number){
			gl.glCompressedTexImage2D(target, level, internalformat, width, height, 0, imageSize, ((Number)data).longValue());
		}else{
			gl.glCompressedTexImage2D(target, level, internalformat, width, height, 0, CacheBuffer.wrapPrimitiveArray(data));
		}

		if(valid_texture2D) GLCheck.checkError();
	}
	
	private static void enablePixelStore(TextureDataDesc desc){
		if(desc == null)
			return;

		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();

		if(desc.unpackSwapBytes){
			gl.glPixelStorei(GLenum.GL_UNPACK_SWAP_BYTES, 1);
		}
		
		if(desc.unpackLSBFirst){
			gl.glPixelStorei(GLenum.GL_UNPACK_LSB_FIRST, 1);
		}
		
		if(desc.unpackRowLength > 0){
			gl.glPixelStorei(GLenum.GL_UNPACK_ROW_LENGTH, desc.unpackRowLength);
		}
		
		if(desc.unpackSkipRows > 0){
			gl.glPixelStorei(GLenum.GL_UNPACK_SKIP_ROWS, desc.unpackSkipRows);
		}
		
		if(desc.unpackSkipPixels > 0){
			gl.glPixelStorei(GLenum.GL_UNPACK_SKIP_PIXELS, desc.unpackSkipPixels);
		}
		
		if(desc.unpackAlignment > 0 && desc.unpackAlignment != 4){
			gl.glPixelStorei(GLenum.GL_UNPACK_ALIGNMENT, desc.unpackAlignment);
		}
		
		if(desc.unpackImageHeight > 0){
			gl.glPixelStorei(GLenum.GL_UNPACK_IMAGE_HEIGHT, desc.unpackImageHeight);
		}
		
		if(desc.unpackSkipImages > 0){
			gl.glPixelStorei(GLenum.GL_UNPACK_SKIP_IMAGES, desc.unpackSkipImages);
		}
		
		if(gl.isSupportExt("GL_ARB_compressed_texture_pixel_storage")){
			if(desc.unpackCompressedBlockWidth > 0){
				gl.glPixelStorei(GLenum.GL_UNPACK_COMPRESSED_BLOCK_WIDTH, desc.unpackCompressedBlockWidth);
			}
			
			if(desc.unpackCompressedBlockHeight > 0){
				gl.glPixelStorei(GLenum.GL_UNPACK_COMPRESSED_BLOCK_HEIGHT, desc.unpackCompressedBlockHeight);
			}
			
			if(desc.unpackCompressedBlockDepth > 0){
				gl.glPixelStorei(GLenum.GL_UNPACK_COMPRESSED_BLOCK_DEPTH, desc.unpackCompressedBlockDepth);
			}
			
			if(desc.unpackCompressedBlockSize > 0){
				gl.glPixelStorei(GLenum.GL_UNPACK_COMPRESSED_BLOCK_SIZE, desc.unpackCompressedBlockSize);
			}
		}
		
		if(valid_texture2D){
			GLCheck.checkError();
		}
	}
	
	private static void disablePixelStore(TextureDataDesc desc){
		if(desc == null)
			return;

		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
		if(desc.unpackSwapBytes){
			gl.glPixelStorei(GLenum.GL_UNPACK_SWAP_BYTES, 0);
		}
		
		if(desc.unpackLSBFirst){
			gl.glPixelStorei(GLenum.GL_UNPACK_LSB_FIRST, 0);
		}
		
		if(desc.unpackRowLength > 0){
			gl.glPixelStorei(GLenum.GL_UNPACK_ROW_LENGTH, 0);
		}
		
		if(desc.unpackSkipRows > 0){
			gl.glPixelStorei(GLenum.GL_UNPACK_SKIP_ROWS, 0);
		}
		
		if(desc.unpackSkipPixels > 0){
			gl.glPixelStorei(GLenum.GL_UNPACK_SKIP_PIXELS, 0);
		}
		
		if(desc.unpackAlignment > 0 && desc.unpackAlignment != 4){
			gl.glPixelStorei(GLenum.GL_UNPACK_ALIGNMENT, 4);
		}
		
		if(desc.unpackImageHeight > 0){
			gl.glPixelStorei(GLenum.GL_UNPACK_IMAGE_HEIGHT, 0);
		}
		
		if(desc.unpackSkipImages > 0){
			gl.glPixelStorei(GLenum.GL_UNPACK_SKIP_IMAGES, 0);
		}
		
//		if(GL.getCapabilities().){
		if(gl.isSupportExt("GL_ARB_compressed_texture_pixel_storage")){
			if(desc.unpackCompressedBlockWidth > 0){
				gl.glPixelStorei(GLenum.GL_UNPACK_COMPRESSED_BLOCK_WIDTH, 0);
			}
			
			if(desc.unpackCompressedBlockHeight > 0){
				gl.glPixelStorei(GLenum.GL_UNPACK_COMPRESSED_BLOCK_HEIGHT, 0);
			}
			
			if(desc.unpackCompressedBlockDepth > 0){
				gl.glPixelStorei(GLenum.GL_UNPACK_COMPRESSED_BLOCK_DEPTH, 0);
			}
			
			if(desc.unpackCompressedBlockSize > 0){
				gl.glPixelStorei(GLenum.GL_UNPACK_COMPRESSED_BLOCK_SIZE, 0);
			}
		}
	}
	
	public static String getTextureTargetName(int target){
		switch (target) {
		case GLenum.GL_TEXTURE_1D:  return "GL_TEXTURE_1D";
		case GLenum.GL_TEXTURE_2D:  return "GL_TEXTURE_2D";
		case GLenum.GL_TEXTURE_3D:  return "GL_TEXTURE_3D";
		case GLenum.GL_TEXTURE_CUBE_MAP:  return "GL_TEXTURE_CUBE_MAP";
		case GLenum.GL_TEXTURE_1D_ARRAY:  return "GL_TEXTURE_1D_ARRAY";
		case GLenum.GL_TEXTURE_2D_ARRAY:  return "GL_TEXTURE_2D_ARRAY";
		case GLenum.GL_TEXTURE_RECTANGLE:  return "GL_TEXTURE_RECTANGLE";
		case GLenum.GL_TEXTURE_CUBE_MAP_ARRAY:  return "GL_TEXTURE_CUBE_MAP_ARRAY";
		case GLenum.GL_TEXTURE_BUFFER:  return "GL_TEXTURE_BUFFER";
		case GLenum.GL_TEXTURE_2D_MULTISAMPLE :  return "GL_TEXTURE_2D_MULTISAMPLE ";
		case GLenum.GL_TEXTURE_2D_MULTISAMPLE_ARRAY:  return "GL_TEXTURE_2D_MULTISAMPLE_ARRAY";
			

		default:
			return "Unkown TextureTarget(0x" + Integer.toHexString(target) + ")";
		}
	}
	
	public static String getTextureFilterName(int filter){
		switch (filter) {
		case GLenum.GL_NEAREST: return "GL_NEAREST";
		case GLenum.GL_LINEAR: return "GL_LINEAR";
		case GLenum.GL_NEAREST_MIPMAP_NEAREST: return "GL_NEAREST_MIPMAP_NEAREST";
		case GLenum.GL_NEAREST_MIPMAP_LINEAR: return "GL_NEAREST_MIPMAP_LINEAR";
		case GLenum.GL_LINEAR_MIPMAP_NEAREST: return "GL_LINEAR_MIPMAP_NEAREST";
		case GLenum.GL_LINEAR_MIPMAP_LINEAR: return "GL_LINEAR_MIPMAP_LINEAR";

		default:
			return "Unkown TextureFilter(0x" + Integer.toHexString(filter) + ")";
		}
	}
	
	public static String getTextureSwizzleName(int swizzle){
		switch (swizzle) {
		case GLenum.GL_RED: return "GL_RED";
		case GLenum.GL_GREEN: return "GL_GREEN";
		case GLenum.GL_BLUE: return "GL_BLUE";
		case GLenum.GL_ALPHA: return "GL_ALPHA";
		case GLenum.GL_ONE: return "GL_ONE";
		case GLenum.GL_ZERO: return "GL_ZERO";

		default:
			return "Unkown TextureSwizzle(0x" + Integer.toHexString(swizzle) + ")";
		}
	}
	
	public static String getTextureWrapName(int wrap){
		switch (wrap) {
		case GLenum.GL_CLAMP_TO_EDGE: return "GL_CLAMP_TO_EDGE";
		case GLenum.GL_REPEAT: return "GL_REPEAT";
		case GLenum.GL_CLAMP_TO_BORDER: return "GL_CLAMP_TO_BORDER";
		case GLenum.GL_MIRRORED_REPEAT: return "GL_MIRRORED_REPEAT";

		default:
			return "Unkown TextureWrap(0x" + Integer.toHexString(wrap) + ")";
		}
	}
	
	public static String getTypeName(int type){
		switch (type) {
		case GLenum.GL_UNSIGNED_BYTE: return "GL_UNSIGNED_BYTE";
		case GLenum.GL_UNSIGNED_SHORT: return "GL_UNSIGNED_SHORT";
		case GLenum.GL_UNSIGNED_INT: return "GL_UNSIGNED_INT";
		case GLenum.GL_BYTE: return "GL_BYTE";
		case GLenum.GL_SHORT: return "GL_SHORT";
		case GLenum.GL_INT: return "GL_INT";
		case GLenum.GL_FLOAT: return "GL_FLOAT";

		default:
			return "Unkown Type(0x" + Integer.toHexString(type) + ")";
		}
	}
	
	public static String getDepthTextureModeName(int mode){
		switch (mode) {
		case GLenum.GL_LUMINANCE: return "GL_LUMINANCE";
		case GLenum.GL_INTENSITY: return "GL_INTENSITY";
		case GLenum.GL_ALPHA: return "GL_ALPHA";
		case GLenum.GL_RED: return "GL_RED";
		case GLenum.GL_NONE: return "GL_NONE";
		default:
			return "Unkown DepthTextureMode(0x" + Integer.toHexString(mode) + ")";
		}
	}
	
	public static String getTypeSignName(int type){
		switch (type) {
		case GLenum.GL_NONE: return "GL_NONE";
		case GLenum.GL_SIGNED_NORMALIZED: return "GL_SIGNED_NORMALIZED";
		case GLenum.GL_UNSIGNED_NORMALIZED: return "GL_UNSIGNED_NORMALIZED";
		case GLenum.GL_FLOAT: return "GL_FLOAT";
		case GLenum.GL_INT: return "GL_INT";
		case GLenum.GL_UNSIGNED_INT : return "GL_UNSIGNED_INT ";
 			
		default:
			return "Unkown Type(0x" + Integer.toHexString(type) + ")";
		}
	}
	
	private static int measureInterformat(int req_comp){
		switch (req_comp) {
		case 1:  	return GLenum.GL_R8;
		case 2:		return GLenum.GL_RG8;
		case 3:     return GLenum.GL_RGB8;
		case 4:     return GLenum.GL_RGBA8;
		default:
			throw new IllegalArgumentException("req_comp = " + req_comp);
		}
	}
	
	public static float measureSizePerPixel(int internalFormat){
		switch (internalFormat) {
		case GLenum.GL_R8:  				return 1;
		case GLenum.GL_R8_SNORM:		    return 1;
		case GLenum.GL_R16: 				return 2;
		case GLenum.GL_R16_SNORM : 		return 2;
		case GLenum.GL_RG8:				return 2;
		case GLenum.GL_RG8_SNORM:			return 2;
		case GLenum.GL_RG16:				return 4;
		case GLenum.GL_RG16_SNORM:		return 4;
		case GLenum.GL_R3_G3_B2:			return 1;
		case GLenum.GL_RGB4:				return 1.5f;
		case GLenum.GL_RGB5:				return 15f/8f;
		case GLenum.GL_RGB8:				return 3;
		case GLenum.GL_RGB8_SNORM:		return 3;
		case GLenum.GL_RGB10:				return 30f/8f;
		case GLenum.GL_RGB12:				return 36f/8f;
		case GLenum.GL_RGB16_SNORM:		return 6;
		case GLenum.GL_RGBA2:				return 1;
		case GLenum.GL_RGBA4:				return 2;
		case GLenum.GL_RGB5_A1:			return 2;
		case GLenum.GL_RGBA8:				return 4;
		case GLenum.GL_RGBA8_SNORM:		return 4;
		case GLenum.GL_RGB10_A2:			return 4;
		case GLenum.GL_RGB10_A2UI:		return 4;
		case GLenum.GL_RGBA12:			return 6;
		case GLenum.GL_RGBA16:			return 8;
		case GLenum.GL_SRGB8:				return 3;
		case GLenum.GL_SRGB8_ALPHA8:		return 4;
		case GLenum.GL_R16F:				return 2;
		case GLenum.GL_RG16F:				return 4;
		case GLenum.GL_RGB16F:			return 6;
		case GLenum.GL_RGBA16F:			return 8;
		case GLenum.GL_R32F:				return 4;
		case GLenum.GL_RG32F:				return 8;
		case GLenum.GL_RGB32F:			return 12;
		case GLenum.GL_RGBA32F:			return 16;
		case GLenum.GL_R11F_G11F_B10F:	return 4;
		case GLenum.GL_RGB9_E5:			return 4; // TODO ?
		case GLenum.GL_R8I:				return 1;
		case GLenum.GL_R8UI:				return 1;
		case GLenum.GL_R16I:				return 2;
		case GLenum.GL_R16UI:				return 2;
		case GLenum.GL_R32I:				return 4;
		case GLenum.GL_R32UI:				return 4;
		case GLenum.GL_RG8I:				return 2;
		case GLenum.GL_RG8UI:				return 2;
		case GLenum.GL_RG16I:				return 4;
		case GLenum.GL_RG16UI:			return 4;
		case GLenum.GL_RG32I:				return 8;
		case GLenum.GL_RG32UI:			return 8;
		case GLenum.GL_RGB8I:				return 3;
		case GLenum.GL_RGB8UI:			return 3;
		case GLenum.GL_RGB16I:			return 6;
		case GLenum.GL_RGB16UI:			return 6;
		case GLenum.GL_RGB32I:			return 12;
		case GLenum.GL_RGB32UI:			return 12;
		
		case GLenum.GL_RGBA8I:			return 4;
		case GLenum.GL_RGBA8UI:			return 4;
		case GLenum.GL_RGBA16I:			return 8;
		case GLenum.GL_RGBA16UI:			return 8;
		case GLenum.GL_RGBA32I:			return 16;
		case GLenum.GL_RGBA32UI:			return 16;
		
		default:
			throw new IllegalArgumentException("Unkown internalFormat: " + internalFormat);
		}
	}
	
	public static String getFormatName(int internalFormat){
		switch (internalFormat) {
		case GLenum.GL_R8:  				return "GL_R8";
		case GLenum.GL_R8_SNORM:		    return "GL_R8_SNORM";
		case GLenum.GL_R16: 				return "GL_R16";
		case GLenum.GL_R16_SNORM : 		return "GL_R16_SNORM";
		case GLenum.GL_RG8:				return "GL_RG8";
		case GLenum.GL_RG8_SNORM:			return "GL_RG8_SNORM";
		case GLenum.GL_RG16:				return "GL_RG16";
		case GLenum.GL_RG16_SNORM:		return "GL_RG16_SNORM";
		case GLenum.GL_R3_G3_B2:			return "GL_R3_G3_B2";
		case GLenum.GL_RGB4:				return "GL_RGB4";
		case GLenum.GL_RGB5:				return "GL_RGB5";
		case GLenum.GL_RGB8:				return "GL_RGB8";
		case GLenum.GL_RGB8_SNORM:		return "GL_RGB8_SNORM";
		case GLenum.GL_RGB10:				return "GL_RGB10";
		case GLenum.GL_RGB12:				return "GL_RGB12";
		case GLenum.GL_RGB16_SNORM:		return "GL_RGB16_SNORM";
		case GLenum.GL_RGBA2:				return "GL_RGBA2";  // TODO
		case GLenum.GL_RGBA4:				return "GL_RGBA4";  // TODO
		case GLenum.GL_RGB5_A1:			return "GL_RGB5_A1";  // TODO
		case GLenum.GL_RGBA8:				return "GL_RGBA8";
		case GLenum.GL_RGBA8_SNORM:		return "GL_RGBA8_SNORM";
		case GLenum.GL_RGB10_A2:			return "GL_RGB10_A2";
		case GLenum.GL_RGB10_A2UI:		return "GL_RGB10_A2UI";
		case GLenum.GL_RGBA12:			return "GL_RGBA12";
		case GLenum.GL_RGBA16:			return "GL_RGBA16";
		case GLenum.GL_SRGB8:				return "GL_SRGB8";
		case GLenum.GL_SRGB8_ALPHA8:		return "GL_SRGB8_ALPHA8";
		case GLenum.GL_R16F:				return "GL_R16F";
		case GLenum.GL_RG16F:				return "GL_RG16F";
		case GLenum.GL_RGB16F:			return "GL_RGB16F";
		case GLenum.GL_RGBA16F:			return "GL_RGBA16F";
		case GLenum.GL_R32F:				return "GL_R32F";
		case GLenum.GL_RG32F:				return "GL_RG32F";
		case GLenum.GL_RGB32F:			return "GL_RGB32F";
		case GLenum.GL_RGBA32F:			return "GL_RGBA32F";
		case GLenum.GL_R11F_G11F_B10F:	return "GL_R11F_G11F_B10F";
		case GLenum.GL_RGB9_E5:			return "GL_RGB9_E5";
		case GLenum.GL_R8I:				return "GL_R8I";
		case GLenum.GL_R8UI:				return "GL_R8UI";
		case GLenum.GL_R16I:				return "GL_R16I";
		case GLenum.GL_R16UI:				return "GL_R16UI";
		case GLenum.GL_R32I:				return "GL_R32I";
		case GLenum.GL_R32UI:				return "GL_R32UI";
		case GLenum.GL_RG8I:				return "GL_RG8I";
		case GLenum.GL_RG8UI:				return "GL_RG8UI";
		case GLenum.GL_RG16I:				return "GL_RG16I";
		case GLenum.GL_RG16UI:			return "GL_RG16UI";
		case GLenum.GL_RG32I:				return "GL_RG32I";
		case GLenum.GL_RG32UI:			return "GL_RG32UI";
		case GLenum.GL_RGB8I:				return "GL_RGB8I";
		case GLenum.GL_RGB8UI:			return "GL_RGB8UI";
		case GLenum.GL_RGB16I:			return "GL_RGB16I";
		case GLenum.GL_RGB16UI:			return "GL_RGB16UI";
		case GLenum.GL_RGB32I:			return "GL_RGB32I";
		case GLenum.GL_RGB32UI:			return "GL_RGB32UI";
		
		case GLenum.GL_RGBA8I:			return "GL_RGBA8I";
		case GLenum.GL_RGBA8UI:			return "GL_RGBA8UI";
		case GLenum.GL_RGBA16I:			return "GL_RGBA16I";
		case GLenum.GL_RGBA16UI:			return "GL_RGBA16UI";
		case GLenum.GL_RGBA32I:			return "GL_RGBA32I";
		case GLenum.GL_RGBA32UI:			return "GL_RGBA32UI";
		case GLenum.GL_DEPTH_COMPONENT16: return "GL_DEPTH_COMPONENT16";
		case GLenum.GL_DEPTH_COMPONENT24:	return "GL_DEPTH_COMPONENT24";
		case GLenum.GL_DEPTH_COMPONENT32: return "GL_DEPTH_COMPONENT32";
		case GLenum.GL_DEPTH_COMPONENT32F:
										return "GL_DEPTH_COMPONENT32F";
//		case GL12.GL_BGRA:				return "GL_BGRA8";
//		case GL12.GL_BGR:				return "";
		case GLenum.GL_COMPRESSED_RGBA_S3TC_DXT1_EXT: return "GL_COMPRESSED_RGBA_S3TC_DXT1_EXT";
		case GLenum.GL_COMPRESSED_RGB_S3TC_DXT1_EXT:  return "GL_COMPRESSED_RGB_S3TC_DXT1_EXT";
		case GLenum.GL_COMPRESSED_RGBA_S3TC_DXT3_EXT: return "GL_COMPRESSED_RGBA_S3TC_DXT3_EXT";
		case GLenum.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT: return "GL_COMPRESSED_RGBA_S3TC_DXT5_EXT";
		case GLenum.GL_RGBA             : return "GLRGBA";
		default:
			return "Unkown Format(0x" + Integer.toHexString(internalFormat) + ")";
		}
	}
	
	public static int measureFormat(int internalFormat){
		switch (internalFormat) {
		case GLenum.GL_R8:  				return RED;
		case GLenum.GL_R8_SNORM:		    return RED;
		case GLenum.GL_R16: 				return RED;
		case GLenum.GL_R16_SNORM : 			return RED;
		case GLenum.GL_RG8:					return RG;
		case GLenum.GL_RG8_SNORM:			return RG;
		case GLenum.GL_RG16:				return RG;
		case GLenum.GL_RG16_SNORM:			return RG;
		case GLenum.GL_R3_G3_B2:			return RGB;
		case GLenum.GL_RGB4:				return RGB;
		case GLenum.GL_RGB5:				return RGB;
		case GLenum.GL_RGB8:				return RGB;
		case GLenum.GL_RGB8_SNORM:			return RGB;
		case GLenum.GL_RGB10:				return RGB;
		case GLenum.GL_RGB12:				return RGB;
		case GLenum.GL_RGB16_SNORM:			return RGB;
		case GLenum.GL_RGBA2:				return RGBA;  // TODO
		case GLenum.GL_RGBA4:				return RGBA;  // TODO
		case GLenum.GL_RGB5_A1:				return RGBA;  // TODO
		case GLenum.GL_RGBA8:				return RGBA;
		case GLenum.GL_RGBA8_SNORM:			return RGBA;
		case GLenum.GL_RGB10_A2:			return RGBA;
		case GLenum.GL_RGB10_A2UI:			return RGBA_INTEGER;
		case GLenum.GL_RGBA12:				return RGBA;
		case GLenum.GL_RGBA16:				return RGBA;
		case GLenum.GL_SRGB8:				return RGB;
		case GLenum.GL_SRGB8_ALPHA8:		return RGBA;
		case GLenum.GL_R16F:				return RED;
		case GLenum.GL_RG16F:				return RG;
		case GLenum.GL_RGB16F:				return RGB;
		case GLenum.GL_RGBA16F:				return RGBA;
		case GLenum.GL_R32F:				return RED;
		case GLenum.GL_RG32F:				return RG;
		case GLenum.GL_RGB32F:				return RGB;
		case GLenum.GL_RGBA32F:				return RGBA;
		case GLenum.GL_R11F_G11F_B10F:		return RGB;
		case GLenum.GL_RGB9_E5:				return RGB; // TODO ?
		case GLenum.GL_R8I:					return RED_INTEGER;
		case GLenum.GL_R8UI:				return RED_INTEGER;
		case GLenum.GL_R16I:				return RED_INTEGER;
		case GLenum.GL_R16UI:				return RED_INTEGER;
		case GLenum.GL_R32I:				return RED_INTEGER;
		case GLenum.GL_R32UI:				return RED_INTEGER;
		case GLenum.GL_RG8I:				return RG_INTEGER;
		case GLenum.GL_RG8UI:				return RG_INTEGER;
		case GLenum.GL_RG16I:				return RG_INTEGER;
		case GLenum.GL_RG16UI:				return RG_INTEGER;
		case GLenum.GL_RG32I:				return RG_INTEGER;
		case GLenum.GL_RG32UI:				return RG_INTEGER;
		case GLenum.GL_RGB8I:				return RGB_INTEGER;
		case GLenum.GL_RGB8UI:				return RGB_INTEGER;
		case GLenum.GL_RGB16I:				return RGB_INTEGER;
		case GLenum.GL_RGB16UI:				return RGB_INTEGER;
		case GLenum.GL_RGB32I:				return RGB_INTEGER;
		case GLenum.GL_RGB32UI:				return RGB_INTEGER;
		
		case GLenum.GL_RGBA8I:				return RGBA_INTEGER;
		case GLenum.GL_RGBA8UI:				return RGBA_INTEGER;
		case GLenum.GL_RGBA16I:				return RGBA_INTEGER;
		case GLenum.GL_RGBA16UI:			return RGBA_INTEGER;
		case GLenum.GL_RGBA32I:				return RGBA_INTEGER;
		case GLenum.GL_RGBA32UI:			return RGBA_INTEGER;
		case GLenum.GL_DEPTH_COMPONENT16:
		case GLenum.GL_DEPTH_COMPONENT24:
		case GLenum.GL_DEPTH_COMPONENT32:
		case GLenum.GL_DEPTH_COMPONENT32F:
											return GLenum.GL_DEPTH_COMPONENT;
		case GLenum.GL_DEPTH24_STENCIL8:
		case GLenum.GL_DEPTH32F_STENCIL8:
											return GLenum.GL_DEPTH_STENCIL;
		case GLenum.GL_STENCIL_INDEX8:
											return GLenum.GL_STENCIL;
		case GLenum.GL_COMPRESSED_RGBA_S3TC_DXT1_EXT:
		case GLenum.GL_COMPRESSED_RGB_S3TC_DXT1_EXT:
		case GLenum.GL_COMPRESSED_RGBA_S3TC_DXT3_EXT:
		case GLenum.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT:
											return GLenum.GL_NONE;
		default:
			throw new IllegalArgumentException("Unkown internalFormat: " + internalFormat);
		}
	}

	public static boolean isDepthFormat(int internalFormat){
		switch (internalFormat) {
			case GLenum.GL_DEPTH_COMPONENT16:
			case GLenum.GL_DEPTH_COMPONENT24:
			case GLenum.GL_DEPTH_COMPONENT32:
			case GLenum.GL_DEPTH_COMPONENT32F:
			case GLenum.GL_DEPTH24_STENCIL8:
			case GLenum.GL_DEPTH32F_STENCIL8:
				return true;
			default:
				return false;
		}
	}

	public static boolean isStencilFormat(int internalFormat){
		switch (internalFormat) {
			case GLenum.GL_DEPTH24_STENCIL8:
			case GLenum.GL_DEPTH32F_STENCIL8:
			case GLenum.GL_STENCIL_INDEX8:
				return true;
			default:
				return false;
		}
	}

	public static boolean isColorFormat(int internalFormat){
		switch (internalFormat) {
			case GLenum.GL_R8:
			case GLenum.GL_R8_SNORM:
			case GLenum.GL_R16:
			case GLenum.GL_R16_SNORM:
			case GLenum.GL_RG8:
			case GLenum.GL_RG8_SNORM:
			case GLenum.GL_RG16:
			case GLenum.GL_RG16_SNORM:
			case GLenum.GL_R3_G3_B2:
			case GLenum.GL_RGB4:
			case GLenum.GL_RGB5:
			case GLenum.GL_RGB8:
			case GLenum.GL_RGB8_SNORM:
			case GLenum.GL_RGB10:
			case GLenum.GL_RGB12:
			case GLenum.GL_RGB16_SNORM:
			case GLenum.GL_RGBA2:
			case GLenum.GL_RGBA4:
			case GLenum.GL_RGB5_A1:
			case GLenum.GL_RGBA8:
			case GLenum.GL_RGBA8_SNORM:
			case GLenum.GL_RGB10_A2:
			case GLenum.GL_RGB10_A2UI:
			case GLenum.GL_RGBA12:
			case GLenum.GL_RGBA16:
			case GLenum.GL_SRGB8:
			case GLenum.GL_SRGB8_ALPHA8:
			case GLenum.GL_R16F:
			case GLenum.GL_RG16F:
			case GLenum.GL_RGB16F:
			case GLenum.GL_RGBA16F:
			case GLenum.GL_R32F:
			case GLenum.GL_RG32F:
			case GLenum.GL_RGB32F:
			case GLenum.GL_RGBA32F:
			case GLenum.GL_R11F_G11F_B10F:
			case GLenum.GL_RGB9_E5:
			case GLenum.GL_R8I:
			case GLenum.GL_R8UI:
			case GLenum.GL_R16I:
			case GLenum.GL_R16UI:
			case GLenum.GL_R32I:
			case GLenum.GL_R32UI:
			case GLenum.GL_RG8I:
			case GLenum.GL_RG8UI:
			case GLenum.GL_RG16I:
			case GLenum.GL_RG16UI:
			case GLenum.GL_RG32I:
			case GLenum.GL_RG32UI:
			case GLenum.GL_RGB8I:
			case GLenum.GL_RGB8UI:
			case GLenum.GL_RGB16I:
			case GLenum.GL_RGB16UI:
			case GLenum.GL_RGB32I:
			case GLenum.GL_RGB32UI:

			case GLenum.GL_RGBA8I:
			case GLenum.GL_RGBA8UI:
			case GLenum.GL_RGBA16I:
			case GLenum.GL_RGBA16UI:
			case GLenum.GL_RGBA32I:
			case GLenum.GL_RGBA32UI:
				return true;
			default:
				return false;
		}
	}
	
	public static int measureDataType(int internalFormat){
		switch (internalFormat) {
		case GLenum.GL_R8:  				return GLenum.GL_UNSIGNED_BYTE;
		case GLenum.GL_R8_SNORM:		    return GLenum.GL_BYTE;
		case GLenum.GL_R16: 				return GLenum.GL_UNSIGNED_SHORT;
		case GLenum.GL_R16_SNORM : 		return GLenum.GL_SHORT;
		case GLenum.GL_RG8:				return GLenum.GL_UNSIGNED_BYTE;
		case GLenum.GL_RG8_SNORM:			return GLenum.GL_BYTE;
		case GLenum.GL_RG16:				return GLenum.GL_UNSIGNED_SHORT;
		case GLenum.GL_RG16_SNORM:		return GLenum.GL_SHORT;
		case GLenum.GL_R3_G3_B2:			return GLenum.GL_UNSIGNED_BYTE_3_3_2;
		case GLenum.GL_RGB4:				return GLenum.GL_UNSIGNED_BYTE;  // TODO ?
		case GLenum.GL_RGB5:				return GLenum.GL_UNSIGNED_BYTE;  // TODO ?
		case GLenum.GL_RGB8:				return GLenum.GL_UNSIGNED_BYTE;
		case GLenum.GL_RGB8_SNORM:		return GLenum.GL_BYTE;
		case GLenum.GL_RGB10:				return GLenum.GL_UNSIGNED_BYTE;  // TODO
		case GLenum.GL_RGB12:				return GLenum.GL_UNSIGNED_BYTE;  // TODO
 		case GLenum.GL_RGB16_SNORM:		return GLenum.GL_SHORT;
		case GLenum.GL_RGBA2:				return GLenum.GL_UNSIGNED_BYTE;  // TODO
		case GLenum.GL_RGBA4:				return GLenum.GL_UNSIGNED_SHORT_4_4_4_4;  // TODO
		case GLenum.GL_RGB5_A1:			return GLenum.GL_UNSIGNED_SHORT_5_5_5_1;  // TODO
		case GLenum.GL_RGBA8:				return GLenum.GL_UNSIGNED_BYTE;
		case GLenum.GL_RGBA8_SNORM:		return GLenum.GL_BYTE;
		case GLenum.GL_RGB10_A2:			return GLenum.GL_UNSIGNED_INT_10_10_10_2;
		case GLenum.GL_RGB10_A2UI:		return GLenum.GL_UNSIGNED_INT_10_10_10_2; // TODO
		case GLenum.GL_RGBA12:			return GLenum.GL_UNSIGNED_BYTE;
		case GLenum.GL_RGBA16:			return GLenum.GL_UNSIGNED_SHORT;
		case GLenum.GL_SRGB8:				return GLenum.GL_BYTE;
		case GLenum.GL_SRGB8_ALPHA8:		return GLenum.GL_BYTE;
		case GLenum.GL_R16F:				return GLenum.GL_HALF_FLOAT;
		case GLenum.GL_RG16F:				return GLenum.GL_HALF_FLOAT;
		case GLenum.GL_RGB16F:			return GLenum.GL_HALF_FLOAT;
		case GLenum.GL_RGBA16F:			return GLenum.GL_HALF_FLOAT;
		case GLenum.GL_R32F:				return GLenum.GL_FLOAT;
		case GLenum.GL_RG32F:				return GLenum.GL_FLOAT;
		case GLenum.GL_RGB32F:			return GLenum.GL_FLOAT;
		case GLenum.GL_RGBA32F:			return GLenum.GL_FLOAT;
		case GLenum.GL_R11F_G11F_B10F:	return GLenum.GL_UNSIGNED_INT_10F_11F_11F_REV;
		case GLenum.GL_RGB9_E5:			return GLenum.GL_UNSIGNED_INT_5_9_9_9_REV; // TODO ?
		case GLenum.GL_R8I:				return GLenum.GL_BYTE;
		case GLenum.GL_R8UI:				return GLenum.GL_UNSIGNED_BYTE;
		case GLenum.GL_R16I:				return GLenum.GL_SHORT;
		case GLenum.GL_R16UI:				return GLenum.GL_UNSIGNED_SHORT;
		case GLenum.GL_R32I:				return GLenum.GL_INT;
		case GLenum.GL_R32UI:				return GLenum.GL_UNSIGNED_INT;
		case GLenum.GL_RG8I:				return GLenum.GL_BYTE;
		case GLenum.GL_RG8UI:				return GLenum.GL_UNSIGNED_BYTE;
		case GLenum.GL_RG16I:				return GLenum.GL_SHORT;
		case GLenum.GL_RG16UI:			return GLenum.GL_UNSIGNED_SHORT;
		case GLenum.GL_RG32I:				return GLenum.GL_INT;
		case GLenum.GL_RG32UI:			return GLenum.GL_UNSIGNED_INT;
		case GLenum.GL_RGB8I:				return GLenum.GL_BYTE;
		case GLenum.GL_RGB8UI:			return GLenum.GL_UNSIGNED_BYTE;
		case GLenum.GL_RGB16I:			return GLenum.GL_SHORT;
		case GLenum.GL_RGB16UI:			return GLenum.GL_UNSIGNED_SHORT;
		case GLenum.GL_RGB32I:			return GLenum.GL_INT;
		case GLenum.GL_RGB32UI:			return GLenum.GL_UNSIGNED_INT;
		
		case GLenum.GL_RGBA8I:			return GLenum.GL_BYTE;
		case GLenum.GL_RGBA8UI:			return GLenum.GL_UNSIGNED_BYTE;
		case GLenum.GL_RGBA16I:			return GLenum.GL_SHORT;
		case GLenum.GL_RGBA16UI:			return GLenum.GL_UNSIGNED_SHORT;
		case GLenum.GL_RGBA32I:			return GLenum.GL_INT;
		case GLenum.GL_RGBA32UI:			return GLenum.GL_UNSIGNED_INT;
		case GLenum.GL_DEPTH_COMPONENT16: return GLenum.GL_UNSIGNED_SHORT;
		case GLenum.GL_DEPTH_COMPONENT24:
		case GLenum.GL_DEPTH24_STENCIL8:  return GLenum.GL_UNSIGNED_INT_24_8;
		case GLenum.GL_DEPTH_COMPONENT32: return GLenum.GL_UNSIGNED_INT;
		case GLenum.GL_DEPTH_COMPONENT32F:return GLenum.GL_FLOAT;
		
		default:
			throw new IllegalArgumentException("Unkown internalFormat: " + internalFormat);
		}
	}

	@CachaRes
	public static TextureDesc getTexParameters(int target, int textureID){
		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();

		GLStateTracker.getInstance().bindTexture(target, textureID);

		TextureDesc desc = new TextureDesc();
		desc.target = target;
		desc.depthStencilTextureMode = gl.glGetTexParameteri(target, GLenum.GL_DEPTH_STENCIL_TEXTURE_MODE);
		desc.lodBias                 = gl.glGetTexParameterf(target, GLenum.GL_TEXTURE_LOD_BIAS);
		desc.magFilter               = gl.glGetTexParameteri(target, GLenum.GL_TEXTURE_MAG_FILTER);
		desc.minFilter               = gl.glGetTexParameteri(target, GLenum.GL_TEXTURE_MIN_FILTER);
		desc.minLod                  = gl.glGetTexParameterf(target, GLenum.GL_TEXTURE_MIN_LOD);
		desc.maxLod                  = gl.glGetTexParameterf(target, GLenum.GL_TEXTURE_MAX_LOD);
		desc.baseLevel               = gl.glGetTexParameteri(target, GLenum.GL_TEXTURE_BASE_LEVEL);
		desc.maxLevel                = gl.glGetTexParameteri(target, GLenum.GL_TEXTURE_MAX_LEVEL);
		desc.swizzleR                = gl.glGetTexParameteri(target, GLenum.GL_TEXTURE_SWIZZLE_R);
		desc.swizzleG                = gl.glGetTexParameteri(target, GLenum.GL_TEXTURE_SWIZZLE_G);
		desc.swizzleB                = gl.glGetTexParameteri(target, GLenum.GL_TEXTURE_SWIZZLE_B);
		desc.swizzleA                = gl.glGetTexParameteri(target, GLenum.GL_TEXTURE_SWIZZLE_A);
		desc.wrapS                   = gl.glGetTexParameteri(target, GLenum.GL_TEXTURE_WRAP_S);
		desc.wrapT                   = gl.glGetTexParameteri(target, GLenum.GL_TEXTURE_WRAP_T);
		desc.wrapR                   = gl.glGetTexParameteri(target, GLenum.GL_TEXTURE_WRAP_R);
		FloatBuffer buffer = CacheBuffer.getCachedFloatBuffer(4);
		gl.glGetTexParameterfv(target, GLenum.GL_TEXTURE_BORDER_COLOR, buffer);
		buffer.get(desc.borderColor);
		desc.compareMode             = gl.glGetTexParameteri(target, GLenum.GL_TEXTURE_COMPARE_MODE);
		desc.compareFunc             = gl.glGetTexParameteri(target, GLenum.GL_TEXTURE_COMPARE_FUNC);
		desc.immutableFormat         = gl.glGetTexParameteri(target, GLenum.GL_TEXTURE_IMMUTABLE_FORMAT) != 0;
		desc.imageFormatCompatibilityType = gl.glGetTexParameteri(target, GLenum.GL_IMAGE_FORMAT_COMPATIBILITY_TYPE);
		if(desc.immutableFormat){
			desc.textureViewMinLevel     = gl.glGetTexParameteri(target, GLenum.GL_TEXTURE_VIEW_MIN_LEVEL);
			desc.textureViewNumLevels    = gl.glGetTexParameteri(target, GLenum.GL_TEXTURE_VIEW_NUM_LEVELS);
			desc.textureViewMinLayer     = gl.glGetTexParameteri(target, GLenum.GL_TEXTURE_VIEW_MIN_LAYER);
			desc.textureViewNumLayers    = gl.glGetTexParameteri(target, GLenum.GL_TEXTURE_VIEW_NUM_LAYERS);
			desc.immutableLayer          = gl.glGetTexParameteri(target, GLenum.GL_TEXTURE_IMMUTABLE_LEVELS);
		}
		
		List<TextureLevelDesc> levelDescs = new ArrayList<TextureLevelDesc>();
		
		int level = 0;
		while (true) {
			TextureLevelDesc levelDesc = new TextureLevelDesc();
			levelDesc.width  = gl.glGetTexLevelParameteri(target, level, GLenum.GL_TEXTURE_WIDTH);
			if(levelDesc.width == 0){
//				System.out.println("Level" + level + ": width = " + levelDesc.width);
				break;
			}
			
			levelDesc.height = gl.glGetTexLevelParameteri(target, level, GLenum.GL_TEXTURE_HEIGHT);
			levelDesc.depth  = gl.glGetTexLevelParameteri(target, level, GLenum.GL_TEXTURE_DEPTH);
			levelDesc.internalFormat = gl.glGetTexLevelParameteri(target, level, GLenum.GL_TEXTURE_INTERNAL_FORMAT);

			levelDesc.redType   = gl.glGetTexLevelParameteri(target, level, GLenum.GL_TEXTURE_RED_TYPE);
			levelDesc.greenType = gl.glGetTexLevelParameteri(target, level, GLenum.GL_TEXTURE_GREEN_TYPE);
			levelDesc.blueType  = gl.glGetTexLevelParameteri(target, level, GLenum.GL_TEXTURE_BLUE_TYPE);
			levelDesc.alphaType = gl.glGetTexLevelParameteri(target, level, GLenum.GL_TEXTURE_ALPHA_TYPE);
			levelDesc.depthType = gl.glGetTexLevelParameteri(target, level, GLenum.GL_TEXTURE_DEPTH_TYPE);
			
			levelDesc.redSize   = gl.glGetTexLevelParameteri(target, level, GLenum.GL_TEXTURE_RED_SIZE);
			levelDesc.greenSize = gl.glGetTexLevelParameteri(target, level, GLenum.GL_TEXTURE_GREEN_SIZE);
			levelDesc.blueSize  = gl.glGetTexLevelParameteri(target, level, GLenum.GL_TEXTURE_BLUE_SIZE);
			levelDesc.alphaSize = gl.glGetTexLevelParameteri(target, level, GLenum.GL_TEXTURE_ALPHA_SIZE);
			levelDesc.depthSize = gl.glGetTexLevelParameteri(target, level, GLenum.GL_TEXTURE_DEPTH_SIZE);
			levelDesc.stencilSize = gl.glGetTexLevelParameteri(target, level, GLenum.GL_TEXTURE_STENCIL_SIZE);
			levelDesc.samples   = gl.glGetTexLevelParameteri(target, level, GLenum.GL_TEXTURE_SAMPLES);
			
			levelDesc.compressed = gl.glGetTexLevelParameteri(target, level, GLenum.GL_TEXTURE_COMPRESSED) != 0;
			if(levelDesc.compressed){
				levelDesc.compressedImageSize = gl.glGetTexLevelParameteri(target, level, GLenum.GL_TEXTURE_COMPRESSED_IMAGE_SIZE);
			}
			
			if(target == GLenum.GL_TEXTURE_BUFFER){
				levelDesc.bufferOffset = gl.glGetTexLevelParameteri(target, level, GLenum.GL_TEXTURE_BUFFER_OFFSET);
				levelDesc.bufferSize   = gl.glGetTexLevelParameteri(target, level, GLenum.GL_TEXTURE_BUFFER_SIZE);
				
				levelDescs.add(levelDesc);
				break;
			}
			
			levelDescs.add(levelDesc);
			if(levelDesc.width == 1 && levelDesc.height == 1){
				break;
			}
			
			level++;
		}
		
		desc.levelDescs = levelDescs.toArray(new TextureLevelDesc[levelDescs.size()]);
		return desc;
	}

	public static String getCompareModeName(int mode){
		switch (mode) {
			case GLenum.GL_ALWAYS:  return "GL_ALWAYS";
			case GLenum.GL_NEVER:  return "GL_NEVER";
			case GLenum.GL_LESS:  return "GL_LESS";
			case GLenum.GL_LEQUAL:  return "GL_LEQUAL";
			case GLenum.GL_GREATER:  return "GL_GREATER";
			case GLenum.GL_GEQUAL:  return "GL_GEQUAL";
			case GLenum.GL_NOTEQUAL:  return "GL_NOTEQUAL";

			default:
				return "Unkown CompareMode(0x" + Integer.toHexString(mode) + ")";
		}
	}

}
