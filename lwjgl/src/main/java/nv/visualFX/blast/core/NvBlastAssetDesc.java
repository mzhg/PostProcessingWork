package nv.visualFX.blast.core;

/**
 * Asset descriptor, used to build an asset with NvBlastCreateAsset
 * <p>
 * A valid asset descriptor must have a non-zero chunkCount and valid chunkDescs.
 * <p>
 * The user may create an asset with no bonds (e.g. a single-chunk asset).  In this case bondCount should be
 * zero and bondDescs is ignored.<p></p>
 * Created by mazhen'gui on 2017/9/9.
 */

public class NvBlastAssetDesc {
    /** The number of chunk descriptors. */
    public int				chunkCount;

    /** Array of chunk descriptors of size chunkCount. */
    public NvBlastChunkDesc[]	chunkDescs;

    /** The number of bond descriptors. */
    public int				bondCount;

    /** Array of bond descriptors of size bondCount. */
    public NvBlastBondDesc[]	bondDescs;
}
