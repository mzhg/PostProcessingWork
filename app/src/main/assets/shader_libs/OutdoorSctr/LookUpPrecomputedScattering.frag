#include "Scattering.frag"

float ZenithAngle2TexCoord(float fCosZenithAngle, float fHeight, in float fTexDim, float power, float fPrevTexCoord)
{
    float fTexCoord;
    float fCosHorzAngle = GetCosHorizonAnlge(fHeight);
    // When performing look-ups into the scattering texture, it is very important that all the look-ups are consistent
    // wrt to the horizon. This means that if the first look-up is above (below) horizon, then the second look-up
    // should also be above (below) horizon. 
    // We use previous texture coordinate, if it is provided, to find out if previous look-up was above or below
    // horizon. If texture coordinate is negative, then this is the first look-up
    bool bIsAboveHorizon = fPrevTexCoord >= 0.5;
    bool bIsBelowHorizon = 0 <= fPrevTexCoord && fPrevTexCoord < 0.5;
    if(  bIsAboveHorizon || 
        !bIsBelowHorizon && (fCosZenithAngle > fCosHorzAngle) )
    {
        // Scale to [0,1]
        fTexCoord = saturate( (fCosZenithAngle - fCosHorzAngle) / (1.0 - fCosHorzAngle) );
        fTexCoord = pow(fTexCoord, power);
        // Now remap texture coordinate to the upper half of the texture.
        // To avoid filtering across discontinuity at 0.5, we must map
        // the texture coordinate to [0.5 + 0.5/fTexDim, 1 - 0.5/fTexDim]
        //
        //      0.5   1.5               D/2+0.5        D-0.5  texture coordinate x dimension
        //       |     |                   |            |
        //    |  X  |  X  | .... |  X  ||  X  | .... |  X  |  
        //       0     1          D/2-1   D/2          D-1    texel index
        //
        fTexCoord = 0.5f + 0.5f / fTexDim + fTexCoord * (fTexDim/2.0 - 1.0) / fTexDim;
    }
    else
    {
        fTexCoord = saturate( (fCosHorzAngle - fCosZenithAngle) / (fCosHorzAngle - (-1.0)) );
        fTexCoord = pow(fTexCoord, power);
        // Now remap texture coordinate to the lower half of the texture.
        // To avoid filtering across discontinuity at 0.5, we must map
        // the texture coordinate to [0.5, 0.5 - 0.5/fTexDim]
        //
        //      0.5   1.5        D/2-0.5             texture coordinate x dimension
        //       |     |            |       
        //    |  X  |  X  | .... |  X  ||  X  | .... 
        //       0     1          D/2-1   D/2        texel index
        //
        fTexCoord = 0.5f / fTexDim + fTexCoord * (fTexDim/2.0 - 1.0) / fTexDim;
    }    

    return fTexCoord;
}

float4 WorldParams2InsctrLUTCoords(float fHeight,
                                   float fCosViewZenithAngle,
                                   float fCosSunZenithAngle,
                                   float fCosSunViewAngle,
                                   float fRefUVWQ)
{
	const float SafetyHeightMargin = 16.f;
    float4 f4UVWQ;
	float4 const_value = PRECOMPUTED_SCTR_LUT_DIM;

    // Limit allowable height range to [SafetyHeightMargin, AtmTopHeight - SafetyHeightMargin] to
    // avoid numeric issues at the Earth surface and the top of the atmosphere
    // (ray/Earth and ray/top of the atmosphere intersection tests are unstable when fHeight == 0 and
    // fHeight == AtmTopHeight respectively)
    fHeight = clamp(fHeight, SafetyHeightMargin, g_fAtmTopHeight - SafetyHeightMargin);
    f4UVWQ.x = saturate( (fHeight - SafetyHeightMargin) / (g_fAtmTopHeight - 2.0*SafetyHeightMargin) );

#if NON_LINEAR_PARAMETERIZATION
    f4UVWQ.x = pow(f4UVWQ.x, HeightPower);

    f4UVWQ.y = ZenithAngle2TexCoord(fCosViewZenithAngle, fHeight, PRECOMPUTED_SCTR_LUT_DIM.y, ViewZenithPower, fRefUVWQ);
    
    // Use Eric Bruneton's formula for cosine of the sun-zenith angle
    f4UVWQ.z = (atan(max(fCosSunZenithAngle, -0.1975) * tan(1.26 * 1.1)) / 1.1 + (1.0 - 0.26)) * 0.5;

    fCosSunViewAngle = clamp(fCosSunViewAngle, -1.0, +1.0);
    f4UVWQ.w = acos(fCosSunViewAngle) / PI;
    f4UVWQ.w = sign(f4UVWQ.w - 0.5) * pow( abs((f4UVWQ.w - 0.5)/0.5), SunViewPower)/2.0 + 0.5;
    
    f4UVWQ.xzw = ((f4UVWQ * (PRECOMPUTED_SCTR_LUT_DIM-1.0) + 0.5) / PRECOMPUTED_SCTR_LUT_DIM).xzw;
#else
	float y = (fCosViewZenithAngle+1.0)/2.0;
    float z = (fCosSunZenithAngle+1.0)/2.0;
	float w = (fCosSunViewAngle   +1.0)/2.0;

	f4UVWQ.yzw = float3(y,z,w);
    f4UVWQ = (f4UVWQ * (const_value-1.0) + 0.5) / const_value;
#endif

    return f4UVWQ;
}

float3 LookUpPrecomputedScattering(float3 f3StartPoint, 
                                   float3 f3ViewDir, 
                                   float3 f3EarthCentre,
                                   float3 f3DirOnLight,
//                                 in Texture3D<float3> tex3DScatteringLUT,
								   sampler3D tex3DScatteringLUT,
                                   inout float4 f4UVWQ)
{
    float3 f3EarthCentreToPointDir = f3StartPoint - f3EarthCentre;
    float fDistToEarthCentre = length(f3EarthCentreToPointDir);
    f3EarthCentreToPointDir /= fDistToEarthCentre;
    float fHeightAboveSurface = fDistToEarthCentre - g_fEarthRadius;
    float fCosViewZenithAngle = dot( f3EarthCentreToPointDir, f3ViewDir    );
    float fCosSunZenithAngle  = dot( f3EarthCentreToPointDir, f3DirOnLight );
    float fCosSunViewAngle    = dot( f3ViewDir,               f3DirOnLight );

    // Provide previous look-up coordinates
    f4UVWQ = WorldParams2InsctrLUTCoords(fHeightAboveSurface, fCosViewZenithAngle,
                                         fCosSunZenithAngle, fCosSunViewAngle, 
                                         f4UVWQ.y);

    float3 f3UVW0; 
    f3UVW0.xy = f4UVWQ.xy;
    float fQ0Slice = floor(f4UVWQ.w * PRECOMPUTED_SCTR_LUT_DIM.w - 0.5);
    fQ0Slice = clamp(fQ0Slice, 0, PRECOMPUTED_SCTR_LUT_DIM.w-1.0);
    float fQWeight = (f4UVWQ.w * PRECOMPUTED_SCTR_LUT_DIM.w - 0.5) - fQ0Slice;
    fQWeight = max(fQWeight, 0.0);
    float2 f2SliceMinMaxZ = float2(fQ0Slice, fQ0Slice+1.0)/PRECOMPUTED_SCTR_LUT_DIM.w + float2(0.5,-0.5) / (PRECOMPUTED_SCTR_LUT_DIM.z*PRECOMPUTED_SCTR_LUT_DIM.w);
    f3UVW0.z =  (fQ0Slice + f4UVWQ.z) / PRECOMPUTED_SCTR_LUT_DIM.w;
    f3UVW0.z = clamp(f3UVW0.z, f2SliceMinMaxZ.x, f2SliceMinMaxZ.y);
    
    float fQ1Slice = min(fQ0Slice+1.0, PRECOMPUTED_SCTR_LUT_DIM.w-1.0);
    float fNextSliceOffset = (fQ1Slice - fQ0Slice) / PRECOMPUTED_SCTR_LUT_DIM.w;
    float3 f3UVW1 = f3UVW0 + float3(0,0,fNextSliceOffset);
//    float3 f3Insctr0 = tex3DScatteringLUT.SampleLevel(samLinearClamp, f3UVW0, 0);
//    float3 f3Insctr1 = tex3DScatteringLUT.SampleLevel(samLinearClamp, f3UVW1, 0);
	float4 f3Insctr0 = textureLod(tex3DScatteringLUT, f3UVW0, 0.0);
	float4 f3Insctr1 = textureLod(tex3DScatteringLUT, f3UVW1, 0.0);
    float4 f3Inscattering = lerp(f3Insctr0, f3Insctr1, fQWeight);

    return f3Inscattering.xyz;
}