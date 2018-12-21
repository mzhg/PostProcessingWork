
#include "ShaderCommon.frag"

in float2 vTex;
in float4 vWorldPos;

layout ( binding = 0) uniform sampler2D tPhaseLUT;

layout(location = 0) out float4 OutColor;

void main()
{
    float fMaxTracingDistance = g_fMaxTracingDistance;
    //                       Light
    //                        *                   -
    //                     .' |\                  |
    //                   .'   | \                 | fClosestDistToLight
    //                 .'     |  \                |
    //               .'       |   \               |
    //          Cam *--------------*--------->    -
    //              |<--------|     \
    //                  \
    //                  fStartDistFromProjection
    //
    float2 f2UV = vTex.xy; //ProjToUV(In.m_f2PosPS.xy);
    float fStartDistFromProjection = /*In.m_f2PosPS.x*/ (2.0 * vTex.x - 1.0) * fMaxTracingDistance;
    float fClosestDistToLight = f2UV.y * fMaxTracingDistance;

    float3 f3InsctrRadinance = float3(0);

    // There is a very important property: pre-computed scattering must be monotonical with respect
    // to u coordinate. However, if we simply subdivide the tracing distance onto the equal number of steps
    // as in the following code, we cannot guarantee this
    //
    // float fStepWorldLen = length(f2StartPos-f2EndPos) / fNumSteps;
    // for( float fRelativePos=0; fRelativePos < 1; fRelativePos += 1.f/fNumSteps )
    // {
    //      float2 f2CurrPos = lerp(f2StartPos, f2EndPos, fRelativePos);
    //      ...
    //
    // To assure that the scattering is monotonically increasing, we must go through
    // exactly the same taps for all pre-computations. The simple method to achieve this
    // is to make the world step the same as the difference between two neighboring texels:
    // The step can also be integral part of it, but not greater! So /2 will work, but *2 won't!
    float fStepWorldLen = dFdx(fStartDistFromProjection);
    for(float fDistFromProj = fStartDistFromProjection; fDistFromProj < fMaxTracingDistance; fDistFromProj += fStepWorldLen)
    {
        float2 f2CurrPos = float2(fDistFromProj, -fClosestDistToLight);
        float fDistToLightSqr = dot(f2CurrPos, f2CurrPos);
        float fDistToLight = sqrt(fDistToLightSqr);
        float attenuation = AttenuationFunc(fDistToLight);
        float fDistToCam = f2CurrPos.x - fStartDistFromProjection;
        float3 f3Extinction = exp( -(fDistToCam + fDistToLight) * g_vSigmaExtinction.rgb );
        float2 f2LightDir = normalize(f2CurrPos);
        float fCosTheta = -f2LightDir.x;

        float3 f3dLInsctr = attenuation * f3Extinction * ComputePhaseFactor(fCosTheta) * fStepWorldLen / max(fDistToLightSqr,fMaxTracingDistance*fMaxTracingDistance*1e-8);
        f3InsctrRadinance += f3dLInsctr;
    }
    OutColor.rgb = f3InsctrRadinance;
    OutColor.a = fStepWorldLen;
}