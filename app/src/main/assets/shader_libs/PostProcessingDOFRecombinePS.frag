#include "PostProcessingCommonPS.frag"

uniform sampler2D g_Input0;  // Scene Color Texture
uniform sampler2D g_Input1;  // Far blurred Texture
uniform sampler2D g_Input2;  // Near blurred Texture
uniform sampler2D g_DepthTex; // Scene Depth Texture

uniform vec4 g_Uniforms[2];

#define g_NearPlane                 g_Uniforms[1].x
#define g_FarPlane                  g_Uniforms[1].y

#define g_DepthOfFieldFocalDistance g_Uniforms[0].x
#define g_DepthOfFieldFocalRegion   g_Uniforms[0].y
#define g_DepthOfFieldNearTransitionRegion g_Uniforms[0].z
#define g_DepthOfFieldFarTransitionRegion  g_Uniforms[0].w

float ComputeDOFFarFocalMask(float SceneDepth)
{
	float FarFocalPlane = g_DepthOfFieldFocalDistance + g_DepthOfFieldFocalRegion;

	return saturate((SceneDepth - FarFocalPlane) / g_DepthOfFieldFarTransitionRegion);
}

float ComputeDOFNearFocalMask(float SceneDepth)
{
	float NearFocalPlane = g_DepthOfFieldFocalDistance;

	return saturate((NearFocalPlane - SceneDepth) / g_DepthOfFieldNearTransitionRegion);
}

float lerpDepth(float depth)
{
	return -g_FarPlane * g_NearPlane / (depth * (g_FarPlane - g_NearPlane) - g_FarPlane);
}

float CalcSceneDepth(vec2 UV)
{
	float DeviceZ = texture(g_DepthTex, UV).x;
	return lerpDepth(DeviceZ);
}

void main()
{
	vec4 SvPosition = gl_FragCoord;
	// SceneColor in full res
	vec2 PixelPosCenter = SvPosition.xy;

#if 1
	vec2 texelSize0 = 1.0/textureSize(g_Input0, 0);
#else
    vec2 texelSize0 = g_Uniforms[1].zw;
#endif
	vec2 FullResUV = PixelPosCenter * texelSize0;

	// DOF in half res
//	vec2 ViewportUV = FullResUV * vec2(1, DepthOfFieldParams[1].z);// - 0.5 * PostprocessInput1Size.zw;
//	vec2 ViewportUV = (PixelPos * 0.5f + 0.5f) * PostprocessInput1Size.zw;
	vec2 ViewportUV = m_f4UVAndScreenPos.xy;

#if 0
	// Clamp UV to avoid pulling bad data.
	ViewportUV.x = clamp(ViewportUV.x, g_DepthOfFieldUVLimit.x, g_DepthOfFieldUVLimit.z);
	ViewportUV.y = clamp(ViewportUV.y, g_DepthOfFieldUVLimit.y, g_DepthOfFieldUVLimit.w);
#endif

#if MOBILE_SHADING
	vec4 SceneColorAndDepth = texture(g_Input0, FullResUV);
#else
	vec4 SceneColorAndDepth = vec4(texture(g_Input0, FullResUV).rgb, CalcSceneDepth(FullResUV));
#endif

	vec3 UnfocusedSceneColor = SceneColorAndDepth.rgb;

	// I'm presuming all that matters here is the W==0 bit to mask out this value
	// TODO: Should check that compiler is doing a good job of removing the usages of this
	// from the rest of the code. It has no reason not to be able to do so...

	vec4 DOFAccumLayer1 = vec4(0,0,0,0);
	vec4 DOFAccumLayer3 = vec4(0,0,0,0);

#if FAR_BLUR
	DOFAccumLayer1 = texture(g_Input1, ViewportUV);
#endif

#if NEAR_BLUR
	DOFAccumLayer3 = texture(g_Input2, ViewportUV);
#endif

	float Layer1Mask = DOFAccumLayer1.a;
	float Layer2Mask = 1.0f - ComputeDOFFarFocalMask(SceneColorAndDepth.a);
//	float Layer2Mask = 1.0f - DOFAccumLayer1.a;
	float Layer3Mask = DOFAccumLayer3.a;
	float PerPixelNearMask = ComputeDOFNearFocalMask(SceneColorAndDepth.a);
#if MOBILE_SHADING
	vec2 DoFMasks = DecodeDOFFocalMask(SceneColorAndDepth.a);
	Layer2Mask = 1.0-DoFMasks.x;
#endif

	// 3 layers
	float Div0Bias = 0.0001f;

	// RGB color, A how much the full resolution showes through
	vec3 LayerMerger = vec3(0);

	// Layer 1: half res background
	LayerMerger = (UnfocusedSceneColor * Div0Bias + DOFAccumLayer1.rgb) / (DOFAccumLayer1.a + Div0Bias);

	// Needed to cope with the skybox not being blurred, the tweak value
	// avoids having a discontinuity between blurry far objects and the skybox
	// and is choosen to not produce too much blobby looking out of focus rendering.
	float Blend = DOFAccumLayer1.a;
	// Magic function to transform alpha into smooth blend function against in-focus skybox.
	Blend = sqrt(Blend);
	Blend = sqrt(Blend);
	Blend = Blend * Blend * (3.0 - 2.0 * Blend);
	LayerMerger = lerp(UnfocusedSceneColor, LayerMerger, Blend);

	// Layer 2: then we add the focused scene to fill the empty areas
	float Smash = 0.25;
	Layer2Mask = saturate((Layer2Mask - (1.0 - Smash)) * rcp(Smash));
	Layer2Mask *= Layer2Mask;
//	LayerMerger = lerp(LayerMerger, SceneColorAndDepth.rgb, Layer2Mask * (1 - PerPixelNearMask));
	LayerMerger = lerp(LayerMerger, SceneColorAndDepth.rgb, Layer2Mask);

#if SEPARATE_TRANSLUCENCY
	{
		//@todo - use UpsampleSeparateTranslucency
		vec4 SeparateTranslucency = Texture2DSample(PostprocessInput3, PostprocessInput3Sampler, FullResUV);

		// add RGB, darken by A (this allows to represent translucent and additive blending)
		LayerMerger.rgb = LayerMerger.rgb * SeparateTranslucency.a + SeparateTranslucency.rgb;
	}
#endif

	vec3 FrontLayer = (UnfocusedSceneColor * Div0Bias + DOFAccumLayer3.rgb) / (DOFAccumLayer3.a + Div0Bias);

	// Layer 3: on top of that blend the front half res layer
	LayerMerger = lerp(LayerMerger, FrontLayer, saturate(Layer3Mask * 5));

	Out_f4Color.rgb = LayerMerger.rgb;
	Out_f4Color.a = 0;
}