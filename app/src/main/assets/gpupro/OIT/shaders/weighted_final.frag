
layout(location=0) out vec4 outColor;

uniform sampler2DRect ColorTex0;
uniform sampler2DRect ColorTex1;
uniform vec3 uBackgroundColor;

void main(void)
{
    vec4 sumColor = texture(ColorTex0, gl_FragCoord.xy);
    float transmittance = texture(ColorTex1, gl_FragCoord.xy).r;
    vec3 averageColor = sumColor.rgb / max(sumColor.a, 0.00001);

    outColor.rgb = averageColor * (1 - transmittance) + uBackgroundColor * transmittance;
}
