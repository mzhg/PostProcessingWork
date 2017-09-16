package nv.visualFX.cloth.libs;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jet.opengl.postprocessing.util.BufferUtils;
import jet.opengl.postprocessing.util.Numeric;
import jet.opengl.postprocessing.util.StackInt;

/**
 * Created by mazhen'gui on 2017/9/14.
 */

public class TripletScheduler {
    public IntBuffer mTriplets;
    public StackInt mSetSizes = new StackInt();

    public TripletScheduler(IntBuffer triplets){
        mTriplets = triplets;
    }

    public void simd(int numParticles, int simdWidth){
        if (mTriplets == null || mTriplets.remaining() == 0)
            return;

//        Vector<uint32_t>::Type mark(numParticles, uint32_t(-1));
        int[] mark = new int[numParticles];
        Arrays.fill(mark, -1);
        int[] tmp0 = new int[4];
        int[] tmp1 = new int[4];

        int setIndex = 0, setSize = 0;
//        for (TripletIter tIt = mTriplets.begin(), tEnd = mTriplets.end(); tIt != tEnd; ++setIndex)
        int tEnd = mTriplets.limit()/4;
        for (int tIt = mTriplets.position()/4; tIt != tEnd;++setIndex )
        {
            int tLast = tIt + Math.min(simdWidth, tEnd - tIt);
            int tSwap = tEnd;

            for (; tIt != tLast && tIt != tSwap; ++tIt, ++setSize)
            {
                final int tIt_x = mTriplets.get(tIt*4+0);
                final int tIt_y = mTriplets.get(tIt*4+1);
                final int tIt_z = mTriplets.get(tIt*4+2);
                // swap from tail until independent triplet found
                while ((mark[tIt_x] == setIndex || mark[tIt_y] == setIndex || mark[tIt_z] == setIndex) && tIt != --tSwap) {
//                    std::iter_swap (tIt, tSwap);
                    for(int i = 0; i < 4; i++){
                        tmp0[i] = mTriplets.get(4*tIt+i);
                        tmp1[i] = mTriplets.get(4*tSwap+i);
                    }

                    for(int i = 0; i < 4; i++){
                        mTriplets.put(4*tIt+i, tmp1[i]);
                        mTriplets.put(4*tSwap+i, tmp0[i]);
                    }

                }

                if (tIt == tSwap)
                    break; // no independent triplet found

                // mark vertices to be used in simdIndex
                mark[tIt_x] = setIndex;
                mark[tIt_y] = setIndex;
                mark[tIt_z] = setIndex;
            }

            if (tIt == tSwap) // remaining triplets depend on current set
            {
                if (setSize > simdWidth) // trim set to multiple of simdWidth
                {
                    int overflow = setSize % simdWidth;
                    setSize -= overflow;
                    tIt -= overflow;
                }
                mSetSizes.push(setSize);
                setSize = 0;
            }
        }
    }

    public void warp(int numParticles, int warpWidth){
        // NV_CLOTH_ASSERT(warpWidth == 32 || warpWidth == 16);

        if (mTriplets == null || mTriplets.remaining() == 0)
            return;

        int tIt, tEnd = mTriplets.limit()/4;
        int tripletIndex;

        // count number of triplets per particle
//        Vector<uint32_t>::Type adjacentCount(numParticles + 1, uint32_t(0));
        int[] adjacentCount = new int[numParticles + 1];
        for (tIt = mTriplets.position()/4; tIt != tEnd; ++tIt)
            for (int i = 0; i < 3; ++i) {
//                ++adjacentCount[( * tIt)[i]];
                int idx = mTriplets.get(tIt * 4 + i);
                ++adjacentCount[idx];
            }

	/* neither of those were really improving number of batches:
	// run simd version to pre-sort particles
	simd(numParticles, blockWidth); mSetSizes.resize(0);
	// sort according to triplet degree (estimated by sum of adjacentCount)
	std::sort(mTriplets.begin(), tEnd, GreaterSum(adjacentCount));
	*/

//        uint32_t maxTripletCount = *shdfnd::maxElement(adjacentCount.begin(), adjacentCount.end());
        int maxTripletCount = -1;
        for(int i = 0; i < adjacentCount.length; i++){
            maxTripletCount = Math.max(maxTripletCount, adjacentCount[i]);
        }

        // compute in place prefix sum (inclusive)
        prefixSum(/*adjacentCount.begin()*/0, adjacentCount.length, /*adjacentCount.begin()*/0, adjacentCount);

        // initialize adjacencies (for each particle, collect touching triplets)
        // also converts partial sum in adjacentCount from inclusive to exclusive
//        Vector<uint32_t>::Type adjacencies(adjacentCount.back());
        int[] adjacencies = new int[adjacentCount[adjacentCount.length - 1]];
        for (tIt = mTriplets.position()/4, tripletIndex = 0; tIt != tEnd; ++tIt, ++tripletIndex)
            for (int i = 0; i < 3; ++i) {
//                adjacencies[--adjacentCount[( * tIt)[i]]] =tripletIndex;
                int idx = mTriplets.get(tIt * 4 + i);
                adjacencies[--adjacentCount[/*( * tIt)[i]*/idx]] =tripletIndex;
            }

        int warpMask = warpWidth - 1;

        int numSets = maxTripletCount; // start with minimum number of sets
//        Vector<TripletSet>::Type sets(numSets);
//        TripletSet[] sets = new TripletSet[numSets];
        List<TripletSet> sets = new ArrayList<>();
        for(int i = 0; i < numSets; i++)
            sets.add(new TripletSet());

//        Vector<uint32_t>::Type setIndices(mTriplets.size(), uint32_t(-1));
        int[] setIndices = new int[mTriplets.remaining()/4];
        Arrays.fill(setIndices, -1);
        mSetSizes.resize(numSets);

        // color triplets (assign to sets)
//        Vector<uint32_t>::Type::ConstIterator aBegin = adjacencies.begin(), aIt, aEnd;
        int aBegin = 0, aIt, aEnd;
        for (tIt = mTriplets.position()/4, tripletIndex = 0; tIt != tEnd; ++tIt, ++tripletIndex)
        {
            // mark sets of adjacent triplets
            for (int i = 0; i < 3; ++i)
            {
                int particleIndex = /*(*tIt)[i]*/mTriplets.get(tIt * 4 + i);
                aIt = aBegin + adjacentCount[particleIndex];
                aEnd = aBegin + adjacentCount[particleIndex + 1];
                for (int setIndex; aIt != aEnd; ++aIt)
                    if (numSets > (setIndex = setIndices[adjacencies[aIt]]))
                sets.get(setIndex).mMark = tripletIndex;
            }

            // find valid set with smallest number of bank conflicts
            int bestIndex = numSets;
            int minReplays = 4;
            for (int setIndex = 0; setIndex < numSets && minReplays != 0; ++setIndex)
            {
			    TripletSet set = sets.get(setIndex);

                if (set.mMark == tripletIndex)
                    continue; // triplet collision

                int numReplays = 0;
                for (int i = 0; i < 3; ++i) {
                    int idx = mTriplets.get(tIt * 4 + i);
                    numReplays += (set.mNumReplays[i] == set.mNumConflicts[i][warpMask & /*( * tIt)[i]*/idx]) ? 1: 0;
                }

                if (minReplays > numReplays)
                {
                    minReplays = numReplays;
                    bestIndex = setIndex;
                }
            }

            // add new set if none found
            if (bestIndex == numSets)
            {
                sets.add(new TripletSet());
                mSetSizes.push(0);
                ++numSets;
            }

            // increment bank conflicts or reset if warp filled
            TripletSet set = sets.get(bestIndex);
            if ((/*++mSetSizes[bestIndex]*/mSetSizes.incrLeft(bestIndex) & warpMask) != 0)
                for (int i = 0; i < 3; ++i) {
                    int idx = mTriplets.get(tIt * 4 + i);
                    set.mNumReplays[i] = (byte) Math.max(Numeric.unsignedByte(set.mNumReplays[i]),
                            Numeric.unsignedByte(++set.mNumConflicts[i][(warpMask & /*( * tIt)[i]*/idx)]));  // TODO unsafe
                }
            else {
//                set = new TripletSet();
            }

            setIndices[tripletIndex] = bestIndex;
        }

        // reorder triplets
//        Vector<uint32_t>::Type setOffsets(mSetSizes.size());
        int[] setOffsets = new int[mSetSizes.size()];
        prefixSum(0, mSetSizes.size(), 0, setOffsets);  // TODO error

//        Vector<Vec4u>::Type triplets(mTriplets.size());
//        Vector<uint32_t>::Type::ConstIterator iIt = setIndices.begin();
        IntBuffer triplets = BufferUtils.createIntBuffer(mTriplets.remaining() * 4);
        int iIt = 0;
        for (tIt = mTriplets.position()/4, tripletIndex = 0; tIt != tEnd; ++tIt, ++iIt) {
//            triplets[--setOffsets[ * iIt]] = *tIt;
            int tIt0 = mTriplets.get(tIt*4+0);
            int tIt1 = mTriplets.get(tIt*4+1);
            int tIt2 = mTriplets.get(tIt*4+2);
            int tIt3 = mTriplets.get(tIt*4+3);

            int idx = --setOffsets[setIndices[iIt]];

            triplets.put(idx*4+0, tIt0);
            triplets.put(idx*4+1, tIt1);
            triplets.put(idx*4+2, tIt2);
            triplets.put(idx*4+3, tIt3);
        }

        mTriplets = triplets;
    }

    void prefixSum(int first, int last, int dest, int[] a)
    {
        if (first == last)
            return;
        else
        {
            a[dest++] = a[first++];

            for (; first != last; ++first, ++dest)
                a[dest] = a[dest - 1] + a[first];
        }
    }


}
