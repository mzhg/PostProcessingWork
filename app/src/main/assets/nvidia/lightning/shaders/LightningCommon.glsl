#include "../../../shader_libs/PostProcessingHLSLCompatiable.glsl"

// uniforms
layout(binding = 0) uniform PerFrame
{
	mat4 world;
	mat4 view;
	mat4 projection;

	mat4 world_view;
	mat4 world_view_projection;

	float time;
};

// note that the order of the variables HAS to match the C++ struct/

layout(binding = 1) uniform LightningAppearance
{
	float3 ColorInside;					// color of the inside of the beam
	float ColorFallOffExponent;			// determines how quickly the color changes from
										// inside to outside

	float3 ColorOutside;				// color of the outside of the beam
	float2 BoltWidth;					// size in world space of the beam


};

layout(binding = 2) uniform LightningStructure
{
	// for ZigZag pattern
	float2 ZigZagFraction;				// determines at which percentage the segment will be broken
	float2 ZigZagDeviationRight;		// min and max of deviation in segments local frame
	float2 ZigZagDeviationUp;			// min and max of deviation in segments local frame
	float  ZigZagDeviationDecay;		// controls how fast the deviation get smaller

	// for Fork pattern
	float2 ForkFraction;				// similiar above, but for fork pattern
	float2 ForkZigZagDeviationRight;
	float2 ForkZigZagDeviationUp;
	float  ForkZigZagDeviationDecay;

	float2 ForkDeviationRight;
	float2 ForkDeviationUp;
	float2 ForkDeviationForward;
	float  ForkDeviationDecay;

	float2	ForkLength;					// min and max of length of fork segments, in world space
	float	ForkLengthDecay;			// decay of length
};

#define MaxTargets 8
layout(binding = 3) uniform LightningChain
{

	float3	ChainSource;

	float4 ChainTargetPositions[MaxTargets];

	int			NumTargets;
};

/*
layout(binding = 4) uniform PerSubdivision
{

};

layout(binding = 5) uniform Lightning
{
	;
};
*/

uniform bool	Fork;
uniform uint	SubdivisionLevel;
uniform float AnimationSpeed;

// decay based on global subdivision level
float Decay(float amount)
{
	return exp(-amount * SubdivisionLevel);
}

// decay based in explicit level
float Decay(float2 amount, uint level)
{
	return  amount.x * exp(-amount.y * level);
}

#if 0

layout(binding=0) uniform sampler1D g_RandomTex;

float Random()
{
    float seed = time * (gl_FragCoord.x * gl_FragCoord.y) * 0.01;
	float value = textureLod(g_RandomTex, seed, 0.0).r;
	return value;
}

#else
// Random number generation
// found in numerical recipes
// http://www.library.cornell.edu/nr/bookcpdf/c7-1.pdf

// per shader global variable to keep track of the last random number
int random_x;

#define RANDOM_IA 16807
#define RANDOM_IM 2147483647
#define RANDOM_AM (1.0f/float(RANDOM_IM))
#define RANDOM_IQ 127773
#define RANDOM_IR 2836
#define RANDOM_MASK 123459876
#define PI        3.14159265

float Random()
{
	int k;
	float ans;

	random_x ^= RANDOM_MASK;								//XORing with MASK allows use of zero and other
	k = random_x / RANDOM_IQ;								//simple bit patterns for idum.
	random_x = RANDOM_IA * (random_x - k * RANDOM_IQ ) - RANDOM_IR * k;	//Compute idum=(IA*idum) % IM without overif
	if (random_x < 0)
		random_x += RANDOM_IM;					//flows by Schrage抯 method.

	ans = RANDOM_AM * random_x;					//Convert idum to a floating result.
	random_x ^= RANDOM_MASK;					//Unmask before return.

	return ans;
}
#endif

void RandomSeed (int value)
{
	random_x = value;
	Random();
}

float Random(float low, float high)
{
	float v = Random();
	return low * (1.0f - v) + high * v;
}

float3 Random(float3 low, float3 high)
{
	float3 v = float3(Random(),Random(),Random());
	return low * (1.0 - v) + high * v;
}
