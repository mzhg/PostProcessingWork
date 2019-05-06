layout(location = 0) out vec4 Out_Color;

layout(binding = 0) uniform samplerCube envMapIrrad;  // or Diffuse LD in Frostbite
layout(binding = 1) uniform samplerCube envMapSpecular;
layout(binding = 2) uniform sampler2D gEnvBrdf;   //  IntegrateBRDF in UE4 or DFG in frostbite
#define saturate(x) clamp(x, 0.0, 1.0)

#define ENV_TYPE_IRRA  0
#define ENV_TYPE_UE4   1
#define ENV_TYPE_FROSTBITE  2

#ifndef ENV_TYPE
#define ENV_TYPE  ENV_TYPE_IRRA
#endif

in vec3 m_Normal;

#if ENV_TYPE == ENV_TYPE_UE4
float ComputeCubemapFromRoughness(float Roughness, float MipCount)
{
    // Level starting from 1x1 mip
    float Level = 3 - 1.15 * log2(Roughness);
    return MipCount - 1 - Level;
}

vec3 EnvBRDF( vec3 SpecularColor, float Roughness, float NoV )
{
	// Importance sampled preintegrated G * F
	vec2 AB = textureLod( gEnvBrdf, vec2( NoV, Roughness ), 0).rg;

	// Anything less than 2% is physically impossible and is instead considered to be shadowing
	vec3 GF = SpecularColor * AB.x + saturate( 50.0 * SpecularColor.g ) * AB.y;
	return GF;
}

vec3 EnvBRDFApprox( vec3 SpecularColor, float Roughness, float NoV )
{
	// [ Lazarov 2013, "Getting More Physical in Call of Duty: Black Ops II" ]
	// Adaptation to fit our G term.
	const vec4 c0 = vec4( -1, -0.0275, -0.572, 0.022 );
	const vec4 c1 = vec4( 1, 0.0425, 1.04, -0.04 );

	vec4 r = Roughness * c0 + c1;
	float a004 = min( r.x * r.x, exp2( -9.28 * NoV ) ) * r.x + r.y;
	vec2 AB = vec2( -1.04, 1.04 ) * a004 + r.zw;

	// Anything less than 2% is physically impossible and is instead considered to be shadowing
	// Note: this is needed for the 'specular' show flag to work, since it uses a SpecularColor of 0
	AB.y *= saturate( 50.0 * SpecularColor.g );
	return SpecularColor * AB.x + AB.y;
}

float EnvBRDFApproxNonmetal( float Roughness, float NoV )
{
	// Same as EnvBRDFApprox( 0.04, Roughness, NoV )
	const vec2 c0 = { -1, -0.0275 };
	const vec2 c1 = { 1, 0.0425 };

	vec2 r = Roughness * c0 + c1;
	return min( r.x * r.x, exp2( -9.28 * NoV ) ) * r.x + r.y;
}

vec3 ApproximateSpecularIBL(vec3 SpecularColor, float Roughness, vec3 N, vec3 V )
{
	// Function replaced with prefiltered environment map sample
	vec3 R = 2 * dot( V, N ) * N - V;
	float Mip = ComputeCubemapFromRoughness(Roughness, textureQueryLevels(envMapSpecular));
	vec3 PrefilteredColor = //PrefilterEnvMap( Random, Roughness, R );
	     textureLod(envMapSpecular, R, Mip).rgb;

	float NoV = saturate( dot( N, V ) );
	return PrefilteredColor * EnvBRDF(SpecularColor, Roughness, NoV);
}

#elif ENV_TYPE == ENV_TYPE_FROSTBITE

float linearRoughnessToLod(float linearRoughness, float mipCount)
{
    return sqrt(linearRoughness) * (mipCount - 1);
}

vec3 getDiffuseDominantDir(vec3 N, vec3 V, float roughness)
{
    float a = 1.02341 * roughness - 1.51174;
    float b = -0.511705 * roughness + 0.755868;
    float factor = saturate((saturate(dot(N, V)) * a + b) * roughness);
    return normalize(mix(N, V, factor));
}

vec3 getSpecularDominantDir(vec3 N, vec3 R, float roughness)
{
    float smoothness = 1 - roughness;
    float factor = smoothness * (sqrt(smoothness) + roughness);
    return normalize(mix(N, R, factor));
}

vec3 evalLightProbeDiffuse(vec3 N, vec3 V, float roughness, vec3 baseColor)
{
    vec3 diffuseN = getDiffuseDominantDir(N, V, roughness);

    vec3 diffuseLighting = textureLod(envMapIrrad, diffuseN, 0.0).rgb;
    float diffDFG = textureLod(gEnvBrdf, vec2(saturate(dot(N,V)), roughness), 0.0).z;

    return diffuseLighting * diffDFG * baseColor;
}

vec3 evalLightProbeDiffuse(vec3 N, vec3 V, float roughness)
{
    return evalLightProbeDiffuse(N,V, roughness, vec3(1));
}

vec3 evalLightProbeSpecular(vec3 N, vec3 R, float NdotV, float roughness)
{
    float mipCount = textureQueryLevels(envMapSpecular);

    vec3 dominantDir = getSpecularDominantDir(N, R, roughness);
    float mipLevel = linearRoughnessToLod(roughness, mipCount);

    // Rebuild the function
    // L . D. ( f0.Gv .(1 - Fc) + Gv.Fc ) . cosTheta / (4 . NdotL . NdotV )
    ivec2 DFG_TEXTURE_SIZE = textureSize(gEnvBrdf, 0);
    NdotV = max (NdotV , 0.5/ DFG_TEXTURE_SIZE.x );
    vec3 preLD = textureLod (envMapSpecular , dominantDir , mipLevel).rgb ;

    // Sample pre - integrate DFG
    // Fc = (1-H.L)^5
    // PreIntegratedDFG .r = Gv .(1 - Fc)
    // PreIntegratedDFG .g = Gv.Fc
    vec2 preDFG = textureLod (gEnvBrdf, vec2 (NdotV , roughness ), 0).xy;

    // LD . ( f0.Gv .(1 - Fc) + Gv.Fc. f90 )
    vec3 f0 = vec3(0.04);
    float f90 = 1;
    return preLD * (f0 * preDFG .x + f90 * preDFG .y);

#if 0
    vec3 ld = probe.resources.specularTexture.SampleLevel(probe.resources.sampler, uv, mipLevel).rgb;

    float2 dfg = gProbeShared.dfgTexture.SampleLevel(gProbeShared.dfgSampler, float2(sd.NdotV, sd.roughness), 0).xy;

    // ld * (f0 * Gv * (1 - Fc)) + (f90 * Gv * Fc)
    return ld * (sd.specular * dfg.x + dfg.y);
#endif
}

/** Evaluate a 2D light-probe filtered using linear-filtering
*/
vec3 evalLightProbeLinear2D(vec3 N, vec3 V, float roughness)
{
    // Calculate the reflection vector
    vec3 R = reflect(-V, N);

    // Evaluate diffuse component
    vec3 diffuse = evalLightProbeDiffuse(N, V, roughness);

    // Get the specular component
    vec3 specular = evalLightProbeSpecular(N, R, saturate(dot(N,V)), roughness);
    return diffuse + specular;
}
#endif

in vec4 m_PositionWS;

uniform float gDiffuseMip = 4.0;
uniform vec3 g_EyePos;

void main()
{
    vec3 N = normalize(m_Normal);

#if ENV_TYPE == ENV_TYPE_IRRA
    Out_Color =  textureLod(envMapIrrad, N, 0.0);
#elif ENV_TYPE == ENV_TYPE_UE4
    vec3 V = normalize(g_EyePos - m_PositionWS.xyz);
    vec3 diffuse = textureLod(envMapSpecular, N, gDiffuseMip).rgb;
    vec3 specular = ApproximateSpecularIBL(vec3(0.04), 0.5, N, V);
    Out_Color.rgb = diffuse + specular;
    Out_Color.a = 1;
#elif ENV_TYPE == ENV_TYPE_FROSTBITE
    vec3 V = normalize(g_EyePos - m_PositionWS.xyz);
    Out_Color.rgb = evalLightProbeLinear2D(N, V, 0.5);
    Out_Color.a = 1;
#endif
}