#include "AtmosphereCommon.ush"
#include "AtmospherePrecomputeCommon.ush"

in vec4 m_f4UVAndScreenPos;

out float OutColor;

void main()
{
    OutColor = Texture2DSample(AtmosphereDeltaETexture, /*AtmosphereDeltaETextureSampler,*/ m_f4UVAndScreenPos.xy);
}