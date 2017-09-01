#include "Fire_Common.glsl"

layout(location=0) in vec3 In_Position;

// Vertex Shader for shadows
// Since we use this shader only for cumputing shadows, we dont need to compute
// its color and normal
void main()
{
    float4 ClipPos = float4( In_Position, 1.0f );

    // This can be done more efficiently by multiplying matrices in the app but I'm just lazy :)

    ClipPos = mul( ClipPos, CubeViewMatrices[CubeMapFace] );
    ClipPos = mul( ClipPos, CubeProjectionMatrix );

    gl_Position = ClipPos;
}