package jet.opengl.renderer.Unreal4;

import org.lwjgl.util.vector.Vector2i;

import java.util.ArrayList;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.util.LogUtil;
import jet.opengl.renderer.Unreal4.api.ERHIFeatureLevel;
import jet.opengl.renderer.Unreal4.api.EShaderPlatform;
import jet.opengl.renderer.Unreal4.api.FExclusiveDepthStencil;
import jet.opengl.renderer.Unreal4.hit.FHitProxyConsumer;
import jet.opengl.renderer.Unreal4.resouces.FGlobalDynamicIndexBuffer;
import jet.opengl.renderer.Unreal4.resouces.FGlobalDynamicReadBuffer;
import jet.opengl.renderer.Unreal4.resouces.FGlobalDynamicVertexBuffer;
import jet.opengl.renderer.Unreal4.scenes.EAntiAliasingMethod;
import jet.opengl.renderer.Unreal4.scenes.FPrimitiveSceneProxy;
import jet.opengl.renderer.Unreal4.scenes.FScene;
import jet.opengl.renderer.Unreal4.scenes.FSceneView;
import jet.opengl.renderer.Unreal4.scenes.FSceneViewFamily;
import jet.opengl.renderer.Unreal4.scenes.FSceneViewState;
import jet.opengl.renderer.Unreal4.utils.RenderUtils;
import jet.opengl.renderer.Unreal4.views.FViewCommands;
import jet.opengl.renderer.Unreal4.views.FViewInfo;

/**
 * Used as the scope for scene rendering functions.
 * It is initialized in the game thread by FSceneViewFamily::BeginRender, and then passed to the rendering thread.
 * The rendering thread calls Render(), and deletes the scene renderer when it returns.
 */
public abstract class FSceneRenderer implements Disposeable {
    public final static EShaderPlatform[] GShaderPlatformForFeatureLevel = {
            EShaderPlatform.SP_OPENGL_SM5,
            EShaderPlatform.SP_OPENGL_SM5,
            EShaderPlatform.SP_OPENGL_SM5,
            EShaderPlatform.SP_OPENGL_SM5,
    };

    /** The scene being rendered. */
    FScene Scene;

    /** The view family being rendered.  This references the Views array. */
    FSceneViewFamily ViewFamily;

    /** The views being rendered. */
    FViewInfo[] Views;

    final FMeshElementCollector MeshCollector = new FMeshElementCollector();

    final FMeshElementCollector RayTracingCollector = new FMeshElementCollector();

    /** Information about the visible lights. */
    final ArrayList<FVisibleLightInfo> VisibleLightInfos = new ArrayList<>();

    /** Array of dispatched parallel shadow depth passes. */
    final ArrayList<FParallelMeshDrawCommandPass> DispatchedShadowDepthPasses = new ArrayList<>();

    final FSortedShadowMaps SortedShadowsForShadowDepthPass = new FSortedShadowMaps();

    /** If a freeze request has been made */
    boolean bHasRequestedToggleFreeze;

    /** True if precomputed visibility was used when rendering the scene. */
    boolean bUsedPrecomputedVisibility;

    /** Lights added if wholescenepointlight shadow would have been rendered (ignoring r.SupportPointLightWholeSceneShadows). Used for warning about unsupported features. */
    final ArrayList<String> UsedWholeScenePointLightNames = new ArrayList<>();

    /** Feature level being rendered */
    int FeatureLevel = ERHIFeatureLevel.SM5;
//    EShaderPlatform ShaderPlatform;

    /**
     * The width in pixels of the stereo view family being rendered. This may be different than FamilySizeX if
     * we're using adaptive resolution stereo rendering. In that case, FamilySizeX represents the maximum size of
     * the family to ensure the backing render targets don't change between frames as the view size varies.
     */
    int InstancedStereoWidth;

    /* Only used if we are going to delay the deletion of the scene renderer until later. */
//    FMemMark* RootMark;

    FSceneRenderer(FSceneViewFamily InViewFamily, FHitProxyConsumer HitProxyConsumer){
        Scene = InViewFamily.Scene != null ? InViewFamily.Scene.GetRenderScene() : null;
        ViewFamily = InViewFamily;
//        MeshCollector(InViewFamily->GetFeatureLevel())  TODO
//                ,	RayTracingCollector(InViewFamily->GetFeatureLevel())  TODO
        bUsedPrecomputedVisibility = (false);
        InstancedStereoWidth = (0);
//                ,	RootMark(nullptr)
//        FamilySize(0, 0)

        UE4Engine.check(Scene != null);

//        check(IsInGameThread());
        ViewFamily.FrameNumber = Scene!= null ? Scene.GetFrameNumber() : UE4Engine.GFrameNumber;

        // Copy the individual views.
        boolean bAnyViewIsLocked = false;
        Views = new FViewInfo[InViewFamily.Views.length];
        for(int ViewIndex = 0;ViewIndex < InViewFamily.Views.length;ViewIndex++)
        {
//#if !(UE_BUILD_SHIPPING || UE_BUILD_TEST)
            for(int ViewIndex2 = 0;ViewIndex2 < InViewFamily.Views.length;ViewIndex2++)
            {
                if (ViewIndex != ViewIndex2 && InViewFamily.Views[ViewIndex].State != null)
                {
                    // Verify that each view has a unique view state, as the occlusion query mechanism depends on it.
                    UE4Engine.check(InViewFamily.Views[ViewIndex].State != InViewFamily.Views[ViewIndex2].State);
                }
            }
//#endif
            // Construct a FViewInfo with the FSceneView properties.
            FViewInfo ViewInfo = Views[ViewIndex] = new FViewInfo(InViewFamily.Views[ViewIndex]);
            ViewFamily.Views[ViewIndex] = ViewInfo;
            ViewInfo.Family = ViewFamily;
            bAnyViewIsLocked |= ViewInfo.bIsLocked;

            UE4Engine.check(ViewInfo.ViewRect.area() == 0);

//#if WITH_EDITOR
            // Should we allow the user to select translucent primitives?
            ViewInfo.bAllowTranslucentPrimitivesInHitProxy =
//                    GEngine->AllowSelectTranslucent() ||		// User preference enabled?
                            !ViewInfo.IsPerspectiveProjection();		// Is orthographic view?
//#endif

            // Batch the view's elements for later rendering.
            if(ViewInfo.Drawer != null)
            {
//                FViewElementPDI ViewElementPDI(ViewInfo,HitProxyConsumer,&ViewInfo->DynamicPrimitiveShaderData);
//                ViewInfo->Drawer->Draw(ViewInfo,&ViewElementPDI);  todo
            }

//		#if !UE_BUILD_SHIPPING
//            if (CVarTestCameraCut.GetValueOnGameThread())
            {
                ViewInfo.bCameraCut = true;
            }
//		#endif
        }

        // Catches inconsistency one engine show flags for screen percentage and whether it is supported or not.
//        ensureMsgf(!(ViewFamily.EngineShowFlags.ScreenPercentage && !ViewFamily.SupportsScreenPercentage()),
//                TEXT("Screen percentage is not supported, but show flag was incorectly set to true."));

        LogUtil.i(LogUtil.LogType.DEFAULT, "Screen percentage is not supported, but show flag was incorectly set to true.");

        // Fork the view family.
        {
//            UE4Engine.check(InViewFamily.ScreenPercentageInterface);  todo
//            ViewFamily.ScreenPercentageInterface = nullptr;
//            ViewFamily.SetScreenPercentageInterface(InViewFamily->ScreenPercentageInterface->Fork_GameThread(ViewFamily));
        }

//	#if !UE_BUILD_SHIPPING
        // Override screen percentage interface.
        /*if (int OverrideId = CVarTestScreenPercentageInterface.GetValueOnGameThread())  todo screen percentage
        {
            check(ViewFamily.ScreenPercentageInterface);

            // Replaces screen percentage interface with dynamic resolution hell's driver.
            if (OverrideId == 1 && ViewFamily.Views[0]->State)
            {
                delete ViewFamily.ScreenPercentageInterface;
                ViewFamily.ScreenPercentageInterface = nullptr;
                ViewFamily.EngineShowFlags.ScreenPercentage = true;
                ViewFamily.SetScreenPercentageInterface(new FScreenPercentageHellDriver(ViewFamily));
            }
        }

        // Override secondary screen percentage for testing purpose.
        if (CVarTestSecondaryUpscaleOverride.GetValueOnGameThread() > 0 && !Views[0].bIsReflectionCapture)
        {
            ViewFamily.SecondaryViewFraction = 1.0 / float(CVarTestSecondaryUpscaleOverride.GetValueOnGameThread());
            ViewFamily.SecondaryScreenPercentageMethod = ESecondaryScreenPercentageMethod::NearestSpatialUpscale;
        }*/
//	#endif

        // If any viewpoint has been locked, set time to zero to avoid time-based
        // rendering differences in materials.
        if (bAnyViewIsLocked)
        {
            ViewFamily.CurrentRealTime = 0.0f;
            ViewFamily.CurrentWorldTime = 0.0f;
        }

        if(HitProxyConsumer != null)
        {
            // Set the hit proxies show flag.
            ViewFamily.EngineShowFlags.SetHitProxies(1);
        }

        // launch custom visibility queries for views
//        if (GCustomCullingImpl)
        {
            for(int ViewIndex = 0;ViewIndex < Views.length;ViewIndex++)
            {
                FViewInfo ViewInfo = Views[ViewIndex];
                ViewInfo.CustomVisibilityQuery = null ; // GCustomCullingImpl->CreateQuery(ViewInfo);
            }
        }

        // copy off the requests
        // (I apologize for the const_cast, but didn't seem worth refactoring just for the freezerendering command)
        bHasRequestedToggleFreeze = false; //const_cast<FRenderTarget*>(InViewFamily.RenderTarget)->HasToggleFreezeCommand();

        FeatureLevel = Scene.GetFeatureLevel();
        ShaderPlatform = Scene.GetShaderPlatform();

        bDumpMeshDrawCommandInstancingStats = false; //!!GDumpInstancingStats;
//        GDumpInstancingStats = 0;
    }

    @Override
    public void dispose() {
        // To prevent keeping persistent references to single frame buffers, clear any such reference at this point.
        ClearPrimitiveSingleFramePrecomputedLightingBuffers();

        if(Scene != null)
        {
            // Destruct the projected shadow infos.
            for(TSparseArray<FLightSceneInfoCompact>::TConstIterator LightIt(Scene->Lights);LightIt;++LightIt)
            {
                if( VisibleLightInfos.IsValidIndex(LightIt.GetIndex()) )
                {
                    FVisibleLightInfo& VisibleLightInfo = VisibleLightInfos[LightIt.GetIndex()];
                    for(int32 ShadowIndex = 0;ShadowIndex < VisibleLightInfo.MemStackProjectedShadows.Num();ShadowIndex++)
                    {
                        // FProjectedShadowInfo's in MemStackProjectedShadows were allocated on the rendering thread mem stack,
                        // Their memory will be freed when the stack is freed with no destructor call, so invoke the destructor explicitly
                        VisibleLightInfo.MemStackProjectedShadows[ShadowIndex]->~FProjectedShadowInfo();
                    }
                }
            }
        }

        // Manually release references to TRefCountPtrs that are allocated on the mem stack, which doesn't call dtors
        SortedShadowsForShadowDepthPass.Release();
    }

    //    virtual ~FSceneRenderer();

    // FSceneRenderer interface

    abstract void Render(/*FRHICommandListImmediate& RHICmdList*/);
    void RenderHitProxies(/*FRHICommandListImmediate& RHICmdList*/) {}

    /** Creates a scene renderer based on the current feature level. */
    static FSceneRenderer CreateSceneRenderer(FSceneViewFamily InViewFamily/*, FHitProxyConsumer* HitProxyConsumer*/){

    }

    /** Setups FViewInfo::ViewRect according to ViewFamilly's ScreenPercentageInterface. */
    void PrepareViewRectsForRendering(){
//        check(IsInRenderingThread());

        // If not supporting screen percentage, bypass all computation.
        if (!ViewFamily.SupportsScreenPercentage())
        {
            // The base pass have to respect FSceneView::UnscaledViewRect.
            for (FViewInfo View : Views)
            {
                View.ViewRect.set(View.UnscaledViewRect);
            }

            ComputeFamilySize();
            return;
        }

//        TArray<FSceneViewScreenPercentageConfig> ViewScreenPercentageConfigs;
//        ViewScreenPercentageConfigs.Reserve(Views.Num());

        // Checks that view rects were still not initialized.
        for (FViewInfo View : Views)
        {
            // Make sure there was no attempt to configure ViewRect and screen percentage method before.
            UE4Engine.check(View.ViewRect.area() == 0);

            // Fallback to no anti aliasing.
            {
			    final boolean bWillApplyTemporalAA = /*GPostProcessing.AllowFullPostProcessing(View, FeatureLevel) ||*/
                        (View.bIsPlanarReflection && FeatureLevel >= ERHIFeatureLevel.SM4);

                if (!bWillApplyTemporalAA)
                {
                    // Disable anti-aliasing if we are not going to be able to apply final post process effects
                    View.AntiAliasingMethod = EAntiAliasingMethod.AAM_None;
                }
            }

//            FSceneViewScreenPercentageConfig Config;
//            ViewScreenPercentageConfigs.Add(FSceneViewScreenPercentageConfig());
        }

//        check(ViewFamily.ScreenPercentageInterface);
//        ViewFamily.ScreenPercentageInterface->ComputePrimaryResolutionFractions_RenderThread(ViewScreenPercentageConfigs);

//        check(ViewScreenPercentageConfigs.Num() == Views.Num());

        // Checks that view rects are correctly initialized.
        for (int i = 0; i < Views.length; i++)
        {
            FViewInfo View = Views[i];
            final float PrimaryResolutionFraction = 1; //ViewScreenPercentageConfigs[i].PrimaryResolutionFraction;

            // Ensure screen percentage show flag is respected. Prefer to check() rather rendering at a differen screen percentage
            // to make sure the renderer does not lie how a frame as been rendering to a dynamic resolution heuristic.
            /*if (!ViewFamily.EngineShowFlags.ScreenPercentage)
            {
                checkf(PrimaryResolutionFraction == 1.0f, TEXT("It is illegal to set ResolutionFraction != 1 if screen percentage show flag is disabled."));
            }*/

            // Make sure the screen percentage interface has not lied to the renderer about the upper bound.
            /*checkf(PrimaryResolutionFraction <= ViewFamily.GetPrimaryResolutionFractionUpperBound(),
                    TEXT("ISceneViewFamilyScreenPercentage::GetPrimaryResolutionFractionUpperBound() should not lie to the renderer."));
            check(FSceneViewScreenPercentageConfig::IsValidResolutionFraction(PrimaryResolutionFraction));

            check(FSceneViewScreenPercentageConfig::IsValidResolutionFraction(PrimaryResolutionFraction));*/

            // Compute final resolution fraction.
            float ResolutionFraction = PrimaryResolutionFraction * ViewFamily.SecondaryViewFraction;

            Vector2i ViewSize = ApplyResolutionFraction(ViewFamily, View.UnscaledViewRect.size(), ResolutionFraction);
            Vector2i ViewRectMin = QuantizeViewRectMin(new Vector2i(
                    (int)Math.ceil(View.UnscaledViewRect.x * ResolutionFraction),
                    (int)Math.ceil(View.UnscaledViewRect.y * ResolutionFraction)));

            // Use the bottom-left view rect if requested, instead of top-left
            if (/*CVarViewRectUseScreenBottom.GetValueOnRenderThread()*/true)
            {
                ViewRectMin.y = (int)Math.ceil( View.UnscaledViewRect.getMaxY() * ViewFamily.SecondaryViewFraction ) - ViewSize.y;
            }

//            View.ViewRect.Min = ViewRectMin;
//            View.ViewRect.Max = ViewRectMin + ViewSize;
            View.ViewRect.set(ViewRectMin.x, ViewRectMin.y,ViewSize.x, ViewSize.y);
		/*#if !UE_BUILD_SHIPPING
            // For testing purpose, override the screen percentage method.
            {
                switch (CVarTestPrimaryScreenPercentageMethodOverride.GetValueOnRenderThread())
                {
                    case 1: View.PrimaryScreenPercentageMethod = EPrimaryScreenPercentageMethod::SpatialUpscale; break;
                    case 2: View.PrimaryScreenPercentageMethod = EPrimaryScreenPercentageMethod::TemporalUpscale; break;
                    case 3: View.PrimaryScreenPercentageMethod = EPrimaryScreenPercentageMethod::RawOutput; break;
                }
            }
		#endif

            // Automatic screen percentage fallback.
            {
                // Tenmporal upsample is supported on SM5 only if TAA is turned on.
                if (View.PrimaryScreenPercentageMethod == EPrimaryScreenPercentageMethod::TemporalUpscale &&
                        (View.AntiAliasingMethod != AAM_TemporalAA ||
                                FeatureLevel < ERHIFeatureLevel::SM5 ||
                                ViewFamily.EngineShowFlags.VisualizeBuffer))
                {
                    View.PrimaryScreenPercentageMethod = EPrimaryScreenPercentageMethod::SpatialUpscale;
                }
            }

            check(View.ViewRect.Area() != 0);
            check(View.VerifyMembersChecks());*/
        }

        // Shifts all view rects layout to the top left corner of the buffers, since post processing will just output the final
        // views in FSceneViewFamily::RenderTarget whereever it was requested with FSceneView::UnscaledViewRect.
        {
//            FIntPoint TopLeftShift = Views[0].ViewRect.Min;
            int TopLeftShiftX = Views[0].ViewRect.x;
            int TopLeftShiftY = Views[0].ViewRect.y;
            for (int i = 1; i < Views.length; i++)
            {
                TopLeftShiftX = Math.min(TopLeftShiftX, Views[i].ViewRect.x);
                TopLeftShiftY = Math.min(TopLeftShiftY, Views[i].ViewRect.y);
            }
            for (int i = 0; i < Views.length; i++)
            {
                Views[i].ViewRect.x -= TopLeftShiftX;
                Views[i].ViewRect.y -= TopLeftShiftY;
            }
        }

//	#if !UE_BUILD_SHIPPING
        {
            int ViewRectOffset = 0; //CVarTestInternalViewRectOffset.GetValueOnRenderThread();

            if (Views.length == 1 && ViewRectOffset > 0)
            {
                FViewInfo View = Views[0];

                Vector2i DesiredBufferSize = GetDesiredInternalBufferSize(ViewFamily);
//                FIntPoint Offset = (DesiredBufferSize - View.ViewRect.Size()) / 2;
                int OffsetX = (DesiredBufferSize.x - View.ViewRect.width) / 2;
                int OffsetY = (DesiredBufferSize.y - View.ViewRect.height) / 2;
                Vector2i NewViewRectMin = new Vector2i();

                switch (ViewRectOffset)
                {
                    // Move to the center of the buffer.
                    case 1: NewViewRectMin.set(OffsetX, OffsetY); break;

                    // Move to top left.
                    case 2: break;

                    // Move to top right.
                    case 3: NewViewRectMin.set(2 * OffsetX, 0); break;

                    // Move to bottom right.
                    case 4: NewViewRectMin.set(0, 2 * OffsetY); break;

                    // Move to bottom left.
                    case 5: NewViewRectMin.set(2 * OffsetX, 2 * OffsetY); break;
                }

//                View.ViewRect += QuantizeViewRectMin(NewViewRectMin) - View.ViewRect.Min;

                int oldX = View.ViewRect.x;
                int oldY = View.ViewRect.y;
                RenderUtils.QuantizeSceneBufferSize(NewViewRectMin, DesiredBufferSize);

                View.ViewRect.x += DesiredBufferSize.x - View.ViewRect.x;
                View.ViewRect.y += DesiredBufferSize.y - View.ViewRect.y;

                UE4Engine.check(View.VerifyMembersChecks());
            }
        }
//	#endif

        ComputeFamilySize();

        // Notify StereoRenderingDevice about new ViewRects
        /*if (GEngine->StereoRenderingDevice.IsValid())
        {
            for (int32 i = 0; i < Views.Num(); i++)
            {
                FViewInfo& View = Views[i];
                GEngine->StereoRenderingDevice->SetFinalViewRect(View.StereoPass, View.ViewRect);
            }
        }*/
    }

    /** Setups each FViewInfo::GPUMask. */
    void ComputeViewGPUMasks(/*FRHIGPUMask RenderTargetGPUMask*/){ }

    /** Update the rendertarget with each view results.*/
    void DoCrossGPUTransfers(/*FRHICommandListImmediate& RHICmdList, FRHIGPUMask RenderTargetGPUMask*/){}

    boolean DoOcclusionQueries(int InFeatureLevel) {
        return InFeatureLevel >= ERHIFeatureLevel.ES3_1 /*&& CVarAllowOcclusionQueries.GetValueOnRenderThread() != 0*/;
    }
    /** Issues occlusion queries. */
    void BeginOcclusionTests(/*FRHICommandListImmediate& RHICmdList,*/ boolean bRenderQueries){

    }

    // fences to make sure the rhi thread has digested the occlusion query renders before we attempt to read them back async
//    static FGraphEventRef OcclusionSubmittedFence[FOcclusionQueryHelpers::MaxBufferedOcclusionFrames];
    /** Fences occlusion queries. */
    void FenceOcclusionTests(/*FRHICommandListImmediate& RHICmdList*/){

    }
    /** Waits for the occlusion fence. */
    void WaitOcclusionTests(/*FRHICommandListImmediate& RHICmdList*/){

    }

    boolean ShouldDumpMeshDrawCommandInstancingStats() { return bDumpMeshDrawCommandInstancingStats; }

    /** bound shader state for occlusion test prims */
//    static FGlobalBoundShaderState OcclusionTestBoundShaderState;

    /**
     * Whether or not to composite editor objects onto the scene as a post processing step
     *
     * @param View The view to test against
     *
     * @return true if compositing is needed
     */
    static boolean ShouldCompositeEditorPrimitives(FViewInfo2 View);

    /** the last thing we do with a scene renderer, lots of cleanup related to the threading **/
    static void WaitForTasksClearSnapshotsAndDeleteSceneRenderer(/*FRHICommandListImmediate& RHICmdList,*/ FSceneRenderer SceneRenderer, boolean bWaitForTasks /*= true*/);
    static void DelayWaitForTasksClearSnapshotsAndDeleteSceneRenderer(/*FRHICommandListImmediate& RHICmdList,*/ FSceneRenderer SceneRenderer);

    /** Apply the ResolutionFraction on ViewSize, taking into account renderer's requirements. */
    static Vector2i ApplyResolutionFraction(
		 FSceneViewFamily ViewFamily, Vector2i UnscaledViewSize, float ResolutionFraction){
        Vector2i ViewSize = new Vector2i();

        // CeilToInt so tha view size is at least 1x1 if ResolutionFraction == FSceneViewScreenPercentageConfig::kMinResolutionFraction.
        ViewSize.x = (int) Math.ceil(UnscaledViewSize.x * ResolutionFraction);
        ViewSize.y = (int) Math.ceil(UnscaledViewSize.y * ResolutionFraction);

        // Mosaic needs the viewport height to be a multiple of 2.
        if (ViewFamily.GetFeatureLevel() <= ERHIFeatureLevel.ES3_1 /*&& IsMobileHDRMosaic()*/)
        {
            ViewSize.y = ViewSize.y + (1 & ViewSize.y);
        }

//        check(ViewSize.GetMin() > 0);

        return ViewSize;
    }

    /** Quantize the ViewRect.Min according to various renderer's downscale requirements. */
    static Vector2i QuantizeViewRectMin(Vector2i ViewRectMin){
        Vector2i Out = new Vector2i();
        RenderUtils.QuantizeSceneBufferSize(ViewRectMin, Out);
        return Out;
    }

    /** Get the desired buffer size from the view family's ResolutionFraction upperbound.
     * Can be called on game thread or render thread.
     */
    static Vector2i GetDesiredInternalBufferSize(FSceneViewFamily ViewFamily){
        // If not supporting screen percentage, bypass all computation.
        if (!ViewFamily.SupportsScreenPercentage())
        {
            final Vector2i FamilySizeUpperBound = new Vector2i();

            for (FSceneView View : ViewFamily.Views)
            {
                FamilySizeUpperBound.x = Math.max(FamilySizeUpperBound.x, View.UnscaledViewRect.getMaxX());
                FamilySizeUpperBound.y = Math.max(FamilySizeUpperBound.y, View.UnscaledViewRect.getMaxY());
            }

            Vector2i DesiredBufferSize = new Vector2i();
            RenderUtils.QuantizeSceneBufferSize(FamilySizeUpperBound, DesiredBufferSize);
            return DesiredBufferSize;
        }

        float PrimaryResolutionFractionUpperBound = ViewFamily.GetPrimaryResolutionFractionUpperBound();

        // Compute final resolution fraction.
        float ResolutionFractionUpperBound = PrimaryResolutionFractionUpperBound * ViewFamily.SecondaryViewFraction;

        final Vector2i FamilySizeUpperBound = new Vector2i();

        for (FSceneView View : ViewFamily.Views)
        {
            Vector2i ViewSize = ApplyResolutionFraction(ViewFamily, View.UnconstrainedViewRect.size(), ResolutionFractionUpperBound);
            Vector2i ViewRectMin = QuantizeViewRectMin(new Vector2i(
                    (int)Math.ceil(View.UnconstrainedViewRect.x * ResolutionFractionUpperBound),
                    (int)Math.ceil(View.UnconstrainedViewRect.y * ResolutionFractionUpperBound)));

            FamilySizeUpperBound.x = Math.max(FamilySizeUpperBound.x, ViewRectMin.x + ViewSize.x);
            FamilySizeUpperBound.y = Math.max(FamilySizeUpperBound.y, ViewRectMin.y + ViewSize.y);
        }

        UE4Engine.check(FamilySizeUpperBound.getMin() > 0);

        Vector2i DesiredBufferSize = new Vector2i();
        RenderUtils.QuantizeSceneBufferSize(FamilySizeUpperBound, DesiredBufferSize);

/*#if !UE_BUILD_SHIPPING
        {
            // Increase the size of desired buffer size by 2 when testing for view rectangle offset.
            static const auto CVar = IConsoleManager::Get().FindTConsoleVariableDataInt(TEXT("r.Test.ViewRectOffset"));
            if (CVar->GetValueOnAnyThread() > 0)
            {
                DesiredBufferSize *= 2;
            }
        }
#endif*/

        return DesiredBufferSize;
    }

    /* Exposes renderer's privilege to fork view family's screen percentage interface.
    static ISceneViewFamilyScreenPercentage* ForkScreenPercentageInterface(
		const ISceneViewFamilyScreenPercentage* ScreenPercentageInterface, FSceneViewFamily& ForkedViewFamily)
    {
        return ScreenPercentageInterface->Fork_GameThread(ForkedViewFamily);
    }*/

    static int GetRefractionQuality(FSceneViewFamily ViewFamily);

    /** Size of the family. */
    protected final Vector2i FamilySize = new Vector2i();

    protected boolean bDumpMeshDrawCommandInstancingStats;

    // Shared functionality between all scene renderers

    protected void InitDynamicShadows(/*FRHICommandListImmediate& RHICmdList,*/ FGlobalDynamicIndexBuffer DynamicIndexBuffer, FGlobalDynamicVertexBuffer DynamicVertexBuffer, FGlobalDynamicReadBuffer DynamicReadBuffer);

    protected void SetupMeshPass(FViewInfo2 View, /*FExclusiveDepthStencil*/int BasePassDepthStencilAccess, FViewCommands ViewCommands);

    protected boolean RenderShadowProjections(/*FRHICommandListImmediate& RHICmdList,*/ FLightSceneInfo LightSceneInfo, Texture2D ScreenShadowMaskTexture, boolean bProjectingForForwardShading, boolean bMobileModulatedProjections);

    /** Finds a matching cached preshadow, if one exists. */
    protected FProjectedShadowInfo GetCachedPreshadow(
            FPrimitiveSceneProxy.FLightPrimitiveInteraction InParentInteraction,
            FProjectedShadowInitializer Initializer,
            FBoxSphereBounds Bounds,
        int InResolutionX);

    /** Creates a per object projected shadow for the given interaction. */
    protected void CreatePerObjectProjectedShadow(
//            FRHICommandListImmediate& RHICmdList,
            FLightPrimitiveInteraction* Interaction,
            bool bCreateTranslucentObjectShadow,
            bool bCreateInsetObjectShadow,
		const TArray<FProjectedShadowInfo*, SceneRenderingAllocator>& ViewDependentWholeSceneShadows,
            TArray<FProjectedShadowInfo*, SceneRenderingAllocator>& OutPreShadows);

    /** Creates shadows for the given interaction. */
    protected void SetupInteractionShadows(
            FRHICommandListImmediate& RHICmdList,
            FLightPrimitiveInteraction* Interaction,
            FVisibleLightInfo& VisibleLightInfo,
            boolean bStaticSceneOnly,
		const TArray<FProjectedShadowInfo*, SceneRenderingAllocator>& ViewDependentWholeSceneShadows,
            TArray<FProjectedShadowInfo*, SceneRenderingAllocator>& PreShadows);

    /** Generates FProjectedShadowInfos for all wholesceneshadows on the given light.*/
    protected void AddViewDependentWholeSceneShadowsForView(
            TArray<FProjectedShadowInfo*, SceneRenderingAllocator>& ShadowInfos,
            TArray<FProjectedShadowInfo*, SceneRenderingAllocator>& ShadowInfosThatNeedCulling,
            FVisibleLightInfo& VisibleLightInfo,
            FLightSceneInfo& LightSceneInfo);

    protected void AllocateShadowDepthTargets(/*FRHICommandListImmediate& RHICmdList*/);

    protected void AllocatePerObjectShadowDepthTargets(/*FRHICommandListImmediate& RHICmdList,*/ TArray<FProjectedShadowInfo*, SceneRenderingAllocator>& Shadows);

    protected void AllocateCachedSpotlightShadowDepthTargets(FRHICommandListImmediate& RHICmdList, TArray<FProjectedShadowInfo*, SceneRenderingAllocator>& CachedShadows);

    protected void AllocateCSMDepthTargets(FRHICommandListImmediate& RHICmdList, const TArray<FProjectedShadowInfo*, SceneRenderingAllocator>& WholeSceneDirectionalShadows);

    protected void AllocateRSMDepthTargets(FRHICommandListImmediate& RHICmdList, const TArray<FProjectedShadowInfo*, SceneRenderingAllocator>& RSMShadows);

    protected void AllocateOnePassPointLightDepthTargets(FRHICommandListImmediate& RHICmdList, const TArray<FProjectedShadowInfo*, SceneRenderingAllocator>& WholeScenePointShadows);

    protected void AllocateTranslucentShadowDepthTargets(FRHICommandListImmediate& RHICmdList, TArray<FProjectedShadowInfo*, SceneRenderingAllocator>& TranslucentShadows);

    /**
     * Used by RenderLights to figure out if projected shadows need to be rendered to the attenuation buffer.
     * Or to render a given shadowdepth map for forward rendering.
     *
     * @param LightSceneInfo Represents the current light
     * @return true if anything needs to be rendered
     */
    protected boolean CheckForProjectedShadows(FLightSceneInfo LightSceneInfo) const;

    /** Gathers the list of primitives used to draw various shadow types */
    protected void GatherShadowPrimitives(
		const TArray<FProjectedShadowInfo*, SceneRenderingAllocator>& PreShadows,
		const TArray<FProjectedShadowInfo*, SceneRenderingAllocator>& ViewDependentWholeSceneShadows,
        bool bReflectionCaptureScene);

    protected void RenderShadowDepthMaps(FRHICommandListImmediate& RHICmdList);
    protected void RenderShadowDepthMapAtlases(FRHICommandListImmediate& RHICmdList);

    /**
     * Creates a projected shadow for all primitives affected by a light.
     * @param LightSceneInfo - The light to create a shadow for.
     */
    protected void CreateWholeSceneProjectedShadow(FLightSceneInfo* LightSceneInfo, uint32& NumPointShadowCachesUpdatedThisFrame, uint32& NumSpotShadowCachesUpdatedThisFrame);

    /** Updates the preshadow cache, allocating new preshadows that can fit and evicting old ones. */
    protected void UpdatePreshadowCache(FSceneRenderTargets& SceneContext);

    /** Gets a readable light name for use with a draw event. */
    protected static void GetLightNameForDrawEvent(const FLightSceneProxy* LightProxy, FString& LightNameWithLevel);

    /** Gathers simple lights from visible primtives in the passed in views. */
    protected static void GatherSimpleLights(const FSceneViewFamily& ViewFamily, const TArray<FViewInfo2>& Views, FSimpleLightArray& SimpleLights);

    /** Splits the gathered simple lights into arrays based on which view they should be rendered in */
    protected static void SplitSimpleLightsByView(const FSceneViewFamily& ViewFamily, const TArray<FViewInfo2>& Views, const FSimpleLightArray& SimpleLights, FSimpleLightArray* SimpleLightsByView);

    /** Calculates projected shadow visibility. */
    protected void InitProjectedShadowVisibility(FRHICommandListImmediate& RHICmdList);

    /** Gathers dynamic mesh elements for all shadows. */
    protected void GatherShadowDynamicMeshElements(FGlobalDynamicIndexBuffer& DynamicIndexBuffer, FGlobalDynamicVertexBuffer& DynamicVertexBuffer, FGlobalDynamicReadBuffer& DynamicReadBuffer);

    /** Performs once per frame setup prior to visibility determination. */
    protected void PreVisibilityFrameSetup(FRHICommandListImmediate& RHICmdList);

    /** Computes which primitives are visible and relevant for each view. */
    protected void ComputeViewVisibility(FRHICommandListImmediate& RHICmdList, FExclusiveDepthStencil::Type BasePassDepthStencilAccess, FViewVisibleCommandsPerView& ViewCommandsPerView,
                               FGlobalDynamicIndexBuffer& DynamicIndexBuffer, FGlobalDynamicVertexBuffer& DynamicVertexBuffer, FGlobalDynamicReadBuffer& DynamicReadBuffer);

    /** Performs once per frame setup after to visibility determination. */
    protected void PostVisibilityFrameSetup(FILCUpdatePrimTaskData& OutILCTaskData);

    protected void GatherDynamicMeshElements(
            TArray<FViewInfo2>& InViews,
		const FScene* InScene,
		const FSceneViewFamily& InViewFamily,
            FGlobalDynamicIndexBuffer& DynamicIndexBuffer,
            FGlobalDynamicVertexBuffer& DynamicVertexBuffer,
            FGlobalDynamicReadBuffer& DynamicReadBuffer,
		const FPrimitiveViewMasks& HasDynamicMeshElementsMasks,
		const FPrimitiveViewMasks& HasDynamicEditorMeshElementsMasks,
		const FPrimitiveViewMasks& HasViewCustomDataMasks,
            FMeshElementCollector& Collector);

    /** Initialized the fog constants for each view. */
    protected void InitFogConstants();

    /** Returns whether there are translucent primitives to be rendered. */
    protected boolean ShouldRenderTranslucency(ETranslucencyPass::Type TranslucencyPass) const;

    /** TODO: REMOVE if no longer needed: Copies scene color to the viewport's render target after applying gamma correction. */
    protected void GammaCorrectToViewportRenderTarget(FRHICommandList& RHICmdList, const FViewInfo2* View, float OverrideGamma);

    /** Updates state for the end of the frame. */
    protected void RenderFinish(/*FRHICommandListImmediate& RHICmdList*/){
        /*if(FRCPassPostProcessBusyWait.IsPassRequired())  todo
        {
            // mostly view independent but to be safe we use the first view
            FViewInfo View = Views[0];

            FMemMark Mark(FMemStack::Get());
            FRenderingCompositePassContext CompositeContext(RHICmdList, View);

            FRenderingCompositeOutputRef BusyWait;
            {
                // for debugging purpose, can be controlled by console variable
                FRenderingCompositePass* Node = CompositeContext.Graph.RegisterPass(new(FMemStack::Get()) FRCPassPostProcessBusyWait());
                BusyWait = FRenderingCompositeOutputRef(Node);
            }

            if(BusyWait.IsValid())
            {
                CompositeContext.Process(BusyWait.GetPass(), TEXT("RenderFinish"));
            }
        }*/

//#if !(UE_BUILD_SHIPPING || UE_BUILD_TEST)
        {
            boolean bShowPrecomputedVisibilityWarning = false;
//            static const auto* CVarPrecomputedVisibilityWarning = IConsoleManager::Get().FindTConsoleVariableDataInt(TEXT("r.PrecomputedVisibilityWarning"));
//            if (CVarPrecomputedVisibilityWarning && CVarPrecomputedVisibilityWarning->GetValueOnRenderThread() == 1)
            {
                bShowPrecomputedVisibilityWarning = !bUsedPrecomputedVisibility;
            }

            boolean bShowGlobalClipPlaneWarning = false;

            if (Scene.PlanarReflections.size() > 0)
            {
//                static const auto* CVarClipPlane = IConsoleManager::Get().FindTConsoleVariableDataInt(TEXT("r.AllowGlobalClipPlane"));
//                if (CVarClipPlane && CVarClipPlane->GetValueOnRenderThread() == 0)
                {
                    bShowGlobalClipPlaneWarning = true;
                }
            }

            FReadOnlyCVARCache ReadOnlyCVARCache = Scene.ReadOnlyCVARCache;
//            static auto* CVarSkinCacheOOM = IConsoleManager::Get().FindTConsoleVariableDataFloat(TEXT("r.SkinCache.SceneMemoryLimitInMB"));

            /*uint64 GPUSkinCacheExtraRequiredMemory = 0;
            extern ENGINE_API bool IsGPUSkinCacheAvailable(EShaderPlatform Platform);
            if (IsGPUSkinCacheAvailable(ShaderPlatform))
            {
                if (FGPUSkinCache* SkinCache = Scene->GetGPUSkinCache())
                {
                    GPUSkinCacheExtraRequiredMemory = SkinCache->GetExtraRequiredMemoryAndReset();
                }
            }*/
		    final boolean bShowSkinCacheOOM = false; /* CVarSkinCacheOOM != nullptr && GPUSkinCacheExtraRequiredMemory > 0*/;

//            extern bool UseDistanceFieldAO();
            final boolean bShowDFAODisabledWarning = false; // !UseDistanceFieldAO() && (ViewFamily.EngineShowFlags.VisualizeMeshDistanceFields || ViewFamily.EngineShowFlags.VisualizeGlobalDistanceField || ViewFamily.EngineShowFlags.VisualizeDistanceFieldAO);

            final boolean bShowAtmosphericFogWarning = Scene.AtmosphericFog != null && !ReadOnlyCVARCache.bEnableAtmosphericFog;

            final boolean bStationarySkylight = Scene.SkyLight && Scene.SkyLight.bWantsStaticShadowing;
		    final boolean bShowSkylightWarning = bStationarySkylight && !ReadOnlyCVARCache.bEnableStationarySkylight;

		    final boolean bShowPointLightWarning = UsedWholeScenePointLightNames.Num() > 0 && !ReadOnlyCVARCache.bEnablePointLightShadows;
		    final boolean bShowShadowedLightOverflowWarning = Scene.OverflowingDynamicShadowedLights.Num() > 0;

            // Mobile-specific warnings
		    final boolean bMobile = (FeatureLevel <= ERHIFeatureLevel.ES3_1);
		    final boolean bShowMobileLowQualityLightmapWarning = bMobile && !ReadOnlyCVARCache.bEnableLowQualityLightmaps && ReadOnlyCVARCache.bAllowStaticLighting;
		    final boolean bShowMobileDynamicCSMWarning = bMobile && Scene->NumMobileStaticAndCSMLights_RenderThread > 0 && !(ReadOnlyCVARCache.bMobileEnableStaticAndCSMShadowReceivers && ReadOnlyCVARCache.bMobileAllowDistanceFieldShadows);
		    final boolean bShowMobileMovableDirectionalLightWarning = bMobile && Scene->NumMobileMovableDirectionalLights_RenderThread > 0 && !ReadOnlyCVARCache.bMobileAllowMovableDirectionalLights;

            boolean bMobileShowVertexFogWarning = false;
            if (bMobile && Scene.ExponentialFogs.Num() > 0)
            {
//                static const auto* CVarDisableVertexFog = IConsoleManager::Get().FindTConsoleVariableDataInt(TEXT("r.Mobile.DisableVertexFog"));
                if (UE4Engine.Mobile.DisableVertexFog /*&& CVarDisableVertexFog->GetValueOnRenderThread() != 0*/)
                {
                    bMobileShowVertexFogWarning = true;
                }
            }

		    final boolean bAnyWarning = bShowPrecomputedVisibilityWarning || bShowGlobalClipPlaneWarning || bShowAtmosphericFogWarning || bShowSkylightWarning || bShowPointLightWarning || bShowDFAODisabledWarning || bShowShadowedLightOverflowWarning || bShowMobileDynamicCSMWarning || bShowMobileLowQualityLightmapWarning || bShowMobileMovableDirectionalLightWarning || bMobileShowVertexFogWarning || bShowSkinCacheOOM;

            /*for(int ViewIndex = 0;ViewIndex < Views.length;ViewIndex++)
            {
                FViewInfo View = Views[ViewIndex];
                if (!View.bIsReflectionCapture && !View.bIsSceneCapture )
                {
                    // display a message saying we're frozen
                    FSceneViewState ViewState = (FSceneViewState)View.State;
                    boolean bViewParentOrFrozen = ViewState && (ViewState.HasViewParent() || ViewState.bIsFrozen);
                    boolean bLocked = View.bIsLocked;
                    if (bViewParentOrFrozen || bLocked || bAnyWarning)
                    {
                        SCOPED_CONDITIONAL_DRAW_EVENTF(RHICmdList, EventView, Views.Num() > 1, TEXT("View%d"), ViewIndex);

                        FRenderTargetTemp TempRenderTarget(View);

                        // create a temporary FCanvas object with the temp render target
                        // so it can get the screen size
                        int Y = 130;
                        FCanvas Canvas(&TempRenderTarget, NULL, View.Family->CurrentRealTime, View.Family->CurrentWorldTime, View.Family->DeltaWorldTime, FeatureLevel);
                        // Make sure draws to the canvas are not rendered upside down.
                        Canvas.SetAllowSwitchVerticalAxis(false);
                        if (bViewParentOrFrozen)
                        {
						const FText StateText =
                                ViewState->bIsFrozen ?
                                        NSLOCTEXT("SceneRendering", "RenderingFrozen", "Rendering frozen...")
                                        :
                                        NSLOCTEXT("SceneRendering", "OcclusionChild", "Occlusion Child");
                            Canvas.DrawShadowedText(10, Y, StateText, GetStatsFont(), FLinearColor(0.8, 1.0, 0.2, 1.0));
                            Y += 14;
                        }
                        if (bShowPrecomputedVisibilityWarning)
                        {
                            static const FText Message = NSLOCTEXT("Renderer", "NoPrecomputedVisibility", "NO PRECOMPUTED VISIBILITY");
                            Canvas.DrawShadowedText(10, Y, Message, GetStatsFont(), FLinearColor(1.0, 0.05, 0.05, 1.0));
                            Y += 14;
                        }
                        if (bShowGlobalClipPlaneWarning)
                        {
                            static const FText Message = NSLOCTEXT("Renderer", "NoGlobalClipPlane", "PLANAR REFLECTION REQUIRES GLOBAL CLIP PLANE PROJECT SETTING ENABLED TO WORK PROPERLY");
                            Canvas.DrawShadowedText(10, Y, Message, GetStatsFont(), FLinearColor(1.0, 0.05, 0.05, 1.0));
                            Y += 14;
                        }
                        if (bShowDFAODisabledWarning)
                        {
                            static const FText Message = NSLOCTEXT("Renderer", "DFAODisabled", "Distance Field AO is disabled through scalability");
                            Canvas.DrawShadowedText(10, Y, Message, GetStatsFont(), FLinearColor(1.0, 0.05, 0.05, 1.0));
                            Y += 14;
                        }

                        if (bShowAtmosphericFogWarning)
                        {
                            static const FText Message = NSLOCTEXT("Renderer", "AtmosphericFog", "PROJECT DOES NOT SUPPORT ATMOSPHERIC FOG");
                            Canvas.DrawShadowedText(10, Y, Message, GetStatsFont(), FLinearColor(1.0, 0.05, 0.05, 1.0));
                            Y += 14;
                        }
                        if (bShowSkylightWarning)
                        {
                            static const FText Message = NSLOCTEXT("Renderer", "Skylight", "PROJECT DOES NOT SUPPORT STATIONARY SKYLIGHT: ");
                            Canvas.DrawShadowedText(10, Y, Message, GetStatsFont(), FLinearColor(1.0, 0.05, 0.05, 1.0));
                            Y += 14;
                        }
                        if (bShowPointLightWarning)
                        {
                            static const FText Message = NSLOCTEXT("Renderer", "PointLight", "PROJECT DOES NOT SUPPORT WHOLE SCENE POINT LIGHT SHADOWS: ");
                            Canvas.DrawShadowedText(10, Y, Message, GetStatsFont(), FLinearColor(1.0, 0.05, 0.05, 1.0));
                            Y += 14;

                            for (FName LightName : UsedWholeScenePointLightNames)
                            {
                                Canvas.DrawShadowedText(10, Y, FText::FromString(LightName.ToString()), GetStatsFont(), FLinearColor(1.0, 0.05, 0.05, 1.0));
                                Y += 14;
                            }
                        }
                        if (bShowShadowedLightOverflowWarning)
                        {
                            static const FText Message = NSLOCTEXT("Renderer", "ShadowedLightOverflow", "TOO MANY OVERLAPPING SHADOWED MOVABLE LIGHTS, SHADOW CASTING DISABLED: ");
                            Canvas.DrawShadowedText(10, Y, Message, GetStatsFont(), FLinearColor(1.0, 0.05, 0.05, 1.0));
                            Y += 14;

                            for (FName LightName : Scene->OverflowingDynamicShadowedLights)
                            {
                                Canvas.DrawShadowedText(10, Y, FText::FromString(LightName.ToString()), GetStatsFont(), FLinearColor(1.0, 0.05, 0.05, 1.0));
                                Y += 14;
                            }
                        }
                        if (bShowMobileLowQualityLightmapWarning)
                        {
                            static const FText Message = NSLOCTEXT("Renderer", "MobileLQLightmap", "MOBILE PROJECTS SUPPORTING STATIC LIGHTING MUST HAVE LQ LIGHTMAPS ENABLED");
                            Canvas.DrawShadowedText(10, Y, Message, GetStatsFont(), FLinearColor(1.0, 0.05, 0.05, 1.0));
                            Y += 14;
                        }
                        if (bShowMobileMovableDirectionalLightWarning)
                        {
                            static const FText Message = NSLOCTEXT("Renderer", "MobileMovableDirectional", "PROJECT HAS MOVABLE DIRECTIONAL LIGHTS ON MOBILE DISABLED");
                            Canvas.DrawShadowedText(10, Y, Message, GetStatsFont(), FLinearColor(1.0, 0.05, 0.05, 1.0));
                            Y += 14;
                        }
                        if (bShowMobileDynamicCSMWarning)
                        {
                            static const FText Message = (!ReadOnlyCVARCache.bMobileEnableStaticAndCSMShadowReceivers)
                                ? NSLOCTEXT("Renderer", "MobileDynamicCSM", "PROJECT HAS MOBILE CSM SHADOWS FROM STATIONARY DIRECTIONAL LIGHTS DISABLED")
                                : NSLOCTEXT("Renderer", "MobileDynamicCSMDistFieldShadows", "MOBILE CSM+STATIC REQUIRES DISTANCE FIELD SHADOWS ENABLED FOR PROJECT");
                            Canvas.DrawShadowedText(10, Y, Message, GetStatsFont(), FLinearColor(1.0, 0.05, 0.05, 1.0));
                            Y += 14;
                        }
                        if (bMobileShowVertexFogWarning)
                        {
                            static const FText Message = NSLOCTEXT("Renderer", "MobileVertexFog", "PROJECT HAS VERTEX FOG ON MOBILE DISABLED");
                            Canvas.DrawShadowedText(10, Y, Message, GetStatsFont(), FLinearColor(1.0, 0.05, 0.05, 1.0));
                            Y += 14;
                        }

                        if (bShowSkinCacheOOM)
                        {
                            FString String = FString::Printf(TEXT("OUT OF MEMORY FOR SKIN CACHE, REQUIRES %.3f extra MB (currently at %.3f)"), (float)GPUSkinCacheExtraRequiredMemory / 1048576.0f, CVarSkinCacheOOM->GetValueOnAnyThread());
                            Canvas.DrawShadowedText(10, Y, FText::FromString(String), GetStatsFont(), FLinearColor(1.0, 0.05, 0.05, 1.0));
                            Y += 14;
                        }

                        if (bLocked)
                        {
                            static const FText Message = NSLOCTEXT("Renderer", "ViewLocked", "VIEW LOCKED");
                            Canvas.DrawShadowedText(10, Y, Message, GetStatsFont(), FLinearColor(0.8, 1.0, 0.2, 1.0));
                            Y += 14;
                        }
                        Canvas.Flush_RenderThread(RHICmdList);
                    }

                    // Software occlusion debug draw
                    if (ViewState && ViewState->SceneSoftwareOcclusion)
                    {
                        ViewState->SceneSoftwareOcclusion->DebugDraw(RHICmdList, View, 20, 20);
                    }
                }
            }*/
        }

//#endif //!(UE_BUILD_SHIPPING || UE_BUILD_TEST)

        // Save the post-occlusion visibility stats for the frame and freezing info
        for(int ViewIndex = 0;ViewIndex < Views.length;ViewIndex++)
        {
            FViewInfo View = Views[ViewIndex];
//            INC_DWORD_STAT_BY(STAT_VisibleStaticMeshElements,View.NumVisibleStaticMeshElements);
//            INC_DWORD_STAT_BY(STAT_VisibleDynamicPrimitives,View.NumVisibleDynamicPrimitives);

//#if !(UE_BUILD_SHIPPING || UE_BUILD_TEST)
            // update freezing info
            FSceneViewState ViewState = (FSceneViewState)View.State;
            if (ViewState != null)
            {
                // if we're finished freezing, now we are frozen
                if (ViewState.bIsFreezing)
                {
                    ViewState.bIsFreezing = false;
                    ViewState.bIsFrozen = true;
                    ViewState.bIsFrozenViewMatricesCached = true;
                    ViewState.CachedViewMatrices = View.ViewMatrices;
                }

                // handle freeze toggle request
                if (bHasRequestedToggleFreeze)
                {
                    // do we want to start freezing or stop?
                    ViewState.bIsFreezing = !ViewState.bIsFrozen;
                    ViewState.bIsFrozen = false;
                    ViewState.bIsFrozenViewMatricesCached = false;
                    ViewState.FrozenPrimitives.Empty();
                }
            }
//#endif
        }

//#if !(UE_BUILD_SHIPPING || UE_BUILD_TEST)
        // clear the commands
        bHasRequestedToggleFreeze = false;

        /*if(ViewFamily.EngineShowFlags.OnScreenDebug)
        {
            for(int32 ViewIndex = 0;ViewIndex < Views.Num();ViewIndex++)
            {
			const FViewInfo& View = Views[ViewIndex];

                if(!View.IsPerspectiveProjection())
                {
                    continue;
                }

                FVisualizeTexturePresent::PresentContent(RHICmdList, View);
            }
        }*/
//#endif

        {
            /*SCOPE_CYCLE_COUNTER(STAT_FDeferredShadingSceneRenderer_ViewExtensionPostRenderView);
            for(int ViewExt = 0; ViewExt < ViewFamily.ViewExtensions.Num(); ++ViewExt)
            {
                ViewFamily.ViewExtensions[ViewExt]->PostRenderViewFamily_RenderThread(RHICmdList, ViewFamily);
                for(int32 ViewIndex = 0; ViewIndex < ViewFamily.Views.Num(); ++ViewIndex)
                {
                    ViewFamily.ViewExtensions[ViewExt]->PostRenderView_RenderThread(RHICmdList, Views[ViewIndex]);
                }
            }*/
        }

        // Notify the RHI we are done rendering a scene.
        /*RHICmdList.EndScene();

        if (GDumpMeshDrawCommandMemoryStats)
        {
            GDumpMeshDrawCommandMemoryStats = 0;
            Scene.DumpMeshDrawCommandMemoryStats();
        }*/
    }

    protected void RenderCustomDepthPassAtLocation(FRHICommandListImmediate& RHICmdList, int32 Location);
    protected void RenderCustomDepthPass(FRHICommandListImmediate& RHICmdList);

    protected void OnStartRender(FRHICommandListImmediate& RHICmdList);

    /** Renders the scene's distortion */
    protected void RenderDistortion(FRHICommandListImmediate& RHICmdList);

    /** Returns the scene color texture multi-view is targeting. */
    FTextureRHIParamRef GetMultiViewSceneColor(const FSceneRenderTargets& SceneContext) const;

    protected void UpdatePrimitiveIndirectLightingCacheBuffers();
    protected void ClearPrimitiveSingleFrameIndirectLightingCacheBuffers();

    protected void RenderPlanarReflection(class FPlanarReflectionSceneProxy* ReflectionSceneProxy);

    protected void ResolveSceneColor(FRHICommandList& RHICmdList);

    private void ComputeFamilySize(){
        UE4Engine.check(FamilySize.x == 0);
//        check(IsInRenderingThread());

        // Calculate the screen extents of the view family.
        boolean bInitializedExtents = false;
        float MaxFamilyX = 0;
        float MaxFamilyY = 0;

        for (FViewInfo View : Views)
        {
            float FinalViewMaxX = (float)View.ViewRect.getMaxX();
            float FinalViewMaxY = (float)View.ViewRect.getMaxY();

            // Derive the amount of scaling needed for screenpercentage from the scaled / unscaled rect
		const float XScale = FinalViewMaxX / (float)View.UnscaledViewRect.getMaxX();
		const float YScale = FinalViewMaxY / (float)View.UnscaledViewRect.getMaxY();

            if (!bInitializedExtents)
            {
                // Note: using the unconstrained view rect to compute family size
                // In the case of constrained views (black bars) this means the scene render targets will fill the whole screen
                // Which is needed for ES2 paths where we render directly to the backbuffer, and the scene depth buffer has to match in size
                MaxFamilyX = View.UnconstrainedViewRect.getMaxX() * XScale;
                MaxFamilyY = View.UnconstrainedViewRect.getMaxY() * YScale;
                bInitializedExtents = true;
            }
            else
            {
                MaxFamilyX = Math.max(MaxFamilyX, View.UnconstrainedViewRect.getMaxX() * XScale);
                MaxFamilyY = Math.max(MaxFamilyY, View.UnconstrainedViewRect.getMaxY() * YScale);
            }

            // floating point imprecision could cause MaxFamilyX to be less than View->ViewRect.Max.X after integer truncation.
            // since this value controls rendertarget sizes, we don't want to create rendertargets smaller than the view size.
            MaxFamilyX = Math.max(MaxFamilyX, FinalViewMaxX);
            MaxFamilyY = Math.max(MaxFamilyY, FinalViewMaxY);

            /*if (!IStereoRendering::IsAnAdditionalView(View.StereoPass, GEngine->StereoRenderingDevice))
            {
                InstancedStereoWidth = FPlatformMath::Max(InstancedStereoWidth, static_cast<uint32>(View.ViewRect.Max.X));
            }*/
        }

        // We render to the actual position of the viewports so with black borders we need the max.
        // We could change it by rendering all to left top but that has implications for splitscreen.
        FamilySize.x = (int)Math.floor(MaxFamilyX);
        FamilySize.y = (int)Math.floor(MaxFamilyY);

        UE4Engine.check(FamilySize.x != 0);
        UE4Engine.check(bInitializedExtents);
    }
}
