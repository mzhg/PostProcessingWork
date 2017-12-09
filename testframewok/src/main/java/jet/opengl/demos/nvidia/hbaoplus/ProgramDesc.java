package jet.opengl.demos.nvidia.hbaoplus;

import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.util.CommonUtil;

final class ProgramDesc {

	String geomFile;
	String fragFile;
	
	boolean sharpnessProfile;
	int kernelRadius;
	boolean foregroundAO;
	boolean backgroundAO;
	boolean depthThreshold;
	int fetchGBufferNormal;
	boolean resolveDepth;
	boolean blur;
	
	void reset(){
		geomFile = null;
		sharpnessProfile = false;
		kernelRadius = 0;
		foregroundAO = false;
		backgroundAO = false;
		depthThreshold = false;
		fetchGBufferNormal = 0;
		resolveDepth = false;
		blur = false;
	}
	
	public ProgramDesc() {}
	
	public ProgramDesc(ProgramDesc o) {
		set(o);
	}
	
	public void set(ProgramDesc o){
		geomFile = o.geomFile;
		fragFile = o.fragFile;
		sharpnessProfile = o.sharpnessProfile;
		kernelRadius = o.kernelRadius;
		foregroundAO = o.foregroundAO;
		backgroundAO = o.backgroundAO;
		depthThreshold = o.depthThreshold;
		fetchGBufferNormal = o.fetchGBufferNormal;
		resolveDepth = o.resolveDepth;
		blur = o.blur;
	}
	
	public Macro[] getMacros(){
		return new Macro[]{
			new Macro("RESOLVE_DEPTH", resolveDepth),
			new Macro("ENABLE_SHARPNESS_PROFILE", sharpnessProfile),
			new Macro("KERNEL_RADIUS", kernelRadius),
			new Macro("ENABLE_FOREGROUND_AO", foregroundAO),
			new Macro("ENABLE_BACKGROUND_AO", backgroundAO),
			new Macro("ENABLE_DEPTH_THRESHOLD", depthThreshold),
			new Macro("FETCH_GBUFFER_NORMAL", fetchGBufferNormal),
		};
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (backgroundAO ? 1231 : 1237);
		result = prime * result + (blur ? 1231 : 1237);
		result = prime * result + (depthThreshold ? 1231 : 1237);
		result = prime * result + fetchGBufferNormal;
		result = prime * result + (foregroundAO ? 1231 : 1237);
		result = prime * result + ((fragFile == null) ? 0 : fragFile.hashCode());
		result = prime * result + ((geomFile == null) ? 0 : geomFile.hashCode());
		result = prime * result + kernelRadius;
		result = prime * result + (resolveDepth ? 1231 : 1237);
		result = prime * result + (sharpnessProfile ? 1231 : 1237);
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

		ProgramDesc other = (ProgramDesc) obj;
		if (backgroundAO != other.backgroundAO)
			return false;
		if (blur != other.blur)
			return false;
		if (depthThreshold != other.depthThreshold)
			return false;
		if (fetchGBufferNormal != other.fetchGBufferNormal)
			return false;
		if (foregroundAO != other.foregroundAO)
			return false;
		
		if(!CommonUtil.equals(fragFile, other.fragFile)){
			return false;
		}
		
		if(!CommonUtil.equals(geomFile, other.geomFile)){
			return false;
		}
		
		if (kernelRadius != other.kernelRadius)
			return false;
		if (resolveDepth != other.resolveDepth)
			return false;
		if (sharpnessProfile != other.sharpnessProfile)
			return false;
		return true;
	}
}
