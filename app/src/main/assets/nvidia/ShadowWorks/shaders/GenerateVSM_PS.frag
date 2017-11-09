in float fDepth;

out vec4 Out_Color;

void main()
{
    Out_Color = vec4(fDepth, fDepth * fDepth, 0.0f, 1.0f );
}
