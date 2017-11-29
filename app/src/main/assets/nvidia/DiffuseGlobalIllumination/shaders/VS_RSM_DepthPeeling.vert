#include "SimpleShading.glsl"

layout(location = 0) in float3 Pos /*: POSITION*/;
layout(location = 1) in float3 Normal /*: NORMAL*/;
layout(location = 2) in float2 TextureUV /*: TEXCOORD0*/;
layout(location = 3) in float3 Tangent /*: TANGENT*/;

out VS_OUTPUT_RSM_DP {
    float4 Pos /*: SV_POSITION*/;
    float3 Normal /*: NORMAL*/;
    float2 TextureUV /*: TEXCOORD0*/;
    float3 worldPos /*: TEXCOORD1*/;
    float4 depthMapuv /*: TEXCOORD2*/;

    float2 ScreenTex      /*: TEXCOORD3*/;
    float2 Depth          /*: TEXCOORD4*/;
}f;

out gl_PerVertex
{
    vec4 gl_Position;
};

void main()
{
    gl_Position = mul( float4( Pos, 1 ), g_WorldViewProj );
    f.Normal = normalize( mul( Normal, float3x3(g_World) ) );
    f.TextureUV = TextureUV;
    f.worldPos = mul( float4( Pos, 1 ), g_World );
    f.depthMapuv = mul( float4( Pos, 1 ), g_WorldViewProjClip2Tex);

    // screenspace coordinates for the lookup into the depth buffer
    f.ScreenTex = f.Pos.xy/f.Pos.w;
    // output depth of this particle
    f.Depth = f.Pos.zw;
}