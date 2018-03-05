
layout(location = 0) in vec3 In_Position;

uniform mat4 gWorld;
uniform mat4 gLightViewProj;
uniform mat4 gLightView;
uniform vec3 gLightPos;

out float fDepth;

void main()
{
    /*float4 worldPos = mul( float4( input.pos, 1.0f ), mWorld );
    output.pos = mul( mul( worldPos, mShadowView ), mShadowProj );
    output.fDepth = length( vLightPos.xyz - worldPos.xyz );*/

    vec4 worldPos = gWorld * vec4(In_Position, 1);
    gl_Position = gLightViewProj * worldPos;
    fDepth = /*length( gLightPos - worldPos.xyz )*/ -(gLightView * worldPos).z ;
}