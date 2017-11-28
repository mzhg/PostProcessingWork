#include "SimpleShading.glsl"

in VS_OUTPUT_RSM_DP {
    float4 Pos /*: SV_POSITION*/;
    float3 Normal /*: NORMAL*/;
    float2 TextureUV /*: TEXCOORD0*/;
    float3 worldPos /*: TEXCOORD1*/;
    float4 depthMapuv /*: TEXCOORD2*/;

    float2 ScreenTex      /*: TEXCOORD3*/;
    float2 Depth          /*: TEXCOORD4*/;
}f;

layout(location = 0) out float4 Color    /*: SV_Target0*/;
layout(location = 1) out float4 Normal    /*: SV_Target1*/;
layout(location = 2) out float4 Albedo   /*: SV_Target2*/;

void main()
{
    float epsilon = 0.0f;

    float2 uv = f.depthMapuv.xy / f.depthMapuv.w;
    float val = textureLod(g_txPrevDepthBuffer, uv, 0.0);  // DepthSampler

    float currentZValue = f.Pos.z;
     if( currentZValue <= (val+epsilon) )
        discard;

    float3 LightDir = g_lightWorldPos.xyz - f.worldPos.xyz;
    float LightDistSq = dot(LightDir, LightDir);
    LightDir = normalize(LightDir);
    float diffuse = max( dot( LightDir, normalize(f.Normal) ), 0) * saturate(1.f - LightDistSq * g_lightWorldPos.w);

    float3 albedo = float3(1,1,1);
    if(g_useTexture)
        albedo = g_txDiffuse.Sample( samAniso, f.TextureUV ).rgb;

    Color = float4(diffuse * albedo, 1.0f );

    Albedo = float4(albedo,1);

    float3 encodedNormal = f.Normal*float3(0.5f,0.5f,0.5f) + float3(0.5f,0.5f,0.5f);
    Normal = float4(encodedNormal,1.0f);
}