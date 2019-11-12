#define _FRAG_SHADER
#include "ocean_spray.glsl"

out float OutColor;

in GS_FOAM_PARTICLE_OUTPUT {
//    float4 Position               : SV_Position;
    float3 ViewPos                /*: ViewPos*/;
    float3 TextureUVAndOpacity    /*: TEXCOORD0*/;
    float  FoamAmount			  /*: FOAMAMOUNT*/;
}In;

void main()
{
    OutColor = In.FoamAmount * GetParticleRGBA(/*g_SamplerTrilinearClamp,*/ In.TextureUVAndOpacity.xy, In.TextureUVAndOpacity.z).a;
}