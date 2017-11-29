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
Texture2DArray   g_txLPVPropRed                    : register( t0 );
Texture2DArray   g_txLPVPropGreen                  : register( t1 );
Texture2DArray   g_txLPVPropBlue                   : register( t2 );
Texture2DArray   g_txLPVAccumRed                   : register( t3 );
Texture2DArray   g_txLPVAccumGreen                 : register( t4 );
Texture2DArray   g_txLPVAccumBlue                  : register( t5 );
RWTexture2DArray<float4>    g_uavAccumulateRed     : register( u0 );
RWTexture2DArray<float4>    g_uavAccumulateGreen   : register( u1 );
RWTexture2DArray<float4>    g_uavAccumulateBlue    : register( u2 );
#else
layout(binding = 0) uniform sampler2DArray g_txLPVPropRed;
layout(binding = 1) uniform sampler2DArray g_txLPVPropGreen;
layout(binding = 2) uniform sampler2DArray g_txLPVPropBlue;
layout(binding = 3) uniform sampler2DArray g_txLPVAccumRed;
layout(binding = 4) uniform sampler2DArray g_txLPVAccumGreen;
layout(binding = 5) uniform sampler2DArray g_txLPVAccumBlue;

layout(binding = 0) uniform image2DArray g_uavAccumulateRed;
layout(binding = 1) uniform image2DArray g_uavAccumulateGreen;
layout(binding = 2) uniform image2DArray g_uavAccumulateBlue;
#endif

#if 0
cbuffer cbConstantsLPVinitialize3 : register( b0 )
{
    int g_numCols        : packoffset(c0.x);    //the number of columns in the flattened 2D LPV
    int g_numRows        : packoffset(c0.y);    //the number of columns in the flattened 2D LPV
    int LPV2DWidth       : packoffset(c0.z);   //the total width of the flattened 2D LPV
    int LPV2DHeight      : packoffset(c0.w);   //the total height of the flattened 2D LPV

    int LPV3DWidth       : packoffset(c1.x);      //the width of the LPV in 3D
    int LPV3DHeight      : packoffset(c1.y);     //the height of the LPV in 3D
    int LPV3DDepth       : packoffset(c1.z);
    float padding7       : packoffset(c1.w);
}
#else
layout(std430, binding = 0) uniform cbConstantsLPVinitialize3
{
    int g_numCols;
    int g_numRows;
    int LPV2DWidth;
    int LPV2DHeight;

    int LPV3DWidth;
    int LPV3DHeight;
    int LPV3DDepth;
    int padding7;
}
#endif

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

    /*val1 = tex1.Load(int4(pos.x,pos.y,pos.z,0));
    val2 = tex2.Load(int4(pos.x,pos.y,pos.z,0));
    val3 = tex3.Load(int4(pos.x,pos.y,pos.z,0));*/
    val1 = texelFetch(tex1, int3(pos), 0);
    val2 = texelFetch(tex2, int3(pos), 0);
    val3 = texelFetch(tex3, int3(pos), 0);
}



//--------------------------------------------------------
//accumulate shader
//--------------------------------------------------------
#if 0
[numthreads(X_BLOCK_SIZE,Y_BLOCK_SIZE,Z_BLOCK_SIZE)]
void AccumulateLPV(uint threadId        : SV_GroupIndex,
                   uint3 groupId        : SV_GroupID,
                   uint3 globalThreadId : SV_DispatchThreadID)
#else
layout (local_size_x = X_BLOCK_SIZE, local_size_y = Y_BLOCK_SIZE, local_size_z = Z_BLOCK_SIZE) in;
void main()
#endif
{
    uint threadId = gl_LocalInvocationIndex;
    uint3 groupId = gl_WorkGroupID;
    uint3 globalThreadId = gl_GlobalInvocationID;
    //find where in the 2d texture that grid point is supposed to land
    bool outside = false;
    if(globalThreadId.x>=LPV3DWidth || globalThreadId.y>=LPV3DHeight || globalThreadId.z>=LPV3DDepth) outside = true;

    int3 writePos = int3(globalThreadId.xyz);
    int3 readPos = int3(globalThreadId.x,globalThreadId.y,globalThreadId.z);

    //write back the accumulated flux
    if(!outside)
    {
        float4 SHCoefficientsRed = /*g_txLPVPropRed.Load(readPos)*/texelFetch(g_txLPVPropRed, readPos, 0);
        float4 SHCoefficientsGreen = /*g_txLPVPropGreen.Load(readPos)*/texelFetch(g_txLPVPropGreen, readPos, 0);
        float4 SHCoefficientsBlue = /*g_txLPVPropBlue.Load(readPos)*/texelFetch(g_txLPVPropBlue, readPos, 0);

        /*g_uavAccumulateRed[writePos] = g_txLPVAccumRed.Load(readPos) + SHCoefficientsRed;
        g_uavAccumulateGreen[writePos] =  g_txLPVAccumGreen.Load(readPos) + SHCoefficientsGreen;
        g_uavAccumulateBlue[writePos] =  g_txLPVAccumBlue.Load(readPos) + SHCoefficientsBlue;*/

        imageStore(g_uavAccumulateRed, writePos, texelFetch(g_txLPVAccumRed, readPos, 0)+ SHCoefficientsRed);
        imageStore(g_uavAccumulateGreen, writePos, texelFetch(g_txLPVAccumGreen, readPos, 0)+ SHCoefficientsGreen);
        imageStore(g_uavAccumulateBlue, writePos, texelFetch(g_txLPVAccumBlue, readPos, 0)+ SHCoefficientsBlue);

    }

}