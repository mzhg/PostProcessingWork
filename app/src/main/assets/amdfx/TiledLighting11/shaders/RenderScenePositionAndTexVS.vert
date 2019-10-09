#include "Forward.glsl"

layout(location = 0) in float3 In_Position;
layout(location = 1) in float2 In_Texcoord;
layout(location = 2) in float3 In_Normal;
layout(location = 3) in float3 In_Tangent;

out vec2 TextureUV;

out gl_PerVertex
{
    float4 gl_Position;
};

//--------------------------------------------------------------------------------------
// This shader just transforms position and passes through tex coord
// (e.g. for depth pre-pass with alpha test)
//--------------------------------------------------------------------------------------
void main()
{
    // Transform the position from object space to homogeneous projection space
    float4 vWorldPos = mul( float4(In_Position,1), g_mWorld );
    gl_Position = mul( vWorldPos, g_mViewProjection );

    // Just copy the texture coordinate through
    TextureUV = In_Texcoord;
}