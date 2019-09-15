package jet.opengl.renderer.Unreal4;

import jet.opengl.postprocessing.texture.TextureGL;

public class FScreenSpaceFilteringHistory {
    // Number of history render target to store.
    public static final int RTCount = 3;

    // Render target specific to the history.
    public final TextureGL[] RT = new TextureGL[RTCount];

    // The texture for tile classification.
    public TextureGL TileClassification;

    public void SafeRelease()
    {
        for (int i = 0; i < RTCount; i++)
            RT[i].dispose();
        TileClassification.dispose();
    }

    public boolean IsValid()
    {
        return RT[0].isValid();
    }
}
