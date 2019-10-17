in vec2 uv;

layout(binding = 0) uniform sampler2D _MainTex;
layout(location = 0) out vec4 OutColor;

void main()
{
#if ADD_FROM_TEX
    OutColor = texture(_MainTex, uv);
#else
    OutColor.x = texture(_MainTex, uv).x;
    OutColor.yzw = vec3(0);
#endif
}