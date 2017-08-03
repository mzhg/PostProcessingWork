#if GL_ES  // Mobile
    #if __VERSION__ >= 300
    in vec2 vInterpTexCoord;
    out vec4 Output;
    #else
    varying vec2 vInterpTexCoord;
    #define Output gl_FragColor
    #endif
#else  // Desktop
    #if __VERSION__ >= 130
    in vec2 vInterpTexCoord;
    out vec4 Output;
    #else
    varying vec2 vInterpTexCoord;
    #define Output gl_FragColor
    #endif
#endif

#ifdef GFSDK_WAVEWORKS_GL4
layout(binding=0) uniform sampler2D g_samplerEnergyMap;  // unit 0
layout(binding=0) uniform globals
{
    vec4 g_DissipationFactors;  // x - the blur extents, y - the fadeout multiplier, z - the accumulation multiplier, w - foam generation threshold
    vec4 g_SourceComponents;    // xyzw - weights of energy map components to be sampled
    vec4 g_UVOffsets;           // xy - defines either horizontal offsets either vertical offsets
};
#else
uniform sampler2D g_samplerEnergyMap;  // unit 0
uniform vec4 g_DissipationFactors;
uniform vec4 g_SourceComponents;
uniform vec4 g_UVOffsets;
#endif

void main(){
    vec2 UVoffset = g_UVOffsets.xy*g_DissipationFactors.x;

    // blur with variable size kernel is done by doing 4 bilinear samples,
    // each sample is slightly offset from the center point
    float foamenergy1	= dot(g_SourceComponents, texture(g_samplerEnergyMap, vInterpTexCoord.xy + UVoffset.xy));
    float foamenergy2	= dot(g_SourceComponents, texture(g_samplerEnergyMap, vInterpTexCoord.xy - UVoffset.xy));
    float foamenergy3	= dot(g_SourceComponents, texture(g_samplerEnergyMap, vInterpTexCoord.xy + UVoffset.xy*2.0));
    float foamenergy4	= dot(g_SourceComponents, texture(g_samplerEnergyMap, vInterpTexCoord.xy - UVoffset.xy*2.0));

    float folding = max(0.0,texture(g_samplerEnergyMap, vInterpTexCoord.xy).z);

    float energy = g_DissipationFactors.y*((foamenergy1 + foamenergy2 + foamenergy3 + foamenergy4)*0.25 + max(0,(1.0-folding-g_DissipationFactors.w))*g_DissipationFactors.z);

    energy = min(1.0,energy);

    // Output
    Output = vec4(energy,energy,energy,energy);
}