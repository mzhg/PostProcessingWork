layout(location = 0) in vec3 In_Position;
layout(location = 1) in vec3 In_Normal;
layout(location = 2) in vec2 In_Texcoord;

out Sparkles_VS
{
    vec4 position;
    vec3 normal;
    flat int instanceID;
}_output;

in int gl_InstanceID;

void main()
{
    vec3 P = In_Position;
    _output.position = vec4(P, 1);
    _output.normal = In_Normal;
    _output.instanceID = gl_InstanceID;
}