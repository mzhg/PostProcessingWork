package jet.opengl.demos.nvidia.waves.ocean;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.TextureCube;

final class OceanSurfaceParams implements TechniqueParams {

    float		g_ZNear = 1.0f;
    float		g_ZFar = 20000.0f;


    Matrix4f    g_matViewProj;
    Matrix4f	g_matView;
    Matrix4f	g_matWorldToShip;

    Vector4f    g_ViewRight;
    Vector4f    g_ViewUp;
    final Vector3f    g_ViewForward = new Vector3f();

    Vector4f		g_GustUV;

    final Vector4f		g_ScreenSizeInv = new Vector4f();
    Vector3f		g_SkyColor;
    Vector4f		g_DeepColor;
    Vector3f		g_BendParam /*= {0.1f, -0.4f, 0.2f}*/;

    Vector3f		g_LightningPosition;
    Vector3f		g_LightningColor;
    Matrix4f	g_matSceneToShadowMap;

    Vector3f		g_LightDir;
    Vector3f		g_LightColor;
    Vector3f      g_WaterDeepColor/*={0.0,0.04,0.09}*/;
    Vector3f      g_WaterScatterColor/*={0.0,0.05,0.025}*/;
    float		g_WaterSpecularIntensity = 0.4f;
    float       g_WaterSpecularPower=100.0f;
    float       g_WaterLightningSpecularPower=20.0f;

    float		g_ShowSpraySim=0.0f;
    float		g_ShowFoamSim=0.0f;

    int			g_LightsNum;
    Vector4f[]		g_SpotlightPosition/*[MaxNumSpotlights]*/;
    Vector4f[]		g_SpotLightAxisAndCosAngle/*[MaxNumSpotlights]*/;
    Vector4f[]		g_SpotlightColor/*[MaxNumSpotlights]*/;

    Vector3f		foam_underwater_color /*= {0.81f, 0.90f, 1.0f}*/;

    float		g_GlobalFoamFade;

    Vector4f[]		g_HullProfileCoordOffsetAndScale/*[MaxNumVessels]*/;
    Vector4f[]		g_HullProfileHeightOffsetAndHeightScaleAndTexelSize/*[MaxNumVessels]*/;

    float		g_CubeBlend;

    final Vector2f g_SkyCube0RotateSinCos = new Vector2f();
    final Vector2f		g_SkyCube1RotateSinCos = new Vector2f();

    Vector3f		g_SkyCubeMult;

    float		g_FogExponent;

    float       g_TimeStep = 0.1f;

    float		g_CloudFactor;

    boolean		g_bGustsEnabled;
    boolean		g_bWakeEnabled;

//#if ENABLE_SHADOWS
    Matrix4f[]    g_SpotlightMatrix/*[MaxNumSpotlights]*/;
    Texture2D[]  g_SpotlightResource/*[MaxNumSpotlights]*/;
//#endif

    /*float3 rotateXY(float3 xyz, float2 sc)
    {
        float3 result = xyz;
        float s = sc.x;
        float c = sc.y;
        result.x = xyz.x * c - xyz.y * s;
        result.y = xyz.x * s + xyz.y * c;
        return result;
    }

#define FRESNEL_TERM_SUPERSAMPLES_RADIUS 1
            #define FRESNEL_TERM_SUPERSAMPLES_INTERVALS (1 + 2*FRESNEL_TERM_SUPERSAMPLES_RADIUS)*/

    //-----------------------------------------------------------------------------------
// Texture & Samplers
//-----------------------------------------------------------------------------------
    Texture2D	g_texColorMap;
    Texture2D	g_texBufferMap;
    TextureCube g_texCubeMap0;
    TextureCube	g_texCubeMap1;
    Texture2D	g_texFoamIntensityMap;
    Texture2D	g_texFoamDiffuseMap;
    Texture2D[]	g_texHullProfileMap/*[MaxNumVessels]*/;

    Texture2D	g_texWakeMap;
    Texture2D	g_texShipFoamMap;
    Texture2D	g_texGustMap;
    Texture2D	g_texLocalFoamMap;

    Texture2D	g_texReflection;
    Texture2D	g_texReflectionPos;

    /*struct SprayParticleData
    {
        float4 position;
        float4 velocity;
    };*/

    /*AppendStructuredBuffer<SprayParticleData>*/ BufferGL g_SprayParticleData /*: register(u1)*/;
    /*StructuredBuffer<SprayParticleData>*/BufferGL       g_SprayParticleDataSRV;

    Vector4f g_UVOffsetBlur;
    float g_FadeAmount;
    Texture2D g_texLocalFoamSource;
    final Vector4f g_PatchColor = new Vector4f();

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        return null;
    }
}
