#include "DebugDraw.glsl"

layout(location = 0) in float3 In_Position;
layout(location = 1) in float2 In_Texcoord;

out VS_OUTPUT_DRAW_POINT_LIGHTS
{
//    float4 Position     : SV_POSITION; // vertex position
    float3 Color        /*: COLOR0*/;      // vertex color
    float2 TextureUV    /*: TEXCOORD0*/;   // vertex texture coords
}_output;


out gl_PerVertex
{
    float4 gl_Position;
};

//--------------------------------------------------------------------------------------
// This shader reads from the light buffer to create a screen-facing quad
// at each light position.
//--------------------------------------------------------------------------------------
void main()
{
    // get the light position from the light buffer (this will be the quad center)
    float4 LightPositionViewSpace = mul( float4(texelFetch(g_PointLightBufferCenterAndRadius, gl_InstanceID).xyz,1), g_mView );

    // move from center to corner in view space (to make a screen-facing quad)
    LightPositionViewSpace.xy = LightPositionViewSpace.xy + In_Position.xy;

    // transform the position from view space to homogeneous projection space
    gl_Position = mul( LightPositionViewSpace, g_mProjection );

    // pass through color from the light buffer and tex coords from the vert data
    _output.Color = texelFetch(g_PointLightBufferColor, gl_InstanceID).rgb;
    _output.TextureUV = In_Texcoord;
}