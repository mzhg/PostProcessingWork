package jet.opengl.demos.amdfx.tiledrendering;

import com.nvidia.developer.opengl.app.GLEventListener;
import com.nvidia.developer.opengl.app.NvSampleApp;
import com.nvidia.developer.opengl.models.sdkmesh.SDKmesh;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.swing.plaf.ProgressBarUI;

import jet.opengl.demos.amdfx.common.CFirstPersonCamera;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.Numeric;

public class TiledLighting11 extends NvSampleApp implements ICONST{

    //-----------------------------------------------------------------------------------------
// Constants
//-----------------------------------------------------------------------------------------
    static final int TEXT_LINE_HEIGHT = 15;

    //--------------------------------------------------------------------------------------
// Global variables
//--------------------------------------------------------------------------------------
    CFirstPersonCamera g_Camera;                // A first-person camera

    // Direct3D 11 resources
    SDKmesh                     g_SceneMesh;
    SDKmesh                     g_AlphaMesh;
    Scene                       g_Scene;

    // depth buffer data
    Texture2D                   g_DepthStencilBuffer;
    Texture2D                   g_DepthStencilBufferForTransparency;

    // GUI state
    GuiState                    g_CurrentGuiState;

// Number of currently active point lights
//    static AMD::Slider*         g_NumPointLightsSlider = NULL;
    static int                  g_iNumActivePointLights = MAX_NUM_LIGHTS;

// Number of currently active spot lights
//    static AMD::Slider*         g_NumSpotLightsSlider = NULL;
    static int                  g_iNumActiveSpotLights = 0;

    // Current triangle density (i.e. the "lots of triangles" system)
    static int                  g_iTriangleDensity = CommonUtil.TRIANGLE_DENSITY_LOW;
    static final String         g_szTriangleDensityLabel[] = { "Low", "Med", "High" };

// Number of currently active grid objects (i.e. the "lots of triangles" system)
//    static AMD::Slider*         g_NumGridObjectsSlider = NULL;
    static int                  g_iNumActiveGridObjects = MAX_NUM_GRID_OBJECTS;

// Number of currently active G-Buffer render targets
//    static AMD::Slider*         g_NumGBufferRTsSlider = NULL;
    static int                  g_iNumActiveGBufferRTs = 3;

    // Shadow bias
    static int                  g_PointShadowBias = 100;
    static int                  g_SpotShadowBias = 12;

    // VPL settings
    static int                  g_VPLSpotStrength = 30;
    static int                  g_VPLPointStrength = 30;
    static int                  g_VPLSpotRadius = 200;
    static int                  g_VPLPointRadius = 100;
    static int                  g_VPLThreshold = 70;
    static int                  g_VPLBrightnessCutOff = 18;
    static int                  g_VPLBackFace = 50;

//    static AMD::Slider*         g_VPLThresholdSlider = 0;
//    static AMD::Slider*         g_VPLBrightnessCutOffSlider = 0;

    // Current lighting mode
    static int                  g_LightingMode = LightUtil.LIGHTING_SHADOWS;
    static int					g_UpdateShadowMap = 4;
    static int					g_UpdateRSMs = 4;

    // The max distance the camera can travel
    static float                g_fMaxDistance = 500.0f;

    final Matrix4f m_mWorld = new Matrix4f();
    final Matrix4f m_mViewProjection = new Matrix4f();

    BufferGL g_pcbPerObject11;
    BufferGL g_pcbPerCamera11;
    BufferGL g_pcbPerFrame11 ;
    BufferGL g_pcbShadowConstants11;

    // Global boolean for HUD rendering
    boolean  g_bRenderHUD = true;

    CommonUtil        g_CommonUtil = new CommonUtil();
    ForwardPlusUtil   g_ForwardPlusUtil = new ForwardPlusUtil();
    LightUtil         g_LightUtil = new LightUtil();
    TiledDeferredUtil g_TiledDeferredUtil = new TiledDeferredUtil();
    ShadowRenderer    g_ShadowRenderer = new ShadowRenderer();
    RSMRenderer       g_RSMRenderer = new RSMRenderer();

    private final CB_PER_FRAME pPerFrame = new CB_PER_FRAME();
    private GLFuncProvider gl;

    private boolean bFirstPass = true;

    @Override
    protected void initRendering() {
        gl = GLFuncProviderFactory.getGLFuncProvider();
        // Load the scene mesh
//        g_SceneMesh.Create( pd3dDevice, L"sponza\\sponza.sdkmesh", false );

        // Load the alpha-test mesh
//        g_AlphaMesh.Create( pd3dDevice, L"sponza\\sponza_alpha.sdkmesh", false );

        Vector3f SceneMin = new Vector3f();
        Vector3f SceneMax = new Vector3f();
        final String root = "E:\\SDK\\TiledLighting11\\tiledlighting11\\media\\sponza\\";
        try {
            g_SceneMesh = new SDKmesh();
            g_SceneMesh.create(root + "sponza.sdkmesh", false, null);
            g_SceneMesh.printMeshInformation("sponza");

            g_AlphaMesh = new SDKmesh();
            g_AlphaMesh.create(root + "sponza_alpha.sdkmesh", false, null);
            g_AlphaMesh.printMeshInformation("sponza");

            g_CommonUtil.CalculateSceneMinMax( g_SceneMesh, SceneMin, SceneMax );
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Put the mesh pointers in the wrapper struct that gets passed around
        g_Scene = new Scene();
        g_Scene.m_pSceneMesh = g_SceneMesh;
        g_Scene.m_pAlphaMesh = g_AlphaMesh;

        // And the camera
        g_Scene.m_pCamera = g_Camera;

        // Create constant buffers
        /*D3D11_BUFFER_DESC CBDesc;
        ZeroMemory( &CBDesc, sizeof(CBDesc) );
        CBDesc.Usage = D3D11_USAGE_DYNAMIC;
        CBDesc.BindFlags = D3D11_BIND_CONSTANT_BUFFER;
        CBDesc.CPUAccessFlags = D3D11_CPU_ACCESS_WRITE;

        CBDesc.ByteWidth = sizeof( CB_PER_OBJECT );
        V_RETURN( pd3dDevice->CreateBuffer( &CBDesc, NULL, &g_pcbPerObject11 ) );
        DXUT_SetDebugName( g_pcbPerObject11, "CB_PER_OBJECT" );

        CBDesc.ByteWidth = sizeof( CB_PER_CAMERA );
        V_RETURN( pd3dDevice->CreateBuffer( &CBDesc, NULL, &g_pcbPerCamera11 ) );
        DXUT_SetDebugName( g_pcbPerCamera11, "CB_PER_CAMERA" );*/

        /*CBDesc.ByteWidth = sizeof( CB_PER_FRAME );
        V_RETURN( pd3dDevice->CreateBuffer( &CBDesc, NULL, &g_pcbPerFrame11 ) );
        DXUT_SetDebugName( g_pcbPerFrame11, "CB_PER_FRAME" );*/

        g_pcbPerFrame11 = new BufferGL();
        g_pcbPerFrame11.initlize(GLenum.GL_UNIFORM_BUFFER, CB_PER_FRAME.SIZE, null, GLenum.GL_DYNAMIC_DRAW);
        g_pcbPerFrame11.setName("CB_PER_FRAME");

        /*CBDesc.ByteWidth = sizeof( CB_SHADOW_CONSTANTS );
        V_RETURN( pd3dDevice->CreateBuffer( &CBDesc, NULL, &g_pcbShadowConstants11 ) );
        DXUT_SetDebugName( g_pcbShadowConstants11, "CB_SHADOW_CONSTANTS" );*/

        g_pcbShadowConstants11 = new BufferGL();
        g_pcbShadowConstants11.initlize(GLenum.GL_UNIFORM_BUFFER, CB_SHADOW_CONSTANTS.SIZE, null, GLenum.GL_DYNAMIC_DRAW);

        // Create AMD_SDK resources here
        /*g_HUD.OnCreateDevice( pd3dDevice );
        TIMER_Init( pd3dDevice );*/

        // One-time setup
        if( bFirstPass )
        {
            // Setup the camera's view parameters
            Vector3f SceneCenter  = Vector3f.linear(SceneMax ,0.5f, SceneMin, 0.5f, null);
            Vector3f SceneExtents = Vector3f.linear(SceneMax ,0.5f, SceneMin, -0.5f, null);
            /*XMVECTOR BoundaryMin  = SceneCenter - 2.0f*SceneExtents;
            XMVECTOR BoundaryMax  = SceneCenter + 2.0f*SceneExtents;
            XMVECTOR BoundaryDiff = 4.0f*SceneExtents;  // BoundaryMax - BoundaryMin*/

            Vector3f BoundaryMin = Vector3f.linear(SceneCenter, SceneExtents, -2, null);
            Vector3f BoundaryMax = Vector3f.linear(SceneCenter, SceneExtents, +2, null);
            Vector3f BoundaryDiff = Vector3f.scale(SceneExtents, 4, null);

            g_fMaxDistance = //XMVectorGetX(XMVector3Length(BoundaryDiff));
                    BoundaryDiff.length();
            /*XMVECTOR vEye = SceneCenter - XMVectorSet(0.45f*XMVectorGetX(SceneExtents), 0.35f*XMVectorGetY(SceneExtents), 0.0f, 0.0f);
            XMVECTOR vAt  = SceneCenter - XMVectorSet(0.0f, 0.35f*XMVectorGetY(SceneExtents), 0.0f, 0.0f);
            g_Camera.SetRotateButtons( true, false, false );
            g_Camera.SetEnablePositionMovement( true );  todo initlize the camera
            g_Camera.SetViewParams( vEye, vAt );
            g_Camera.SetScalers( 0.005f, 0.1f*g_fMaxDistance );

            XMFLOAT3 vBoundaryMin, vBoundaryMax;
            XMStoreFloat3( &vBoundaryMin, BoundaryMin );
            XMStoreFloat3( &vBoundaryMax, BoundaryMax );
            g_Camera.SetClipToBoundary( true, &vBoundaryMin, &vBoundaryMax );*/

            // Init light buffer data
            LightUtil.InitLights( SceneMin, SceneMax );
        }

        // Create helper resources here
        g_CommonUtil.OnCreateDevice( /*pd3dDevice*/ );
        g_ForwardPlusUtil.OnCreateDevice( /*pd3dDevice*/ );
        g_LightUtil.OnCreateDevice( /*pd3dDevice*/ );
        g_TiledDeferredUtil.OnCreateDevice( /*pd3dDevice*/ );
        g_ShadowRenderer.OnCreateDevice( /*pd3dDevice*/ );
        g_RSMRenderer.OnCreateDevice( /*pd3dDevice*/ );

        // Generate shaders ( this is an async operation - call AMD::ShaderCache::ShadersReady() to find out if they are complete )
        if( bFirstPass )
        {
            // Add the applications shaders to the cache
            AddShadersToCache();
//            g_ShaderCache.GenerateShaders( AMD::ShaderCache::CREATE_TYPE_COMPILE_CHANGES );    // Only compile shaders that have changed (development mode)
            bFirstPass = false;
        }
    }

    @Override
    protected void reshape(int width, int height) {
        if(width <= 0 || height <= 0){
            return;
        }

        OnD3D11ReleasingSwapChain();
        // Setup the camera's projection parameters
        // Note, we are using inverted 32-bit float depth for better precision,
        // so reverse near and far below
        float fAspectRatio = (float)width / ( float )height;
        g_Camera.SetProjParams( (float)Math.toDegrees(Numeric.PI / 4), fAspectRatio, g_fMaxDistance, 0.1f );

        // Set the location and size of the AMD standard HUD
        /*g_HUD.m_GUI.SetLocation( pBackBufferSurfaceDesc->Width - AMD::HUD::iDialogWidth, 0 );
        g_HUD.m_GUI.SetSize( AMD::HUD::iDialogWidth, pBackBufferSurfaceDesc->Height );
        g_HUD.OnResizedSwapChain( pBackBufferSurfaceDesc );*/

        if(g_DepthStencilBuffer != null){
            g_DepthStencilBuffer.dispose();
            g_DepthStencilBufferForTransparency.dispose();
        }

        // Create our own depth stencil surface that's bindable as a shader resource
        /*V_RETURN( AMD::CreateDepthStencilSurface( &g_DepthStencilBuffer.m_pDepthStencilTexture, &g_DepthStencilBuffer.m_pDepthStencilSRV, &g_DepthStencilBuffer.m_pDepthStencilView,
                DXGI_FORMAT_D32_FLOAT, DXGI_FORMAT_R32_FLOAT, pBackBufferSurfaceDesc->Width, pBackBufferSurfaceDesc->Height, pBackBufferSurfaceDesc->SampleDesc.Count ) );*/
        Texture2DDesc desc = new Texture2DDesc(width, height, GLenum.GL_DEPTH_COMPONENT32F);
        g_DepthStencilBuffer = TextureUtils.createTexture2D(desc, null);

        // And another depth buffer for transparent objects
        /*V_RETURN( AMD::CreateDepthStencilSurface( &g_DepthStencilBufferForTransparency.m_pDepthStencilTexture, &g_DepthStencilBufferForTransparency.m_pDepthStencilSRV, &g_DepthStencilBufferForTransparency.m_pDepthStencilView,
                DXGI_FORMAT_D32_FLOAT, DXGI_FORMAT_R32_FLOAT, pBackBufferSurfaceDesc->Width, pBackBufferSurfaceDesc->Height, pBackBufferSurfaceDesc->SampleDesc.Count ) );*/
        desc.format = GLenum.GL_R32F;
        g_DepthStencilBufferForTransparency = TextureUtils.createTexture2D(desc, null);

        g_CommonUtil.OnResizedSwapChain( /*pd3dDevice, pBackBufferSurfaceDesc,*/width, height, TEXT_LINE_HEIGHT );
        g_ForwardPlusUtil.OnResizedSwapChain( /*pd3dDevice, pBackBufferSurfaceDesc*/width, height );
        g_LightUtil.OnResizedSwapChain( /*pd3dDevice, pBackBufferSurfaceDesc*/width, height );
        g_TiledDeferredUtil.OnResizedSwapChain( /*pd3dDevice, pBackBufferSurfaceDesc*/width, height, 1 );
        g_ShadowRenderer.OnResizedSwapChain( /*pd3dDevice, pBackBufferSurfaceDesc*/width, height );
        g_RSMRenderer.OnResizedSwapChain( /*pd3dDevice, pBackBufferSurfaceDesc*/width, height );

        g_UpdateShadowMap = 4;
        g_UpdateRSMs = 4;
    }

    @Override
    public void display() {
        // Reset the timer at start of frame
        /*TIMER_Reset();

        // If the settings dialog is being shown, then render it instead of rendering the app's scene
        if( g_SettingsDlg.IsActive() )
        {
            g_SettingsDlg.OnRender( fElapsedTime );
            return;
        }*/

        ClearD3D11DeviceContext();

//        const DXGI_SURFACE_DESC * BackBufferDesc = DXUTGetDXGIBackBufferSurfaceDesc();
        boolean bMSAAEnabled = false; //( BackBufferDesc->SampleDesc.Count > 1 );

        g_CurrentGuiState.m_uMSAASampleCount = 1; //BackBufferDesc->SampleDesc.Count;
        g_CurrentGuiState.m_uNumPointLights = g_iNumActivePointLights;
        g_CurrentGuiState.m_uNumSpotLights = g_iNumActiveSpotLights;

        // Check GUI state for lighting mode
        g_CurrentGuiState.m_nLightingMode =  LightUtil.LIGHTING_RANDOM; // g_HUD.m_GUI.GetComboBox( IDC_COMBO_LIGHTING_MODE )->GetSelectedIndex();

        // Check GUI state for debug drawing
        boolean bDebugDrawingEnabled = false;/*g_HUD.m_GUI.GetCheckBox( IDC_CHECKBOX_ENABLE_DEBUG_DRAWING )->GetEnabled() &&
                g_HUD.m_GUI.GetCheckBox( IDC_CHECKBOX_ENABLE_DEBUG_DRAWING )->GetChecked();*/
        boolean bDebugDrawMethodOne = false; /*g_HUD.m_GUI.GetRadioButton( IDC_RADIOBUTTON_DEBUG_DRAWING_ONE )->GetEnabled() &&
                g_HUD.m_GUI.GetRadioButton( IDC_RADIOBUTTON_DEBUG_DRAWING_ONE )->GetChecked();*/

        g_CurrentGuiState.m_nDebugDrawType =CommonUtil.DEBUG_DRAW_NONE;
        if( bDebugDrawingEnabled )
        {
            g_CurrentGuiState.m_nDebugDrawType = bDebugDrawMethodOne ? CommonUtil.DEBUG_DRAW_RADAR_COLORS : CommonUtil.DEBUG_DRAW_GRAYSCALE;
        }

        // Check GUI state for light drawing enabled
        g_CurrentGuiState.m_bLightDrawingEnabled = true;/*g_HUD.m_GUI.GetCheckBox( IDC_CHECKBOX_ENABLE_LIGHT_DRAWING )->GetEnabled() &&
                g_HUD.m_GUI.GetCheckBox( IDC_CHECKBOX_ENABLE_LIGHT_DRAWING )->GetChecked();*/

        // Check GUI state for transparent objects enabled
        g_CurrentGuiState.m_bTransparentObjectsEnabled = true/*g_HUD.m_GUI.GetCheckBox( IDC_CHECKBOX_ENABLE_TRANSPARENT_OBJECTS )->GetEnabled() &&
                g_HUD.m_GUI.GetCheckBox( IDC_CHECKBOX_ENABLE_TRANSPARENT_OBJECTS )->GetChecked()*/;

        // Check GUI state for shadows enabled
        g_CurrentGuiState.m_bShadowsEnabled = true/*g_HUD.m_GUI.GetCheckBox( IDC_CHECKBOX_ENABLE_SHADOWS )->GetEnabled() &&
                g_HUD.m_GUI.GetCheckBox( IDC_CHECKBOX_ENABLE_SHADOWS )->GetChecked()*/;

        // Check GUI state for virtual point lights (VPLs) enabled
        g_CurrentGuiState.m_bVPLsEnabled = true/*g_HUD.m_GUI.GetCheckBox( IDC_CHECKBOX_ENABLE_VPLS )->GetEnabled() &&
                g_HUD.m_GUI.GetCheckBox( IDC_CHECKBOX_ENABLE_VPLS )->GetChecked()*/;

        g_CurrentGuiState.m_nGridObjectTriangleDensity = g_iTriangleDensity;
        g_CurrentGuiState.m_nNumGridObjects = g_iNumActiveGridObjects;
        g_CurrentGuiState.m_nNumGBufferRenderTargets = g_iNumActiveGBufferRTs;

        /*XMMATRIX mWorld = XMMatrixIdentity();  todo camera update

        // Get the projection & view matrix from the camera class
        XMMATRIX mView = g_Camera.GetViewMatrix();
        XMMATRIX mProj = g_Camera.GetProjMatrix();
        XMMATRIX mViewProjection = mView * mProj;

        // we need the inverse proj matrix in the per-tile light culling
        // compute shader
        XMFLOAT4X4 f4x4Proj, f4x4InvProj;
        XMStoreFloat4x4( &f4x4Proj, mProj );
        XMStoreFloat4x4( &f4x4InvProj, XMMatrixIdentity() );
        f4x4InvProj._11 = 1.0f / f4x4Proj._11;
        f4x4InvProj._22 = 1.0f / f4x4Proj._22;
        f4x4InvProj._33 = 0.0f;
        f4x4InvProj._34 = 1.0f / f4x4Proj._43;
        f4x4InvProj._43 = 1.0f;
        f4x4InvProj._44 = -f4x4Proj._33 / f4x4Proj._43;
        XMMATRIX mInvProj = XMLoadFloat4x4( &f4x4InvProj );

        // we need the inverse viewproj matrix with viewport mapping,
        // for converting from depth back to world-space position
        XMMATRIX mInvViewProj = XMMatrixInverse( NULL, mViewProjection );
        XMFLOAT4X4 f4x4Viewport ( 2.0f / (float)BackBufferDesc->Width, 0.0f,                                 0.0f, 0.0f,
                0.0f,                               -2.0f / (float)BackBufferDesc->Height, 0.0f, 0.0f,
                0.0f,                                0.0f,                                 1.0f, 0.0f,
                -1.0f,                                1.0f,                                 0.0f, 1.0f  );
        XMMATRIX mInvViewProjViewport = XMLoadFloat4x4(&f4x4Viewport) * mInvViewProj;*/

        Matrix4f mView = g_Camera.GetViewMatrix();
        Matrix4f mViewProjection = null;

        Vector4f CameraPosAndAlphaTest = new Vector4f();
//        XMStoreFloat4( &CameraPosAndAlphaTest, g_Camera.GetEyePt() );
        CameraPosAndAlphaTest.set(g_Camera.GetEyePt());
        // different alpha test for MSAA enabled vs. disabled
        CameraPosAndAlphaTest.w = bMSAAEnabled ? 0.003f : 0.5f;

        // Set the constant buffers
        /*HRESULT hr;
        D3D11_MAPPED_SUBRESOURCE MappedResource;*/

        // per-camera constants
        /*V( pd3dImmediateContext->Map( g_pcbPerCamera11, 0, D3D11_MAP_WRITE_DISCARD, 0, &MappedResource ) );
        CB_PER_CAMERA* pPerCamera = ( CB_PER_CAMERA* )MappedResource.pData;
        pPerCamera->m_mViewProjection = XMMatrixTranspose( mViewProjection );  todo
        pd3dImmediateContext->Unmap( g_pcbPerCamera11, 0 );
        pd3dImmediateContext->VSSetConstantBuffers( 1, 1, &g_pcbPerCamera11 );
        pd3dImmediateContext->PSSetConstantBuffers( 1, 1, &g_pcbPerCamera11 );
        pd3dImmediateContext->CSSetConstantBuffers( 1, 1, &g_pcbPerCamera11 );

        // per-frame constants
        V( pd3dImmediateContext->Map( g_pcbPerFrame11, 0, D3D11_MAP_WRITE_DISCARD, 0, &MappedResource ) );
        CB_PER_FRAME* pPerFrame = ( CB_PER_FRAME* )MappedResource.pData;*/
//        pPerFrame.m_mView = XMMatrixTranspose( mView );    todo
//        pPerFrame.m_mProjection = XMMatrixTranspose( mProj );todo
//        pPerFrame.m_mProjectionInv = XMMatrixTranspose( mInvProj );todo
//        pPerFrame.m_mViewProjectionInvViewport = XMMatrixTranspose( mInvViewProjViewport );todo
        pPerFrame.m_AmbientColorUp.set( 0.013f, 0.015f, 0.050f, 1.0f );
        pPerFrame.m_AmbientColorDown.set( 0.0013f, 0.0015f, 0.0050f, 1.0f );
        pPerFrame.m_vCameraPosAndAlphaTest.set( CameraPosAndAlphaTest );
        pPerFrame.m_uNumLights = g_iNumActivePointLights;
        pPerFrame.m_uNumSpotLights = g_iNumActiveSpotLights;
        pPerFrame.m_uWindowWidth = getGLContext().width();
        pPerFrame.m_uWindowHeight = getGLContext().height();
        pPerFrame.m_uMaxNumLightsPerTile = g_CommonUtil.GetMaxNumLightsPerTile();
        pPerFrame.m_uMaxNumElementsPerTile = g_CommonUtil.GetMaxNumElementsPerTile();
        pPerFrame.m_uNumTilesX = g_CommonUtil.GetNumTilesX();
        pPerFrame.m_uNumTilesY = g_CommonUtil.GetNumTilesY();
        pPerFrame.m_uMaxVPLs = ( g_CurrentGuiState.m_nLightingMode == LightUtil.LIGHTING_SHADOWS && g_CurrentGuiState.m_bVPLsEnabled ) ? Numeric.MAX_USHORT : 0;
        pPerFrame.m_uMaxNumVPLsPerTile = g_CommonUtil.GetMaxNumVPLsPerTile();
        pPerFrame.m_uMaxNumVPLElementsPerTile = g_CommonUtil.GetMaxNumVPLElementsPerTile();
        pPerFrame.m_fVPLSpotStrength = 0.2f * ( (float)g_VPLSpotStrength / 100.0f );
        pPerFrame.m_fVPLSpotRadius = (float)g_VPLSpotRadius;
        pPerFrame.m_fVPLPointStrength = 0.2f * ( (float)g_VPLPointStrength / 100.0f );
        pPerFrame.m_fVPLPointRadius = (float)g_VPLPointRadius;
        pPerFrame.m_fVPLRemoveBackFaceContrib = 1.0f * ( (float)g_VPLBackFace / 100.0f );
        pPerFrame.m_fVPLColorThreshold = 1.0f * ( (float)g_VPLThreshold / 100.0f );
        pPerFrame.m_fVPLBrightnessThreshold = 0.01f * ( (float)g_VPLBrightnessCutOff / 100.0f );
        pPerFrame.m_fPerFramePad1 = 0.0f;
        pPerFrame.m_fPerFramePad2 = 0.0f;
        /*pd3dImmediateContext->Unmap( g_pcbPerFrame11, 0 );
        pd3dImmediateContext->VSSetConstantBuffers( 2, 1, &g_pcbPerFrame11 );
        pd3dImmediateContext->PSSetConstantBuffers( 2, 1, &g_pcbPerFrame11 );
        pd3dImmediateContext->CSSetConstantBuffers( 2, 1, &g_pcbPerFrame11 );*/

        ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(CB_PER_FRAME.SIZE);
        pPerFrame.store(buffer).flip();
        g_pcbPerFrame11.update(0, buffer);

        // per-object constants
        /*V( pd3dImmediateContext->Map( g_pcbPerObject11, 0, D3D11_MAP_WRITE_DISCARD, 0, &MappedResource ) );
        CB_PER_OBJECT* pPerObject = ( CB_PER_OBJECT* )MappedResource.pData;
        pPerObject->m_mWorld = XMMatrixTranspose( mWorld );
        pd3dImmediateContext->Unmap( g_pcbPerObject11, 0 );
        pd3dImmediateContext->VSSetConstantBuffers( 0, 1, &g_pcbPerObject11 );
        pd3dImmediateContext->PSSetConstantBuffers( 0, 1, &g_pcbPerObject11 );
        pd3dImmediateContext->CSSetConstantBuffers( 0, 1, &g_pcbPerObject11 );*/

        // shadow constants
        UpdateShadowConstants();

        boolean bForwardPlus = true; /* g_HUD.m_GUI.GetRadioButton( IDC_RADIOBUTTON_FORWARD_PLUS )->GetEnabled() &&
                g_HUD.m_GUI.GetRadioButton( IDC_RADIOBUTTON_FORWARD_PLUS )->GetChecked()*/;
        float fElapsedTime = getFrameDeltaTime();
        // Render objects here...
//        if( g_ShaderCache.ShadersReady() )
        {
            if ( g_CurrentGuiState.m_nLightingMode == LightUtil.LIGHTING_SHADOWS && g_UpdateShadowMap > 0 )
            {
                g_ShadowRenderer.RenderPointMap( MAX_NUM_SHADOWCASTING_POINTS );
                g_ShadowRenderer.RenderSpotMap( MAX_NUM_SHADOWCASTING_SPOTS );

                // restore main camera viewProj
                UpdateCameraConstantBufferWithTranspose( mViewProjection );

                g_UpdateShadowMap--;
            }

            if ( g_CurrentGuiState.m_nLightingMode == LightUtil.LIGHTING_SHADOWS && g_CurrentGuiState.m_bVPLsEnabled )
            {
                if ( g_UpdateRSMs > 0 )
                {
                    g_RSMRenderer.RenderSpotRSMs( g_iNumActiveSpotLights, g_CurrentGuiState, g_Scene, g_CommonUtil );
                    g_RSMRenderer.RenderPointRSMs( g_iNumActivePointLights, g_CurrentGuiState, g_Scene, g_CommonUtil );

                    // restore main camera viewProj
                    UpdateCameraConstantBufferWithTranspose( mViewProjection );

                    g_UpdateRSMs--;
                }

                g_RSMRenderer.GenerateVPLs( g_iNumActiveSpotLights, g_iNumActivePointLights, g_LightUtil );
            }

//            TIMER_Begin( 0, L"Render" );

            if( bForwardPlus )
            {
                g_ForwardPlusUtil.OnRender( fElapsedTime, g_CurrentGuiState, g_DepthStencilBuffer, g_DepthStencilBufferForTransparency, g_Scene, g_CommonUtil, g_LightUtil, g_ShadowRenderer, g_RSMRenderer );
            }
            else
            {
                g_TiledDeferredUtil.OnRender( fElapsedTime, g_CurrentGuiState, g_DepthStencilBuffer, g_DepthStencilBufferForTransparency, g_Scene, g_CommonUtil, g_LightUtil, g_ShadowRenderer, g_RSMRenderer );
            }

//            TIMER_End(); // Render
        }

//        DXUT_BeginPerfEvent( DXUT_PERFEVENTCOLOR, L"HUD / Stats" );

//        AMD::ProcessUIChanges();

       /* if ( g_ShaderCache.ShadersReady() )
        {

            // Render the HUD
            if( g_bRenderHUD )
            {
                g_HUD.OnRender( fElapsedTime );
            }

            RenderText();

            AMD::RenderHUDUpdates( g_pTxtHelper );
        }
        else*/
        {
            /*float ClearColor[4] = { 0.0013f, 0.0015f, 0.0050f, 0.0f };
            ID3D11RenderTargetView* pRTV = DXUTGetD3D11RenderTargetView();
            ID3D11DepthStencilView* pNULLDSV = NULL;
            pd3dImmediateContext->ClearRenderTargetView( pRTV, ClearColor );

            // Render shader cache progress if still processing
            pd3dImmediateContext->OMSetRenderTargets( 1, &pRTV, pNULLDSV );
            pd3dImmediateContext->OMSetDepthStencilState( g_CommonUtil.GetDepthStencilState(DEPTH_STENCIL_STATE_DISABLE_DEPTH_TEST), 0x00 );
            g_ShaderCache.RenderProgress( g_pTxtHelper, TEXT_LINE_HEIGHT, XMVectorSet( 1.0f, 1.0f, 0.0f, 1.0f ) );*/
        }

//        DXUT_EndPerfEvent();
    }

    //--------------------------------------------------------------------------------------
// Adds all shaders to the shader cache
//--------------------------------------------------------------------------------------
    void AddShadersToCache()
    {
        g_CommonUtil.AddShadersToCache();
        g_ForwardPlusUtil.AddShadersToCache();
        g_LightUtil.AddShadersToCache();
        g_TiledDeferredUtil.AddShadersToCache();
        g_RSMRenderer.AddShadersToCache();

    }

    //--------------------------------------------------------------------------------------
// Stripped down version of DXUT ClearD3D11DeviceContext.
// For this sample, the HS, DS, and GS are not used. And it
// is assumed that drawing code will always call VSSetShader,
// PSSetShader, IASetVertexBuffers, IASetIndexBuffer (if applicable),
// and IASetInputLayout.
//--------------------------------------------------------------------------------------
    void ClearD3D11DeviceContext()
    {
        /*ID3D11DeviceContext* pd3dDeviceContext = DXUTGetD3D11DeviceContext();

        ID3D11ShaderResourceView* pSRVs[16] = { 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 };
        ID3D11RenderTargetView* pRTVs[16] = { 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 };
        ID3D11DepthStencilView* pDSV = NULL;
        ID3D11Buffer* pBuffers[16] = { 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 };
        ID3D11SamplerState* pSamplers[16] = { 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 };


        // Constant buffers
        pd3dDeviceContext->VSSetConstantBuffers( 0, 14, pBuffers );
        pd3dDeviceContext->PSSetConstantBuffers( 0, 14, pBuffers );
        pd3dDeviceContext->CSSetConstantBuffers( 0, 14, pBuffers );

        // Resources
        pd3dDeviceContext->VSSetShaderResources( 0, 16, pSRVs );
        pd3dDeviceContext->PSSetShaderResources( 0, 16, pSRVs );
        pd3dDeviceContext->CSSetShaderResources( 0, 16, pSRVs );

        // Samplers
        pd3dDeviceContext->VSSetSamplers( 0, 16, pSamplers );
        pd3dDeviceContext->PSSetSamplers( 0, 16, pSamplers );
        pd3dDeviceContext->CSSetSamplers( 0, 16, pSamplers );

        // Render targets
        pd3dDeviceContext->OMSetRenderTargets( 8, pRTVs, pDSV );

        // States
        FLOAT BlendFactor[4] = { 0,0,0,0 };
        pd3dDeviceContext->OMSetBlendState( NULL, BlendFactor, 0xFFFFFFFF );
        pd3dDeviceContext->OMSetDepthStencilState( g_CommonUtil.GetDepthStencilState(DEPTH_STENCIL_STATE_DEPTH_GREATER), 0x00 );  // we are using inverted 32-bit float depth for better precision
        pd3dDeviceContext->RSSetState( NULL );*/
    }

    private final CB_SHADOW_CONSTANTS pShadowConstants = new CB_SHADOW_CONSTANTS();

    void UpdateShadowConstants()
    {
        final int POINT_SHADOW_SIZE = 256;

        /*HRESULT hr;
        D3D11_MAPPED_SUBRESOURCE MappedResource;

        ID3D11DeviceContext* pd3dImmediateContext = DXUTGetD3D11DeviceContext();

        V( pd3dImmediateContext->Map( g_pcbShadowConstants11, 0, D3D11_MAP_WRITE_DISCARD, 0, &MappedResource ) );

        CB_SHADOW_CONSTANTS* pShadowConstants = ( CB_SHADOW_CONSTANTS* )MappedResource.pData;*/

//        memcpy(pShadowConstants->m_mPointShadowViewProj, LightUtil::GetShadowCastingPointLightViewProjTransposedArray(), sizeof(pShadowConstants->m_mPointShadowViewProj));
//        memcpy(pShadowConstants->m_mSpotShadowViewProj, LightUtil::GetShadowCastingSpotLightViewProjTransposedArray(), sizeof(pShadowConstants->m_mSpotShadowViewProj));

        ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(CB_SHADOW_CONSTANTS.SIZE);
        Matrix4f[][] pointShadowViewProj = LightUtil.GetShadowCastingPointLightViewProjTransposedArray();
        for(int i = 0; i < pointShadowViewProj.length; i++)
            CacheBuffer.put(buffer, pointShadowViewProj[i]);

        Matrix4f[] spotShadowViewProj = LightUtil.GetShadowCastingSpotLightViewProjTransposedArray();
        CacheBuffer.put(buffer, spotShadowViewProj);
        float pointShadowMapSize = (float)POINT_SHADOW_SIZE;
        float pointBlurSize = 2.5f;
        /*XMFLOAT4 ShadowBias;
        ShadowBias.x = (pointShadowMapSize - 2.0f * pointBlurSize) / pointShadowMapSize;
        ShadowBias.y = pointBlurSize / pointShadowMapSize;
        ShadowBias.z = (float)g_PointShadowBias * 0.00001f;
        ShadowBias.w = (float)g_SpotShadowBias * 0.00001f;
        pShadowConstants->m_ShadowBias = XMLoadFloat4(&ShadowBias);*/

        buffer.putFloat((pointShadowMapSize - 2.0f * pointBlurSize) / pointShadowMapSize);
        buffer.putFloat(pointBlurSize / pointShadowMapSize);
        buffer.putFloat(g_PointShadowBias * 0.00001f);
        buffer.putFloat(g_SpotShadowBias * 0.00001f);
        buffer.flip();

        g_pcbShadowConstants11.update(0, buffer);

        /*pd3dImmediateContext->Unmap( g_pcbShadowConstants11, 0 );
        pd3dImmediateContext->VSSetConstantBuffers( 3, 1, &g_pcbShadowConstants11 );
        pd3dImmediateContext->PSSetConstantBuffers( 3, 1, &g_pcbShadowConstants11 );
        pd3dImmediateContext->CSSetConstantBuffers( 3, 1, &g_pcbShadowConstants11 );*/
    }


    void UpdateCameraConstantBuffer( Matrix4f mViewProjAlreadyTransposed )
    {
        /*HRESULT hr;
        D3D11_MAPPED_SUBRESOURCE MappedResource;
        ID3D11DeviceContext* pd3dImmediateContext = DXUTGetD3D11DeviceContext();

        V( pd3dImmediateContext->Map( g_pcbPerCamera11, 0, D3D11_MAP_WRITE_DISCARD, 0, &MappedResource ) );
        CB_PER_CAMERA* pPerCamera = ( CB_PER_CAMERA* )MappedResource.pData;
        memcpy( &pPerCamera->m_mViewProjection, &mViewProjAlreadyTransposed, sizeof(XMMATRIX) );
        pd3dImmediateContext->Unmap( g_pcbPerCamera11, 0 );*/
    }


    void UpdateCameraConstantBufferWithTranspose( Matrix4f mViewProj )
    {
//        XMMATRIX mViewProjTransposed = XMMatrixTranspose( mViewProj );
        UpdateCameraConstantBuffer( mViewProj );
    }


    void RenderDepthOnlyScene()
    {
        g_ForwardPlusUtil.RenderSceneForShadowMaps( g_CurrentGuiState, g_Scene, g_CommonUtil );
    }

    void OnD3D11ReleasingSwapChain(  )
    {
        g_CommonUtil.OnReleasingSwapChain();
        g_ForwardPlusUtil.OnReleasingSwapChain();
        g_LightUtil.OnReleasingSwapChain();
        g_TiledDeferredUtil.OnReleasingSwapChain();
        g_ShadowRenderer.OnReleasingSwapChain();
        g_RSMRenderer.OnReleasingSwapChain();

//        g_DialogResourceManager.OnD3D11ReleasingSwapChain();

        /*SAFE_RELEASE( g_DepthStencilBuffer.m_pDepthStencilTexture );
        SAFE_RELEASE( g_DepthStencilBuffer.m_pDepthStencilView );
        SAFE_RELEASE( g_DepthStencilBuffer.m_pDepthStencilSRV );

        SAFE_RELEASE( g_DepthStencilBufferForTransparency.m_pDepthStencilTexture );
        SAFE_RELEASE( g_DepthStencilBufferForTransparency.m_pDepthStencilView );
        SAFE_RELEASE( g_DepthStencilBufferForTransparency.m_pDepthStencilSRV );*/
    }

    @Override
    public void onDestroy() {
        /*g_DialogResourceManager.OnD3D11DestroyDevice();
        g_SettingsDlg.OnD3D11DestroyDevice();
        DXUTGetGlobalResourceCache().OnDestroyDevice();
        SAFE_DELETE( g_pTxtHelper );*/


        if(g_DepthStencilBufferForTransparency != null)
            g_DepthStencilBufferForTransparency.dispose();


        if(g_DepthStencilBuffer != null)
            g_DepthStencilBuffer.dispose();

        // Delete additional render resources here...
        g_SceneMesh.dispose();
        g_AlphaMesh.dispose();

        if(g_pcbPerObject11 != null){
            g_pcbPerObject11.dispose();
            g_pcbPerObject11 = null;
        }
        if(g_pcbPerCamera11 != null){
            g_pcbPerCamera11.dispose();
            g_pcbPerCamera11 = null;
        }
        if(g_pcbPerFrame11 != null){
            g_pcbPerFrame11.dispose();
            g_pcbPerFrame11 = null;
        }
        if(g_pcbShadowConstants11 != null){
            g_pcbShadowConstants11.dispose();
            g_pcbShadowConstants11 = null;
        }

        g_CommonUtil.OnDestroyDevice();
        g_ForwardPlusUtil.OnDestroyDevice();
        g_LightUtil.OnDestroyDevice();
        g_TiledDeferredUtil.OnDestroyDevice();
        g_ShadowRenderer.OnDestroyDevice();
        g_RSMRenderer.OnDestroyDevice();

        // Destroy AMD_SDK resources here
        /*g_ShaderCache.OnDestroyDevice();
        g_HUD.OnDestroyDevice();
        TIMER_Destroy();*/
    }

    private static final class CB_PER_FRAME implements Readable
    {
        static final int SIZE = Matrix4f.SIZE * 4 + Vector4f.SIZE * 3 + 20*4;
        final Matrix4f m_mView = new Matrix4f();
        final Matrix4f m_mProjection = new Matrix4f();
        final Matrix4f m_mProjectionInv = new Matrix4f();
        final Matrix4f m_mViewProjectionInvViewport = new Matrix4f();
        final Vector4f m_AmbientColorUp = new Vector4f();
        final Vector4f m_AmbientColorDown = new Vector4f();
        final Vector4f m_vCameraPosAndAlphaTest = new Vector4f();
        int m_uNumLights;
        int m_uNumSpotLights;
        int m_uWindowWidth;
        int m_uWindowHeight;
        int m_uMaxNumLightsPerTile;
        int m_uMaxNumElementsPerTile;
        int m_uNumTilesX;
        int m_uNumTilesY;
        int m_uMaxVPLs;
        int m_uMaxNumVPLsPerTile;
        int m_uMaxNumVPLElementsPerTile;
        float    m_fVPLSpotStrength;
        float    m_fVPLSpotRadius;
        float    m_fVPLPointStrength;
        float    m_fVPLPointRadius;
        float    m_fVPLRemoveBackFaceContrib;
        float    m_fVPLColorThreshold;
        float    m_fVPLBrightnessThreshold;
        float    m_fPerFramePad1;
        float    m_fPerFramePad2;
        @Override
        public ByteBuffer store(ByteBuffer buf) {
            m_mView.store(buf);
            m_mProjection.store(buf);
            m_mProjectionInv.store(buf);
            m_mViewProjectionInvViewport.store(buf);
            m_AmbientColorUp.store(buf);
            m_AmbientColorDown.store(buf);
            m_vCameraPosAndAlphaTest.store(buf);

            buf.putInt(m_uNumLights);
            buf.putInt(m_uNumSpotLights);
            buf.putInt(m_uWindowWidth);
            buf.putInt(m_uWindowHeight);

            buf.putInt(m_uMaxNumLightsPerTile);
            buf.putInt(m_uMaxNumElementsPerTile);
            buf.putInt(m_uNumTilesX);
            buf.putInt(m_uNumTilesY);

            buf.putInt(m_uMaxVPLs);
            buf.putInt(m_uMaxNumVPLsPerTile);
            buf.putInt(m_uMaxNumVPLElementsPerTile);
            buf.putFloat(m_fVPLSpotStrength);

            buf.putFloat(m_fVPLSpotRadius);
            buf.putFloat(m_fVPLPointStrength);
            buf.putFloat(m_fVPLPointRadius);
            buf.putFloat(m_fVPLRemoveBackFaceContrib);

            buf.putFloat(m_fVPLColorThreshold);
            buf.putFloat(m_fVPLBrightnessThreshold);
            buf.putLong(0);
            return buf;
        }
    };

    private static final class CB_SHADOW_CONSTANTS{
        static final int SIZE = (MAX_NUM_SHADOWCASTING_POINTS * 6 +MAX_NUM_SHADOWCASTING_SPOTS) * Matrix4f.SIZE + Vector4f.SIZE;

//        final Matrix4f[][] m_mPointShadowViewProj = new Matrix4f[ MAX_NUM_SHADOWCASTING_POINTS ][ 6 ];
//        final Matrix4f[] m_mSpotShadowViewProj = new Matrix4f[ MAX_NUM_SHADOWCASTING_SPOTS ];
//        final Vector4f m_ShadowBias = new Vector4f();
    };
}
