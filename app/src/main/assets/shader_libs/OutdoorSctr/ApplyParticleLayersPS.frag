#include "CloudsCommon.glsl"

layout(location = 0) out float fTransparenc;// : SV_Target0,
layout(location = 0) out float2 f2MinMaxDist;// : SV_Target1,
layout(location = 0) out float4 f4Color;// : SV_Target2

void main()
{
    uint2 ui2PixelIJ = In.m_f4Pos.xy;
    uint uiLayerDataInd = (ui2PixelIJ.x + ui2PixelIJ.y * g_GlobalCloudAttribs.uiDownscaledBackBufferWidth) * NUM_PARTICLE_LAYERS;
    SParticleLayer Layers[NUM_PARTICLE_LAYERS];
//    [unroll]
    for(int iLayer = 0; iLayer < NUM_PARTICLE_LAYERS; ++iLayer)
        Layers[iLayer] = g_bufParticleLayers[uiLayerDataInd + iLayer];

    //for(iLastLayer = NUM_PARTICLE_LAYERS-1; iLastLayer >= 0; --iLastLayer)
    //    if( Layers[iLastLayer].f2MinMaxDist.x < FLT_MAX )
    //        break;

    //if( iLastLayer < 0 )
    //    discard;

    int iLastLayer = NUM_PARTICLE_LAYERS-1;
    SParticleLayer LastLayer = Layers[0];

    f4Color = float4(0);
    fTransparency = 1;
//    [unroll]
    for(iLayer = 1; iLayer <= iLastLayer; ++iLayer)
    {
        float3 f3MergedColor;
        float fMergedTransparency;
        SParticleLayer MergedLayer;
        MergeParticleLayers(LastLayer, Layers[iLayer], MergedLayer, f3MergedColor.rgb, fMergedTransparency, true);

        fTransparency *= fMergedTransparency;
        f4Color.rgb = f4Color.rgb * fMergedTransparency + f3MergedColor;
        LastLayer = MergedLayer;
    }
    float fLastTransparency = exp( -LastLayer.fOpticalMass );
    f4Color.rgb = f4Color.rgb * fLastTransparency + LastLayer.f3Color * (1-fLastTransparency);
    fTransparency *= fLastTransparency;
    f4Color.a = fTransparency;
    f2MinMaxDist = LastLayer.f2MinMaxDist.x;
}