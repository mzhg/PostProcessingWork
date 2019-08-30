package jet.opengl.renderer.Unreal4;

// Stores the primitive count of each translucency pass (redundant, could be computed after sorting but this way we touch less memory)
public class FTranslucenyPrimCount {
    private final int[] Count = new int[ETranslucencyPass.TPT_MAX];
    private final boolean[] UseSceneColorCopyPerPass = new boolean[ETranslucencyPass.TPT_MAX];
    private final boolean[] DisableOffscreenRenderingPerPass = new boolean[ETranslucencyPass.TPT_MAX];

    // constructor
    /*FTranslucenyPrimCount()
    {
        for(int i = 0; i < ETranslucencyPass.TPT_MAX; ++i)
        {
            Count[i] = 0;
            UseSceneColorCopyPerPass[i] = false;
            DisableOffscreenRenderingPerPass[i] = false;
        }
    }*/

    // interface similar to TArray but here we only store the count of Prims per pass
    void Append(FTranslucenyPrimCount InSrc)
    {
        for(int i = 0; i < ETranslucencyPass.TPT_MAX; ++i)
        {
            Count[i] += InSrc.Count[i];
            UseSceneColorCopyPerPass[i] |= InSrc.UseSceneColorCopyPerPass[i];
            DisableOffscreenRenderingPerPass[i] |= InSrc.DisableOffscreenRenderingPerPass[i];
        }
    }

    // interface similar to TArray but here we only store the count of Prims per pass
    void Add(/*ETranslucencyPass::Type*/int InPass, boolean bUseSceneColorCopy, boolean bDisableOffscreenRendering)
    {
        ++Count[InPass];
        UseSceneColorCopyPerPass[InPass] |= bUseSceneColorCopy;
        DisableOffscreenRenderingPerPass[InPass] |= bDisableOffscreenRendering;
    }

    int Num(/*ETranslucencyPass::Type*/int InPass)
    {
        return Count[InPass];
    }

    int NumPrims()
    {
        int NumTotal = 0;
        for (int PassIndex = 0; PassIndex < ETranslucencyPass.TPT_MAX; ++PassIndex)
        {
            NumTotal += Count[PassIndex];
        }
        return NumTotal;
    }

    boolean UseSceneColorCopy(/*ETranslucencyPass::Type*/int InPass)
    {
        return UseSceneColorCopyPerPass[InPass];
    }

    boolean DisableOffscreenRendering(/*ETranslucencyPass::Type*/int InPass)
    {
        return DisableOffscreenRenderingPerPass[InPass];
    }
}
