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
Texture2DArray   g_txLPVRed                          : register( t3 );
Texture2DArray   g_txLPVGreen                        : register( t4 );
Texture2DArray    g_txLPVBlue                        : register( t5 );
Texture2DArray    g_txGV                             : register( t6 );
Texture2DArray    g_txGVColor                        : register( t7 );
Texture2DArray    g_txAccumulateRed                  : register( t8 );
Texture2DArray    g_txAccumulateGreen                : register( t9 );
Texture2DArray    g_txAccumulateBlue                 : register( t10 );
RWTexture2DArray<float4>    g_uavLPVRed              : register( u0 );
RWTexture2DArray<float4>    g_uavLPVGreen            : register( u1 );
RWTexture2DArray<float4>    g_uavLPVBlue             : register( u2 );
RWTexture2DArray<float4>    g_uavLPVRedAccum         : register( u3 );
RWTexture2DArray<float4>    g_uavLPVGreenAccum       : register( u4 );
RWTexture2DArray<float4>    g_uavLPVBlueAccum        : register( u5 );
#else
layout(binding = 0) uniform sampler2DArray g_txLPVRed;
layout(binding = 1) uniform sampler2DArray g_txLPVGreen;
layout(binding = 2) uniform sampler2DArray g_txLPVBlue;
layout(binding = 3) uniform sampler2DArray g_txGV;
layout(binding = 4) uniform sampler2DArray g_txGVColor;
layout(binding = 5) uniform sampler2DArray g_txAccumulateRed;
layout(binding = 6) uniform sampler2DArray g_txAccumulateGreen;
layout(binding = 7) uniform sampler2DArray g_txAccumulateBlue;

layout(rgba8, binding = 0) uniform image2DArray  g_uavLPVRed;
layout(rgba8, binding = 1) uniform image2DArray  g_uavLPVGreen;
layout(rgba8, binding = 2) uniform image2DArray  g_uavLPVBlue;
layout(rgba8, binding = 3) uniform image2DArray  g_uavLPVRedAccum;
layout(rgba8, binding = 4) uniform image2DArray  g_uavLPVGreenAccum;
layout(rgba8, binding = 5) uniform image2DArray  g_uavLPVBlueAccum;
#endif

layout(std430, binding = 0) uniform cbConstantsLPVinitialize/* : register( b0 )*/
{
    int RSMWidth         /*: packoffset(c0.x)*/;
    int RSMHeight        /*: packoffset(c0.y)*/;
    float tan_FovXhalf   /*: packoffset(c0.z)*/;
    float tan_FovYhalf   /*: packoffset(c0.w)*/;
};

layout(std430, binding = 5) uniform cbConstantsLPVinitialize3 // : register( b5 )
{
    int g_numCols        /*: packoffset(c0.x)*/;    //the number of columns in the flattened 2D LPV
    int g_numRows        /*: packoffset(c0.y)*/;      //the number of columns in the flattened 2D LPV
    int LPV2DWidth       /*: packoffset(c0.z)*/;   //the total width of the flattened 2D LPV
    int LPV2DHeight      /*: packoffset(c0.w)*/;   //the total height of the flattened 2D LPV

    int LPV3DWidth       /*: packoffset(c1.x)*/;     //the width of the LPV in 3D
    int LPV3DHeight      /*: packoffset(c1.y)*/;     //the height of the LPV in 3D
    int LPV3DDepth       /*: packoffset(c1.z)*/;        //the depth of the LPV in 3D
    int g_Accumulate     /*: packoffset(c1.w)*/;     //bool to choose whether to accumulate the propagated wavefront into the accumulation buffer
};



layout(std430, binding = 1) uniform cbConstantsLPVinitialize2// : register( b1 )
{
    float4x4  g_ViewToLPV        /*: packoffset( c0 )*/;
    float4x4  g_LPVtoView        /*: packoffset( c4 )*/;
    float displacement         /*: packoffset( c8.x )*/;   //amount to displace the VPL in the direction of the cell normal
};


struct propagateValues
{
    float4 neighborOffsets;
    float solidAngle;
    float x;
    float y;
    float z;
};

struct propagateValues2
{
    int occlusionOffsetX;
    int occlusionOffsetY;
    int occlusionOffsetZ;
    int occlusionOffsetW;

    int multiBounceOffsetX;
    int multiBounceOffsetY;
    int multiBounceOffsetZ;
    int multiBounceOffsetW;
};

//these precomputed arrays let us save time on the propapation step
//only the first 30 values from each array are used (the other two are used for alignment/padding)
//xyz give the normalized direction to each of the 5 pertinent faces of the 6 neighbors of a cube
//solid angle gives the fractional solid angle that that face subtends
layout(std430, binding = 2) uniform cbLPVPropagateGather// : register( b2 )
{
    propagateValues g_propagateValues[36];
};

layout(std430, binding = 6) uniform cbLPVPropagateGather2 //: register( b6 )
{
    propagateValues2 g_propagateValues2[8];
};


layout(std430, binding = 3) uniform cbGV //: register( b3 )
{
    int g_useGVOcclusion;
    int temp;
    int g_useMultipleBounces;
    int g_copyPropToAccum;

    float fluxAmplifier;//4
    float reflectedLightAmplifier; //4.8
    float occlusionAmplifier; //0.8f
    int temp2;
};


struct initLPV_VSOUT
{
    float4 pos : POSITION; // 2D slice vertex coordinates in homogenous clip space
    float3 normal : NORMAL;
    float3 color : COLOR;
};

struct initLPV_GSOUT
{
    float4 pos : SV_Position; // 2D slice vertex coordinates in homogenous clip space
    float3 normal : NORMAL;
    float3 color : COLOR;
    uint RTIndex : SV_RenderTargetArrayIndex;  // used to choose the destination slice
};




//--------------------------------------------------------
//helper routines for SH
//--------------------------------------------------------
#ifndef PI
#define PI 3.14159265
#endif

void SH(float3 xyz, inout float4 SHCoefficients)
{
    SHCoefficients.x =  0.282094792;
    SHCoefficients.y = -0.4886025119*xyz.y;
    SHCoefficients.z =  0.4886025119*xyz.z;
    SHCoefficients.w = -0.4886025119*xyz.x;
}

float4 clampledCosineCoeff(float3 xyz)
{
    float4 c;
    c.x = PI * 0.282094792;
    c.y = ((2.0*PI)/3.0f) * -0.4886025119 * xyz.y;
    c.z = ((2.0*PI)/3.0f) *  0.4886025119 * xyz.z;
    c.w = ((2.0*PI)/3.0f) * -0.4886025119 * xyz.x;
    return c;
}

float innerProductSH(float4 SH1, float4 SH2)
{
    return SH1.x*SH2.x + SH1.y*SH2.y + SH1.z*SH2.z + SH1.w*SH2.w;
}


//----------------------------------------------------------
//helper function for loading texture
//----------------------------------------------------------

float4 loadOffsetTexValue(sampler2DArray tex, uint3 pos)
{
    if(pos.x >= LPV3DWidth  || pos.x < 1) return float4(0,0,0,0);
    if(pos.y >= LPV3DHeight || pos.y < 1) return float4(0,0,0,0);
    if(pos.z >= LPV3DDepth  || pos.z < 1) return float4(0,0,0,0);

//    return tex.Load(int4(pos.x,pos.y,pos.z,0));
    return texelFetch(tex, int3(pos), 0);
}

void loadOffsetTexValues(sampler2DArray tex1, sampler2DArray tex2, sampler2DArray tex3,
uint3 pos, inout float4 val1, inout float4 val2, inout float4 val3 )
{
    if(pos.x >= LPV3DWidth  || pos.x < 0) return;
    if(pos.y >= LPV3DHeight || pos.y < 0) return;
    if(pos.z >= LPV3DDepth  || pos.z < 0) return;

    val1 = /*tex1.Load(int4(pos.x,pos.y,pos.z,0))*/texelFetch(tex1, int3(pos), 0);
    val2 = /*tex2.Load(int4(pos.x,pos.y,pos.z,0))*/texelFetch(tex2, int3(pos), 0);
    val3 = /*tex3.Load(int4(pos.x,pos.y,pos.z,0))*/texelFetch(tex3, int3(pos), 0);
}
