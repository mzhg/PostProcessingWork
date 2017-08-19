#version 400

in PSIn_Diffuse
{
	vec4 position;
	vec2 texcoord;
	vec3 normal;
	vec3 positionWS;
//	vec4 layerdef;
	vec4 depthmap_scaler;
}_input;

layout (location = 0) out vec4 fragColor;

#include "RenderHeightfieldCommon.glsl"

subroutine vec4 ColorOutput();
subroutine uniform ColorOutput pass;

subroutine(ColorOutput) vec4 WaterPatchPS()
{
	vec4 color;
	vec3 pixel_to_light_vector = normalize(g_LightPosition-_input.positionWS);
	vec3 pixel_to_eye_vector = normalize(g_CameraPosition-_input.positionWS);
	vec3 reflected_eye_to_pixel_vector;
	vec3 microbump_normal; 
	mat3 normal_rotation_matrix;

	float fresnel_factor;
	float diffuse_factor;
	float specular_factor;
	float scatter_factor;
	vec4 refraction_color;
	vec4 reflection_color;
	vec4 disturbance_eyespace;

	float water_depth;
	vec4 water_color;

	// calculating pixel position in light space
//	float4 positionLS = mul(float4(_input.positionWS,1),g_LightModelViewProjectionMatrix);
	vec4 positionLS = g_LightModelViewProjectionMatrix * vec4(_input.positionWS, 1);
	positionLS.xyz/=positionLS.w;
	positionLS.x=(positionLS.x+1.0)*0.5;
	positionLS.y=(positionLS.y+1.0)*0.5;

	// calculating shadow multiplier to be applied to diffuse/scatter/specular light components
	float dsf=1.0f/4096.0f;
//	float shadow_factor=0.2*g_DepthTexture.SampleCmp(SamplerDepthAnisotropic,positionLS.xy,positionLS.z* 0.995f).r;
//	shadow_factor+=0.2*g_DepthTexture.SampleCmp(SamplerDepthAnisotropic,positionLS.xy+float2(dsf,dsf),positionLS.z* 0.995f).r;
//	shadow_factor+=0.2*g_DepthTexture.SampleCmp(SamplerDepthAnisotropic,positionLS.xy+float2(-dsf,dsf),positionLS.z* 0.995f).r;
//	shadow_factor+=0.2*g_DepthTexture.SampleCmp(SamplerDepthAnisotropic,positionLS.xy+float2(dsf,-dsf),positionLS.z* 0.995f).r;
//	shadow_factor+=0.2*g_DepthTexture.SampleCmp(SamplerDepthAnisotropic,positionLS.xy+float2(-dsf,-dsf),positionLS.z* 0.995f).r;
	
	float shadow_factor = 0.2*sampleCmp(positionLS.xy,positionLS.z* 0.995f);
	shadow_factor+=0.2*sampleCmp(positionLS.xy+vec2(dsf,dsf),positionLS.z* 0.995f);
	shadow_factor+=0.2*sampleCmp(positionLS.xy+vec2(-dsf,dsf),positionLS.z* 0.995f);
	shadow_factor+=0.2*sampleCmp(positionLS.xy+vec2(dsf,-dsf),positionLS.z* 0.995f);
	shadow_factor+=0.2*sampleCmp(positionLS.xy+vec2(-dsf,-dsf),positionLS.z* 0.995f);
//	shadow_factor = max(1.0, shadow_factor);
//	vec4 positionLS = g_DepthModelViewProjectionMatrix * vec4(_input.positionWS, 1);
//	float shadow_factor = max(textureProj(g_DepthTexture, positionLS),1.0);
	
	// need more high frequency bumps for plausible water surface, so creating normal defined by 2 instances of same bump texture
//	microbump_normal = normalize(2*g_WaterBumpTexture.Sample(SamplerAnisotropicWrap,_input.texcoord-g_WaterBumpTexcoordShift*0.2).gbr - float3 (1,-8,1));
//	microbump_normal+= normalize(2*g_WaterBumpTexture.Sample(SamplerAnisotropicWrap,_input.texcoord*0.5+g_WaterBumpTexcoordShift*0.05).gbr - float3 (1,-8,1));
	microbump_normal = normalize(2.0*texture(g_WaterBumpTexture, _input.texcoord-g_WaterBumpTexcoordShift*0.2).gbr - vec3 (1,-8,1));
	microbump_normal+= normalize(2.0*texture(g_WaterBumpTexture, _input.texcoord*0.5+g_WaterBumpTexcoordShift*0.05).gbr - vec3 (1,-8,1));
	// calculating base normal rotation matrix
	normal_rotation_matrix[1]=_input.normal.xyz;
	normal_rotation_matrix[2]=normalize(cross(vec3(0.0,0.0,-1.0),normal_rotation_matrix[1]));
	normal_rotation_matrix[0]=normalize(cross(normal_rotation_matrix[2],normal_rotation_matrix[1]));

	// applying base normal rotation matrix to high frequency bump normal
//	microbump_normal=mul(normalize(microbump_normal),normal_rotation_matrix);
//	normal_rotation_matrix=transpose(normal_rotation_matrix);
	microbump_normal=normal_rotation_matrix * normalize(microbump_normal);
//	microbump_normal = NormalSampleToWorldSpace(microbump_normal, _input.normal, vec3(0,0,-1));
	
	// simulating scattering/double refraction: light hits the side of wave, travels some distance in water, and leaves wave on the other side
	// it's difficult to do it physically correct without photon mapping/ray tracing, so using simple but plausible emulation below
	
	// only the crests of water waves generate double refracted light
	scatter_factor=2.5*max(0.0,_input.positionWS.y*0.25+0.25);
	if(scatter_factor > 0.0)
	{
		// the waves that lie between camera and light projection on water plane generate maximal amount of double refracted light 
		scatter_factor*=shadow_factor*pow(max(0.0,dot(normalize(vec3(pixel_to_light_vector.x,0.0,pixel_to_light_vector.z)),-pixel_to_eye_vector)),2.0);
	
		// the slopes of waves that are oriented back to light generate maximal amount of double refracted light 
		scatter_factor*=pow(max(0.0,1.0-dot(pixel_to_light_vector,microbump_normal)),8.0);
	}
	
	// water crests gather more light than lobes, so more light is scattered under the crests
	scatter_factor+=shadow_factor*1.5*g_WaterColorIntensity.y*max(0,_input.positionWS.y+1)*
		// the scattered light is best seen if observing direction is normal to slope surface
		max(0,dot(pixel_to_eye_vector,microbump_normal))*
		// fading scattered light out at distance and if viewing direction is vertical to avoid unnatural look
		max(0,1-pixel_to_eye_vector.y)*(300.0/(300+length(g_CameraPosition-_input.positionWS)));

	// fading scatter out by 90% near shores so it looks better
	scatter_factor*=0.1+0.9*_input.depthmap_scaler.g;

	// calculating fresnel factor 
	float r=(1.2-1.0)/(1.2+1.0);
	fresnel_factor = max(0.0,min(1.0,r+(1.0-r)*pow(1.0-dot(microbump_normal,pixel_to_eye_vector),4.0)));

	// calculating specular factor
	reflected_eye_to_pixel_vector=-pixel_to_eye_vector+2.0*dot(pixel_to_eye_vector,microbump_normal)*microbump_normal;
	specular_factor=shadow_factor*fresnel_factor*pow(max(0.0,dot(pixel_to_light_vector,reflected_eye_to_pixel_vector)),g_WaterSpecularPower);

	// calculating diffuse intensity of water surface itself
	diffuse_factor=g_WaterColorIntensity.x+g_WaterColorIntensity.y*max(0.0,dot(pixel_to_light_vector,microbump_normal));

	// calculating disturbance which has to be applied to planar reflections/refractions to give plausible results
//	disturbance_eyespace=mul(float4(microbump_normal.x,0,microbump_normal.z,0),g_ModelViewMatrix);
	disturbance_eyespace=g_ModelViewMatrix * vec4(microbump_normal.x,0,microbump_normal.z,0);
	disturbance_eyespace.xz *=-1.0;

    // TODO Problem occur here. g_ModelViewMatrix and Directx LookAt matrix.
	vec2 reflection_disturbance=vec2(disturbance_eyespace.x,disturbance_eyespace.z)*0.03;
	vec2 refraction_disturbance=vec2(-disturbance_eyespace.x,disturbance_eyespace.y)*0.05*
		// fading out reflection disturbance at distance so reflection doesn't look noisy at distance
		(20.0/(20.0+length(g_CameraPosition-_input.positionWS)));

//	vec2 refraction_disturbance = 1.0 - reflection_disturbance;	
	// calculating correction that shifts reflection up/down according to water wave Y position
//	float4 projected_waveheight = mul(float4(_input.positionWS.x,_input.positionWS.y,_input.positionWS.z,1),g_ModelViewProjectionMatrix);
	vec4 projected_waveheight = g_ModelViewProjectionMatrix * vec4(_input.positionWS,1);
	float waveheight_correction=-0.5*projected_waveheight.y/projected_waveheight.w;
//	projected_waveheight = mul(float4(_input.positionWS.x,-0.8,_input.positionWS.z,1),g_ModelViewProjectionMatrix);
	projected_waveheight = g_ModelViewProjectionMatrix * vec4(_input.positionWS.x,-0.8,_input.positionWS.z,1);
	waveheight_correction+=0.5*projected_waveheight.y/projected_waveheight.w;
	reflection_disturbance.y=max(-0.15,waveheight_correction+reflection_disturbance.y);

	vec2 screen_coord = gl_FragCoord.xy*g_ScreenSizeInv;
	// picking refraction depth at non-displaced point, need it to scale the refraction texture displacement amount according to water depth
	float refraction_depth=GetRefractionDepth(screen_coord);
	refraction_depth=g_ZFar*g_ZNear/(g_ZFar-refraction_depth*(g_ZFar-g_ZNear));
//	float4 vertex_in_viewspace=mul(float4(_input.positionWS,1),g_ModelViewMatrix);
	vec4 vertex_in_viewspace = g_ModelViewMatrix * vec4(_input.positionWS,1);
	vertex_in_viewspace.z *= -1.0;
	water_depth=refraction_depth-vertex_in_viewspace.z;
	float nondisplaced_water_depth=water_depth;
	
	// scaling refraction texture displacement amount according to water depth, with some limit
	refraction_disturbance*=min(2.0,water_depth);

	// picking refraction depth again, now at displaced point, need it to calculate correct water depth
	refraction_depth=GetRefractionDepth(screen_coord+refraction_disturbance);
	refraction_depth=g_ZFar*g_ZNear/(g_ZFar-refraction_depth*(g_ZFar-g_ZNear));
//	vertex_in_viewspace=mul(float4(_input.positionWS,1),g_ModelViewMatrix);
//	vertex_in_viewspace=g_ModelViewMatrix * vec4(_input.positionWS,1);
	water_depth=refraction_depth-vertex_in_viewspace.z;

	// zeroing displacement for points where displaced position points at geometry which is actually closer to the camera than the water surface
	float conservative_refraction_depth=GetConservativeRefractionDepth(screen_coord+refraction_disturbance);
	conservative_refraction_depth=g_ZFar*g_ZNear/(g_ZFar-conservative_refraction_depth*(g_ZFar-g_ZNear));
//	vertex_in_viewspace=mul(float4(_input.positionWS,1),g_ModelViewMatrix);
//	vertex_in_viewspace=g_ModelViewMatrix * vec4(_input.positionWS,1);
	float conservative_water_depth=conservative_refraction_depth-vertex_in_viewspace.z;

	if(conservative_water_depth<0.0)
	{
		refraction_disturbance=vec2(0);
		water_depth=nondisplaced_water_depth;
	}
	water_depth=max(0.0,water_depth);

	// getting reflection and refraction color at disturbed texture coordinates
//	reflection_color=g_ReflectionTexture.SampleLevel(SamplerLinearClamp,float2(_input.position.x*g_ScreenSizeInv.x,1.0-_input.position.y*g_ScreenSizeInv.y)+reflection_disturbance,0);
//	refraction_color=g_RefractionTexture.SampleLevel(SamplerLinearClamp,screen_coord+refraction_disturbance,0);
//	reflection_color=textureLod(g_ReflectionTexture, vec2(_input.position.x*g_ScreenSizeInv.x,1.0-_input.position.y*g_ScreenSizeInv.y)+reflection_disturbance,0.0);
	reflection_color=textureLod(g_ReflectionTexture, vec2(screen_coord.x, 1.0 - screen_coord.y)+reflection_disturbance,0.0);
	refraction_color=textureLod(g_RefractionTexture, screen_coord+refraction_disturbance,0.0);

	// calculating water surface color and applying atmospheric fog to it
	water_color=diffuse_factor*vec4(g_WaterDeepColor,1);
	water_color.rgb=mix(CalculateFogColor(pixel_to_light_vector,pixel_to_eye_vector).rgb,water_color.rgb,min(1.0,exp(-length(g_CameraPosition-_input.positionWS)*g_FogDensity)));
	
	// fading fresnel factor to 0 to soften water surface edges
	fresnel_factor*=min(1.0,water_depth*5.0);
//	fresnel_factor = clamp(fresnel_factor, 0.54, 0.55);

	// fading refraction color to water color according to distance that refracted ray travels in water 
	refraction_color=mix(water_color,refraction_color,min(1.0,1.0*exp(-water_depth/8.0)));
	
//	fresnel_factor = clamp(fresnel_factor);
	// combining final water color
	color.rgb=mix(refraction_color.rgb,reflection_color.rgb,fresnel_factor);
	color.rgb+=g_WaterSpecularIntensity*specular_factor*g_WaterSpecularColor*fresnel_factor;
	color.rgb+=g_WaterScatterColor*scatter_factor;
	color.a=1.0;
	
	return color;
}

subroutine(ColorOutput) vec4 ColorPS() { return vec4(1,1,1,1);}

void main()
{
	fragColor = pass();
}