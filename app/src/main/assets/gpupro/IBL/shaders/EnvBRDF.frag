
#include "UE4Common.glsl"

// Appoximation of joint Smith term for GGX

// [Heitz 2014, "Understanding the Masking-Shadowing Function in Microfacet-Based BRDFs"]

float Vis_SmithJointApprox( float a2, float NoV, float NoL )
{
	float a = sqrt(a2);
	float Vis_SmithV = NoL * ( NoV * ( 1 - a ) + a );
	float Vis_SmithL = NoV * ( NoL * ( 1 - a ) + a );
	return 0.5 / ( Vis_SmithV + Vis_SmithL );

}

vec3 IntegrateBRDF( uvec2 Random, float Roughness, float NoV )
{
	vec3 V;
	V.x = sqrt( 1.0f - NoV * NoV );	// sin
	V.y = 0;
	V.z = NoV;						// cos

	float A = 0;
	float B = 0;
	float C = 0;

	const uint NumSamples = 64;

	for( uint i = 0; i < NumSamples; i++ )
	{
		vec2 E = Hammersley( i, NumSamples, Random );
		{
			vec3 H = ImportanceSampleGGX( E, Pow4(Roughness) ).xyz;
			vec3 L = 2 * dot( V, H ) * H - V;
			float NoL = saturate( L.z );
			float NoH = saturate( H.z );
			float VoH = saturate( dot( V, H ) );
			if( NoL > 0 )
			{
				float a = Square( Roughness );
				float a2 = a*a;
				float Vis = Vis_SmithJointApprox( a2, NoV, NoL );
				float Vis_SmithV = NoL * sqrt( NoV * (NoV - NoV * a2) + a2 );
				float Vis_SmithL = NoV * sqrt( NoL * (NoL - NoL * a2) + a2 );

				//float Vis = 0.5 * rcp( Vis_SmithV + Vis_SmithL );
				// Incident light = NoL
				// pdf = D * NoH / (4 * VoH)
				// NoL * Vis / pdf
				float NoL_Vis_PDF = NoL * Vis * (4 * VoH / NoH);
				float Fc = pow( 1 - VoH, 5 );
				A += (1 - Fc) * NoL_Vis_PDF;
				B += Fc * NoL_Vis_PDF;
			}
		}

#if 0
		{
			vec3 L = CosineSampleHemisphere( E ).xyz;
			vec3 H = normalize(V + L);
			float NoL = saturate( L.z );
			float NoH = saturate( H.z );
			float VoH = saturate( dot( V, H ) );
			float FD90 = ( 0.5 + 2 * VoH * VoH ) * Roughness;
			float FdV = 1 + (FD90 - 1) * pow( 1 - NoV, 5 );
			float FdL = 1 + (FD90 - 1) * pow( 1 - NoL, 5 );
			C += FdV * FdL * ( 1 - 0.3333 * Roughness );
		}
#endif
	}

	return vec3( A, B, C ) / NumSamples;
}

in vec4 m_f4UVAndScreenPos;
uniform vec2 g_Viewport;

layout(location = 0) out vec3 Out_Color;

void main()
{
    uvec2 Random = Rand3DPCG16( ivec3( gl_FragCoord.xy, 0) ).xy;

    float Roughness = m_f4UVAndScreenPos.y - 0.5/g_Viewport.y;
    float NoV = m_f4UVAndScreenPos.x - 0.5/g_Viewport.x;

    Out_Color = IntegrateBRDF(Random, Roughness, NoV);
}