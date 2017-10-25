#include "Fire_Common.glsl"

layout(location=0) in vec3 In_Position;
layout(location=1) in vec3 In_Normal;
layout(location=2) in vec2 In_TexCoord;

out MeshVertex
{
    float3 Pos /*: POSITION*/;
    float3 Normal /*: NORMAL*/;
    float2 TexCoord /*: TEXCOORD0*/;
}Out;

void main()
{
    gl_Position = mul( float4( In_Position, 1 ), WorldViewProj );
    Out.Pos = In_Position;
    Out.Normal = In_Normal;
    Out.TexCoord = In_TexCoord;
}