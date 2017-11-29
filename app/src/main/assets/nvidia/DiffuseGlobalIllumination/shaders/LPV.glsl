// Copyright (c) 2011 NVIDIA Corporation. All rights reserved.
//
// TO  THE MAXIMUM  EXTENT PERMITTED  BY APPLICABLE  LAW, THIS SOFTWARE  IS PROVIDED
// *AS IS*  AND NVIDIA AND  ITS SUPPLIERS DISCLAIM  ALL WARRANTIES,  EITHER  EXPRESS
// OR IMPLIED, INCLUDING, BUT NOT LIMITED  TO, NONINFRINGEMENT,IMPLIED WARRANTIES OF
// MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.  IN NO EVENT SHALL  NVIDIA
// OR ITS SUPPLIERS BE  LIABLE  FOR  ANY  DIRECT, SPECIAL,  INCIDENTAL,  INDIRECT,  OR
// CONSEQUENTIAL DAMAGES WHATSOEVER (INCLUDING, WITHOUT LIMITATION,  DAMAGES FOR LOSS
// OF BUSINESS PROFITS, BUSINESS INTERRUPTION, LOSS OF BUSINESS INFORMATION, OR ANY
// OTHER PECUNIARY LOSS) ARISING OUT OF THE  USE OF OR INABILITY  TO USE THIS SOFTWARE,
// EVEN IF NVIDIA HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
//
// Please direct any bugs or questions to SDKFeedback@nvidia.com


#include "Defines.glsl"

#if 0
//textures for initializing the LPV
Texture2D   g_txRSMColor                        : register( t0 );
Texture2D   g_txRSMNormal                       : register( t1 );
Texture2D   g_txRSMDepth                        : register( t2 );

Texture2DArray   g_txLPVRed                     : register( t3 );
Texture2DArray   g_txLPVGreen                   : register( t4 );
Texture2DArray   g_txLPVBlue                    : register( t5 );
RWTexture2DArray<float4>    g_uavLPVRed         : register( u0 );
RWTexture2DArray<float4>    g_uavLPVGreen       : register( u1 );
RWTexture2DArray<float4>    g_uavLPVBlue        : register( u2 );
#else
layout(binding = 0) uniform sampler2D g_txRSMColor;
layout(binding = 1) uniform sampler2D g_txRSMNormal;
layout(binding = 2) uniform sampler2D g_txRSMDepth;

layout(binding = 3) uniform sampler2DArray g_txLPVRed;
layout(binding = 4) uniform sampler2DArray g_txLPVGreen;
layout(binding = 5) uniform sampler2DArray g_txLPVBlue;

layout(rgba8, binding = 0) uniform image2DArray g_uavLPVRed;
layout(rgba8, binding = 1) uniform image2DArray g_uavLPVGreen;
layout(rgba8, binding = 2) uniform image2DArray g_uavLPVBlue;
#endif


#if 0
cbuffer cbConstantsLPVinitialize : register( b0 )
{
    int RSMWidth          : packoffset(c0.x);
    int RSMHeight         : packoffset(c0.y);
    float gLightScale     : packoffset(c0.z);
    float temp7           : packoffset(c0.w);

    matrix  g_mInvProj    : packoffset( c1 );
}
#else
layout(std430, binding = 0) uniform cbConstantsLPVinitialize
{
    int RSMWidth;
    int RSMHeight;
    float gLightScale;
    float temp7;
    float4x4 g_mInvProj;
}

#endif

#if 0
cbuffer cbConstantsLPVinitialize3 : register( b5 )
{
    int g_numCols         : packoffset(c0.x);    //the number of columns in the flattened 2D LPV
    int g_numRows         : packoffset(c0.y);      //the number of columns in the flattened 2D LPV
    int LPV2DWidth        : packoffset(c0.z);   //the total width of the flattened 2D LPV
    int LPV2DHeight       : packoffset(c0.w);   //the total height of the flattened 2D LPV

    int LPV3DWidth        : packoffset(c1.x);      //the width of the LPV in 3D
    int LPV3DHeight       : packoffset(c1.y);     //the height of the LPV in 3D
    int LPV3DDepth        : packoffset(c1.z);
    float g_fluxWeight    : packoffset(c1.w);

    int g_useFluxWeight   : packoffset(c2.x); //flux weight only needed for perspective light matrix not orthogonal
    float padding0        : packoffset(c2.y);
    float padding1        : packoffset(c2.z);
    float padding2        : packoffset(c2.w);

}
#else
layout(std430,binding = 5) uniform cbConstantsLPVinitialize3
{
    int g_numCols;
    int g_numRows;
    int LPV2DWidth;
    int LPV2DHeight;

    int LPV3DWidth;
    int LPV3DHeight;
    int LPV3DDepth;
    float g_fluxWeight;

    int g_useFluxWeight;
}
#endif

#if 0
cbuffer cbConstantsLPVinitialize2 : register( b1 )
{
    matrix  g_ViewToLPV        : packoffset( c0 );
    matrix  g_LPVtoView        : packoffset( c4 );
    float3 lightDirGridSpace   : packoffset( c8 );   //light direction in the grid's space
    float displacement         : packoffset( c8.w );   //amount to displace the VPL in the direction of the cell normal
}
#else
layout(std430,binding = 5) uniform cbConstantsLPVinitialize2
{
    float4x4 g_ViewToLPV;
    float4x4 g_LPVtoView;
    float3 lightDirGridSpace;
    float displacement;
}
#endif

struct propagateValues
{
    float4 neighborOffsets;
    float solidAngle;
    float x;
    float y;
    float z;
};

//these precomputed arrays let us save time on the propapation step
//only the first 30 values from each array are used (the other two are used for alignment/padding)
//xyz give the normalized direction to each of the 5 pertinent faces of the 6 neighbors of a cube
//solid angle gives the fractional solid angle that that face subtends
layout(std430,binding = 2) uniform cbLPVPropagateGather // : register( b2 )
{
    propagateValues g_propagateValues[32];
};

layout(std430,binding = 3) uniform cbGV //: register( b3 )
{
    int g_useGVOcclusion;
    int temp;
    int g_useMultipleBounces;
    int g_copyPropToAccum;
};


layout(std430,binding = 6) uniform cbConstantsLPVReconstruct //: register( b6 )
{
    float farClip     ;
    float temp2       ;
    float temp3       ;
    float temp4       ;
};

layout(std430,binding = 7) uniform cbConstantsLPVReconstruct2 //: register( b7 )
{
    float4x4  g_InverseProjection    /*: packoffset( c0 )*/;
};


/*
SamplerState samLinear : register( s1 )
{
    Filter = MIN_MAG_LINEAR_MIP_POINT;
};*/

//--------------------------------------------------------
//helper routines for SH
//--------------------------------------------------------
#ifndef PI
#define PI 3.14159265
#endif

float SH(int l, int m, float3 xyz)
{
    if(l==0) return 0.282094792;//1/(2sqrt(PI);
    if(l==1 && m==-1) return -0.4886025119*xyz.y;
    if(l==1 && m==0)  return  0.4886025119*xyz.z;
    if(l==1 && m==1)  return -0.4886025119*xyz.x;
    return 0.0;
}

//polynomial form of SH basis functions
void SH(float3 xyz, inout float4 SHCoefficients)
{
    SHCoefficients.x =  0.282094792;         // 1/(2*sqrt(PI))     : l=0, m=0
    SHCoefficients.y = -0.4886025119*xyz.y;  // - sqrt(3/(4*PI))*y : l=1, m=-1
    SHCoefficients.z =  0.4886025119*xyz.z;  //   sqrt(3/(4*PI))*z : l=1, m=0
    SHCoefficients.w = -0.4886025119*xyz.x;  // - sqrt(3/(4*PI))*x : l=1, m=1

    //sqrt(15)*y*x/(2*sqrt(PI))        : l=2, m=-2
    //-sqrt(15)*y*z/(2*sqrt(PI))       : l=2, m=-1
    //sqrt(5)*(3*z*z-1)/(4*sqrt(PI))   : l=2, m=0
    //-sqrt(15)*x*z/(2*sqrt(PI))       : l=2, m=1
    //sqrt(15)*(x*x-y*y)/(4*sqrt(PI))  : l=2, m=2
}

//clamped cosine oriented in z direction expressed in zonal harmonics and rotated to direction 'd'
//   zonal harmonics of clamped cosine in z:
//     l=0 : A0 = 0.5*sqrt(PI)
//     l=1 : A1 = sqrt(PI/3)
//     l=2 : A2 = (PI/4)*sqrt(5/(4*PI))
//     l=3 : A3 = 0
//   to rotate zonal harmonics in direction 'd' : sqrt( (4*PI)/(2*l+1)) * zl * SH coefficients in direction 'd'
//     l=0 : PI * SH coefficients in direction 'd'
//     l=1 : 2*PI/3 * SH coefficients in direction 'd'
//     l=2 : PI/4 * SH coefficients in direction 'd'
//     l=3 : 0
void clampledCosineCoeff(float3 xyz, inout float c0, inout float c1, inout float c2, inout float c3)
{
    c0 = PI * 0.282094792;
    c1 = ((2.0*PI)/3.0f) * -0.4886025119 * xyz.y;
    c2 = ((2.0*PI)/3.0f) *  0.4886025119 * xyz.z;
    c3 = ((2.0*PI)/3.0f) * -0.4886025119 * xyz.x;
}

float innerProductSH(float4 SH1, float4 SH2)
{
    return SH1.x*SH2.x + SH1.y*SH2.y + SH1.z*SH2.z + SH1.w*SH2.w;
}


//--------------------------------------------------------
//shaders
//--------------------------------------------------------

#define NORMAL_DEPTH_BIAS (0.5f)
#define LIGHT_DEPTH_BIAS (0.5f)
