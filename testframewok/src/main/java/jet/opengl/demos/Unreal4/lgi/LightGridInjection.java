package jet.opengl.demos.Unreal4.lgi;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.effect.Light;
import jet.opengl.demos.Unreal4.FForwardLightData;
import jet.opengl.demos.Unreal4.FForwardLocalLightData;
import jet.opengl.demos.Unreal4.UE4LightCollections;
import jet.opengl.demos.Unreal4.UE4LightInfo;
import jet.opengl.demos.intel.va.VaBoundingSphere;
import jet.opengl.postprocessing.core.volumetricLighting.LightType;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.util.Numeric;

public class LightGridInjection {

    private static final int INDEX_NONE = -1;

    public static final class Params extends UE4LightCollections {
        /** "Size of a cell in the light grid, in pixels.  r.Forward.LightGridPixelSize" */
        public int GLightGridPixelSize = 64;

        /** Number of Z slices in the light grid.  r.Forward.LightGridSizeZ */
        public int GLightGridSizeZ = 32;

        /** Controls how much memory is allocated for each cell for light culling.  When r.Forward.LightLinkedListCulling is enabled, this is used to compute a global max instead of a per-cell limit on culled lights. r.Forward.MaxCulledLightsPerCell*/
        public int GMaxCulledLightsPerCell = 32;

        /** Uses a reverse linked list to store culled lights, removing the fixed limit on how many lights can affect a cell - it becomes a global limit instead. r.Forward.LightLinkedListCulling*/
        public int GLightLinkedListCulling = 1;

        /** Whether to run compute light culling pass. 0: off, 1: on (default), r.LightCulling.Quality */
        public boolean GLightCullingQuality = true;

        public Matrix4f view;
        public Matrix4f proj;

        public float cameraFar, cameraNear;
        public TextureGL shadowmap;
    }

    private Params mParams;
    private final FForwardLightData ForwardLightData = new FForwardLightData(1);

    public void computeLightGrid(Params params){
        mParams = params;

//        QUICK_SCOPE_CYCLE_COUNTER(STAT_ComputeLightGrid);
//        SCOPED_DRAW_EVENT(RHICmdList, ComputeLightGrid);

//        static const auto AllowStaticLightingVar = IConsoleManager::Get().FindTConsoleVariableDataInt(TEXT("r.AllowStaticLighting"));
        final boolean AllowStaticLightingVar = true;
        final boolean bAllowStaticLighting = (!AllowStaticLightingVar /*|| AllowStaticLightingVar->GetValueOnRenderThread() != 0*/);
        final boolean bAllowFormatConversion = true; //RHISupportsBufferLoadTypeConversion(GMaxRHIShaderPlatform);

        boolean bAnyViewUsesForwardLighting = false;

        /*for (int32 ViewIndex = 0; ViewIndex < Views.Num(); ViewIndex++)
        {
			const FViewInfo& View = Views[ViewIndex];
            bAnyViewUsesForwardLighting |= View.bTranslucentSurfaceLighting || ShouldRenderVolumetricFog();
        }*/

		final boolean bCullLightsToGrid = params.GLightCullingQuality; // && (ViewFamily.EngineShowFlags.DirectLighting && (IsForwardShadingEnabled(ShaderPlatform) || bAnyViewUsesForwardLighting || IsRayTracingEnabled()));

        /*FSimpleLightArray SimpleLights;

        if (bCullLightsToGrid)
        {
            GatherSimpleLights(ViewFamily, Views, SimpleLights);
        }*/
        List<UE4LightInfo> SimpleLights = params.lightInfos;


//        for (int32 ViewIndex = 0; ViewIndex < Views.Num(); ViewIndex++)
        {
//            FViewInfo& View = Views[ViewIndex];
//            FForwardLightData& ForwardLightData = View.ForwardLightingResources->ForwardLightData;
//            ForwardLightData = FForwardLightData();

//            TArray<FForwardLocalLightData, SceneRenderingAllocator> ForwardLocalLightData;
            ArrayList<FForwardLocalLightData> ForwardLocalLightData = new ArrayList<>();
            float FurthestLight = 1000;

            if (bCullLightsToGrid) {
//                ForwardLocalLightData.Empty(Scene->Lights.Num() + SimpleLights.InstanceData.Num());
                ForwardLocalLightData.ensureCapacity(SimpleLights.size());

//                for (TSparseArray<FLightSceneInfoCompact>::TConstIterator LightIt(Scene->Lights); LightIt; ++LightIt)
                for (UE4LightInfo LightSceneInfoCompact : SimpleLights) {
//					const FLightSceneInfoCompact& LightSceneInfoCompact = *LightIt;
//					const FLightSceneInfo* const LightSceneInfo = LightSceneInfoCompact.LightSceneInfo;
//					const FVisibleLightInfo& VisibleLightInfo = VisibleLightInfos[LightIt.GetIndex()];
//					const FLightSceneProxy* LightProxy = LightSceneInfo->Proxy;

//                    if (LightSceneInfo->ShouldRenderLightViewIndependent()
//                            && LightSceneInfo->ShouldRenderLight(View)
                    // Reflection override skips direct specular because it tends to be blindingly bright with a perfectly smooth surface
//                            && !ViewFamily.EngineShowFlags.ReflectionOverride)
                    {
                        /*FLightShaderParameters LightParameters;
                        LightProxy->GetLightShaderParameters(LightParameters);

                        if (LightProxy->IsInverseSquared())
                        {
                            LightParameters.FalloffExponent = 0;
                        }*/

                        // When rendering reflection captures, the direct lighting of the light is actually the indirect specular from the main view
                        /*if (View.bIsReflectionCapture)
                        {
                            LightParameters.Color *= LightProxy->GetIndirectLightingScale();
                        }*/

                        float FalloffExponent = 0;

                        int ShadowMapChannel = 0;//LightProxy->GetShadowMapChannel();
                        int DynamicShadowMapChannel = 0; //LightSceneInfo->GetDynamicShadowMapChannel();

                        if (!bAllowStaticLighting) {
                            ShadowMapChannel = INDEX_NONE;
                        }

                        // Static shadowing uses ShadowMapChannel, dynamic shadows are packed into light attenuation using DynamicShadowMapChannel
                        int ShadowMapChannelMaskPacked =
                                (ShadowMapChannel == 0 ? 1 : 0) |
                                        (ShadowMapChannel == 1 ? 2 : 0) |
                                        (ShadowMapChannel == 2 ? 4 : 0) |
                                        (ShadowMapChannel == 3 ? 8 : 0) |
                                        (DynamicShadowMapChannel == 0 ? 16 : 0) |
                                        (DynamicShadowMapChannel == 1 ? 32 : 0) |
                                        (DynamicShadowMapChannel == 2 ? 64 : 0) |
                                        (DynamicShadowMapChannel == 3 ? 128 : 0);

                        ShadowMapChannelMaskPacked |= /*LightProxy->GetLightingChannelMask()*/0 << 8;

                        if ((LightSceneInfoCompact.type == LightType.POINT /*&& ViewFamily.EngineShowFlags.PointLights*/) ||
                                (LightSceneInfoCompact.type == LightType.SPOT /*&& ViewFamily.EngineShowFlags.SpotLights*/) ||
                                (LightSceneInfoCompact.type == LightType.RECT  /*&& ViewFamily.EngineShowFlags.RectLights*/)) {
//                            ForwardLocalLightData.AddUninitialized(1);
//                            FForwardLocalLightData& LightData = ForwardLocalLightData.Last();

                            FForwardLocalLightData LightData = new FForwardLocalLightData();
                            ForwardLocalLightData.add(LightData);

//                            final float LightFade = GetLightFadeFactor(View, LightProxy);
//                            LightParameters.Color *= LightFade;

                            LightData.LightPositionAndInvRadius.set(LightSceneInfoCompact.position, 1 / LightSceneInfoCompact.range);
                            LightData.LightColorAndFalloffExponent.set(LightSceneInfoCompact.color, FalloffExponent);
                            LightData.LightDirectionAndShadowMapChannelMask.set(LightSceneInfoCompact.direction, Float.intBitsToFloat(ShadowMapChannelMaskPacked));
                            if(LightSceneInfoCompact.type == LightType.POINT){
                                LightData.SpotAnglesAndSourceRadiusPacked.set(-2, -1, 1, 0);
                            }else{   //Spot light
                                final float ClampedInnerConeAngle = Numeric.clamp(LightSceneInfoCompact.spotAngle* 0.8f,0.0f,(float)Math.toRadians(89.0f));
                                final float ClampedOuterConeAngle = Numeric.clamp(LightSceneInfoCompact.spotAngle,ClampedInnerConeAngle + 0.001f,(float)Math.toRadians(89.0f)+0.01f);
                                float CosOuterCone = (float) Math.cos(ClampedOuterConeAngle);
                                float CosInnerCone = (float) Math.cos(ClampedInnerConeAngle);
                                float InvCosConeDifference = 1.0f / (CosInnerCone - CosOuterCone);
                                LightData.SpotAnglesAndSourceRadiusPacked.set(CosOuterCone, InvCosConeDifference, LightSceneInfoCompact.SourceRadius, 0);
                            }

//                            LightData.SpotAnglesAndSourceRadiusPacked.set(LightParameters.SpotAngles.X, LightParameters.SpotAngles.Y, LightParameters.SourceRadius, 0);

                            LightData.LightTangentAndSoftSourceRadius.set(/*LightParameters.Tangent, LightParameters.SoftSourceRadius*/1,0,0, 0);

                            float VolumetricScatteringIntensity = LightSceneInfoCompact.VolumetricScatteringIntensity;
                                //LightProxy -> GetVolumetricScatteringIntensity();

                            /*if (LightNeedsSeparateInjectionIntoVolumetricFog(LightSceneInfo, VisibleLightInfos[LightSceneInfo -> Id])) {
                                // Disable this lights forward shading volumetric scattering contribution
                                VolumetricScatteringIntensity = 0;
                            }*/

                            // Pack both values into a single float to keep float4 alignment
							final short SourceLength16f = Numeric.convertFloatToHFloat(LightSceneInfoCompact.SourceLength); //  FFloat16(LightParameters.SourceLength);
                            final short VolumetricScatteringIntensity16f = Numeric.convertFloatToHFloat(VolumetricScatteringIntensity);
							final int PackedWInt = Numeric.encode(SourceLength16f, VolumetricScatteringIntensity16f); //  ((uint32) SourceLength16f.Encoded) | ((uint32) VolumetricScatteringIntensity16f.Encoded << 16);
                            LightData.SpotAnglesAndSourceRadiusPacked.w = Float.intBitsToFloat(PackedWInt); //   *( float*)&PackedWInt;

                            VaBoundingSphere  BoundingSphere = LightSceneInfoCompact.boundingSphere; // LightProxy -> GetBoundingSphere();
                            final float Distance = //View.ViewMatrices.GetViewMatrix().TransformPosition(BoundingSphere.Center).Z + BoundingSphere.W;
                                    -(params.view.m02 * BoundingSphere.Center.x + params.view.m12 * BoundingSphere.Center.y + params.view.m22 * BoundingSphere.Center.y) + BoundingSphere.Radius;

                            FurthestLight = Math.max (FurthestLight, Distance);
                        } else if (LightSceneInfoCompact.type == LightType.DIRECTIONAL /*&& ViewFamily.EngineShowFlags.DirectionalLights*/) {
                            ForwardLightData.HasDirectionalLight = true;
                            ForwardLightData.DirectionalLightColor.set(LightSceneInfoCompact.color);
                            ForwardLightData.DirectionalLightVolumetricScatteringIntensity = LightSceneInfoCompact.VolumetricScatteringIntensity; //LightProxy -> GetVolumetricScatteringIntensity();
                            ForwardLightData.DirectionalLightDirection.set(LightSceneInfoCompact.direction);
                            ForwardLightData.DirectionalLightShadowMapChannelMask = ShadowMapChannelMaskPacked;

//							const FVector2D FadeParams = LightProxy -> GetDirectionalLightDistanceFadeParameters(View.GetFeatureLevel(), LightSceneInfo -> IsPrecomputedLightingValid(), View.MaxShadowCascades);

                            ForwardLightData.DirectionalLightDistanceFadeMAD.set(1, 0);//  = FVector2D(FadeParams.Y, -FadeParams.X * FadeParams.Y);

//                            if (ViewFamily.EngineShowFlags.DynamicShadows && VisibleLightInfos.IsValidIndex(LightSceneInfo -> Id) && VisibleLightInfos[LightSceneInfo -> Id].AllProjectedShadows.Num() > 0)
                            {
//								const TArray<FProjectedShadowInfo*,
//                                SceneRenderingAllocator > & DirectionalLightShadowInfos = VisibleLightInfos[LightSceneInfo -> Id].AllProjectedShadows;

                                ForwardLightData.NumDirectionalLightCascades = 1;
                                Matrix4f.mul(LightSceneInfoCompact.proj, LightSceneInfoCompact.view, ForwardLightData.DirectionalLightWorldToShadowMatrix[0]);
                                ForwardLightData.CascadeEndDepths[0] = mParams.cameraFar;
                                ForwardLightData.DirectionalLightShadowmapMinMax[0].set(0,0,1,1);

//                result.DirectionalLightShadowmapAtlas = ShadowInfo->RenderTargets.DepthTarget->GetRenderTargetItem().ShaderResourceTexture.GetReference();
                                ForwardLightData.DirectionalLightDepthBias = 0.0001f;
                                float shadowmapSizeX = mParams.shadowmap.getWidth();
                                float shadowmapSizeY = mParams.shadowmap.getHeight();
                                ForwardLightData.DirectionalLightShadowmapAtlasBufferSize.set(shadowmapSizeX, shadowmapSizeY, 1.0f / shadowmapSizeX, 1.0f / shadowmapSizeY);

                                /*for (int ShadowIndex = 0; ShadowIndex < DirectionalLightShadowInfos.Num(); ShadowIndex++) {
									const
                                    FProjectedShadowInfo * ShadowInfo = DirectionalLightShadowInfos[ShadowIndex];
									const
                                    int32 CascadeIndex = ShadowInfo -> CascadeSettings.ShadowSplitIndex;

                                    if (ShadowInfo -> IsWholeSceneDirectionalShadow() && ShadowInfo -> bAllocated && CascadeIndex < GMaxForwardShadowCascades) {
                                        ForwardLightData.NumDirectionalLightCascades++;
                                        ForwardLightData.DirectionalLightWorldToShadowMatrix[CascadeIndex] = ShadowInfo -> GetWorldToShadowMatrix(ForwardLightData.DirectionalLightShadowmapMinMax[CascadeIndex]);
                                        ForwardLightData.CascadeEndDepths[CascadeIndex] = ShadowInfo -> CascadeSettings.SplitFar;

                                        if (CascadeIndex == 0) {
                                            ForwardLightData.DirectionalLightShadowmapAtlas = ShadowInfo -> RenderTargets.DepthTarget->
                                            GetRenderTargetItem().ShaderResourceTexture.GetReference();
                                            ForwardLightData.DirectionalLightDepthBias = ShadowInfo -> GetShaderDepthBias();
                                            FVector2D AtlasSize = ShadowInfo -> RenderTargets.DepthTarget->
                                            GetDesc().Extent;
                                            ForwardLightData.DirectionalLightShadowmapAtlasBufferSize = FVector4(AtlasSize.X, AtlasSize.Y, 1.0f / AtlasSize.X, 1.0f / AtlasSize.Y);
                                        }
                                    }
                                }*/
                            }

							/*const
                            FStaticShadowDepthMap * StaticShadowDepthMap = LightSceneInfo -> Proxy -> GetStaticShadowDepthMap();
							const
                            uint32 bStaticallyShadowedValue = LightSceneInfo -> IsPrecomputedLightingValid() && StaticShadowDepthMap && StaticShadowDepthMap -> Data && StaticShadowDepthMap -> TextureRHI ? 1 : 0;

                            ForwardLightData.DirectionalLightUseStaticShadowing = bStaticallyShadowedValue;
                            ForwardLightData.DirectionalLightStaticShadowBufferSize = bStaticallyShadowedValue ? FVector4(StaticShadowDepthMap -> Data -> ShadowMapSizeX, StaticShadowDepthMap -> Data -> ShadowMapSizeY, 1.0f / StaticShadowDepthMap -> Data -> ShadowMapSizeX, 1.0f / StaticShadowDepthMap -> Data -> ShadowMapSizeY) : FVector4(0, 0, 0, 0);
                            ForwardLightData.DirectionalLightWorldToStaticShadow = bStaticallyShadowedValue ? StaticShadowDepthMap -> Data -> WorldToLight : FMatrix::Identity;
                            ForwardLightData.DirectionalLightStaticShadowmap = bStaticallyShadowedValue ? StaticShadowDepthMap -> TextureRHI : GWhiteTexture -> TextureRHI;*/
                        }
                    }
                }
            }
        }
    }

    private Vector3f GetLightGridZParams(float NearPlane, float FarPlane)
    {
        // S = distribution scale
        // B, O are solved for given the z distances of the first+last slice, and the # of slices.
        //
        // slice = log2(z*B + O) * S

        // Don't spend lots of resolution right in front of the near plane
        double NearOffset = .095 * 100;
        // Space out the slices so they aren't all clustered at the near plane
        double S = 4.05;

        double N = NearPlane + NearOffset;
        double F = FarPlane;

        double O = (F - N * Numeric.exp2((mParams.GLightGridSizeZ - 1) / S)) / (F - N);
        double B = (1 - O) / N;

        return new Vector3f((float)B, (float)O, (float)S);
    }
}
