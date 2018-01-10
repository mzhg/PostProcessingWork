
#include "vaShared.glsl"

#include "vaSimpleShadowMap.glsl"

layout(location = 0) in vec4 In_Position; //             : SV_Position;
layout(location = 1) in vec4 In_Color; //                : COLOR;
layout(location = 2) in vec4 In_Normal; //               : NORMAL;
layout(location = 3) in vec4 In_Tangent; //              : TANGENT;
layout(location = 4) in vec2 In_Texcoord0; //            : TEXCOORD0;
layout(location = 5) in vec2 In_Texcoord1; //            : TEXCOORD1;

out gl_PerVertex
{
    vec4 gl_Position;
};

out RenderMeshStandardVertexInput
{
//    float4 Position             : SV_Position;
    float4 Color                ;//: COLOR;
    float4 ViewspacePos         ;//: TEXCOORD0;
    float4 ViewspaceNormal      ;//: NORMAL0;
    float4 ViewspaceTangent     ;//: NORMAL1;
    float4 ViewspaceBitangent   ;//: NORMAL2;
    float4 Texcoord0            ;//: TEXCOORD1;
}ret;

void main()
{
    ret.Color                   = In_Color;
    ret.Texcoord0               = float4( In_Texcoord0, In_Texcoord1 );

    ret.ViewspacePos            = mul( g_RenderMeshGlobal.WorldView, In_Position );
    ret.ViewspaceNormal.xyz     = normalize( mul( g_RenderMeshGlobal.WorldView, float4(In_Normal.xyz, 0.0) ).xyz );
    ret.ViewspaceTangent.xyz    = normalize( mul( g_RenderMeshGlobal.WorldView, float4(In_Tangent.xyz, 0.0) ).xyz );
    ret.ViewspaceBitangent.xyz  = normalize( cross( ret.ViewspaceNormal.xyz, ret.ViewspaceTangent.xyz) * In_Tangent.w );     // Tangent.w contains handedness/uv.y direction!

    // distance to camera
    ret.ViewspacePos.w          = length( ret.ViewspacePos.xyz );

    gl_Position                 = mul( g_Global.Proj, float4( ret.ViewspacePos.xyz, 1.0 ) );

    ret.ViewspaceNormal.w       = 0.0;
    ret.ViewspaceTangent.w      = 0.0;
    ret.ViewspaceBitangent.w    = 0.0;
}