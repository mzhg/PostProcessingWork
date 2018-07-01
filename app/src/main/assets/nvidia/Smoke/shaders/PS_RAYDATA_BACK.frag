#include "VolumeRenderer.glsl"

layout(location = 0) out vec4 OutColor;

in vec3 m_WorldViewPos;

void main()
{
    //get the distance from the eye to the scene point
    //we do this by unprojecting the scene point to view space and then taking the length of the vector
    float2 normalizedInputPos = float2(gl_FragCoord.x/RTWidth, gl_FragCoord.y/RTHeight);
    float sceneZ = sceneDepthTex.SampleLevel( samLinearClamp, normalizedInputPos ,0).r;
    float2 inputPos = float2((normalizedInputPos.x*2.0)-1.0,(normalizedInputPos.y*2.0)-1.0);
    sceneZ = length(float3( inputPos.x * sceneZ * tan_FovXhalf, inputPos.y * sceneZ * tan_FovYhalf, sceneZ ));
    
    float inputDepth = length(m_WorldViewPos);

    // This value will only remain if no fragments get blended on top in the next pass (front-faces)
    //  which would happen if the front faces of the box get clipped by the near plane of the camera
    OutColor.xyz = NEARCLIPPED_PIXEL_RAYPOS;

    OutColor.w = min(inputDepth, sceneZ);
}