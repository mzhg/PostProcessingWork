layout(location = 0) in vec4 In_Position;

uniform mat4 g_Local;
uniform mat4 g_View;
uniform mat4 g_Proj;

void main()
{
    gl_Position = g_Proj * g_View * g_Local * In_Position;
//    gl_Position = vec4(In_Position.x, In_Position.z, 0, 1);
}