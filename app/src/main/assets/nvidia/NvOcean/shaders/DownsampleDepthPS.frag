in vec4 m_f4UVAndScreenPos;

layout(binding = 0) uniform sampler2DMS g_texDepthMS;

out float OutColor;
void main()
{
    ivec2 iCoords = ivec2(gl_Fragcoord.xy);
    iCoords *= 2;

    OutColor = texelFetch(g_texDepthMS, iCoords, 0);
}