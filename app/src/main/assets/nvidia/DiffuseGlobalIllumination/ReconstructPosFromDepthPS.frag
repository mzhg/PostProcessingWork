in vec2 m_Tex;
out vec4 Out_Color;

layout(binding = 0) uniform sampler2D g_textureToDisplay2D;
uniform mat4 g_InverseProjection;

void main()
{
//float4 col = saturate(g_textureToDisplay3D.Sample( samLinear, float3(f.Tex.xyz)) );
    vec2 inputPos = /*vec2( f.Tex.x*2.0f-1, (1.0f-f.Tex.y)*2.0f-1)*/ m_Tex * 2.0 - 1.0;
    float depthSample = texture( g_textureToDisplay2D, m_Tex.xy ).x;

    vec4 vProjectedPos = vec4(inputPos.x, inputPos.y, 2.0 * depthSample - 1.0, 1.0f);
    vec4 viewSpacePos = g_InverseProjection * vProjectedPos;
    viewSpacePos.xyz = viewSpacePos.xyz / viewSpacePos.w;

    vec4 col = clamp(vec4(viewSpacePos.xyz,1));
    if(abs(viewSpacePos.z) >= farClip) col = vec4(0,0,0,0);

    Out_Color = col;
}