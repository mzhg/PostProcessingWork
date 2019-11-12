#define _FRAG_SHADER
#include "ocean_spray.glsl"

in DS_SCENE_PARTICLE_OUTPUT {
//    float4 Position               : SV_Position;
    float3 ViewPos                /*: ViewPos*/;
    float3 TextureUVAndOpacity    /*: TEXCOORD0*/;
// NOT USED float3 PSMCoords              : PSMCoords;
    float FogFactor               /*: FogFactor*/;
    float3 Lighting               /*: LIGHTING*/;
}In;

out float4 OutColor;

#if USE_DOWNSAMPLING
layout(binding = 0) uniform sampler2D g_texDepth;
#else
layout(binding = 0) uniform sampler2DMS g_texDepth;
#endif

void main()
{
    if(g_SimpleParticles>0)
    {
        OutColor = float4(0.5,0.0,0.0,1.0);
        return;
    }

    // disable PSM on spray dynamic_lighting *= CalcPSMShadowFactor(In.PSMCoords);
    float4 result = GetParticleRGBA(/*g_SamplerTrilinearClamp,*/ In.TextureUVAndOpacity.xy, In.TextureUVAndOpacity.z);

    result.rgb *= In.Lighting;

    result.rgb = lerp(g_AmbientColor + g_LightningColor,result.rgb,In.FogFactor);
    result.a = 0.125 * In.TextureUVAndOpacity.z;

//    #if USE_DOWNSAMPLING
    float depth = texelFetch(g_texDepth, int2(gl_FragCoord), 0).x; // coarse depth
//    #else
//    float depth = g_texDepth.Load((int2)In.Position.xy, 0); // fine depth
//    #endif

    float4 clipPos = float4(0, 0, depth, 1.0);
    float4 viewSpace = mul(clipPos, g_matProjInv);
    viewSpace.z /= viewSpace.w;

    result.a *= saturate(abs(In.ViewPos.z - viewSpace.z) * 0.5);
    OutColor = result;
}