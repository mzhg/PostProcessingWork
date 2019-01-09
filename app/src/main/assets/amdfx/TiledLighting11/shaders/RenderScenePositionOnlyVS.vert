#include "Forward.glsl"

layout(location = 0) in float3 In_Position;
layout(location = 1) in float2 In_Texcoord;
layout(location = 2) in float3 In_Normal;
layout(location = 3) in float3 In_Tangent;

//--------------------------------------------------------------------------------------
// This shader just transforms position (e.g. for depth pre-pass)
//--------------------------------------------------------------------------------------
//VS_OUTPUT_POSITION_ONLY RenderScenePositionOnlyVS( VS_INPUT_SCENE Input )
void main()
{

    // Transform the position from object space to homogeneous projection space
    float4 vWorldPos = mul( float4(Input.Position,1), g_mWorld );
    gl_Position = mul( vWorldPos, g_mViewProjection );
}
