package jet.opengl.demos.nvidia.waves.samples;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

/** The common parameters. */
/*public*/ class IsParameters {
    // Uniform variables
	final Matrix4f g_ModelViewProjectionMatrix = new Matrix4f();
	final Matrix4f g_LightModelViewProjectionMatrix = new Matrix4f();
	final Matrix4f g_ModelViewMatrix = new Matrix4f();
	final Matrix4f g_DepthModelViewProjectionMatrix =new Matrix4f();
	Matrix4f g_Projection;  // Camera projection
	final Vector3f g_CameraPosition = new Vector3f();
	final Vector3f g_CameraDirection = new Vector3f();
	final Vector3f  g_LightPosition = new Vector3f(-10000.0f,6500.0f,10000.0f);
	final Vector2f  g_ScreenSizeInv = new Vector2f();
	float	   g_ZNear;
	float	   g_ZFar;
	
	boolean g_FrustumCullInHS = true;
	float g_TerrainBeingRendered;
	 
	final Vector2f g_WaterBumpTexcoordShift = new Vector2f();
	float g_StaticTessFactor = 12.0f;
	float g_DynamicTessFactor = 50.0f;
	boolean g_UseDynamicLOD = true;
	int   g_SkipCausticsCalculation;
	boolean g_RenderCaustics = true;  // TODO default true
	float g_HalfSpaceCullSign;
	float g_HalfSpaceCullPosition;
	// Uniform variable End.
	
	boolean g_Wireframe = false;
	boolean g_showReflection = false;
	boolean g_RenderWater = true;

	final Matrix4f g_WorldToTopDownTextureMatrix = new Matrix4f();
	float g_Time;
	float g_BaseGerstnerWavelength;
	float g_BaseGerstnerParallelness;
	float g_BaseGerstnerSpeed;
	float g_BaseGerstnerAmplitude;
	final Vector2f  g_WindDirection = new Vector2f();
	float g_GerstnerSteepness;
	int g_enableShoreEffects;
	int g_ApplyFog;
}
