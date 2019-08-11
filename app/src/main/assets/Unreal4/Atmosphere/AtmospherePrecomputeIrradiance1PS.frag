#include "AtmosphereCommon.ush"
#include "AtmospherePrecomputeCommon.ush"

in vec4 m_f4UVAndScreenPos;

out float OutColor;

void main()
{
    // RETURN_COLOR not needed unless writing to SceneColor
    float Radius, MuS;
    GetIrradianceRMuS(Input.OutTexCoord, Radius, MuS);
    OutColor = float4(Transmittance(Radius, MuS) * max(MuS, 0.0), 0.0);
}