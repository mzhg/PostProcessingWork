package jet.opengl.particles;

import org.lwjgl.util.vector.Vector3f;

public class ParticleEmitter {
    /** The color scale of a particle over time. */
    public final RangeVector3f colorScale = new RangeVector3f(Vector3f.ONE, Vector3f.ONE);
    /** The alpha scale of a particle over time */
    public final RangeFloat alphaScale = new RangeFloat(1, 1);

    // 4.26, 5--9,13,20;;  5.22矿工一听啊
}
