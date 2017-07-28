#version 420 core

layout(location = 0) out vec4 fragColor;

// Textures and sampling states
layout(binding = 0) uniform sampler2D g_samplerDisplacementMap;

// #define USE_CONST

#define PI 3.1415926536f
#define BLOCK_SIZE_X 16
#define BLOCK_SIZE_Y 16

#ifdef USE_CONST

const int g_ActualDim;
const int g_InWidth;
const int g_OutWidth;
const int g_OutHeight;
const int g_DxAddressOffset;
const int g_DyAddressOffset;

#else
uniform int g_ActualDim;
uniform int g_InWidth;
uniform int g_OutWidth;
uniform int g_OutHeight;
uniform int g_DxAddressOffset;
uniform int g_DyAddressOffset;

#endif

uniform float g_Time;
uniform float g_ChoppyScale;
uniform float g_GridLen;

// The following three should contains only real numbers. But we have only C2C FFT now.
layout (rg32f, binding = 0) uniform imageBuffer g_InputDxyz;

subroutine void Technique();
subroutine uniform Technique technique;

in vec2 v_texcoords;

subroutine (Technique) void UpdateDisplacementPS()
{
    uint index_x = uint(v_texcoords.x * float(g_OutWidth));
	uint index_y = uint(v_texcoords.y * float(g_OutHeight));
	uint addr = g_OutWidth * index_y + index_x;

	// cos(pi * (m1 + m2))
	float sign_correction = (((index_x + index_y) & 1u)!=0) ? -1.0 : 1.0;

	float dx = imageLoad(g_InputDxyz, int(addr + g_DxAddressOffset)).r * sign_correction * g_ChoppyScale;
	float dy = imageLoad(g_InputDxyz, int(addr + g_DyAddressOffset)).r * sign_correction * g_ChoppyScale;
	float dz = imageLoad(g_InputDxyz, int(addr)).r * sign_correction;
	
	fragColor = vec4(dx, dy, dz, 1);
}

subroutine (Technique) void GenGradientFoldingPS()
{
    // Sample neighbour texels
	vec2 one_texel = vec2(1.0f / float(g_OutWidth), 1.0f / float(g_OutHeight));

	vec2 tc_left  = vec2(v_texcoords.x - one_texel.x, v_texcoords.y);
	vec2 tc_right = vec2(v_texcoords.x + one_texel.x, v_texcoords.y);
	vec2 tc_back  = vec2(v_texcoords.x, v_texcoords.y - one_texel.y);
	vec2 tc_front = vec2(v_texcoords.x, v_texcoords.y + one_texel.y);

	vec3 displace_left  = texture(g_samplerDisplacementMap, tc_left).xyz;
	vec3 displace_right = texture(g_samplerDisplacementMap, tc_right).xyz;
	vec3 displace_back  = texture(g_samplerDisplacementMap, tc_back).xyz;
	vec3 displace_front = texture(g_samplerDisplacementMap, tc_front).xyz;
	
	// Do not store the actual normal value. Using gradient instead, which preserves two differential values.
	vec2 gradient = {-(displace_right.z - displace_left.z), -(displace_front.z - displace_back.z)};
	

	// Calculate Jacobian corelation from the partial differential of height field
	vec2 Dx = (displace_right.xy - displace_left.xy) * g_ChoppyScale * g_GridLen;
	vec2 Dy = (displace_front.xy - displace_back.xy) * g_ChoppyScale * g_GridLen;
	float J = (1.0f + Dx.x) * (1.0f + Dy.y) - Dx.y * Dy.x;

	// Practical subsurface scale calculation: max[0, (1 - J) + Amplitude * (2 * Coverage - 1)].
	float fold = max(1.0f - J, 0.0);
	
	fragColor = vec4(gradient, 0, fold);
}

void main()
{
    technique();
}
