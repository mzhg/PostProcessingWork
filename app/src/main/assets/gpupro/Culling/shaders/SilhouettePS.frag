
out vec4 OutColor;

uniform vec3 gSihoutteColor = vec3(0.9,0.8,0.5);

layout(binding = 0) uniform sampler2D gSceneDepth;

uniform mat4 gModel;
uniform mat4 gNormal;
uniform mat4 gProj;
uniform mat4 gView;

#define FRONT 1
#define BACK  2

in VS_OUT
{
    vec3 WorldSilhouette;
    vec3 WorldPos;
    vec2 TexCoord;
    vec3 Normal;
}_input;

void main()
{
    #if RENDER_FACE == FRONT

    vec4 ClipPosition = gProj * gView * vec4(_input.WorldPos, 1);
    ClipPosition.xyz /= ClipPosition.w;
    ClipPosition.xyz = ClipPosition.xyz * 0.5 + 0.5;

    float SceneDepth = textureLod(gSceneDepth, ClipPosition.xy, 0.0).x;
    if(SceneDepth >= ClipPosition.z)
    {
        discard;
    }
    #endif

    OutColor = vec4(gSihoutteColor, 1);
}