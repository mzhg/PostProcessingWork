package nv.visualFX.cloth.libs;

import com.nvidia.developer.opengl.utils.BoundingBox;

import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.ReadableVector4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import jdk.internal.org.objectweb.asm.tree.MultiANewArrayInsnNode;

/**
 * Created by mazhen'gui on 2017/9/16.
 */

public class SwInterCollision {
    private final Vector4f mParticle = new Vector4f();
    private final Vector4f mImpulse = new Vector4f();

    private final Vector4f mCollisionDistance = new Vector4f();
    private final Vector4f mCollisionSquareDistance = new Vector4f();
    private final Vector4f mStiffness = new Vector4f();

    private short mClothIndex;
    private int mClothMask;
    private int mParticleIndex;

    private int mNumIterations;

    private SwInterCollisionData[] mInstances;
    private int mNumInstances;

    private short[] mClothIndices;
    private int[] mParticleIndices;
    private int mNumParticles;
    private int[] mOverlapMasks;

    private int mTotalParticles;

    private InterCollisionFilter mFilter;

//    SwKernelAllocator& mAllocator;

    public int mNumTests;
    public int mNumCollisions;

    public SwInterCollision(SwInterCollisionData[] cloths, int n, float colDist, float stiffness,
                     int iterations, InterCollisionFilter filter){
        mInstances =cloths;
        mNumInstances = n;
        mFilter = filter;

        mCollisionDistance.set(colDist, colDist, colDist, 0.0f);
//        mCollisionSquareDistance = mCollisionDistance * mCollisionDistance;
        Vector4f.scale(mCollisionDistance, mCollisionDistance, mCollisionSquareDistance);
        mStiffness.set(stiffness,stiffness,stiffness,stiffness);
        mNumIterations = iterations;

        // calculate particle size
        for (int i = 0; i < n; ++i)
            mTotalParticles += mInstances[i].mNumParticles;
    }

    static void/*<Simd4f>*/ expandBounds(BoundingBox a, BoundingBox b, BoundingBox result)
    {
//        BoundingBox<Simd4f> result;
//        result.mLower = min(a.mLower, b.mLower);
//        result.mUpper = max(a.mUpper, b.mUpper);
//        return result;
        Vector3f.min(a._min, b._min, result._min);
        Vector3f.max(a._max, b._max, result._max);
    }

    static int longestAxis(ReadableVector3f edgeLength)
    {
//	const float* e = array(edgeLength);
        ReadableVector3f e =edgeLength;

        if (e.get(0) > e.get(1))
            return (e.get(0) > e.get(2) ? 0 : 2);
        else
            return (e.get(1) > e.get(2) ? 1 : 2);
    }

    private static final class ClothSorter implements PsSort.Predicate
    {

        ClothSorter(BoundingBox[] bounds, int axis)// : mBounds(bounds), mNumBounds(n), mAxis(axis)
        {
            mBounds = bounds;
            mAxis = axis;
        }

        public boolean compare(int i, int j)
        {
//            NV_CLOTH_ASSERT(i < mNumBounds);
//            NV_CLOTH_ASSERT(j < mNumBounds);

//            return array(mBounds[i].mLower)[mAxis] < array(mBounds[j].mLower)[mAxis];
            return mBounds[i]._min.get(mAxis) < mBounds[j]._min.get(mAxis);
        }

        BoundingBox[] mBounds;
        int mAxis;

    };

    // for the given cloth array this function calculates the set of particles
// which potentially interact, the potential colliders are returned with their
// cloth index and particle index in clothIndices and particleIndices, the
// function returns the number of potential colliders
    private static int calculatePotentialColliders(/*const cloth::SwInterCollisionData* cBegin, const cloth::SwInterCollisionData* cEnd,*/SwInterCollisionData[] datas,
                                     ReadableVector4f colDist, short[] clothIndices, int[] particleIndices,
                                         BoundingBox bounds, int[] overlapMasks,
                                         InterCollisionFilter filter/*, cloth::SwKernelAllocator& allocator*/)
    {
//        using namespace cloth;
//
//        typedef BoundingBox<T4f> BoundingBox;

        int numParticles = 0;
	    final int numCloths = /*uint32_t(cEnd - cBegin)*/datas.length;

        // bounds of each cloth objects in world space
//        BoundingBox* const clothBounds = static_cast<BoundingBox*>(allocator.allocate(numCloths * sizeof(BoundingBox)));
//        BoundingBox* const overlapBounds = static_cast<BoundingBox*>(allocator.allocate(numCloths * sizeof(BoundingBox)));
        BoundingBox[] clothBounds = new BoundingBox[numCloths];
        BoundingBox[] overlapBounds = new BoundingBox[numCloths];
        for(int i = 0; i < numCloths; i++){
            clothBounds[i] = new BoundingBox();
            overlapBounds[i] = new BoundingBox();
        }

        // union of all cloth world bounds
        BoundingBox totalClothBounds = new BoundingBox();

//        uint32_t* sortedIndices = static_cast<uint32_t*>(allocator.allocate(numCloths * sizeof(uint32_t)));
        int[] sortedIndices = new int[numCloths];
        BoundingBox lcBounds = new BoundingBox();
        for (int i = 0; i < numCloths; ++i)
        {
		    SwInterCollisionData c = datas[i];

            // transform bounds from b local space to local space of a
//            PxBounds3 lcBounds = PxBounds3::centerExtents(c.mBoundsCenter, c.mBoundsHalfExtent + PxVec3(array(colDist)[0]));
            lcBounds._max.x = c.mBoundsCenter.x + c.mBoundsHalfExtent.x + colDist.getX();  // TODO
            lcBounds._max.y = c.mBoundsCenter.y + c.mBoundsHalfExtent.y + colDist.getX();
            lcBounds._max.z = c.mBoundsCenter.z + c.mBoundsHalfExtent.z + colDist.getX();
            lcBounds._min.x = c.mBoundsCenter.x - c.mBoundsHalfExtent.x + colDist.getX();
            lcBounds._min.y = c.mBoundsCenter.y - c.mBoundsHalfExtent.y + colDist.getX();
            lcBounds._min.z = c.mBoundsCenter.z - c.mBoundsHalfExtent.z + colDist.getX();

            assert (lcBounds.valid());
            /*PxBounds3 cWorld = PxBounds3::transformFast(c.mGlobalPose,lcBounds);
            BoundingBox cBounds = { simd4f(cWorld.minimum.x, cWorld.minimum.y, cWorld.minimum.z, 0.0f),
                    simd4f(cWorld.maximum.x, cWorld.maximum.y, cWorld.maximum.z, 0.0f) };
            clothBounds[i] = cBounds;*/
            BoundingBox cBounds = clothBounds[i];
            PxTransform.transform(c.mGlobalPose, lcBounds, cBounds);

            sortedIndices[i] = i;

            expandBounds(totalClothBounds, cBounds, totalClothBounds);
        }

        // sort indices by their minimum extent on the longest axis
	    final int sweepAxis = longestAxis(Vector3f.sub(totalClothBounds._max, totalClothBounds._min, null));

//        ClothSorter<T4f> predicate(clothBounds, numCloths, sweepAxis);
//        shdfnd::sort(sortedIndices, numCloths, predicate, nv::cloth::NonTrackingAllocator());
        ClothSorter predicate = new ClothSorter(clothBounds, sweepAxis);
        PsSort.sort(sortedIndices, predicate, 24);

        for (uint32_t i = 0; i < numCloths; ++i)
        {
            NV_CLOTH_ASSERT(sortedIndices[i] < numCloths);

		const SwInterCollisionData& a = cBegin[sortedIndices[i]];

            // local bounds
		const T4f aCenter = load(reinterpret_cast<const float*>(&a.mBoundsCenter));
		const T4f aHalfExtent = load(reinterpret_cast<const float*>(&a.mBoundsHalfExtent)) + colDist;
		const BoundingBox aBounds = { aCenter - aHalfExtent, aCenter + aHalfExtent };

		const PxMat44 aToWorld = PxMat44(a.mGlobalPose);
		const PxTransform aToLocal = a.mGlobalPose.getInverse();

		const float axisMin = array(clothBounds[sortedIndices[i]].mLower)[sweepAxis];
		const float axisMax = array(clothBounds[sortedIndices[i]].mUpper)[sweepAxis];

            uint32_t overlapMask = 0;
            uint32_t numOverlaps = 0;

            // scan back to find first intersecting bounding box
            uint32_t startIndex = i;
            while (startIndex > 0 && array(clothBounds[sortedIndices[startIndex]].mUpper)[sweepAxis] > axisMin)
                --startIndex;

            // compute all overlapping bounds
            for (uint32_t j = startIndex; j < numCloths; ++j)
            {
                // ignore self-collision
                if (i == j)
                    continue;

                // early out if no more cloths along axis intersect us
                if (array(clothBounds[sortedIndices[j]].mLower)[sweepAxis] > axisMax)
                    break;

			const SwInterCollisionData& b = cBegin[sortedIndices[j]];

                // check if collision between these shapes is filtered
                if (!filter(a.mUserData, b.mUserData))
                    continue;

                // set mask bit for this cloth
                overlapMask |= 1 << sortedIndices[j];

                // transform bounds from b local space to local space of a
                PxBounds3 lcBounds = PxBounds3::centerExtents(b.mBoundsCenter, b.mBoundsHalfExtent + PxVec3(array(colDist)[0]));
                NV_CLOTH_ASSERT(!lcBounds.isEmpty());
                PxBounds3 bLocal = PxBounds3::transformFast(aToLocal * b.mGlobalPose,lcBounds);

                BoundingBox bBounds = { simd4f(bLocal.minimum.x, bLocal.minimum.y, bLocal.minimum.z, 0.0f),
                        simd4f(bLocal.maximum.x, bLocal.maximum.y, bLocal.maximum.z, 0.0f) };

                BoundingBox iBounds = intersectBounds(aBounds, bBounds);

                // setup bounding box w to make point containment test cheaper
                T4f floatMax = gSimd4fFloatMax & static_cast<T4f>(sMaskW);
                iBounds.mLower = (iBounds.mLower & sMaskXYZ) | -floatMax;
                iBounds.mUpper = (iBounds.mUpper & sMaskXYZ) | floatMax;

                if (!isEmptyBounds(iBounds))
                    overlapBounds[numOverlaps++] = iBounds;
            }

            //----------------------------------------------------------------
            // cull all particles to overlapping bounds and transform particles to world space

		const uint32_t clothIndex = sortedIndices[i];
            overlapMasks[clothIndex] = overlapMask;

            T4f* pBegin = reinterpret_cast<T4f*>(a.mParticles);
            T4f* qBegin = reinterpret_cast<T4f*>(a.mPrevParticles);

		const T4f xform[4] = {    load(reinterpret_cast<const float*>(&aToWorld.column0)),
            load(reinterpret_cast<const float*>(&aToWorld.column1)),
            load(reinterpret_cast<const float*>(&aToWorld.column2)),
            load(reinterpret_cast<const float*>(&aToWorld.column3)) };

            T4f impulseInvScale = recip(T4f(simd4f(cBegin[clothIndex].mImpulseScale)));

            for (uint32_t k = 0; k < a.mNumParticles; ++k)
            {
                T4f* pIt = a.mIndices ? pBegin + a.mIndices[k] : pBegin + k;
                T4f* qIt = a.mIndices ? qBegin + a.mIndices[k] : qBegin + k;

			const T4f p = *pIt;

                for (const BoundingBox* oIt = overlapBounds, *oEnd = overlapBounds + numOverlaps; oIt != oEnd; ++oIt)
                {
                    // point in box test
                    if (anyGreater(oIt->mLower, p) != 0)
                        continue;
                    if (anyGreater(p, oIt->mUpper) != 0)
                        continue;

                    // transform particle to world space in-place
                    // (will be transformed back after collision)
				*pIt = transform(xform, p);

                    T4f impulse = (p - *qIt) * impulseInvScale;
				*qIt = rotate(xform, impulse);

                    // update world bounds
                    bounds = expandBounds(bounds, pIt, pIt + 1);

                    // add particle to output arrays
                    clothIndices[numParticles] = uint16_t(clothIndex);
                    particleIndices[numParticles] = uint32_t(pIt - pBegin);

                    // output each particle only once
                    ++numParticles;
                    break;
                }
            }
        }

        allocator.deallocate(sortedIndices);
        allocator.deallocate(overlapBounds);
        allocator.deallocate(clothBounds);

        return numParticles;
    }

    public void invoke(){
        mNumTests = mNumCollisions = 0;

        mClothIndices = new short[mTotalParticles];
        mParticleIndices = new int[mTotalParticles];
        mOverlapMasks = new int[mNumInstances];

        BoundingBox bounds = new BoundingBox();
        for (int k = 0; k < mNumIterations; ++k)
        {
            // world bounds of particles
//            BoundingBox<T4f> bounds = emptyBounds<T4f>();

            // calculate potentially colliding set
            {
//                NV_CLOTH_PROFILE_ZONE("cloth::SwInterCollision::BroadPhase", /*ProfileContext::None*/ 0);

                mNumParticles =
                        calculatePotentialColliders(mInstances, mInstances + mNumInstances, mCollisionDistance, mClothIndices,
                                mParticleIndices, bounds, mOverlapMasks, mFilter, mAllocator);
            }

            // collide
            if (mNumParticles)
            {
                NV_CLOTH_PROFILE_ZONE("cloth::SwInterCollision::Collide", /*ProfileContext::None*/ 0);

                T4f lowerBound = bounds.mLower;
                T4f edgeLength = max(bounds.mUpper - lowerBound, sEpsilon);

                // sweep along longest axis
                uint32_t sweepAxis = longestAxis(edgeLength);
                uint32_t hashAxis0 = (sweepAxis + 1) % 3;
                uint32_t hashAxis1 = (sweepAxis + 2) % 3;

                // reserve 0, 127, and 65535 for sentinel
                T4f cellSize = max(mCollisionDistance, simd4f(1.0f / 253) * edgeLength);
                array(cellSize)[sweepAxis] = array(edgeLength)[sweepAxis] / 65533;

                T4f one = gSimd4fOne;
                T4f gridSize = simd4f(254.0f);
                array(gridSize)[sweepAxis] = 65534.0f;

                T4f gridScale = recip<1>(cellSize);
                T4f gridBias = -lowerBound * gridScale + one;

			void* buffer = mAllocator.allocate(getBufferSize(mNumParticles));

                uint32_t* __restrict sortedIndices = reinterpret_cast<uint32_t*>(buffer);
                uint32_t* __restrict sortedKeys = sortedIndices + mNumParticles;
                uint32_t* __restrict keys = std::max(sortedKeys + mNumParticles, sortedIndices + 2 * mNumParticles + 1024);

                typedef typename Simd4fToSimd4i<T4f>::Type Simd4i;

                // create keys
                for (uint32_t i = 0; i < mNumParticles; ++i)
                {
                    // grid coordinate
                    T4f indexf = getParticle(i) * gridScale + gridBias;

                    // need to clamp index because shape collision potentially
                    // pushes particles outside of their original bounds
                    Simd4i indexi = intFloor(max(one, min(indexf, gridSize)));

				const int32_t* ptr = array(indexi);
                    keys[i] = uint32_t(ptr[sweepAxis] | (ptr[hashAxis0] << 16) | (ptr[hashAxis1] << 24));
                }

                // compute sorted keys indices
                radixSort(keys, keys + mNumParticles, sortedIndices);

                // snoop histogram: offset of first index with 8 msb > 1 (0 is sentinel)
                uint32_t firstColumnSize = sortedIndices[2 * mNumParticles + 769];

                // sort keys
                for (uint32_t i = 0; i < mNumParticles; ++i)
                    sortedKeys[i] = keys[sortedIndices[i]];
                sortedKeys[mNumParticles] = uint32_t(-1); // sentinel

                // calculate the number of buckets we need to search forward
			const Simd4i data = intFloor(gridScale * mCollisionDistance);
                uint32_t collisionDistance = uint32_t(2 + array(data)[sweepAxis]);

                // collide particles
                collideParticles(sortedKeys, firstColumnSize, sortedIndices, mNumParticles, collisionDistance);

                mAllocator.deallocate(buffer);
            }

		/*
		// verify against brute force (disable collision response when testing)
		uint32_t numCollisions = mNumCollisions;
		mNumCollisions = 0;

		for (uint32_t i = 0; i < mNumParticles; ++i)
		    for (uint32_t j = i + 1; j < mNumParticles; ++j)
		        if (mOverlapMasks[mClothIndices[i]] & (1 << mClothIndices[j]))
		            collideParticles(getParticle(i), getParticle(j));

		static uint32_t iter = 0; ++iter;
		if (numCollisions != mNumCollisions)
		    printf("%u: %u != %u\n", iter, numCollisions, mNumCollisions);
		*/

            // transform back to local space
            {
                NV_CLOTH_PROFILE_ZONE("cloth::SwInterCollision::PostTransform", /*ProfileContext::None*/ 0);

                T4f toLocal[4], impulseScale;
                uint16_t lastCloth = uint16_t(0xffff);

                for (uint32_t i = 0; i < mNumParticles; ++i)
                {
                    uint16_t clothIndex = mClothIndices[i];
				const SwInterCollisionData* instance = mInstances + clothIndex;

                    // todo: could pre-compute these inverses
                    if (clothIndex != lastCloth)
                    {
					const PxMat44 xform = PxMat44(instance->mGlobalPose.getInverse());

                        toLocal[0] = load(reinterpret_cast<const float*>(&xform.column0));
                        toLocal[1] = load(reinterpret_cast<const float*>(&xform.column1));
                        toLocal[2] = load(reinterpret_cast<const float*>(&xform.column2));
                        toLocal[3] = load(reinterpret_cast<const float*>(&xform.column3));

                        impulseScale = simd4f(instance->mImpulseScale);

                        lastCloth = mClothIndices[i];
                    }

                    uint32_t particleIndex = mParticleIndices[i];
                    T4f& particle = reinterpret_cast<T4f&>(instance->mParticles[particleIndex]);
                    T4f& impulse = reinterpret_cast<T4f&>(instance->mPrevParticles[particleIndex]);

                    particle = transform(toLocal, particle);
                    // avoid w becoming negative due to numerical inaccuracies
                    impulse = max(sZeroW, particle - rotate(toLocal, T4f(impulse * impulseScale)));
                }
            }
        }

        mAllocator.deallocate(mOverlapMasks);
        mAllocator.deallocate(mParticleIndices);
        mAllocator.deallocate(mClothIndices);
    }

    static size_t estimateTemporaryMemory(SwInterCollisionData* cloths, uint32_t n);

    private static int getBufferSize(int aaa);

    private void collideParticles(int[] keys, int firstColumnSize, int[] sortedIndices,
                                  int numParticles, int collisionDistance);

    private ReadableVector4f getParticle(int index);

    // better wrap these in a struct
    private void collideParticle(int index);
}
