#include "DebugDraw.glsl"

layout(location = 0) in float3 In_Position;
layout(location = 1) in float2 In_Texcoord;
layout(location = 2) in float3 In_Normal;

out VS_OUTPUT_DRAW_SPOT_LIGHTS
{
//    float4 Position     : SV_POSITION; // vertex position
    float3 Normal       /*: NORMAL*/;      // vertex normal vector
    float4 Color        /*: COLOR0*/;      // vertex color
    float2 TextureUV    /*: TEXCOORD0*/;   // vertex texture coords
    float3 PositionWS   /*: TEXCOORD1*/;   // vertex position (world space)
}_output;

//--------------------------------------------------------------------------------------
// This shader reads from the spot light buffers to place and orient a particular
// instance of a cone.
//--------------------------------------------------------------------------------------
void main()
{
    float4 BoundingSphereCenterAndRadius = g_SpotLightBufferCenterAndRadius[gl_InstanceID];
    float4 SpotParams = g_SpotLightBufferSpotParams[gl_InstanceID];

    // reconstruct z component of the light dir from x and y
    float3 SpotLightDir;
    SpotLightDir.xy = SpotParams.xy;
    SpotLightDir.z = sqrt(1 - SpotLightDir.x*SpotLightDir.x - SpotLightDir.y*SpotLightDir.y);

    // the sign bit for cone angle is used to store the sign for the z component of the light dir
    SpotLightDir.z = (SpotParams.z > 0) ? SpotLightDir.z : -SpotLightDir.z;

    // calculate the light position from the bounding sphere (we know the top of the cone is
    // r_bounding_sphere units away from the bounding sphere center along the negated light direction)
    float3 LightPosition = BoundingSphereCenterAndRadius.xyz - BoundingSphereCenterAndRadius.w*SpotLightDir;

    // rotate the light to point along the light direction vector
    float4x4 LightRotation = float4x4( g_SpotLightBufferSpotMatrices[4*Input.InstanceID],
                               g_SpotLightBufferSpotMatrices[4*Input.InstanceID+1],
                               g_SpotLightBufferSpotMatrices[4*Input.InstanceID+2],
                               g_SpotLightBufferSpotMatrices[4*Input.InstanceID+3] );
    float3 VertexPosition = mul( In_Position, float3x3(LightRotation) ) + LightPosition;
    float3 VertexNormal = mul( In_Normal, float3x3(LightRotation) );

    // transform the position to homogeneous projection space
    gl_Position = mul( float4(VertexPosition,1), g_mViewProjection );

    // position and normal in world space
    Output.PositionWS = VertexPosition;//, (float3x3)g_mWorld );
    Output.Normal = VertexNormal;//, (float3x3)g_mWorld );

    // pass through color from the light buffer and tex coords from the vert data
    Output.Color = g_SpotLightBufferColor[gl_InstanceID];
    Output.TextureUV = Input.TextureUV;
}