// Crest Ocean System

// This file is subject to the MIT License as seen in the root of this folder structure (LICENSE)

#if _FOAM_ON

uniform sampler2D _FoamTexture;
uniform float _FoamScale;
uniform float4 _FoamTexture_TexelSize;
uniform float4 _FoamWhiteColor;
uniform float4 _FoamBubbleColor;
uniform float _FoamBubbleParallax;
uniform float _ShorelineFoamMinDepth;
uniform float _WaveFoamFeather;
uniform float _WaveFoamBubblesCoverage;
uniform float _WaveFoamNormalStrength;
uniform float _WaveFoamSpecularFallOff;
uniform float _WaveFoamSpecularBoost;
uniform float _WaveFoamLightScale;

float3 AmbientLight()
{
    return float3(unity_SHAr.w, unity_SHAg.w, unity_SHAb.w);
}

float WhiteFoamTexture(float i_foam, float2 i_worldXZUndisplaced, float lodVal)
{
    float ft = lerp(
    tex2D(_FoamTexture, (1.25*i_worldXZUndisplaced + _CrestTime / 10.) / (4.*_LD_Params[_LD_SliceIndex].x*_FoamScale)).r,
    tex2D(_FoamTexture, (1.25*i_worldXZUndisplaced + _CrestTime / 10.) / (4.*_LD_Params[_LD_SliceIndex + 1].x*_FoamScale)).r,
    lodVal);

    // black point fade
    i_foam = saturate(1. - i_foam);
    return smoothstep(i_foam, i_foam + _WaveFoamFeather, ft);
}

float BubbleFoamTexture(float2 i_worldXZ, float2 i_worldXZUndisplaced, float3 i_n, float3 i_view, float lodVal)
{
    float2 windDir = float2(0.866, 0.5);
    float2 foamUVBubbles = (lerp(i_worldXZUndisplaced, i_worldXZ, 0.7) + 0.5 * _CrestTime * windDir) / _FoamScale + 0.125 * i_n.xz;
    float2 parallaxOffset = -_FoamBubbleParallax * i_view.xz / dot(i_n, i_view);
    float ft = lerp(
    textureLod(_FoamTexture, float4((0.74 * foamUVBubbles + parallaxOffset) / (4.0*_LD_Params[_LD_SliceIndex].x), 0., 3.)).r,
    textureLod(_FoamTexture, float4((0.74 * foamUVBubbles + parallaxOffset) / (4.0*_LD_Params[_LD_SliceIndex + 1].x), 0., 3.)).r,
    lodVal);

    return ft;
}

void ComputeFoam(float i_foam, float2 i_worldXZUndisplaced, float2 i_worldXZ, float3 i_n, float i_pixelZ, float i_sceneZ, float3 i_view, float3 i_lightDir, float i_shadow, float lodVal, out float3 o_bubbleCol, out float4 o_whiteFoamCol)
{
    float foamAmount = i_foam;

    // feather foam very close to shore
    foamAmount *= saturate((i_sceneZ - i_pixelZ) / _ShorelineFoamMinDepth);

    // Additive underwater foam - use same foam texture but add mip bias to blur for free
    float bubbleFoamTexValue = BubbleFoamTexture(i_worldXZ, i_worldXZUndisplaced, i_n, i_view, lodVal);
    o_bubbleCol = float3(bubbleFoamTexValue) * _FoamBubbleColor.rgb * saturate(i_foam * _WaveFoamBubblesCoverage) * AmbientLight();

    // White foam on top, with black-point fading
    float whiteFoam = WhiteFoamTexture(foamAmount, i_worldXZUndisplaced, lodVal);

#if _FOAM3DLIGHTING_ON
    // Scale up delta by Z - keeps 3d look better at distance. better way to do this?
    float2 dd = float2(0.25 * i_pixelZ * _FoamTexture_TexelSize.x, 0.);
    float whiteFoam_x = WhiteFoamTexture(foamAmount, i_worldXZUndisplaced + dd.xy, lodVal);
    float whiteFoam_z = WhiteFoamTexture(foamAmount, i_worldXZUndisplaced + dd.yx, lodVal);

    // compute a foam normal
    float dfdx = whiteFoam_x - whiteFoam, dfdz = whiteFoam_z - whiteFoam;
    float3 fN = normalize(i_n + _WaveFoamNormalStrength * float3(-dfdx, 0., -dfdz));
    // do simple NdL and phong lighting
    float foamNdL = max(0., dot(fN, i_lightDir));
    o_whiteFoamCol.rgb = _FoamWhiteColor.rgb * (AmbientLight() + _WaveFoamLightScale * _LightColor0 * foamNdL * i_shadow);
    float3 refl = reflect(-i_view, fN);
    o_whiteFoamCol.rgb += pow(max(0., dot(refl, i_lightDir)), _WaveFoamSpecularFallOff) * _WaveFoamSpecularBoost * _LightColor0 * i_shadow;
#else // _FOAM3DLIGHTING_ON
    o_whiteFoamCol.rgb = _FoamWhiteColor.rgb * (AmbientLight() + _WaveFoamLightScale * _LightColor0 * i_shadow);
#endif // _FOAM3DLIGHTING_ON

    o_whiteFoamCol.a = _FoamWhiteColor.a * whiteFoam;
}

void ComputeFoamWithFlow(float2 flow, float i_foam, float2 i_worldXZUndisplaced, float2 i_worldXZ, float3 i_n, float i_pixelZ, float i_sceneZ, float3 i_view, float3 i_lightDir, float i_shadow, float lodVal, out float3 o_bubbleCol, out float4 o_whiteFoamCol)
{
    const float float_period = 1;
    const float period = float_period * 2;
    float sample1_offset = fmod(_CrestTime, period);
    float sample1_weight = sample1_offset / float_period;
    if (sample1_weight > 1.0) sample1_weight = 2.0 - sample1_weight;
    float sample2_offset = fmod(_CrestTime + float_period, period);
    float sample2_weight = 1.0 - sample1_weight;

    // In order to prevent flow from distorting the UVs too much,
    // we fade between two samples of normal maps so that for each
    // sample the UVs can be reset
    float3 o_bubbleCol1 = float3(0, 0, 0);
    float4 o_whiteFoamCol1 = float4(0, 0, 0, 0);
    float3 o_bubbleCol2 = float3(0, 0, 0);
    float4 o_whiteFoamCol2 = float4(0, 0, 0, 0);

    ComputeFoam(i_foam, i_worldXZUndisplaced - (flow * sample1_offset), i_worldXZ, i_n, i_pixelZ, i_sceneZ, i_view, i_lightDir, i_shadow, lodVal, o_bubbleCol1, o_whiteFoamCol1);
    ComputeFoam(i_foam, i_worldXZUndisplaced - (flow * sample2_offset), i_worldXZ, i_n, i_pixelZ, i_sceneZ, i_view, i_lightDir, i_shadow, lodVal, o_bubbleCol2, o_whiteFoamCol2);
    o_bubbleCol = (sample1_weight * o_bubbleCol1) + (sample2_weight * o_bubbleCol2);
    o_whiteFoamCol = (sample1_weight * o_whiteFoamCol1) + (sample2_weight * o_whiteFoamCol2);
}

#endif // _FOAM_ON
