/*
Open Asset Import Library (assimp)
----------------------------------------------------------------------

Copyright (c) 2006-2012, assimp team
All rights reserved.

Redistribution and use of this software in source and binary forms, 
with or without modification, are permitted provided that the 
following conditions are met:

* Redistributions of source code must retain the above
  copyright notice, this list of conditions and the
  following disclaimer.

* Redistributions in binary form must reproduce the above
  copyright notice, this list of conditions and the
  following disclaimer in the documentation and/or other
  materials provided with the distribution.

* Neither the name of the assimp team, nor the names of its
  contributors may be used to endorse or promote products
  derived from this software without specific prior
  written permission of the assimp team.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT 
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE 
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

----------------------------------------------------------------------
*/
package assimp.common;

import it.unimi.dsi.fastutil.ints.IntList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.lwjgl.util.vector.Vector3f;

/** Specialized version of SpatialSort to support smoothing groups
 *  This is used in by the 3DS, ASE and LWO loaders. 3DS and ASE share their 
 *  normal computation code in SmoothingGroups.inl, the LWO loader has its own
 *  implementation to handle all details of its file format correctly.
 */
public class SGSpatialSort {

	/** Normal of the sorting plane, normalized. The center is always at (0, 0, 0) */
	protected final Vector3f mPlaneNormal = new Vector3f();
	// all positions, sorted by distance to the sorting plane
	protected final List<Entry> mPositions = new ArrayList<>();
	
	public SGSpatialSort(){
		// define the reference plane. We choose some arbitrary vector away from all basic axises 
		// in the hope that no model spreads all its vertices along this plane.
		mPlaneNormal.set( 0.8523f, 0.34321f, 0.5736f);
		mPlaneNormal.normalise();
	}

	// -------------------------------------------------------------------
	/** Add a vertex to the spatial sort
	 * @param vPosition Vertex position to be added
	 * @param index Index of the vrtex
	 * @param smoothingGroup SmoothingGroup for this vertex
	 */
	public void add(Vector3f vPosition, int index, int smoothingGroup){
		// store position by index and distance
		float distance = Vector3f.dot(vPosition, mPlaneNormal);
		mPositions.add(new Entry( index, vPosition, distance, smoothingGroup));
	}

	// -------------------------------------------------------------------
	/** Prepare the spatial sorter for use. This step runs in O(logn)
	 */
	public void prepare(){
		Collections.sort(mPositions);
	}

	// -------------------------------------------------------------------
	/** Returns an iterator for all positions close to the given position.
	 * @param pPosition The position to look for vertices.
	 * @param pSG Only included vertices with at least one shared smooth group
	 * @param pRadius Maximal distance from the position a vertex may have
	 *   to be counted in.
	 * @param poResults The container to store the indices of the found
	 *   positions. Will be emptied by the call so it may contain anything.
	 * @param exactMatch Specifies whether smoothing groups are bit masks 
	 *   (false) or integral values (true). In the latter case, a vertex
	 *   cannot belong to more than one smoothing group.
	 * @return An iterator to iterate over all vertices in the given area.
	 */
	// -------------------------------------------------------------------
	public void findPositions(Vector3f pPosition, int pSG, float pRadius, IntList poResults, boolean exactMatch/* = false*/){
		float dist = Vector3f.dot(pPosition, mPlaneNormal);
		float minDist = dist - pRadius, maxDist = dist + pRadius;

		// clear the array in this strange fashion because a simple clear() would also deallocate
		// the array which we want to avoid
//		poResults.erase( poResults.begin(), poResults.end());
		poResults.clear();

		// quick check for positions outside the range
		int size = mPositions.size();
		if( size == 0)
			return;
		if( maxDist < mPositions.get(0).mDistance)
			return;
		if( minDist > mPositions.get(size - 1).mDistance)
			return;

		// do a binary search for the minimal distance to start the iteration there
		int index = size / 2;
		int binaryStepSize = size / 4;
		while( binaryStepSize > 1)
		{
			if( mPositions.get(index).mDistance < minDist)
				index += binaryStepSize;
			else
				index -= binaryStepSize;

			binaryStepSize /= 2;
		}

		// depending on the direction of the last step we need to single step a bit back or forth
		// to find the actual beginning element of the range
		while( index > 0 && mPositions.get(index).mDistance > minDist)
			index--;
		while( index < (mPositions.size() - 1) && mPositions.get(index).mDistance < minDist)
			index++;

		// Mow start iterating from there until the first position lays outside of the distance range.
		// Add all positions inside the distance range within the given radius to the result aray

		float squareEpsilon = pRadius * pRadius;
//		std::vector<Entry>::const_iterator it  = mPositions.begin() + index;
//		std::vector<Entry>::const_iterator end = mPositions.end();
		int _it = index;
		int end = size;

		if (exactMatch)
		{
			Entry it = mPositions.get(_it);
			while( it.mDistance < maxDist)
			{
				if(Vector3f.distanceSquare(it.mPosition, pPosition) < squareEpsilon && it.mSmoothGroups == pSG)
				{
					poResults.add( it.mIndex);
				}
//				++it;
				_it++; 
				if( end == _it )break;
				it = mPositions.get(_it);
			}
		}
		else
		{
			Entry it = mPositions.get(_it);
			// if the given smoothing group is 0, we'll return all surrounding vertices
			if (pSG == 0)
			{
				while( it.mDistance < maxDist)
				{
					if(Vector3f.distanceSquare(it.mPosition, pPosition) < squareEpsilon)
						poResults.add( it.mIndex);
					++_it;
					if( end == _it)break;
					it = mPositions.get(_it);
				}
			}
			else while( it.mDistance < maxDist)
			{
				if(Vector3f.distanceSquare(it.mPosition, pPosition) < squareEpsilon &&
					((it.mSmoothGroups & pSG)!=0 || it.mSmoothGroups == 0))
				{
					poResults.add( it.mIndex);
				}
//				++it;
				++_it;
				if( end == _it)break;
				it = mPositions.get(_it);
			}
		}
	}
	
	/** An entry in a spatially sorted position array. Consists of a 
	 *  vertex index, its position and its precalculated distance from
	 *  the reference plane */
	protected static final class Entry implements Comparable<Entry>{
		int mIndex;	///< The vertex referred by this entry
		Vector3f mPosition;	///< Position
		int mSmoothGroups;
		float mDistance;		///< Distance of this vertex to the sorting plane
		
		public Entry(int pIndex, Vector3f pPosition, float pDistance,int pSG) {
			mIndex = pIndex;
			mPosition = pPosition;
			mSmoothGroups = pSG;
			mDistance = pDistance;
		}

		@Override
		public int compareTo(Entry o) {
			if(mDistance < o.mDistance)
				return -1;
			else if(mDistance > o.mDistance)
				return 1;
			else
				return 0;
		}
	}
}
