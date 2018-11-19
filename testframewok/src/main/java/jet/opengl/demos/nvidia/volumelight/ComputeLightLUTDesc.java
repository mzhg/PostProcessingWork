package jet.opengl.demos.nvidia.volumelight;

final class ComputeLightLUTDesc {

	int lightMode;
	int attenuationMode;
	int computePass;
	
	public ComputeLightLUTDesc() {}
	
	public ComputeLightLUTDesc(ComputeLightLUTDesc o) {
		set(o);
	}
	
	public void set(ComputeLightLUTDesc o){
		lightMode = o.lightMode;
		attenuationMode = o.attenuationMode;
		computePass = o.computePass;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + attenuationMode;
		result = prime * result + computePass;
		result = prime * result + lightMode;
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
		ComputeLightLUTDesc other = (ComputeLightLUTDesc) obj;
		if (attenuationMode != other.attenuationMode)
			return false;
		if (computePass != other.computePass)
			return false;
		if (lightMode != other.lightMode)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "[lightMode=" + lightMode + ", attenuationMode=" + attenuationMode + ", computePass="
				+ computePass + "]";
	}
}
