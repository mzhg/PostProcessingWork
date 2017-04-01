package jet.opengl.postprocessing.texture;

public class TextureLevelDesc {

	TextureLevelDesc() {}
	public int width;
	public int height;
	public int depth;
	public int internalFormat;
	
	public int redType;
	public int greenType;
	public int blueType;
	public int alphaType;
	public int depthType;
	
	public int redSize;
	public int greenSize;
	public int blueSize;
	public int alphaSize;
	public int depthSize;
	public int stencilSize;
	
	public int totalBytes;
	
	public int samples;
	
	public boolean compressed;
	public int compressedImageSize;
	public int bufferOffset;
	public int bufferSize;
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("width = ").append(width).append("\n");
		sb.append("height = ").append(height).append("\n");
		sb.append("depth = ").append(depth).append("\n");
		
		sb.append("internalFormat = ").append(TextureUtils.getFormatName(internalFormat)).append("\n");
		
		sb.append("redType = ").append(TextureUtils.getTypeSignName(redType)).append('\n');
		sb.append("greenType = ").append(TextureUtils.getTypeSignName(greenType)).append('\n');
		sb.append("blueType = ").append(TextureUtils.getTypeSignName(blueType)).append('\n');
		sb.append("alphaType = ").append(TextureUtils.getTypeSignName(alphaType)).append('\n');
		sb.append("depthType = ").append(TextureUtils.getTypeSignName(depthType)).append('\n');
		
		sb.append("redSize = ").append(redSize).append("\n");
		sb.append("greenSize = ").append(greenSize).append("\n");
		sb.append("blueSize = ").append(blueSize).append("\n");
		sb.append("alphaSize = ").append(alphaSize).append("\n");
		sb.append("depthSize = ").append(depthSize).append("\n");
		sb.append("stencilSize = ").append(stencilSize).append("\n");

		sb.append("samples = ").append(samples).append("\n");

		sb.append("compressed = ").append(compressed).append("\n");
		if(compressed){
			sb.append("compressedImageSize = ").append(compressedImageSize).append("\n");
		}
		
		sb.append("bufferOffset = ").append(bufferOffset).append("\n");
		sb.append("bufferSize = ").append(bufferSize).append("\n");
		return sb.toString();
	}
}
