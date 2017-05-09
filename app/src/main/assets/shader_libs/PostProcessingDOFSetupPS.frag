#include "PostProcessingCommonPS.frag"

// g_Uniforms[0].x : g_DepthOfFieldFocalDistance
// g_Uniforms[0].y : g_DepthOfFieldFocalRegion
// g_Uniforms[0].z : g_DepthOfFieldNearTransitionRegion
// g_Uniforms[0].w : g_DepthOfFieldFarTransitionRegion

// g_Uniforms[1].x : g_DepthOfFieldScale
// g_Uniforms[1].y : g_NearPlane
// g_Uniforms[1].z : g_FarPlane

// g_Uniforms[2].xy : The dimension of the g_Input0
// g_Uniforms[2].zw : The dimension of the g_Input1 (DepthTex)
uniform vec4 g_Uniforms[3];

#define g_DepthOfFieldFocalDistance g_Uniforms[0].x
#define g_DepthOfFieldFocalRegion   g_Uniforms[0].y
#define g_DepthOfFieldNearTransitionRegion g_Uniforms[0].z
#define g_DepthOfFieldFarTransitionRegion  g_Uniforms[0].w
#define g_DepthOfFieldScale         g_Uniforms[1].x

uniform sampler2D g_Input0;  // Scene Color Texture
uniform sampler2D g_Input1;  // Scene Depth Texture

#define g_NearPlane                 g_Uniforms[1].y
#define g_FarPlane                  g_Uniforms[1].z
uniform mat4  g_ScreenToWorld; // The inverse of the projection matrix.

#if MRT_COUNT > 1
layout(location =1) out vec4 Out_f4Color1;
#endif

float lerpDepth(float depth)
{
	return -g_FarPlane * g_NearPlane / (depth * (g_FarPlane - g_NearPlane) - g_FarPlane);
}

vec4 GatherSceneDepth(vec2 UV, vec2 texelSize)
{
	vec4 DeviceZ = textureGather(g_Input1, UV);
	return vec4
	(
		lerpDepth(DeviceZ.x),
		lerpDepth(DeviceZ.y),
		lerpDepth(DeviceZ.z),
		lerpDepth(DeviceZ.w)
	);
}

vec4 ReadFullResAndDepth(vec2 UV, in out float Depth)
{
	vec4 Color = texture(g_Input0, UV);
#if MOBILE_SHADING
	Depth = Color.a;
#endif
	Color.a = 1.0;
	return Color;
}

// todo move to central place
float ComputeDOFNearFocalMask(float SceneDepth)
{
	float NearFocalPlane = g_DepthOfFieldFocalDistance;

	return saturate((NearFocalPlane - SceneDepth) / g_DepthOfFieldNearTransitionRegion);
}

// todo move to central place
float ComputeDOFFarFocalMask(float SceneDepth)
{
	float FarFocalPlane = g_DepthOfFieldFocalDistance + g_DepthOfFieldFocalRegion;

	return saturate((SceneDepth - FarFocalPlane) / g_DepthOfFieldFarTransitionRegion);
}

// Calculate focal masks from ES2's PP CoC.
vec2 DecodeDOFFocalMask(float SceneDepth)
{
	vec2 Ret;
	SceneDepth = min(1.0, SceneDepth);
	if (SceneDepth > 0.5)
	{
		Ret.x = (SceneDepth - 0.5) * 2.0;
		Ret.y = 0;
	}
	else
	{
		Ret.y = 1.0 - (SceneDepth * 2.0);
		Ret.x = 0;
	}
	return Ret * g_DepthOfFieldScale;
}

// @return .x:far, .y:near
vec2 ComputeDOFFocalMask(float SceneDepth, float SkyWithoutHorizonMask)
{
	vec2 Ret = vec2(ComputeDOFFarFocalMask(SceneDepth), ComputeDOFNearFocalMask(SceneDepth));

#if 0
	float SkyFocusDistance = DepthOfFieldParams[0].x;

	// The skybox should not be faded out, expect in the horizon, this can be optimized
	if(SceneDepth > SkyFocusDistance)
	{
		Ret.x = lerp(Ret.x, 0,  SkyWithoutHorizonMask);
	}
#endif
#if MOBILE_SHADING
	Ret = DecodeDOFFocalMask(SceneDepth);
#endif

	return Ret;
}


void main()
{
    vec2 UV = m_f4UVAndScreenPos.xy;
#if 1
    vec2 texelSize0 = 1.0/textureSize(g_Input0, 0);
    vec2 texelSize1 = 1.0/textureSize(g_Input1, 0);
#else
    vec2 texelSize0 = g_Uniforms[2].xy;
    vec2 texelSize1 = g_Uniforms[2].zw;
#endif
    vec2 Offset = 0.5f * texelSize0;

    float MaskDistance = g_DepthOfFieldFocalDistance + g_DepthOfFieldFocalRegion * 0.5f;

    vec4 DepthQuad = GatherSceneDepth(UV, texelSize1);

    vec4 FarColor = vec4(0);
    vec4 NearColor = vec4(0);

    vec2 Mask;
    vec4 Sample;

    // for each sample of the full res input image
    // we compute the mask (front of back layer)
    // and put into MRT0 or MRT1

    // screen position in [-1, 1] screen space
    vec2 ScreenSpacePos = m_f4UVAndScreenPos.zw;

    // can be optimized, needed to not blur the skybox
//	float3 ScreenVector = normalize(mul(vec4(ScreenSpacePos, 1, 0), View.ScreenToWorld).xyz);
    // The position in camera world.
    vec3 ScreenVector = normalize((g_ScreenToWorld * vec4(ScreenSpacePos, 1, 0)).xyz);
    float SkyWithoutHorizonMask = saturate(-ScreenVector.z * 3.0f);

    // 0:see though..1:Near blurred
    float VignetteMask = 0.0;

#if DOF_VIGNETTE
    {
        float DepthOfFieldVignetteMul = DepthOfFieldParams[0].y;
        float DepthOfFieldVignetteAdd = DepthOfFieldParams[0].z;

        // todo: we could optimize that multiplication away
        float InvAspectRatio = View.ViewSizeAndInvSize.y * View.ViewSizeAndInvSize.z;

        float CenterDist = length(ScreenSpacePos * vec2(1, InvAspectRatio));

        // We prepare the constants on CPU side and use MAD (Multiply and Add) as this is faster
        VignetteMask = saturate(CenterDist * DepthOfFieldVignetteMul + DepthOfFieldVignetteAdd);
    }
#endif

    Sample = ReadFullResAndDepth(UV + Offset * vec2(-1, 1), DepthQuad.x);
    Mask = ComputeDOFFocalMask(DepthQuad.x, SkyWithoutHorizonMask);
    FarColor += Sample * Mask.x;
    NearColor += Sample * lerp(Mask.y, 1, VignetteMask);

    Sample = ReadFullResAndDepth(UV + Offset * vec2(1, 1), DepthQuad.y);
    Mask = ComputeDOFFocalMask(DepthQuad.y, SkyWithoutHorizonMask);
    FarColor += Sample * Mask.x;
    NearColor += Sample * lerp(Mask.y, 1, VignetteMask);

    Sample = ReadFullResAndDepth(UV + Offset * vec2(1, -1), DepthQuad.z);
    Mask = ComputeDOFFocalMask(DepthQuad.z, SkyWithoutHorizonMask);
    FarColor += Sample * Mask.x;
    NearColor += Sample * lerp(Mask.y, 1, VignetteMask);

    Sample = ReadFullResAndDepth(UV + Offset * vec2(-1, -1), DepthQuad.w);
    Mask = ComputeDOFFocalMask(DepthQuad.w, SkyWithoutHorizonMask);
    FarColor += Sample * Mask.x;
    NearColor += Sample * lerp(Mask.y, 1, VignetteMask);

    // we average 4 samples
    FarColor /= 4.0;
    NearColor /= 4.0;

#if MRT_COUNT > 1
    Out_f4Color = FarColor;
    Out_f4Color1 = NearColor;
#else
    #if NEAR_BLUR
        Out_f4Color = NearColor;
    #else
        Out_f4Color = FarColor;
    #endif
#endif
}