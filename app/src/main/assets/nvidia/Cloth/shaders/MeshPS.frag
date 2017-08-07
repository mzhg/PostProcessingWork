#include "Cloth_Common.h"

in VS_OUT
{
//    float4 SV_Position : SV_Position;
    float3 Position /*: Position*/;
    float3 Normal /*: Normal*/;
    float3 TangentX /*: TangentX*/;
    float2 TexCoord /*: TexCoord*/;
}_input;

layout(location = 0) out vec4 Out_f4Color;

void main()
{
    float3 normal = normalize(_input.Normal);
    float3 diffuse, specular;
    Shade(_input.Position, normal, diffuse, specular);
    Out_f4Color = float4(Color * diffuse + specular, 1);
}

