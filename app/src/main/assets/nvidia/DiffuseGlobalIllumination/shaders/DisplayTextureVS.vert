layout(location = 0) in vec4 In_Position;  // 2D slice vertex coordinates in clip space
layout(location = 1) in vec3 In_Texcoord;  // 3D cell coordinates (x,y,z in 0-dimension range)

out vec3 m_Tex;

out gl_PerVertex
{
    vec4 gl_Position;
};

layout(binding = 0) uniform cbConstantsLPVinitialize
{
    float g_3DWidth        /*: packoffset(c0.x)*/;
    float g_3DHeight       /*: packoffset(c0.y)*/;
    float g_3DDepth        /*: packoffset(c0.z)*/;
    int temp               /*: packoffset(c0.w)*/;
};

void main()
{
    gl_Position = In_Position;
    m_Tex = vec3( (In_Texcoord.x)/g_3DWidth,
                             (In_Texcoord.y)/g_3DHeight,
                             (In_Texcoord.z+0.5));
}