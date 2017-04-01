package jet.opengl.postprocessing.texture;

public class Texture2D extends TextureGL{

	int width;
	int height;
	
	int arraySize = 1;
	int samples;
    
    public Texture2D() {
	}
    
    public int getArraySize() { return arraySize;}
    public int getWidth()   { return width;}
    public int getHeight()  { return height;}
    public int getSampleCount() { return samples;}
    
    @Override
    public String toString() {
    	return toString("Texture2D");
    }
    
    public String toString(String name) {
    	StringBuilder sb = new StringBuilder(80);
    	sb.append(name).append(' ').append('[');
    	sb.append("textureID: ").append(textureID).append(',').append(' ');
    	sb.append("target: ").append(TextureUtils.getTextureTargetName(target)).append(',').append(' ');
    	sb.append("width = ").append(width).append(',').append(' ');
    	sb.append("height = ").append(height).append(',').append(' ');
    	sb.append("format = ").append(TextureUtils.getFormatName(format)).append(',').append(' ');
    	sb.append("arraySize = ").append(arraySize).append(',').append(' ');
    	sb.append("mipLevels = ").append(mipLevels).append(',').append(' ');
    	sb.append("samples = ").append(samples).append(',').append(' ');
    	
    	return sb.toString();
    }
    
    public Texture2DDesc getDesc() { return getDesc(null);}

	public Texture2DDesc getDesc(Texture2DDesc out){
		if(out == null)
			out = new Texture2DDesc();
		out.arraySize = arraySize;
		out.format = format;
		out.height = height;
		out.mipLevels = getMipLevels();
		out.sampleCount =  samples;
		out.width = width;
		
		return out;
	}

}
