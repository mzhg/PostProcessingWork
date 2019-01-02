#include "globals.glsl"

/*Texture2D colorMap: register(COLOR_TEX_BP); // albedoGloss
SamplerState colorMapSampler: register(COLOR_SAM_BP);
Texture2D normalMap: register(NORMAL_TEX_BP); // normalDepth
SamplerState normalMapSampler: register(NORMAL_SAM_BP);

GLOBAL_CAMERA_UB(cameraUB);
GLOBAL_POINT_LIGHT_UB(pointLightUB);*/
layout(binding = COLOR_TEX_BP) uniform sampler2D colorMap; // albedoGloss
layout(binding = NORMAL_TEX_BP) uniform sampler2D normalMap; // normalDepth

in VS_OUTPUT
{
//	float4 position: SV_POSITION;
	float4 screenPos/*: SCREEN_POS*/;
	float3 viewRay/*: VIEW_RAY*/;
}_input;

/*struct FS_OUTPUT
{
  float4 fragColor: SV_TARGET;
};*/

out float4 fragColor;

// reconstruct world-space position from depth
float4 DecodePosition(in float depth,in float3 viewRay)
{
    float4 position;
	float3 viewRayN = normalize(viewRay);
	position.xyz = cameraUB.position+(viewRayN*depth);
	position.w =  1.0f;
	return position;
}

//FS_OUTPUT main(VS_OUTPUT input)
void main()
{
	float2 sceneTC = _input.screenPos.xy/_input.screenPos.w;
	float4 albedoGloss = colorMap.Sample(colorMapSampler,sceneTC);
	float4 bumpDepth = normalMap.Sample(normalMapSampler,sceneTC);

	float3 bump = bumpDepth.xyz;
	float4 position = DecodePosition(bumpDepth.w,_input.viewRay);

	float3 lightVec = pointLightUB.position-position.xyz;
	float3 viewVec = cameraUB.position-position.xyz;
	float3 viewVecN  = normalize(viewVec);

	float lightVecLen = length(lightVec);
	float att = saturate(1.0f-(1.0f/pointLightUB.radius)*lightVecLen);
    float3 lightVecN = lightVec/lightVecLen;

	float diffuse = max(dot(lightVecN,bump),0.0f);
	float4 vDiffuse = pointLightUB.color*diffuse;

	// Phong equation for specular lighting
	float shininess = 100.0f;
	float specular = pow(saturate(dot(reflect(-lightVecN,bump),viewVecN)),shininess);
	float4 vSpecular = float4(albedoGloss.aaa,1.0f)*specular;

	fragColor = (vDiffuse*float4(albedoGloss.rgb,1.0f)+vSpecular)*pointLightUB.multiplier*att;
}
