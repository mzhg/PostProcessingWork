package jet.opengl.demos.Unreal4.distancefield;

import org.lwjgl.util.vector.Vector3f;

import java.util.ArrayList;

import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.util.BoundingBox;

public class FGlobalDistanceFieldClipmap {
    /** World space bounds. */
    public final BoundingBox Bounds = new BoundingBox();

    /** Offset applied to UVs so that only new or dirty areas of the volume texture have to be updated. */
    public final Vector3f ScrollOffset = new Vector3f();

    /** Regions in the volume texture to update. */
    public final ArrayList<FVolumeUpdateRegion> UpdateRegions = new ArrayList<>();

    /** Volume texture for this clipmap. TODO Cached texture */
    public TextureGL RenderTarget;
}
