package jet.opengl.renderer.Unreal4.scenes;

import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.renderer.Unreal4.ESceneCaptureCompositeMode;
import jet.opengl.renderer.Unreal4.ESceneCaptureSource;
import jet.opengl.renderer.Unreal4.FSceneRenderer;
import jet.opengl.renderer.Unreal4.api.EShaderPlatform;
import jet.opengl.renderer.Unreal4.views.ESecondaryScreenPercentageMethod;
import jet.opengl.renderer.Unreal4.views.EViewModeIndex;

/**
 * A set of views into a scene which only have different view transforms and owner actors.
 */
public class FSceneViewFamily {

    /**
     * Helper struct for creating FSceneViewFamily instances
     * If created with specifying a time it will retrieve them from the world in the given scene.
     *
     * param InRenderTarget		The render target which the views are being rendered to.
     * param InScene			The scene being viewed.
     * param InShowFlags		The show flags for the views.
     *
     */
    public static class ConstructionValues{
/** The views which make up the family. */
		public TextureGL RenderTarget;

        /** The render target which the views are being rendered to. */
        public FSceneInterface Scene;

        /* The engine show flags for the views.
        FEngineShowFlags EngineShowFlags;*/

        /** Additional view params related to the current viewmode (example : texcoord index) */
        public int ViewModeParam;
        /** An name bound to the current viewmode param. (example : texture name) */
        public String ViewModeParamName;

        /** The current world time. */
        public float CurrentWorldTime;

        /** The difference between the last world time and CurrentWorldTime. */
        public float DeltaWorldTime;

        /** The current real time. */
        public float CurrentRealTime;

        /** Gamma correction used when rendering this family. Default is 1.0 */
        public float GammaCorrection;

        /** Indicates whether the view family is updated in real-time. */
        public boolean bRealtimeUpdate;

        /** Used to defer the back buffer clearing to just before the back buffer is drawn to */
        public boolean bDeferClear;

        /** If true then results of scene rendering are copied/resolved to the RenderTarget. */
        public boolean bResolveScene;

        /** Safety check to ensure valid times are set either from a valid world/scene pointer or via the SetWorldTimes function */
        public boolean bTimesSet;

        /** Set the world time ,difference between the last world time and CurrentWorldTime and current real time. */
        public ConstructionValues SetWorldTimes(float InCurrentWorldTime,float InDeltaWorldTime,float InCurrentRealTime) { CurrentWorldTime = InCurrentWorldTime; DeltaWorldTime = InDeltaWorldTime; CurrentRealTime = InCurrentRealTime;bTimesSet = true;return this; }

        /** Set  whether the view family is updated in real-time. */
        public ConstructionValues SetRealtimeUpdate(boolean Value) { bRealtimeUpdate = Value; return this; }

        /** Set whether to defer the back buffer clearing to just before the back buffer is drawn to */
        public ConstructionValues SetDeferClear(boolean Value) { bDeferClear = Value; return this; }

        /** Setting to if true then results of scene rendering are copied/resolved to the RenderTarget. */
        public ConstructionValues SetResolveScene(boolean Value) { bResolveScene = Value; return this; }

        /** Set Gamma correction used when rendering this family. */
        public ConstructionValues SetGammaCorrection(float Value) { GammaCorrection = Value; return this; }

        /** Set the view param. */
        public ConstructionValues SetViewModeParam(int InViewModeParam, String InViewModeParamName) { ViewModeParam = InViewModeParam; ViewModeParamName = InViewModeParamName; return this; }

    }

        /** The views which make up the family. */
        public FSceneView[] Views;

        /** View mode of the family. */
        public EViewModeIndex ViewMode;

        /** The render target which the views are being rendered to. */
        public TextureGL RenderTarget;

        /** The scene being viewed. */
        public FSceneInterface Scene;

        /** The new show flags for the views (meant to replace the old system).
        FEngineShowFlags EngineShowFlags;*/

        /** The current world time. */
        public float CurrentWorldTime;

        /** The difference between the last world time and CurrentWorldTime. */
        public float DeltaWorldTime;

        /** The current real time. */
        public float CurrentRealTime;

        /** Copy from main thread GFrameNumber to be accessible on render thread side. UINT_MAX before CreateSceneRenderer() or BeginRenderingViewFamily() was called */
        public int FrameNumber;

        /** Indicates whether the view family is updated in realtime. */
        public boolean bRealtimeUpdate;

        /** Used to defer the back buffer clearing to just before the back buffer is drawn to */
        public boolean bDeferClear;

        /** if true then results of scene rendering are copied/resolved to the RenderTarget. */
        public boolean bResolveScene;

        /** if true then each view is not rendered using the same GPUMask. */
        public boolean bMultiGPUForkAndJoin;

        /**
         * Which component of the scene rendering should be output to the final render target.
         * If SCS_FinalColorLDR this indicates do nothing.
         */
        public ESceneCaptureSource SceneCaptureSource;

        /** When enabled, the scene capture will composite into the render target instead of overwriting its contents. */
        public ESceneCaptureCompositeMode SceneCaptureCompositeMode;

        /**
         * GetWorld->IsPaused() && !Simulate
         * Simulate is excluded as the camera can move which invalidates motionblur
         */
        public boolean bWorldIsPaused;

        /** When enabled, the post processing will output in HDR space */
        public boolean bIsHDR;

        /** Gamma correction used when rendering this family. Default is 1.0 */
        public float GammaCorrection;

        /** Editor setting to allow designers to override the automatic expose. 0:Automatic, following indices: -4 .. +4
        FExposureSettings ExposureSettings;*/

        /** Extensions that can modify view parameters on the render thread.
        TArray<TSharedRef<class ISceneViewExtension, ESPMode::ThreadSafe> > ViewExtensions;*/

        // for r.DisplayInternals (allows for easy passing down data from main to render thread)
        //FDisplayInternalsData DisplayInternalsData;

        /**
         * Secondary view fraction to support High DPI monitor still with same primary screen percentage range for temporal
         * upscale to test content consistently in editor no mater of the HighDPI scale.
         */
        public float SecondaryViewFraction;
        public ESecondaryScreenPercentageMethod SecondaryScreenPercentageMethod;

        // Override the LOD of landscape in this viewport
        public int LandscapeLODOverride;

        /** Indicates whether, or not, the base attachment volume should be drawn. */
        public boolean bDrawBaseInfo;

        /**
         * Indicates whether the shader world space position should be forced to 0. Also sets the view vector to (0,0,1) for all pixels.
         * This is used in the texture streaming build when computing material tex coords scale.
         * Because the material are rendered in tiles, there is no actual valid mapping for world space position.
         * World space mapping would require to render mesh with the level transforms to be valid.
         */
        public boolean bNullifyWorldSpacePosition;

        /** Initialization constructor. */
        public FSceneViewFamily( ConstructionValues CVS ){
                throw new UnsupportedOperationException();
        }
//	    ~FSceneViewFamily();

        public int GetFeatureLevel() {
                throw new UnsupportedOperationException();
        }

        EShaderPlatform GetShaderPlatform() { return FSceneRenderer.GShaderPlatformForFeatureLevel[GetFeatureLevel()]; }

//        EDebugViewShaderMode DebugViewShaderMode;
        public int ViewModeParam;
        public String ViewModeParamName;

        public boolean bUsedDebugViewVSDSHS;
//        FORCEINLINE EDebugViewShaderMode GetDebugViewShaderMode() const { return DebugViewShaderMode; }
        public int GetViewModeParam() { return ViewModeParam; }
        public String GetViewModeParamName() { return ViewModeParamName; }
//        EDebugViewShaderMode ChooseDebugViewShaderMode() const;
        public boolean UseDebugViewVSDSHS()  { return bUsedDebugViewVSDSHS; }
        public boolean UseDebugViewPS() { return /*DebugViewShaderMode != DVSM_None*/false; }

        /** Returns the appropriate view for a given eye in a stereo pair.
        public FSceneView GetStereoEyeView(const EStereoscopicPass Eye) const;*/

        /** Returns whether the screen percentage show flag is supported or not for this view family. */
        public boolean SupportsScreenPercentage() {
                return false;
        }

        public boolean AllowTranslucencyAfterDOF() {
                throw new UnsupportedOperationException();
        }

        /* Returns the maximum FSceneViewScreenPercentageConfig::PrimaryResolutionFraction. */
        public float GetPrimaryResolutionFractionUpperBound()
        {
                /*UE4Engine.check(ScreenPercentageInterface != nullptr);
                float PrimaryUpperBoundFraction = ScreenPercentageInterface->GetPrimaryResolutionFractionUpperBound();

                checkf(FSceneViewScreenPercentageConfig::IsValidResolutionFraction(PrimaryUpperBoundFraction),
                TEXT("ISceneViewFamilyScreenPercentage::GetPrimaryResolutionFractionUpperBound()")
                TEXT(" should return a valide value."));

                if (!EngineShowFlags.ScreenPercentage)
                {
                        checkf(PrimaryUpperBoundFraction >= 1.0f,
                                TEXT("ISceneViewFamilyScreenPercentage::GetPrimaryResolutionFractionUpperBound()")
                                TEXT(" should return >= 1 if screen percentage show flag is off."));
                }

                return PrimaryUpperBoundFraction;*/

                return 1;
        }

        /*FORCEINLINE const ISceneViewFamilyScreenPercentage* GetScreenPercentageInterface() const
        {
                return ScreenPercentageInterface;
        }*/

        /**
         * Safely sets the view family's screen percentage interface.
         * This is meant to be set by one of the ISceneViewExtension::BeginRenderViewFamily(). And collision will
         * automatically be detected. If no extension sets it, that is fine since the renderer is going to use an
         * internal default one.
         *
         * The renderer reserves the right to delete and replace the view family's screen percentage interface
         * for testing purposes with the r.Test.OverrideScreenPercentageInterface CVar.

        public void SetScreenPercentageInterface(ISceneViewFamilyScreenPercentage* InScreenPercentageInterface)
        {
                check(InScreenPercentageInterface);
                checkf(ScreenPercentageInterface == nullptr, TEXT("View family already had a screen percentage interface assigned."));
                ScreenPercentageInterface = InScreenPercentageInterface;
        }*/


        // Allow moving view family as long as no screen percentage interface are set.
        /*FSceneViewFamily(const FSceneViewFamily&& InViewFamily)
		: FSceneViewFamily(static_cast<const FSceneViewFamily&>(InViewFamily))
        {
                check(ScreenPercentageInterface == nullptr);
        }

        // Interface to handle screen percentage of the views of the family.
        ISceneViewFamilyScreenPercentage* ScreenPercentageInterface;

        // Only FSceneRenderer can copy a view family.
        FSceneViewFamily(const FSceneViewFamily&) = default;*/

//        friend class FSceneRenderer;
}
