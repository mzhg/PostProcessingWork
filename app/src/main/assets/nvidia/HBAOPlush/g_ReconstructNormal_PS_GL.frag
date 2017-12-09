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
uniform sampler2D g_t0;
 in  vec4 VtxGeoOutput1;
vec4 Input1;
out  vec4 PixOutput0;
#define Output0 PixOutput0
vec4 Temp[5];
ivec4 Temp_int[5];
uvec4 Temp_uint[5];
void main()
{
    Input1 = VtxGeoOutput1;
    Temp[0].y = vec4(GlobalConstantBuffer_2.xxxy.z).y;
    Temp[0].z = vec4(0.000000).z;
    Temp[0].xy = vec4(Temp[0].yzyy + Input1.xyxx).xy;
    Temp[0].zw = vec4(GlobalConstantBuffer_3.xyxx.xxxy * Temp[0].xxxy + GlobalConstantBuffer_4.xxxy.zzzw).zw;
    Temp[1].x = (textureLod(g_t0, Temp[0].xy, 0.000000)).x;
    Temp[1].yz = vec4(Temp[0].zzwz * Temp[1].xxxx).yz;
    Temp[0].xy = vec4(GlobalConstantBuffer_3.xyxx.yxyy * Input1.yxyy + GlobalConstantBuffer_4.xxxy.wzww).xy;
    Temp[2].y = (textureLod(g_t0, Input1.xy, 0.000000).yxzw).y;
    Temp[2].xz = vec4(Temp[0].xxyx * Temp[2].yyyy).xz;
    Temp[0].xyz = vec4(Temp[1].xyzx + -Temp[2].yzxy).xyz;
    Temp[0].w = vec4(dot((Temp[0].xyzx).xyz, (Temp[0].xyzx).xyz)).w;
    Temp[1].y = vec4(-GlobalConstantBuffer_2.xxxy.z).y;
    Temp[1].z = vec4(0.000000).z;
    Temp[1].xy = vec4(Temp[1].yzyy + Input1.xyxx).xy;
    Temp[1].zw = vec4(GlobalConstantBuffer_3.xyxx.xxxy * Temp[1].xxxy + GlobalConstantBuffer_4.xxxy.zzzw).zw;
    Temp[3].x = (textureLod(g_t0, Temp[1].xy, 0.000000)).x;
    Temp[3].yz = vec4(Temp[1].zzwz * Temp[3].xxxx).yz;
    Temp[1].xyz = vec4(Temp[2].yzxy + -Temp[3].xyzx).xyz;
    Temp[1].w = vec4(dot((Temp[1].xyzx).xyz, (Temp[1].xyzx).xyz)).w;
    Temp_uint[0].w = ((Temp[0].w)< (Temp[1].w)) ? 0xFFFFFFFFu : 0u;
    if(vec4(Temp_uint[0].wwww).x != 0.0) {
        Temp[0].xyz = vec4(Temp[0].xyzx).xyz;
    } else {
        Temp[0].xyz = vec4(Temp[1].xyzx).xyz;
    }
    Temp[1].z = vec4(0.000000).z;
    Temp[1].x = vec4(GlobalConstantBuffer_2.xxxy.w).x;
    Temp[1].xy = vec4(Temp[1].xzxx + Input1.yxyy).xy;
    Temp[1].zw = vec4(GlobalConstantBuffer_3.xyxx.yyyx * Temp[1].xxxy + GlobalConstantBuffer_4.xxxy.wwwz).zw;
    Temp[3].y = (textureLod(g_t0, Temp[1].yx, 0.000000).yxzw).y;
    Temp[3].xz = vec4(Temp[1].zzwz * Temp[3].yyyy).xz;
    Temp[1].xyz = vec4(-Temp[2].xyzx + Temp[3].xyzx).xyz;
    Temp[0].w = vec4(dot((Temp[1].xyzx).xyz, (Temp[1].xyzx).xyz)).w;
    Temp[3].z = vec4(0.000000).z;
    Temp[3].x = vec4(-GlobalConstantBuffer_2.xxxy.w).x;
    Temp[3].xy = vec4(Temp[3].xzxx + Input1.yxyy).xy;
    Temp[3].zw = vec4(GlobalConstantBuffer_3.xyxx.yyyx * Temp[3].xxxy + GlobalConstantBuffer_4.xxxy.wwwz).zw;
    Temp[4].y = (textureLod(g_t0, Temp[3].yx, 0.000000).yxzw).y;
    Temp[4].xz = vec4(Temp[3].zzwz * Temp[4].yyyy).xz;
    Temp[2].xyz = vec4(Temp[2].xyzx + -Temp[4].xyzx).xyz;
    Temp[1].w = vec4(dot((Temp[2].xyzx).xyz, (Temp[2].xyzx).xyz)).w;
    Temp_uint[0].w = ((Temp[0].w)< (Temp[1].w)) ? 0xFFFFFFFFu : 0u;
    if(vec4(Temp_uint[0].wwww).x != 0.0) {
        Temp[1].xyz = vec4(Temp[1].xyzx).xyz;
    } else {
        Temp[1].xyz = vec4(Temp[2].xyzx).xyz;
    }
    Temp[2].xyz = vec4(Temp[0].xyzx * Temp[1].xyzx).xyz;
    Temp[0].xyz = vec4(Temp[0].zxyz * Temp[1].yzxy + -Temp[2].xyzx).xyz;
    Temp[0].w = vec4(dot((Temp[0].xyzx).xyz, (Temp[0].xyzx).xyz)).w;
    Temp[0].w = vec4(inversesqrt(Temp[0].w)).w;
    Temp[0].xyz = vec4(Temp[0].wwww * Temp[0].xyzx).xyz;
    Output0.xyz = vec4(Temp[0].xyzx * vec4(0.500000, 0.500000, 0.500000, 0.000000) + vec4(0.500000, 0.500000, 0.500000, 0.000000)).xyz;
    Output0.w = vec4(0.000000).w;
    return;
}