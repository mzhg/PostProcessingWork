package jet.opengl.renderer.Unreal4;

import java.util.ArrayList;

public class FSortedShadowMaps {
    /** Visible shadows sorted by their shadow depth map render target. */
    final ArrayList<FSortedShadowMapAtlas> ShadowMapAtlases = new ArrayList<>();

    final ArrayList<FSortedShadowMapAtlas> RSMAtlases = new ArrayList<>();

    final ArrayList<FSortedShadowMapAtlas> ShadowMapCubemaps = new ArrayList<>();

    final FSortedShadowMapAtlas PreshadowCache = new FSortedShadowMapAtlas();

    final ArrayList<FSortedShadowMapAtlas> TranslucencyShadowMapAtlases = new ArrayList<>();

    void Release(){
        throw new UnsupportedOperationException();
    }

    long ComputeMemorySize()
    {
        long MemorySize = 0;

        for (int i = 0; i < ShadowMapAtlases.size(); i++)
        {
            MemorySize += ShadowMapAtlases.get(i).RenderTargets.ComputeMemorySize();
        }

        for (int i = 0; i < RSMAtlases.size(); i++)
        {
            MemorySize += RSMAtlases.get(i).RenderTargets.ComputeMemorySize();
        }

        for (int i = 0; i < ShadowMapCubemaps.size(); i++)
        {
            MemorySize += ShadowMapCubemaps.get(i).RenderTargets.ComputeMemorySize();
        }

        MemorySize += PreshadowCache.RenderTargets.ComputeMemorySize();

        for (int i = 0; i < TranslucencyShadowMapAtlases.size(); i++)
        {
            MemorySize += TranslucencyShadowMapAtlases.get(i).RenderTargets.ComputeMemorySize();
        }

        return MemorySize;
    }
}
