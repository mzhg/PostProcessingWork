#include "PS_OIT_Include.glsl"

in float3 vNormal;
in float2 vTex;

struct Fragment_And_Link_Buffer_STRUCT
{
    uint    uPixelColor;
    uint    uDepthAndCoverage;       // Coverage is only used in the MSAA case
    uint    uNext;
};

//--------------------------------------------------------------------------------------
// UAVs
//--------------------------------------------------------------------------------------
//RWByteAddressBuffer StartOffsetBuffer                                     : register(u1);
//RWStructuredBuffer<Fragment_And_Link_Buffer_STRUCT> FragmentAndLinkBuffer : register(u2);
layout(binding = 1) buffer RWByteAddressBuffer
{
    uint StartOffsetBuffer[];
};

layout(binding = 2) buffer RWStructuredBuffer
{
    Fragment_And_Link_Buffer_STRUCT FragmentAndLinkBuffer[];
};

//--------------------------------------------------------------------------------------
// Globals
//--------------------------------------------------------------------------------------
uniform uint2 SortedFragments[MAX_SORTED_FRAGMENTS+1];

layout(early_fragment_tests) in;

out float4 OutColor;

#ifndef NUM_SAMPLES
#define NUM_SAMPLES 1
#endif

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
    float4 vTextureColor = texture(g_txDiffuse, vTex);
    vColor.xyz *= vTextureColor.xyz;

    // Retrieve current pixel count and increase counter
    uint uPixelCount = FragmentAndLinkBuffer.IncrementCounter();

    // Calculate position in tile
    int2 uTilePosition = floor(gl_FragCoord.xy - g_vRectangleCoordinates.xy);

    // Exchange indices in StartOffsetTexture corresponding to pixel location
    uint uStartOffsetLinearAddress = 4 *( uTilePosition.x + int(g_vTileSize.x) * uTilePosition.y );
    uint uOldStartOffset;
    StartOffsetBuffer.InterlockedExchange(uStartOffsetLinearAddress, uPixelCount, uOldStartOffset);

    // Append new element at the end of the Fragment and Link Buffer
    Fragment_And_Link_Buffer_STRUCT Element;
    Element.uPixelColor         = PackFloat4IntoUint(vColor);
    #if NUM_SAMPLES>1
    Element.uDepthAndCoverage   = PackDepthAndCoverageIntoUint(gl_FragCoord.z, input.uCoverage);
    #else
    Element.uDepthAndCoverage   = PackDepthIntoUint(gl_FragCoord.z);
    #endif
    Element.uNext               = uOldStartOffset;
    FragmentAndLinkBuffer[uPixelCount] = Element;

    // This won't write anything into the RT because color writes are off
    OutColor =  float4(0,0,0,0);
}