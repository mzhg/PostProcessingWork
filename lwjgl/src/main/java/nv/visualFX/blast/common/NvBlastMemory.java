package nv.visualFX.blast.common;

/**
 * Created by mazhen'gui on 2017/9/9.
 */

public final class NvBlastMemory {
    /**
     Utility function to align the given value to the next 16-byte boundary.

     Returns the aligned value.
     */
    public static int align16(int value)
    {
        return (value + 0xF)&~0xF;
    }
}
