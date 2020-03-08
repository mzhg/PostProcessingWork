layout(location = 0) in vec4 In_Position;
layout(location = 1) in vec3 In_Normal;
layout(location = 2) in vec2 In_TexCoord;

uniform mat4 gModel;
uniform mat4 gNormal;
uniform mat4 gProj;
uniform mat4 gView;

uniform vec3 gSihouetteScale = vec3(1.05);

out VS_OUT
{
    vec3 WorldSilhouette;
    vec3 WorldPos;
    vec2 TexCoord;
    vec3 Normal;
}_output;

layout(binding = 0) uniform Instance
{
    mat4 gInstance[64];
    mat4 gNormalMats[64];
    int gMaterialID[64];
};

void main()
{
    vec4 WorldPos = gInstance[gl_InstanceID] * In_Position;
    vec4 WorldSilhouette = vec4(WorldPos.x, WorldPos.y * gSihouetteScale.y, WorldPos.z, 1);

    gl_Position = gProj *gView*WorldSilhouette;

    _output.WorldSilhouette = WorldSilhouette.xyz;
    _output.WorldPos = WorldPos.xyz;
    _output.TexCoord = In_TexCoord;
    _output.Normal = (gNormalMats[gl_InstanceID] * vec4(In_Normal, 0)).xyz;
}