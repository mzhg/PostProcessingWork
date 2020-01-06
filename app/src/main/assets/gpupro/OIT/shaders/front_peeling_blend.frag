
layout(location=0) out vec4 outColor;

uniform sampler2D TempTex;
uniform vec2 ViewSize;
void main(void)
{
    outColor = texture(TempTex, gl_FragCoord.xy/ViewSize);
}
