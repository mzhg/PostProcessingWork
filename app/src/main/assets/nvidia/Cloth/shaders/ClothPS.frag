#include "Cloth.h"

struct ClothPixel
{
    float4 SV_Position /*: SV_Position*/;
    float3 Position /*: Position2*/;
    float3 Normal /*: Normal*/;
    float3 TangentX /*: TangentX*/;
    float3 TangentY /*: TangentY*/;
    TexCoord TexCoord /*: TexCoord*/;
};

in GS_OUT
{
    ClothPixel PixelData;
}_intput;

uniform float4x4 World;
uniform float4x4 WorldIT;
uniform float4x4 ViewProjection;
uniform float4x4 WorldViewProjection;
uniform float4x4 Transform;
uniform Texture2D DiffuseTexture;
uniform Texture2D NormalMap;

/*
SamplerState Linear
{
    Filter = MIN_MAG_MIP_LINEAR;
    AddressU = Wrap;
    AddressV = Wrap;
};*/

layout(location = 0) out vec4 Out_f4Color;

// Light positions in world space
float3 LightPosition[] = float3[3](
    float3(0, 0, -5),
    float3(-5, 2, 4),
    float3(5, 2, 2)
);
#define NumLights 3

// Material properties
float3 Color;
float DiffuseCoeff;
float SpecularCoeff;
float SpecularPower;

// Apply lighting
void Shade(float3 position, float3 normal, out float3 diffuse, out float3 specular)
{
    diffuse = 0;
    specular = 0;
    float3 V = normalize(Eye - position);
    for (int i = 0; i < NumLights; ++i) {
        float3 L = LightPosition[i] - position;
        float intensity = 1 / (1 + 0.01 * dot(L, L));
        L = normalize(L);
        diffuse += intensity * max(0, dot(normal, L));
        float3 H = normalize(L + V);
        specular += intensity * pow(max(0, dot(normal, H)), SpecularPower);
    }
    diffuse *= DiffuseCoeff;
    specular *= SpecularCoeff;
}

// Compute bump normal
float3 ComputeBumpNormal(inout float3 normal, float3 tangentX, float2 texCoord)
{
    normal = normalize(normal);
    tangentX = normalize(tangentX);
    float3 tangentY = cross(normal, tangentX);
    float3 bumpNormal = normalize(NormalMap.Sample(Linear, texCoord) - 0.5);
    return normalize(bumpNormal.x * tangentX + bumpNormal.y * tangentY + bumpNormal.z * normal);
}

void main()
{
    float3 normal = ComputeBumpNormal(_intput.PixelData.Normal, _intput.PixelData.TangentX, _intput.PixelData.TexCoord);
    float3 diffuse, specular;
    Shade(pixel.Position, normal, diffuse, specular);
    Out_f4Color = float4(texture(DiffuseTexture, _intput.PixelData.TexCoord) * Color * diffuse + specular, 1);  // Linear
}