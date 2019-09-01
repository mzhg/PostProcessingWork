package jet.opengl.renderer.Unreal4.distancefield;

import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3i;
import org.lwjgl.util.vector.Vector4f;

import java.util.List;

import jet.opengl.renderer.Unreal4.scenes.FScene;
import jet.opengl.renderer.Unreal4.FViewInfo;
import jet.opengl.renderer.Unreal4.UE4Engine;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.RenderTexturePool;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.texture.Texture3DDesc;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.util.BoundingBox;
import jet.opengl.postprocessing.util.Numeric;

import static jet.opengl.renderer.Unreal4.distancefield.EVolumeUpdateType.VUT_Heightfields;

public class GlobalDistanceField {
    public static final int
    GDF_MostlyStatic = 0,
    GDF_Full = 1,
    GDF_Num = 2;

    private static final int
            Flatten_XAxis = 0,
            Flatten_YAxis = 1,
            Flatten_ZAxis = 2,
            Flatten_None = 3;

    private static final int GCullGridTileSize = 16;

    public static class Params{
        public int GAOGlobalDFResolution;
        /** Exponent used to derive each clipmap's size, together with r.AOInnerGlobalDFClipmapDistance. ----------r.AOGlobalDFClipmapDistanceExponent */
        public float GAOGlobalDFClipmapDistanceExponent = 2;
        /** Whether to cache mostly static primitives separately from movable primitives, which reduces global DF update cost when a movable primitive is modified.  Adds another 12Mb of volume textures.<br>-----------r.AOGlobalDistanceFieldCacheMostlyStaticSeparately*/
        public boolean GAOGlobalDistanceFieldCacheMostlyStaticSeparately = true;

        /** Whether to force full global distance field update every frame. -------------r.AOGlobalDistanceFieldForceFullUpdate*/
        public boolean GAOGlobalDistanceFieldForceFullUpdate = false;
        /** Whether to allow the larger clipmaps to be updated less frequently.  ----------r.AOGlobalDistanceFieldStaggeredUpdates*/
        public boolean GAOGlobalDistanceFieldStaggeredUpdates=true;
        /** Whether to allow partial updates of the global distance field.  When profiling it's useful to disable this and get the worst case composition time that happens on camera cuts*/
        public boolean GAOGlobalDistanceFieldPartialUpdates = true;
        /** Whether to update the global distance field, useful for debugging.*/
        public boolean GAOUpdateGlobalDistanceField = true;

        /**
         * World space distance along a cone trace to switch to using the global distance field instead of the object distance fields.
         * This has to be large enough to hide the low res nature of the global distance field, but smaller values result in faster cone tracing.
         */
        public float GAOGlobalDFStartDistance = 100;
    }

    private Params mParams;
    private boolean m_printOnce;

    private float ComputeClipmapExtent(int ClipmapIndex, FScene Scene)
    {
	    final double InnerClipmapDistance = Scene.GlobalDistanceFieldViewDistance / Math.pow(mParams.GAOGlobalDFClipmapDistanceExponent, 3);
        return (float) (InnerClipmapDistance * Math.pow(mParams.GAOGlobalDFClipmapDistanceExponent, ClipmapIndex));
    }

    private static int GetCompositeTileSize(int Dimension, int FlattenedDimension)
    {
        if (FlattenedDimension == Flatten_None)
        {
            return 4;
        }

        return Dimension == FlattenedDimension ? 1 : 8;
    }

    private static int GetCompositeIndex(boolean flag, int dimension){
        return flag ? (4 + dimension) : dimension;
    }

    private TextureGL AllocateClipmapTexture(/*FRHICommandListImmediate& RHICmdList,*/ int ClipmapIndex, int CacheType/*, TRefCountPtr<IPooledRenderTarget>& Texture*/)
    {
	    String TextureName = CacheType == GDF_MostlyStatic ? ("MostlyStaticGlobalDistanceField0") :("GlobalDistanceField0");

        if (ClipmapIndex == 1)
        {
            TextureName = CacheType == GDF_MostlyStatic ? ("MostlyStaticGlobalDistanceField1") : ("GlobalDistanceField1");
        }
        else if (ClipmapIndex == 2)
        {
            TextureName = CacheType == GDF_MostlyStatic ? ("MostlyStaticGlobalDistanceField2") : ("GlobalDistanceField2");
        }
        else if (ClipmapIndex == 3)
        {
            TextureName = CacheType == GDF_MostlyStatic ? ("MostlyStaticGlobalDistanceField3") : ("GlobalDistanceField3");
        }

        /*FPooledRenderTargetDesc VolumeDesc = FPooledRenderTargetDesc(FPooledRenderTargetDesc::CreateVolumeDesc(
            GAOGlobalDFResolution,
            GAOGlobalDFResolution,
            GAOGlobalDFResolution,
            PF_R16F,
            FClearValueBinding::None,
            0,
            // TexCreate_ReduceMemoryWithTilingMode used because 128^3 texture comes out 4x bigger on PS4 with recommended volume texture tiling modes
            TexCreate_ShaderResource | TexCreate_RenderTargetable | TexCreate_UAV | TexCreate_ReduceMemoryWithTilingMode,
            false));
        VolumeDesc.AutoWritable = false;*/
        Texture3DDesc VolumeDesc = new Texture3DDesc(mParams.GAOGlobalDFResolution, mParams.GAOGlobalDFResolution, mParams.GAOGlobalDFResolution, 1, GLenum.GL_R16F);

        /*GRenderTargetPool.FindFreeElement(
                RHICmdList,
                VolumeDesc,
                Texture,
                TextureName,
                true,
                ERenderTargetTransience::NonTransient
        );*/

        return RenderTexturePool.getInstance().findFreeElement(VolumeDesc);
    }

    /** Constructs and adds an update region based on camera movement for the given axis. */
    private void AddUpdateRegionForAxis(Vector3i Movement, BoundingBox ClipmapBounds, float CellSize, int ComponentIndex, List<FVolumeUpdateRegion> UpdateRegions)
    {
        FVolumeUpdateRegion UpdateRegion = new FVolumeUpdateRegion();
        UpdateRegion.Bounds.set(ClipmapBounds);
        UpdateRegion.CellsSize.set(mParams.GAOGlobalDFResolution,mParams.GAOGlobalDFResolution, mParams.GAOGlobalDFResolution);
        UpdateRegion.CellsSize.setValue(ComponentIndex, Math.min(Math.abs(Movement.get(ComponentIndex)), mParams.GAOGlobalDFResolution));

        if (Movement.get(ComponentIndex) > 0)
        {
            // Positive axis movement, set the min of that axis to contain the newly exposed area
//            UpdateRegion.Bounds.Min[ComponentIndex] = Math.max(ClipmapBounds.Max[ComponentIndex] - Movement[ComponentIndex] * CellSize, ClipmapBounds.Min[ComponentIndex]);
            float value = Math.max(ClipmapBounds._max.get(ComponentIndex) - Movement.get(ComponentIndex) * CellSize, ClipmapBounds._min.get(ComponentIndex));
            UpdateRegion.Bounds._min.setValue(ComponentIndex, value);
        }
        else if (Movement.get(ComponentIndex) < 0)
        {
            // Negative axis movement, set the max of that axis to contain the newly exposed area
//            UpdateRegion.Bounds.Max[ComponentIndex] = Math.min(ClipmapBounds.Min[ComponentIndex] - Movement[ComponentIndex] * CellSize, ClipmapBounds.Max[ComponentIndex]);
            float value = Math.min(ClipmapBounds._min.get(ComponentIndex) - Movement.get(ComponentIndex) * CellSize, ClipmapBounds._max.get(ComponentIndex));
            UpdateRegion.Bounds._max.setValue(ComponentIndex, value);
        }

        if (UpdateRegion.CellsSize.get(ComponentIndex) > 0)
        {
            UpdateRegions.add(UpdateRegion);
        }
    }

    private long GetUpdateFrequencyForClipmap(int ClipmapIndex/*, int32& OutFrequency, int32& OutPhase*/)
    {
        int OutFrequency = 1;
        int OutPhase = 0;

        if (ClipmapIndex == 0 || !mParams.GAOGlobalDistanceFieldStaggeredUpdates)
        {
            OutFrequency = 1;
            OutPhase = 0;
        }
        else if (ClipmapIndex == 1)
        {
            OutFrequency = 2;
            OutPhase = 0;
        }
        else if (ClipmapIndex == 2)
        {
            OutFrequency = 4;
            OutPhase = 1;
        }
        else
        {
            assert (ClipmapIndex == 3);
            OutFrequency = 4;
            OutPhase = 3;
        }

        return Numeric.encode(OutFrequency, OutPhase);
    }

    /** Staggers clipmap updates so there are only 2 per frame */
    private boolean ShouldUpdateClipmapThisFrame(int ClipmapIndex, int GlobalDistanceFieldUpdateIndex)
    {
        int Frequency;
        int Phase;
        long value = GetUpdateFrequencyForClipmap(ClipmapIndex/*, Frequency, Phase*/);

        Frequency = Numeric.decodeFirst(value);
        Phase = Numeric.decodeSecond(value);

        return GlobalDistanceFieldUpdateIndex % Frequency == Phase;
    }

    /** Constructs and adds an update region based on the given primitive bounds. */
    private void AddUpdateRegionForPrimitive(Vector4f Bounds, float MaxSphereQueryRadius, BoundingBox ClipmapBounds, float CellSize, List<FVolumeUpdateRegion> UpdateRegions)
    {
        // Object influence bounds
        BoundingBox _BoundingBox = new BoundingBox(/*(FVector)Bounds - Bounds.W - MaxSphereQueryRadius, (FVector)Bounds + Bounds.W + MaxSphereQueryRadius*/);
        _BoundingBox._min.set(Bounds.x - Bounds.w - MaxSphereQueryRadius, Bounds.y - Bounds.w - MaxSphereQueryRadius, Bounds.z - Bounds.w - MaxSphereQueryRadius);
        _BoundingBox._max.set(Bounds.x + Bounds.w + MaxSphereQueryRadius, Bounds.y + Bounds.w + MaxSphereQueryRadius, Bounds.z + Bounds.w + MaxSphereQueryRadius);

        FVolumeUpdateRegion UpdateRegion = new FVolumeUpdateRegion();
        UpdateRegion.Bounds.init();
        // Snap the min and clamp to clipmap bounds
        UpdateRegion.Bounds._min.x = Math.max(CellSize * (int)(_BoundingBox._min.x / CellSize), ClipmapBounds._min.x);
        UpdateRegion.Bounds._min.y = Math.max(CellSize * (int)(_BoundingBox._min.y / CellSize), ClipmapBounds._min.y);
        UpdateRegion.Bounds._min.z = Math.max(CellSize * (int)(_BoundingBox._min.z / CellSize), ClipmapBounds._min.z);

        // Derive the max from the snapped min and size, clamp to clipmap bounds
        UpdateRegion.Bounds._max.x = UpdateRegion.Bounds._min.x + (int)((Bounds.w + MaxSphereQueryRadius) * 2 / CellSize) * CellSize;

        UpdateRegion.Bounds._max.x = Math.min(UpdateRegion.Bounds._max.x, ClipmapBounds._max.x);
        UpdateRegion.Bounds._max.y = Math.min(UpdateRegion.Bounds._max.y, ClipmapBounds._max.y);
        UpdateRegion.Bounds._max.z = Math.min(UpdateRegion.Bounds._max.z, ClipmapBounds._max.z);

//	    const FVector UpdateRegionSize = UpdateRegion.Bounds.GetSize();
        final float UpdateRegionSizeX = (UpdateRegion.Bounds._max.x - UpdateRegion.Bounds._min.x);
        final float UpdateRegionSizeY = (UpdateRegion.Bounds._max.y - UpdateRegion.Bounds._min.y);
        final float UpdateRegionSizeZ = (UpdateRegion.Bounds._max.z - UpdateRegion.Bounds._min.z);
        UpdateRegion.CellsSize.x = (int)(UpdateRegionSizeX / CellSize + .5f);
        UpdateRegion.CellsSize.y = (int)(UpdateRegionSizeY / CellSize + .5f);
        UpdateRegion.CellsSize.z = (int)(UpdateRegionSizeZ / CellSize + .5f);

        // Only add update regions with positive area
        if (UpdateRegion.CellsSize.x > 0 && UpdateRegion.CellsSize.y > 0 && UpdateRegion.CellsSize.z > 0)
        {
            assert (UpdateRegion.CellsSize.x <= mParams.GAOGlobalDFResolution && UpdateRegion.CellsSize.y <= mParams.GAOGlobalDFResolution && UpdateRegion.CellsSize.z <= mParams.GAOGlobalDFResolution);
            UpdateRegions.add(UpdateRegion);
        }
    }

    private static void TrimOverlappingAxis(int TrimAxis, float CellSize, FVolumeUpdateRegion OtherUpdateRegion, FVolumeUpdateRegion UpdateRegion)
    {
        int OtherAxis0 = (TrimAxis + 1) % 3;
        int OtherAxis1 = (TrimAxis + 2) % 3;

        // Check if the UpdateRegion is entirely contained in 2d
        if (UpdateRegion.Bounds._max.get(OtherAxis0) <= OtherUpdateRegion.Bounds._max.get(OtherAxis0)
                && UpdateRegion.Bounds._min.get(OtherAxis0) >= OtherUpdateRegion.Bounds._min.get(OtherAxis0)
                && UpdateRegion.Bounds._max.get(OtherAxis1) <= OtherUpdateRegion.Bounds._max.get(OtherAxis1)
                && UpdateRegion.Bounds._min.get(OtherAxis1) >= OtherUpdateRegion.Bounds._min.get(OtherAxis1))
        {
            if (UpdateRegion.Bounds._min.get(TrimAxis) >= OtherUpdateRegion.Bounds._min.get(TrimAxis) && UpdateRegion.Bounds._min.get(TrimAxis) <= OtherUpdateRegion.Bounds._max.get(TrimAxis))
            {
                // Min on this axis is completely contained within the other region, clip it so there's no overlapping update region
                UpdateRegion.Bounds._min.setValue(TrimAxis, OtherUpdateRegion.Bounds._max.get(TrimAxis));
            }
            else
            {
                // otherwise Max on this axis must be inside the other region, because we know the two volumes intersect
                UpdateRegion.Bounds._max.setValue(TrimAxis, OtherUpdateRegion.Bounds._min.get(TrimAxis));
            }

            UpdateRegion.CellsSize.setValue(TrimAxis, (int)((Math.max(UpdateRegion.Bounds._max.get(TrimAxis) - UpdateRegion.Bounds._min.get(TrimAxis), 0.0f)) / CellSize + .5f));
        }
    }

    private void ComputeUpdateRegionsAndUpdateViewState(
//            FRHICommandListImmediate& RHICmdList,
	        FViewInfo View,
	        FScene Scene,
            FGlobalDistanceFieldInfo GlobalDistanceFieldInfo,
            int NumClipmaps,
            float MaxOcclusionDistance,
            float GAOConeHalfAngle)
    {
//        GlobalDistanceFieldInfo.Clipmaps.AddZeroed(NumClipmaps);
//        GlobalDistanceFieldInfo.MostlyStaticClipmaps.AddZeroed(NumClipmaps);

        for(int i = 0; i < NumClipmaps; i++){
            GlobalDistanceFieldInfo.Clipmaps.add(new FGlobalDistanceFieldClipmap());
            GlobalDistanceFieldInfo.MostlyStaticClipmaps.add(new FGlobalDistanceFieldClipmap());
        }

        // Cache the heightfields update region boxes for fast reuse for each clip region.
        /*ArrayList<BoundingBox> PendingStreamingHeightfieldBoxes = new ArrayList<>();  todo
        for (FPrimitiveSceneInfo HeightfieldPrimitive : Scene.DistanceFieldSceneData.HeightfieldPrimitives)
        {
            if (HeightfieldPrimitive.Proxy->HeightfieldHasPendingStreaming())
            {
                PendingStreamingHeightfieldBoxes.Add(HeightfieldPrimitive->Proxy->GetBounds().GetBox());
            }
        }*/

        if (View.ViewState != null)
        {
            View.ViewState.GlobalDistanceFieldUpdateIndex++;

            if (View.ViewState.GlobalDistanceFieldUpdateIndex > 4)
            {
                View.ViewState.GlobalDistanceFieldUpdateIndex = 0;
            }

            for (int ClipmapIndex = 0; ClipmapIndex < NumClipmaps; ClipmapIndex++)
            {
                FGlobalDistanceFieldClipmapState ClipmapViewState = View.ViewState.GlobalDistanceFieldClipmapState[ClipmapIndex];

			    final float Extent = ComputeClipmapExtent(ClipmapIndex, Scene);
                final float CellSize = (Extent * 2) / mParams.GAOGlobalDFResolution;

                boolean bReallocated = false;

                // Accumulate primitive modifications in the viewstate in case we don't update the clipmap this frame
                for (int CacheType = 0; CacheType < GDF_Num; CacheType++)
                {
				    final int SourceCacheType = mParams.GAOGlobalDistanceFieldCacheMostlyStaticSeparately ? CacheType : GDF_Full;
                    ClipmapViewState.Cache[CacheType].PrimitiveModifiedBounds.addAll(Scene.DistanceFieldSceneData.PrimitiveModifiedBounds[SourceCacheType]);

                    if (CacheType == GDF_Full || mParams.GAOGlobalDistanceFieldCacheMostlyStaticSeparately)
                    {
                        TextureGL RenderTarget = ClipmapViewState.Cache[CacheType].VolumeTexture;

                        if (RenderTarget == null || RenderTarget.getWidth()/* ->GetDesc().Extent.X*/ != mParams.GAOGlobalDFResolution)
                        {
//                            AllocateClipmapTexture(RHICmdList, ClipmapIndex, (FGlobalDFCacheType)CacheType, RenderTarget);
                            RenderTarget = AllocateClipmapTexture(ClipmapIndex, CacheType);
                            ClipmapViewState.Cache[CacheType].VolumeTexture = RenderTarget;
                            bReallocated = true;
                        }
                    }
                }

			    final boolean bForceFullUpdate = bReallocated
                    || !View.ViewState.bInitializedGlobalDistanceFieldOrigins
                    // Detect when max occlusion distance has changed
                    || ClipmapViewState.CachedMaxOcclusionDistance != MaxOcclusionDistance
                    || ClipmapViewState.CachedGlobalDistanceFieldViewDistance != Scene.GlobalDistanceFieldViewDistance
                    || ClipmapViewState.CacheMostlyStaticSeparately != mParams.GAOGlobalDistanceFieldCacheMostlyStaticSeparately
//                    || ClipmapViewState.LastUsedSceneDataForFullUpdate != &Scene->DistanceFieldSceneData  TODO
                    || mParams.GAOGlobalDistanceFieldForceFullUpdate;

                if (ShouldUpdateClipmapThisFrame(ClipmapIndex, View.ViewState.GlobalDistanceFieldUpdateIndex) || bForceFullUpdate)
                {
				    ReadableVector3f NewCenter = View.ViewOrigin; //  ViewMatrices.GetViewOrigin();

                    Vector3i GridCenter = new Vector3i();
                    GridCenter.x = (int)(NewCenter.getX() / CellSize);
                    GridCenter.y = (int)(NewCenter.getY() / CellSize);
                    GridCenter.z = (int)(NewCenter.getZ() / CellSize);

//				    const FVector SnappedCenter = FVector(GridCenter) * CellSize;
//				    const FBox ClipmapBounds(SnappedCenter - Extent, SnappedCenter + Extent);

				    final float SnappedCenterX = GridCenter.x * CellSize;
				    final float SnappedCenterY = GridCenter.y * CellSize;
				    final float SnappedCenterZ = GridCenter.z * CellSize;
				    final BoundingBox ClipmapBounds = new BoundingBox();
                    ClipmapBounds.set(SnappedCenterX - Extent, SnappedCenterY - Extent, SnappedCenterZ - Extent,
                            SnappedCenterX + Extent, SnappedCenterY + Extent, SnappedCenterZ + Extent);

				    final boolean bUsePartialUpdates = mParams.GAOGlobalDistanceFieldPartialUpdates && !bForceFullUpdate;

                    if (!bUsePartialUpdates)
                    {
                        // Store the location of the full update
                        ClipmapViewState.FullUpdateOrigin.set(GridCenter);
                        View.ViewState.bInitializedGlobalDistanceFieldOrigins = true;
//                        ClipmapViewState.LastUsedSceneDataForFullUpdate = &Scene->DistanceFieldSceneData;  TODO
                    }

				    final int StartCacheType = mParams.GAOGlobalDistanceFieldCacheMostlyStaticSeparately ? GDF_MostlyStatic : GDF_Full;

                    for (int CacheType = StartCacheType; CacheType < GDF_Num; CacheType++)
                    {
                        FGlobalDistanceFieldClipmap Clipmap = (CacheType == GDF_MostlyStatic
                            ? GlobalDistanceFieldInfo.MostlyStaticClipmaps.get(ClipmapIndex)
						: GlobalDistanceFieldInfo.Clipmaps.get(ClipmapIndex));

                        boolean bLocalUsePartialUpdates = bUsePartialUpdates
                                // Only use partial updates with small numbers of primitive modifications
                                && ClipmapViewState.Cache[CacheType].PrimitiveModifiedBounds.size() < 100;

                        if (bLocalUsePartialUpdates)
                        {
                            Vector3i Movement = Vector3i.sub(GridCenter, ClipmapViewState.LastPartialUpdateOrigin, null);

                            if (CacheType == GDF_MostlyStatic || !mParams.GAOGlobalDistanceFieldCacheMostlyStaticSeparately)
                            {
                                // Add an update region for each potential axis of camera movement
                                AddUpdateRegionForAxis(Movement, ClipmapBounds, CellSize, 0, Clipmap.UpdateRegions);
                                AddUpdateRegionForAxis(Movement, ClipmapBounds, CellSize, 1, Clipmap.UpdateRegions);
                                AddUpdateRegionForAxis(Movement, ClipmapBounds, CellSize, 2, Clipmap.UpdateRegions);
                            }
                            else
                            {
                                // Inherit from parent   TODO reference copy
                                Clipmap.UpdateRegions.addAll(GlobalDistanceFieldInfo.MostlyStaticClipmaps.get(ClipmapIndex).UpdateRegions);
                            }

						    final float GlobalMaxSphereQueryRadius = (float) (MaxOcclusionDistance / (1 + Math.tan(GAOConeHalfAngle)));

                            // Add an update region for each primitive that has been modified
                            for (int BoundsIndex = 0; BoundsIndex < ClipmapViewState.Cache[CacheType].PrimitiveModifiedBounds.size(); BoundsIndex++)
                            {
                                AddUpdateRegionForPrimitive(ClipmapViewState.Cache[CacheType].PrimitiveModifiedBounds.get(BoundsIndex), GlobalMaxSphereQueryRadius, ClipmapBounds, CellSize, Clipmap.UpdateRegions);
                            }

                            int TotalTexelsBeingUpdated = 0;

                            // Trim fully contained update regions
                            for (int UpdateRegionIndex = 0; UpdateRegionIndex < Clipmap.UpdateRegions.size(); UpdateRegionIndex++)
                            {
                                FVolumeUpdateRegion UpdateRegion = Clipmap.UpdateRegions.get(UpdateRegionIndex);
                                boolean bCompletelyContained = false;

                                for (int OtherUpdateRegionIndex = 0; OtherUpdateRegionIndex < Clipmap.UpdateRegions.size(); OtherUpdateRegionIndex++)
                                {
                                    if (UpdateRegionIndex != OtherUpdateRegionIndex)
                                    {
									    FVolumeUpdateRegion OtherUpdateRegion = Clipmap.UpdateRegions.get(OtherUpdateRegionIndex);

                                        if (OtherUpdateRegion.Bounds.contains(UpdateRegion.Bounds._min)
                                                && OtherUpdateRegion.Bounds.contains(UpdateRegion.Bounds._max))
                                        {
                                            bCompletelyContained = true;
                                            break;
                                        }
                                    }
                                }

                                if (bCompletelyContained)
                                {
                                    Clipmap.UpdateRegions.remove(UpdateRegionIndex);
                                    UpdateRegionIndex--;
                                }
                            }

                            // Trim overlapping regions
                            for (int UpdateRegionIndex = 0; UpdateRegionIndex < Clipmap.UpdateRegions.size(); UpdateRegionIndex++)
                            {
                                FVolumeUpdateRegion UpdateRegion = Clipmap.UpdateRegions.get(UpdateRegionIndex);
                                boolean bEmptyRegion = false;

                                for (int OtherUpdateRegionIndex = 0; OtherUpdateRegionIndex < Clipmap.UpdateRegions.size(); OtherUpdateRegionIndex++)
                                {
                                    if (UpdateRegionIndex != OtherUpdateRegionIndex)
                                    {
                                        FVolumeUpdateRegion OtherUpdateRegion = Clipmap.UpdateRegions.get(OtherUpdateRegionIndex);

                                        if (OtherUpdateRegion.Bounds.intersects(UpdateRegion.Bounds))
                                        {
                                            TrimOverlappingAxis(0, CellSize, OtherUpdateRegion, UpdateRegion);
                                            TrimOverlappingAxis(1, CellSize, OtherUpdateRegion, UpdateRegion);
                                            TrimOverlappingAxis(2, CellSize, OtherUpdateRegion, UpdateRegion);

                                            if (UpdateRegion.CellsSize.x == 0 || UpdateRegion.CellsSize.y == 0 || UpdateRegion.CellsSize.z == 0)
                                            {
                                                bEmptyRegion = true;
                                                break;
                                            }
                                        }
                                    }
                                }

                                if (bEmptyRegion)
                                {
                                    Clipmap.UpdateRegions.remove(UpdateRegionIndex);
                                    UpdateRegionIndex--;
                                }
                            }

                            // Count how many texels are being updated
                            for (int UpdateRegionIndex = 0; UpdateRegionIndex < Clipmap.UpdateRegions.size(); UpdateRegionIndex++)
                            {
                                FVolumeUpdateRegion UpdateRegion = Clipmap.UpdateRegions.get(UpdateRegionIndex);
                                TotalTexelsBeingUpdated += UpdateRegion.CellsSize.x * UpdateRegion.CellsSize.y * UpdateRegion.CellsSize.z;
                            }

                            // Fall back to a full update if the partial updates were going to do more work
                            if (TotalTexelsBeingUpdated >= mParams.GAOGlobalDFResolution * mParams.GAOGlobalDFResolution * mParams.GAOGlobalDFResolution)
                            {
                                Clipmap.UpdateRegions.clear();
                                bLocalUsePartialUpdates = false;
                            }
                        }

                        if (!bLocalUsePartialUpdates)
                        {
                            FVolumeUpdateRegion UpdateRegion = new FVolumeUpdateRegion();
                            UpdateRegion.Bounds.set(ClipmapBounds);
                            UpdateRegion.CellsSize.set(mParams.GAOGlobalDFResolution, mParams.GAOGlobalDFResolution, mParams.GAOGlobalDFResolution);
                            Clipmap.UpdateRegions.add(UpdateRegion);
                        }

                        // Check if the clipmap intersects with a pending update region
                        boolean bHasPendingStreaming = false;
                        /*for (const FBox& HeightfieldBox : PendingStreamingHeightfieldBoxes)  todo
                        {
                            if (ClipmapBounds.Intersect(HeightfieldBox))
                            {
                                bHasPendingStreaming = true;
                                break;
                            }
                        }*/

                        // If some of the height fields has pending streaming regions, postpone a full update.
                        if (bHasPendingStreaming)
                        {
                            // Mark a pending update for this height field. It will get processed when all pending texture streaming affecting it will be completed.
//                            View.ViewState.DeferredGlobalDistanceFieldUpdates[CacheType].AddUnique(ClipmapIndex);  TODO
                            // Remove the height fields from the update.
                            for (FVolumeUpdateRegion UpdateRegion : Clipmap.UpdateRegions)
                            {
                                UpdateRegion.UpdateType = (UpdateRegion.UpdateType & ~EVolumeUpdateType.VUT_Heightfields);
                            }
                        }
                        else //if (View.ViewState.DeferredGlobalDistanceFieldUpdates[CacheType].Remove(ClipmapIndex) > 0)
                        {
                            // Remove the height fields from the current update as we are pushing a new full update.
                            for (FVolumeUpdateRegion UpdateRegion : Clipmap.UpdateRegions)
                            {
                                UpdateRegion.UpdateType = (UpdateRegion.UpdateType & ~EVolumeUpdateType.VUT_Heightfields);
                            }

                            FVolumeUpdateRegion UpdateRegion = new FVolumeUpdateRegion();
                            UpdateRegion.Bounds.set(ClipmapBounds);
                            UpdateRegion.CellsSize.set(mParams.GAOGlobalDFResolution, mParams.GAOGlobalDFResolution, mParams.GAOGlobalDFResolution);
                            UpdateRegion.UpdateType = EVolumeUpdateType.VUT_Heightfields;
                            Clipmap.UpdateRegions.add(UpdateRegion);
                        }

                        ClipmapViewState.Cache[CacheType].PrimitiveModifiedBounds.clear();
                    }

                    ClipmapViewState.LastPartialUpdateOrigin.set(GridCenter);
                }

//			    const FVector Center = FVector(ClipmapViewState.LastPartialUpdateOrigin) * CellSize;
                final float CenterX = ClipmapViewState.LastPartialUpdateOrigin.x * CellSize;
                final float CenterY = ClipmapViewState.LastPartialUpdateOrigin.y * CellSize;
                final float CenterZ = ClipmapViewState.LastPartialUpdateOrigin.z * CellSize;
			    final int StartCacheType = mParams.GAOGlobalDistanceFieldCacheMostlyStaticSeparately ? GDF_MostlyStatic : GDF_Full;

                for (int CacheType = StartCacheType; CacheType < GDF_Num; CacheType++)
                {
                    FGlobalDistanceFieldClipmap Clipmap = (CacheType == GDF_MostlyStatic
                        ? GlobalDistanceFieldInfo.MostlyStaticClipmaps.get(ClipmapIndex)
					: GlobalDistanceFieldInfo.Clipmaps.get(ClipmapIndex));

                    // Setup clipmap properties from view state exclusively, so we can skip updating on some frames
                    Clipmap.RenderTarget = ClipmapViewState.Cache[CacheType].VolumeTexture;
                    Clipmap.Bounds.set(CenterX - Extent, CenterY - Extent,CenterZ - Extent,
                                        CenterX + Extent, CenterY + Extent, CenterZ + Extent);
                    // Scroll offset so the contents of the global distance field don't have to be moved as the camera moves around, only updated in slabs
                    Clipmap.ScrollOffset.x = (ClipmapViewState.LastPartialUpdateOrigin.x - ClipmapViewState.FullUpdateOrigin.x) * CellSize;
                    Clipmap.ScrollOffset.y = (ClipmapViewState.LastPartialUpdateOrigin.y - ClipmapViewState.FullUpdateOrigin.y) * CellSize;
                    Clipmap.ScrollOffset.z = (ClipmapViewState.LastPartialUpdateOrigin.z - ClipmapViewState.FullUpdateOrigin.z) * CellSize;
                }

                ClipmapViewState.CachedMaxOcclusionDistance = MaxOcclusionDistance;
                ClipmapViewState.CachedGlobalDistanceFieldViewDistance = Scene.GlobalDistanceFieldViewDistance;
                ClipmapViewState.CacheMostlyStaticSeparately = mParams.GAOGlobalDistanceFieldCacheMostlyStaticSeparately;
            }
        }
        else
        {
            for (int ClipmapIndex = 0; ClipmapIndex < NumClipmaps; ClipmapIndex++)
            {
			    final int StartCacheType = mParams.GAOGlobalDistanceFieldCacheMostlyStaticSeparately ? GDF_MostlyStatic : GDF_Full;

                for (int CacheType = StartCacheType; CacheType < GDF_Num; CacheType++)
                {
                    FGlobalDistanceFieldClipmap Clipmap = (CacheType == GDF_MostlyStatic
                        ? GlobalDistanceFieldInfo.MostlyStaticClipmaps.get(ClipmapIndex)
					: GlobalDistanceFieldInfo.Clipmaps.get(ClipmapIndex));

                    Clipmap.RenderTarget = AllocateClipmapTexture(/*RHICmdList,*/ ClipmapIndex, CacheType);
                    Clipmap.ScrollOffset.set(0,0,0);

				    final float Extent = ComputeClipmapExtent(ClipmapIndex, Scene);
//                    FVector Center = View.ViewMatrices.GetViewOrigin();
                    float CenterX = View.ViewOrigin.x;
                    float CenterY = View.ViewOrigin.y;
                    float CenterZ = View.ViewOrigin.z;

				    final float CellSize = (Extent * 2) / mParams.GAOGlobalDFResolution;

//                    FIntVector GridCenter;
                    int GridCenterX = (int)(CenterX / CellSize);
                    int GridCenterY = (int)(CenterY / CellSize);
                    int GridCenterZ = (int)(CenterZ / CellSize);

//                    Center = FVector(GridCenter) * CellSize;
                    CenterX = GridCenterX * CellSize;
                    CenterY = GridCenterY * CellSize;
                    CenterZ = GridCenterZ * CellSize;

//                    FBox ClipmapBounds(Center - Extent, Center + Extent);
//                    Clipmap.Bounds = ClipmapBounds;
                    Clipmap.Bounds.set(CenterX - Extent, CenterY - Extent,CenterZ - Extent,
                            CenterX + Extent, CenterY + Extent, CenterZ + Extent);

                    FVolumeUpdateRegion UpdateRegion = new FVolumeUpdateRegion();
//                    UpdateRegion.Bounds = ClipmapBounds;
                    UpdateRegion.Bounds.set(Clipmap.Bounds);
                    UpdateRegion.CellsSize.set(mParams.GAOGlobalDFResolution,mParams.GAOGlobalDFResolution,mParams.GAOGlobalDFResolution);
                    Clipmap.UpdateRegions.add(UpdateRegion);
                }
            }
        }

        GlobalDistanceFieldInfo.UpdateParameterData(MaxOcclusionDistance, mParams.GAOGlobalDFResolution, GAOConeHalfAngle);
    }

    private static final FDistanceFieldObjectBufferResource GGlobalDistanceFieldCulledObjectBuffers = new FDistanceFieldObjectBufferResource();
    private static final FObjectGridBuffers GObjectGridBuffers = new FObjectGridBuffers();
    private static int CullObjectsGroupSize = 64;

    private GLSLProgram mFCullObjectsForVolumeCS;
    private GLSLProgram mFCullObjectsToGridCS;
    private final GLSLProgram[] mTCompositeObjectDistanceFieldsCS = new GLSLProgram[8];

    static final String SHADER_PATH = UE4Engine.SHADER_PATH + "DistanceField/";
    private static final String FCullObjectsForVolumeCS = SHADER_PATH + "CullObjectsForVolumeCS.comp";
    private static final String FCullObjectsToGridCS    = SHADER_PATH + "CullObjectsToGridCS.comp";
    private static final String TCompositeObjectDistanceFieldsCS    = SHADER_PATH + "CompositeObjectDistanceFieldsCS.comp";

    /**
     * Updates the global distance field for a view.
     * Typically issues updates for just the newly exposed regions of the volume due to camera movement.
     * In the worst case of a camera cut or large distance field scene changes, a full update of the global distance field will be done.
     **/
    public void UpdateGlobalDistanceFieldVolume(FViewInfo View, FScene Scene,
                                                float MaxOcclusionDistance,
                                                FGlobalDistanceFieldInfo GlobalDistanceFieldInfo,
                                                float GAOConeHalfAngle){
//        SCOPED_GPU_STAT(RHICmdList, GlobalDistanceFieldUpdate);

        final GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
//        extern float GAOConeHalfAngle;
	    final float GlobalMaxSphereQueryRadius = (float) (MaxOcclusionDistance / (1 + Math.tan(GAOConeHalfAngle)));

        if (Scene.DistanceFieldSceneData.NumObjectsInBuffer > 0)
        {
            ComputeUpdateRegionsAndUpdateViewState(/*RHICmdList,*/ View, Scene, GlobalDistanceFieldInfo, UE4Engine.GMaxGlobalDistanceFieldClipmaps, MaxOcclusionDistance, GAOConeHalfAngle);

            // Recreate the view uniform buffer now that we have updated GlobalDistanceFieldInfo
//            View.SetupGlobalDistanceFieldUniformBufferParameters(*View.CachedViewUniformShaderParameters);  TODO
//            View.ViewUniformBuffer = TUniformBufferRef<FViewUniformShaderParameters>::CreateUniformBufferImmediate(*View.CachedViewUniformShaderParameters, UniformBuffer_SingleFrame);

            boolean bHasUpdateRegions = false;

            for (int ClipmapIndex = 0; ClipmapIndex < GlobalDistanceFieldInfo.Clipmaps.size(); ClipmapIndex++)
            {
                bHasUpdateRegions = bHasUpdateRegions || GlobalDistanceFieldInfo.Clipmaps.get(ClipmapIndex).UpdateRegions.size() > 0;
            }

            for (int ClipmapIndex = 0; ClipmapIndex < GlobalDistanceFieldInfo.MostlyStaticClipmaps.size(); ClipmapIndex++)
            {
                bHasUpdateRegions = bHasUpdateRegions || GlobalDistanceFieldInfo.MostlyStaticClipmaps.get(ClipmapIndex).UpdateRegions.size() > 0;
            }

            if (bHasUpdateRegions && mParams.GAOUpdateGlobalDistanceField)
            {
//                SCOPED_DRAW_EVENT(RHICmdList, UpdateGlobalDistanceFieldVolume);

                if (!GGlobalDistanceFieldCulledObjectBuffers.IsInitialized()
                        || GGlobalDistanceFieldCulledObjectBuffers.Buffers.MaxObjects < Scene.DistanceFieldSceneData.NumObjectsInBuffer
                        || GGlobalDistanceFieldCulledObjectBuffers.Buffers.MaxObjects > 3 * Scene.DistanceFieldSceneData.NumObjectsInBuffer)
                {
                    GGlobalDistanceFieldCulledObjectBuffers.Buffers.MaxObjects = Scene.DistanceFieldSceneData.NumObjectsInBuffer * 5 / 4;
                    GGlobalDistanceFieldCulledObjectBuffers.ReleaseDynamicRHI();
                    GGlobalDistanceFieldCulledObjectBuffers.InitDynamicRHI();
                }
//                GGlobalDistanceFieldCulledObjectBuffers.Buffers.AcquireTransientResource();

			    final int MaxCullGridDimension = mParams.GAOGlobalDFResolution / GCullGridTileSize;

			    final boolean b16BitObjectIndices = Scene.DistanceFieldSceneData.CanUse16BitObjectIndices();

                if (GObjectGridBuffers.GridDimension != MaxCullGridDimension || GObjectGridBuffers.b16BitIndices != b16BitObjectIndices)
                {
                    GObjectGridBuffers.b16BitIndices = b16BitObjectIndices;
                    GObjectGridBuffers.GridDimension = MaxCullGridDimension;
//                    GObjectGridBuffers.UpdateRHI();  todo
                    GObjectGridBuffers.InitDynamicRHI();
                }
                GObjectGridBuffers.AcquireTransientResource();

                final int StartCacheType = mParams.GAOGlobalDistanceFieldCacheMostlyStaticSeparately ? GDF_MostlyStatic : GDF_Full;

                for (int CacheType = StartCacheType; CacheType < GDF_Num; CacheType++)
                {
                    List<FGlobalDistanceFieldClipmap> Clipmaps = CacheType == GDF_MostlyStatic
                        ? GlobalDistanceFieldInfo.MostlyStaticClipmaps
                        : GlobalDistanceFieldInfo.Clipmaps;

                    for (int ClipmapIndex = 0; ClipmapIndex < Clipmaps.size(); ClipmapIndex++)
                    {
//                        SCOPED_DRAW_EVENTF(RHICmdList, Clipmap, TEXT("CacheType %s Clipmap %u"), CacheType == GDF_MostlyStatic ? TEXT("MostlyStatic") : TEXT("Movable"), ClipmapIndex);

                        FGlobalDistanceFieldClipmap Clipmap = Clipmaps.get(ClipmapIndex);

                        for (int UpdateRegionIndex = 0; UpdateRegionIndex < Clipmap.UpdateRegions.size(); UpdateRegionIndex++)
                        {
                            FVolumeUpdateRegion UpdateRegion = Clipmap.UpdateRegions.get(UpdateRegionIndex);

                            if ((UpdateRegion.UpdateType & EVolumeUpdateType.VUT_MeshDistanceFields)!= 0)
                            {
                                {
//                                    SCOPED_DRAW_EVENT(RHICmdList, GridCull);

                                    // Cull the global objects to the volume being updated
                                    {
//                                        ClearUAV(RHICmdList, GGlobalDistanceFieldCulledObjectBuffers.Buffers.ObjectIndirectArguments, 0);
                                        gl.glClearNamedBufferData(GGlobalDistanceFieldCulledObjectBuffers.Buffers.ObjectIndirectArguments.getBuffer(),
                                                GGlobalDistanceFieldCulledObjectBuffers.Buffers.ObjectIndirectArguments.InternalFormat, GLenum.GL_RGBA, GLenum.GL_FLOAT, null);

                                        /*TShaderMapRef<FCullObjectsForVolumeCS> ComputeShader(View.ShaderMap);
                                        RHICmdList.SetComputeShader(ComputeShader->GetComputeShader());
									    const FVector4 VolumeBounds(UpdateRegion.Bounds.GetCenter(), UpdateRegion.Bounds.GetExtent().Size());
                                        ComputeShader->SetParameters(RHICmdList, Scene, View, MaxOcclusionDistance, VolumeBounds, (FGlobalDFCacheType)CacheType);

                                        DispatchComputeShader(RHICmdList, *ComputeShader, FMath::DivideAndRoundUp<uint32>(Scene->DistanceFieldSceneData.NumObjectsInBuffer, CullObjectsGroupSize), 1, 1);
                                        ComputeShader->UnsetParameters(RHICmdList, Scene);*/

                                        if(mFCullObjectsForVolumeCS == null){
                                            Macro[] macros = {
                                              new Macro("CULLOBJECTS_THREADGROUP_SIZE", CullObjectsGroupSize)
                                            };
                                            mFCullObjectsForVolumeCS = GLSLProgram.createProgram(FCullObjectsForVolumeCS,macros);
                                            mFCullObjectsForVolumeCS.setName("FCullObjectsForVolumeCS");
                                        }

                                        // TODO binding shader resources.
                                        mFCullObjectsForVolumeCS.enable();
                                        gl.glDispatchCompute(Numeric.divideAndRoundUp(Scene.DistanceFieldSceneData.NumObjectsInBuffer, CullObjectsGroupSize),1,1);

                                        if(!m_printOnce)
                                            mFCullObjectsForVolumeCS.printPrograminfo();
                                    }

                                    // Further cull the objects into a low resolution grid
                                    {
                                        /*TShaderMapRef<FCullObjectsToGridCS> ComputeShader(View.ShaderMap);
                                        RHICmdList.SetComputeShader(ComputeShader->GetComputeShader());
                                        ComputeShader->SetParameters(RHICmdList, Scene, View, MaxOcclusionDistance, GlobalDistanceFieldInfo, ClipmapIndex, UpdateRegion);*/
                                        if(mFCullObjectsToGridCS == null){
                                            Macro[] macros = {
                                                    new Macro("CULL_GRID_TILE_SIZE", GCullGridTileSize),
                                                    new Macro("MAX_GRID_CULLED_DF_OBJECTS", FObjectGridBuffers.GMaxGridCulledObjects),
                                            };
                                            mFCullObjectsToGridCS = GLSLProgram.createProgram(FCullObjectsToGridCS,macros);
                                            mFCullObjectsToGridCS.setName("FCullObjectsToGridCS");
                                        }

                                        final int NumGroupsX = Numeric.divideAndRoundUp(UpdateRegion.CellsSize.x, GCullGridTileSize);
                                        final int NumGroupsY = Numeric.divideAndRoundUp(UpdateRegion.CellsSize.y, GCullGridTileSize);
                                        final int NumGroupsZ = Numeric.divideAndRoundUp(UpdateRegion.CellsSize.z, GCullGridTileSize);

                                        // TODO binding shader resources.
                                        mFCullObjectsToGridCS.enable();
                                        gl.glDispatchCompute(NumGroupsX, NumGroupsY, NumGroupsZ);
//                                        ComputeShader->UnsetParameters(RHICmdList);
                                    }
                                }

                                // Further cull the objects to the dispatch tile and composite the global distance field by computing the min distance from intersecting per-object distance fields
                                {
//                                    SCOPED_DRAW_EVENTF(RHICmdList, TileCullAndComposite, TEXT("TileCullAndComposite %ux%ux%u"), UpdateRegion.CellsSize.X, UpdateRegion.CellsSize.Y, UpdateRegion.CellsSize.Z);

                                    int MinDimension = 2;

                                    if (UpdateRegion.CellsSize.x < UpdateRegion.CellsSize.y && UpdateRegion.CellsSize.x < UpdateRegion.CellsSize.z)
                                    {
                                        MinDimension = 0;
                                    }
                                    else if (UpdateRegion.CellsSize.y < UpdateRegion.CellsSize.x && UpdateRegion.CellsSize.y < UpdateRegion.CellsSize.z)
                                    {
                                        MinDimension = 1;
                                    }

                                    int MinSize = UpdateRegion.CellsSize.get(MinDimension);
                                    int MaxSize = Math.max(UpdateRegion.CellsSize.x, Math.max(UpdateRegion.CellsSize.y, UpdateRegion.CellsSize.z));
								    final int FlattenedDimension = MaxSize >= MinSize * 8 ? MinDimension : Flatten_None;

								    final int NumGroupsX = Numeric.divideAndRoundUp(UpdateRegion.CellsSize.x, GetCompositeTileSize(0, FlattenedDimension));
                                    final int NumGroupsY = Numeric.divideAndRoundUp(UpdateRegion.CellsSize.y, GetCompositeTileSize(1, FlattenedDimension));
                                    final int NumGroupsZ = Numeric.divideAndRoundUp(UpdateRegion.CellsSize.z, GetCompositeTileSize(2, FlattenedDimension));

//                                    IPooledRenderTarget* ParentDistanceField = GlobalDistanceFieldInfo.MostlyStaticClipmaps[ClipmapIndex].RenderTarget;
                                    TextureGL ParentDistanceField = GlobalDistanceFieldInfo.MostlyStaticClipmaps.get(ClipmapIndex).RenderTarget;

                                    boolean bUseParentDistanceField = (CacheType == GDF_Full && mParams.GAOGlobalDistanceFieldCacheMostlyStaticSeparately && ParentDistanceField != null);
                                    final int ShaderIndex = GetCompositeIndex(bUseParentDistanceField, FlattenedDimension);
                                    if(mTCompositeObjectDistanceFieldsCS[ShaderIndex] == null){
                                        Macro[] macros = {
                                                new Macro("COMPOSITE_THREADGROUP_SIZEX", GetCompositeTileSize(0, FlattenedDimension)),
                                                new Macro("COMPOSITE_THREADGROUP_SIZEY", GetCompositeTileSize(1, FlattenedDimension)),
                                                new Macro("COMPOSITE_THREADGROUP_SIZEZ", GetCompositeTileSize(2, FlattenedDimension)),
                                                new Macro("CULL_GRID_TILE_SIZE", GCullGridTileSize),
                                                new Macro("MAX_GRID_CULLED_DF_OBJECTS", FObjectGridBuffers.GMaxGridCulledObjects),
                                                new Macro("USE_PARENT_DISTANCE_FIELD", bUseParentDistanceField ? 1 : 0),
                                        };
                                        mTCompositeObjectDistanceFieldsCS[ShaderIndex] = GLSLProgram.createProgram(FCullObjectsToGridCS,macros);
                                        mTCompositeObjectDistanceFieldsCS[ShaderIndex].setName("TCompositeObjectDistanceFieldsCS[" + ShaderIndex + "]");
                                    }

                                    // TODO binding shader resources.
                                    mTCompositeObjectDistanceFieldsCS[ShaderIndex].enable();
                                    gl.glDispatchCompute(NumGroupsX, NumGroupsY, NumGroupsZ);

                                    /*if (CacheType == GDF_Full && mParams.GAOGlobalDistanceFieldCacheMostlyStaticSeparately && ParentDistanceField != null)
                                    {
                                        if (FlattenedDimension == Flatten_None)
                                        {
                                            TShaderMapRef<TCompositeObjectDistanceFieldsCS<true, Flatten_None>> ComputeShader(View.ShaderMap);
                                            RHICmdList.SetComputeShader(ComputeShader->GetComputeShader());
                                            ComputeShader->SetParameters(RHICmdList, Scene, View, MaxOcclusionDistance, GlobalDistanceFieldInfo.ParameterData, Clipmap, ParentDistanceField, ClipmapIndex, UpdateRegion);
                                            DispatchComputeShader(RHICmdList, *ComputeShader, NumGroupsX, NumGroupsY, NumGroupsZ);
                                            ComputeShader->UnsetParameters(RHICmdList, Clipmap);
                                        }
                                        else if (FlattenedDimension == Flatten_XAxis)
                                        {
                                            TShaderMapRef<TCompositeObjectDistanceFieldsCS<true, Flatten_XAxis>> ComputeShader(View.ShaderMap);
                                            RHICmdList.SetComputeShader(ComputeShader->GetComputeShader());
                                            ComputeShader->SetParameters(RHICmdList, Scene, View, MaxOcclusionDistance, GlobalDistanceFieldInfo.ParameterData, Clipmap, ParentDistanceField, ClipmapIndex, UpdateRegion);
                                            DispatchComputeShader(RHICmdList, *ComputeShader, NumGroupsX, NumGroupsY, NumGroupsZ);
                                            ComputeShader->UnsetParameters(RHICmdList, Clipmap);
                                        }
                                        else if (FlattenedDimension == Flatten_YAxis)
                                        {
                                            TShaderMapRef<TCompositeObjectDistanceFieldsCS<true, Flatten_YAxis>> ComputeShader(View.ShaderMap);
                                            RHICmdList.SetComputeShader(ComputeShader->GetComputeShader());
                                            ComputeShader->SetParameters(RHICmdList, Scene, View, MaxOcclusionDistance, GlobalDistanceFieldInfo.ParameterData, Clipmap, ParentDistanceField, ClipmapIndex, UpdateRegion);
                                            DispatchComputeShader(RHICmdList, *ComputeShader, NumGroupsX, NumGroupsY, NumGroupsZ);
                                            ComputeShader->UnsetParameters(RHICmdList, Clipmap);
                                        }
                                        else
                                        {
                                            check(FlattenedDimension == Flatten_ZAxis);
                                            TShaderMapRef<TCompositeObjectDistanceFieldsCS<true, Flatten_ZAxis>> ComputeShader(View.ShaderMap);
                                            RHICmdList.SetComputeShader(ComputeShader->GetComputeShader());
                                            ComputeShader->SetParameters(RHICmdList, Scene, View, MaxOcclusionDistance, GlobalDistanceFieldInfo.ParameterData, Clipmap, ParentDistanceField, ClipmapIndex, UpdateRegion);
                                            DispatchComputeShader(RHICmdList, *ComputeShader, NumGroupsX, NumGroupsY, NumGroupsZ);
                                            ComputeShader->UnsetParameters(RHICmdList, Clipmap);
                                        }
                                    }
                                    else
                                    {
                                        if (FlattenedDimension == Flatten_None)
                                        {
                                            TShaderMapRef<TCompositeObjectDistanceFieldsCS<false, Flatten_None>> ComputeShader(View.ShaderMap);
                                            RHICmdList.SetComputeShader(ComputeShader->GetComputeShader());
                                            ComputeShader->SetParameters(RHICmdList, Scene, View, MaxOcclusionDistance, GlobalDistanceFieldInfo.ParameterData, Clipmap, NULL, ClipmapIndex, UpdateRegion);
                                            DispatchComputeShader(RHICmdList, *ComputeShader, NumGroupsX, NumGroupsY, NumGroupsZ);
                                            ComputeShader->UnsetParameters(RHICmdList, Clipmap);
                                        }
                                        else if (FlattenedDimension == Flatten_XAxis)
                                        {
                                            TShaderMapRef<TCompositeObjectDistanceFieldsCS<false, Flatten_XAxis>> ComputeShader(View.ShaderMap);
                                            RHICmdList.SetComputeShader(ComputeShader->GetComputeShader());
                                            ComputeShader->SetParameters(RHICmdList, Scene, View, MaxOcclusionDistance, GlobalDistanceFieldInfo.ParameterData, Clipmap, NULL, ClipmapIndex, UpdateRegion);
                                            DispatchComputeShader(RHICmdList, *ComputeShader, NumGroupsX, NumGroupsY, NumGroupsZ);
                                            ComputeShader->UnsetParameters(RHICmdList, Clipmap);
                                        }
                                        else if (FlattenedDimension == Flatten_YAxis)
                                        {
                                            TShaderMapRef<TCompositeObjectDistanceFieldsCS<false, Flatten_YAxis>> ComputeShader(View.ShaderMap);
                                            RHICmdList.SetComputeShader(ComputeShader->GetComputeShader());
                                            ComputeShader->SetParameters(RHICmdList, Scene, View, MaxOcclusionDistance, GlobalDistanceFieldInfo.ParameterData, Clipmap, NULL, ClipmapIndex, UpdateRegion);
                                            DispatchComputeShader(RHICmdList, *ComputeShader, NumGroupsX, NumGroupsY, NumGroupsZ);
                                            ComputeShader->UnsetParameters(RHICmdList, Clipmap);
                                        }
                                        else
                                        {
                                            check(FlattenedDimension == Flatten_ZAxis);
                                            TShaderMapRef<TCompositeObjectDistanceFieldsCS<false, Flatten_ZAxis>> ComputeShader(View.ShaderMap);
                                            RHICmdList.SetComputeShader(ComputeShader->GetComputeShader());
                                            ComputeShader->SetParameters(RHICmdList, Scene, View, MaxOcclusionDistance, GlobalDistanceFieldInfo.ParameterData, Clipmap, NULL, ClipmapIndex, UpdateRegion);
                                            DispatchComputeShader(RHICmdList, *ComputeShader, NumGroupsX, NumGroupsY, NumGroupsZ);
                                            ComputeShader->UnsetParameters(RHICmdList, Clipmap);
                                        }
                                    }*/
                                }
                            }
                        }
                    }


                    // Make sure we finish all writing into clipmaps and they are ready to be read.
                    for (int ClipmapIndex = 0; ClipmapIndex < Clipmaps.size(); ClipmapIndex++)
                    {
                        FGlobalDistanceFieldClipmap Clipmap = Clipmaps.get(ClipmapIndex);
//					    const FSceneRenderTargetItem& ClipMapRTI = Clipmap.RenderTarget->GetRenderTargetItem();
                        TextureGL ClipMapRTI = Clipmap.RenderTarget;

//                        RHICmdList.TransitionResource(EResourceTransitionAccess::ERWBarrier, EResourceTransitionPipeline::EComputeToCompute, ClipMapRTI.UAV);
                    }

                    // Composite heighfields.
                    if (CacheType == GDF_MostlyStatic || !mParams.GAOGlobalDistanceFieldCacheMostlyStaticSeparately)
                    {
//                        SCOPED_DRAW_EVENT(RHICmdList, CompositeHeightfields);

                        for (int ClipmapIndex = 0; ClipmapIndex < Clipmaps.size(); ClipmapIndex++)
                        {
//                            SCOPED_DRAW_EVENTF(RHICmdList, Clipmap, TEXT("CacheType %s Clipmap %u"), CacheType == GDF_MostlyStatic ? TEXT("MostlyStatic") : TEXT("Movable"), ClipmapIndex);

                            FGlobalDistanceFieldClipmap Clipmap = Clipmaps.get(ClipmapIndex);

                            for (int UpdateRegionIndex = 0; UpdateRegionIndex < Clipmap.UpdateRegions.size(); UpdateRegionIndex++)
                            {
                                FVolumeUpdateRegion UpdateRegion = Clipmap.UpdateRegions.get(UpdateRegionIndex);

                                if ((UpdateRegion.UpdateType & VUT_Heightfields) != 0)
                                {
                                    // TODO
//                                    View.HeightfieldLightingViewInfo.CompositeHeightfieldsIntoGlobalDistanceField(RHICmdList, Scene, View, GlobalMaxSphereQueryRadius, GlobalDistanceFieldInfo, Clipmap, ClipmapIndex, UpdateRegion);
                                }
                            }
                        }
                    }


                    // Transition clipmaps from compute to gfx.
                    /*for (int ClipmapIndex = 0; ClipmapIndex < Clipmaps.size(); ClipmapIndex++)
                    {
                        FGlobalDistanceFieldClipmap& Clipmap = Clipmaps[ClipmapIndex];

                        RHICmdList.TransitionResource(EResourceTransitionAccess::EReadable, EResourceTransitionPipeline::EComputeToGfx, Clipmap.RenderTarget->GetRenderTargetItem().UAV);
                    }*/
                }

                /*if (IsTransientResourceBufferAliasingEnabled())
                {
                    GGlobalDistanceFieldCulledObjectBuffers.Buffers.DiscardTransientResource();
                    GObjectGridBuffers.DiscardTransientResource();
                }*/
            }
        }
    }
}
