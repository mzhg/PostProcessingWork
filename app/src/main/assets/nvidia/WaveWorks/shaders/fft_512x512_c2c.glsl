#version 430 core

subroutine void Technique();
subroutine uniform Technique technique;

#define COS_PI_4_16 0.70710678118654752440084436210485f
#define TWIDDLE_1_8 COS_PI_4_16, -COS_PI_4_16
#define TWIDDLE_3_8 -COS_PI_4_16, -COS_PI_4_16

#define COHERENCY_GRANULARITY 128

layout (local_size_x = COHERENCY_GRANULARITY) in;

layout (std430, binding = 0) buffer BufferObject
{
	uint thread_count;
	uint ostride;
	uint istride;
	uint pstride;
	float phase_base;
};

layout (rg32f, binding = 1) uniform imageBuffer g_SrcData;
layout (rg32f, binding = 2) uniform imageBuffer g_DstData;

const int d_index[8] = {0, 4, 2, 6, 1, 5, 3, 7};

void FT2(inout vec2 a, inout vec2 b)
{
	float t;

	t = a.x;
	a.x += b.x;
	b.x = t - b.x;

	t = a.y;
	a.y += b.y;
	b.y = t - b.y;
}

void CMUL_forward(inout vec2 a, float bx, float by)
{
	float t = a.x;
	a.x = t * bx - a.y * by;
	a.y = t * by + a.y * bx;
}

void UPD_forward(inout vec2 a, inout vec2 b)
{
	float A = a.x;
	float B = b.y;

	a.x += b.y;
	b.y = a.y + b.x;
	a.y -= b.x;
	b.x = A - B;
}

void FFT_forward_4(inout vec2 D[8])
{
	FT2(D[0], D[2]);
	FT2(D[1], D[3]);
	FT2(D[0], D[1]);

	UPD_forward(D[2], D[3]);
}

void FFT_forward_8(inout vec2 D[8])
{
	FT2(D[0], D[4]);
	FT2(D[1], D[5]);
	FT2(D[2], D[6]);
	FT2(D[3], D[7]);

	UPD_forward(D[4], D[6]);
	UPD_forward(D[5], D[7]);

	CMUL_forward(D[5], TWIDDLE_1_8);
	CMUL_forward(D[7], TWIDDLE_3_8);

	FFT_forward_4(D);
	FT2(D[4], D[5]);
	FT2(D[6], D[7]);
}

void TWIDDLE(inout vec2 d, float phase)
{
	float tx, ty;

//	sincos(phase, ty, tx);
    ty = sin(phase);
    tx = cos(phase); 
	float t = d.x;
	d.x = t * tx - d.y * ty;
	d.y = t * ty + d.y * tx;
}

void TWIDDLE_8(inout vec2 D[8], float phase)
{
	TWIDDLE(D[4], 1 * phase);
	TWIDDLE(D[2], 2 * phase);
	TWIDDLE(D[6], 3 * phase);
	TWIDDLE(D[1], 4 * phase);
	TWIDDLE(D[5], 5 * phase);
	TWIDDLE(D[3], 6 * phase);
	TWIDDLE(D[7], 7 * phase);
}

subroutine (Technique) void Radix008A_CS()
{
    uvec3 thread_id = gl_GlobalInvocationID;
    if (thread_id.x >= thread_count)
        return;

	// Fetch 8 complex numbers
	vec2 D[8];

	uint i;
	uint imod = uint(thread_id.x & (istride - 1));
	uint iaddr = uint(((thread_id.x - imod) << 3) + imod);
	for (i = 0; i < 8; i++)
		D[i] = imageLoad(g_SrcData, int(iaddr + i * istride)).xy;

	// Math
	FFT_forward_8(D);
	uint p = uint(thread_id.x & (istride - pstride));
	float phase = phase_base * float(p);
	TWIDDLE_8(D, phase);

	// Store the result
	uint omod = uint(thread_id.x & (ostride - 1));
	uint oaddr = uint(((thread_id.x - omod) << 3) + omod);
//    g_DstData[oaddr + 0 * ostride] = D[0];
//    g_DstData[oaddr + 1 * ostride] = D[4];
//    g_DstData[oaddr + 2 * ostride] = D[2];
//    g_DstData[oaddr + 3 * ostride] = D[6];
//    g_DstData[oaddr + 4 * ostride] = D[1];
//    g_DstData[oaddr + 5 * ostride] = D[5];
//    g_DstData[oaddr + 6 * ostride] = D[3];
//    g_DstData[oaddr + 7 * ostride] = D[7];

    for(i = 0; i < 8; i++)
    {
       imageStore(g_DstData, int(oaddr + i * ostride), vec4(D[d_index[i]], 0, 0));
    }     
}

subroutine (Technique) void Radix008A_CS2()
{
    uvec3 thread_id = gl_GlobalInvocationID;
    if(thread_id.x >= thread_count)
		return;

	// Fetch 8 complex numbers
	uint i;
	vec2 D[8];
	uint iaddr = thread_id.x << 3;
	for (i = 0; i < 8; i++)
		D[i] = imageLoad(g_SrcData, int(iaddr + i)).xy;

	// Math
	FFT_forward_8(D);

	// Store the result
	uint omod = uint(thread_id.x & (ostride - 1));
	uint oaddr = uint(((thread_id.x - omod) << 3) + omod);
	
	
	for(i = 0; i < 8; i++)
    {
       imageStore(g_DstData, int(oaddr + i * ostride), vec4(D[d_index[i]], 0, 0));
    } 
}

void main()
{
    technique();
}