#include "OceanHelpers.hlsl"

in Varyings
{
//    float4 positionCS : SV_POSITION;
    float4 flow_shadow /*: TEXCOORD1*/;
    float4 foam_screenPosXYW /*: TEXCOORD4*/;
    float4 lodAlpha_worldXZUndisplaced_oceanDepth /*: TEXCOORD5*/;
    float3 worldPos /*: TEXCOORD7*/;
    #if _DEBUGVISUALISESHAPESAMPLE_ON
    half3 debugtint /*: TEXCOORD8*/;
    #endif
    float4 grabPos /*: TEXCOORD9*/;

//    UNITY_FOG_COORDS(3)
}_input;

out vec4 OutColor;

#include "OceanFoam.hlsl"
#include "OceanEmission.hlsl"
#include "OceanReflection.hlsl"
uniform sampler2D _Normals;
#include "OceanNormalMapping.hlsl"

uniform sampler2D _CameraDepthTexture;

// Hack - due to SV_IsFrontFace occasionally coming through as true for backfaces,
// add a param here that forces ocean to be in undrwater state. I think the root
// cause here might be imprecision or numerical issues at ocean tile boundaries, although
// i'm not sure why cracks are not visible in this case.
uniform float _ForceUnderwater;

float3 WorldSpaceLightDir(float3 worldPos)
{
    float3 lightDir = _WorldSpaceLightPos0.xyz;
    if (_WorldSpaceLightPos0.w > 0.)
    {
        // non-directional light - this is a position, not a direction
        lightDir = normalize(lightDir - worldPos.xyz);
    }
    return lightDir;
}

#ifndef _UNDERWATER_ON
#define _UNDERWATER_ON 0
#endif

bool IsUnderwater(const float facing)
{
    #if !_UNDERWATER_ON
    return false;
    #endif
    const bool backface = facing < 0.0;
    return backface || _ForceUnderwater > 0.0;
}

void main()
{
    const bool underwater = IsUnderwater(facing);
    const float lodAlpha = _input.lodAlpha_worldXZUndisplaced_oceanDepth.x;

    half3 view = normalize(_WorldSpaceCameraPos - _input.worldPos);

    // water surface depth, and underlying scene opaque surface depth
    float pixelZ = LinearEyeDepth(gl_FragCoord.z);
    half3 screenPos = _input.foam_screenPosXYW.yzw;
    half2 uvDepth = screenPos.xy / screenPos.z;
    float sceneZ01 = tex2D(_CameraDepthTexture, uvDepth).x;
    float sceneZ = LinearEyeDepth(sceneZ01);

    float3 lightDir = WorldSpaceLightDir(_input.worldPos);
    // Soft shadow, hard shadow
    float2 shadow = 1.0
    #if _SHADOWS_ON
    - _input.flow_shadow.zw
    #endif
    ;

    // Normal - geom + normal mapping. Subsurface scattering.
    const float3 uv_slice_smallerLod = WorldToUV(_input.lodAlpha_worldXZUndisplaced_oceanDepth.yz);
    const float3 uv_slice_biggerLod = WorldToUV_BiggerLod(_input.lodAlpha_worldXZUndisplaced_oceanDepth.yz);
    const float wt_smallerLod = (1. - lodAlpha) * _LD_Params[_LD_SliceIndex].z;
    const float wt_biggerLod = (1. - wt_smallerLod) * _LD_Params[_LD_SliceIndex + 1].z;
    float3 dummy = float3(0.);
    float3 n_geom = float3(0.0, 1.0, 0.0);
    float sss = 0.;
    if (wt_smallerLod > 0.001) SampleDisplacementsNormals(_LD_TexArray_AnimatedWaves, uv_slice_smallerLod, wt_smallerLod, _LD_Params[_LD_SliceIndex].w, _LD_Params[_LD_SliceIndex].x, dummy, n_geom.xz, sss);
    if (wt_biggerLod > 0.001) SampleDisplacementsNormals(_LD_TexArray_AnimatedWaves, uv_slice_biggerLod, wt_biggerLod, _LD_Params[_LD_SliceIndex + 1].w, _LD_Params[_LD_SliceIndex + 1].x, dummy, n_geom.xz, sss);
    n_geom = normalize(n_geom);

    if (underwater) n_geom = -n_geom;
    half3 n_pixel = n_geom;
#if _APPLYNORMALMAPPING_ON
    #if _FLOW_ON
    ApplyNormalMapsWithFlow(_input.lodAlpha_worldXZUndisplaced_oceanDepth.yz, _input.flow_shadow.xy, lodAlpha, n_pixel);
    #else
    n_pixel.xz += (underwater ? -1. : 1.) * SampleNormalMaps(_input.lodAlpha_worldXZUndisplaced_oceanDepth.yz, lodAlpha);
    n_pixel = normalize(n_pixel);
    #endif
#endif

    // Foam - underwater bubbles and whitefoam
    float3 bubbleCol = float3(0);
#if _FOAM_ON
    half4 whiteFoamCol;
    #if !_FLOW_ON
    ComputeFoam(_input.foam_screenPosXYW.x, _input.lodAlpha_worldXZUndisplaced_oceanDepth.yz, _input.worldPos.xz, n_pixel, pixelZ, sceneZ, view, lightDir, shadow.y, lodAlpha, bubbleCol, whiteFoamCol);
    #else
    ComputeFoamWithFlow(_input.flow_shadow.xy, _input.foam_screenPosXYW.x, _input.lodAlpha_worldXZUndisplaced_oceanDepth.yz, _input.worldPos.xz, n_pixel, pixelZ, sceneZ, view, lightDir, shadow.y, lodAlpha, bubbleCol, whiteFoamCol);
    #endif // _FLOW_ON
#endif // _FOAM_ON

    // Compute color of ocean - in-scattered light + refracted scene
    half3 scatterCol = ScatterColour(_input.worldPos, _input.lodAlpha_worldXZUndisplaced_oceanDepth.w, _WorldSpaceCameraPos, lightDir, view, shadow.x, underwater, true, sss);
    half3 col = OceanEmission(view, n_pixel, lightDir, _input.grabPos, pixelZ, uvDepth, sceneZ, sceneZ01, bubbleCol, _Normals, _CameraDepthTexture, underwater, scatterCol);

    // Light that reflects off water surface
    float reflAlpha = saturate((sceneZ - pixelZ) / 0.2);
    #if _UNDERWATER_ON
    if (underwater)
    {
        ApplyReflectionUnderwater(view, n_pixel, lightDir, shadow.y, _input.foam_screenPosXYW.yzzw, scatterCol, reflAlpha, col);
    }
    else
    #endif
    {
        ApplyReflectionSky(view, n_pixel, lightDir, shadow.y, _input.foam_screenPosXYW.yzzw, reflAlpha, col);
    }

    // Override final result with white foam - bubbles on surface
    #if _FOAM_ON
    col = lerp(col, whiteFoamCol.rgb, whiteFoamCol.a);
    #endif

    // Fog
    if (!underwater)
    {
        // Above water - do atmospheric fog. If you are using a third party sky package such as Azure, replace this with their stuff!
//        UNITY_APPLY_FOG(_input.fogCoord, col);
    }
    else
    {
        // underwater - do depth fog
        col = lerp(col, scatterCol, 1. - exp(-_DepthFogDensity.xyz * pixelZ));
    }

    #if _DEBUGVISUALISESHAPESAMPLE_ON
    col = lerp(col.rgb, _input.debugtint, 0.5);
    #endif
    #if _DEBUGVISUALISEFLOW_ON
    #if _FLOW_ON
    col.rg = lerp(col.rg, _input.flow_shadow.xy, 0.5);
    #endif
    #endif

    OutColor = float4(col, 1.);
}

