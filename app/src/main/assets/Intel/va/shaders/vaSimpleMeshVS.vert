
#include "vaShared.glsl"
#include "vaSimpleShadowMap.glsl"

layout(location = 0) vec4 Position             ;//: SV_Position;
layout(location = 1) vec4 Color                ;//: COLOR;
layout(location = 2) vec4 Normal                ;//: NORMAL0;
layout(location = 3) vec4 Tangent     ;//: NORMAL1;
layout(location = 4) vec4 Texcoord            ;//: TEXCOORD1;

out  GenericSceneVertexTransformed
{
   float4 Position             ;//: SV_Position;
   float4 Color                ;//: COLOR;
   float4 ViewspacePos         ;//: TEXCOORD0;
   float4 ViewspaceNormal      ;//: NORMAL0;
   float4 ViewspaceTangent     ;//: NORMAL1;
   float4 ViewspaceBitangent   ;//: NORMAL2;
   float4 Texcoord0            ;//: TEXCOORD1;
}ret;

void main()
{

    ret.Color                   = Color;
    ret.Texcoord0               = Texcoord;

    ret.ViewspacePos            = mul( g_PerInstanceConstants.WorldView, float4( Position.xyz, 1) );
    ret.ViewspaceNormal.xyz     = normalize( mul( g_PerInstanceConstants.WorldView, float4(Normal.xyz, 0.0) ).xyz );
    ret.ViewspaceTangent.xyz    = normalize( mul( g_PerInstanceConstants.WorldView, float4(Tangent.xyz, 0.0) ).xyz );
    ret.ViewspaceBitangent.xyz  = normalize( cross( ret.ViewspaceNormal.xyz, ret.ViewspaceTangent.xyz) );

    // distance to camera
    ret.ViewspacePos.w          = length( ret.ViewspacePos.xyz );

    gl_Position                 = mul( g_Global.Proj, float4( ret.ViewspacePos.xyz, 1.0 ) );

    ret.ViewspaceNormal.w       = 0.0;
    ret.ViewspaceTangent.w      = 0.0;
    ret.ViewspaceBitangent.w    = 0.0;
}