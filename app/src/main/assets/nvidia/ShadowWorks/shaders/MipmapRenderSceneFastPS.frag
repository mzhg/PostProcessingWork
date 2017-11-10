#include "MipmapSoftShadowCommon.glsl"

float4 FastShadow(float3 vLightPos, float4 vDiffColor)
{
    // this is our white rectangle
    float2 vLightMax = vLightPos + g_fFilterSize;
    vLightPos.xy -= g_fFilterSize;
    uint pStackLow = 0xffffffffu, pStackHigh;
    uint iPix = 0;
    uint3 iLevel = uint3(0, 0, N_LEVELS - 1);
    float fTotShadow = 1; ///< completely unshadowed in the beginning

    /*[loop]*/ for ( ; ; )
    {
        uint2 iPixel = uint2(iLevel.x + (iPix & 1u), iLevel.y + (iPix >> 1));
        float fDiag = float(1 << iLevel.z);
        float2 vTexMin = float2(iPixel) * fDiag;

        // shrink texel to the white rectangle size
        float2 vTexMax = min(vTexMin + fDiag, vLightMax);
        vTexMax -= max(vTexMin, vLightPos.xy);

        // fetch the depth map
        float2 vPixel = float2(iPixel) + 0.5;
        /*[flatten]*/ if (iLevel.z != 0)


        { vPixel += float2(DEPTH_RES, 1 << (N_LEVELS - iLevel.z)); }
        vPixel /= float2(DEPTH_RES * 3 / 2, DEPTH_RES);
        float2 fMapMinMax = textureLod(DepthMip2, vPixel, 0.0);   // DepthSampler

        // compute shadowing
        float fPotentialShadow = saturate(vTexMax.x * g_fDoubleFilterSizeRev) * saturate(vTexMax.y * g_fDoubleFilterSizeRev);
        bool bShadowed = (vLightPos.z >= fMapMinMax.y); // our z larger or equal than shadow map max
        /*[flatten]*/ if (bShadowed) fTotShadow -= fPotentialShadow;
        if (fTotShadow <= 0)return float4(0, 0, 0, 1);

        // decide if we must go lower
        bool bNextLevel = (fPotentialShadow > 0) && (!bShadowed) && (vLightPos.z > fMapMinMax.x);
        /*[flatten]*/ if (bNextLevel) iLevel.xy = iPixel + iPixel;
        // new values only rewrite old ones if we actually go to the next level
        bool bPushToStack = bNextLevel && (iPix < 3);
        /*[flatten]*/ if (bPushToStack)
        {
            pStackHigh = (pStackLow & 0xfc000000) | (pStackHigh >> 6);
            pStackLow = (pStackLow << 6) | (iLevel.z << 2) | (iPix + 1);
        }
        iLevel.z -= bNextLevel; // go to more detailed level
        /*[flatten]*/ if (bNextLevel) iPix = 0;
        else iPix += 1;

        // now get values from stack if necessary
        uint iPrevLevel = iLevel.z;
        /*[branch]*/ if (iPix >= 4)
        {
            iLevel.z = (pStackLow >> 2) & 0xfu;
            iPix = pStackLow & 3u;
            pStackLow = (pStackLow >> 6) | (pStackHigh & 0xfc000000u);
            pStackHigh <<= 6;
        }
        iLevel.xy = (iLevel.xy >> (iLevel.z - iPrevLevel)) & 0xfffffffe;
        if (iLevel.z == 0xf)
        {
            vDiffColor.xyz *= saturate(fTotShadow);
            return vDiffColor;
        }
    }
    return float4(1, 0, 0, 1);  // this is never reached, but compiler curses if the line is not here
}

in VS_OUT0
{
//    float4 vPos; : SV_Position; ///< vertex position
    float4 vDiffColor;// : COLOR0; ///< vertex diffuse color (note that COLOR0 is clamped from 0..1)
    float2 vTCoord;// : TEXCOORD0; ///< vertex texture coords
    float4 vLightPos;// : TEXCOORD2;
}In;

out float4 Out_Color;

//float4 RenderSceneFastPS(VS_OUT0 In) : SV_Target0
void main()
{
    if (dot(In.vDiffColor.xyz, In.vDiffColor.xyz) == 0)
        return float4(0, 0, 0, 1);
    if (bTextured) In.vDiffColor.xyz *= texture(DiffuseTex, In.vTCoord);   // DiffuseSampler
    Out_Color = FastShadow(In.vLightPos.xyz / In.vLightPos.w, In.vDiffColor);
}