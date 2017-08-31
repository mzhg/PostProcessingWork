#include "LightningCommon.glsl"

layout(location=0) out float4 Out_Color;

const int blur_search_width = 8;

const int blur_search_start = -blur_search_width;
const int blur_search_end = blur_search_width + 1;
const float  blur_scale = 2.0f;

uniform vec3 BlurSigma;
uniform bool horizontal;
uniform vec2 buffer_texel_size;
layout(binding = 0) uniform sampler2D Buffer;

float Gaussian(float2 xy, float sigma)
{
	return exp(- (dot(xy,xy) / (2.0f * sigma * sigma ))) / (2.0f * pi * sigma * sigma);
}

float3 Gaussian(float2 xy, float3 sigma)
{
	float3 sigma_prime = sigma * sigma * 2;
	float3 d = dot(xy,xy);

	return	exp(- d / sigma_prime) / ( pi * sigma_prime);

}
float3 Gaussian(float d, float3 sigma)
{
	float3 sigma_prime = sigma * sigma * 2;
	return	exp(- abs(d) / sigma_prime) / ( pi * sigma_prime);
}

in vec4 m_f4UVAndScreenPos;

void main()
{
    float4 sum =  float4(0,0,0,0);

    if(horizontal)
    {

        for(int i = blur_search_start;  i < blur_search_end; ++i)
            sum.rgb += Gaussian(i, BlurSigma) * textureLod(Buffer, m_f4UVAndScreenPos.xy + buffer_texel_size * float2(0.5f + 2.0f * i,0.5f) ,0).rgb;  // LinearSample
    }
    else
    {
        for(int i = blur_search_start;  i < blur_search_end; ++i)
            sum.rgb += Gaussian(i, BlurSigma) * textureLod(Buffer, m_f4UVAndScreenPos.xy + buffer_texel_size * float2(0.5f, 0.5f + 2 * i) ,0).rgb;

    }

    Out_Color = blur_scale * sum;
}