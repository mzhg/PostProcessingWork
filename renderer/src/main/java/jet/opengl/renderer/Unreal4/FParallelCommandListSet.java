package jet.opengl.renderer.Unreal4;

import jet.opengl.renderer.Unreal4.api.FMeshPassProcessorRenderState;

public class FParallelCommandListSet {
    FViewInfo2 View;
    FSceneRenderer SceneRenderer;
    final FMeshPassProcessorRenderState DrawRenderState = new FMeshPassProcessorRenderState();
//    FRHICommandListImmediate& ParentCmdList;
//    FRHIGPUMask GPUMask; // Copy of the Parent GPUMask at creation (since it could change).
    FSceneRenderTargets Snapshot;
    TStatId	ExecuteStat;
    int32 Width;
    int32 NumAlloc;
    int32 MinDrawsPerCommandList;
    // see r.RHICmdBalanceParallelLists
    bool bBalanceCommands;
    // see r.RHICmdSpewParallelListBalance
    bool bSpewBalance;
}
