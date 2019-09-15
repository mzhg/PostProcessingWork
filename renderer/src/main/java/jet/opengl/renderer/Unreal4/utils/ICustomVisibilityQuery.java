package jet.opengl.renderer.Unreal4.utils;

import jet.opengl.postprocessing.util.BoundingBox;

public interface ICustomVisibilityQuery {
    /** prepares the query for visibility tests */
    boolean Prepare();

    /** test primitive visiblity */
    boolean IsVisible(int VisibilityId, /*FBoxSphereBounds*/BoundingBox Bounds);

    /** return true if we can call IsVisible from a ParallelFor */
    default boolean IsThreadsafe()
    {
        return false;
    }
}
