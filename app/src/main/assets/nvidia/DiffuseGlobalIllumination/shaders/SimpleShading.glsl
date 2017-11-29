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

layout(binding = 0) uniform sampler2D   g_txDiffuse                         /*: register( t0 )*/;
layout(binding = 1) uniform sampler2D   g_shadowMap                         /*: register( t1 )*/;
layout(binding = 39) uniform sampler2D   g_txNormalMap                       /*: register( t39 )*/;
layout(binding = 40) uniform sampler2D   g_txAlpha                           /*: register( t40 )*/;

layout(binding = 2) uniform sampler2DArray   g_propagatedLPVRed             /*: register( t2 )*/; //used for the first level of the cascade, or for the hierarchy
layout(binding = 3) uniform sampler2DArray   g_propagatedLPVGreen           /*: register( t3 )*/; //used for the first level of the cascade, or for the hierarchy
layout(binding = 4) uniform sampler2DArray   g_propagatedLPVBlue            /*: register( t4 )*/; //used for the first level of the cascade, or for the hierarchy
layout(binding = 5) uniform sampler2DArray   g_propagatedLPV2Red            /*: register( t5 )*/; //used for the second level of the cascade
layout(binding = 6) uniform sampler2DArray   g_propagatedLPV2Green          /*: register( t6 )*/; //used for the second level of the cascade
layout(binding = 7) uniform sampler2DArray   g_propagatedLPV2Blue           /*: register( t7 )*/; //used for the second level of the cascade
layout(binding = 8) uniform sampler2DArray   g_propagatedLPV3Red            /*: register( t8 )*/; //used for the third level of the cascade
layout(binding = 9) uniform sampler2DArray   g_propagatedLPV3Green          /*: register( t9 )*/; //used for the third level of the cascade
layout(binding = 10) uniform sampler2DArray   g_propagatedLPV3Blue           /*: register( t10 )*/; //used for the third level of the cascade

layout(binding = 11) uniform sampler2DArray   g_LPVTex                       /*: register( t11 )*/;

//auxiliary textures incase we are binding float textures instead of float4 textures
layout(binding = 12) uniform sampler2DArray   g_propagatedLPVRed_1           /*: register( t12 )*/; //used for the first level of the cascade, or for the hierarchy
layout(binding = 13) uniform sampler2DArray   g_propagatedLPVGreen_1         /*: register( t13 )*/; //used for the first level of the cascade, or for the hierarchy
layout(binding = 14) uniform sampler2DArray   g_propagatedLPVBlue_1          /*: register( t14 )*/; //used for the first level of the cascade, or for the hierarchy
layout(binding = 15) uniform sampler2DArray   g_propagatedLPV2Red_1          /*: register( t15 )*/; //used for the second level of the cascade
layout(binding = 16) uniform sampler2DArray   g_propagatedLPV2Green_1        /*: register( t16 )*/; //used for the second level of the cascade
layout(binding = 17) uniform sampler2DArray   g_propagatedLPV2Blue_1         /*: register( t17 )*/; //used for the second level of the cascade
layout(binding = 18) uniform sampler2DArray   g_propagatedLPV3Red_1          /*: register( t18 )*/; //used for the third level of the cascade
layout(binding = 19) uniform sampler2DArray   g_propagatedLPV3Green_1        /*: register( t19 )*/; //used for the third level of the cascade
layout(binding = 20) uniform sampler2DArray   g_propagatedLPV3Blue_1         /*: register( t20 )*/; //used for the third level of the cascade

layout(binding = 21) uniform sampler2DArray   g_propagatedLPVRed_2           /*: register( t21 )*/; //used for the first level of the cascade, or for the hierarchy
layout(binding = 22) uniform sampler2DArray   g_propagatedLPVGreen_2         /*: register( t22 )*/; //used for the first level of the cascade, or for the hierarchy
layout(binding = 23) uniform sampler2DArray   g_propagatedLPVBlue_2          /*: register( t23 )*/; //used for the first level of the cascade, or for the hierarchy
layout(binding = 24) uniform sampler2DArray   g_propagatedLPV2Red_2          /*: register( t24 )*/; //used for the second level of the cascade
layout(binding = 25) uniform sampler2DArray   g_propagatedLPV2Green_2        /*: register( t25 )*/; //used for the second level of the cascade
layout(binding = 26) uniform sampler2DArray   g_propagatedLPV2Blue_2         /*: register( t26 )*/; //used for the second level of the cascade
layout(binding = 27) uniform sampler2DArray   g_propagatedLPV3Red_2          /*: register( t27 )*/; //used for the third level of the cascade
layout(binding = 28) uniform sampler2DArray   g_propagatedLPV3Green_2        /*: register( t28 )*/; //used for the third level of the cascade
layout(binding = 29) uniform sampler2DArray   g_propagatedLPV3Blue_2         /*: register( t29 )*/; //used for the third level of the cascade

layout(binding = 30) uniform sampler2DArray   g_propagatedLPVRed_3           /*: register( t30 )*/; //used for the first level of the cascade, or for the hierarchy
layout(binding = 31) uniform sampler2DArray   g_propagatedLPVGreen_3         /*: register( t31 )*/; //used for the first level of the cascade, or for the hierarchy
layout(binding = 32) uniform sampler2DArray   g_propagatedLPVBlue_3          /*: register( t32 )*/; //used for the first level of the cascade, or for the hierarchy
layout(binding = 33) uniform sampler2DArray   g_propagatedLPV2Red_3          /*: register( t33 )*/; //used for the second level of the cascade
layout(binding = 34) uniform sampler2DArray   g_propagatedLPV2Green_3        /*: register( t34 )*/; //used for the second level of the cascade
layout(binding = 35) uniform sampler2DArray   g_propagatedLPV2Blue_3         /*: register( t35 )*/; //used for the second level of the cascade
layout(binding = 36) uniform sampler2DArray   g_propagatedLPV3Red_3          /*: register( t36 )*/; //used for the third level of the cascade
layout(binding = 37) uniform sampler2DArray   g_propagatedLPV3Green_3        /*: register( t37 )*/; //used for the third level of the cascade
layout(binding = 38) uniform sampler2DArray   g_propagatedLPV3Blue_3         /*: register( t38 )*/; //used for the third level of the cascade


layout(binding = 6) uniform sampler2D    g_txPrevDepthBuffer                /*: register( t6 )*/;


/*
SamplerState samLinear : register( s0 )
{
    Filter = MIN_MAG_LINEAR_MIP_LINEAR;
};*/

/*SamplerComparisonState PCF_Sampler : register( s1 )
{
    ComparisonFunc = LESS;
    Filter = COMPARISON_MIN_MAG_LINEAR_MIP_POINT;
    AddressU = Border;
    AddressV = Border;
    BorderColor = float4(MAX_LINEAR_DEPTH, 0, 0, 0);
};

SamplerState DepthSampler : register( s2 )
{
    Filter = MIN_MAG_MIP_POINT;
    AddressU = Clamp;
    AddressV = Clamp;
};
SamplerState samAniso : register( s3 )
{
    Filter = MIN_MAG_LINEAR_MIP_LINEAR;
};*/


layout(binding = 0) uniform cbConstants //: register( b0 )
{
    mat4  g_WorldViewProj            /*: packoffset( c0 )*/;
    mat4  g_WorldViewIT            /*: packoffset( c4 )*/;
    mat4  g_World                    /*: packoffset( c8 )*/;
    mat4  g_WorldViewProjClip2Tex /*: packoffset( c12 )*/;
};

layout(binding = 1) uniform cbConstants2 //: register( b1 )
{
    float4 g_lightWorldPos;

    float g_depthBiasFromGUI;
    int g_bUseSM;
    int g_minCascadeMethod;
    int g_numCascadeLevels;
};

layout(binding = 2) uniform cbLPV_Light_index //: register(b2)
{
    float3 g_LPVSpacePos;
    float padding3;
};

layout(binding = 2) uniform cbConstants4 //: register( b4 )
{
    mat4 g_WorldViewProjSimple        /*: packoffset( c0 )*/;
    float4 g_color                      /*: packoffset( c4 )*/;
    float4 temp                         /*: packoffset( c5 )*/;
    float4  g_sphereScale               /*: packoffset( c6 )*/;
};


layout(std430, binding = 5) uniform cbConstantsRenderPS //: register( b5 )
{
    float g_diffuseScale                /*: packoffset(c0.x)*/;  //PS
    int g_useDiffuseInterreflection     /*: packoffset(c0.y)*/;  //PS
    float g_directLight                 /*: packoffset(c0.z)*/;  //PS
    float g_ambientLight                /*: packoffset(c0.w)*/;  //PS

    int g_useFloat4s                    /*: packoffset(c1.x)*/;  //PS
    int g_useFloats                     /*: packoffset(c1.y)*/;  //PS
    float temp2                         /*: packoffset(c1.z)*/;  //PS
    float temp3                         /*: packoffset(c1.w)*/;  //PS

    float invSMSize                           /*: packoffset(c2.x)*/;  //PS
    float g_NormalmapMultiplier               /*: packoffset(c2.y)*/;  //PS
    int g_useDirectionalDerivativeClamping    /*: packoffset(c2.z)*/;  //PS
    float g_directionalDampingAmount    /*: packoffset(c2.w)*/;  //PS

    mat4 g_WorldToLPVSpace            /*: packoffset( c3 )*/;  //PS / VS
    mat4 g_WorldToLPVSpace2           /*: packoffset( c7 )*/;  //PS / VS
    mat4 g_WorldToLPVSpace3           /*: packoffset( c11 )*/;  //PS / VS

    mat4 g_WorldToLPVSpaceRender      /*: packoffset( c15 )*/;  //PS / VS
    mat4 g_WorldToLPVSpaceRender2     /*: packoffset( c19 )*/;  //PS / VS
    mat4 g_WorldToLPVSpaceRender3     /*: packoffset( c23 )*/;  //PS / VS
};

layout(std430, binding = 6) uniform cbConstantsMeshRenderOptions //: register(b6)
{
    int g_useTexture;
    int g_useAlpha;
    float padding8;
    float padding9;
};

layout(std430, binding = 7) uniform cbConstantsRenderPSLPV //: register( b7 )
{
    int g_numCols1          /*: packoffset(c0.x)*/;    //the number of columns in the flattened 2D LPV
    int g_numRows1          /*: packoffset(c0.y)*/;      //the number of columns in the flattened 2D LPV
    int LPV2DWidth1         /*: packoffset(c0.z)*/;   //the total width of the flattened 2D LPV
    int LPV2DHeight1        /*: packoffset(c0.w)*/;   //the total height of the flattened 2D LPV

    int LPV3DWidth1         /*: packoffset(c1.x)*/;      //the width of the LPV in 3D
    int LPV3DHeight1        /*: packoffset(c1.y)*/;     //the height of the LPV in 3D
    int LPV3DDepth1         /*: packoffset(c1.z)*/;
    int   padding4          /*: packoffset(c1.w)*/;
};

layout(std430, binding = 8) uniform cbConstantsRenderPSLPV2 //: register( b8 )
{
    int g_numCols2          /*: packoffset(c0.x)*/;    //the number of columns in the flattened 2D LPV
    int g_numRows2          /*: packoffset(c0.y)*/;      //the number of columns in the flattened 2D LPV
    int LPV2DWidth2         /*: packoffset(c0.z)*/;   //the total width of the flattened 2D LPV
    int LPV2DHeight2        /*: packoffset(c0.w)*/;   //the total height of the flattened 2D LPV

    int LPV3DWidth2         /*: packoffset(c1.x)*/;      //the width of the LPV in 3D
    int LPV3DHeight2        /*: packoffset(c1.y)*/;     //the height of the LPV in 3D
    int LPV3DDepth2         /*: packoffset(c1.z)*/;
    int padding5          /*: packoffset(c1.w)*/;
};

layout(std430, binding = 9) uniform cbPoissonDiskSamples //: register( b9 )
{
    int g_numTaps                             /*: packoffset(c0.x)*/;
    float g_FilterSize                        /*: packoffset(c0.y)*/;
    float padding11                           /*: packoffset(c0.z)*/;
    float padding12                           /*: packoffset(c0.w)*/;
    float4 g_Poisson_samples[MAX_P_SAMPLES]   /*: packoffset(c1.x)*/;
};

//--------------------------------------------------------
//helper routines for SH
//-------------------------------------------------------
#ifndef PI
#define PI 3.14159265
#endif

void clampledCosineCoeff(float3 xyz, inout float4 c)
{
    c.x = PI * 0.282094792;
    c.y = ((2.0*PI)/3.0f) * -0.4886025119 * xyz.y;
    c.z = ((2.0*PI)/3.0f) *  0.4886025119 * xyz.z;
    c.w = ((2.0*PI)/3.0f) * -0.4886025119 * xyz.x;
}

float innerProductSH(float4 SH1, float4 SH2)
{
    return SH1.x*SH2.x + SH1.y*SH2.y + SH1.z*SH2.z + SH1.w*SH2.w;
}

float SHLength(float4 SH1, float4 SH2)
{
    return sqrt(innerProductSH( SH1, SH2));
}

float innerProductSH(float3 SH1, float3 SH2)
{
    return SH1.x*SH2.x + SH1.y*SH2.y + SH1.z*SH2.z;
}

void SHMul(inout float4 shVal, float mul)
{
    shVal.x *= mul;
    shVal.y *= mul;
    shVal.z *= mul;
    shVal.w *= mul;
}

float4 SHSub(float4 val1, float4 val2)
{
    return float4(val1.x-val2.x, val1.y-val2.y, val1.z-val2.z, val1.w-val2.w);
}

float4 SHNormalize(float4 SHval)
{
    float ip = SHLength(SHval,SHval);
    if(ip>0.000001)
        return float4(SHval.x/ip, SHval.y/ip, SHval.z/ip, SHval.w/ip );
    return float4(0.0f,0.0f,0.0f,0.0f);
}

float3 SHNormalize(float3 SHval)
{
    float ip = sqrt(innerProductSH(SHval,SHval));
    if(ip>0.000001)
        return float3(SHval.x/ip, SHval.y/ip, SHval.z/ip );
    return float3(0.0f,0.0f,0.0f);
}


//--------------------------------------------------------
//helper routines for PCF
//-------------------------------------------------------

#define PCF_FILTER_STEP_COUNT 2

float poissonPCFmultitap(float2 uv, float mult, float z)
{
    float sum = 0;
    for(int i=0;i<g_numTaps;i++)
    {
        float2 offset = g_Poisson_samples[i].xy * mult;
//        sum += g_shadowMap.SampleCmpLevelZero(PCF_Sampler, uv + offset, z-g_depthBiasFromGUI);
        sum += texture(g_shadowMap, float3(uv + offset, z-g_depthBiasFromGUI));
    }
    return sum /g_numTaps;
}


float regularPCFmultitapFilter(float2 uv, float2 stepUV, float z)
{
        float sum = 0;
        stepUV = stepUV / PCF_FILTER_STEP_COUNT;
        for( float x = -PCF_FILTER_STEP_COUNT; x <= PCF_FILTER_STEP_COUNT; ++x )
            for( float y = -PCF_FILTER_STEP_COUNT; y <= PCF_FILTER_STEP_COUNT; ++y )
            {
                float2 offset = float2( x, y ) * stepUV;
//                sum += g_shadowMap.SampleCmpLevelZero(PCF_Sampler, uv + offset, z-g_depthBiasFromGUI);
                sum += texture(g_shadowMap, float3(uv + offset, z-g_depthBiasFromGUI));
            }
        float numSamples = (PCF_FILTER_STEP_COUNT*2+1);
        return  ( sum / (numSamples*numSamples));
}

float lookupShadow(float2 uv, float z)
{
    //return g_shadowMap.SampleCmpLevelZero(PCF_Sampler, uv, z-g_depthBiasFromGUI);
    //return regularPCFmultitapFilter(uv, g_FilterSize * invSMSize, z);
    return poissonPCFmultitap(uv, g_FilterSize*invSMSize ,z);
}

//trilinear sample from a spatted 2D texture or a 2D texture array
void trilinearSample(inout float4 SHred, inout float4 SHgreen, inout float4 SHblue, float3 lookupCoords,
                    sampler2DArray LPVRed, sampler2DArray LPVGreen, sampler2DArray LPVBlue,
                    int LPV3DDepth)
{

    int zL = floor(lookupCoords.z);
    int zH = min(zL+1, LPV3DDepth-1);
    float zHw = lookupCoords.z - zL;
    float zLw = 1.0f - zHw;
    float3 lookupCoordsLow = float3(lookupCoords.x, lookupCoords.y, zL);
    float3 lookupCoordsHigh = float3(lookupCoords.x, lookupCoords.y, zH);

    SHred = zLw*texture(LPVRed, lookupCoordsLow) + zHw*texture(LPVRed, lookupCoordsHigh );   // samLinear
    SHgreen = zLw*texture(LPVGreen, lookupCoordsLow) + zHw*texture(LPVGreen, lookupCoordsHigh );
    SHblue = zLw*texture(LPVBlue, lookupCoordsLow) + zHw*texture(LPVBlue, lookupCoordsHigh );
}

//trilinear sample from a spatted 2D texture or a 2D texture array
void trilinearSample(inout float4 SHred, inout float4 SHgreen, inout float4 SHblue, float3 lookupCoords,
                    sampler2DArray LPVRed_0,   sampler2DArray LPVRed_1,   sampler2DArray LPVRed_2,   sampler2DArray LPVRed_3,
                    sampler2DArray LPVGreen_0, sampler2DArray LPVGreen_1, sampler2DArray LPVGreen_2, sampler2DArray LPVGreen_3,
                    sampler2DArray LPVBlue_0,  sampler2DArray LPVBlue_1,  sampler2DArray LPVBlue_2,  sampler2DArray LPVBlue_3,
                    int LPV3DDepth)
{
    int zL = floor(lookupCoords.z);
    int zH = min(zL+1,LPV3DDepth-1);
    float zHw = lookupCoords.z - zL;
    float zLw = 1.0f - zHw;
    float3 lookupCoordsLow = float3(lookupCoords.x, lookupCoords.y, zL);
    float3 lookupCoordsHigh = float3(lookupCoords.x, lookupCoords.y, zH);

// samLinear
    SHred =   zLw*float4(texture(LPVRed_0, lookupCoordsLow).x,   texture(LPVRed_1, lookupCoordsLow).x,   texture(LPVRed_2, lookupCoordsLow).x,   texture(LPVRed_3, lookupCoordsLow).x)
            + zHw*float4(texture(LPVRed_0, lookupCoordsHigh).x,   texture(LPVRed_1, lookupCoordsHigh).x, texture(LPVRed_2, lookupCoordsHigh).x,   texture(LPVRed_3, lookupCoordsHigh).x);
    SHgreen = zLw*float4(texture(LPVGreen_0, lookupCoordsLow).x, texture(LPVGreen_1, lookupCoordsLow).x, texture(LPVGreen_2, lookupCoordsLow).x, texture(LPVGreen_3, lookupCoordsLow).x)
            + zHw*float4(texture(LPVGreen_0, lookupCoordsHigh).x, texture(LPVGreen_1, lookupCoordsHigh).x, texture(LPVGreen_2, lookupCoordsHigh).x, texture(LPVGreen_3, lookupCoordsHigh).x);
    SHblue =  zLw*float4(texture(LPVBlue_0, lookupCoordsLow).x,  texture(LPVBlue_1, lookupCoordsLow).x,  texture(LPVBlue_2, lookupCoordsLow).x,  texture(LPVBlue_3, lookupCoordsLow).x)
            + zHw*float4(texture(LPVBlue_0, lookupCoordsHigh).x,  texture(LPVBlue_1, lookupCoordsHigh).x,  texture(LPVBlue_2, lookupCoordsHigh).x,  texture(LPVBlue_3, lookupCoordsHigh).x);

}


float3 lookupSHAux(float3 LPVSpacePos, int numCols, int numRows, int LPV2DWidth, int LPV2DHeight, int LPV3DWidth, int LPV3DHeight, int LPV3DDepth)
{
    float3 lookupCoords = float3(LPVSpacePos.x,LPVSpacePos.y,LPVSpacePos.z*LPV3DDepth) + float3(0.5f/LPV3DWidth,0.5f/LPV3DHeight, 0.5f);
    return lookupCoords;
}

void lookupSHSamples(float3 LPVSpacePos, float3 UnsnappedLPVSpacePos, inout float4 SHred, inout float4 SHgreen, inout float4 SHblue, inout float inside,
sampler2DArray LPVRed, sampler2DArray LPVGreen, sampler2DArray LPVBlue,
int numCols, int numRows, int LPV2DWidth, int LPV2DHeight, int LPV3DWidth, int LPV3DHeight, int LPV3DDepth)
{

    float blendRegion = 0.3f;
    float insideX = min( min(blendRegion, UnsnappedLPVSpacePos.x), min(blendRegion, 1.0f - UnsnappedLPVSpacePos.x));
    float insideY = min( min(blendRegion, UnsnappedLPVSpacePos.y), min(blendRegion, 1.0f - UnsnappedLPVSpacePos.y));
    float insideZ = min( min(blendRegion, UnsnappedLPVSpacePos.z), min(blendRegion, 1.0f - UnsnappedLPVSpacePos.z));
    inside = min(insideX, (insideY,insideZ));
    inside /= blendRegion;
    if(inside<=0)
    {
        inside = 0;
        return;
    }
    if(inside>1) inside = 1;


    float3 lookupCoords = lookupSHAux(LPVSpacePos, numCols, numRows, LPV2DWidth, LPV2DHeight, LPV3DWidth, LPV3DHeight, LPV3DDepth);

    trilinearSample(SHred, SHgreen, SHblue, lookupCoords, LPVRed, LPVGreen, LPVBlue, LPV3DDepth);

}

void lookupSHSamples(float3 LPVSpacePos, float3 UnsnappedLPVSpacePos, inout float4 SHred, inout float4 SHgreen, inout float4 SHblue, inout float inside,
sampler2DArray LPVRed_0,   sampler2DArray LPVRed_1,   sampler2DArray LPVRed_2,   sampler2DArray LPVRed_3,
sampler2DArray LPVGreen_0, sampler2DArray LPVGreen_1, sampler2DArray LPVGreen_2, sampler2DArray LPVGreen_3,
sampler2DArray LPVBlue_0,  sampler2DArray LPVBlue_1,  sampler2DArray LPVBlue_2,  sampler2DArray LPVBlue_3,
int numCols, int numRows, int LPV2DWidth, int LPV2DHeight, int LPV3DWidth, int LPV3DHeight, int LPV3DDepth)
{

    float blendRegion = 0.3f;
    float insideX = min( min(blendRegion, UnsnappedLPVSpacePos.x), min(blendRegion, 1.0f - UnsnappedLPVSpacePos.x));
    float insideY = min( min(blendRegion, UnsnappedLPVSpacePos.y), min(blendRegion, 1.0f - UnsnappedLPVSpacePos.y));
    float insideZ = min( min(blendRegion, UnsnappedLPVSpacePos.z), min(blendRegion, 1.0f - UnsnappedLPVSpacePos.z));
    inside = min(insideX, (insideY,insideZ));
    inside /= blendRegion;
    if(inside<=0)
    {
        inside = 0;
        return;
    }
    if(inside>1) inside = 1;

    float3 lookupCoords = lookupSHAux(LPVSpacePos, numCols, numRows, LPV2DWidth, LPV2DHeight, LPV3DWidth, LPV3DHeight, LPV3DDepth);

    trilinearSample(SHred, SHgreen, SHblue, lookupCoords,
                   LPVRed_0, LPVRed_1, LPVRed_2, LPVRed_3,
                   LPVGreen_0,  LPVGreen_1,  LPVGreen_2,  LPVGreen_3,
                   LPVBlue_0,   LPVBlue_1,   LPVBlue_2,   LPVBlue_3, LPV3DDepth);
}

float3 loadNormal(VS_OUTPUT f)
{
    float3 Normal = normalize(f.Normal);
    float3 Tn = normalize(f.tangent);
    float3 Bn = normalize(f.binorm);
    float4 bumps = g_txNormalMap.Sample(samAniso, f.TextureUV);
    bumps = float4(2*bumps.xyz - float3(1,1,1),0);
    float3x3 TangentToWorld = float3x3(Tn,Bn,Normal);
    float3 Nb = lerp(Normal,mul(bumps.xyz,TangentToWorld),g_NormalmapMultiplier);
    Nb = normalize(Nb);
    return Nb;
}

float4 SHCNormalize(in float4 res)
{
    // extract direction
    float l = dot(res.gba, res.gba);
    res.gba /= max(0.05f, sqrt(l));
    res.r = 1.0;
    return res;
}