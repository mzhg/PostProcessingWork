#include "OceanLODData.glsl"

in Varyings
{
    float2 worldPos;
    float3 uv_slice;
}_input;

layout(location = 0) out float4 OutColor;

// IMPORTANT - this mirrors the constant with the same name in ShapeGerstnerBatched.cs, both must be updated together!
#define BATCH_SIZE 32
#define PI 3.141593

#define MAX_FFT_RESOLUTION 512
#define WARP_WIDTH 8 // minimum number of threads which execute in lockstep

// constants, needs to match struct in FFT_Simulation_DirectCompute.cpp
layout(binding=0) uniform MyConstantBuffer
{
    uint m_resolution;
    uint m_resolution_plus_one;
    uint m_half_resolution;
    uint m_half_resolution_plus_one;

    uint m_resolution_plus_one_squared_minus_one;
    uint m_32_minus_log2_resolution;
    uint2 m_ipad2;

    float m_window_in;
    float m_window_out;

    float2 m_wind_dir;
    float m_frequency_scale;
    float m_linear_scale;
    float m_wind_scale;
    float m_root_scale;
    float m_power_scale;

    float m_time;

    float m_choppy_scale;
};

void main()
{
    float nx = _input.worldPos.x;
    float ny = _input.worldPos.y;
    float nr = sqrt(float(nx*nx + ny*ny));

//    if((nx !=0 || ny != 0) && nr >= m_window_in && nr < m_window_out)
    {
        float2 k = float2(nx * m_frequency_scale, ny * m_frequency_scale);

        float kSqr = k.x * k.x + k.y * k.y;
        float kCos = k.x * m_wind_dir.x + k.y * m_wind_dir.y;

        float scale = m_linear_scale * kCos * rsqrt(kSqr * kSqr * kSqr);

        if (kCos < 0)
        scale *= m_wind_scale;

        float amplitude = scale * exp(m_power_scale * kSqr + m_root_scale / kSqr);

        // Gerternal Wave

    }

    OutColor = float4(_input.worldPos.x, amplitude, _input.worldPos.y, 1);
}