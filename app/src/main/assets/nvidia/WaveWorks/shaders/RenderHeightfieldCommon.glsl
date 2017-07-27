

const float g_HeightFieldSize = 512.0;
const vec2	g_RockBumpTexcoordScale=vec2(10.0,10.0);
const float g_RockBumpHeightScale=3.0;
const float g_WaterHeightBumpScale=1.0;
const float g_FogDensity             =1.0f/700.0f;
const vec3 g_AtmosphereDarkColor     =vec3(0.6,0.6,0.7);
const vec3 g_AtmosphereBrightColor   =vec3(1.0,1.1,1.4);
const vec2 g_WaterBumpTexcoordScale = vec2(7,7);
const float g_WaterSpecularPower = 1000.0;
const vec2 g_WaterColorIntensity= vec2(0.1,0.2);
const float g_WaterSpecularIntensity=350.0;
const vec3  g_WaterSpecularColor=vec3(1,1,1);
const vec3  g_WaterScatterColor=vec3(0.3,0.7,0.6);
const vec2  g_SandBumpTexcoordScale= vec2(3.5,3.5);
const float g_SandBumpHeightScale=0.5;
const vec2	g_DiffuseTexcoordScale= vec2(130.0,130.0);
const vec2	g_WaterMicroBumpTexcoordScale=vec2(225,225);
const vec3  g_WaterDeepColor=vec3(0.1,0.4,0.7);

//uniform Uniforms{
 uniform mat4 g_ModelViewProjectionMatrix;
 uniform mat4 g_LightModelViewProjectionMatrix;
 uniform mat4 g_ModelViewMatrix;
 uniform mat4 g_DepthModelViewProjectionMatrix;
 uniform vec3 g_CameraPosition;
 uniform int g_FrustumCullInHS;
 uniform vec3 g_CameraDirection;
 uniform float g_TerrainBeingRendered;
 
 uniform vec2 g_WaterBumpTexcoordShift;
 uniform float g_StaticTessFactor;
 uniform float g_DynamicTessFactor;
 uniform float g_UseDynamicLOD;
 uniform int g_SkipCausticsCalculation;
 uniform float g_RenderCaustics;
 uniform float		g_HalfSpaceCullSign;
 uniform float		g_HalfSpaceCullPosition;
 
 uniform vec3      g_LightPosition;
 uniform vec2      g_ScreenSizeInv;
 uniform float	   g_ZNear;
 uniform float	   g_ZFar;
//};

// uniform sampler2D g_HeightfieldTexture;


//uniform vec3 g_CameraPosition;
//uniform vec3 g_LightPosition;
//uniform mat4 g_ModelViewProjectionMatrix;

uniform sampler2D g_HeightfieldTexture;     //unit 0
uniform sampler2D g_LayerdefTexture;        //unit 1
uniform sampler2D g_SandBumpTexture;        //unit 2
uniform sampler2D g_RockBumpTexture;        //unit 3
uniform sampler2D g_WaterNormalMapTexture;  //unit 4
uniform sampler2D g_SandMicroBumpTexture;   //unit 5
uniform sampler2D g_RockMicroBumpTexture;   //unit 6

// RenderHeightFieldPS
uniform sampler2D g_SlopeDiffuseTexture;    //unit 7
uniform sampler2D g_SandDiffuseTexture;     //unit 8
uniform sampler2D g_RockDiffuseTexture;     //unit 9
uniform sampler2D g_GrassDiffuseTexture;    //unit 10
uniform sampler2DShadow g_DepthTexture;     //unit 11
uniform sampler2D g_WaterBumpTexture;       //unit 12
uniform sampler2D g_RefractionDepthTextureResolved;   //unit 13
uniform sampler2D g_ReflectionTexture;      //unit 14
uniform sampler2D g_RefractionTexture;      //unit 15

// WaterPatchDS

// calculating tessellation factor. It is either constant or hyperbolic depending on g_UseDynamicLOD switch
float CalculateTessellationFactor(float distance)
{
	return mix(g_StaticTessFactor,g_DynamicTessFactor*(1.0/(0.015*distance)),g_UseDynamicLOD);
}

// to avoid vertex swimming while tessellation varies, one can use mipmapping for displacement maps
// it's not always the best choice, but it effificiently suppresses high frequencies at zero cost
float CalculateMIPLevelForDisplacementTextures(float distance)
{
	return log2(128.0/CalculateTessellationFactor(distance));
}

vec3 TransformNormal(vec3 base_normal, vec3 local_normal)
{
	const vec3 x_axis = vec3(1,0,0);
	mat3 transform;
	transform[1] = base_normal;  //Y-Axis
	transform[2] = normalize(cross(x_axis, base_normal));  // Z-Axis
	transform[0] = normalize(cross(base_normal, transform[2])); // X-Axis
//	transform = transpose(transform);
	
	return transform * local_normal;
}

//---------------------------------------------------------------------------------------
// Transforms a normal map sample to world space.
//---------------------------------------------------------------------------------------
vec3 NormalSampleToWorldSpace(vec3 normalMapSample, vec3 unitNormalW, vec3 tangentW)
{
	// Uncompress each component from [0,1] to [-1,1].
	//vec3 normalT = 2.0 * normalMapSample - 1.0;

	// Build orthonormal basis.
	vec3 N = unitNormalW;
	vec3 T = normalize(cross(tangentW, N));
	vec3 B = normalize(cross(N, T));

	mat3 TBN = mat3(T, B, N);
//	TBN = transpose(TBN); // Need this ?

	// Transform from tangent space to world space.
	vec3 bumpedNormalW = TBN * normalMapSample;

	return bumpedNormalW;
}

// constructing the displacement amount and normal for water surface geometry
vec4 CombineWaterNormal(vec3 world_position)
{
	vec4 water_normal=vec4(0.0,4.0,0.0,0.0);
	float water_miplevel;
	float distance_to_camera;
	vec4 texvalue;
	float texcoord_scale=1.0;
	float height_disturbance_scale=1.0;
	float normal_disturbance_scale=1.0;
	vec2 tc;
	vec2 variance=vec2(1.0,1.0);

	// calculating MIP level for water texture fetches
	distance_to_camera=length(g_CameraPosition-world_position);
	water_miplevel= CalculateMIPLevelForDisplacementTextures(distance_to_camera)/2.0-2.0;
	tc=(world_position.xz*g_WaterBumpTexcoordScale/g_HeightFieldSize);

	// fetching water heightmap
	for(int i=0;i<5;i++)
	{
//		texvalue=g_WaterBumpTexture.SampleLevel(SamplerLinearWrap, tc*texcoord_scale+g_WaterBumpTexcoordShift*0.03*variance,water_miplevel).rbga;
		texvalue=textureLod(g_WaterBumpTexture, tc*texcoord_scale+g_WaterBumpTexcoordShift*0.03*variance,water_miplevel).rbga;
		variance.x*=-1.0;
		water_normal.xz+=(2*texvalue.xz-vec2(1.0,1.0))*normal_disturbance_scale;
		water_normal.w += (texvalue.w-0.5)*height_disturbance_scale;
		texcoord_scale*=1.4;
		height_disturbance_scale*=0.65;
		normal_disturbance_scale*=0.65;
	}
	
	water_normal.w*=g_WaterHeightBumpScale;
	return vec4(normalize(water_normal.xyz),water_normal.w);
}

// primitive simulation of non-uniform atmospheric fog
vec3 CalculateFogColor(vec3 pixel_to_light_vector, vec3 pixel_to_eye_vector)
{
	return mix(g_AtmosphereDarkColor,g_AtmosphereBrightColor,0.5*dot(pixel_to_light_vector,-pixel_to_eye_vector)+0.5);
}

float sampleCmp(vec2 texcoord, float compareValue)
{
	/*
	vec4 texel = texture(g_DepthTexture, texcoord);
	if(texel.z < compareValue)
	{
		return texel.r;
	}
	else
	{
		return 0.0;
	}*/
	
	return texture(g_DepthTexture, vec3(texcoord, compareValue));
}

// calculating water refraction caustics intensity
float CalculateWaterCausticIntensity(vec3 worldpos)
{

	float distance_to_camera=length(g_CameraPosition-worldpos);

	vec2 refraction_disturbance;
	vec3 n;
	float m=0.2;
	float cc=0;
	float k=0.15;
	float water_depth=0.5-worldpos.y;

	vec3 pixel_to_light_vector=normalize(g_LightPosition-worldpos);

	worldpos.xz-=worldpos.y*pixel_to_light_vector.xz;
	vec3 pixel_to_water_surface_vector=pixel_to_light_vector*water_depth;
	vec3 refracted_pixel_to_light_vector;

	// tracing approximately refracted rays back to light
	for(float i=-3; i<=3;i+=1)
		for(float j=-3; j<=3;j+=1)
		{
			vec3 texel = textureLod(g_WaterNormalMapTexture, (worldpos.xz-g_CameraPosition.xz-vec2(200.0,200.0)+vec2(i*k,j*k)*m*water_depth)/400.0, 0.0).rgb;
//			n=2.0f*g_WaterNormalMapTexture.SampleLevel(SamplerLinearWrap,(worldpos.xz-g_CameraPosition.xz-vec2(200.0,200.0)+vec2(i*k,j*k)*m*water_depth)/400.0,0).rgb-vec3(1.0f,1.0f,1.0f);
			n = 2.0f * texel - 1.0;
			refracted_pixel_to_light_vector=m*(pixel_to_water_surface_vector+vec3(i*k,0,j*k))-0.5*vec3(n.x,0,n.z);
			cc+=0.05*max(0.0,pow(max(0.0,dot(normalize(refracted_pixel_to_light_vector),normalize(pixel_to_light_vector))),500.0f));
		}
	return cc;
}

float GetRefractionDepth(vec2 position)
{
//	return g_RefractionDepthTextureResolved.SampleLevel(SamplerLinearClamp,position,0).r;
	float depth = textureLod(g_RefractionDepthTextureResolved, position, 0.0).r;
//	return pow(depth, 1.0 - depth);
	return depth;
}

float GetConservativeRefractionDepth(vec2 position)
{
//	float result =      g_RefractionDepthTextureResolved.SampleLevel(SamplerPointClamp,position + 2.0*float2(g_ScreenSizeInv.x,g_ScreenSizeInv.y),0).r;
//	result = min(result,g_RefractionDepthTextureResolved.SampleLevel(SamplerPointClamp,position + 2.0*float2(g_ScreenSizeInv.x,-g_ScreenSizeInv.y),0).r);
//	result = min(result,g_RefractionDepthTextureResolved.SampleLevel(SamplerPointClamp,position + 2.0*float2(-g_ScreenSizeInv.x,g_ScreenSizeInv.y),0).r);
//	result = min(result,g_RefractionDepthTextureResolved.SampleLevel(SamplerPointClamp,position + 2.0*float2(-g_ScreenSizeInv.x,-g_ScreenSizeInv.y),0).r);
	
	float result = textureLod(g_RefractionDepthTextureResolved, position + 2.0*vec2(g_ScreenSizeInv.x,g_ScreenSizeInv.y),0.0).r;
	result = min(result, textureLod(g_RefractionDepthTextureResolved, position + 2.0*vec2(g_ScreenSizeInv.x,-g_ScreenSizeInv.y),0.0).r);
	result = min(result, textureLod(g_RefractionDepthTextureResolved, position + 2.0*vec2(-g_ScreenSizeInv.x,g_ScreenSizeInv.y),0.0).r);
	result = min(result, textureLod(g_RefractionDepthTextureResolved, position + 2.0*vec2(-g_ScreenSizeInv.x,-g_ScreenSizeInv.y),0.0).r);
	return result;
}