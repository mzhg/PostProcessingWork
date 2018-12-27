#version 430

#extension GL_NV_shader_atomic_float : require
#extension GL_NV_shader_atomic_fp16_vector : require
#extension GL_NV_gpu_shader5 : require

#define PI 3.1415926f

/*Spherical harmonics coefficients - precomputed*/
#define SH_C0 0.282094792f // 1 / 2sqrt(pi)
#define SH_C1 0.488602512f // sqrt(3/pi) / 2

/*Cosine lobe coeff*/
#define SH_cosLobe_C0 0.886226925f // sqrt(pi)/2 
#define SH_cosLobe_C1 1.02332671f // sqrt(pi/3) 

// SH_C0 * SH_cosLobe_C0 = 0.25000000007f
// SH_C1 * SH_cosLobe_C1 = 0.5000000011f

layout(rgba16f ,location = 0) uniform image3D LPVGridR;
layout(rgba16f ,location = 1) uniform image3D LPVGridG;
layout(rgba16f ,location = 2) uniform image3D LPVGridB;

layout(early_fragment_tests )in;//turn on early depth tests

uniform vec3 v_gridDim;

flat in ivec3 v_volumeCellIndex;
in vec3 v_posFromRSM;
in vec3 v_normalFromRSM;
in vec4 v_fluxFromRSM;

//Should I normalize the dir vector?
vec4 evalCosineLobeToDir(vec3 dir) {
	dir = normalize(dir);
	//f00, f-11, f01, f11
	return vec4( SH_cosLobe_C0, -SH_cosLobe_C1 * dir.y, SH_cosLobe_C1 * dir.z, -SH_cosLobe_C1 * dir.x );
}

void main()
{

	//Discard pixels with really small normal
	/*if( length( v_normalFromRSM ) < 0.01 ) {
		discard;
	}*/

	vec4 SHCoeffsR = evalCosineLobeToDir(v_normalFromRSM)/ PI * v_fluxFromRSM.r;
	vec4 SHCoeffsG = evalCosineLobeToDir(v_normalFromRSM)/ PI * v_fluxFromRSM.g;
	vec4 SHCoeffsB = evalCosineLobeToDir(v_normalFromRSM)/ PI * v_fluxFromRSM.b;


	imageAtomicAdd(LPVGridR,v_volumeCellIndex,f16vec4(SHCoeffsR));
	imageAtomicAdd(LPVGridG,v_volumeCellIndex,f16vec4(SHCoeffsG));
	imageAtomicAdd(LPVGridB,v_volumeCellIndex,f16vec4(SHCoeffsB));

}