#version 430

layout(location=0) out vec4 LPVGridR;
layout(location=1) out vec4 LPVGridG;
layout(location=2) out vec4 LPVGridB;

in vec3 v_GSposFromRSM;
in vec3 v_GSnormalFromRSM;
in vec4 v_GSfluxFromRSM;

#define PI 3.1415926f

/*Spherical harmonics coefficients - precomputed*/
#define SH_C0 0.282094792f // 1 / 2sqrt(pi)
#define SH_C1 0.488602512f // sqrt(3/pi) / 2

/*Cosine lobe coeff*/
#define SH_cosLobe_C0 0.886226925f // sqrt(pi)/2 
#define SH_cosLobe_C1 1.02332671f // sqrt(pi/3) 

// SH_C0 * SH_cosLobe_C0 = 0.25000000007f
// SH_C1 * SH_cosLobe_C1 = 0.5000000011f

uniform vec3 v_gridDim;

//Should I normalize the dir vector?
vec4 evalCosineLobeToDir(vec3 dir) {
	dir = normalize(dir);
	//f00, f-11, f01, f11
	return vec4( SH_cosLobe_C0, -SH_cosLobe_C1 * dir.y, SH_cosLobe_C1 * dir.z, -SH_cosLobe_C1 * dir.x );
}

void main()
{

	//Discard pixels with really small normal
	/*if( length( v_GSnormalFromRSM ) < 0.01 ) {
		discard;
	}*/

	vec4 SHCoeffsR = evalCosineLobeToDir(v_GSnormalFromRSM) / PI * v_GSfluxFromRSM.r;
	vec4 SHCoeffsG = evalCosineLobeToDir(v_GSnormalFromRSM) / PI* v_GSfluxFromRSM.g;
	vec4 SHCoeffsB = evalCosineLobeToDir(v_GSnormalFromRSM) / PI * v_GSfluxFromRSM.b;

	LPVGridR = SHCoeffsR;
	LPVGridG = SHCoeffsG;
	LPVGridB = SHCoeffsB;
}