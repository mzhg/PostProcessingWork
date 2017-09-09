package nv.visualFX.blast.common;

/**
 * Created by mazhen'gui on 2017/9/9.
 */

public final class NvBlastIndexFns {
    /**
     Test for invalid index (max representable integer).
     */
    public static boolean isInvalidIndex(int index)
    {
        return index == (~0);
    }
}
