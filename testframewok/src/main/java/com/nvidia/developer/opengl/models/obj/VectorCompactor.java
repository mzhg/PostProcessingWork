package com.nvidia.developer.opengl.models.obj;

import org.lwjgl.util.vector.Vector;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import jet.opengl.postprocessing.util.StackInt;

final class VectorCompactor<T extends Vector> {

	// The comparator object used to evaluate vectors for merging
	private Difference<T> m_comp;
	// The tolerance to be used when evaluating vectors for merging
	private float m_epsilon;
	
	// The compacted set of vectors
	private final ArrayList<T> m_vecs = new ArrayList<>();
	// The map of existing vectors to their index in the compacted set
	private final TreeMap<T, Integer> m_vecMap = new TreeMap<>(new VecCompare<>());
	
	// One entry per vector added to the container, 
    // containing the index in the compacted set that 
    // holds the vector (or the one that it was merged
    // with), in order of their addition to the set
	private final StackInt m_mappedIndices = new StackInt();
	
	VectorCompactor(float epsilon, int reserveSize, Difference<T> comp){
		m_comp = comp;
		m_epsilon = epsilon;
		m_comp = comp;
		Reserve(reserveSize);
		
	}
	
	// Clear out the data from the container so that it may be reused
    void Clear()
    {
        m_vecs.clear();
        m_mappedIndices.resize(0);
        m_vecMap.clear();
    }

    // Reserve an initial size for the container and its underlying structures.
    // It will grow as needed, but providing a reasonable starting size will
    // reduce the number of re-allocations and copies.
    /// \param[in] size Number of elements to be resize the container to hold
    void Reserve(int size)
    {
        m_vecs.ensureCapacity(size);
        m_mappedIndices.reserve(size);
    }

    // Adds a vector to the container.  The vector's original index will be 
    // equal to the number of vectors already added.  The returned index will
    // be the vector's position in the compacted set of vectors.
    // \param[in] v Vector to add to the container
    // \return Index into the compacted set of vectors tha matches the added vector
    int Append(T v)
    {
        int index = FindOrAddObject(v);
//        NV_ASSERT(-1 != index);
        if(index == -1)
        	throw new IllegalArgumentException();
        m_mappedIndices.push(index);
        return index;
    }

    // Returns the vector stored at the given index in the compacted set of vectors
    // \param[in] id Index in the compacted set of vectors that holds the requested vector
    // \param[out] out Object in which to store the retrieved vector
    // \return True if a vector existed at the provided index and was retrieved into
    //         the 'out' parameter.  False if no vector existed with the given index.
    boolean GetObject(int id, T out)
    {
        if ((id < 0) || (id >= (m_vecs.size())))
        {
            return false;
        }
//        out = m_vecs[id];
        T value = m_vecs.get(id);
        for(int i = 0 ; i < out.getCount(); i++){
        	out.setValue(i, value.get(i));
        }
        return true;
    }

    // Returns the index, into the array of compacted vectors, that holds the vector
    // inserted with the given, original index.
    // \param[in] index Original index of the vector
    // \return Matching index in the compacted set of vectors that was used to store the vector
    int Remap(int index)
    {
        if ((index < 0) || (index >= (m_mappedIndices.size())))
        {
//            NV_ASSERT(0);
            return -1;
        }
//        NV_ASSERT(m_mappedIndices[index] != -1);
        int idx = m_mappedIndices.get(index);
        if(idx == -1)
        	throw new IllegalArgumentException();
        return idx;
    }

    // Returns the set of compacted vectors
    List<T> GetVectors() { return m_vecs; }

    // Returns the number of vectors in the compacted set of vectors
    int GetVectorCount() { return m_vecs.size(); }

    // Note that the key can not be modified once added, so doing this rescale
    // causes the keys to be out of sync with the vectors themselves.  No new
    // vectors should be added after this method is called, or they may be
    // sorted incorrectly.
    // \param[in] scale Scale factor to multiply all vectors by
    // \param[in] center Object space point about which center the vectors before scaling them
    void RescaleToOrigin(float scale, T center)
    {
//        typename std::vector<T>::iterator it = m_vecs.begin();
//        typename std::vector<T>::const_iterator itEnd = m_vecs.end();
//
//        while (it != itEnd)
//        {
//            (*it) = scale * ((*it) - center);
//            ++it;
//        }
    	for(T it : m_vecs){
    		for(int i = 0; i < it.getCount(); i++){
    			float value = scale * (it.get(i) - center.get(i));
    			it.setValue(i, value);
    		}
    	}
    }
    
 // Finds an existing vector in the set of compacted vectors that is
    // close enough to the given vector to merge and returns the index
    // of that vector.  If none is within the current tolerance, the new
    // vector will be added and its new index returned.  Uses the 
    // specialized Diff() and ShouldMerge() methods to determine redundancy.
    // \param[in] v Vector to be added to the container
    // \return The index of the vector in the compacted set
    @SuppressWarnings("unchecked")
	private int FindOrAddObject(T v)
    {
        // If we have nothing yet, then we simply add this vector to the set
        if (m_vecs.isEmpty())
        {
            return Add(v, 0);
        }

        // We'll need to find the subset of existing vectors that are potential candidates
        // for welding by finding all within the epsilon of the given vector.  We
        // will use a vector whose components are all epsilon to add and subtract
        // from the given vector to define the extents of our search.  Since we don't
        // know how many components our contained vectors will have, we will use a
        // vec4f to initialize it, as all vectors can be initialized from one.
//        T epsilonVec = T(nv::vec4f(m_epsilon, m_epsilon, m_epsilon, m_epsilon));

        // Find the first vector whose x is greater than our minimum
//        T minVec = v - epsilonVec;
        T minVec = (T) new Vector2f(v.get(0) - m_epsilon, v.get(1) - m_epsilon);
//        typename VecMap::iterator rangeStart = m_vecMap.lower_bound(minVec);
        T rangeStart = m_vecMap.floorKey(minVec);
//        Map.Entry<T, StackInt> rangeStart = m_vecMap.lowerEntry(minVec);

        if (rangeStart == null)
        {
            // All existing positions are "below" our minimum point, so insert a 
            // new vector at the end of the map
            return Add(v, /*--rangeStart*/0);
        }

        // Find the first vector whose x is greater than our maximum, thus out of our 
        // range of candidates
//        T maxVec = v + epsilonVec;
        T maxVec = (T) new Vector2f(v.get(0) + m_epsilon, v.get(1) + m_epsilon);
//        typename VecMap::iterator rangeEnd = m_vecMap.upper_bound(maxVec);
//        Map.Entry<T, StackInt> rangeEnd = m_vecMap.higherEntry(maxVec);
        T rangeEnd = m_vecMap.ceilingKey(maxVec);

        if (rangeStart == rangeEnd)
        {
            // No positions are in the range we're looking for, so insert one 
            // at the closest point in the map
            return Add(v, /*--rangeStart*/0);
        }

        // Use the first candidate within our possible range to initialize 
        // our search as the closest candidate
//        typename VecMap::iterator closestPosition = rangeStart;
//        typename VecMap::iterator currentIt = rangeStart;
        int closestPosition = -1;
        float bestDist2 = Float.MAX_VALUE; // m_comp.diff(v, rangeStart);
        // Check all the other vectors in the range for a closer fit
//        for (++currentIt; currentIt != rangeEnd; ++currentIt)
//        {
//            float dist2 = m_comp.Diff(v, currentIt->first);
//            if (dist2 < bestDist2)
//            {
//                bestDist2 = dist2;
//                closestPosition = currentIt;
//            }
//        }
        
        Map<T, Integer> range_map = m_vecMap.subMap(rangeStart, rangeEnd);
        for(Map.Entry<T, Integer> currentIt : range_map.entrySet()){
	          float dist2 = m_comp.diff(v, currentIt.getKey());
	          if (dist2 < bestDist2)
	          {
	              bestDist2 = dist2;
	              closestPosition = currentIt.getValue();
	          }
        }

        // Once we have our closest match, see if it's close enough
        if (!m_comp.shouldMerge(bestDist2, m_epsilon))
        {
            // It wasn't, so we need to add the given vector as a new one
            return Add(v, 0);
        }

        // We found a suitable vector for re-use
        return closestPosition;
    }

    // Adds the given vector to the compacted set, using the given hint
    // to find the right spot
    private int Add(T v, /*const typename VecMap::iterator& hint*/int position)
    {
        // The remapped index will be the current number of vectors in the compacted set
        int index = m_vecs.size();
        m_vecs.add(v);

        // Add a map entry for the given vector to our vector map, pointing it towards
        // the proper location in the compacted set
        m_vecMap.put(v, index);

        // Return the new, compacted set index to the caller to be added to the remapping vector
        return index;
    }
	
	interface Difference<V>{
		float diff(V v1, V v2);
		boolean shouldMerge(float diff, float epsilon);
	}
	
	private static class VecCompare<T extends Vector> implements Comparator<T>{
		@Override
		public int compare(T o1, T o2) {
			return Float.compare(o1.get(0), o1.get(0));
		}
	}
	
	
}
