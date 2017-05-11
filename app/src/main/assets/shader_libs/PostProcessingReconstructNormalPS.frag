#include "PostProcessingCommonPS.frag"


// uniform mat4 g_ProjMat;
// uniform mat4 g_InvMat;

uniform mat4 g_Uniforms[2];
#define g_ProjMat g_Uniforms[0]
#define g_InvMat  g_Uniforms[1]

uniform sampler2D g_LinearDepthTex;

vec3 UVToView(vec2 uv, float eye_z)
{
//  return vec3((uv * projInfo.xy + projInfo.zw) * (projOrtho != 0 ? 1. : eye_z), eye_z);
    float fDepth = -g_ProjMat[2][2]  + g_ProjMat[3][2]/ eye_z;  // f3PosPS.z = -z
    float4 projPosition = float4(uv.xy, fDepth, 1) * f3PosPS.z;
    float4 ReconstructedPosWS = g_InvMat * projPosition;
    ReconstructedPosWS /= ReconstructedPosWS.w;
    return ReconstructedPosWS.xyz;
}

vec3 FetchViewPos(vec2 UV)
{
  float ViewDepth = textureLod(g_LinearDepthTex,UV,0.0).x;
  return UVToView(UV, ViewDepth);
}

vec3 MinDiff(vec3 P, vec3 Pr, vec3 Pl)
{
  vec3 V1 = Pr - P;
  vec3 V2 = P - Pl;
  return (dot(V1,V1) < dot(V2,V2)) ? V1 : V2;
}

vec3 ReconstructNormal(vec2 UV, vec3 P)
{
  vec3 Pr = FetchViewPos(UV + vec2(InvFullResolution.x, 0));
  vec3 Pl = FetchViewPos(UV + vec2(-InvFullResolution.x, 0));
  vec3 Pt = FetchViewPos(UV + vec2(0, InvFullResolution.y));
  vec3 Pb = FetchViewPos(UV + vec2(0, -InvFullResolution.y));
  return normalize(cross(MinDiff(P, Pr, Pl), MinDiff(P, Pt, Pb)));
}

//----------------------------------------------------------------------------------

void main() {
  vec3 Pos  = FetchViewPos(m_f4UVAndScreenPos.xy);
  vec3 Normal  = ReconstructNormal(m_f4UVAndScreenPos.xy, Pos);

  Out_f4Color.xyz = vec4(Normal*0.5 + 0.5,0);
  Out_f4Color.a = 0.0;
}