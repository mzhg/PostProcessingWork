uniform float2	g_WakeTexScale = float2(0.0028,0.0028);
uniform float2	g_WakeTexOffset = float2(0.82,0.5);

/*SamplerComparisonState g_samplerShadow
{
    Filter = COMPARISON_MIN_MAG_LINEAR_MIP_POINT;
    AddressU = Border;
    AddressV = Border;
    BorderColor = float4(1, 1, 1, 1);
    ComparisonFunc = LESS;
};*/

float RND_1d(float2 x)
{
    uint n = asuint(x.y * 6435.1392 + x.x * 45.97345);
    n = (n<<13)^n;
    n = n * (n*n*15731u + 789221u) + 1376312589u;
    n = (n>>9u) | 0x3F800000u;

    return 2.0 - asfloat(n);
}

const float2 g_SamplePositions[] = float2[](
    // Poisson disk with 16 points
    float2(-0.3935238f, 0.7530643f),
    float2(-0.3022015f, 0.297664f),
    float2(0.09813362f, 0.192451f),
    float2(-0.7593753f, 0.518795f),
    float2(0.2293134f, 0.7607011f),
    float2(0.6505286f, 0.6297367f),
    float2(0.5322764f, 0.2350069f),
    float2(0.8581018f, -0.01624052f),
    float2(-0.6928226f, 0.07119545f),
    float2(-0.3114384f, -0.3017288f),
    float2(0.2837671f, -0.179743f),
    float2(-0.3093514f, -0.749256f),
    float2(-0.7386893f, -0.5215692f),
    float2(0.3988827f, -0.617012f),
    float2(0.8114883f, -0.458026f),
    float2(0.08265103f, -0.8939569f)
);

float GetShadowValue(sampler2DShadow shadowMap, float4x4 lightMatrix, float3 fragmentPos, bool simpleShadows=false)
{
    float4 clipPos = mul(float4(fragmentPos, 1.0f), lightMatrix);

//    clipPos.z *= 0.99999;
    clipPos.xyz /= clipPos.w;
//    clipPos.x = clipPos.x * 0.5f + 0.5f;
//    clipPos.y = 0.5f - clipPos.y * 0.5f;
    clipPos.xyz = 0.5 * clipPos.xyz + 0.5;
    clipPos.z -= 0.0001;

    if (    clipPos.x < 0 || clipPos.x > 1 ||
            clipPos.y < 0 || clipPos.y > 1 )
    {
        return 0;
    }

	if (simpleShadows)
	{
		return textureLod(shadowMap, clipPos.xyz, 0.0);   //g_samplerShadow
	}

    float shadow = 0;
    float totalWeight = 0;

    for(int nSample = 0; nSample < 16; ++nSample)
    {
        float2 offset = g_SamplePositions[nSample];
        float weight = 1.0;
        offset *= (1.0f / kSpotlightShadowResolution) * 2.25;
        float _sample = //shadowMap.SampleCmpLevelZero(g_samplerShadow, clipPos.xy + offset, clipPos.z);
                        textureLod(shadowMap, float3(clipPos.xy + offset, clipPos.z), 0);
        shadow += _sample * weight;
        totalWeight += weight;
    }

    shadow /= totalWeight;

    return shadow;
}

