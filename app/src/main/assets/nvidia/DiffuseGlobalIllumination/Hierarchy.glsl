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
Texture2DArray   g_txLevelToSample                 : register( t0 );
Texture2DArray   g_txLevelToSampleAccumulate       : register( t1 );
Texture2DArray   g_tx0                             : register( t2 );
Texture2DArray   g_tx1                             : register( t3 );
Texture2DArray   g_tx2                             : register( t4 );
Texture2DArray   g_tx3                             : register( t5 );
RWTexture2DArray<float4>g_uavLevelToWrite          : register( u0 );
RWTexture2DArray<float>    g_uav0                  : register( u1 );
RWTexture2DArray<float>    g_uav1                  : register( u2 );
RWTexture2DArray<float>    g_uav2                  : register( u3 );
RWTexture2DArray<float>    g_uav3                  : register( u4 );
#else
layout(binding = 0) uniform sampler2DArray g_txLevelToSample;
layout(binding = 1) uniform sampler2DArray g_txLevelToSampleAccumulate;
layout(binding = 2) uniform sampler2DArray g_tx0;
layout(binding = 3) uniform sampler2DArray g_tx1;
layout(binding = 4) uniform sampler2DArray g_tx2;
layout(binding = 5) uniform sampler2DArray g_tx3;

layout(rgba8,binding = 0) uniform image2DArray g_uavLevelToWrite;
layout(r32f,binding = 1) uniform image2DArray g_uav0;
layout(r32f,binding = 2) uniform image2DArray g_uav1;
layout(r32f,binding = 3) uniform image2DArray g_uav2;
layout(r32f,binding = 4) uniform image2DArray g_uav3;
#endif


layout(binding = 0) uniform cbConstantsHierarchy /*: register( b0 )*/
{
    int finerLevelWidth        /*: packoffset(c0.x)*/;
    int finerLevelHeight       /*: packoffset(c0.y)*/;
    int finerLevelDepth        /*: packoffset(c0.z)*/;
    int g_numColsFiner         /*: packoffset(c0.w)*/;

    int g_numRowsFiner         /*: packoffset(c1.x)*/;
    int g_numColsCoarser       /*: packoffset(c1.y)*/;
    int g_numRowsCoarser       /*: packoffset(c1.z)*/;
    int coarserLevelWidth      /*: packoffset(c1.w)*/;

    int coarserLevelHeight     /*: packoffset(c2.x)*/;
    int coarserLevelDepth      /*: packoffset(c2.y)*/;
    int padding1               /*: packoffset(c2.z)*/;
    int padding2               /*: packoffset(c2.w)*/;
}

//make sure to not blur across the boundaries of the 2D texture chunk!
float4 loadOffsetTexValue(sampler2DArray tex,
int Width3D, int Height3D, int Depth3D, int numCols, int numRows, uint3 pos)
{
    if(pos.x >= Width3D  || pos.x < 1) return float4(0,0,0,0);
    if(pos.y >= Height3D || pos.y < 1) return float4(0,0,0,0);
    if(pos.z >= Depth3D  || pos.z < 1) return float4(0,0,0,0);

//    return tex.Load(int4(pos.x,pos.y,pos.z,0));
    return texelFetch(tex, int3(pos), 0);
}

float4 loadOffsetTexValueCoarser(sampler2DArray tex, uint3 pos)
{
    return loadOffsetTexValue( g_txLevelToSample, coarserLevelWidth, coarserLevelHeight, coarserLevelDepth, g_numColsCoarser, g_numRowsCoarser, pos);
}

float4 loadOffsetTexValue(sampler2DArray tex0, Texture2DArray tex1, Texture2DArray tex2, Texture2DArray tex3,
int Width3D, int Height3D, int Depth3D, int numCols, int numRows, uint3 pos)
{
    if(pos.x >= Width3D  || pos.x < 1) return float4(0,0,0,0);
    if(pos.y >= Height3D || pos.y < 1) return float4(0,0,0,0);
    if(pos.z >= Depth3D  || pos.z < 1) return float4(0,0,0,0);

    int4 readPos = int4(pos.x,pos.y,pos.z,0);

    float4 retVal;
    retVal.x = texelFetch(tex0, readPos.xyz, 0);
    retVal.y = texelFetch(tex1, readPos.xyz, 0);
    retVal.z = texelFetch(tex2, readPos.xyz, 0);
    retVal.w = texelFetch(tex3, readPos.xyz, 0);
    return retVal;
}


float4 loadOffsetTexValueCoarserSingleChannels(sampler2DArray tx0, sampler2DArray tx1, sampler2DArray tx2, sampler2DArray tx3, uint3 pos)
{
    if(pos.x >= coarserLevelWidth  || pos.x < 1) return float4(0,0,0,0);
    if(pos.y >= coarserLevelHeight || pos.y < 1) return float4(0,0,0,0);
    if(pos.z >= coarserLevelDepth  || pos.z < 1) return float4(0,0,0,0);

    int4 readPos = int4(pos.x,pos.y,pos.z,0);

    float4 outValue;
    outValue.x = texelFetch(tx0, readPos.xyz, 0);
    outValue.y = texelFetch(tx1, readPos.xyz, 0);
    outValue.z = texelFetch(tx2, readPos.xyz, 0);
    outValue.w = texelFetch(tx3, readPos.xyz, 0);
    return outValue;
}


void writeOffsetTexValue(/*RWTexture2DArray<float4>*/image2DArray  uavToWrite,
uint3 pos, float4 value, int Width3D, int Height3D, int Depth3D, int numCols, int numRows )
{
    if(pos.x >= Width3D  || pos.x < 1) return;
    if(pos.y >= Height3D || pos.y < 1) return;
    if(pos.z >= Depth3D  || pos.z < 1) return;

//    uavToWrite[pos] = value;
    imageStore(uavToWrite, int3(pos), value);
}

void writeOffsetTexValue(/*RWTexture2DArray<float>*/image2DArray uav0, /*RWTexture2DArray<float>*/image2DArray uav1,
                         /*RWTexture2DArray<float>*/image2DArray uav2, /*RWTexture2DArray<float>*/image2DArray uav3,
uint3 pos, float4 value, int Width3D, int Height3D, int Depth3D, int numCols, int numRows )
{
    if(pos.x >= Width3D  || pos.x < 1) return;
    if(pos.y >= Height3D || pos.y < 1) return;
    if(pos.z >= Depth3D  || pos.z < 1) return;

    int3 posToWrite = int3(pos);

//    uav0[posToWrite] = value.x;
//    uav1[posToWrite] = value.y;
//    uav2[posToWrite] = value.z;
//    uav3[posToWrite] = value.w;

    imageStore(uav0, posToWrite, float4(value.x, 0,0,0));
    imageStore(uav1, posToWrite, float4(value.y, 0,0,0));
    imageStore(uav2, posToWrite, float4(value.z, 0,0,0));
    imageStore(uav3, posToWrite, float4(value.w, 0,0,0));
}

layout (local_size_x = X_BLOCK_SIZE, local_size_y = Y_BLOCK_SIZE, local_size_z = Z_BLOCK_SIZE) in;
#if DOWNSAMPLE_MAX

#if 0
[numthreads(X_BLOCK_SIZE,Y_BLOCK_SIZE,Z_BLOCK_SIZE)]
void DownsampleMax(uint threadId        : SV_GroupIndex,
                   uint3 groupId        : SV_GroupID,
                   uint3 globalThreadId : SV_DispatchThreadID)
#else
void main()
#endif
{
    uint threadId = gl_LocalInvocationIndex;
    uint3 groupId = gl_WorkGroupID;
    uint3 globalThreadId = gl_GlobalInvocationID;
    bool outside = false;
    if(globalThreadId.x>=coarserLevelWidth || globalThreadId.y>=coarserLevelHeight || globalThreadId.z>=coarserLevelDepth) outside = true;

    float4 outValue = max(
                          max(
                                    max( loadOffsetTexValue( g_txLevelToSample, finerLevelWidth, finerLevelHeight, finerLevelDepth, g_numColsFiner, g_numRowsFiner, uint3(globalThreadId.x*2,globalThreadId.y*2,globalThreadId.z*2)),
                                    loadOffsetTexValue( g_txLevelToSample, finerLevelWidth, finerLevelHeight, finerLevelDepth, g_numColsFiner, g_numRowsFiner, uint3(globalThreadId.x*2+1,globalThreadId.y*2,globalThreadId.z*2))),
                                    max( loadOffsetTexValue( g_txLevelToSample, finerLevelWidth, finerLevelHeight, finerLevelDepth, g_numColsFiner, g_numRowsFiner, uint3(globalThreadId.x*2,globalThreadId.y*2+1,globalThreadId.z*2)),
                                    loadOffsetTexValue( g_txLevelToSample, finerLevelWidth, finerLevelHeight, finerLevelDepth, g_numColsFiner, g_numRowsFiner, uint3(globalThreadId.x*2+1,globalThreadId.y*2+1,globalThreadId.z*2)))
                             ),
                          max(
                                    max( loadOffsetTexValue( g_txLevelToSample, finerLevelWidth, finerLevelHeight, finerLevelDepth, g_numColsFiner, g_numRowsFiner, uint3(globalThreadId.x*2,globalThreadId.y*2,globalThreadId.z*2+1)),
                                    loadOffsetTexValue( g_txLevelToSample, finerLevelWidth, finerLevelHeight, finerLevelDepth, g_numColsFiner, g_numRowsFiner, uint3(globalThreadId.x*2+1,globalThreadId.y*2,globalThreadId.z*2+1))),
                                    max( loadOffsetTexValue( g_txLevelToSample, finerLevelWidth, finerLevelHeight, finerLevelDepth, g_numColsFiner, g_numRowsFiner, uint3(globalThreadId.x*2,globalThreadId.y*2+1,globalThreadId.z*2+1)),
                                    loadOffsetTexValue( g_txLevelToSample, finerLevelWidth, finerLevelHeight, finerLevelDepth, g_numColsFiner, g_numRowsFiner, uint3(globalThreadId.x*2+1,globalThreadId.y*2+1,globalThreadId.z*2+1)))
                             )
                        );

    writeOffsetTexValue(g_uavLevelToWrite,globalThreadId, outValue, coarserLevelWidth, coarserLevelHeight, coarserLevelDepth, g_numColsCoarser, g_numRowsCoarser);
}
#endif

#if DOWNSAMPLE_MIN
#if 0
[numthreads(X_BLOCK_SIZE,Y_BLOCK_SIZE,Z_BLOCK_SIZE)]
void DownsampleMin(uint threadId        : SV_GroupIndex,
                   uint3 groupId        : SV_GroupID,
                   uint3 globalThreadId : SV_DispatchThreadID)
#else
void main()
#endif
{
    uint threadId = gl_LocalInvocationIndex;
    uint3 groupId = gl_WorkGroupID;
    uint3 globalThreadId = gl_GlobalInvocationID;

    bool outside = false;
    if(globalThreadId.x>=coarserLevelWidth || globalThreadId.y>=coarserLevelHeight || globalThreadId.z>=coarserLevelDepth) outside = true;

    float4 outValue = min(
                          min(
                                    min( loadOffsetTexValue( g_txLevelToSample, finerLevelWidth, finerLevelHeight, finerLevelDepth, g_numColsFiner, g_numRowsFiner, uint3(globalThreadId.x*2,globalThreadId.y*2,globalThreadId.z*2)),
                                    loadOffsetTexValue( g_txLevelToSample, finerLevelWidth, finerLevelHeight, finerLevelDepth, g_numColsFiner, g_numRowsFiner, uint3(globalThreadId.x*2+1,globalThreadId.y*2,globalThreadId.z*2))),
                                    min( loadOffsetTexValue( g_txLevelToSample, finerLevelWidth, finerLevelHeight, finerLevelDepth, g_numColsFiner, g_numRowsFiner, uint3(globalThreadId.x*2,globalThreadId.y*2+1,globalThreadId.z*2)),
                                    loadOffsetTexValue( g_txLevelToSample, finerLevelWidth, finerLevelHeight, finerLevelDepth, g_numColsFiner, g_numRowsFiner, uint3(globalThreadId.x*2+1,globalThreadId.y*2+1,globalThreadId.z*2)))
                             ),
                          min(
                                    min( loadOffsetTexValue( g_txLevelToSample, finerLevelWidth, finerLevelHeight, finerLevelDepth, g_numColsFiner, g_numRowsFiner, uint3(globalThreadId.x*2,globalThreadId.y*2,globalThreadId.z*2+1)),
                                    loadOffsetTexValue( g_txLevelToSample, finerLevelWidth, finerLevelHeight, finerLevelDepth, g_numColsFiner, g_numRowsFiner, uint3(globalThreadId.x*2+1,globalThreadId.y*2,globalThreadId.z*2+1))),
                                    min( loadOffsetTexValue( g_txLevelToSample, finerLevelWidth, finerLevelHeight, finerLevelDepth, g_numColsFiner, g_numRowsFiner, uint3(globalThreadId.x*2,globalThreadId.y*2+1,globalThreadId.z*2+1)),
                                    loadOffsetTexValue( g_txLevelToSample, finerLevelWidth, finerLevelHeight, finerLevelDepth, g_numColsFiner, g_numRowsFiner, uint3(globalThreadId.x*2+1,globalThreadId.y*2+1,globalThreadId.z*2+1)))
                             )
                        );

    writeOffsetTexValue(g_uavLevelToWrite,globalThreadId, outValue, coarserLevelWidth, coarserLevelHeight, coarserLevelDepth, g_numColsCoarser, g_numRowsCoarser);
}
#endif

#if DOWNSAMPLE_AVERAGE

#if 0
[numthreads(X_BLOCK_SIZE,Y_BLOCK_SIZE,Z_BLOCK_SIZE)]
void DownsampleAverage(uint threadId        : SV_GroupIndex,
                       uint3 groupId        : SV_GroupID,
                       uint3 globalThreadId : SV_DispatchThreadID)
#else
void main()
#endif
{
    uint threadId = gl_LocalInvocationIndex;
    uint3 groupId = gl_WorkGroupID;
    uint3 globalThreadId = gl_GlobalInvocationID;
    bool outside = false;
    if(globalThreadId.x>=coarserLevelWidth || globalThreadId.y>=coarserLevelHeight || globalThreadId.z>=coarserLevelDepth) outside = true;

    float4 outValue = ( loadOffsetTexValue( g_txLevelToSample, finerLevelWidth, finerLevelHeight, finerLevelDepth, g_numColsFiner, g_numRowsFiner, uint3(globalThreadId.x*2,globalThreadId.y*2,globalThreadId.z*2))      +
                        loadOffsetTexValue( g_txLevelToSample, finerLevelWidth, finerLevelHeight, finerLevelDepth, g_numColsFiner, g_numRowsFiner, uint3(globalThreadId.x*2+1,globalThreadId.y*2,globalThreadId.z*2))    +
                        loadOffsetTexValue( g_txLevelToSample, finerLevelWidth, finerLevelHeight, finerLevelDepth, g_numColsFiner, g_numRowsFiner, uint3(globalThreadId.x*2,globalThreadId.y*2+1,globalThreadId.z*2))    +
                        loadOffsetTexValue( g_txLevelToSample, finerLevelWidth, finerLevelHeight, finerLevelDepth, g_numColsFiner, g_numRowsFiner, uint3(globalThreadId.x*2+1,globalThreadId.y*2+1,globalThreadId.z*2))  +
                        loadOffsetTexValue( g_txLevelToSample, finerLevelWidth, finerLevelHeight, finerLevelDepth, g_numColsFiner, g_numRowsFiner, uint3(globalThreadId.x*2,globalThreadId.y*2,globalThreadId.z*2+1))    +
                        loadOffsetTexValue( g_txLevelToSample, finerLevelWidth, finerLevelHeight, finerLevelDepth, g_numColsFiner, g_numRowsFiner, uint3(globalThreadId.x*2+1,globalThreadId.y*2,globalThreadId.z*2+1))  +
                        loadOffsetTexValue( g_txLevelToSample, finerLevelWidth, finerLevelHeight, finerLevelDepth, g_numColsFiner, g_numRowsFiner, uint3(globalThreadId.x*2,globalThreadId.y*2+1,globalThreadId.z*2+1))  +
                        loadOffsetTexValue( g_txLevelToSample, finerLevelWidth, finerLevelHeight, finerLevelDepth, g_numColsFiner, g_numRowsFiner, uint3(globalThreadId.x*2+1,globalThreadId.y*2+1,globalThreadId.z*2+1))
                        )* 0.125;

    writeOffsetTexValue(g_uavLevelToWrite,globalThreadId, outValue, coarserLevelWidth, coarserLevelHeight, coarserLevelDepth, g_numColsCoarser, g_numRowsCoarser);
}
#endif

#if DOWNSAMPLE_AVERAGE_INPLACE

#if 0
[numthreads(X_BLOCK_SIZE,Y_BLOCK_SIZE,Z_BLOCK_SIZE)]
void DownsampleAverageInplace(    uint threadId        : SV_GroupIndex,
                                uint3 groupId        : SV_GroupID,
                                uint3 globalThreadId : SV_DispatchThreadID)
#else
void main()
#endif
{
    uint threadId = gl_LocalInvocationIndex;
    uint3 groupId = gl_WorkGroupID;
    uint3 globalThreadId = gl_GlobalInvocationID;
    bool outside = false;
    if(globalThreadId.x>=coarserLevelWidth || globalThreadId.y>=coarserLevelHeight || globalThreadId.z>=coarserLevelDepth) outside = true;

    float4 outValue = ( loadOffsetTexValue( g_tx0, g_tx1, g_tx2, g_tx3, finerLevelWidth, finerLevelHeight, finerLevelDepth, g_numColsFiner, g_numRowsFiner, uint3(globalThreadId.x*2,globalThreadId.y*2,globalThreadId.z*2))      +
                        loadOffsetTexValue( g_tx0, g_tx1, g_tx2, g_tx3, finerLevelWidth, finerLevelHeight, finerLevelDepth, g_numColsFiner, g_numRowsFiner, uint3(globalThreadId.x*2+1,globalThreadId.y*2,globalThreadId.z*2))    +
                        loadOffsetTexValue( g_tx0, g_tx1, g_tx2, g_tx3, finerLevelWidth, finerLevelHeight, finerLevelDepth, g_numColsFiner, g_numRowsFiner, uint3(globalThreadId.x*2,globalThreadId.y*2+1,globalThreadId.z*2))    +
                        loadOffsetTexValue( g_tx0, g_tx1, g_tx2, g_tx3, finerLevelWidth, finerLevelHeight, finerLevelDepth, g_numColsFiner, g_numRowsFiner, uint3(globalThreadId.x*2+1,globalThreadId.y*2+1,globalThreadId.z*2))  +
                        loadOffsetTexValue( g_tx0, g_tx1, g_tx2, g_tx3, finerLevelWidth, finerLevelHeight, finerLevelDepth, g_numColsFiner, g_numRowsFiner, uint3(globalThreadId.x*2,globalThreadId.y*2,globalThreadId.z*2+1))    +
                        loadOffsetTexValue( g_tx0, g_tx1, g_tx2, g_tx3, finerLevelWidth, finerLevelHeight, finerLevelDepth, g_numColsFiner, g_numRowsFiner, uint3(globalThreadId.x*2+1,globalThreadId.y*2,globalThreadId.z*2+1))  +
                        loadOffsetTexValue( g_tx0, g_tx1, g_tx2, g_tx3, finerLevelWidth, finerLevelHeight, finerLevelDepth, g_numColsFiner, g_numRowsFiner, uint3(globalThreadId.x*2,globalThreadId.y*2+1,globalThreadId.z*2+1))  +
                        loadOffsetTexValue( g_tx0, g_tx1, g_tx2, g_tx3, finerLevelWidth, finerLevelHeight, finerLevelDepth, g_numColsFiner, g_numRowsFiner, uint3(globalThreadId.x*2+1,globalThreadId.y*2+1,globalThreadId.z*2+1))
                        )* 0.125;

    writeOffsetTexValue(g_uav0, g_uav1, g_uav2, g_uav3, globalThreadId, outValue, coarserLevelWidth, coarserLevelHeight, coarserLevelDepth, g_numColsCoarser, g_numRowsCoarser);
}
#endif

#if UPSAMPLE

#if 0
[numthreads(X_BLOCK_SIZE,Y_BLOCK_SIZE,Z_BLOCK_SIZE)]
void Upsample(uint threadId        : SV_GroupIndex,
              uint3 groupId        : SV_GroupID,
              uint3 globalThreadId : SV_DispatchThreadID)
#else
void main()
#endif
{
    uint threadId = gl_LocalInvocationIndex;
    uint3 groupId = gl_WorkGroupID;
    uint3 globalThreadId = gl_GlobalInvocationID;
    bool outside = false;
    if(globalThreadId.x>=coarserLevelWidth || globalThreadId.y>=coarserLevelHeight || globalThreadId.z>=coarserLevelDepth) outside = true;

    //load the value in the coarse texture
    float4 outValue = loadOffsetTexValue( g_txLevelToSample, coarserLevelWidth, coarserLevelHeight, coarserLevelDepth, g_numColsCoarser, g_numRowsCoarser, globalThreadId.xyz);

    //write this value to the four locations of the finer texture

    g_uavLevelToWrite[uint3(globalThreadId.x*2,globalThreadId.y*2,globalThreadId.z*2+1)] = outValue;
    g_uavLevelToWrite[uint3(globalThreadId.x*2+1,globalThreadId.y*2,globalThreadId.z*2+1)] = outValue;
    g_uavLevelToWrite[uint3(globalThreadId.x*2,globalThreadId.y*2+1,globalThreadId.z*2+1)] = outValue;
    g_uavLevelToWrite[uint3(globalThreadId.x*2+1,globalThreadId.y*2+1,globalThreadId.z*2+1)] = outValue;
    g_uavLevelToWrite[uint3(globalThreadId.x*2,globalThreadId.y*2,globalThreadId.z*2)] = outValue;
    g_uavLevelToWrite[uint3(globalThreadId.x*2+1,globalThreadId.y*2,globalThreadId.z*2)] = outValue;
    g_uavLevelToWrite[uint3(globalThreadId.x*2,globalThreadId.y*2+1,globalThreadId.z*2)] = outValue;
    g_uavLevelToWrite[uint3(globalThreadId.x*2+1,globalThreadId.y*2+1,globalThreadId.z*2)] = outValue;

}

#endif

//used for upsample Bilinear, going from coarser level to finer level
float4 BilenearInterpolateCoarser(uint3 pos)
{
    if(pos.x>=finerLevelWidth || pos.y>=finerLevelHeight || pos.z>=finerLevelDepth) return float4(0,0,0,0);

    float xPos = max(0,min(coarserLevelWidth-1  ,pos.x/2.0f - 0.25));
    float yPos = max(0,min(coarserLevelHeight-1 ,pos.y/2.0f - 0.25));
    float zPos = max(0,min(coarserLevelDepth-1  ,pos.z/2.0f - 0.25));

    int xPosF = floor(xPos);
    int xPosC = xPosF + 1;
    int yPosF = floor(yPos);
    int yPosC = yPosF + 1;
    int zPosF = floor(zPos);
    int zPosC = zPosF + 1;

    float xWeightC = xPos - xPosF;
    float xWeightF = 1 - xWeightC;
    float yWeightC = yPos - yPosF;
    float yWeightF = 1 - yWeightC;
    float zWeightC = zPos - zPosF;
    float zWeightF = 1 - zWeightC;

    //do the bilinear sampling:
    float4 outValue =
                    zWeightF*(//InterpZF
                        yWeightF*(loadOffsetTexValueCoarser( g_txLevelToSample, uint3(xPosF,yPosF,zPosF))*xWeightF + loadOffsetTexValueCoarser( g_txLevelToSample, uint3(xPosC,yPosF,zPosF))*xWeightC)+
                        yWeightC*(loadOffsetTexValueCoarser( g_txLevelToSample, uint3(xPosF,yPosC,zPosF))*xWeightF + loadOffsetTexValueCoarser( g_txLevelToSample, uint3(xPosC,yPosC,zPosF))*xWeightC)) +
                    zWeightC*(//InterpZC
                        yWeightF*(loadOffsetTexValueCoarser( g_txLevelToSample, uint3(xPosF,yPosF,zPosC))*xWeightF + loadOffsetTexValueCoarser( g_txLevelToSample, uint3(xPosC,yPosF,zPosC))*xWeightC)+
                        yWeightC*(loadOffsetTexValueCoarser( g_txLevelToSample, uint3(xPosF,yPosC,zPosC))*xWeightF + loadOffsetTexValueCoarser( g_txLevelToSample, uint3(xPosC,yPosC,zPosC))*xWeightC))
                    ;

    return outValue;

}

#if UPSAMPLE_BILINEAR
#if 0
//note: this is a slow version, can be made faster by using shared memory
[numthreads(X_BLOCK_SIZE,Y_BLOCK_SIZE,Z_BLOCK_SIZE)]
void UpsampleBilinear(uint threadId        : SV_GroupIndex,
                      uint3 groupId        : SV_GroupID,
                      uint3 globalThreadId : SV_DispatchThreadID)
#else
void main()
#endif
{
    uint threadId = gl_LocalInvocationIndex;
    uint3 groupId = gl_WorkGroupID;
    uint3 globalThreadId = gl_GlobalInvocationID;
    float4 value = BilenearInterpolateCoarser(globalThreadId);
    writeOffsetTexValue(g_uavLevelToWrite,globalThreadId, value, finerLevelWidth, finerLevelHeight, finerLevelDepth, g_numColsFiner, g_numRowsFiner);
}

#endif

#if UPSAMPLE_BILINEAR_ACCUMULATE
#if 0
//note: this is a slow version, can be made faster by using shared memory
[numthreads(X_BLOCK_SIZE,Y_BLOCK_SIZE,Z_BLOCK_SIZE)]
void UpsampleBilinearAccumulate(uint threadId        : SV_GroupIndex,
                                uint3 groupId        : SV_GroupID,
                                uint3 globalThreadId : SV_DispatchThreadID)
#else
void main()
#endif
{
    uint threadId = gl_LocalInvocationIndex;
    uint3 groupId = gl_WorkGroupID;
    uint3 globalThreadId = gl_GlobalInvocationID;
    float4 value = BilenearInterpolateCoarser(globalThreadId);

    value += loadOffsetTexValue( g_txLevelToSampleAccumulate, finerLevelWidth, finerLevelHeight, finerLevelDepth, g_numColsFiner, g_numRowsFiner,globalThreadId);

    writeOffsetTexValue(g_uavLevelToWrite,globalThreadId, value, finerLevelWidth, finerLevelHeight, finerLevelDepth, g_numColsFiner, g_numRowsFiner);
}
#endif

#if UPSAMPLE_BILINE_ARACCUMULATE_INPLACE
#if 0
//note: this is a slow version, can be made faster by using shared memory
//note: the inplace version is specialized for single channel textures because inplace updates are only allowed on single channel uavs
//it is also specialized for exactly 4 such single channel textures, if more or less are needed you need to change this function or add more specialized functions
[numthreads(X_BLOCK_SIZE,Y_BLOCK_SIZE,Z_BLOCK_SIZE)]
void UpsampleBilinearAccumulateInplace(    uint threadId        : SV_GroupIndex,
                                        uint3 groupId        : SV_GroupID,
                                        uint3 globalThreadId : SV_DispatchThreadID)
#else
void main()
#endif
{
    uint threadId = gl_LocalInvocationIndex;
    uint3 groupId = gl_WorkGroupID;
    uint3 globalThreadId = gl_GlobalInvocationID;
    uint3 pos = globalThreadId;

    if(pos.x>=finerLevelWidth || pos.y>=finerLevelHeight || pos.z>=finerLevelDepth ||
       pos.x<1 || pos.y<1 || pos.z<1) return; //position is out of bounds, we dont need to do anything

    float xPos = max(0,min(coarserLevelWidth-1  ,pos.x/2.0f - 0.25));
    float yPos = max(0,min(coarserLevelHeight-1 ,pos.y/2.0f - 0.25));
    float zPos = max(0,min(coarserLevelDepth-1  ,pos.z/2.0f - 0.25));

    int xPosF = floor(xPos);
    int xPosC = xPosF + 1;
    int yPosF = floor(yPos);
    int yPosC = yPosF + 1;
    int zPosF = floor(zPos);
    int zPosC = zPosF + 1;

    float xWeightC = xPos - xPosF;
    float xWeightF = 1 - xWeightC;
    float yWeightC = yPos - yPosF;
    float yWeightF = 1 - yWeightC;
    float zWeightC = zPos - zPosF;
    float zWeightF = 1 - zWeightC;

    //do the bilinear sampling:
    float4 value =
                    zWeightF*(//InterpZF
                        yWeightF*( loadOffsetTexValueCoarserSingleChannels( g_tx0, g_tx1, g_tx2, g_tx3, uint3(xPosF,yPosF,zPosF))*xWeightF +
                                   loadOffsetTexValueCoarserSingleChannels( g_tx0, g_tx1, g_tx2, g_tx3, uint3(xPosC,yPosF,zPosF))*xWeightC)+
                        yWeightC*( loadOffsetTexValueCoarserSingleChannels( g_tx0, g_tx1, g_tx2, g_tx3, uint3(xPosF,yPosC,zPosF))*xWeightF +
                                   loadOffsetTexValueCoarserSingleChannels( g_tx0, g_tx1, g_tx2, g_tx3, uint3(xPosC,yPosC,zPosF))*xWeightC)) +
                    zWeightC*(//InterpZC
                        yWeightF*(loadOffsetTexValueCoarserSingleChannels( g_tx0, g_tx1, g_tx2, g_tx3, uint3(xPosF,yPosF,zPosC))*xWeightF +
                                  loadOffsetTexValueCoarserSingleChannels( g_tx0, g_tx1, g_tx2, g_tx3, uint3(xPosC,yPosF,zPosC))*xWeightC)+
                        yWeightC*(loadOffsetTexValueCoarserSingleChannels( g_tx0, g_tx1, g_tx2, g_tx3, uint3(xPosF,yPosC,zPosC))*xWeightF +
                                  loadOffsetTexValueCoarserSingleChannels( g_tx0, g_tx1, g_tx2, g_tx3, uint3(xPosC,yPosC,zPosC))*xWeightC))
                    ;


    //accumulate the results
    int3 writePos = int3(pos);

    /*g_uav0[writePos] += value.x;
    g_uav1[writePos] += value.y;
    g_uav2[writePos] += value.z;
    g_uav3[writePos] += value.w;*/

    float v0 = imageLoad(g_uav0, writePos).x + value.x;
    float v1 = imageLoad(g_uav1, writePos).x + value.y;
    float v2 = imageLoad(g_uav2, writePos).x + value.z;
    float v3 = imageLoad(g_uav3, writePos).x + value.w;

    imageStore(g_uav0, writePos, float4(v0, 0,0,0));
    imageStore(g_uav1, writePos, float4(v1, 0,0,0));
    imageStore(g_uav2, writePos, float4(v2, 0,0,0));
    imageStore(g_uav3, writePos, float4(v3, 0,0,0));
}
