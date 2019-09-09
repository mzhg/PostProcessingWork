package jet.opengl.renderer.Unreal4.scenes;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

import jet.opengl.renderer.Unreal4.editor.BlueprintReadWrite;
import jet.opengl.renderer.Unreal4.editor.Category;

public class FDepthFieldGlowInfo {
    /** Whether to turn on the outline glow (depth field fonts only) */
    @BlueprintReadWrite
    @Category(value = "Glow")
    public boolean bEnableGlo;

    /** Base color to use for the glow */
    @BlueprintReadWrite
    @Category(value = "Glow")
    public final Vector4f GlowColor = new Vector4f();

    /**
     * If bEnableGlow, outline glow outer radius (0 to 1, 0.5 is edge of character silhouette)
     * glow influence will be 0 at GlowOuterRadius.X and 1 at GlowOuterRadius.Y
     */
    @BlueprintReadWrite
    @Category(value = "Glow")
    public final Vector2f GlowOuterRadius = new Vector2f();

    /***
     * If bEnableGlow, outline glow inner radius (0 to 1, 0.5 is edge of character silhouette)
     * glow influence will be 1 at GlowInnerRadius.X and 0 at GlowInnerRadius.Y
     */
    @BlueprintReadWrite
    @Category(value = "Glow")
    public final Vector2f GlowInnerRadius = new Vector2f();
}
