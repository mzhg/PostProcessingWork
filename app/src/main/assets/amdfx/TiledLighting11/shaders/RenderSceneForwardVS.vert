#include "Forward.glsl"

layout(location = 0) in float3 In_Position;
layout(location = 1) in float2 In_Texcoord;
layout(location = 2) in float3 In_Normal;
layout(location = 3) in float3 In_Tangent;

out VS_OUTPUT_SCENE
{
//    float4 Position     : SV_POSITION; // vertex position
    float3 Normal       /*: NORMAL*/;      // vertex normal vector
    float2 TextureUV    /*: TEXCOORD0*/;   // vertex texture coords
    float3 Tangent      /*: TEXCOORD1*/;   // vertex tangent vector
    float3 PositionWS   /*: TEXCOORD2*/;   // vertex position (world space)
}Output;

//--------------------------------------------------------------------------------------
// This shader transforms position, calculates world-space position, normal,
// and tangent, and passes tex coords through to the pixel shader.
//--------------------------------------------------------------------------------------
void main()
{
    // Transform the position from object space to homogeneous projection space
    float4 vWorldPos = mul( float4(In_Position,1), g_mWorld );
    gl_Position = mul( vWorldPos, g_mViewProjection );

    // Position, normal, and tangent in world space
    Output.PositionWS = vWorldPos.xyz;
    Output.Normal = mul( In_Normal, float3x3(g_mWorld) );
    Output.Tangent = mul( In_Tangent, float3x3(g_mWorld) );

    // Just copy the texture coordinate through
    Output.TextureUV = In_Texcoord;
}