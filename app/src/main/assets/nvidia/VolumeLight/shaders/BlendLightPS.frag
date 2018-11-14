
in vec4 m_f4UVAndScreenPos;
out vec4 Out_Color;

uniform float g_BufferWidthInv;
uniform float g_BufferHeightInv;

layout(binding = 0) uniform sampler2D s0;

#define float2 vec2

//-----------------------------------------------------------------------------
// Name: BlendLight
// Type: Pixel Shader
// Desc: Blend light shafts with the final image
//-----------------------------------------------------------------------------
void main()
{

    Out_Color.xyz = vec3(0.75);
    Out_Color.w = texture(s0,  m_f4UVAndScreenPos.xy).x;   // samplerLinearClamp
}