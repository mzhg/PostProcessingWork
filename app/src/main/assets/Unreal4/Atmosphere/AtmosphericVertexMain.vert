#include "AtmosphereCommon.ush"
#include "AtmospherePrecomputeCommon.ush"

layout(location = 0) in float2 InPosition;

out float2 OutTexCoord;
out float4 OutScreenVector;

void main()
{
    gl_Position = float4(InPosition, 0, 1);
    OutTexCoord = InPosition * 0.5 + 0.5;

    // deproject to world space
    OutScreenVector = mul(float4(InPosition,1,0), View.ScreenToTranslatedWorld);
}