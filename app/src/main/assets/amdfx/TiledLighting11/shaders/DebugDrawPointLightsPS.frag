#include "DebugDraw.glsl"

layout(location = 0) out float4 Out_Color;

in VS_OUTPUT_DRAW_POINT_LIGHTS
{
//    float4 Position     : SV_POSITION; // vertex position
    float3 Color        /*: COLOR0*/;      // vertex color
    float2 TextureUV    /*: TEXCOORD0*/;   // vertex texture coords
}Input;

/** This shader creates a procedural texture to visualize the point lights. */
void main()
{
    float fRad = 0.5f;
    float2 Crd = Input.TextureUV - float2(fRad, fRad);
    float fCrdLength = length(Crd);

    // early out if outside the circle
    if( fCrdLength > fRad ) discard;

    // use pow function to make a point light visualization
    float x = ( 1.f-fCrdLength/fRad );
    Out_Color = float4(0.5f*pow(x,5.f)*Input.Color + 2.f*pow(x,20.f), 1);
}