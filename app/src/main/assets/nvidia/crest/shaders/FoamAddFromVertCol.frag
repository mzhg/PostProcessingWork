layout(location = 0) out vec4 OutColor;

in vec4 color;
uniform float _Strength = 1.f;
void main()
{
    OutColor = color * _Strength;
}