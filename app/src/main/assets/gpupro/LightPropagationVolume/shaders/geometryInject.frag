#version 430

#extension GL_NV_shader_atomic_float : require
#extension GL_NV_shader_atomic_fp16_vector : require
#extension GL_NV_gpu_shader5 : require

/*Cosine lobe coeff*/
#define SH_cosLobe_C0 0.886226925f // sqrt(pi)/2 
#define SH_cosLobe_C1 1.02332671f // sqrt(pi/3) 

// SH_C0 * SH_cosLobe_C0 = 0.25000000007f
// SH_C1 * SH_cosLobe_C1 = 0.5000000011f

layout(rgba16f ,location = 0) uniform image3D GeometryVolume;
layout(early_fragment_tests )in;//turn on early depth tests

uniform vec3 v_gridDim;
uniform vec3 v_lightPos;
uniform float f_cellSize;

flat in ivec3 v_volumeCellIndex;
in vec3 v_posFromRSM;
in vec3 v_normalFromRSM;
in float surfelArea;

//Should I normalize the dir vector?
vec4 evalCosineLobeToDir(vec3 dir) {
	dir = normalize(dir);
	//f00, f-11, f01, f11
	return vec4( SH_cosLobe_C0, -SH_cosLobe_C1 * dir.y, SH_cosLobe_C1 * dir.z, -SH_cosLobe_C1 * dir.x );
}

//(As * clamp(dot(ns,w),0.0,1.0))/(cellsize * cellsize)
float calculateBlockingPotencial(vec3 dir, vec3 normal) {
	return clamp((surfelArea * clamp(dot(normal,dir),0.0,1.0))/(f_cellSize*f_cellSize),0.0,1.0); //It is probability so 0.0 - 1.0
}

void main()
{

	//Discard pixels with really small normal
	if( length( v_normalFromRSM ) < 0.01 ) {
		discard;
	}
	vec3 lightDir = normalize( v_lightPos - v_posFromRSM); //Both are in World space
	float blockingPotencial = calculateBlockingPotencial(lightDir, v_normalFromRSM);

	vec4 SHCoeffGV = evalCosineLobeToDir(v_normalFromRSM) * blockingPotencial;
	//surfelArea
	//vec4 SHCoeffGV = evalCosineLobeToDir(v_normalFromRSM) * surfelArea;

	imageAtomicAdd(GeometryVolume,v_volumeCellIndex,f16vec4(SHCoeffGV));

}