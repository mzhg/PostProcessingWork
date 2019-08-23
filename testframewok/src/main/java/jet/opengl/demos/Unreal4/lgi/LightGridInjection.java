package jet.opengl.demos.Unreal4.lgi;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import jet.opengl.demos.Unreal4.FForwardLightData;
import jet.opengl.demos.Unreal4.FForwardLocalLightData;
import jet.opengl.demos.Unreal4.TextureBuffer;
import jet.opengl.demos.Unreal4.UE4Engine;
import jet.opengl.demos.Unreal4.UE4LightCollections;
import jet.opengl.demos.Unreal4.UE4LightInfo;
import jet.opengl.demos.Unreal4.UE4View;
import jet.opengl.demos.intel.fluid.scene.Light;
import jet.opengl.demos.intel.va.VaBoundingSphere;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.volumetricLighting.LightType;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.LogUtil;
import jet.opengl.postprocessing.util.Numeric;
import jet.opengl.postprocessing.util.RecycledPool;

public class LightGridInjection {

    private static final String SHADER_PATH = UE4Engine.SHADER_PATH + "LightGridInjection/";

    private static final int INDEX_NONE = -1;
    int NumCulledLightsGridStride = 2;
    int NumCulledGridPrimitiveTypes = 2;
    int LightLinkStride = 2;

    private static final int LightGridInjectionGroupSize = 4;

    private final TextureBuffer CulledLightLinks = new TextureBuffer("CulledLightLinks");
    private final TextureBuffer NextCulledLightLink = new TextureBuffer("NextCulledLightLink");
    private final TextureBuffer StartOffsetGrid = new TextureBuffer("StartOffsetGrid");
    private final TextureBuffer NextCulledLightData = new TextureBuffer("NextCulledLightData");

    private FLightGridCompactCS mLightGridCompactCS;
    private TLightGridInjectionCS mLightGridInjectionCS0;
    private TLightGridInjectionCS mLightGridInjectionCS1;

    private boolean m_printOnce;

    private final ArrayList<FForwardLocalLightData> ForwardLocalLightData = new ArrayList<>();
    private final RecycledPool<FForwardLocalLightData> mRecycledPool = new RecycledPool(()-> new FForwardLocalLightData(), 32);

    public static final class Params extends UE4LightCollections {
        /** "Size of a cell in the light grid, in pixels.  r.Forward.LightGridPixelSize" */
        public int GLightGridPixelSize = 64;

        /** Number of Z slices in the light grid.  r.Forward.LightGridSizeZ */
        public int GLightGridSizeZ = 32;

        /** Controls how much memory is allocated for each cell for light culling.  When r.Forward.LightLinkedListCulling is enabled, this is used to compute a global max instead of a per-cell limit on culled lights. r.Forward.MaxCulledLightsPerCell*/
        public int GMaxCulledLightsPerCell = 32;

        /** Uses a reverse linked list to store culled lights, removing the fixed limit on how many lights can affect a cell - it becomes a global limit instead. r.Forward.LightLinkedListCulling*/
        public boolean GLightLinkedListCulling = true;

        /** Whether to run compute light culling pass. 0: off, 1: on (default), r.LightCulling.Quality */
        public boolean GLightCullingQuality = true;

        public TextureGL shadowmap;
    }

    private Params mParams;
    public void computeLightGrid(Params params, UE4View View){
        if(!GLFuncProviderFactory.isInitlized())
            return;

        final GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();

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
            FForwardLightData ForwardLightData = View.ForwardLightingResources.ForwardLightData;
            ForwardLightData.reset();

//            TArray<FForwardLocalLightData, SceneRenderingAllocator> ForwardLocalLightData;
            float FurthestLight = 1000;

            if (bCullLightsToGrid) {
//                ForwardLocalLightData.Empty(Scene->Lights.Num() + SimpleLights.InstanceData.Num());
                ForwardLocalLightData.clear();
                ForwardLocalLightData.ensureCapacity(SimpleLights.size());
                mRecycledPool.ensureCapacity(SimpleLights.size());

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

                            FForwardLocalLightData LightData = mRecycledPool.obtain();
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
                                    -(View.TranslatedWorldToView.m02 * BoundingSphere.Center.x + View.TranslatedWorldToView.m12 * BoundingSphere.Center.y + View.TranslatedWorldToView.m22 * BoundingSphere.Center.y) + BoundingSphere.Radius;

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
                                ForwardLightData.CascadeEndDepths[0] = View.FarClippingDistance;
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

                /*
                // Pack both values into a single float to keep float4 alignment
				const FFloat16 SimpleLightSourceLength16f = FFloat16(0);
                FLightingChannels SimpleLightLightingChannels;
                // Put simple lights in all lighting channels
                SimpleLightLightingChannels.bChannel0 = SimpleLightLightingChannels.bChannel1 = SimpleLightLightingChannels.bChannel2 = true;
				const uint32 SimpleLightLightingChannelMask = GetLightingChannelMaskForStruct(SimpleLightLightingChannels);

                for (int32 SimpleLightIndex = 0; SimpleLightIndex < SimpleLights.InstanceData.Num(); SimpleLightIndex++)
                {
                    ForwardLocalLightData.AddUninitialized(1);
                    FForwardLocalLightData& LightData = ForwardLocalLightData.Last();

					const FSimpleLightEntry& SimpleLight = SimpleLights.InstanceData[SimpleLightIndex];
					const FSimpleLightPerViewEntry& SimpleLightPerViewData = SimpleLights.GetViewDependentData(SimpleLightIndex, ViewIndex, Views.Num());
                    LightData.LightPositionAndInvRadius = FVector4(SimpleLightPerViewData.Position, 1.0f / FMath::Max(SimpleLight.Radius, KINDA_SMALL_NUMBER));
                    LightData.LightColorAndFalloffExponent = FVector4(SimpleLight.Color, SimpleLight.Exponent);

                    // No shadowmap channels for simple lights
                    uint32 ShadowMapChannelMask = 0;
                    ShadowMapChannelMask |= SimpleLightLightingChannelMask << 8;

                    LightData.LightDirectionAndShadowMapChannelMask = FVector4(FVector(1, 0, 0), *((float*)&ShadowMapChannelMask));

                    // Pack both values into a single float to keep float4 alignment
					const FFloat16 VolumetricScatteringIntensity16f = FFloat16(SimpleLight.VolumetricScatteringIntensity);
					const uint32 PackedWInt = ((uint32)SimpleLightSourceLength16f.Encoded) | ((uint32)VolumetricScatteringIntensity16f.Encoded << 16);

                    LightData.SpotAnglesAndSourceRadiusPacked = FVector4(-2, 1, 0, *(float*)&PackedWInt);
                    LightData.LightTangentAndSoftSourceRadius = FVector4(1.0f, 0.0f, 0.0f, 0.0f);
                }*/
            }

            // Store off the number of lights before we add a fake entry
			final int NumLocalLightsFinal = ForwardLocalLightData.size();

            if (ForwardLocalLightData.size() == 0)
            {
                // Make sure the buffer gets created even though we're not going to read from it in the shader, for platforms like PS4 that assert on null resources being bound
                ForwardLocalLightData.add(mRecycledPool.obtain());
            }

            { // TODO Create and fill the ForwardLocalLightData
				final int NumBytesRequired = ForwardLocalLightData.size() * ForwardLocalLightData.get(0).sizeInBytes();

                if (View.ForwardLightingResources.ForwardLocalLightBuffer.NumBytes < NumBytesRequired)
                {
                    View.ForwardLightingResources.ForwardLocalLightBuffer.Release();
                    View.ForwardLightingResources.ForwardLocalLightBuffer.Initialize(Vector4f.SIZE, NumBytesRequired / Vector4f.SIZE, GLenum.GL_RGBA32F, UE4Engine.BUF_Volatile);
                }

                ForwardLightData.ForwardLocalLightBuffer = View.ForwardLightingResources.ForwardLocalLightBuffer;
//                View.ForwardLightingResources.ForwardLocalLightBuffer.Lock();  TODO
//                FPlatformMemory::Memcpy(View.ForwardLightingResources->ForwardLocalLightBuffer.MappedBuffer, ForwardLocalLightData.GetData(), ForwardLocalLightData.Num() * ForwardLocalLightData.GetTypeSize());
//                View.ForwardLightingResources.ForwardLocalLightBuffer.Unlock();

                ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(NumBytesRequired);
                CacheBuffer.put(buffer, ForwardLocalLightData);
                buffer.flip();
                View.ForwardLightingResources.ForwardLocalLightBuffer.update(0, buffer);
            }

//			const FIntPoint LightGridSizeXY = FIntPoint::DivideAndRoundUp(View.ViewRect.Size(), GLightGridPixelSize);
            final int LightGridSizeX = Numeric.divideAndRoundUp(View.ViewRect.width, mParams.GLightGridPixelSize);
            final int LightGridSizeY = Numeric.divideAndRoundUp(View.ViewRect.height, mParams.GLightGridPixelSize);
            ForwardLightData.NumLocalLights = NumLocalLightsFinal;
            ForwardLightData.NumReflectionCaptures = 0; //View.NumBoxReflectionCaptures + View.NumSphereReflectionCaptures;
            ForwardLightData.NumGridCells = LightGridSizeX * LightGridSizeY * mParams.GLightGridSizeZ;
            ForwardLightData.CulledGridSize.set(LightGridSizeX, LightGridSizeY, mParams.GLightGridSizeZ);
            ForwardLightData.MaxCulledLightsPerCell = mParams.GMaxCulledLightsPerCell;
            ForwardLightData.LightGridPixelSizeShift = (int)Numeric.log2(mParams.GLightGridPixelSize);

            // Clamp far plane to something reasonable
            float FarPlane = Math.min(Math.max(FurthestLight, View.FurthestReflectionCaptureDistance), UE4Engine.HALF_WORLD_MAX / 5.0f);
            Vector3f ZParams = GetLightGridZParams(View.NearClippingDistance, FarPlane + 10.f);
            ForwardLightData.LightGridZParams.set(ZParams);

			final long NumIndexableLights = 2048; //CHANGE_LIGHTINDEXTYPE_SIZE && !bAllowFormatConversion ? (1llu << (sizeof(FLightIndexType32) * 8llu)) : (1llu << (sizeof(FLightIndexType) * 8llu));

            if (ForwardLocalLightData.size() > NumIndexableLights)
            {
                /*static bool bWarned = false;
                if (!bWarned)
                {
                    UE_LOG(LogRenderer, Warning, TEXT("Exceeded indexable light count, glitches will be visible (%u / %llu)"), ForwardLocalLightData.Num(), NumIndexableLights);
                    bWarned = true;
                }*/

                LogUtil.i(LogUtil.LogType.DEFAULT, String.format("Exceeded indexable light count, glitches will be visible (%d / %d)", ForwardLocalLightData.size(), (int)NumIndexableLights));
            }

        }

        final int PF_R32_UINT = GLenum.GL_R32UI;
        final int PF_R16_UINT = GLenum.GL_R16UI;

        //            const SIZE_T LightIndexTypeSize = CHANGE_LIGHTINDEXTYPE_SIZE && !bAllowFormatConversion ? sizeof(FLightIndexType32) : sizeof(FLightIndexType);
        final int LightIndexTypeSize = 4;

//       for (int32 ViewIndex = 0; ViewIndex < Views.Num(); ViewIndex++)
        {
//                FViewInfo& View = Views[ViewIndex];
            FForwardLightData ForwardLightData = View.ForwardLightingResources.ForwardLightData;

//            const FIntPoint LightGridSizeXY = FIntPoint::DivideAndRoundUp(View.ViewRect.Size(), GLightGridPixelSize);
            final int LightGridSizeX = Numeric.divideAndRoundUp(View.ViewRect.width, mParams.GLightGridPixelSize);
            final int LightGridSizeY = Numeric.divideAndRoundUp(View.ViewRect.height, mParams.GLightGridPixelSize);

            final int NumCells = LightGridSizeX * LightGridSizeY * mParams.GLightGridSizeZ * NumCulledGridPrimitiveTypes;

            if (View.ForwardLightingResources.NumCulledLightsGrid.NumBytes != NumCells * NumCulledLightsGridStride * 4/*sizeof(uint32)*/)
            {
//                UE_CLOG(NumCells * NumCulledLightsGridStride * sizeof(uint32) > 256llu * (1llu << 20llu), LogRenderer, Warning,
//                        TEXT("Attempt to allocate large FRWBuffer (not supported by Metal): View.ForwardLightingResources->NumCulledLightsGrid %u Bytes, LightGridSize %dx%dx%d, NumCulledGridPrimitiveTypes %d, NumCells %d, NumCulledLightsGridStride %d, View Resolution %dx%d"),
//                        NumCells * NumCulledLightsGridStride * sizeof(uint32), LightGridSizeXY.X, LightGridSizeXY.Y, GLightGridSizeZ, NumCulledGridPrimitiveTypes, NumCells, NumCulledLightsGridStride, View.ViewRect.Size().X, View.ViewRect.Size().Y);

                View.ForwardLightingResources.NumCulledLightsGrid.Initialize(/*sizeof(uint32)*/4, NumCells * NumCulledLightsGridStride, PF_R32_UINT);
            }

            if (View.ForwardLightingResources.CulledLightDataGrid.NumBytes != NumCells *mParams.GMaxCulledLightsPerCell  * LightIndexTypeSize)
            {
//                UE_CLOG(NumCells * GMaxCulledLightsPerCell * sizeof(FLightIndexType) > 256llu * (1llu << 20llu), LogRenderer, Warning,
//                        TEXT("Attempt to allocate large FRWBuffer (not supported by Metal): View.ForwardLightingResources->CulledLightDataGrid %u Bytes, LightGridSize %dx%dx%d, NumCulledGridPrimitiveTypes %d, NumCells %d, GMaxCulledLightsPerCell %d, View Resolution %dx%d"),
//                        NumCells * GMaxCulledLightsPerCell * sizeof(FLightIndexType), LightGridSizeXY.X, LightGridSizeXY.Y, GLightGridSizeZ, NumCulledGridPrimitiveTypes, NumCells, GMaxCulledLightsPerCell, View.ViewRect.Size().X, View.ViewRect.Size().Y);

                View.ForwardLightingResources.CulledLightDataGrid.Initialize(LightIndexTypeSize, NumCells * mParams.GMaxCulledLightsPerCell, LightIndexTypeSize == 2 ? PF_R16_UINT : PF_R32_UINT);
            }

//            final boolean bShouldCacheTemporaryBuffers = View.ViewState != nullptr;
//            FForwardLightingCullingResources LocalCullingResources;
//            FForwardLightingCullingResources& ForwardLightingCullingResources = bShouldCacheTemporaryBuffers ? View.ViewState->ForwardLightingCullingResources : LocalCullingResources;

            final int CulledLightLinksElements = NumCells * mParams.GMaxCulledLightsPerCell * LightLinkStride;
            if (CulledLightLinks.NumBytes != (CulledLightLinksElements * /*sizeof(uint32)*/4)
                    || (/*GFastVRamConfig.bDirty &&*/ CulledLightLinks.NumBytes > 0))
            {
//                UE_CLOG(CulledLightLinksElements * sizeof(uint32) > 256llu * (1llu << 20llu), LogRenderer, Warning,
//                        TEXT("Attempt to allocate large FRWBuffer (not supported by Metal): ForwardLightingCullingResources.CulledLightLinks %u Bytes, LightGridSize %dx%dx%d, NumCulledGridPrimitiveTypes %d, NumCells %d, GMaxCulledLightsPerCell %d, LightLinkStride %d, View Resolution %dx%d"),
//                        CulledLightLinksElements * sizeof(uint32), LightGridSizeXY.X, LightGridSizeXY.Y, GLightGridSizeZ, NumCulledGridPrimitiveTypes, NumCells, GMaxCulledLightsPerCell, LightLinkStride, View.ViewRect.Size().X, View.ViewRect.Size().Y);

				final int FastVRamFlag = 0; //GFastVRamConfig.ForwardLightingCullingResources | (IsTransientResourceBufferAliasingEnabled() ? BUF_Transient : BUF_None);
                CulledLightLinks.Initialize(/*sizeof(uint32)*/4, CulledLightLinksElements, PF_R32_UINT, FastVRamFlag);
                NextCulledLightLink.Initialize(/*sizeof(uint32)*/4, 1, PF_R32_UINT, FastVRamFlag);
                StartOffsetGrid.Initialize(/*sizeof(uint32)*/4, NumCells, PF_R32_UINT, FastVRamFlag);
                NextCulledLightData.Initialize(/*sizeof(uint32)*/4, 1, PF_R32_UINT, FastVRamFlag);
            }

            ForwardLightData.NumCulledLightsGrid = View.ForwardLightingResources.NumCulledLightsGrid;
            ForwardLightData.CulledLightDataGrid = View.ForwardLightingResources.CulledLightDataGrid;

            // TODO
//            View.ForwardLightingResources.ForwardLightDataUniformBuffer = TUniformBufferRef<FForwardLightData>::CreateUniformBufferImmediate(ForwardLightData, UniformBuffer_SingleFrame);
            View.updateForwardLightData();
            View.bindResources();
            /*if (IsTransientResourceBufferAliasingEnabled())
            {
                // Acquire resources
                ForwardLightingCullingResources.CulledLightLinks.AcquireTransientResource();
                ForwardLightingCullingResources.NextCulledLightLink.AcquireTransientResource();
                ForwardLightingCullingResources.StartOffsetGrid.AcquireTransientResource();
                ForwardLightingCullingResources.NextCulledLightData.AcquireTransientResource();
            }*/

//            const FIntVector NumGroups = FIntVector::DivideAndRoundUp(FIntVector(LightGridSizeXY.X, LightGridSizeXY.Y, GLightGridSizeZ), LightGridInjectionGroupSize);
            final int NumGroupX = Numeric.divideAndRoundUp(LightGridSizeX, LightGridInjectionGroupSize);
            final int NumGroupY = Numeric.divideAndRoundUp(LightGridSizeY, LightGridInjectionGroupSize);
            final int NumGroupZ = Numeric.divideAndRoundUp(mParams.GLightGridSizeZ, LightGridInjectionGroupSize);
            {
                /*SCOPED_DRAW_EVENTF(RHICmdList, CullLights, TEXT("CullLights %ux%ux%u NumLights %u NumCaptures %u"),
                        ForwardLightData.CulledGridSize.X,
                        ForwardLightData.CulledGridSize.Y,
                        ForwardLightData.CulledGridSize.Z,
                        ForwardLightData.NumLocalLights,
                        ForwardLightData.NumReflectionCaptures);*/

                /*TArray<FUnorderedAccessViewRHIParamRef, TInlineAllocator<6>> OutUAVs;  todo UVAs
                OutUAVs.Add(View.ForwardLightingResources->NumCulledLightsGrid.UAV);
                OutUAVs.Add(View.ForwardLightingResources->CulledLightDataGrid.UAV);
                OutUAVs.Add(ForwardLightingCullingResources.NextCulledLightLink.UAV);
                OutUAVs.Add(ForwardLightingCullingResources.StartOffsetGrid.UAV);
                OutUAVs.Add(ForwardLightingCullingResources.CulledLightLinks.UAV);
                OutUAVs.Add(ForwardLightingCullingResources.NextCulledLightData.UAV);
                RHICmdList.TransitionResources(EResourceTransitionAccess::EWritable, EResourceTransitionPipeline::EGfxToCompute, OutUAVs.GetData(), OutUAVs.Num());*/

                gl.glBindBufferBase(GLenum.GL_ATOMIC_COUNTER_BUFFER, 2, NextCulledLightLink.getBuffer());
                gl.glBindImageTexture(3,  StartOffsetGrid.getTexture(),0, false, 0, GLenum.GL_READ_WRITE, StartOffsetGrid.InternalFormat);
                gl.glBindImageTexture(4,  CulledLightLinks.getTexture(),0, false, 0, GLenum.GL_READ_WRITE, CulledLightLinks.InternalFormat);

                if (mParams.GLightLinkedListCulling)
                {
//                    ClearUAV(RHICmdList, ForwardLightingCullingResources.StartOffsetGrid, 0xFFFFFFFF);
//                    ClearUAV(RHICmdList, ForwardLightingCullingResources.NextCulledLightLink, 0);
//                    ClearUAV(RHICmdList, ForwardLightingCullingResources.NextCulledLightData, 0);
                    GLCheck.checkError();
                    gl.glClearNamedBufferData(StartOffsetGrid.getBuffer(), StartOffsetGrid.InternalFormat, GLenum.GL_RED, GLenum.GL_UNSIGNED_INT, CacheBuffer.wrap(0xFFFFFFFF,0xFFFFFFFF,0xFFFFFFFF,0xFFFFFFFF));
                    gl.glClearNamedBufferData(NextCulledLightLink.getBuffer(), NextCulledLightLink.InternalFormat, GLenum.GL_RED, GLenum.GL_UNSIGNED_INT, null);
                    gl.glClearNamedBufferData(NextCulledLightData.getBuffer(), NextCulledLightData.InternalFormat, GLenum.GL_RED, GLenum.GL_UNSIGNED_INT, null);

                    /*TShaderMapRef<TLightGridInjectionCS<true> > ComputeShader(View.ShaderMap);
                    RHICmdList.SetComputeShader(ComputeShader->GetComputeShader());
                    ComputeShader->SetParameters(RHICmdList, View, ForwardLightingCullingResources);
                    DispatchComputeShader(RHICmdList, *ComputeShader, NumGroups.X, NumGroups.Y, NumGroups.Z);
                    ComputeShader->UnsetParameters(RHICmdList, View, ForwardLightingCullingResources);*/
                    if(mLightGridInjectionCS1 == null){
                        mLightGridInjectionCS1 = new TLightGridInjectionCS(SHADER_PATH, LightGridInjectionGroupSize, true);
                        mLightGridInjectionCS1.setName("LightGridInjectionCS1");
                    }
                    mLightGridInjectionCS1.enable();
                    gl.glDispatchCompute(NumGroupX, NumGroupY, NumGroupZ);

                    if(!m_printOnce)
                        mLightGridInjectionCS1.printPrograminfo();
                }
                else
                {
//                    ClearUAV(RHICmdList, View.ForwardLightingResources->NumCulledLightsGrid, 0);

                    gl.glClearNamedBufferData(NextCulledLightData.getBuffer(), NextCulledLightData.InternalFormat, GLenum.GL_UNSIGNED_INT, GLenum.GL_RED, null);
                   /* TShaderMapRef<TLightGridInjectionCS<false> > ComputeShader(View.ShaderMap);
                    RHICmdList.SetComputeShader(ComputeShader->GetComputeShader());
                    ComputeShader->SetParameters(RHICmdList, View, ForwardLightingCullingResources);
                    DispatchComputeShader(RHICmdList, *ComputeShader, NumGroups.X, NumGroups.Y, NumGroups.Z);
                    ComputeShader->UnsetParameters(RHICmdList, View, ForwardLightingCullingResources);*/

                    if(mLightGridInjectionCS0 == null){
                        mLightGridInjectionCS0 = new TLightGridInjectionCS(SHADER_PATH, LightGridInjectionGroupSize, false);
                        mLightGridInjectionCS0.setName("LightGridInjectionCS0");
                    }
                    mLightGridInjectionCS0.enable();
                    gl.glDispatchCompute(NumGroupX, NumGroupY, NumGroupZ);

                    if(!m_printOnce)
                        mLightGridInjectionCS0.printPrograminfo();
                }

                gl.glBindBufferBase(GLenum.GL_ATOMIC_COUNTER_BUFFER, 2, 0);
                gl.glBindImageTexture(3,  0,0, false, 0, GLenum.GL_READ_WRITE, StartOffsetGrid.InternalFormat);
                gl.glBindImageTexture(4,  0,0, false, 0, GLenum.GL_READ_WRITE, CulledLightLinks.InternalFormat);
            }

            if (mParams.GLightLinkedListCulling) {
//                SCOPED_DRAW_EVENT(RHICmdList, Compact);

                /*TShaderMapRef<FLightGridCompactCS> ComputeShader(View.ShaderMap);
                RHICmdList.SetComputeShader(ComputeShader->GetComputeShader());
                ComputeShader->SetParameters(RHICmdList, View, ForwardLightingCullingResources);
                DispatchComputeShader(RHICmdList, *ComputeShader, NumGroups.X, NumGroups.Y, NumGroups.Z);
                ComputeShader->UnsetParameters(RHICmdList, View, ForwardLightingCullingResources);*/
                if(mLightGridCompactCS == null){
                    mLightGridCompactCS = new FLightGridCompactCS(SHADER_PATH, LightGridInjectionGroupSize, UE4Engine.GMaxNumReflectionCaptures);
                    mLightGridCompactCS.setName("LightGridCompactCS");
                }

                gl.glBindImageTexture(0,  View.ForwardLightingResources.NumCulledLightsGrid.getTexture(),0, false, 0, GLenum.GL_READ_WRITE, View.ForwardLightingResources.NumCulledLightsGrid.InternalFormat);
                gl.glBindImageTexture(1,  View.ForwardLightingResources.CulledLightDataGrid.getTexture(),0, false, 0, GLenum.GL_READ_WRITE, View.ForwardLightingResources.CulledLightDataGrid.InternalFormat);

                gl.glBindTextureUnit(1, StartOffsetGrid.getTexture());
                gl.glBindTextureUnit(2, CulledLightLinks.getTexture());

                gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 0, NextCulledLightData.getBuffer());

                mLightGridCompactCS.enable();
                gl.glDispatchCompute(NumGroupX, NumGroupY, NumGroupZ);

                if(!m_printOnce)
                    mLightGridCompactCS.printPrograminfo();

                gl.glBindImageTexture(0,  0,0, false, 0, GLenum.GL_READ_WRITE, View.ForwardLightingResources.NumCulledLightsGrid.InternalFormat);
                gl.glBindImageTexture(1,  0,0, false, 0, GLenum.GL_READ_WRITE, View.ForwardLightingResources.CulledLightDataGrid.InternalFormat);
                gl.glBindTextureUnit(1, 0);
                gl.glBindTextureUnit(2, 0);
                gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 0, 0);
            }

            /*if (IsTransientResourceBufferAliasingEnabled())
            {
                ForwardLightingCullingResources.CulledLightLinks.DiscardTransientResource();
                ForwardLightingCullingResources.NextCulledLightLink.DiscardTransientResource();
                ForwardLightingCullingResources.StartOffsetGrid.DiscardTransientResource();
                ForwardLightingCullingResources.NextCulledLightData.DiscardTransientResource();
            }*/
        }

        if(!m_printOnce){
            GLCheck.checkError();
            m_printOnce = true;
        }
    }

    private Vector3f GetLightGridZParams(float NearPlane, float FarPlane) {
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
