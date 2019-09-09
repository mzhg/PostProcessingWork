package jet.opengl.renderer.Unreal4.mesh;

import org.lwjgl.util.vector.ReadableVector2f;
import org.lwjgl.util.vector.ReadableVector4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import jet.opengl.renderer.Unreal4.utils.FHitProxyId;

/** The type used to store batched line vertices. */
public class FSimpleElementVertex {

    public final Vector3f Position = new Vector3f();
    public final Vector2f TextureCoordinate = new Vector2f();
    public final Vector4f Color = new Vector4f();
    public int HitProxyIdColor;  //sRGB

    FSimpleElementVertex() {}

    FSimpleElementVertex(ReadableVector4f InPosition, ReadableVector2f InTextureCoordinate,ReadableVector4f InColor, FHitProxyId InHitProxyId)
    {
        Position.set(InPosition);
        TextureCoordinate.set(InTextureCoordinate);
        Color.set(InColor);
        HitProxyIdColor = InHitProxyId.GetColor();
    }
}
