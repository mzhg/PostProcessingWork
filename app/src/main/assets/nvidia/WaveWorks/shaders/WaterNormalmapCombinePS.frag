#version 400

uniform sampler2D g_WaterBumpTexture;

const vec2 g_WaterBumpTexcoordScale = vec2(7, 7);
const float g_HeightFieldSize = 512.0;


uniform vec2 g_WaterBumpTexcoordShift;
uniform vec3 g_CameraPosition;

in vec2 v_texcoords;
// layout (location = 0) out vec4 color;

// constructing water surface normal for water refraction caustics
vec3 CombineSimplifiedWaterNormal(vec3 world_position, float mip_level)
{
	vec3 water_normal=vec3(0.0,4.0,0.0);

	float water_miplevel;
	float distance_to_camera;
	vec4 texvalue;
	float texcoord_scale=1.0;
	float normal_disturbance_scale=1.0;
	vec2 tc;
	vec2 variance=vec2(1.0,1.0);

	tc=(world_position.xz*g_WaterBumpTexcoordScale/g_HeightFieldSize);
	
	// need more high frequensy details for caustics, so summing more "octaves"
	for(float i=0;i<8;i++)
	{
		//texvalue=g_WaterBumpTexture.SampleLevel(SamplerLinearWrap, tc*texcoord_scale+g_WaterBumpTexcoordShift*0.03*variance,mip_level/*+i*/).rbga;
		texvalue = textureLod(g_WaterBumpTexture, tc*texcoord_scale+g_WaterBumpTexcoordShift*0.03*variance, mip_level/*+i*/).rbga;
		variance.x*=-1.0;
		water_normal.xz+=(2.0*texvalue.xz-vec2(1,1))*normal_disturbance_scale;
		texcoord_scale*=1.4;
		normal_disturbance_scale*=0.85;
	}
	return normalize(water_normal);
}

void main()
{
	vec4 color;
	color.rgb = (CombineSimplifiedWaterNormal(g_CameraPosition+vec3(v_texcoords.x*400.0f-200.0,0,v_texcoords.y*400.0f-200.0),0).rgb+vec3(1.0f,1.0f,1.0f))*0.5f;
	color.a=0;
	
	gl_FragColor = color;
}