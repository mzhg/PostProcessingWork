
#include "ShaderCommon.frag"

in float2 vTex;
in float4 vWorldPos;

layout ( binding = 0) uniform sampler2D tPhaseLUT;

layout(location = 0) out float4 OutColor;

void main()
{
    float fPrecomputedFuncValue = 0;
    float2 f2UV = vTex.xy; //ProjToUV(In.m_f2PosPS.xy);
    f2UV *= GetSRNN05LUTParamLimits();
    float fKsiStep = dFdy(f2UV.y);
    for(float fKsi = 0; fKsi < f2UV.y; fKsi += fKsiStep)
    {
        fPrecomputedFuncValue += exp( -f2UV.x * tan(fKsi) );
    }

    fPrecomputedFuncValue *= fKsiStep;
    OutColor.r = fPrecomputedFuncValue;
    OutColor.gba = float3(0);
}