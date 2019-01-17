#include "WavePackets.glsl"


in PS_INPUT_PACKET
{
//	float4 oPosition	 : SV_POSITION;
	float4 pos			 /*: TEXTURE0*/;
	float4 Att			 /*: TEXTURE1*/;
	float4 Att2			 /*: TEXTURE2*/;
}In;

out vec4 oColor;

// rasterize wave packet quad
// wave packet data:
// position vector: x,y = [-1..1], position in envelope
// attribute vector: x=amplitude, y=wavelength, z=time phase, w=envelope size
// attribute2 vector: (x,y)=position of bending point, z=central distance to ref point, 0
void main()
{
    float centerDiff = length(In.pos.zw - float2(0.0f, In.Att2.x)) - abs(In.pos.w - In.Att2.x);   //	centerDiff = 0; -> straight waves
    float phase = -In.Att.z + (In.pos.w + centerDiff)*2.0*PI / In.Att.y;
    float3 rippleAdd =	1.0*(1.0 + cos(In.pos.x*PI)) *(1.0 + cos(In.pos.y*PI))*In.Att.x // gaussian envelope
                        * float3(0, cos(phase), 0);									 	 // ripple function
    oColor.xyzw = float4(rippleAdd, 1.0);
}