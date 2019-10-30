layout(location = 0) in vec4 In_Position;

uniform mat4 g_Local;
uniform mat4 g_View;
uniform mat4 g_Proj;

uniform vec4 _InstanceData;
uniform vec4 _GeomData;
uniform vec3 _OceanCenterPosWorld;

uniform vec4 _LD_Pos_Scale[8];

uniform int _LD_SliceIndex;

float ComputeLodAlpha(vec3 i_worldPos, float i_meshScaleAlpha)
{
    // taxicab distance from ocean center drives LOD transitions
    vec2 offsetFromCenter = abs(vec2(i_worldPos.x - _OceanCenterPosWorld.x, i_worldPos.z - _OceanCenterPosWorld.z));
    float taxicab_norm = max(offsetFromCenter.x, offsetFromCenter.y);

    // interpolation factor to next lod (lower density / higher sampling period)
    float lodAlpha = taxicab_norm / _LD_Pos_Scale[_LD_SliceIndex].z - 1.0;

    // lod alpha is remapped to ensure patches weld together properly. patches can vary significantly in shape (with
    // strips added and removed), and this variance depends on the base density of the mesh, as this defines the strip width.
    // using .15 as black and .85 as white should work for base mesh density as low as 16.
    const float BLACK_POINT = 0.15, WHITE_POINT = 0.85;
    lodAlpha = max((lodAlpha - BLACK_POINT) / (WHITE_POINT - BLACK_POINT), 0.);

    // blend out lod0 when viewpoint gains altitude
    lodAlpha = min(lodAlpha + i_meshScaleAlpha, 1.);

    return lodAlpha;
}

void SnapAndTransitionVertLayout(float i_meshScaleAlpha, inout vec3 io_worldPos, inout float o_lodAlpha)
{
    // see comments above on _GeomData
    const float GRID_SIZE_2 = 2.0*_GeomData.y, GRID_SIZE_4 = 4.0*_GeomData.y;

    // snap the verts to the grid
    // The snap size should be twice the original size to keep the shape of the eight triangles (otherwise the edge layout changes).
    io_worldPos.xz -= fract(/*unity_ObjectToWorld._m03_m23*/vec2(g_Local[3][0], g_Local[3][2]) / GRID_SIZE_2) * GRID_SIZE_2; // caution - sign of frac might change in non-hlsl shaders

    // compute lod transition alpha
    o_lodAlpha = ComputeLodAlpha(io_worldPos, i_meshScaleAlpha);

    // now smoothly transition vert layouts between lod levels - move interior verts inwards towards center
    vec2 m = fract(io_worldPos.xz / GRID_SIZE_4); // this always returns positive
    vec2 offset = m - 0.5;
    // check if vert is within one square from the center point which the verts move towards
    const float minRadius = 0.26; //0.26 is 0.25 plus a small "epsilon" - should solve numerical issues
    if (abs(offset.x) < minRadius) io_worldPos.x += offset.x * o_lodAlpha * GRID_SIZE_4;
    if (abs(offset.y) < minRadius) io_worldPos.z += offset.y * o_lodAlpha * GRID_SIZE_4;
}

void main()
{
    vec3 worldPos = (g_Local * In_Position).xyz;
    // Vertex snapping and lod transition
    float lodAlpha = 1;
    SnapAndTransitionVertLayout(_InstanceData.x, worldPos, lodAlpha);
    gl_Position = g_Proj * g_View * vec4(worldPos, 1);
}