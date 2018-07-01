#include "VolumeRenderer.glsl"

layout(location = 0) out float4 OutColor;

in float4 m_PosInGrid;

#include "RayCast.glsl"

float4 RaycastUpsample(/*PS_INPUT_RAYCAST input,*/ /*uniform*/ int raycastMode, /*uniform*/ float sampleFactor)
{
    float edge = texture(edgeTex, float2(gl_FragCoord.x/RTWidth,gl_FragCoord.y/RTHeight)).r;  //samLinearClamp
    float4 tex = texture(rayCastTex, float2(gl_FragCoord.x/RTWidth,gl_FragCoord.y/RTHeight)); // samLinearClamp

    if(edge > 0 && tex.a > 0)
        return Raycast(/*input,*/ raycastMode, sampleFactor );
    else
        return tex;
}

void main()
{
    float4 color = RaycastUpsample(/*input,*/ raycastMode, sampleFactor);

    if( allowGlow )
    {
        if(useGlow)
        {
            float4 glow = texture(glowTex, float2(gl_FragCoord.x/RTWidth,gl_FragCoord.y/RTHeight));  //samLinearClamp
            color.rgba += glowContribution*glow.rgba;
        }

        //tone map
        color.rgb /= finalIntensityScale;

        color.a = clamp(color.a,0,1);
        color.rgb *= color.a;
        color.a = color.a*finalAlphaScale;
    }

    OutColor = color;
}