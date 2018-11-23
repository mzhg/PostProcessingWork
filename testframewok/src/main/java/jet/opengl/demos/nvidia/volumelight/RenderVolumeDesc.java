package jet.opengl.demos.nvidia.volumelight;

class RenderVolumeDesc {

	public static final int MESHMODE_FRUSTUM_GRID = 1,
							MESHMODE_FRUSTUM_BASE = 2,
							MESHMODE_FRUSTUM_CAP = 3,
							MESHMODE_OMNI_VOLUME = 4,
							MESHMODE_GEOMETRY = 5;
	
	public static final int SHADOWMAPTYPE_ATLAS = 1;
	public static final int SHADOWMAPTYPE_ARRAY = 2;
	
	public static final int CASCADECOUNT_1 = 1;
	public static final int CASCADECOUNT_2 = 2;
	public static final int CASCADECOUNT_3 = 3;
	public static final int CASCADECOUNT_4 = 4;
	
	public static final int VOLUMETYPE_FRUSTUM = 1;
	public static final int VOLUMETYPE_PARABOLOID = 2;
	
	public static final float MAXTESSFACTOR_LOW = 16.0f;
	public static final float MAXTESSFACTOR_MEDIUM = 32.0f;
	public static final float MAXTESSFACTOR_HIGH = 64.0f;
	
	public static final int SAMPLEMODE_SINGLE = 1;
	public static final int SAMPLEMODE_MSAA = 2;
	
	public static final int LIGHTMODE_DIRECTIONAL = 1;
	public static final int LIGHTMODE_SPOTLIGHT = 2;
	public static final int LIGHTMODE_OMNI = 3;
	
	public static final int PASSMODE_GEOMETRY = 1;
	public static final int PASSMODE_SKY = 2;
	public static final int PASSMODE_FINAL = 3;
	
	public static final int ATTENUATIONMODE_NONE = AttenuationMode.NONE.ordinal();
	public static final int ATTENUATIONMODE_POLYNOMIAL = AttenuationMode.POLYNOMIAL.ordinal();
	public static final int ATTENUATIONMODE_INV_POLYNOMIAL = AttenuationMode.INV_POLYNOMIAL.ordinal();
	
	public static final int FALLOFFMODE_NONE = SpotlightFalloffMode.NONE.ordinal();
	public static final int FALLOFFMODE_FIXED = SpotlightFalloffMode.FIXED.ordinal();
	public static final int FALLOFFMODE_CUSTOM = SpotlightFalloffMode.CUSTOM.ordinal();
	public static final int FALLOFFMODE_INTEL = SpotlightFalloffMode.INTEL.ordinal();
	public static final int FALLOFFMODE_SRNN05 = SpotlightFalloffMode.SRNN05.ordinal();

	public static final int UPSAMPLEMODE_POINT = 1;
	public static final int UPSAMPLEMODE_BILINEAR = 2;
	public static final int UPSAMPLEMODE_BILATERAL = 3;
	
	public static final int FOGMODE_NONE = 0;
	public static final int FOGMODE_NOSKY = 1;
	public static final int FOGMODE_FULL = 2;
	
	public static final int COMPUTEPASS_CALCULATE = 1;
	public static final int COMPUTEPASS_SUM = 2;
	
	public int meshMode = MESHMODE_FRUSTUM_GRID;
	public int shadowMapType = SHADOWMAPTYPE_ATLAS;
	public int cascadeCount = CASCADECOUNT_1;
	public int volumeType = VOLUMETYPE_FRUSTUM;
	public float maxtessfactor = MAXTESSFACTOR_LOW;
	
	public int sampleMode = SAMPLEMODE_SINGLE;
	public int lightMode = LIGHTMODE_DIRECTIONAL;
	public int passMode = PASSMODE_FINAL;
	public int attenuationMode = ATTENUATIONMODE_POLYNOMIAL;
	public int falloffMode = FALLOFFMODE_FIXED;
	
	public boolean useQuadVS = false;
	public boolean includeTesslation = true;
	public boolean debugPS = false;
	
	public RenderVolumeDesc() {}
	
	public RenderVolumeDesc(RenderVolumeDesc o) {
		set(o);
	}
	
	public void set(RenderVolumeDesc o){
		meshMode = o.meshMode;
		shadowMapType = o.shadowMapType;
		cascadeCount = o.cascadeCount;
		volumeType = o.volumeType;
		maxtessfactor = o.maxtessfactor;
		sampleMode = o.sampleMode;
		lightMode = o.lightMode;
		passMode = o.passMode;
		attenuationMode = o.attenuationMode;
		falloffMode = o.falloffMode;
		
		useQuadVS = o.useQuadVS;
		includeTesslation = o.includeTesslation;
		debugPS = o.debugPS;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		
		if(!useQuadVS){
			result = prime * result + meshMode;
		}
		
		if(includeTesslation){
			result = prime * result + shadowMapType;
			result = prime * result + cascadeCount;
			result = prime * result + volumeType;
			result = prime * result + Float.floatToIntBits(maxtessfactor);
		}
		
		if(!debugPS){
			result = prime * result + attenuationMode;
			result = prime * result + falloffMode;
			result = prime * result + lightMode;
			result = prime * result + passMode;
			result = prime * result + sampleMode;
		}
		result = prime * result + (includeTesslation ? 1231 : 1237);
		result = prime * result + (useQuadVS ? 1231 : 1237);
		result = prime * result + (debugPS ? 1231 : 1237);
		
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
		RenderVolumeDesc other = (RenderVolumeDesc) obj;
		
		if (useQuadVS != other.useQuadVS)
			return false;
		if (includeTesslation != other.includeTesslation)
			return false;
		if (debugPS != other.debugPS)
			return false;
		
		if(includeTesslation){
			if (shadowMapType != other.shadowMapType)
				return false;
			
			if (cascadeCount != other.cascadeCount)
				return false;
			
			if (volumeType != other.volumeType)
				return false;
			
			if (Float.floatToIntBits(maxtessfactor) != Float.floatToIntBits(other.maxtessfactor))
				return false;
		}
		
		if(!debugPS){
			if (attenuationMode != other.attenuationMode)
				return false;
			if (falloffMode != other.falloffMode)
				return false;
			if (lightMode != other.lightMode)
				return false;
			if (passMode != other.passMode)
				return false;
			if (sampleMode != other.sampleMode)
				return false;
		}
		
		if (!useQuadVS && meshMode != other.meshMode)
			return false;
		return true;
	}
	
}
