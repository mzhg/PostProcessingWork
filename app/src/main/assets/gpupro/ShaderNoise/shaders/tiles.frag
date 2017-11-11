// #include "cellular3.glsl"
#version 120
#include "../../../shader_libs/NoiseLib.glsl"
varying vec3 vTexCoord3D;

void main(void) {
	vec2 F = WorleyNoise3(vTexCoord3D.xyz);
	float n = 0.1+F.y-F.x;
	gl_FragColor = vec4(n*0.6, n*1.1, n*0.5, 1.0);
}
