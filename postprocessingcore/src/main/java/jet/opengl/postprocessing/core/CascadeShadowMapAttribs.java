package jet.opengl.postprocessing.core;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;

/**
 * Created by mazhen'gui on 2017/6/3.
 */

public class CascadeShadowMapAttribs {
    public static final int MAX_CASCADES = 8;

    public Matrix4f worldToLightView;
    public final Matrix4f[] worldToShadowMapUVDepth = new Matrix4f[MAX_CASCADES];
    public final Vector2f[] startEndZ = new Vector2f[MAX_CASCADES];
    public int numCascades;
}
