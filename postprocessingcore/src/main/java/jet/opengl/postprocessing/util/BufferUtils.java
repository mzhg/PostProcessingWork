package jet.opengl.postprocessing.util;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

/**
 * Created by mazhen'gui on 2017/4/1.
 */

public class BufferUtils {
    /**
     * Allocates a direct native-ordered bytebuffer with the specified capacity.
     *
     * @param capacity The capacity, in bytes
     *
     * @return a ByteBuffer
     */
    public static ByteBuffer createByteBuffer(int capacity) {
        return ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder());
    }

    /**
     * Allocates a direct native-order shortbuffer with the specified number
     * of elements.
     *
     * @param capacity The capacity, in shorts
     *
     * @return a ShortBuffer
     */
    public static ShortBuffer createShortBuffer(int capacity) {
        return createByteBuffer(capacity << 1).asShortBuffer();
    }

    /**
     * Allocates a direct native-order charbuffer with the specified number
     * of elements.
     *
     * @param capacity The capacity, in chars
     *
     * @return an CharBuffer
     */
    public static CharBuffer createCharBuffer(int capacity) {
        return createByteBuffer(capacity << 1).asCharBuffer();
    }

    /**
     * Allocates a direct native-order intbuffer with the specified number
     * of elements.
     *
     * @param capacity The capacity, in ints
     *
     * @return an IntBuffer
     */
    public static IntBuffer createIntBuffer(int capacity) {
        return createByteBuffer(capacity << 2).asIntBuffer();
    }

    /**
     * Allocates a direct native-order longbuffer with the specified number
     * of elements.
     *
     * @param capacity The capacity, in longs
     *
     * @return an LongBuffer
     */
    public static LongBuffer createLongBuffer(int capacity) {
        return createByteBuffer(capacity << 3).asLongBuffer();
    }

    /**
     * Allocates a direct native-order floatbuffer with the specified number
     * of elements.
     *
     * @param capacity The capacity, in floats
     *
     * @return a FloatBuffer
     */
    public static FloatBuffer createFloatBuffer(int capacity) {
        return createByteBuffer(capacity << 2).asFloatBuffer();
    }

    /**
     * Allocates a direct native-order doublebuffer with the specified number
     * of elements.
     *
     * @param capacity The capacity, in floats
     *
     * @return a FloatBuffer
     */
    public static DoubleBuffer createDoubleBuffer(int capacity) {
        return createByteBuffer(capacity << 3).asDoubleBuffer();
    }

    /**
     * Compute the buffer size by the given buffer.
     * @param buffer
     * @return The size of the buffer.
     */
    public static int measureSize(Buffer buffer){
        if(buffer == null){
            return 0;
        }else if(buffer instanceof ByteBuffer){
            return buffer.remaining();
        }else if(buffer instanceof ShortBuffer || buffer instanceof CharBuffer){
            return buffer.remaining() * 2;
        }else if(buffer instanceof  IntBuffer || buffer instanceof FloatBuffer){
            return buffer.remaining() * 4;
        }else if(buffer instanceof LongBuffer || buffer instanceof DoubleBuffer){
            return buffer.remaining() * 8;
        }else{
            throw new IllegalArgumentException("Unkown buffer type: " + buffer.getClass().getName());
        }
    }
}
