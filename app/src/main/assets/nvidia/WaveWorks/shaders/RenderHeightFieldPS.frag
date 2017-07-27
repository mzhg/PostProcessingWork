#version 400

in PSIn_Diffuse
{
//	vec4 position;
	vec2 texcoord;
	vec3 normal;
	vec3 positionWS;
	vec4 layerdef;
	vec4 depthmap_scaler;
}_input;

layout (location = 0) out vec4 fragColor;
uniform bool g_RenderShadowmap;

#include "RenderHeightfieldCommon.glsl"

subroutine vec4 ColorOutput();
subroutine uniform ColorOutput pass;

subroutine(ColorOutput) vec4 RenderHeightFieldPS()
{
	vec3 pixel_to_light_vector = normalize(g_LightPosition-_input.positionWS);
	vec3 pixel_to_eye_vector = normalize(g_CameraPosition-_input.positionWS);
	vec3 microbump_normal; 
	vec3 texel;
	vec4 color;

	mat3 normal_rotation_matrix;

	// culling halfspace if needed
//	clip(g_HalfSpaceCullSign*(_input.positionWS.y-g_HalfSpaceCullPosition));
	if(g_HalfSpaceCullSign*(_input.positionWS.y-g_HalfSpaceCullPosition) < 0.0)
		discard;
	
	// fetching default microbump normal
	texel = texture(g_SandMicroBumpTexture, _input.texcoord).rbg * vec3(1,1,-1);
//	microbump_normal = normalize(2.0*g_SandMicroBumpTexture.Sample(SamplerAnisotropicWrap,_input.texcoord).rbg - float3 (1.0,1.0,1.0));
	microbump_normal = normalize(2.0*texel - 1.0);
	texel = texture(g_RockMicroBumpTexture, _input.texcoord).rbg * vec3(1,1,-1);
//	microbump_normal = normalize(lerp(microbump_normal,2*g_RockMicroBumpTexture.Sample(SamplerAnisotropicWrap,_input.texcoord).rbg - float3 (1.0,1.0,1.0),_input.layerdef.w));
	microbump_normal = normalize(mix(microbump_normal, 2.0 * texel - 1.0, _input.layerdef.w));

	//calculating base normal rotation matrix
	normal_rotation_matrix[1]=_input.normal;
	normal_rotation_matrix[2]=normalize(cross(vec3(-1.0,0.0,0.0),normal_rotation_matrix[1]));
	normal_rotation_matrix[0]=normalize(cross(normal_rotation_matrix[2],normal_rotation_matrix[1]));
//	microbump_normal=mul(microbump_normal,normal_rotation_matrix);
//	normal_rotation_matrix = transpose(normal_rotation_matrix);
	microbump_normal=normal_rotation_matrix * microbump_normal;
//	microbump_normal = NormalSampleToWorldSpace(microbump_normal, _input.normal, vec3(1,0,0));

	// getting diffuse color
//	color=g_SlopeDiffuseTexture.Sample(SamplerAnisotropicWrap,_input.texcoord);
//	color=lerp(color,g_SandDiffuseTexture.Sample(SamplerAnisotropicWrap,_input.texcoord),_input.layerdef.g*_input.layerdef.g);
//	color=lerp(color,g_RockDiffuseTexture.Sample(SamplerAnisotropicWrap,_input.texcoord),_input.layerdef.w*_input.layerdef.w);
//	color=lerp(color,g_GrassDiffuseTexture.Sample(SamplerAnisotropicWrap,_input.texcoord),_input.layerdef.b);
	color=texture(g_SlopeDiffuseTexture, _input.texcoord);
	color=mix(color, texture(g_SandDiffuseTexture,  _input.texcoord),_input.layerdef.g*_input.layerdef.g);
	color=mix(color, texture(g_RockDiffuseTexture,  _input.texcoord),_input.layerdef.w*_input.layerdef.w);
	color=mix(color, texture(g_GrassDiffuseTexture, _input.texcoord),_input.layerdef.b);
	// adding per-vertex lighting defined by displacement of vertex 
	color*=0.5+0.5*min(1.0,max(0.0,_input.depthmap_scaler.b/3.0f+0.5f));

	// calculating pixel position in light view space
//	float4 positionLS = mul(float4(_input.positionWS,1),g_LightModelViewProjectionMatrix);
//	vec4 positionLS = g_LightModelViewProjectionMatrix * vec4(_input.positionWS,1);
//	positionLS.xyz/=positionLS.w;
//	positionLS.x=(positionLS.x+1)*0.5;
//	positionLS.y=(1+positionLS.y)*0.5;


	// fetching shadowmap and shading
	float dsf=0.75f/4096.0f;
//	float shadow_factor=0.2*g_DepthTexture.SampleCmp(SamplerDepthAnisotropic,positionLS.xy,positionLS.z* 0.995f).r;
//	shadow_factor+=0.2*g_DepthTexture.SampleCmp(SamplerDepthAnisotropic,positionLS.xy+float2(dsf,dsf),positionLS.z* 0.995f).r;
//	shadow_factor+=0.2*g_DepthTexture.SampleCmp(SamplerDepthAnisotropic,positionLS.xy+float2(-dsf,dsf),positionLS.z* 0.995f).r;
//	shadow_factor+=0.2*g_DepthTexture.SampleCmp(SamplerDepthAnisotropic,positionLS.xy+float2(dsf,-dsf),positionLS.z* 0.995f).r;
//	shadow_factor+=0.2*g_DepthTexture.SampleCmp(SamplerDepthAnisotropic,positionLS.xy+float2(-dsf,-dsf),positionLS.z* 0.995f).r;
	
	vec4 positionLS = g_DepthModelViewProjectionMatrix * vec4(_input.positionWS, 1);
	positionLS.xyz/=positionLS.w;
	float shadow_factor = 0.2 * sampleCmp(positionLS.xy,positionLS.z* 0.995f);
	shadow_factor+= 0.2*sampleCmp(positionLS.xy+vec2(dsf,dsf),positionLS.z* 0.995f);
	shadow_factor+= 0.2*sampleCmp(positionLS.xy+vec2(-dsf,dsf),positionLS.z* 0.995f);
	shadow_factor+= 0.2*sampleCmp(positionLS.xy+vec2(dsf,-dsf),positionLS.z* 0.995f);
	shadow_factor+= 0.2*sampleCmp(positionLS.xy+vec2(-dsf,-dsf),positionLS.z* 0.995f);
	shadow_factor = max(1.0, shadow_factor);
//	vec4 positionLS = g_DepthModelViewProjectionMatrix * vec4(_input.positionWS, 1);
//	float shadow_factor =max(textureProj(g_DepthTexture, positionLS),1.0);
	color.rgb*=max(0.0,dot(pixel_to_light_vector,microbump_normal))*shadow_factor+0.2;


	// adding light from the sky
	color.rgb+=(0.0+0.2*max(0.0,(dot(vec3(0,1,0),microbump_normal))))*vec3(0.2,0.2,0.3);

	// making all a bit brighter, simultaneously pretending the wet surface is darker than normal;
	color.rgb*=0.5+0.8*max(0.0,min(1.0,_input.positionWS.y*0.5+0.5));

	// applying refraction caustics
	color.rgb*=(1.0+max(0.0,0.4+0.6*dot(pixel_to_light_vector,microbump_normal))*_input.depthmap_scaler.a*(0.4+0.6*shadow_factor));

	// applying fog
	color.rgb=mix(CalculateFogColor(pixel_to_light_vector,pixel_to_eye_vector).rgb,color.rgb,min(1.0,exp(-length(g_CameraPosition-_input.positionWS)*g_FogDensity)));
	color.a=length(g_CameraPosition-_input.positionWS);
	
	return color;
}

subroutine(ColorOutput) vec4 ColorPS() { return vec4(1.0, 1.0, 1.0, 1.0);}

void main()
{
	if(g_RenderShadowmap)
		fragColor = vec4(1);
	else
		fragColor = pass();
}