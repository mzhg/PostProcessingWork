layout(location = 0) in vec3 f3PosWS;
layout(location = 1) in vec2 f2MaskUV0;

uniform mat4 g_WorldViewProj;

out gl_PerVertex
{
	vec4 gl_Position;
};

void main()
{
	gl_Position = g_WorldViewProj * vec4(f3PosWS, 1.0);
}