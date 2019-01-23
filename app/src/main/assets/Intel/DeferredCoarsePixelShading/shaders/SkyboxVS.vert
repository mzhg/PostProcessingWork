#include "GPUQuad.glsl"

layout(location = 0) in float3 In_Position;
out float3 skyboxCoord;

void main()
{
    // NOTE: Don't translate skybox and make sure depth == 1 (no clipping)
    gl_Position = mul(float4(In_Position, 0.0f), mCameraViewProj).xyww;
    skyboxCoord = In_Position;
}