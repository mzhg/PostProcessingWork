#version 400

layout(location = 0) out vec4 color;

uniform vec3 g_LightPosition;
uniform vec3 g_CameraPosition;

uniform sampler2D g_SkyTexture;

const vec3 g_AtmosphereDarkColor     =vec3(0.6,0.6,0.7);
const vec3 g_AtmosphereBrightColor   =vec3(1.0,1.1,1.4);

// primitive simulation of non-uniform atmospheric fog
vec3 CalculateFogColor(vec3 pixel_to_light_vector, vec3 pixel_to_eye_vector)
{
	return mix(g_AtmosphereDarkColor,g_AtmosphereBrightColor,0.5*dot(pixel_to_light_vector,-pixel_to_eye_vector)+0.5);
}


in PSIn_Diffuse
{
	vec2 texcoord;
	vec3 positionWS;
}_input;

subroutine vec4 ColorOutput();
subroutine uniform ColorOutput pass;

subroutine(ColorOutput)
vec4 SkyPS()
{
	vec4 outcolor;
	vec3 acolor;
	vec3 pixel_to_light_vector = normalize(g_LightPosition-_input.positionWS);
	vec3 pixel_to_eye_vector = normalize(g_CameraPosition-_input.positionWS);

//	outcolor=g_SkyTexture.Sample(SamplerLinearWrap,float2(_input.texcoord.x,pow(_input.texcoord.y,2)));
	outcolor=texture(g_SkyTexture, vec2(_input.texcoord.x,pow(_input.texcoord.y,2.0)));
	acolor =CalculateFogColor(pixel_to_light_vector,pixel_to_eye_vector);
	outcolor.rgb = mix(outcolor.rgb,acolor,pow(clamp(_input.texcoord.y, 0.0, 1.0),10.0));
	outcolor.a =1.0;
	
	return outcolor;
}

subroutine(ColorOutput) vec4 ColorPS() { return vec4(1,1,1,1);}

void main()
{
	color = pass();
}