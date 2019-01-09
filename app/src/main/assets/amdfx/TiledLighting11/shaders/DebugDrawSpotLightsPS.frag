#include "DebugDraw.glsl"

layout(location = 0) out float4 Out_Color;

out VS_OUTPUT_DRAW_SPOT_LIGHTS
{
//    float4 Position     : SV_POSITION; // vertex position
    float3 Normal       /*: NORMAL*/;      // vertex normal vector
    float4 Color        /*: COLOR0*/;      // vertex color
    float2 TextureUV    /*: TEXCOORD0*/;   // vertex texture coords
    float3 PositionWS   /*: TEXCOORD1*/;   // vertex position (world space)
}Input;

//--------------------------------------------------------------------------------------
// This shader creates a procedural texture to visualize the spot lights.
//--------------------------------------------------------------------------------------
//float4 DebugDrawSpotLightsPS( VS_OUTPUT_DRAW_SPOT_LIGHTS Input ) : SV_TARGET
void main()
{
    float3 vViewDir = normalize( g_vCameraPos - Input.PositionWS );
    float3 vNormal = normalize(Input.Normal);
    float fViewDirDotSurfaceNormal = dot(vViewDir,vNormal);

    float3 color = Input.Color.rgb;
    color *= fViewDirDotSurfaceNormal < 0.0 ? 0.5 : 1.0;

    Out_Color =  float4(color,1);
}