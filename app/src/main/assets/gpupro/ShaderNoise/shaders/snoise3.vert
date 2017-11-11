#version 120

attribute vec4 aPosition;
uniform mat4 mvp;

uniform float time;
varying vec3 vTexCoord3D;

void main(void) {
	vTexCoord3D = aPosition.xyz * 4.0 + vec3(0.0, 0.0, time);
	gl_Position = mvp * aPosition;
}
