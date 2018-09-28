#ifndef RF_NUM_BONES
#define RF_NUM_BONES 0
#endif

#ifndef RF_NUM_WEIGHT_BONES
#define RF_NUM_WEIGHT_BONES 0
#endif

#ifndef RF_ENABLE_INSTANCE
#define RF_ENABLE_INSTANCE 0
#endif

#ifndef RF_ENABLE_BLENDSHAPE
#define RF_ENABLE_BLENDSHAPE 0
#endif

#if GL_ES   // opengl es
#if __VERSION__ >= 300
#define OUT_ATTRIB  out
    #if __VERSION__ >= 310
    #define IN_ATTRIB(loc)  layout(location = loc) in
    #else
    #define IN_ATTRIB(loc)  in
    #endif
#else  // __VERSION__ < 300
#define OUT_ATTRIB  varying
#define IN_ATTRIB(loc)  attribute
#endif

#else   // opengl desktop
    #if __VERSION__ >= 410
    #define IN_ATTRIB(loc)  layout(location = loc) in
    #define OUT_ATTRIB  out
    #elif __VERSION__ >= 130
    #define IN_ATTRIB(loc)  in
    #define OUT_ATTRIB  out
    #else
    #define IN_ATTRIB(loc)  attribute
    #define OUT_ATTRIB  varying
    #endif
#endif

IN_ATTRIB(0) vec3 In_Position;
IN_ATTRIB(1) vec3 In_Normal;
IN_ATTRIB(2) vec3 In_Tangent;
IN_ATTRIB(3) vec3 In_Bitangent;
IN_ATTRIB(4) vec4 In_Color;
IN_ATTRIB(5) vec4 In_Texcoord;
IN_ATTRIB(6) vec4 In_Texcoord1;  // texcoord1- 4 for the bone weights or instance attributes or blendshape
IN_ATTRIB(7) vec4 In_Texcoord2;
IN_ATTRIB(8) vec4 In_Texcoord3;
IN_ATTRIB(9) vec4 In_Texcoord4;

uniform mat4 g_Model;
uniform mat4 g_ViewProj;
uniform mat4 g_LightViewProj;

#if RF_NUM_WEIGHT_BONES > 0

uniform mat4 g_Bones[RF_NUM_BONES];

mat4 accumulate_bones(vec4 weights)
{
    float weight0 = weights[0];
    int index0 = int(weights[1]);

    mat4 bone_mat = g_Bones[index0] * weight0;

    float weight1 = weights[2];
    int index1 = int(weights[3]);

    bone_mat += g_Bones[index0] * weight0;

    return bone_mat;
}
#endif

OUT_ATTRIB vec4 m_Color;
OUT_ATTRIB vec4 m_PositionWS;
OUT_ATTRIB vec3 m_NormalWS;
OUT_ATTRIB vec3 m_TangentWS;
OUT_ATTRIB vec3 m_BitangentWS;
OUT_ATTRIB vec2 m_Texcoord;
OUT_ATTRIB vec4 m_PositionLS;

#if RF_ENABLE_INSTANCE
flat OUT_ATTRIB int m_InstanceID;
#endif

void compute_blend_vertex(out vec3 position, out vec3 normal, out vec3 tangent, out vec3 bitangent)
{
#if RF_ENABLE_BLENDSHAPE
    vec3 real_pos = In_Position + In_Texcoord1.xyz;
#else
    vec3 real_pos = In_Position;
#endif

#if RF_NUM_WEIGHT_BONES > 0
    mat4 bone_mat = accumulate_bones(In_Texcoord1);

    #if RF_NUM_WEIGHT_BONES > 2
        bone_mat += accumulate_bones(In_Texcoord2);
    #endif

    #if RF_NUM_WEIGHT_BONES > 4
        bone_mat += accumulate_bones(In_Texcoord3);
    #endif

    #if RF_NUM_WEIGHT_BONES > 6
        bone_mat += accumulate_bones(In_Texcoord4);
    #endif

    position = (bone_mat * vec4(real_pos, 1)).xyz;
    normal = (bone_mat * vec4(In_Normal, 0)).xyz;
    tangent = (bone_mat * vec4(In_Tangent, 0)).xyz;
    bitangent = (bone_mat * vec4(In_Bitangent, 0)).xyz;
#else
    position = real_pos;
    normal = In_Normal;
    tangent = In_Tangent;
    bitangent = In_Bitangent;
#endif
}

mat4 makeInstanceTransform(vec4 pos, vec4 scale, vec4 rot)
{
    mat4 mat = mat4(1);

    float q0 = rot.w;
    float q1 = rot.x;
    float q2 = rot.y;
    float q3 = rot.z;

    float q00 = q0 * q0;
    float q11 = q1 * q1;
    float q22 = q2 * q2;
    float q33 = q3 * q3;

    // Diagonal elements
    mat[0][0] = q00 + q11 - q22 - q33;
    mat[1][1] = q00 - q11 + q22 - q33;
    mat[2][2] = q00 - q11 - q22 + q33;
    // 0,1 and 1,0 elements
    float q03 = q0 * q3;
    float q12 = q1 * q2;
    mat[1][0] = 2.0 * (q12 - q03);
    mat[0][1] = 2.0 * (q03 + q12);
    // 0,2 and 2,0 elements
    float q02 = q0 * q2;
    float q13 = q1 * q3;
    mat[2][0] = 2.0 * (q02 + q13);
    mat[0][2] = 2.0 * (q13 - q02);
    // 1,2 and 2,1 elements
    float q01 = q0 * q1;
    float q23 = q2 * q3;
    mat[2][1] = 2.0 * (q23 - q01);
    mat[1][2] = 2.0 * (q01 + q23);

    return mat;
}

void main()
{

    m_Color = In_Color;
    vec3 position;
    vec3 normal;
    vec3 tangent;
    vec3 bitangent;

    compute_blend_vertex(position, normal, tangent, bitangent);

#if  RF_ENABLE_INSTANCE
    m_InstanceID = gl_InstanceID;
    mat4 model_mat = makeInstanceTransform(In_Texcoord1, In_Texcoord2, In_Texcoord3);
#else
    mat4 model_mat = g_Model;
#endif

    m_PositionWS = model_mat * vec4(position, 1);
    m_NormalWS = (model_mat * vec4(normal, 0)).xyz;
    m_TangentWS = (model_mat * vec4(tangent, 0)).xyz;
    m_BitangentWS = (model_mat * vec4(bitangent, 0)).xyz;

    m_Texcoord = In_Texcoord.xy;

    gl_Position = g_ViewProj * m_PositionWS;
    m_PositionLS = g_LightViewProj * m_PositionLS;
}