
#include "vaShared.glsl"
#include "vaSimpleShadowMap.glsl"

layout(location = 0) vec4 Position             ;//: SV_Position;

void main()
{
    gl_Position =  mul( g_PerInstanceConstants.ShadowWorldViewProj, float4(Position.xyz, 1) );
}