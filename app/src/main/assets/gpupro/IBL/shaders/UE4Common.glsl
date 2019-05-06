#define PI 3.1415926

vec2 Hammersley( uint Index, uint NumSamples, uvec2 Random )
{
	float E1 = fract( float(Index) / float(NumSamples) + float( Random.x & 0xffff ) / float(1<<16) );
	float E2 = float( bitfieldReverse(Index) ^ Random.y ) * 2.3283064365386963e-10;
	return vec2( E1, E2 );
}

vec4 ImportanceSampleGGX( vec2 E, float a2 )
{
	float Phi = 2 * PI * E.x;
	float CosTheta = sqrt( (1 - E.y) / ( 1 + (a2 - 1) * E.y ) );
	float SinTheta = sqrt( 1 - CosTheta * CosTheta );

	vec3 H;
	H.x = SinTheta * cos( Phi );
	H.y = SinTheta * sin( Phi );
	H.z = CosTheta;

	float d = ( CosTheta * a2 - CosTheta ) * CosTheta + 1;
	float D = a2 / ( PI*d*d );
	float PDF = D * CosTheta;

	return vec4( H, PDF );
}

float Pow2(float x) { return x * x;}
float Pow4(float x) { return x*x*x*x;}
float Square(float x) { return x*x;}

// [ Duff et al. 2017, "Building an Orthonormal Basis, Revisited" ]
mat3 GetTangentBasis( vec3 TangentZ )
{
	const float Sign = TangentZ.z >= 0 ? 1 : -1;
	const float a = -1.0/( Sign + TangentZ.z );
	const float b = TangentZ.x * TangentZ.y * a;

	vec3 TangentX = vec3( 1 + Sign * a * Pow2( TangentZ.x ), Sign * b, -Sign * TangentZ.x );
	vec3 TangentY = vec3( b,  Sign + a * Pow2( TangentZ.y ), -TangentZ.y );

	return mat3( TangentX, TangentY, TangentZ );
}

vec3 TangentToWorld( vec3 Vec, vec3 TangentZ )
{
//	return mul( Vec, GetTangentBasis( TangentZ ) );
    return GetTangentBasis( TangentZ ) * Vec;
}

#define saturate(x) clamp(x, 0.0, 1.0)

// 3D random number generator inspired by PCGs (permuted congruential generator)
// Using a **simple** Feistel cipher in place of the usual xor shift permutation step
// @param v = 3D integer coordinate
// @return three elements w/ 16 random bits each (0-0xffff).
// ~8 ALU operations for result.x    (7 mad, 1 >>)
// ~10 ALU operations for result.xy  (8 mad, 2 >>)
// ~12 ALU operations for result.xyz (9 mad, 3 >>)
uvec3 Rand3DPCG16(ivec3 p)
{
	// taking a signed int then reinterpreting as unsigned gives good behavior for negatives
	uvec3 v = uvec3(p);

	// Linear congruential step. These LCG constants are from Numerical Recipies
	// For additional #'s, PCG would do multiple LCG steps and scramble each on output
	// So v here is the RNG state
	v = v * 1664525u + 1013904223u;

	// PCG uses xorshift for the final shuffle, but it is expensive (and cheap
	// versions of xorshift have visible artifacts). Instead, use simple MAD Feistel steps
	//
	// Feistel ciphers divide the state into separate parts (usually by bits)
	// then apply a series of permutation steps one part at a time. The permutations
	// use a reversible operation (usually ^) to part being updated with the result of
	// a permutation function on the other parts and the key.
	//
	// In this case, I'm using v.x, v.y and v.z as the parts, using + instead of ^ for
	// the combination function, and just multiplying the other two parts (no key) for
	// the permutation function.
	//
	// That gives a simple mad per round.
	v.x += v.y*v.z;
	v.y += v.z*v.x;
	v.z += v.x*v.y;
	v.x += v.y*v.z;
	v.y += v.z*v.x;
	v.z += v.x*v.y;

	// only top 16 bits are well shuffled
	return v >> 16u;
}