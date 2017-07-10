#include "CloudsCommon.glsl"

layout(location = 0) out float4 Out_f4Color;

// This shader renders cloud depth into the shadow map
void main()
{
    // TODO: transparency map / when shadow map dimension ration is <= 1:4, wider filtering is required
    float fLiSpCloudTransparency = g_tex2DLiSpCloudTransparency.Sample(samLinearClamp, float3(ProjToUV(In.m_f2PosPS.xy), g_GlobalCloudAttribs.f4Parameter.x) );
    float fMaxDepth = g_tex2DLiSpCloudMinMaxDepth.Sample(samLinearClamp, float3(ProjToUV(In.m_f2PosPS.xy), g_GlobalCloudAttribs.f4Parameter.x) ).y;
    gl_FragDepth = ( fLiSpCloudTransparency < 0.1 ) ? fMaxDepth : 0.f;
    Out_f4Color = float4(0);
}