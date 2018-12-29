
in VS_OUTPUT
{
//	float4 position: SV_POSITION;
    float2 texCoords/*: TEXCOORD*/;
	float3 posVS/*: POS_VS*/;
	float3 normal/*: NORMAL*/;
	float3 tangent/*: TANGENT*/;
	float3 binormal/*: BINORMAL*/;
}_input;

/*Texture2D colorMap: register(COLOR_TEX_BP);
SamplerState colorMapSampler: register(COLOR_SAM_BP);
Texture2D normalMap: register(NORMAL_TEX_BP);
SamplerState normalMapSampler: register(NORMAL_SAM_BP);
Texture2D specularMap: register(SPECULAR_TEX_BP);
SamplerState specularMapSampler: register(SPECULAR_SAM_BP);*/
layout(binding = COLOR_TEX_BP) uniform sampler2D colorMap;
layout(binding = NORMAL_TEX_BP) uniform sampler2D normalMap;
layout(binding = SPECULAR_TEX_BP) uniform sampler2D specularMap;

/*struct VS_OUTPUT
{
	float4 position: SV_POSITION;
  float2 texCoords: TEXCOORD;
	float3 posVS: POS_VS;
	float3 normal: NORMAL;
	float3 tangent: TANGENT;
	float3 binormal: BINORMAL;
};

struct FS_OUTPUT
{
  float4 fragColor0: SV_TARGET0;
	float4 fragColor1: SV_TARGET1;
};*/

layout(location = 0) out float4 fragColor0;
layout(location = 1) out float4 fragColor1;

#define ALPHA_THRESHOLD 0.3f

//FS_OUTPUT main(VS_OUTPUT input)
void main()
{
	float4 albedo = texture(colorMap,_input.texCoords);  // colorMapSampler

#ifdef ALPHA_TEST
	if(albedo.a<ALPHA_THRESHOLD)
		discard;
#endif

    float3x3 tangentMatrix;
	tangentMatrix[0] = normalize(_input.tangent);
	tangentMatrix[1] = normalize(_input.binormal);
    tangentMatrix[2] = normalize(_input.normal);

	float3 bump = texture(normalMap,_input.texCoords).xyz*2.0f-1.0f;  // normalMapSampler
	bump = mul(bump,tangentMatrix);
	bump = normalize(bump);
	float gloss = texture(specularMap,_input.texCoords).r;   // specularMapSampler

	fragColor0 = float4(albedo.rgb,gloss);
	fragColor1 = float4(bump,length(_input.posVS));
}
