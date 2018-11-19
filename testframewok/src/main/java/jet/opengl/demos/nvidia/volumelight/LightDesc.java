package jet.opengl.demos.nvidia.volumelight;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import jet.opengl.postprocessing.core.volumetricLighting.LightType;

/** Light Source Description */
public class LightDesc {

	/** Type of light source */
	public LightType eType;
	/** Light clip-space to world-space transform */
	public final Matrix4f mLightToWorld = new Matrix4f();
	/** Color of light */
	public final Vector3f vIntensity = new Vector3f();
	
	/**
	 * LightType = Directional or LightType = Spotlight<p>
	 * Normalized light direction
	 */
	public final Vector3f vDirection = new Vector3f();
	
	/**
	 * LightType = Omni or LightType = Spotlight<p>
	 * Light position in world-space. 
	 */
	public final Vector3f vPosition = new Vector3f();
	
	/**
	 * LightType = Omni or LightType = Spotlight<p>
	 * World-space distance to near view plane. 
	 */
	public float fZNear;
	/**
	 * LightType = Omni or LightType = Spotlight<p>
	 * World-space distance to far view plane. 
	 */
	public float fZFar;
	
	/**LightType = Spotlight<p> Equation to use for angular falloff */
	public SpotlightFalloffMode eFalloffMode;
	
	/**LightType = Spotlight<p> Spotlight falloff angle */
	public float fFalloff_CosTheta;
	
	/**LightType = Spotlight<p> Spotlight power */
	public float fFalloff_Power;
	
	/**
	 * LightType = Omni or LightType = Spotlight<p>
	 * Light falloff equation
	 */
	public AttenuationMode eAttenuationMode;
	
	/**
	 * LightType = Omni or LightType = Spotlight<p>
	 * Factors in the attenuation equation
	 */
	public final float[] fAttenuationFactors = new float[4];
}
