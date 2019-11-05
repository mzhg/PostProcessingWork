#include "OceanHelpers.glsl"

layout(location = 0) in float4 In_Position;

out Varyings
{
//    float4 positionCS : SV_POSITION;
    float4 flow_shadow /*: TEXCOORD1*/;
    float4 foam_screenPosXYW /*: TEXCOORD4*/;
    float4 lodAlpha_worldXZUndisplaced_oceanDepth /*: TEXCOORD5*/;
    float3 worldPos /*: TEXCOORD7*/;
    #if _DEBUGVISUALISESHAPESAMPLE_ON
    half3 debugtint /*: TEXCOORD8*/;
    #endif
    float4 grabPos /*: TEXCOORD9*/;

//    UNITY_FOG_COORDS(3)
}o;

uniform float _CrestTime;

// MeshScaleLerp, FarNormalsWeight, LODIndex (debug), unused
uniform float4 _InstanceData;

void main()
{
    // Move to world space
    o.worldPos = mul(unity_ObjectToWorld, float4(In_Position.xyz, 1.0)).xyz;

    // Vertex snapping and lod transition
    float lodAlpha;
    SnapAndTransitionVertLayout(_InstanceData.x, o.worldPos, lodAlpha);
    o.lodAlpha_worldXZUndisplaced_oceanDepth.x = lodAlpha;
    o.lodAlpha_worldXZUndisplaced_oceanDepth.yz = o.worldPos.xz;

    // sample shape textures - always lerp between 2 LOD scales, so sample two textures
    o.flow_shadow = float4(0., 0., 0., 0.);
    o.foam_screenPosXYW.x = 0.;

    o.lodAlpha_worldXZUndisplaced_oceanDepth.w = CREST_OCEAN_DEPTH_BASELINE;
    // Sample shape textures - always lerp between 2 LOD scales, so sample two textures

    // Calculate sample weights. params.z allows shape to be faded out (used on last lod to support pop-less scale transitions)
    const float wt_smallerLod = (1. - lodAlpha) * _LD_Params[_LD_SliceIndex].z;
    const float wt_biggerLod = (1. - wt_smallerLod) * _LD_Params[_LD_SliceIndex + 1].z;
    // Sample displacement textures, add results to current world pos / normal / foam
    const float2 positionWS_XZ_before = o.worldPos.xz;

    // Data that needs to be sampled at the undisplaced position
    if (wt_smallerLod > 0.001)
    {
        const float3 uv_slice_smallerLod = WorldToUV(positionWS_XZ_before);

    #if !_DEBUGDISABLESHAPETEXTURES_ON
        float sss = 0.;
        SampleDisplacements(_LD_TexArray_AnimatedWaves, uv_slice_smallerLod, wt_smallerLod, o.worldPos, sss);
    #endif

    #if _FOAM_ON
        SampleFoam(_LD_TexArray_Foam, uv_slice_smallerLod, wt_smallerLod, o.foam_screenPosXYW.x);
    #endif

    #if _FLOW_ON
        SampleFlow(_LD_TexArray_Flow, uv_slice_smallerLod, wt_smallerLod, o.flow_shadow.xy);
    #endif
    }
    if (wt_biggerLod > 0.001)
    {
        const float3 uv_slice_biggerLod = WorldToUV_BiggerLod(positionWS_XZ_before);

    #if !_DEBUGDISABLESHAPETEXTURES_ON
        float sss = 0.;
        SampleDisplacements(_LD_TexArray_AnimatedWaves, uv_slice_biggerLod, wt_biggerLod, o.worldPos, sss);
    #endif

    #if _FOAM_ON
        SampleFoam(_LD_TexArray_Foam, uv_slice_biggerLod, wt_biggerLod, o.foam_screenPosXYW.x);
    #endif

    #if _FLOW_ON
        SampleFlow(_LD_TexArray_Flow, uv_slice_biggerLod, wt_biggerLod, o.flow_shadow.xy);
    #endif
    }

    // Data that needs to be sampled at the displaced position
    if (wt_smallerLod > 0.001)
    {
        const float3 uv_slice_smallerLodDisp = WorldToUV(o.worldPos.xz);

        #if _SUBSURFACESHALLOWCOLOUR_ON
        SampleSeaDepth(_LD_TexArray_SeaFloorDepth, uv_slice_smallerLodDisp, wt_smallerLod, o.lodAlpha_worldXZUndisplaced_oceanDepth.w);
        #endif

        #if _SHADOWS_ON
        SampleShadow(_LD_TexArray_Shadow, uv_slice_smallerLodDisp, wt_smallerLod, o.flow_shadow.zw);
        #endif
    }
    if (wt_biggerLod > 0.001)
    {
        const float3 uv_slice_biggerLodDisp = WorldToUV_BiggerLod(o.worldPos.xz);

        #if _SUBSURFACESHALLOWCOLOUR_ON
        SampleSeaDepth(_LD_TexArray_SeaFloorDepth, uv_slice_biggerLodDisp, wt_biggerLod, o.lodAlpha_worldXZUndisplaced_oceanDepth.w);
        #endif

        #if _SHADOWS_ON
        SampleShadow(_LD_TexArray_Shadow, uv_slice_biggerLodDisp, wt_biggerLod, o.flow_shadow.zw);
        #endif
    }

    // Foam can saturate
    o.foam_screenPosXYW.x = saturate(o.foam_screenPosXYW.x);

    // debug tinting to see which shape textures are used
    #if _DEBUGVISUALISESHAPESAMPLE_ON
    #define TINT_COUNT 7
    float3 tintCols[TINT_COUNT];
    tintCols[0] = half3(1., 0., 0.); tintCols[1] = half3(1., 1., 0.); tintCols[2] = half3(1., 0., 1.); tintCols[3] = half3(0., 1., 1.); tintCols[4] = half3(0., 0., 1.); tintCols[5] = half3(1., 0., 1.); tintCols[6] = half3(.5, .5, 1.);
    o.debugtint = wt_smallerLod * tintCols[_LD_LodIdx_0 % TINT_COUNT] + wt_biggerLod * tintCols[_LD_LodIdx_1 % TINT_COUNT];
    #endif

    // view-projection
    gl_Position = mul(UNITY_MATRIX_VP, float4(o.worldPos, 1.));

//    UNITY_TRANSFER_FOG(o, o.positionCS);

    // unfortunate hoop jumping - this is inputs for refraction. depending on whether HDR is on or off, the grabbed scene
    // colours may or may not come from the backbuffer, which means they may or may not be flipped in y. use these macros
    // to get the right results, every time.
    o.grabPos = ComputeGrabScreenPos(gl_Position);
    o.foam_screenPosXYW.yzw = ComputeScreenPos(gl_Position).xyw;
}