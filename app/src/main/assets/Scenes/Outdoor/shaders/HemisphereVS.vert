
#include "Terrain.frag"

layout(location = 0) in float3 f3PosWS;
layout(location = 1) in float2 f2MaskUV0;

out SHemisphereVSOutput
{
//    float4 f4PosPS ;// SV_Position;
    float2 TileTexUV  ;// TileTextureUV;
    float3 f3Normal ;// Normal;
    float3 f3PosInLightViewSpace ;// POS_IN_LIGHT_VIEW_SPACE;
    float3 f3PosInWorldSapce;
    float fCameraSpaceZ ;// CAMERA_SPACE_Z;
    float2 f2MaskUV0 ;// MASK_UV0;
    float3 f3Tangent ;// TANGENT;
    float3 f3Bitangent ;// BITANGENT;
    float3 f3SunLightExtinction ;// EXTINCTION;
    float3 f3AmbientSkyLight ;// AMBIENT_SKY_LIGHT;
}Out;

out gl_PerVertex
{
	vec4 gl_Position;
};

void GetSunLightExtinctionAndSkyLight(in float3 f3PosWS,
                                      out float3 f3Extinction,
                                      out float3 f3AmbientSkyLight)
{
    float3 f3EarthCentre = float3(0, -g_fEarthRadius, 0);
    float3 f3DirFromEarthCentre = f3PosWS - f3EarthCentre;
    float fDistToCentre = length(f3DirFromEarthCentre);
    f3DirFromEarthCentre /= fDistToCentre;
    float fHeightAboveSurface = fDistToCentre - g_fEarthRadius;
    float fCosZenithAngle = dot(f3DirFromEarthCentre, g_f4DirOnLight.xyz);

    float fRelativeHeightAboveSurface = fHeightAboveSurface / g_fAtmTopHeight;
    float2 f2ParticleDensityToAtmTop = 
    				//g_tex2DOccludedNetDensityToAtmTop.SampleLevel(samLinearClamp, float2(fRelativeHeightAboveSurface, fCosZenithAngle*0.5+0.5), 0).xy;
    				  textureLod(g_tex2DOccludedNetDensityToAtmTop, float2(fRelativeHeightAboveSurface, fCosZenithAngle*0.5+0.5), 0.0).xy;
    
    float3 f3RlghOpticalDepth = g_f4RayleighExtinctionCoeff.rgb * f2ParticleDensityToAtmTop.x;
    float3 f3MieOpticalDepth  = g_f4MieExtinctionCoeff.rgb      * f2ParticleDensityToAtmTop.y;
        
    // And total extinction for the current integration point:
    f3Extinction = exp( -(f3RlghOpticalDepth + f3MieOpticalDepth) );
    
    f3AmbientSkyLight = 
    				//g_tex2DAmbientSkylight.SampleLevel(samLinearClamp, float2(fCosZenithAngle*0.5+0.5, 0.5), 0);
    				  textureLod(g_tex2DAmbientSkylight, float2(fCosZenithAngle*0.5+0.5, 0.5), 0.0).rgb;
}

void main()
{
	Out.TileTexUV = f3PosWS.xz;
	Out.f3PosInWorldSapce = f3PosWS;

    gl_Position = mul( float4(f3PosWS,1), g_WorldViewProj);
    
    float4 ShadowMapSpacePos = mul( float4(f3PosWS,1), g_WorldToLightView);
    Out.f3PosInLightViewSpace = ShadowMapSpacePos.xyz / ShadowMapSpacePos.w;
    Out.fCameraSpaceZ = abs(gl_Position.w);
    Out.f2MaskUV0 = f2MaskUV0;
    float3 f3Normal = normalize(f3PosWS - float3(0, -g_fEarthRadius, 0));
    Out.f3Normal = f3Normal;
    Out.f3Tangent = normalize( cross(f3Normal, float3(0,0,1)) );
    Out.f3Bitangent = normalize( cross(Out.f3Tangent, f3Normal) );

    GetSunLightExtinctionAndSkyLight(f3PosWS, Out.f3SunLightExtinction, Out.f3AmbientSkyLight);
}