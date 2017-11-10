#include "MipmapSoftShadowCommon.glsl"

in VS_OUT0
{
//    float4 vPos; : SV_Position; ///< vertex position
    float4 vDiffColor;// : COLOR0; ///< vertex diffuse color (note that COLOR0 is clamped from 0..1)
    float2 vTCoord;// : TEXCOORD0; ///< vertex texture coords
    float4 vLightPos;// : TEXCOORD2;
}In;
out float4 Out_Color;

float Shadow2D(float2 vMin, float2 vMax)
{
    vMin = max(vMin, -g_fFilterSize);
    vMax = min(vMax,  g_fFilterSize);
    return saturate((vMax.x - vMin.x) * g_fDoubleFilterSizeRev) * saturate((vMax.y - vMin.y) * g_fDoubleFilterSizeRev);
}

float4 AccurateShadow(float3 vLightPos, float4 vDiffColor)
{
    uint pStackLow = 0xffffffffu, pStackHigh;
    uint iPix = 0;
    uint3 iLevel = uint3(0, 0, N_LEVELS - 1);

    float fTotShadow = 1.0;
    /*[loop]*/ for (int ireps = 0; ireps < 4096; ++ireps)
    {
        uint2 iPixel = uint2(iLevel.x + (iPix & 1u), iLevel.y + (iPix >> 1));
        float2 vPixel = float2(iPixel) + 0.5;
        /*[flatten]*/ if (iLevel.z != 0)
        { vPixel += float2(DEPTH_RES, 1 << (N_LEVELS - iLevel.z)); }
        vPixel /= float2(DEPTH_RES * 3 / 2, DEPTH_RES);
        float2 vMinMax = textureLod(DepthMip2, vPixel, 0);   // DepthSampler
        // we need to assure that there will be no gaps between this shadow texel and it's neighbours
        // to do that we will compute minimum depth on the edges as average between neighbours
        float4 vDepthMin = vMinMax.x;
        // convert depth to linear space
        vMinMax.y = 1. / (vMinMax.y * mLightProjClip2TexInv[2][3] + mLightProjClip2TexInv[3][3]);
        // here we remove light leaks by extending/shrinking shadow texel depending on the neighbours dept
        // this makes sense only at the finest mip level and for texels that are closer to light than the fragment is
        /*[branch]*/ if (iLevel.z == 0 && vMinMax.y < vLightPos.z)
        {
            // we use here DepthTex0 because it is one float instead of two (should be cheaper to fetch)
            vPixel.x = float(iPixel.x) / DEPTH_RES;
            vDepthMin.x = min(vMinMax.x, textureLod(DepthTex0, float2(vPixel.x - 1. / DEPTH_RES, vPixel.y), 0));   // DepthSampler
            vDepthMin.y = min(vMinMax.x, textureLod(DepthTex0, float2(vPixel.x, vPixel.y + 1. / DEPTH_RES), 0));
            vDepthMin.z = min(vMinMax.x, textureLod(DepthTex0, float2(vPixel.x + 1. / DEPTH_RES, vPixel.y), 0));
            vDepthMin.w = min(vMinMax.x, textureLod(DepthTex0, float2(vPixel.x, vPixel.y - 1. / DEPTH_RES), 0));
        }
        // convert depth to linear space
        vDepthMin = 1. / (vDepthMin * mLightProjClip2TexInv[2][3] + mLightProjClip2TexInv[3][3]);  // TODO

        // these are coordinates of shadow texel hanging in light space at their own depth
        float2 vMin = 0.5 - float2(iPixel.x, iPixel.y + 1) * g_fResRev[iLevel.z];  // TODO
        float2 vMax = float2(vMin.x - g_fResRev[iLevel.z], vMin.y + g_fResRev[iLevel.z]);
        vMin *= 2 * float2(mLightProjClip2TexInv[3][0], mLightProjClip2TexInv[3][1]);
        vMax *= 2 * float2(mLightProjClip2TexInv[3][0], mLightProjClip2TexInv[3][1]);
        // vMin, vMax is shadow texel located at unit depth. now we project shadow texel to plane of light source
        vMin = (vMin * vLightPos.zz - vLightPos.xy) / (vLightPos.zz / vDepthMin.xy - 1);
        vMax = (vMax * vLightPos.zz - vLightPos.xy) / (vLightPos.zz / vDepthMin.zw - 1);

        float fShadow = Shadow2D(vMin, vMax);
        bool bNextLevel = (iLevel.z > 0 && fShadow > 0);
        /*[flatten]*/ if (!bNextLevel) fTotShadow -= fShadow;
        if (fTotShadow <= 0 || (fShadow == 1 && vMinMax.y < vLightPos.z))
        { return float4(0, 0, 0, 1); } // the point is completely in shadow

        /*[flatten]*/ if (bNextLevel) iLevel.xy = iPixel + iPixel;
        // new values only rewrite old ones if we actually go to the next level
//        bool bPushToStack = bNextLevel & (iPix < 3);
        bool bPushToStack = bool(uint(bNextLevel) & uint(iPix < 3));  // TODO ???
        /*[flatten]*/ if (bPushToStack)
        {
            pStackHigh = (pStackLow & 0xfc000000u) | (pStackHigh >> 6);
            pStackLow = (pStackLow << 6) | iLevel.z * 4 | (iPix + 1);
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
            pStackHigh *= 64;
        }
        iLevel.xy = (iLevel.xy >> (iLevel.z - iPrevLevel)) & 0xfffffffe;
        if (iLevel.z == 0xf)
        {
            vDiffColor.xyz *= saturate(fTotShadow);
            return vDiffColor;
        }
    }
    return float4(1, 0, 0, 1);
}

void main()
{
    if (dot(In.vDiffColor.xyz, In.vDiffColor.xyz) == 0)
        return float4(0, 0, 0, 1);
    /*[flatten]*/ if (bTextured) In.vDiffColor *= texture(DiffuseTex, In.vTCoord);  //DiffuseSampler
    Out_Color = AccurateShadow(In.vLightPos.xyz, In.vDiffColor);
}
