package jet.opengl.demos.amdfx.tiledrendering;

import org.lwjgl.util.vector.Matrix;
import org.lwjgl.util.vector.Matrix4f;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import jet.opengl.demos.intel.cput.ID3D11InputLayout;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.ShaderProgram;
import jet.opengl.postprocessing.texture.AttachType;
import jet.opengl.postprocessing.texture.RenderTargets;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureAttachDesc;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;

final class RSMRenderer implements Disposeable, ICONST {

    private static final int gRSMSpotResolution = 32;
    private static final int gRSMPointResolution = 32;

    private UpdateCameraCallback        m_CameraCallback;

    private final class GBufferAtlas
    {
        void Release()
        {
            SAFE_RELEASE( m_pDepthSRV );
            SAFE_RELEASE( m_pDepthDSV );
            SAFE_RELEASE( m_pDepthTexture );

            SAFE_RELEASE( m_pNormalRTV );
            SAFE_RELEASE( m_pNormalSRV );
            SAFE_RELEASE( m_pNormalTexture );

            SAFE_RELEASE( m_pDiffuseRTV );
            SAFE_RELEASE( m_pDiffuseSRV );
            SAFE_RELEASE( m_pDiffuseTexture );
        }

        private Texture2D m_pDepthTexture;
        private Texture2D m_pDepthDSV;
        private Texture2D m_pDepthSRV;

        private Texture2D m_pNormalTexture;
        private Texture2D m_pNormalRTV;
        private Texture2D m_pNormalSRV;

        private Texture2D m_pDiffuseTexture;
        private Texture2D m_pDiffuseRTV;
        private Texture2D m_pDiffuseSRV;
    };

    private GBufferAtlas m_SpotAtlas = new GBufferAtlas();
    private GBufferAtlas m_PointAtlas = new GBufferAtlas();

    private BufferGL     m_pVPLBufferCenterAndRadius;
    private BufferGL     m_pVPLBufferCenterAndRadiusSRV;
    private BufferGL     m_pVPLBufferCenterAndRadiusUAV;

    private BufferGL     m_pVPLBufferData;
    private BufferGL     m_pVPLBufferDataSRV;
    private BufferGL     m_pVPLBufferDataUAV;

    private BufferGL     m_pSpotInvViewProjBuffer;
    private BufferGL     m_pSpotInvViewProjBufferSRV;

    private BufferGL     m_pPointInvViewProjBuffer;
    private BufferGL     m_pPointInvViewProjBufferSRV;

    private BufferGL     m_pNumVPLsConstantBuffer;
    private BufferGL     m_pCPUReadbackConstantBuffer;

    private GLSLProgram m_pRSMProg;
    private ID3D11InputLayout m_pRSMLayout;

    private GLSLProgram m_pGenerateSpotVPLsCS;
    private GLSLProgram m_pGeneratePointVPLsCS;
    private RenderTargets m_FBO;
    private GLFuncProvider gl;

    private TextureAttachDesc[] m_AttachDesces = new TextureAttachDesc[3];

    void SetCallbacks( UpdateCameraCallback cameraCallback ){
        m_CameraCallback = cameraCallback;
    }

    void AddShadersToCache( /*AMD::ShaderCache *pShaderCache*/ ){
        // Ensure all shaders (and input layouts) are released
        /*SAFE_RELEASE( m_pRSMVS );
        SAFE_RELEASE( m_pRSMPS );
        SAFE_RELEASE( m_pRSMLayout );
        SAFE_RELEASE( m_pGenerateSpotVPLsCS );
        SAFE_RELEASE( m_pGeneratePointVPLsCS );

        const D3D11_INPUT_ELEMENT_DESC Layout[] =
                {
                        { "POSITION", 0, DXGI_FORMAT_R32G32B32_FLOAT, 0,  0, D3D11_INPUT_PER_VERTEX_DATA, 0 },
                        { "NORMAL",   0, DXGI_FORMAT_R32G32B32_FLOAT, 0, 12, D3D11_INPUT_PER_VERTEX_DATA, 0 },
                        { "TEXCOORD", 0, DXGI_FORMAT_R32G32_FLOAT,    0, 24, D3D11_INPUT_PER_VERTEX_DATA, 0 },
                        { "TANGENT",  0, DXGI_FORMAT_R32G32B32_FLOAT, 0, 32, D3D11_INPUT_PER_VERTEX_DATA, 0 },
                };

        pShaderCache->AddShader( (ID3D11DeviceChild**)&m_pRSMVS, AMD::ShaderCache::SHADER_TYPE_VERTEX, L"vs_5_0", L"RSMVS",
                L"RSM.hlsl", 0, NULL, &m_pRSMLayout, Layout, ARRAYSIZE( Layout ) );

        pShaderCache->AddShader( (ID3D11DeviceChild**)&m_pRSMPS, AMD::ShaderCache::SHADER_TYPE_PIXEL, L"ps_5_0", L"RSMPS",
                L"RSM.hlsl", 0, NULL, NULL, NULL, 0 );

        AMD::ShaderCache::Macro GenerateVPLMacros[ 1 ];
        wcscpy_s( GenerateVPLMacros[0].m_wsName, AMD::ShaderCache::m_uMACRO_MAX_LENGTH, L"SPOT_LIGHTS" );

        GenerateVPLMacros[ 0 ].m_iValue = 1;
        pShaderCache->AddShader( (ID3D11DeviceChild**)&m_pGenerateSpotVPLsCS, AMD::ShaderCache::SHADER_TYPE_COMPUTE, L"cs_5_0", L"GenerateVPLsCS",
                L"GenerateVPLs.hlsl", ARRAYSIZE( GenerateVPLMacros ), GenerateVPLMacros, NULL, NULL, 0 );

        GenerateVPLMacros[ 0 ].m_iValue = 0;
        pShaderCache->AddShader( (ID3D11DeviceChild**)&m_pGeneratePointVPLsCS, AMD::ShaderCache::SHADER_TYPE_COMPUTE, L"cs_5_0", L"GenerateVPLsCS",
                L"GenerateVPLs.hlsl", ARRAYSIZE( GenerateVPLMacros ), GenerateVPLMacros, NULL, NULL, 0 );*/

        throw new UnsupportedOperationException();
    }

    // Various hook functions
    void OnCreateDevice( /*ID3D11Device* pd3dDevice*/ ){
        gl = GLFuncProviderFactory.getGLFuncProvider();
        m_FBO = new RenderTargets();

        for(int i = 0; i < 3; i++){
            m_AttachDesces[i] = new TextureAttachDesc();
            m_AttachDesces[i].index = i;
            m_AttachDesces[i].type = AttachType.TEXTURE_2D;
        }

        int width = gRSMSpotResolution * MAX_NUM_SHADOWCASTING_SPOTS;
        int height = gRSMSpotResolution;
//        V( AMD::CreateDepthStencilSurface( &m_SpotAtlas.m_pDepthTexture, &m_SpotAtlas.m_pDepthSRV, &m_SpotAtlas.m_pDepthDSV, DXGI_FORMAT_D16_UNORM, DXGI_FORMAT_R16_UNORM, width, height, 1 ) );
        Texture2DDesc desc = new Texture2DDesc(width, height, GLenum.GL_DEPTH_COMPONENT16);
        m_SpotAtlas.m_pDepthTexture = TextureUtils.createTexture2D(desc, null);
        m_SpotAtlas.m_pDepthDSV = m_SpotAtlas.m_pDepthSRV = m_SpotAtlas.m_pDepthTexture;

//        V( AMD::CreateSurface( &m_SpotAtlas.m_pNormalTexture, &m_SpotAtlas.m_pNormalSRV, &m_SpotAtlas.m_pNormalRTV, 0, DXGI_FORMAT_R11G11B10_FLOAT, width, height, 1 ) );
        desc.format = GLenum.GL_R11F_G11F_B10F_EXT;
        m_SpotAtlas.m_pNormalTexture = TextureUtils.createTexture2D(desc, null);
        m_SpotAtlas.m_pNormalSRV = m_SpotAtlas.m_pNormalRTV = m_SpotAtlas.m_pNormalTexture;

//        V( AMD::CreateSurface( &m_SpotAtlas.m_pDiffuseTexture, &m_SpotAtlas.m_pDiffuseSRV, &m_SpotAtlas.m_pDiffuseRTV, 0, DXGI_FORMAT_R11G11B10_FLOAT, width, height, 1 ) );
        m_SpotAtlas.m_pDiffuseTexture = TextureUtils.createTexture2D(desc, null);
        m_SpotAtlas.m_pDiffuseSRV = m_SpotAtlas.m_pDiffuseRTV = m_SpotAtlas.m_pDiffuseTexture;

        int maxVPLs = width * height;

        width = gRSMPointResolution * 6;
        height = gRSMPointResolution * MAX_NUM_SHADOWCASTING_POINTS;
//        V( AMD::CreateDepthStencilSurface( &m_PointAtlas.m_pDepthTexture, &m_PointAtlas.m_pDepthSRV, &m_PointAtlas.m_pDepthDSV, DXGI_FORMAT_D16_UNORM, DXGI_FORMAT_R16_UNORM, width, height, 1 ) );
        desc.width = width;
        desc.height = height;
        desc.format = GLenum.GL_DEPTH_COMPONENT16;
        m_PointAtlas.m_pDepthTexture = TextureUtils.createTexture2D(desc, null);
        m_PointAtlas.m_pDepthDSV = m_PointAtlas.m_pDepthSRV = m_PointAtlas.m_pDepthTexture;
//        V( AMD::CreateSurface( &m_PointAtlas.m_pNormalTexture, &m_PointAtlas.m_pNormalSRV, &m_PointAtlas.m_pNormalRTV, 0, DXGI_FORMAT_R11G11B10_FLOAT, width, height, 1 ) );
        desc.format = GLenum.GL_R11F_G11F_B10F_EXT;
        m_PointAtlas.m_pNormalTexture = TextureUtils.createTexture2D(desc, null);
        m_PointAtlas.m_pNormalSRV = m_PointAtlas.m_pNormalRTV = m_PointAtlas.m_pNormalTexture;
//        V( AMD::CreateSurface( &m_PointAtlas.m_pDiffuseTexture, &m_PointAtlas.m_pDiffuseSRV, &m_PointAtlas.m_pDiffuseRTV, 0, DXGI_FORMAT_R11G11B10_FLOAT, width, height, 1 ) );
        m_PointAtlas.m_pDiffuseTexture = TextureUtils.createTexture2D(desc, null);
        m_PointAtlas.m_pDiffuseSRV = m_PointAtlas.m_pDiffuseRTV = m_PointAtlas.m_pDiffuseTexture;

        maxVPLs += width * height;

        /*D3D11_BUFFER_DESC desc;
        ZeroMemory( &desc, sizeof( desc ) );
        desc.Usage = D3D11_USAGE_DEFAULT;
        desc.StructureByteStride = 16;
        desc.ByteWidth = desc.StructureByteStride * maxVPLs;
        desc.BindFlags = D3D11_BIND_SHADER_RESOURCE | D3D11_BIND_UNORDERED_ACCESS;
        desc.MiscFlags = D3D11_RESOURCE_MISC_BUFFER_STRUCTURED;
        V( pd3dDevice->CreateBuffer( &desc, 0, &m_pVPLBufferCenterAndRadius ) );
        DXUT_SetDebugName( m_pVPLBufferCenterAndRadius, "VPLBufferCenterAndRadius" );*/

        m_pVPLBufferCenterAndRadius = new BufferGL();
        m_pVPLBufferCenterAndRadius.initlize(GLenum.GL_ARRAY_BUFFER, 16 * maxVPLs, null, GLenum.GL_DYNAMIC_DRAW);
        m_pVPLBufferCenterAndRadius.setName("VPLBufferCenterAndRadius");

        // see struct VPLData in CommonHeader.h
        /*desc.StructureByteStride = 48;
        desc.ByteWidth = desc.StructureByteStride * maxVPLs;
        V( pd3dDevice->CreateBuffer( &desc, 0, &m_pVPLBufferData ) );
        DXUT_SetDebugName( m_pVPLBufferData, "VPLBufferData" );*/

        m_pVPLBufferData = new BufferGL();
        m_pVPLBufferData.initlize(GLenum.GL_ARRAY_BUFFER, 48 * maxVPLs, null, GLenum.GL_DYNAMIC_DRAW);
        m_pVPLBufferData.setName("VPLBufferData");

        /*D3D11_SHADER_RESOURCE_VIEW_DESC SRVDesc;
        ZeroMemory( &SRVDesc, sizeof( SRVDesc ) );
        SRVDesc.Format = DXGI_FORMAT_UNKNOWN;
        SRVDesc.ViewDimension = D3D11_SRV_DIMENSION_BUFFER;
        SRVDesc.Buffer.ElementOffset = 0;
        SRVDesc.Buffer.ElementWidth = maxVPLs;
        V( pd3dDevice->CreateShaderResourceView( m_pVPLBufferCenterAndRadius, &SRVDesc, &m_pVPLBufferCenterAndRadiusSRV ) );
        V( pd3dDevice->CreateShaderResourceView( m_pVPLBufferData, &SRVDesc, &m_pVPLBufferDataSRV ) );*/

        m_pVPLBufferCenterAndRadiusSRV = m_pVPLBufferCenterAndRadius;
        m_pVPLBufferDataSRV = m_pVPLBufferData;

        /*D3D11_UNORDERED_ACCESS_VIEW_DESC UAVDesc;
        UAVDesc.Format = SRVDesc.Format;
        UAVDesc.ViewDimension = D3D11_UAV_DIMENSION_BUFFER;
        UAVDesc.Buffer.FirstElement = 0;
        UAVDesc.Buffer.NumElements = maxVPLs;
        UAVDesc.Buffer.Flags = D3D11_BUFFER_UAV_FLAG_COUNTER;
        V( pd3dDevice->CreateUnorderedAccessView( m_pVPLBufferCenterAndRadius, &UAVDesc, &m_pVPLBufferCenterAndRadiusUAV ) );
        V( pd3dDevice->CreateUnorderedAccessView( m_pVPLBufferData, &UAVDesc, &m_pVPLBufferDataUAV ) );*/

        m_pVPLBufferCenterAndRadiusUAV = m_pVPLBufferCenterAndRadius;
        m_pVPLBufferDataUAV = m_pVPLBufferData;

        /*desc.Usage = D3D11_USAGE_DYNAMIC;
        desc.StructureByteStride = 16*4;
        desc.ByteWidth = desc.StructureByteStride * MAX_NUM_SHADOWCASTING_SPOTS;
        desc.BindFlags = D3D11_BIND_SHADER_RESOURCE;
        desc.CPUAccessFlags = D3D11_CPU_ACCESS_WRITE;
        V( pd3dDevice->CreateBuffer( &desc, 0, &m_pSpotInvViewProjBuffer ) );
        DXUT_SetDebugName( m_pSpotInvViewProjBuffer, "SpotInvViewProjBuffer" );*/

        m_pSpotInvViewProjBuffer = new BufferGL();
        m_pSpotInvViewProjBuffer.initlize(GLenum.GL_ARRAY_BUFFER, 16*4*MAX_NUM_SHADOWCASTING_SPOTS, null, GLenum.GL_DYNAMIC_DRAW);
        m_pSpotInvViewProjBuffer.setName("SpotInvViewProjBuffer");

        /*SRVDesc.Buffer.ElementWidth = MAX_NUM_SHADOWCASTING_SPOTS;
        V( pd3dDevice->CreateShaderResourceView( m_pSpotInvViewProjBuffer, &SRVDesc, &m_pSpotInvViewProjBufferSRV ) );*/
        m_pSpotInvViewProjBufferSRV = m_pSpotInvViewProjBuffer;

        /*desc.ByteWidth = desc.StructureByteStride * MAX_NUM_SHADOWCASTING_POINTS * 6;
        V( pd3dDevice->CreateBuffer( &desc, 0, &m_pPointInvViewProjBuffer ) );
        DXUT_SetDebugName( m_pPointInvViewProjBuffer, "PointInvViewProjBuffer" );*/
        m_pPointInvViewProjBuffer = new BufferGL();
        m_pPointInvViewProjBuffer.initlize(GLenum.GL_ARRAY_BUFFER, 16*4*MAX_NUM_SHADOWCASTING_POINTS * 6,null, GLenum.GL_DYNAMIC_DRAW);
        m_pPointInvViewProjBuffer.setName("PointInvViewProjBuffer");

        /*SRVDesc.Buffer.ElementWidth = 6*MAX_NUM_SHADOWCASTING_POINTS;
        V( pd3dDevice->CreateShaderResourceView( m_pPointInvViewProjBuffer, &SRVDesc, &m_pPointInvViewProjBufferSRV ) );*/
        m_pPointInvViewProjBufferSRV = m_pPointInvViewProjBuffer;

        /*ZeroMemory( &desc, sizeof( desc ) );
        desc.Usage = D3D11_USAGE_DEFAULT;
        desc.ByteWidth = 16;
        desc.BindFlags = D3D11_BIND_CONSTANT_BUFFER;
        V( pd3dDevice->CreateBuffer( &desc, 0, &m_pNumVPLsConstantBuffer ) );
        DXUT_SetDebugName( m_pNumVPLsConstantBuffer, "NumVPLsConstantBuffer" );*/

        m_pNumVPLsConstantBuffer = new BufferGL();
        m_pNumVPLsConstantBuffer.initlize(GLenum.GL_ARRAY_BUFFER,16, null, GLenum.GL_DYNAMIC_DRAW);
        m_pNumVPLsConstantBuffer.setName("NumVPLsConstantBuffer");

        /*desc.BindFlags = 0;
        desc.Usage = D3D11_USAGE_STAGING;
        desc.CPUAccessFlags = D3D11_CPU_ACCESS_READ;
        V( pd3dDevice->CreateBuffer( &desc, 0, &m_pCPUReadbackConstantBuffer ) );
        DXUT_SetDebugName( m_pCPUReadbackConstantBuffer, "CPUReadbackConstantBuffer" );*/

    }
    void OnDestroyDevice(){
        SAFE_RELEASE( m_pGeneratePointVPLsCS );
        SAFE_RELEASE( m_pGenerateSpotVPLsCS );

//        SAFE_RELEASE( m_pRSMLayout );
        SAFE_RELEASE( m_pRSMProg );
//        SAFE_RELEASE( m_pRSMVS );

        SAFE_RELEASE( m_pCPUReadbackConstantBuffer );
        SAFE_RELEASE( m_pNumVPLsConstantBuffer );

        SAFE_RELEASE( m_pPointInvViewProjBufferSRV );
        SAFE_RELEASE( m_pPointInvViewProjBuffer );

        SAFE_RELEASE( m_pSpotInvViewProjBufferSRV );
        SAFE_RELEASE( m_pSpotInvViewProjBuffer );

        SAFE_RELEASE( m_pVPLBufferCenterAndRadiusSRV );
        SAFE_RELEASE( m_pVPLBufferCenterAndRadiusUAV );
        SAFE_RELEASE( m_pVPLBufferCenterAndRadius );
        SAFE_RELEASE( m_pVPLBufferDataSRV );
        SAFE_RELEASE( m_pVPLBufferDataUAV );
        SAFE_RELEASE( m_pVPLBufferData );

        m_SpotAtlas.Release();
        m_PointAtlas.Release();
    }
    void OnResizedSwapChain( int width, int height){}
    void OnReleasingSwapChain(){}

    void RenderSpotRSMs( int NumSpotLights, GuiState CurrentGuiState,  Scene Scene,  CommonUtil CommonUtil ){
        /*D3D11_VIEWPORT oldVp[ 8 ];
        UINT numVPs = 1;
        pd3dImmediateContext->RSGetViewports( &numVPs, oldVp );

        ID3D11RenderTargetView* pRTVs[] = { m_SpotAtlas.m_pNormalRTV, m_SpotAtlas.m_pDiffuseRTV };
        const int NumTargets = ARRAYSIZE( pRTVs );

        float clearColor[4] = { 0.0f, 0.0f, 0.0f, 0.0f };

        pd3dImmediateContext->ClearDepthStencilView( m_SpotAtlas.m_pDepthDSV, D3D11_CLEAR_DEPTH, 1.0f, 0 );
        pd3dImmediateContext->ClearRenderTargetView( m_SpotAtlas.m_pNormalRTV, clearColor );
        pd3dImmediateContext->ClearRenderTargetView( m_SpotAtlas.m_pDiffuseRTV, clearColor );

        pd3dImmediateContext->OMSetRenderTargets( NumTargets, pRTVs, m_SpotAtlas.m_pDepthDSV );*/

        final int NumTargets = 2;
        final TextureGL[] pRTVs = {m_SpotAtlas.m_pNormalRTV, m_SpotAtlas.m_pDiffuseRTV,m_SpotAtlas.m_pDepthDSV };
        m_FBO.bind();
        m_FBO.setRenderTextures(pRTVs, m_AttachDesces);
        gl.glClearBufferfv(GLenum.GL_COLOR, 0, CacheBuffer.wrap(0.f, 0.f, 0.f, 0.f));
        gl.glClearBufferfv(GLenum.GL_COLOR, 1, CacheBuffer.wrap(0.f, 0.f, 0.f, 0.f));
        gl.glClearBufferfv(GLenum.GL_DEPTH, 0, CacheBuffer.wrap(1.f));

        /*D3D11_VIEWPORT vps[ 8 ];
        for ( int i = 0; i < NumTargets; i++ )
        {
            vps[ i ].Width = gRSMSpotResolution;
            vps[ i ].Height = gRSMSpotResolution;
            vps[ i ].MinDepth = 0.0f;
            vps[ i ].MaxDepth = 1.0f;
            vps[ i ].TopLeftY = 0.0f;
            vps[ i ].TopLeftX = 0.0f;
        }*/

        /*HRESULT hr;
        D3D11_MAPPED_SUBRESOURCE MappedSubresource;
        V( pd3dImmediateContext->Map( m_pSpotInvViewProjBuffer, 0, D3D11_MAP_WRITE_DISCARD, 0, &MappedSubresource ) );
        memcpy(MappedSubresource.pData, LightUtil::GetShadowCastingSpotLightViewProjInvTransposedArray(), sizeof(XMMATRIX)*MAX_NUM_SHADOWCASTING_SPOTS);
        pd3dImmediateContext->Unmap( m_pSpotInvViewProjBuffer, 0 );*/

        Matrix4f[] array = LightUtil.GetShadowCastingSpotLightViewProjInvTransposedArray();
        FloatBuffer buffer = CacheBuffer.wrap(array);
        m_pSpotInvViewProjBuffer.update(0, buffer);

        Matrix4f[] SpotLightViewProjArray = LightUtil.GetShadowCastingSpotLightViewProjTransposedArray();

        for ( int i = 0; i < NumSpotLights; i++ )
        {
            /*for ( int j = 0; j < NumTargets; j++ )
            {
                vps[ j ].TopLeftX = (float)i * gRSMSpotResolution;
            }
            pd3dImmediateContext->RSSetViewports( NumTargets, vps );*/

            gl.glViewport(i * gRSMSpotResolution, 0, gRSMSpotResolution, gRSMSpotResolution);
            m_CameraCallback.onUpdateCamera( SpotLightViewProjArray[i] );

            RenderRSMScene(CurrentGuiState, Scene, CommonUtil );
        }

        /*pd3dImmediateContext->RSSetViewports( 1, oldVp );
        ID3D11RenderTargetView* pNullRTVs[] = { 0, 0, 0 };
        pd3dImmediateContext->OMSetRenderTargets( ARRAYSIZE( pNullRTVs ), pNullRTVs, 0 );*/
    }

    void RenderPointRSMs( int NumPointLights, GuiState CurrentGuiState, Scene Scene, CommonUtil CommonUtil ){
        /*ID3D11DeviceContext* pd3dImmediateContext = DXUTGetD3D11DeviceContext();

        D3D11_VIEWPORT oldVp[ 8 ];
        UINT numVPs = 1;
        pd3dImmediateContext->RSGetViewports( &numVPs, oldVp );

        ID3D11RenderTargetView* pRTVs[] = { m_PointAtlas.m_pNormalRTV, m_PointAtlas.m_pDiffuseRTV };
        const int NumTargets = ARRAYSIZE( pRTVs );

        float clearColor[4] = { 0.0f, 0.0f, 0.0f, 0.0f };

        pd3dImmediateContext->ClearDepthStencilView( m_PointAtlas.m_pDepthDSV, D3D11_CLEAR_DEPTH, 1.0f, 0 );
        pd3dImmediateContext->ClearRenderTargetView( m_PointAtlas.m_pNormalRTV, clearColor );
        pd3dImmediateContext->ClearRenderTargetView( m_PointAtlas.m_pDiffuseRTV, clearColor );

        pd3dImmediateContext->OMSetRenderTargets( NumTargets, pRTVs, m_PointAtlas.m_pDepthDSV );*/

        final int NumTargets = 2;
        final TextureGL[] pRTVs = {m_PointAtlas.m_pNormalRTV, m_PointAtlas.m_pDiffuseRTV,m_PointAtlas.m_pDepthDSV };
        m_FBO.bind();
        m_FBO.setRenderTextures(pRTVs, m_AttachDesces);
        gl.glClearBufferfv(GLenum.GL_COLOR, 0, CacheBuffer.wrap(0.f, 0.f, 0.f, 0.f));
        gl.glClearBufferfv(GLenum.GL_COLOR, 1, CacheBuffer.wrap(0.f, 0.f, 0.f, 0.f));
        gl.glClearBufferfv(GLenum.GL_DEPTH, 0, CacheBuffer.wrap(1.f));

        /*D3D11_VIEWPORT vps[ 8 ];
        for ( int i = 0; i < NumTargets; i++ )
        {
            vps[ i ].Width = gRSMPointResolution;
            vps[ i ].Height = gRSMPointResolution;
            vps[ i ].MinDepth = 0.0f;
            vps[ i ].MaxDepth = 1.0f;
            vps[ i ].TopLeftY = 0.0f;
            vps[ i ].TopLeftX = 0.0f;
        }*/

        /*HRESULT hr;
        D3D11_MAPPED_SUBRESOURCE MappedSubresource;
        V( pd3dImmediateContext->Map( m_pPointInvViewProjBuffer, 0, D3D11_MAP_WRITE_DISCARD, 0, &MappedSubresource ) );
        memcpy(MappedSubresource.pData, LightUtil::GetShadowCastingPointLightViewProjInvTransposedArray(), sizeof(XMMATRIX)*MAX_NUM_SHADOWCASTING_POINTS*6);
        pd3dImmediateContext->Unmap( m_pPointInvViewProjBuffer, 0 );*/
        Matrix4f[][] array = LightUtil.GetShadowCastingPointLightViewProjInvTransposedArray();
        ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(array.length * array[0].length * Matrix4f.SIZE);
        for(Matrix4f[] a : array){
            CacheBuffer.put(buffer, a);
        }
        buffer.flip();
        m_pPointInvViewProjBuffer.update(0, buffer);

        Matrix4f[][] PointLightViewProjArray = LightUtil.GetShadowCastingPointLightViewProjTransposedArray();

        for ( int i = 0; i < NumPointLights; i++ )
        {
            /*for ( int j = 0; j < NumTargets; j++ )
            {
                vps[ j ].TopLeftY = (float)i * gRSMPointResolution;
            }*/

            for ( int f = 0; f < 6; f++ )
            {
                m_CameraCallback.onUpdateCamera( PointLightViewProjArray[i][f] );

                /*for ( int j = 0; j < NumTargets; j++ )
                {
                    vps[ j ].TopLeftX = (float)f * gRSMPointResolution;
                }

                pd3dImmediateContext->RSSetViewports( NumTargets, vps );*/

                gl.glViewportIndexedf(0, f * gRSMPointResolution, i * gRSMPointResolution, gRSMPointResolution, gRSMPointResolution);
                gl.glViewportIndexedf(1, f * gRSMPointResolution, i * gRSMPointResolution, gRSMPointResolution, gRSMPointResolution);

                RenderRSMScene( CurrentGuiState, Scene, CommonUtil );
            }
        }

        /*pd3dImmediateContext->RSSetViewports( 1, oldVp );

        ID3D11RenderTargetView* pNullRTVs[] = { 0, 0, 0 };

        pd3dImmediateContext->OMSetRenderTargets( ARRAYSIZE( pNullRTVs ), pNullRTVs, 0 );*/
    }

    void GenerateVPLs( int NumSpotLights, int NumPointLights, LightUtil LightUtil ){
//        ID3D11DeviceContext* pd3dImmediateContext = DXUTGetD3D11DeviceContext();

        /*ID3D11UnorderedAccessView* pUAVs[] = { m_pVPLBufferCenterAndRadiusUAV, m_pVPLBufferDataUAV };
        // Clear the VPL counter
        UINT InitialCounts[ 2 ] = { 0, 0 };
        pd3dImmediateContext->CSSetUnorderedAccessViews( 0, ARRAYSIZE( pUAVs ), pUAVs, InitialCounts );*/
        // todo binding UnorderedAccessView

        final int numThreadsX = 16;
        final int numThreadsY = 16;

        final int sampleKernel = 2;

        if ( NumSpotLights > 0 )
        {
            /*TextureGL pSRVs[] =
            {
                m_SpotAtlas.m_pDepthSRV,
                    m_SpotAtlas.m_pNormalSRV,
                        m_SpotAtlas.m_pDiffuseSRV,
                        m_pSpotInvViewProjBufferSRV,
				LightUtil.GetSpotLightBufferCenterAndRadiusSRVParam(LightUtil.LIGHTING_SHADOWS),
				LightUtil.GetSpotLightBufferColorSRVParam(LightUtil.LIGHTING_SHADOWS),
				LightUtil.GetSpotLightBufferSpotParamsSRVParam(LightUtil.LIGHTING_SHADOWS)
            };
            pd3dImmediateContext->CSSetShaderResources( 0, ARRAYSIZE( pSRVs ), pSRVs );*/
            // todo binding resources.

            m_pGenerateSpotVPLsCS.enable();

            int dispatchCountX = (NumSpotLights * gRSMSpotResolution/sampleKernel) / numThreadsX;
            int dispatchCountY = (gRSMSpotResolution/sampleKernel) / numThreadsY;

            gl.glDispatchCompute(dispatchCountX, dispatchCountY, 1);

            /*ZeroMemory( pSRVs, sizeof( pSRVs ) );
            pd3dImmediateContext->CSSetShaderResources( 0, ARRAYSIZE( pSRVs ), pSRVs );*/
        }

        if ( NumPointLights > 0  )
        {
            throw new UnsupportedOperationException();
            /*ID3D11ShaderResourceView* pSRVs[] =
            {
                m_PointAtlas.m_pDepthSRV,
                        m_PointAtlas.m_pNormalSRV,
                        m_PointAtlas.m_pDiffuseSRV,
                        m_pPointInvViewProjBufferSRV,
				*LightUtil.GetPointLightBufferCenterAndRadiusSRVParam(LIGHTING_SHADOWS),
				*LightUtil.GetPointLightBufferColorSRVParam(LIGHTING_SHADOWS)
            };
            pd3dImmediateContext->CSSetShaderResources( 0, ARRAYSIZE( pSRVs ), pSRVs );
            pd3dImmediateContext->CSSetShader( m_pGeneratePointVPLsCS, 0, 0 );

            int dispatchCountX = (6 * gRSMPointResolution/sampleKernel) / numThreadsX;
            int dispatchCountY = (NumPointLights * gRSMPointResolution/sampleKernel) / numThreadsY;

            pd3dImmediateContext->Dispatch( dispatchCountX, dispatchCountY, 1 );

            ZeroMemory( pSRVs, sizeof( pSRVs ) );
            pd3dImmediateContext->CSSetShaderResources( 0, ARRAYSIZE( pSRVs ), pSRVs );*/
        }

        throw new UnsupportedOperationException();

        /*ID3D11UnorderedAccessView* pNullUAVs[] = { NULL, NULL };
        pd3dImmediateContext->CSSetUnorderedAccessViews( 0, ARRAYSIZE( pNullUAVs ), pNullUAVs, NULL );

        // Copy the number of items counter from the UAV into a constant buffer for reading in the forward pass
        pd3dImmediateContext->CopyStructureCount( m_pNumVPLsConstantBuffer, 0, m_pVPLBufferCenterAndRadiusUAV );

        pd3dImmediateContext->CSSetConstantBuffers( 4, 1, &m_pNumVPLsConstantBuffer );
        pd3dImmediateContext->PSSetConstantBuffers( 4, 1, &m_pNumVPLsConstantBuffer );*/
    }

    TextureGL GetSpotDepthSRV() { return m_SpotAtlas.m_pDepthSRV; }
    TextureGL GetSpotNormalSRV() { return m_SpotAtlas.m_pNormalSRV; }
    TextureGL GetSpotDiffuseSRV() { return m_SpotAtlas.m_pDiffuseSRV; }

    TextureGL GetPointDepthSRV() { return m_PointAtlas.m_pDepthSRV; }
    TextureGL GetPointNormalSRV() { return m_PointAtlas.m_pNormalSRV; }
    TextureGL GetPointDiffuseSRV() { return m_PointAtlas.m_pDiffuseSRV; }

    BufferGL GetVPLBufferCenterAndRadiusSRVParam()  { return m_pVPLBufferCenterAndRadiusSRV; }
    BufferGL GetVPLBufferDataSRVParam()  { return m_pVPLBufferDataSRV; }

    int ReadbackNumVPLs(){
        /*ID3D11DeviceContext* pd3dImmediateContext = DXUTGetD3D11DeviceContext();
        int NumVPLs = 0;

        pd3dImmediateContext->CopyStructureCount( m_pCPUReadbackConstantBuffer, 0, m_pVPLBufferCenterAndRadiusUAV );

        D3D11_MAPPED_SUBRESOURCE resource;
        if ( pd3dImmediateContext->Map( m_pCPUReadbackConstantBuffer, 0, D3D11_MAP_READ, 0, &resource ) == S_OK )
        {
            NumVPLs = *( (int*)resource.pData );

            pd3dImmediateContext->Unmap( m_pCPUReadbackConstantBuffer, 0 );
        }

        return NumVPLs;*/

        throw new UnsupportedOperationException();
    }

    private void RenderRSMScene( GuiState CurrentGuiState, Scene Scene, CommonUtil CommonUtil ){
        /*ID3D11DeviceContext* pd3dImmediateContext = DXUTGetD3D11DeviceContext();

        pd3dImmediateContext->OMSetDepthStencilState( CommonUtil.GetDepthStencilState(DEPTH_STENCIL_STATE_DEPTH_LESS), 0x00 );
        pd3dImmediateContext->IASetInputLayout( m_pRSMLayout );
        pd3dImmediateContext->VSSetShader( m_pRSMVS, NULL, 0 );
        pd3dImmediateContext->PSSetShader( m_pRSMPS, NULL, 0 );
        pd3dImmediateContext->PSSetSamplers( 0, 1, CommonUtil.GetSamplerStateParam(SAMPLER_STATE_POINT) );

        // Draw the main scene
        Scene.m_pSceneMesh->Render( pd3dImmediateContext, 0, 1 );

        // Draw the grid objects (i.e. the "lots of triangles" system)
        for( int i = 0; i < CurrentGuiState.m_nNumGridObjects; i++ )
        {
            CommonUtil.DrawGrid(i, CurrentGuiState.m_nGridObjectTriangleDensity);
        }*/

        throw new UnsupportedOperationException();
    }


    @Override
    public void dispose() {

    }
}
