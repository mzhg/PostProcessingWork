#include "CommonHeader.glsl"
#include "LightingCommonHeader.glsl"

layout(location = 0) in float3 In_Position;
layout(location = 1) in float2 In_Texcoord;
layout(location = 2) in float3 In_Normal;
layout(location = 3) in float3 In_Tangent;


out VS_OUTPUT_ALPHA_BLENDED
{
//    float4 Position     : SV_POSITION; // vertex position
    float3 Normal       /*: NORMAL*/;      // vertex normal vector
    float3 PositionWS   /*: TEXCOORD0*/;   // vertex position (world space)
}Output;

//--------------------------------------------------------------------------------------
// This shader transforms position, calculates world-space position and normal,
// and passes tex coords through to the pixel shader.
//--------------------------------------------------------------------------------------
void main()
{
    matrix mWorld = g_InstanceTransform[gl_InstanceID];

    // Transform the position from object space to homogeneous projection space
    float4 vWorldPos = mul( float4(In_Position,1), mWorld );
    gl_Position = mul( vWorldPos, g_mViewProjection );

    // Position and normal in world space
    Output.PositionWS = vWorldPos.xyz;
    Output.Normal = mul( In_Normal, float3x3(mWorld) );
}