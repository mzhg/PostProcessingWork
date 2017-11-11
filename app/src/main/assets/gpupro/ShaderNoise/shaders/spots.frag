
// #include "cellular2x2x2.glsl"
#version 120
#include "../../../shader_libs/NoiseLib.glsl"
varying vec3 vTexCoord3D;

void main(void) {
	vec2 F = WorleyNoise2x2x2(vTexCoord3D);
	float s = fwidth(F.x);
	float n1 = smoothstep(0.4-s, 0.4+s, F.x);
	float n2 = smoothstep(0.5-s, 0.5+s, F.x);
	gl_FragColor = vec4(n1, n2, n2, 1.0);
}
