#include "vaShared.glsl"
#include "vaSimpleShadowMap.glsl"
#include "vaLighting.glsl"

layout(location = 0) out vec4 Out_Color;

void main()
{
    Out_Color = ApplyDirectionalAmbient( /*Position,*/ false );
}