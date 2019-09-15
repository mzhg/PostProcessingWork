package jet.opengl.renderer.Unreal4.hit;

/**
 * An interface to a hit proxy consumer.
 */
public interface FHitProxyConsumer {
    /**
     * Called when a new hit proxy is rendered.  The hit proxy consumer should keep a TRefCountPtr to the HitProxy to prevent it from being
     * deleted before the rendered hit proxy map.
     */
    void AddHitProxy(HHitProxy HitProxy);
}
