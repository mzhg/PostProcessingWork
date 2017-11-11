#include "SceneRenderCommon.glsl"
/*
float3 f3Position   : POSITION;
  float3 f3Normal     : NORMAL;
  float2 f2TexCoord   : TEXCOORD;
  float3 f3Tangent   : TANGENT;
  */
  
  layout(location =0) in float3 In_f3Position;
  layout(location =1) in float3 In_f3Normal;
  layout(location =2) in float2 In_f2TexCoord;
  layout(location =3) in float3 In_f3Tangent;
  
  out float3 m_f3PositionWS; //   : WS_POSITION;
  out float3 m_f3Normal; //       : NORMAL;
  out float2 m_f2TexCoord; //     : TEXCOORD0;
  
  out gl_PerVertex
{
	vec4 gl_Position;
};
  
  void main()
  {
  	  // Transform the position from object space to homogeneous projection space
  	  gl_Position = mul( float4( In_f3Position, 1.0f ), g_Model.m_WorldViewProjection );
	  m_f3PositionWS = mul( float4( In_f3Position, 1.0f ), g_Model.m_World ).xyz;
	
	  // Transform the normal from object space to world space
	  m_f3Normal = normalize( mul( In_f3Normal, float3x3(g_Model.m_World) ) );
	
	  // Pass through texture coords
	  m_f2TexCoord = In_f2TexCoord;
  }