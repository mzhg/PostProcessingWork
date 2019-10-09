#include "Forward.glsl"

in VS_OUTPUT_SCENE
{
//    float4 Position     : SV_POSITION; // vertex position
    float3 Normal       /*: NORMAL*/;      // vertex normal vector
    float2 TextureUV    /*: TEXCOORD0*/;   // vertex texture coords
    float3 Tangent      /*: TEXCOORD1*/;   // vertex tangent vector
    float3 PositionWS   /*: TEXCOORD2*/;   // vertex position (world space)
}Input;

out float4 Out_Color;

//--------------------------------------------------------------------------------------
// This shader calculates diffuse and specular lighting for all lights.
//--------------------------------------------------------------------------------------
//float4 RenderSceneForwardPS( VS_OUTPUT_SCENE Input ) : SV_TARGET
void main()
{
    float3 vPositionWS = Input.PositionWS;

    float3 AccumDiffuse = float3(0,0,0);
    float3 AccumSpecular = float3(0,0,0);

    float4 DiffuseTex = texture( g_TxDiffuse, Input.TextureUV );

#if ( USE_ALPHA_TEST == 1 )
    float fSpecMask = 0.0f;
    float fAlpha = DiffuseTex.a;
    if( fAlpha < g_fAlphaTest ) discard;
#else
    float fSpecMask = DiffuseTex.a;
#endif

    // get normal from normal map
    float3 vNorm = texture( g_TxNormal, Input.TextureUV ).xyz;
    vNorm *= 2.0;
    vNorm -= float3(1,1,1);

    // transform normal into world space
    float3 vBinorm = normalize( cross( Input.Normal, Input.Tangent ) );
    float3x3 BTNMatrix = float3x3( vBinorm, Input.Tangent, Input.Normal );
    vNorm = normalize(mul( vNorm, BTNMatrix ));

    float3 vViewDir = normalize( g_vCameraPos - vPositionWS );

    // loop over the point lights
    {
        uint nStartIndex, nLightCount;
        GetLightListInfo(g_PerTileLightIndexBuffer, g_uMaxNumLightsPerTile, g_uMaxNumElementsPerTile, gl_FragCoord, nStartIndex, nLightCount);

        for ( uint i = nStartIndex; i < nStartIndex+nLightCount; i++ )
        {
            int nLightIndex = int(texelFetch(g_PerTileLightIndexBuffer, int(i)));

            float3 LightColorDiffuseResult;
            float3 LightColorSpecularResult;

#if ( SHADOWS_ENABLED == 1 )
            DoLighting(true, g_PointLightBufferCenterAndRadius, g_PointLightBufferColor, nLightIndex, vPositionWS, vNorm, vViewDir, LightColorDiffuseResult, LightColorSpecularResult);
#else
            DoLighting(false, g_PointLightBufferCenterAndRadius, g_PointLightBufferColor, nLightIndex, vPositionWS, vNorm, vViewDir, LightColorDiffuseResult, LightColorSpecularResult);
#endif

            AccumDiffuse += LightColorDiffuseResult;
            AccumSpecular += LightColorSpecularResult;
        }
    }

    // loop over the spot lights
    {
        uint nStartIndex, nLightCount;
        GetLightListInfo(g_PerTileSpotIndexBuffer, g_uMaxNumLightsPerTile, g_uMaxNumElementsPerTile, gl_FragCoord, nStartIndex, nLightCount);

        for ( uint i = nStartIndex; i < nStartIndex+nLightCount; i++ )
        {
//            uint nLightIndex = g_PerTileSpotIndexBuffer[i];
            int nLightIndex = int(texelFetch(g_PerTileSpotIndexBuffer, int(i)));

            float3 LightColorDiffuseResult;
            float3 LightColorSpecularResult;

#if ( SHADOWS_ENABLED == 1 )
            DoSpotLighting(true, g_SpotLightBufferCenterAndRadius, g_SpotLightBufferColor, g_SpotLightBufferSpotParams, nLightIndex, vPositionWS, vNorm, vViewDir, LightColorDiffuseResult, LightColorSpecularResult);
#else
            DoSpotLighting(false, g_SpotLightBufferCenterAndRadius, g_SpotLightBufferColor, g_SpotLightBufferSpotParams, nLightIndex, vPositionWS, vNorm, vViewDir, LightColorDiffuseResult, LightColorSpecularResult);
#endif

            AccumDiffuse += LightColorDiffuseResult;
            AccumSpecular += LightColorSpecularResult;
        }
    }

#if ( VPLS_ENABLED == 1 )
    // loop over the VPLs
    {
        uint nStartIndex, nLightCount;
        GetLightListInfo(g_PerTileVPLIndexBuffer, g_uMaxNumVPLsPerTile, g_uMaxNumVPLElementsPerTile, gl_FragCoord, nStartIndex, nLightCount);

        for ( uint i = nStartIndex; i < nStartIndex+nLightCount; i++ )
        {
//            uint nLightIndex = g_PerTileVPLIndexBuffer[i];
            int nLightIndex = int(texelFetch(g_PerTileVPLIndexBuffer, int(i)));

            float3 LightColorDiffuseResult;

            DoVPLLighting(g_VPLBufferCenterAndRadius, /*g_VPLBufferData,*/ nLightIndex, vPositionWS, vNorm, LightColorDiffuseResult);

            AccumDiffuse += LightColorDiffuseResult;
        }
    }
#endif

    // pump up the lights
    AccumDiffuse *= 2;
    AccumSpecular *= 8;

    // This is a poor man's ambient cubemap (blend between an up color and a down color)
    float fAmbientBlend = 0.5f * vNorm.y + 0.5;
    float3 Ambient = g_AmbientColorUp.rgb * fAmbientBlend + g_AmbientColorDown.rgb * (1-fAmbientBlend);

    // modulate mesh texture with lighting
    float3 DiffuseAndAmbient = AccumDiffuse + Ambient;
    Out_Color = float4(DiffuseTex.xyz*(DiffuseAndAmbient + AccumSpecular*fSpecMask),1);
}