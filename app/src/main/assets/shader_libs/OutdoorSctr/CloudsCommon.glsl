/////////////////////////////////////////////////////////////////////////////////////////////
// Copyright 2017 Intel Corporation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or imlied.
// See the License for the specific language governing permissions and
// limitations under the License.
/////////////////////////////////////////////////////////////////////////////////////////////

#include "../PostProcessingHLSLCompatiable.glsl"
#ifndef OPTICAL_DEPTH_LUT_DIM
#   define OPTICAL_DEPTH_LUT_DIM float4(64,32,64,32)
#endif

#ifndef NUM_PARTICLE_LAYERS
#   define NUM_PARTICLE_LAYERS 1
#endif

#ifndef SRF_SCATTERING_IN_PARTICLE_LUT_DIM
#   define SRF_SCATTERING_IN_PARTICLE_LUT_DIM float3(32,64,16)
#endif

#ifndef VOL_SCATTERING_IN_PARTICLE_LUT_DIM
#   define VOL_SCATTERING_IN_PARTICLE_LUT_DIM float4(32,64,32,8)
#endif

#ifndef THREAD_GROUP_SIZE
#   define THREAD_GROUP_SIZE 64
#endif

#ifndef CLOUD_DENSITY_TEX_DIM
#   define CLOUD_DENSITY_TEX_DIM float2(1024, 1024)
#endif

// Downscale factor for cloud color, transparency and distance buffers
#ifndef BACK_BUFFER_DOWNSCALE_FACTOR
#   define BACK_BUFFER_DOWNSCALE_FACTOR 2
#endif

#ifndef LIGHT_SPACE_PASS
#   define LIGHT_SPACE_PASS 0
#endif

#ifndef VOLUMETRIC_BLENDING
#   define VOLUMETRIC_BLENDING 1
#endif

#if !PS_ORDERING_AVAILABLE
#   undef VOLUMETRIC_BLENDING
#   define VOLUMETRIC_BLENDING 0
#endif

const float g_fCloudExtinctionCoeff = 100;

// Minimal cloud transparancy not flushed to zero
const float g_fTransparencyThreshold = 0.01;

// Fraction of the particle cut off distance which serves as
// a transition region from particles to flat clouds
const float g_fParticleToFlatMorphRatio = 0.2;

const float g_fTimeScale = 1.f;
const float2 g_f2CloudDensitySamplingScale = float2(1.f / 150000.f, 1.f / 19000.f);

#if 0
Texture2DArray<float>  g_tex2DLightSpaceDepthMap_t0 : register( t0 );
Texture2DArray<float>  g_tex2DLiSpCloudTransparency : register( t0 );
Texture2DArray<float2> g_tex2DLiSpCloudMinMaxDepth  : register( t1 );
Texture2D<float>       g_tex2DCloudDensity          : register( t1 );
Texture2D<float3>      g_tex2DWhiteNoise            : register( t3 );
Texture3D<float>       g_tex3DNoise                 : register( t4 );
Texture2D<float>       g_tex2MaxDensityMip          : register( t3 );
StructuredBuffer<uint> g_PackedCellLocations        : register( t0 );
StructuredBuffer<SCloudCellAttribs> g_CloudCells    : register( t2 );
StructuredBuffer<SParticleAttribs>  g_Particles     : register( t3 );
Texture3D<float>       g_tex3DLightAttenuatingMass      : register( t6 );
Texture3D<float>       g_tex3DCellDensity			: register( t4 );
StructuredBuffer<SParticleIdAndDist>  g_VisibleParticlesUnorderedList : register( t1 );
StructuredBuffer<SCloudParticleLighting> g_bufParticleLighting : register( t7 );
Texture2D<float3>       g_tex2DAmbientSkylight               : register( t7 );
Texture2DArray<float>   g_tex2DLightSpCloudTransparency      : register( t6 );
Texture2DArray<float2>  g_tex2DLightSpCloudMinMaxDepth       : register( t7 );
Buffer<uint>            g_ValidCellsCounter                  : register( t0 );
StructuredBuffer<uint>  g_ValidCellsUnorderedList            : register( t1 );
StructuredBuffer<uint>  g_ValidParticlesUnorderedList        : register( t1 );
Texture3D<float>        g_tex3DParticleDensityLUT			 : register( t10 );
Texture3D<float>        g_tex3DSingleScatteringInParticleLUT   : register( t11 );
Texture3D<float>        g_tex3DMultipleScatteringInParticleLUT : register( t12 );
#else
#define START_TEXTURE_UNIT 23
#define TEX2D_LIGHT_SPACE_DEPTH         START_TEXTURE_UNIT+0
#define TEX2D_CLOUD_TRANSPARENCY        START_TEXTURE_UNIT+1
#define TEX2D_CLOUD_MIN_MAX_DEPTH       START_TEXTURE_UNIT+2
#define TEX2D_CLOUD_DENSITY             START_TEXTURE_UNIT+3
#define TEX2D_WHITE_NOISE               START_TEXTURE_UNIT+4
#define TEX3D_NOISE                     START_TEXTURE_UNIT+5
#define TEX2D_MAX_DENSITY               START_TEXTURE_UNIT+6
#define TEX3D_LIGHT_ATTEN_MASS          START_TEXTURE_UNIT+7
#define TEX3D_CELL_DENSITY              START_TEXTURE_UNIT+8
#define TEX2D_AMB_SKY_LIGHT             START_TEXTURE_UNIT+9
#define TEX2D_LIGHT_CLOUD_TRANSPARENCY  START_TEXTURE_UNIT+10
#define TEX2D_LIGHT_CLOUD_MIN_MAX_DEPTH START_TEXTURE_UNIT+11
#define TEX3D_PARTICLE_DENSITY_LUT      START_TEXTURE_UNIT+12
#define TEX3D_SINGLE_SCATT_IN_PART_LUT  START_TEXTURE_UNIT+13
#define TEX3D_MULTIL_SCATT_IN_PART_LUT  START_TEXTURE_UNIT+14

#define TEXBUFFER_PACKED_CELLS          START_TEXTURE_UNIT+0
#define TEXBUFFER_CLOUD_CELLS           START_TEXTURE_UNIT+0
#define TEXBUFFER_PARTICLES             START_TEXTURE_UNIT+0
#define TEXBUFFER_VISIP_UNORDEREDLIST   START_TEXTURE_UNIT+0
#define TEXBUFFER_PARTICLE_LIGHTING     START_TEXTURE_UNIT+0
#define TEXBUFFER_CELL_COUNTER          START_TEXTURE_UNIT+0
#define TEXBUFFER_CELL_UNORDEREDLIST    START_TEXTURE_UNIT+0
#define TEXBUFFER_VALIDP_UNORDEREDLIST  START_TEXTURE_UNIT+0

layout(binding = TEX2D_LIGHT_SPACE_DEPTH) uniform sampler2DArray g_tex2DLightSpaceDepthMap_t0;
layout(binding = TEX2D_CLOUD_TRANSPARENCY) uniform sampler2DArray g_tex2DLiSpCloudTransparency;
layout(binding = TEX2D_CLOUD_MIN_MAX_DEPTH) uniform sampler2DArray g_tex2DLiSpCloudMinMaxDepth;
layout(binding = TEX2D_CLOUD_DENSITY) uniform sampler2D g_tex2DCloudDensity;
layout(binding = TEX2D_WHITE_NOISE) uniform sampler2D g_tex2DWhiteNoise;
layout(binding = TEX3D_NOISE) uniform sampler3D g_tex3DNoise;
layout(binding = TEX2D_MAX_DENSITY) uniform sampler2D g_tex2MaxDensityMip;
layout(binding = TEXBUFFER_PACKED_CELLS) uniform imageBuffer g_PackedCellLocations;
layout(binding = TEXBUFFER_CLOUD_CELLS) uniform imageBuffer g_CloudCells;
layout(binding = TEXBUFFER_PARTICLES) uniform imageBuffer g_Particles;
layout(binding = TEX3D_LIGHT_ATTEN_MASS) uniform sampler3D g_tex3DLightAttenuatingMass;
layout(binding = TEX3D_CELL_DENSITY) uniform sampler3D g_tex3DCellDensity;
layout(binding = TEXBUFFER_VISIP_UNORDEREDLIST) uniform imageBuffer g_VisibleParticlesUnorderedList;
layout(binding = TEXBUFFER_PARTICLE_LIGHTING) uniform imageBuffer g_bufParticleLighting;
layout(binding = TEX2D_AMB_SKY_LIGHT) uniform sampler2D g_tex2DAmbientSkylight;
#endif

RWStructuredBuffer<SParticleLayer> g_rwbufParticleLayers : register( u3 );
StructuredBuffer<SParticleLayer> g_bufParticleLayers : register( t0 );

StructuredBuffer<float4> g_SunLightAttenuation : register( t4 );

SamplerState samLinearWrap : register( s1 );
SamplerState samPointWrap : register( s2 );

cbuffer cbPostProcessingAttribs : register( b0 )
{
    SGlobalCloudAttribs g_GlobalCloudAttribs;
};

struct SScreenSizeQuadVSOutput
{
    float4 m_f4Pos : SV_Position;
    float2 m_f2PosPS : PosPS; // Position in projection space [-1,1]x[-1,1]
};

// Vertex shader for generating screen-size quad
SScreenSizeQuadVSOutput ScreenSizeQuadVS(in uint VertexId : SV_VertexID)
{
    float4 MinMaxUV = float4(-1, -1, 1, 1);

    SScreenSizeQuadVSOutput Verts[4] =
    {
        {float4(MinMaxUV.xy, 1.0, 1.0), MinMaxUV.xy},
        {float4(MinMaxUV.xw, 1.0, 1.0), MinMaxUV.xw},
        {float4(MinMaxUV.zy, 1.0, 1.0), MinMaxUV.zy},
        {float4(MinMaxUV.zw, 1.0, 1.0), MinMaxUV.zw}
    };

    return Verts[VertexId];
}

// This shader computes level 0 of the maximum density mip map
float RenderMaxMipLevel0PS(SScreenSizeQuadVSOutput In) : SV_Target
{
    // Compute maximum density in the 3x3 neighborhood
    // Since dimensions of the source and destination textures are the same,
    // rasterization locations are aligned with source texels
    float2 f2UV = ProjToUV(In.m_f2PosPS.xy);
    float fMaxDensity = 0;
    [unroll]
    for(int i=-1; i <= +1; ++i)
        [unroll]
        for(int j=-1; j <= +1; ++j)
        {
            float fCurrDensity = g_tex2DCloudDensity.SampleLevel(samPointWrap, f2UV, 0, int2(i,j));
            fMaxDensity = max(fMaxDensity, fCurrDensity);
        }
    return fMaxDensity;
}

// This shader renders next coarse level of the maximum density mip map
float RenderCoarseMaxMipLevelPS(SScreenSizeQuadVSOutput In) : SV_Target
{
    // Compute maximum density of 4 finer-level texels
    float2 f2MipSize;
    float fMipLevels;
    float fSrcMip = g_GlobalCloudAttribs.f4Parameter.x-1;
    g_tex2MaxDensityMip.GetDimensions(fSrcMip, f2MipSize.x, f2MipSize.y, fMipLevels);
    // Note that since dst level is 2 times smaller than the src level, we have to
    // align texture coordinates
    //   _____ _____
    //  |     |     |  1
    //  |_____._____|
    //  |  .  |     |  0
    //  |_____|_____|
    //     0      1
    float2 f2UV = ProjToUV(In.m_f2PosPS.xy) - 0.5f / f2MipSize;
    float fMaxDensity = 0;
    [unroll]
    for(int i=0; i <= +1; ++i)
        [unroll]
        for(int j=0; j <= +1; ++j)
        {
            float fCurrDensity = g_tex2MaxDensityMip.SampleLevel(samPointWrap, f2UV, fSrcMip, int2(i,j));
            fMaxDensity = max(fMaxDensity, fCurrDensity);
        }
    return fMaxDensity;
}

float2 ComputeDensityTexLODsFromUV(in float4 fDeltaUV01)
{
    fDeltaUV01 *= CLOUD_DENSITY_TEX_DIM.xyxy;
    float2 f2UVDeltaLen = float2( length(fDeltaUV01.xy), length(fDeltaUV01.zw) );
    f2UVDeltaLen = max(f2UVDeltaLen,1 );
    return log2( f2UVDeltaLen );
}

float2 ComputeDensityTexLODsFromStep(in float fSamplingStep)
{
    float2 f2dU = fSamplingStep * g_f2CloudDensitySamplingScale * CLOUD_DENSITY_TEX_DIM.xx;
    float2 f2LODs = log2(max(f2dU, 1));
    return f2LODs;
}

float4 GetCloudDensityUV(in float3 CloudPosition, in float fTime)
{
    const float4 f2Offset01 = float4( 0.1*float2(-0.04, +0.01) * fTime, 0.2*float2( 0.01,  0.04) * fTime );
    float4 f2UV01 = CloudPosition.xzxz * g_f2CloudDensitySamplingScale.xxyy + f2Offset01;
    return f2UV01;
}

float GetCloudDensity(in float4 f4UV01, in float2 f2LODs = float2(0,0))
{
    float fDensity =
        g_tex2DCloudDensity.SampleLevel(samLinearWrap, f4UV01.xy, f2LODs.x) *
        g_tex2DCloudDensity.SampleLevel(samLinearWrap, f4UV01.zw, f2LODs.y);

    fDensity = saturate((fDensity-g_GlobalCloudAttribs.fCloudDensityThreshold)/(1-g_GlobalCloudAttribs.fCloudDensityThreshold));

    return fDensity;
}

float GetCloudDensityAutoLOD(in float4 f4UV01)
{
    float fDensity =
        g_tex2DCloudDensity.Sample(samLinearWrap, f4UV01.xy) *
        g_tex2DCloudDensity.Sample(samLinearWrap, f4UV01.zw);

    fDensity = saturate((fDensity-g_GlobalCloudAttribs.fCloudDensityThreshold)/(1-g_GlobalCloudAttribs.fCloudDensityThreshold));

    return fDensity;
}

float GetCloudDensity(in float3 CloudPosition, in const float fTime, in float2 f2LODs = float2(0,0))
{
    float4 f4UV01 = GetCloudDensityUV(CloudPosition, fTime);
    return GetCloudDensity(f4UV01, f2LODs);
}

float GetMaxDensity(in float4 f4UV01, in float2 f2LODs = float2(0,0))
{
    float fDensity =
        g_tex2MaxDensityMip.SampleLevel(samPointWrap, f4UV01.xy, f2LODs.x) *
        g_tex2MaxDensityMip.SampleLevel(samPointWrap, f4UV01.zw, f2LODs.y);

    fDensity = saturate((fDensity-g_GlobalCloudAttribs.fCloudDensityThreshold)/(1-g_GlobalCloudAttribs.fCloudDensityThreshold));

    return fDensity;
}

float GetMaxDensity(in float3 CloudPosition, in const float fTime, in float2 f2LODs = float2(0,0))
{
    float4 f4UV01 = GetCloudDensityUV(CloudPosition, fTime);
    return GetMaxDensity(f4UV01, f2LODs);
}

// Computes direction from the zenith and azimuth angles in XZY (Y Up) coordinate system
float3 ZenithAzimuthAngleToDirectionXZY(in float fZenithAngle, in float fAzimuthAngle)
{
    //       Y   Zenith
    //       |  /
    //       | / /'
    //       |  / '
    //       | /  '
    //       |/___'________X
    //      / \  -Azimuth
    //     /   \  '
    //    /     \ '
    //   Z       \'

    float fZenithSin, fZenithCos, fAzimuthSin, fAzimuthCos;
    sincos(fZenithAngle,  fZenithSin,  fZenithCos);
    sincos(fAzimuthAngle, fAzimuthSin, fAzimuthCos);

    float3 f3Direction;
    f3Direction.y = fZenithCos;
    f3Direction.x = fZenithSin * fAzimuthCos;
    f3Direction.z = fZenithSin * fAzimuthSin;

    return f3Direction;
}

// Computes the zenith and azimuth angles in XZY (Y Up) coordinate system from direction
void DirectionToZenithAzimuthAngleXZY(in float3 f3Direction, out float fZenithAngle, out float fAzimuthAngle)
{
    float fZenithCos = f3Direction.y;
    fZenithAngle = acos(fZenithCos);
    //float fZenithSin = sqrt( max(1 - fZenithCos*fZenithCos, 1e-10) );
    float fAzimuthCos = f3Direction.x;// / fZenithSin;
    float fAzimuthSin = f3Direction.z;// / fZenithSin;
    fAzimuthAngle = atan2(fAzimuthSin, fAzimuthCos);
}

// Constructs local XYZ (Z Up) frame from Up and Inward vectors
void ConstructLocalFrameXYZ(in float3 f3Up, in float3 f3Inward, out float3 f3X, out float3 f3Y, out float3 f3Z)
{
    //      Z (Up)
    //      |    Y  (Inward)
    //      |   /
    //      |  /
    //      | /
    //      |/
    //       -----------> X
    //
    f3Z = normalize(f3Up);
    f3X = normalize(cross(f3Inward, f3Z));
    f3Y = normalize(cross(f3Z, f3X));
}

// Computes direction in local XYZ (Z Up) frame from zenith and azimuth angles
float3 GetDirectionInLocalFrameXYZ(in float3 f3LocalX,
                                in float3 f3LocalY,
                                in float3 f3LocalZ,
                                in float fLocalZenithAngle,
                                in float fLocalAzimuthAngle)
{
    // Compute sin and cos of the angle between ray direction and local zenith
    float fDirLocalSinZenithAngle, fDirLocalCosZenithAngle;
    sincos(fLocalZenithAngle, fDirLocalSinZenithAngle, fDirLocalCosZenithAngle);
    // Compute sin and cos of the local azimuth angle

    float fDirLocalAzimuthCos, fDirLocalAzimuthSin;
    sincos(fLocalAzimuthAngle, fDirLocalAzimuthSin, fDirLocalAzimuthCos);
    // Reconstruct view ray
    return f3LocalZ * fDirLocalCosZenithAngle +
           fDirLocalSinZenithAngle * (fDirLocalAzimuthCos * f3LocalX + fDirLocalAzimuthSin * f3LocalY );
}

// Computes zenith and azimuth angles in local XYZ (Z Up) frame from the direction
void ComputeLocalFrameAnglesXYZ(in float3 f3LocalX,
                             in float3 f3LocalY,
                             in float3 f3LocalZ,
                             in float3 f3RayDir,
                             out float fLocalZenithAngle,
                             out float fLocalAzimuthAngle)
{
    fLocalZenithAngle = acos(saturate( dot(f3LocalZ, f3RayDir) ));

    // Compute azimuth angle in the local frame
    float fViewDirLocalAzimuthCos = dot(f3RayDir, f3LocalX);
    float fViewDirLocalAzimuthSin = dot(f3RayDir, f3LocalY);
    fLocalAzimuthAngle = atan2(fViewDirLocalAzimuthSin, fViewDirLocalAzimuthCos);
}

void WorldParamsToOpticalDepthLUTCoords(in float3 f3NormalizedStartPos, in float3 f3RayDir, out float4 f4LUTCoords)
{
    DirectionToZenithAzimuthAngleXZY(f3NormalizedStartPos, f4LUTCoords.x, f4LUTCoords.y);

    float3 f3LocalX, f3LocalY, f3LocalZ;
    // Construct local tangent frame for the start point on the sphere (z up)
    // For convinience make the Z axis look into the sphere
    ConstructLocalFrameXYZ( -f3NormalizedStartPos, float3(0,1,0), f3LocalX, f3LocalY, f3LocalZ);

    // z coordinate is the angle between the ray direction and the local frame zenith direction
    // Note that since we are interested in rays going inside the sphere only, the allowable
    // range is [0, PI/2]

    float fRayDirLocalZenith, fRayDirLocalAzimuth;
    ComputeLocalFrameAnglesXYZ(f3LocalX, f3LocalY, f3LocalZ, f3RayDir, fRayDirLocalZenith, fRayDirLocalAzimuth);
    f4LUTCoords.z = fRayDirLocalZenith;
    f4LUTCoords.w = fRayDirLocalAzimuth;

    f4LUTCoords.xyzw = f4LUTCoords.xyzw / float4(PI, 2*PI, PI/2, 2*PI) + float4(0.0, 0.5, 0, 0.5);

    // Clamp only zenith (yz) coordinate as azimuth is filtered with wraparound mode
    f4LUTCoords.xz = clamp(f4LUTCoords, 0.5/OPTICAL_DEPTH_LUT_DIM, 1.0-0.5/OPTICAL_DEPTH_LUT_DIM).xz;
}

void OpticalDepthLUTCoordsToWorldParams(in float4 f4LUTCoords, out float3 f3NormalizedStartPos, out float3 f3RayDir)
{
    float fStartPosZenithAngle  = f4LUTCoords.x * PI;
    float fStartPosAzimuthAngle = (f4LUTCoords.y - 0.5) * 2 * PI;
    f3NormalizedStartPos = ZenithAzimuthAngleToDirectionXZY(fStartPosZenithAngle, fStartPosAzimuthAngle);

    // Construct local tangent frame (z up)
    float3 f3LocalX, f3LocalY, f3LocalZ;
    ConstructLocalFrameXYZ(-f3NormalizedStartPos, float3(0,1,0), f3LocalX, f3LocalY, f3LocalZ);

    float fDirZentihAngle = f4LUTCoords.z * PI/2;
    float fDirLocalAzimuthAngle = (f4LUTCoords.w - 0.5) * 2 * PI;
    f3RayDir = GetDirectionInLocalFrameXYZ(f3LocalX, f3LocalY, f3LocalZ, fDirZentihAngle, fDirLocalAzimuthAngle);
}

float GetCloudRingWorldStep(uint uiRing, SGlobalCloudAttribs g_GlobalCloudAttribs)
{
    const float fLargestRingSize = g_GlobalCloudAttribs.fParticleCutOffDist * 2;
    uint uiRingDimension = g_GlobalCloudAttribs.uiRingDimension;
    uint uiNumRings = g_GlobalCloudAttribs.uiNumRings;
    float fRingWorldStep = fLargestRingSize / (float)((uiRingDimension) << ((uiNumRings-1) - uiRing));
    return fRingWorldStep;
}

float GetParticleSize(in float fRingWorldStep)
{
    return fRingWorldStep;
}

void ParticleScatteringLUTToWorldParams(in float4 f4LUTCoords,
                                        out float3 f3StartPosUSSpace,
                                        out float3 f3ViewDirUSSpace,
                                        out float3 f3LightDirUSSpace,
                                        in /*uniform*/ bool bSurfaceOnly)
{
    f3LightDirUSSpace = float3(0,0,1);
    float fStartPosZenithAngle = f4LUTCoords.x * PI;
    f3StartPosUSSpace = float3(0,0,0);
    sincos(fStartPosZenithAngle, f3StartPosUSSpace.x, f3StartPosUSSpace.z);

    float3 f3LocalX, f3LocalY, f3LocalZ;
    ConstructLocalFrameXYZ(-f3StartPosUSSpace, f3LightDirUSSpace, f3LocalX, f3LocalY, f3LocalZ);

    if( !bSurfaceOnly )
    {
        float fDistFromCenter = f4LUTCoords.w;
        // Scale the start position according to the distance from center
        f3StartPosUSSpace *= fDistFromCenter;
    }

    float fViewDirLocalAzimuth = (f4LUTCoords.y - 0.5) * (2 * PI);
    float fViewDirLocalZenith = f4LUTCoords.z * ( bSurfaceOnly ? (PI/2) : PI );
    f3ViewDirUSSpace = GetDirectionInLocalFrameXYZ(f3LocalX, f3LocalY, f3LocalZ, fViewDirLocalZenith, fViewDirLocalAzimuth);
}

// All parameters must be defined in the unit sphere (US) space
float4 WorldParamsToParticleScatteringLUT(in float3 f3StartPosUSSpace,
                                          in float3 f3ViewDirInUSSpace,
                                          in float3 f3LightDirInUSSpace,
                                          in /*uniform*/ bool bSurfaceOnly)
{
    float4 f4LUTCoords = 0;

    float fDistFromCenter = 0;
    if( !bSurfaceOnly )
    {
        // Compute distance from center and normalize start position
        fDistFromCenter = length(f3StartPosUSSpace);
        f3StartPosUSSpace /= max(fDistFromCenter, 1e-5);
    }
    float fStartPosZenithCos = dot(f3StartPosUSSpace, f3LightDirInUSSpace);
    f4LUTCoords.x = acos(fStartPosZenithCos);

    float3 f3LocalX, f3LocalY, f3LocalZ;
    ConstructLocalFrameXYZ(-f3StartPosUSSpace, f3LightDirInUSSpace, f3LocalX, f3LocalY, f3LocalZ);

    float fViewDirLocalZenith, fViewDirLocalAzimuth;
    ComputeLocalFrameAnglesXYZ(f3LocalX, f3LocalY, f3LocalZ, f3ViewDirInUSSpace, fViewDirLocalZenith, fViewDirLocalAzimuth);
    f4LUTCoords.y = fViewDirLocalAzimuth;
    f4LUTCoords.z = fViewDirLocalZenith;

    // In case the parameterization is performed for the sphere surface, the allowable range for the
    // view direction zenith angle is [0, PI/2] since the ray should always be directed into the sphere.
    // Otherwise the range is whole [0, PI]
    f4LUTCoords.xyz = f4LUTCoords.xyz / float3(PI, 2*PI, bSurfaceOnly ? (PI/2) : PI) + float3(0, 0.5, 0);
    if( bSurfaceOnly )
        f4LUTCoords.w = 0;
    else
        f4LUTCoords.w = fDistFromCenter;
    if( bSurfaceOnly )
        f4LUTCoords.xz = clamp(f4LUTCoords.xyz, 0.5/SRF_SCATTERING_IN_PARTICLE_LUT_DIM, 1-0.5/SRF_SCATTERING_IN_PARTICLE_LUT_DIM).xz;
    else
        f4LUTCoords.xzw = clamp(f4LUTCoords, 0.5/VOL_SCATTERING_IN_PARTICLE_LUT_DIM, 1-0.5/VOL_SCATTERING_IN_PARTICLE_LUT_DIM).xzw;

    return f4LUTCoords;
}


#define SAMPLE_4D_LUT(tex3DLUT, LUT_DIM, f4LUTCoords, fLOD, Result)  \
{                                                               \
    float3 f3UVW;                                               \
    f3UVW.xy = f4LUTCoords.xy;                                  \
    float fQSlice = f4LUTCoords.w * LUT_DIM.w - 0.5;            \
    float fQ0Slice = floor(fQSlice);                            \
    float fQWeight = fQSlice - fQ0Slice;                        \
                                                                \
    f3UVW.z = (fQ0Slice + f4LUTCoords.z) / LUT_DIM.w;           \
                                                                \
    Result = lerp(                                              \
        tex3DLUT.SampleLevel(samLinearWrap, f3UVW, fLOD),       \
        /* frac() assures wraparound filtering of w coordinate*/                            \
        tex3DLUT.SampleLevel(samLinearWrap, frac(f3UVW + float3(0,0,1/LUT_DIM.w)), fLOD),   \
        fQWeight);                                                                          \
}

float HGPhaseFunc(float fCosTheta, const float g = 0.9)
{
    return (1/(4*PI) * (1 - g*g)) / pow( max((1 + g*g) - (2*g)*fCosTheta,0), 3.f/2.f);
}