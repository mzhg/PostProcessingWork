#include "globals.glsl"

in GS_OUTPUT
{
//  float4 position: SV_POSITION;
  float2 texCoords/*: TEXCOORD*/;
	float3 frustumRay/*: FRUSTUM_RAY*/;
}_input;

/*Texture2D colorMap: register(COLOR_TEX_BP); // albedoGloss
SamplerState colorMapSampler: register(COLOR_SAM_BP);
Texture2D normalMap: register(NORMAL_TEX_BP); // normalDepth
SamplerState normalMapSampler: register(NORMAL_SAM_BP);
Texture2D specularMap: register(SPECULAR_TEX_BP); // shadowMap
SamplerComparisonState specularMapSampler: register(SPECULAR_SAM_BP);*/

layout(binding = COLOR_TEX_BP) uniform sampler2D colorMap;  // albedoGloss
layout(binding = NORMAL_TEX_BP) uniform sampler2D normalDepth;  // albedoGloss
layout(binding = SPECULAR_TEX_BP) uniform sampler2DShadow specularMap;  // albedoGloss

//GLOBAL_CAMERA_UB(cameraUB);
//GLOBAL_DIR_LIGHT_UB(dirLightUB);


out float4 fragColor;

#define PCF_NUM_SAMPLES 16
#define SHADOW_FILTER_RADIUS 2.0f
#define SHADOW_BIAS 0.006f

// poisson disk samples
const float2 filterKernel[PCF_NUM_SAMPLES] = float2[PCF_NUM_SAMPLES]( float2(-0.94201624f,-0.39906216f),
                                              float2(0.94558609f,-0.76890725f),
                                              float2(-0.094184101f,-0.92938870f),
                                              float2(0.34495938f,0.29387760f),
                                              float2(-0.91588581f,0.45771432f),
                                              float2(-0.81544232f,-0.87912464f),
                                              float2(-0.38277543f,0.27676845f),
                                              float2(0.97484398f,0.75648379f),
                                              float2(0.44323325f,-0.97511554f),
                                              float2(0.53742981f,-0.47373420f),
                                              float2(-0.26496911f,-0.41893023f),
                                              float2(0.79197514f,0.19090188f),
                                              float2(-0.24188840f,0.99706507f),
                                              float2(-0.81409955f,0.91437590f),
                                              float2(0.19984126f,0.78641367f),
                                              float2(0.14383161f,-0.14100790f) );

// reconstruct world-space position from depth
float4 DecodePosition(in float depth,in float3 frustumRay)
{
  float4 position;
	float3 frustumRayN = normalize(frustumRay);
	position.xyz = cameraUB.position+(frustumRayN*depth);
	position.w = 1.0f;
	return position;
}

// compute shadow-term using 16x PCF in combination with hardware shadow filtering
float ComputeShadowTerm(in float4 positionWS)
{
  float4 result = mul(dirLightUB.shadowViewProjTexMatrix,positionWS);
  result.xyz /= result.w;
  float filterRadius = SHADOW_FILTER_RADIUS*dirLightUB.invShadowMapSize;
  float shadowTerm = 0.0f;
  for(int i=0;i<PCF_NUM_SAMPLES;i++)
  {
    float2 offset = filterKernel[i]*filterRadius;
    float2 texCoords = result.xy+offset;
//    texCoords.y = 1.0f-texCoords.y;
    shadowTerm += texture(specularMap,texCoords,result.z-SHADOW_BIAS);  // specularMapSampler
  }

  shadowTerm /= PCF_NUM_SAMPLES;
  return shadowTerm;
}

void main()
{
    float4 albedoGloss = texture(colorMap,_input.texCoords.xy);   // colorMapSampler
    float4 bumpDepth = texture(normalMap,_input.texCoords.xy);    // normalMapSampler

    float3 bump = bumpDepth.xyz;
    float4 position = DecodePosition(bumpDepth.w,_input.frustumRay);

    float3 lightVecN = -dirLightUB.direction;
    float3 viewVec = cameraUB.position-position.xyz;
    float3 viewVecN  = normalize(viewVec);

    float diffuse = max(dot(lightVecN,bump),0.0f);
    float4 vDiffuse = dirLightUB.color*diffuse;

    // Phong equation for specular lighting
    float shininess = 100.0f;
    float specular = pow(saturate(dot(reflect(-lightVecN,bump),viewVecN)),shininess);
    float4 vSpecular = float4(albedoGloss.aaa,1.0f)*specular;

    float shadowTerm = ComputeShadowTerm(position);
    fragColor = (vDiffuse*float4(albedoGloss.rgb,1.0f)+vSpecular)*dirLightUB.multiplier*shadowTerm;
}