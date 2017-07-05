#include "Cloud_Common.glsl"

in float4 vWorldPos;
layout(location = 0) out float4 Out_f4Color;

void main()
{
    // ray direction
    float3 _vWorldPos = vWorldPos.xyz/vWorldPos.w;
    float3 _vRay = _vWorldPos - vEye;
    float _fSqDistance = dot( _vRay, _vRay );
    _vRay = normalize( _vRay );

    // calcurating in-scattering
    float _fVL = dot( litDir, -_vRay );
    float fG = scat[1].w + scat[0].w * _fVL;
    fG = rsqrt( fG );
    fG = fG*fG*fG;
    float3 _vMie = scat[1].rgb * fG;
    float3 _vRayleigh = scat[0].rgb*(1.0f + _fVL*_fVL);
    float3 _vInscattering = scat[2] * (_vMie + _vRayleigh) + scat[4].rgb;

    // compute distance the light passes through the atmosphere
    float _fSin = _vRay.y;
    float _fRSin = scat[2].w * _fSin;
    float _fDistance = sqrt( _fRSin * _fRSin + scat[3].w ) - _fRSin;

    float3 fRatio = exp( -scat[3].rgb * _fDistance );
    float4 _color;
    _color.rgb = (1.0f-fRatio) *_vInscattering;
    _color.a = 1.0f;
    Out_f4Color = _color;
}