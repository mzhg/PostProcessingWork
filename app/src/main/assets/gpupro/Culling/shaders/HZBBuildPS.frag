layout(binding = 0) uniform sampler2D DepthBuffer;

uniform vec2 InvSize;
uniform vec4 InputUvFactorAndOffset;
uniform vec2 InputViewportMaxBound;

in vec4 m_f4UVAndScreenPos;
out float OutColor;

void main()
{
    vec4 Depth;

#if STAGE == 0
    const vec2 ViewUV = m_f4UVAndScreenPos.zw * vec2(0.5f, 0.5f) + 0.5f;
    const vec2 InUV = ViewUV * InputUvFactorAndOffset.xy + InputUvFactorAndOffset.zw;

    // min(..., InputViewportMaxBound) because we don't want to sample outside of the viewport
    // when the view size has odd dimensions on X/Y axis.
#if 0 //COMPILER_GLSL || COMPILER_GLSL_ES2 || COMPILER_GLSL_ES3_1 || FEATURE_LEVEL < FEATURE_LEVEL_SM5
    vec2 UV[4];
    UV[0] = min(InUV + float2(-0.25f, -0.25f) * InvSize, InputViewportMaxBound);
    UV[1] = min(InUV + float2( 0.25f, -0.25f) * InvSize, InputViewportMaxBound);
    UV[2] = min(InUV + float2(-0.25f,  0.25f) * InvSize, InputViewportMaxBound);
    UV[3] = min(InUV + float2( 0.25f,  0.25f) * InvSize, InputViewportMaxBound);

    Depth.x = SceneTexturesStruct.SceneDepthTexture.SampleLevel( SceneTexturesStruct.SceneDepthTextureSampler, UV[0], 0 ).r;
    Depth.y = SceneTexturesStruct.SceneDepthTexture.SampleLevel( SceneTexturesStruct.SceneDepthTextureSampler, UV[1], 0 ).r;
    Depth.z = SceneTexturesStruct.SceneDepthTexture.SampleLevel( SceneTexturesStruct.SceneDepthTextureSampler, UV[2], 0 ).r;
    Depth.w = SceneTexturesStruct.SceneDepthTexture.SampleLevel( SceneTexturesStruct.SceneDepthTextureSampler, UV[3], 0 ).r;
#else
    vec2 UV = min(InUV + vec2(-0.25f, -0.25f) * InvSize, InputViewportMaxBound);
    Depth = textureGather(DepthBuffer, UV, 0 );
#endif

#else  // STAGE != 0
    const vec2 InUV = m_f4UVAndScreenPos.xy;

#if 0 //COMPILER_GLSL || COMPILER_GLSL_ES2 || COMPILER_GLSL_ES3_1 || FEATURE_LEVEL < FEATURE_LEVEL_SM5
    vec2 UV[4];
    UV[0] = InUV + vec2(-0.25f, -0.25f) * InvSize;
    UV[1] = InUV + vec2( 0.25f, -0.25f) * InvSize;
    UV[2] = InUV + vec2(-0.25f,  0.25f) * InvSize;
    UV[3] = InUV + vec2( 0.25f,  0.25f) * InvSize;

    Depth.x = Texture.SampleLevel( TextureSampler, UV[0], 0 ).r;
    Depth.y = Texture.SampleLevel( TextureSampler, UV[1], 0 ).r;
    Depth.z = Texture.SampleLevel( TextureSampler, UV[2], 0 ).r;
    Depth.w = Texture.SampleLevel( TextureSampler, UV[3], 0 ).r;
#else
    vec2 UV = InUV + vec2(-0.25f, -0.25f) * InvSize;
    Depth = textureGather( DepthBuffer, UV, 0 );
#endif

#endif

    OutColor = max( max(Depth.x, Depth.y), max(Depth.z, Depth.w) );
}