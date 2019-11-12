in vec4 m_f4UVAndScreenPos;

layout(binding = 0) uniform sampler3D g_PSMMap;
out vec4 OutColor;
void main()
{
    int PSMMapW, PSMMapH, PSMMapD;
    PSMMapD = textureSize(g_PSMMap, 0).z;

    float tile_wh = ceil(sqrt(float(PSMMapD)));
    vec2 tile_coord = m_f4UVAndScreenPos.xy * tile_wh;
    vec2 tile_xy = floor(tile_coord);
    vec2 within_tile_xy = fract(tile_coord);

    float slice = (tile_xy.y * tile_wh + tile_xy.x);

    OutColor =texture(g_PSMMap,vec3(within_tile_xy,slice/float(PSMMapD)));  // g_SamplerPSMUI
}