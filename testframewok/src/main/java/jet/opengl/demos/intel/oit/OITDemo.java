package jet.opengl.demos.intel.oit;

import com.nvidia.developer.opengl.app.GLEventListener;
import com.nvidia.developer.opengl.app.NvSampleApp;
import com.nvidia.developer.opengl.utils.NvGPUTimer;

import org.lwjgl.util.vector.Vector;
import org.lwjgl.util.vector.Vector3f;

import java.io.IOException;
import java.nio.IntBuffer;
import java.util.ArrayList;

import jet.opengl.demos.intel.cput.CPUTAssetLibrary;
import jet.opengl.demos.intel.cput.CPUTAssetSet;
import jet.opengl.demos.intel.cput.CPUTBufferDX11;
import jet.opengl.demos.intel.cput.CPUTCamera;
import jet.opengl.demos.intel.cput.CPUTMaterial;
import jet.opengl.demos.intel.cput.CPUTMesh;
import jet.opengl.demos.intel.cput.CPUTMeshDX11;
import jet.opengl.demos.intel.cput.CPUTModel;
import jet.opengl.demos.intel.cput.CPUTModelDX11;
import jet.opengl.demos.intel.cput.CPUTRenderParameters;
import jet.opengl.demos.intel.cput.CPUTRenderParametersDX;
import jet.opengl.demos.intel.cput.CPUTRenderStateBlockDX11;
import jet.opengl.demos.intel.cput.CPUTRenderTargetColor;
import jet.opengl.demos.intel.cput.CPUTRenderTargetDepth;
import jet.opengl.demos.intel.cput.CPUTSprite;
import jet.opengl.demos.intel.cput.ID3D11InputLayout;
import jet.opengl.demos.scene.BaseScene;
import jet.opengl.demos.scene.CameraData;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.CacheBuffer;

public final class OITDemo extends BaseScene {
    private CPUTAssetSet          mpConservatorySet;
    private CPUTAssetSet          mpGroundSet;
    private CPUTAssetSet		  mpoutdoorPlantsSet;
    private CPUTAssetSet		  mpindoorPlantsSet;
    private CPUTAssetSet		  mpBarrierSet;
//    CPUTCameraController  *mpCameraController;
    private CPUTSprite            mpDebugSprite;
    private CPUTSprite			  mpSkyBoxSprite;
    private CPUTSprite			  mpFSSprite;
    private CPUTCamera            mpCamera = new CPUTCamera();
    private CPUTCamera            mpShadowCamera;

    private CPUTMaterial          mpGrassA1Material;
    private CPUTMaterial		  mpGrassA2Material;
    private CPUTMaterial		  mpLeavesTreeAMaterial;
    private CPUTMaterial		  mpPRP_ShrubSmall_DM_AMaterial;
    private CPUTMaterial		  mpGlassMaterial;
    private CPUTMaterial		  mpFenceMaterial;

    private CPUTRenderTargetColor mpMSAABackBuffer;
    private CPUTRenderTargetDepth mpMSAADepthBuffer;

//    CPUTText               *mpFPSCounter;
    private CPUTRenderTargetDepth  mpShadowRenderTarget;
    private CPUTRenderTargetDepth  mpInternalShadowRenderTarget;

    // AOIT Stuff
    private boolean                mROVsSupported;
    private AOITTechnique		   mPostProcess;

    private CPUTSprite             mpDepthSprite;
    private CPUTSprite             mpResolveSprite;

    // Debug stuff
    private CPUTBufferDX11      mpAOITDebugViewConsts;
    private CPUTMaterial        mpAOITDebugViewMaterial;
    private CPUTMaterial        mpAOITDebugDepthMaterial;
    private CPUTMaterial        mpAOITDebugMSAADepthMaterial;
    private CPUTMaterial        mpResolveMaterial;
    private CPUTMaterial  		mpSkyBoxMaterial;

    private boolean        mpShowDebugView;
    private boolean        mpShowDepth;
    private boolean		mpZoomBox;
    private boolean 		mbPixelSync;
    private boolean		mbVsync;
    private boolean		mbFullscreen;
    private boolean		mpSortFoliage;

    private boolean        mpShowStats;

    private NvGPUTimer  mGPUTimerPrePass;
    private NvGPUTimer    mGPUTimerAOITCreation;
    private NvGPUTimer    mGPUTimerResolve;
    private NvGPUTimer    mGPUTimerAll;
    private final FrameStats Stats = new FrameStats();

    private CPUTRenderTargetColor mpBackBuffer;
    private CPUTRenderTargetDepth mpDepthBuffer;

    RenderType		mpRenderType = RenderType.AlphaBlending;

    static final int
        TwoNode = 0,
        FourNode = 1,
        EightNode = 2;
    int		mpNodeCount = TwoNode;

    private static final int SHADOW_WIDTH_HEIGHT = 8192;
    // set file to open
    private String g_OpenFilePath;
    private String g_OpenShaderPath;
    private String g_OpenFileName;
    private String g_OpenSceneFileName = "../../../Media/defaultscene.scene";
    private final CPUTZoomBox g_CPUTZoomBox = new CPUTZoomBox();

    @Override
    protected void onCreate(Object prevSavedData) {
        // Call ResizeWindow() because it creates some resources that our blur material needs (e.g., the back buffer)
        int width, height;
//        mpWindow->GetClientDimensions(&width, &height);
        width = mNVApp.getGLContext().width();
        height = mNVApp.getGLContext().height();

        CPUTRenderStateBlockDX11 pBlock = new CPUTRenderStateBlockDX11();
        CPUTRenderStateDX11 pStates = pBlock.GetState();

        CPUTAssetLibrary pAssetLibrary = CPUTAssetLibrary.GetAssetLibrary();

//        String ExecutableDirectory;
        String mediaDirectory = "Intel/OIT/Media/";
//        CPUTFileSystem::GetExecutableDirectory(&ExecutableDirectory);
//        CPUTFileSystem::ResolveAbsolutePathAndFilename(ExecutableDirectory + _L("../../../Media/"), &mediaDirectory);

        pAssetLibrary.SetMediaDirectoryName(mediaDirectory);
        pAssetLibrary.SetSystemDirectoryName(mediaDirectory + ("System/"));

        /*CPUTGuiControllerDX11 *pGUI = CPUTGetGuiController();
        pAssetLibrary->SetAssetDirectoryName(mediaDirectory + _L("gui_assets/"));
        pGUI->Initialize(mpContext, mediaDirectory);
        pGUI->SetCallback(this);
        pGUI->SetWindow(mpWindow);

        CPUTFont *pFont = CPUTFont::CreateFont(mediaDirectory + _L("System/Font/"), _L("arial_16.fnt"));
        pGUI->SetFont(pFont);*/

        // Create some controls
        //
        // Create some controls
        //


        UpdateEnableStatsCheckbox();


        // Add our programatic (and global) material parameters
        CPUTMaterial.mGlobalProperties.AddValue( "cbPerFrameValues", "$cbPerFrameValues" );
        CPUTMaterial.mGlobalProperties.AddValue( "cbPerModelValues", "$cbPerModelValues" );
        CPUTMaterial.mGlobalProperties.AddValue( "_Shadow", "$shadow_depth" );
        CPUTMaterial.mGlobalProperties.AddValue( "_InternalShadow", "$internalshadow" );
        CPUTMaterial.mGlobalProperties.AddValue( "OffscreenColorBuffer", "$OffscreenColorBuffer" );

        AddGlobalProperties();

//        if(GetFeatureLevel() >= D3D_FEATURE_LEVEL_11_0)
//            mPostProcess.OnCreate(mpD3dDevice, width, height, mpContext, mpSwapChain);  TODO

        // load shadow casting material+sprite object
        mpShadowRenderTarget = new CPUTRenderTargetDepth();
        mpShadowRenderTarget.CreateRenderTarget( "$shadow_depth", SHADOW_WIDTH_HEIGHT, SHADOW_WIDTH_HEIGHT, GLenum.GL_DEPTH_COMPONENT32F,1,false);
        mpInternalShadowRenderTarget = new CPUTRenderTargetDepth();
        mpInternalShadowRenderTarget.CreateRenderTarget( "$internalshadow", SHADOW_WIDTH_HEIGHT, SHADOW_WIDTH_HEIGHT, GLenum.GL_DEPTH_COMPONENT32F,1,false );


        mpBackBuffer = new CPUTRenderTargetColor();
        mpBackBuffer.CreateRenderTarget( "$OffscreenColorBuffer", width, height, GLenum.GL_RGBA8);
        mpDepthBuffer = new CPUTRenderTargetDepth();
        mpDepthBuffer.CreateRenderTarget( "$OffscreenDepthBuffer", width, height, GLenum.GL_DEPTH_COMPONENT32F,1,false );

        g_CPUTZoomBox.OnCreate();

	    final int MSAA_COUNT  = 4;

        mpMSAABackBuffer = new CPUTRenderTargetColor();
        mpMSAABackBuffer.CreateRenderTarget("$MSAAColorBuffer", width, height, GLenum.GL_RGBA8 /*DXGI_FORMAT_R8G8B8A8_UNORM_SRGB*/, MSAA_COUNT, false, false);

        mpMSAADepthBuffer = new CPUTRenderTargetDepth();
        mpMSAADepthBuffer.CreateRenderTarget("$MSAADepthBuffer", width, height, GLenum.GL_DEPTH_COMPONENT32F, MSAA_COUNT, false);

        // Override default sampler desc for our default shadowing sampler
        /*pStates->SamplerDesc[1].Filter         = D3D11_FILTER_COMPARISON_MIN_MAG_LINEAR_MIP_POINT;  TODO
        pStates->SamplerDesc[1].AddressU       = D3D11_TEXTURE_ADDRESS_BORDER;
        pStates->SamplerDesc[1].AddressV       = D3D11_TEXTURE_ADDRESS_BORDER;
        pStates->SamplerDesc[1].ComparisonFunc = D3D11_COMPARISON_GREATER;
        pBlock->CreateNativeResources();
        pAssetLibrary->AddRenderStateBlock( _L("$DefaultRenderStates"), _L(""), _L(""), pBlock );
        pBlock->Release(); // We're done with it.  The library owns it now.*/

        // ***************************
        // Render the terrain to our height field texture.
        // TODO: How much of this memory can we reclaim after this step?
        // Can theoretically just release.  But, AssetLibrary holds references too.
        // ***************************

        ResizeWindow(width, height);

        try{
            String ExecutableDirectory = "";
            pAssetLibrary.SetMediaDirectoryName( ExecutableDirectory +   _L("..\\..\\..\\Media\\conservatory_01\\") );
            mpConservatorySet = pAssetLibrary.GetAssetSet( _L("conservatory_01") );
            mpGlassMaterial = pAssetLibrary.GetMaterial( _L("Glass1"),false );
            mpFenceMaterial = pAssetLibrary.GetMaterial( _L("Fence"),false );


            pAssetLibrary.SetMediaDirectoryName( ExecutableDirectory +    _L("..\\..\\..\\Media\\ground\\") );
            mpGroundSet = pAssetLibrary.GetAssetSet( _L("ground") );


            pAssetLibrary.SetMediaDirectoryName(  ExecutableDirectory +   _L("..\\..\\..\\Media\\outdoorPlants_01\\") );
            mpoutdoorPlantsSet = pAssetLibrary.GetAssetSet( _L("outdoorPlants_01") );
            mpGrassA1Material = pAssetLibrary.GetMaterial( _L("grassA1"),false );
            mpGrassA2Material = pAssetLibrary.GetMaterial( _L("grassA2"),false );

            pAssetLibrary.SetMediaDirectoryName( ExecutableDirectory +    _L("..\\..\\..\\Media\\indoorPlants_01\\") );
            mpindoorPlantsSet = pAssetLibrary.GetAssetSet( _L("indoorPlants_01") );

            mpLeavesTreeAMaterial = pAssetLibrary.GetMaterial( _L("leavesTreeA"),false );
            mpPRP_ShrubSmall_DM_AMaterial = pAssetLibrary.GetMaterial( _L("PRP_ShrubSmall_DM_A"),false );

            pAssetLibrary.SetMediaDirectoryName( ExecutableDirectory +    _L("..\\..\\..\\Media\\barrier_01\\") );
            mpBarrierSet = pAssetLibrary.GetAssetSet( _L("barrier_01") );

            pAssetLibrary.SetMediaDirectoryName( ExecutableDirectory +    _L("..\\..\\..\\Media\\"));
            mpSkyBoxMaterial = pAssetLibrary.GetMaterial( "SkyBox11" ,false);

            mpSkyBoxSprite = CPUTSprite.CreateSprite( -1.0f, -1.0f, 2.0f, 2.0f, _L("SkyBox11"));

            mpFSSprite = CPUTSprite.CreateSprite( -1.0f, -1.0f, 2.0f, 2.0f, _L("FSSprite"));
        }catch (IOException e){
            e.printStackTrace();
        }

        CreateDebugViews();

        CreateCameras(width, height);
    }

    private static String _L(String str) {return str;}

    @Override
    protected void update(float dt) {
//        static int sbFullscreen = -1;

//        mpCameraController.Update((float)deltaSeconds);


        mpSortFoliage = (mpRenderType == RenderType.AlphaBlending);

        /*if (sbFullscreen == -1)
        {
            mbFullscreen = sbFullscreen = CPUTGetFullscreenState();
        }

        if (mbFullscreen != ((sbFullscreen == 1) ? true: false))
        {
            sbFullscreen = mbFullscreen;
            CPUTToggleFullScreenMode();
        }*/
    }

    @Override
    public void onResize(int width, int height) {
        super.onResize(width, height);

        CPUTAssetLibrary pAssetLibrary = CPUTAssetLibrary.GetAssetLibrary();

        // Before we can resize the swap chain, we must release any references to it.
        // We could have a "AssetLibrary::ReleaseSwapChainResources(), or similar.  But,
        // Generic "release all" works, is simpler to implement/maintain, and is not performance critical.
        pAssetLibrary.ReleaseTexturesAndBuffers();

        // Resize the CPUT-provided render target
//        CPUT_DX11::ResizeWindow( width, height );

//        if(GetFeatureLevel() >= D3D_FEATURE_LEVEL_11_0)
        if(mPostProcess != null)
        {
            mPostProcess.OnSize(width, height);
        }

        mpBackBuffer.RecreateRenderTarget(width, height, 0, 1 );
        mpDepthBuffer.RecreateRenderTarget(width, height );

        g_CPUTZoomBox.OnSize( width, height);

        // Resize any application-specific render targets here
        if( mpCamera != null) mpCamera.SetAspectRatio(((float)width)/((float)height));

        pAssetLibrary.RebindTexturesAndBuffers();
    }

    private final CPUTRenderParametersDX renderParams = new CPUTRenderParametersDX();
    private int frameIndex = 0;
    @Override
    protected void onRender(boolean clearFBO) {
        frameIndex ++;

        renderParams.mpShadowCamera = null;
        renderParams.mpCamera = mpShadowCamera;
//        renderParams.mpPerFrameConstants = (CPUTBuffer*)mpPerFrameConstantBuffer;  TODO
//        renderParams.mpPerModelConstants = (CPUTBuffer*)mpPerModelConstantBuffer;  TODO
        /*int windowWidth, windowHeight;
        mpWindow->GetClientDimensions( &windowWidth, &windowHeight);*/
        renderParams.mWidth = mNVApp.getGLContext().width();
        renderParams.mHeight = mNVApp.getGLContext().height();


        UpdatePerFrameConstantBuffer(renderParams, deltaSeconds);
        //*******************************
        // Draw the shadow scene
        //*******************************

        // 2. Draw the shadowed scene using a standard shadow map from the light's point of view
        // one could also use cascades shadow maps or other shadowing techniques.  We choose simple
        // shadow mapping for simplicity/demonstration purposes
        CPUTCamera pLastCamera = mpCamera;
        if(frameIndex==1)
        {
            mpCamera = renderParams.mpCamera = mpShadowCamera;
            mpShadowRenderTarget.SetRenderTarget( renderParams, 0, 0.0f, true );

            BeginDrawLists();
            if( mpConservatorySet != null) { mpConservatorySet.RenderRecursive( renderParams, CPUT_MATERIAL_INDEX_SHADOW_CAST ); }
            if( mpindoorPlantsSet != null) { mpindoorPlantsSet.RenderRecursive( renderParams, CPUT_MATERIAL_INDEX_SHADOW_CAST ); }
            if( mpoutdoorPlantsSet != null) { mpoutdoorPlantsSet.RenderRecursive( renderParams, CPUT_MATERIAL_INDEX_SHADOW_CAST ); }
            if( mpBarrierSet != null) { mpBarrierSet.RenderRecursive(renderParams,CPUT_MATERIAL_INDEX_SHADOW_CAST); }
            DrawSolidLists(renderParams);
            DrawTransparentLists(renderParams,true );

            mpShadowRenderTarget.RestoreRenderTarget(renderParams);

            mpInternalShadowRenderTarget.SetRenderTarget( renderParams, 0, 0.0f, true );
            BeginDrawLists();

            if( mpConservatorySet != null) { mpConservatorySet.RenderRecursive(renderParams, CPUT_MATERIAL_INDEX_SHADOW_CAST); }
            if( mpindoorPlantsSet != null) { mpindoorPlantsSet.RenderRecursive( renderParams, CPUT_MATERIAL_INDEX_SHADOW_CAST ); }
            if( mpoutdoorPlantsSet != null) { mpoutdoorPlantsSet.RenderRecursive( renderParams, CPUT_MATERIAL_INDEX_SHADOW_CAST ); }
            DrawSolidLists(renderParams);

            mpInternalShadowRenderTarget.RestoreRenderTarget(renderParams);


            mpCamera = renderParams.mpCamera = pLastCamera;
        }

        renderParams.mpCamera = mpCamera;
        renderParams.mpShadowCamera = mpShadowCamera;
        UpdatePerFrameConstantBuffer(renderParams, deltaSeconds);

        // Clear back buffer
        final float clearColor[] = { 0.0f, 0.5f, 1.0f, 1.0f };
        /*
        mpContext->ClearRenderTargetView( mpBackBufferRTV,  clearColor );
        mpContext->ClearDepthStencilView( mpDepthStencilView, D3D11_CLEAR_DEPTH | D3D11_CLEAR_STENCIL, 0.0f, 0);*/
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
        gl.glClearColor(0.0f, 0.5f, 1.0f, 1.0f);
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT|GLenum.GL_DEPTH_BUFFER_BIT|GLenum.GL_STENCIL_BUFFER_BIT);

        BeginDrawLists();

        int NodeIndex = 0;


        if(mpRenderType == RenderType.AlphaBlending)
        {
            mpGlassMaterial.SetCurrentEffect(0);
            if(!mpSortFoliage)
            {
                mpGrassA1Material.SetCurrentEffect(0);
                mpGrassA2Material.SetCurrentEffect(0);
                mpLeavesTreeAMaterial.SetCurrentEffect(0);
                mpPRP_ShrubSmall_DM_AMaterial.SetCurrentEffect(0);
            }
            else
            {
                mpGrassA1Material.SetCurrentEffect(1);
                mpGrassA2Material.SetCurrentEffect(1);
                mpLeavesTreeAMaterial.SetCurrentEffect(1);
                mpPRP_ShrubSmall_DM_AMaterial.SetCurrentEffect(1);
            }
            mpFenceMaterial.SetCurrentEffect(0);
        }
        else if(mpRenderType == RenderType.AlphaBlending_A2C)
        {
            mpGlassMaterial.SetCurrentEffect(0);

            mpGrassA1Material.SetCurrentEffect(2);
            mpGrassA2Material.SetCurrentEffect(2);
            mpLeavesTreeAMaterial.SetCurrentEffect(2);
            mpPRP_ShrubSmall_DM_AMaterial.SetCurrentEffect(2);
            mpFenceMaterial.SetCurrentEffect(1);
        }
        else if((mpRenderType == RenderType.ROV_OIT|| mpRenderType == RenderType.ROV_HDR_OIT) && mROVsSupported)
        {
            int MaterialOffset = (mbPixelSync)?3:0;
            int HDROffset = (mpRenderType == RenderType.ROV_HDR_OIT) ? 6 : 0;

            if(mpNodeCount == TwoNode)
            {
                mpGlassMaterial.SetCurrentEffect(1+MaterialOffset+ HDROffset);
                mpGrassA1Material.SetCurrentEffect(3+MaterialOffset + HDROffset);
                mpGrassA2Material.SetCurrentEffect(3+MaterialOffset + HDROffset);
                mpLeavesTreeAMaterial.etCurrentEffect(3+MaterialOffset + HDROffset);
                mpPRP_ShrubSmall_DM_AMaterial.SetCurrentEffect(3+MaterialOffset + HDROffset);
                mpFenceMaterial.SetCurrentEffect(2+MaterialOffset + HDROffset);
            }
            else if(mpNodeCount == FourNode)
            {
                mpGlassMaterial.SetCurrentEffect(2+MaterialOffset + HDROffset);
                mpGrassA1Material.SetCurrentEffect(4+MaterialOffset + HDROffset);
                mpGrassA2Material.SetCurrentEffect(4+MaterialOffset + HDROffset);
                mpLeavesTreeAMaterial.SetCurrentEffect(4+MaterialOffset + HDROffset);
                mpPRP_ShrubSmall_DM_AMaterial.etCurrentEffect(4+MaterialOffset + HDROffset);
                mpFenceMaterial.SetCurrentEffect(3+MaterialOffset + HDROffset);
                NodeIndex=1;
            }
            else if(mpNodeCount == EightNode)
            {
                mpGlassMaterial.SetCurrentEffect(3+MaterialOffset + HDROffset);
                mpGrassA1Material.SetCurrentEffect(5+MaterialOffset + HDROffset);
                mpGrassA2Material.SetCurrentEffect(5+MaterialOffset + HDROffset);
                mpLeavesTreeAMaterial.SetCurrentEffect(5+MaterialOffset + HDROffset);
                mpPRP_ShrubSmall_DM_AMaterial.SetCurrentEffect(5+MaterialOffset + HDROffset);
                mpFenceMaterial.SetCurrentEffect(4+MaterialOffset + HDROffset);
                NodeIndex=2;
            }

        }
        else if(mpRenderType == RenderType.DX11_AOIT)
        {
            mpGlassMaterial.SetCurrentEffect(13);

            mpGrassA1Material.SetCurrentEffect(15);
            mpGrassA2Material.SetCurrentEffect(15);
            mpLeavesTreeAMaterial.SetCurrentEffect(15);
            mpPRP_ShrubSmall_DM_AMaterial.SetCurrentEffect(15);
            mpFenceMaterial.SetCurrentEffect(14);
        }

        if(mpRenderType == RenderType.AlphaBlending_A2C)
        {
            mpMSAABackBuffer.SetRenderTarget( renderParams, mpMSAADepthBuffer, 0, clearColor, true, 1.f );
        }
        else
        {
            if(mpZoomBox)
                mpBackBuffer.SetRenderTarget(  renderParams, mpDepthBuffer, 0, clearColor, true, 1.f );
        }

        mpSkyBoxSprite.DrawSprite(renderParams,mpSkyBoxMaterial);
        if( mpGroundSet != null) { mpGroundSet.RenderRecursive(renderParams); }
        if( mpBarrierSet  != null) { mpBarrierSet.RenderRecursive(renderParams); }
        if( mpindoorPlantsSet  != null) { mpindoorPlantsSet.RenderRecursive(renderParams); }
        if( mpConservatorySet  != null) { mpConservatorySet.RenderRecursive(renderParams); }
        if( mpoutdoorPlantsSet  != null) { mpoutdoorPlantsSet.RenderRecursive(renderParams); }


        // Render Solid Objects and any normal transpartent objects
        {
//#ifdef AOIT_METRICS
//            CPUTGPUProfilerDX11_AutoScopeProfile timer( mGPUTimerPrePass, mpShowStats );
//#endif
            DrawSolidLists(renderParams);

            if(mpRenderType == RenderType.AlphaBlending_A2C || mpRenderType == RenderType.AlphaBlending )
            {
                // Basic Pass rebder scene once with no AOIT
                DrawTransparentLists(renderParams,true );
            }
        }


        // Render AOIT Objects a

        if (mpRenderType == RenderType.ROV_OIT || mpRenderType == RenderType.ROV_HDR_OIT || mpRenderType == RenderType.DX11_AOIT)
        {
            if (mpRenderType == RenderType.ROV_OIT || mpRenderType == RenderType.ROV_HDR_OIT )
            {
//	#ifdef AOIT_METRICS
//                CPUTGPUProfilerDX11_AutoScopeProfile timer( mGPUTimerAOITCreation, mpShowStats );
//	#endif
                mPostProcess.InitFrameRender(mpRenderType, NodeIndex, mpShowDebugView);
                DrawTransparentLists(renderParams,true );
            }
            else if(mpRenderType == RenderType.DX11_AOIT)
            {
//	#ifdef AOIT_METRICS
//                CPUTGPUProfilerDX11_AutoScopeProfile timer( mGPUTimerAOITCreation,  mpShowStats );
//	#endif
                mPostProcess.InitFrameRender( mpRenderType, 0, mpShowDebugView);
                DrawTransparentLists(renderParams,true);
            }

//#ifdef AOIT_METRICS
//            CPUTGPUProfilerDX11_AutoScopeProfile timer( mGPUTimerResolve,  mpShowStats );
//#endif
            mPostProcess.FinalizeFrameRender(mpRenderType,  true ,NodeIndex, mpShowDebugView);
        }

        if(mpRenderType == RenderType.AlphaBlending_A2C)
        {
            mpMSAABackBuffer.RestoreRenderTarget( renderParams );

            if(mpZoomBox)
            {
                mpBackBuffer.SetRenderTarget(  renderParams, mpDepthBuffer, 0, clearColor, true, 1.f );
                mpResolveSprite.DrawSprite(renderParams, mpResolveMaterial );
                mpBackBuffer.RestoreRenderTarget( renderParams );
            }

            mpResolveSprite.DrawSprite(renderParams, mpResolveMaterial );
        }
        else
        {
            if(mpZoomBox)
            {
                mpBackBuffer.RestoreRenderTarget( renderParams );
                mpFSSprite.DrawSprite(renderParams);
            }
        }
        DisplayDebugViews(renderParams);

        /*if(mpCameraController.GetCamera() == mpShadowCamera)
        {
            mpDebugSprite->DrawSprite(renderParams);  TODO
        }*/

//        RenderText();
//        CPUTDrawGUI();

//        ImGui::Render();

        if(mpZoomBox)
            g_CPUTZoomBox.OnFrameRender( renderParams, false );
    }

    void UpdateEnableStatsCheckbox( )
    {
        boolean bEnableStats = true;

        if(mpShowDebugView)
            bEnableStats = false;

//        if( mSyncInterval )// && ! CPUTGetFullscreenState() )
//            bEnableStats = false;

        mpShowStats = bEnableStats;
    }

    @Override
    protected void onDestroy() {
        SAFE_RELEASE(mpResolveMaterial);
        SAFE_RELEASE(mpAOITDebugDepthMaterial);
        SAFE_RELEASE(mpAOITDebugMSAADepthMaterial);
        SAFE_RELEASE(mpAOITDebugViewMaterial);
        SAFE_RELEASE(mpAOITDebugViewConsts);

        g_CPUTZoomBox.OnShutdown();

        DeleteDebugViews();

        if(mPostProcess != null) mPostProcess.OnShutdown();

        // Note: these two are defined in the base.  We release them because we addref them.
        SAFE_RELEASE(mpGrassA1Material);
        SAFE_RELEASE(mpGrassA2Material);
        SAFE_RELEASE(mpLeavesTreeAMaterial);
        SAFE_RELEASE(mpPRP_ShrubSmall_DM_AMaterial);
        SAFE_RELEASE(mpGlassMaterial);
        SAFE_RELEASE(mpFenceMaterial);
        SAFE_RELEASE(mpSkyBoxMaterial);

        SAFE_RELEASE(mpCamera);
        SAFE_RELEASE(mpShadowCamera);
        SAFE_RELEASE(mpConservatorySet);
        SAFE_RELEASE(mpGroundSet);
        SAFE_RELEASE(mpoutdoorPlantsSet);
        SAFE_RELEASE( mpBarrierSet );
        SAFE_RELEASE( mpShadowRenderTarget );
        SAFE_RELEASE( mpInternalShadowRenderTarget);

        SAFE_RELEASE(mpindoorPlantsSet);
        SAFE_DELETE( mpCameraController );
        SAFE_RELEASE( mpDebugSprite);
        SAFE_RELEASE( mpSkyBoxSprite );
        SAFE_RELEASE( mpFSSprite );
        SAFE_RELEASE( mpDebugSprite);
        SAFE_RELEASE( mpMSAABackBuffer);
        SAFE_RELEASE( mpMSAADepthBuffer);
        SAFE_RELEASE( mpBackBuffer);
        SAFE_RELEASE( mpDepthBuffer);
    }

    void AddGlobalProperties()
    {
        CPUTMaterial.mGlobalProperties.AddValue( _L("gFragmentListNodesUAV"), _L("$gFragmentListNodesUAV") );
        CPUTMaterial.mGlobalProperties.AddValue( _L("gFragmentListFirstNodeAddressUAV"), _L("$gFragmentListFirstNodeAddressUAV") );
        CPUTMaterial.mGlobalProperties.AddValue( _L("gAOITSPClearMaskUAV"), _L("$gAOITSPClearMaskUAV") );
        CPUTMaterial.mGlobalProperties.AddValue( _L("gAOITSPDepthDataUAV"), _L("$gAOITSPDepthDataUAV") );
        CPUTMaterial.mGlobalProperties.AddValue( _L("gAOITSPColorDataUAV"), _L("$gAOITSPColorDataUAV") );
        CPUTMaterial.mGlobalProperties.AddValue( _L("g8AOITSPDepthDataUAV"), _L("$g8AOITSPDepthDataUAV") );
        CPUTMaterial.mGlobalProperties.AddValue( _L("g8AOITSPColorDataUAV"), _L("$g8AOITSPColorDataUAV") );
        CPUTMaterial.mGlobalProperties.AddValue( _L("g_IntelExt"), _L("$g_IntelExt") );

        CPUTMaterial.mGlobalProperties.AddValue( _L("cbAOITDebugViewConsts"), _L("$cbAOITDebugViewConsts") );
        CPUTMaterial.mGlobalProperties.AddValue( _L("FL_Constants"), _L("$FL_Constants") );
    }

    void CreateDebugViews()
    {
        CPUTAssetLibrary pAssetLibrary = CPUTAssetLibrary.GetAssetLibrary();

        // Create our constant buffer.

        /*D3D11_BUFFER_DESC bd = {0};
        bd.ByteWidth = sizeof(AOITDebugViewConsts);
        bd.BindFlags = D3D11_BIND_CONSTANT_BUFFER;
        bd.Usage = D3D11_USAGE_DYNAMIC;
        bd.CPUAccessFlags = D3D11_CPU_ACCESS_WRITE;

        ID3D11Buffer * pOurConstants;
        HRESULT hr = (CPUT_DX11::GetDevice())->CreateBuffer( &bd, NULL, &pOurConstants );*/
        BufferGL pOurConstants = new BufferGL();
        pOurConstants.initlize(GLenum.GL_UNIFORM_BUFFER, 16, null, GLenum.GL_DYNAMIC_DRAW);

        String name = _L("$cbAOITDebugViewConsts");
        mpAOITDebugViewConsts = new CPUTBufferDX11( name, pOurConstants );

        CPUTAssetLibrary.GetAssetLibrary().AddConstantBuffer( name,  mpAOITDebugViewConsts );
//        SAFE_RELEASE(pOurConstants); // We're done with it.  The CPUTBuffer now owns it.
        //mGlobalProperties

        String ExecutableDirectory= "";
//        CPUTFileSystem::GetExecutableDirectory(&ExecutableDirectory);

        pAssetLibrary.SetMediaDirectoryName(ExecutableDirectory +     _L("..\\..\\..\\Media\\DebugAssets\\") );
        mpAOITDebugViewMaterial = pAssetLibrary.GetMaterial( "AOITDebugView" ,false);
        mpAOITDebugDepthMaterial = pAssetLibrary.GetMaterial( "AOITDebugDepth" ,false);
        mpAOITDebugMSAADepthMaterial = pAssetLibrary.GetMaterial( "AOITDebugMSAADepth" ,false);
        mpResolveMaterial = pAssetLibrary.GetMaterial( "ResolveTarget" ,false);

        mpDebugSprite = CPUTSprite.CreateSprite(0.5f, 0.5f, 0.5f, 0.5f, _L("Sprite") );
        mpDepthSprite = CPUTSprite.CreateSprite(-1.0f, 0.5f, 0.5f, 0.5f, _L("Sprite") );
        mpResolveSprite = CPUTSprite.CreateSprite(-1.0f, -1.0f, 2.0f, 2.0f, _L("ResolveTarget") );

    }


    void DisplayDebugViews(CPUTRenderParametersDX renderParams)
    {
        CPUTAssetLibrary pAssetLibrary = CPUTAssetLibrary.GetAssetLibrary();

        if (mpRenderType == RenderType.ROV_OIT || mpRenderType == RenderType.ROV_HDR_OIT)
        {
            if (true == mROVsSupported)
            {
                BufferGL pBuffer = mpAOITDebugViewConsts.GetNativeBuffer();
                // update parameters of constant buffer
//                D3D11_MAPPED_SUBRESOURCE mapInfo;
//                mpContext->Map(pBuffer, 0, D3D11_MAP_WRITE_DISCARD, 0, &mapInfo);
                {
//                    AOITDebugViewConsts & consts = *((AOITDebugViewConsts*)mapInfo.pData);
                    IntBuffer consts = CacheBuffer.wrap(0,8,0,2);
                    // for future use
//                    consts.a = 0;
//                    consts.b = 8;
//                    consts.c = 0;
//                    consts.d = 2;
//                    mpContext->Unmap(pBuffer, 0);
                    pBuffer.update(0, consts);
                }
                /*mpContext->VSSetConstantBuffers(4, 1, &pBuffer);
                mpContext->PSSetConstantBuffers(4, 1, &pBuffer);
                mpContext->CSSetConstantBuffers(4, 1, &pBuffer);*/
                if ((mpAOITDebugViewConsts != null) && (mpShowDebugView))
                {

                    mpDebugSprite.DrawSprite(renderParams, mpAOITDebugViewMaterial);
                    /*ID3D11ShaderResourceView* nullViews[32] = { 0 };
                    mpContext->PSSetShaderResources(0, 32, nullViews);*/
                }
            }
        }
        if( (mpAOITDebugViewConsts != null) && (mpShowDepth) )
        {
            /*ID3D11RenderTargetView* pSwapChainRTV;		// render target view retrieved at InitFrameRender
            ID3D11DepthStencilView* pDSView;			// depth stencil view retried at InitFrameRender

            mpContext->OMGetRenderTargets(1, &(pSwapChainRTV), &pDSView);  TODO

            mpContext->OMSetRenderTargets(1, &pSwapChainRTV, NULL);*/
            gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
            gl.glDisable(GLenum.GL_DEPTH_TEST);

            if(mpRenderType == RenderType.AlphaBlending_A2C)
            {
                mpDepthSprite.DrawSprite( renderParams, mpAOITDebugMSAADepthMaterial );
            }
            else
            {
                mpDepthSprite.DrawSprite( renderParams, mpAOITDebugDepthMaterial );
            }
            /*ID3D11ShaderResourceView* nullViews[32] = {0};
            mpContext->PSSetShaderResources(0, 32, nullViews);
            mpContext->OMSetRenderTargets(1, &pSwapChainRTV, pDSView);
            SAFE_RELEASE(pSwapChainRTV);
            SAFE_RELEASE(pDSView);*/
        }
    }


    void DeleteDebugViews(  )
    {
        SAFE_RELEASE( mpDebugSprite);
        SAFE_RELEASE( mpDepthSprite);
        SAFE_RELEASE( mpResolveSprite);
    }

    private static final class DrawNode{

        CPUTModelDX11 m_pModel;
        CPUTMeshDX11 m_pMesh;
        CPUTMaterialEffect m_pMaterial;
        ID3D11InputLayout m_mpInputLayout;

        DrawNode(CPUTModelDX11 pModel, CPUTMeshDX11 pMesh, CPUTMaterialEffect pMaterial, ID3D11InputLayout mpInputLayout)
        {
            m_pMesh = pMesh;
            m_pModel = pModel;
            m_pMaterial = pMaterial;
            m_mpInputLayout = mpInputLayout;
        }
    };

    private final ArrayList<DrawNode> m_pMeshNode = new ArrayList<>();
    private final ArrayList<DrawNode> m_pTransparentNode = new ArrayList<>();

    private void BeginDrawLists()
    {
        m_pMeshNode.clear();
        m_pTransparentNode.clear();
    }

    private void AddModel(CPUTModelDX11 pModel,CPUTMeshDX11 pMesh, CPUTMaterialEffect pMaterial, ID3D11InputLayout mpInputLayout, boolean Transparent)
    {
        if(Transparent)
        {
            m_pTransparentNode.add(new DrawNode(pModel, pMesh, pMaterial, mpInputLayout ));
        }
        else
        {
            m_pMeshNode.add(new DrawNode(pModel, pMesh, pMaterial, mpInputLayout ));
        }
    }

    private void DrawSolidLists(CPUTRenderParameters renderParams)
    {
        CPUTModelDX11 pCurrentModel = null;

        for(int x=0; x< m_pMeshNode.size(); x++)
        {
            // Update the model's constant buffer.
            // Note that the materials reference this, so we need to do it only once for all of the model's meshes.
            DrawNode meshNode = m_pMeshNode.get(x);
            if(pCurrentModel != meshNode.m_pModel)
            {
                pCurrentModel = meshNode.m_pModel;
                pCurrentModel.UpdateShaderConstants(renderParams);
            }

            meshNode.m_pMaterial.SetRenderStates(renderParams);

            meshNode.m_pMesh.Draw(renderParams, meshNode.m_mpInputLayout);
        }
    }

    private void DrawTransparentLists(CPUTRenderParameters renderParams, boolean RenderTransparent)
    {
        CPUTModelDX11 pCurrentModel = null;

        if(RenderTransparent)
        {
            for(int x=0; x< m_pTransparentNode.size(); x++)
            {
                // Update the model's constant buffer.
                // Note that the materials reference this, so we need to do it only once for all of the model's meshes.
                DrawNode transparentNode = m_pTransparentNode.get(x);
                if(pCurrentModel != transparentNode.m_pModel)
                {
                    pCurrentModel = transparentNode.m_pModel;
                    pCurrentModel.UpdateShaderConstants(renderParams);
                }

                transparentNode.m_pMaterial.SetRenderStates(renderParams);

                transparentNode.m_pMesh.Draw(renderParams, transparentNode.m_mpInputLayout);
            }
        }
    }

    private boolean DrawModelCallBack(CPUTModel pModel, CPUTRenderParameters renderParams, CPUTMesh pMesh, CPUTMaterialEffect pMaterial,
                                      CPUTMaterialEffect pOriginalMaterial, Object pInputLayout)
    {
        ID3D11InputLayout pLayout = (ID3D11InputLayout )pInputLayout;
        pMaterial.SetRenderStates(renderParams);

        AddModel((CPUTModelDX11)pModel, (CPUTMeshDX11)pMesh, (CPUTMaterialEffectDX11)pMaterial, pLayout,((CPUTMaterialEffectDX11)pOriginalMaterial).mLayer == CPUT_LAYER_TRANSPARENT);

        return true;
    }

    void CreateCameras(int width, int height)
    {
        CPUTAssetLibrary pAssetLibrary = CPUTAssetLibrary.GetAssetLibrary();

        // If no cameras were created from the model sets then create a default simple camera
        if( mpConservatorySet != null && mpConservatorySet.GetCameraCount() > 0)
        {
//            mpCamera = mpConservatorySet.GetFirstCamera();
        }
        else
        {
            pAssetLibrary.AddCamera("Camera",  mpCamera );

            mpCamera.SetPosition( 26.00f, 3.00f, 25.00f );
            mpCamera.LookAt(11.00f,5.33f,15.00f);
            // Set the projection matrix for all of the cameras to match our window.
            // TODO: this should really be a viewport matrix.  Otherwise, all cameras will have the same FOV and aspect ratio, etc instead of just viewport dimensions.
            mpCamera.SetAspectRatio(((float)width)/((float)height));
        }

        final float defaultFOVRadians = 3.14f/2.0f;
        mpCamera.SetFov(defaultFOVRadians); // TODO: Fix converter's FOV bug (Maya generates cameras for which fbx reports garbage for fov)
        mpCamera.SetFarPlaneDistance(100000.0f);
        mpCamera.Update();


        // Position and orient the shadow camera so that it sees the whole scene.
        // Set the near and far planes so that the frustum contains the whole scene.
        // Note that if we are allowed to move the shadow camera or the scene, this may no longer be true.
        // Consider updating the shadow camera when it (or the scene) moves.
        // Treat bounding box half as radius for an approximate bounding sphere.
        // The tightest-fitting sphere might be smaller, but this is close enough for placing our shadow camera.
        // Set up the shadow camera (a camera that sees what the light sees)
        Vector3f lookAtPoint = new Vector3f(0.0f, 0.0f, 0.0f);
        Vector3f half = new Vector3f(1.0f, 1.0f, 1.0f);
        if( mpBarrierSet != null)
        {
            mpBarrierSet.GetBoundingBox( lookAtPoint, half );
        }

        float  length = half.length();
        mpShadowCamera = new CPUTCamera( CPUT_ORTHOGRAPHIC );
        mpShadowCamera.SetAspectRatio(1.0f);
        mpShadowCamera.SetNearPlaneDistance(1.0f);
        mpShadowCamera.SetFarPlaneDistance(2.0f*length + 1.0f);
        mpShadowCamera.SetPosition( lookAtPoint - float3(0, -1, 1) * length );
        mpShadowCamera.LookAt( lookAtPoint.x,lookAtPoint.y,lookAtPoint.z );
        mpShadowCamera.SetWidth( length*2);
        mpShadowCamera.SetHeight(length*2);
        mpShadowCamera.Update();

        /*mpCameraController = new CPUTCameraControllerFPS();
        mpCameraController->SetCamera(mpCamera);
        mpCameraController->SetLookSpeed(0.004f);
        mpCameraController->SetMoveSpeed(20.0f);*/

    }

    // Handle mouse events
//-----------------------------------------------------------------------------
    int  HandleMouseEvent(int x, int y, int wheel, CPUTMouseState state, CPUTEventID message)
    {
        if(state & CPUT_MOUSE_RIGHT_DOWN)
        {
            FLOAT zoomx =  x / (FLOAT) m_ScreenWidth;
            FLOAT zoomy =  y / (FLOAT) m_ScreenHeight;

            g_CPUTZoomBox.SetZoomCenterPosition((float)zoomx, (float)zoomy);
        }

        if( mpCameraController )
        {
            return mpCameraController->HandleMouseEvent(x, y, wheel, state, message);
        }
        return CPUT_EVENT_UNHANDLED;
    }

    private final static class FrameStats
    {
        float AOITCreationTime;
        float ResolveTime;
        float TotalTime;
        float PrePassTime;
    }

    private final static class AOITDebugViewConsts
    {
        int a;
        int b;
        int c;
        int d;
    };
}
