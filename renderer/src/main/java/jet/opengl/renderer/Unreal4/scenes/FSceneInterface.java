package jet.opengl.renderer.Unreal4.scenes;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.WritableVector3f;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import jet.opengl.postprocessing.texture.TextureCube;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.renderer.Unreal4.FPrimitiveComponentId;
import jet.opengl.renderer.Unreal4.FPrimitiveSceneInfo;
import jet.opengl.renderer.Unreal4.FRenderResource;
import jet.opengl.renderer.Unreal4.FSceneRenderer;
import jet.opengl.renderer.Unreal4.api.ERHIFeatureLevel;
import jet.opengl.renderer.Unreal4.api.EShaderPlatform;
import jet.opengl.renderer.Unreal4.api.EShadingPath;
import jet.opengl.renderer.Unreal4.atmosphere.FAtmosphericFogSceneInfo;
import jet.opengl.renderer.Unreal4.atmosphere.FSkyAtmosphereRenderSceneInfo;
import jet.opengl.renderer.Unreal4.atmosphere.FWindSourceSceneProxy;
import jet.opengl.renderer.Unreal4.atmosphere.UAtmosphericFogComponent;
import jet.opengl.renderer.Unreal4.atmosphere.USkyAtmosphereComponent;
import jet.opengl.renderer.Unreal4.atmosphere.UWindDirectionalSourceComponent;
import jet.opengl.renderer.Unreal4.distancefield.FPrecomputedVisibilityHandler;
import jet.opengl.renderer.Unreal4.distancefield.FPrecomputedVolumeDistanceField;
import jet.opengl.renderer.Unreal4.fx.FFXSystemInterface;
import jet.opengl.renderer.Unreal4.volumetricfog.UExponentialHeightFogComponent;

/**
 * An interface to the private scene manager implementation of a scene.  Use GetRendererModule().AllocateScene to create.
 * The scene
 */
public abstract class FSceneInterface {
    protected int FeatureLevel;

    public FSceneInterface(int InFeatureLevel) {
        FeatureLevel = (InFeatureLevel);
    }

    // FSceneInterface interface

    /**
     * Adds a new primitive component to the scene
     *
     * @param Primitive - primitive component to add
     */
    public abstract void AddPrimitive(UPrimitiveComponent Primitive);
    /**
     * Removes a primitive component from the scene
     *
     * @param Primitive - primitive component to remove
     */
    public abstract void RemovePrimitive(UPrimitiveComponent Primitive);
    /** Called when a primitive is being unregistered and will not be immediately re-registered. */
    public abstract void ReleasePrimitive(UPrimitiveComponent Primitive);
    /**
     * Updates the transform of a primitive which has already been added to the scene.
     *
     * @param Primitive - primitive component to update
     */
    public abstract void UpdatePrimitiveTransform(UPrimitiveComponent Primitive);
    /** Updates primitive attachment state. */
    public abstract void UpdatePrimitiveAttachment(UPrimitiveComponent Primitive);
    /**
     * Updates the custom primitive data of a primitive component which has already been added to the scene.
     *
     * @param Primitive - Primitive component to update
     */
    public abstract void UpdateCustomPrimitiveData(UPrimitiveComponent Primitive);
    /**
     * Updates distance field scene data (transforms, uv scale, self-shadow bias, etc.) but doesn't change allocation in the atlas
     */
    public void UpdatePrimitiveDistanceFieldSceneData_GameThread(UPrimitiveComponent Primitive) {}
    /** Finds the  primitive with the associated component id. */
    public abstract FPrimitiveSceneInfo GetPrimitiveSceneInfo(int PrimitiveIndex);
    /** Get the primitive previous local to world (used for motion blur). Returns true if the matrix was set. */
    public boolean GetPreviousLocalToWorld( FPrimitiveSceneInfo PrimitiveSceneInfo, Matrix4f OutPreviousLocalToWorld)
    { return false; }
    /**
     * Adds a new light component to the scene
     *
     * @param Light - light component to add
     */
    public abstract void AddLight(ULightComponent Light);
    /**
     * Removes a light component from the scene
     *
     * @param Light - light component to remove
     */
    public abstract void RemoveLight(ULightComponent Light);
    /**
     * Adds a new light component to the scene which is currently invisible, but needed for editor previewing
     *
     * @param Light - light component to add
     */
    public abstract void AddInvisibleLight(ULightComponent Light);
    public abstract void SetSkyLight(FSkyLightSceneProxy Light);
    public abstract void DisableSkyLight(FSkyLightSceneProxy Light);

    public abstract boolean HasSkyLightRequiringLightingBuild();
    public abstract boolean HasAtmosphereLightRequiringLightingBuild();

    /**
     * Adds a new decal component to the scene
     *
     * @param Component - component to add
     */
    public abstract void AddDecal(UDecalComponent Component);
    /**
     * Removes a decal component from the scene
     *
     * @param Component - component to remove
     */
    public abstract void RemoveDecal(UDecalComponent Component);
    /**
     * Updates the transform of a decal which has already been added to the scene.
     *
     * @param Component - Decal component to update
     */
    public abstract void UpdateDecalTransform(UDecalComponent Component) ;
    public abstract void UpdateDecalFadeOutTime(UDecalComponent Component) ;
    public abstract void UpdateDecalFadeInTime(UDecalComponent Component);

    /** Adds a reflection capture to the scene. */
    public void AddReflectionCapture( UReflectionCaptureComponent Component) {}

    /** Removes a reflection capture from the scene. */
    public void RemoveReflectionCapture( UReflectionCaptureComponent Component) {}

    /** Reads back reflection capture data from the GPU.  Very slow operation that blocks the GPU and rendering thread many times. */
    public  void GetReflectionCaptureData(UReflectionCaptureComponent Component, FReflectionCaptureData OutCaptureData) {}

    /** Updates a reflection capture's transform, and then re-captures the scene. */
    public  void UpdateReflectionCaptureTransform(UReflectionCaptureComponent Component) {}

    /**
     * Allocates reflection captures in the scene's reflection cubemap array and updates them by recapturing the scene.
     * Existing captures will only be updated.  Must be called from the game thread.
     */
    public void AllocateReflectionCaptures(ArrayList<UReflectionCaptureComponent> NewCaptures, String CaptureReason, boolean bVerifyOnlyCapturing) {}
    public void ReleaseReflectionCubemap(UReflectionCaptureComponent CaptureComponent) {}

    /**
     * Updates the contents of the given sky capture by rendering the scene.
     * This must be called on the game thread. Return the AverageBrightness.
     */
    public float UpdateSkyCaptureContents(USkyLightComponent CaptureComponent, boolean bCaptureEmissiveOnly, TextureCube SourceCubemap, TextureGL OutProcessedTexture, /*float& OutAverageBrightness,*/ Vector3f OutIrradianceEnvironmentMap, ByteBuffer OutRadianceMap) {
        return 0;
    }

    public void AddPlanarReflection( UPlanarReflectionComponent Component) {}
    public void RemovePlanarReflection( UPlanarReflectionComponent Component) {}
    public void UpdatePlanarReflectionTransform(UPlanarReflectionComponent Component) {}

    /**
     * Updates the contents of the given scene capture by rendering the scene.
     * This must be called on the game thread.
     */
    public void UpdateSceneCaptureContents(USceneCaptureComponent2D CaptureComponent) {}
    public void UpdateSceneCaptureContents(USceneCaptureComponentCube CaptureComponent) {}
    public void UpdatePlanarReflectionContents(UPlanarReflectionComponent CaptureComponent, FSceneRenderer MainSceneRenderer) {}

    public void AddPrecomputedLightVolume(FPrecomputedLightVolume Volume) {}
    public void RemovePrecomputedLightVolume(FPrecomputedLightVolume Volume) {}

    public boolean HasPrecomputedVolumetricLightmap_RenderThread() { return false; }
    public void AddPrecomputedVolumetricLightmap(FPrecomputedVolumetricLightmap Volume) {}
    public void RemovePrecomputedVolumetricLightmap(FPrecomputedVolumetricLightmap Volume) {}

    /** Add a runtime virtual texture object to the scene. */
    public void AddRuntimeVirtualTexture(URuntimeVirtualTextureComponent Component) {}

    /** Removes a runtime virtual texture object from the scene. */
    public void RemoveRuntimeVirtualTexture(URuntimeVirtualTextureComponent Component) {}

    /**
     * Retrieves primitive uniform shader parameters that are internal to the renderer.  return the bHasPrecomputedVolumetricLightmap |  OutputVelocity | SingleCaptureIndex
     */
    public long GetPrimitiveUniformShaderParameters_RenderThread(FPrimitiveSceneInfo PrimitiveSceneInfo, /*bool& bHasPrecomputedVolumetricLightmap,*/ Matrix4f PreviousLocalToWorld/*, int32& SingleCaptureIndex, bool& OutputVelocity*/)  {
        return 0;
    }

    /**
     * Updates the transform of a light which has already been added to the scene.
     *
     * @param Light - light component to update
     */
    public abstract void UpdateLightTransform(ULightComponent Light);
    /**
     * Updates the color and brightness of a light which has already been added to the scene.
     *
     * @param Light - light component to update
     */
    public abstract void UpdateLightColorAndBrightness(ULightComponent Light);

    /** Sets the precomputed visibility handler for the scene, or NULL to clear the current one. */
    public void SetPrecomputedVisibility(FPrecomputedVisibilityHandler PrecomputedVisibilityHandler) {}

    /** Sets the precomputed volume distance field for the scene, or NULL to clear the current one. */
    public void SetPrecomputedVolumeDistanceField(FPrecomputedVolumeDistanceField PrecomputedVolumeDistanceField) {}

    /** Updates all static draw lists. */
    public void UpdateStaticDrawLists() {}

    /**
     * Adds a new exponential height fog component to the scene
     *
     * @param FogComponent - fog component to add
     */
    public abstract void AddExponentialHeightFog(UExponentialHeightFogComponent FogComponent);
    /**
     * Removes a exponential height fog component from the scene
     *
     * @param FogComponent - fog component to remove
     */
    public abstract void RemoveExponentialHeightFog(UExponentialHeightFogComponent FogComponent);

    /**
     * Adds a new atmospheric fog component to the scene
     *
     * @param FogComponent - fog component to add
     */
    public abstract void AddAtmosphericFog(UAtmosphericFogComponent FogComponent);

    /**
     * Removes a atmospheric fog component from the scene
     *
     * @param FogComponent - fog component to remove
     */
    public abstract void RemoveAtmosphericFog(UAtmosphericFogComponent FogComponent);

    /**
     * Removes a atmospheric fog resource from the scene...this is just a double check to make sure we don't have stale stuff hanging around; should already be gone.
     *
     * @param FogResource - fog resource to remove
     */
    public abstract void RemoveAtmosphericFogResource_RenderThread(FRenderResource FogResource);

    /**
     * Returns the scene's FAtmosphericFogSceneInfo if it exists
     */
    public abstract FAtmosphericFogSceneInfo GetAtmosphericFogSceneInfo();

    /**
     * Adds the unique sky atmosphere component to the scene
     *
     * @param SkyAtmosphereComponent - component to add
     */
    public abstract void AddSkyAtmosphere(USkyAtmosphereComponent SkyAtmosphereComponent, boolean bStaticLightingBuilt);
    /**
     * Removes the unique sky atmosphere component to the scene
     *
     * @param SkyAtmosphereComponent - component to remove
     */
    public abstract void RemoveSkyAtmosphere(USkyAtmosphereComponent SkyAtmosphereComponent);
    /**
     * Returns the scene's unique FSkyAtmosphereRenderSceneInfo if it exists
     */
    public abstract FSkyAtmosphereRenderSceneInfo GetSkyAtmosphereSceneInfo();
    /**
     * Override a sky atmosphere light direction
     * @param SkyAtmosphereComponent - component to verify it is the actual unique SkyAtmosphere
     * @param AtmosphereLightIndex - the atmosphere light index to consider
     * @param LightDirection - the new light direction to override the atmosphere light with
     */
    public abstract void OverrideSkyAtmosphereLightDirection(USkyAtmosphereComponent SkyAtmosphereComponent, int AtmosphereLightIndex, ReadableVector3f LightDirection);

    /**
     * Adds a wind source component to the scene.
     * @param WindComponent - The component to add.
     */
    public abstract void AddWindSource(UWindDirectionalSourceComponent WindComponent);
    /**
     * Removes a wind source component from the scene.
     * @param WindComponent - The component to remove.
     */
    public abstract void RemoveWindSource(UWindDirectionalSourceComponent WindComponent);
    /**
     * Accesses the wind source list.  Must be called in the rendering thread.
     * @return The list of wind sources in the scene.
     */
    public abstract List<FWindSourceSceneProxy> GetWindSources_RenderThread();

    /** Accesses wind parameters.  XYZ will contain wind direction * Strength, W contains wind speed. */
    public abstract void GetWindParameters(ReadableVector3f Position, WritableVector3f OutDirection, /*float& OutSpeed, float& OutMinGustAmt, float& OutMaxGustAmt*/ WritableVector3f out);

    /** Accesses wind parameters safely for game thread applications */
    public abstract void GetWindParameters_GameThread(ReadableVector3f Position, WritableVector3f OutDirection, /*float& OutSpeed, float& OutMinGustAmt, float& OutMaxGustAmt*/WritableVector3f out);

    /** Same as GetWindParameters, but ignores point wind sources. */
    public abstract void GetDirectionalWindParameters(WritableVector3f OutDirection, /*float& OutSpeed, float& OutMinGustAmt, float& OutMaxGustAmt*/ WritableVector3f out);

    /**
     * Adds a SpeedTree wind computation object to the scene.
     * @param StaticMesh - The SpeedTree static mesh whose wind to add.

    public abstract void AddSpeedTreeWind(class FVertexFactory* VertexFactory, const class UStaticMesh* StaticMesh) = 0;
     */
    /**
     * Removes a SpeedTree wind computation object to the scene.
     * @param StaticMesh - The SpeedTree static mesh whose wind to remove.

    virtual void RemoveSpeedTreeWind_RenderThread(class FVertexFactory* VertexFactory, const class UStaticMesh* StaticMesh) = 0;
     */

    /** Ticks the SpeedTree wind object and updates the uniform buffer.
    virtual void UpdateSpeedTreeWind(double CurrentTime) = 0;*/

    /**
     * Looks up the SpeedTree uniform buffer for the passed in vertex factory.
     * @param VertexFactory - The vertex factory registered for SpeedTree.

    virtual FRHIUniformBuffer* GetSpeedTreeUniformBuffer(const FVertexFactory* VertexFactory) const = 0;*/

    /**
     * Release this scene and remove it from the rendering thread
     */
    public abstract void Release();
    /**
     * Retrieves the lights interacting with the passed in primitive and adds them to the out array.
     *
     * @param	Primitive				Primitive to retrieve interacting lights for
     * @param	RelevantLights	[out]	Array of lights interacting with primitive
     */
    public abstract void GetRelevantLights( UPrimitiveComponent Primitive, List<ULightComponent> RelevantLights );
    /**
     * Indicates if hit proxies should be processed by this scene
     *
     * @return true if hit proxies should be rendered in this scene.
     */
    public abstract boolean RequiresHitProxies();
    /**
     * Get the optional UWorld that is associated with this scene
     *
     * @return UWorld instance used by this scene

    virtual class UWorld* GetWorld() const = 0;*/
    /**
     * Return the scene to be used for rendering. Note that this can return NULL if rendering has
     * been disabled!
     */
    public FScene GetRenderScene()
    {
        return null;
    }

//    public void UpdateSceneSettings(AWorldSettings WorldSettings) {}

    /**
     * Gets the GPU Skin Cache system associated with the scene.
    public class FGPUSkinCache GetGPUSkinCache()
    {
        return null;
    }*/

    /**
     * Sets the FX system associated with the scene.
     */
    public abstract void SetFXSystem( FFXSystemInterface InFXSystem );

    /**
     * Get the FX system associated with the scene.
     */
    public abstract FFXSystemInterface GetFXSystem();

//    public void DumpUnbuiltLightInteractions( FOutputDevice& Ar ) const { }

    /** Updates the scene's list of parameter collection id's and their uniform buffers.
    public void UpdateParameterCollections(List<FMaterialParameterCollectionInstanceResource> InParameterCollections) {}*/

    /**
     * Exports the scene.
     *
     * @param	Ar		The Archive used for exporting.
     *
    virtual void Export( FArchive& Ar ) const
    {}*/


    /**
     * Shifts scene data by provided delta
     * Called on world origin changes
     *
     * @param	InOffset	Delta to shift scene by
     */
    public void ApplyWorldOffset(ReadableVector3f InOffset) {}

    /**
     * Notification that level was added to a world
     *
     * @param	InLevelName		Level name
     */
//    public void OnLevelAddedToWorld(String InLevelName, UWorld* InWorld, bool bIsLightingScenario) {}
//    public void OnLevelRemovedFromWorld(UWorld* InWorld, bool bIsLightingScenario) {}

    /**
     * @return True if there are any lights in the scene
     */
    public abstract boolean HasAnyLights() ;

    public boolean IsEditorScene() { return false; }

    public int GetFeatureLevel() { return FeatureLevel; }

    public EShaderPlatform GetShaderPlatform() { return FSceneRenderer.GShaderPlatformForFeatureLevel[GetFeatureLevel()]; }

    public static EShadingPath GetShadingPath(int InFeatureLevel)
    {
        if (InFeatureLevel >= ERHIFeatureLevel.SM4)
        {
            return EShadingPath.Deferred;
        }
        else
        {
            return EShadingPath.Mobile;
        }
    }

    public EShadingPath GetShadingPath()
    {
        return GetShadingPath(GetFeatureLevel());
    }

//#if WITH_EDITOR
    /**
     * Initialize the pixel inspector buffers.
     * @return True if implemented false otherwise.
     */
    public boolean InitializePixelInspector(TextureGL BufferFinalColor, TextureGL BufferSceneColor, TextureGL BufferDepth, TextureGL BufferHDR,
                                            TextureGL BufferA, TextureGL BufferBCDE, int BufferIndex)
    {
        return false;
    }

    /**
     * Add a pixel inspector request.
     * @return True if implemented false otherwise.

    public boolean AddPixelInspectorRequest(FPixelInspectorRequest PixelInspectorRequest)
    {
        return false;
    }*/
//#endif //WITH_EDITOR

    /**
     * Returns the FPrimitiveComponentId for all primitives in the scene
     */
    public List<FPrimitiveComponentId> GetScenePrimitiveComponentIds() {
        throw new UnsupportedOperationException();
    }

    public void StartFrame() {}
    public int GetFrameNumber() { return 0; }
    public void IncrementFrameNumber() {}
}
