in vec4 m_f4UVAndScreenPos;

layout(binding = 0) uniform sampler2D g_VelocityHeightMap;

uniform vec4 g_TextureSize;  // xy: texture size; zw:texel size
uniform vec4 g_DropInfos[4];  // xy: posiion; z: radius; w:strength
uniform int g_DropCount;      //
uniform float g_TimeScale;   //
uniform float g_damping = 0.99;

#define STRATEGY_UPDATE_MAP4  1// update the velocity height map
#define STRATEGY_UPDATE_MAP8  2//
#define STRATEGY_ADD_DROP  3  //
#define STRATEGY_UPDATE_MAP4_ADD_DROP  4
#define STRATEGY_UPDATE_MAP8_ADD_DROP  5

/*#ifndef STRATEGY_PROFILE
#define STRATEGY_PROFILE STRATEGY_UPDATE_MAP4_ADD_DROP
#endif*/

out vec4 Out_Color;

void main()
{
    vec2 vel_hei = textureLod(g_VelocityHeightMap, m_f4UVAndScreenPos.xy, 0.0).xy;

    float force = 0.0;
#if (STRATEGY_PROFILE != STRATEGY_ADD_DROP)
    // TODO Using the mehod 'textureLodOffset' or 'textureGather' may improve the performance.
    force += textureLod(g_VelocityHeightMap, m_f4UVAndScreenPos.xy + vec2(g_TextureSize.z, 0), 0.0).y - vel_hei.y;
    force += textureLod(g_VelocityHeightMap, m_f4UVAndScreenPos.xy - vec2(g_TextureSize.z, 0), 0.0).y - vel_hei.y;
    force += textureLod(g_VelocityHeightMap, m_f4UVAndScreenPos.xy + vec2(0, g_TextureSize.w), 0.0).y - vel_hei.y;
    force += textureLod(g_VelocityHeightMap, m_f4UVAndScreenPos.xy - vec2(0, g_TextureSize.w), 0.0).y - vel_hei.y;
#endif

#if ((STRATEGY_PROFILE == STRATEGY_UPDATE_MAP8)||(STRATEGY_PROFILE == STRATEGY_UPDATE_MAP8_ADD_DROP))
    const float SQRT2_INV = 0.707107;
    force += SQRT2_INV * (textureLod(g_VelocityHeightMap, m_f4UVAndScreenPos.xy + vec2(g_TextureSize.z, g_TextureSize.w), 0.0).y - vel_hei.y);
    force += SQRT2_INV * (textureLod(g_VelocityHeightMap, m_f4UVAndScreenPos.xy + vec2(g_TextureSize.z, -g_TextureSize.w), 0.0).y - vel_hei.y);
    force += SQRT2_INV * (textureLod(g_VelocityHeightMap, m_f4UVAndScreenPos.xy + vec2(-g_TextureSize.z, g_TextureSize.w), 0.0).y - vel_hei.y);
    force += SQRT2_INV * (textureLod(g_VelocityHeightMap, m_f4UVAndScreenPos.xy + vec2(-g_TextureSize.z, -g_TextureSize.w), 0.0).y - vel_hei.y);

    force *= 0.125;
#else
    force *= 0.25;
#endif

#if (STRATEGY_PROFILE != STRATEGY_ADD_DROP)
    vel_hei.x += force /** g_TimeScale*/;   // updat the velocity
    vel_hei.x *= g_damping;
    vel_hei.y += vel_hei.x /** g_TimeScale*/;  // update the position
#endif

#if (STRATEGY_PROFILE >= STRATEGY_ADD_DROP)
    float prop = g_TextureSize.x /g_TextureSize.y;
    vec2 coords = vec2(m_f4UVAndScreenPos.x, m_f4UVAndScreenPos.y/prop);
    for(int i = 0; i < g_DropCount; i++)
    {
        vec2 center = vec2(g_DropInfos[i].x, g_DropInfos[i].y/prop);
        float radius = g_DropInfos[i].z;
        float strength = g_DropInfos[i].w;

        float d = distance(coords, center);
        vel_hei.y += strength * max(radius - d, 0.0);

       /* d = (d / radius) * 3.0;
        vel_hei.y += exp(-d*d) * strength;*/
    }
#endif

    Out_Color = vec4(vel_hei,0, 0);
}