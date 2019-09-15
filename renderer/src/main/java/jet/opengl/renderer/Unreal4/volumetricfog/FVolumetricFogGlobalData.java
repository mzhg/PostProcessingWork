package jet.opengl.renderer.Unreal4.volumetricfog;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector2i;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector3i;

public class FVolumetricFogGlobalData {
    public final Vector3i GridSizeInt = new Vector3i();
    public final Vector3f GridSize = new Vector3f();
    public final Vector3f GridZParams = new Vector3f();
    public final Vector2f SVPosToVolumeUV = new Vector2f();
    public final Vector2i FogGridToPixelXY = new Vector2i();
    public float MaxDistance;
    public final Vector3f HeightFogInscatteringColor = new Vector3f();
    public final Vector3f HeightFogDirectionalLightInscatteringColor = new Vector3f();
}
