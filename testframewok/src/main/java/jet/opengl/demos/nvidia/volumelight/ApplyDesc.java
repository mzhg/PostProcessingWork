package jet.opengl.demos.nvidia.volumelight;

final class ApplyDesc {

	int sampleMode;
	int upsampleMode;
	int fogMode;
	
	public ApplyDesc() {}
	
	public ApplyDesc(ApplyDesc o) {
		set(o);
	}
	
	public void set(ApplyDesc o){
		sampleMode = o.sampleMode;
		upsampleMode = o.upsampleMode;
		fogMode = o.fogMode;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + fogMode;
		result = prime * result + sampleMode;
		result = prime * result + upsampleMode;
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
		ApplyDesc other = (ApplyDesc) obj;
		if (fogMode != other.fogMode)
			return false;
		if (sampleMode != other.sampleMode)
			return false;
		if (upsampleMode != other.upsampleMode)
			return false;
		return true;
	}
}
