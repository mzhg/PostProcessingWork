package jet.opengl.postprocessing.core.outdoorLighting;

import jet.opengl.postprocessing.texture.Texture2D;

/**
 * Created by mazhen'gui on 2017/6/2.
 */

public class OutdoorLightScatteringInitAttribs {
    public int m_uiInitialSampleStepInSlice = 16;
    public int m_uiNumEpipolarSlices = 512;
    public int m_uiMaxSamplesInSlice = 256;
    public boolean m_bOptimizeSampleLocations = true;
    public boolean m_bEnableLightShafts = true;
    public boolean m_bUseCombinedMinMaxTexture = true;
    /**
     * <b>True</b>: Render extinction in epipolar space and perform bilateral filtering in the same manner as for inscattering<p>
     * <b>False</b>:Evaluate extinction for each pixel using analytic formula by Eric Bruneton
     */
    public boolean m_bExtinctionEvalMode = true;
    public boolean m_bRefinementCriterionInsctrDiff = true;
    public MultipleSctrMode m_uiMultipleScatteringMode = MultipleSctrMode.UNOCCLUDED;
    public SingleSctrMode m_uiSingleScatteringMode = SingleSctrMode.LUT;
    public int m_uiBackBufferWidth, m_uiBackBufferHeight;

    public boolean m_bCorrectScatteringAtDepthBreaks = false;
    public boolean m_bShowSampling = false;
    public boolean m_bEnableEpipolarSampling = true;
    public boolean m_bAutoExposure = true;

    public int m_iPrecomputedSctrUDim = 32;
    public int m_iPrecomputedSctrVDim = 128;
    public int m_iPrecomputedSctrWDim = 64;
    public int m_iPrecomputedSctrQDim = 16;

    public int m_iNumRandomSamplesOnSphere = 128;

    /** output texture */
    //    public int m_iNumPrecomputedHeights = 256;
//    public int m_iNumPrecomputedAngles = 256;
    public Texture2D m_ptex2DOccludedNetDensityToAtmTop;
    /** output texture */
    //    public int m_iAmbientSkyLightTexDim = 1024;
    public Texture2D m_ptex2DAmbientSkyLight;

    public void set(OutdoorLightScatteringInitAttribs other){

    }
}
