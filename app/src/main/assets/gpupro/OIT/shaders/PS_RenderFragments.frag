#include "PS_OIT_Include.glsl"

in float3 vNormal;
in float2 vTex;

//--------------------------------------------------------------------------------------
// SRVs
//--------------------------------------------------------------------------------------
//Buffer<uint> StartOffsetBufferSRV : register(t0);

layout(binding = 0) readonly buffer _StartOffsetBufferSRV
{
    uint StartOffsetBufferSRV[];
};

//StructuredBuffer<Fragment_And_Link_Buffer_STRUCT> FragmentAndLinkBufferSRV : register(t1);
layout(binding = 1) readonly buffer _FragmentAndLinkBufferSRV
{
    Fragment_And_Link_Buffer_STRUCT FragmentAndLinkBufferSRV[];
};


#ifndef NUM_SAMPLES
#define NUM_SAMPLES 1
#endif

#if NUM_SAMPLES>1
//Texture2DMS<float4, NUM_SAMPLES> BackgroundTexture : register(t3);
#else
//Texture2D BackgroundTexture : register(t3);
layout(binding = 0) uniform sampler2D BackgroundTexture;
#endif

out float4 OutColor;
void main()
{
    // Calculate position in tile
    int2 uTilePosition = int2(floor(gl_FragCoord.xy - g_vRectangleCoordinates.xy));

    // Calculate start offset buffer address
    uint uStartOffsetLinearAddress = uint( uTilePosition.x + g_vTileSize.x * uTilePosition.y );

    // Fetch offset of first fragment for current pixel
    uint uOffset = StartOffsetBufferSRV.Load(uStartOffsetLinearAddress);

    // Fetch structure element at this offset
    int nNumFragments = 0;
    while (uOffset!=0xFFFFFFFF)
    {
        // Retrieve fragment at current offset
        Fragment_And_Link_Buffer_STRUCT Element = FragmentAndLinkBufferSRV[uOffset];

        #if NUM_SAMPLES>1
        // Only include fragment in sorted list if coverage mask includes the sample currently being rendered
        uint uCoverage = UnpackCoverageIntoUint(Element.uDepthAndCoverage);
        if ( uCoverage & (1<<input.uSample) )
        {
        #endif
            // Copy fragment color and depth into sorted list
            SortedFragments[nNumFragments] = uint2(Element.uPixelColor, Element.uDepthAndCoverage);

            // Sort fragments in front to back (increasing) order using insertion sorting
            // max(j-1,0) is used to cater for the case where nNumFragments=0 (cheaper than a branch)
            int j = nNumFragments;
            while ( (j>0) && (SortedFragments[max(j-1, 0)].y > SortedFragments[j].y) )
            {
                // Swap required
                int jminusone = max(j-1, 0);
                uint2 Tmp                  = SortedFragments[j];
                SortedFragments[j]         = SortedFragments[jminusone];
                SortedFragments[jminusone] = Tmp;
                j--;
            }

            // Increase number of fragment if under the limit
            nNumFragments = min(nNumFragments+1, MAX_SORTED_FRAGMENTS);

        #if NUM_SAMPLES>1
        }
        #endif

        // Retrieve next offset
        uOffset = Element.uNext;
    }

    // Retrieve current color from background color
    #if NUM_SAMPLES>1
    float4 vCurrentColor = BackgroundTexture.Load(int3(input.vPos.xy, 0), input.uSample);
    #else
    float4 vCurrentColor = texelFetch(BackgroundTexture, int2(gl_FragCoord.xy), 0);
    #endif

    // Render fragments using SRCALPHA-INVSRCALPHA blending
    for (int k=nNumFragments-1; k>=0; k--)
    {
        float4 vFragmentColor = UnpackUintIntoFloat4(SortedFragments[k].x);
        vCurrentColor.xyz     = lerp(vCurrentColor.xyz, vFragmentColor.xyz, vFragmentColor.w);
    }

        #if 0
    // Use under-blending: produces the same result as traditional back-to-front alpha blending
    float4 vCurrentColor = float4(0,0,0,1);
    for (int k=0; k<nNumFragments; k++)
    {
        float4 vFragmentColor = UnpackUintIntoFloat4(SortedFragments[k].uPixelColor);
        vCurrentColor.xyz = vCurrentColor.w*vFragmentColor.w*vFragmentColor.xyz + vCurrentColor.xyz;
        vCurrentColor.w   =  (1.0 - vFragmentColor.w)*vCurrentColor.w;
    }
        #endif

    // Return manually-blended color
    OutColor = vCurrentColor;
}