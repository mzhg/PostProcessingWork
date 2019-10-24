#include "skybox.glsl"

layout(location = 0) in float4 vPos;

out VS_OUTPUT
{
//    float4 Position	 : SV_Position;
    float3 EyeVec	 /*: TEXCOORD0*/;
    float3 PosWorld  /*: TEXCOORD1*/;
}Output;

//-----------------------------------------------------------------------------
// Name: SkyboxVS
// Type: Vertex shader
// Desc:
//-----------------------------------------------------------------------------
void main()
{
    gl_Position = mul(vPos, g_matViewProj);
    gl_Position.z = gl_Position.w;
    Output.EyeVec = normalize(vPos.xyz);
    Output.PosWorld = vPos.xyz;
}