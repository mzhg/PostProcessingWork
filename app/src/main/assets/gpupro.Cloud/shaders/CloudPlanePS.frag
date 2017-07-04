#include "Cloud_Common.glsl"

in SVSOutput
{
    float2 vTex;
    float4 vWorldPos;
}_Input;

layout(location = 0) out float4 Out_f4Color;

//------------------------------------------------
// pixel shader
//------------------------------------------------

float3 ApplyScattering( float3 _clInput, float3 _vRay )
{
	// calcurating in-scattering
	float _fVL = dot( litDir, -_vRay );
	float fG = scat[1].w + scat[0].w * _fVL;
	fG = rsqrt( fG );
	fG = fG*fG*fG;
	float3 _vMie = scat[1].rgb * fG;
	float3 _vRayleigh = scat[0].rgb*(1.0f + _fVL*_fVL);
	float3 _vInscattering = scat[2] * (_vMie + _vRayleigh) + scat[4].rgb;

	// compute distance to the cloud
	float _fSin = _vRay.y;
	float _fRSin = vDistance.x * _fSin;
	float _fDistance = sqrt( _fRSin * _fRSin + vDistance.y ) - _fRSin;

	float3 fRatio = exp( -scat[3].rgb * _fDistance );
	return lerp( _vInscattering, _clInput, fRatio );
}

void main()
{
    float4 _clDensity = texture( sDensity, _Input.vTex.xy );
    float4 _clLit     = 1.0f - texture( sLit, _Input.vTex.xy );

    // light cloud
    float3 _clCloud = cAmb + cLit * _clLit.r;

    // compute ray direction
    float3 _vWorldPos = _Input.vWorldPos.xyz/_Input.vWorldPos.w;
    float3 _vRay = _vWorldPos - vEye;
    float _fSqDistance = dot( _vRay, _vRay );
    _vRay = normalize( _vRay );

    // apply scattering
    Out_f4Color.rgb = ApplyScattering( _clCloud, _vRay );
    Out_f4Color.rgb = saturate(Out_f4Color.rgb);
    Out_f4Color.a = _clDensity.a;
}