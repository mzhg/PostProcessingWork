package jet.opengl.demos.intel.clustered;

import com.nvidia.developer.opengl.app.NvSampleApp;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import jet.opengl.demos.intel.coarse.PointLight;
import jet.opengl.demos.intel.coarse.PointLightInitTransform;
import jet.opengl.demos.intel.cput.CPUTAssetLibrary;
import jet.opengl.demos.intel.cput.CPUTAssetSet;
import jet.opengl.demos.intel.cput.CPUTBuffer;
import jet.opengl.demos.intel.cput.CPUTBufferDX11;
import jet.opengl.demos.intel.cput.CPUTCamera;
import jet.opengl.demos.intel.cput.CPUTMaterial;
import jet.opengl.demos.intel.cput.CPUTRenderParameters;
import jet.opengl.demos.intel.cput.CPUTScene;
import jet.opengl.demos.intel.cput.CPUTSprite;
import jet.opengl.demos.intel.cput.CPUTTextureDX11;
import jet.opengl.demos.intel.cput.CPUT_PROJECTION_MODE;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.FramebufferGL;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.TextureAttachDesc;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.Numeric;
import jet.opengl.postprocessing.util.StackFloat;
import jet.opengl.postprocessing.util.StackInt;

public class ClusteredShadingSample extends NvSampleApp implements ICONST{
    private static final int SHADOW_WIDTH_HEIGHT = 512;
    private float                 mfElapsedTime;
    private CPUTAssetSet          mpAssetSet;
//    CPUTCameraController  *mpCameraController;
    private CPUTSprite            mpDebugSprite;

    private CPUTAssetSet          mpShadowCameraSet;
    private FramebufferGL         mpShadowRenderTarget;

    private CPUTScene             mpScene;
    private final List<CPUTMaterial>    mpMaterials = new ArrayList<>();
//    CPUTDropdown          *mpShadingTechDropdown;
//    CPUTCheckbox          *mpAnimateLightsCheckbox;

    private GLSLProgram m_DeferredNoCullProgram;
    private GLSLProgram m_GPUQuadProgram;
    private GLSLProgram m_TiledDeferredCSProgram;
    private GLSLProgram m_CopyTextureProgram;

    private static final int
    MATERIAL_FORWARD = 0,
    MATERIAL_CLUSTERED = 1,
    MATERIAL_GBUFFER = 2;

    private int mActiveLights;
    private PointLightInitTransform[] mLightInitialTransform;
    private PointLight[] mPointLightParameters;
    private final StackFloat mLightsInfoTextureData = new StackFloat();
    private final StackInt mLightGridTextureData = new StackInt();
    private Vector3f[] mPointLightPositionWorld;

    private LightGridBuilder mLightGridBuilder;

    private CPUTTextureDX11 mpPointLightsInfoTexture;
    private CPUTTextureDX11  mpLightGridTexture;
    private final UIConstants mUIConstants = new UIConstants();
    private CPUTBufferDX11 mpUIConstantsBuffer;
    private Texture2D mpGBufferDiffuseColor;
    private Texture2D  mpGBufferNormal;
    private Texture2D  mpGBufferLightMap;
    private Texture2D  mpGBufferDepth;
    private Texture2D  mpShadedBackBuffer;

    private int          m_GBufferFBO;
    private int          m_DummyVAO;
    private int			mBackBufferWidth, mBackBufferHeight;
    private float mTotalTime;

    private CPUTCamera mpCamera;
    private CPUTCamera mpShadowCamera;
    private GLFuncProvider gl;
    private CPUTBufferDX11 mpPerFrameConstantBuffer;
    private CPUTBufferDX11 mpPerModelConstantBuffer;

    @Override
    protected void initRendering() {
        gl = GLFuncProviderFactory.getGLFuncProvider();
        CPUTAssetLibrary pAssetLibrary = CPUTAssetLibrary.GetAssetLibrary();

        mUIConstants.lightingOnly = 0;
        mUIConstants.faceNormals = 0;
        mUIConstants.visualizeLightCount = 0;
        mUIConstants.visualizePerSampleShading =0 ;

        mUIConstants.lightCullTechnique = CULL_CLUSTERED;
        mUIConstants.clusteredGridScale = 16;
        mUIConstants.Dummy0 = 0;
        mUIConstants.Dummy1 = 0;

        {
            String name = "$ui_constants";

            BufferGL UIConstantsBuffer = new BufferGL();
            UIConstantsBuffer.initlize(GLenum.GL_UNIFORM_BUFFER, 32+Vector4f.SIZE*2, null, GLenum.GL_DYNAMIC_DRAW);
            mpUIConstantsBuffer = new CPUTBufferDX11(name, /*GL_UNIFORM_BUFFER, GL_DYNAMIC_DRAW, 32 + sizeof(mUIConstants), nullptr*/UIConstantsBuffer );
            pAssetLibrary.AddConstantBuffer(name, mpUIConstantsBuffer);
        }

        String mediaDirectory = "";

//#ifdef CPUT_OS_WINDOWS
//        cString executableDirectory;
//        CPUTFileSystem::GetExecutableDirectory(&executableDirectory);
//        CPUTFileSystem::ResolveAbsolutePathAndFilename(executableDirectory + _L("../../../Media/"), &mediaDirectory);
//#endif

/*#ifdef CPUT_OS_ANDROID
        android_app *pState = CPUTWindowAndroid::GetAppState();
        cString guiDirectory;
        cString systemDirectory;
#   ifdef CPUT_USE_ANDROID_ASSET_MANAGER
        CPUTFileSystem::GetExecutableDirectory(&mediaDirectory);
#   else
        ANativeActivity* nativeActivity = pState->activity;
        const char* externalDataPath = nativeActivity->externalDataPath;
        mediaDirectory.assign(externalDataPath);
        mediaDirectory.append("/Media/");
#   endif
#endif*/

        pAssetLibrary.SetMediaDirectoryName(mediaDirectory);
        pAssetLibrary.SetSystemDirectoryName(mediaDirectory + "System/");

//        CPUTGuiControllerOGL *pGUI = (CPUTGuiControllerOGL*)CPUTGetGuiController();
        pAssetLibrary.SetAssetSetDirectoryName(mediaDirectory + "gui_assets/");
//        pGUI->Initialize(mediaDirectory);
//        pGUI->SetCallback(this);
//        pGUI->SetWindow(mpWindow);

        /*CPUTFont *pFont = CPUTFont::CreateFont(mediaDirectory + _L("System/Font/"), _L("arial_64.fnt"));

#ifdef CPUT_OS_WINDOWS
        pAssetLibrary->AddFont(_L("DefaultFont"), _L(""), _L(""), pFont);
#endif
#ifdef CPUT_OS_ANDROID
        pAssetLibrary->GetFontByName(_L("arial_64.fnt"));
#endif
        pGUI->SetFont(pFont);*/

        //
        // Create some controls
        //
        int iNumLightsPow = 7;
        int iNumLights = 1<<iNumLightsPow;
        /*pGUI->CreateDropdown(_L("Technique: forward"), ID_SHADING_TECH_DROPDOWN, ID_MAIN_PANEL, &mpShadingTechDropdown);
        mpShadingTechDropdown->AddSelectionItem(_L("Technique: deferred") );
        mpShadingTechDropdown->AddSelectionItem(_L("Technique: quad") );
        mpShadingTechDropdown->AddSelectionItem(_L("Technique: clustered") );
        mpShadingTechDropdown->AddSelectionItem(_L("Technique: CS tile") );
        mpShadingTechDropdown->SetSelectedItem(mUIConstants.lightCullTechnique);

        CPUTSlider *pNumLightSlider = nullptr;
#if defined(CPUT_OS_ANDROID)
        std::stringstream ssNumLights;
#elif defined(CPUT_OS_WINDOWS)
                std::wstringstream ssNumLights;
#endif
        ssNumLights << _L("Num lights: ") << iNumLights;
        pGUI->CreateSlider(ssNumLights.str(), ID_NUM_LIGHTS_SLIDER, ID_MAIN_PANEL, &pNumLightSlider, 2.0f);
        pNumLightSlider->SetScale(0, 10, 11);
        pNumLightSlider->SetValue((float)iNumLightsPow);

        pGUI->CreateCheckbox(_L("Animate Lights"), ID_ANIMATE_LIGHTS_CHECKBOX, ID_MAIN_PANEL, &mpAnimateLightsCheckbox, 2.0f);
        mpAnimateLightsCheckbox->SetCheckboxState(CPUT_CHECKBOX_CHECKED);

        CPUTCheckbox *pVisualizeLightCountCheckbox;
        pGUI->CreateCheckbox(_L("Visualize light count"), ID_VISUALIZE_LIGHT_COUNT_CHECKBOX, ID_MAIN_PANEL, &pVisualizeLightCountCheckbox, 2.0f);
        pVisualizeLightCountCheckbox->SetCheckboxState(mUIConstants.visualizeLightCount ? CPUT_CHECKBOX_CHECKED : CPUT_CHECKBOX_UNCHECKED);*/

        InitializeLightParameters();
        SetActiveLights(iNumLights);
        // initialize Cluster Size parameters
        mLightGridBuilder.reset(32, 16, 128);

        /*int width, height;
        mpWindow->GetClientDimensions(&width, &height);*/

//        CPUTTextureOGL*  pDepthTexture = (CPUTTextureOGL*)CPUTTextureOGL::CreateTexture(_L("$shadow_depth"), GL_DEPTH_COMPONENT, SHADOW_WIDTH_HEIGHT, SHADOW_WIDTH_HEIGHT, GL_DEPTH_COMPONENT, GL_UNSIGNED_INT, NULL);
        CPUTTextureDX11 pDepthTexture = (CPUTTextureDX11)CPUTTextureDX11.CreateTexture("$shadow_depth", GLenum.GL_DEPTH_COMPONENT32F, SHADOW_WIDTH_HEIGHT, SHADOW_WIDTH_HEIGHT, GLenum.GL_DEPTH_COMPONENT, 0,0);
        mpShadowRenderTarget = new FramebufferGL();
        mpShadowRenderTarget.bind();
        mpShadowRenderTarget.addTexture(pDepthTexture.GetTexture(), new TextureAttachDesc());
//        SAFE_RELEASE(pDepthTexture);

        mpLightGridTexture = (CPUTTextureDX11)CPUTTextureDX11.CreateTexture("$light_grid", GLenum.GL_RGBA32UI, LIGHT_GRID_TEXTURE_WIDTH, LIGHT_GRID_TEXTURE_HEIGHT, 0, 0, 1);
        mLightGridTextureData.resize(LIGHT_GRID_TEXTURE_WIDTH * LIGHT_GRID_TEXTURE_HEIGHT * 4);

        // Call ResizeWindow() because it creates some resources that our blur material needs (e.g., the back buffer)
//        ResizeWindow(width, height);

        mpScene = new CPUTScene();

        final String ASSET_LOCATION = "";
        final String ASSET_SET_FILE = "";
        pAssetLibrary.SetAssetSetDirectoryName( mediaDirectory + ASSET_LOCATION );
        CPUTAssetSet pAssetSet = null;
        try {
            pAssetSet = pAssetLibrary.GetAssetSet( ASSET_SET_FILE );
        } catch (IOException e) {
            e.printStackTrace();
        }
        mpScene.AddAssetSet(pAssetSet);
//        pAssetSet->Release();


        final String MaterialNames[] =
        {
                ("concreteroof.mtl"),
                ("concretewallsturtle.mtl"),
                ("levelgridframes.mtl"),
                ("levelmetal.mtl"),
                ("metalgrunge.mtl"),
                ("roofpipes.mtl"),
                ("roofvents1.mtl"),
                ("tilefloorturtle.mtl"),
                ("tilewallsturtle.mtl"),
                ("ventpipes1.mtl"),
                ("vents.mtl"),
                ("windows.mtl")
        };
        for( int Mtrl = 0; Mtrl < MaterialNames.length; ++Mtrl)
        {
            try {
                mpMaterials.add( pAssetLibrary.GetMaterial( MaterialNames[Mtrl], false ) );
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Get the camera. Get the first camera encountered in the scene or
        // if there are none, create a new one.
        int numAssets = mpScene.GetNumAssetSets();
        for ( int i = 0; i < numAssets; ++i) {
            pAssetSet = mpScene.GetAssetSet(i);
            if (pAssetSet.GetCameraCount() > 0) {
                mpCamera = pAssetSet.GetFirstCamera();
                break;
            }
        }

        if (mpCamera == null) {
            mpCamera = new CPUTCamera();
            pAssetLibrary.AddCamera( "Default Camera",  mpCamera );

            mpCamera.SetPosition( 0.0f, 10.0f, 0.0f );
            // Set the projection matrix for all of the cameras to match our window.
            // Note: this should really be a viewport matrix.  Otherwise, all cameras will have the same FOV and aspect ratio, etc instead of just viewport dimensions.
            mpCamera.SetAspectRatio(((float)1280)/((float)720));
        }
        mpCamera.SetFov((90.0f));
        mpCamera.SetFarPlaneDistance(100.0f);
        mpCamera.SetNearPlaneDistance(0.1f);
        mpCamera.Update();

        // Position and orient the shadow camera so that it sees the whole scene.
        // Set the near and far planes so that the frustum contains the whole scene.
        // Note that if we are allowed to move the shadow camera or the scene, this may no longer be true.
        // Consider updating the shadow camera when it (or the scene) moves.
        // Treat bounding box half as radius for an approximate bounding sphere.
        // The tightest-fitting sphere might be smaller, but this is close enough for placing our shadow camera.
        Vector3f sceneCenterPoint = new Vector3f(), halfVector = new Vector3f();
        mpScene.GetBoundingBox(sceneCenterPoint, halfVector);
        float  length = halfVector.length();
        mpShadowCamera = new CPUTCamera(CPUT_PROJECTION_MODE.CPUT_ORTHOGRAPHIC );
        mpShadowCamera.SetAspectRatio(1.0f);
        mpShadowCamera.SetNearPlaneDistance(1.0f);
        mpShadowCamera.SetFarPlaneDistance(2.0f*length + 1.0f);
        mpShadowCamera.SetPosition( /*sceneCenterPoint - float3(0, -1, 1) * length*/ sceneCenterPoint.x, sceneCenterPoint.y + length, sceneCenterPoint.z - length);
        mpShadowCamera.LookAt( sceneCenterPoint.x,  sceneCenterPoint.y, sceneCenterPoint.z);
        mpShadowCamera.SetWidth( length*2);
        mpShadowCamera.SetHeight(length*2);
        mpShadowCamera.Update();

        pAssetLibrary.AddCamera( "ShadowCamera", mpShadowCamera );
//        mpCameraController = new CPUTCameraControllerFPS();
//        mpCameraController->SetCamera(mpCamera);
//        mpCameraController->SetLookSpeed(0.004f);
//        mpCameraController->SetMoveSpeed(20.0f);

        try {
            mpDebugSprite = CPUTSprite.CreateSprite( -1.0f, -1.0f, 0.5f, 0.5f, ("%spriteOGL") );
        } catch (IOException e) {
            e.printStackTrace();
        }


        String ShaderDirectory = mediaDirectory + ("building/Shader/");
        /*std::vector< cString > Includes;
        Includes.push_back( _L("PerFrameConstants.h") );
        Includes.push_back( _L("ShaderDefines.h") );
        Includes.push_back( _L("Rendering.h") );
        Includes.push_back( _L("GBuffer.h") );
        Includes.push_back( _L("FramebufferFlat.h") );*/
        m_DeferredNoCullProgram //.CreateProgram(ShaderDirectory, Includes, _L("FullScreenTriangleVS.glsl"), _L("BasicLoopFS.glsl") );
                    = GLSLProgram.createProgram(ShaderDirectory + "FullScreenTriangleVS.glsl", ShaderDirectory + "BasicLoopFS.glsl",null);

//        Includes.push_back( _L("GPUQuad.h") );
        m_GPUQuadProgram //.CreateProgram(ShaderDirectory, Includes, _L("GPUQuadVS.glsl"), _L("GPUQuadFS.glsl") );
                    = GLSLProgram.createProgram(ShaderDirectory + "GPUQuadVS.glsl", ShaderDirectory+"GPUQuadFS.glsl", null);
        m_TiledDeferredCSProgram//.CreateProgram(ShaderDirectory, Includes, _L(""), _L(""), _L("TiledDeferredCS.glsl"));
                = GLSLProgram.createProgram(ShaderDirectory + "TiledDeferredCS.glsl", null);
        m_CopyTextureProgram //.CreateProgram(ShaderDirectory, std::vector< cString >(), _L("FullScreenTriangleVS.glsl"), _L("CopyTextureFS.glsl"));
                = GLSLProgram.createProgram(ShaderDirectory + "FullScreenTriangleVS.glsl", ShaderDirectory+"CopyTextureFS.glsl", null);
    }

    @Override
    public void display() {
        ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(UIConstants.SIZE);
        mUIConstants.store(buffer).flip();

//        mpUIConstantsBuffer->SetSubData(32, sizeof(mUIConstants), &mUIConstants);
        mpUIConstantsBuffer.GetNativeBuffer().update(32, buffer);

        UpdateLights( mpCamera.GetViewMatrix() );

        CPUTRenderParameters renderParams = new CPUTRenderParameters();
        renderParams.mpPerFrameConstants = mpPerFrameConstantBuffer;
        renderParams.mpPerModelConstants = mpPerModelConstantBuffer;
        renderParams.mpCamera = mpCamera;
//        UpdatePerFrameConstantBuffer(renderParams, deltaSeconds);  todo
        /*int x, y, w, h;
        mpWindow->GetClientDimensions(&x, &y, &w, &h);
        renderParams.mWidth = w;
        renderParams.mHeight = h;
        GL_CHECK(glViewport(0, 0, w, h ));
        GL_CHECK(glClearColor ( 0.0f, 0.02f, 0.05f, 1 ));
        GL_CHECK(glClearDepthf(0.0f));
        GL_CHECK(glClear ( GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT ));*/

        gl.glViewport(0,0, getGLContext().width(), getGLContext().height());
        gl.glClearColor ( 0.0f, 0.02f, 0.05f, 1 );
        gl.glClearDepthf(1.f);
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT | GLenum.GL_DEPTH_BUFFER_BIT);
        gl.glEnable(GLenum.GL_DEPTH_TEST);


        if( mUIConstants.lightCullTechnique == CULL_FORWARD_NONE )
        {
            RenderForward(renderParams);
        }
        else if( mUIConstants.lightCullTechnique == CULL_CLUSTERED )
        {
            RenderForwardClustered(renderParams);
        }
        else
        {
            RenderGBuffer(renderParams);
            ComputeLighting(renderParams);
        }
    }

    private void InitializeLightParameters(){
        mPointLightParameters = new PointLight[MAX_LIGHTS];
        mLightsInfoTextureData.resize(MAX_LIGHTS * 8 );

        mLightInitialTransform = new PointLightInitTransform[MAX_LIGHTS];
        mPointLightPositionWorld = new Vector3f[MAX_LIGHTS];

        // Use a constant seed for consistency
        final Random random = new Random(1337);
        class CRndHelper
        {
            CRndHelper(float fMin, float fMax) {
                m_fMin = fMin;
                m_fMax = fMax;
            }

            float get ()
            {
                return random.nextFloat() * (m_fMax - m_fMin) + m_fMin;
            }

            float m_fMin, m_fMax;
        };


//        srand(1337);

        CRndHelper radiusNormDist = new CRndHelper(0.0f, 1.0f);
        final float maxRadius = 50.0f;
        CRndHelper angleDist = new CRndHelper(0.0f, 2.0f * Numeric.PI);
        CRndHelper heightDist = new CRndHelper(2.0f, 18.0f);
        CRndHelper animationSpeedDist = new CRndHelper(2.0f, 20.0f);
        CRndHelper animationDirection = new CRndHelper(0, 1);
        CRndHelper hueDist = new CRndHelper(0.0f, 1.0f);
        CRndHelper intensityDist = new CRndHelper(0.1f, 0.5f);
        CRndHelper attenuationDist = new CRndHelper(4.0f, 10.0f);
        final float attenuationStartFactor = 0.8f;

        for (int i = 0; i < MAX_LIGHTS; ++i) {
            PointLight params = mPointLightParameters[i] = new PointLight();
            PointLightInitTransform init = mLightInitialTransform[i] = new PointLightInitTransform();

            init.radius = (float) (Math.sqrt(radiusNormDist.get()) * maxRadius);
            init.angle = angleDist.get();
            init.height = heightDist.get();
            // Normalize by arc length
            init.animationSpeed = (animationDirection.get() > 0.5f ? +1.f : -1.f) * animationSpeedDist.get() / init.radius;

            // HSL->RGB, vary light hue
//            params.color = intensityDist() * HueToRGB(hueDist());
            HueToRGB(hueDist.get(), params.color); params.color.scale(intensityDist.get());
            params.attenuationEnd = attenuationDist.get();
            params.attenuationBegin = attenuationStartFactor * params.attenuationEnd;
        }
    }

    private void ClusterCullingRasterizeLights(Matrix4f mCameraProj){
        int n = mUIConstants.clusteredGridScale;
        mLightGridBuilder.reset(2*n, n, 8*n);

        {
            //int64_t raster_clk = -get_tsc();
            {
                mLightGridBuilder.clearAllFragments();
                RasterizeLights(mLightGridBuilder, mCameraProj.m00, mCameraProj.m11, mpCamera.GetNearPlaneDistance(),
                        mpCamera.GetFarPlaneDistance(), mPointLightParameters, mActiveLights);
            }
            //raster_clk += get_tsc();

            //int64_t upload_clk = -get_tsc();
            {
                mLightGridBuilder.buildAndUpload(mLightGridTextureData.getData(), LIGHT_GRID_TEXTURE_WIDTH * LIGHT_GRID_TEXTURE_HEIGHT * /*sizeof(unsigned int)*/4 * 4);
                mpLightGridTexture.UpdateData(CacheBuffer.wrap(mLightGridTextureData.getData()), GLenum.GL_RGBA_INTEGER, GLenum.GL_UNSIGNED_INT);
            }
            //upload_clk += get_tsc();

            //dprintf("rasterization: %.2f clk/entry \n", 1.0*raster_clk / entry_count);
            //dprintf("grid build: %.2f clk/entry \n", 1.0*list_clk / entry_count);
            //dprintf("grid size: %.2f MB \n", 1.0*mLightGridBuilder.allocatedBytes / 1024/1024);
            //dprintf("grid mapd: %.2f clk/entry \n", 1.0*upload_clk / entry_count);
        }
    }

    private void UpdateLights(Matrix4f cameraView){
        assert( mActiveLights < MAX_LIGHTS );
        // Transform light world positions into view space and store in our parameters array
        for(int iLight = 0; iLight < mActiveLights; ++iLight)
        {
            /*float4 LighWorldPos(mPointLightPositionWorld[iLight], 1.f);
            float4 LightViewPos = LighWorldPos * cameraView;
            LightViewPos.x /= LightViewPos.w;
            LightViewPos.y /= LightViewPos.w;
            LightViewPos.z /= LightViewPos.w;*/
            Vector3f LightViewPos = Matrix4f.transformCoord(cameraView, mPointLightPositionWorld[iLight], null);

            PointLight CurrPointLightParams = mPointLightParameters[iLight];
            CurrPointLightParams.positionView.set(LightViewPos);
            /*mLightsInfoTextureData[iLight*4 + 0] = LightViewPos.x;
            mLightsInfoTextureData[iLight*4 + 1] = LightViewPos.y;
            mLightsInfoTextureData[iLight*4 + 2] = LightViewPos.z;
            mLightsInfoTextureData[iLight*4 + 3] = CurrPointLightParams.attenuationBegin;*/

            mLightsInfoTextureData.set(iLight*4 + 0, LightViewPos.x);
            mLightsInfoTextureData.set(iLight*4 + 1, LightViewPos.x);
            mLightsInfoTextureData.set(iLight*4 + 2, LightViewPos.x);
            mLightsInfoTextureData.set(iLight*4 + 3, CurrPointLightParams.attenuationBegin);

            mLightsInfoTextureData.set(iLight*4 + mActiveLights * 4 + 0, CurrPointLightParams.color.x);
            mLightsInfoTextureData.set(iLight*4 + mActiveLights * 4 + 1, CurrPointLightParams.color.y);
            mLightsInfoTextureData.set(iLight*4 + mActiveLights * 4 + 2, CurrPointLightParams.color.z);
            mLightsInfoTextureData.set(iLight*4 + mActiveLights * 4 + 3, CurrPointLightParams.attenuationEnd);
        }

        // Copy light list into shader buffer
        {
            mpPointLightsInfoTexture.UpdateData(CacheBuffer.wrap(mLightsInfoTextureData.getData()), GLenum.GL_RGBA, GLenum.GL_FLOAT);
        }
    }

    private void Move(float elapsedTime){
        mTotalTime += elapsedTime;

        // Update positions of active lights
        for (int i = 0; i < mActiveLights; ++i) {
            PointLightInitTransform initTransform = mLightInitialTransform[i];
            float angle = initTransform.angle + mTotalTime * initTransform.animationSpeed;
            mPointLightPositionWorld[i].set(
                    initTransform.radius * (float)Math.cos(angle),
                    initTransform.height,
                    initTransform.radius * (float)Math.sin(angle));
        }
    }


    private void RenderForward(CPUTRenderParameters renderParams){
        RenderScene(renderParams, MATERIAL_FORWARD);
    }

    private void RenderForwardClustered(CPUTRenderParameters renderParams){
        ClusterCullingRasterizeLights(mpCamera.GetProjectionMatrix() );
        RenderScene(renderParams, MATERIAL_CLUSTERED);
    }

    private void RenderGBuffer(CPUTRenderParameters renderParams){
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, m_GBufferFBO);

//        int x, y, w, h;
//        mpWindow->GetClientDimensions(&x, &y, &w, &h);
        gl.glViewport(0, 0, getGLContext().width(), getGLContext().height() );
        gl.glClearColor ( 0.0f, 0.02f, 0.05f, 1e+5f );
        gl.glClearDepthf(0.0f);
        gl.glClear ( GLenum.GL_COLOR_BUFFER_BIT | GLenum.GL_DEPTH_BUFFER_BIT );
        gl.glEnable(GLenum.GL_DEPTH_TEST);

        RenderScene(renderParams, MATERIAL_GBUFFER);
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
    }

    private void ComputeLighting(CPUTRenderParameters renderParams){
        gl.glBindVertexArray(m_DummyVAO);

        if (mUIConstants.lightCullTechnique == CULL_QUAD || mUIConstants.lightCullTechnique == CULL_DEFERRED_NONE) {
            gl.glDisable(GLenum.GL_DEPTH_TEST);
            gl.glUseProgram(m_DeferredNoCullProgram.getProgram());
            // When quad mode is selected, we stil need to apply the light maps.
            // To do this, we use DeferredNoCull, but set gLightsBuffer uniform to 0.
            // This gives us the desired effect
            SetGLProgramUniforms(m_DeferredNoCullProgram.getProgram(), mUIConstants.lightCullTechnique == CULL_DEFERRED_NONE);
            gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
            gl.glEnable(GLenum.GL_DEPTH_TEST);
        }

        if( mUIConstants.lightCullTechnique == CULL_QUAD ) {
            gl.glUseProgram(m_GPUQuadProgram.getProgram());
            SetGLProgramUniforms(m_GPUQuadProgram.getProgram());

            gl.glEnable(GLenum.GL_BLEND);
            gl.glBlendFunc(GLenum.GL_ONE, GLenum.GL_ONE);
            gl.glDepthMask(false);

            gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, mActiveLights * 6);

            gl.glDepthMask(true);
            gl.glDisable(GLenum.GL_BLEND);
        }

        if (mUIConstants.lightCullTechnique == CULL_COMPUTE_SHADER_TILE ) {
            gl.glUseProgram(m_TiledDeferredCSProgram.getProgram());
            SetGLProgramUniforms(m_TiledDeferredCSProgram.getProgram());

            gl.glBindImageTexture(0, mpShadedBackBuffer.getTexture(), 0, false, 0, GLenum.GL_WRITE_ONLY, GLenum.GL_RGBA16F);
            int dispatchWidth = (mBackBufferWidth + COMPUTE_SHADER_TILE_GROUP_DIM - 1) / COMPUTE_SHADER_TILE_GROUP_DIM;
            int dispatchHeight = (mBackBufferHeight + COMPUTE_SHADER_TILE_GROUP_DIM - 1) / COMPUTE_SHADER_TILE_GROUP_DIM;
            gl.glDispatchCompute(dispatchWidth, dispatchHeight, 1);

            gl.glDisable(GLenum.GL_DEPTH_TEST);
            gl.glUseProgram(m_CopyTextureProgram.getProgram());
            {
                int SrcSamplerLocation = gl.glGetUniformLocation(m_CopyTextureProgram.getProgram(), "gSrcTex");
                int SrcSamplerBindPoint = 0;
                gl.glActiveTexture(GLenum.GL_TEXTURE0 + SrcSamplerBindPoint);
                gl.glBindTexture(GLenum.GL_TEXTURE_2D, mpShadedBackBuffer.getTexture() );
                gl.glUniform1i(SrcSamplerLocation, SrcSamplerBindPoint);
            }
            gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
            gl.glEnable(GLenum.GL_DEPTH_TEST);
        }

        gl.glBindVertexArray(0);
        gl.glUseProgram(0);
    }

    private void SetGLProgramUniforms(int GLProgram){
        SetGLProgramUniforms(GLProgram, true);
    }

    private void SetGLProgramUniforms(int GLProgram, boolean BindLightsBuffer /*= true*/){
        int DiffuseSamplerLocation = gl.glGetUniformLocation(GLProgram, "gGBufferDiffuse");
        int DiffuseSamplerBindPoint = 0;
        gl.glActiveTexture(GLenum.GL_TEXTURE0 + DiffuseSamplerBindPoint);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D, mpGBufferDiffuseColor.getTexture());
        gl.glUniform1i(DiffuseSamplerLocation, DiffuseSamplerBindPoint);

        int NormalSamplerLocation = gl.glGetUniformLocation(GLProgram, "gGBufferNormal");
        int NormalSamplerBindPoint = 1;
        gl.glActiveTexture(GLenum.GL_TEXTURE0 + NormalSamplerBindPoint);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D, mpGBufferNormal.getTexture());
        gl.glUniform1i(NormalSamplerLocation, NormalSamplerBindPoint);

        int LightMapSamplerLocation = gl.glGetUniformLocation(GLProgram, "gGBufferLightMap");
        int LightMapSamplerBindPoint = 2;
        gl.glActiveTexture(GLenum.GL_TEXTURE0 + LightMapSamplerBindPoint);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D, mpGBufferLightMap.getTexture());
        gl.glUniform1i(LightMapSamplerLocation, LightMapSamplerBindPoint);

        //auto DepthBufferSamplerLocation = glGetUniformLocation(m_DeferredNoCullProgram.GetProgram(), "gDepthBuffer");
        //int DepthBufferSamplerBindPoint = 3;
        //GL_CHECK(glActiveTexture(GL_TEXTURE0 + DepthBufferSamplerBindPoint));
        //GL_CHECK(glBindTexture(GL_TEXTURE_2D, mpGBufferDepth->GetTexture()));
        //glUniform1i(DepthBufferSamplerLocation, DepthBufferSamplerBindPoint);

        int LightsBufferSamplerLocation = gl.glGetUniformLocation(GLProgram, "gLightsBuffer");
        int LightsBufferSamplerBindPoint = 4;
        gl.glActiveTexture(GLenum.GL_TEXTURE0 + LightsBufferSamplerBindPoint);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D, BindLightsBuffer ? mpPointLightsInfoTexture.GetTexture().getTexture() : 0);
        gl.glUniform1i(LightsBufferSamplerLocation, LightsBufferSamplerBindPoint);

        int PerFrameValuesCBLocation = gl.glGetUniformBlockIndex(GLProgram, "cbPerFrameValues");
        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, PerFrameValuesCBLocation, mpPerFrameConstantBuffer.GetNativeBuffer().getBuffer());
        gl.glUniformBlockBinding(GLProgram, PerFrameValuesCBLocation, PerFrameValuesCBLocation);

        int UIConstantsCBLocation = gl.glGetUniformBlockIndex(GLProgram, "cbUIConstants");
        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, UIConstantsCBLocation, mpUIConstantsBuffer.GetNativeBuffer().getBuffer());
        gl.glUniformBlockBinding(GLProgram, UIConstantsCBLocation, UIConstantsCBLocation);
    }

    @Override
    protected void reshape(int width, int height) {
        if(width <=0 || height <= 0)
            return;

        if(mBackBufferWidth == width && mBackBufferHeight == height)
            return;

        mBackBufferWidth  = width;
        mBackBufferHeight = height;

        if(mpGBufferDiffuseColor != null){
            mpGBufferDiffuseColor.dispose();
            mpGBufferNormal.dispose();
        }

        /*mpGBufferDiffuseColor = (CPUTTextureDX11)CPUTTextureDX11.CreateTexture(_L("$GBufferDiffuse"), GL_RGBA16F, width, height, GL_RGBA, GL_HALF_FLOAT, NULL);
        mpGBufferNormal = (CPUTTextureDX11)CPUTTextureDX11.CreateTexture(_L("$GBufferNormal"), GL_RG16F, width, height, GL_RG, GL_HALF_FLOAT, NULL);
        mpGBufferLightMap = (CPUTTextureDX11)CPUTTextureDX11.CreateTexture(_L("$GBufferLightMap"), GL_RGBA16F, width, height, GL_RGBA, GL_HALF_FLOAT, NULL);
        mpGBufferDepth = (CPUTTextureDX11)CPUTTextureDX11.CreateTexture(_L("$GBufferDepth"), GL_DEPTH_COMPONENT32F, width, height, GL_DEPTH_COMPONENT, GL_FLOAT, NULL);
        mpShadedBackBuffer = (CPUTTextureDX11)CPUTTextureDX11.CreateTexture(_L("$GLitBackBuffer"), GL_RGBA16F, width, height, GL_RGBA, GL_HALF_FLOAT, NULL, true);

        todo
        if( !m_GBufferFBO )
            GL_CHECK( glGenFramebuffers(1, &m_GBufferFBO) );
        if( !m_DummyVAO )
            GL_CHECK( glGenVertexArrays(1, &m_DummyVAO) );

        GL_CHECK( glBindFramebuffer(GL_DRAW_FRAMEBUFFER, m_GBufferFBO) );
        GL_CHECK( glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, mpGBufferDiffuseColor->GetTexture(), 0) );
        GL_CHECK( glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT1, GL_TEXTURE_2D, mpGBufferNormal->GetTexture(), 0) );
        GL_CHECK( glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT2, GL_TEXTURE_2D, mpGBufferLightMap->GetTexture(), 0) );
        GL_CHECK( glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER, GL_DEPTH_ATTACHMENT,  GL_TEXTURE_2D, mpGBufferDepth->GetTexture(), 0) );
        GLenum DrawBuffers[] = { GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1, GL_COLOR_ATTACHMENT2, GL_COLOR_ATTACHMENT3 };
        glDrawBuffers(sizeof(DrawBuffers)/sizeof(DrawBuffers[0]), DrawBuffers);
        auto Completness = glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER);
        if( Completness != GL_FRAMEBUFFER_COMPLETE )
        {
            GL_CHECK(GL_INVALID_VALUE);
        }
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);*/
    }

    private void RenderScene(CPUTRenderParameters renderParams, int MaterialIndex){
        for(CPUTMaterial it : mpMaterials)
            it.SetCurrentEffect(MaterialIndex);
        mpScene.Render( renderParams, 0 );
    }

    private void SetActiveLights(int activeLights) {
        mActiveLights = activeLights;

        if( mpPointLightsInfoTexture != null) {
//            mpPointLightsInfoTexture.InitGLTexture(GL_RGBA32F, mActiveLights, 2, GL_RGBA, GL_FLOAT, NULL);
        }else
            mpPointLightsInfoTexture = (CPUTTextureDX11)CPUTTextureDX11.CreateTexture("$lights_buffer",  GLenum.GL_RGBA32F, mActiveLights, 2, 0, 0, 0);

        // Make sure all the active lights are set up
        Move(0.0f);
    }

    private static void HueToRGB(float hue, Vector3f color) {
        hue *= 6;
        float intPart = (int)Math.floor(hue);
        float fracPart =  hue - intPart; // modff(hue * 6.0f, &intPart);
        int region = (int) intPart;

        switch (region) {
            case 0: color.set(1.0f, fracPart, 0.0f); return;
            case 1: color.set(1.0f - fracPart, 1.0f, 0.0f);return;
            case 2: color.set(0.0f, 1.0f, fracPart);return;
            case 3: color.set(0.0f, 1.0f - fracPart, 1.0f);return;
            case 4: color.set(fracPart, 0.0f, 1.0f);return;
            case 5: color.set(1.0f, 0.0f, 1.0f - fracPart);return;
        }

        color.set(0.0f, 0.0f, 0.0f);
    }

    private static void RasterizeLights(LightGridBuilder builder, float proj00, float proj11, float near, float far, PointLight lights[], int lightCount)
    {
        FragmentFactory fragmentFactory = new FragmentFactory();
        //D3DXMATRIX mCameraProj = *viewerCamera->GetProjMatrix();

        // z is flipped...
//        GetNearFarFromProjMatr(mCameraProj, camera.far, camera.near);
        //camera.far = viewerCamera->GetNearClip();
        //camera.near = viewerCamera->GetFarClip();
//        camera.proj11 = mCameraProj.r0.x;
//        camera.proj22 = mCameraProj.r1.y;

        for (int lightIndex = 0; lightIndex < lightCount; lightIndex++)
        {
            GenerateLightFragments(fragmentFactory, builder, proj00, proj11, near, far, lights[lightIndex], lightIndex);
        }
    }

    private static void GenerateLightFragments(FragmentFactory fragmentFactory, LightGridBuilder builder,
                                               float proj00, float proj11, float near, float far, PointLight light, int lightIndex)
    {
        LightGridDimensions dim = builder.dimensions();
//        float4 mCameraNearFar;// = float4(viewerCamera->GetFarClip(), viewerCamera->GetNearClip(), 0.0f, 0.0f);
//        GetNearFarFromProjMatr(mCameraProj, mCameraNearFar.y, mCameraNearFar.x);
        //D3DXMATRIX mCameraProj = *viewerCamera->GetProjMatrix();

        Vector2f mCameraNearFar = new Vector2f(near, far);
        // compute view space quad
        Vector4f clipRegion = ComputeClipRegion(light.positionView, light.attenuationEnd, proj00, proj11, mCameraNearFar);

        //clipRegion = float4(-1.0f, -1.0f, 1.0f, 1.0f);
//        clipRegion = (clipRegion + float4(1.0f, 1.0f, 1.0f, 1.0f)) / 2; // map coordinates to [0..1]
        Vector4f.add(clipRegion, Vector4f.ONE, clipRegion);  clipRegion.scale(1.f/2);

        // meh, this is upside-down
        clipRegion.y = 1 - clipRegion.y;
        clipRegion.w = 1 - clipRegion.w;
//        std::swap(clipRegion.y, clipRegion.w);
        float t = clipRegion.y;
        clipRegion.y = clipRegion.w;
        clipRegion.w = t;

        int[] intClipRegion = new int[4];
        intClipRegion[0] = (int)(clipRegion.x * dim.width);
        intClipRegion[1] = (int)(clipRegion.y * dim.height);
        intClipRegion[2] = (int)(clipRegion.z * dim.width);
        intClipRegion[3] = (int)(clipRegion.w * dim.height);

        if (intClipRegion[0] < 0) intClipRegion[0] = 0;
        if (intClipRegion[1] < 0) intClipRegion[1] = 0;
        if (intClipRegion[2] >= dim.width) intClipRegion[2] = dim.width - 1;
        if (intClipRegion[3] >= dim.height) intClipRegion[3] = dim.height - 1;

        float center_z = (light.positionView.z - mCameraNearFar.x) / (mCameraNearFar.y - mCameraNearFar.x);
        float dist_z = light.attenuationEnd / (mCameraNearFar.y - mCameraNearFar.x);

        //dist_z = center_z = 0.5f;

        int[] intZBounds = new int[2];
        intZBounds[0] = (int)((center_z - dist_z)* dim.depth);
        intZBounds[1] = (int)((center_z + dist_z)* dim.depth);

        if (intZBounds[0] < 0) intZBounds[0] = 0;
        if (intZBounds[1] >= dim.depth) intZBounds[1] = dim.depth - 1;


        for (int y = intClipRegion[1] / 4; y <= intClipRegion[3] / 4; y++)
            for (int x = intClipRegion[0] / 4; x <= intClipRegion[2] / 4; x++)
                for (int z = intZBounds[0] / 4; z <= intZBounds[1] / 4; z++)
                {
                    int x1 = Numeric.clamp(intClipRegion[0] - x * 4, 0, 3);
                    int x2 = Numeric.clamp(intClipRegion[2] - x * 4, 0, 3);
                    int y1 = Numeric.clamp(intClipRegion[1] - y * 4, 0, 3);
                    int y2 = Numeric.clamp(intClipRegion[3] - y * 4, 0, 3);
                    int z1 = Numeric.clamp(intZBounds[0] - z * 4, 0, 3);
                    int z2 = Numeric.clamp(intZBounds[1] - z * 4, 0, 3);

                    long coverage = 0;
                    coverage = fragmentFactory.coverage(x1, x2, y1, y2, z1, z2);

                    if (false)
                        for (int zz = z1; zz <= z2; zz++)
                            for (int yy = y1; yy <= y2; yy++)
                                for (int xx = x1; xx <= x2; xx++)
                                {
                                    int fineIndex = (yy / 2 % 2) * 32 + (xx / 2 % 2) * 16 + (yy % 2) * 8 + (xx % 2) * 4 + (zz % 4);

                                    boolean separated = false;

                                    int[] grid = new int[3];
                                    grid[0] = dim.width;
                                    grid[1] = dim.height;
                                    grid[2] = dim.depth;
                                    //separated = separationtTest(x * 4 + xx, y * 4 + yy, z * 4 + zz, grid, viewerCamera, light);

                                    if (!separated)
                                        coverage |= 1 << fineIndex;
                                }

                    builder.pushFragment(dim.cellIndex(x, y, z), lightIndex, coverage);
                }
    }

    // Returns bounding box [min.xy, max.xy] in clip [-1, 1] space.
    private static Vector4f ComputeClipRegion(ReadableVector3f lightPosView, float lightRadius,
        float proj00, float proj11, Vector2f mCameraNearFar)
    {
        // Early out with empty rectangle if the light is too far behind the view frustum
        Vector4f clipRegion = new Vector4f(1, 1, 0, 0);
        if (lightPosView.getZ() + lightRadius >= mCameraNearFar.x) {
            Vector2f clipMin = new Vector2f(-1.0f, -1.0f);
            Vector2f clipMax = new Vector2f(1.0f, 1.0f);

            long v = UpdateClipRegion(lightPosView.getX(), lightPosView.getZ(), lightRadius, proj00, clipMin.x, clipMax.x);
            clipMin.x = Float.intBitsToFloat(Numeric.decodeFirst(v));
            clipMax.x = Float.intBitsToFloat(Numeric.decodeSecond(v));
            v = UpdateClipRegion(lightPosView.getY(), lightPosView.getZ(), lightRadius, proj11, clipMin.y, clipMax.y);
            clipMin.y = Float.intBitsToFloat(Numeric.decodeFirst(v));
            clipMax.y = Float.intBitsToFloat(Numeric.decodeSecond(v));

            clipRegion.set(clipMin.x, clipMin.y, clipMax.x, clipMax.y);
        }

        return clipRegion;
    }

    private static long UpdateClipRegion(float lc,          // Light x/y coordinate (view space)
                          float lz,          // Light z coordinate (view space)
                          float lightRadius,
                          float cameraScale, // Project scale for coordinate (r0.x or r1.y for x/y respectively)
                          float clipMin,
                          float clipMax)
    {
        float rSq = lightRadius * lightRadius;
        float lcSqPluslzSq = lc * lc + lz * lz;
        float d = rSq * lc * lc - lcSqPluslzSq * (rSq - lz * lz);

        if (d > 0)
        {
            float a = lightRadius * lc;
            float b = (float) Math.sqrt(d);
            float nx0 = (a + b) / lcSqPluslzSq;
            float nx1 = (a - b) / lcSqPluslzSq;

            long v = UpdateClipRegionRoot(nx0, lc, lz, lightRadius, cameraScale, clipMin, clipMax);
            clipMin = Float.intBitsToFloat(Numeric.decodeFirst(v));
            clipMax = Float.intBitsToFloat(Numeric.decodeSecond(v));
            return  UpdateClipRegionRoot(nx1, lc, lz, lightRadius, cameraScale, clipMin, clipMax);
        }

        return Numeric.encode(Float.floatToIntBits(clipMin), Float.floatToIntBits(clipMax));
    }

    // Bounds computation utilities, similar to GPUQuad.hlsl
    private static long UpdateClipRegionRoot(float nc,          // Tangent plane x/y normal coordinate (view space)
                              float lc,          // Light x/y coordinate (view space)
                              float lz,          // Light z coordinate (view space)
                              float lightRadius,
                              float cameraScale, // Project scale for coordinate (r0.x or r1.y for x/y respectively)
                              float clipMin,
                              float clipMax)
    {
        float nz = (lightRadius - nc * lc) / lz;
        float pz = (lc * lc + lz * lz - lightRadius * lightRadius) / (lz - (nz / nc) * lc);

        if (pz > 0.0f) {
            float c = -nz * cameraScale / nc;
            if (nc > 0.0f)
            {                      // Left side boundary
                clipMin = Math.max(clipMin, c);
            }
            else
            {                       // Right side boundary
                clipMax = Math.min(clipMax, c);
            }
        }

        return Numeric.encode(Float.floatToIntBits(clipMin), Float.floatToIntBits(clipMax));
    }
}
