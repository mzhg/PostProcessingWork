#include "SimpleShading.glsl"

layout(location = 0) in float3 Pos /*: POSITION*/;
layout(location = 1) in float3 Normal /*: NORMAL*/;
layout(location = 2) in float2 TextureUV /*: TEXCOORD0*/;
layout(location = 3) in float3 Tangent /*: TANGENT*/;

out VS_OUTPUT {
//    float4 Pos : SV_POSITION;
    float3 Normal /*: NORMAL*/;
    float2 TextureUV /*: TEXCOORD0*/;
    float3 worldPos /*: TEXCOORD1*/;
    float4 LightPos /*: TEXCOORD2*/;
    float3 LPVSpacePos /*: TEXCOORD3*/;
    float3 LPVSpacePos2 /*: TEXCOORD4*/;

    float3 LPVSpaceUnsnappedPos /*: TEXCOORD5*/;
    float3 LPVSpaceUnsnappedPos2 /*: TEXCOORD6*/;

    float3 tangent  /*: TANGENT*/;
    float3 binorm   /*: BINORMAL*/;
}f;

out gl_PerVertex
{
    vec4 gl_Position;
};

void main()
{
    gl_Position = mul( float4( Pos, 1 ), g_WorldViewProj );
    f.Normal = normalize( mul( Normal, float3x3(g_World ) ));
    f.TextureUV = TextureUV;
    float3 worldPos = mul( float4( Pos, 1 ), g_World );
    f.worldPos = worldPos;
    f.LightPos = mul( float4( Pos, 1 ), g_WorldViewProjClip2Tex);
    f.LPVSpacePos = mul( float4(worldPos,1), g_WorldToLPVSpace );
    f.LPVSpacePos2 = mul( float4(worldPos,1), g_WorldToLPVSpace2 );

    f.LPVSpaceUnsnappedPos = mul( float4(worldPos,1), g_WorldToLPVSpaceRender );
    f.LPVSpaceUnsnappedPos2 = mul( float4(worldPos,1), g_WorldToLPVSpaceRender2 );

    f.tangent = normalize(mul(Tangent.xyz, float3x3(g_World )));
    f.binorm = cross(f.Normal,f.tangent);
}