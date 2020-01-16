#include "../../../Intel/OIT/shaders/AOIT.glsl"

in VS_OUT
{
    vec3 WorldPos;
    vec2 TexCoord;
    vec3 Normal;
}_input;

uniform vec3 gEyePos;
uniform vec4 gColor;

layout(binding = 0) uniform sampler2D gDiffuse;

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

    const vec3 lightDir = normalize(vec3(2,5,1));
    vec3 view = normalize(gEyePos - _input.WorldPos);

    OutColor = max(0, dot(lightDir, normalize(_input.Normal))) * diffuse + 0.2/* ambient color */;
    OutColor.a = diffuse.a;
    OutColor.rgb = clamp(OutColor.rgb, vec3(0), vec3(1));

//    result.a *= countbits( coverageMask ) * MultiSampleCount.y;
    float surfaceDepth = gl_FragCoord.z;
    if (OutColor.a > 0.01f)
    {
        WriteNewPixelToAOIT(gl_FragCoord.xy,surfaceDepth,OutColor);

        // need to write out something for the stencil mask, but not added since we're alpha blending
        // (better approach would be to not select any RT with OMSetRenderTargetsAndUnorderedAccessViews
//        return; // float4( 1, 0, 0, 0 );
    }
}