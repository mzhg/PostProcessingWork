package jet.opengl.demos.scenes.outdoor;

final class SRenderingParams {

	static final int
        TM_HEIGHT_BASED = 0,
		TM_MATERIAL_MASK = 1,
        TM_MATERIAL_MASK_NM = 2
    ;
    
	final STerrainAttribs m_TerrainAttribs = new STerrainAttribs();

    // Patch rendering params
    int m_TexturingMode = TM_MATERIAL_MASK;
    int m_iRingDimension = 65;
    int m_iNumRings = 15;

    int m_iNumShadowCascades = 4;
    int m_bBestCascadeSearch = 1;
    int m_bSmoothShadows = 1;
    int m_iColOffset = 0, m_iRowOffset = 0;
}
