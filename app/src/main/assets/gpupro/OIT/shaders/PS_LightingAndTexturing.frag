#include "PS_OIT_Include.glsl"

in float3 vNormal;
in float2 vTex;

out float4 OutColor;

void main()
{
    // Renormalize normal
    float3 Normal = normalize(vNormal);

    // Invert normal when dealing with back faces
    if (!gl_FrontFace) Normal = -Normal;

    // Lighting
    float fLightIntensity = saturate(saturate(dot(Normal.xyz, g_vLightVector.xyz)) + 0.2);
    float4 vColor = float4(g_vMeshColor.xyz * fLightIntensity, g_vMeshColor.w);

    // Texturing
    float4 vTextureColor = textureLod( g_txDiffuse, vTex );
    vColor.xyz *= vTextureColor.xyz;

    OutColor = vColor;
}