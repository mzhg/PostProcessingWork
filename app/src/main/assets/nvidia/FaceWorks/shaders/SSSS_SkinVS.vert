#include "SSSS_Common.glsl"

layout(location =0) in float3		In_pos		/*: POSITION*/;
layout(location =1) in float3		In_normal	/*: NORMAL*/;
layout(location =2) in float2		In_uv		/*: UV*/;
layout(location =3) in float3		In_tangent	/*: TANGENT*/;
layout(location =4) in float		In_curvature /*: CURVATURE*/;

out SceneV2P
{
    float2 texcoord;
    float3 tangentView;
    float3 worldPosition;
    float3 tangentLight;
    float3 normal;
    float3 tangent;
}_output;

void main()
{
    gl_Position = mul(float4(In_pos, 1), worldViewProjection);
    _output.texcoord = In_uv;
    _output.worldPosition = In_pos.xyz;
    _output.normal = In_normal;
    _output.tangent = In_tangent;

    float3 N = normalize(mul(In_normal, float3x3(worldInverseTranspose)));
    float3 T = normalize(mul(In_tangent, float3x3(worldInverseTranspose)));
    float3 B = cross(N, T);
    float3x3 frame = float3x3(T, B, N);

    float3 view = cameraPosition.xyz - _output.worldPosition;
    _output.tangentView = mul(frame, view);
    _output.tangentView = normalize(view);

    float3 light = lightPos.xyz - _output.worldPosition;
    _output.tangentLight = mul(frame, light);
}