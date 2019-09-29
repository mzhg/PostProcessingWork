
// The pragma below is critical for optimal performance
// in this fragment shader to let the shader compiler
// fully optimize the maths and batch the texture fetches
// optimally
#ifndef GL_ES
//#pragma optionNV(unroll all)
/*
#pragma optionNV(fastmath on)
#pragma optionNV(fastprecision on)
#pragma optionNV(inline all)
#pragma optionNV(ifcvt none)
#pragma optionNV(strict on)*/
#endif

#define float2 vec2
#define bool4 bvec4

#define float3 vec3
#define float4 vec4
#define bool2 bvec2
#define bool3 bvec3
#define float2x2 mat2
#define float3x3 mat3
#define float4x4 mat4
#define matrix   mat4
#define float4x3 mat4x3

#define half2 vec2
#define half3 vec3
#define half4 vec4

#define uint2 uvec2
#define uint3 uvec3
#define uint4 uvec4

#define int2 ivec2
#define int3 ivec3
#define int4 ivec4

#define lerp(x,y,v) mix(x,y,v)
#define rcp(x) 1.0/x
#define saturate(x) clamp(x, 0.0, 1.0)
#define frac(x) fract(x)
#define rsqrt(x) inversesqrt(x)
#define InterlockedOr(x, y) atomicOr(x, y)
#define firstbithigh(x) findMSB(x)
#define firstbitlow(x)  findLSB(x)
#define atan2(y,x)  atan(y,x)
#define reversebits(x) bitfieldReverse(x)
#define countbits(x)   bitCount(x)
#define asuint(x) floatBitsToUint(x)
#define ddx(x)    dFdx(x)
#define ddy(x)    dFdy(x)

// #define mul(M, V) M * V

#define isfinite(x) !(isnan(x) || isinf(x))

#ifndef GroupMemoryBarrierWithGroupSync
#define GroupMemoryBarrierWithGroupSync barrier
#endif
#define groupshared shared

#define GroupMemoryBarrier memoryBarrier

vec4 mul(in vec4 v, in mat4 m )
{
	return m * v;
}

vec3 mul(in vec3 v, in mat3 m )
{
	return m * v;
}

vec4 mul(in mat4 m , in vec4 v)
{
	return m * v;
}

vec3 mul(in mat3 m , in vec3 v)
{
	return m * v;
}

vec2 mul(in mat2 m , in vec2 v)
{
	return m * v;
}

void sincos(float angle, out float _sin, out float _cos)
{
    _sin = sin(angle);
    _cos = cos(angle);
}

void sincos(float2 angle, out float2 _sin, out float2 _cos)
{
    _sin = sin(angle);
    _cos = cos(angle);
}

float asfloat(uint i)
{
    return uintBitsToFloat(i);
}

float asfloat(int i)
{
    return intBitsToFloat(i);
}

float f16tof32( in uint value)
{
    return unpackHalf2x16(value).x;
}

float2 f16tof32( in uint2 value)
{
    return float2(unpackHalf2x16(value.x).x, unpackHalf2x16(value.y).x);
}

float3 f16tof32( in uint3 value)
{
    return float3(unpackHalf2x16(value.x).x, unpackHalf2x16(value.y).x, unpackHalf2x16(value.z).x);
}

float4 f16tof32( in uint4 value)
{
    return float4(unpackHalf2x16(value.x).x, unpackHalf2x16(value.y).x, unpackHalf2x16(value.z).x, unpackHalf2x16(value.w).x);
}

uint f32tof16(in float value)
{
    return packHalf2x16(vec2(value, 0));
}

uint2 f32tof16(in vec2 value)
{
    return uint2(packHalf2x16(vec2(value.x, 0)), packHalf2x16(vec2(value.y, 0)));
}

uint3 f32tof16(in float3 value)
{
    return uint3(packHalf2x16(vec2(value.x, 0)), packHalf2x16(vec2(value.y, 0)), packHalf2x16(vec2(value.z, 0)));
}


uint4 f32tof16(in float4 value)
{
    return uint4(packHalf2x16(vec2(value.x, 0)), packHalf2x16(vec2(value.y, 0)), packHalf2x16(vec2(value.z, 0)), packHalf2x16(vec2(value.w, 0)));
}

#define SV_DispatchThreadID gl_GlobalInvocationID