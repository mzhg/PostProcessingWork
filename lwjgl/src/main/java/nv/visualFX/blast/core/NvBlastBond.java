package nv.visualFX.blast.core;

/**
 * Represents the interface between two chunks.  At most one bond is created for a chunk pair.<p></p>
 * Created by mazhen'gui on 2017/9/9.
 */

public class NvBlastBond {
    /**
     Interface average normal
     */
    public final float[]		normal=new float[3];

    /**
     Area of interface
     */
    public float		area;

    /**
     Central position on the interface between chunks
     */
    public final float[]		centroid = new float[3];

    /**
     Extra data associated with bond, e.g. whether or not to create a joint
     */
    public int	userData;
}
