#include "CommonHeader.glsl"
#include "LightingCommonHeader.glsl"
#include "Transparency.glsl"

layout(location = 0) in float3 In_Position;

out gl_PerVertex
{
    float4 gl_Position;
};

//--------------------------------------------------------------------------------------
// This shader just transforms position (e.g. to write depth to the depth buffer)
//--------------------------------------------------------------------------------------
//VS_OUTPUT_ALPHA_BLENDED_DEPTH RenderBlendedDepthVS( VS_INPUT_ALPHA_BLENDED Input, uint InstanceID : SV_InstanceID )
void main()
{
    mat4 mWorld = g_InstanceTransform[gl_InstanceID];

    // Transform the position from object space to homogeneous projection space
    float4 vWorldPos = mul( float4(In_Position,1), mWorld );
    gl_Position = mul( vWorldPos, g_mViewProjection );
}