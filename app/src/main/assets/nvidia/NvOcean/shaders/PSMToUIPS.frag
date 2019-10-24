in vec4 m_f4UVAndScreenPos;

layout(binding = 0) uniform sampler3D g_PSMMap;
out float OutColor;
void main()
{
    int PSMMapW, PSMMapH, PSMMapD;
    PSMMapD = textureSize(g_PSMMap, 0).z;

    float tile_wh = ceil(sqrt(float(PSMMapD)));
    float2 tile_coord = i.uv * tile_wh;
    float2 tile_xy = floor(tile_coord);
    float2 within_tile_xy = frac(tile_coord);

    float slice = (tile_xy.y * tile_wh + tile_xy.x);

    OutColor =texture(g_PSMMap,float3(within_tile_xy,slice/float(PSMMapD)));  // g_SamplerPSMUI
}