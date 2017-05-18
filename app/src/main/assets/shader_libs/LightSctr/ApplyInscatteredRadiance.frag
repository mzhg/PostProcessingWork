#include "PostProcessingLightScatteringCommon.frag"

in float4 UVAndScreenPos;

// The scene correction just work for the static scene test.
#if CORRECT_STATIC_SCENE == 0
//layout(origin_upper_left) in vec4 gl_FragCoord;
#endif

layout(location = 0) out float4 OutColor;

void main()
{
	float2 f2PosPS = m_f4UVAndScreenPos.zw;
	
#if CORRECT_STATIC_SCENE == 1
	// remap the lower-left corner to upper_left corner.
	f2PosPS.y = -f2PosPS.y;
#endif
	
	float fCamSpaceZ = GetCamSpaceZ( ProjToUV(f2PosPS));
    float3 f3InsctrIntegral = UnwarpEpipolarInsctrImage(f2PosPS, fCamSpaceZ);

    float3 f3ReconstructedPosWS = ProjSpaceXYZToWorldSpace(float3(f2PosPS, fCamSpaceZ));
    float3 f3EyeVector = f3ReconstructedPosWS.xyz - g_f4CameraPos.xyz;
    float fDistToCamera = length(f3EyeVector);
#if LIGHT_TYPE == LIGHT_TYPE_DIRECTIONAL
    f3EyeVector /= fDistToCamera;
    float3 f3InsctrColor = ApplyPhaseFunction(f3InsctrIntegral, dot(f3EyeVector, g_f4DirOnLight.xyz));
#else
    float3 f3InsctrColor = f3InsctrIntegral;
#endif

    float3 f3BackgroundColor = GetAttenuatedBackgroundColor(gl_FragCoord.xy, fDistToCamera);
	OutColor.rgb = ToneMap(f3BackgroundColor + f3InsctrColor);
	OutColor.a = 0.0;
}