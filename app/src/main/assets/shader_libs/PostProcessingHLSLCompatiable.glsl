
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