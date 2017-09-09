package nv.visualFX.blast.core;

/**
 * Chunk descriptor used to build an asset.  See NvBlastAssetDesc.<p></p>
 * Created by mazhen'gui on 2017/9/9.
 */

public class NvBlastChunkDesc {
    public static final int
    NO_FLAGS = 0,

    /** If this flag is set then the chunk will become a support chunk, unless an ancestor chunk is also marked as support. */
    SUPPORT_FLAGS = (1 << 0);

    /** Central position in chunk. */
    public final float[]		centroid = new float[3];

    /** Volume of chunk. */
    public float		volume;

    /** Index of this chunk's parent.  If this is a root chunk, then this value must be UINT32_MAX. */
    public int	parentChunkIndex;

    /** See Flags enum for possible flags. */
    public int	flags;

    /** User-supplied data which will be accessible to the user in chunk fracture events. */
    public int	userData;
}
