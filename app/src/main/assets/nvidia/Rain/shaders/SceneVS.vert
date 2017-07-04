#include "Rain_Common.glsl"

layout(location = 0) in float3 In_Position;
layout(location = 1) in float3 In_Normal;
layout(location = 2) in float2 In_Texture;
layout(location = 3) in float3 In_Tan;

out VS_OUTPUT_SCENE
{
//    float4 Position            : SV_POSITION;
    float3 Normal;//              : NORMAL;
    float3 Tan;//                 : TANGENT;
    float4 worldPos;//            : WPOSITION;
    float2 Texture;//             : TEXTURE0;
}_output;

void main()
{
#if 0
    float3 GL_Position = float3(In_Position.xy, -In_Position.z);
    float3 GL_Normal   = float3(In_Normal.x, In_Normal.y, -In_Normal.z);

    gl_Position      = mul( float4(GL_Position,1), g_mWorldViewProj );
    _output.Normal   = mul( GL_Normal, float3x3(g_mWorld) );
    _output.Tan      = normalize( mul( float3(1,0,0), float3x3(g_mWorld) ) );
    _output.worldPos = mul( float4(GL_Position,1), g_mWorld );
//    float3 worldPos = mul( In_Position, g_mWorld );
    _output.Texture  = In_Texture;
#else
    _output.Normal = In_Normal;
    _output.Tan = In_Tan;
    _output.worldPos = float4(In_Position, 1);
    _output.Texture = In_Texture;
#endif
}