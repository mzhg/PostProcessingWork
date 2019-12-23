layout(location = 0) in vec4 In_Position;
layout(location = 1) in vec3 In_Normal;
layout(location = 2) in vec2 In_TexCoord;

uniform mat4 gModel;
uniform mat4 gNormal;
uniform mat4 gProj;
uniform mat4 gView;

out VS_OUT
{
    vec3 WorldPos;
    vec2 TexCoord;
    vec3 Normal;
}_output;

layout(binding = 0) uniform Instance
{
    mat4 gInstance[64];
    mat4 gNormalMats[64];
};

void main()
{
    vec4 WorldPos = gInstance[gl_InstanceID] * In_Position;
    gl_Position = gProj *gView*WorldPos;

    _output.WorldPos = WorldPos.xyz;
    _output.TexCoord = In_TexCoord;
    _output.Normal = (gNormalMats[gl_InstanceID] * vec4(In_Normal, 0)).xyz;
}