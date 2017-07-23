#include "../PostProcessingHLSLCompatiable.glsl"

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

	float m_window_in;
	float m_window_out;

	float2 m_wind_dir;
	float m_frequency_scale;
	float m_linear_scale;
	float m_wind_scale;
	float m_root_scale;
	float m_power_scale;

	double m_time;

	float m_choppy_scale;
};

