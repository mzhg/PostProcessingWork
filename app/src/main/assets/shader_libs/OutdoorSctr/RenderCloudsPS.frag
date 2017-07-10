#include "CloudsCommon.glsl"

// This helper function computes intersection of the view ray with the particle ellipsoid
void IntersectRayWithParticle(const in SParticleAttribs ParticleAttrs,
                              const in SCloudCellAttribs CellAttrs,
                              const in float3 f3CameraPos,
                              const in float3 f3ViewRay,
                              out float2 f2RayIsecs,
                              out float3 f3EntryPointUSSpace, // Entry point in Unit Sphere (US) space
                              out float3 f3ViewRayUSSpace,    // View ray direction in Unit Sphere (US) space
                              out float3 f3LightDirUSSpace,   // Light direction in Unit Sphere (US) space
                              out float fDistanceToEntryPoint,
                              out float fDistanceToExitPoint)
{
    // Construct local frame matrix
    float3 f3Normal    = CellAttrs.f3Normal.xyz;
    float3 f3Tangent   = CellAttrs.f3Tangent.xyz;
    float3 f3Bitangent = CellAttrs.f3Bitangent.xyz;
    float3x3 f3x3ObjToWorldSpaceRotation = float3x3(f3Tangent, f3Normal, f3Bitangent);
    // World to obj space is inverse of the obj to world space matrix, which is simply transpose
    // for orthogonal matrix:
    float3x3 f3x3WorldToObjSpaceRotation = transpose(f3x3ObjToWorldSpaceRotation);

    // Compute camera location and view direction in particle's object space:
    float3 f3CamPosObjSpace = f3CameraPos - ParticleAttrs.f3Pos;
    f3CamPosObjSpace = mul(f3CamPosObjSpace, f3x3WorldToObjSpaceRotation);
    float3 f3ViewRayObjSpace = mul(f3ViewRay, f3x3WorldToObjSpaceRotation );
    float3 f3LightDirObjSpce = mul(-g_LightAttribs.f4DirOnLight.xyz, f3x3WorldToObjSpaceRotation );

    // Compute scales to transform ellipsoid into the unit sphere:
    float3 f3Scale = 1.f / GetParticleScales(ParticleAttrs.fSize, CellAttrs.uiNumActiveLayers);

    float3 f3ScaledCamPosObjSpace;
    f3ScaledCamPosObjSpace  = f3CamPosObjSpace*f3Scale;
    f3ViewRayUSSpace = normalize(f3ViewRayObjSpace*f3Scale);
    f3LightDirUSSpace = normalize(f3LightDirObjSpce*f3Scale);
    // Scale camera pos and view dir in obj space and compute intersection with the unit sphere:
    GetRaySphereIntersection(f3ScaledCamPosObjSpace, f3ViewRayUSSpace, 0, 1.f, f2RayIsecs);

    f3EntryPointUSSpace = f3ScaledCamPosObjSpace + f3ViewRayUSSpace*f2RayIsecs.x;

    fDistanceToEntryPoint = length(f3ViewRayUSSpace/f3Scale) * f2RayIsecs.x;
    fDistanceToExitPoint  = length(f3ViewRayUSSpace/f3Scale) * f2RayIsecs.y;
}

void SwapLayers( inout SParticleLayer Layer0,
                 inout SParticleLayer Layer1 )
{
    SParticleLayer Tmp = Layer0;
    Layer0 = Layer1;
    Layer1 = Tmp;
}

void MergeParticleLayers(in SParticleLayer Layer0,
                         in SParticleLayer Layer1,
                         out SParticleLayer fMergedLayer,
                         out float3 f3ColorToCommit,
                         out float fTranspToCommit,
                         /*uniform*/ bool BackToFront)
{
    if( Layer0.f2MinMaxDist.x < Layer1.f2MinMaxDist.x )
    {
        SwapLayers( Layer0, Layer1 );
    }

    //
    //       Min1                 Max1
    //        [-------Layer 1------]
    //
    //                  [-------Layer 0------]
    //                 Min0                 Max0
    //
    //     --------------------------------------> Distance

    float fMinDist0 = Layer0.f2MinMaxDist.x;
    float fMaxDist0 = Layer0.f2MinMaxDist.y;
    float fMinDist1 = Layer1.f2MinMaxDist.x;
    float fMaxDist1 = Layer1.f2MinMaxDist.y;

    float fIsecLen = min(fMaxDist1, fMaxDist0) - fMinDist0;
    if( fIsecLen <= 0  )
    {
        if( BackToFront )
        {
            fTranspToCommit = exp( -Layer0.fOpticalMass  );
            f3ColorToCommit.rgb = Layer0.f3Color * (1 - fTranspToCommit);
            fMergedLayer = Layer1;
        }
        else
        {
            fTranspToCommit = exp( -Layer1.fOpticalMass );
            f3ColorToCommit.rgb = Layer1.f3Color * (1 - fTranspToCommit);
            fMergedLayer = Layer0;
        }
    }
    else
    {
        float fLayer0Ext = max(fMaxDist0 - fMinDist0, 1e-5);
        float fLayer1Ext = max(fMaxDist1 - fMinDist1, 1e-5);
        float fDensity0 = Layer0.fOpticalMass / fLayer0Ext;
        float fDensity1 = Layer1.fOpticalMass / fLayer1Ext;

        float fBackDist = fMaxDist0 - fMaxDist1;
        // fBackDist > 0  (fMaxDist0 > fMaxDist1)
        // ------------------------------------------------------> Distance
        //
        //       Min1                         Max1
        //        [-----------Layer 1----------]
        //
        //                        [-----------Layer 0-----------]
        //                       Min0                          Max0
        //
        //        |               |             |               |
        //             Front             Isec          Back
        //

        // fBackDist < 0 (fMaxDist0 < fMaxDist1)
        // ------------------------------------------------------> Distance
        //
        //       Min1                                                         Max1
        //        [-----------Layer 1------------------------------------------]
        //
        //                        [-----------Layer 0-----------]
        //                       Min0                          Max0
        //
        //        |               |                             |               |
        //             Front                    Isec                   Back
        //

        float fBackDensity = fBackDist > 0 ? fDensity0      : fDensity1;
        float3 f3BackColor = fBackDist > 0 ? Layer0.f3Color : Layer1.f3Color;
        fBackDist = fBackDist > 0 ? fBackDist : -fBackDist;
        float fBackTransparency = exp(-fBackDist*fBackDensity);
        f3BackColor = f3BackColor * (1 - fBackTransparency );

        float fIsecTransparency = exp( -(fDensity0 + fDensity1) * fIsecLen );
        float3 f3IsecColor = (Layer0.f3Color * fDensity0 + Layer1.f3Color * fDensity1)/max(fDensity0 + fDensity1, 1e-4) * (1 - fIsecTransparency);

        float fFrontDist = fMinDist0 - fMinDist1;
        float fFrontTransparency = exp( -fDensity1 * fFrontDist );
        float3 f3FrontColor = Layer1.f3Color * (1 - fFrontTransparency );

        float3 f3Color = float3(0);
        float fNetTransparency =  1;

        if( BackToFront )
        {
            f3ColorToCommit.rgb = f3BackColor;
            fTranspToCommit = fBackTransparency;

            float3 f3Color = f3FrontColor + fFrontTransparency * f3IsecColor;

            float fNetTransparency =  fIsecTransparency * fFrontTransparency;
            fMergedLayer.f3Color = f3Color / max(saturate(1 - fNetTransparency), 1e-10);

            fMergedLayer.f2MinMaxDist.x = fMinDist1;
            fMergedLayer.f2MinMaxDist.y = min(fMaxDist0, fMaxDist1);
            fMergedLayer.fOpticalMass = fDensity1 * fFrontDist + (fDensity1 + fDensity0) * fIsecLen;
        }
        else
        {
            //if( 1 || ForceMerge )
            //{
                f3ColorToCommit.rgb = float3(0);
                fTranspToCommit = 1;

                float3 f3Color = f3FrontColor + fFrontTransparency*(f3IsecColor + fIsecTransparency * f3BackColor);

                float fNetTransparency =  fFrontTransparency * fIsecTransparency * fBackTransparency;
                fMergedLayer.f3Color = f3Color / max(saturate(1 - fNetTransparency), 1e-10);

                fMergedLayer.f2MinMaxDist.x = fMinDist1;
                fMergedLayer.f2MinMaxDist.y = max(fMaxDist0, fMaxDist1);
                fMergedLayer.fOpticalMass = Layer0.fOpticalMass + Layer1.fOpticalMass;
            //}
            //else
            //{
            //    f3ColorToCommit.rgb = f3FrontColor;
            //    fTranspToCommit = fFrontTransparency;

            //    float3 f3Color = f3IsecColor + fIsecTransparency * f3BackColor;

            //    float fNetTransparency =  fIsecTransparency * fBackTransparency;
            //    fMergedLayer.f3Color = f3Color / max(saturate(1 - fNetTransparency), 1e-10);

            //    fMergedLayer.f2MinMaxDist.x = fMinDist0;
            //    fMergedLayer.f2MinMaxDist.y = max(fMaxDist0, fMaxDist1);
            //    fMergedLayer.fOpticalMass = fBackDensity * fBackDist + (fDensity1 + fDensity0) * fIsecLen;
            //}
        }
        // Do not output color
    }
}

float SampleCellAttribs3DTexture(sampler3D tex3DData, in float3 f3WorldPos, in uint uiRing, /*uniform*/ bool bAutoLOD )
{
    float3 f3EarthCentre = float3(0, -g_MediaParams.fEarthRadius, 0);
    float3 f3DirFromEarthCenter = f3WorldPos - f3EarthCentre;
    float fDistFromCenter = length(f3DirFromEarthCenter);
	//Reproject to y=0 plane
    float3 f3CellPosFlat = f3EarthCentre + f3DirFromEarthCenter / f3DirFromEarthCenter.y * g_MediaParams.fEarthRadius;
    float3 f3CellPosSphere = f3EarthCentre + f3DirFromEarthCenter * ((g_MediaParams.fEarthRadius + g_GlobalCloudAttribs.fCloudAltitude)/fDistFromCenter);
	float3 f3Normal = f3DirFromEarthCenter / fDistFromCenter;
	float fCloudAltitude = dot(f3WorldPos - f3CellPosSphere, f3Normal);

    // Compute cell center world space coordinates
    const float fRingWorldStep = GetCloudRingWorldStep(uiRing, g_GlobalCloudAttribs);

    //
    //
    //                                 Camera
    //                               |<----->|
    //   |   X   |   X   |   X   |   X   |   X   |   X   |   X   |   X   |       CameraI == 4
    //   0  0.5     1.5     2.5     3.5  4  4.5     5.5     6.5     7.5  8       uiRingDimension == 8
    //                                   |
    //                                CameraI
    float fCameraI = floor(g_CameraAttribs.f4CameraPos.x/fRingWorldStep + 0.5);
    float fCameraJ = floor(g_CameraAttribs.f4CameraPos.z/fRingWorldStep + 0.5);

	uint uiRingDimension = g_GlobalCloudAttribs.uiRingDimension;
    float fCellI = f3CellPosFlat.x / fRingWorldStep - fCameraI + (uiRingDimension/2);
    float fCellJ = f3CellPosFlat.z / fRingWorldStep - fCameraJ + (uiRingDimension/2);
	float fU = fCellI / float(uiRingDimension);
	float fV = fCellJ / float(uiRingDimension);
	float fW0 = float(uiRing)	/ float(g_GlobalCloudAttribs.uiNumRings);
	float fW1 = float(uiRing+1) / float(g_GlobalCloudAttribs.uiNumRings);
	float fW = fW0 + ( (fCloudAltitude + g_GlobalCloudAttribs.fCloudThickness*0.5) / g_GlobalCloudAttribs.fCloudThickness) / float(g_GlobalCloudAttribs.uiNumRings);

	float Width,Height,Depth;
	tex3DData.GetDimensions(Width,Height,Depth);
	fW = clamp(fW, fW0 + 0.5/Depth, fW1 - 0.5/Depth);
	return bAutoLOD ?
			texture(tex3DData, float3(fU, fV, fW)) :     // samLinearClamp
			textureLod(tex3DData, float3(fU, fV, fW), 0.0);
}

// This function computes different attributes of a particle which will be used
// for rendering
void ComputeParticleRenderAttribs(const in SParticleAttribs ParticleAttrs,
                            const in SCloudCellAttribs CellAttrs,
                            in float fTime,
                            in float3 f3CameraPos,
                            in float3 f3ViewRay,
                            in float3 f3EntryPointUSSpace, // Ray entry point in unit sphere (US) space
                            in float3 f3ViewRayUSSpace,    // View direction in unit sphere (US) space
                            in float  fIsecLenUSSpace,     // Length of the intersection of the view ray with the unit sphere
                            in float3 f3LightDirUSSpace,   // Light direction in unit sphere (US) space
                            in float fDistanceToExitPoint,
                            in float fDistanceToEntryPoint,
                            out float fCloudMass,
                            out float fTransparency,
                            /*uniform*/ in bool bAutoLOD
#if !LIGHT_SPACE_PASS
                            , in SCloudParticleLighting ParticleLighting
                            , out float4 f4Color
#endif
                            )
{
    float3 f3EntryPointWS = f3CameraPos + fDistanceToEntryPoint * f3ViewRay;
    float3 f3ExitPointWS  = f3CameraPos + fDistanceToExitPoint * f3ViewRay;

#if 0 && !LIGHT_SPACE_PASS
	{
		float3 f3VoxelCenter = f3EntryPointWS;
		float fNoisePeriod = 9413;
		//f3VoxelCenter.y = 0;
		float fNoise = g_tex3DNoise.SampleLevel(samLinearWrap, f3VoxelCenter/fNoisePeriod,0);
		f4Color = fNoise;
		uint uiCellI, uiCellJ, uiRing, uiLayerUnused;
		UnPackParticleIJRing(CellAttrs.uiPackedLocation, uiCellI, uiCellJ, uiRing, uiLayerUnused);
		float fMass = SampleCellAttribs3DTexture(g_tex3DLightAttenuatingMass, f3VoxelCenter, uiRing);
		const float fMaxMass = GetAttenuatingMassNormFactor();
		f4Color.rgb = exp( - fMass * fMaxMass * g_GlobalCloudAttribs.fAttenuationCoeff * 0.1 );

		//f4Color.rgb = SampleCellAttribs3DTexture(g_tex3DCellDensity, f3VoxelCenter, uiRing);
		fTransparency = 0;
		f4Color.a = fTransparency;
		fCloudMass = 100;
		return;
	}
#endif

	// Compute look-up coordinates
    float4 f4LUTCoords;
    WorldParamsToOpticalDepthLUTCoords(f3EntryPointUSSpace, f3ViewRayUSSpace, f4LUTCoords);
    // Randomly rotate the sphere
    f4LUTCoords.y += ParticleAttrs.fRndAzimuthBias;

    float fLOD = 0;//log2( 256.f / (ParticleAttrs.fSize / max(fDistanceToEntryPoint,1) * g_GlobalCloudAttribs.fBackBufferWidth)  );
	// Get the normalized density along the view ray
    float fNormalizedDensity = 1.f;
    SAMPLE_4D_LUT(g_tex3DParticleDensityLUT, OPTICAL_DEPTH_LUT_DIM, f4LUTCoords, fLOD, fNormalizedDensity);

	// Compute actual cloud mass by multiplying normalized density with ray length
    fCloudMass = fNormalizedDensity * (fDistanceToExitPoint - fDistanceToEntryPoint);
    float fFadeOutDistance = g_GlobalCloudAttribs.fParticleCutOffDist * g_fParticleToFlatMorphRatio;
    float fFadeOutFactor = saturate( (g_GlobalCloudAttribs.fParticleCutOffDist - fDistanceToEntryPoint) /  max(fFadeOutDistance,1) );
    fCloudMass *= fFadeOutFactor * CellAttrs.fMorphFadeout;
    fCloudMass *= ParticleAttrs.fDensity;

	// Compute transparency
    fTransparency = exp( -fCloudMass * g_GlobalCloudAttribs.fAttenuationCoeff );

#if !LIGHT_SPACE_PASS
	// Evaluate phase function for single scattering
	float fCosTheta = dot(-f3ViewRayUSSpace, f3LightDirUSSpace);
	float PhaseFunc = HGPhaseFunc(fCosTheta, 0.8);

	float2 f2SunLightAttenuation = ParticleLighting.f2SunLightAttenuation;
	float3 f3SingleScattering =  fTransparency *  ParticleLighting.f4SunLight.rgb * f2SunLightAttenuation.x * PhaseFunc * pow(CellAttrs.fMorphFadeout,2);

	float4 f4MultipleScatteringLUTCoords = WorldParamsToParticleScatteringLUT(f3EntryPointUSSpace, f3ViewRayUSSpace, f3LightDirUSSpace, true);
    float fMultipleScattering =
		g_tex3DMultipleScatteringInParticleLUT.SampleLevel(samLinearWrap, f4MultipleScatteringLUTCoords.xyz, 0);
	float3 f3MultipleScattering = (1-fTransparency) * fMultipleScattering * f2SunLightAttenuation.y * ParticleLighting.f4SunLight.rgb;

	// Compute ambient light
	float3 f3EarthCentre = float3(0, -g_MediaParams.fEarthRadius, 0);
	float fEnttryPointAltitude = length(f3EntryPointWS - f3EarthCentre);
	float fCloudBottomBoundary = g_MediaParams.fEarthRadius + g_GlobalCloudAttribs.fCloudAltitude - g_GlobalCloudAttribs.fCloudThickness/2.f;
	float fAmbientStrength =  (fEnttryPointAltitude - fCloudBottomBoundary) /  g_GlobalCloudAttribs.fCloudThickness;//(1-fNoise)*0.5;//0.3;
	fAmbientStrength = clamp(fAmbientStrength, 0.3, 1.0);
	float3 f3Ambient = (1-fTransparency) * fAmbientStrength * ParticleLighting.f4AmbientLight.rgb;


	f4Color.rgb = 0;
	const float fSingleScatteringScale = 0.2;
	f4Color.rgb += f3SingleScattering * fSingleScatteringScale;
	f4Color.rgb += f3MultipleScattering * PI;
	f4Color.rgb += f3Ambient;
	f4Color.rgb *= 2;

    f4Color.a = fTransparency;
#endif

}


// This function gets conservative minimal depth (which corresponds to the furthest surface)
// When rendering clouds in lower resolution, it is essentially important to assure that the
// furthest depth is taken, which is required for proper bilateral upscaling
float GetConservativeScreenDepth(in float2 f2UV)
{
    float fDepth;

#if BACK_BUFFER_DOWNSCALE_FACTOR == 1
    fDepth = textureLod(g_tex2DDepthBuffer, f2UV, 0.0 ).x;   // samLinearClamp
#else

    float2 f2GatherUV0 = f2UV;
#   if BACK_BUFFER_DOWNSCALE_FACTOR > 2
        float2 f2DepthBufferSize = float2(g_GlobalCloudAttribs.fBackBufferWidth, g_GlobalCloudAttribs.fBackBufferHeight);
        f2GatherUV0 -= (BACK_BUFFER_DOWNSCALE_FACTOR/2.f-1.f)/f2DepthBufferSize.xy;
#   endif

    fDepth = 1;
//    [unroll]
    for(int i=0; i < BACK_BUFFER_DOWNSCALE_FACTOR/2; ++i)
//        [unroll]
        for(int j=0; j < BACK_BUFFER_DOWNSCALE_FACTOR/2; ++j)
        {
            float4 f4Depths = textureGatherOffset(g_tex2DDepthBuffer, f2GatherUV0, 2*int2(i,j) );    // samLinearClamp
            fDepth = min(fDepth,f4Depths.x);
            fDepth = min(fDepth,f4Depths.y);
            fDepth = min(fDepth,f4Depths.z);
            fDepth = min(fDepth,f4Depths.w);
        }
#endif
    return fDepth;
}

in flat uint ps_uiParticleID;
layout(location = 0) out float fTransparency;
#if !VOLUMETRIC_BLENDING || LIGHT_SPACE_PASS
layout(location = 1) out float fDistToCloud;// : SV_Target1
#endif
#if !LIGHT_SPACE_PASS
layout(location = 2) out float4 f4Color; // : SV_Target2
#endif

void main()
{
    #if !LIGHT_SPACE_PASS && PS_ORDERING_AVAILABLE
        IntelExt_Init();
    #endif

        SParticleAttribs ParticleAttrs = g_Particles[In.uiParticleID];
        SCloudCellAttribs CellAttribs = g_CloudCells[In.uiParticleID / g_GlobalCloudAttribs.uiMaxLayers];

    #if !LIGHT_SPACE_PASS
        SCloudParticleLighting ParticleLighting = g_bufParticleLighting[In.uiParticleID];
    #endif
        float fTime = g_fTimeScale*g_GlobalCloudAttribs.fTime;

        float3 f3CameraPos, f3ViewRay;
    #if LIGHT_SPACE_PASS
        // For directional light source, we should use position on the near clip plane instead of
        // camera location as a ray start point
        float2 f2PosPS = UVToProj( (In.f4Pos.xy / g_GlobalCloudAttribs.f2LiSpCloudDensityDim.xy) );
        float4 f4PosOnNearClipPlaneWS = mul( float4(f2PosPS.xy,1,1), g_CameraAttribs.mViewProjInv );
        f3CameraPos = f4PosOnNearClipPlaneWS.xyz/f4PosOnNearClipPlaneWS.w;

        //f4PosOnNearClipPlaneWS = mul( float4(f2PosPS.xy,1e-4,1), g_CameraAttribs.mViewProjInv );
        //f3CameraPos = f4PosOnNearClipPlaneWS.xyz/f4PosOnNearClipPlaneWS.w;
        float4 f4PosOnFarClipPlaneWS = mul( float4(f2PosPS.xy,0,1), g_CameraAttribs.mViewProjInv );
        f4PosOnFarClipPlaneWS.xyz = f4PosOnFarClipPlaneWS.xyz/f4PosOnFarClipPlaneWS.w;
        f3ViewRay = normalize(f4PosOnFarClipPlaneWS.xyz - f4PosOnNearClipPlaneWS.xyz);
    #else
        f3CameraPos = g_CameraAttribs.f4CameraPos.xyz;
        //f3ViewRay = normalize(In.f3ViewRay);
        float2 f2ScreenDim = float2(g_GlobalCloudAttribs.fDownscaledBackBufferWidth, g_GlobalCloudAttribs.fDownscaledBackBufferHeight);
        float2 f2PosPS = UVToProj( In.f4Pos.xy / f2ScreenDim );
        float fDepth = GetConservativeScreenDepth( ProjToUV(f2PosPS.xy) );
        float4 f4ReconstructedPosWS = mul( float4(f2PosPS.xy,fDepth,1.0), g_CameraAttribs.mViewProjInv );
        float3 f3WorldPos = f4ReconstructedPosWS.xyz / f4ReconstructedPosWS.w;

        // Compute view ray
        f3ViewRay = f3WorldPos - f3CameraPos;
        float fRayLength = length(f3ViewRay);
        f3ViewRay /= fRayLength;

    #endif

        // Intersect view ray with the particle
        float2 f2RayIsecs;
        float fDistanceToEntryPoint, fDistanceToExitPoint;
        float3 f3EntryPointUSSpace, f3ViewRayUSSpace, f3LightDirUSSpace;
        IntersectRayWithParticle(ParticleAttrs, CellAttribs, f3CameraPos,  f3ViewRay,
                                 f2RayIsecs, f3EntryPointUSSpace, f3ViewRayUSSpace,
                                 f3LightDirUSSpace,
                                 fDistanceToEntryPoint, fDistanceToExitPoint);


    #if LIGHT_SPACE_PASS
        if( /*all*/(f2RayIsecs.x == NO_INTERSECTIONS && f2RayIsecs.y == NO_INTERSECTIONS) )
            discard;
    #else
        if( f2RayIsecs.y < 0 || fRayLength < fDistanceToEntryPoint )
            discard;
        fDistanceToExitPoint = min(fDistanceToExitPoint, fRayLength);
    #endif
        float fCloudMass;
        float fIsecLenUSSpace = f2RayIsecs.y - f2RayIsecs.x;
        // Compute particle rendering attributes
        ComputeParticleRenderAttribs(ParticleAttrs, CellAttribs,
                                fTime,
                                f3CameraPos,
                                f3ViewRay,
                                f3EntryPointUSSpace,
                                f3ViewRayUSSpace,
                                fIsecLenUSSpace,
                                f3LightDirUSSpace,
                                fDistanceToExitPoint,
                                fDistanceToEntryPoint,
                                fCloudMass,
                                fTransparency,
                                true
    #if !LIGHT_SPACE_PASS
                                , ParticleLighting
                                , f4Color
    #endif
                                );

    #if !LIGHT_SPACE_PASS
        #if VOLUMETRIC_BLENDING
            uint2 ui2PixelIJ = In.f4Pos.xy;
            uint uiLayerDataInd = (ui2PixelIJ.x + ui2PixelIJ.y * g_GlobalCloudAttribs.uiDownscaledBackBufferWidth) * NUM_PARTICLE_LAYERS;
            SParticleLayer Layers[NUM_PARTICLE_LAYERS+1];

            Layers[NUM_PARTICLE_LAYERS].f2MinMaxDist = float2(fDistanceToEntryPoint, fDistanceToExitPoint);
            Layers[NUM_PARTICLE_LAYERS].fOpticalMass = fCloudMass * g_GlobalCloudAttribs.fAttenuationCoeff;
            Layers[NUM_PARTICLE_LAYERS].f3Color = f4Color.rgb;

            f4Color = float4(0,0,0,1);
            fTransparency = 1;

            IntelExt_BeginPixelShaderOrdering();

//            [unroll]
            for(int iLayer=0; iLayer < NUM_PARTICLE_LAYERS; ++iLayer)
                Layers[iLayer] = g_rwbufParticleLayers[uiLayerDataInd + iLayer];

            // Sort layers
            //for(int i = 0; i < NUM_PARTICLE_LAYERS; ++i)
            //    for(int j = i+1; j < NUM_PARTICLE_LAYERS; ++j)
            //        if( Layers[i].f2MinMaxDist.x < Layers[j].f2MinMaxDist.x )
            //            SwapLayers( Layers[i], Layers[j] );


            // Merge two furthest layers
            SParticleLayer MergedLayer;
            MergeParticleLayers(Layers[0], Layers[1], MergedLayer, f4Color.rgb, fTransparency, true);
            f4Color.a = fTransparency;

            Layers[1] = MergedLayer;

            // Store updated layers
//            [unroll]
            for(iLayer = 0; iLayer < NUM_PARTICLE_LAYERS; ++iLayer)
                g_rwbufParticleLayers[uiLayerDataInd + iLayer] = Layers[iLayer+1];
        #else
            f4Color.rgb *= 1-fTransparency;
        #endif
    #endif


    #if !VOLUMETRIC_BLENDING || LIGHT_SPACE_PASS
        fDistToCloud = fTransparency < 0.99 ? fDistanceToEntryPoint : +FLT_MAX;
    #endif
}