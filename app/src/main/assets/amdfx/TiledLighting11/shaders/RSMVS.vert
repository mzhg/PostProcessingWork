#include "CommonHeader.glsl"

layout(location = 0) in float3 In_Position;
layout(location = 1) in float2 In_Texcoord;
layout(location = 2) in float3 In_Normal;
layout(location = 3) in float3 In_Tangent;

out VS_OUTPUT
{
    float3 Normal       /*: NORMAL*/;      // vertex normal vector
    float2 TextureUV    /*: TEXCOORD0*/;   // vertex texture coords
    float3 PositionWS   /*: TEXCOORD1*/;
//    float4 Position     : SV_POSITION; // vertex position
}Output;

//VS_OUTPUT RSMVS( VS_INPUT Input )
void main()
{
    float4 vWorldPos = mul( float4(In_Position,1), g_mWorld );

    Output.PositionWS = vWorldPos.xyz;
    gl_Position = mul( vWorldPos, g_mViewProjection );

    Output.Normal = mul( In_Normal, float3x3(g_mWorld) );
    Output.TextureUV = In_Texcoord;
}