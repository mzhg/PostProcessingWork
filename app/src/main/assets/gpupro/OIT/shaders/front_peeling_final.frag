
layout(location=0) out vec4 outColor;

uniform sampler2DRect ColorTex;
uniform vec3 uBackgroundColor;

void main(void)
{
    vec4 frontColor = texture(ColorTex, gl_FragCoord.xy);
    float transmittance = frontColor.a;

    outColor.rgb = frontColor.rgb + uBackgroundColor * transmittance;
}
