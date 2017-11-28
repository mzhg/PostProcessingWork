
in VS_OUTPUT {
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
//output color and light space depth to the color MRT (0)
//output world space normal to the normal MRT (1)

layout(location = 0) out float4 Color    /*: SV_Target0*/;
layout(location = 1) out float4 Normal    /*: SV_Target1*/;
layout(location = 2) out float4 Albedo   /*: SV_Target2*/;

void main()
{
    float3 LightDir = g_lightWorldPos.xyz - f.worldPos.xyz;
    float LightDistSq = dot(LightDir, LightDir);
    LightDir = normalize(LightDir);
    float diffuse = max( dot( LightDir, normalize(f.Normal) ), 0) * saturate(1.f - LightDistSq * g_lightWorldPos.w);

    float3 albedo = float3(1,1,1);
    if(g_useTexture)
        albedo = g_txDiffuse.Sample( samAniso, f.TextureUV ).rgb;

    Color = float4(diffuse * albedo, 1);
    Albedo = float4(albedo,1);

    float3 encodedNormal = f.Normal*float3(0.5f,0.5f,0.5f) + float3(0.5f,0.5f,0.5f);
    Normal = float4(encodedNormal,1.0f);
}