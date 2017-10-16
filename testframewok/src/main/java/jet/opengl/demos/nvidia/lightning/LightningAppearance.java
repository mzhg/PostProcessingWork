package jet.opengl.demos.nvidia.lightning;

import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

/**
 * Created by mazhen'gui on 2017/8/28.
 */

final class LightningAppearance implements Readable{
    static final int SIZE = Vector4f.SIZE * 3;
    final Vector3f ColorInside = new Vector3f();
    float		ColorFallOffExponent;		// to match HLSL packing rules

    final Vector3f ColorOutside = new Vector3f();
//    float		Dummy1;						// dummy to match HLSL padding

    final Vector2f BoltWidth = new Vector2f();

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        ColorInside.store(buf);
        buf.putFloat(ColorFallOffExponent);

        ColorOutside.store(buf);
        buf.putFloat(0);

        BoltWidth.store(buf);
        buf.putInt(0).putInt(0);
        return buf;
    }

    public void set(LightningAppearance other) {
        ColorInside.set(other.ColorInside);
        ColorFallOffExponent = other.ColorFallOffExponent;
        ColorOutside.set(other.ColorOutside);
        BoltWidth.set(other.BoltWidth);
    }
}
