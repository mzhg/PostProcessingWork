layout(location=0) out vec4 Out_Color;

// little debug helpers
vec3 colors[15] = vec3[15]
(
	vec3(1.0f,0.0f,0.0f),
	vec3(0.0f,1.0f,0.0f),
	vec3(0.0f,0.0f,1.0f),
	vec3(0.0f,1.0f,1.0f),
	vec3(1.0f,0.0f,1.0f),
	vec3(1.0f,1.0f,0.0f),
	vec3(0.0f,0.0f,0.0f),
	vec3(1.0f,1.0f,1.0f),

	0.5f * vec3(1.0f,0.0f,0.0f),
	0.5f * vec3(0.0f,1.0f,0.0f),
	0.5f * vec3(0.0f,0.0f,1.0f),
	0.5f * vec3(0.0f,1.0f,1.0f),
	0.5f * vec3(1.0f,0.0f,1.0f),
	0.5f * vec3(1.0f,1.0f,0.0f),
	0.5f * vec3(1.0f,1.0f,1.0f)
);

in LinesOutVertexGS2PS
{
//    float4 Position;
   flat uint Level;
}_input;

void main()
{
    Out_Color = vec4(colors[_input.Level], 1.0f);
}