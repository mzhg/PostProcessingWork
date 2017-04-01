package jet.opengl.postprocessing.texture;

public class Texture2DDesc {

	public int width;
    public int height;
    public int mipLevels = 1;
    public int arraySize = 1;
    public int format;
    public int sampleCount;
//    public int bindFlags;
//    public int cpuAccessFlags;
//    public int miscFlags;
    
    public Texture2DDesc(int width, int height, int format){
    	this(width, height, 1, 1, format, 1);
    }
    
    public Texture2DDesc(int width, int height, int mipLevels, int arraySize, int format, int samples){
    	this.width = width;
    	this.height = height;
    	this.mipLevels = mipLevels;
    	this.arraySize = arraySize;
    	this.format = format;
    	this.sampleCount = samples;
    }
    
    public Texture2DDesc(int width, int height, int mipLevels, int arraySize, int format, int samples, int qulity){
    	this.width = width;
    	this.height = height;
    	this.mipLevels = mipLevels;
    	this.arraySize = arraySize;
    	this.format = format;
    	this.sampleCount = samples;
    }
    
	public Texture2DDesc() {}
	
	public Texture2DDesc(Texture2DDesc o) {
		set(o);
	}
	
	public void set(Texture2DDesc o){
		width = o.width;
		height = o.height;
		mipLevels = o.mipLevels;
		arraySize = o.arraySize;
		format = o.format;
		sampleCount = o.sampleCount;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + arraySize;
		result = prime * result + format;
		result = prime * result + height;
		result = prime * result + mipLevels;
		result = prime * result + sampleCount;
		result = prime * result + width;
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Texture2DDesc other = (Texture2DDesc) obj;
		if (arraySize != other.arraySize)
			return false;
		if (format != other.format)
			return false;
		if (height != other.height)
			return false;
		if (mipLevels != other.mipLevels)
			return false;
		if(sampleCount != other.sampleCount)
			return false;
		if (width != other.width)
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return toString("Texture2DDesc");
	}

	public String toString(String name) {
		return name + " [width=" + width + ", height=" + height + ", mipLevels=" + mipLevels + ", arraySize="
				+ arraySize + ", format=" + TextureUtils.getFormatName(format) + ", samples=" + sampleCount + "]";
	}
}
