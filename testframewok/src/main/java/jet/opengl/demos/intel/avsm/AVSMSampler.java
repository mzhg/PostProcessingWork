package jet.opengl.demos.intel.avsm;

import com.nvidia.developer.opengl.utils.NvGPUTimer;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4i;

import java.io.IOException;

import jet.opengl.demos.intel.cput.CPUTAssetLibrary;
import jet.opengl.demos.intel.cput.CPUTAssetLibraryDX11;
import jet.opengl.demos.intel.cput.CPUTAssetSet;
import jet.opengl.demos.intel.cput.CPUTCamera;
import jet.opengl.demos.intel.cput.CPUTMaterial;
import jet.opengl.demos.intel.cput.CPUTMaterialDX11;
import jet.opengl.demos.intel.cput.CPUTRenderParameters;
import jet.opengl.demos.scene.BaseScene;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.shader.ShaderProgram;
import jet.opengl.postprocessing.shader.ShaderType;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureUtils;

/**
 * Created by mazhen'gui on 2017/11/11.
 */

public final class AVSMSampler extends BaseScene {
    static final int AVSM_SHADOW_TEXTURE_DIM = 256;
    static final int AVSM_START_NODE_COUNT = 8;
    static final int SHADOW_WIDTH_HEIGHT = 2048;

    //  globals
    final Matrix4f gWorldMatrix = new Matrix4f();
    ParticleSystem gParticleSystem = null;

    private float                  mfElapsedTime;
    private CPUTAssetSet           mpSkyBox;
    private CPUTAssetSet           mpAssetSet;
//    CPUTCameraController  *mpCameraController;


    private CPUTAssetSet          mpShadowCameraSet;
    private Texture2D             mpShadowRenderTarget;

    private float mScreenWidth;
    private float mScreenHeight;

//    CPUTText              *mpFPSCounter;

    // AVSM technique
    private AVSMTechnique                   mpAVSMTechnique;
    private AVSMTechnique.Options          mAppOptions;

    private AVSMGuiState         mCurrentGUIState;


    // Debug stuff
    private boolean                mAVSMExtensionAvailable;
//    AVSMDebugView       *mpAVSMDebugView;

    // GPU timers
    private NvGPUTimer mGPUTimerAVSMCreation;
    private NvGPUTimer    mGPUTimerParticlesRendering;
    private NvGPUTimer    mGPUTimerSolidGeometryRendering;
    private NvGPUTimer    mGPUTimerAll;

    // Mouse
    private boolean                mLButtonDown;
    private boolean                mRButtonDown;
    private CPUTCamera             mpShadowCamera = new CPUTCamera();
    private CPUTCamera             mpCamera = new CPUTCamera();
    private CharSequence  gpDefaultShaderSource;


    @Override
    protected void onCreate(Object prevSavedData) {
        // Detect and report Intel extensions on this system
        /*HRESULT hr = IGFX::Init( this->mpD3dDevice );
        if ( FAILED(hr) )
        {
            printf("Failed hardware detection initialization: incorrect vendor or device.\n\n");
        }
        // detect the available extensions
        IGFX::Extensions extensions = IGFX::getAvailableExtensions( this->mpD3dDevice );

        mAVSMExtensionAvailable = extensions.PixelShaderOrdering;*/
        mAVSMExtensionAvailable = false;

        // report and disable the AVSM extension method if the hardware/driver does not support Pixel Shader Ordering feature
        /*if ( !extensions.PixelShaderOrdering )
        {
            CPUTOSServices::GetOSServices()->OpenMessageBox("Pixel Shader Ordering feature not found",
                "Your hardware or graphics driver does not support the pixel shader ordering feature.  " +
                        "Please update your driver or run on a system that supports the required feature to see that option.  " +
                        "Only the pure DirectX version will be enabled.\n\nSee this link for information and a video about Pixel Shader Ordering in this sample:\n" +
                        "http://software.intel.com/gamecode");
        }*/

        // Create the GUI controls
        CPUTAssetLibraryDX11 pAssetLibrary = (CPUTAssetLibraryDX11) CPUTAssetLibrary.GetAssetLibrary();
        initGUI();

        // create the 'slice' debug view
        /*SAFE_RELEASE(mpAVSMDebugView); TODO
        mpAVSMDebugView = new AVSMDebugView();
        mpAVSMDebugView->Create(ID_MAIN_PANEL, ID_DEBUG_DISPLAY_SLICE_SLIDER, CPUT_CHECKBOX_CHECKED == mpDebugViewShow->GetCheckboxState());
        // create the debug view sprite
        mpAVSMDebugView->CreateSprite();
        */

        // Add our programatic (and global) material parameters
        pAssetLibrary.SetMediaDirectoryName(    "Media\\");
        CPUTMaterial.mGlobalProperties.AddValue( "cbPerFrameValues", "$cbPerFrameValues" );
        CPUTMaterial.mGlobalProperties.AddValue( "cbPerModelValues", "#cbPerModelValues" );
        CPUTMaterial.mGlobalProperties.AddValue( "_Shadow", "$shadow_depth" );

        // Create default shaders
        ShaderProgram pPS       = pAssetLibrary.CreateShaderFromMemory(          "$DefaultShader", /*CPUT_DX11::mpD3dDevice,*/          "PSMain", "ps_4_0", gpDefaultShaderSource, ShaderType.FRAGMENT );
        ShaderProgram pPSNoTex  = pAssetLibrary.CreateShaderFromMemory( "$DefaultShaderNoTexture", /*CPUT_DX11::mpD3dDevice,*/ "PSMainNoTexture", "ps_4_0", gpDefaultShaderSource, ShaderType.FRAGMENT );
        ShaderProgram pVS       = pAssetLibrary.CreateShaderFromMemory(          "$DefaultShader", /*CPUT_DX11::mpD3dDevice,*/          "VSMain", "vs_4_0", gpDefaultShaderSource, ShaderType.VERTEX);
        ShaderProgram pVSNoTex  = pAssetLibrary.CreateShaderFromMemory( "$DefaultShaderNoTexture", /*CPUT_DX11::mpD3dDevice,*/ "VSMainNoTexture", "vs_4_0", gpDefaultShaderSource, ShaderType.VERTEX );

        // We just want to create them, which adds them to the library.  We don't need them any more so release them, leaving refCount at 1 (only library owns a ref)
        /*SAFE_RELEASE(pPS);
        SAFE_RELEASE(pPSNoTex);
        SAFE_RELEASE(pVS);
        SAFE_RELEASE(pVSNoTex);*/

        // load shadow casting material+sprite object
        /*cString ExecutableDirectory;  TODO
        CPUTOSServices::GetOSServices()->GetExecutableDirectory(&ExecutableDirectory);
        pAssetLibrary.SetMediaDirectoryName(  ExecutableDirectory+_L(".\\Media\\"));*/
        pAssetLibrary.SetMediaDirectoryName("Intel/AVSM/Media/");

        /*mpShadowRenderTarget = new CPUTRenderTargetDepth();
        mpShadowRenderTarget->CreateRenderTarget( cString(_L("$shadow_depth")), SHADOW_WIDTH_HEIGHT, SHADOW_WIDTH_HEIGHT, DXGI_FORMAT_D16_UNORM );*/
        mpShadowRenderTarget = TextureUtils.createTexture2D(new Texture2DDesc(SHADOW_WIDTH_HEIGHT, SHADOW_WIDTH_HEIGHT, GLenum.GL_DEPTH_COMPONENT16), null);

        /*CPUTRenderStateBlockDX11 *pBlock = new CPUTRenderStateBlockDX11(); TODO
        CPUTRenderStateDX11 *pStates = pBlock->GetState();

        // Override default sampler desc for our default shadowing sampler
        pStates->SamplerDesc[1].Filter         = D3D11_FILTER_COMPARISON_MIN_MAG_LINEAR_MIP_POINT;
        pStates->SamplerDesc[1].AddressU       = D3D11_TEXTURE_ADDRESS_BORDER;
        pStates->SamplerDesc[1].AddressV       = D3D11_TEXTURE_ADDRESS_BORDER;
        pStates->SamplerDesc[1].ComparisonFunc = D3D11_COMPARISON_LESS; // D3D11_COMPARISON_GREATER

        // what mpAVSMTechnique->mAVSMSampler used to be
        {
            CD3D11_SAMPLER_DESC desc(D3D11_DEFAULT);
            desc.Filter = D3D11_FILTER_MIN_MAG_MIP_POINT;
            desc.AddressU = D3D11_TEXTURE_ADDRESS_CLAMP;
            desc.AddressV = D3D11_TEXTURE_ADDRESS_CLAMP;
            desc.AddressW = D3D11_TEXTURE_ADDRESS_CLAMP;
            pStates->SamplerDesc[2] = desc;
        }

        // what mpAVSMTechnique->mAVSMGenSampler used to be (gAVSMGenCtrlSurfaceSampler in shaders)
        {
            float borderColor[4] = {0.f, 0.f, 0.f, 0.f};
            CD3D11_SAMPLER_DESC desc(D3D11_DEFAULT);
            desc.Filter = D3D11_FILTER_MIN_MAG_MIP_LINEAR;
            desc.AddressU = D3D11_TEXTURE_ADDRESS_BORDER;
            desc.AddressV = D3D11_TEXTURE_ADDRESS_BORDER;
            desc.AddressW = D3D11_TEXTURE_ADDRESS_BORDER;
            *desc.BorderColor = *borderColor;
            pStates->SamplerDesc[3] = desc;
        }

        pBlock->CreateNativeResources();
        CPUTAssetLibrary::GetAssetLibrary()->AddRenderStateBlock( _L("$DefaultRenderStates"), pBlock );
        pBlock->Release(); // We're done with it.  The library owns it now.*/


        // initialize the AVSM subsystem
        int shadowTextureDim     = SHADOW_WIDTH_HEIGHT;
        int avsmShadowTextureDim = AVSM_SHADOW_TEXTURE_DIM;    //  Shadow Texture Dimension - ID_SHADOW_TEXTURE_DIMENSION

        mAppOptions.scene = AVSMTechnique.GROUND_PLANE_SCENE;
        mAppOptions.enableParticles = true;
        mAppOptions.enableAutoBoundsAVSM = false;
        mAppOptions.enableShadowPicking = false;
        mAppOptions.NodeCount = AVSM_START_NODE_COUNT;
        mAppOptions.enableTransmittanceCurve = false;
        mAppOptions.enableVolumeShadowCreation = true;
        mAppOptions.pickedX = 0;
        mAppOptions.pickedY = 0;

        // create the technique class
        mpAVSMTechnique = new AVSMTechnique(/*mpD3dDevice, mpContext,*/ mAppOptions.NodeCount, shadowTextureDim, avsmShadowTextureDim);

        //  Load the custom shaders needed for the AVSM technique
        //-------------------------------------------------------
        /*boolean ReturnCode =*/ mpAVSMTechnique.LoadAVSMShaders(/*mpD3dDevice, mpContext,*/ mAppOptions.NodeCount, shadowTextureDim, avsmShadowTextureDim);
//        ASSERT( (true==ReturnCode), _L("CPUT Error loading AVSM shaders.") );

        //  Create the render states needed for the AVSM technique
        //-------------------------------------------------------
        /*ReturnCode =*/ mpAVSMTechnique.CreateAVSMRenderStates(/*mpD3dDevice, mpContext,*/ mAppOptions.NodeCount, shadowTextureDim, avsmShadowTextureDim);
//        ASSERT( (true==ReturnCode), _L("CPUT Error creating AVSM render states.") );

        //  Create the buffers needed for the AVSM technique
        //-------------------------------------------------------
        /*ReturnCode =*/ mpAVSMTechnique.CreateAVSMBuffers(/*mpD3dDevice, mpContext,*/ mAppOptions.NodeCount, avsmShadowTextureDim);
//        ASSERT( (true==ReturnCode), _L("CPUT Error creating AVSM buffers.") );

        // We wrap buffers used by the AVSM technique when they are used by CPUT because it allows
        // the CPUT shader auto-bind system to automatically bind the buffers when they're used
        //-------------------------------------------------------
        mpAVSMTechnique.WrapBuffers();

        // Set the debug view material
//        mpAVSMDebugView->SetMaterial( pAssetLibrary->GetMaterial( L"AVSMDebugView" ));  TODO


        // Load .set file that was specified on the command line
        // Otherwise, load the default object if no .set was specified
        String MediaPath = ".\\Media\\";
        pAssetLibrary.SetMediaDirectoryName( MediaPath );

        try {
            // Load assets
            mpSkyBox    = pAssetLibrary.GetAssetSet("sky", false );
            mpAssetSet  = pAssetLibrary.GetAssetSet( "roofTop", false );
        } catch (IOException e) {
            e.printStackTrace();
        }


        // If no cameras were created from the model sets then create a default simple camera
        // and add it to the camera array.
        if( mpAssetSet != null && mpAssetSet.GetCameraCount() > 0 )
        {
            /*mpCamera = mpAssetSet->GetFirstCamera(); TODO
            mpCamera->AddRef();*/
        }
        else
        {
            float factorfactor = 1.5f;
            /*mpCamera = new CPUTCamera();
            CPUTAssetLibraryDX11::GetAssetLibrary()->AddCamera( _L("SampleStart Camera"), mpCamera );

            mpCamera->SetPosition( 70.44f/factorfactor, 13.27f/factorfactor, -18.786f/factorfactor);
            // Set the projection matrix for all of the cameras to match our window.
            mpCamera->SetAspectRatio(((float)width)/((float)height));

            mpCamera->SetFov(XMConvertToRadians(60.0f));
            mpCamera->SetFarPlaneDistance(10000.0f);
            mpCamera->LookAt( 18.823f, 1.077f, -0.038f);
            mpCamera->Update();*/

            initCamera(0, new Vector3f( 70.44f/factorfactor, 13.27f/factorfactor, -18.786f/factorfactor),
                    new Vector3f(18.823f, 1.077f, -0.038f));
        }


        mpShadowCamera = new CPUTCamera();
        CPUTAssetLibraryDX11.GetAssetLibrary().AddCamera( "ShadowCamera", mpShadowCamera );

        /*mpCameraController = new CPUTCameraControllerFPS();
        mpCameraController->SetCamera(mpCamera);
        mpCameraController->SetLookSpeed(0.004f);
        mpCameraController->SetMoveSpeed(70.0f);*/

        // Initialize with the current surface description
        /*D3D11_RENDER_TARGET_VIEW_DESC BackBufferRTVDesc;
        mpBackBufferRTV->GetDesc(&BackBufferRTVDesc);

        ID3D11Resource* pBackBufferRTV=NULL;
        mpBackBufferRTV->GetResource(&pBackBufferRTV);

        // get the texture
        ID3D11Texture2D* pBackBufferTexture=NULL;
        pBackBufferRTV->QueryInterface(__uuidof(ID3D11Texture2D), (void**)&pBackBufferTexture);

        // get surface desc
        D3D11_TEXTURE2D_DESC surfDesc;
        pBackBufferTexture->GetDesc(&surfDesc);
        SAFE_RELEASE(pBackBufferTexture);
        SAFE_RELEASE(pBackBufferRTV);*/

        /*D3DXMatrixIdentity( &gWorldMatrix );*/
        gWorldMatrix.setIdentity();

        // Create a particle system for this specific scene
        CreateParticles(/*mpD3dDevice*/);

        // get the current gui state
        GetGUIState(mCurrentGUIState);

        /*SetAmbientColor( float3( 0.05f, 0.05f, 0.05f ) );
        SetLightColor( float3( 1.2f, 1.2f, 1.2f ) );*/
    }

    @Override
    public void onResize(int width, int height) {
        mSceneData.setProjection(60, (float)width/height, 60, 10000.0f);

        CPUTAssetLibrary pAssetLibrary = CPUTAssetLibrary.GetAssetLibrary();

        // Before we can resize the swap chain, we must release any references to it.
        // We could have a "AssetLibrary::ReleaseSwapChainResources(), or similar.  But,
        // Generic "release all" works, is simpler to implement/maintain, and is not performance critical.
        pAssetLibrary.ReleaseTexturesAndBuffers();

//        mpAVSMDebugView.ReleaseTexturesAndBuffers();  TODO

        // Resize the CPUT-provided render target
//        CPUT_DX11::ResizeWindow( width, height );

        // Resize any application-specific render targets here
//        if( mpCamera ) mpCamera->SetAspectRatio(((float)width)/((float)height));

        pAssetLibrary.RebindTexturesAndBuffers();

//        mpAVSMDebugView->Resize(width, height);  TODO

        // save the new screen width/height
        mScreenWidth = (float)width;
        mScreenHeight = (float)height;
    }

    // Create our very simple particle system.  Could use much more fancy system
    // with wind physics/etc - but this works for our demonstration purposes.
    //-----------------------------------------------------------------------------
    private void CreateParticles(/*ID3D11Device *d3dDevice*/){
        AVSMGuiState GUIState = new AVSMGuiState();
        String shadowAAStr;
        String avsmNodeCountStr;
        StringBuilder oss = new StringBuilder();

        // get the current GUI settings to make sure we have most updated
        // particle size/opacity values
        GetGUIState(GUIState);

        // convert to strings
        oss.append(GUIState.NumberOfNodes);
        shadowAAStr = oss.toString();
        oss.append(GUIState.ShadowTextureDimensions);
        avsmNodeCountStr = oss.toString();

        // far left low mushroom plume
        ParticleEmitter emitter0 = new ParticleEmitter();
        {
            emitter0.mDrag           = 0.1f;
            emitter0.mGravity        = -0.1f;
            emitter0.mRandScaleX     = 1.0f;
            emitter0.mRandScaleY     = 0.1f;
            emitter0.mRandScaleZ     = 1.0f;
            emitter0.mLifetime       = 15.0f;
            emitter0.mStartSize      = 3.0f;
            emitter0.mSizeRate       = 0.05f;
            emitter0.mpPos[0]        = 12.15f;
            emitter0.mpPos[1]        = -5.578f;
            emitter0.mpPos[2]        = -9.48f;
            emitter0.mpVelocity[0]   = 0.0f;
            emitter0.mpVelocity[1]   = 1.0f;
            emitter0.mpVelocity[2]   = 0.5f;
        }

        // tall front plume
        ParticleEmitter emitter1 = new ParticleEmitter();
        {
            emitter1.mDrag           = 0.2f;
            emitter1.mGravity        = 0.0f;
            emitter1.mRandScaleX     = 1.5f;
            emitter1.mRandScaleY     = 0.5f;
            emitter1.mRandScaleZ     = 1.5f;
            emitter1.mLifetime       = 18.0f;
            emitter1.mStartSize      = 4.0f;
            emitter1.mSizeRate       = 0.05f;
            emitter1.mpPos[0]        = 21.932f;
            emitter1.mpPos[1]        = -0.899f;
            emitter1.mpPos[2]        = 12.227f;
            emitter1.mpVelocity[0]   = -1.0f;
            emitter1.mpVelocity[1]   = 3.0f;
            emitter1.mpVelocity[2]   = 0.0f;
        }


        // horizontal steam jet
        ParticleEmitter emitter2 = new ParticleEmitter();
        {
            emitter2.mDrag           = 0.2f;
            emitter2.mGravity        = 0.5f;
            emitter2.mRandScaleX     = 0.7f;
            emitter2.mRandScaleY     = 0.7f;
            emitter2.mRandScaleZ     = 0.7f;
            emitter2.mLifetime       = 6.0f;
            emitter2.mStartSize      = 5.0f;
            emitter2.mSizeRate       = 0.05f;
            emitter2.mpPos[0]        = 27.6f; // front to back
            emitter2.mpPos[1]        = -1.5f; // height
            emitter2.mpPos[2]        = 5.4f;  // left/right
            emitter2.mpVelocity[0]   = -5.0f;
            emitter2.mpVelocity[1]   = 1.2f;
            emitter2.mpVelocity[2]   = -3.0f;
        }

        ParticleEmitter[] emitters = {emitter0, emitter1, emitter2};
        final int maxNumPartices = 5000;
        Macro shaderDefines[] = {
                new Macro("AVSM_NODE_COUNT", avsmNodeCountStr)
        };

        // create our simple particle system with 3 emitters
        gParticleSystem = new ParticleSystem(maxNumPartices, null);
        gParticleSystem.InitializeParticles(emitters, 3, /*d3dDevice,*/ 8, 8, shaderDefines);
    }

    private void GetGUIState(AVSMGuiState pState){}
    private void SetAVSMFrameContextData(AVSMTechnique.FrameContext AVSMFrameContext){
        // update render method:
        switch( mCurrentGUIState.Method )
        {
            case AVSM_DISABLED:          AVSMFrameContext.GPUUIConstants.volumeShadowMethod = AVSMTechnique.VOL_SHADOW_NO_SHADOW;  break;
            case AVSM_METHOD_DIRECTX:    AVSMFrameContext.GPUUIConstants.volumeShadowMethod = AVSMTechnique.VOL_SHADOW_AVSM;       break;
            case AVSM_METHOD_INTEL_EXT:  AVSMFrameContext.GPUUIConstants.volumeShadowMethod = AVSMTechnique.VOL_SHADOW_AVSM_GEN;   break;
        }

        AVSMFrameContext.GPUUIConstants.enableStats = mCurrentGUIState.EnableStats?1:0;
        AVSMFrameContext.GPUUIConstants.pauseParticleAnimaton = mCurrentGUIState.ParticlesPaused?1:0;
        AVSMFrameContext.GPUUIConstants.particleSize = mCurrentGUIState.ParticleSize;
        AVSMFrameContext.GPUUIConstants.particleOpacity = (int)mCurrentGUIState.ParticleOpacity;
        AVSMFrameContext.GPUUIConstants.wireframe = mCurrentGUIState.WireFrame?1:0;
        AVSMFrameContext.GPUUIConstants.vertexShaderShadowLookup = mCurrentGUIState.vertexShaderShadowLookup?1:0;
        AVSMFrameContext.GPUUIConstants.tessellate = mCurrentGUIState.tessellate?1:0;
        AVSMFrameContext.GPUUIConstants.TessellationDensity = 1.0f/(31.0f - mCurrentGUIState.TessellationDensity);

        // set the current size
        AVSMFrameContext.mScreenWidth = mScreenWidth;
        AVSMFrameContext.mScreenHeight = mScreenHeight;
    }

    private void initGUI(){
        /*CPUTGuiControllerDX11 *pGUI = CPUTGetGuiController();
        pGUI->CreateButton(_L("Fullscreen"), ID_FULLSCREEN_BUTTON, ID_MAIN_PANEL, &mpFullScreen);

        pGUI->CreateText(_L(" "), ID_IGNORE_CONTROL_ID, ID_MAIN_PANEL);
        pGUI->CreateText(_L("Volume Shadowing Method"), ID_IGNORE_CONTROL_ID, ID_MAIN_PANEL);
        pGUI->CreateDropdown(_L("Disabled"), ID_VOLUME_SHADOWING_METHOD, ID_MAIN_PANEL, &mpVolumeShadowingMethod);
        mpVolumeShadowingMethod->AddSelectionItem(_L("DirectX11 Method"), true);
        if(true == mAVSMExtensionAvailable)
        {
            mpVolumeShadowingMethod->AddSelectionItem(_L("Pixel Shader Ordering"), true);
        }

        pGUI->CreateText(_L(" "), ID_IGNORE_CONTROL_ID, ID_MAIN_PANEL);
        pGUI->CreateText(_L("Shadow Texture Dimension"), ID_IGNORE_CONTROL_ID, ID_MAIN_PANEL);
        pGUI->CreateDropdown(_L("512x512"), ID_SHADOW_TEXTURE_DIMENSION, ID_MAIN_PANEL, &mpShadowTextureDimension);
        mpShadowTextureDimension->AddSelectionItem(_L("256x256"), true);
        mpShadowTextureDimension->AddSelectionItem(_L("128x128"));

        pGUI->CreateText(_L("AVSM Node Count"), ID_IGNORE_CONTROL_ID, ID_MAIN_PANEL);
        pGUI->CreateDropdown(_L("8 nodes per sample"), ID_AVSM_NODE_COUNT, ID_MAIN_PANEL, &mpAVSMNodeCount);
        mpAVSMNodeCount->AddSelectionItem(_L("4 nodes per sample"));

        pGUI->CreateText(_L("\t "), ID_IGNORE_CONTROL_ID, ID_MAIN_PANEL);
        pGUI->CreateText(_L("\t "), ID_IGNORE_CONTROL_ID, ID_MAIN_PANEL);
        pGUI->CreateSlider(_L("Particle Size"), ID_PARTICLE_SIZE, ID_MAIN_PANEL, &mpParticleSize);
        mpParticleSize->SetScale(5, 50, 20);
        mpParticleSize->SetValue(25);

        pGUI->CreateText(_L("Pixel shader ordering feature not supported on this system.  Option disabled."), ID_IGNORE_CONTROL_ID, ID_MAIN_PANEL, &mpExtensionWarning);
        mpExtensionWarning->SetAutoArranged(false);
        int x,y, Winwidth,Winheight;
        CPUTOSServices::GetOSServices()->GetClientDimensions(&x,&y, &Winwidth, &Winheight);
        mpExtensionWarning->GetDimensions(x,y);
        mpExtensionWarning->SetPosition(0,Winheight-y-2);
        CPUTColor4 color;
        color.r=1.0f; color.g=0.0f; color.b=0.0f; color.a=1.0f;
        mpExtensionWarning->SetColor(color);
        if(mAVSMExtensionAvailable)
        {
            mpExtensionWarning->SetVisibility(false);
        }

        pGUI->CreateSlider(_L("Particle Opacity"), ID_PARTICLE_OPACITY, ID_MAIN_PANEL, &mpParticleOpacity);
        mpParticleOpacity->SetScale(5, 100, 20);
        mpParticleOpacity->SetValue(50);

        pGUI->CreateCheckbox(_L("Pause Particles"), ID_PAUSE_PARTICLES, ID_MAIN_PANEL, &mpPauseParticles);
*//*#ifdef AVSM_METRICS
        pGUI->CreateCheckbox(_L("Enable Stats"), ID_ENABLE_STATS, ID_MAIN_PANEL, &mpEnableStats);
#endif*//*
        // Extended Vertex shader functionality
        pGUI->CreateCheckbox(_L("WireFrame Particles"), ID_WIREFRAME_PARTICLES, ID_MAIN_PANEL, &mpWireFrameParticles);
        pGUI->CreateCheckbox(_L("Tessellated VS-based particle AVSM"), ID_VERTEX_SHADE_PARTICLES, ID_MAIN_PANEL, &mpVertexShadeParticles);
        mpVertexShadeParticles->SetCheckboxState( CPUT_CHECKBOX_CHECKED );

        pGUI->CreateSlider(_L("Particle Tessellation"), ID_PARTICLETESSELLATION, ID_MAIN_PANEL, &mpParticleTessellation);
        mpParticleTessellation->SetScale(1, 30, 31);
        mpParticleTessellation->SetValue(5);

        pGUI->CreateSlider(_L("Light longitude"), ID_LIGHT_ORBIT_LONG_SLIDER, ID_MAIN_PANEL, &mpLightLongitudeSlider);
        mpLightLongitudeSlider->SetScale( -180.0f, 180.0f, 361 );
        mpLightLongitudeSlider->SetValue( 29.0f );
        pGUI->CreateSlider(_L("Light latitude"), ID_LIGHT_ORBIT_LAT_SLIDER, ID_MAIN_PANEL, &mpLightLatitudeSlider);
        mpLightLatitudeSlider->SetScale( 10.0f, 90.0f, 81 );
        mpLightLatitudeSlider->SetValue( 48.0f );

        pGUI->CreateCheckbox(_L("Show debug view"), ID_DEBUG_DISPLAY, ID_MAIN_PANEL, &mpDebugViewShow);

        // Create help text panel
        pGUI->CreateText( _L("F1 for Help"), ID_IGNORE_CONTROL_ID, ID_SECONDARY_PANEL);
        pGUI->CreateText( _L("[Escape] to quit application"), ID_IGNORE_CONTROL_ID, ID_SECONDARY_PANEL);
        pGUI->CreateText( _L("A,S,D,F - move camera position"), ID_IGNORE_CONTROL_ID, ID_SECONDARY_PANEL);
        pGUI->CreateText( _L("Q - camera position up"), ID_IGNORE_CONTROL_ID, ID_SECONDARY_PANEL);
        pGUI->CreateText( _L("E - camera position down"), ID_IGNORE_CONTROL_ID, ID_SECONDARY_PANEL);
        pGUI->CreateText( _L("mouse + right click - camera look location"), ID_IGNORE_CONTROL_ID, ID_SECONDARY_PANEL);
*//*#ifdef AVSM_METRICS
        pGUI->CreateText( _L("Volumetric shadow map render time (ms):"), ID_SMRT, ID_MAIN_PANEL,&mpSMRT);
        mpSMRT->SetAutoArranged(false);
        mpSMRT->SetPosition(5,50);

        mpSMRT->SetVisibility(CPUT_CHECKBOX_CHECKED == mpEnableStats->GetCheckboxState());

        pGUI->CreateText( _L("Solid geometry render time (ms):"), ID_WST, ID_MAIN_PANEL,&mpWST);
        mpWST->SetAutoArranged(false);
        mpWST->SetPosition(5,65);
        mpWST->SetVisibility(CPUT_CHECKBOX_CHECKED == mpEnableStats->GetCheckboxState());

        pGUI->CreateText( _L("Particles (...) render time (ms): "), ID_PST, ID_MAIN_PANEL,&mpPST);
        mpPST->SetAutoArranged(false);
        mpPST->SetPosition(5,80);
        mpWST->SetVisibility(CPUT_CHECKBOX_CHECKED == mpEnableStats->GetCheckboxState());
#endif*//*

        pGUI->SetActivePanel(ID_MAIN_PANEL);
        pGUI->DrawFPS(true);*/
    }

    @Override
    protected void update(float dt) {
        /*if( mpCameraController != NULL )
        {
            mpCameraController->Update((float)deltaSeconds);
        }*/

        // Set up the shadow camera (a camera that sees what the light sees)
        {
            Vector3f lookAtPoint = new Vector3f(0.0f, 0.0f, 0.0f);
            Vector3f half = new Vector3f(1.0f, 1.0f, 1.0f);
            if( mpAssetSet !=null)
            {
                Vector3f center = new Vector3f();
                mpAssetSet.GetBoundingBox( center, half );
            }
            float length = half.length();

            mpShadowCamera.SetFov(/*XMConvertToRadians*/(45));
            mpShadowCamera.SetAspectRatio(1.0f);
            float fov = mpShadowCamera.GetFov();
            float tanHalfFov = (float) Math.tan(Math.toRadians(fov) * 0.5f);
            float cameraDistance = length/tanHalfFov;
            float nearDistance = cameraDistance * 0.1f;
            mpShadowCamera.SetNearPlaneDistance(nearDistance);
            mpShadowCamera.SetFarPlaneDistance(1000.0f * cameraDistance);

            float shadowCameraDistance = 140.0f;
            Vector3f shadowCameraPosition = new Vector3f(); // = float3( 85.0f, 38.0f, 42.0f);

            // always update these guys so we can track at realtime
            /*mpLightLatitudeSlider.GetValue( mCurrentGUIState.LightLatitude );
            mpLightLongitudeSlider.GetValue( mCurrentGUIState.LightLongitude );*/

            float longRad = mCurrentGUIState.LightLongitude / 180.0f * 3.14159265f;
            float latRad = mCurrentGUIState.LightLatitude / 180.0f * 3.14159265f;

            // Convert from long/lat to 3D spherical coordinates
            shadowCameraPosition.x = (float) (Math.cos( longRad ) * Math.cos( latRad ));
            shadowCameraPosition.y = (float) Math.sin( latRad ); // y is up
            shadowCameraPosition.z = (float) (Math.sin( longRad ) * Math.cos( latRad ));

            shadowCameraPosition.scale(shadowCameraDistance);

            mpShadowCamera.SetPosition( shadowCameraPosition );
            mpShadowCamera.LookAt( lookAtPoint.x, lookAtPoint.y, lookAtPoint.z );
            mpShadowCamera.Update(dt);
        }
    }

    private final CPUTRenderParameters m_renderParams = new CPUTRenderParameters();
    private final AVSMTechnique.FrameContext m_AVSMFrameContext = new AVSMTechnique.FrameContext();

    @Override
    protected void onRender(boolean clearFBO) {
        /*CPUTGPUProfilerDX11_AutoScopeProfile timerAll( mGPUTimerAll, mCurrentGUIState.EnableStats );
        CPUTRenderParametersDX renderParams(mpContext);*/
        CPUTRenderParameters renderParams = m_renderParams;

        if( mpAVSMTechnique == null )
        {
            /*const float clearColor[] = { 1.0f, 0.0f, 0.0f, 1.0f };
            mpContext->ClearRenderTargetView( mpBackBufferRTV,  clearColor );*/
            gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
            gl.glClearColor(1.0f, 0.0f, 0.0f, 1.0f);
            gl.glClear(GLenum.GL_COLOR_BUFFER_BIT);
            return;
        }


        // Set the viewport back to full window size
        /*D3D11_VIEWPORT viewport;
        viewport.Width    = (float)mScreenWidth;
        viewport.Height   = (float)mScreenHeight;
        viewport.MinDepth = 0.0f;
        viewport.MaxDepth = 1.0f;
        viewport.TopLeftX = 0.0f;
        viewport.TopLeftY = 0.0f;*/
        Vector4i viewport = new Vector4i(0,0, (int)mScreenWidth, (int)mScreenHeight);

        // 1. Initialize AVSM structures and update particle movement (but don't do any actual rendering yet)

        // Tell the materials the number of nodes to be used in the technique
        if( mCurrentGUIState.NumberOfNodes == 4 ) {
            CPUTMaterialDX11.GlobalSetCurrentSubMaterial (0);
        }else if( mCurrentGUIState.NumberOfNodes == 8 ) {
            CPUTMaterialDX11.GlobalSetCurrentSubMaterial (1);
        }else
        {
            throw new RuntimeException("Only 4 or 8 supported for mCurrentGUIState.NumberOfNodes");
        }

        // initialize the frame context
        AVSMTechnique.FrameContext AVSMFrameContext =m_AVSMFrameContext;
        mpAVSMTechnique.InitializeFrameContext( AVSMFrameContext, mAppOptions, /*mpContext,*/ gParticleSystem, gWorldMatrix,
                mpCamera, mpShadowCamera, viewport );

//        CPUTBufferDX11 * pMainDepthBuffer = (CPUTBufferDX11 *)CPUTAssetLibrary::GetAssetLibrary()->GetBuffer( L"$DepthBuffer" );
//        if( pMainDepthBuffer != NULL )  todo why ????
//        {
//            AVSMFrameContext.DepthBufferSRV = pMainDepthBuffer->GetShaderResourceView();
//            pMainDepthBuffer->Release();
//        }

        // translate the GUI state to AVSMFrameContext parameters
        SetAVSMFrameContextData(AVSMFrameContext);

        // update the particle positions (but do not render)
        mpAVSMTechnique.UpdateParticles( AVSMFrameContext );

        // set up the number of nodes
        mpAVSMTechnique.SetNodeCount(mCurrentGUIState.NumberOfNodes);

        // 2. Draw the shadowed scene using a standard shadow map from the light's point of view
        // one could also use cascades shadow maps or other shadowing techniques.  We choose simple
        // shadow mapping for simplicity/demonstration purposes
        CPUTCamera pLastCamera = mpCamera;
        /*mpCamera =*/ renderParams.mpCamera = mpShadowCamera;
//        mpShadowRenderTarget.SetRenderTarget( renderParams, 0, 1.0f, true ); TODO setup the shadow mapping

        if( mpAssetSet!= null ) { mpAssetSet.RenderShadowRecursive( renderParams ); }

        /*mpShadowRenderTarget->RestoreRenderTarget(renderParams);*/
        /*mpCamera =*/ renderParams.mpCamera = pLastCamera;
        renderParams.mpShadowCamera = mpShadowCamera;

        // Clear render target and depth buffer
        /*const float clearColor[] = { 0.0993f, 0.0993f, 0.1993f, 1.0f };
        mpContext->ClearRenderTargetView( mpBackBufferRTV,  clearColor );
        mpContext->ClearDepthStencilView( mpDepthStencilView, D3D11_CLEAR_DEPTH | D3D11_CLEAR_STENCIL, 1.0f, 0);

        // Draw scene
        {
            // Save our main RTV/DSV
            ID3D11RenderTargetView* pRTV = CPUTRenderTargetColor::GetActiveRenderTargetView();
            ID3D11DepthStencilView* pDSV = CPUTRenderTargetDepth::GetActiveDepthStencilView();

            // 3. create the AVSM shadow map and capture particle fragments into the AVSM render targets
            {
                CPUTGPUProfilerDX11_AutoScopeProfile timer( mGPUTimerAVSMCreation, mCurrentGUIState.EnableStats );
                mpAVSMTechnique->CreateAVSMShadowMap( AVSMFrameContext, mpAssetSet, &renderParams );
            }

            // 4. set the standard render target and draw the regular scene geometry
            {
                CPUTGPUProfilerDX11_AutoScopeProfile timer( mGPUTimerSolidGeometryRendering, mCurrentGUIState.EnableStats );

                // draw CPUT loaded geometry/scene
                // set the render targets and viewports
                mpContext->OMSetRenderTargets( 1, &pRTV, pDSV );
                mpContext->RSSetViewports(1, &viewport);

                // set all 'external' states for drawing regular scene geometry
                mpAVSMTechnique->UpdateStatesAndConstantsForAVSMUse( AVSMFrameContext, true );

                // render scene geometry - passing in the AVSM buffers
                renderParams.mAVSMMethod = mCurrentGUIState.Method;

                // render the skybox - which does not accept AVSM shadows
                if( mpSkyBox ) { mpSkyBox->RenderRecursive(renderParams); }

                // render the scene - accepting AVSM shadows
                if( mpAssetSet ) { mpAssetSet->RenderAVSMShadowedRecursive(renderParams); }
            }

            // 5. draw screen space ambient occlusion
*//*#ifdef SSAO_ENABLE
            {
                mSSAO.UpdateConsts( mpContext, mpCamera );
                mSSAO.GenerateSSAO( mpContext );
                mSSAO.FullscreenApplySSAO( mpContext );
            }
#endif*//*

            // 6. Render the shaded particles into the standard render target
            {
                CPUTGPUProfilerDX11_AutoScopeProfile timer( mGPUTimerParticlesRendering, mCurrentGUIState.EnableStats );

                // remove depth buffer since we're doing soft depth blend in the pixel shader
                mpContext->OMSetRenderTargets( 1, &pRTV, NULL );

                // set all 'external' states for drawing 'external' geometry (particles)
                mpAVSMTechnique->UpdateStatesAndConstantsForAVSMUse( AVSMFrameContext, false );

                // render the shadowed/lit particles
                mpAVSMTechnique->RenderShadedParticles( AVSMFrameContext, pRTV, pDSV );
            }

            // restore our main RTV/DSV
            mpContext->OMSetRenderTargets( 1, &pRTV, pDSV );
        }

        // Draw the debug view
        if( CPUT_CHECKBOX_CHECKED == mpDebugViewShow->GetCheckboxState() )
        {
            mpAVSMDebugView->Render(mpContext, mpAVSMTechnique, &AVSMFrameContext, &renderParams);
        }

        // update stats
        if( mCurrentGUIState.EnableStats )
        {
            AVSMFrameContext.Stats.VolumetricShadowTime = (float)(mGPUTimerAVSMCreation.GetAvgTime() * 1000.0);
            AVSMFrameContext.Stats.SolidGeometryTime    = (float)(mGPUTimerSolidGeometryRendering.GetAvgTime()* 1000.0);
            AVSMFrameContext.Stats.ParticlesTime        = (float)(mGPUTimerParticlesRendering.GetAvgTime()* 1000.0);
            AVSMFrameContext.Stats.TotalTime            = (float)(mGPUTimerAll.GetAvgTime() * 1000.0);
        }
        else
        {
            memset( &AVSMFrameContext.Stats, 0, sizeof(AVSMFrameContext.Stats) );
        }*/
    }

    @Override
    protected void onDestroy() {
// destroy the particle system
//        SAFE_DELETE(gParticleSystem);

        // destroy the AVSMTechnique
//        SAFE_DELETE(mpAVSMTechnique);

        // destroy the debug view
//        SAFE_RELEASE(mpAVSMDebugView);
    }
}
