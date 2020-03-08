in VS_OUT
{
    vec3 WorldPos;
    vec2 TexCoord;
    vec3 Normal;
}_input;

uniform vec3 gEyePos;
uniform vec4 gColor;

#define SINGLE 1
#define RECT   2

uniform ivec4 gPickRect;
uniform int gPickType;
uniform int gObjectID;

layout(binding = 0) uniform sampler2D gDiffuse;
layout(binding = 0, r32ui) uniform uimage2D gPickResults;

out vec4 OutColor;
layout(early_fragment_tests) in;

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

    if(diffuse.w < 0.2)
        discard;

    const vec3 lightDir = normalize(vec3(2,5,1));
    vec3 view = normalize(gEyePos - _input.WorldPos);

    OutColor = max(0, dot(lightDir, normalize(_input.Normal))) * diffuse + 0.2/* ambient color */;
    OutColor.a = 1;
    OutColor.rgb = clamp(OutColor.rgb, vec3(0), vec3(1));

    if(gPickType == SINGLE)
    {
        if(ivec2(gl_FragCoord.xy) == gPickRect.xy)
        {
            imageAtomicExchange(gPickResults, ivec2(0), gObjectID);
        }
    }
    else if(gPickType == RECT)
    {
        ivec2 Size = gPickRect.zw - gPickRect.xy;
        ivec2 RelativePos = ivec2(gl_FragCoord.xy) - gPickRect.xy;

        if(all(lessThan(RelativePos, Size)) && all(equal(RelativePos % 4, ivec2(0))))
        {
            imageAtomicExchange(gPickResults, RelativePos/4, gObjectID);
        }
    }
}