
struct BindlessTexture
{
    sampler2D sampler;
};

uniform BindlessTexture texture[8];

out vec4 OutColor;

void main()
{
    OutColor = texelFetch(texture[1].sampler, ivec2(gl_FragCoord.xy), 0);
}