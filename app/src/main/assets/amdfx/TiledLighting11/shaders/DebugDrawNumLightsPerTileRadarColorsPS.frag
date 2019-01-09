#include "DebugDraw.glsl"

layout(location = 0) out float4 Out_Color;

in float m_Position;

//--------------------------------------------------------------------------------------
// This shader visualizes the number of lights per tile, using weather radar colors.
//--------------------------------------------------------------------------------------
//float4 DebugDrawNumLightsPerTileRadarColorsPS( VS_OUTPUT_POSITION_ONLY Input ) : SV_TARGET
void main()
{
    uint nNumLightsInThisTile = GetNumLightsInThisTile(g_PerTileLightIndexBuffer, g_uMaxNumLightsPerTile, g_uMaxNumElementsPerTile, m_Position);
    nNumLightsInThisTile += GetNumLightsInThisTile(g_PerTileSpotIndexBuffer, g_uMaxNumLightsPerTile, g_uMaxNumElementsPerTile, m_Position);
    uint uMaxNumLightsPerTile = 2*g_uMaxNumLightsPerTile;  // max for points plus max for spots
#if ( VPLS_ENABLED == 1 )
#if ( BLENDED_PASS == 0 )
    nNumLightsInThisTile += GetNumLightsInThisTile(g_PerTileVPLIndexBuffer, g_uMaxNumVPLsPerTile, g_uMaxNumVPLElementsPerTile, m_Position);
#endif
    uMaxNumLightsPerTile += g_uMaxNumVPLsPerTile;
#endif
    Out_Color = ConvertNumberOfLightsToRadarColor(nNumLightsInThisTile, uMaxNumLightsPerTile);
}