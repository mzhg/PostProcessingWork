
#include "vaShared.glsl"

#include "vaSimpleShadowMap.glsl"

layout(location = 0) in vec4 In_Position;

out gl_PerVertex
{
    vec4 gl_Position;
};

void main()
{
    gl_Position = mul( g_RenderMeshGlobal.ShadowWorldViewProj, In_Position );
}