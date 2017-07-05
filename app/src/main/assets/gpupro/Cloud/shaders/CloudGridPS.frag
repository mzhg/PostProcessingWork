#include "Cloud_Common.glsl"

in float2 vTex;

layout(location = 0) out float Out_fColor;

void main()
{
    float4 clTex = texture( sCloud, vTex );

    // blend 4 channel of the cloud texture according to cloud cover
    float4 vDensity;
    vDensity = abs( fCloudCover - float4( 0.25f, 0.5f, 0.75f, 1.0f ) ) / 0.25f;
    vDensity = saturate( 1.0 - vDensity );
    float _fDensity = dot( clTex, vDensity );
#ifdef SHAODW
    // 0 : shadowed, 1 : lit
    Out_fColor = 1.0 - _fDensity;
#else
    Out_fColor = _fDensity;
#endif
}