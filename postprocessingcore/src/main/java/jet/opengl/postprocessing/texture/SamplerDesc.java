package jet.opengl.postprocessing.texture;

import jet.opengl.postprocessing.common.GLenum;

public class SamplerDesc {

	/** Min filter, default value is LINEAR. */
	public int minFilter = GLenum.GL_LINEAR;
	/** Mag filter, default value is LINEAR. */
	public int magFilter = GLenum.GL_LINEAR;
	/** Default value is CLAMP_TO_EDGE. */
	public int wrapS = GLenum.GL_CLAMP_TO_EDGE;
	/** Default value is CLAMP_TO_EDGE. */
	public int wrapT = GLenum.GL_CLAMP_TO_EDGE;
	/** Default value is CLAMP_TO_EDGE. */
	public int wrapR = GLenum.GL_CLAMP_TO_EDGE;
	/** The texture border color, default is 0. */
	public int borderColor;
	/** Default is 0. */
	public int anisotropic = 0;
	
	/** Used with depth texture sampling. Default is 0. */
	public int compareFunc = 0,
			   compareMode = 0;
	
	public SamplerDesc(int minFilter, int magFilter, int wrapS, int wrapT, int wrapR){
		this(minFilter, magFilter, wrapS, wrapT, wrapR, 0, 0, 0, 0);
	}
	public SamplerDesc(int minFilter, int magFilter, int wrapS, int wrapT, int wrapR, int borderColor, int anisotropic,
			int compareFunc, int compareMode) {
		this.minFilter = minFilter;
		this.magFilter = magFilter;
		this.wrapS = wrapS;
		this.wrapT = wrapT;
		this.wrapR = wrapR;
		this.borderColor = borderColor;
		this.anisotropic = anisotropic;
		this.compareFunc = compareFunc;
		this.compareMode = compareMode;
	}

	public SamplerDesc() {
	}
	
	public SamplerDesc(SamplerDesc other) {
		set(other);
	}
	
	public void set(SamplerDesc other){
		minFilter = other.minFilter;
		magFilter = other.magFilter;
		wrapS = other.wrapS;
		wrapT = other.wrapT;
		wrapR = other.wrapR;
		borderColor = other.borderColor;
		anisotropic = other.anisotropic;
		compareFunc = other.compareFunc;
		compareMode = other.compareMode;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + anisotropic;
		result = prime * result + compareFunc;
		result = prime * result + compareMode;
		result = prime * result + magFilter;
		result = prime * result + minFilter;
		result = prime * result + wrapR;
		result = prime * result + wrapS;
		result = prime * result + wrapT;
		
		if(wrapR == GLenum.GL_CLAMP_TO_BORDER || wrapS == GLenum.GL_CLAMP_TO_BORDER || wrapT == GLenum.GL_CLAMP_TO_BORDER){
			result = prime * result + borderColor;
		}
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
		SamplerDesc other = (SamplerDesc) obj;
		if (anisotropic != other.anisotropic)
			return false;
		if (compareFunc != other.compareFunc)
			return false;
		if (compareMode != other.compareMode)
			return false;
		if (magFilter != other.magFilter)
			return false;
		if (minFilter != other.minFilter)
			return false;
		if (wrapR != other.wrapR)
			return false;
		if (wrapS != other.wrapS)
			return false;
		if (wrapT != other.wrapT)
			return false;
		
		if(wrapR == GLenum.GL_CLAMP_TO_BORDER || wrapS == GLenum.GL_CLAMP_TO_BORDER || wrapT == GLenum.GL_CLAMP_TO_BORDER){
			return borderColor == other.borderColor;
		}
		
		return true;
	}
}
