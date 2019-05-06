layout(location = 0) out vec4 Out_Irradiance;

layout(binding = 0) uniform samplerCube g_EnvMap;
uniform float g_Roughness;

in vec3 m_Normal;

#include "UE4Common.glsl"

vec3 PrefilterEnvMap( uvec2 Random, float Roughness, vec3 R )
{
	vec3 FilteredColor = vec3(0);
	float Weight = 0;
	const uint NumSamples = 64;

	for( uint i = 0; i < NumSamples; i++ )
	{
		vec2 E = Hammersley( i, NumSamples, Random );
		vec3 H = TangentToWorld( ImportanceSampleGGX( E, Pow4(Roughness) ).xyz, R );
		vec3 L = 2 * dot( R, H ) * H - R;
		float NoL = saturate( dot( R, L ) );

		if( NoL > 0 )
		{
			FilteredColor += textureLod( g_EnvMap, L, 0 ).rgb * NoL;
			Weight += NoL;
		}
	}

	return FilteredColor / max( Weight, 0.001 );
}


void main()
{
    uvec2 Random = Rand3DPCG16( ivec3( gl_FragCoord.xy, 0) ).xy;
    vec3 N = normalize(m_Normal);

    Out_Irradiance.rgb = PrefilterEnvMap(Random, g_Roughness, N);
    Out_Irradiance.a = 0;
}