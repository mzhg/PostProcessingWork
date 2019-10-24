#include "skybox.glsl"

layout(location = 0) in float4 vPos;


//-----------------------------------------------------------------------------
// Name: SkyboxVS
// Type: Vertex shader
// Desc:
//-----------------------------------------------------------------------------
void main()
{
    gl_Position = mul(vPos, g_matViewProj);
}