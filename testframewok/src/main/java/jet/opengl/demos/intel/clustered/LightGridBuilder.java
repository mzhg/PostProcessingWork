package jet.opengl.demos.intel.clustered;

import java.util.ArrayList;
import java.util.List;

import jet.opengl.postprocessing.util.Numeric;
import jet.opengl.postprocessing.util.StackInt;
import jet.opengl.postprocessing.util.StackLong;

final class LightGridBuilder {

    private final LightGridDimensions dim;
    // cell grid
    private StackInt[] lightIndexLists;
    private StackLong[] coverageLists;

    private int[] pDstBuffer;
    private int allocatedBytes;

    LightGridBuilder(){
        dim = new LightGridDimensions(0,0,0);
    }

    // packedCells: 2x2x1 packed entries per cell, with 16-bit coverage mask
    void reset(LightGridDimensions dim){
        reset(dim.width, dim.height, dim.depth);
    }

    // packedCells: 2x2x1 packed entries per cell, with 16-bit coverage mask
    void reset(int width, int height, int depth){
        this.dim.set(width, height, depth);
        int cellCount = dim.width * dim.height * dim.depth / 64;
        lightIndexLists= new StackInt[cellCount];
        coverageLists = new StackLong[cellCount];

        for(int i = 0; i < cellCount; i++){
            lightIndexLists[i] = new StackInt();
            coverageLists[i] = new StackLong();
        }
    }

    LightGridDimensions dimensions() { return dim;}

    void clearAllFragments(){
        for (int cellIndex = 0; cellIndex < cellCount(); cellIndex++)
        {
            lightIndexLists[cellIndex].resize(0);
            coverageLists[cellIndex].resize(0);
        }
    }

    void pushFragment(int cellIndex, int lightIndex, long coverage){
        lightIndexLists[cellIndex].push(lightIndex);
        coverageLists[cellIndex].push(coverage);
    }

    void buildAndUpload(int[] gpuBuffer, int bufferSize){
        pDstBuffer = gpuBuffer;

        allocatedBytes = cellCount() * 64 * 16; // uint4: 16 bytes per entry

        for (int y = 0; y < dim.height / 4; y++)
            for (int x = 0; x < dim.width / 4; x++)
                for (int z = 0; z < dim.depth / 4; z++)
                {
                    buildFlatEntries(x, y, z);

                    assert(allocatedBytes <= bufferSize) : "gpu buffer not big enough";
                }
    }

    private static int getFineIndex(int xx, int yy) {
        final int fineIndexTable[][/*4*/] =
        {
            { 0, 1, 4, 5 },
            { 2, 3, 6, 7 },
            { 8, 9, 12, 13 },
            { 10, 11, 14, 15 },
        };
        return fineIndexTable[yy][xx];
    }

    private static int swap_word_pair(int pair) {
        return (pair << 16) | (pair >> 16);
    }

    private void buildFlatEntries(int _x, int _y, int _z){
        int cellIndex = dim.cellIndex(_x, _y, _z);
        StackInt lightIndexList = lightIndexLists[cellIndex];
        StackLong coverageList = coverageLists[cellIndex];
        int count = lightIndexList.size();
        assert(count == coverageList.size());

        if (count == 0)
        {
            for (int entryIndex = 0; entryIndex < 64; entryIndex++)
            {
                int yy = entryIndex / 16;
                int xx = entryIndex / 4 % 4;
                int zz = entryIndex % 4;

                int x = _x * 4 + xx;
                int y = _y * 4 + yy;
                int z = _z * 4 + zz;

                int headerIndex = (y*dim.width + x)*dim.depth + z;
//                uint32_t* entry_ptr = (uint32_t*)&pDstBuffer[16 * headerIndex];
//                entry_ptr[0] = 0; // list size: 0

                pDstBuffer[16 * headerIndex + 0] = 0;
                pDstBuffer[16 * headerIndex + 1] = 0;
                pDstBuffer[16 * headerIndex + 2] = 0;
                pDstBuffer[16 * headerIndex + 3] = 0;
            }

            return;
        }

        int[] lightIndexList_ptr = lightIndexList.getData();
//        uint32_t* coverageList_ptr = (uint32_t*)&coverageList[0];
        long[] coverageList_ptr = coverageList.getData();

        for (int entryIndex = 0; entryIndex < 64; entryIndex++)
        {
            int yy = entryIndex / 16;
            int xx = entryIndex / 4 % 4;
            int zz = entryIndex % 4;

            int x = _x * 4 + xx;
            int y = _y * 4 + yy;
            int z = _z * 4 + zz;

            int headerIndex = (y*dim.width + x)*dim.depth + z;
//            uint32_t* entry_ptr = (uint32_t*)&pDstBuffer[16 * headerIndex];
//            uint16_t* tail_ptr = (uint16_t*)&pDstBuffer[allocatedBytes];

            int entry_ptr_index = 16 * headerIndex/4;
            int tail_ptr_index = allocatedBytes / 4;

            int fineIndex = getFineIndex(xx, yy) * 4 + zz;
            long mask = 1 << fineIndex;
            int sub_mask = (int)mask;
//            uint32_t* sub_coverageList_ptr = coverageList_ptr;
            boolean sub_coverageList_ptr = false;
            if (sub_mask == 0)
            {
                sub_mask = (int)(mask >> 32);
                sub_coverageList_ptr = true;
            }

            int cursor = 0;
            for (int k = 0; k < count; k++)
            {
//                tail_ptr[cursor] = lightIndexList_ptr[k];
//                Numeric.getBytes((short)lightIndexList_ptr[k], pDstBuffer, tail_ptr_index + cursor * 2);
                int destIndex = tail_ptr_index + cursor/2;
                int originValue = pDstBuffer[destIndex];
                short firstValue = (short)Numeric.decodeFirst(originValue);
                short secondValue = (short)Numeric.decodeSecond(originValue);

                if((cursor%2) == 0){
                    pDstBuffer[destIndex] = Numeric.encode((short)lightIndexList_ptr[k], secondValue);
                }else{
                    pDstBuffer[destIndex] = Numeric.encode(firstValue, (short)lightIndexList_ptr[k]);
                }

//                cursor += !!(sub_coverageList_ptr[k * 2] & sub_mask);
                int step;
                if(sub_coverageList_ptr){
                    step = (Numeric.decodeSecond(coverageList_ptr[k]) & sub_mask) != 0 ? 1 : 0;
                }else{
                    step = (Numeric.decodeFirst(coverageList_ptr[k]) & sub_mask) != 0 ? 1 : 0;
                }

                cursor += step;
            }

            /*entry_ptr[1] = swap_word_pair(*(uint32_t*)&tail_ptr[cursor - 2]);
            entry_ptr[2] = swap_word_pair(*(uint32_t*)&tail_ptr[cursor - 4]);
            entry_ptr[3] = swap_word_pair(*(uint32_t*)&tail_ptr[cursor - 6]);*/

            /*int entry_ptr1 = swap_word_pair(Numeric.getInt(pDstBuffer, tail_ptr_index + (cursor - 2) * 2));
            int entry_ptr2 = swap_word_pair(Numeric.getInt(pDstBuffer, tail_ptr_index + (cursor - 4) * 2));
            int entry_ptr3 = swap_word_pair(Numeric.getInt(pDstBuffer, tail_ptr_index + (cursor - 6) * 2));

            Numeric.getBytes(entry_ptr1, pDstBuffer, entry_ptr_index + 1 * 4);
            Numeric.getBytes(entry_ptr2, pDstBuffer, entry_ptr_index + 2 * 4);
            Numeric.getBytes(entry_ptr3, pDstBuffer, entry_ptr_index + 3 * 4);*/

            pDstBuffer[entry_ptr_index + 1] = swap_word_pair(getValue(tail_ptr_index, cursor - 2));
            pDstBuffer[entry_ptr_index + 2]= swap_word_pair(getValue(tail_ptr_index, cursor - 4));
            pDstBuffer[entry_ptr_index + 3] = swap_word_pair(getValue(tail_ptr_index, cursor - 6));

            int list_size = cursor;
            assert(list_size < 0x100);
            assert(allocatedBytes / 16 < 0x1000000);
//            entry_ptr[0] = (((allocatedBytes) / 16) << 8) | list_size;
//            Numeric.getBytes((((allocatedBytes) / 16) << 8) | list_size, pDstBuffer, entry_ptr_index);
            pDstBuffer[entry_ptr_index+0] = (((allocatedBytes) / 16) << 8) | list_size;

            allocatedBytes += (Math.max(0, list_size - 6) + 7) / 8 * 16;
        }
    }

    private int getValue(int index, int cursor){
        int destIndex = index + cursor/2;
        int originValue = pDstBuffer[destIndex];

        if((cursor%2) == 0){
            return originValue;
        }else{
            if(cursor >=0){
                short secondValue = (short)Numeric.decodeSecond(originValue);
                if(destIndex == pDstBuffer.length - 1){
                    return secondValue;
                }else{
                    short firstValue = (short)Numeric.decodeSecond(pDstBuffer[destIndex + 1]);
                    return Numeric.encode(secondValue, firstValue);
                }
            }else{  // cursor  < 0
                originValue = pDstBuffer[destIndex - 1];
                short secondValue = (short)Numeric.decodeSecond(originValue);
                if(destIndex == 0){
                    throw new IllegalStateException("Inner error");
                }else{
                    short firstValue = (short)Numeric.decodeSecond(pDstBuffer[destIndex]);
                    return Numeric.encode(secondValue, firstValue);
                }
            }
        }
    }

    private int cellCount(){
        assert(lightIndexLists.length == coverageLists.length);
        return lightIndexLists.length;
    }
}
