in vec4 m_f4UVAndScreenPos;
out vec2 Out_Depth;

uniform float g_BufferWidthInv;
uniform float g_BufferHeightInv;

layout(binding = 0) uniform sampler2D s0;

void main()
{
    float sceneDepth = textureLod(s0, input.texCoord.xy );

    float4 clipPos;
    clipPos.x = 2.0 * input.texCoord.x - 1.0;
    clipPos.y = -2.0 * input.texCoord.y + 1.0;
    clipPos.z = sceneDepth;
    clipPos.w = 1.0;

    float4 positionWS = mul( clipPos, g_MWorldViewProjectionInv );
    positionWS.w = 1.0 / positionWS.w;
    positionWS.xyz *= positionWS.w;

    Out_Depth =  dot( positionWS.xyz - g_EyePosition, g_WorldFront );
}