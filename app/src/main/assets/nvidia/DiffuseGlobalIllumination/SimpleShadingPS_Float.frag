#include "SimpleShading.glsl"

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

layout(location = 0) out float4 Out_Color;

void main()
{
    float3 LightDir = g_lightWorldPos.xyz - f.worldPos.xyz;
    float LightDistSq = dot(LightDir, LightDir);
    LightDir = normalize(LightDir);
    float diffuse = 0.0f;

    //load the albedo
    float3 albedo = float3(1,1,1);
    if(g_useTexture)
        albedo = texture(g_txDiffuse , f.TextureUV ).rgb;  // samAniso

    //load the alphamask
    float alpha = 1.0f;
    if (g_useAlpha)
        alpha = texture(g_txAlpha, f.TextureUV ).r;

    //load and use the normal map
    float3 Normal = loadNormal(f);

    //compute the direct lighting
    diffuse = max( dot( LightDir, Normal ), 0);
    diffuse *= g_directLight * saturate(1.f - LightDistSq * g_lightWorldPos.w);

    //compute the shadows
    float shadow = 1.0f;
    if(g_bUseSM)
    {
        float2 uv = f.LightPos.xy / f.LightPos.w;
        float z = f.LightPos.z / f.LightPos.w;
        shadow = lookupShadow(uv,z);
    }

    //compute the amount of diffuse inter-reflected light
    float3 diffuseGI = float3(0,0,0);
    if(g_useDiffuseInterreflection)
    {
        //get the radiance distribution by trilinearly interpolating the SH coefficients
        //evaluate the irradiance by convolving the obtained radiant distribution with the cosine lobe of the surface's normal (from the presentation at I3D)

        float4 surfaceNormalLobe;
        clampledCosineCoeff(float3(Normal.x,Normal.y,Normal.z), surfaceNormalLobe);

        float inside = 1;
        float4 SHred   = float4(0,0,0,0);
        float4 SHgreen = float4(0,0,0,0);
        float4 SHblue  = float4(0,0,0,0);

        lookupSHSamples(f.LPVSpacePos, f.LPVSpaceUnsnappedPos, SHred, SHgreen, SHblue, inside,
                        g_propagatedLPVRed,   g_propagatedLPVRed_1,   g_propagatedLPVRed_2,   g_propagatedLPVRed_3,
                        g_propagatedLPVGreen, g_propagatedLPVGreen_1, g_propagatedLPVGreen_2, g_propagatedLPVGreen_3,
                        g_propagatedLPVBlue,  g_propagatedLPVBlue_1,  g_propagatedLPVBlue_2,  g_propagatedLPVBlue_3,
                        g_numCols1, g_numRows1, LPV2DWidth1, LPV2DHeight1, LPV3DWidth1, LPV3DHeight1, LPV3DDepth1 );

        diffuseGI.r = innerProductSH( SHred, surfaceNormalLobe ) * inside;
        diffuseGI.g = innerProductSH( SHgreen, surfaceNormalLobe ) * inside;
        diffuseGI.b = innerProductSH( SHblue, surfaceNormalLobe ) * inside;

        if(inside<1 && g_minCascadeMethod && g_numCascadeLevels>1)
        {
            float inside2 = 1;

            lookupSHSamples(f.LPVSpacePos2, f.LPVSpaceUnsnappedPos, SHred, SHgreen, SHblue, inside2,
                        g_propagatedLPV2Red, g_propagatedLPV2Red_1, g_propagatedLPV2Red_2, g_propagatedLPV2Red_3,
                        g_propagatedLPV2Green, g_propagatedLPV2Green_1, g_propagatedLPV2Green_2, g_propagatedLPV2Green_3,
                        g_propagatedLPV2Blue, g_propagatedLPV2Blue_1, g_propagatedLPV2Blue_2, g_propagatedLPV2Blue_3,
                        g_numCols2, g_numRows2, LPV2DWidth2, LPV2DHeight2, LPV3DWidth2, LPV3DHeight2, LPV3DDepth2 );

            diffuseGI.r += (1.0f - inside)*innerProductSH( SHred, surfaceNormalLobe ) * inside2;
            diffuseGI.g += (1.0f - inside)*innerProductSH( SHgreen, surfaceNormalLobe ) * inside2;
            diffuseGI.b += (1.0f - inside)*innerProductSH( SHblue, surfaceNormalLobe ) * inside2;

        }

        diffuseGI *= float4(g_diffuseScale/PI,g_diffuseScale/PI,g_diffuseScale/PI,1);
    }

    Out_Color = float4(shadow.xxx *diffuse*albedo*0.8 + diffuseGI.xyz*albedo + g_ambientLight*albedo, alpha);
}