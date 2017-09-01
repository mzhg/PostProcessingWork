#include "LightningCommon.glsl"

in BoltOutVertexGS2PS
{
//	float4	Position : SV_Position;
	float2	Gradient /*: Gradient*/;
	flat uint		Level	/*: Level*/;
}_input;

layout(location=0) out vec4 Out_Color;

void main()
{
    float f = saturate(length(_input.Gradient));
	float brightness = 1.0-f;
	float color_shift = saturate(pow(1.-f, ColorFallOffExponent));
	Out_Color = brightness * float4(lerp(ColorOutside,ColorInside,color_shift), 1.);
}