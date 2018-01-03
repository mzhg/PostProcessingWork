layout(location = 0) out vec4 Out_Color;

layout(binding = 0) uniform sampler2D g_textureSlot0;

void main()
{
    float v = texelFetch(g_textureSlot0, int2(gl_FragCoord.xy), 0 ).x;
    Out_Color=float4( frac(v), frac(v*10.0), frac(v*100.0), 1.0 );
}