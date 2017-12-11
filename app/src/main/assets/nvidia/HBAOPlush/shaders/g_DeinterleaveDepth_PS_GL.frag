#version 150
struct vec1 {
	float x;
};
struct uvec1 {
	uint x;
};
struct ivec1 {
	int x;
};
layout(std140) uniform;
uniform GlobalConstantBuffer {
 	uvec4 GlobalConstantBuffer_0;
	vec2 GlobalConstantBuffer_1;
	vec2 GlobalConstantBuffer_2;
	vec2 GlobalConstantBuffer_3;
	vec2 GlobalConstantBuffer_4;
	float GlobalConstantBuffer_5;
	float GlobalConstantBuffer_6;
	float GlobalConstantBuffer_7;
	float GlobalConstantBuffer_8;
	float GlobalConstantBuffer_9;
	float GlobalConstantBuffer_10;
	float GlobalConstantBuffer_11;
	int GlobalConstantBuffer_12;
	float GlobalConstantBuffer_13;
	float GlobalConstantBuffer_14;
	float GlobalConstantBuffer_15;
	float GlobalConstantBuffer_16;
	float GlobalConstantBuffer_17;
	float GlobalConstantBuffer_18;
	float GlobalConstantBuffer_19;
	float GlobalConstantBuffer_20;
	vec2 GlobalConstantBuffer_21;
	float GlobalConstantBuffer_22;
	float GlobalConstantBuffer_23;
	float GlobalConstantBuffer_24;
	float GlobalConstantBuffer_25;
	int GlobalConstantBuffer_26;
	vec4 GlobalConstantBuffer_27[4];
	float GlobalConstantBuffer_28;
	float GlobalConstantBuffer_29;
};
struct PerPassConstantBuffer_0_Type {
	vec4 f4Jitter;
	vec2 f2Offset;
	float fSliceIndex;
	uint uSliceIndex;
};
uniform PerPassConstantBuffer {
 	PerPassConstantBuffer_0_Type PerPassConstantBuffer_0;
};
uniform sampler2D g_t0;
vec4 Input0;
out  vec4 PixOutput0;
#define Output0 PixOutput0
out  vec4 PixOutput1;
#define Output1 PixOutput1
out  vec4 PixOutput2;
#define Output2 PixOutput2
out  vec4 PixOutput3;
#define Output3 PixOutput3
out  vec4 PixOutput4;
#define Output4 PixOutput4
out  vec4 PixOutput5;
#define Output5 PixOutput5
out  vec4 PixOutput6;
#define Output6 PixOutput6
out  vec4 PixOutput7;
#define Output7 PixOutput7
vec4 Temp[1];
ivec4 Temp_int[1];
uvec4 Temp_uint[1];
void main()
{
    Input0.xy = gl_FragCoord.xy;
    Temp[0].xy = vec4(floor(Input0.xyxx)).xy;
    Temp[0].xy = vec4(Temp[0].xyxx * vec4(4.000000, 4.000000, 0.000000, 0.000000) + PerPassConstantBuffer_0.f2Offset.xyxx.xyxx).xy;
    Temp[0].xy = vec4(Temp[0].xyxx * GlobalConstantBuffer_2.xxxy.zwzz).xy;
    Temp[0].z = (texture(g_t0, Temp[0].xy).yzxw).z;
    Output0.x = vec4(Temp[0].z).x;
    Temp[0].z = (textureOffset(g_t0, Temp[0].xy, ivec2(1, 0)).yzxw).z;
    Output1.x = vec4(Temp[0].z).x;
    Temp[0].z = (textureOffset(g_t0, Temp[0].xy, ivec2(2, 0)).yzxw).z;
    Output2.x = vec4(Temp[0].z).x;
    Temp[0].z = (textureOffset(g_t0, Temp[0].xy, ivec2(3, 0)).yzxw).z;
    Output3.x = vec4(Temp[0].z).x;
    Temp[0].z = (textureOffset(g_t0, Temp[0].xy, ivec2(0, 1)).yzxw).z;
    Output4.x = vec4(Temp[0].z).x;
    Temp[0].z = (textureOffset(g_t0, Temp[0].xy, ivec2(1, 1)).yzxw).z;
    Output5.x = vec4(Temp[0].z).x;
    Temp[0].z = (textureOffset(g_t0, Temp[0].xy, ivec2(2, 1)).yzxw).z;
    Temp[0].x = (textureOffset(g_t0, Temp[0].xy, ivec2(3, 1))).x;
    Output7.x = vec4(Temp[0].x).x;
    Output6.x = vec4(Temp[0].z).x;
    return;
}