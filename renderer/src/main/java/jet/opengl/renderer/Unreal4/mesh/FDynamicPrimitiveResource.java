package jet.opengl.renderer.Unreal4.mesh;

/**
 * An interface implemented by dynamic resources which need to be initialized and cleaned up by the rendering thread.
 */
public interface FDynamicPrimitiveResource {
    void InitPrimitiveResource();
    void ReleasePrimitiveResource();
}
