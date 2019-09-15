package jet.opengl.renderer.Unreal4.heightfield;

import jet.opengl.renderer.Unreal4.FProjectedShadowInfo;
import jet.opengl.renderer.Unreal4.FViewInfo;
import jet.opengl.renderer.Unreal4.scenes.FScene;

public class FHeightfieldLightingViewInfo {
    private final FHeightfieldDescription Heightfield = new FHeightfieldDescription();

    public void SetupVisibleHeightfields(FViewInfo View/*, FRHICommandListImmediate RHICmdList*/){
        throw new UnsupportedOperationException();
    }

    public void SetupHeightfieldsForScene(FScene Scene/*, FRHICommandListImmediate RHICmdList*/){
        throw new UnsupportedOperationException();
    }

    public void ClearShadowing(FViewInfo View, /*FRHICommandListImmediate RHICmdList,*/ FLightSceneInfo LightSceneInfo){
        throw new UnsupportedOperationException();
    }

    public void ComputeShadowMapShadowing(FViewInfo View, /*FRHICommandListImmediate RHICmdList,*/ FProjectedShadowInfo ProjectedShadowInfo){
        throw new UnsupportedOperationException();
    }

    public void ComputeRayTracedShadowing(FViewInfo View, /*FRHICommandListImmediate RHICmdList,*/ FProjectedShadowInfo ProjectedShadowInfo, FLightTileIntersectionResources TileIntersectionResources, FDistanceFieldObjectBufferResource CulledObjectBuffers){
        throw new UnsupportedOperationException();
    }

    public void ComputeLighting(FViewInfo View, /*FRHICommandListImmediate RHICmdList,*/ FLightSceneInfo LightSceneInfo){
        throw new UnsupportedOperationException();
    }

    public void ComputeOcclusionForScreenGrid(FViewInfo View, /*FRHICommandListImmediate RHICmdList,*/ FSceneRenderTargetItem DistanceFieldNormal, FAOScreenGridResources ScreenGridResources, FDistanceFieldAOParameters Parameters){
        throw new UnsupportedOperationException();
    }

    public void ComputeIrradianceForScreenGrid(FViewInfo View, /*FRHICommandListImmediate RHICmdList,*/ FSceneRenderTargetItem DistanceFieldNormal, FAOScreenGridResources ScreenGridResources, FDistanceFieldAOParameters Parameters){
        throw new UnsupportedOperationException();
    }

    public void CompositeHeightfieldsIntoGlobalDistanceField(FScene Scene, FViewInfo View, float GlobalMaxSphereQueryRadius, FGlobalDistanceFieldInfo GlobalDistanceFieldInfo, FGlobalDistanceFieldClipmap Clipmap, int ClipmapIndexValue, FVolumeUpdateRegion UpdateRegion){
        throw new UnsupportedOperationException();
    }
}
