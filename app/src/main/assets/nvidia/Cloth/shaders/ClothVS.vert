#include "Cloth.h"

layout(location = 0) in uint In_State;
layout(location = 1) in vec3 In_Position;
layout(location = 2) in vec3 In_Normal;
layout(location = 3) in vec3 In_TangentX;
layout(location = 4) in vec2 In_Texcoord;

struct ClothPixel
{
    float4 SV_Position /*: SV_Position*/;
    float3 Position /*: Position2*/;
    float3 Normal /*: Normal*/;
    float3 TangentX /*: TangentX*/;
    float3 TangentY /*: TangentY*/;
    TexCoord TexCoord /*: TexCoord*/;
};

out ParticleVertex
{
    Particle ParticleData;
    ClothPixel PixelData;
}_output;

void main()
{
    _output.ParticleData = particle;
    SV_Position = mul(float4(particle.Position, 1), ViewProjection);
    _output.PixelData.Position = particle.Position;
    _output.PixelData.Normal = normalTangent.Normal;
    _output.PixelData.TangentX = normalTangent.TangentX;
    _output.PixelData.TangentY = cross(vertex.PixelData.Normal, vertex.PixelData.TangentX);
    _output.PixelData.TexCoord = texCoord;
}