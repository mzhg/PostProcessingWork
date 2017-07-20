#include "CloudsCommon.glsl"

layout(binding = 0) uniform sampler2D g_tex2DDepthBuffer;
layout(binding = 1) uniform sampler2D g_tex2DOccludedNetDensityToAtmTop;

in float4 m_f4UVAndScreenPos;
layout(location = 0) out float  Out_fTransparency;
layout(location = 1) out float2 Out_f2MinMaxZRange;
#if !LIGHT_SPACE_PASS
layout(location = 2) out float4 Out_f4Color;
#endif

// This function performs bilateral upscaling of the cloud color, transparency and distance buffers
void FilterDownscaledCloudBuffers(in float2 f2UV,
                                  in float fDistToCamera,
                                  out float fCloudTransparency,
                                  out float3 f3CloudsColor,
                                  out float fDistToCloud)
{
    fCloudTransparency = 1;
    f3CloudsColor = float3(0);
    fDistToCloud = +FLT_MAX;

    float2 f2CloudBufferSize = float2(g_GlobalCloudAttribs.fDownscaledBackBufferWidth, g_GlobalCloudAttribs.fDownscaledBackBufferHeight);
    // Get location of the left bottom source texel
    float2 f2SrcIJ = f2UV * f2CloudBufferSize - 0.5;
    float2 f2SrcIJ0 = floor(f2SrcIJ);
    float2 f2UVWeights = f2SrcIJ - f2SrcIJ0;
    // Compute UV coordinates of the gather location
    float2 f2GatherUV = (f2SrcIJ0+1) / f2CloudBufferSize;

    // The values in float4, which Gather() returns are arranged as follows:
    //   _______ _______
    //  |       |       |
    //  |   x   |   y   |
    //  |_______o_______|  o gather location
    //  |       |       |
    //  |   w   |   z   |
    //  |_______|_______|

    // Read data from the source buffers
    float4 f4SrcDistToCloud = textureGather( g_tex2DScrSpaceCloudMinMaxDist, f2GatherUV );   // samLinearClamp
    float4 f4SrcCloudTransp = textureGather( g_tex2DScrSpaceCloudTransparency, f2GatherUV );
    float4 f4CloudColorR = textureGather  ( g_tex2DScrSpaceCloudColor, f2GatherUV );
    float4 f4CloudColorG = textureGather( g_tex2DScrSpaceCloudColor, f2GatherUV, 1 );
    float4 f4CloudColorB = textureGather ( g_tex2DScrSpaceCloudColor, f2GatherUV, 2 );

    // Compute bilateral weights, start by bilinear:
    float4 f4BilateralWeights = float4(1 - f2UVWeights.x, f2UVWeights.x,   f2UVWeights.x, 1-f2UVWeights.x) *
                                float4(    f2UVWeights.y, f2UVWeights.y, 1-f2UVWeights.y, 1-f2UVWeights.y);

    // Take into account only these source texels, which are closer to the camera than the opaque surface
    //
    //              . .
    //   ---------->.'.      /\
    //   ----->/\   .'.     /  \
    //   -----/--\->.'.    /    \
    //       /    \       /      \
    //
    // To assure smooth filtering at cloud boundaries especially when cloud is in front of opaque surfaces,
    // we also need to account for transparent pixels (identified by dist to clouds == +FLT_MAX)
    //
    //       Finite +FLT_MAX
    //        dist
    //         |     |
    //         |     |
    //      ...V...  |
    //     ''''''''' |
    //               |
    //   ____________|___
    //               |
    //               V
    //
    f4BilateralWeights.xyzw *= float4( lessThan(f4SrcDistToCloud.xyzw, float4(fDistToCamera)) || equal(f4SrcDistToCloud.xyzw, float4(+FLT_MAX)) );

    float fSumWeight = dot(f4BilateralWeights.xyzw, float4(1));

    if( fSumWeight > 1e-2 )
    {
        f4BilateralWeights /= fSumWeight;

        fCloudTransparency = dot(f4SrcCloudTransp, f4BilateralWeights);

        f3CloudsColor.r = dot(f4CloudColorR, f4BilateralWeights);
        f3CloudsColor.g = dot(f4CloudColorG, f4BilateralWeights);
        f3CloudsColor.b = dot(f4CloudColorB, f4BilateralWeights);

        // Use minimum distance to avoid filtering FLT_MAX
        fDistToCloud = min(f4SrcDistToCloud.x, f4SrcDistToCloud.y);
        fDistToCloud = min(fDistToCloud, f4SrcDistToCloud.z);
        fDistToCloud = min(fDistToCloud, f4SrcDistToCloud.w);
    }
}


// This shader renders flat clouds by sampling cloud density at intersection
// of the view ray with the cloud layer
// If particles are rendred in lower resolution, it also upscales the
// downscaled buffers and combines with the result

//void RenderFlatCloudsPS(SScreenSizeQuadVSOutput In,
//                          out float fTransparency   : SV_Target0,
//                          out float2 f2MinMaxZRange : SV_Target1
//                          #if !LIGHT_SPACE_PASS
//                            , out float4 f4Color : SV_Target2
//                          #endif
//                          )
void main()
{
    // Load depth from the depth buffer
    float fDepth;
    float3 f3RayStart;
    float2 f2UV = m_f4UVAndScreenPos.xy; // ProjToUV(In.m_f2PosPS.xy);

#if DEBUG_STATIC_SCENE
    float2 f2ProjUV =  float2(m_f4UVAndScreenPos.z, -m_f4UVAndScreenPos.w);
#else
    float2 f2ProjUV =  m_f4UVAndScreenPos.zw;
#endif

#if LIGHT_SPACE_PASS
    fDepth = textureLod(g_tex2DLightSpaceDepthMap_t0, float3(f2UV,g_GlobalCloudAttribs.f4Parameter.x), 0 ).x;   // samLinearClamp
    fDepth = 0.0;
    // For directional light source, we should use position on the near clip plane instead of
    // camera location as a ray start point (use 1.01 to avoid issues when depth == 1)

    #if DEBUG_STATIC_SCENE
    float4 f4PosOnNearClipPlaneWS = mul( float4(f2ProjUV,1.01,1),   g_ViewProjInv );
    #else
    float4 f4PosOnNearClipPlaneWS = mul( float4(f2ProjUV,-0.999,1), g_ViewProjInv );
    #endif
    f3RayStart = f4PosOnNearClipPlaneWS.xyz/f4PosOnNearClipPlaneWS.w;
#else
    f3RayStart = g_f4CameraPos.xyz;
    #if DEBUG_STATIC_SCENE
    fDepth = texelFetch(g_tex2DDepthBuffer, int2(gl_FragCoord), 0).x;
    #else
    fDepth = texelFetch(g_tex2DDepthBuffer, int2(gl_FragCoord), 0).x;
    fDepth = 2.0 * fDepth - 1.0;
    #endif
#endif

    // Reconstruct world space position
    float4 f4ReconstructedPosWS = mul( float4(f2ProjUV,fDepth,1), g_ViewProjInv );
    float3 f3WorldPos = f4ReconstructedPosWS.xyz / f4ReconstructedPosWS.w;

    // Compute view ray
    float3 f3ViewDir = f3WorldPos - f3RayStart;
    float fDistToCamera = length(f3ViewDir);
    f3ViewDir /= fDistToCamera;
    float fRayLength = fDistToCamera;

    float fTime = g_fTimeScale*g_GlobalCloudAttribs.fTime;

    // Handle the case when the ray does not hit any surface
    // When rendering from light, we do not need to trace the ray
    // further than the far clipping plane because there is nothing
    // visible there
#if !LIGHT_SPACE_PASS
    #if DEBUG_STATIC_SCENE
    if( fDepth < 1e-10 )
    #else
    if( fDepth > 1.0 - (1e-10))
    #endif
        fRayLength = + FLT_MAX;
#endif

    // Compute intersection of the view ray with the Earth and the spherical cloud layer
    float3 f3EarthCentre = float3(0, -g_fEarthRadius, 0);
    float4 f4CloudLayerAndEarthIsecs;
    GetRaySphereIntersection2(f3RayStart.xyz,
                              f3ViewDir,
                              f3EarthCentre,
                              float2(g_fEarthRadius, g_fEarthRadius + g_GlobalCloudAttribs.fCloudAltitude),
                              f4CloudLayerAndEarthIsecs);
    float2 f2EarthIsecs = f4CloudLayerAndEarthIsecs.xy;
    float2 f2CloudLayerIsecs = f4CloudLayerAndEarthIsecs.zw;

    bool bIsValid = true;
    // Check if the view ray does not hit the cloud layer
    if( f2CloudLayerIsecs.y < f2CloudLayerIsecs.x )
        bIsValid = false;

    float fFadeOutFactor = 1;
#if LIGHT_SPACE_PASS
    // For light space pass, always use the first intersection with the cloud layer
    float fDistToCloudLayer = f2CloudLayerIsecs.x;
    if(f2EarthIsecs.x < f2EarthIsecs.y)
        fRayLength = min(fRayLength, f2EarthIsecs.x);
#else
    // For the camera space pass, select either first or second intersection
    // If the camera is under the cloud layer, use second intersection
    // If the camera is above the cloud layer, use first intersection
    float fDistToCloudLayer = f2CloudLayerIsecs.x > 0.0 ? f2CloudLayerIsecs.x : f2CloudLayerIsecs.y;

    // Region [fParticleCutOffDist - fFadeOutDistance, fParticleCutOffDist] servers as transition
    // from particles to flat clouds
    float fFadeOutDistance = g_GlobalCloudAttribs.fParticleCutOffDist * g_fParticleToFlatMorphRatio;
    float fFadeOutStartDistance = g_GlobalCloudAttribs.fParticleCutOffDist - fFadeOutDistance;

    if( fDistToCloudLayer < fFadeOutStartDistance )
        bIsValid = false;

    // Compute fade out factor
    fFadeOutFactor = saturate( (fDistToCloudLayer - fFadeOutStartDistance) /  max(fFadeOutDistance,1) );
#endif

    if( fDistToCloudLayer > fRayLength  )
        bIsValid = false;

#if LIGHT_SPACE_PASS || BACK_BUFFER_DOWNSCALE_FACTOR == 1
    if( !bIsValid) discard;
#endif

    float fTotalMass = 0;
    float3 f3CloudLayerIsecPos = float3(0);
    float fDensity = 0;
    float fCloudPathLen = 0;
    if( bIsValid)
    {
        // Get intersection point
        f3CloudLayerIsecPos = f3RayStart.xyz + f3ViewDir * fDistToCloudLayer;

        // Get cloud density at intersection point
        float4 f4UV01 = GetCloudDensityUV(f3CloudLayerIsecPos, fTime);  // TODO need to check f4UV01
        fDensity = GetCloudDensityAutoLOD(f4UV01)* fFadeOutFactor;
        // Fade out clouds when view angle is orthogonal to zenith
        float3 f3ZenithDir = normalize(f3CloudLayerIsecPos - f3EarthCentre);
        float fCosZenithAngle = dot(f3ViewDir, f3ZenithDir);
        fDensity *= abs(fCosZenithAngle)*2;

        fCloudPathLen = g_GlobalCloudAttribs.fCloudThickness;
        fTotalMass = fCloudPathLen * g_GlobalCloudAttribs.fCloudVolumeDensity * fDensity;

        // This helps improve perofrmance by reducing memory bandwidth
        if(fTotalMass < 1e-5)
            bIsValid = false;
    }

#if LIGHT_SPACE_PASS || BACK_BUFFER_DOWNSCALE_FACTOR == 1
    if(!bIsValid) discard;
#endif

    Out_fTransparency = exp(-g_fCloudExtinctionCoeff*fTotalMass);

#if LIGHT_SPACE_PASS
    // Transform intersection point into light view space
    float4 f4LightSpacePosPS = mul( float4(f3CloudLayerIsecPos,1), g_WorldViewProj );

    #if DEBUG_STATIC_SCENE
    Out_f2MinMaxZRange = float2(f4LightSpacePosPS.z / f4LightSpacePosPS.w);
//    Out_f2MinMaxZRange = float4(f4LightSpacePosPS.z / f4LightSpacePosPS.w, exp(-g_fCloudExtinctionCoeff*fTotalMass),fTotalMass, fDensity);
//    Out_f2MinMaxZRange = float4(f3CloudLayerIsecPos, fTime);
//    Out_f2MinMaxZRange = float2(g_fCloudExtinctionCoeff, fTotalMass);
//    Out_f2MinMaxZRange = float4(float(bIsValid), fTotalMass, 0,0);
    #else
    Out_f2MinMaxZRange = float2(f4LightSpacePosPS.z / f4LightSpacePosPS.w);
    Out_f2MinMaxZRange = 2.0 * Out_f2MinMaxZRange - 1.0; // remap[-1,1] to [0,1]
    #endif
#else
    Out_f4Color = float4(0);
    if( bIsValid )
    {
        float3 f3SunLightExtinction, f3AmbientLight;
        GetSunLightExtinctionAndSkyLight(f3CloudLayerIsecPos, f3SunLightExtinction, f3AmbientLight, g_tex2DOccludedNetDensityToAtmTop, g_tex2DAmbientSkylight);
        Out_f4Color.rgb = fDensity * fCloudPathLen * g_GlobalCloudAttribs.fCloudVolumeDensity * f3SunLightExtinction;
    }
    else
        fDistToCloudLayer = +FLT_MAX;

#   if BACK_BUFFER_DOWNSCALE_FACTOR > 1
        // Upscale buffers
        float fParticleDistToCloud = 0;
        float3 f3ParticleColor = float3(0);
        float fParticleTransparency = 0;
        FilterDownscaledCloudBuffers( f2UV,
                                      fDistToCamera,
                                      fParticleTransparency,
                                      f3ParticleColor,
                                      fParticleDistToCloud);
        // Combine with flat clouds
        Out_f4Color.rgb = Out_f4Color.rgb * fParticleTransparency + f3ParticleColor;
        fDistToCloudLayer = min(fDistToCloudLayer, fParticleDistToCloud);
        Out_fTransparency *= fParticleTransparency;
        // Save bandwidth by not outputting fully transparent clouds
        if( Out_fTransparency > 1 - 1e-5 )
            discard;
#   endif

    Out_f4Color.a = Out_fTransparency;
    Out_f2MinMaxZRange.xy = float2(fDistToCloudLayer);
#endif
}
