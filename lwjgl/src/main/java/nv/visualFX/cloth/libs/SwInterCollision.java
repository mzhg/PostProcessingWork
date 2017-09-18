package nv.visualFX.cloth.libs;

import com.nvidia.developer.opengl.utils.BoundingBox;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.ReadableVector4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.nio.FloatBuffer;
import java.util.Arrays;

import jet.opengl.postprocessing.util.Numeric;

/**
 * Created by mazhen'gui on 2017/9/16.
 */

public class SwInterCollision {
    private final Vector4f mParticle = new Vector4f();
    private final Vector4f mImpulse = new Vector4f();

    private final Vector4f mCollisionDistance = new Vector4f();
    private final Vector4f mCollisionSquareDistance = new Vector4f();
    private final Vector4f mStiffness = new Vector4f();
    private final Vector4f mTemp = new Vector4f();

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

    private static boolean anyGreater(ReadableVector3f a, ReadableVector3f b){
        return a.getX() > b.getX() || a.getY() > b.getY() || a.getZ() > b.getZ();
    }

    // returns sorted indices, output needs to be at least 2*(last - first) + 1024
//    static void radixSort(const uint32_t* first, const uint32_t* last, uint32_t* out)
    static void radixSort(int[] values, int first, int last, int out)
    {
        int n = (last - first);

//        uint32_t* buffer = out + 2 * n;
//        uint32_t* __restrict histograms[] = { buffer, buffer + 256, buffer + 512, buffer + 768 };
        int buffer = out + 2 * n;
        int histograms[] = { buffer, buffer + 256, buffer + 512, buffer + 768 };

//        memset(buffer, 0, 1024 * sizeof(uint32_t));
        Arrays.fill(values, buffer, buffer + 1024, 0);  // TODO

        // build 3 histograms in one pass
        for (int it = first; it != last; ++it)
        {
            int key = values[it];
//            ++histograms[0][0xff & key];
//            ++histograms[1][0xff & (key >> 8)];
//            ++histograms[2][0xff & (key >> 16)];
//            ++histograms[3][key >> 24];
            ++values[histograms[0] + (0xff & key)];
            ++values[histograms[1] + (0xff & (key >> 8))];
            ++values[histograms[2] + (0xff & (key >> 16))];
            ++values[histograms[3] + (key >> 24)];
        }

        // convert histograms to offset tables in-place
        int sums[] = {0,0,0,0};
        for (int i = 0; i < 256; ++i)
        {
            int temp0 = /*histograms[0][i]*/values[histograms[0]+i] + sums[0];
            /*histograms[0][i]*/values[histograms[0]+i] = sums[0]; sums[0] = temp0;

            int temp1 = /*histograms[1][i]*/values[histograms[1]+i] + sums[1];
            /*histograms[1][i]*/values[histograms[0]+i] = sums[1]; sums[1] = temp1;

            int temp2 = /*histograms[2][i]*/values[histograms[2]+i] + sums[2];
            /*histograms[2][i]*/values[histograms[2]+i] = sums[2]; sums[2] = temp2;

            int temp3 = /*histograms[3][i]*/values[histograms[3]+i] + sums[3];
            /*histograms[3][i]*/values[histograms[3]+i] = sums[3]; sums[3] = temp3;
        }

        assert (sums[0] == n && sums[1] == n && sums[2] == n && sums[3] == n);

//#if PX_DEBUG
//        memset(out, 0xff, 2 * n * sizeof(uint32_t));
//#endif

        // sort 8 bits per pass
//        uint32_t* __restrict indices[] = { out, out + n };
        int[] indices = {out, out + n };

        for (int i = 0; i != n; ++i) {
//            indices[1][histograms[0][0xff & values[first + i]]++] = i;
            int offset = values[histograms[0] + 0xff & values[first + i]];
            values[indices[1] + offset] = i;
            values[offset]++;
        }

        for (int i = 0, index; i != n; ++i)
        {
            index = /*indices[1][i]*/values[indices[1]+i];
//            indices[0][histograms[1][0xff & (first[index] >> 8)]++] = index;
            int offset = values[histograms[1] + (0xff & (values[first + index] >> 8))];
            values[indices[0] + offset] = index;
            values[offset]++;
        }

        for (int i = 0, index; i != n; ++i)
        {
            index = /*indices[0][i]*/values[indices[0]+i];
//            indices[1][histograms[2][0xff & (first[index] >> 16)]++] = index;
            int offset = values[histograms[2] + (0xff & (values[first + index] >> 16))];
            values[indices[1] + offset] = index;
            values[offset]++;
        }

        for (int i = 0, index; i != n; ++i)
        {
            index = /*indices[1][i]*/values[indices[1]+i];
//            indices[0][histograms[3][first[index] >> 24]++] = index;
            int offset = values[histograms[3] + (values[first + index] >> 24)];
            values[indices[0] + offset] = index;
            values[offset]++;
        }
    }

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
            lcBounds._min.x = c.mBoundsCenter.x - c.mBoundsHalfExtent.x - colDist.getX();
            lcBounds._min.y = c.mBoundsCenter.y - c.mBoundsHalfExtent.y - colDist.getX();
            lcBounds._min.z = c.mBoundsCenter.z - c.mBoundsHalfExtent.z - colDist.getX();

            assert (lcBounds.valid());
            /*PxBounds3 cWorld = PxBounds3::transformFast(c.mGlobalPose,lcBounds);
            BoundingBox cBounds = { simd4f(cWorld.minimum.x, cWorld.minimum.y, cWorld.minimum.z, 0.0f),
                    simd4f(cWorld.maximum.x, cWorld.maximum.y, cWorld.maximum.z, 0.0f) };
            clothBounds[i] = cBounds;*/
            BoundingBox cBounds = clothBounds[i];
            PxTransform.transform(c.mGlobalPose, lcBounds, cBounds);

            sortedIndices[i] = i;

//            expandBounds(totalClothBounds, cBounds, totalClothBounds);
            totalClothBounds.expandBy(cBounds);
        }

        // sort indices by their minimum extent on the longest axis
	    final int sweepAxis = longestAxis(Vector3f.sub(totalClothBounds._max, totalClothBounds._min, null));

//        ClothSorter<T4f> predicate(clothBounds, numCloths, sweepAxis);
//        shdfnd::sort(sortedIndices, numCloths, predicate, nv::cloth::NonTrackingAllocator());
        ClothSorter predicate = new ClothSorter(clothBounds, sweepAxis);
        PsSort.sort(sortedIndices, predicate, 24);

        final Vector3f aHalfExtent = new Vector3f();
        final BoundingBox aBounds =new BoundingBox();
        final BoundingBox bBounds =new BoundingBox();
        final BoundingBox iBounds =new BoundingBox();
        final PxTransform aToLocal = new PxTransform();
        final Matrix4f aToWorld = new Matrix4f();
        final Vector4f p4f = new Vector4f();
        final Vector4f q4f = new Vector4f();
        for (int i = 0; i < numCloths; ++i)
        {
            assert (sortedIndices[i] < numCloths);

            SwInterCollisionData a = datas[sortedIndices[i]];

                // local bounds
//            const T4f aCenter = load(reinterpret_cast<const float*>(&a.mBoundsCenter));
//            const T4f aHalfExtent = load(reinterpret_cast<const float*>(&a.mBoundsHalfExtent)) + colDist;
//            const BoundingBox aBounds = { aCenter - aHalfExtent, aCenter + aHalfExtent };
            final ReadableVector3f aCenter = a.mBoundsCenter;
            Vector3f.add(a.mBoundsHalfExtent, colDist, aHalfExtent);
            Vector3f.add(aCenter, aHalfExtent,aBounds._max);
            Vector3f.sub(aCenter, aHalfExtent,aBounds._min);

//            const PxMat44 aToWorld = PxMat44(a.mGlobalPose);
//            const PxTransform aToLocal = a.mGlobalPose.getInverse();
            a.mGlobalPose.toMat(aToWorld);
            PxTransform.invert(a.mGlobalPose, aToLocal);

//            const float axisMin = array(clothBounds[sortedIndices[i]].mLower)[sweepAxis];
//            const float axisMax = array(clothBounds[sortedIndices[i]].mUpper)[sweepAxis];
            final float axisMin = clothBounds[sortedIndices[i]]._min.get(sweepAxis);
            final float axisMax = clothBounds[sortedIndices[i]]._max.get(sweepAxis);

            int overlapMask = 0;
            int numOverlaps = 0;

            // scan back to find first intersecting bounding box
            int startIndex = i;
            while (startIndex > 0 && clothBounds[sortedIndices[startIndex]]._max.get(sweepAxis) > axisMin)
                --startIndex;

            // compute all overlapping bounds
            for (int j = startIndex; j < numCloths; ++j)
            {
                // ignore self-collision
                if (i == j)
                    continue;

                // early out if no more cloths along axis intersect us
                if (clothBounds[sortedIndices[j]]._min.get(sweepAxis) > axisMax)
                    break;

                SwInterCollisionData b = datas[sortedIndices[j]];

                // check if collision between these shapes is filtered
                if (!filter.onCollision(a.mUserData, b.mUserData))
                    continue;

                // set mask bit for this cloth
                overlapMask |= 1 << sortedIndices[j];

                // transform bounds from b local space to local space of a
//                PxBounds3 lcBounds = PxBounds3::centerExtents(b.mBoundsCenter, b.mBoundsHalfExtent + PxVec3(array(colDist)[0]));
                lcBounds._max.x = b.mBoundsCenter.x + b.mBoundsHalfExtent.x + colDist.getX();  // TODO
                lcBounds._max.y = b.mBoundsCenter.y + b.mBoundsHalfExtent.y + colDist.getX();
                lcBounds._max.z = b.mBoundsCenter.z + b.mBoundsHalfExtent.z + colDist.getX();
                lcBounds._min.x = b.mBoundsCenter.x - b.mBoundsHalfExtent.x - colDist.getX();
                lcBounds._min.y = b.mBoundsCenter.y - b.mBoundsHalfExtent.y - colDist.getX();
                lcBounds._min.z = b.mBoundsCenter.z - b.mBoundsHalfExtent.z - colDist.getX();

                assert (lcBounds.valid());
//                PxBounds3 bLocal = PxBounds3::transformFast(aToLocal * b.mGlobalPose,lcBounds);
//                BoundingBox bBounds = { simd4f(bLocal.minimum.x, bLocal.minimum.y, bLocal.minimum.z, 0.0f),
//                        simd4f(bLocal.maximum.x, bLocal.maximum.y, bLocal.maximum.z, 0.0f) };
                PxTransform.transform(b.mGlobalPose, lcBounds, bBounds);
                PxTransform.transform(aToLocal, bBounds, bBounds);   // TODO need valid

//                BoundingBox iBounds = intersectBounds(aBounds, bBounds);
                aBounds.intersect(bBounds, iBounds);
                        // setup bounding box w to make point containment test cheaper
//                T4f floatMax = gSimd4fFloatMax & static_cast<T4f>(sMaskW);
//                iBounds.mLower = (iBounds.mLower & sMaskXYZ) | -floatMax;
//                iBounds.mUpper = (iBounds.mUpper & sMaskXYZ) | floatMax;  TODO

                if (iBounds.valid())
                    overlapBounds[numOverlaps++].set(iBounds);
            }

            //----------------------------------------------------------------
            // cull all particles to overlapping bounds and transform particles to world space

		    final int clothIndex = sortedIndices[i];
            overlapMasks[clothIndex] = overlapMask;

//            T4f* pBegin = reinterpret_cast<T4f*>(a.mParticles);
//            T4f* qBegin = reinterpret_cast<T4f*>(a.mPrevParticles);
            FloatBuffer pBegin = a.mParticles;
            FloatBuffer qBegin = a.mPrevParticles;

//		    const T4f xform[4] = {    load(reinterpret_cast<const float*>(&aToWorld.column0)),
//            load(reinterpret_cast<const float*>(&aToWorld.column1)),
//            load(reinterpret_cast<const float*>(&aToWorld.column2)),
//            load(reinterpret_cast<const float*>(&aToWorld.column3)) };
            final Matrix4f xform = aToWorld;

//            T4f impulseInvScale = recip(T4f(simd4f(datas[clothIndex].mImpulseScale)));
            float impulseInvScale = 1.0f/datas[clothIndex].mImpulseScale;

            for (int k = 0; k < a.mNumParticles; ++k)
            {
//                T4f* pIt = a.mIndices ? pBegin + a.mIndices[k] : pBegin + k;
//                T4f* qIt = a.mIndices ? qBegin + a.mIndices[k] : qBegin + k;
                int pIt = a.mIndices != null ? a.mIndices.get(k) : k;
                int qIt = a.mIndices != null ? a.mIndices.get(k) : k;

//			    const T4f p = *pIt;
                Vector4f p = p4f;
                p.x  = pBegin.get(4*pIt+0);
                p.y  = pBegin.get(4*pIt+1);
                p.z  = pBegin.get(4*pIt+2);
                p.w  = pBegin.get(4*pIt+3);

//                for (const BoundingBox* oIt = overlapBounds, *oEnd = overlapBounds + numOverlaps; oIt != oEnd; ++oIt)
                for(int i1 = 0; i1 < overlapBounds.length; i1++)
                {
                    BoundingBox oIt = overlapBounds[i1];
                    // point in box test
                    if (anyGreater(oIt._min, p))
                        continue;
                    if (anyGreater(p, oIt._max))
                        continue;

//                    T4f impulse = (p - *qIt) * impulseInvScale;
//				    *qIt = rotate(xform, impulse);
                    q4f.x = qBegin.get(4*qIt+0);
                    q4f.y = qBegin.get(4*qIt+1);
                    q4f.z = qBegin.get(4*qIt+2);
                    q4f.w = qBegin.get(4*qIt+3);
                    Vector4f.sub(p,q4f, q4f);
                    Vector4f impulse = Vector4f.scale(q4f, impulseInvScale, q4f);
                    impulse.w = 0;
                    Matrix4f.transform(xform, impulse, q4f);
                    set(qBegin, qIt, q4f);

                    // transform particle to world space in-place
                    // (will be transformed back after collision)
//				    *pIt = transform(xform, p);
                    Matrix4f.transform(xform,p, p4f);
                    set(pBegin, pIt, p4f);


                    // update world bounds
//                    bounds = expandBounds(bounds, pIt, pIt + 1);
                    bounds.expandBy(p4f);

                    // add particle to output arrays
                    clothIndices[numParticles] = (short)(clothIndex);
                    particleIndices[numParticles] = (short)(pIt - /*pBegin*/0);

                    // output each particle only once
                    ++numParticles;
                    break;
                }
            }
        }

//        allocator.deallocate(sortedIndices);
//        allocator.deallocate(overlapBounds);
//        allocator.deallocate(clothBounds);

        return numParticles;
    }

    private static void set(FloatBuffer buffer, int idx, ReadableVector4f value){
        buffer.put(4*idx+0, value.getX());
        buffer.put(4*idx+1, value.getY());
        buffer.put(4*idx+2, value.getZ());
        buffer.put(4*idx+3, value.getW());
    }

    private static void get(FloatBuffer buffer, int idx, Vector4f value){
        value.x = buffer.get(4*idx+0);
        value.y = buffer.get(4*idx+1);
        value.z = buffer.get(4*idx+2);
        value.w = buffer.get(4*idx+3);
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
                        calculatePotentialColliders(mInstances, /*mInstances + mNumInstances,*/ mCollisionDistance, mClothIndices,
                                mParticleIndices, bounds, mOverlapMasks, mFilter/*, mAllocator*/);
            }

            // collide
            if (mNumParticles > 0)
            {
//                NV_CLOTH_PROFILE_ZONE("cloth::SwInterCollision::Collide", /*ProfileContext::None*/ 0);

                final Vector3f lowerBound = bounds._min;
//                T4f edgeLength = max(bounds.mUpper - lowerBound, sEpsilon);
                final Vector3f edgeLength = Vector3f.sub(bounds._max, lowerBound, null);
                edgeLength.x=Math.max(edgeLength.x, Float.MIN_VALUE);
                edgeLength.y=Math.max(edgeLength.y, Float.MIN_VALUE);
                edgeLength.z=Math.max(edgeLength.z, Float.MIN_VALUE);

                // sweep along longest axis
                int sweepAxis = longestAxis(edgeLength);
                int hashAxis0 = (sweepAxis + 1) % 3;
                int hashAxis1 = (sweepAxis + 2) % 3;

                // reserve 0, 127, and 65535 for sentinel
//                T4f cellSize = max(mCollisionDistance, simd4f(1.0f / 253) * edgeLength);
                final Vector3f cellSize = new Vector3f();
                Vector3f.scale(edgeLength, 1.0f/253, cellSize);
                Vector3f.max(cellSize, mCollisionDistance, cellSize);
//                array(cellSize)[sweepAxis] = array(edgeLength)[sweepAxis] / 65533;
                cellSize.setValue(sweepAxis, edgeLength.get(sweepAxis)/65533);

//                T4f one = gSimd4fOne;
//                T4f gridSize = simd4f(254.0f);
//                array(gridSize)[sweepAxis] = 65534.0f;
                Vector3f one = new Vector3f(1,1,1);
                Vector3f gridSize = new Vector3f(254,254,254);
                gridSize.setValue(sweepAxis, 65534.0f);

//                T4f gridScale = recip<1>(cellSize);
//                T4f gridBias = -lowerBound * gridScale + one;
                Vector3f gridScale = Vector3f.div(1.0f, cellSize, null);
                Vector3f gridBias = Vector3f.scale(lowerBound, gridScale, null);
                gridBias.scale(-1);
                Vector3f.add(gridBias, one, gridBias);

//			    void* buffer = mAllocator.allocate(getBufferSize(mNumParticles));
//                uint32_t* __restrict sortedIndices = reinterpret_cast<uint32_t*>(buffer);
//                uint32_t* __restrict sortedKeys = sortedIndices + mNumParticles;
//                uint32_t* __restrict keys = std::max(sortedKeys + mNumParticles, sortedIndices + 2 * mNumParticles + 1024);
                int[] buffer = new int[getBufferSize(mNumParticles)/4];
                int sortedIndices = 0;
                int sortedKeys = mNumParticles;
                int keys = Math.max(sortedKeys + mNumParticles, sortedIndices + 2 * mNumParticles + 1024);

//                typedef typename Simd4fToSimd4i<T4f>::Type Simd4i;

                Vector3f indexf = new Vector3f();
                final int[] indexi = new int[3];
                // create keys
                for (int i = 0; i < mNumParticles; ++i)
                {
                    // grid coordinate
//                    T4f indexf = getParticle(i) * gridScale + gridBias;
                    ReadableVector3f particle = getParticle(i);
                    Vector3f.scale(particle, gridScale, indexf);
                    Vector3f.add(indexf, gridBias, indexf);

                    // need to clamp index because shape collision potentially
                    // pushes particles outside of their original bounds
//                    Simd4i indexi = intFloor(max(one, min(indexf, gridSize)));
                    indexi[0] = (int) Math.floor(Math.max(1, Math.min(indexf.x, gridSize.x)));
                    indexi[1] = (int) Math.floor(Math.max(1, Math.min(indexf.y, gridSize.y)));
                    indexi[2] = (int) Math.floor(Math.max(1, Math.min(indexf.z, gridSize.z)));

//				    const int32_t* ptr = array(indexi);
//                    keys[i] = uint32_t(ptr[sweepAxis] | (ptr[hashAxis0] << 16) | (ptr[hashAxis1] << 24));
                    buffer[keys+i] = indexi[sweepAxis] | (indexi[hashAxis0] << 16) | (indexi[hashAxis1] << 24);
                }

                // compute sorted keys indices
                radixSort(buffer, keys, keys + mNumParticles, sortedIndices);

                // snoop histogram: offset of first index with 8 msb > 1 (0 is sentinel)
                int firstColumnSize = /*sortedIndices[2 * mNumParticles + 769]*/buffer[sortedIndices + 2 * mNumParticles + 769];

                // sort keys
                for (int i = 0; i < mNumParticles; ++i) {
//                    sortedKeys[i] = keys[sortedIndices[i]];
                    buffer[sortedKeys + i] = buffer[keys + buffer[sortedIndices + i]];
                }
//                sortedKeys[mNumParticles] = uint32_t(-1); // sentinel
                buffer[sortedKeys + mNumParticles] = -1;

                // calculate the number of buckets we need to search forward
//			    const Simd4i data = intFloor(gridScale * mCollisionDistance);
                final int[] data = {
                        (int)Math.floor(gridScale.x * mCollisionDistance.x),
                        (int)Math.floor(gridScale.y * mCollisionDistance.y),
                        (int)Math.floor(gridScale.z * mCollisionDistance.z),
                };
//                uint32_t collisionDistance = uint32_t(2 + array(data)[sweepAxis]);
                int collisionDistance = 2 + data[sweepAxis];

                // collide particles
                collideParticles(buffer, sortedKeys, firstColumnSize, sortedIndices, mNumParticles, collisionDistance);
//                mAllocator.deallocate(buffer);
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
//                NV_CLOTH_PROFILE_ZONE("cloth::SwInterCollision::PostTransform", /*ProfileContext::None*/ 0);

//                T4f toLocal[4], impulseScale;
//                uint16_t lastCloth = uint16_t(0xffff);
                final Matrix4f toLocal = new Matrix4f();
                final Vector3f impulseScale = new Vector3f();
                short lastCloth = (short)0xffff;

                final Vector4f particle = new Vector4f();
                final Vector4f impulse = new Vector4f();
                for (int i = 0; i < mNumParticles; ++i)
                {
                    int clothIndex = mClothIndices[i];
				    SwInterCollisionData instance = mInstances[clothIndex];

                    // todo: could pre-compute these inverses
                    if (clothIndex != lastCloth)
                    {
//					    const PxMat44 xform = PxMat44(instance->mGlobalPose.getInverse());
//                        toLocal[0] = load(reinterpret_cast<const float*>(&xform.column0));
//                        toLocal[1] = load(reinterpret_cast<const float*>(&xform.column1));
//                        toLocal[2] = load(reinterpret_cast<const float*>(&xform.column2));
//                        toLocal[3] = load(reinterpret_cast<const float*>(&xform.column3));
                        instance.mGlobalPose.toMat(toLocal);
                        toLocal.invert();

//                        impulseScale = simd4f(instance->mImpulseScale);
                        impulseScale.set(instance.mImpulseScale, instance.mImpulseScale, instance.mImpulseScale);
                        lastCloth = mClothIndices[i];
                    }

                    int particleIndex = mParticleIndices[i];
//                    T4f& particle = reinterpret_cast<T4f&>(instance->mParticles[particleIndex]);
//                    T4f& impulse = reinterpret_cast<T4f&>(instance->mPrevParticles[particleIndex]);
                    get(instance.mParticles, particleIndex, particle);
                    get(instance.mPrevParticles, particleIndex, impulse);

//                    particle = transform(toLocal, particle);
                    Matrix4f.transform(toLocal, particle, particle);
                    // avoid w becoming negative due to numerical inaccuracies
//                    impulse = max(sZeroW, particle - rotate(toLocal, T4f(impulse * impulseScale)));
                    Vector4f.scale(impulse, instance.mImpulseScale, impulse);
                    impulse.w = 0;
                    Matrix4f.transform(toLocal, impulse, impulse);
                    Vector4f.sub(particle, impulse, impulse);
                    impulse.w = Math.max(0, impulse.w);

                    set(instance.mParticles, particleIndex, particle);
                    set(instance.mPrevParticles, particleIndex, impulse);
                }
            }
        }

//        mAllocator.deallocate(mOverlapMasks);
//        mAllocator.deallocate(mParticleIndices);
//        mAllocator.deallocate(mClothIndices);
    }

    public static int estimateTemporaryMemory(SwInterCollisionData[] cloths, int n){
        // count total particles
        int numParticles = 0;
        for (int i = 0; i < n; ++i)
            numParticles += cloths[i].mNumParticles;

        int boundsSize = 2 * n * /*sizeof(BoundingBox<T4f>)*/Vector4f.SIZE * 2 + n * /*sizeof(uint32_t)*/4;
        int clothIndicesSize = numParticles * /*sizeof(uint16_t)*/2;
        int particleIndicesSize = numParticles * /*sizeof(uint32_t)*/4;
        int masksSize = n * /*sizeof(uint32_t)*/4;

        return boundsSize + clothIndicesSize + particleIndicesSize + masksSize + getBufferSize(numParticles);
    }

    private static int getBufferSize(int numParticles){
        int keysSize = numParticles * /*sizeof(uint32_t)*/4;
        int indicesSize = numParticles * /*sizeof(uint32_t)*/4;
        int histogramSize = 1024 * /*sizeof(uint32_t)*/4;

        return keysSize + indicesSize + Math.max(indicesSize + histogramSize, keysSize);
    }

    private void collideParticles(int[] values, int/*[]*/ keys, int firstColumnSize, int/*[]*/ indices,
                                  int numParticles, int collisionDistance){
        final int bucketMask = Numeric.unsignedShort((short)-1);

        final int keyOffsets[] = { 0, 0x00010000, 0x00ff0000, 0x01000000, 0x01010000 };

//        const uint32_t* __restrict kFirst[5];
//        const uint32_t* __restrict kLast[5];
        final int[] kFirst = new int[5];
        final int[] kLast = new int[5];

        {
            // optimization: scan forward iterator starting points once instead of 9 times
		    /*const uint32_t* __restrict*/int kIt = keys;

            int key = /**kIt*/values[kIt];
            int firstKey = key - Math.min(collisionDistance, key & bucketMask);
            int lastKey = Math.min(key + collisionDistance, key | bucketMask);

            kFirst[0] = kIt;
            while (/**kIt*/values[kIt] < lastKey)
            ++kIt;
            kLast[0] = kIt;

            for (int k = 1; k < 5; ++k)
            {
                for (int n = firstKey + keyOffsets[k]; /**kIt*/values[kIt] < n;)
                ++kIt;
                kFirst[k] = kIt;

                for (int n = lastKey + keyOffsets[k]; /**kIt*/values[kIt] < n;)
                ++kIt;
                kLast[k] = kIt;

                // jump forward once to second column
                kIt = keys + firstColumnSize;
                firstColumnSize = 0;
            }
        }

        /*const uint32_t* __restrict*/int iIt = indices;
        /*const uint32_t* __restrict*/int iEnd = indices + numParticles;

        /*const uint32_t* __restrict*/int jIt;
        /*const uint32_t* __restrict*/int jEnd;

        for (; iIt != iEnd; ++iIt, ++kFirst[0])
        {
            // load current particle once outside of inner loop
            int index = /**iIt*/values[iIt];
            assert (index < mNumParticles);
            mClothIndex = mClothIndices[index];
            assert (mClothIndex < mNumInstances);
            mClothMask = mOverlapMasks[mClothIndex];

		    SwInterCollisionData instance = mInstances[mClothIndex];

            mParticleIndex = mParticleIndices[index];
//            mParticle = reinterpret_cast<const T4f&>(instance->mParticles[mParticleIndex]);
//            mImpulse = reinterpret_cast<const T4f&>(instance->mPrevParticles[mParticleIndex]);
            get(instance.mParticles, mParticleIndex, mParticle);
            get(instance.mPrevParticles, mParticleIndex, mImpulse);

            int key = /**kFirst[0]*/values[kFirst[0]];

            // range of keys we need to check against for this particle
            int firstKey = key - Math.min(collisionDistance, key & bucketMask);
            int lastKey = Math.min(key + collisionDistance, key | bucketMask);

            // scan forward end point
            while (/**kLast[0]*/values[kLast[0]] < lastKey)
            ++kLast[0];

            // process potential colliders of same cell
            jEnd = indices + (kLast[0] - keys);
            for (jIt = iIt + 1; jIt != jEnd; ++jIt)
                collideParticle(/**jIt*/values[jIt]);

            // process neighbor cells
            for (int k = 1; k < 5; ++k)
            {
                // scan forward start point
                for (int n = firstKey + keyOffsets[k]; /**kFirst[k]*/values[kFirst[k]] < n;)
                ++kFirst[k];

                // scan forward end point
                for (int n = lastKey + keyOffsets[k]; /**kLast[k]*/values[kLast[k]] < n;)
                ++kLast[k];

                // process potential colliders
                jEnd = indices + (kLast[k] - keys);
                for (jIt = indices + (kFirst[k] - keys); jIt != jEnd; ++jIt)
                    collideParticle(/**jIt*/values[jIt]);
            }

            // write back particle and impulse
//            reinterpret_cast<T4f&>(instance->mParticles[mParticleIndex]) = mParticle;
//            reinterpret_cast<T4f&>(instance->mPrevParticles[mParticleIndex]) = mImpulse;
            set(instance.mParticles, mParticleIndex, mParticle);
            set(instance.mPrevParticles, mParticleIndex, mImpulse);
        }
    }

    private ReadableVector4f getParticle(int index){
        assert (index < mNumParticles);

        short clothIndex = mClothIndices[index];
        int particleIndex = mParticleIndices[index];

        assert (clothIndex < mNumInstances);

//        return reinterpret_cast<T4f&>(mInstances[clothIndex].mParticles[particleIndex]);
        get(mInstances[clothIndex].mParticles, particleIndex, mTemp);
        return mTemp;
    }

    private static boolean allGreater(float left, Vector4f right){
        return left > right.x && left > right.y && left > right.z && left > right.w;
    }

    // better wrap these in a struct
    private void collideParticle(int index){
        int clothIndex = mClothIndices[index];

        if (((1 << clothIndex) & ~mClothMask) != 0)
            return;

	    SwInterCollisionData instance = mInstances[clothIndex];

        final int particleIndex = mParticleIndices[index];
//        T4f& particle = reinterpret_cast<T4f&>(instance->mParticles[particleIndex]);
        final Vector4f particle = new Vector4f();
        get(instance.mParticles, particleIndex, particle);

//        T4f diff = particle - mParticle;
//        T4f distSqr = dot3(diff, diff);
        Vector4f diff = Vector4f.sub(particle, mParticle, null);
        float distSqr = Vector3f.lengthSquared(diff);

//#if PX_DEBUG
                ++mNumTests;
//#endif

        if (allGreater(distSqr, mCollisionSquareDistance))
            return;

//        T4f w0 = splat<3>(mParticle);
//        T4f w1 = splat<3>(particle);
        float w0 = mParticle.w;  // TODO
        float w1 = particle.w;

//        T4f ratio = mCollisionDistance * rsqrt<1>(distSqr);
//        T4f scale = mStiffness * recip<1>(sEpsilon + w0 + w1);
//        T4f delta = (scale * (diff - diff * ratio)) & sMaskXYZ;
        Vector4f ratio = Vector4f.scale(mCollisionDistance, 1.0f/(float)Math.sqrt(distSqr), null); //TODO
        Vector4f scale = Vector4f.scale(mStiffness, 1.0f/(float)Math.sqrt(Float.MIN_NORMAL + w0+w1), null); // TODO
        Vector4f delta = Vector4f.scale(diff, ratio, null);
        Vector4f.sub(diff, delta, delta);
        Vector4f.scale(scale, delta, delta);
        delta.w = 0;

//        mParticle = mParticle + delta * w0;
//        particle = particle - delta * w1;
        mParticle.x += delta.x * w0;
        mParticle.y += delta.y * w0;
        mParticle.z += delta.z * w0;

        particle.x -= delta.x * w1;
        particle.y -= delta.y * w1;
        particle.z -= delta.z * w1;
        set(instance.mParticles, particleIndex, particle);

//        T4f& impulse = reinterpret_cast<T4f&>(instance->mPrevParticles[particleIndex]);
        Vector4f impulse = particle;
        get(instance.mPrevParticles, particleIndex, impulse);

//        mImpulse = mImpulse + delta * w0;
//        impulse = impulse - delta * w1;
        mImpulse.x += delta.x * w0;
        mImpulse.y += delta.y * w0;
        mImpulse.z += delta.z * w0;

        impulse.x -= delta.x * w1;
        impulse.y -= delta.y * w1;
        impulse.z -= delta.z * w1;
        set(instance.mPrevParticles, particleIndex, impulse);


//#if PX_DEBUG || PX_PROFILE
                ++mNumCollisions;
//#endif
    }
}
