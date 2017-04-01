package jet.opengl.postprocessing.texture;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;

public class SamplerDesc {

	/** Min filter, default value is LINEAR. */
	public int minFilter = GL11.GL_LINEAR;
	/** Mag filter, default value is LINEAR. */
	public int magFilter = GL11.GL_LINEAR;
	/** Default value is CLAMP_TO_EDGE. */
	public int wrapS = GL12.GL_CLAMP_TO_EDGE;
	/** Default value is CLAMP_TO_EDGE. */
	public int wrapT = GL12.GL_CLAMP_TO_EDGE;
	/** Default value is CLAMP_TO_EDGE. */
	public int wrapR = GL12.GL_CLAMP_TO_EDGE;
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
		
		if(wrapR == GL13.GL_CLAMP_TO_BORDER || wrapS == GL13.GL_CLAMP_TO_BORDER || wrapT == GL13.GL_CLAMP_TO_BORDER){
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
		
		if(wrapR == GL13.GL_CLAMP_TO_BORDER || wrapS == GL13.GL_CLAMP_TO_BORDER || wrapT == GL13.GL_CLAMP_TO_BORDER){
			return borderColor == other.borderColor;
		}
		
		return true;
	}
}
