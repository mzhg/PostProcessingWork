package jet.opengl.demos.intel.cloud;

import com.nvidia.developer.opengl.utils.StackInt;

import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.texture.AttachType;
import jet.opengl.postprocessing.texture.RenderTargets;
import jet.opengl.postprocessing.texture.SamplerDesc;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.Texture3D;
import jet.opengl.postprocessing.texture.Texture3DDesc;
import jet.opengl.postprocessing.texture.TextureAttachDesc;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.CommonUtil;
import jet.opengl.postprocessing.util.Numeric;

/**
 * Created by mazhen'gui on 2017/7/7.
 */

final class CCloudsController {
    private static final int sm_iCSThreadGroupSize = 128;
    private static final int sm_iTileSize = 16;
    private static final float FLT_MAX = Float.MAX_VALUE;

    private String m_strEffectPath = "Intel/Cloud/";
    private String m_strPreprocessingEffectPath;

    private boolean m_bPSOrderingAvailable;

    private final SPrecomputedOpticalDepthTexDim m_PrecomputedOpticalDepthTexDim = new SPrecomputedOpticalDepthTexDim();
    private final SPrecomputedScatteringInParticleTexDim m_PrecomputedSctrInParticleLUTDim = new SPrecomputedScatteringInParticleTexDim();

    private final Vector3f m_f3PrevLightDir = new Vector3f();

    private int m_uiCloudDensityTexWidth = 1024, m_uiCloudDensityTexHeight = 1024;
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
    private RenderTargets    m_RenderTarget;
    private TextureAttachDesc[] m_AttachDescs = new TextureAttachDesc[4];
    private Macro[]          m_Processers;

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
    private Texture3D m_ptex3DNoiseSRV;

    // SRV and UAV for cloud grid
    private int m_pbufCloudGridSRV;
    private int m_pbufCloudGridUAV;

    // SRV and UAV for particle lattice
    private int m_pbufCloudParticlesUAV;
    private int m_pbufCloudParticlesSRV;

    private int m_pbufAtomicCounter;

    private int m_pbufParticlesLightingSRV;
    private int m_pbufParticlesLightingUAV;

    // Buffer containing unordered list of all valid cells
    private int m_pbufValidCellsUnorderedList;
    private int m_pbufVisibleCellsUnorderedList;

    // Buffer containing number of valis cells or particles
    private int m_pbufValidCellsCounter;
    private int m_pbufValidCellsCounterSRV;
    private int m_pbufVisibleParticlesCounter;
    private int m_pbufVisibleParticlesCounterSRV;

    private Texture3D m_ptex3DCellDensitySRV;
    private Texture3D m_ptex3DCellDensityUAV;
    private Texture3D m_ptex3DLightAttenuatingMassSRV;
    private Texture3D m_ptex3DLightAttenuatingMassUAV;

    private int m_pbufVisibleParticlesUnorderedListUAV;
    private int m_pbufVisibleParticlesUnorderedListSRV;
    private int m_pbufVisibleParticlesSortedListUAV, m_pbufVisibleParticlesMergedListUAV;
    private int m_pbufVisibleParticlesSortedListSRV, m_pbufVisibleParticlesMergedListSRV;


    // Buffer containing sorted list of VISIBLE particles only
    private int m_pbufSerializedVisibleParticles;
    private Texture2D m_pbufSerializedVisibleParticlesUAV;

    // Buffer used to store DispatchIndirect() arguments
    private int m_pbufDispatchArgs;
    private int m_pbufDispatchArgsUAV;

    // Buffer used to store DrawIndirect() arguments
    private int m_pbufDrawIndirectArgs;

    // SRV for the buffer containing packed cell locations
    private int m_pbufPackedCellLocationsSRV;

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

    private int m_pbufParticleLayersSRV;
    private int m_pbufParticleLayersUAV;
    private int m_pbufClearParticleLayers;

    private Texture3D m_ptex3DPrecomputedParticleDensitySRV;
    private Texture3D m_ptex3DSingleSctrInParticleLUT_SRV;
    private Texture3D m_ptex3DMultipleSctrInParticleLUT_SRV;
    private GLFuncProvider gl;

    // Inpute layout for streamed out particles
    private int m_pRenderCloudsInputLayout;
    private int m_DummyVAO;
    private int m_LastViewportX, m_LastViewportY;
    private int m_LastViewportWidth = -1, m_LastViewportHeight = -1;
    private boolean m_printOnce;

    CCloudsController(){
        m_PackedCellLocations = new StackInt();
        m_PackedParticleLocations = new StackInt();
        for(int i = 0; i < m_AttachDescs.length; i++){
            m_AttachDescs[i] = new TextureAttachDesc();
            m_AttachDescs[i].index = i;
        }
    }

    void OnCreateDevice(/*ID3D11Device *pDevice, ID3D11DeviceContext *pDeviceContext*/){
        gl = GLFuncProviderFactory.getGLFuncProvider();
        m_RenderTarget = new RenderTargets();
        m_DummyVAO = gl.glGenVertexArray();

//        HRESULT hr;
//
//        // Detect and report Intel extensions on this system
//        hr = IGFX::Init( pDevice );
//        if ( FAILED(hr) )
//        {
//            //CPUTOSServices::GetOSServices()->OpenMessageBox( _L("Error"), _L("Failed hardware detection initialization: incorrect vendor or device.\n\n") );
//        }
//        // detect the available extensions
//        IGFX::Extensions extensions = IGFX::getAvailableExtensions( pDevice );
//
//        m_bPSOrderingAvailable = extensions.PixelShaderOrdering;
//
//        // Disable the AVSM extension method if the hardware/driver does not support Pixel Shader Ordering feature
//        if ( !extensions.PixelShaderOrdering )
//        {
//            CPUTOSServices::GetOSServices()->OpenMessageBox(_L("Pixel Shader Ordering feature not found"), _L("Your hardware or graphics driver does not support the pixel shader ordering feature. Volume-aware blending will be disabled. Please update your driver or run on a system that supports the required feature to see that option."));
//        }

        CreateParticleDataBuffer(/*pDevice*/);

        // Create buffer for storing number of valid cells
        {
//            D3D11_BUFFER_DESC ValidCellsCounterBuffDesc =
//                    {
//                            sizeof(UINT)*4,                           //UINT ByteWidth;
//                            D3D11_USAGE_DEFAULT,                    //D3D11_USAGE Usage;
//                            D3D11_BIND_SHADER_RESOURCE,             //UINT BindFlags;
//                            0,                                      //UINT CPUAccessFlags;
//                            0,                                      //UINT MiscFlags;
//                            0	            						//UINT StructureByteStride;
//                    };
//            V( CreateBufferAndViews( pDevice, ValidCellsCounterBuffDesc, nullptr, &m_pbufValidCellsCounter) );
//            V(CreateBufferAndViews( pDevice, ValidCellsCounterBuffDesc, nullptr, &m_pbufVisibleParticlesCounter));
//
//            D3D11_SHADER_RESOURCE_VIEW_DESC SRVDesc;
//            ZeroMemory(&SRVDesc, sizeof(SRVDesc));
//            SRVDesc.Format =  DXGI_FORMAT_R32_UINT;
//            SRVDesc.ViewDimension = D3D11_SRV_DIMENSION_BUFFER;
//            SRVDesc.Buffer.ElementOffset = 0;
//            SRVDesc.Buffer.ElementWidth = sizeof(UINT);
//            V_RETURN(pDevice->CreateShaderResourceView( m_pbufValidCellsCounter, &SRVDesc, &m_pbufValidCellsCounterSRV));
//            V_RETURN(pDevice->CreateShaderResourceView( m_pbufVisibleParticlesCounter, &SRVDesc, &m_pbufVisibleParticlesCounterSRV));

            m_pbufValidCellsCounter = gl.glGenBuffer();
            gl.glBindBuffer(GLenum.GL_TEXTURE_BUFFER, m_pbufValidCellsCounter);
            gl.glBufferData(GLenum.GL_TEXTURE_BUFFER, 16, GLenum.GL_DYNAMIC_COPY);
            gl.glBindBuffer(GLenum.GL_TEXTURE_BUFFER, 0);

            m_pbufValidCellsCounterSRV = gl.glGenTexture();
            gl.glBindTexture(GLenum.GL_TEXTURE_BUFFER, m_pbufValidCellsCounterSRV);
            gl.glTexBuffer(GLenum.GL_TEXTURE_BUFFER, GLenum.GL_R32I, m_pbufValidCellsCounter);
            gl.glBindTexture(GLenum.GL_TEXTURE_BUFFER, 0);

            m_pbufVisibleParticlesCounter = gl.glGenBuffer();
            gl.glBindBuffer(GLenum.GL_TEXTURE_BUFFER, m_pbufVisibleParticlesCounter);
            gl.glBufferData(GLenum.GL_TEXTURE_BUFFER, 16, GLenum.GL_DYNAMIC_COPY);
            gl.glBindBuffer(GLenum.GL_TEXTURE_BUFFER, 0);

            m_pbufVisibleParticlesCounterSRV = gl.glGenTexture();
            gl.glBindTexture(GLenum.GL_TEXTURE_BUFFER, m_pbufVisibleParticlesCounterSRV);
            gl.glTexBuffer(GLenum.GL_TEXTURE_BUFFER, GLenum.GL_R32I, m_pbufVisibleParticlesCounter);
            gl.glBindTexture(GLenum.GL_TEXTURE_BUFFER, 0);
        }

        // Create buffer for storing DispatchIndirect() arguments
        {
//            UINT DispatchArgs[] =
//                    {
//                            1, // UINT ThreadGroupCountX
//                            1, // UINT ThreadGroupCountY
//                            1, // UINT ThreadGroupCountZ
//                    };
//            D3D11_BUFFER_DESC DispatchArgsBuffDesc =
//                    {
//                            sizeof(DispatchArgs),                   //UINT ByteWidth;
//                            D3D11_USAGE_DEFAULT,                    //D3D11_USAGE Usage;
//                            D3D11_BIND_UNORDERED_ACCESS,            //UINT BindFlags;
//                            0,                                      //UINT CPUAccessFlags;
//                            D3D11_RESOURCE_MISC_DRAWINDIRECT_ARGS,  //UINT MiscFlags;
//                            0                                       //UINT StructureByteStride;
//                    };
//            D3D11_SUBRESOURCE_DATA InitData = {&DispatchArgs, 0, 0};
//            V( CreateBufferAndViews( pDevice, DispatchArgsBuffDesc, &InitData, &m_pbufDispatchArgs, nullptr, nullptr) );
//            D3D11_UNORDERED_ACCESS_VIEW_DESC UAVDesc;
//            UAVDesc.Format = DXGI_FORMAT_R32_UINT;
//            UAVDesc.ViewDimension = D3D11_UAV_DIMENSION_BUFFER;
//            UAVDesc.Buffer.FirstElement = 0;
//            UAVDesc.Buffer.NumElements = _countof(DispatchArgs);
//            UAVDesc.Buffer.Flags = 0;
//            V_RETURN(pDevice->CreateUnorderedAccessView( m_pbufDispatchArgs, &UAVDesc, &m_pbufDispatchArgsUAV));

            m_pbufDispatchArgsUAV = m_pbufDispatchArgs = gl.glGenBuffer();
            gl.glBindBuffer(GLenum.GL_DISPATCH_INDIRECT_BUFFER, m_pbufDispatchArgs);
            gl.glBufferData(GLenum.GL_DISPATCH_INDIRECT_BUFFER, 12, GLenum.GL_DYNAMIC_COPY);
            gl.glBindBuffer(GLenum.GL_DISPATCH_INDIRECT_BUFFER, 0);
        }

        // Create buffer for storing DrawIndirect() arguments
        {
//            UINT DrawInstancedArgs[] =
//                    {
//                            0, // UINT VertexCountPerInstance,
//                            1, // UINT InstanceCount,
//                            0, // StartVertexLocation,
//                            0  // StartInstanceLocation
//                    };
//            D3D11_BUFFER_DESC DrawArgsBuffDesc =
//                    {
//                            sizeof(DrawInstancedArgs),              //UINT ByteWidth;
//                            D3D11_USAGE_DEFAULT,                    //D3D11_USAGE Usage;
//                            0,                                      //UINT BindFlags;
//                            0,                                      //UINT CPUAccessFlags;
//                            D3D11_RESOURCE_MISC_DRAWINDIRECT_ARGS,  //UINT MiscFlags;
//                            0                                       //UINT StructureByteStride;
//                    };
//            D3D11_SUBRESOURCE_DATA InitData = {&DrawInstancedArgs, 0, 0};
//            V( CreateBufferAndViews( pDevice, DrawArgsBuffDesc, &InitData, &m_pbufDrawIndirectArgs, nullptr, nullptr) );

            m_pbufDrawIndirectArgs = gl.glGenBuffer();
            gl.glBindBuffer(GLenum.GL_DRAW_INDIRECT_BUFFER, m_pbufDrawIndirectArgs);
            gl.glBufferData(GLenum.GL_DRAW_INDIRECT_BUFFER, 16, GLenum.GL_DYNAMIC_COPY);
            gl.glBindBuffer(GLenum.GL_DRAW_INDIRECT_BUFFER, 0);
        }

        {
            m_pbufAtomicCounter = gl.glGenBuffer();
            gl.glBindBuffer(GLenum.GL_ATOMIC_COUNTER_BUFFER, m_pbufAtomicCounter);
            gl.glBufferData(GLenum.GL_ATOMIC_COUNTER_BUFFER, 8, GLenum.GL_DYNAMIC_COPY);  // two integers
            gl.glBindBuffer(GLenum.GL_ATOMIC_COUNTER_BUFFER, 0);
        }

//        D3D11_BUFFER_DESC GlobalCloudAttribsCBDesc =
//                {
//                        sizeof(SGlobalCloudAttribs), //UINT ByteWidth;
//                        D3D11_USAGE_DYNAMIC,         //D3D11_USAGE Usage;
//                        D3D11_BIND_CONSTANT_BUFFER,  //UINT BindFlags;
//                        D3D11_CPU_ACCESS_WRITE,      //UINT CPUAccessFlags;
//                        0,                                      //UINT MiscFlags;
//                        0                                       //UINT StructureByteStride;
//                };
//        V(pDevice->CreateBuffer( &GlobalCloudAttribsCBDesc, nullptr, &m_pcbGlobalCloudAttribs));

        // Create depth stencil states
//        D3D11_DEPTH_STENCIL_DESC EnableDepthTestDSDesc;
//        ZeroMemory(&EnableDepthTestDSDesc, sizeof(EnableDepthTestDSDesc));
//        EnableDepthTestDSDesc.DepthEnable = TRUE;
//        EnableDepthTestDSDesc.DepthWriteMask = D3D11_DEPTH_WRITE_MASK_ALL;
//        EnableDepthTestDSDesc.DepthFunc = D3D11_COMPARISON_GREATER;
//        V( pDevice->CreateDepthStencilState(  &EnableDepthTestDSDesc, &m_pdsEnableDepth) );

        m_pdsEnableDepth = ()->
        {
            gl.glEnable(GLenum.GL_DEPTH_TEST);
            gl.glDepthFunc(GLenum.GL_LESS);
            gl.glDepthMask(true);
        };

//        D3D11_DEPTH_STENCIL_DESC DisableDepthTestDSDesc;
//        ZeroMemory(&DisableDepthTestDSDesc, sizeof(DisableDepthTestDSDesc));
//        DisableDepthTestDSDesc.DepthEnable = FALSE;
//        DisableDepthTestDSDesc.DepthWriteMask = D3D11_DEPTH_WRITE_MASK_ZERO;
//        DisableDepthTestDSDesc.DepthFunc = D3D11_COMPARISON_GREATER;
//        V( pDevice->CreateDepthStencilState(  &DisableDepthTestDSDesc, &m_pdsDisableDepth) );
        m_pdsDisableDepth = ()-> gl.glDisable(GLenum.GL_DEPTH_TEST);

        // Create rasterizer states
//        D3D11_RASTERIZER_DESC SolidFillCullBackRSDesc;
//        ZeroMemory(&SolidFillCullBackRSDesc, sizeof(SolidFillCullBackRSDesc));
//        SolidFillCullBackRSDesc.FillMode = D3D11_FILL_SOLID;
//        SolidFillCullBackRSDesc.CullMode = D3D11_CULL_FRONT;
//        SolidFillCullBackRSDesc.DepthClipEnable = FALSE; // TODO: temp
//        V( pDevice->CreateRasterizerState( &SolidFillCullBackRSDesc, &m_prsSolidFillCullFront) );
        m_prsSolidFillCullFront = ()->
        {
            gl.glEnable(GLenum.GL_CULL_FACE);
            gl.glCullFace(GLenum.GL_FRONT);
            gl.glFrontFace(GLenum.GL_CW);
        };

//        D3D11_RASTERIZER_DESC SolidFillNoCullRSDesc;
//        ZeroMemory(&SolidFillNoCullRSDesc, sizeof(SolidFillNoCullRSDesc));
//        SolidFillNoCullRSDesc.FillMode = D3D11_FILL_SOLID;
//        SolidFillNoCullRSDesc.CullMode = D3D11_CULL_NONE;
//        SolidFillNoCullRSDesc.DepthClipEnable = TRUE;
//        V( pDevice->CreateRasterizerState( &SolidFillNoCullRSDesc, &m_prsSolidFillNoCull) );
        m_prsSolidFillNoCull = ()->gl.glDisable(GLenum.GL_CULL_FACE);

        // Create default blend state
//        D3D11_BLEND_DESC DefaultBlendStateDesc;
//        ZeroMemory(&DefaultBlendStateDesc, sizeof(DefaultBlendStateDesc));
//        DefaultBlendStateDesc.IndependentBlendEnable = FALSE;
//        for(int i=0; i< _countof(DefaultBlendStateDesc.RenderTarget); i++)
//            DefaultBlendStateDesc.RenderTarget[i].RenderTargetWriteMask = D3D11_COLOR_WRITE_ENABLE_ALL;
//        V( pDevice->CreateBlendState( &DefaultBlendStateDesc, &m_pbsDefault) );
        m_pbsDefault = ()->gl.glDisable(GLenum.GL_BLEND);

        // Create blend state for rendering particles
//        D3D11_BLEND_DESC AlphaBlendStateDesc;
//        ZeroMemory(&AlphaBlendStateDesc, sizeof(AlphaBlendStateDesc));
//        AlphaBlendStateDesc.IndependentBlendEnable = TRUE;
//        for(int i=0; i< _countof(AlphaBlendStateDesc.RenderTarget); i++)
//            AlphaBlendStateDesc.RenderTarget[i].RenderTargetWriteMask = D3D11_COLOR_WRITE_ENABLE_ALL;
//        AlphaBlendStateDesc.RenderTarget[0].BlendEnable = TRUE;
//        AlphaBlendStateDesc.RenderTarget[0].BlendOp        = D3D11_BLEND_OP_ADD;
//        AlphaBlendStateDesc.RenderTarget[0].SrcBlend       = D3D11_BLEND_ZERO;
//        AlphaBlendStateDesc.RenderTarget[0].DestBlend      = D3D11_BLEND_SRC_COLOR;
//
//        AlphaBlendStateDesc.RenderTarget[0].BlendOpAlpha  = D3D11_BLEND_OP_ADD;
//        AlphaBlendStateDesc.RenderTarget[0].SrcBlendAlpha = D3D11_BLEND_ZERO;
//        AlphaBlendStateDesc.RenderTarget[0].DestBlendAlpha= D3D11_BLEND_SRC_ALPHA;
//
//        AlphaBlendStateDesc.RenderTarget[1].BlendEnable    = TRUE;
//        AlphaBlendStateDesc.RenderTarget[1].BlendOp        = D3D11_BLEND_OP_MIN;
//        AlphaBlendStateDesc.RenderTarget[1].SrcBlend       = D3D11_BLEND_ONE;
//        AlphaBlendStateDesc.RenderTarget[1].DestBlend      = D3D11_BLEND_ONE;
//
//        AlphaBlendStateDesc.RenderTarget[1].BlendOpAlpha  = D3D11_BLEND_OP_MIN;
//        AlphaBlendStateDesc.RenderTarget[1].SrcBlendAlpha = D3D11_BLEND_ONE;
//        AlphaBlendStateDesc.RenderTarget[1].DestBlendAlpha= D3D11_BLEND_ONE;
//
//        AlphaBlendStateDesc.RenderTarget[2].BlendEnable = TRUE;
//        AlphaBlendStateDesc.RenderTarget[2].BlendOp        = D3D11_BLEND_OP_ADD;
//        AlphaBlendStateDesc.RenderTarget[2].SrcBlend       = D3D11_BLEND_ONE;
//        AlphaBlendStateDesc.RenderTarget[2].DestBlend      = D3D11_BLEND_SRC_ALPHA;
//
//        AlphaBlendStateDesc.RenderTarget[2].BlendOpAlpha   = D3D11_BLEND_OP_ADD;
//        AlphaBlendStateDesc.RenderTarget[2].SrcBlendAlpha  = D3D11_BLEND_ONE;
//        AlphaBlendStateDesc.RenderTarget[2].DestBlendAlpha = D3D11_BLEND_ONE;
//
//        V( pDevice->CreateBlendState( &AlphaBlendStateDesc, &m_pbsRT0MulRT1MinRT2Over) );
        m_pbsRT0MulRT1MinRT2Over = ()->
        {
            gl.glEnablei(GLenum.GL_BLEND, 0);
            gl.glBlendEquationi(0, GLenum.GL_FUNC_ADD);
            gl.glBlendFuncSeparatei(0, GLenum.GL_ZERO, GLenum.GL_SRC_COLOR, GLenum.GL_ZERO, GLenum.GL_SRC_ALPHA);

            gl.glEnablei(GLenum.GL_BLEND, 1);
            gl.glBlendEquationi(1, GLenum.GL_MIN);
            gl.glBlendFuncSeparatei(1, GLenum.GL_ONE, GLenum.GL_ONE, GLenum.GL_ONE, GLenum.GL_ONE);

            gl.glEnablei(GLenum.GL_BLEND, 2);
            gl.glBlendEquationi(1, GLenum.GL_FUNC_ADD);
            gl.glBlendFuncSeparatei(1, GLenum.GL_ONE, GLenum.GL_SRC_ALPHA, GLenum.GL_ONE, GLenum.GL_ONE);
        };

        SamplerDesc SamLinearWrap = new SamplerDesc();
        SamLinearWrap.minFilter = GLenum.GL_LINEAR_MIPMAP_LINEAR;
        SamLinearWrap.wrapR = GLenum.GL_REPEAT;
        SamLinearWrap.wrapS = GLenum.GL_REPEAT;
        SamLinearWrap.wrapT = GLenum.GL_REPEAT;
//                {
//                        D3D11_FILTER_MIN_MAG_MIP_LINEAR,
//                        D3D11_TEXTURE_ADDRESS_WRAP,
//                        D3D11_TEXTURE_ADDRESS_WRAP,
//                        D3D11_TEXTURE_ADDRESS_WRAP,
//                        0, //FLOAT MipLODBias;
//                        0, //UINT MaxAnisotropy;
//                        D3D11_COMPARISON_NEVER, // D3D11_COMPARISON_FUNC ComparisonFunc;
//                        {0.f, 0.f, 0.f, 0.f}, //FLOAT BorderColor[ 4 ];
//                        -FLT_MAX, //FLOAT MinLOD;
//                        +FLT_MAX //FLOAT MaxLOD;
//                };
//        V( pDevice->CreateSamplerState( &SamLinearWrap, &m_psamLinearWrap) );
        m_psamLinearWrap = SamplerUtils.createSampler(SamLinearWrap);

        SamplerDesc SamPointWrap = SamLinearWrap;
        SamPointWrap.minFilter = GLenum.GL_NEAREST_MIPMAP_NEAREST;
        SamPointWrap.magFilter = GLenum.GL_NEAREST;
//        SamPointWrap.Filter = D3D11_FILTER_MIN_MAG_MIP_POINT;
//        V( pDevice->CreateSamplerState( &SamPointWrap, &m_psamPointWrap) );
        m_psamPointWrap = SamplerUtils.createSampler(SamPointWrap);


//        SamLinearWrap.AddressU = D3D11_TEXTURE_ADDRESS_CLAMP;
//        SamLinearWrap.AddressV = D3D11_TEXTURE_ADDRESS_CLAMP;
//        SamLinearWrap.AddressW = D3D11_TEXTURE_ADDRESS_CLAMP;
        SamLinearWrap.magFilter = GLenum.GL_LINEAR;
        SamLinearWrap.minFilter = GLenum.GL_LINEAR_MIPMAP_LINEAR;
        SamLinearWrap.wrapR = GLenum.GL_CLAMP_TO_EDGE;
        SamLinearWrap.wrapS = GLenum.GL_CLAMP_TO_EDGE;
        SamLinearWrap.wrapT = GLenum.GL_CLAMP_TO_EDGE;
//        V( pDevice->CreateSamplerState( &SamLinearWrap, &m_psamLinearClamp) );
        m_psamLinearClamp = SamplerUtils.createSampler(SamLinearWrap);

//        D3DX11_IMAGE_LOAD_INFO LoadInfo;
//        ZeroMemory(&LoadInfo, sizeof(D3DX11_IMAGE_LOAD_INFO));
//        LoadInfo.Width          = D3DX11_FROM_FILE;
//        LoadInfo.Height         = D3DX11_FROM_FILE;
//        LoadInfo.Depth          = D3DX11_FROM_FILE;
//        LoadInfo.FirstMipLevel  = D3DX11_FROM_FILE;
//        LoadInfo.MipLevels      = D3DX11_DEFAULT;
//        LoadInfo.Usage          = D3D11_USAGE_IMMUTABLE;
//        LoadInfo.BindFlags      = D3D11_BIND_SHADER_RESOURCE;
//        LoadInfo.CpuAccessFlags = 0;
//        LoadInfo.MiscFlags      = 0;
//        LoadInfo.MipFilter      = D3DX11_FILTER_LINEAR;
//        LoadInfo.pSrcInfo       = NULL;
//        LoadInfo.Format         = DXGI_FORMAT_BC4_UNORM;
//        LoadInfo.Filter         = D3DX11_FILTER_LINEAR;
//
//        // Load noise textures. Important to use BC4 compression
//        LoadInfo.Format         = DXGI_FORMAT_BC4_UNORM;
//        D3DX11CreateShaderResourceViewFromFile(pDevice, L"media\\Noise.png", &LoadInfo, nullptr, &m_ptex2DCloudDensitySRV, nullptr);
        try {
            m_ptex2DCloudDensitySRV = TextureUtils.createTexture2DFromFile(m_strEffectPath + "textures/Noise.png", true);
            m_ptex2DWhiteNoiseSRV = TextureUtils.createTexture2DFromFile(m_strEffectPath + "textures/WhiteNoise.png", true);
        } catch (IOException e) {
            e.printStackTrace();
        }


        // Noise is not compressed well. Besides, it seems like there are some strange unstable results when using BC1 (?)
//        LoadInfo.Format         = DXGI_FORMAT_R8G8B8A8_UNORM;//DXGI_FORMAT_BC1_UNORM;
//        D3DX11CreateShaderResourceViewFromFile(pDevice, L"media\\WhiteNoise.png", &LoadInfo, nullptr, &m_ptex2DWhiteNoiseSRV, nullptr);

        {
            // Create maximum density mip map
//            CComPtr<ID3D11Resource> pCloudDensityRes;
//            m_ptex2DCloudDensitySRV->GetResource(&pCloudDensityRes);
//            D3D11_TEXTURE2D_DESC CloudDensityTexDesc;
//            CComQIPtr<ID3D11Texture2D>(pCloudDensityRes)->GetDesc(&CloudDensityTexDesc);
            m_uiCloudDensityTexWidth = m_ptex2DCloudDensitySRV.getWidth();
            m_uiCloudDensityTexHeight = m_ptex2DCloudDensitySRV.getHeight();

//            D3D11_TEXTURE2D_DESC MaxCloudDensityMipDesc = CloudDensityTexDesc;
//            MaxCloudDensityMipDesc.Format = DXGI_FORMAT_R8_UNORM;
//            MaxCloudDensityMipDesc.Usage = D3D11_USAGE_DEFAULT;
//            MaxCloudDensityMipDesc.BindFlags = D3D11_BIND_SHADER_RESOURCE;
//            CComPtr<ID3D11Texture2D> ptex2DMaxDensityMipMap, ptex2DTmpMaxDensityMipMap;
//            V(pDevice->CreateTexture2D(&MaxCloudDensityMipDesc, nullptr, &ptex2DMaxDensityMipMap));
//            V(pDevice->CreateShaderResourceView(ptex2DMaxDensityMipMap, nullptr, &m_ptex2DMaxDensityMipMapSRV));
//
//            MaxCloudDensityMipDesc.BindFlags = D3D11_BIND_RENDER_TARGET;
//            V(pDevice->CreateTexture2D(&MaxCloudDensityMipDesc, nullptr, &ptex2DTmpMaxDensityMipMap));
            Texture2DDesc MaxCloudDensityMipDesc = new Texture2DDesc(m_uiCloudDensityTexWidth, m_uiCloudDensityTexHeight, GLenum.GL_R8);
            m_ptex2DMaxDensityMipMapSRV = TextureUtils.createTexture2D(MaxCloudDensityMipDesc, null);
            Texture2D ptex2DTmpMaxDensityMipMap = TextureUtils.createTexture2D(MaxCloudDensityMipDesc, null);

            RenderMaxDensityMip( //pDevice, pDeviceContext,
                    m_ptex2DMaxDensityMipMapSRV, ptex2DTmpMaxDensityMipMap,
                    MaxCloudDensityMipDesc );
        }

        Create3DNoise(/*pDevice*/);
    }

    void OnDestroyDevice(){
        if(m_pcbGlobalCloudAttribs != 0){
            gl.glDeleteBuffer(m_pcbGlobalCloudAttribs);
            m_pcbGlobalCloudAttribs = 0;
        }

        for(int i=0; i < _countof(m_RenderCloudsTech); ++i) {
            CommonUtil.safeRelease(m_RenderCloudsTech[i]);
            m_RenderCloudsTech[i] = null;
        }
        for(int i=0; i < _countof(m_RenderFlatCloudsTech); ++i){
            CommonUtil.safeRelease(m_RenderFlatCloudsTech[i]);
            m_RenderFlatCloudsTech[i] = null;
        }

        CommonUtil.safeRelease(m_CombineWithBBTech);
        CommonUtil.safeRelease(m_RenderCloudDetphToShadowMap);
        CommonUtil.safeRelease(m_ProcessCloudGridTech);
        for(int i=0; i < _countof(m_ComputeParticleVisibilityTech); ++i)
            CommonUtil.safeRelease(m_ComputeParticleVisibilityTech[i]);
        CommonUtil.safeRelease(m_GenerateVisibleParticlesTech);
        CommonUtil.safeRelease(m_ProcessVisibleParticlesTech);
        CommonUtil.safeRelease(m_EvaluateDensityTech);
        CommonUtil.safeRelease(m_ComputeLightAttenuatingMass);
        CommonUtil.safeRelease(m_Clear3DTexTech);
        for(int i=0; i < _countof(m_ComputeDispatchArgsTech); ++i){
            CommonUtil.safeRelease(m_ComputeDispatchArgsTech[i]);
        }
        CommonUtil.safeRelease(m_ComputeOpticalDepthTech);
        CommonUtil.safeRelease(m_ApplyParticleLayersTech);
        CommonUtil.safeRelease(m_ComputeSingleSctrInParticleTech);
        CommonUtil.safeRelease(m_GatherPrevSctrOrderTech);
        CommonUtil.safeRelease(m_ComputeScatteringOrderTech);
        CommonUtil.safeRelease(m_AccumulateInscatteringTech);
        CommonUtil.safeRelease(m_RenderScatteringLUTSliceTech);
        CommonUtil.safeRelease(m_SortSubsequenceBitonicTech);
        CommonUtil.safeRelease(m_WriteSortedPariclesToVBTech);
        CommonUtil.safeRelease(m_MergeSubsequencesTech);

        CommonUtil.safeRelease(m_ptex2DCloudDensitySRV);
        CommonUtil.safeRelease(m_ptex2DWhiteNoiseSRV);
        CommonUtil.safeRelease(m_ptex2DMaxDensityMipMapSRV);
        CommonUtil.safeRelease(m_ptex3DNoiseSRV);

        if(m_pbufCloudGridSRV != 0){
            gl.glDeleteBuffer(m_pbufCloudGridSRV);
            gl.glDeleteTexture(m_pbufCloudGridUAV);

            m_pbufCloudGridSRV =m_pbufCloudGridUAV= 0;
        }

        if(m_pbufCloudParticlesSRV != 0){
            gl.glDeleteBuffer(m_pbufCloudParticlesSRV);
            gl.glDeleteTexture(m_pbufCloudParticlesUAV);

            m_pbufCloudParticlesUAV =m_pbufCloudParticlesSRV= 0;
        }

//        CommonUtil.safeRelease(m_pbufParticlesLightingSRV);
//        CommonUtil.safeRelease(m_pbufParticlesLightingUAV);
        if(m_pbufValidCellsUnorderedList != 0){
            gl.glDeleteBuffer(m_pbufValidCellsUnorderedList);
            m_pbufValidCellsUnorderedList = 0;
        }
//        CommonUtil.safeRelease(m_pbufValidCellsUnorderedListUAV);
//
//        CommonUtil.safeRelease(m_pbufValidCellsUnorderedListSRV);
//        CommonUtil.safeRelease(m_pbufVisibleCellsUnorderedListSRV);
//        CommonUtil.safeRelease(m_pbufVisibleCellsUnorderedListUAV);
        if(m_pbufValidCellsCounter != 0){
            gl.glDeleteBuffer(m_pbufValidCellsCounter);
            m_pbufValidCellsCounter = 0;
            m_pbufValidCellsCounterSRV =0;
        }
        if(m_pbufVisibleParticlesCounter != 0){
            gl.glDeleteBuffer(m_pbufVisibleParticlesCounter);
            m_pbufVisibleParticlesCounter = 0;
            m_pbufVisibleParticlesCounterSRV =0;
        }

        CommonUtil.safeRelease(m_ptex3DCellDensitySRV);
        CommonUtil.safeRelease(m_ptex3DCellDensityUAV);
        CommonUtil.safeRelease(m_ptex3DLightAttenuatingMassSRV);
        CommonUtil.safeRelease(m_ptex3DLightAttenuatingMassUAV);
//        CommonUtil.safeRelease(m_pbufVisibleParticlesUnorderedListUAV);
//        CommonUtil.safeRelease(m_pbufVisibleParticlesUnorderedListSRV);
//        m_pbufVisibleParticlesSortedListUAV.Release();
//        m_pbufVisibleParticlesSortedListSRV.Release();
//        m_pbufVisibleParticlesMergedListSRV.Release();
//        m_pbufVisibleParticlesMergedListUAV.Release();
//
//        m_pbufSerializedVisibleParticles.Release();
//        m_pbufSerializedVisibleParticlesUAV.Release();
//
//        m_pbufDispatchArgsUAV.Release();
//        m_pbufDispatchArgs.Release();
//        m_pbufDrawIndirectArgs.Release();
//
//        m_pbufPackedCellLocationsSRV.Release();
//
//        m_ptex2DScreenCloudColorSRV.Release();
//        m_ptex2DScreenCloudColorRTV.Release();
//        m_ptex2DScrSpaceCloudTransparencySRV.Release();
//        m_ptex2DScrSpaceCloudTransparencyRTV.Release();
//        m_ptex2DScrSpaceDistToCloudSRV.Release();
//        m_ptex2DScrSpaceDistToCloudRTV.Release();
//
//        m_ptex2DDownscaledScrCloudColorSRV.Release();
//        m_ptex2DDownscaledScrCloudColorRTV.Release();
//        m_ptex2DDownscaledScrCloudTransparencySRV.Release();
//        m_ptex2DDownscaledScrCloudTransparencyRTV.Release();
//        m_ptex2DDownscaledScrDistToCloudSRV.Release();
//        m_ptex2DDownscaledScrDistToCloudRTV.Release();
//
//        m_pbufParticleLayersSRV.Release();
//        m_pbufParticleLayersUAV.Release();
//        m_pbufClearParticleLayers.Release();
//
//        m_ptex3DPrecomputedParticleDensitySRV.Release();
//        m_ptex3DSingleSctrInParticleLUT_SRV.Release();
//        m_ptex3DMultipleSctrInParticleLUT_SRV.Release();
//
//
//        m_pRenderCloudsInputLayout.Release();
    }

    void Update( SGlobalCloudAttribs NewAttribs,
                 final ReadableVector3f CameraPos,
                 final ReadableVector3f LightDir,
//                 ID3D11Device *pDevice,
//                 ID3D11DeviceContext *pDeviceContext,
                 int pcbCameraAttribs,
                 int pcbLightAttribs,
                 int pcMediaScatteringParams ){
        m_CloudAttribs.fCloudDensityThreshold = NewAttribs.fCloudDensityThreshold;
        m_CloudAttribs.fCloudAltitude         = NewAttribs.fCloudAltitude;
        m_CloudAttribs.fCloudThickness        = NewAttribs.fCloudThickness;
        m_CloudAttribs.fParticleCutOffDist    = NewAttribs.fParticleCutOffDist;

        if( m_CloudAttribs.uiNumRings     != NewAttribs.uiNumRings ||
                m_CloudAttribs.uiInnerRingDim != NewAttribs.uiInnerRingDim ||
                m_CloudAttribs.uiMaxLayers    != NewAttribs.uiMaxLayers )
        {
            m_CloudAttribs.uiNumRings     = NewAttribs.uiNumRings;
            m_CloudAttribs.uiInnerRingDim = NewAttribs.uiInnerRingDim;
            m_CloudAttribs.uiMaxLayers    = NewAttribs.uiMaxLayers;
            CreateParticleDataBuffer(/*pDevice*/);
            m_f3PrevLightDir.set(0,0,0);
        }

        if( m_CloudAttribs.uiDownscaleFactor != NewAttribs.uiDownscaleFactor )
        {
            m_CloudAttribs.uiDownscaleFactor = NewAttribs.uiDownscaleFactor;
            OnResize(/*pDevice,*/ m_uiBackBufferWidth, m_uiBackBufferHeight);
            for(int i=0; i < _countof(m_RenderCloudsTech); ++i) {
                CommonUtil.safeRelease(m_RenderCloudsTech[i]);
                m_RenderCloudsTech[i] = null;
            }
            for(int i=0; i < _countof(m_RenderFlatCloudsTech); ++i) {
                CommonUtil.safeRelease(m_RenderFlatCloudsTech[i]);
                m_RenderFlatCloudsTech[i] = null;
            }

            CommonUtil.safeRelease(m_CombineWithBBTech);
            m_CombineWithBBTech = null;
        }

        if( m_CloudAttribs.uiNumCascades != NewAttribs.uiNumCascades )
        {
            m_CloudAttribs.uiNumCascades = NewAttribs.uiNumCascades;
            for(int i=0; i < _countof(m_RenderCloudsTech); ++i){
                CommonUtil.safeRelease(m_RenderCloudsTech[i]);
                m_RenderCloudsTech[i] = null;
            }
            for(int i=0; i < _countof(m_RenderFlatCloudsTech); ++i){
                CommonUtil.safeRelease(m_RenderFlatCloudsTech[i]);
                m_RenderFlatCloudsTech[i] = null;
            }
        }

        if( m_CloudAttribs.bVolumetricBlending != NewAttribs.bVolumetricBlending )
        {
            m_CloudAttribs.bVolumetricBlending = NewAttribs.bVolumetricBlending;
            CommonUtil.safeRelease(m_RenderCloudsTech[0]);
            m_RenderCloudsTech[0] = null;
        }

        if( m_CloudAttribs.uiDensityGenerationMethod != NewAttribs.uiDensityGenerationMethod )
        {
            m_CloudAttribs.uiDensityGenerationMethod = NewAttribs.uiDensityGenerationMethod;
//            m_ptex3DPrecomputedParticleDensitySRV.Release();
//            m_ComputeOpticalDepthTech.Release();
            CommonUtil.safeRelease(m_ptex3DPrecomputedParticleDensitySRV);
            CommonUtil.safeRelease(m_ComputeOpticalDepthTech);
            m_ptex3DPrecomputedParticleDensitySRV = null;
            m_ComputeOpticalDepthTech = null;
        }

        // Process cloud grid
        if( m_ProcessCloudGridTech == null )
        {
//            CD3DShaderMacroHelper Macros;
//            DefineMacros(Macros);
//            Macros.AddShaderMacro("THREAD_GROUP_SIZE", sm_iCSThreadGroupSize);
//            Macros.Finalize();
//            m_ProcessCloudGridTech.SetDeviceAndContext(pDevice, pDeviceContext);
//            m_ProcessCloudGridTech.CreateComputeShaderFromFile(m_strEffectPath, "ProcessCloudGridCS", Macros);

            m_ProcessCloudGridTech = new CRenderTechnique(null, "ProcessCloudGridCS.comp", DefineMacros());
        }

//        UpdateConstantBuffer(pDeviceContext, m_pcbGlobalCloudAttribs, &m_CloudAttribs, sizeof(m_CloudAttribs));
    }

    // Renders cloud color, transparency and distance to clouds from camera
    void RenderScreenSpaceDensityAndColor(SRenderAttribs RenderAttribs){
//        ID3D11DeviceContext *pDeviceContext = RenderAttribs.pDeviceContext;
//        ID3D11Device *pDevice = RenderAttribs.pDevice;
//
        if( m_ptex3DPrecomputedParticleDensitySRV == null)
        {
            PrecomputParticleDensity(/*pDevice, pDeviceContext*/);
        }

        if(m_ptex3DSingleSctrInParticleLUT_SRV == null || m_ptex3DMultipleSctrInParticleLUT_SRV == null )
        {
            PrecomputeScatteringInParticle();
        }
        GenerateParticles(RenderAttribs);

        GLCheck.checkError();
        m_CloudAttribs.fTime = RenderAttribs.fCurrTime;
        m_CloudAttribs.f4Parameter.x = (float)RenderAttribs.iCascadeIndex;
//        UpdateConstantBuffer(pDeviceContext, m_pcbGlobalCloudAttribs, &m_CloudAttribs, sizeof(m_CloudAttribs));
//
//        ID3D11Buffer *pCBs[] = {m_pcbGlobalCloudAttribs, RenderAttribs.pcMediaScatteringParams, RenderAttribs.pcbCameraAttribs, RenderAttribs.pcbLightAttribs};
//        pDeviceContext->VSSetConstantBuffers(0, _countof(pCBs), pCBs);
//        pDeviceContext->GSSetConstantBuffers(0, _countof(pCBs), pCBs);
//        pDeviceContext->PSSetConstantBuffers(0, _countof(pCBs), pCBs);
//        pDeviceContext->CSSetConstantBuffers(0, _countof(pCBs), pCBs);
//
//        ID3D11SamplerState *pSamplers[] = {m_psamLinearClamp, m_psamLinearWrap, m_psamPointWrap};
//        pDeviceContext->VSSetSamplers(0, _countof(pSamplers), pSamplers);
//        pDeviceContext->GSSetSamplers(0, _countof(pSamplers), pSamplers);
//        pDeviceContext->PSSetSamplers(0, _countof(pSamplers), pSamplers);
//        pDeviceContext->CSSetSamplers(0, _countof(pSamplers), pSamplers);
//
//        CComPtr<ID3D11RenderTargetView> pOrigRTV;
//        CComPtr<ID3D11DepthStencilView> pOrigDSV;
//        pDeviceContext->OMGetRenderTargets(1, &pOrigRTV, &pOrigDSV);
//
//        D3D11_VIEWPORT OrigViewPort;
//        UINT iNumOldViewports = 1;
//        pDeviceContext->RSGetViewports(&iNumOldViewports, &OrigViewPort);
//
//        float Zero[4]={0,0,0,FLT_MIN};
//        pDeviceContext->ClearRenderTargetView(m_ptex2DScreenCloudColorRTV, Zero);
//
//        float fOneMinusEpsilon = 1.f;
//        --((int&)fOneMinusEpsilon);
//        const float One[4] = {1, 1, 1, fOneMinusEpsilon}; // Use 1-Epsilon to block fast clear path
//        pDeviceContext->ClearRenderTargetView(m_ptex2DScrSpaceCloudTransparencyRTV, One);
//
        if( m_bPSOrderingAvailable && m_CloudAttribs.bVolumetricBlending )
        {
//            CComPtr<ID3D11Resource> pDstRes;
//            m_pbufParticleLayersUAV->GetResource(&pDstRes);
//            pDeviceContext->CopyResource(pDstRes, m_pbufClearParticleLayers);
            // TODO
        }
//        // With complimentary depth buffer 0 is the far clipping plane
//        // TODO: output some distance from shader (or clear with distanc to horizon?). (Do not forget about sample refinement!)
//        const float InitialMinMaxZ[4] = {+FLT_MAX, -FLT_MAX, 0, 0};
//        pDeviceContext->ClearRenderTargetView(m_ptex2DScrSpaceDistToCloudRTV, InitialMinMaxZ);
//
//        RenderAttribs.bLightSpacePass = false;
        if(m_CloudAttribs.uiDownscaleFactor > 1 )
        {
//            D3D11_VIEWPORT NewViewPort = OrigViewPort;
//            NewViewPort.Width  = m_CloudAttribs.fDownscaledBackBufferWidth;
//            NewViewPort.Height = m_CloudAttribs.fDownscaledBackBufferHeight;
//            pDeviceContext->RSSetViewports(1, &NewViewPort);
            gl.glViewport(0,0, m_CloudAttribs.uiDownscaledBackBufferWidth, m_CloudAttribs.uiDownscaledBackBufferWidth);
            GLCheck.checkError();

//            pDeviceContext->ClearRenderTargetView(m_ptex2DDownscaledScrCloudColorRTV, Zero);
//            pDeviceContext->ClearRenderTargetView(m_ptex2DDownscaledScrCloudTransparencyRTV, One);
//            pDeviceContext->ClearRenderTargetView(m_ptex2DDownscaledScrDistToCloudRTV, InitialMinMaxZ);
            Texture2D pRTVs[] = {m_ptex2DDownscaledScrCloudTransparencyRTV, m_ptex2DDownscaledScrDistToCloudRTV, m_ptex2DDownscaledScrCloudColorRTV};
            for(int i = 0; i < pRTVs.length;i++){
                m_AttachDescs[i].index = i;
                m_AttachDescs[i].level = 0;
                m_AttachDescs[i].layer = 0;
                m_AttachDescs[i].type = AttachType.TEXTURE_2D;
            }

            m_RenderTarget.bind();
            m_RenderTarget.setRenderTextures(pRTVs, m_AttachDescs);
            gl.glClearBufferfv(GLenum.GL_COLOR, 0, CacheBuffer.wrap(1.0f, 1.0f, 1.0f, Float.floatToIntBits(1.0f) - 1));
            gl.glClearBufferfv(GLenum.GL_COLOR, 1, CacheBuffer.wrap(+FLT_MAX, -FLT_MAX, 0, 0));
            gl.glClearBufferfv(GLenum.GL_COLOR, 2, CacheBuffer.wrap(0,0,0,Float.MIN_NORMAL));

            if( m_bPSOrderingAvailable && m_CloudAttribs.bVolumetricBlending )
            {
//                ID3D11UnorderedAccessView *pUAVs[] = {m_pbufParticleLayersUAV};
//                UINT puiInitialCounts[_countof(pUAVs)] = {0};
//                pDeviceContext->OMSetRenderTargetsAndUnorderedAccessViews(_countof(pRTVs), pRTVs, nullptr, 3, _countof(pUAVs), pUAVs, puiInitialCounts);
                // TODO
            }
            else
            {
//                pDeviceContext->OMSetRenderTargets(_countof(pRTVs), pRTVs, nullptr);

            }
            RenderParticles(RenderAttribs);

//            pDeviceContext->RSSetViewports(1, &OrigViewPort);
        }
//
        Texture2D pRTVs[] = {m_ptex2DScrSpaceCloudTransparencyRTV, m_ptex2DScrSpaceDistToCloudRTV, m_ptex2DScreenCloudColorRTV};
//        pDeviceContext->OMSetRenderTargets(_countof(pRTVs), pRTVs, nullptr);
        for(int i = 0; i < pRTVs.length;i++){
            m_AttachDescs[i].index = i;
            m_AttachDescs[i].level = 0;
            m_AttachDescs[i].layer = 0;
            m_AttachDescs[i].type = AttachType.TEXTURE_2D;
        }

        m_RenderTarget.bind();
        m_RenderTarget.setRenderTextures(pRTVs, m_AttachDescs);
        gl.glClearBufferfv(GLenum.GL_COLOR, 0, CacheBuffer.wrap(1.0f, 1.0f, 1.0f, Float.floatToIntBits(1.0f) - 1));
        gl.glClearBufferfv(GLenum.GL_COLOR, 1, CacheBuffer.wrap(+FLT_MAX, -FLT_MAX, 0, 0));
        gl.glClearBufferfv(GLenum.GL_COLOR, 2, CacheBuffer.wrap(0,0,0,Float.MIN_NORMAL));
        gl.glViewport(0,0,pRTVs[0].getWidth(), pRTVs[0].getHeight());
//
        RenderFlatClouds(RenderAttribs);
        if(m_CloudAttribs.uiDownscaleFactor == 1 )
        {
            if( m_bPSOrderingAvailable && m_CloudAttribs.bVolumetricBlending )
            {
//                Texture2D pRTVs[] = {m_ptex2DScrSpaceCloudTransparencyRTV, nullptr, m_ptex2DScreenCloudColorRTV};
                pRTVs[0] = m_ptex2DScrSpaceCloudTransparencyRTV;
                pRTVs[1] = null;
                pRTVs[2] = m_ptex2DScreenCloudColorRTV;
//                ID3D11UnorderedAccessView *pUAVs[] = {m_pbufParticleLayersUAV};
//                UINT puiInitialCounts[_countof(pUAVs)] = {0};
//                pDeviceContext->OMSetRenderTargetsAndUnorderedAccessViews(_countof(pRTVs), pRTVs, nullptr, 3, _countof(pUAVs), pUAVs, puiInitialCounts);
                m_RenderTarget.setRenderTextures(pRTVs, m_AttachDescs);
                // TODO unorderedAcccessView
            }
            RenderParticles(RenderAttribs);
        }
//
//        pDeviceContext->OMSetRenderTargets(1, &pOrigRTV.p, pOrigDSV);
//        pDeviceContext->RSSetViewports(iNumOldViewports, &OrigViewPort);
    }

    // Renders light space density from light
    void RenderLightSpaceDensity(SRenderAttribs RenderAttribs){
//        ID3D11DeviceContext *pDeviceContext = RenderAttribs.pDeviceContext;
//        ID3D11Device *pDevice = RenderAttribs.pDevice;
//
        m_CloudAttribs.fTime = RenderAttribs.fCurrTime;
        m_CloudAttribs.f4Parameter.x = RenderAttribs.iCascadeIndex;
//        UpdateConstantBuffer(pDeviceContext, m_pcbGlobalCloudAttribs, &m_CloudAttribs, sizeof(m_CloudAttribs));
//
//        ID3D11Buffer *pCBs[] = {m_pcbGlobalCloudAttribs, RenderAttribs.pcMediaScatteringParams, RenderAttribs.pcbCameraAttribs, RenderAttribs.pcbLightAttribs};
//        pDeviceContext->VSSetConstantBuffers(0, _countof(pCBs), pCBs);
//        pDeviceContext->GSSetConstantBuffers(0, _countof(pCBs), pCBs);
//        pDeviceContext->PSSetConstantBuffers(0, _countof(pCBs), pCBs);
//        pDeviceContext->CSSetConstantBuffers(0, _countof(pCBs), pCBs);
//
//        ID3D11SamplerState *pSamplers[] = {m_psamLinearClamp, m_psamLinearWrap, m_psamPointWrap};
//        pDeviceContext->VSSetSamplers(0, _countof(pSamplers), pSamplers);
//        pDeviceContext->GSSetSamplers(0, _countof(pSamplers), pSamplers);
//        pDeviceContext->PSSetSamplers(0, _countof(pSamplers), pSamplers);
//        pDeviceContext->CSSetSamplers(0, _countof(pSamplers), pSamplers);
//
//
//        ID3D11RenderTargetView *ppOrigRTVs[2];
//        CComPtr<ID3D11DepthStencilView> pOrigDSV;
//        pDeviceContext->OMGetRenderTargets(_countof(ppOrigRTVs), ppOrigRTVs, &pOrigDSV);
//        CComPtr<ID3D11RenderTargetView> pTransparencyRTV, pMinMaxDepthRTV;
//        pTransparencyRTV.Attach(ppOrigRTVs[0]);
//        pMinMaxDepthRTV.Attach(ppOrigRTVs[1]);
//
        m_CloudAttribs.f2LiSpCloudDensityDim.x = RenderAttribs.uiLiSpCloudDensityDim;
        m_CloudAttribs.f2LiSpCloudDensityDim.y = RenderAttribs.uiLiSpCloudDensityDim;
//
        float fOneMinusEpsilon = 1.f;
//        --((int&)fOneMinusEpsilon);
//        const float One[4] = {1, 1, 1, fOneMinusEpsilon};
//        pDeviceContext->ClearRenderTargetView(pTransparencyRTV, One);
//
//        const float InitialMinMaxDepth[4] = {0, 0, 0, FLT_MIN};
//        pDeviceContext->ClearRenderTargetView(pMinMaxDepthRTV, InitialMinMaxDepth);
        gl.glClearBufferfv(GLenum.GL_COLOR, 0, CacheBuffer.wrap(1, 1, 1, fOneMinusEpsilon - 1));
        gl.glClearBufferfv(GLenum.GL_COLOR, 1, CacheBuffer.wrap(0, 0, 0, Float.MIN_NORMAL));
//
        RenderAttribs.bLightSpacePass = 1;
        RenderFlatClouds(RenderAttribs);
    }

    // Merges light space distance to cloud with the shadow map
    void MergeLiSpDensityWithShadowMap(SRenderAttribs RenderAttribs){
//        ID3D11DeviceContext *pDeviceContext = RenderAttribs.pDeviceContext;
//        ID3D11Device *pDevice = RenderAttribs.pDevice;
//
        if( m_RenderCloudDetphToShadowMap == null )
        {
//            CD3DShaderMacroHelper Macros;
//            DefineMacros(Macros);
//            Macros.Finalize();
//            m_RenderCloudDetphToShadowMap.SetDeviceAndContext(pDevice, pDeviceContext);
//            m_RenderCloudDetphToShadowMap.CreateVGPShadersFromFile(m_strEffectPath, "ScreenSizeQuadVS", nullptr, "RenderCloudDepthToShadowMapPS", Macros);
//            m_RenderCloudDetphToShadowMap.SetDS( m_pdsEnableDepth );
//            m_RenderCloudDetphToShadowMap.SetRS( m_prsSolidFillNoCull );
//            m_RenderCloudDetphToShadowMap.SetBS( m_pbsDefault );
            m_RenderCloudDetphToShadowMap = new CRenderTechnique("RenderCloudDepthToShadowMapPS.frag", DefineMacros());
        }
//
//        CComPtr<ID3D11RenderTargetView> pOrigRTV;
//        CComPtr<ID3D11DepthStencilView> pOrigDSV;
//        pDeviceContext->OMGetRenderTargets(1, &pOrigRTV, &pOrigDSV);
//
//        D3D11_VIEWPORT OrigViewPort;
//        UINT iNumOldViewports = 1;
//        pDeviceContext->RSGetViewports(&iNumOldViewports, &OrigViewPort);
//
//        pDeviceContext->OMSetRenderTargets(0, nullptr, RenderAttribs.pShadowMapDSV);
        m_RenderTarget.bind();
        m_AttachDescs[0].index = 0;
        m_AttachDescs[0].layer = 0;
        m_AttachDescs[0].level = 0;
        m_AttachDescs[0].type = AttachType.TEXTURE_2D;
        m_RenderTarget.setRenderTexture(RenderAttribs.pShadowMapDSV, m_AttachDescs[0]);
        m_pdsEnableDepth.run();

//        ID3D11ShaderResourceView *pSRVs[] =
//        {
//            RenderAttribs.pLiSpCloudTransparencySRV,
//                    RenderAttribs.pLiSpCloudMinMaxDepthSRV
//        };
//        pDeviceContext->PSSetShaderResources(0, _countof(pSRVs), pSRVs);
//
//        ID3D11SamplerState *pSamplers[] = {m_psamLinearClamp, m_psamLinearWrap, m_psamPointWrap};
//        pDeviceContext->VSSetSamplers(0, _countof(pSamplers), pSamplers);
//        pDeviceContext->PSSetSamplers(0, _countof(pSamplers), pSamplers);
//
        m_CloudAttribs.f4Parameter.x = (float)RenderAttribs.iCascadeIndex;
//        UpdateConstantBuffer(pDeviceContext, m_pcbGlobalCloudAttribs, &m_CloudAttribs, sizeof(m_CloudAttribs));
//
//        ID3D11Buffer *pCBs[] = {m_pcbGlobalCloudAttribs};
//        pDeviceContext->PSSetConstantBuffers(0, _countof(pCBs), pCBs);
//
        RenderQuad(/*pDeviceContext,*/ m_RenderCloudDetphToShadowMap);
//
//        pDeviceContext->OMSetRenderTargets(1, &pOrigRTV.p, pOrigDSV);
//        pDeviceContext->RSSetViewports(iNumOldViewports, &OrigViewPort);
    }

    // Combines cloud color & transparancy with back buffer
    void CombineWithBackBuffer(//ID3D11Device *pDevice,
                               //ID3D11DeviceContext *pDeviceContext,
                               Texture2D pDepthBufferSRV,
                               Texture2D pBackBufferSRV){
        if( m_CombineWithBBTech == null )
        {
//            CD3DShaderMacroHelper Macros;
//            DefineMacros(Macros);
//            Macros.Finalize();
//            m_CombineWithBBTech.SetDeviceAndContext(pDevice, pDeviceContext);
//            m_CombineWithBBTech.CreateVGPShadersFromFile(m_strEffectPath, "ScreenSizeQuadVS", nullptr, "CombineWithBackBufferPS", Macros);
//            m_CombineWithBBTech.SetDS( m_pdsDisableDepth );
//            m_CombineWithBBTech.SetRS( m_prsSolidFillNoCull );
//            m_CombineWithBBTech.SetBS( m_pbsDefault );

            m_CombineWithBBTech = new CRenderTechnique("CombineWithBackBufferPS.frag", DefineMacros());
        }

//        ID3D11ShaderResourceView *pSRVs[] =
//        {
//            pDepthBufferSRV,
//                    pBackBufferSRV
//        };
//        pDeviceContext->PSSetShaderResources(0, _countof(pSRVs), pSRVs);
//
//        ID3D11ShaderResourceView *pSRVs2[] =
//        {
//            m_ptex2DScrSpaceCloudTransparencySRV,
//                    m_ptex2DScrSpaceDistToCloudSRV,
//                    m_ptex2DScreenCloudColorSRV
//        };
//        pDeviceContext->PSSetShaderResources(11, _countof(pSRVs2), pSRVs2);

        RenderQuad(/*pDeviceContext,*/ m_CombineWithBBTech, m_uiBackBufferWidth, m_uiBackBufferHeight);
    }

    void OnResize(/*ID3D11Device *pDevice,*/int uiWidth, int uiHeight){
        m_uiBackBufferWidth  = uiWidth;
        m_uiBackBufferHeight = uiHeight;
        int uiDownscaledWidth  = m_uiBackBufferWidth /m_CloudAttribs.uiDownscaleFactor;
        int uiDownscaledHeight = m_uiBackBufferHeight/m_CloudAttribs.uiDownscaleFactor;
        m_CloudAttribs.uiBackBufferWidth = m_uiBackBufferWidth;
        m_CloudAttribs.uiBackBufferHeight = m_uiBackBufferHeight;
        m_CloudAttribs.uiDownscaledBackBufferWidth  = uiDownscaledWidth;
        m_CloudAttribs.uiDownscaledBackBufferHeight = uiDownscaledHeight;

//        m_CloudAttribs.fBackBufferWidth  = (float)m_uiBackBufferWidth;
//        m_CloudAttribs.fBackBufferHeight = (float)m_uiBackBufferHeight;
//        m_CloudAttribs.fDownscaledBackBufferWidth  = (float)uiDownscaledWidth;
//        m_CloudAttribs.fDownscaledBackBufferHeight = (float)uiDownscaledHeight;

        // Release existing resources
        CommonUtil.safeRelease(m_ptex2DScreenCloudColorSRV);
        CommonUtil.safeRelease(m_ptex2DScreenCloudColorRTV);
        CommonUtil.safeRelease(m_ptex2DScrSpaceCloudTransparencySRV);
        CommonUtil.safeRelease(m_ptex2DScrSpaceCloudTransparencyRTV);
        CommonUtil.safeRelease(m_ptex2DScrSpaceDistToCloudSRV);
        CommonUtil.safeRelease(m_ptex2DScrSpaceDistToCloudRTV);

        CommonUtil.safeRelease(m_ptex2DDownscaledScrCloudColorSRV);
        CommonUtil.safeRelease(m_ptex2DDownscaledScrCloudColorRTV);
        CommonUtil.safeRelease(m_ptex2DDownscaledScrCloudTransparencySRV);
        CommonUtil.safeRelease(m_ptex2DDownscaledScrCloudTransparencyRTV);
        CommonUtil.safeRelease(m_ptex2DDownscaledScrDistToCloudSRV);
        CommonUtil.safeRelease(m_ptex2DDownscaledScrDistToCloudRTV);

//        CommonUtil.safeRelease(m_pbufParticleLayersSRV);
//        CommonUtil.safeRelease(m_pbufParticleLayersUAV);
//        m_pbufClearParticleLayers TODO

        // Create screen space cloud color buffer
        Texture2DDesc ScreenCloudColorTexDesc = new Texture2DDesc
        (
            m_uiBackBufferWidth,                //UINT Width;
            m_uiBackBufferHeight,               //UINT Height;
            1,                                  //UINT MipLevels;
            1,                                  //UINT ArraySize;
            GLenum.GL_R11F_G11F_B10F,           //DXGI_FORMAT Format;
            1                                   //DXGI_SAMPLE_DESC SampleDesc;
//                    D3D11_USAGE_DEFAULT,                //D3D11_USAGE Usage;
//                    D3D11_BIND_RENDER_TARGET | D3D11_BIND_SHADER_RESOURCE,           //UINT BindFlags;
//                    0,                                  //UINT CPUAccessFlags;
//                    0,                                  //UINT MiscFlags;
        );

//        HRESULT hr;
//        CComPtr<ID3D11Texture2D> ptex2DScreenCloudColor;
//        V( pDevice->CreateTexture2D(&ScreenCloudColorTexDesc, nullptr, &ptex2DScreenCloudColor) );
//        V( pDevice->CreateShaderResourceView(ptex2DScreenCloudColor, nullptr, &m_ptex2DScreenCloudColorSRV) );
//        V( pDevice->CreateRenderTargetView(ptex2DScreenCloudColor, nullptr, &m_ptex2DScreenCloudColorRTV) );
        m_ptex2DScreenCloudColorSRV = TextureUtils.createTexture2D(ScreenCloudColorTexDesc, null);
        m_ptex2DScreenCloudColorRTV = m_ptex2DScreenCloudColorSRV;

        if( m_CloudAttribs.uiDownscaleFactor > 1 )
        {
            // Create downscaled screen space cloud color buffer
            Texture2DDesc DownscaledScreenCloudColorTexDesc = ScreenCloudColorTexDesc;
            DownscaledScreenCloudColorTexDesc.width  /= m_CloudAttribs.uiDownscaleFactor;
            DownscaledScreenCloudColorTexDesc.height /= m_CloudAttribs.uiDownscaleFactor;
//            CComPtr<ID3D11Texture2D> ptex2DDownscaledScrCloudColor;
//            V( pDevice->CreateTexture2D(&DownscaledScreenCloudColorTexDesc, nullptr, &ptex2DDownscaledScrCloudColor) );
//            V( pDevice->CreateShaderResourceView(ptex2DDownscaledScrCloudColor, nullptr, &m_ptex2DDownscaledScrCloudColorSRV) );
//            V( pDevice->CreateRenderTargetView(ptex2DDownscaledScrCloudColor, nullptr, &m_ptex2DDownscaledScrCloudColorRTV) );
            m_ptex2DDownscaledScrCloudColorSRV = TextureUtils.createTexture2D(DownscaledScreenCloudColorTexDesc, null);
            m_ptex2DDownscaledScrCloudColorRTV = m_ptex2DDownscaledScrCloudColorSRV;
        }

        {
            // Create screen space cloud transparency buffer
            Texture2DDesc ScreenTransparencyTexDesc = ScreenCloudColorTexDesc;
            ScreenTransparencyTexDesc.format = GLenum.GL_R8;
            ScreenTransparencyTexDesc.width = m_uiBackBufferWidth;
            ScreenTransparencyTexDesc.height = m_uiBackBufferHeight;
//            CComPtr<ID3D11Texture2D> ptex2DScreenTransparency;
//            V( pDevice->CreateTexture2D(&ScreenTransparencyTexDesc, nullptr, &ptex2DScreenTransparency) );
//            V( pDevice->CreateShaderResourceView(ptex2DScreenTransparency, nullptr, &m_ptex2DScrSpaceCloudTransparencySRV) );
//            V( pDevice->CreateRenderTargetView(ptex2DScreenTransparency, nullptr, &m_ptex2DScrSpaceCloudTransparencyRTV) );
            m_ptex2DScrSpaceCloudTransparencySRV = TextureUtils.createTexture2D(ScreenTransparencyTexDesc, null);
            m_ptex2DScrSpaceCloudTransparencyRTV = m_ptex2DScrSpaceCloudTransparencySRV;

            if( m_CloudAttribs.uiDownscaleFactor > 1 )
            {
                // Create downscaled screen space cloud transparency buffer
                ScreenTransparencyTexDesc.width  /= m_CloudAttribs.uiDownscaleFactor;
                ScreenTransparencyTexDesc.height /= m_CloudAttribs.uiDownscaleFactor;
//                CComPtr<ID3D11Texture2D> ptex2DDownscaledScrTransparency;
//                V( pDevice->CreateTexture2D(&ScreenTransparencyTexDesc, nullptr, &ptex2DDownscaledScrTransparency) );
//                V( pDevice->CreateShaderResourceView(ptex2DDownscaledScrTransparency, nullptr, &m_ptex2DDownscaledScrCloudTransparencySRV) );
//                V( pDevice->CreateRenderTargetView(ptex2DDownscaledScrTransparency, nullptr, &m_ptex2DDownscaledScrCloudTransparencyRTV) );
                m_ptex2DDownscaledScrCloudTransparencySRV = TextureUtils.createTexture2D(ScreenTransparencyTexDesc, null);
                m_ptex2DDownscaledScrCloudTransparencyRTV = m_ptex2DDownscaledScrCloudTransparencySRV;
            }
        }

        {
            // Create screen space distance to cloud buffer
            Texture2DDesc ScreenDistToCloudTexDesc = ScreenCloudColorTexDesc;
            ScreenDistToCloudTexDesc.format = GLenum.GL_R32F; // We need only the closest distance to camera
            ScreenDistToCloudTexDesc.width = m_uiBackBufferWidth;
            ScreenDistToCloudTexDesc.height = m_uiBackBufferHeight;

//            CComPtr<ID3D11Texture2D> ptex2DScrSpaceDistToCloud;
//            V( pDevice->CreateTexture2D(&ScreenDistToCloudTexDesc, nullptr, &ptex2DScrSpaceDistToCloud) );
//            V( pDevice->CreateShaderResourceView(ptex2DScrSpaceDistToCloud, nullptr, &m_ptex2DScrSpaceDistToCloudSRV) );
//            V( pDevice->CreateRenderTargetView(ptex2DScrSpaceDistToCloud, nullptr, &m_ptex2DScrSpaceDistToCloudRTV) );
            m_ptex2DScrSpaceDistToCloudSRV = TextureUtils.createTexture2D(ScreenDistToCloudTexDesc, null);
            m_ptex2DScrSpaceDistToCloudRTV = m_ptex2DScrSpaceDistToCloudSRV;
            if( m_CloudAttribs.uiDownscaleFactor > 1 )
            {
                // Create downscaled screen space distance to cloud buffer
                ScreenDistToCloudTexDesc.width  /= m_CloudAttribs.uiDownscaleFactor;
                ScreenDistToCloudTexDesc.height /= m_CloudAttribs.uiDownscaleFactor;
//                CComPtr<ID3D11Texture2D> ptex2DDownscaledScrDistToCloud;
//                V( pDevice->CreateTexture2D(&ScreenDistToCloudTexDesc, nullptr, &ptex2DDownscaledScrDistToCloud) );
//                V( pDevice->CreateShaderResourceView(ptex2DDownscaledScrDistToCloud, nullptr, &m_ptex2DDownscaledScrDistToCloudSRV) );
//                V( pDevice->CreateRenderTargetView(ptex2DDownscaledScrDistToCloud, nullptr, &m_ptex2DDownscaledScrDistToCloudRTV) );

                m_ptex2DDownscaledScrDistToCloudSRV = TextureUtils.createTexture2D(ScreenDistToCloudTexDesc, null);
                m_ptex2DDownscaledScrDistToCloudRTV = m_ptex2DDownscaledScrDistToCloudSRV;
            }
        }

        if( m_bPSOrderingAvailable )
        {
            int iNumElements = (m_uiBackBufferWidth  / m_CloudAttribs.uiDownscaleFactor) *
                    (m_uiBackBufferHeight / m_CloudAttribs.uiDownscaleFactor) *
                    m_CloudAttribs.uiNumParticleLayers;
//            D3D11_BUFFER_DESC ParticleLayersBufDesc =
//                    {
//                            iNumElements * sizeof(SParticleLayer), //UINT ByteWidth;
//                            D3D11_USAGE_DEFAULT,                    //D3D11_USAGE Usage;
//                            D3D11_BIND_SHADER_RESOURCE | D3D11_BIND_UNORDERED_ACCESS, //UINT BindFlags;
//                            0,                                      //UINT CPUAccessFlags;
//                            D3D11_RESOURCE_MISC_BUFFER_STRUCTURED,  //UINT MiscFlags;
//                            sizeof(SParticleLayer)               //UINT StructureByteStride;
//                    };
//
//            CComPtr<ID3D11Buffer> pbufParticleLayers;
//            V(pDevice->CreateBuffer( &ParticleLayersBufDesc, nullptr, &pbufParticleLayers));
//            D3D11_SHADER_RESOURCE_VIEW_DESC SRVDesc;
//            ZeroMemory(&SRVDesc, sizeof(SRVDesc));
//            SRVDesc.Format = DXGI_FORMAT_UNKNOWN;
//            SRVDesc.ViewDimension = D3D11_SRV_DIMENSION_BUFFER;
//            SRVDesc.Buffer.FirstElement = 0;
//            SRVDesc.Buffer.NumElements = iNumElements;
//            V(pDevice->CreateShaderResourceView( pbufParticleLayers, &SRVDesc, &m_pbufParticleLayersSRV));
//            D3D11_UNORDERED_ACCESS_VIEW_DESC UAVDesc;
//            UAVDesc.Format = DXGI_FORMAT_UNKNOWN;
//            UAVDesc.ViewDimension = D3D11_UAV_DIMENSION_BUFFER;
//            UAVDesc.Buffer.FirstElement = 0;
//            UAVDesc.Buffer.NumElements = iNumElements;
//            UAVDesc.Buffer.Flags = 0;
//            V(pDevice->CreateUnorderedAccessView( pbufParticleLayers, &UAVDesc, &m_pbufParticleLayersUAV));

            {
                int buffer = gl.glGenBuffer();
                gl.glBindBuffer(GLenum.GL_TEXTURE_BUFFER, buffer);
                gl.glBufferData(GLenum.GL_TEXTURE_BUFFER, SParticleLayer.SIZE * iNumElements, GLenum.GL_DYNAMIC_COPY);
                gl.glBindBuffer(GLenum.GL_TEXTURE_BUFFER, 0);

                m_pbufParticleLayersSRV = gl.glGenTexture();
                gl.glBindTexture(GLenum.GL_TEXTURE_BUFFER, m_pbufParticleLayersSRV);
                gl.glTexBuffer(GLenum.GL_TEXTURE_BUFFER, GLenum.GL_RGBA32I, m_pbufParticleLayersSRV);
                gl.glBindTexture(GLenum.GL_TEXTURE_BUFFER, 0);
                m_pbufParticleLayersUAV = m_pbufParticleLayersSRV;

//                m_pIntermediateSetupUAV = gl.glGenTexture();
//                gl.glBindTexture(GLenum.GL_TEXTURE_BUFFER, m_pIntermediateSetupUAV);
//                gl.glTexBuffer(GLenum.GL_TEXTURE_BUFFER, GLenum.GL_R32I, m_pIntermediateBuffer);
//                gl.glBindTexture(GLenum.GL_TEXTURE_BUFFER, 0);
            }

//            std::vector<SParticleLayer> ClearLayers(iNumElements);
//            ParticleLayersBufDesc.Usage = D3D11_USAGE_IMMUTABLE;
//            ParticleLayersBufDesc.BindFlags = D3D11_BIND_SHADER_RESOURCE;
//            D3D11_SUBRESOURCE_DATA InitData = {&ClearLayers[0],0,0};
//            V(pDevice->CreateBuffer( &ParticleLayersBufDesc, &InitData, &m_pbufClearParticleLayers));
        }
    }

    Texture2D GetScrSpaceCloudColor(){return m_ptex2DScreenCloudColorSRV;}
    Texture2D GetScrSpaceCloudTransparency(){return m_ptex2DScrSpaceCloudTransparencySRV;}
    Texture2D GetScrSpaceCloudMinMaxDist(){return m_ptex2DScrSpaceDistToCloudSRV;}
    SGlobalCloudAttribs GetCloudAttribs(){return m_CloudAttribs;}
    boolean IsPSOrderingAvailable(){return m_bPSOrderingAvailable;}

    private Macro[] DefineMacros(){
        if(m_Processers == null){
            List<Macro> macros = new ArrayList<>();
            macros.add(new Macro("CLOUD_DENSITY_TEX_DIM", "float2("+m_uiCloudDensityTexWidth+","+m_uiCloudDensityTexHeight+")"));
            macros.add(new Macro("OPTICAL_DEPTH_LUT_DIM", "float4("+ m_PrecomputedOpticalDepthTexDim.iNumStartPosZenithAngles  +","
                                                + m_PrecomputedOpticalDepthTexDim.iNumStartPosAzimuthAngles +","
                                                + m_PrecomputedOpticalDepthTexDim.iNumDirectionZenithAngles +","
                                                + m_PrecomputedOpticalDepthTexDim.iNumDirectionAzimuthAngles+ ")"));

            macros.add(new Macro("NUM_PARTICLE_LAYERS", m_CloudAttribs.uiNumParticleLayers));
            macros.add(new Macro("PS_ORDERING_AVAILABLE", m_bPSOrderingAvailable));

            macros.add(new Macro("VOL_SCATTERING_IN_PARTICLE_LUT_DIM", "float4("+ m_PrecomputedSctrInParticleLUTDim.iNumStartPosZenithAngles +","
                                                + m_PrecomputedSctrInParticleLUTDim.iNumViewDirAzimuthAngles +","
                                                + m_PrecomputedSctrInParticleLUTDim.iNumViewDirZenithAngles +","
                                                + m_PrecomputedSctrInParticleLUTDim.iNumDistancesFromCenter + ")"));

            macros.add(new Macro("SRF_SCATTERING_IN_PARTICLE_LUT_DIM", "float3("+ m_PrecomputedSctrInParticleLUTDim.iNumStartPosZenithAngles +","
                                                + m_PrecomputedSctrInParticleLUTDim.iNumViewDirAzimuthAngles +","
                                                + m_PrecomputedSctrInParticleLUTDim.iNumViewDirZenithAngles/2+")"));

            macros.add(new Macro("BACK_BUFFER_DOWNSCALE_FACTOR", m_CloudAttribs.uiDownscaleFactor));
            macros.add(new Macro("THREAD_GROUP_SIZE", sm_iCSThreadGroupSize));

            m_Processers = macros.toArray(new Macro[macros.size()]);
        }

        return m_Processers;
    }
    private void RenderMaxDensityMip(//ID3D11Device *pDevice,
                             //ID3D11DeviceContext *pDeviceContext,
                             Texture2D ptex2DMaxDensityMipMap,
                             Texture2D ptex2DTmpMaxDensityMipMap,
                             Texture2DDesc MaxCloudDensityMipDesc){
//        HRESULT hr;
//        CD3DShaderMacroHelper Macros;
//        DefineMacros(Macros);
//        Macros.Finalize();

        m_RenderTarget.bind();
        // Create techniques
        CRenderTechnique RenderMaxDensityLevel0Tech = new CRenderTechnique("RenderMaxMipLevel0PS.frag", DefineMacros());
//        RenderMaxDensityLevel0Tech.SetDeviceAndContext(pDevice, pDeviceContext);
//        RenderMaxDensityLevel0Tech.CreateVGPShadersFromFile(m_strEffectPath, "ScreenSizeQuadVS", nullptr, "RenderMaxMipLevel0PS", Macros);
//        RenderMaxDensityLevel0Tech.SetDS( m_pdsDisableDepth );
//        RenderMaxDensityLevel0Tech.SetRS( m_prsSolidFillNoCull );
//        RenderMaxDensityLevel0Tech.SetBS( m_pbsDefault );

        CRenderTechnique RenderCoarseMaxMipLevelTech = new CRenderTechnique("RenderCoarseMaxMipLevelPS.frag", DefineMacros());
//        RenderCoarseMaxMipLevelTech.SetDeviceAndContext(pDevice, pDeviceContext);
//        RenderCoarseMaxMipLevelTech.CreateVGPShadersFromFile(m_strEffectPath, "ScreenSizeQuadVS", nullptr, "RenderCoarseMaxMipLevelPS", Macros);
//        RenderCoarseMaxMipLevelTech.SetDS( m_pdsDisableDepth );
//        RenderCoarseMaxMipLevelTech.SetRS( m_prsSolidFillNoCull );
//        RenderCoarseMaxMipLevelTech.SetBS( m_pbsDefault );

//        CComPtr<ID3D11RenderTargetView> pOrigRTV;
//        CComPtr<ID3D11DepthStencilView> pOrigDSV;
//        pDeviceContext->OMGetRenderTargets(1, &pOrigRTV, &pOrigDSV);

//        D3D11_VIEWPORT OrigViewPort;
//        UINT iNumOldViewports = 1;
//        pDeviceContext->RSGetViewports(&iNumOldViewports, &OrigViewPort);

        int uiCurrMipWidth = MaxCloudDensityMipDesc.width;
        int uiCurrMipHeight = MaxCloudDensityMipDesc.height;
        for(int uiMip = 0; uiMip < MaxCloudDensityMipDesc.mipLevels; ++uiMip)
        {
//            D3D11_RENDER_TARGET_VIEW_DESC RTVDesc;
//            RTVDesc.Format = MaxCloudDensityMipDesc.Format;
//            RTVDesc.ViewDimension = D3D11_RTV_DIMENSION_TEXTURE2D;
//            RTVDesc.Texture2D.MipSlice = uiMip;
//            CComPtr<ID3D11RenderTargetView> ptex2DTmpMaxDensityMipMapRTV;
//            V(pDevice->CreateRenderTargetView(ptex2DTmpMaxDensityMipMap, &RTVDesc, &ptex2DTmpMaxDensityMipMapRTV));
//
//            pDeviceContext->OMSetRenderTargets(1, &ptex2DTmpMaxDensityMipMapRTV.p, nullptr);
            TextureAttachDesc attachDesc = m_AttachDescs[0];
            attachDesc.index = 0;
            attachDesc.level = uiMip;
            attachDesc.layer = 0;
            attachDesc.type = AttachType.TEXTURE_2D;
            m_RenderTarget.setRenderTexture(ptex2DTmpMaxDensityMipMap, attachDesc);

//            ID3D11SamplerState *pSamplers[] = {m_psamPointWrap};
//            pDeviceContext->PSSetSamplers(2, _countof(pSamplers), pSamplers);  TODO samplers

            m_CloudAttribs.f4Parameter.x = (float)uiMip;
//            UpdateConstantBuffer(pDeviceContext, m_pcbGlobalCloudAttribs, &m_CloudAttribs, sizeof(m_CloudAttribs));

//            ID3D11Buffer *pCBs[] = {m_pcbGlobalCloudAttribs};
//            pDeviceContext->PSSetConstantBuffers(0, _countof(pCBs), pCBs);

            if(uiMip == 0)
            {
//                ID3D11ShaderResourceView *pSRVs[] = {m_ptex2DCloudDensitySRV};
//                pDeviceContext->PSSetShaderResources(1, _countof(pSRVs), pSRVs);
                bindTexture(CRenderTechnique.TEX2D_CLOUD_DENSITY, m_ptex2DCloudDensitySRV, m_psamPointWrap);
            }
            else
            {
//                ID3D11ShaderResourceView *pSRVs[] = {m_ptex2DMaxDensityMipMapSRV};
//                pDeviceContext->PSSetShaderResources(3, _countof(pSRVs), pSRVs);
                bindTexture(CRenderTechnique.TEX2D_MAX_DENSITY, m_ptex2DMaxDensityMipMapSRV, m_psamPointWrap);
            }

            RenderQuad(/*pDeviceContext,*/ uiMip == 0 ? RenderMaxDensityLevel0Tech : RenderCoarseMaxMipLevelTech, uiCurrMipWidth, uiCurrMipHeight);

//            pDeviceContext->CopySubresourceRegion(ptex2DMaxDensityMipMap, uiMip, 0,0,0, ptex2DTmpMaxDensityMipMap, uiMip, nullptr);
            gl.glCopyImageSubData(ptex2DTmpMaxDensityMipMap.getTexture(), ptex2DTmpMaxDensityMipMap.getTarget(), uiMip, 0,0,0,
                    ptex2DMaxDensityMipMap.getTexture(), ptex2DMaxDensityMipMap.getTarget(), uiMip, 0,0,0,
                    ptex2DTmpMaxDensityMipMap.getWidth(),ptex2DTmpMaxDensityMipMap.getHeight(), 1);

            uiCurrMipWidth /= 2;
            uiCurrMipHeight /= 2;
        }
        assert( uiCurrMipWidth == 0 && uiCurrMipHeight == 0 );

//        pDeviceContext->OMSetRenderTargets(1, &pOrigRTV.p, pOrigDSV);
//        pDeviceContext->RSSetViewports(iNumOldViewports, &OrigViewPort);
    }

    private void RenderQuad(CRenderTechnique state){
        RenderQuad(state, 1);
    }

    private void RenderQuad(CRenderTechnique state, int iNumInstances){
        gl.glBindVertexArray(m_DummyVAO);
        state.enable();
        state.setUniforms(m_CloudAttribs);
        if(iNumInstances <= 1){
            gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
        }else{
            gl.glDrawArraysInstanced(GLenum.GL_TRIANGLES, 0, 3, iNumInstances);
        }

        gl.glBindVertexArray(0);
        GLCheck.checkError();

        if(!m_printOnce){
            state.printPrograminfo();
        }
    }

    public void closeInfo(){
        m_printOnce = true;
    }

    private void RenderQuad(CRenderTechnique state, int iWidth /*= 0*/, int iHeight /*= 0*/){
        RenderQuad(state, iWidth, iHeight, 0,0,1);
    }

    private void RenderQuad(CRenderTechnique state, int iWidth /*= 0*/, int iHeight /*= 0*/, int iTopLeftX /*= 0*/, int iTopLeftY /*= 0*/){
        RenderQuad(state, iWidth, iHeight, iTopLeftX,iTopLeftY,1);
    }

    private void RenderQuad(CRenderTechnique state, int iWidth /*= 0*/, int iHeight /*= 0*/,
                            int iTopLeftX /*= 0*/, int iTopLeftY /*= 0*/, int iNumInstances){
        if(m_LastViewportWidth != iWidth || m_LastViewportHeight != iHeight
                || m_LastViewportX != iTopLeftX || m_LastViewportY != iTopLeftY){
            gl.glViewport(iTopLeftX, iTopLeftY, iWidth, iHeight);

            m_LastViewportX = iTopLeftX;
            m_LastViewportY = iTopLeftY;
            m_LastViewportWidth = iWidth;
            m_LastViewportHeight = iHeight;
        }

        RenderQuad(state,1);
    }

    private void bindTexture(int unit, TextureGL src, int sampler){
        gl.glActiveTexture(GLenum.GL_TEXTURE0 + unit);
        if(src != null)
            gl.glBindTexture(src.getTarget(), src.getTexture());
        else{
            gl.glBindTexture(GLenum.GL_TEXTURE_2D, 0);
        }
        gl.glBindSampler(unit, sampler);
    }

    private void RenderFlatClouds(SRenderAttribs RenderAttribs){
//        ID3D11DeviceContext *pDeviceContext = RenderAttribs.pDeviceContext;
//        ID3D11Device *pDevice = RenderAttribs.pDevice;
//
        boolean bLightSpacePass = RenderAttribs.bLightSpacePass != 0;

        CRenderTechnique RenderFlatCloudsTech = m_RenderFlatCloudsTech[bLightSpacePass ? 1 : 0];
        if(RenderFlatCloudsTech == null)
        {
            Macro[] macros = DefineMacros();
            int length = macros.length;
            macros = Arrays.copyOf(macros, length + 3);
            macros[length++] = new Macro("LIGHT_SPACE_PASS", RenderAttribs.bLightSpacePass);
            macros[length++] = new Macro("NUM_SHADOW_CASCADES", m_CloudAttribs.uiNumCascades);
            macros[length++] = new Macro("BEST_CASCADE_SEARCH", false);

//            RenderFlatCloudsTech.SetDeviceAndContext(pDevice, pDeviceContext);
//            RenderFlatCloudsTech.CreateVGPShadersFromFile(m_strEffectPath, "ScreenSizeQuadVS", nullptr, "RenderFlatCloudsPS", Macros);
//            RenderFlatCloudsTech.SetDS( m_pdsDisableDepth );
//            RenderFlatCloudsTech.SetRS( m_prsSolidFillNoCull );
//            RenderFlatCloudsTech.SetBS( m_pbsDefault );
            m_RenderFlatCloudsTech[bLightSpacePass ? 1 : 0] = RenderFlatCloudsTech = new CRenderTechnique("RenderFlatCloudsPS.frag", macros);
            RenderFlatCloudsTech.setName("RenderFlatClouds_" + (bLightSpacePass ? "LightPass": "RenderPass"));
        }


//        ID3D11ShaderResourceView *pSRVs[] =
//        {
//            RenderAttribs.pDepthBufferSRV,
//                    m_ptex2DCloudDensitySRV,
//                    nullptr,
//                    m_ptex2DMaxDensityMipMapSRV,             // Texture2D<float> g_tex2MaxDensityMip           : register(t3);
//                    m_ptex3DNoiseSRV,                        // Texture3D<float> g_tex3DNoise                  : register(t4);
//                    RenderAttribs.pPrecomputedNetDensitySRV, // Texture2D<float2> g_tex2DOccludedNetDensityToAtmTop : register( t5 );
//                    nullptr,                                 // t6
//                    RenderAttribs.pAmbientSkylightSRV        // t7
//        };
//        pDeviceContext->VSSetShaderResources(0, _countof(pSRVs), pSRVs);
//        pDeviceContext->PSSetShaderResources(0, _countof(pSRVs), pSRVs);
//
//        if( !bLightSpacePass && m_CloudAttribs.uiDownscaleFactor > 1 )
//        {
//            ID3D11ShaderResourceView *pSRVs2[] =
//            {
//                m_ptex2DDownscaledScrCloudTransparencySRV,
//                        m_ptex2DDownscaledScrDistToCloudSRV,
//                        m_ptex2DDownscaledScrCloudColorSRV
//            };
//            pDeviceContext->PSSetShaderResources(11, _countof(pSRVs2), pSRVs2);
//        }
        gl.glBindTextureUnit(3, m_ptex2DCloudDensitySRV.getTexture());
        gl.glBindSampler(3, m_psamLinearWrap);
        gl.glBindTextureUnit(0, RenderAttribs.pShadowMapDSV.getTexture());
        gl.glBindSampler(0, m_psamLinearClamp);
        gl.glDisable(GLenum.GL_CULL_FACE);
        gl.glDisable(GLenum.GL_DEPTH_TEST);

        RenderFlatCloudsTech.enable();
        RenderFlatCloudsTech.setWorldViewProj(RenderAttribs.ViewProjMatr);
        RenderFlatCloudsTech.setViewProjInv(RenderAttribs.viewProjInv);

        RenderQuad(/*pDeviceContext,*/ RenderFlatCloudsTech);

//        UnbindPSResources(pDeviceContext);
//        UnbindVSResources(pDeviceContext);
//        UnbindGSResources(pDeviceContext);

        gl.glBindTextureUnit(3, 0);
        gl.glBindTextureUnit(0, 0);
        gl.glBindSampler(0, 0);
        gl.glBindSampler(3, 0);
    }

    // Renders all visible particles
    private void RenderParticles(SRenderAttribs RenderAttribs){
//        ID3D11DeviceContext *pDeviceContext = RenderAttribs.pDeviceContext;
//        ID3D11Device *pDevice = RenderAttribs.pDevice;
//
        boolean bLightSpacePass = RenderAttribs.bLightSpacePass !=0;
//
        CRenderTechnique RenderCloudsTech = m_RenderCloudsTech[bLightSpacePass ? 1 : 0];
//
        if( RenderCloudsTech == null )
        {
            Macro[] macros = DefineMacros();
            int length = macros.length;
            macros = Arrays.copyOf(macros, length + 5);
            macros[length++] = new Macro("LIGHT_SPACE_PASS", bLightSpacePass);
            macros[length++] = new Macro("NUM_SHADOW_CASCADES", m_CloudAttribs.uiNumCascades);
            macros[length++] = new Macro("BEST_CASCADE_SEARCH", false);
            macros[length++] = new Macro("TILING_MODE", false);
            macros[length++] = new Macro("VOLUMETRIC_BLENDING", m_bPSOrderingAvailable && m_CloudAttribs.bVolumetricBlending);
//
//            RenderCloudsTech.SetDeviceAndContext(pDevice, pDeviceContext);
//            RenderCloudsTech.CreateVGPShadersFromFile(m_strEffectPath, "RenderCloudsVS", "RenderCloudsGS", "RenderCloudsPS", Macros);
//            RenderCloudsTech.SetDS( m_pdsDisableDepth /*m_pdsEnableDepth*/ );
//            RenderCloudsTech.SetRS( m_prsSolidFillCullFront );
//            RenderCloudsTech.SetBS( m_pbsRT0MulRT1MinRT2Over );
            RenderCloudsTech = m_RenderCloudsTech[bLightSpacePass ? 1 : 0] = new CRenderTechnique("RenderCloudsVS.vert", "RenderCloudsGS.gemo", "RenderCloudsPS.frag", macros);
//
            if(m_pRenderCloudsInputLayout == 0 )
            {
                // Create vertex input layout
//                const D3D11_INPUT_ELEMENT_DESC layout[] =
//                    {
//                            { "PARTICLE_ID",  0, DXGI_FORMAT_R32_UINT, 0, 0, D3D11_INPUT_PER_VERTEX_DATA, 0 }
//                    };
//
//                auto pVSByteCode = RenderCloudsTech.GetVSByteCode();
//                HRESULT hr;
//                V( pDevice->CreateInputLayout( layout, ARRAYSIZE( layout ),
//                        pVSByteCode->GetBufferPointer(),
//                        pVSByteCode->GetBufferSize(),
//                        &m_pRenderCloudsInputLayout ) );
                m_pRenderCloudsInputLayout = gl.glGenVertexArray();
                gl.glBindVertexArray(m_pRenderCloudsInputLayout);
                gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, m_pbufSerializedVisibleParticles);
                gl.glEnableVertexAttribArray(0);
                gl.glVertexAttribIPointer(0, 1, GLenum.GL_UNSIGNED_INT, 0,0);
                gl.glBindVertexArray(0);
                gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
            }
        }
//
        SortVisibileParticles(RenderAttribs);
//
//        ID3D11ShaderResourceView *pSRVs[] =
//        {
//            RenderAttribs.pDepthBufferSRV,
//                    m_ptex2DCloudDensitySRV,
//                    m_pbufCloudGridSRV,                // StructuredBuffer<SCloudCellAttribs> g_CloudCells : register( t2 );
//                    m_pbufCloudParticlesSRV,           // StructuredBuffer<SParticleAttribs> g_Particles : register( t3 );
//                    m_ptex3DCellDensitySRV,                  // Texture3D<float> g_tex3DNoise                  : register(t4);
//                    nullptr,							// t5
//                    m_ptex3DLightAttenuatingMassSRV,       // Texture3D<float>       g_tex3DLightAttenuatingMass      : register( t6 );
//                    m_pbufParticlesLightingSRV,         // t7
//                    nullptr,         // t8
//                    nullptr,         // t9
//                    m_ptex3DPrecomputedParticleDensitySRV, // t10
//                    m_ptex3DSingleSctrInParticleLUT_SRV,   // t11
//                    m_ptex3DMultipleSctrInParticleLUT_SRV  // t12
//        };

//
//        pDeviceContext->VSSetShaderResources(0, _countof(pSRVs), pSRVs);
//        pDeviceContext->GSSetShaderResources(0, _countof(pSRVs), pSRVs);
//        pDeviceContext->PSSetShaderResources(0, _countof(pSRVs), pSRVs);

        bindTexture(12, m_ptex3DPrecomputedParticleDensitySRV, m_psamLinearWrap);
//
//        pDeviceContext->IASetPrimitiveTopology(D3D11_PRIMITIVE_TOPOLOGY_POINTLIST);
//        RenderCloudsTech.Apply();
//        UINT Strides[] = {sizeof(UINT)};
//        UINT Offsets[] = {0};
//        pDeviceContext->IASetVertexBuffers(0, 1, &m_pbufSerializedVisibleParticles.p, Strides, Offsets);
//        pDeviceContext->IASetInputLayout(m_pRenderCloudsInputLayout);
//        pDeviceContext->DrawInstancedIndirect(m_pbufDrawIndirectArgs, 0);
        m_pbsRT0MulRT1MinRT2Over.run();
        gl.glBindVertexArray(m_pRenderCloudsInputLayout);
        RenderCloudsTech.enable();
        RenderCloudsTech.setUniforms(m_CloudAttribs);
        RenderCloudsTech.setViewProjInv(RenderAttribs.viewProjInv);
        RenderCloudsTech.setWorldViewProj(RenderAttribs.ViewProjMatr);

        gl.glBindBuffer(GLenum.GL_DRAW_INDIRECT_BUFFER, m_pbufDrawIndirectArgs);
        gl.glDrawArraysIndirect(GLenum.GL_POINTS, 0);
        gl.glBindBuffer(GLenum.GL_DRAW_INDIRECT_BUFFER, 0);
        gl.glBindVertexArray(0);
        if(!m_printOnce){
            RenderCloudsTech.printPrograminfo();
        }

//
        if( RenderAttribs.bLightSpacePass ==0 && m_bPSOrderingAvailable && m_CloudAttribs.bVolumetricBlending )
        {
            if( m_ApplyParticleLayersTech == null )
            {
//                CD3DShaderMacroHelper Macros;
//                DefineMacros(Macros);
//                Macros.Finalize();
//
//                m_ApplyParticleLayersTech.SetDeviceAndContext(pDevice, pDeviceContext);
//                m_ApplyParticleLayersTech.CreateVGPShadersFromFile(m_strEffectPath, "ScreenSizeQuadVS", nullptr, "ApplyParticleLayersPS", Macros);
//                m_ApplyParticleLayersTech.SetDS( m_pdsDisableDepth );
//                m_ApplyParticleLayersTech.SetRS( m_prsSolidFillNoCull );
//                m_ApplyParticleLayersTech.SetBS( m_pbsRT0MulRT1MinRT2Over );
                m_ApplyParticleLayersTech = new CRenderTechnique("ApplyParticleLayersPS.frag", DefineMacros());
            }
//
//            // We need to remove UAVs from the pipeline to be able to bind it as shader resource
            TextureGL pRTVs[] = {m_ptex2DScrSpaceCloudTransparencyRTV, m_ptex2DScrSpaceDistToCloudRTV, m_ptex2DScreenCloudColorRTV};
            TextureGL pDwnsclRTVs[] = {m_ptex2DDownscaledScrCloudTransparencyRTV, m_ptex2DDownscaledScrDistToCloudRTV, m_ptex2DDownscaledScrCloudColorRTV};
            for(int i = 0; i < pRTVs.length;i++){
                m_AttachDescs[i].index = i;
                m_AttachDescs[i].level = 0;
                m_AttachDescs[i].layer = 0;
                m_AttachDescs[i].type = AttachType.TEXTURE_2D;
            }

            m_RenderTarget.bind();
            if(m_CloudAttribs.uiDownscaleFactor > 1 ) {
//                pDeviceContext -> OMSetRenderTargets(_countof(pDwnsclRTVs), pDwnsclRTVs, nullptr);
                m_RenderTarget.setRenderTextures(pDwnsclRTVs, m_AttachDescs);
                gl.glViewport(0,0,pDwnsclRTVs[0].getWidth(), pDwnsclRTVs[0].getHeight());
            }else {
//                pDeviceContext -> OMSetRenderTargets(_countof(pRTVs), pRTVs, nullptr);
                m_RenderTarget.setRenderTextures(pRTVs, m_AttachDescs);
                gl.glViewport(0,0,pRTVs[0].getWidth(), pRTVs[0].getHeight());
            }
//
//            ID3D11ShaderResourceView *pSRVs[] =
//            {
//                m_pbufParticleLayersSRV
//            };
//            pDeviceContext->PSSetShaderResources(0, _countof(pSRVs), pSRVs);
//
            RenderQuad(/*pDeviceContext,*/ m_ApplyParticleLayersTech);
            gl.glDisable(GLenum.GL_BLEND);
        }
//
//        UnbindPSResources(pDeviceContext);
//        UnbindVSResources(pDeviceContext);
//        UnbindGSResources(pDeviceContext);
    }

    private void GenerateParticles(SRenderAttribs RenderAttribs){
//        ID3D11DeviceContext *pDeviceContext = RenderAttribs.pDeviceContext;
//        ID3D11Device *pDevice = RenderAttribs.pDevice;

//        ID3D11Buffer *pCBs[] = {m_pcbGlobalCloudAttribs, RenderAttribs.pcMediaScatteringParams, RenderAttribs.pcbCameraAttribs, RenderAttribs.pcbLightAttribs};
//        pDeviceContext->CSSetConstantBuffers(0, _countof(pCBs), pCBs);

//        ID3D11SamplerState *pSamplers[] = {m_psamLinearClamp, m_psamLinearWrap, m_psamPointWrap};
//        pDeviceContext->CSSetSamplers(0, _countof(pSamplers), pSamplers);

//        ID3D11ShaderResourceView *pSRVs[] =
//        {
//            m_pbufPackedCellLocationsSRV, // StructuredBuffer<uint> g_PackedCellLocations : register( t0 );
//                    m_ptex2DCloudDensitySRV,
//                    m_ptex3DNoiseSRV,
//                    m_ptex2DMaxDensityMipMapSRV,
//                    nullptr,
//                    nullptr, // Texture2D<float2> g_tex2DOccludedNetDensityToAtmTop : register( t5 );
//        };
//        pDeviceContext->CSSetShaderResources(0, _countof(pSRVs), pSRVs);

//        ID3D11UnorderedAccessView *pUAVs[] = {m_pbufCloudGridUAV, m_pbufValidCellsUnorderedListUAV, m_pbufVisibleCellsUnorderedListUAV};
//        UINT uiZeroCounters[_countof(pUAVs)] =  {0};
//        pDeviceContext->CSSetUnorderedAccessViews(0, _countof(pUAVs), pUAVs, uiZeroCounters);
        GLCheck.checkError();
        gl.glBindImageTexture(0, m_pbufPackedCellLocationsSRV, 0, false, 0, GLenum.GL_READ_ONLY, GLenum.GL_R32UI);
        bindTexture(3, m_ptex2DCloudDensitySRV, m_psamLinearWrap);
        bindTexture(6, m_ptex2DMaxDensityMipMapSRV, m_psamPointWrap);

        {// binding unordered resource views.
            gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 0, m_pbufCloudGridUAV);
            gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 1, m_pbufValidCellsUnorderedList);
            gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 2, m_pbufVisibleCellsUnorderedList);

            gl.glBindBuffer(GLenum.GL_ATOMIC_COUNTER_BUFFER, m_pbufAtomicCounter);
            gl.glClearBufferData(GLenum.GL_ATOMIC_COUNTER_BUFFER, GLenum.GL_R32UI, GLenum.GL_RED_INTEGER, GLenum.GL_UNSIGNED_INT, null);  // clear to zero
            gl.glBindBuffer(GLenum.GL_ATOMIC_COUNTER_BUFFER, 0);
            gl.glBindBufferBase(GLenum.GL_ATOMIC_COUNTER_BUFFER, 3, m_pbufAtomicCounter);
        }
        GLCheck.checkError();
        m_ProcessCloudGridTech.enable();
        m_ProcessCloudGridTech.setUniforms(m_CloudAttribs);
        m_ProcessCloudGridTech.setCameraPos(RenderAttribs.f3CameraPos);

//        pDeviceContext->Dispatch( (m_CloudAttribs.uiNumCells + (sm_iCSThreadGroupSize-1)) / sm_iCSThreadGroupSize, 1, 1);
        gl.glDispatchCompute((m_CloudAttribs.uiNumCells + (sm_iCSThreadGroupSize-1)) / sm_iCSThreadGroupSize, 1, 1);
        gl.glMemoryBarrier(GLenum.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
        if(!m_printOnce){
            m_ProcessCloudGridTech.printPrograminfo();
        }
        GLCheck.checkError();
//        memset(pUAVs, 0, sizeof(pUAVs));
//        pDeviceContext->CSSetUnorderedAccessViews(0, _countof(pUAVs), pUAVs, nullptr);
//        pDeviceContext->CopyStructureCount(m_pbufValidCellsCounter, 0, m_pbufValidCellsUnorderedListUAV);
        CopyStructureCount(m_pbufValidCellsCounter, m_pbufValidCellsUnorderedList, 16);

        GLCheck.checkError();
        gl.glBindImageTexture(0, 0, 0, false, 0, GLenum.GL_READ_ONLY, GLenum.GL_R32UI);
        bindTexture(3, null, 0);
        bindTexture(6, null, 0);

        {// unbind unordered resource views.
            gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 0, 0);
            gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 1, 0);
            gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 2, 0);
            gl.glBindBufferBase(GLenum.GL_ATOMIC_COUNTER_BUFFER, 3, 0);
        }
        GLCheck.checkError();
//        // It is more efficient to clear both UAVs simultaneously using CS
//        //ClearCellDensityAndAttenuationTextures(RenderAttribs);
//        float Zero[4]={0,0,0,0};
//        pDeviceContext->ClearUnorderedAccessViewFloat(m_ptex3DCellDensityUAV,Zero);
//        pDeviceContext->ClearUnorderedAccessViewFloat(m_ptex3DLightAttenuatingMassUAV,Zero);
        gl.glClearTexImage(m_ptex3DCellDensityUAV.getTexture(), 0,
                TextureUtils.measureFormat(m_ptex3DCellDensityUAV.getFormat()), TextureUtils.measureDataType(m_ptex3DCellDensityUAV.getFormat()), (ByteBuffer)null);
        gl.glClearTexImage(m_ptex3DLightAttenuatingMassUAV.getTexture(), 0,
                TextureUtils.measureFormat(m_ptex3DLightAttenuatingMassUAV.getFormat()), TextureUtils.measureDataType(m_ptex3DLightAttenuatingMassUAV.getFormat()), (ByteBuffer)null);
//
        if( m_EvaluateDensityTech == null )
        {
//            CD3DShaderMacroHelper Macros;
//            DefineMacros(Macros);
//            Macros.AddShaderMacro("THREAD_GROUP_SIZE", sm_iCSThreadGroupSize);
//            Macros.Finalize();
//
//            m_EvaluateDensityTech.SetDeviceAndContext(pDevice, pDeviceContext);
//            m_EvaluateDensityTech.CreateComputeShaderFromFile(m_strEffectPath, "EvaluateDensityCS", Macros);
            m_EvaluateDensityTech = new CRenderTechnique(null, "EvaluateDensityCS.comp", DefineMacros());
        }
//
//        // TODO: need to process only VISIBLE AND LIGHT OCCLUDING cells here!
        PrepareDispatchArgsBuffer(RenderAttribs, m_pbufValidCellsCounterSRV, 1);
//
//        pSRVs[0] = m_pbufValidCellsCounterSRV;       // Buffer<uint> g_ValidCellsCounter                 : register( t0 );
//        pSRVs[1] = m_pbufValidCellsUnorderedListSRV; // StructuredBuffer<uint> g_ValidCellsUnorderedList : register( t1 );
//        pSRVs[2] = m_pbufCloudGridSRV;               // StructuredBuffer<SCloudCellAttribs> g_CloudCells : register( t2 );
//        pSRVs[3] = nullptr;						     // t3
//        pSRVs[4] = m_ptex3DNoiseSRV;				 // t4
//        pDeviceContext->CSSetShaderResources(0, 5, pSRVs);
//        pUAVs[0] = m_ptex3DCellDensityUAV;
//        pDeviceContext->CSSetUnorderedAccessViews(0, 1, pUAVs, nullptr);

        bindTexture(5, m_ptex3DNoiseSRV, m_psamLinearWrap);
        gl.glBindImageTexture(5, m_pbufValidCellsCounterSRV, 0, false, 0, GLenum.GL_READ_ONLY, GLenum.GL_R32UI);
        gl.glBindImageTexture(0, m_ptex3DCellDensityUAV.getTexture(), 0, false, 0, GLenum.GL_WRITE_ONLY, m_ptex3DCellDensityUAV.getFormat());
        m_EvaluateDensityTech.enable();
        m_EvaluateDensityTech.setUniforms(m_CloudAttribs);
//        pDeviceContext->DispatchIndirect(m_pbufDispatchArgs, 0);
        gl.glBindBuffer(GLenum.GL_DISPATCH_INDIRECT_BUFFER, m_pbufDispatchArgs);
        gl.glDispatchComputeIndirect(0);
        gl.glMemoryBarrier(GLenum.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
        if(!m_printOnce){
            m_EvaluateDensityTech.printPrograminfo();
        }
        gl.glBindBuffer(GLenum.GL_DISPATCH_INDIRECT_BUFFER, 0);
        gl.glBindImageTexture(0, 0, 0, false, 0, GLenum.GL_WRITE_ONLY, m_ptex3DCellDensityUAV.getFormat());
        bindTexture(5, null, 0);
        gl.glBindImageTexture(5, 0, 0, false, 0, GLenum.GL_READ_ONLY, GLenum.GL_R32UI);
//        pUAVs[0] = nullptr;
//        pDeviceContext->CSSetUnorderedAccessViews(0, 1, pUAVs, nullptr);
//
//
//        pDeviceContext->CopyStructureCount(m_pbufValidCellsCounter, 0, m_pbufVisibleCellsUnorderedListUAV);
        CopyStructureCount(m_pbufValidCellsCounter, m_pbufVisibleCellsUnorderedList, 16);
        GLCheck.checkError();
        PrepareDispatchArgsBuffer(RenderAttribs, m_pbufValidCellsCounterSRV, 1);
//
        if( m_ComputeLightAttenuatingMass == null )
        {
//            CD3DShaderMacroHelper Macros;
//            DefineMacros(Macros);
//            Macros.AddShaderMacro("THREAD_GROUP_SIZE", sm_iCSThreadGroupSize);
//            Macros.Finalize();
//
//            m_ComputeLightAttenuatingMass.SetDeviceAndContext(pDevice, pDeviceContext);
//            m_ComputeLightAttenuatingMass.CreateComputeShaderFromFile(m_strEffectPath, "ComputeLightAttenuatingMassCS", Macros);
            m_ComputeLightAttenuatingMass = new CRenderTechnique(null, "ComputeLightAttenuatingMassCS.comp", DefineMacros());
        }
//
//        pSRVs[0] = m_pbufValidCellsCounterSRV;       // Buffer<uint> g_ValidCellsCounter                 : register( t0 );
//        pSRVs[1] = m_pbufVisibleCellsUnorderedListSRV; // StructuredBuffer<uint> g_ValidCellsUnorderedList : register( t1 );
//        pSRVs[2] = m_pbufCloudGridSRV;               // StructuredBuffer<SCloudCellAttribs> g_CloudCells : register( t2 );
//        pSRVs[3] = nullptr;						     // t3
//        pSRVs[4] = m_ptex3DCellDensitySRV;			 // t4
//        pDeviceContext->CSSetShaderResources(0, 5, pSRVs);
//        pUAVs[0] = m_ptex3DLightAttenuatingMassUAV;
//        pDeviceContext->CSSetUnorderedAccessViews(0, 1, pUAVs, nullptr);

        bindTexture(8, m_ptex3DCellDensitySRV, m_psamLinearClamp);
        gl.glBindImageTexture(5, m_pbufValidCellsCounterSRV, 0, false, 0, GLenum.GL_READ_ONLY, GLenum.GL_R32UI);
        gl.glBindImageTexture(0, m_ptex3DLightAttenuatingMassUAV.getTexture(), 0, false, 0, GLenum.GL_WRITE_ONLY, m_ptex3DLightAttenuatingMassUAV.getFormat());

        m_ComputeLightAttenuatingMass.enable();
        m_ComputeLightAttenuatingMass.setUniforms(m_CloudAttribs);
        m_ComputeLightAttenuatingMass.setCameraPos(RenderAttribs.f3CameraPos);
        m_ComputeLightAttenuatingMass.setDirOnLight(RenderAttribs.f4DirOnLight);

        gl.glBindBuffer(GLenum.GL_DISPATCH_INDIRECT_BUFFER, m_pbufDispatchArgs);
        gl.glDispatchComputeIndirect( 0);
        gl.glMemoryBarrier(GLenum.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
        gl.glBindBuffer(GLenum.GL_DISPATCH_INDIRECT_BUFFER, 0);
        if(!m_printOnce){
            m_ComputeLightAttenuatingMass.printPrograminfo();
        }
//        pUAVs[0] = nullptr;
//        pDeviceContext->CSSetUnorderedAccessViews(0, 1, pUAVs, nullptr);
        bindTexture(8, null, 0);
        gl.glBindImageTexture(5, 0, 0, false, 0, GLenum.GL_READ_ONLY, GLenum.GL_R32UI);
        gl.glBindImageTexture(0, 0, 0, false, 0, GLenum.GL_WRITE_ONLY, m_ptex3DLightAttenuatingMassUAV.getFormat());
        GLCheck.checkError();

//
//        // Process all valid cells and generate visible particles
        if(m_GenerateVisibleParticlesTech == null)
        {
//            CD3DShaderMacroHelper Macros;
//            DefineMacros(Macros);
//            Macros.AddShaderMacro("THREAD_GROUP_SIZE", sm_iCSThreadGroupSize);
//            Macros.Finalize();
//
//            m_GenerateVisibleParticlesTech.SetDeviceAndContext(pDevice, pDeviceContext);
//            m_GenerateVisibleParticlesTech.CreateComputeShaderFromFile(m_strEffectPath, "GenerateVisibleParticlesCS", Macros);
            m_GenerateVisibleParticlesTech = new CRenderTechnique(null, "GenerateVisibleParticlesCS.comp", DefineMacros());
        }
//
//        // We now need to use the first method to calculate dispatch args
        PrepareDispatchArgsBuffer(RenderAttribs, m_pbufValidCellsCounterSRV, 0);
//
//        pUAVs[0] = m_pbufCloudParticlesUAV;
//        pUAVs[1] = m_pbufVisibleParticlesUnorderedListUAV;
//        pDeviceContext->CSSetUnorderedAccessViews(0, 2, pUAVs, uiZeroCounters);
//        pSRVs[0] = m_pbufValidCellsCounterSRV;       // Buffer<uint> g_ValidCellsCounter                 : register( t0 );
//        pSRVs[1] = m_pbufVisibleCellsUnorderedListSRV;// StructuredBuffer<uint> g_ValidCellsUnorderedList : register( t1 );
//        pSRVs[2] = m_pbufCloudGridSRV;               // StructuredBuffer<SCloudCellAttribs> g_CloudCells : register( t2 );
//        pSRVs[3] = m_ptex2DWhiteNoiseSRV;            // Texture2D<float3> g_tex2DWhiteNoise              : register( t3 );
//        pSRVs[4] = m_ptex3DCellDensitySRV;			 // t4
//        pDeviceContext->CSSetShaderResources(0, 5, pSRVs);

        bindTexture(4, m_ptex2DWhiteNoiseSRV, m_psamLinearWrap);
        bindTexture(8, m_ptex3DCellDensitySRV, m_psamLinearClamp);

        gl.glBindImageTexture(5, m_pbufValidCellsCounterSRV, 0, false, 0, GLenum.GL_READ_ONLY, GLenum.GL_R32UI);
        gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 0, m_pbufCloudParticlesUAV);
        gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 1, m_pbufVisibleParticlesUnorderedListUAV);

        gl.glBindBuffer(GLenum.GL_ATOMIC_COUNTER_BUFFER, m_pbufAtomicCounter);
        gl.glClearBufferData(GLenum.GL_ATOMIC_COUNTER_BUFFER, GLenum.GL_R32UI, GLenum.GL_RED_INTEGER, GLenum.GL_UNSIGNED_INT, null);  // clear to zero
        gl.glBindBuffer(GLenum.GL_ATOMIC_COUNTER_BUFFER, 0);
        gl.glBindBufferBase(GLenum.GL_ATOMIC_COUNTER_BUFFER, 2, m_pbufAtomicCounter);

        m_GenerateVisibleParticlesTech.enable();
        m_GenerateVisibleParticlesTech.setUniforms(m_CloudAttribs);
        m_GenerateVisibleParticlesTech.setCameraPos(RenderAttribs.f3CameraPos);
        m_GenerateVisibleParticlesTech.setViewFrustumPlanes(RenderAttribs.f4ViewFrustumPlanes);

        gl.glBindBuffer(GLenum.GL_DISPATCH_INDIRECT_BUFFER, m_pbufDispatchArgs);
        gl.glDispatchComputeIndirect( 0);
        gl.glMemoryBarrier(GLenum.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
        gl.glBindBuffer(GLenum.GL_DISPATCH_INDIRECT_BUFFER, 0);
        if(!m_printOnce){
            m_GenerateVisibleParticlesTech.printPrograminfo();
        }

//        memset(pUAVs, 0, sizeof(pUAVs));
//        pDeviceContext->CSSetUnorderedAccessViews(0, 2, pUAVs, nullptr);
        bindTexture(4, null, 0);
        bindTexture(8, null, 0);

        gl.glBindImageTexture(5, 0, 0, false, 0, GLenum.GL_READ_ONLY, GLenum.GL_R32UI);
        gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 0, 0);
        gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 1, 0);
        gl.glBindBufferBase(GLenum.GL_ATOMIC_COUNTER_BUFFER, 2, 0);
        GLCheck.checkError();
//
//        pDeviceContext->CopyStructureCount(m_pbufVisibleParticlesCounter, 0, m_pbufVisibleParticlesUnorderedListUAV);
        CopyStructureCount(m_pbufVisibleParticlesCounter, m_pbufVisibleParticlesUnorderedListUAV, 16);
//
        {
//            // Process all valid cells and generate visible particles
            if(m_ProcessVisibleParticlesTech == null)
            {
//                CD3DShaderMacroHelper Macros;
//                DefineMacros(Macros);
//                Macros.AddShaderMacro("THREAD_GROUP_SIZE", sm_iCSThreadGroupSize);
//                Macros.Finalize();
//
//                m_ProcessVisibleParticlesTech.SetDeviceAndContext(pDevice, pDeviceContext);
//                m_ProcessVisibleParticlesTech.CreateComputeShaderFromFile(m_strEffectPath, "ProcessVisibleParticlesCS", Macros);
                m_ProcessVisibleParticlesTech = new CRenderTechnique(null, "ProcessVisibleParticlesCS.comp", DefineMacros());
            }
//
            PrepareDispatchArgsBuffer(RenderAttribs, m_pbufVisibleParticlesCounterSRV, 0);
//            ID3D11UnorderedAccessView *pUAVs[] =
//            {
//                m_pbufParticlesLightingUAV
//            };
//
//            pDeviceContext->CSSetUnorderedAccessViews(0, _countof(pUAVs), pUAVs, uiZeroCounters);
//            ID3D11ShaderResourceView *pSRVs[] =
//            {
//                m_pbufVisibleParticlesCounterSRV,       // Buffer<uint> g_ValidCellsCounter                 : register( t0 );
//                        m_pbufVisibleParticlesUnorderedListSRV, // StructuredBuffer<SParticleIdAndDist>  g_VisibleParticlesUnorderedList : register( t1 );
//                        m_pbufCloudGridSRV,				        // StructuredBuffer<SCloudCellAttribs> g_CloudCells : register( t2 );
//                        m_pbufCloudParticlesSRV,	            // StructuredBuffer<SParticleAttribs>  g_Particles     : register( t3 );
//                        nullptr,					  		    // t4
//                        RenderAttribs.pPrecomputedNetDensitySRV,// Texture2D<float2> g_tex2DOccludedNetDensityToAtmTop : register( t5 );
//                        m_ptex3DLightAttenuatingMassSRV,        // Texture3D<float>       g_tex3DLightAttenuatingMass      : register( t6 );
//                        RenderAttribs.pAmbientSkylightSRV		// Texture2D<float3>       g_tex2DAmbientSkylight       : register( t7 );
//            };
//            pDeviceContext->CSSetShaderResources(0, _countof(pSRVs), pSRVs);

            // UnorderedAccessView
            gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 0, m_pbufParticlesLightingUAV);

            // ShaderResourceViews
            gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 1, m_pbufVisibleParticlesUnorderedListSRV);
            gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 2, m_pbufCloudGridSRV);
            gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 3, m_pbufCloudParticlesSRV);

            gl.glBindImageTexture(0, m_pbufVisibleParticlesCounterSRV, 0, false, 0, GLenum.GL_READ_ONLY, GLenum.GL_R32UI);
            bindTexture(0, null, 0); //TODO g_tex2DOccludedNetDensityToAtmTop
            bindTexture(7, m_ptex3DLightAttenuatingMassSRV, m_psamLinearClamp); //g_tex2DOccludedNetDensityToAtmTop
            bindTexture(9, null, 0); //TODO g_tex2DAmbientSkylight

            m_ProcessVisibleParticlesTech.enable();
            m_ProcessVisibleParticlesTech.setUniforms(m_CloudAttribs);
            m_ProcessVisibleParticlesTech.setCameraPos(RenderAttribs.f3CameraPos);
            m_ProcessVisibleParticlesTech.setDirOnLight(RenderAttribs.f4DirOnLight);

//            pDeviceContext->DispatchIndirect(m_pbufDispatchArgs, 0);
            gl.glBindBuffer(GLenum.GL_DISPATCH_INDIRECT_BUFFER, m_pbufDispatchArgs);
            gl.glDispatchComputeIndirect( 0);
            gl.glMemoryBarrier(GLenum.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
            gl.glBindBuffer(GLenum.GL_DISPATCH_INDIRECT_BUFFER, 0);
            if(!m_printOnce){
                m_ProcessVisibleParticlesTech.printPrograminfo();
            }
//            memset(pUAVs, 0, sizeof(pUAVs));
//            pDeviceContext->CSSetUnorderedAccessViews(0, _countof(pUAVs), pUAVs, nullptr);
//            memset(pSRVs, 0, sizeof(pSRVs));
//            pDeviceContext->CSSetShaderResources(0, _countof(pSRVs), pSRVs);

            gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 0, 0);
            gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 1, 0);
            gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 2, 0);
            gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 3, 0);
            gl.glBindImageTexture(0, 0, 0, false, 0, GLenum.GL_READ_ONLY, GLenum.GL_R32UI);
            bindTexture(0, null, 0); //g_tex2DOccludedNetDensityToAtmTop
            bindTexture(7, null, 0); //g_tex2DOccludedNetDensityToAtmTop
            bindTexture(9, null, 0); //g_tex2DAmbientSkylight

            GLCheck.checkError();
        }
    }

    private void PrepareDispatchArgsBuffer(SRenderAttribs RenderAttribs, int pCounterSRV, int iTechInd){
//        ID3D11DeviceContext *pDeviceContext = RenderAttribs.pDeviceContext;
//        ID3D11Device *pDevice = RenderAttribs.pDevice;

        CRenderTechnique ComputeDispatchArgsTech = m_ComputeDispatchArgsTech[iTechInd];
//        // Compute DispatchIndirect() arguments
        if(ComputeDispatchArgsTech == null )
        {
//            CD3DShaderMacroHelper Macros;
//            DefineMacros(Macros);
//            Macros.AddShaderMacro("THREAD_GROUP_SIZE", sm_iCSThreadGroupSize);
//            Macros.Finalize();
//
//            ComputeDispatchArgsTech.SetDeviceAndContext(pDevice, pDeviceContext);
//            std::stringstream ss;
//            ss << "ComputeDispatchArgs" << iTechInd << "CS";
//            ComputeDispatchArgsTech.CreateComputeShaderFromFile(m_strEffectPath, ss.str().c_str(), Macros);
            ComputeDispatchArgsTech = m_ComputeDispatchArgsTech[iTechInd] = new CRenderTechnique(null, String.format("ComputeDispatchArgs%dCS.comp", iTechInd), DefineMacros());
        }
//        ID3D11Buffer *pCBs[] = {m_pcbGlobalCloudAttribs};
//        pDeviceContext->CSSetConstantBuffers(0, _countof(pCBs), pCBs);
//        ID3D11ShaderResourceView *pSRVs[] = {pCounterSRV};
//        pDeviceContext->CSSetShaderResources(0, 1, pSRVs);
//        ID3D11UnorderedAccessView *pUAVs[] = {m_pbufDispatchArgsUAV};
//        pDeviceContext->CSSetUnorderedAccessViews(0, 1, pUAVs, nullptr);
        ComputeDispatchArgsTech.enable();
        ComputeDispatchArgsTech.setUniforms(m_CloudAttribs);

        gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 0, m_pbufDispatchArgs);  // Write buffer
        gl.glBindImageTexture(5, pCounterSRV, 0, false, 0, GLenum.GL_READ_ONLY, GLenum.GL_R32UI);  // Read buffer
        gl.glDispatchCompute(1,1,1);
        gl.glMemoryBarrier(GLenum.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
        if(!m_printOnce){
            ComputeDispatchArgsTech.printPrograminfo();
        }

        gl.glBindImageTexture(5, 0, 0, false, 0, GLenum.GL_READ_ONLY, GLenum.GL_R32UI);
        gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 0, 0);
//        pUAVs[0] = nullptr;
//        pDeviceContext->CSSetUnorderedAccessViews(0, 1, pUAVs, nullptr);
    }

    // Method sorts all visible particles and writes them into the buffer suitable for
    // binding as vertex buffer
    private void SortVisibileParticles(SRenderAttribs RenderAttribs){
//        ID3D11DeviceContext *pDeviceContext = RenderAttribs.pDeviceContext;
//        ID3D11Device *pDevice = RenderAttribs.pDevice;

        if( m_SortSubsequenceBitonicTech == null )
        {
//            CD3DShaderMacroHelper Macros;
//            DefineMacros(Macros);
//            Macros.AddShaderMacro("THREAD_GROUP_SIZE", sm_iCSThreadGroupSize);
//            Macros.Finalize();
//            m_SortSubsequenceBitonicTech.SetDeviceAndContext(pDevice, pDeviceContext);
//            m_SortSubsequenceBitonicTech.CreateComputeShaderFromFile(L"fx\\Sort.fx", "SortSubsequenceBitonicCS", Macros);
            m_SortSubsequenceBitonicTech = new CRenderTechnique(null, "SortSubsequenceBitonicCS.comp", DefineMacros());
        }

        if( m_WriteSortedPariclesToVBTech == null )
        {
//            CD3DShaderMacroHelper Macros;
//            DefineMacros(Macros);
//            Macros.AddShaderMacro("THREAD_GROUP_SIZE", sm_iCSThreadGroupSize);
//            Macros.Finalize();
//            m_WriteSortedPariclesToVBTech.SetDeviceAndContext(pDevice, pDeviceContext);
//            m_WriteSortedPariclesToVBTech.CreateComputeShaderFromFile(L"fx\\Sort.fx", "WriteSortedPariclesToVBCS", Macros);
            m_WriteSortedPariclesToVBTech = new CRenderTechnique(null, "WriteSortedPariclesToVBCS.comp", DefineMacros());
        }

        if( m_MergeSubsequencesTech == null )
        {
//            CD3DShaderMacroHelper Macros;
//            DefineMacros(Macros);
//            Macros.AddShaderMacro("THREAD_GROUP_SIZE", sm_iCSThreadGroupSize);
//            Macros.Finalize();
//            m_MergeSubsequencesTech.SetDeviceAndContext(pDevice, pDeviceContext);
//            m_MergeSubsequencesTech.CreateComputeShaderFromFile(L"fx\\Sort.fx", "MergeSubsequencesCS", Macros);
            m_MergeSubsequencesTech = new CRenderTechnique(null, "MergeSubsequencesCS.comp", DefineMacros());
        }

        PrepareDispatchArgsBuffer(RenderAttribs, m_pbufVisibleParticlesCounterSRV, 0);

        // Perform bitonic sorting of subsequences
        {
//            ID3D11ShaderResourceView *pSRVs[] = {m_pbufVisibleParticlesCounterSRV, m_pbufVisibleParticlesUnorderedListSRV};
//            pDeviceContext->CSSetShaderResources(0, _countof(pSRVs), pSRVs);
//            ID3D11UnorderedAccessView *pUAVs[] = {m_pbufVisibleParticlesSortedListUAV};
//            pDeviceContext->CSSetUnorderedAccessViews(0, 1, pUAVs, nullptr);
            gl.glBindImageTexture(0, m_pbufVisibleParticlesCounterSRV, 0,false, 0, GLenum.GL_READ_ONLY, GLenum.GL_R32UI);
            gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 1, m_pbufVisibleParticlesUnorderedListSRV);
            gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 0, m_pbufVisibleParticlesSortedListUAV);

            m_SortSubsequenceBitonicTech.enable();
            m_SortSubsequenceBitonicTech.setUniforms(m_CloudAttribs);
//            pDeviceContext->DispatchIndirect(m_pbufDispatchArgs, 0);
            gl.glBindBuffer(GLenum.GL_DISPATCH_INDIRECT_BUFFER, m_pbufDispatchArgs);
            gl.glDispatchComputeIndirect(0);
            gl.glMemoryBarrier(GLenum.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
            if(!m_printOnce){
                m_SortSubsequenceBitonicTech.printPrograminfo();
            }
            gl.glBindBuffer(GLenum.GL_DISPATCH_INDIRECT_BUFFER, 0);

            gl.glBindImageTexture(0, 0, 0,false, 0, GLenum.GL_READ_ONLY, GLenum.GL_R32UI);
            gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 1, 0);
            gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 0, 0);

//            pUAVs[0] = nullptr;
//            pDeviceContext->CSSetUnorderedAccessViews(0, 1, pUAVs, nullptr);
//            pSRVs[0] = nullptr;
//            pSRVs[1] = nullptr;
//            pDeviceContext->CSSetShaderResources(0, _countof(pSRVs), pSRVs);

        }

        // Merge sorted subsequences
        {
            // We do not know how many passes we need to perform, because only the GPU knows the particle counter
            // We thus perform enough passes to sort maximum possible particles. The last passes do nothing and
            // have very little performance impact
            for(int iSubseqLen = sm_iCSThreadGroupSize; iSubseqLen < m_CloudAttribs.uiMaxParticles; iSubseqLen*=2)
            {
                m_CloudAttribs.uiParameter = iSubseqLen;
//                UpdateConstantBuffer(pDeviceContext, m_pcbGlobalCloudAttribs, &m_CloudAttribs, sizeof(m_CloudAttribs));
//
//                ID3D11Buffer *pCBs[] = {m_pcbGlobalCloudAttribs};
//                pDeviceContext->CSSetConstantBuffers(0, _countof(pCBs), pCBs);
//
//                ID3D11ShaderResourceView *pSRVs[] = {m_pbufVisibleParticlesCounterSRV, m_pbufVisibleParticlesSortedListSRV};
//                pDeviceContext->CSSetShaderResources(0, _countof(pSRVs), pSRVs);
//                ID3D11UnorderedAccessView *pUAVs[] = {m_pbufVisibleParticlesMergedListUAV};
//                pDeviceContext->CSSetUnorderedAccessViews(0, 1, pUAVs, nullptr);
                m_MergeSubsequencesTech.enable();
                m_MergeSubsequencesTech.setUniforms(m_CloudAttribs);
//                pDeviceContext->DispatchIndirect(m_pbufDispatchArgs, 0);
                gl.glBindBuffer(GLenum.GL_DISPATCH_INDIRECT_BUFFER, m_pbufDispatchArgs);
                gl.glDispatchComputeIndirect(0);
                gl.glMemoryBarrier(GLenum.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
                if(!m_printOnce){
                    m_MergeSubsequencesTech.printPrograminfo();
                }
                gl.glBindBuffer(GLenum.GL_DISPATCH_INDIRECT_BUFFER, 0);
//                pUAVs[0] = nullptr;
//                pDeviceContext->CSSetUnorderedAccessViews(0, 1, pUAVs, nullptr);
//                pSRVs[0] = nullptr;
//                pSRVs[1] = nullptr;
//                pDeviceContext->CSSetShaderResources(0, _countof(pSRVs), pSRVs);
//                std::swap(m_pbufVisibleParticlesMergedListUAV, m_pbufVisibleParticlesSortedListUAV);
//                std::swap(m_pbufVisibleParticlesMergedListSRV, m_pbufVisibleParticlesSortedListSRV);

                int temp = m_pbufVisibleParticlesMergedListUAV;
                m_pbufVisibleParticlesMergedListUAV = m_pbufVisibleParticlesSortedListUAV;
                m_pbufVisibleParticlesSortedListUAV = temp;

                temp = m_pbufVisibleParticlesMergedListSRV;
                m_pbufVisibleParticlesMergedListSRV = m_pbufVisibleParticlesSortedListSRV;
                m_pbufVisibleParticlesSortedListSRV = temp;
            }
        }

        {
            // Write sorted particle indices into the buffer suitable for binding as vertex buffer
//            ID3D11ShaderResourceView *pSRVs[] = {m_pbufVisibleParticlesCounterSRV, m_pbufVisibleParticlesSortedListSRV};
//            pDeviceContext->CSSetShaderResources(0, _countof(pSRVs), pSRVs);
//            ID3D11UnorderedAccessView *pUAVs[] = {m_pbufSerializedVisibleParticlesUAV};
//            pDeviceContext->CSSetUnorderedAccessViews(0, 1, pUAVs, nullptr);
            m_WriteSortedPariclesToVBTech.enable();
            m_WriteSortedPariclesToVBTech.setUniforms(m_CloudAttribs);
//            pDeviceContext->DispatchIndirect(m_pbufDispatchArgs, 0);
            gl.glBindBuffer(GLenum.GL_DISPATCH_INDIRECT_BUFFER, m_pbufDispatchArgs);
            gl.glDispatchComputeIndirect(0);
            gl.glMemoryBarrier(GLenum.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
            if(!m_printOnce){
                m_WriteSortedPariclesToVBTech.printPrograminfo();
            }
            gl.glBindBuffer(GLenum.GL_DISPATCH_INDIRECT_BUFFER, 0);
//            pUAVs[0] = nullptr;
//            pDeviceContext->CSSetUnorderedAccessViews(0, 1, pUAVs, nullptr);
//            pSRVs[0] = nullptr;
//            pDeviceContext->CSSetShaderResources(0, 1, pSRVs);
        }

//        pDeviceContext->CopyStructureCount(m_pbufDrawIndirectArgs, 0, m_pbufVisibleParticlesUnorderedListUAV);
        CopyStructureCount(m_pbufDrawIndirectArgs, m_pbufVisibleParticlesUnorderedListUAV, 4);

    }

    private void CopyStructureCount(int dstBuffer, int srcBuffer, int length){
        gl.glBindBuffer(GLenum.GL_COPY_READ_BUFFER, srcBuffer);
        gl.glBindBuffer(GLenum.GL_COPY_WRITE_BUFFER, dstBuffer);
        gl.glCopyBufferSubData(GLenum.GL_COPY_READ_BUFFER, GLenum.GL_COPY_WRITE_BUFFER, 0,0, length);
        gl.glBindBuffer(GLenum.GL_COPY_READ_BUFFER, 0);
        gl.glBindBuffer(GLenum.GL_COPY_WRITE_BUFFER, 0);

        GLCheck.checkError();
    }

    // Auxiliary method which creates a buffer and views
    private void CreateBufferAndViews(//ID3D11Device *pDevice,
                                 /*const D3D11_BUFFER_DESC &BuffDesc,
                                 D3D11_SUBRESOURCE_DATA *pInitData,
                                 ID3D11Buffer **ppBuffer,
                                 ID3D11ShaderResourceView **ppSRV = nullptr,
                                 ID3D11UnorderedAccessView **ppUAV = nullptr,
                                 UINT UAVFlags = 0*/){
        // TODO
    }

    private interface CProc{
        void invoke(int iCol, int iRow, int iRing, int iLayer);
    }

    // This helper template function traverses the 3D cloud lattice
    static void TraverseCloudLattice(int iNumRings,
                              int iInnerRingDim,
                              int iRingExtension,
                              int iNumLayers,
                              CProc Proc)
    {
        int iRingDimension = iInnerRingDim + 2*iRingExtension;
        assert( (iInnerRingDim % 4) == 0 );
        for(int iRing = iNumRings-1; iRing >= 0; --iRing)
        {
            for(int iRow = 0; iRow < iRingDimension; ++iRow)
            {
                int iFirstQuart = iRingExtension + iInnerRingDim*1/4;
                int iThirdQuart = iRingExtension + iInnerRingDim*3/4;
                int iStartCol[] = {0,           iThirdQuart   };
                int iEndCol[]   = {iFirstQuart, iRingDimension};
                if( !(iRing > 0 && iRow >= iFirstQuart && iRow < iThirdQuart) )
                    iStartCol[1] = iEndCol[0];

                for(int i=0; i < _countof(iStartCol); ++i)
                    for(int iCol = iStartCol[i]; iCol < iEndCol[i]; ++iCol)
                        for(int iLayer = 0; iLayer < iNumLayers; ++iLayer)
                            Proc.invoke(iCol, iRow, iRing, iLayer);
            }
        }
    }

    private static final int _countof(int[] a) { return a.length;}
    private static final int _countof(Object[] a) { return a.length;}

    static int PackParticleIJRing(int i, int j, int ring, int layer)
    {
        return i | (j<<12) | (ring<<24) | (layer<<28);
    }


    // Method crates particle buffers
    private void CreateParticleDataBuffer(/*ID3D11Device *pDevice*/){
        m_CloudAttribs.uiRingDimension = m_CloudAttribs.uiInnerRingDim + m_CloudAttribs.uiRingExtension*2;

        // Populate cell locations array
        m_PackedCellLocations.clear();
        m_PackedCellLocations.reserve(m_CloudAttribs.uiRingDimension * m_CloudAttribs.uiRingDimension * m_CloudAttribs.uiNumRings);
        TraverseCloudLattice(m_CloudAttribs.uiNumRings, m_CloudAttribs.uiInnerRingDim, m_CloudAttribs.uiRingExtension, 1,
                (int i, int j, int ring, int layer)-> m_PackedCellLocations.push( PackParticleIJRing(i,j,ring, layer) )
        );
        m_CloudAttribs.uiNumCells = m_PackedCellLocations.size();

        // Populate particle locations array
        m_PackedParticleLocations.clear();
        m_PackedParticleLocations.reserve(m_CloudAttribs.uiNumCells * m_CloudAttribs.uiMaxLayers);
        TraverseCloudLattice(m_CloudAttribs.uiNumRings, m_CloudAttribs.uiInnerRingDim, m_CloudAttribs.uiRingExtension, m_CloudAttribs.uiMaxLayers,
                (int i, int j, int ring, int layer)-> m_PackedParticleLocations.push( PackParticleIJRing(i,j,ring, layer) )
        );
        m_CloudAttribs.uiMaxParticles = m_PackedParticleLocations.size();

        // Create cloud cell attributes buffer
        {
//            D3D11_BUFFER_DESC CloudGridBuffDesc =
//                    {
//                            m_CloudAttribs.uiNumCells * sizeof(SCloudCellAttribs), //UINT ByteWidth;
//                            D3D11_USAGE_DEFAULT,                    //D3D11_USAGE Usage;
//                            D3D11_BIND_SHADER_RESOURCE | D3D11_BIND_UNORDERED_ACCESS, //UINT BindFlags;
//                            0,                                      //UINT CPUAccessFlags;
//                            D3D11_RESOURCE_MISC_BUFFER_STRUCTURED,  //UINT MiscFlags;
//                            sizeof(SCloudCellAttribs)  //UINT StructureByteStride;
//                    };
//            m_pbufCloudGridSRV.Release();
//            m_pbufCloudGridUAV.Release();
//            V( CreateBufferAndViews( pDevice, CloudGridBuffDesc, nullptr, nullptr, &m_pbufCloudGridSRV, &m_pbufCloudGridUAV ) );
            m_pbufCloudGridUAV = gl.glGenBuffer();
            gl.glBindBuffer(GLenum.GL_SHADER_STORAGE_BUFFER, m_pbufCloudGridUAV);
            gl.glBufferData(GLenum.GL_SHADER_STORAGE_BUFFER, SCloudCellAttribs.SIZE * m_CloudAttribs.uiNumCells, GLenum.GL_DYNAMIC_COPY);
            gl.glBindBuffer(GLenum.GL_SHADER_STORAGE_BUFFER, 0);
            m_pbufCloudGridSRV = m_pbufCloudGridUAV;
        }

        // Create particle attributes buffer
        {
//            D3D11_BUFFER_DESC ParticleBuffDesc =
//                    {
//                            m_CloudAttribs.uiMaxParticles * sizeof(SParticleAttribs), //UINT ByteWidth;
//                            D3D11_USAGE_DEFAULT,                    //D3D11_USAGE Usage;
//                            D3D11_BIND_SHADER_RESOURCE | D3D11_BIND_UNORDERED_ACCESS, //UINT BindFlags;
//                            0,                                      //UINT CPUAccessFlags;
//                            D3D11_RESOURCE_MISC_BUFFER_STRUCTURED,  //UINT MiscFlags;
//                            sizeof(SParticleAttribs)  //UINT StructureByteStride;
//                    };
//            m_pbufCloudParticlesSRV.Release();
//            m_pbufCloudParticlesUAV.Release();
//            V( CreateBufferAndViews( pDevice, ParticleBuffDesc, nullptr, nullptr, &m_pbufCloudParticlesSRV, &m_pbufCloudParticlesUAV ) );

            m_pbufCloudParticlesUAV = gl.glGenBuffer();
            gl.glBindBuffer(GLenum.GL_SHADER_STORAGE_BUFFER, m_pbufCloudParticlesUAV);
            gl.glBufferData(GLenum.GL_SHADER_STORAGE_BUFFER, SParticleAttribs.SIZE * m_CloudAttribs.uiMaxParticles, GLenum.GL_DYNAMIC_COPY);
            gl.glBindBuffer(GLenum.GL_SHADER_STORAGE_BUFFER, 0);
            m_pbufCloudParticlesSRV = m_pbufCloudParticlesUAV;
        }

        // Create buffer for storing particle lighting info
        {
//            D3D11_BUFFER_DESC LightingBuffDesc =
//                    {
//                            m_CloudAttribs.uiMaxParticles * sizeof(SCloudParticleLighting), //UINT ByteWidth;
//                            D3D11_USAGE_DEFAULT,                    //D3D11_USAGE Usage;
//                            D3D11_BIND_SHADER_RESOURCE | D3D11_BIND_UNORDERED_ACCESS, //UINT BindFlags;
//                            0,                                      //UINT CPUAccessFlags;
//                            D3D11_RESOURCE_MISC_BUFFER_STRUCTURED,  //UINT MiscFlags;
//                            sizeof(SCloudParticleLighting)			//UINT StructureByteStride;
//                    };
//            m_pbufParticlesLightingSRV.Release();
//            m_pbufParticlesLightingUAV.Release();
//            V( CreateBufferAndViews( pDevice, LightingBuffDesc, nullptr, nullptr, &m_pbufParticlesLightingSRV, &m_pbufParticlesLightingUAV) );
            m_pbufParticlesLightingSRV = gl.glGenBuffer();
            gl.glBindBuffer(GLenum.GL_SHADER_STORAGE_BUFFER, m_pbufParticlesLightingSRV);
            gl.glBufferData(GLenum.GL_SHADER_STORAGE_BUFFER, SCloudParticleLighting.SIZE * m_CloudAttribs.uiMaxParticles, GLenum.GL_DYNAMIC_COPY);
            gl.glBindBuffer(GLenum.GL_SHADER_STORAGE_BUFFER, 0);
            m_pbufParticlesLightingUAV = m_pbufParticlesLightingSRV;

        }

        // Create buffer for storing cell locations
        {
//            D3D11_BUFFER_DESC PackedCellLocationsBuffDesc =
//                    {
//                            m_CloudAttribs.uiNumCells * sizeof(UINT), //UINT ByteWidth;
//                            D3D11_USAGE_IMMUTABLE,                  //D3D11_USAGE Usage;
//                            D3D11_BIND_SHADER_RESOURCE,             //UINT BindFlags;
//                            0,                                      //UINT CPUAccessFlags;
//                            D3D11_RESOURCE_MISC_BUFFER_STRUCTURED,  //UINT MiscFlags;
//                            sizeof(UINT)                            //UINT StructureByteStride;
//                    };
//
//            m_pbufPackedCellLocationsSRV.Release();
//            D3D11_SUBRESOURCE_DATA InitData = {&m_PackedCellLocations[0], 0, 0};
//            CreateBufferAndViews( pDevice, PackedCellLocationsBuffDesc, &InitData, nullptr, &m_pbufPackedCellLocationsSRV, nullptr);
            int pbufPackedCellLocations = gl.glGenBuffer();
            gl.glBindBuffer(GLenum.GL_TEXTURE_BUFFER, pbufPackedCellLocations);
            gl.glBufferData(GLenum.GL_TEXTURE_BUFFER, CacheBuffer.wrap(m_PackedCellLocations.getData(), 0, m_PackedCellLocations.size()), GLenum.GL_STATIC_READ);
            gl.glBindBuffer(GLenum.GL_TEXTURE_BUFFER, 0);

            m_pbufPackedCellLocationsSRV = gl.glGenTexture();
            gl.glBindTexture(GLenum.GL_TEXTURE_BUFFER, m_pbufPackedCellLocationsSRV);
            gl.glTexBuffer(GLenum.GL_TEXTURE_BUFFER, GLenum.GL_R32I, pbufPackedCellLocations);
            gl.glBindTexture(GLenum.GL_TEXTURE_BUFFER, 0);
        }

        // Create buffer for storing unordered list of valid cell
        {
//            D3D11_BUFFER_DESC ValidCellsBuffDesc =
//                    {
//                            m_CloudAttribs.uiNumCells * sizeof(UINT),//UINT ByteWidth;
//                            D3D11_USAGE_DEFAULT,                    //D3D11_USAGE Usage;
//                            D3D11_BIND_SHADER_RESOURCE | D3D11_BIND_UNORDERED_ACCESS,            //UINT BindFlags;
//                            0,                                      //UINT CPUAccessFlags;
//                            D3D11_RESOURCE_MISC_BUFFER_STRUCTURED,  //UINT MiscFlags;
//                            sizeof(UINT)							//UINT StructureByteStride;
//                    };
//            m_pbufValidCellsUnorderedList.Release();
//            m_pbufValidCellsUnorderedListSRV.Release();
//            m_pbufValidCellsUnorderedListUAV.Release();
//            V( CreateBufferAndViews( pDevice, ValidCellsBuffDesc, nullptr, &m_pbufValidCellsUnorderedList, &m_pbufValidCellsUnorderedListSRV, &m_pbufValidCellsUnorderedListUAV, D3D11_BUFFER_UAV_FLAG_APPEND) );
//
//            m_pbufVisibleCellsUnorderedListSRV.Release();
//            m_pbufVisibleCellsUnorderedListUAV.Release();
//            V( CreateBufferAndViews( pDevice, ValidCellsBuffDesc, nullptr, nullptr, &m_pbufVisibleCellsUnorderedListSRV, &m_pbufVisibleCellsUnorderedListUAV, D3D11_BUFFER_UAV_FLAG_APPEND) );
            m_pbufValidCellsUnorderedList = gl.glGenBuffer();
            gl.glBindBuffer(GLenum.GL_SHADER_STORAGE_BUFFER, m_pbufValidCellsUnorderedList);
            gl.glBufferData(GLenum.GL_SHADER_STORAGE_BUFFER, m_CloudAttribs.uiNumCells * 4, GLenum.GL_DYNAMIC_COPY);

            m_pbufVisibleCellsUnorderedList = gl.glGenBuffer();
            gl.glBindBuffer(GLenum.GL_SHADER_STORAGE_BUFFER, m_pbufVisibleCellsUnorderedList);
            gl.glBufferData(GLenum.GL_SHADER_STORAGE_BUFFER, m_CloudAttribs.uiNumCells * 4, GLenum.GL_DYNAMIC_COPY);
            gl.glBindBuffer(GLenum.GL_SHADER_STORAGE_BUFFER, 0);

        }

        {
//            D3D11_BUFFER_DESC VisibleParticlesBuffDesc =
//                    {
//                            m_CloudAttribs.uiMaxParticles * sizeof(SParticleIdAndDist),           //UINT ByteWidth;
//                            D3D11_USAGE_DEFAULT,                    //D3D11_USAGE Usage;
//                            D3D11_BIND_SHADER_RESOURCE | D3D11_BIND_UNORDERED_ACCESS,            //UINT BindFlags;
//                            0,                                      //UINT CPUAccessFlags;
//                            D3D11_RESOURCE_MISC_BUFFER_STRUCTURED,  //UINT MiscFlags;
//                            sizeof(SParticleIdAndDist)			    //UINT StructureByteStride;
//                    };
//            m_pbufVisibleParticlesUnorderedListSRV.Release();
//            m_pbufVisibleParticlesUnorderedListUAV.Release();
//            V(CreateBufferAndViews(pDevice, VisibleParticlesBuffDesc, nullptr, nullptr, &m_pbufVisibleParticlesUnorderedListSRV, &m_pbufVisibleParticlesUnorderedListUAV, D3D11_BUFFER_UAV_FLAG_APPEND));
            m_pbufVisibleParticlesUnorderedListUAV = m_pbufVisibleParticlesUnorderedListSRV = gl.glGenBuffer();
            gl.glBindBuffer(GLenum.GL_SHADER_STORAGE_BUFFER, m_pbufVisibleParticlesUnorderedListSRV);
            gl.glBufferData(GLenum.GL_SHADER_STORAGE_BUFFER, m_CloudAttribs.uiMaxParticles * 8, GLenum.GL_DYNAMIC_COPY);
            gl.glBindBuffer(GLenum.GL_SHADER_STORAGE_BUFFER, 0);

//            m_pbufVisibleParticlesSortedListSRV.Release();
//            m_pbufVisibleParticlesSortedListUAV.Release();
//            V(CreateBufferAndViews(pDevice, VisibleParticlesBuffDesc, nullptr, nullptr, &m_pbufVisibleParticlesSortedListSRV, &m_pbufVisibleParticlesSortedListUAV));
//
            m_pbufVisibleParticlesSortedListUAV = m_pbufVisibleParticlesSortedListSRV = gl.glGenBuffer();
            gl.glBindBuffer(GLenum.GL_SHADER_STORAGE_BUFFER, m_pbufVisibleParticlesUnorderedListSRV);
            gl.glBufferData(GLenum.GL_SHADER_STORAGE_BUFFER, m_CloudAttribs.uiMaxParticles * 8, GLenum.GL_DYNAMIC_COPY);
            gl.glBindBuffer(GLenum.GL_SHADER_STORAGE_BUFFER, 0);

//            m_pbufVisibleParticlesMergedListSRV.Release();
//            m_pbufVisibleParticlesMergedListUAV.Release();
//            V(CreateBufferAndViews(pDevice, VisibleParticlesBuffDesc, nullptr, nullptr, &m_pbufVisibleParticlesMergedListSRV, &m_pbufVisibleParticlesMergedListUAV));
            m_pbufVisibleParticlesMergedListUAV = m_pbufVisibleParticlesMergedListSRV = gl.glGenBuffer();
            gl.glBindBuffer(GLenum.GL_SHADER_STORAGE_BUFFER, m_pbufVisibleParticlesUnorderedListSRV);
            gl.glBufferData(GLenum.GL_SHADER_STORAGE_BUFFER, m_CloudAttribs.uiMaxParticles * 8, GLenum.GL_DYNAMIC_COPY);
            gl.glBindBuffer(GLenum.GL_SHADER_STORAGE_BUFFER, 0);
        }

        // Create buffer for storing streamed out list of visible particles
        {
//            D3D11_BUFFER_DESC SerializedParticlesBuffDesc =
//                    {
//                            m_CloudAttribs.uiMaxParticles * sizeof(UINT),                           //UINT ByteWidth;
//                            D3D11_USAGE_DEFAULT,                    //D3D11_USAGE Usage;
//                            D3D11_BIND_UNORDERED_ACCESS | D3D11_BIND_VERTEX_BUFFER,               //UINT BindFlags;
//                            0,                                      //UINT CPUAccessFlags;
//                            0,                                      //UINT MiscFlags;
//                            0                                       //UINT StructureByteStride;
//                    };
//
//            m_pbufSerializedVisibleParticles.Release();
//            m_pbufSerializedVisibleParticlesUAV.Release();
//
//            V(pDevice->CreateBuffer( &SerializedParticlesBuffDesc, nullptr, &m_pbufSerializedVisibleParticles));
//
//            D3D11_UNORDERED_ACCESS_VIEW_DESC UAVDesc;
//            UAVDesc.Format = DXGI_FORMAT_R32_UINT;
//            UAVDesc.ViewDimension = D3D11_UAV_DIMENSION_BUFFER;
//            UAVDesc.Buffer.FirstElement = 0;
//            UAVDesc.Buffer.NumElements = SerializedParticlesBuffDesc.ByteWidth / sizeof(UINT);
//            UAVDesc.Buffer.Flags = 0;
//            V(pDevice->CreateUnorderedAccessView( m_pbufSerializedVisibleParticles, &UAVDesc, &m_pbufSerializedVisibleParticlesUAV));
            // TODO
        }

        {
            CommonUtil.safeRelease(m_ptex3DCellDensitySRV);
            CommonUtil.safeRelease(m_ptex3DCellDensityUAV);
            CommonUtil.safeRelease(m_ptex3DLightAttenuatingMassSRV);
            CommonUtil.safeRelease(m_ptex3DLightAttenuatingMassUAV);
            Texture3DDesc Tex3DDesc = new Texture3DDesc
                    (
                            m_CloudAttribs.uiRingDimension * m_CloudAttribs.uiDensityBufferScale, //UINT Width;
                            m_CloudAttribs.uiRingDimension * m_CloudAttribs.uiDensityBufferScale, //UINT Height;
                            m_CloudAttribs.uiMaxLayers * m_CloudAttribs.uiDensityBufferScale * m_CloudAttribs.uiNumRings,  //UINT Depth;
                            1,							//UINT MipLevels;
                            GLenum.GL_R16F		//DXGI_FORMAT Format;
//                            D3D11_USAGE_DEFAULT,		//D3D11_USAGE Usage;
//                            D3D11_BIND_SHADER_RESOURCE | D3D11_BIND_UNORDERED_ACCESS, //UINT BindFlags;
//                            0,							//UINT CPUAccessFlags;
//                            0							//UINT MiscFlags;
                    );

            // Accuracy of R8_UNORM is not sufficient to provide smooth animation
            Tex3DDesc.format = GLenum.GL_R16F;
//            CComPtr<ID3D11Texture3D> ptex3DCellDensity;
//            V(pDevice->CreateTexture3D( &Tex3DDesc, nullptr, &ptex3DCellDensity));
//            V(pDevice->CreateShaderResourceView( ptex3DCellDensity, nullptr, &m_ptex3DCellDensitySRV));
//            V(pDevice->CreateUnorderedAccessView( ptex3DCellDensity, nullptr, &m_ptex3DCellDensityUAV));
            m_ptex3DCellDensityUAV = m_ptex3DCellDensitySRV = TextureUtils.createTexture3D(Tex3DDesc, null);

            Tex3DDesc.format = GLenum.GL_R8;
//            CComPtr<ID3D11Texture3D> ptex3DLightAttenuatingMass;
//            V(pDevice->CreateTexture3D( &Tex3DDesc, nullptr, &ptex3DLightAttenuatingMass));
//            V(pDevice->CreateShaderResourceView( ptex3DLightAttenuatingMass, nullptr, &m_ptex3DLightAttenuatingMassSRV));
//            V(pDevice->CreateUnorderedAccessView( ptex3DLightAttenuatingMass, nullptr, &m_ptex3DLightAttenuatingMassUAV));
            m_ptex3DLightAttenuatingMassSRV = m_ptex3DLightAttenuatingMassUAV = TextureUtils.createTexture3D(Tex3DDesc, null);
        }
    }

    private void PrecomputParticleDensity(/*ID3D11Device *pDevice, ID3D11DeviceContext *pDeviceContext*/){
        int iNumStartPosZenithAngles  = m_PrecomputedOpticalDepthTexDim.iNumStartPosZenithAngles;
        int iNumStartPosAzimuthAngles = m_PrecomputedOpticalDepthTexDim.iNumStartPosAzimuthAngles;
        int iNumDirectionZenithAngles = m_PrecomputedOpticalDepthTexDim.iNumDirectionZenithAngles;
        int iNumDirectionAzimuthAngles= m_PrecomputedOpticalDepthTexDim.iNumDirectionAzimuthAngles;

        Texture3DDesc PrecomputedOpticalDepthTexDesc = new Texture3DDesc
                (
                        iNumStartPosZenithAngles,  //UINT Width;
                        iNumStartPosAzimuthAngles,  //UINT Height;
                        iNumDirectionZenithAngles * iNumDirectionAzimuthAngles,  //UINT Depth;
                        5, //UINT MipLevels;
                        GLenum.GL_R8//DXGI_FORMAT_R8G8_UNORM,//DXGI_FORMAT Format;
//                        D3D11_USAGE_DEFAULT, //D3D11_USAGE Usage;
//                        D3D11_BIND_SHADER_RESOURCE | D3D11_BIND_RENDER_TARGET,//UINT BindFlags;
//                        0,//UINT CPUAccessFlags;
//                        D3D11_RESOURCE_MISC_GENERATE_MIPS //UINT MiscFlags;
                );

//        CComPtr<ID3D11Texture3D> ptex3DPrecomputedParticleDensity;
//        V_RETURN( pDevice->CreateTexture3D(&PrecomputedOpticalDepthTexDesc, nullptr, &ptex3DPrecomputedParticleDensity));

        CommonUtil.safeRelease(m_ptex3DPrecomputedParticleDensitySRV);
//        V_RETURN(pDevice->CreateShaderResourceView( ptex3DPrecomputedParticleDensity, nullptr, &m_ptex3DPrecomputedParticleDensitySRV));
        m_ptex3DPrecomputedParticleDensitySRV = TextureUtils.createTexture3D(PrecomputedOpticalDepthTexDesc, null);

        if(m_ComputeOpticalDepthTech == null )
        {
//            CD3DShaderMacroHelper Macros;
//            DefineMacros(Macros);
//            Macros.AddShaderMacro("DENSITY_GENERATION_METHOD", m_CloudAttribs.uiDensityGenerationMethod);
//            Macros.Finalize();
//
//            m_ComputeOpticalDepthTech.SetDeviceAndContext(pDevice, pDeviceContext);
//            m_ComputeOpticalDepthTech.CreateVGPShadersFromFile(m_strPreprocessingEffectPath, "ScreenSizeQuadVS", nullptr, "PrecomputeOpticalDepthPS", Macros);
//            m_ComputeOpticalDepthTech.SetDS( m_pdsDisableDepth );
//            m_ComputeOpticalDepthTech.SetRS( m_prsSolidFillNoCull );
//            m_ComputeOpticalDepthTech.SetBS( m_pbsDefault );

            Macro[] macros = DefineMacros();
            int length = macros.length;
            macros = Arrays.copyOf(macros, length+1);
            macros[length] = new Macro("DENSITY_GENERATION_METHOD", m_CloudAttribs.uiDensityGenerationMethod);
            m_ComputeOpticalDepthTech = new CRenderTechnique("PrecomputeOpticalDepthPS.frag", DefineMacros());
        }

//        CComPtr<ID3D11RenderTargetView> pOrigRTV;
//        CComPtr<ID3D11DepthStencilView> pOrigDSV;
//        pDeviceContext->OMGetRenderTargets(1, &pOrigRTV, &pOrigDSV);

//        D3D11_VIEWPORT OrigViewPort;
//        UINT iNumOldViewports = 1;
//        pDeviceContext->RSGetViewports(&iNumOldViewports, &OrigViewPort);

//        ID3D11Buffer *pCBs[] = {m_pcbGlobalCloudAttribs/*, RenderAttribs.pcMediaScatteringParams*/};
//        pDeviceContext->PSSetConstantBuffers(0, _countof(pCBs), pCBs); // TODO uniform buffer

//        ID3D11SamplerState *pSamplers[] = {m_psamLinearClamp, m_psamLinearWrap, m_psamPointWrap};
//        pDeviceContext->VSSetSamplers(0, _countof(pSamplers), pSamplers);
//        pDeviceContext->PSSetSamplers(0, _countof(pSamplers), pSamplers);

//        ID3D11ShaderResourceView *pSRVs[] =
//        {
//            m_ptex3DNoiseSRV,
//        };

        gl.glBindTextureUnit(0, m_ptex3DNoiseSRV.getTexture());
        gl.glBindSampler(0, m_psamLinearWrap);

        TextureAttachDesc attachDesc = m_AttachDescs[0];
        m_RenderTarget.bind();
        for(int Slice = 0; Slice < PrecomputedOpticalDepthTexDesc.depth; ++Slice)
        {
//            D3D11_RENDER_TARGET_VIEW_DESC RTVDesc;
//            RTVDesc.Format = PrecomputedOpticalDepthTexDesc.Format;
//            RTVDesc.ViewDimension = D3D11_RTV_DIMENSION_TEXTURE3D;
//            RTVDesc.Texture3D.MipSlice = 0;
//            RTVDesc.Texture3D.FirstWSlice = Slice;
//            RTVDesc.Texture3D.WSize = 1;
//
//            CComPtr<ID3D11RenderTargetView> pSliceRTV;
//            V_RETURN(pDevice->CreateRenderTargetView( ptex3DPrecomputedParticleDensity, &RTVDesc, &pSliceRTV));
            attachDesc.type = AttachType.TEXTURE_LAYER;
            attachDesc.layer = Slice;
            attachDesc.level = 0;
            attachDesc.index = 0;
            m_RenderTarget.setRenderTexture(m_ptex3DPrecomputedParticleDensitySRV, attachDesc);

            int uiDirectionZenith = Slice % iNumDirectionZenithAngles;
            int uiDirectionAzimuth= Slice / iNumDirectionZenithAngles;
            m_CloudAttribs.f4Parameter.x = ((float)uiDirectionZenith + 0.5f)  / (float)iNumDirectionZenithAngles;
            m_CloudAttribs.f4Parameter.y = ((float)uiDirectionAzimuth + 0.5f) / (float)iNumDirectionAzimuthAngles;
            assert(0 < m_CloudAttribs.f4Parameter.x && m_CloudAttribs.f4Parameter.x < 1);
            assert(0 < m_CloudAttribs.f4Parameter.y && m_CloudAttribs.f4Parameter.y < 1);
//            UpdateConstantBuffer(pDeviceContext, m_pcbGlobalCloudAttribs, &m_CloudAttribs, sizeof(m_CloudAttribs));

//            pDeviceContext->OMSetRenderTargets(1, &pSliceRTV.p, nullptr);

//            pDeviceContext->PSSetShaderResources(0, _countof(pSRVs), pSRVs);

            RenderQuad(/*pDeviceContext,*/ m_ComputeOpticalDepthTech, PrecomputedOpticalDepthTexDesc.width, PrecomputedOpticalDepthTexDesc.height);
        }
        // TODO: need to use proper filtering for coarser mip levels
//        pDeviceContext->GenerateMips( m_ptex3DPrecomputedParticleDensitySRV);
        gl.glBindTexture(m_ptex3DPrecomputedParticleDensitySRV.getTarget(), m_ptex3DPrecomputedParticleDensitySRV.getTexture());
        gl.glGenerateMipmap(m_ptex3DPrecomputedParticleDensitySRV.getTarget());
        gl.glBindTexture(m_ptex3DPrecomputedParticleDensitySRV.getTarget(), 0);

        gl.glBindTextureUnit(0, 0);
        gl.glBindSampler(0, 0);

//        pDeviceContext->OMSetRenderTargets(1, &pOrigRTV.p, pOrigDSV);
//        pDeviceContext->RSSetViewports(iNumOldViewports, &OrigViewPort);
    }

    private void PrecomputeScatteringInParticle(/*ID3D11Device *pDevice, ID3D11DeviceContext *pDeviceContext*/){
        final String SingleSctrTexPath   = "media\\SingleSctr.dds";
        final String MultipleSctrTexPath = "media\\MultipleSctr.dds";
//        HRESULT hr1 = D3DX11CreateShaderResourceViewFromFile(pDevice, SingleSctrTexPath, nullptr, nullptr, &m_ptex3DSingleSctrInParticleLUT_SRV, nullptr);
//        HRESULT hr2 = D3DX11CreateShaderResourceViewFromFile(pDevice, MultipleSctrTexPath, nullptr, nullptr, &m_ptex3DMultipleSctrInParticleLUT_SRV, nullptr);
//        if( SUCCEEDED(hr1) && SUCCEEDED(hr2) )
//            return S_OK;

        Texture3DDesc PrecomputedScatteringTexDesc = new Texture3DDesc
                (
                        m_PrecomputedSctrInParticleLUTDim.iNumStartPosZenithAngles,  //UINT Width;
                        m_PrecomputedSctrInParticleLUTDim.iNumViewDirAzimuthAngles,  //UINT Height;
                        // We are only interested in rays going into the sphere, which is half of the total number of diections
                        m_PrecomputedSctrInParticleLUTDim.iNumViewDirZenithAngles/2,  //UINT Depth;
                        1, //UINT MipLevels;
                        GLenum.GL_R16F//DXGI_FORMAT Format;
//                        D3D11_USAGE_DEFAULT, //D3D11_USAGE Usage;
//                        D3D11_BIND_SHADER_RESOURCE | D3D11_BIND_RENDER_TARGET,//UINT BindFlags;
//                        0,//UINT CPUAccessFlags;
//                        0 //UINT MiscFlags;
                );

//        CComPtr<ID3D11Texture3D> ptex3DSingleScatteringInParticleLUT, ptex3DMultipleScatteringInParticleLUT;
//        V_RETURN( pDevice->CreateTexture3D(&PrecomputedScatteringTexDesc, nullptr, &ptex3DSingleScatteringInParticleLUT));
//        V_RETURN( pDevice->CreateTexture3D(&PrecomputedScatteringTexDesc, nullptr, &ptex3DMultipleScatteringInParticleLUT));
//        m_ptex3DSingleSctrInParticleLUT_SRV.Release();
//        m_ptex3DMultipleSctrInParticleLUT_SRV.Release();
//        V_RETURN(pDevice->CreateShaderResourceView( ptex3DSingleScatteringInParticleLUT,   nullptr, &m_ptex3DSingleSctrInParticleLUT_SRV));
//        V_RETURN(pDevice->CreateShaderResourceView( ptex3DMultipleScatteringInParticleLUT, nullptr, &m_ptex3DMultipleSctrInParticleLUT_SRV));
        m_ptex3DSingleSctrInParticleLUT_SRV = TextureUtils.createTexture3D(PrecomputedScatteringTexDesc, null);
        m_ptex3DMultipleSctrInParticleLUT_SRV = TextureUtils.createTexture3D(PrecomputedScatteringTexDesc, null);

        Texture3DDesc TmpScatteringTexDesc = PrecomputedScatteringTexDesc;
        TmpScatteringTexDesc.format = GLenum.GL_R32F;
        TmpScatteringTexDesc.depth  = m_PrecomputedSctrInParticleLUTDim.iNumViewDirZenithAngles * m_PrecomputedSctrInParticleLUTDim.iNumDistancesFromCenter;


        Texture3D ptex3DSingleSctr, ptex3DGatheredScatteringN, ptex3DSctrOrderN, ptex3DMultipeScattering;
//        V_RETURN( pDevice->CreateTexture3D(&TmpScatteringTexDesc, nullptr, &ptex3DSingleSctr));
//        V_RETURN( pDevice->CreateTexture3D(&TmpScatteringTexDesc, nullptr, &ptex3DGatheredScatteringN));
//        V_RETURN( pDevice->CreateTexture3D(&TmpScatteringTexDesc, nullptr, &ptex3DSctrOrderN));
//        V_RETURN( pDevice->CreateTexture3D(&TmpScatteringTexDesc, nullptr, &ptex3DMultipeScattering));
        ptex3DSingleSctr          = TextureUtils.createTexture3D(TmpScatteringTexDesc, null);
        ptex3DGatheredScatteringN = TextureUtils.createTexture3D(TmpScatteringTexDesc, null);
        ptex3DSctrOrderN          = TextureUtils.createTexture3D(TmpScatteringTexDesc, null);
        ptex3DMultipeScattering   = TextureUtils.createTexture3D(TmpScatteringTexDesc, null);
        PrecomputedScatteringTexDesc.depth = m_PrecomputedSctrInParticleLUTDim.iNumViewDirZenithAngles/2;

//        std::vector< CComPtr<ID3D11RenderTargetView> > ptex3DSingleSctrRTVs(TmpScatteringTexDesc.Depth);
//        std::vector< CComPtr<ID3D11RenderTargetView> > ptex3DGatheredScatteringN_RTVs(TmpScatteringTexDesc.Depth);
//        std::vector< CComPtr<ID3D11RenderTargetView> > ptex3DSctrOrderN_RTVs(TmpScatteringTexDesc.Depth);
//        std::vector< CComPtr<ID3D11RenderTargetView> > ptex3DMultipeScatteringRTVs(TmpScatteringTexDesc.Depth);
//
//        CComPtr<ID3D11ShaderResourceView> ptex3DSingleSctrSRV;
//        CComPtr<ID3D11ShaderResourceView> ptex3DGatheredScatteringN_SRV;
//        CComPtr<ID3D11ShaderResourceView> ptex3DSctrOrderN_SRV;
//        CComPtr<ID3D11ShaderResourceView> ptex3DMultipeScatteringSRV;
//        V_RETURN(pDevice->CreateShaderResourceView( ptex3DSingleSctr,          nullptr, &ptex3DSingleSctrSRV));
//        V_RETURN(pDevice->CreateShaderResourceView( ptex3DGatheredScatteringN, nullptr, &ptex3DGatheredScatteringN_SRV));
//        V_RETURN(pDevice->CreateShaderResourceView( ptex3DSctrOrderN,          nullptr, &ptex3DSctrOrderN_SRV));
//        V_RETURN(pDevice->CreateShaderResourceView( ptex3DMultipeScattering,   nullptr, &ptex3DMultipeScatteringSRV));

//        for(UINT Slice = 0; Slice < TmpScatteringTexDesc.Depth; ++Slice)
//        {
//            D3D11_RENDER_TARGET_VIEW_DESC RTVDesc;
//            RTVDesc.ViewDimension = D3D11_RTV_DIMENSION_TEXTURE3D;
//            RTVDesc.Texture3D.MipSlice = 0;
//            RTVDesc.Texture3D.FirstWSlice = Slice;
//            RTVDesc.Texture3D.WSize = 1;
//            RTVDesc.Format = TmpScatteringTexDesc.Format;
//            V_RETURN(pDevice->CreateRenderTargetView( ptex3DSingleSctr,          &RTVDesc, &ptex3DSingleSctrRTVs[Slice])          );
//            V_RETURN(pDevice->CreateRenderTargetView( ptex3DGatheredScatteringN, &RTVDesc, &ptex3DGatheredScatteringN_RTVs[Slice]));
//            V_RETURN(pDevice->CreateRenderTargetView( ptex3DSctrOrderN,          &RTVDesc, &ptex3DSctrOrderN_RTVs[Slice])         );
//            V_RETURN(pDevice->CreateRenderTargetView( ptex3DMultipeScattering,   &RTVDesc, &ptex3DMultipeScatteringRTVs[Slice])   );
//        }


        if( m_ComputeSingleSctrInParticleTech == null )
        {
//            CD3DShaderMacroHelper Macros;
//            DefineMacros(Macros);
//            Macros.Finalize();
//
//            m_ComputeSingleSctrInParticleTech.SetDeviceAndContext(pDevice, pDeviceContext);
//            m_ComputeSingleSctrInParticleTech.CreateVGPShadersFromFile(m_strPreprocessingEffectPath, "ScreenSizeQuadVS", nullptr, "PrecomputeSingleSctrPS", Macros);
//            m_ComputeSingleSctrInParticleTech.SetDS( m_pdsDisableDepth );
//            m_ComputeSingleSctrInParticleTech.SetRS( m_prsSolidFillNoCull );
//            m_ComputeSingleSctrInParticleTech.SetBS( m_pbsDefault );
            m_ComputeSingleSctrInParticleTech = new CRenderTechnique("PrecomputeSingleSctrPS.frag", DefineMacros());
        }

        if( m_RenderScatteringLUTSliceTech == null )
        {
//            CD3DShaderMacroHelper Macros;
//            DefineMacros(Macros);
//            Macros.Finalize();
//
//            m_RenderScatteringLUTSliceTech.SetDeviceAndContext(pDevice, pDeviceContext);
//            m_RenderScatteringLUTSliceTech.CreateVGPShadersFromFile(m_strPreprocessingEffectPath, "ScreenSizeQuadVS", nullptr, "RenderScatteringLUTSlicePS", Macros);
//            m_RenderScatteringLUTSliceTech.SetDS( m_pdsDisableDepth );
//            m_RenderScatteringLUTSliceTech.SetRS( m_prsSolidFillNoCull );
//            m_RenderScatteringLUTSliceTech.SetBS( m_pbsDefault );
            m_RenderScatteringLUTSliceTech = new CRenderTechnique("RenderScatteringLUTSlicePS.frag", DefineMacros());
        }

        if( m_GatherPrevSctrOrderTech == null )
        {
//            CD3DShaderMacroHelper Macros;
//            DefineMacros(Macros);
//            Macros.Finalize();
//
//            m_GatherPrevSctrOrderTech.SetDeviceAndContext(pDevice, pDeviceContext);
//            m_GatherPrevSctrOrderTech.CreateVGPShadersFromFile(m_strPreprocessingEffectPath, "ScreenSizeQuadVS", nullptr, "GatherScatteringPS", Macros);
//            m_GatherPrevSctrOrderTech.SetDS( m_pdsDisableDepth );
//            m_GatherPrevSctrOrderTech.SetRS( m_prsSolidFillNoCull );
//            m_GatherPrevSctrOrderTech.SetBS( m_pbsDefault );
            m_GatherPrevSctrOrderTech = new CRenderTechnique("GatherScatteringPS.frag", DefineMacros());
        }

        if( m_ComputeScatteringOrderTech == null )
        {
//            CD3DShaderMacroHelper Macros;
//            DefineMacros(Macros);
//            Macros.Finalize();
//
//            m_ComputeScatteringOrderTech.SetDeviceAndContext(pDevice, pDeviceContext);
//            m_ComputeScatteringOrderTech.CreateVGPShadersFromFile(m_strPreprocessingEffectPath, "ScreenSizeQuadVS", nullptr, "ComputeScatteringOrderPS", Macros);
//            m_ComputeScatteringOrderTech.SetDS( m_pdsDisableDepth );
//            m_ComputeScatteringOrderTech.SetRS( m_prsSolidFillNoCull );
//            m_ComputeScatteringOrderTech.SetBS( m_pbsDefault );
            m_ComputeScatteringOrderTech = new CRenderTechnique("ComputeScatteringOrderPS.frag", DefineMacros());
        }

        if( m_AccumulateInscatteringTech == null )
        {
//            CD3DShaderMacroHelper Macros;
//            DefineMacros(Macros);
//            Macros.Finalize();
//
//            m_AccumulateInscatteringTech.SetDeviceAndContext(pDevice, pDeviceContext);
//            m_AccumulateInscatteringTech.CreateVGPShadersFromFile(m_strPreprocessingEffectPath, "ScreenSizeQuadVS", nullptr, "AccumulateMultipleScattering", Macros);
//            m_AccumulateInscatteringTech.SetDS( m_pdsDisableDepth );
//            m_AccumulateInscatteringTech.SetRS( m_prsSolidFillNoCull );
            m_AccumulateInscatteringTech = new CRenderTechnique("AccumulateMultipleScattering.frag", DefineMacros());

//            D3D11_BLEND_DESC AdditiveBlendStateDesc;
//            ZeroMemory(&AdditiveBlendStateDesc, sizeof(AdditiveBlendStateDesc));
//            AdditiveBlendStateDesc.IndependentBlendEnable = FALSE;
//            for(int i=0; i< _countof(AdditiveBlendStateDesc.RenderTarget); i++)
//                AdditiveBlendStateDesc.RenderTarget[i].RenderTargetWriteMask = D3D11_COLOR_WRITE_ENABLE_ALL;
//            AdditiveBlendStateDesc.RenderTarget[0].BlendEnable = TRUE;
//            AdditiveBlendStateDesc.RenderTarget[0].BlendOp     = D3D11_BLEND_OP_ADD;
//            AdditiveBlendStateDesc.RenderTarget[0].BlendOpAlpha= D3D11_BLEND_OP_ADD;
//            AdditiveBlendStateDesc.RenderTarget[0].DestBlend   = D3D11_BLEND_ONE;
//            AdditiveBlendStateDesc.RenderTarget[0].DestBlendAlpha= D3D11_BLEND_ONE;
//            AdditiveBlendStateDesc.RenderTarget[0].SrcBlend     = D3D11_BLEND_ONE;
//            AdditiveBlendStateDesc.RenderTarget[0].SrcBlendAlpha= D3D11_BLEND_ONE;
//            CComPtr<ID3D11BlendState> pAdditiveBlendBS;
//            V_RETURN( pDevice->CreateBlendState( &AdditiveBlendStateDesc, &pAdditiveBlendBS) );
//            m_AccumulateInscatteringTech.SetBS( pAdditiveBlendBS );
        }

//        CComPtr<ID3D11RenderTargetView> pOrigRTV;
//        CComPtr<ID3D11DepthStencilView> pOrigDSV;
//        pDeviceContext->OMGetRenderTargets(1, &pOrigRTV, &pOrigDSV);

//        D3D11_VIEWPORT OrigViewPort;
//        UINT iNumOldViewports = 1;
//        pDeviceContext->RSGetViewports(&iNumOldViewports, &OrigViewPort);

//        ID3D11Buffer *pCBs[] = {m_pcbGlobalCloudAttribs/*, RenderAttribs.pcMediaScatteringParams*/};
//        pDeviceContext->PSSetConstantBuffers(0, _countof(pCBs), pCBs);

//        ID3D11SamplerState *pSamplers[] = {m_psamLinearClamp, m_psamLinearWrap, m_psamPointWrap};
//        pDeviceContext->VSSetSamplers(0, _countof(pSamplers), pSamplers);
//        pDeviceContext->PSSetSamplers(0, _countof(pSamplers), pSamplers);

//        for(int Slice = 0; Slice < TmpScatteringTexDesc.depth; ++Slice)
//        {
//            float Zero[4]={0,0,0,0};
//            pDeviceContext->ClearRenderTargetView(ptex3DMultipeScatteringRTVs[Slice], Zero);
//        }
        gl.glClearTexImage(ptex3DMultipeScattering.getTexture(), 0, GLenum.GL_RED, GLenum.GL_FLOAT, (ByteBuffer) null);
        m_RenderTarget.bind();
//        bindTexture(CRenderTechnique.TEX3D_NOISE, m_ptex3DNoiseSRV, 0);
        TextureAttachDesc attachDesc = m_AttachDescs[0];
        // Precompute single scattering
        for(int Slice = 0; Slice < TmpScatteringTexDesc.depth; ++Slice)
        {
            int uiViewDirZenith = Slice % m_PrecomputedSctrInParticleLUTDim.iNumViewDirZenithAngles;
            int uiDistFromCenter = Slice / m_PrecomputedSctrInParticleLUTDim.iNumViewDirZenithAngles;
            m_CloudAttribs.f4Parameter.x = ((float)uiViewDirZenith + 0.5f)  / (float)m_PrecomputedSctrInParticleLUTDim.iNumViewDirZenithAngles;
            m_CloudAttribs.f4Parameter.y = ((float)uiDistFromCenter + 0.5f) / (float)m_PrecomputedSctrInParticleLUTDim.iNumDistancesFromCenter;
            assert(0 < m_CloudAttribs.f4Parameter.x && m_CloudAttribs.f4Parameter.x < 1);
            assert(0 < m_CloudAttribs.f4Parameter.y && m_CloudAttribs.f4Parameter.y < 1);
//            UpdateConstantBuffer(pDeviceContext, m_pcbGlobalCloudAttribs, &m_CloudAttribs, sizeof(m_CloudAttribs));

//            ID3D11RenderTargetView *pSliceRTV = ptex3DSingleSctrRTVs[Slice];
//            pDeviceContext->OMSetRenderTargets(1, &pSliceRTV, nullptr);
            attachDesc.type = AttachType.TEXTURE_LAYER;
            attachDesc.layer = Slice;
            attachDesc.level = 0;
            attachDesc.index = 0;
            m_RenderTarget.setRenderTexture(ptex3DSingleSctr, attachDesc);

//            ID3D11ShaderResourceView *pSRVs[] =
//            {
//                m_ptex3DNoiseSRV,
//            };
//            pDeviceContext->PSSetShaderResources(0, _countof(pSRVs), pSRVs);

            RenderQuad(/*pDeviceContext,*/ m_ComputeSingleSctrInParticleTech, TmpScatteringTexDesc.width, TmpScatteringTexDesc.height);
        }
//        bindTexture(CRenderTechnique.TEX3D_NOISE, null, 0);

        // Number of scattering orders is chosen so as to obtain reasonable exitance through the particle surface
        final int iMaxScatteringOrder = 18;
        for(int iSctrOrder = 1; iSctrOrder < iMaxScatteringOrder; ++iSctrOrder)
        {
            for(int iPass = 0; iPass < 3; ++iPass)
            {
                // Gather scattering of previous order
                for(int Slice = 0; Slice < TmpScatteringTexDesc.depth; ++Slice)
                {
                    if( iPass < 2 )
                    {
                        int uiViewDirZenith = Slice % m_PrecomputedSctrInParticleLUTDim.iNumViewDirZenithAngles;
                        int uiDistFromCenter = Slice / m_PrecomputedSctrInParticleLUTDim.iNumViewDirZenithAngles;
                        m_CloudAttribs.f4Parameter.x = ((float)uiViewDirZenith + 0.5f)  / (float)m_PrecomputedSctrInParticleLUTDim.iNumViewDirZenithAngles;
                        m_CloudAttribs.f4Parameter.y = ((float)uiDistFromCenter + 0.5f) / (float)m_PrecomputedSctrInParticleLUTDim.iNumDistancesFromCenter;
                        assert(0 < m_CloudAttribs.f4Parameter.x && m_CloudAttribs.f4Parameter.x < 1);
                        assert(0 < m_CloudAttribs.f4Parameter.y && m_CloudAttribs.f4Parameter.y < 1);
                        m_CloudAttribs.f4Parameter.w = (float)iSctrOrder;
                    }
                    else
                    {
                        m_CloudAttribs.f4Parameter.x = ((float)Slice + 0.5f) / (float)TmpScatteringTexDesc.depth;
                        assert(0 < m_CloudAttribs.f4Parameter.x && m_CloudAttribs.f4Parameter.x < 1);
                    }
//                    UpdateConstantBuffer(pDeviceContext, m_pcbGlobalCloudAttribs, &m_CloudAttribs, sizeof(m_CloudAttribs));

                    Texture3D pSliceRTV = null;
                    CRenderTechnique pTechnique = null;
                    TextureGL pSRVs = null;
                    int unit = 0;
                    switch(iPass)
                    {
                        // Gather scattering of previous order
                        case 0:
                            pSRVs = iSctrOrder > 1 ? ptex3DSctrOrderN : ptex3DSingleSctr;
                            pSliceRTV = ptex3DGatheredScatteringN/*_RTVs[Slice]*/;
                            pTechnique = m_GatherPrevSctrOrderTech;
                            break;

                        // Compute current scattering order
                        case 1:
                            pSRVs = ptex3DGatheredScatteringN;
                            pSliceRTV = ptex3DSctrOrderN/*_RTVs[Slice]*/;
                            pTechnique = m_ComputeScatteringOrderTech;
                            break;

                        // Accumulate current scattering order
                        case 2:
                            pSRVs = ptex3DSctrOrderN/*_SRV*/;
                            pSliceRTV = ptex3DMultipeScattering/*RTVs[Slice]*/;
                            pTechnique = m_AccumulateInscatteringTech;
                            break;
                    }

//                    pDeviceContext->OMSetRenderTargets(1, &pSliceRTV, nullptr);
                    attachDesc.index = 0;
                    attachDesc.level = 0;
                    attachDesc.layer = Slice;
                    attachDesc.type = AttachType.TEXTURE_LAYER;
                    m_RenderTarget.setRenderTexture(pSliceRTV, attachDesc);

//                    pDeviceContext->PSSetShaderResources(0, _countof(pSRVs), pSRVs);
                    bindTexture(unit, pSRVs, m_psamLinearWrap);

                    if(m_AccumulateInscatteringTech == pTechnique){
                        gl.glEnable(GLenum.GL_BLEND);
                        gl.glBlendFunc(GLenum.GL_ONE, GLenum.GL_ONE);
                        gl.glBlendEquation(GLenum.GL_FUNC_ADD);
                    }else{
                        gl.glDisable(GLenum.GL_BLEND);
                    }

                    RenderQuad(/*pDeviceContext,*/ pTechnique, TmpScatteringTexDesc.width, TmpScatteringTexDesc.height);
                }
            }
        }

        bindTexture(0, null, 0);
        TextureGL[] pRTVs = {m_ptex3DSingleSctrInParticleLUT_SRV, m_ptex3DMultipleSctrInParticleLUT_SRV};
        TextureAttachDesc attachDesc1 = m_AttachDescs[1];
        // Copy single and multiple scattering to the textures

        bindTexture(0, ptex3DSingleSctr, m_psamLinearWrap);
        bindTexture(1, ptex3DMultipeScattering, m_psamLinearWrap);
        for(int Slice = 0; Slice < PrecomputedScatteringTexDesc.depth; ++Slice)
        {
//            D3D11_RENDER_TARGET_VIEW_DESC RTVDesc;
//            RTVDesc.ViewDimension = D3D11_RTV_DIMENSION_TEXTURE3D;
//            RTVDesc.Texture3D.MipSlice = 0;
//            RTVDesc.Texture3D.FirstWSlice = Slice;
//            RTVDesc.Texture3D.WSize = 1;
//            RTVDesc.Format = PrecomputedScatteringTexDesc.Format;
//            CComPtr<ID3D11RenderTargetView> pSingleSctrSliceRTV, pMultSctrSliceRTV;
//            V_RETURN(pDevice->CreateRenderTargetView( ptex3DSingleScatteringInParticleLUT, &RTVDesc, &pSingleSctrSliceRTV));
//            V_RETURN(pDevice->CreateRenderTargetView( ptex3DMultipleScatteringInParticleLUT, &RTVDesc, &pMultSctrSliceRTV));

            m_CloudAttribs.f4Parameter.x = ((float)Slice + 0.5f)  / (float)PrecomputedScatteringTexDesc.depth;
//            UpdateConstantBuffer(pDeviceContext, m_pcbGlobalCloudAttribs, &m_CloudAttribs, sizeof(m_CloudAttribs));

//            ID3D11RenderTargetView *pRTVs[] = {pSingleSctrSliceRTV, pMultSctrSliceRTV};
//            pDeviceContext->OMSetRenderTargets(_countof(pRTVs), pRTVs, nullptr);

            attachDesc.layer = Slice;
            attachDesc.level = 0;
            attachDesc.index = 0;
            attachDesc.type = AttachType.TEXTURE_LAYER;

            attachDesc1.layer = Slice;
            attachDesc1.index = 1;
            attachDesc1.level = 0;
            attachDesc1.type = AttachType.TEXTURE_LAYER;
            m_RenderTarget.setRenderTextures(pRTVs, m_AttachDescs);


//            ID3D11ShaderResourceView *pSRVs[] =
//            {
//                ptex3DSingleSctrSRV,
//                        ptex3DMultipeScatteringSRV
//            };
//            pDeviceContext->PSSetShaderResources(0, _countof(pSRVs), pSRVs);
            RenderQuad(/*pDeviceContext,*/ m_RenderScatteringLUTSliceTech, PrecomputedScatteringTexDesc.width, PrecomputedScatteringTexDesc.height);
        }

        bindTexture(0, null, 0);
        bindTexture(1, null, 0);

//        D3DX11SaveTextureToFile(pDeviceContext, ptex3DSingleScatteringInParticleLUT, D3DX11_IFF_DDS, SingleSctrTexPath);
//        D3DX11SaveTextureToFile(pDeviceContext, ptex3DMultipleScatteringInParticleLUT, D3DX11_IFF_DDS, MultipleSctrTexPath);
//
//        pDeviceContext->OMSetRenderTargets(1, &pOrigRTV.p, pOrigDSV);
//        pDeviceContext->RSSetViewports(iNumOldViewports, &OrigViewPort);

//        Texture3D ptex3DSingleSctr, ptex3DGatheredScatteringN, ptex3DSctrOrderN, ptex3DMultipeScattering;
        ptex3DSingleSctr.dispose();
        ptex3DGatheredScatteringN.dispose();
        ptex3DSctrOrderN.dispose();
        ptex3DMultipeScattering.dispose();
    }

    private void ComputeExitance(/*ID3D11Device *pDevice, ID3D11DeviceContext *pDeviceContext*/){

    }

    private static final float CubicInterpolate(float ym1, float y0, float y1, float y2, float x)
    {
        float b0 = 0*ym1 + 6*y0 + 0*y1 + 0*y2;
        float b1 =-2*ym1 - 3*y0 + 6*y1 - 1*y2;
        float b2 = 3*ym1 - 6*y0 + 3*y1 + 0*y2;
        float b3 =-1*ym1 + 3*y0 - 3*y1 + 1*y2;
        float x2 = x*x;
        float x3 = x2*x;
        return 1.f/6.f * (b0 + x*b1 + x2*b2 + x3*b3);
    }

    private void Create3DNoise(/*ID3D11Device *pDevice*/){
//        HRESULT hr;
        // Create 3D noise
        int uiMips = 8;
        int uiDim = 1 << (uiMips-1);
        Texture3DDesc NoiseTexDesc = new Texture3DDesc(
                        uiDim,  //UINT Width;
                        uiDim,  //UINT Height;
                        uiDim,  //UINT Depth;
                        uiMips, //UINT MipLevels;
                        GLenum.GL_R8//DXGI_FORMAT Format;
//                        D3D11_USAGE_DEFAULT, //D3D11_USAGE Usage;
//                        D3D11_BIND_SHADER_RESOURCE,//UINT BindFlags;
//                        0,//UINT CPUAccessFlags;
//                        0//UINT MiscFlags;
        );

        int DataSize = 0;
        for(int Mip=0; Mip < uiMips; ++Mip)
            DataSize += (NoiseTexDesc.width>>Mip) * (NoiseTexDesc.height>>Mip) * (NoiseTexDesc.depth>>Mip);
//        std::vector<float> NoiseData(DataSize);
        final float[] NoiseData = new float[DataSize];

        class Noise{
            void set(int i,  int j, int k, float value) {
                NoiseData[i + j * NoiseTexDesc.width + k * (NoiseTexDesc.width * NoiseTexDesc.height)] = value;
            }
            float get(int i,  int j, int k) { return NoiseData[i + j * NoiseTexDesc.width + k * (NoiseTexDesc.width * NoiseTexDesc.height)];}
        }

//        #define NOISE(i,j,k) NoiseData[i + j * NoiseTexDesc.Width + k * (NoiseTexDesc.Width * NoiseTexDesc.Height)]
        Noise noise = new Noise();

        // Populate texture with random noise
        int InitialStep = 8;
        for(int i=0; i < NoiseTexDesc.width; i+=InitialStep)
            for(int j=0; j < NoiseTexDesc.height; j+=InitialStep)
                for(int k=0; k < NoiseTexDesc.depth; k+=InitialStep)
//                    NOISE(i,j,k) = (float)rand() / (float)RAND_MAX;
                    noise.set(i,j,k, Numeric.random());

        // Smooth rows
        for(int i=0; i < NoiseTexDesc.width; ++i)
            for(int j=0; j < NoiseTexDesc.height; j+=InitialStep)
                for(int k=0; k < NoiseTexDesc.depth; k+=InitialStep)
                {
                    int i0 = (i/InitialStep)*InitialStep;
                    int im1 = i0-InitialStep;
                    if( im1 < 0 )im1 += NoiseTexDesc.width;
                    int i1 = (i0+InitialStep) % NoiseTexDesc.width;
                    int i2 = (i0+2*InitialStep) % NoiseTexDesc.width;
//                    NOISE(i,j,k) = CubicInterpolate( NOISE(im1,j,k), NOISE(i0,j,k), NOISE(i1,j,k), NOISE(i2,j,k), (float)(i-i0) / (float)InitialStep );
                    noise.set(i,j,k, CubicInterpolate( noise.get(im1,j,k), noise.get(i0,j,k), noise.get(i1,j,k), noise.get(i2,j,k), (float)(i-i0) / (float)InitialStep ));
                }

        // Smooth columns
        for(int i=0; i < NoiseTexDesc.width; ++i)
            for(int j=0; j < NoiseTexDesc.height; ++j)
                for(int k=0; k < NoiseTexDesc.depth; k+=InitialStep)
                {
                    int j0 = (j/InitialStep)*InitialStep;
                    int jm1 = j0 - InitialStep;
                    if( jm1 < 0 )jm1+=NoiseTexDesc.height;
                    int j1 = (j0+InitialStep) % NoiseTexDesc.height;
                    int j2 = (j0+2*InitialStep) % NoiseTexDesc.height;
//                    NOISE(i,j,k) = CubicInterpolate(NOISE(i,jm1,k), NOISE(i,j0,k), NOISE(i,j1,k), NOISE(i,j2,k), (float)(j-j0) / (float)InitialStep);
                    noise.set(i,j,k, CubicInterpolate(noise.get(i,jm1,k), noise.get(i,j0,k), noise.get(i,j1,k), noise.get(i,j2,k), (float)(j-j0) / (float)InitialStep));
                }

        // Smooth in depth direction
        for(int i=0; i < NoiseTexDesc.width; ++i)
            for(int j=0; j < NoiseTexDesc.height; ++j)
                for(int k=0; k < NoiseTexDesc.depth; ++k)
                {
                    int k0 = (k/InitialStep)*InitialStep;
                    int km1 = k0-InitialStep;
                    if( km1 < 0 )km1+=NoiseTexDesc.depth;
                    int k1 = (k0+InitialStep) % NoiseTexDesc.depth;
                    int k2 = (k0+2*InitialStep) % NoiseTexDesc.depth;
//                    NOISE(i,j,k) = CubicInterpolate(NOISE(i,j,km1), NOISE(i,j,k0), NOISE(i,j,k1), NOISE(i,j,k2), (float)(k-k0) / (float)InitialStep);
                    noise.set(i,j,k, CubicInterpolate(noise.get(i,j,km1), noise.get(i,j,k0), noise.get(i,j,k1), noise.get(i,j,k2), (float)(k-k0) / (float)InitialStep));
                }

        // Generate mips
//        auto FinerMipIt = NoiseData.begin();
        int FinerMipIt = 0;
        for(int Mip = 1; Mip < uiMips; ++Mip)
        {
            int uiFinerMipWidth  = NoiseTexDesc.width  >> (Mip-1);
            int uiFinerMipHeight = NoiseTexDesc.height >> (Mip-1);
            int uiFinerMipDepth  = NoiseTexDesc.depth  >> (Mip-1);

            int CurrMipIt = FinerMipIt + uiFinerMipWidth * uiFinerMipHeight * uiFinerMipDepth;
            int uiMipWidth  = NoiseTexDesc.width  >> Mip;
            int uiMipHeight = NoiseTexDesc.height >> Mip;
            int uiMipDepth  = NoiseTexDesc.depth  >> Mip;
            for(int i=0; i < uiMipWidth; ++i)
                for(int j=0; j < uiMipHeight; ++j)
                    for(int k=0; k < uiMipDepth; ++k)
                    {
                        float fVal=0;
                        for(int x=0; x<2;++x)
                            for(int y=0; y<2;++y)
                                for(int z=0; z<2;++z)
                                {
//                                    fVal += FinerMipIt[(i*2+x) + (j*2 + y) * uiFinerMipWidth + (k*2+z) * (uiFinerMipWidth * uiFinerMipHeight)];
                                    fVal += NoiseData[FinerMipIt + (i*2+x) + (j*2 + y) * uiFinerMipWidth + (k*2+z) * (uiFinerMipWidth * uiFinerMipHeight)];
                                }
//                        CurrMipIt[i + j * uiMipWidth + k * (uiMipWidth * uiMipHeight)] = fVal / 8.f;
                        NoiseData[CurrMipIt + i + j * uiMipWidth + k * (uiMipWidth * uiMipHeight)] = fVal / 8.f;
                    }
            FinerMipIt = CurrMipIt;
        }
        assert(FinerMipIt+1 == NoiseData.length);

        // Convert to 8-bit
//        std::vector<BYTE> NoiseDataR8(NoiseData.size());
//        for(auto it=NoiseData.begin(); it != NoiseData.end(); ++it)
//            NoiseDataR8[it-NoiseData.begin()] = (BYTE)min(max((int)( *it*255.f), 0),255);
        byte[] NoiseDataR8 = new byte[NoiseData.length];
        for(int i = 0; i < NoiseData.length; i++)
            NoiseDataR8[i] = (byte)Math.min(Math.max((int)(NoiseData[i] *255.f), 0),255);

        // Prepare init data
//        std::vector<D3D11_SUBRESOURCE_DATA>InitData(uiMips);
//        auto CurrMipIt = NoiseDataR8.begin();
//        for( UINT Mip = 0; Mip < uiMips; ++Mip )
//        {
//            UINT uiMipWidth  = NoiseTexDesc.Width  >> Mip;
//            UINT uiMipHeight = NoiseTexDesc.Height >> Mip;
//            UINT uiMipDepth  = NoiseTexDesc.Depth  >> Mip;
//            InitData[Mip].pSysMem = &(*CurrMipIt);
//            InitData[Mip].SysMemPitch = uiMipWidth*sizeof(NoiseDataR8[0]);
//            InitData[Mip].SysMemSlicePitch = uiMipWidth*uiMipHeight*sizeof(NoiseDataR8[0]);
//            CurrMipIt += uiMipWidth * uiMipHeight * uiMipDepth;
//        }
//        assert(CurrMipIt == NoiseDataR8.end());
//
//        // Create 3D texture
//        CComPtr<ID3D11Texture3D> ptex3DNoise;
//        V( pDevice->CreateTexture3D(&NoiseTexDesc, &InitData[0], &ptex3DNoise));
//        V( pDevice->CreateShaderResourceView(ptex3DNoise, nullptr, &m_ptex3DNoiseSRV));

        int ptex3DNoiseSRV = gl.glGenTexture();
        gl.glBindTexture(GLenum.GL_TEXTURE_3D, ptex3DNoiseSRV);
        int CurrMipIt = 0;
        for( int Mip = 0; Mip < uiMips; ++Mip )
        {
            int uiMipWidth  = NoiseTexDesc.width  >> Mip;
            int uiMipHeight = NoiseTexDesc.height >> Mip;
            int uiMipDepth  = NoiseTexDesc.depth  >> Mip;

//            InitData[Mip].pSysMem = &(*CurrMipIt);
//            InitData[Mip].SysMemPitch = uiMipWidth*sizeof(NoiseDataR8[0]);
//            InitData[Mip].SysMemSlicePitch = uiMipWidth*uiMipHeight*sizeof(NoiseDataR8[0]);
            int size = uiMipWidth * uiMipHeight * uiMipDepth;
            gl.glTexImage3D(GLenum.GL_TEXTURE_3D, Mip, NoiseTexDesc.format, uiMipWidth, uiMipHeight, uiMipDepth, 0, GLenum.GL_RED, GLenum.GL_UNSIGNED_BYTE, CacheBuffer.wrap(NoiseDataR8, CurrMipIt, size));
            CurrMipIt += size;
        }
        GLCheck.checkError();
        gl.glTexParameteri(GLenum.GL_TEXTURE_3D, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_LINEAR_MIPMAP_LINEAR);
        gl.glTexParameteri(GLenum.GL_TEXTURE_3D, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_LINEAR);
        gl.glTexParameteri(GLenum.GL_TEXTURE_3D, GLenum.GL_TEXTURE_WRAP_R, GLenum.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GLenum.GL_TEXTURE_3D, GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GLenum.GL_TEXTURE_3D, GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_CLAMP_TO_EDGE);

        m_ptex3DNoiseSRV = TextureUtils.createTexture3D(GLenum.GL_TEXTURE_3D, ptex3DNoiseSRV);
        gl.glBindTexture(GLenum.GL_TEXTURE_3D, 0);
    }

    private void ClearCellDensityAndAttenuationTextures(SRenderAttribs RenderAttribs){
//        ID3D11DeviceContext *pDeviceContext = RenderAttribs.pDeviceContext;
//        ID3D11Device *pDevice = RenderAttribs.pDevice;
//
//        if( !m_Clear3DTexTech.IsValid() )
//        {
//            CD3DShaderMacroHelper Macros;
//            DefineMacros(Macros);
//            Macros.AddShaderMacro("THREAD_GROUP_SIZE", sm_iCSThreadGroupSize);
//            Macros.Finalize();
//
//            m_Clear3DTexTech.SetDeviceAndContext(pDevice, pDeviceContext);
//            m_Clear3DTexTech.CreateComputeShaderFromFile(m_strEffectPath, "Clear3DTextureCS", Macros);
//        }
//
//        ID3D11UnorderedAccessView * pUAVs[] = {m_ptex3DCellDensityUAV, m_ptex3DLightAttenuatingMassUAV};
//        pDeviceContext->CSSetUnorderedAccessViews(0, _countof(pUAVs), pUAVs, nullptr);
//        uint TotalVoxels =
//                m_CloudAttribs.uiRingDimension * m_CloudAttribs.uiDensityBufferScale *
//                        m_CloudAttribs.uiRingDimension * m_CloudAttribs.uiDensityBufferScale *
//                        m_CloudAttribs.uiMaxLayers * m_CloudAttribs.uiDensityBufferScale * m_CloudAttribs.uiNumRings;
//
//        m_Clear3DTexTech.Apply();
//        pDeviceContext->Dispatch( (TotalVoxels + (sm_iCSThreadGroupSize-1)) / sm_iCSThreadGroupSize, 1, 1);
//        pUAVs[0] = nullptr;
//        pUAVs[1] = nullptr;
//        pDeviceContext->CSSetUnorderedAccessViews(0, _countof(pUAVs), pUAVs, nullptr);
    }
}
