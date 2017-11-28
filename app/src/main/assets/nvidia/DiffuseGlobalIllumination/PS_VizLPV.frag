#include "SimpleShading.glsl"

layout(location = 0) out float4 Color    /*: SV_Target0*/;

void main()
{
    int row, col;
    bool outside;

    float4 color = texture(g_LPVTex, float3(LPVSpacePos.x,LPVSpacePos.y,LPVSpacePos.z*LPV3DDepth1) + float3(0.5f/LPV3DWidth1,0.5f/LPV3DHeight1, 0.5f));  // samLinear

    clip( abs(color.r) + abs(color.g) + abs(color.b) + abs(color.a) - 0.001); //kill visualizing this point if there is no radiance here
    Color = float4(color.r,color.g,color.b,1);
}