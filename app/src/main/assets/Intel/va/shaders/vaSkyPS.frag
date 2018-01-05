#include "vaShared.glsl"
layout(location = 0) out float4 Out_Color;

in vec4 m_Position;

void main()
{
   float4 skyDirection = mul( g_SimpleSkyGlobal.ProjToWorld, m_Position ).xyzw;
   skyDirection *= 1 / skyDirection.wwww;
   skyDirection.xyz = normalize( skyDirection.xyz );

   ////////////////////////////////////////////////////////////////////////////////////////////////
   // this is a quick ad-hoc noise algorithm used to dither the gradient, to mask the
   // banding on these awful monitors
   // FS_2015: lol - this comment is from around 2005 - LCD dispays those days were apparently really bad :)
   float noise = frac( dot( sin( skyDirection.xyz * float3( 533, 599, 411 ) ) * 10, float3( 1.0, 1.0, 1.0 ) ) );
   float noiseAdd = (noise - 0.5) * 0.1;
   // noiseAdd = 0.0; // to disable noise, just uncomment this
   //
   float noiseMul = 1 - noiseAdd;
   ////////////////////////////////////////////////////////////////////////////////////////////////

   // Horizon
   float horizonK = 1 - dot( skyDirection.xyz, float3( 0, 0, 1 ) );
   horizonK = saturate( pow( abs( horizonK ), g_SimpleSkyGlobal.SkyColorLowPow ) * g_SimpleSkyGlobal.SkyColorLowMul );
   horizonK *= noiseMul;

   float4 finalColor = lerp( g_SimpleSkyGlobal.SkyColorHigh, g_SimpleSkyGlobal.SkyColorLow, horizonK );

   // Sun
   float dirToSun = saturate( dot( skyDirection.xyz, g_SimpleSkyGlobal.SunDir.xyz ) / 2.0 + 0.5 );

   float sunPrimary = clamp( pow( dirToSun, g_SimpleSkyGlobal.SunColorPrimaryPow ) * g_SimpleSkyGlobal.SunColorPrimaryMul, 0.0f, 1.0f );
   sunPrimary *= noiseMul;

   finalColor += g_SimpleSkyGlobal.SunColorPrimary * sunPrimary;

   float sunSecondary = clamp( pow( dirToSun, g_SimpleSkyGlobal.SunColorSecondaryPow ) * g_SimpleSkyGlobal.SunColorSecondaryMul, 0.0f, 1.0f );
   sunSecondary *= noiseMul;

   finalColor += g_SimpleSkyGlobal.SunColorSecondary * sunSecondary;

   Out_Color = float4( finalColor.xyz, 1 );
}