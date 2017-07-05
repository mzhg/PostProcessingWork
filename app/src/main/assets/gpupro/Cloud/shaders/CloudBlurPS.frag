#include "Cloud_Common.glsl"

in SVSOutput
{
    float4 vTex;
    float4 vWorldPos;
}_Input;

layout(location = 0) out float4 Out_f4Color;

void main()
{
    // compute ray direction
    float3 _vWorldPos = _Input.vWorldPos.xyz/_Input.vWorldPos.w;
    float3 _vRay = _vWorldPos - vEye;
    float _fSqDistance = dot( _vRay, _vRay );
    _vRay = normalize( _vRay );

    // compute distance the light passes through the atmosphere.
    float _fSin = _vRay.y;
    float _fRSin = vParam.x * _fSin;
    float _fDistance = sqrt( _fRSin * _fRSin + vParam.y ) - _fRSin;

    // Compute UV offset.
    float2 _vUVOffset = _Input.vTex.zw / FILTER_WIDTH * (vParam.z / _fDistance);

    // limit blur vector
    float2 _len = abs( _vUVOffset * invMax );
    float _over = max( _len.x, _len.y );
    float _scale = _over > 1.0f ? 1.0f/_over : 1.0f;
    _vUVOffset.xy *= _scale;

    // scale parameter of exponential weight
    float4 _distance;
    _distance = dot( _vUVOffset.xy, _vUVOffset.xy );
    _distance *= vFallOff;

    // blur
    float2 _uv = _Input.vTex.xy;
    float4 _clOut = texture( sDensity, _uv );
    float4 _fWeightSum = float4(1);
    for ( int i = 1; i < FILTER_WIDTH; ++i ) {
        float4 _weight = exp( _distance * float(i) );
        _fWeightSum += _weight;

        float2 _vMove = _vUVOffset * float(i);
        float4 _clDensity = texture( sDensity, _uv + _vMove );
        _clOut += _weight * _clDensity;
    }
    _clOut /= _fWeightSum;

    Out_f4Color = _clOut;
}