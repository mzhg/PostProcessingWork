#include "CommonHeader.glsl"

in VS_OUTPUT
{
    float3 Normal       /*: NORMAL*/;      // vertex normal vector
    float2 TextureUV    /*: TEXCOORD0*/;   // vertex texture coords
    float3 PositionWS   /*: TEXCOORD1*/;
//    float4 Position     : SV_POSITION; // vertex position
}Input;

layout(location = 0) out vec4 Out_Normal;
layout(location = 1) out vec4 Out_Diffuse;

layout(binding = 0) uniform sampler2D g_TxDiffuse;

void main()
{
    Out_Normal.xyz = 0.5 * (1 + normalize( Input.Normal ));
    Out_Normal.w = 0;
    Out_Diffuse = texture( g_TxDiffuse, Input.TextureUV );
    Out_Diffuse.a = 1;
}