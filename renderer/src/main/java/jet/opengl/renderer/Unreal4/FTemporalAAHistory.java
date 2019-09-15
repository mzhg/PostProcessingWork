package jet.opengl.renderer.Unreal4;

import org.lwjgl.util.vector.Vector2i;

import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.util.CachaRes;
import jet.opengl.postprocessing.util.Recti;

// Structure in charge of storing all information about TAA's history.
public class FTemporalAAHistory {
    // Number of render target in the history.
    public static final int kRenderTargetCount = 2;

    // Render targets holding's pixel history.
    //  scene color's RGBA are in RT[0].
    @CachaRes
    public final TextureGL[] RT = new TextureGL[kRenderTargetCount];

    // Reference size of RT. Might be different than RT's actual size to handle down res.
    public final Vector2i ReferenceBufferSize = new Vector2i();

    // Viewport coordinate of the history in RT according to ReferenceBufferSize.
    public final Recti ViewportRect = new Recti();

    void SafeRelease()
    {
        for (int i = 0; i < kRenderTargetCount; i++)
        {
            RT[i].dispose();
        }
    }

    public boolean IsValid()
    {
        return RT[0].isValid();
    }
}
