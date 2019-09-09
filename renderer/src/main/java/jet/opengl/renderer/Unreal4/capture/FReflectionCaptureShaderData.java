package jet.opengl.renderer.Unreal4.capture;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector4f;

import jet.opengl.renderer.Unreal4.UE4Engine;

/** Per-reflection capture data needed by the shader. */
public class FReflectionCaptureShaderData {
    public final Vector4f[] PositionAndRadius = new Vector4f[UE4Engine.GMaxNumReflectionCaptures];
    public final Vector4f[] CaptureProperties = new Vector4f[UE4Engine.GMaxNumReflectionCaptures];
    public final Vector4f[] CaptureOffsetAndAverageBrightness = new Vector4f[UE4Engine.GMaxNumReflectionCaptures];
    public final Matrix4f[] BoxTransform = new Matrix4f[UE4Engine.GMaxNumReflectionCaptures];
    public final Vector4f[] BoxScales = new Vector4f[UE4Engine.GMaxNumReflectionCaptures];
}
