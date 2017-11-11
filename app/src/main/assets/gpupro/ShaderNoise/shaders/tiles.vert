// #include "snoise3.glsl"
#version 120
#include "../../../shader_libs/NoiseLib.glsl"
attribute vec4 aPosition;

uniform float time;
uniform mat4 mvp;

varying vec3 vTexCoord3D;

void main(void) {
	vTexCoord3D = aPosition.xyz * 4.0
      + 0.2 * vec3(SimplexNoise(aPosition.xyz + vec3(0.0, 0.0, time)),
             SimplexNoise(aPosition.xyz + vec3(43.0, 17.0, time)),
             SimplexNoise(aPosition.xyz + vec3(0.0, -43.0, time-17.0)));
	gl_Position = mvp * aPosition;
}
