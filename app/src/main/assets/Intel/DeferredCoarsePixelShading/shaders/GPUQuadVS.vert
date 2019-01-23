#include "GPUQuad.glsl"


// One per quad - gets expanded in the geometry shader
out GPUQuadVSOut
{
    float4 coords /*: coords*/;         // [min.xy, max.xy] in clip space
    float quadZ /*: quadZ*/;
    flat int lightIndex /*: lightIndex*/;
}_output;

void main()
{
    int lightIndex = gl_VertexID;

    _output.lightIndex = lightIndex;

    // Work out tight clip-space rectangle
    PointLight light = gLight[lightIndex];
    _output.coords = ComputeClipRegion(light.positionView, light.attenuationEnd);

    // Work out nearest depth for quad Z
    // Clamp to near plane in case this light intersects the near plane... don't want our quad to be clipped
    float quadDepth = max(mCameraNearFar.x, light.positionView.z - light.attenuationEnd);

    // Project quad depth into clip space
    float4 quadClip = mul(float4(0.0f, 0.0f, quadDepth, 1.0f), mCameraProj);
    _output.quadZ = quadClip.z / quadClip.w;
}