
layout(location=0) out vec4 Out_Color;

in LinesOutVertexGS2PS
{
//    float4 Position;
    uint Level;
}_input;

void main()
{
    Out_Color = float4(colors[_input.Level], 1.0f);
}