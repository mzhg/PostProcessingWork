#include "Cloud_Common.glsl"

in SVSOutput
{
    float4 vTex;//         : TEXCOORD0;
    float3 vWorldNormal;// : TEXCOORD1;
    float3 vWorldPos;//    : TEXCOORD2;
    float4 vShadowPos;//   : TEXCOORD3;
}_Input;

layout(location = 0) out float4 Out_f4Color;

//------------------------------------------------
// Sample and blend ground textures
//------------------------------------------------
float4 GetGroundTexture(float4 vTex)
{
	// Sample blend texture
	// Each channel of the blend texture means the density of each ground textures.
	float4 _clGroundBlend = texture( sGroundBlend, vTex.zw );
	// sample ground textures
	float4 _clGroundTex[4];
	_clGroundTex[0] = texture( sGround1, vTex.xy );
	_clGroundTex[1] = texture( sGround2, vTex.xy );
	_clGroundTex[2] = texture( sGround3, vTex.xy );
	_clGroundTex[3] = texture( sGround3, vTex.xy );
	// blend ground textures
	float4 _clGround = texture( sGround0, vTex.xy );
	_clGround = lerp( _clGround, _clGroundTex[0], _clGroundBlend.r );
	_clGround = lerp( _clGround, _clGroundTex[1], _clGroundBlend.g );
	_clGround = lerp( _clGround, _clGroundTex[2], _clGroundBlend.b );
	_clGround = lerp( _clGround, _clGroundTex[3], _clGroundBlend.a );

	return _clGround;
}

float4 lit(float n_dot_l, float n_dot_h, float m)
{
    return float4
    (
        1,  // ambient
        n_dot_l < 0.0 ? 0 : n_dot_l,
        (n_dot_l < 0.0 || n_dot_h < 0.0) ? 0.0 : (n_dot_h * m),
        1
    );
}

void main()
{
    // Sample shadow map
    // Cloud shadow is not depth but transparency of shadow.
    float4 _clShadow = texture( sShadow, _Input.vShadowPos.xy/_Input.vShadowPos.w );
    _clShadow = max(float4(1), _clShadow);

    float3 _vNormal = normalize( _Input.vWorldNormal );

    float3 _vRay = _Input.vWorldPos - vEye;
    float _fSqDistance = dot( _vRay, _vRay );
    _vRay *= rsqrt(_fSqDistance);

    // apply light
    float3 _clDiffuse = litAmb;
    float3 _clSpecular = float3(0);
    float  _fDot = dot( -litDir, _vNormal );
    float3 _vHalf = -normalize( litDir.xyz + _vRay );
    float4 _lit = lit( _fDot, dot(_vHalf, _vNormal ), mSpc.w );
    float3 _litCol = _clShadow.xxx * litCol.rgb;
    _clDiffuse += _litCol * _lit.y;
    _clSpecular += _litCol * _lit.z;

    // blend ground textures
    // alpha channel is used as a mask of specular
    float4 _clGround = GetGroundTexture( _Input.vTex );

    // apply material
    float4 _color;
    _color.rgb = mDif.rgb * _clDiffuse * _clGround.rgb;
    _color.a   = mDif.a;
    _color.rgb += _clSpecular * mSpc.rgb * _clGround.a;

    // apply scattering

    // compute in-scattering term
    float _fVL = dot( litDir, -_vRay );
    float fG = scat[1].w + scat[0].w * _fVL;
    fG = rsqrt( fG );
    fG = fG*fG*fG;
    float3 _vMie = scat[1].rgb * fG;
    float3 _vRayleigh = scat[0].rgb*(1.0f + _fVL*_fVL);
    float3 _vInscattering = scat[2].rgb * (_vMie + _vRayleigh) + scat[4].rgb;

    // ratio of scattering
    float fDistance = sqrt( _fSqDistance );
    float3 fRatio = exp( -scat[3].rgb * fDistance );

    _color.rgb = lerp( _vInscattering, _color.rgb, fRatio );
    Out_f4Color = _color;
}