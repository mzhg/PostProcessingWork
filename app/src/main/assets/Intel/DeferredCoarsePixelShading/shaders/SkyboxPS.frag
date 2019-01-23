#include "GPUQuad.glsl"

//--------------------------------------------------------------------------------------
// Tone mapping, post processing, skybox, etc.
// Rendered using skybox geometry, hence the naming
//--------------------------------------------------------------------------------------
layout(binding = 0) uniform samplerCube gSkyboxTexture; // : register(t5);
#if MSAA_SAMPLES > 1
layout(binding = 1) uniform sampler2DMS gDepthTexture;// : register(t6);
#else
layout(binding = 1) uniform sampler2D gDepthTexture;// : register(t6);
#endif // MSAA_SAMPLES > 1

// This is the regular multisampled lit texture
// StephanieB5: fixing runtime error by adding different code path for MSAA_SAMPLES > 1
#if MSAA_SAMPLES > 1
layout(binding = 2) uniform sampler2DMS gLitTexture /*: register(t7)*/;
#else
layout(binding = 2) uniform sampler2D gLitTexture /*: register(t7)*/;
#endif // MSAA_SAMPLES > 1

// Since compute shaders cannot write to multisampled UAVs, this texture is used by
// the CS paths. It stores each sample separately in rows (i.e. y).
//StructuredBuffer<uint2> gLitTextureFlat : register(t8);
layout(binding = 2) uniform isamplerBuffer gLitTextureFlat;

/*layout(binding = 0) readonly buffer LitTextureFlat
{
    uint2 gLitTextureFlat[];
};*/

layout(location = 0) out float3 Out_Color;
in float3 skyboxCoord;

void main()
{
    // Use the flattened MSAA lit buffer if provided
    /*uint2 dims;
    gLitTextureFlat.GetDimensions(dims.x, dims.y);*/
    bool useFlatLitBuffer = textureSize(gLitTextureFlat) > 0;

    int2 coords = int2(gl_FragCoord.xy);

    float3 lit = float3(0.0f, 0.0f, 0.0f);
    float skyboxSamples = 0.0f;
    #if MSAA_SAMPLES <= 1
    #endif
    for (int sampleIndex = 0; sampleIndex < MSAA_SAMPLES; ++sampleIndex) {
#if MSAA_SAMPLES > 1
        float depth = texelFetch(gDepthTexture, coords, sampleIndex);
#else
        float depth = texelFetch(gDepthTexture, coords, 0);
#endif // MSAA_SAMPLES > 1

        // Check for skybox case (NOTE: complementary Z!)
        if (depth <= 0.0f && !mUI.visualizeLightCount) {
            ++skyboxSamples;
        } else {
            float3 sampleLit;
            if (useFlatLitBuffer) {
                sampleLit = UnpackRGBA16(gLitTextureFlat[GetFramebufferSampleAddress(coords, sampleIndex)]).xyz;
            } else {
// StephanieB5: fixing runtime error by adding different code path for MSAA_SAMPLES > 1
#if MSAA_SAMPLES > 1
                sampleLit = texelFetch(gLitTexture, coords, sampleIndex).xyz;
#else
                sampleLit = texelFetch(gLitTexture, coords, 0).xyz;
#endif // MSAA_SAMPLES > 1
            }

            // Tone map each sample separately (identity for now) and accumulate
            lit += sampleLit;
        }
    }

    // If necessary, add skybox contribution
    if (skyboxSamples > 0) {
        float3 skybox = texture(gSkyboxTexture, skyboxCoord).xyz;  // gDiffuseSampler
        // Tone map and accumulate
        lit += skyboxSamples * skybox;
    }

    // Resolve MSAA samples (simple box filter)
    Out_Color = float4(lit * rcp(MSAA_SAMPLES), 1.0f);
}