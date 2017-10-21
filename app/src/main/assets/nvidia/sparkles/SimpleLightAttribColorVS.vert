#version 120
attribute vec4 In_Position;
attribute vec3 In_Normal;
attribute vec2 In_Texcoord;
attribute vec4 In_Color;

uniform mat4 g_Model;
uniform mat4 g_ModelViewProj;

varying vec3 m_PositionWS;
varying vec3 m_NormalWS;
varying vec2 m_Texcoord;
varying vec4 m_Color;

void main()
{
    m_PositionWS = (g_Model * In_Position).xyz;
    m_NormalWS = mat3(g_Model) * In_Normal;
    gl_Position = g_ModelViewProj * In_Position;
    m_Texcoord = In_Texcoord;
    m_Color = In_Color;
}