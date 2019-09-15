package jet.opengl.renderer.Unreal4.utils;

import org.lwjgl.util.vector.Vector2i;

import java.nio.ByteBuffer;

public final class RenderUtils {

    public static void QuantizeSceneBufferSize(Vector2i InBufferSize, Vector2i OutBufferSize)
    {
        // Ensure sizes are dividable by the ideal group size for 2d tiles to make it more convenient.
	    final int DividableBy = 4;

//        check(DividableBy % 4 == 0); // A lot of graphic algorithms where previously assuming DividableBy == 4.

	    final int Mask = ~(DividableBy - 1);
        OutBufferSize.x = (InBufferSize.x + DividableBy - 1) & Mask;
        OutBufferSize.y = (InBufferSize.y + DividableBy - 1) & Mask;
    }

    public static ByteBuffer slice(ByteBuffer src, int offset){
        int oldOffset = src.position();
        src.position(offset);

        ByteBuffer result = src.slice();
        src.position(oldOffset);

        return result;
    }
}
