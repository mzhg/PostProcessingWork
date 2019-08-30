package jet.opengl.renderer.Unreal4;

import java.util.ArrayList;

import jet.opengl.postprocessing.texture.TextureGL;

public class FShadowMapRenderTargetsRefCounted {
//    TArray<TRefCountPtr<IPooledRenderTarget>, SceneRenderingAllocator> ColorTargets;
//    TRefCountPtr<IPooledRenderTarget> DepthTarget;
    final ArrayList<TextureGL> ColorTargets = new ArrayList<>();
    TextureGL DepthTarget;

    boolean IsValid()
    {
        if (DepthTarget != null)
        {
            return true;
        }
        else
        {
            return ColorTargets.size() > 0;
        }
    }

    /*FIntPoint GetSize() const
    {
		const FPooledRenderTargetDesc* Desc = NULL;

        if (DepthTarget)
        {
            Desc = &DepthTarget->GetDesc();
        }
        else
        {
            check(ColorTargets.Num() > 0);
            Desc = &ColorTargets[0]->GetDesc();
        }

        return Desc->Extent;
    }*/

    int GetSizeX(){
        if(DepthTarget != null){
            return DepthTarget.getWidth();
        }
        else {
            return ColorTargets.get(0).getWidth();
        }
    }

    int GetSizeY(){
        if(DepthTarget != null){
            return DepthTarget.getHeight();
        }
        else {
            return ColorTargets.get(0).getHeight();
        }
    }

    long ComputeMemorySize()
    {
        long MemorySize = 0;

        for (int i = 0; i < ColorTargets.size(); i++)
        {
            MemorySize += ColorTargets.get(i).computeMemorySize();
        }

        if (DepthTarget != null)
        {
            MemorySize += DepthTarget.computeMemorySize();
        }

        return MemorySize;
    }

    void Release()
    {
        for (int i = 0; i < ColorTargets.size(); i++)
        {
//            ColorTargets[i] = NULL;
            ColorTargets.get(i).dispose();
        }

        ColorTargets.clear();

        if(DepthTarget != null)
            DepthTarget.dispose();
        DepthTarget = null;
    }
}
