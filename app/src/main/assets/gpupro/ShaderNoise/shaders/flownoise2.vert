#version 120

attribute vec4 aPosition;
uniform mat4 mvp;

varying vec2 vTexCoord2D;

void main(void) {
	vTexCoord2D = aPosition.xy * 8.0;
	gl_Position = mvp * aPosition;
}
