package jet.opengl.demos.intel.cloud;

import com.nvidia.developer.opengl.utils.StackInt;

import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;

import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;

/**
 * Created by mazhen'gui on 2017/7/7.
 */

final class CCloudsController {
    private static final int sm_iCSThreadGroupSize = 128;
    private static final int sm_iTileSize = 16;

    private String m_strEffectPath;
    private String m_strPreprocessingEffectPath;

    private boolean m_bPSOrderingAvailable;

    private final SPrecomputedOpticalDepthTexDim m_PrecomputedOpticalDepthTexDim = new SPrecomputedOpticalDepthTexDim();
    private final SPrecomputedScatteringInParticleTexDim m_PrecomputedSctrInParticleLUTDim = new SPrecomputedScatteringInParticleTexDim();

    private final Vector3f m_f3PrevLightDir = new Vector3f();

    private int m_uiCloudDensityTexWidth, m_uiCloudDensityTexHeight;
    private int m_uiBackBufferWidth, m_uiBackBufferHeight;

    private int m_psamLinearWrap, m_psamPointWrap, m_psamLinearClamp;

    private final SGlobalCloudAttribs m_CloudAttribs = new SGlobalCloudAttribs();

    // Packed locations for all cells and particles
    private StackInt m_PackedCellLocations, m_PackedParticleLocations;

    private int m_pcbGlobalCloudAttribs;

    // Render techniques
    private CRenderTechnique[] m_RenderCloudsTech = new CRenderTechnique[2];
    private CRenderTechnique[] m_RenderFlatCloudsTech = new CRenderTechnique[2];
    private CRenderTechnique m_CombineWithBBTech;
    private CRenderTechnique m_RenderCloudDetphToShadowMap;
    private CRenderTechnique m_ProcessCloudGridTech;
    private CRenderTechnique[] m_ComputeParticleVisibilityTech = new CRenderTechnique[2];
    private CRenderTechnique m_GenerateVisibleParticlesTech, m_ProcessVisibleParticlesTech;
    private CRenderTechnique m_EvaluateDensityTech, m_ComputeLightAttenuatingMass, m_Clear3DTexTech;
    private CRenderTechnique[] m_ComputeDispatchArgsTech =new CRenderTechnique[2];
    private CRenderTechnique m_ComputeOpticalDepthTech;
    private CRenderTechnique m_ApplyParticleLayersTech;
    private CRenderTechnique m_ComputeSingleSctrInParticleTech;
    private CRenderTechnique m_GatherPrevSctrOrderTech;
    private CRenderTechnique m_ComputeScatteringOrderTech;
    private CRenderTechnique m_AccumulateInscatteringTech;
    private CRenderTechnique m_RenderScatteringLUTSliceTech;
    private CRenderTechnique m_SortSubsequenceBitonicTech;
    private CRenderTechnique m_WriteSortedPariclesToVBTech;
    private CRenderTechnique m_MergeSubsequencesTech;


    // States
    private Runnable m_pdsEnableDepth, m_pdsDisableDepth;
    private Runnable m_prsSolidFillCullFront, m_prsSolidFillNoCull;
    private Runnable m_pbsDefault;
    private Runnable m_pbsRT0MulRT1MinRT2Over;

    // 2D cloud density and noise textures
    private Texture2D m_ptex2DCloudDensitySRV, m_ptex2DWhiteNoiseSRV;
    // Maximum mip map pyramid
    private Texture2D m_ptex2DMaxDensityMipMapSRV;
    // 3D noise texture
    private Texture2D m_ptex3DNoiseSRV;

    // SRV and UAV for cloud grid
    private Texture2D m_pbufCloudGridSRV;
    private Texture2D m_pbufCloudGridUAV;

    // SRV and UAV for particle lattice
    private Texture2D m_pbufCloudParticlesUAV;
    private Texture2D m_pbufCloudParticlesSRV;

    private Texture2D m_pbufParticlesLightingSRV;
    private Texture2D m_pbufParticlesLightingUAV;

    // Buffer containing unordered list of all valid cells
    private int m_pbufValidCellsUnorderedList;
    private Texture2D m_pbufValidCellsUnorderedListUAV;
    private Texture2D m_pbufValidCellsUnorderedListSRV;

    private Texture2D m_pbufVisibleCellsUnorderedListUAV;
    private Texture2D m_pbufVisibleCellsUnorderedListSRV;

    // Buffer containing number of valis cells or particles
    private int m_pbufValidCellsCounter;
    private Texture2D m_pbufValidCellsCounterSRV;
    private int m_pbufVisibleParticlesCounter;
    private Texture2D m_pbufVisibleParticlesCounterSRV;

    private Texture2D m_ptex3DCellDensitySRV;
    private Texture2D m_ptex3DCellDensityUAV;
    private Texture2D m_ptex3DLightAttenuatingMassSRV;
    private Texture2D m_ptex3DLightAttenuatingMassUAV;

    private Texture2D m_pbufVisibleParticlesUnorderedListUAV;
    private Texture2D m_pbufVisibleParticlesUnorderedListSRV;
    private Texture2D m_pbufVisibleParticlesSortedListUAV, m_pbufVisibleParticlesMergedListUAV;
    private Texture2D m_pbufVisibleParticlesSortedListSRV, m_pbufVisibleParticlesMergedListSRV;


    // Buffer containing sorted list of VISIBLE particles only
    private int m_pbufSerializedVisibleParticles;
    private Texture2D m_pbufSerializedVisibleParticlesUAV;

    // Buffer used to store DispatchIndirect() arguments
    private int m_pbufDispatchArgs;
    private Texture2D m_pbufDispatchArgsUAV;

    // Buffer used to store DrawIndirect() arguments
    private int m_pbufDrawIndirectArgs;

    // SRV for the buffer containing packed cell locations
    private Texture2D m_pbufPackedCellLocationsSRV;

    // Cloud color, transparancy and distance buffer for camera space
    private Texture2D m_ptex2DScreenCloudColorSRV;
    private Texture2D m_ptex2DScreenCloudColorRTV;
    private Texture2D m_ptex2DScrSpaceCloudTransparencySRV, m_ptex2DScrSpaceDistToCloudSRV;
    private Texture2D   m_ptex2DScrSpaceCloudTransparencyRTV, m_ptex2DScrSpaceDistToCloudRTV;

    // Downscaled cloud color, transparancy and distance buffer for camera space
    private Texture2D m_ptex2DDownscaledScrCloudColorSRV;
    private Texture2D   m_ptex2DDownscaledScrCloudColorRTV;
    private Texture2D m_ptex2DDownscaledScrCloudTransparencySRV, m_ptex2DDownscaledScrDistToCloudSRV;
    private Texture2D   m_ptex2DDownscaledScrCloudTransparencyRTV, m_ptex2DDownscaledScrDistToCloudRTV;

    private Texture2D m_pbufParticleLayersSRV;
    private Texture2D m_pbufParticleLayersUAV;
    private int m_pbufClearParticleLayers;

    private Texture2D m_ptex3DPrecomputedParticleDensitySRV;
    private Texture2D m_ptex3DSingleSctrInParticleLUT_SRV;
    private Texture2D m_ptex3DMultipleSctrInParticleLUT_SRV;


    // Inpute layout for streamed out particles
    private int m_pRenderCloudsInputLayout;

    void OnCreateDevice(/*ID3D11Device *pDevice, ID3D11DeviceContext *pDeviceContext*/){

    }

    void OnDestroyDevice(){

    }

    void Update( SGlobalCloudAttribs NewAttribs,
                 final ReadableVector3f CameraPos,
                 final ReadableVector3f LightDir,
//                 ID3D11Device *pDevice,
//                 ID3D11DeviceContext *pDeviceContext,
                 int pcbCameraAttribs,
                 int pcbLightAttribs,
                 int pcMediaScatteringParams ){

    }

    void RenderScreenSpaceDensityAndColor(SRenderAttribs RenderAttribs){

    }

    void RenderLightSpaceDensity(SRenderAttribs RenderAttribs){

    }

    void MergeLiSpDensityWithShadowMap(SRenderAttribs RenderAttribs){

    }

    void CombineWithBackBuffer(//ID3D11Device *pDevice,
                               //ID3D11DeviceContext *pDeviceContext,
                               Texture2D pDepthBufferSRV,
                               Texture2D pBackBufferSRV){

    }

    void OnResize(//ID3D11Device *pDevice,
                  int uiWidth, int uiHeight){

    }

    Texture2D GetScrSpaceCloudColor(){return m_ptex2DScreenCloudColorSRV;}
    Texture2D GetScrSpaceCloudTransparency(){return m_ptex2DScrSpaceCloudTransparencySRV;}
    Texture2D GetScrSpaceCloudMinMaxDist(){return m_ptex2DScrSpaceDistToCloudSRV;}
    SGlobalCloudAttribs GetCloudAttribs(){return m_CloudAttribs;}
    boolean IsPSOrderingAvailable(){return m_bPSOrderingAvailable;}

    private Macro[] DefineMacros(){ return null;}
    private void RenderMaxDensityMip(//ID3D11Device *pDevice,
                             //ID3D11DeviceContext *pDeviceContext,
                             Texture2D ptex2DMaxDensityMipMap,
                             Texture2D ptex2DTmpMaxDensityMipMap,
                             Texture2DDesc MaxCloudDensityMipDesc){

    }

    private void RenderFlatClouds(SRenderAttribs RenderAttribs){

    }
    private void RenderParticles(SRenderAttribs RenderAttribs){

    }

    private void GenerateParticles(SRenderAttribs RenderAttribs){

    }

    private void PrepareDispatchArgsBuffer(SRenderAttribs RenderAttribs, Texture2D pCounterSRV, int iTechInd){

    }

    private void SortVisibileParticles(SRenderAttribs RenderAttribs){

    }

    private void CreateBufferAndViews(//ID3D11Device *pDevice,
                                 /*const D3D11_BUFFER_DESC &BuffDesc,
                                 D3D11_SUBRESOURCE_DATA *pInitData,
                                 ID3D11Buffer **ppBuffer,
                                 ID3D11ShaderResourceView **ppSRV = nullptr,
                                 ID3D11UnorderedAccessView **ppUAV = nullptr,
                                 UINT UAVFlags = 0*/){

    }

    private void CreateParticleDataBuffer(/*ID3D11Device *pDevice*/){

    }

    private void PrecomputParticleDensity(/*ID3D11Device *pDevice, ID3D11DeviceContext *pDeviceContext*/){

    }

    private void PrecomputeScatteringInParticle(/*ID3D11Device *pDevice, ID3D11DeviceContext *pDeviceContext*/){

    }

    private void ComputeExitance(/*ID3D11Device *pDevice, ID3D11DeviceContext *pDeviceContext*/){

    }

    private void Create3DNoise(/*ID3D11Device *pDevice*/){

    }

    private void ClearCellDensityAndAttenuationTextures(SRenderAttribs RenderAttribs){

    }
}
