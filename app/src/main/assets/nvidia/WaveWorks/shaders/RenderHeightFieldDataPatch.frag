#version 400

in PSIn_Diffuse
{
//	vec4 position;
	vec2 texcoord;
	vec3 normal;
	vec3 positionWS;
	vec4 layerdef;
	vec4 depthmap_scaler;
}_input;

layout (location = 0) out vec4 fragColor;
#include "RenderHeightfieldCommon.glsl"

void main()
{
    float y_biased = _input.positionWS.y + 2.0;
    fragColor = vec4( y_biased, y_biased, 0, 0 );
}