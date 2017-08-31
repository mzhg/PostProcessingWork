#include "LightningCommon.glsl"

layout(location=0) in float3 In_Position;
layout(location=1) in float3 In_Normal;
layout(location=2) in float2 In_TexCoord;

out SceneVS2PS
{
//    float4 Position : SV_Position;
    float2 TexCoord /*: TexCoord0*/;
    float4 Color /*: Color*/;
}_output;

//void SceneVS( SceneApp2VS input, out SceneVS2PS output )
void main()
{
    float3 world_position = mul( float4(In_Position, 1.0f), world );
	float3 world_normal = normalize(mul(world, float4(In_Normal,0)).xyz);

	float3 light_position = float3(0, 20,0);
	float3 light_dir = normalize(light_position - world_position);

	gl_Position = mul( float4(In_Position, 1.0f), world_view_projection );
    _output.Color = 0.3f + saturate(dot(world_normal, light_dir));
    _output.TexCoord = In_TexCoord;

}