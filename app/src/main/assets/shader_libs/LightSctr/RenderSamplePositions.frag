

in SRenderSamplePositionsGS_Output
{
	vec3 f3Color;
	vec2 f2PosXY;
	vec4 f4QuadCenterAndSize;
}In;

layout(location = 0) out vec4 OutColor;

void main()
{
	OutColor = vec4(In.f3Color, 1 - pow( length( (In.f2PosXY - In.f4QuadCenterAndSize.xy) / In.f4QuadCenterAndSize.zw),4.0) );
}