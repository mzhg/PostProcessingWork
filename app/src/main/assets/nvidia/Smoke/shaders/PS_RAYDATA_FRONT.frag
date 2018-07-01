#include "VolumeRenderer.glsl"

layout(location = 0) out vec4 OutColor;

in PS_INPUT_RAYDATA_FRONT
{
//    float4 pos      : SV_Position;
    float3 posInGrid/*: POSITION*/;
    float3 worldViewPos /*: TEXCOORD0*/;
}_input;

void main()
{
    float2 normalizedInputPos = float2(gl_FragCoord.x/RTWidth, gl_FragCoord.y/RTHeight);
    float sceneZ = sceneDepthTex.SampleLevel( samLinearClamp, normalizedInputPos, 0).r;
    float2 inputPos = float2((normalizedInputPos.x*2.0)-1.0,(normalizedInputPos.y*2.0)-1.0);
    sceneZ = length(float3( inputPos.x * sceneZ * tan_FovXhalf, inputPos.y * sceneZ * tan_FovYhalf, sceneZ ));

    float inputDepth = length(_input.worldViewPos);

    if(sceneZ < inputDepth)
    {
        // If the scene occludes intersection point we want to kill the pixel early in PS
        OutColor =  OCCLUDED_PIXEL_RAYVALUE;
        return;
    }
    // We negate gl_FragCoordInGrid because we use subtractive blending in front faces
    //  Note that we set xyz to 0 when rendering back faces
    OutColor.xyz = -_input.posInGrid;
    OutColor.w = inputDepth;
}