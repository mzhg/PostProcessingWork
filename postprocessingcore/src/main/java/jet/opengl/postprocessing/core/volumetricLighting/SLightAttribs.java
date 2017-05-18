package jet.opengl.postprocessing.core.volumetricLighting;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

final class SLightAttribs{
	final Vector4f f4DirOnLight = new Vector4f();

	Vector4f f4LightColorAndIntensity;
	Vector4f f4LightWorldPos;            // For point and spot lights only
	final Vector4f f4AmbientLight = new Vector4f();
	final Vector4f f4CameraUVAndDepthInShadowMap = new Vector4f();
	final Vector4f f4LightScreenPos = new Vector4f();
	final Vector4f f4SpotLightAxisAndCosAngle = new Vector4f();     // For spot light only

	final Vector4f f4AttenuationFactors = new Vector4f();
	boolean bIsLightOnScreen;

	final Vector2f m_f2ShadowMapTexelSize = new Vector2f(0,0);
	int m_uiShadowMapResolution;
	int m_uiMinMaxShadowMapResolution = 0;

	//    Vector3f f3Dummy = new Vector3f();
	float f3DummyX,f3DummyY, f3DummyZ;

	final Matrix4f mWorldToLightProjSpaceT = new Matrix4f();
}
