#include "GBuffer.glsl"

// Initialize stencil mask with per-sample/per-pixel flags
void main()
{
    SurfaceData surfaceSamples[MSAA_SAMPLES];
    ComputeSurfaceDataFromGBufferAllSamples(int2(gl_FragCoord.xy), surfaceSamples);
    bool perSample = RequiresPerSampleShading(surfaceSamples);

    // Kill fragment (i.e. don't write stencil) if we don't require per sample shading
    if (!perSample)
    {
        discard;
    }
}