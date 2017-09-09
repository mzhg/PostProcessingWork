package nv.visualFX.blast.core;

/**
 * Chunk bond descriptor used to build an asset.  See {@link NvBlastAssetDesc}.<p></p>
 * Created by mazhen'gui on 2017/9/9.
 */

public class NvBlastBondDesc {
    /** Bond data (see NvBlastBond). */
    public final NvBlastBond	bond = new NvBlastBond();

    /**
     The indices of the chunks linked by this bond.  They must be different support chunk indices.
     If one of the chunk indices is the invalid index (UINT32_MAX), then this will create a bond between
     the chunk indexed by the other index (which must be valid) and "the world."  Any actor containing
     this bond will cause the function NvBlastActorIsBoundToWorld to return true.
     */
    public final int[]	chunkIndices = new int[2];
}
