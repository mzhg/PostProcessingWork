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

#include "CloudsBase.glsl"
#define FLT_MAX 3.402823466e+38f

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
RWStructuredBuffer<SParticleLayer> g_rwbufParticleLayers : register( u3 );
StructuredBuffer<SParticleLayer> g_bufParticleLayers : register( t0 );

StructuredBuffer<float4> g_SunLightAttenuation : register( t4 );

SamplerState samLinearWrap : register( s1 );
SamplerState samPointWrap : register( s2 );

#else
#define START_TEXTURE_UNIT 0
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
#define TEX3D_LIGHT_CLOUD_TRANSPARENCY  START_TEXTURE_UNIT+10
#define TEX3D_LIGHT_CLOUD_MIN_MAX_DEPTH START_TEXTURE_UNIT+11
#define TEX3D_PARTICLE_DENSITY_LUT      START_TEXTURE_UNIT+12
#define TEX3D_SINGLE_SCATT_IN_PART_LUT  START_TEXTURE_UNIT+13
#define TEX3D_MULTIL_SCATT_IN_PART_LUT  START_TEXTURE_UNIT+14
#define TEX2D_SCR_CLOUD_MIN_MAX_DIST    START_TEXTURE_UNIT+15
#define TEX2D_SCR_CLOUD_TRANSPARENCY    START_TEXTURE_UNIT+16
#define TEX2D_SCR_CLOUD_COLOR           START_TEXTURE_UNIT+17

#define TEXBUFFER_PACKED_CELLS          0
#define TEXBUFFER_CLOUD_CELLS           1
#define TEXBUFFER_PARTICLES             2
#define TEXBUFFER_VISIP_UNORDEREDLIST   3
#define TEXBUFFER_PARTICLE_LIGHTING     4
#define TEXBUFFER_CELL_COUNTER          5
#define TEXBUFFER_CELL_UNORDEREDLIST    6
#define TEXBUFFER_VALIDP_UNORDEREDLIST  7

layout(binding = TEX2D_LIGHT_SPACE_DEPTH) uniform sampler2DArray g_tex2DLightSpaceDepthMap_t0;
layout(binding = TEX2D_CLOUD_TRANSPARENCY) uniform sampler2DArray g_tex2DLiSpCloudTransparency;
layout(binding = TEX2D_CLOUD_MIN_MAX_DEPTH) uniform sampler2DArray g_tex2DLiSpCloudMinMaxDepth;
layout(binding = TEX2D_CLOUD_DENSITY) uniform sampler2D g_tex2DCloudDensity;
layout(binding = TEX2D_WHITE_NOISE) uniform sampler2D g_tex2DWhiteNoise;
layout(binding = TEX3D_NOISE) uniform sampler3D g_tex3DNoise;
layout(binding = TEX2D_MAX_DENSITY) uniform sampler2D g_tex2MaxDensityMip;
layout(r32ui, binding = TEXBUFFER_PACKED_CELLS) uniform uimageBuffer g_PackedCellLocations;
//layout(binding = TEXBUFFER_CLOUD_CELLS) uniform imageBuffer g_CloudCells;
//layout(binding = TEXBUFFER_PARTICLES) uniform imageBuffer g_Particles;
layout(binding = TEX3D_LIGHT_ATTEN_MASS) uniform sampler3D g_tex3DLightAttenuatingMass;
layout(binding = TEX3D_CELL_DENSITY) uniform sampler3D g_tex3DCellDensity;
//layout(binding = TEXBUFFER_VISIP_UNORDEREDLIST) uniform imageBuffer g_VisibleParticlesUnorderedList;
//layout(binding = TEXBUFFER_PARTICLE_LIGHTING) uniform imageBuffer g_bufParticleLighting;
layout(binding = TEX2D_AMB_SKY_LIGHT) uniform sampler2D g_tex2DAmbientSkylight;
layout(binding = TEX3D_LIGHT_CLOUD_TRANSPARENCY) uniform sampler2DArray g_tex2DLightSpCloudTransparency;
layout(binding = TEX3D_LIGHT_CLOUD_MIN_MAX_DEPTH) uniform sampler2DArray g_tex2DLightSpCloudMinMaxDepth;
layout(r32ui, binding = TEXBUFFER_CELL_COUNTER) uniform uimageBuffer g_ValidCellsCounter;
//layout(binding = TEXBUFFER_CELL_UNORDEREDLIST) uniform uimageBuffer g_ValidCellsUnorderedList;
//layout(binding = TEXBUFFER_VALIDP_UNORDEREDLIST) uniform uimageBuffer g_ValidParticlesUnorderedList;
layout(binding = TEX3D_PARTICLE_DENSITY_LUT) uniform sampler3D g_tex3DParticleDensityLUT;
layout(binding = TEX3D_SINGLE_SCATT_IN_PART_LUT) uniform sampler3D g_tex3DSingleScatteringInParticleLUT;
layout(binding = TEX3D_MULTIL_SCATT_IN_PART_LUT) uniform sampler3D g_tex3DMultipleScatteringInParticleLUT;
layout(binding = TEX2D_SCR_CLOUD_MIN_MAX_DIST) uniform sampler2D g_tex2DScrSpaceCloudMinMaxDist;
layout(binding = TEX2D_SCR_CLOUD_TRANSPARENCY) uniform sampler2D g_tex2DScrSpaceCloudTransparency;
layout(binding = TEX2D_SCR_CLOUD_COLOR) uniform sampler2D g_tex2DScrSpaceCloudColor;

layout(binding = 1) buffer StructuredBuffer0
{
 uint g_ValidCellsUnorderedList[];
};

layout(std430, binding = 2) buffer StructuredBuffer1
{
 SCloudCellAttribs g_CloudCells[];
};

layout(binding = 1) buffer StructuredBuffer2
{
   SParticleIdAndDist g_VisibleParticlesUnorderedList[];
};
//StructuredBuffer<SParticleAttribs>  g_Particles     : register( t3 );
layout(binding = 3) buffer StructuredBuffer3
{
   SParticleAttribs g_Particles[];
};

#endif


#if 0
cbuffer cbPostProcessingAttribs : register( b0 )
{
    SGlobalCloudAttribs g_GlobalCloudAttribs;
};
#else
uniform SGlobalCloudAttribs g_GlobalCloudAttribs;

uniform vec4  g_f4CameraPos;
uniform float4x4 g_ViewProjInv;
uniform float4x4 g_WorldViewProj;
uniform float4 g_f4ViewFrustumPlanes[6];
#endif

#if 0
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
#endif

void GetRaySphereIntersection2(in float3 f3RayOrigin,
                               in float3 f3RayDirection,
                               in float3 f3SphereCenter,
                               in float2 f2SphereRadius,
                               out float4 f4Intersections)
{
    // http://wiki.cgsociety.org/index.php/Ray_Sphere_Intersection
    f3RayOrigin -= f3SphereCenter;
    float A = dot(f3RayDirection, f3RayDirection);
    float B = 2.0 * dot(f3RayOrigin, f3RayDirection);
    float2 C = dot(f3RayOrigin,f3RayOrigin) - f2SphereRadius*f2SphereRadius;
    float2 D = B*B - 4.0*A*C;
    // If discriminant is negative, there are no real roots hence the ray misses the
    // sphere
	float2 f2RealRootMask = float2(greaterThanEqual(D.xy, float2(0)));
    D = sqrt( max(D,0) );
    f4Intersections =   f2RealRootMask.xxyy * float4(-B - D.x, -B + D.x, -B - D.y, -B + D.y) / (2*A) +
                      (1-f2RealRootMask.xxyy) * float4(-1,-2,-1,-2);
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
    float2 f2LODs = log2(max(f2dU, float2(1)));
    return f2LODs;
}

float4 GetCloudDensityUV(in float3 CloudPosition, in float fTime)
{
    float4 f2Offset01 = float4( 0.1*float2(-0.04, +0.01) * fTime, 0.2*float2( 0.01,  0.04) * fTime );
    float4 f2UV01 = CloudPosition.xzxz * g_f2CloudDensitySamplingScale.xxyy + f2Offset01;
    return f2UV01;
}

float GetCloudDensity(in float4 f4UV01, in float2 f2LODs /*= float2(0,0)*/)
{
    float fDensity =
//        g_tex2DCloudDensity.SampleLevel(samLinearWrap, f4UV01.xy, f2LODs.x) *
//        g_tex2DCloudDensity.SampleLevel(samLinearWrap, f4UV01.zw, f2LODs.y);
        textureLod(g_tex2DCloudDensity, f4UV01.xy, f2LODs.x).x *
        textureLod(g_tex2DCloudDensity, f4UV01.zw, f2LODs.y).x;

    fDensity = saturate((fDensity-g_GlobalCloudAttribs.fCloudDensityThreshold)/(1.0-g_GlobalCloudAttribs.fCloudDensityThreshold));

    return fDensity;
}

float GetCloudDensityAutoLOD(in float4 f4UV01)
{
    float fDensity =
//        g_tex2DCloudDensity.Sample(samLinearWrap, f4UV01.xy) *
//        g_tex2DCloudDensity.Sample(samLinearWrap, f4UV01.zw);
        texture(g_tex2DCloudDensity, f4UV01.xy).x *
        texture(g_tex2DCloudDensity, f4UV01.zw).x;

    fDensity = saturate((fDensity-g_GlobalCloudAttribs.fCloudDensityThreshold)/(1.0-g_GlobalCloudAttribs.fCloudDensityThreshold));

    return fDensity;
}

float GetCloudDensity(in float3 CloudPosition, in float fTime, in float2 f2LODs = float2(0,0))
{
    float4 f4UV01 = GetCloudDensityUV(CloudPosition, fTime);
    return GetCloudDensity(f4UV01, f2LODs);
}

float GetMaxDensity(in float4 f4UV01, in float2 f2LODs /*= float2(0,0)*/)
{
    float fDensity =
//        g_tex2MaxDensityMip.SampleLevel(samPointWrap, f4UV01.xy, f2LODs.x) *
//        g_tex2MaxDensityMip.SampleLevel(samPointWrap, f4UV01.zw, f2LODs.y);
        textureLod(g_tex2MaxDensityMip, f4UV01.xy, f2LODs.x).x *
        textureLod(g_tex2MaxDensityMip, f4UV01.zw, f2LODs.y).x;

    fDensity = saturate((fDensity-g_GlobalCloudAttribs.fCloudDensityThreshold)/(1-g_GlobalCloudAttribs.fCloudDensityThreshold));

    return fDensity;
}

float GetMaxDensity(in float3 CloudPosition, in float fTime, in float2 f2LODs /*= float2(0,0)*/)
{
    float4 f4UV01 = GetCloudDensityUV(CloudPosition, fTime);
    return GetMaxDensity(f4UV01, f2LODs);
}

// This function computes visibility for the particle
bool IsParticleVisibile(in float3 f3Center, in float3 f3Scales, float4 f4ViewFrustumPlanes[6])
{
    float fParticleBoundSphereRadius = length(f3Scales);
    bool bIsVisible = true;
    for(int iPlane = 0; iPlane < 6; ++iPlane)
    {
//#if LIGHT_SPACE_PASS
//        // Do not clip against far clipping plane for light pass
//        if( iPlane == 5 )
//            continue;
//#endif
        float4 f4CurrPlane = f4ViewFrustumPlanes[iPlane];
#if 1
        // Note that the plane normal is not normalized to 1
        float DMax = dot(f3Center.xyz, f4CurrPlane.xyz) + f4CurrPlane.w + fParticleBoundSphereRadius*length(f4CurrPlane.xyz);
#else
        // This is a bit more accurate but significantly more computationally expensive test
        float DMax = -FLT_MAX;
        for(uint uiCorner=0; uiCorner < 8; ++uiCorner)
        {
            float4 f4CurrCornerWS = ParticleAttrs.f4BoundBoxCornersWS[uiCorner];
            float D = dot( f4CurrCornerWS.xyz, f4CurrPlane.xyz) + f4CurrPlane.w;
            DMax = max(DMax, D);
        }
#endif
        if( DMax < 0 )
        {
            bIsVisible = false;
//            return false;
        }
    }
    return bIsVisible;
}

#if 1
bool VolumeProcessingCSHelperFunc(uint3 Gid, uint3 GTid,
								  out SCloudCellAttribs CellAttrs,
							      out uint uiLayer,
								  out uint uiRing,
								  out float fLayerAltitude,
								  out float3 f3VoxelCenter,
								  out uint3 DstCellInd)
{
	uint uiThreadID = Gid.x * THREAD_GROUP_SIZE + GTid.x;
	uint s = g_GlobalCloudAttribs.uiDensityBufferScale;
	uint uiCellNum = uiThreadID / (s*s*s * g_GlobalCloudAttribs.uiMaxLayers);
    uint uiNumValidCells = imageLoad(g_ValidCellsCounter, 0).x;
    if( uiCellNum >= uiNumValidCells )
        return false;

	// Load valid cell id from the list
    uint uiCellId = g_ValidCellsUnorderedList[uiCellNum];
    // Get the cell attributes
    CellAttrs = g_CloudCells[uiCellId];
	uint uiTmp = uiThreadID;
	uint uiSubCellX = uiTmp % s; uiTmp /= s;
	uint uiSubCellY = uiTmp % s; uiTmp /= s;
	uint uiSubCellZ = uiTmp % s; uiTmp /= s;
	uiLayer = uiTmp % g_GlobalCloudAttribs.uiMaxLayers;

    uint uiCellI, uiCellJ, uiLayerUnused;
	// For cells, layer index is always 0
    UnPackParticleIJRing(CellAttrs.uiPackedLocation, uiCellI, uiCellJ, uiRing, uiLayerUnused);

	DstCellInd.x = uiCellI*s + uiSubCellX;
	DstCellInd.y = uiCellJ*s + uiSubCellY;
	DstCellInd.z = g_GlobalCloudAttribs.uiMaxLayers*s*uiRing + uiLayer*s + uiSubCellZ;

	fLayerAltitude = (float(uiLayer) + 0.5) / float(g_GlobalCloudAttribs.uiMaxLayers) - 0.5;
	f3VoxelCenter = CellAttrs.f3Center + CellAttrs.f3Normal.xyz * fLayerAltitude * g_GlobalCloudAttribs.fCloudThickness;

	return true;
}

float GetCloudRingWorldStep(uint uiRing/*, SGlobalCloudAttribs g_GlobalCloudAttribs*/)
{
    const float fLargestRingSize = g_GlobalCloudAttribs.fParticleCutOffDist * 2;
    uint uiRingDimension = g_GlobalCloudAttribs.uiRingDimension;
    uint uiNumRings = g_GlobalCloudAttribs.uiNumRings;
    float fRingWorldStep = fLargestRingSize / float((uiRingDimension) << ((uiNumRings-1) - uiRing));
    return fRingWorldStep;
}

float GetCloudRingWorldStep(uint uiRing, SGlobalCloudAttribs g_GlobalCloudAttribs)
{
    const float fLargestRingSize = g_GlobalCloudAttribs.fParticleCutOffDist * 2;
    uint uiRingDimension = g_GlobalCloudAttribs.uiRingDimension;
    uint uiNumRings = g_GlobalCloudAttribs.uiNumRings;
    float fRingWorldStep = fLargestRingSize / float((uiRingDimension) << ((uiNumRings-1) - uiRing));
    return fRingWorldStep;
}


float SampleCellAttribs3DTexture(sampler3D tex3DData, in float3 f3WorldPos, in uint uiRing, /*uniform*/ bool bAutoLOD )
{
    float3 f3EarthCentre = float3(0, -g_fEarthRadius, 0);
    float3 f3DirFromEarthCenter = f3WorldPos - f3EarthCentre;
    float fDistFromCenter = length(f3DirFromEarthCenter);
	//Reproject to y=0 plane
    float3 f3CellPosFlat = f3EarthCentre + f3DirFromEarthCenter / f3DirFromEarthCenter.y * g_fEarthRadius;
    float3 f3CellPosSphere = f3EarthCentre + f3DirFromEarthCenter * ((g_fEarthRadius + g_GlobalCloudAttribs.fCloudAltitude)/fDistFromCenter);
	float3 f3Normal = f3DirFromEarthCenter / fDistFromCenter;
	float fCloudAltitude = dot(f3WorldPos - f3CellPosSphere, f3Normal);

    // Compute cell center world space coordinates
    const float fRingWorldStep = GetCloudRingWorldStep(uiRing/*, g_GlobalCloudAttribs*/);

    //
    //
    //                                 Camera
    //                               |<----->|
    //   |   X   |   X   |   X   |   X   |   X   |   X   |   X   |   X   |       CameraI == 4
    //   0  0.5     1.5     2.5     3.5  4  4.5     5.5     6.5     7.5  8       uiRingDimension == 8
    //                                   |
    //                                CameraI
    float fCameraI = floor(g_f4CameraPos.x/fRingWorldStep + 0.5);
    float fCameraJ = floor(g_f4CameraPos.z/fRingWorldStep + 0.5);

	uint uiRingDimension = g_GlobalCloudAttribs.uiRingDimension;
    float fCellI = f3CellPosFlat.x / fRingWorldStep - fCameraI + (uiRingDimension/2);
    float fCellJ = f3CellPosFlat.z / fRingWorldStep - fCameraJ + (uiRingDimension/2);
	float fU = fCellI / float(uiRingDimension);
	float fV = fCellJ / float(uiRingDimension);
	float fW0 = float(uiRing)	  / float(g_GlobalCloudAttribs.uiNumRings);
	float fW1 = float(uiRing+1) / float(g_GlobalCloudAttribs.uiNumRings);
	float fW = fW0 + ( (fCloudAltitude + g_GlobalCloudAttribs.fCloudThickness*0.5) / g_GlobalCloudAttribs.fCloudThickness) / float(g_GlobalCloudAttribs.uiNumRings);

	float Width,Height,Depth;
//	tex3DData.GetDimensions(Width,Height,Depth);
    int3 i3Size = textureSize(tex3DData, 0);
    Width = float(i3Size.x);
    Height = float(i3Size.y);
    Depth = float(i3Size.z);

	fW = clamp(fW, fW0 + 0.5/Depth, fW1 - 0.5/Depth);
	return bAutoLOD ?
			texture(tex3DData, float3(fU, fV, fW)).x :    // samLinearClamp
			textureLod(tex3DData, float3(fU, fV, fW), 0.0).x;
}
#endif

float3 GetParticleScales(in float fSize, in float fNumActiveLayers)
{
    float3 f3Scales = float3(fSize);
    //if( fNumActiveLayers > 1 )
    //    f3Scales.y = max(f3Scales.y, g_GlobalCloudAttribs.fCloudThickness/fNumActiveLayers);
    f3Scales.y = min(f3Scales.y, g_GlobalCloudAttribs.fCloudThickness/2.f);
    return f3Scales;
}

void GetSunLightExtinctionAndSkyLight(in float3 f3PosWS,
                                      out float3 f3Extinction,
                                      out float3 f3AmbientSkyLight,
                                      sampler2D tex2DOccludedNetDensityToAtmTop,
                                      sampler2D tex2DAmbientSkylight )
{
    float3 f3EarthCentre = float3(0, -g_fEarthRadius, 0);
    float3 f3DirFromEarthCentre = f3PosWS - f3EarthCentre;
    float fDistToCentre = length(f3DirFromEarthCentre);
    f3DirFromEarthCentre /= fDistToCentre;
    float fHeightAboveSurface = fDistToCentre - g_fEarthRadius;
    float fCosZenithAngle = dot(f3DirFromEarthCentre, g_f4DirOnLight.xyz);

    float fRelativeHeightAboveSurface = fHeightAboveSurface / g_fAtmTopHeight;
    float2 f2ParticleDensityToAtmTop = textureLod(tex2DOccludedNetDensityToAtmTop, float2(fRelativeHeightAboveSurface, fCosZenithAngle*0.5+0.5), 0.0).xy; // samLinearClamp

    float3 f3RlghOpticalDepth = g_f4RayleighExtinctionCoeff.rgb * f2ParticleDensityToAtmTop.x;
    float3 f3MieOpticalDepth  = g_f4MieExtinctionCoeff.rgb      * f2ParticleDensityToAtmTop.y;

    // And total extinction for the current integration point:
    f3Extinction = exp( -(f3RlghOpticalDepth + f3MieOpticalDepth) );

    f3AmbientSkyLight = textureLod(tex2DAmbientSkylight, float2(fCosZenithAngle*0.5+0.5, 0.5), 0.0).rgb;  // samLinearClamp
}