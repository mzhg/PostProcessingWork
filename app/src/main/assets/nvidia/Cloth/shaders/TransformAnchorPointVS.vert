layout(location = 0) in vec4 In_Coordinate;

void main()
{
    gl_Position = vec4(In_Coordinate.x, 0,0,1);
}