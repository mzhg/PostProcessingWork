package jet.opengl.postprocessing.texture;

public class Texture3DDesc {

	public int width;
    public int height;
    public int depth;
    public int mipLevels = 1;
    public int format;
//    public int bindFlags;
//    public int cpuAccessFlags;
//    public int miscFlags;
    
    public Texture3DDesc(int width, int height, int depth, int mipLevels, int format){
    	this.width = width;
    	this.height = height;
    	this.depth = depth;
    	this.mipLevels = mipLevels;
    	this.format = format;
    }
    
	public Texture3DDesc() {}
	
	public Texture3DDesc(Texture3DDesc o) {
		set(o);
	}
	
	public void set(Texture3DDesc o){
		width = o.width;
		height = o.height;
		mipLevels = o.mipLevels;
		format = o.format;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + depth;
		result = prime * result + format;
		result = prime * result + height;
		result = prime * result + mipLevels;
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
		Texture3DDesc other = (Texture3DDesc) obj;
		if (depth != other.depth)
			return false;
		if (format != other.format)
			return false;
		if (height != other.height)
			return false;
		if (mipLevels != other.mipLevels)
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
		return name + " [width=" + width + ", height=" + height + ", depth=" + depth + ", mipLevels=" + mipLevels + 
				", format=" + TextureUtils.getFormatName(format) + "]";
	}
}
