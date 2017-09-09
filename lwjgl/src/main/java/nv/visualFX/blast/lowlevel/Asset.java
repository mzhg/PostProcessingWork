package nv.visualFX.blast.lowlevel;

import nv.visualFX.blast.common.AssetDataOffsets;
import nv.visualFX.blast.common.NvBlastIndexFns;
import nv.visualFX.blast.common.NvBlastMemory;
import nv.visualFX.blast.core.NvBlastAsset;
import nv.visualFX.blast.core.NvBlastAssetDesc;
import nv.visualFX.blast.core.NvBlastBondDesc;
import nv.visualFX.blast.core.NvBlastChunkDesc;

/**
 * Created by mazhen'gui on 2017/9/9.
 */

public class Asset implements nv.visualFX.blast.core.NvBlastAsset{
    /**
     Returns the number of bytes of memory that an asset created using the given descriptor will require.  A pointer
     to a block of memory of at least this size must be passed in as the mem argument of create.

     @param desc	The asset descriptor that will be passed into NvBlastCreateAsset.
     */
    public static int getMemorySize(NvBlastAssetDesc desc)
    {
        // Count graph nodes
        int graphNodeCount = 0;
        for (int i = 0; i < desc.chunkCount; ++i)
        {
            graphNodeCount += ((desc.chunkDescs[i].flags & NvBlastChunkDesc.SUPPORT_FLAGS) != 0)?1:0;
        }

        for (int i = 0; i < desc.bondCount; ++i)
        {
            final NvBlastBondDesc bondDesc = desc.bondDescs[i];
            final int chunkIndex0 = bondDesc.chunkIndices[0];
            final int chunkIndex1 = bondDesc.chunkIndices[1];
            if ((NvBlastIndexFns.isInvalidIndex(chunkIndex0) && chunkIndex1 < desc.chunkCount) ||
                    (NvBlastIndexFns.isInvalidIndex(chunkIndex1) && chunkIndex0 < desc.chunkCount))
            {
                ++graphNodeCount;	// world node
                break;
            }
        }

        AssetDataOffsets offsets = new AssetDataOffsets();
        return createAssetDataOffsets(offsets, desc.chunkCount, graphNodeCount, desc.bondCount);
    }

    /**
     Returns the size of the scratch space (in bytes) required to be passed into the create function, based upon
     the input descriptor that will be passed to the create function.

     @param desc	The descriptor that will be passed to the create function.

     @return the number of bytes required.
     */
    public static int createRequiredScratch(NvBlastAssetDesc desc)
    {
        /*#if NVBLASTLL_CHECK_PARAMS
        if (desc == nullptr)
        {
            NVBLAST_ALWAYS_ASSERT();
            return 0;
        }
        #endif*/

        // Aligned and padded
        return 16 + NvBlastMemory.align16(desc.chunkCount*/*sizeof(char)*/1) +
                NvBlastMemory.align16(desc.chunkCount*/*sizeof(uint32_t)*/4) +
                NvBlastMemory.align16(2 * desc.bondCount*BondSortData.SIZE) +
                NvBlastMemory.align16(desc.bondCount*/*sizeof(uint32_t)*/4);
    }

    public static int createAssetDataOffsets(AssetDataOffsets offsets, int chunkCount, int graphNodeCount, int bondCount)
    {
        /*NvBlastCreateOffsetStart(sizeof(Asset));
        NvBlastCreateOffsetAlign16(offsets.m_chunks, chunkCount * sizeof(NvBlastChunk));
        NvBlastCreateOffsetAlign16(offsets.m_bonds, bondCount * sizeof(NvBlastBond));
        NvBlastCreateOffsetAlign16(offsets.m_subtreeLeafChunkCounts, chunkCount * sizeof(uint32_t));
        NvBlastCreateOffsetAlign16(offsets.m_supportChunkIndices, graphNodeCount * sizeof(uint32_t));
        NvBlastCreateOffsetAlign16(offsets.m_chunkToGraphNodeMap, chunkCount * sizeof(uint32_t));
        NvBlastCreateOffsetAlign16(offsets.m_graphAdjacencyPartition, (graphNodeCount + 1) * sizeof(uint32_t));
        NvBlastCreateOffsetAlign16(offsets.m_graphAdjacentNodeIndices, (2 * bondCount) * sizeof(uint32_t));
        NvBlastCreateOffsetAlign16(offsets.m_graphAdjacentBondIndices, (2 * bondCount) * sizeof(uint32_t));
        return NvBlastCreateOffsetEndAlign16();*/

        return 0;  // TODO
    }

    public static NvBlastAsset create(NvBlastAssetDesc desc) {

        return null;
    }
}
