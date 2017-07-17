package jet.opengl.demos.demos.nvidia.rain;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

/**
 * Created by mazhen'gui on 2017/7/1.
 */

final class RainParams {
    final Matrix4f g_mInverseProjection = new Matrix4f();

    final Matrix4f g_mWorld = new Matrix4f();
    final Matrix4f g_mWorldViewProj = new Matrix4f();
    final Matrix4f g_mWorldView = new Matrix4f();
    final Matrix4f g_mProjection = new Matrix4f();
    final Matrix4f g_mViewProjectionInverse = new Matrix4f();
    final Matrix4f g_mInvView = new Matrix4f();
    final Vector3f g_eyePos = new Vector3f();   //eye in world space
    final Vector3f g_lightPos = new Vector3f(10,10,0); //the directional light in world space
    float g_de;
    final Vector3f g_ViewSpaceLightVec = new Vector3f();
    final Vector3f g_ViewSpaceLightVec2 = new Vector3f();
    float g_DSVPointLight;
    float g_DSVPointLight2;
    float g_DSVPointLight3;
    final Vector3f g_VecPointLightEye = new Vector3f();
    final Vector3f g_VecPointLightEye2 = new Vector3f();
    final Vector3f g_VecPointLightEye3 = new Vector3f();
    boolean g_useSpotLight = true;
    float g_cosSpotlightAngle = 0.8f;
    final Vector3f g_SpotLightDir = new Vector3f(0,-1,0);
    float g_FrameRate;
    float g_timeCycle;
    float g_splashXDisplace;
    float g_splashYDisplace;

//changesOften
    float g_ResponseDirLight = 1.0f;
    float g_ResponsePointLight = 1.0f;
    float dirLightIntensity = 1.0f;
    boolean renderBg = false;
    boolean moveParticles = false;
    final Vector3f g_TotalVel = new Vector3f(0,-0.25f,0);
    final Vector4f g_DiffuseColor = new Vector4f();
    float g_PointLightIntensity = 2.0f;
    float g_SpriteSize = 1.0f;
    final Vector3f g_beta = new Vector3f(0.04f,0.04f,0.04f);
    float g_BgAirLight = 0.0f;
    float g_Kd = 0.1f;
    float g_KsPoint = 20;
    float g_KsDir = 10;
    float g_specPower = 20;


    float g_ScreenWidth = 640.0f;
    float g_ScreenHeight = 480.0f;
    float g_ScreenWidthMultiplier =  0.0031299f;
    float g_ScreenHeightMultiplier = 0.0041754f;
    float g_heightMin = 0.0f;
    float g_radiusMin = 1.0f;
    float g_heightRange = 30.0f;
    float g_radiusRange = 30.0f;
    float maxHeight;
    float g_Near;
    float g_Far;
}
