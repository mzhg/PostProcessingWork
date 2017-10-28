layout(location = 0) in float3 In_Position;
layout(location = 1) in float4 In_Color;

uniform float4 dim;
uniform mat4 Proj /*: state.matrix.projection,*/;
uniform mat4 Model /*: state.matrix.modelview*/;

out float4 m_color;

void main()
{
	float2 z_offset;

	float4 v = 0.5*float4(In_Position,0);
	v = mul( Model,v );
	v += 0.5*dim;

	v.z = round(v.z);
	z_offset.y = floor(v.z / dim.w);
	z_offset.x = v.z - z_offset.y*dim.w;

	v = float4(v.xy + z_offset*dim.xy,0,1);
	gl_Position = mul(Proj,v);
	m_color = In_Color;
}