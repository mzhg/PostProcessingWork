package jet.opengl.renderer.Unreal4.distancefield;

import org.lwjgl.util.vector.Vector3i;

import jet.opengl.postprocessing.util.BoundingBox;

public class FVolumeUpdateRegion {
    /** World space bounds. */
    public final BoundingBox Bounds = new BoundingBox();

    /** Number of texels in each dimension to update. */
    public final Vector3i CellsSize = new Vector3i();

    public int  UpdateType = EVolumeUpdateType.VUT_All;
}
