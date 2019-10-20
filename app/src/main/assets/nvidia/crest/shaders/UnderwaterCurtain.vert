#include "OceanLODData.glsl"

layout(location = 0) in float4 In_Position;
layout(location = 1) in float2 In_UV;

#define MAX_OFFSET 5.0

uniform float _CrestTime;
uniform float _MeniscusWidth;

out struct Varyings
{
//    float4 positionCS : SV_POSITION;
    float2 uv /*: TEXCOORD0*/;
    half4 foam_screenPos /*: TEXCOORD1*/;
    half4 grabPos /*: TEXCOORD2*/;
    float3 worldPos /*: TEXCOORD3*/;
}o;

#define CREST_MAX_UPDOWN_AMOUNT 0.8

float IntersectRayWithWaterSurface(const float3 pos, const float3 dir)
{
    // Find intersection of the near plane and the water surface at this vert using FPI. See here for info about
    // FPI http://www.huwbowles.com/fpi-gdc-2016/

    // get point at sea level
    float2 sampleXZ = pos.xz - dir.xz * (pos.y - _OceanCenterPosWorld.y) / dir.y;
    float3 disp;
    for (int i = 0; i < 6; i++)
    {
        // Sample displacement textures, add results to current world pos / normal / foam
        disp = float3(sampleXZ.x, _OceanCenterPosWorld.y, sampleXZ.y);
        float sss = 0.;
        SampleDisplacements(_LD_TexArray_AnimatedWaves, WorldToUV(sampleXZ), 1.0, disp, sss);
        float3 nearestPointOnRay = pos + dir * dot(disp - pos, dir);
        const float2 error = disp.xz - nearestPointOnRay.xz;
        sampleXZ -= error;
    }

    return dot(disp - pos, dir);
}

void main()
{
    // view coordinate frame for camera
    const float3 right = unity_CameraToWorld._11_21_31;
    const float3 up = unity_CameraToWorld._12_22_32;
    const float3 forward = unity_CameraToWorld._13_23_33;

    const float3 nearPlaneCenter = _WorldSpaceCameraPos + forward * _ProjectionParams.y * 1.001;
    // Spread verts across the near plane.
    const float aspect = _ScreenParams.x / _ScreenParams.y;
    o.worldPos = nearPlaneCenter
    + 2.6 * unity_CameraInvProjection._m11 * aspect * right * In_Position.x * _ProjectionParams.y
    + up * In_Position.z * _ProjectionParams.y;

    if (abs(forward.y) < CREST_MAX_UPDOWN_AMOUNT)
    {
        o.worldPos += min(IntersectRayWithWaterSurface(o.worldPos, up), MAX_OFFSET) * up;

        const float offset = 0.001 * _ProjectionParams.y * _MeniscusWidth;
        if (In_Position.z > 0.49)
        {
            o.worldPos += offset * up;
        }
        else
        {
            o.worldPos -= offset * up;
        }
    }
    else
    {
        // kill completely if looking up/down
        o.worldPos *= 0.0;
    }

    gl_Position = mul(UNITY_MATRIX_VP, float4(o.worldPos, 1.0));
    gl_Position.z = gl_Position.w;

    o.foam_screenPos.yzw = ComputeScreenPos(gl_Position).xyw;
    o.foam_screenPos.x = 0.0;
    o.grabPos = ComputeGrabScreenPos(gl_Position);

    o.uv = In_UV;
}