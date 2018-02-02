#include "../../../AVSM/shaders/AVSM_def.h"
#include "../../../AVSM/shaders/AVSM_Gen_def.h"

struct AVSMGenSegment {
	int	  index;
	float depthA;
	float depthB;
	float transA;
	float transB;
};

struct AVSMSegment
{
    int   index;
    float depthA;
    float depthB;
    float transA;
    float transB;
};

struct AVSMData
{
    vec4 depth[AVSM_RT_COUNT];
    vec4 trans[AVSM_RT_COUNT];
};

struct AVSMGenData
{
	// this holds the nodes for a single pixel in depth 1 f32 per node w/ lower 8 bits = transmittence
	vec4 data[AVSM_RT_COUNT];
};

struct NONCPUT_UIConstants
{
    uint  faceNormals;
    uint  enableStats;
    uint  volumeShadowMethod;
    uint  enableVolumeShadowLookup;
    uint  pauseParticleAnimaton;
    uint  particleOpacity;
    uint  vertexShaderShadowLookup;
    uint  tessellate;
    uint  wireframe;
    uint  lightingOnly;
    float particleSize;
    float TessellatioDensity;               //  1/desired triangle size
};

layout(binding = 2) uniform PerFrameConstants
{
    /*row_major*/   float4x4    mCameraWorldViewProj;
    /*row_major*/   float4x4    mCameraWorldView;
    /*row_major*/   float4x4    mCameraViewProj;
    /*row_major*/   float4x4    mCameraProj;
                    float4      mCameraPos;
    /*row_major*/   float4x4    mLightWorldViewProj;
    /*row_major*/   float4x4    mAvsmLightWorldViewProj;
    /*row_major*/   float4x4    mCameraViewToLightProj;
    /*row_major*/   float4x4    mCameraViewToLightView;
    /*row_major*/   float4x4    mCameraViewToAvsmLightProj;
    /*row_major*/   float4x4    mCameraViewToAvsmLightView;
                    float4      mLightDir;
                    float4		mScreenResolution;
                    float4      mScreenToViewConsts;

    NONCPUT_UIConstants mUI;
};

layout(binding = 3) uniform AVSMConstants
{
    float4    mMask0;
    float4    mMask1;
    float4    mMask2;
    float4    mMask3;
    float4    mMask4;
    float     mEmptyNode;
    float     mOpaqueNodeTrans;
    float     mShadowMapSize;
};

float2 ProjectIntoAvsmLightTexCoord(float3 positionView)
{
    float4 positionLight = mul(float4(positionView, 1.0f), mCameraViewToAvsmLightProj);
#ifdef DEBUG_DX
    float2 texCoord = (positionLight.xy / positionLight.w) * float2(0.5f, -0.5f) + float2(0.5f, 0.5f);
#else
    float2 texCoord = (positionLight.xy / positionLight.w) * float2(0.5f, +0.5f) + float2(0.5f, 0.5f);
#endif
    return texCoord;
}

layout(binding = 8) uniform sampler2DArray NONCPUT_gAVSMTexture;
layout(binding = 9) uniform sampler2D NONCPUT_gAVSMGenClearMaskSRV;

layout(binding = 12) buffer NONCPUT_ShaderBuffer1
{
    AVSMGenData NONCPUT_gAVSMGenDataSRV[];
};

float AVSMGenLinearInterp(in float d, in AVSMGenSegment seg)
{
	const  float m = (seg.transB - seg.transA) * rcp(seg.depthB - seg.depthA);
	return seg.transA + m * (d - seg.depthA);
}

float AVSMGenUnpackTrans(float value)
{
	return float(asuint(value) & uint(AVSM_GEN_MAX_UNNORM_TRANS)) / float(AVSM_GEN_MAX_UNNORM_TRANS);
}

uint AVSMGenAddrGen(uint2 addr2D, int surfaceWidth)
{
	return addr2D[0] + surfaceWidth * addr2D[1];
}

uint AVSMGenAddrGenSRV(uint2 addr2D)
{
	/*uint2 dim;
	NONCPUT_gAVSMGenClearMaskSRV.GetDimensions(dim[0], dim[1]);*/

	int2 dim = textureSize(NONCPUT_gAVSMGenClearMaskSRV, 0);
	return AVSMGenAddrGen(addr2D, dim[0]);
}

void AVSMGenLoadRawDataSRV(in uint2 pixelAddr, out AVSMGenData avsmData)
{
	uint addr = AVSMGenAddrGenSRV(pixelAddr);
	avsmData = NONCPUT_gAVSMGenDataSRV[addr];
}

float AVSMGenPointSampleInternalSoft(in int2 pixelAddr, in float receiverDepth)
{
	float rec = receiverDepth;

    // mask out transmittance during depth search
    receiverDepth = asfloat(asuint(receiverDepth) | uint(AVSM_GEN_MAX_UNNORM_TRANS));

    AVSMGenData avsmData;
    AVSMGenLoadRawDataSRV(pixelAddr, avsmData);

	float value;
	/*[flatten]*/if (receiverDepth <= avsmData.data[0][0]) {
		value = 1;
	} else /*[flatten]*/ if (receiverDepth > avsmData.data[AVSM_RT_COUNT - 1][3]) {
		value = AVSMGenUnpackTrans(avsmData.data[AVSM_RT_COUNT - 1][3]);
	} else {
		float nextNode;
		/*[unroll]*/for (int i = AVSM_RT_COUNT - 1; i >= 0; i--) {
			/*[unroll]*/ for (int j = 3; j >= 0; j--) {
				/*[flatten]*/if (receiverDepth > avsmData.data[i][j]) {
					// node found! make it negative so it won't be updated anymore
					receiverDepth = asfloat(asuint(avsmData.data[i][j]) | 0x80000000U);
					/*[flatten]*/if (3 != j) {
							nextNode = avsmData.data[i][j + 1];
					} else if (AVSM_RT_COUNT - 1 != i) {
							nextNode = avsmData.data[i + 1][0];
					}
				}
			}
		}


		const float transLeft  = AVSMGenUnpackTrans(receiverDepth);
		const float transRight = AVSMGenUnpackTrans(nextNode);
		const float depthLeft  = asfloat(asuint(receiverDepth) & uint(AVSM_GEN_TRANS_MASK2));
		const float depthRight = asfloat(asuint(nextNode) & uint(AVSM_GEN_TRANS_MASK));

		AVSMGenSegment seg = {0, depthLeft, depthRight, transLeft, transRight};

		value = AVSMGenLinearInterp(rec, seg);
	}
	return value;
}

void LoadDataLevel(inout AVSMData data, in float2 uv, in float mipLevel)
{
#if AVSM_RT_COUNT > 3
    data.depth[3] = textureLod(NONCPUT_gAVSMTexture, float3(uv, 3), mipLevel);    // gAVSMSampler
    data.trans[3] = textureLod(NONCPUT_gAVSMTexture, float3(uv, 3 + AVSM_RT_COUNT), mipLevel);
#endif
#if AVSM_RT_COUNT > 2
    data.depth[2] = textureLod(NONCPUT_gAVSMTexture, float3(uv, 2), mipLevel);
    data.trans[2] = textureLod(NONCPUT_gAVSMTexture, float3(uv, 2 + AVSM_RT_COUNT), mipLevel);
#endif
#if AVSM_RT_COUNT > 1
    data.depth[1] = textureLod(NONCPUT_gAVSMTexture, float3(uv, 1), mipLevel);
    data.trans[1] = textureLod(NONCPUT_gAVSMTexture, float3(uv, 1 + AVSM_RT_COUNT), mipLevel);
#endif
    data.depth[0] = textureLod(NONCPUT_gAVSMTexture, float3(uv, 0), mipLevel);
    data.trans[0] = textureLod(NONCPUT_gAVSMTexture, float3(uv, 0 + AVSM_RT_COUNT), mipLevel);
}

float AVSMGenLoadControlSurfaceSRV(in uint2 pixelAddr)
{
//	return NONCPUT_gAVSMGenClearMaskSRV[pixelAddr];
    return texelFetch(NONCPUT_gAVSMGenClearMaskSRV, int2(pixelAddr), 0).r;
}

float linstep(float min, float max, float v)
{
    return saturate((v - min) / (max - min));
}

float Interp(float d0, float d1, float t0, float t1, float r)
{
    float depth = linstep(d0, d1, r);
    return t0 + (t1 - t0) * depth;
}

//////////////////////////////////////////////
// AVSM Insertion Functions
//////////////////////////////////////////////

AVSMSegment FindSegmentAVSM(in AVSMData data, in float receiverDepth)
{
    int    index;
    vec4 depth, trans;
    float  leftDepth, rightDepth;
    float  leftTrans, rightTrans;

    AVSMSegment Output;

#if AVSM_RT_COUNT > 3
    if (receiverDepth > data.depth[2][3])
    {
        depth        = data.depth[3];
        trans        = data.trans[3];
        leftDepth    = data.depth[2][3];
        leftTrans    = data.trans[2][3];
#if AVSM_RT_COUNT == 4
        rightDepth   = data.depth[3][3];
        rightTrans   = data.trans[3][3];
#else
        rightDepth   = data.depth[4][0];
        rightTrans   = data.trans[4][0];
#endif
        Output.index = 12;
    }
    else
#endif
#if AVSM_RT_COUNT > 2
    if (receiverDepth > data.depth[1][3])
    {
        depth        = data.depth[2];
        trans        = data.trans[2];
        leftDepth    = data.depth[1][3];
        leftTrans    = data.trans[1][3];
#if AVSM_RT_COUNT == 3
        rightDepth   = data.depth[2][3];
        rightTrans   = data.trans[2][3];
#else
        rightDepth   = data.depth[3][0];
        rightTrans   = data.trans[3][0];
#endif
        Output.index = 8;
    }
    else
#endif
#if AVSM_RT_COUNT > 1
    if (receiverDepth > data.depth[0][3])
    {
        depth        = data.depth[1];
        trans        = data.trans[1];
        leftDepth    = data.depth[0][3];
        leftTrans    = data.trans[0][3];
#if AVSM_RT_COUNT == 2
        rightDepth   = data.depth[1][3];
        rightTrans   = data.trans[1][3];
#else
        rightDepth   = data.depth[2][0];
        rightTrans   = data.trans[2][0];
#endif
        Output.index = 4;
    }
    else
#endif
    {
        depth        = data.depth[0];
        trans        = data.trans[0];
        leftDepth    = data.depth[0][0];
        leftTrans    = data.trans[0][0];
#if AVSM_RT_COUNT == 1
        rightDepth   = data.depth[0][3];
        rightTrans   = data.trans[0][3];
#else
        rightDepth   = data.depth[1][0];
        rightTrans   = data.trans[1][0];
#endif
        Output.index = 0;
    }

    if (receiverDepth <= depth[0]) {
        Output.depthA = leftDepth;
        Output.depthB = depth[0];
        Output.transA = leftTrans;
        Output.transB = trans[0];
    } else if (receiverDepth <= depth[1]) {
        Output.index += 1;
        Output.depthA = depth[0];
        Output.depthB = depth[1];
        Output.transA = trans[0];
        Output.transB = trans[1];
    } else if (receiverDepth <= depth[2]) {
        Output.index += 2;
        Output.depthA = depth[1];
        Output.depthB = depth[2];
        Output.transA = trans[1];
        Output.transB = trans[2];
    } else if (receiverDepth <= depth[3]) {
        Output.index += 3;
        Output.depthA = depth[2];
        Output.depthB = depth[3];
        Output.transA = trans[2];
        Output.transB = trans[3];
    } else {
        Output.index += 4;
        Output.depthA = depth[3];
        Output.depthB = rightDepth;
        Output.transA = trans[3];
        Output.transB = rightTrans;
    }

    return Output;
}

float EvalAbsTransmittance(in AVSMData avsmData, in float receiverDepth)
{
    AVSMSegment seg = FindSegmentAVSM(avsmData, receiverDepth);
    return Interp(seg.depthA, seg.depthB, seg.transA, seg.transB, receiverDepth);
}

float AVSMPointSample(in float2 uv, in float receiverDepth)
{
    AVSMData avsmData;
    LoadDataLevel(avsmData, uv, 0.0f);

    return EvalAbsTransmittance(avsmData, receiverDepth);
}

float AVSMBilinearSample(in float2 textureCoords, in float receiverDepth)
{
    float2 unnormCoords = textureCoords * mShadowMapSize.xx;

    const float a = frac(unnormCoords.x - 0.5f);
    const float b = frac(unnormCoords.y - 0.5f);
    const float i = floor(unnormCoords.x - 0.5f);
    const float j = floor(unnormCoords.y - 0.5f);

    float sample00 = AVSMPointSample(float2(i, j)         / mShadowMapSize.xx, receiverDepth);
    float sample01 = AVSMPointSample(float2(i, j + 1)     / mShadowMapSize.xx, receiverDepth);
    float sample10 = AVSMPointSample(float2(i + 1, j)     / mShadowMapSize.xx, receiverDepth);
    float sample11 = AVSMPointSample(float2(i + 1, j + 1) / mShadowMapSize.xx, receiverDepth);

	return (1 - a)*(1 - b)*sample00 + a*(1-b)*sample10 + (1-a)*b*sample01 + a*b*sample11;
}

float AVSMGenBilinearSampleSoft(in float2 textureCoords, in float receiverDepth)
{
//	uint2 dim;
    float shadow = 1;
//	NONCPUT_gAVSMGenClearMaskSRV.GetDimensions(dim[0], dim[1]);
	int2 dim = textureSize(NONCPUT_gAVSMGenClearMaskSRV, 0);
	const float2 unnormCoords = textureCoords * float2(dim);

	const float a = frac(unnormCoords.x  - 0.5f);
	const float b = frac(unnormCoords.y  - 0.5f);
	const int i   = int(unnormCoords.x - 0.5f);
	const int j   = int(unnormCoords.y - 0.5f);

	// replacing the following four texture fetches with a gather4
	// generates artifacts since the second group of four
	// texture fetches uses texture coordinates that don't always
	// match the HW generated coordinates used by gather4
	bool4 mustSample;
	mustSample[0] = (0. != texelFetch(NONCPUT_gAVSMGenClearMaskSRV, int2(i, j), 0).x);
	mustSample[1] = (0. != texelFetchOffset(NONCPUT_gAVSMGenClearMaskSRV, int2(i, j), 0, int2(0,1)).x);
	mustSample[2] = (0. != texelFetchOffset(NONCPUT_gAVSMGenClearMaskSRV, int2(i, j), 0, int2(1,0)).x);
	mustSample[3] = (0. != texelFetchOffset(NONCPUT_gAVSMGenClearMaskSRV, int2(i, j), 0, int2(1,1)).x);

	/*[branch]*/if (any(mustSample)) {
		const float sample00 = mustSample[0] ? AVSMGenPointSampleInternalSoft(int2(i, j)         , receiverDepth) : 1;
		const float sample01 = mustSample[1] ? AVSMGenPointSampleInternalSoft(int2(i, j + 1)     , receiverDepth) : 1;
		const float sample10 = mustSample[2] ? AVSMGenPointSampleInternalSoft(int2(i + 1, j)     , receiverDepth) : 1;
		const float sample11 = mustSample[3] ? AVSMGenPointSampleInternalSoft(int2(i + 1, j + 1) , receiverDepth) : 1;
		shadow = (1 - a)*(1 - b)*sample00 + a*(1-b)*sample10 + (1-a)*b*sample01 + a*b*sample11;
	}
    return shadow;
}

// Generalized volume sampling function
float VolumeSample(in uint method, in float2 textureCoords, in float receiverDepth)
{
    switch (method) {
        case(VOL_SHADOW_AVSM):
#ifdef AVSM_BILINEARF
            return AVSMBilinearSample(textureCoords, receiverDepth);
#else
//            return AVSMPointSample(textureCoords, receiverDepth);
#endif
        case(VOL_SHADOW_AVSM_GEN):
#ifdef AVSM_GEN_SOFT
    #ifdef AVSM_GEN_BILINEARF
			    return AVSMGenBilinearSampleSoft(textureCoords, receiverDepth);
    #else
//                return AVSMGenPointSampleSoft(textureCoords, receiverDepth);
    #endif
#else
    #ifdef AVSM_GEN_BILINEARF
//			    return AVSMGenBilinearSample(textureCoords, receiverDepth);
    #else
//                return AVSMGenPointSample(textureCoords, receiverDepth);
    #endif
#endif
        default:
            return 1.0f;
    }
}

float ShadowContrib( float3 viewspacePos )
{
    float2 lightTexCoord = ProjectIntoAvsmLightTexCoord(viewspacePos);
    float receiverDepth = mul(float4(viewspacePos, 1.0f), mCameraViewToAvsmLightView).z;

    return VolumeSample(mUI.volumeShadowMethod, lightTexCoord, -receiverDepth);
}