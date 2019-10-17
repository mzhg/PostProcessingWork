layout(location = 0) out vec2 OutColor;

in vec2 vel;

void main()
{
    OutColor = vel;
}