#include "LightningCommon.glsl"

layout(location=0) out float4 Out_Color;

in SceneVS2PS
{
//    float4 Position : SV_Position;
    float2 TexCoord /*: TexCoord0*/;
    float4 Color /*: Color*/;
}_input;

layout(binding = 0) uniform sampler2D tex_diffuse;

//void ScenePS(SceneVS2PS input, out float4 output : SV_Target)
void main()
{
	Out_Color = texture(tex_diffuse,_input.TexCoord) * _input.Color;   // LinearSample
}