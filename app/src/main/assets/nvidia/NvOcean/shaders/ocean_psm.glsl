//------------------------------------------------------------------------------------
// Global variables
//------------------------------------------------------------------------------------
uniform float4x4 g_matViewToPSM;
uniform float  g_PSMSlices;
uniform float3 g_PSMTint;
layout(binding = 0) uniform sampler3D g_PSMMap;

//------------------------------------------------------------------------------------
// Constants
//------------------------------------------------------------------------------------

//-----------------------------------------------------------------------------------
// Texture & Samplers
//-----------------------------------------------------------------------------------
/*SamplerState g_SamplerPSM
{
    Filter = MIN_MAG_MIP_LINEAR;
    AddressU = Clamp;
    AddressV = Clamp;
    AddressW = Clamp;
};*/

//--------------------------------------------------------------------------------------
// DepthStates
//--------------------------------------------------------------------------------------

//--------------------------------------------------------------------------------------
// RasterStates
//--------------------------------------------------------------------------------------

//--------------------------------------------------------------------------------------
// BlendStates
//--------------------------------------------------------------------------------------
/*BlendState PSMBlend
{
    BlendEnable[0] = TRUE;
    RenderTargetWriteMask[0] = 0xF;

    SrcBlend = Zero;
    DestBlend = Inv_Src_Color;
    BlendOp = Add;

    SrcBlendAlpha = Zero;
    DestBlendAlpha = Inv_Src_Alpha;
    BlendOpAlpha = Add;
};*/

//--------------------------------------------------------------------------------------
// Structs
//--------------------------------------------------------------------------------------

//--------------------------------------------------------------------------------------
// Functions
//--------------------------------------------------------------------------------------
float3 CalcPSMShadowFactor(float3 PSMCoords)
{
    int PSMMapW, PSMMapH, PSMMapD;
//    g_PSMMap.GetDimensions(PSMMapW, PSMMapH, PSMMapD);
    int3 texSize = textureSize(g_PSMMap,0);
    PSMMapW = texSize.x;
    PSMMapH = texSize.y;
    PSMMapD = texSize.z;

    float NumSlices = g_PSMSlices;
    float slice = NumSlices * saturate(PSMCoords.z) + 1.f; // +1.f because zero slice reserved for coverage
    slice = min(slice, NumSlices + 0.5f);
    float slice_upper = ceil(slice);
    float slice_lower = slice_upper - 1;

    float3 lower_uvz = float3(PSMCoords.xy,(0.5f+slice_lower)/float(PSMMapD));
    float3 upper_uvz = float3(PSMCoords.xy,(0.5f+slice_upper)/float(PSMMapD));

    float4 raw_lower_vals = slice_lower < 1.f ? 1.f : g_PSMMap.SampleLevel(g_SamplerPSM, lower_uvz, 0);
    float raw_upper_val = slice_upper < 1.f ? 1.f : g_PSMMap.SampleLevel(g_SamplerPSM, upper_uvz, 0).r;
    float lower_val;
    float upper_val;
    float slice_lerp = 2.f * frac(slice);
    if(slice_lerp >= 1.f) {
        lower_val = raw_lower_vals.g;
        upper_val = raw_upper_val;
    } else if(slice_lerp < 1.f) {
        lower_val = raw_lower_vals.r;
        upper_val = raw_lower_vals.g;
    }

    float shadow_factor = lerp(lower_val,upper_val,frac(slice_lerp));

    return pow(shadow_factor,g_PSMTint);
}