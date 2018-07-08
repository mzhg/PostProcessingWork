#include "Voxelizer.glsl"

layout (triangles) in;
layout (triangle_strip, max_vertices = 3) out;

in VsResOutput
{
    float4 Pos          /*: POSITION*/;
    float3 Tex          /*: TEXCOORD*/;
}_inputs[];


out vec3 m_Tex;

void main()
{
//    GsResOutput output;
//    output.RTIndex = input[0].Tex.z;
    gl_Layer = int(_inputs[0].Tex.z);
    for(int v=0; v<3; v++)
    {
        gl_Position = _inputs[v].Pos;
        m_Tex = _inputs[v].Tex;

        EmitVertex();
//        triStream.Append( output );
    }
//    triStream.RestartStrip( );
}