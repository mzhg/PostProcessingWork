#include "CloudsCommon.glsl"

layout(location = 0) out float4 Out_f4Color;
in float4 m_f4UVAndScreenPos;
// This shader renders cloud depth into the shadow map
void main()
{
    // TODO: transparency map / when shadow map dimension ration is <= 1:4, wider filtering is required
    float fLiSpCloudTransparency = texture(g_tex2DLiSpCloudTransparency, float3(m_f4UVAndScreenPos.xy, g_GlobalCloudAttribs.f4Parameter.x) ).x;  // samLinearClamp
    float fMaxDepth = texture(g_tex2DLiSpCloudMinMaxDepth, float3(m_f4UVAndScreenPos.xy, g_GlobalCloudAttribs.f4Parameter.x) ).y;  // samLinearClamp
    gl_FragDepth = ( fLiSpCloudTransparency < 0.1 ) ? fMaxDepth : 0.f;
    Out_f4Color = float4(0);
}