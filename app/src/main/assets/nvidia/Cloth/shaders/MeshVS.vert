#include "Cloth.h"

layout(location = 0) in float3 Position /*: Position*/;
layout(location = 1) in float3 Normal /*: Normal*/;
layout(location = 2) in float3 TangentX/* : TangentX*/;
layout(location = 3) in float2 TexCoord /*: TexCoord*/;

out VS_OUT
{
//    float4 SV_Position : SV_Position;
    float3 Position /*: Position*/;
    float3 Normal /*: Normal*/;
    float3 TangentX /*: TangentX*/;
    float2 TexCoord /*: TexCoord*/;
}_output;

void main()
{
    gl_Position = mul(float4(Position, 1), WorldViewProjection);
    _output.Position = mul(float4(Position, 1), World);
    _output.Normal = normalize(mul(Normal, float3x3(WorldIT)));
    _output.TangentX = normalize(mul(TangentX, float3x3(WorldIT)));
    _output.TexCoord = TexCoord;
}