#include "../../../shader_libs/OutdoorSctr/Base.frag"

in float2 m_f2PosPS;

const float fSunAngularRadius =  32./2. / 60. * ((2. * 3.1415926)/180.0); // Sun angular DIAMETER is 32 arc minutes
const float fTanSunAngularRadius = tan(fSunAngularRadius); 

// xy: g_CameraAttribs.mProj[0][0], g_CameraAttribs.mProj[1][1], zw: g_LightAttribs.f4LightScreenPos
uniform float4 uniformData;

layout(location = 0) out float4 OutColor;

void main()
{
	float2 fCotanHalfFOV = uniformData.xy;  //float2( g_CameraAttribs.mProj[0][0], g_CameraAttribs.mProj[1][1] );
    float2 f2SunScreenSize = fTanSunAngularRadius * fCotanHalfFOV;
    float2 f2dXY = (m_f2PosPS - uniformData.zw/*g_LightAttribs.f4LightScreenPos.xy*/) / f2SunScreenSize;
    float cmp = sqrt(saturate(1.0 - dot(f2dXY, f2dXY)));
    OutColor.rgb = float3(cmp);
    OutColor.a = 1.0;
}