#include "../../../shader_libs/PostProcessingHLSLCompatiable.glsl"
//------------------------------------------------------------------------------------
// Global variables
//------------------------------------------------------------------------------------

// Constant

uniform float3      g_LightPosition;
uniform float3      g_CameraPosition;
uniform float4x4    g_ModelViewMatrix;
uniform float4x4    g_ModelViewProjectionMatrix;
uniform float4x4    g_LightModelViewProjectionMatrix;
uniform float4x4    g_WorldToTopDownTextureMatrix;

uniform float3      g_WaterTransmittance = {0.065,0.028,0.035}; // light absorption per meter for coastal water, taken from here: http://www.seafriends.org.nz/phgraph/water.htm http://www.seafriends.org.nz/phgraph/phdwg34.gif
uniform float3      g_WaterScatterColor = {0.0,0.7,0.3};
uniform float3      g_WaterSpecularColor = {1.1,0.8,0.5};
uniform float       g_WaterScatterIntensity = 0.1;
uniform float		g_WaterSpecularIntensity = 10.0f;

uniform float3		g_FoamColor = {0.90f, 0.95f, 1.0f};
uniform float3		g_FoamUnderwaterColor = {0.0,0.7,0.6};

uniform float       g_WaterSpecularPower = 200.0;
uniform float3      g_AtmosphereBrightColor = {1.1,0.9,0.6};
uniform float3      g_AtmosphereDarkColor = {0.4,0.4,0.5};
uniform float		g_FogDensity = 1.0f/1500.0f;

uniform float4		g_WireframeColor = {1.0,1.0,1.0,1.0};

uniform float2      g_WindDirection;

uniform float2      g_ScreenSizeInv = {1.0/1280.0, 1.0/720.0};
uniform float		g_ZNear;
uniform float		g_ZFar;
uniform float		g_Time;

uniform float		g_GerstnerSteepness;
uniform float		g_BaseGerstnerAmplitude;
uniform float		g_BaseGerstnerWavelength;
uniform float		g_BaseGerstnerSpeed;
uniform float		g_BaseGerstnerParallelness;
uniform int		    g_enableShoreEffects;

uniform float		g_Wireframe;
uniform float2		g_WinSize = {1280.0,720.0};

//-----------------------------------------------------------------------------------
// Texture & Samplers
//-----------------------------------------------------------------------------------
//layout(binding = 8) uniform sampler2D g_LogoTexture;
layout(binding = 8) uniform sampler2D g_ReflectionTexture;
layout(binding = 9) uniform sampler2D g_RefractionTexture;
layout(binding = 10) uniform sampler2D g_RefractionDepthTextureResolved;
layout(binding = 11) uniform sampler2D g_WaterNormalMapTexture;
layout(binding = 12) uniform sampler2DShadow g_ShadowmapTexture;
layout(binding = 13) uniform sampler2D g_FoamIntensityTexture;
layout(binding = 14) uniform sampler2D g_FoamDiffuseTexture;
layout(binding = 15) uniform sampler2D g_DataTexture;

const float kTopDownDataPixelsPerMeter = 256.0f/700.0; // taken from SDF generation source code, the SDF texture size is 256x256, the viewport size is 700x700
const float kMaxDepthBelowSea = 50.0f;
const float kMaxDistance = 20.0f; // taken from SDF generation code
const float kNumWaves = 1.0; // Total number of Gerster waves of different amplitude, speed etc to calculate, 
									// i+1-th wave has 20% smaller amplitude, 
								    // 20% smaller phase and group speed and 20% less parallelity
							        // Note that all the waves will share the same gerstnerMultiplierOut (lerping between ocean waves and Gerstner waves) for simplicity

float3 ConvertToWorldPos(float3 pos)
{
    return float3(pos.xz, -pos.y);
}

void GetGerstnerVertexAttributes(float3 posWS, out float2 sdfUVOut, out float3 offsetOut, out float gerstnerMultiplierOut)
{
	// getting UV for fetching SDF texture
	float4 topDownPosition = mul( float4( posWS.xyz, 1), g_WorldToTopDownTextureMatrix );
	float2 uv = topDownPosition.xy/topDownPosition.w * 0.5f + 0.5f;
//	uv.y = 1-uv.y;  TODO

	// initializing the outputs so we can exit early
	sdfUVOut = uv;
	offsetOut = float3 (0.0,0.0,0.0);
	gerstnerMultiplierOut = 0;

	// getting SDF
	const float4 tdData = textureLod(g_DataTexture, uv, 0 );  // SamplerTrilinearBorder

	// early out without adding gerstner waves if far from shore
	if((tdData.x >= kMaxDistance - 0.1))
	{
		return;
	}

	// initializing variables common to all Gerstner waves
	float phaseShift = g_Time;
	float sdfPhase = tdData.x*kMaxDistance/kTopDownDataPixelsPerMeter;
	float distanceMultiplier =  saturate(1.0-tdData.x); // Shore waves linearly fade in on the edges of SDF
	float depthMultiplier = saturate((g_BaseGerstnerWavelength*0.5 + tdData.y)*0.5); // Shore waves fade in when depth is less than half the wave length, we use 0.25 as this parameter also allows shore waves to heighten as the depth decreases
	gerstnerMultiplierOut = distanceMultiplier*depthMultiplier;

	// initializing variables to be changed along summing up the waves
	float gerstnerWavelength = g_BaseGerstnerWavelength;
	float gerstnerOmega = 2.0*3.141592 / g_BaseGerstnerWavelength; // angular speed of gerstner wave
	float gerstnerParallelness = g_BaseGerstnerParallelness; // "parallelness" of shore waves. 0 means the waves are parallel to shore, 1 means the waves are parallel to wind gradient
	float gerstnerSpeed = g_BaseGerstnerSpeed; // phase speed of gerstner waves
	float gerstnerAmplitude = g_BaseGerstnerAmplitude;
	float2 windDirection = g_WindDirection;

	// summing up the waves
	for(float i = 0.0; i < kNumWaves; i+=1.0)
	{
		float windPhase = dot(windDirection, posWS.xz);
		float gerstnerPhase = 2.0*3.141592*(lerp( sdfPhase, windPhase, gerstnerParallelness)/gerstnerWavelength);
		float2 propagationDirection = normalize( lerp(-tdData.zw + windDirection * 0.000001f, g_WindDirection, gerstnerParallelness*gerstnerParallelness));
		float gerstnerGroupSpeedPhase = 2.0*3.141592*(lerp( sdfPhase, windPhase, gerstnerParallelness*3.0)/gerstnerWavelength); // letting the group speed phase to be non-parallel to propagation phase, so altering parallelness modificator fot this

		float groupSpeedMultiplier = 0.5 + 0.5*cos((gerstnerGroupSpeedPhase + gerstnerOmega*gerstnerSpeed*phaseShift/2.0)/2.7); // Group speed for water waves is half of the phase speed, we allow 2.7 wavelengths to be in wave group, not so much as breaking shore waves lose energy quickly
		float worldSpacePosMultiplier = 0.75 + 0.25*sin(phaseShift*0.3 + 0.5*posWS.x/gerstnerWavelength)*sin(phaseShift*0.4 + 0.5*posWS.y/gerstnerWavelength); // slowly crawling worldspace aligned checkerboard pattern that damps gerstner waves further
		float depthMultiplier = saturate((gerstnerWavelength*0.5 + tdData.y)*0.5); // Shore waves fade in when depth is less than half the wave length
		float gerstnerMultiplier = distanceMultiplier*depthMultiplier*groupSpeedMultiplier*worldSpacePosMultiplier; // final scale factor applied to base Gerstner amplitude and used to mix between ocean waves and shore waves

		float steepness = g_GerstnerSteepness;
		float baseAmplitude = gerstnerMultiplier * gerstnerAmplitude; //amplitude gradually increases as wave runs over shallower seabed
		float breakerMultiplier = saturate((baseAmplitude*2.0*1.28 + tdData.y)/gerstnerAmplitude); // Wave height is 2*amplitude, a wave will start to break when it approximately reaches a water depth of 1.28 times the wave height, empirically: http://passyworldofmathematics.com/mathematics-of-ocean-waves-and-surfing/

		// calculating Gerstner offset
		float s,c;
		sincos(gerstnerPhase + gerstnerOmega*gerstnerSpeed*phaseShift, s, c);
		float waveVerticalOffset = s * baseAmplitude;
		offsetOut.y += waveVerticalOffset;
		offsetOut.xz += c * propagationDirection * steepness * baseAmplitude; // trochoidal Gerstner wave
		offsetOut.xz -= propagationDirection * s * baseAmplitude * breakerMultiplier * 2.0; // adding wave forward skew due to its bottom slowing down, so the forward wave front gradually becomes vertical
		float breakerPhase = gerstnerPhase + gerstnerOmega*gerstnerSpeed*phaseShift + 3.141592*0.05;
		float fp = frac(breakerPhase/(3.141592*2.0));
		offsetOut.xz -= 0.5*baseAmplitude*propagationDirection*breakerMultiplier*(saturate(fp*10.0) - saturate(-1.0 + fp*10.0)); // moving breaking area of the wave further forward

		// updating the parameters for next wave
		gerstnerWavelength *= 0.66;
		gerstnerOmega /= 0.66;
		gerstnerSpeed *= 0.66;
		gerstnerAmplitude *= 0.66;
		gerstnerParallelness *= 0.66;
		windDirection.xy *= float2(-1.0,1.0)*windDirection.yx; // rotating wind direction

		offsetOut.y += baseAmplitude*1.2; // Adding vertical displacement as the wave increases while rolling on the shallow area
	}

}

void GetGerstnerSurfaceAttributes( float2 sdfUV, float2 posWS, out float3 normalOut, out float breakerOut, out float foamTrailOut)
{
	// initializing the outputs
	normalOut = float3 (0.0,1.0,0.0);
	foamTrailOut = 0.0;
	breakerOut = 0.0;

	// getting SDF
	const float4 tdData = textureLod(g_DataTexture, sdfUV, 0 );  // SamplerTrilinearBorder

	// initializing variables common to all Gerstner waves
	float phaseShift = g_Time;
	float sdfPhase = tdData.x*kMaxDistance/kTopDownDataPixelsPerMeter;
	float distanceMultiplier = saturate(1.0-tdData.x); // Shore waves linearly fade in on the edges of SDF

	// initializing variables to be changed along summing up the waves
	float gerstnerWavelength = g_BaseGerstnerWavelength;
	float gerstnerOmega = 2.0*3.141592 / g_BaseGerstnerWavelength; // angular speed of gerstner wave
	float gerstnerParallelness = g_BaseGerstnerParallelness; // "parallelness" of shore waves. 0 means the waves are parallel to shore, 1 means the waves are parallel to wind gradient
	float gerstnerSpeed = g_BaseGerstnerSpeed; // phase speed of gerstner waves
	float gerstnerAmplitude = g_BaseGerstnerAmplitude;
	float2 windDirection = g_WindDirection;

	// summing up the waves
	for(float i = 0.0; i < kNumWaves; i+=1.0)
	{
		float windPhase = dot(windDirection, posWS.xy);
		float gerstnerPhase = 2.0*3.141592*(lerp( sdfPhase, windPhase, gerstnerParallelness)/gerstnerWavelength);
		float2 propagationDirection = normalize( lerp(-tdData.zw + windDirection * 0.000001f, g_WindDirection, gerstnerParallelness*gerstnerParallelness));
		float gerstnerGroupSpeedPhase = 2.0*3.141592*(lerp( sdfPhase, windPhase, gerstnerParallelness*3.0)/gerstnerWavelength); // letting the group speed phase to be non-parallel to propagation phase, so altering parallelness modificator fot this

		float groupSpeedMultiplier = 0.5 + 0.5*cos((gerstnerGroupSpeedPhase + gerstnerOmega*gerstnerSpeed*phaseShift/2.0)/2.7); // Group speed for water waves is half of the phase speed, we allow 2.7 wavelengths to be in wave group, not so much as breaking shore waves lose energy quickly
		float worldSpacePosMultiplier = 0.75 + 0.25*sin(phaseShift*0.3 + 0.5*posWS.x/gerstnerWavelength)*sin(phaseShift*0.4 + 0.5*posWS.y/gerstnerWavelength); // slowly crawling worldspace aligned checkerboard pattern that damps gerstner waves further
		float depthMultiplier = saturate((gerstnerWavelength*0.5 + tdData.y)*0.5); // Shore waves fade in when depth is less than half the wave length
		float gerstnerMultiplier = distanceMultiplier*depthMultiplier*groupSpeedMultiplier*worldSpacePosMultiplier; // final scale factor applied to base Gerstner amplitude and used to mix between ocean waves and shore waves

		float steepness = g_GerstnerSteepness;
		float baseAmplitude = gerstnerMultiplier * gerstnerAmplitude; //amplitude gradually increases as wave runs over shallower seabed

		// calculating normal
		float s,c;
		sincos(gerstnerPhase + gerstnerOmega*gerstnerSpeed*phaseShift, s, c);
		normalOut.y -= gerstnerOmega*steepness*baseAmplitude*s;
		normalOut.xz -= gerstnerOmega*baseAmplitude*c*propagationDirection;   // orienting normal according to direction of wave propagation. No need to normalize, it is unit length.

		// calculating foam parameters
		float breakerMultiplier = saturate((baseAmplitude*2.0*1.28 + tdData.y)/gerstnerAmplitude); // Wave height is 2*amplitude, a wave will start to break when it approximately reaches a water depth of 1.28 times the wave height, empirically: http://passyworldofmathematics.com/mathematics-of-ocean-waves-and-surfing/

		float foamTrailPhase = gerstnerPhase + gerstnerOmega*gerstnerSpeed*phaseShift + 3.141592*0.05; // delaying foam trail a bit so it's following the breaker
		float fp = frac(foamTrailPhase/(3.141592*2.0));
		foamTrailOut += gerstnerMultiplier*breakerMultiplier*(saturate(fp*10.0) - saturate(fp*1.1)); // only breaking waves leave foamy trails
		breakerOut += gerstnerMultiplier*breakerMultiplier*(saturate(fp*10.0) - saturate(-1.0 + fp*10.0)); // making narrow sawtooth pattern

		// updating the parameters for next wave
		gerstnerWavelength *= 0.66;
		gerstnerOmega /= 0.66;
		gerstnerSpeed *= 0.66;
		gerstnerAmplitude *= 0.66;
		gerstnerParallelness *= 0.66;
		windDirection.xy *= float2(-1.0,1.0)*windDirection.yx; // rotating wind direction
	}
}