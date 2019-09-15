package jet.opengl.renderer.Unreal4.scenes;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.ReadableVector4f;
import org.lwjgl.util.vector.Transform;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector2i;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.util.ArrayList;
import java.util.HashSet;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.util.Recti;
import jet.opengl.postprocessing.util.StackBool;
import jet.opengl.renderer.Unreal4.FForwardLightingViewResources;
import jet.opengl.renderer.Unreal4.FPrimitiveComponentId;
import jet.opengl.renderer.Unreal4.UE4Engine;
import jet.opengl.renderer.Unreal4.api.EShaderPlatform;
import jet.opengl.renderer.Unreal4.editor.FPostProcessSettings;
import jet.opengl.renderer.Unreal4.utils.FConvexVolume;
import jet.opengl.renderer.Unreal4.utils.FMirrorMatrix;
import jet.opengl.renderer.Unreal4.utils.FPlane;
import jet.opengl.renderer.Unreal4.utils.TBitArray;
import jet.opengl.renderer.Unreal4.views.FViewElementDrawer;
import jet.opengl.renderer.Unreal4.views.FViewMatrices;
import jet.opengl.renderer.Unreal4.views.FViewUniformShaderParameters;

/**
 * A projection from scene space into a 2D screen region.
 */
public class FSceneView {

    public FSceneViewFamily Family;
    /** can be 0 (thumbnail rendering) */
    public FSceneViewStateInterface State;

    /** The uniform buffer for the view's parameters. This is only initialized in the rendering thread's copies of the FSceneView. FViewUniformShaderParameters*/
    public BufferGL ViewUniformBuffer;

    /** Mobile Directional Lighting uniform buffers, one for each lighting channel
     * The first is used for primitives with no lighting channels set.
     * Only initialized in the rendering thread's copies of the FSceneView. FMobileDirectionalLightShaderParameters
     */
    public final BufferGL[] MobileDirectionalLightUniformBuffers = new BufferGL[UE4Engine.NUM_LIGHTING_CHANNELS+1];

    /** During GetDynamicMeshElements this will be the correct cull volume for shadow stuff */
	private FConvexVolume DynamicMeshElementsShadowCullFrustum ;
    /** If the above is non-null, a translation that is applied to world-space before transforming by one of the shadow matrices. */
    private  final Vector3f PreShadowTranslation = new Vector3f();

    public final FSceneViewInitOptions SceneViewInitOptions = new FSceneViewInitOptions();

    /** The actor which is being viewed from.
	const AActor* ViewActor;*/

    /** Player index this view is associated with or INDEX_NONE. */
    public int PlayerIndex;

    /** An interaction which draws the view's interaction elements. */
    public FViewElementDrawer Drawer;

    /* Final position of the view in the final render target (in pixels), potentially constrained by an aspect ratio requirement (black bars) */
	public final Recti UnscaledViewRect = new Recti();

    /* Raw view size (in pixels), used for screen space calculations */
    public final Recti UnconstrainedViewRect = new Recti();

    /** Maximum number of shadow cascades to render with. */
    public int MaxShadowCascades;

    public FViewMatrices ViewMatrices;

    /** Variables used to determine the view matrix */
    public final Vector3f		ViewLocation = new Vector3f();
    public final Vector3f	ViewRotation = new Vector3f();
    public final Quaternion BaseHmdOrientation = new Quaternion();
    public final Vector3f		BaseHmdLocation = new Vector3f();
    public float		WorldToMetersScale;
//    TOptional<FTransform> PreviousViewTransform;
    public final Transform PreviousViewTransform = new Transform();

    // normally the same as ViewMatrices unless "r.Shadow.FreezeCamera" is activated
    public FViewMatrices ShadowViewMatrices;

    public final Matrix4f ProjectionMatrixUnadjustedForRHI = new Matrix4f();

    public final Vector4f BackgroundColor = new Vector4f();
    public final Vector4f OverlayColor = new Vector4f();

    /** Color scale multiplier used during post processing */
    public final Vector4f ColorScale = new Vector4f();

    /** For stereoscopic rendering, whether or not this is a full pass, or a left / right eye pass
    EStereoscopicPass StereoPass;*/

    /** Half of the view's stereo IPD (- for lhs, + for rhs) */
    public float StereoIPD;

    /** Whether this view should render the first instance only of any meshes using instancing. */
    public boolean bRenderFirstInstanceOnly;

    // Whether to use FOV when computing mesh LOD.
    public boolean bUseFieldOfViewForLOD;

    /** Actual field of view and that desired by the camera originally */
    public float FOV;
    public float DesiredFOV;

//    EDrawDynamicFlags::Type DrawDynamicFlags;

    /** Current buffer visualization mode */
    public String CurrentBufferVisualizationMode;

//#if WITH_EDITOR
    /* Whether to use the pixel inspector */
    public boolean bUsePixelInspector;
//#endif //WITH_EDITOR

    /**
     * These can be used to override material parameters across the scene without recompiling shaders.
     * The last component is how much to include of the material's value for that parameter, so 0 will completely remove the material's value.
     */
    public final Vector4f DiffuseOverrideParameter = new Vector4f();
    public final Vector4f SpecularOverrideParameter = new Vector4f();
    public final Vector4f NormalOverrideParameter = new Vector4f();
    public final Vector2f RoughnessOverrideParameter = new Vector2f();

    /** The primitives which are hidden for this view. */
    public final HashSet<FPrimitiveComponentId> HiddenPrimitives = new HashSet<>();

    /** The primitives which are visible for this view. If the array is not empty, all other primitives will be hidden.
    TOptional<TSet<FPrimitiveComponentId>> ShowOnlyPrimitives;*/

    // Derived members.

    public boolean bAllowTemporalJitter;

    public final FConvexVolume ViewFrustum = new FConvexVolume();

    public boolean bHasNearClippingPlane;

    public final FPlane NearClippingPlane = new FPlane();

    public float NearClippingDistance;

    /** true if ViewMatrix.Determinant() is negative. */
    public boolean bReverseCulling;

    /* Vector used by shaders to convert depth buffer samples into z coordinates in world space */
    public final Vector4f InvDeviceZToWorldZTransform = new Vector4f();

    /** World origin offset value. Non-zero only for a single frame when origin is rebased */
    public final Vector4f OriginOffsetThisFrame = new Vector4f();

    /** FOV based multiplier for cull distance on objects */
    public float LODDistanceFactor;
    /** Square of the FOV based multiplier for cull distance on objects */
    public float LODDistanceFactorSquared;

    /** Whether we did a camera cut for this view this frame. */
    public boolean bCameraCut;

    // -1,-1 if not setup
    public final Vector2i CursorPos = new Vector2i(-1,-1);

    /** True if this scene was created from a game world. */
    public boolean bIsGameView;

    /** For sanity checking casts that are assumed to be safe. */
    public boolean bIsViewInfo;

    /** Whether this view is being used to render a scene capture. */
    public boolean bIsSceneCapture;

    /** Whether this view is being used to render a reflection capture. */
    public boolean bIsReflectionCapture;

    /** Whether this view is being used to render a planar reflection. */
    public boolean bIsPlanarReflection;

    /** Whether to force two sided rendering for this view. */
    public boolean bRenderSceneTwoSided;

    /** Whether this view was created from a locked viewpoint. */
    public boolean bIsLocked;

    /**
     * Whether to only render static lights and objects.
     * This is used when capturing the scene for reflection captures, which aren't updated at runtime.
     */
    public boolean bStaticSceneOnly;

    /** True if instanced stereo is enabled. */
    public boolean bIsInstancedStereoEnabled;

    /** True if multi-view is enabled. */
    public boolean bIsMultiViewEnabled;

    /** True if mobile multi-view is enabled. */
    public boolean bIsMobileMultiViewEnabled;

    /** True if mobile multi-view direct is enabled. */
    public boolean bIsMobileMultiViewDirectEnabled;

    /** True if we need to bind the instanced view uniform buffer parameters. */
    public boolean bShouldBindInstancedViewUB;

    /** Global clipping plane being applied to the scene, or all 0's if disabled.  This is used when rendering the planar reflection pass. */
    FPlane GlobalClippingPlane;

    /** Aspect ratio constrained view rect. In the editor, when attached to a camera actor and the camera black bar showflag is enabled, the normal viewrect
     * remains as the full viewport, and the black bars are just simulated by drawing black bars. This member stores the effective constrained area within the
     * bars.
     **/
    public final Recti CameraConstrainedViewRect = new Recti();

    /** Sort axis for when TranslucentSortPolicy is SortAlongAxis */
    public final Vector3f TranslucentSortAxis = new Vector3f();

    /** Translucent sort mode
    TEnumAsByte<ETranslucentSortPolicy::Type> TranslucentSortPolicy;*/

//#if WITH_EDITOR
    /** The set of (the first 64) groups' visibility info for this view */
    public long EditorViewBitflag;

    /** For ortho views, this can control how to determine LOD parenting (ortho has no "distance-to-camera") */
    public final Vector3f OverrideLODViewOrigin = new Vector3f();

    /** True if we should draw translucent objects when rendering hit proxies */
    public boolean bAllowTranslucentPrimitivesInHitProxy;

    /** BitArray representing the visibility state of the various sprite categories in the editor for this view */
    public final TBitArray SpriteCategoryVisibility = new TBitArray();
    /** Selection color for the editor (used by post processing) */
    public final Vector4f SelectionOutlineColor = new Vector4f();
    /** Selection color for use in the editor with inactive primitives */
    public final Vector4f SubduedSelectionOutlineColor = new Vector4f();
    /** True if any components are selected in isolation (independent of actor selection) */
    public boolean bHasSelectedComponents;
//#endif

    /**
     * The final settings for the current viewer position (blended together from many volumes).
     * Setup by the main thread, passed to the render thread and never touched again by the main thread.

    FFinalPostProcessSettings FinalPostProcessSettings;*/

    // The antialiasing method.
    public EAntiAliasingMethod AntiAliasingMethod;

    // Primary screen percentage method to use.
    public EPrimaryScreenPercentageMethod PrimaryScreenPercentageMethod;

    /** Parameters for atmospheric fog. */
    public TextureGL AtmosphereTransmittanceTexture;
    public TextureGL AtmosphereIrradianceTexture;
    public TextureGL AtmosphereInscatterTexture;

    /** Points to the view state's resources if a view state exists. */
    public FForwardLightingViewResources ForwardLightingResources;

    /** Feature level for this scene */
	public int FeatureLevel;

//    friend class FSceneRenderer;

    /** Custom data per primitives */
    protected final ArrayList<Object> PrimitivesCustomData = new ArrayList<>(); // Size == MaxPrimitive

    public static final int NumBufferedSubIsOccludedArrays = 2;
    public final StackBool[] FrameSubIsOccluded = new StackBool[NumBufferedSubIsOccludedArrays];

    /** Initialization constructor. */
    public FSceneView(FSceneViewInitOptions InitOptions){
        throw new UnsupportedOperationException();
    }

    /** Verifies all the assertions made on members. */
    public boolean VerifyMembersChecks(){
        throw new UnsupportedOperationException();
    }

    public boolean AllowGPUParticleUpdate() { return !bIsPlanarReflection && !bIsSceneCapture && !bIsReflectionCapture; }

    /** Transforms a point from world-space to the view's screen-space. */
    public Vector4f WorldToScreen(ReadableVector3f WorldPoint){
        throw new UnsupportedOperationException();
    }

    /** Transforms a point from the view's screen-space to world-space. */
    public Vector3f ScreenToWorld(ReadableVector4f ScreenPoint){
        throw new UnsupportedOperationException();
    }

    /** Transforms a point from the view's screen-space into pixel coordinates relative to the view's X,Y. */
    public boolean ScreenToPixel(ReadableVector4f ScreenPoint,Vector2f OutPixelLocation) {
        throw new UnsupportedOperationException();
    }

    /** Transforms a point from pixel coordinates relative to the view's X,Y (left, top) into the view's screen-space. */
    public Vector4f PixelToScreen(float X,float Y,float Z) {
        throw new UnsupportedOperationException();
    }

    /** Transforms a point from the view's world-space into pixel coordinates relative to the view's X,Y (left, top). */
    public boolean WorldToPixel(ReadableVector3f WorldPoint,Vector2f OutPixelLocation){
        throw new UnsupportedOperationException();
    }

    /** Transforms a point from pixel coordinates relative to the view's X,Y (left, top) into the view's world-space. */
    public Vector4f PixelToWorld(float X,float Y,float Z) {
        throw new UnsupportedOperationException();
    }

    /**
     * Transforms a point from the view's world-space into the view's screen-space.
     * Divides the resulting X, Y, Z by W before returning.
     */
    public FPlane Project(ReadableVector3f WorldPoint) {
        throw new UnsupportedOperationException();
    }

    /**
     * Transforms a point from the view's screen-space into world coordinates
     * multiplies X, Y, Z by W before transforming.
     */
    public Vector3f Deproject(FPlane ScreenPoint) {
        throw new UnsupportedOperationException();
    }

    /**
     * Transforms 2D screen coordinates into a 3D world-space origin and direction
     * @param ScreenPos - screen coordinates in pixels
     * @param out_WorldOrigin (out) - world-space origin vector
     * @param out_WorldDirection (out) - world-space direction vector
     */
    public void DeprojectFVector2D(Vector2f ScreenPos, Vector3f out_WorldOrigin, Vector3f out_WorldDirection) {
        throw new UnsupportedOperationException();
    }

    /**
     * Transforms 2D screen coordinates into a 3D world-space origin and direction
     * @param ScreenPos - screen coordinates in pixels
     * @param ViewRect - view rectangle
     * @param InvViewMatrix - inverse view matrix
     * @param InvProjMatrix - inverse projection matrix
     * @param out_WorldOrigin (out) - world-space origin vector
     * @param out_WorldDirection (out) - world-space direction vector
     */
    public static void DeprojectScreenToWorld(Vector2f ScreenPos, Recti ViewRect, Matrix4f InvViewMatrix, Matrix4f InvProjMatrix, Vector3f out_WorldOrigin, Vector3f out_WorldDirection){
        throw new UnsupportedOperationException();
    }

    /** Overload to take a single combined view projection matrix. */
    public static void DeprojectScreenToWorld(Vector2f ScreenPos, Recti ViewRect, Matrix4f InvViewProjMatrix, Vector3f out_WorldOrigin, Vector3f out_WorldDirection){
        throw new UnsupportedOperationException();
    }

    /**
     * Transforms 3D world-space origin into 2D screen coordinates
     * @param WorldPosition - the 3d world point to transform
     * @param ViewRect - view rectangle
     * @param ViewProjectionMatrix - combined view projection matrix
     * @param out_ScreenPos (out) - screen coordinates in pixels
     */
    public static boolean ProjectWorldToScreen(Vector3f WorldPosition, Recti ViewRect, Matrix4f ViewProjectionMatrix, Vector2f out_ScreenPos){
        throw new UnsupportedOperationException();
    }

    public ReadableVector3f GetViewRight()  { return ViewMatrices.GetViewRight(); }
    public ReadableVector3f GetViewUp()  { return ViewMatrices.GetViewUp(); }
    public ReadableVector3f GetViewDirection()  { return ViewMatrices.GetViewDirection(); }

    public FConvexVolume GetDynamicMeshElementsShadowCullFrustum() { return DynamicMeshElementsShadowCullFrustum; }
    public void SetDynamicMeshElementsShadowCullFrustum(FConvexVolume InDynamicMeshElementsShadowCullFrustum) { DynamicMeshElementsShadowCullFrustum = InDynamicMeshElementsShadowCullFrustum; }

    public ReadableVector3f GetPreShadowTranslation() { return PreShadowTranslation; }
    public void SetPreShadowTranslation(ReadableVector3f InPreShadowTranslation) { PreShadowTranslation.set(InPreShadowTranslation); }

    /** @return true:perspective, false:orthographic */
    public boolean IsPerspectiveProjection() { return ViewMatrices.IsPerspectiveProjection(); }

    /** Returns the location used as the origin for LOD computations
     * @param Index, 0 or 1, which LOD origin to return
     * @return LOD origin
     */
    public Vector3f GetTemporalLODOrigin(int Index, boolean bUseLaggedLODTransition /*= true*/) {
        throw new UnsupportedOperationException();
    }

    /** Get LOD distance factor: Sqrt(GetLODDistanceFactor()*SphereRadius*SphereRadius / ScreenPercentage) = distance to this LOD transition
     * @return distance factor
     */
    public float GetLODDistanceFactor() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the blend factor between the last two LOD samples
     */
    public float GetTemporalLODTransition() {
        throw new UnsupportedOperationException();
    }

    /**
     * returns a unique key for the view state if one exists, otherwise returns zero
     */
    public int GetViewKey() {
        throw new UnsupportedOperationException();
    }

    /**
     * returns a the occlusion frame counter or MAX_uint32 if there is no view state
     */
    public int GetOcclusionFrameCounter() {
        throw new UnsupportedOperationException();
    }

    public void UpdateProjectionMatrix(Matrix4f NewProjectionMatrix){
        throw new UnsupportedOperationException();
    }

    /** Allow things like HMD displays to update the view matrix at the last minute, to minimize perceived latency */
    public void UpdateViewMatrix(){
        throw new UnsupportedOperationException();
    }

    /** If we late update a view, we need to also late update any planar reflection views derived from it */
    public void UpdatePlanarReflectionViewMatrix(FSceneView SourceView, FMirrorMatrix MirrorMatrix){
        throw new UnsupportedOperationException();
    }

    /** Setup defaults and depending on view position (postprocess volumes) */
    public void StartFinalPostprocessSettings(ReadableVector3f InViewLocation){
        throw new UnsupportedOperationException();
    }

    /**
     * custom layers can be combined with the existing settings
     * @param Weight usually 0..1 but outside range is clamped
     */
    public void OverridePostProcessSettings(FPostProcessSettings Src, float Weight){
        throw new UnsupportedOperationException();
    }

    /** applied global restrictions from show flags */
    public void EndFinalPostprocessSettings(FSceneViewInitOptions ViewInitOptions){
        throw new UnsupportedOperationException();
    }

    public void SetupAntiAliasingMethod(){
        throw new UnsupportedOperationException();
    }

    /** Configure post process settings for the buffer visualization system */
    public void ConfigureBufferVisualizationSettings(){
        throw new UnsupportedOperationException();
    }

    /** Get the feature level for this view (cached from the scene so this is not different per view) **/
    public int GetFeatureLevel() { return FeatureLevel; }

    /** Get the feature level for this view **/
    public EShaderPlatform GetShaderPlatform() {
        throw new UnsupportedOperationException();
    }

    /** True if the view should render as an instanced stereo pass */
    public boolean IsInstancedStereoPass() { return bIsInstancedStereoEnabled /*&& StereoPass == eSSP_LEFT_EYE*/; }

    /** Sets up the view rect parameters in the view's uniform shader parameters */
    public void SetupViewRectUniformBufferParameters(FViewUniformShaderParameters ViewUniformShaderParameters,
                                                     Recti InBufferSize,
                                                     Recti InEffectiveViewRect,
                                                     FViewMatrices InViewMatrices,
                                                     FViewMatrices InPrevViewMatrice) {
        throw new UnsupportedOperationException();
    }

    /**
     * Populates the uniform buffer prameters common to all scene view use cases
     * View parameters should be set up in this method if they are required for the view to render properly.
     * This is to avoid code duplication and uninitialized parameters in other places that create view uniform parameters (e.g Slate)
     */
    public void SetupCommonViewUniformBufferParameters(FViewUniformShaderParameters ViewUniformShaderParameters,
                                                       Recti InBufferSize,
                                                       int NumMSAASamples,
                                                       Recti InEffectiveViewRect,
                                                       FViewMatrices InViewMatrices,
                                                       FViewMatrices InPrevViewMatrices) {
        throw new UnsupportedOperationException();
    }

//#if RHI_RAYTRACING
//    /** Setup ray tracing based rendering */
//    void SetupRayTracedRendering();
//
//    ERayTracingRenderMode RayTracingRenderMode = ERayTracingRenderMode::Disabled;
//
//    /** Current ray tracing debug visualization mode */
//    FName CurrentRayTracingDebugVisualizationMode;
//#endif

    /** Will return custom data associated with the specified primitive index.	*/
    public Object GetCustomData(int InPrimitiveSceneInfoIndex) { return PrimitivesCustomData.size() > InPrimitiveSceneInfoIndex ? PrimitivesCustomData.get(InPrimitiveSceneInfoIndex) : null; }

}
