

layout (triangles) in;
layout (triangle_strip, max_vertices = 6) out;

layout(binding = 0) uniform FrameCB
{
    mat4 g_ViewProj;
    mat4 g_Model;
    mat4 g_LightViewProj;
    vec4 g_LightPos;

    float g_LightZNear;
    float g_LightZFar;
//    float g_NormScale;
};

in vec3 m_WorldPos[];
in vec3 m_WorldNormal[];

////////////////////////////////////////////////////////////////////////////////
// Geometry Shader

vec3 ParaboloidProject(vec3 P, float zNear, float zFar)
{
	vec3 outP;
	float lenP = length(P.xyz);
	outP.xyz = P.xyz/lenP;
	outP.x = outP.x / (outP.z + 1);
	outP.y = outP.y / (outP.z + 1);			
	outP.z = (lenP - zNear) / (zFar - zNear);
	outP.z = 2 * outP.z - 1;
	return outP;
}

void GenerateOmniTriangle(int target, vec4 vA, vec4 vB, vec4 vC/*, inout TriangleStream<GS_OUTPUT> output*/)
{
//    GS_OUTPUT outValue;
    gl_Layer = target;
    gl_Position = vec4(ParaboloidProject(vA.xyz, g_LightZNear, g_LightZFar), 1);
    EmitVertex();
    
    gl_Position = vec4(ParaboloidProject(vB.xyz, g_LightZNear, g_LightZFar), 1);
	EmitVertex();
	
    gl_Position = vec4(ParaboloidProject(vC.xyz, g_LightZNear, g_LightZFar), 1);
	EmitVertex();
	
    EndPrimitive();
}

void main()
{
    vec4 v0 = g_LightViewProj * vec4(m_WorldPos[0], 1);  v0 /= v0.w;
    vec4 v1 = g_LightViewProj * vec4(m_WorldPos[1], 1);  v1 /= v1.w;
    vec4 v2 = g_LightViewProj * vec4(m_WorldPos[2], 1);  v2 /= v2.w;

	float minZ = min(v0.z, min(v1.z, v2.z));
	float maxZ = max(v0.z, max(v1.z, v2.z));

    if (maxZ >= 0)
    {
        GenerateOmniTriangle(0, v2, v1, v0);
    }

    if (minZ <= 0)
    {
        v0.z *= -1.0;
        v1.z *= -1.0;
        v2.z *= -1.0;
        GenerateOmniTriangle(1, v0, v1, v2);
    }
}