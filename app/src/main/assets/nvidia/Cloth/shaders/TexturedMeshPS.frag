#include "Cloth_Common.h"

in VS_OUT
{
//    float4 SV_Position : SV_Position;
    float3 Position /*: Position*/;
    float3 Normal /*: Normal*/;
    float3 TangentX /*: TangentX*/;
    float2 TexCoord /*: TexCoord*/;
}pixel;

layout(location = 0) out vec4 Out_f4Color;

void main()
{
    float3 normal = ComputeBumpNormal(pixel.Normal, pixel.TangentX, pixel.TexCoord);
    float3 diffuse, specular;
    Shade(pixel.Position, normal, diffuse, specular);
    Out_f4Color = float4(DiffuseTexture.Sample(Linear, pixel.TexCoord) * Color * diffuse + specular, 1);
}