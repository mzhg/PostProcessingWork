
#include "PostProcessingLightScatteringCommon.frag"

layout(location = 0) out float3 OutColor;
in vec4 m_f4UVAndScreenPos;
void main()
{
	float2 f2UV = m_f4UVAndScreenPos.xy;  //ProjToUV(In.m_f2PosPS);

    // We need to manually perform bilateral filtering of the downscaled scattered radiance texture to
    // eliminate artifacts at depth discontinuities
    float2 f2DownscaledInsctrTexDim;
//    g_tex2DDownscaledInsctrRadiance.GetDimensions(f2DownscaledInsctrTexDim.x, f2DownscaledInsctrTexDim.y);
    f2DownscaledInsctrTexDim = float2(textureSize(g_tex2DDownscaledInsctrRadiance));
    // Offset by 0.5 is essential, because texel centers have UV coordinates that are offset by half the texel size
    float2 f2UVScaled = f2UV.xy * f2DownscaledInsctrTexDim.xy - float2(0.5, 0.5);
    float2 f2LeftBottomSrcTexelUV = floor(f2UVScaled);
    // Get bilinear filtering weights
    float2 f2BilinearWeights = f2UVScaled - f2LeftBottomSrcTexelUV;
    // Get texture coordinates of the left bottom source texel. Again, offset by 0.5 is essential
    // to align with texel center
    f2LeftBottomSrcTexelUV = (f2LeftBottomSrcTexelUV + float2(0.5, 0.5)) / f2DownscaledInsctrTexDim.xy;

    // Load camera space Z values corresponding to locations of the source texels in g_tex2DDownscaledInsctrRadiance texture
    // We must arrange the data in the same manner as Gather() does:
    float4 f4SrcLocationsCamSpaceZ;
    f4SrcLocationsCamSpaceZ.x = GetCamSpaceZ( f2LeftBottomSrcTexelUV + float2(0,1) / f2DownscaledInsctrTexDim.xy );
    f4SrcLocationsCamSpaceZ.y = GetCamSpaceZ( f2LeftBottomSrcTexelUV + float2(1,1) / f2DownscaledInsctrTexDim.xy );
    f4SrcLocationsCamSpaceZ.z = GetCamSpaceZ( f2LeftBottomSrcTexelUV + float2(1,0) / f2DownscaledInsctrTexDim.xy );
    f4SrcLocationsCamSpaceZ.w = GetCamSpaceZ( f2LeftBottomSrcTexelUV + float2(0,0) / f2DownscaledInsctrTexDim.xy );

    // Get camera space z of the current screen pixel
    float fCamSpaceZ = GetCamSpaceZ( f2UV );

    float3 f3InsctrIntegral = PerformBilateralInterpolation(f2BilinearWeights, f2LeftBottomSrcTexelUV, f4SrcLocationsCamSpaceZ, fCamSpaceZ, g_tex2DDownscaledInsctrRadiance, f2DownscaledInsctrTexDim/*, samLinearClamp*/);
    
    float3 f3ReconstructedPosWS = ProjSpaceXYZToWorldSpace( float3(m_f4UVAndScreenPos.zw,fCamSpaceZ) );
    float3 f3EyeVector = f3ReconstructedPosWS.xyz - g_f4CameraPos.xyz;
    float fDistToCamera = length(f3EyeVector);

#if LIGHT_TYPE == LIGHT_TYPE_DIRECTIONAL
    f3EyeVector /= fDistToCamera;
    float3 f3ScatteredLight = ApplyPhaseFunction(f3InsctrIntegral, dot(f3EyeVector, g_f4DirOnLight.xyz));
#else
    float3 f3ScatteredLight = f3InsctrIntegral;
#endif

    float3 f3BackgroundColor = GetAttenuatedBackgroundColor(In, fDistToCamera);

	OutColor = ToneMap(f3BackgroundColor + f3ScatteredLight);
}