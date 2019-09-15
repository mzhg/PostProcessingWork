package jet.opengl.renderer.Unreal4.views;

import jet.opengl.renderer.Unreal4.primitive.FPrimitiveDrawInterface;
import jet.opengl.renderer.Unreal4.scenes.FSceneView;

/**
 * An interface to a scene interaction.
 */
public interface FViewElementDrawer {
    /**
     * Draws the interaction using the given draw interface.
     */
    default void Draw(FSceneView View, FPrimitiveDrawInterface PDI) {}
}
