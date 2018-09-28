//
// Atmospheric scattering fragment shader
//
// Author: Sean O'Neil
//
// Copyright (c) 2004 Sean O'Neil
//

//uniform sampler2D s2Tex1;
//uniform sampler2D s2Tex2;

in vec2 vTexcoord1;
in vec2 vTexcoord2;
in vec3 vSecondColor;
in vec3 vColor;

layout(location = 0) out vec4 Out_Color;

void main (void)
{
	Out_Color.rgb = vColor + 0.25 * vSecondColor;
	Out_Color.a = 1.;
	//gl_FragColor = gl_Color + texture2D(s2Tex1, gl_TexCoord[0].st) * texture2D(s2Tex2, gl_TexCoord[1].st) * gl_SecondaryColor;
}
