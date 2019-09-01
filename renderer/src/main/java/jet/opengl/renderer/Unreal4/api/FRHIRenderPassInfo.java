package jet.opengl.renderer.Unreal4.api;

import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.util.LogUtil;
import jet.opengl.renderer.Unreal4.UE4Engine;
import jet.opengl.renderer.Unreal4.utils.FResolveParams;

public class FRHIRenderPassInfo{
    private static final class FColorEntry
    {
        public TextureGL RenderTarget;
        public TextureGL ResolveTarget;
        public int ArraySlice;
        public int MipIndex;
        public /*ERenderTargetActions*/int Action;
    };
    private final FColorEntry[] ColorRenderTargets = new FColorEntry[RHIDefinitions.MaxSimultaneousRenderTargets];

    private static final class FDepthStencilEntry
    {
        public TextureGL DepthStencilTarget;
        public TextureGL ResolveTarget;
        public int /*EDepthStencilTargetActions*/ Action;
        public int /*FExclusiveDepthStencil*/ ExclusiveDepthStencil;
    };
    private final FDepthStencilEntry DepthStencilRenderTarget = new FDepthStencilEntry();

    private final FResolveParams ResolveParameters = new FResolveParams();

    // Some RHIs require a hint that occlusion queries will be used in this render pass
    private int NumOcclusionQueries = 0;
    private boolean bOcclusionQueries = false;

    // Some RHIs need to know if this render pass is going to be reading and writing to the same texture in the case of generating mip maps for partial resource transitions
    private boolean bGeneratingMips = false;

    // If this render pass should be multiview
    private boolean bMultiviewPass = false;

    // Hint for some RHI's that renderpass will have specific sub-passes
    private ESubpassHint SubpassHint = ESubpassHint.None;

    // TODO: Remove once FORT-162640 is solved
    private boolean bTooManyUAVs = false;

    //#RenderPasses
    private int UAVIndex = -1;
    private int NumUAVs = 0;
    private TextureGL[] UAVs = new TextureGL[RHIDefinitions.MaxSimultaneousUAVs];

    // Color, no depth, optional resolve, optional mip, optional array slice
    public FRHIRenderPassInfo(TextureGL ColorRT, /*ERenderTargetActions*/int ColorAction, TextureGL ResolveRT /*= nullptr*/, int InMipIndex /*= 0*/, int InArraySlice /*= -1*/)
    {
        UE4Engine.check(ColorRT);
        ColorRenderTargets[0].RenderTarget = ColorRT;
        ColorRenderTargets[0].ResolveTarget = ResolveRT;
        ColorRenderTargets[0].ArraySlice = InArraySlice;
        ColorRenderTargets[0].MipIndex = InMipIndex;
        ColorRenderTargets[0].Action = ColorAction;
        DepthStencilRenderTarget.DepthStencilTarget = null;
        DepthStencilRenderTarget.Action = EDepthStencilTargetActions.DontLoad_DontStore;
        DepthStencilRenderTarget.ExclusiveDepthStencil = FExclusiveDepthStencil.DepthNop_StencilNop;
        DepthStencilRenderTarget.ResolveTarget = null;
        bIsMSAA = ColorRT/*->GetNumSamples()*/.getSampleCount() > 1;
//        FMemory::Memzero(&ColorRenderTargets[1], sizeof(FColorEntry) * (MaxSimultaneousRenderTargets - 1));
    }

    // Color MRTs, no depth
    public FRHIRenderPassInfo(int NumColorRTs, TextureGL ColorRTs[], /*ERenderTargetActions*/int ColorAction)
    {
        UE4Engine.check(NumColorRTs > 0);
        for (int Index = 0; Index < NumColorRTs; ++Index)
        {
            UE4Engine.check(ColorRTs[Index]);
            ColorRenderTargets[Index].RenderTarget = ColorRTs[Index];
            ColorRenderTargets[Index].ResolveTarget = null;
            ColorRenderTargets[Index].ArraySlice = -1;
            ColorRenderTargets[Index].MipIndex = 0;
            ColorRenderTargets[Index].Action = ColorAction;
        }
        DepthStencilRenderTarget.DepthStencilTarget = null;
        DepthStencilRenderTarget.Action = EDepthStencilTargetActions.DontLoad_DontStore;
        DepthStencilRenderTarget.ExclusiveDepthStencil = FExclusiveDepthStencil.DepthNop_StencilNop;
        DepthStencilRenderTarget.ResolveTarget = null;
        if (NumColorRTs < RHIDefinitions.MaxSimultaneousRenderTargets)
        {
//            FMemory::Memzero(&ColorRenderTargets[NumColorRTs], sizeof(FColorEntry) * (MaxSimultaneousRenderTargets - NumColorRTs));
            ColorRenderTargets[NumColorRTs] = null;
        }
    }

    // Color MRTs, no depth
    public FRHIRenderPassInfo(int NumColorRTs, TextureGL ColorRTs[], /*ERenderTargetActions*/int ColorAction, TextureGL ResolveTargets[])
    {
        UE4Engine.check(NumColorRTs > 0);
        for (int Index = 0; Index < NumColorRTs; ++Index)
        {
            UE4Engine.check(ColorRTs[Index]);
            ColorRenderTargets[Index].RenderTarget = ColorRTs[Index];
            ColorRenderTargets[Index].ResolveTarget = ResolveTargets[Index];
            ColorRenderTargets[Index].ArraySlice = -1;
            ColorRenderTargets[Index].MipIndex = 0;
            ColorRenderTargets[Index].Action = ColorAction;
        }
        DepthStencilRenderTarget.DepthStencilTarget = null;
        DepthStencilRenderTarget.Action = EDepthStencilTargetActions.DontLoad_DontStore;
        DepthStencilRenderTarget.ExclusiveDepthStencil = FExclusiveDepthStencil.DepthNop_StencilNop;
        DepthStencilRenderTarget.ResolveTarget = null;
        if (NumColorRTs < RHIDefinitions.MaxSimultaneousRenderTargets)
        {
//            FMemory::Memzero(&ColorRenderTargets[NumColorRTs], sizeof(FColorEntry) * (MaxSimultaneousRenderTargets - NumColorRTs));
        }
    }

    // Color MRTs and depth
    public FRHIRenderPassInfo(int NumColorRTs, TextureGL ColorRTs[], /*ERenderTargetActions*/int ColorAction, TextureGL DepthRT, /*EDepthStencilTargetActions*/int DepthActions, /*FExclusiveDepthStencil*/int InEDS /*= FExclusiveDepthStencil::DepthWrite_StencilWrite*/)
    {
        UE4Engine.check(NumColorRTs > 0);
        for (int Index = 0; Index < NumColorRTs; ++Index)
        {
            UE4Engine.check(ColorRTs[Index]);
            ColorRenderTargets[Index].RenderTarget = ColorRTs[Index];
            ColorRenderTargets[Index].ResolveTarget = null;
            ColorRenderTargets[Index].ArraySlice = -1;
            ColorRenderTargets[Index].MipIndex = 0;
            ColorRenderTargets[Index].Action = ColorAction;
        }
        UE4Engine.check(DepthRT);
        DepthStencilRenderTarget.DepthStencilTarget = DepthRT;
        DepthStencilRenderTarget.ResolveTarget = null;
        DepthStencilRenderTarget.Action = DepthActions;
        DepthStencilRenderTarget.ExclusiveDepthStencil = InEDS;
        bIsMSAA = DepthRT/*->GetNumSamples()*/.getSampleCount() > 1;
        if (NumColorRTs < RHIDefinitions.MaxSimultaneousRenderTargets)
        {
//            FMemory::Memzero(&ColorRenderTargets[NumColorRTs], sizeof(FColorEntry) * (MaxSimultaneousRenderTargets - NumColorRTs));
        }
    }

    // Color MRTs and depth
    public FRHIRenderPassInfo(int NumColorRTs, TextureGL ColorRTs[], /*ERenderTargetActions*/int ColorAction, TextureGL ResolveRTs[], TextureGL DepthRT, /*EDepthStencilTargetActions*/int DepthActions, /*FRHITexture**/TextureGL ResolveDepthRT, /*FExclusiveDepthStencil*/int InEDS /*= FExclusiveDepthStencil::DepthWrite_StencilWrite*/)
    {
        UE4Engine.check(NumColorRTs > 0);
        for (int Index = 0; Index < NumColorRTs; ++Index)
        {
            UE4Engine.check(ColorRTs[Index]);
            ColorRenderTargets[Index].RenderTarget = ColorRTs[Index];
            ColorRenderTargets[Index].ResolveTarget = ResolveRTs[Index];
            ColorRenderTargets[Index].ArraySlice = -1;
            ColorRenderTargets[Index].MipIndex = 0;
            ColorRenderTargets[Index].Action = ColorAction;
        }
        UE4Engine.check(DepthRT);
        DepthStencilRenderTarget.DepthStencilTarget = DepthRT;
        DepthStencilRenderTarget.ResolveTarget = ResolveDepthRT;
        DepthStencilRenderTarget.Action = DepthActions;
        DepthStencilRenderTarget.ExclusiveDepthStencil = InEDS;
        bIsMSAA = DepthRT/*->GetNumSamples()*/.getSampleCount() > 1;
        if (NumColorRTs < RHIDefinitions.MaxSimultaneousRenderTargets)
        {
//            FMemory::Memzero(&ColorRenderTargets[NumColorRTs], sizeof(FColorEntry) * (MaxSimultaneousRenderTargets - NumColorRTs));
        }
    }

    // Depth, no color
    public FRHIRenderPassInfo(TextureGL DepthRT, /*EDepthStencilTargetActions*/int DepthActions, TextureGL ResolveDepthRT /*= nullptr*/, /*FExclusiveDepthStencil*/int InEDS /*= FExclusiveDepthStencil::DepthWrite_StencilWrite*/)
    {
        UE4Engine.check(DepthRT);
        DepthStencilRenderTarget.DepthStencilTarget = DepthRT;
        DepthStencilRenderTarget.ResolveTarget = ResolveDepthRT;
        DepthStencilRenderTarget.Action = DepthActions;
        DepthStencilRenderTarget.ExclusiveDepthStencil = InEDS;
        bIsMSAA = DepthRT/*->GetNumSamples()*/.getSampleCount() > 1;
//        FMemory::Memzero(ColorRenderTargets, sizeof(FColorEntry) * MaxSimultaneousRenderTargets);
    }

    // Depth, no color, occlusion queries
    public FRHIRenderPassInfo(TextureGL DepthRT, int InNumOcclusionQueries, /*EDepthStencilTargetActions*/int DepthActions, TextureGL ResolveDepthRT /*= nullptr*/, /*FExclusiveDepthStencil*/int InEDS /*= FExclusiveDepthStencil::DepthWrite_StencilWrite*/)
//		: NumOcclusionQueries(InNumOcclusionQueries)
//		, bOcclusionQueries(true)
    {
        NumOcclusionQueries = InNumOcclusionQueries;
        bOcclusionQueries = true;
        UE4Engine.check(DepthRT);
        DepthStencilRenderTarget.DepthStencilTarget = DepthRT;
        DepthStencilRenderTarget.ResolveTarget = ResolveDepthRT;
        DepthStencilRenderTarget.Action = DepthActions;
        DepthStencilRenderTarget.ExclusiveDepthStencil = InEDS;
        bIsMSAA = DepthRT/*->GetNumSamples()*/.getSampleCount() > 1;
//        FMemory::Memzero(ColorRenderTargets, sizeof(FColorEntry) * MaxSimultaneousRenderTargets);
    }

    // Color and depth
    public FRHIRenderPassInfo(TextureGL ColorRT, /*ERenderTargetActions*/int ColorAction, TextureGL DepthRT, /*EDepthStencilTargetActions*/int DepthActions, /*FExclusiveDepthStencil*/int InEDS /*= FExclusiveDepthStencil::DepthWrite_StencilWrite*/, int _void)
    {
        UE4Engine.check(ColorRT);
        ColorRenderTargets[0].RenderTarget = ColorRT;
        ColorRenderTargets[0].ResolveTarget = null;
        ColorRenderTargets[0].ArraySlice = -1;
        ColorRenderTargets[0].MipIndex = 0;
        ColorRenderTargets[0].Action = ColorAction;
        bIsMSAA = ColorRT/*->GetNumSamples()*/.getSampleCount() > 1;
        UE4Engine.check(DepthRT);
        DepthStencilRenderTarget.DepthStencilTarget = DepthRT;
        DepthStencilRenderTarget.ResolveTarget = null;
        DepthStencilRenderTarget.Action = DepthActions;
        DepthStencilRenderTarget.ExclusiveDepthStencil = InEDS;
//        FMemory::Memzero(&ColorRenderTargets[1], sizeof(FColorEntry) * (MaxSimultaneousRenderTargets - 1));
    }

    // Color and depth with resolve
    public FRHIRenderPassInfo(/*FRHITexture**/TextureGL ColorRT, /*ERenderTargetActions*/int ColorAction, TextureGL ResolveColorRT,
                                              TextureGL DepthRT, /*EDepthStencilTargetActions*/int DepthActions, TextureGL ResolveDepthRT, /*FExclusiveDepthStencil*/int InEDS /*= FExclusiveDepthStencil::DepthWrite_StencilWrite*/)
    {
        UE4Engine.check(ColorRT);
        ColorRenderTargets[0].RenderTarget = ColorRT;
        ColorRenderTargets[0].ResolveTarget = ResolveColorRT;
        ColorRenderTargets[0].ArraySlice = -1;
        ColorRenderTargets[0].MipIndex = 0;
        ColorRenderTargets[0].Action = ColorAction;
        bIsMSAA = ColorRT/*->GetNumSamples()*/.getSampleCount() > 1;
        UE4Engine.check(DepthRT);
        DepthStencilRenderTarget.DepthStencilTarget = DepthRT;
        DepthStencilRenderTarget.ResolveTarget = ResolveDepthRT;
        DepthStencilRenderTarget.Action = DepthActions;
        DepthStencilRenderTarget.ExclusiveDepthStencil = InEDS;
//        FMemory::Memzero(&ColorRenderTargets[1], sizeof(FColorEntry) * (MaxSimultaneousRenderTargets - 1));
    }

    public FRHIRenderPassInfo(int InNumUAVs, TextureGL[] InUAVs)
    {
        if (InNumUAVs > RHIDefinitions.MaxSimultaneousUAVs)
        {
            OnVerifyNumUAVsFailed(InNumUAVs);
            InNumUAVs = RHIDefinitions.MaxSimultaneousUAVs;
        }

//        FMemory::Memzero(*this);
        NumUAVs = InNumUAVs;
        for (int Index = 0; Index < InNumUAVs; Index++)
        {
            UAVs[Index] = InUAVs[Index];
        }
    }

    public int GetNumColorRenderTargets()
    {
        int ColorIndex = 0;
        for (; ColorIndex < RHIDefinitions.MaxSimultaneousRenderTargets; ++ColorIndex)
        {
            FColorEntry Entry = ColorRenderTargets[ColorIndex];
            if (Entry.RenderTarget == null)
            {
                break;
            }
        }

        return ColorIndex;
    }

    public  FRHIRenderPassInfo()
    {
//        FMemory::Memzero(*this);
    }

    public boolean IsMSAA()
    {
        return bIsMSAA;
    }

//#if !(UE_BUILD_SHIPPING || UE_BUILD_TEST)
//    RHI_API void Validate() const;
//#else
//    RHI_API void Validate() const {}
//#endif
    /*public void ConvertToRenderTargetsInfo(FRHISetRenderTargetsInfo OutRTInfo){

    }*/


    private boolean bIsMSAA = false;

    private void OnVerifyNumUAVsFailed(int InNumUAVs){
        bTooManyUAVs = true;
//        UE_LOG(LogRHI, Warning, TEXT(), InNumUAVs, MaxSimultaneousUAVs);
        LogUtil.w(LogUtil.LogType.DEFAULT, String.format("NumUAVs is %d which is greater the max %d. Trailing UAVs will be dropped", InNumUAVs, RHIDefinitions.MaxSimultaneousUAVs));
        // Trigger an ensure to get callstack in dev builds
        UE4Engine.ensure(InNumUAVs <= RHIDefinitions.MaxSimultaneousUAVs);
    }
}
