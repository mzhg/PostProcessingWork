package jet.opengl.renderer.Unreal4.distancefield;

import org.lwjgl.util.vector.Vector4f;

import java.util.ArrayList;

import jet.opengl.postprocessing.texture.TextureGL;

public class FGlobalDistanceFieldCacheTypeState {

    public final ArrayList<Vector4f> PrimitiveModifiedBounds = new ArrayList<>();
    // TODO Cached TextureGL.
    public TextureGL VolumeTexture;
}
