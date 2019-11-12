#include "ocean_surface.glsl"

in VS_OUTPUT
{
    float4								worldspace_position	/*: VSO */;
    float								hull_proximity /*: HULL_PROX*/;
}I[];

out VS_OUTPUT
{
    float4								worldspace_position	/*: VSO */;
    float								hull_proximity /*: HULL_PROX*/;
}Outputs[];

patch out HS_ConstantOutput
{
    float fTargetEdgeLength[3] /*: TargetEdgeLength*/;
    float fHullProxMult[3] /*: HullProxMult*/;
}O;

layout (vertices = 3) out;

float CalcTessFactorMultFromProximity(float proximity)
{
    const float max_proximity = 0.5f;
    const float max_mult = 4.f;
    return 1.f + (max_mult-1.f) * saturate(proximity/max_proximity);
}

void main()
{
    Outputs[gl_InvocationID].worldspace_position = I[gl_InvocationID].worldspace_position;
    Outputs[gl_InvocationID].hull_proximity      = I[gl_InvocationID].hull_proximity;

    O.fHullProxMult[0] = CalcTessFactorMultFromProximity(max(I[1].hull_proximity,I[2].hull_proximity));
    O.fHullProxMult[1] = CalcTessFactorMultFromProximity(max(I[2].hull_proximity,I[0].hull_proximity));
    O.fHullProxMult[2] = CalcTessFactorMultFromProximity(max(I[0].hull_proximity,I[1].hull_proximity));

    gl_TessLevelOuter[0] = GFSDK_WaveWorks_GetEdgeTessellationFactor(I[1].worldspace_position,I[2].worldspace_position) * O.fHullProxMult[0];
    gl_TessLevelOuter[1] = GFSDK_WaveWorks_GetEdgeTessellationFactor(I[2].worldspace_position,I[0].worldspace_position) * O.fHullProxMult[1];
    gl_TessLevelOuter[2] = GFSDK_WaveWorks_GetEdgeTessellationFactor(I[0].worldspace_position,I[1].worldspace_position) * O.fHullProxMult[2];

    gl_TessLevelInner[0]= (gl_TessLevelOuter[0] + gl_TessLevelOuter[1] + gl_TessLevelOuter[2])/3.0f;

    O.fTargetEdgeLength[0] = GFSDK_WaveWorks_GetVertexTargetTessellatedEdgeLength(I[0].worldspace_position.xyz);
    O.fTargetEdgeLength[1] = GFSDK_WaveWorks_GetVertexTargetTessellatedEdgeLength(I[1].worldspace_position.xyz);
    O.fTargetEdgeLength[2] = GFSDK_WaveWorks_GetVertexTargetTessellatedEdgeLength(I[2].worldspace_position.xyz);
}