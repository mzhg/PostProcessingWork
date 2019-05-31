package jet.opengl.particles;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

public class Particle {
    /** The unique name of the particle */
    public int ID;

    /** The current position of the particle. */
    public final Vector3f position = new Vector3f();
    /** The relative time of the particle. */
    public float relativeTime;
    /** The current velocity of the particle. */
    public final Vector3f velocity = new Vector3f();
    /** The time scale for the particle. */
    public float timeScale;
    /** The current size of the particle. */
    public final Vector2f size = new Vector2f();
}
