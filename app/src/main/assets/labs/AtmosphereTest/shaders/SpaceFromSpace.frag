//
// Atmospheric scattering fragment shader
//
// Author: Sean O'Neil
//
// Copyright (c) 2004 Sean O'Neil
//

layout(binding = 0) uniform sampler2D s2Test;

in vec3 vSecondColor;
in vec2 vTexcoord;

layout(location = 0) out vec4 Out_Color;

void main (void)
{
	Out_Color = vec4(vSecondColor, 1) * texture(s2Test, vTexcoord.st);
	//gl_FragColor = gl_SecondaryColor;
}
