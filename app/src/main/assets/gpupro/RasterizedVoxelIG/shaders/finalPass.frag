#include "globals.glsl"

//Texture2D colorMap: register(COLOR_TEX_BP);
//SamplerState colorMapSampler: register(COLOR_SAM_BP);

layout(binding = COLOR_TEX_BP) uniform sampler2D colorMap;

in GS_OUTPUT
{
//  float4 position: SV_POSITION;
  float2 texCoords/*: TEXCOORD*/;
}_input;

/*struct FS_OUTPUT
{
  float4 fragColor: SV_TARGET;
};*/

out float4 fragColor;

//FS_OUTPUT main(GS_OUTPUT input)
void main()
{
//  FS_OUTPUT output;
	float4 color = texture(colorMap,_input.texCoords);  // colorMapSampler
	fragColor = color;
//	return output;
}