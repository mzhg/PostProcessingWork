package jet.opengl.renderer.Unreal4.scenes;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector4f;

import jet.opengl.postprocessing.buffer.BufferGL;

/**
 * The information used to render a light.  This is the rendering thread's mirror of the game thread's ULightComponent.
 * FLightSceneInfo is internal to the renderer module and contains internal scene state.
 */
public class FLightSceneInfo {
    /** The light's scene proxy. */
    public FLightSceneProxy Proxy;

    /** The list of dynamic primitives affected by the light. */
    public FLightPrimitiveInteraction DynamicInteractionOftenMovingPrimitiveList;

    public FLightPrimitiveInteraction* DynamicInteractionStaticPrimitiveList;

    /** If bVisible == true, this is the index of the primitive in Scene->Lights. */
    public int Id;

    /** The identifier for the primitive in Scene->PrimitiveOctree. */
    FOctreeElementId OctreeId;

    /** Tile intersection buffer for distance field shadowing, stored on the light to avoid reallocating each frame. */
    public FLightTileIntersectionResources TileIntersectionResources;

    public FVertexBufferRHIRef ShadowCapsuleShapesVertexBuffer;
    public BufferGL ShadowCapsuleShapesSRV;

    /**
     * ShadowMap channel assigned in the forward renderer when a movable shadow casting light is added to the scene.
     * Used to pack shadow projections into channels of the light attenuation texture which is read in the base pass.
     */
    protected int DynamicShadowMapChannel;

    /** True if the light is built. */
    protected boolean bPrecomputedLightingIsValid;

    /**
     * True if the light is visible.
     * False if the light is invisible but still needed for previewing, which can only happen in the editor.
     */
    public boolean bVisible;

    /**
     * Whether to render light shaft bloom from this light.
     * For directional lights, the color around the light direction will be blurred radially and added back to the scene.
     * for point lights, the color on pixels closer than the light's SourceRadius will be blurred radially and added back to the scene.
     */
    public int bEnableLightShaftBloom;

    /** Scales the additive color. */
    public float BloomScale;

    /** Scene color must be larger than this to create bloom in the light shafts. */
    public float BloomThreshold;

    /** Multiplies against scene color to create the bloom color. */
    public final Vector4f BloomTint = new Vector4f();

    /** Number of dynamic interactions with statically lit primitives. */
    public int NumUnbuiltInteractions;

    /** Cached value from the light proxy's virtual function, since it is checked many times during shadow setup. */
    public boolean bCreatePerObjectShadowsForDynamicObjects;

    /** The scene the light is in. */
    public FScene Scene;

    /** Initialization constructor. */
    public FLightSceneInfo(FLightSceneProxy InProxy, boolean InbVisible){

    }

    /** Adds the light to the scene. */
    void AddToScene();

    /**
     * If the light affects the primitive, create an interaction, and process children
     * @param LightSceneInfoCompact Compact representation of the light
     * @param PrimitiveSceneInfoCompact Compact representation of the primitive
     */
    void CreateLightPrimitiveInteraction(const FLightSceneInfoCompact& LightSceneInfoCompact, const FPrimitiveSceneInfoCompact& PrimitiveSceneInfoCompact);

    /** Removes the light from the scene. */
    void RemoveFromScene();

    /** Detaches the light from the primitives it affects. */
    void Detach();

    /* Octree bounds setup.
    public FBoxCenterAndExtent GetBoundingBox()
    {
        FSphere BoundingSphere = Proxy->GetBoundingSphere();
        return FBoxCenterAndExtent(BoundingSphere.Center, FVector(BoundingSphere.W, BoundingSphere.W, BoundingSphere.W));
    }*/

    bool ShouldRenderLight(const FViewInfo& View) const;

    /** Encapsulates all View-Independent reasons to have this light render. */
    bool ShouldRenderLightViewIndependent() const
    {
        return !Proxy->GetColor().IsAlmostBlack()
                // Only render lights with dynamic lighting or unbuilt static lights
                && (!Proxy->HasStaticLighting() || !IsPrecomputedLightingValid());
    }

    /** Encapsulates all View-Independent reasons to render ViewIndependentWholeSceneShadows for this light */
    bool ShouldRenderViewIndependentWholeSceneShadows() const
    {
        bool bShouldRenderLight = ShouldRenderLightViewIndependent();
        bool bCastDynamicShadow = Proxy->CastsDynamicShadow();

        // Also create a whole scene shadow for lights with precomputed shadows that are unbuilt
		const bool bCreateShadowToPreviewStaticLight =
            Proxy->HasStaticShadowing()
                    && bCastDynamicShadow
                    && !IsPrecomputedLightingValid();

        bool bShouldRenderShadow = bShouldRenderLight && bCastDynamicShadow && (!Proxy->HasStaticLighting() || bCreateShadowToPreviewStaticLight);
        return bShouldRenderShadow;
    }

    bool IsPrecomputedLightingValid() const;

    void SetDynamicShadowMapChannel(int32 NewChannel)
    {
        if (Proxy->HasStaticShadowing())
        {
            // This ensure would trigger if several static shadowing light intersects eachother and have the same channel.
            // ensure(Proxy->GetPreviewShadowMapChannel() == NewChannel);
        }
        else
        {
            DynamicShadowMapChannel = NewChannel;
        }
    }

    int32 GetDynamicShadowMapChannel() const
    {
        if (Proxy->HasStaticShadowing())
        {
            // Stationary lights get a channel assigned by ReassignStationaryLightChannels
            return Proxy->GetPreviewShadowMapChannel();
        }

        // Movable lights get a channel assigned when they are added to the scene
        return DynamicShadowMapChannel;
    }

    /** Hash function. */
    friend uint32 GetTypeHash(const FLightSceneInfo* LightSceneInfo)
    {
        return (uint32)LightSceneInfo->Id;
    }

    // FRenderResource interface.
    virtual void ReleaseRHI();
}
