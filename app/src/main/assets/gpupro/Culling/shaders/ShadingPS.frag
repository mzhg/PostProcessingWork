in VS_OUT
{
    vec3 WorldPos;
    vec2 TexCoord;
    vec3 Normal;
}_input;

uniform vec4 gEyePos;
uniform vec4 gColor;

layout(binding = 0) uniform sampler2D gDiffuse;

out vec4 OutColor;

void main()
{
    vec4 diffuse;
    if(gColor.w == 0)
    {
        diffuse = texture(gDiffuse, _input.TexCoord);
    }
    else
    {
        diffuse = gColor;
    }

    const vec3 lightDir = normalise(vec3(1,1,1));
    vec3 view = normalise(gEyePos - _input.WorldPos);

    OutColor = max(0, dot(lightDir, view)) * diffuse + 0.2/* ambient color */;
    OutColor.a = 1;
    OutColor.rgb = clamp(OutColor.rgb, vec3(0), vec3(1));
}