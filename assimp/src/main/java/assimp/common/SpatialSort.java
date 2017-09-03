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

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.lwjgl.util.vector.Vector3f;

/** A little helper class to quickly find all vertices in the epsilon environment of a given
 * position. Construct an instance with an array of positions. The class stores the given positions
 * by their indices and sorts them by their distance to an arbitrary chosen plane.
 * You can then query the instance for all vertices close to a given position in an average O(log n) 
 * time, with O(n) worst case complexity when all vertices lay on the plane. The plane is chosen
 * so that it avoids common planes in usual data sets. */
public class SpatialSort {

	/** Normal of the sorting plane, normalized. The center is always at (0, 0, 0) */
	protected final Vector3f mPlaneNormal = new Vector3f(0.8523f, 0.34321f, 0.5736f);
	protected final List<Entry> mPositions = new ArrayList<>();
	

	public SpatialSort(){
		mPlaneNormal.normalise();
	}

	// ------------------------------------------------------------------------------------
	/** Constructs a spatially sorted representation from the given position array.
	 * Supply the positions in its layout in memory, the class will only refer to them
	 * by index.
	 * @param pPositions Pointer to the first position vector of the array.
	 * @param pNumPositions Number of vectors to expect in that array.
	 * @param pElementOffset Offset in bytes from the beginning of one vector in memory 
	 *   to the beginning of the next vector. */
	public SpatialSort(FloatBuffer pPositions, int pNumPositions, int pElementOffset){
		fill(pPositions,pNumPositions,pElementOffset, true);
	}
	
	/** Sets the input data for the SpatialSort. This replaces existing data, if any.
	 *  The new data receives new indices in ascending order.
	 *
	 * @param pPositions Pointer to the first position vector of the array.
	 * @param pNumPositions Number of vectors to expect in that array.
	 * @param pElementOffset Offset in bytes from the beginning of one vector in memory 
	 *   to the beginning of the next vector. 
	 * @param pFinalize Specifies whether the SpatialSort's internal representation
	 *   is finalized after the new data has been added. Finalization is
	 *   required in order to use #FindPosition() or #GenerateMappingTable().
	 *   If you don't finalize yet, you can use #Append() to add data from
	 *   other sources.*/
	public void fill(FloatBuffer pPositions, int pNumPositions, int pElementOffset, boolean pFinalize/* = true*/){
		mPositions.clear();
		append(pPositions,pNumPositions,pElementOffset,pFinalize);
	}


	// ------------------------------------------------------------------------------------
	/** Same as #Fill(), except the method appends to existing data in the #SpatialSort. */
	public void append(FloatBuffer pPositions, int pNumPositions, int pElementOffset, boolean pFinalize){
		// store references to all given positions along with their distance to the reference plane
		final int initial = mPositions.size();
//		mPositions.reserve(initial + (pFinalize?pNumPositions:pNumPositions*2));
		for(int a = 0; a < pNumPositions; a++)
		{
//			const char* tempPointer = reinterpret_cast<const char*> (pPositions);
//			const aiVector3D* vec   = reinterpret_cast<const aiVector3D*> (tempPointer + a * pElementOffset);
//			Vector3f vec = pPositions[pElementOffset/(3 * 4)];
			int index = pElementOffset/4;
			Vector3f vec = new Vector3f();
			vec.x = pPositions.get(index++);
			vec.y = pPositions.get(index++);
			vec.z = pPositions.get(index++);

			// store position by index and distance
			float distance = Vector3f.dot(vec, mPlaneNormal);
			mPositions.add(new Entry( a+initial, vec, distance));
		}

		if (pFinalize) {
			// now sort the array ascending by distance.
			finalise();
		}
	}
	
	/** Get the count of positions. */
	public int size() { return mPositions.size();}


	// ------------------------------------------------------------------------------------
	/** Finalize the spatial hash data structure. This can be useful after
	 *  multiple calls to #Append() with the pFinalize parameter set to false.
	 *  This is finally required before one of #FindPositions() and #GenerateMappingTable()
	 *  can be called to query the spatial sort.*/
	public void finalise(){
		Collections.sort(mPositions);
	}

	// ------------------------------------------------------------------------------------
	/** Returns an iterator for all positions close to the given position.
	 * @param pPosition The position to look for vertices.
	 * @param pRadius Maximal distance from the position a vertex may have to be counted in.
	 * @param poResults The container to store the indices of the found positions. 
	 *   Will be emptied by the call so it may contain anything.
	 * @return An iterator to iterate over all vertices in the given area.*/
	public void findPositions(Vector3f pPosition, float pRadius, List<Integer> poResults){
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
		int index = mPositions.size() / 2;
		int binaryStepSize = mPositions.size() / 4;
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
		final float pSquared = pRadius*pRadius;
//		std::vector<Entry>::const_iterator it = mPositions.begin() + index;
//		while( it->mDistance < maxDist)
//		{
//			if( (it->mPosition - pPosition).SquareLength() < pSquared)
//				poResults.push_back( it->mIndex);
//			++it;
//			if( it == mPositions.end())
//				break;
//		}
		
		for(int i = index; i < mPositions.size(); i++){
			Entry e = mPositions.get(i);
			if(e.mDistance < maxDist){
				if(Vector3f.distanceSquare(e.mPosition, pPosition) < pSquared)
					poResults.add(e.mIndex);
			}else
				break;
		}
	}
	
	@SuppressWarnings("unused")
	private static int toBinary(float pValue){
		final int binValue = Float.floatToIntBits(pValue);
		
		// floating-point numbers are of sign-magnitude format, so find out what signed number
		//	representation we must convert negative values to.
		// See http://en.wikipedia.org/wiki/Signed_number_representations.
		// Two's complement?
		if( (-42 == (~42 + 1)) && (binValue & 0x80000000) != 0)
			return (1 << (8 * 4 - 1)) - binValue;
		// One's complement?
		else if( (-42 == ~42) && (binValue & 0x80000000) != 0)
			return (-0) - binValue;
		// Sign-magnitude?
		else if( (-42 == (42 | (-0))) && (binValue & 0x80000000) != 0) // -0 = 1000... binary
			return binValue;
		else
			return binValue;
	}

	// ------------------------------------------------------------------------------------
	/** Fills an array with indices of all positions indentical to the given position. In
	 *  opposite to FindPositions(), not an epsilon is used but a (very low) tolerance of
	 *  four floating-point units.
	 * @param pPosition The position to look for vertices.
	 * @param poResults The container to store the indices of the found positions. 
	 *   Will be emptied by the call so it may contain anything.*/
	public void findIdenticalPositions(Vector3f pPosition, List<Integer> poResults){
		// Epsilons have a huge disadvantage: they are of constant precision, while floating-point
		//	values are of log2 precision. If you apply e=0.01 to 100, the epsilon is rather small, but
		//	if you apply it to 0.001, it is enormous.

		// The best way to overcome this is the unit in the last place (ULP). A precision of 2 ULPs
		//	tells us that a float does not differ more than 2 bits from the "real" value. ULPs are of
		//	logarithmic precision - around 1, they are 1?2^24) and around 10000, they are 0.00125.

		// For standard C math, we can assume a precision of 0.5 ULPs according to IEEE 754. The
		//	incoming vertex positions might have already been transformed, probably using rather
		//	inaccurate SSE instructions, so we assume a tolerance of 4 ULPs to safely identify
		//	identical vertex positions.
		final int toleranceInULPs = 4;
		// An interesting point is that the inaccuracy grows linear with the number of operations:
		//	multiplying to numbers, each inaccurate to four ULPs, results in an inaccuracy of four ULPs
		//	plus 0.5 ULPs for the multiplication.
		// To compute the distance to the plane, a dot product is needed - that is a multiplication and
		//	an addition on each number.
		final int distanceToleranceInULPs = toleranceInULPs + 1;
		// The squared distance between two 3D vectors is computed the same way, but with an additional
		//	subtraction.
		final int distance3DToleranceInULPs = distanceToleranceInULPs + 1;

		// Convert the plane distance to its signed integer representation so the ULPs tolerance can be
		//	applied. For some reason, VC won't optimize two calls of the bit pattern conversion.
		final int minDistBinary = toBinary(Vector3f.dot(pPosition , mPlaneNormal)) - distanceToleranceInULPs;
		final int maxDistBinary = minDistBinary + 2 * distanceToleranceInULPs;

		// clear the array in this strange fashion because a simple clear() would also deallocate
	    // the array which we want to avoid
//		poResults.erase( poResults.begin(), poResults.end());
		poResults.clear();

		// do a binary search for the minimal distance to start the iteration there
		int index = mPositions.size() / 2;
		int binaryStepSize = mPositions.size() / 4;
		while( binaryStepSize > 1)
		{
			// Ugly, but conditional jumps are faster with integers than with floats
			if( minDistBinary > toBinary(mPositions.get(index).mDistance))
				index += binaryStepSize;
			else
				index -= binaryStepSize;

			binaryStepSize /= 2;
		}

		// depending on the direction of the last step we need to single step a bit back or forth
		// to find the actual beginning element of the range
		while( index > 0 && minDistBinary < toBinary(mPositions.get(index).mDistance) )
			index--;
		while( index < (mPositions.size() - 1) && minDistBinary > toBinary(mPositions.get(index).mDistance))
			index++;

		// Now start iterating from there until the first position lays outside of the distance range.
		// Add all positions inside the distance range within the tolerance to the result aray
//		std::vector<Entry>::const_iterator it = mPositions.begin() + index;
//		while( ToBinary(it->mDistance) < maxDistBinary)
//		{
//			if( distance3DToleranceInULPs >= ToBinary((it->mPosition - pPosition).SquareLength()))
//				poResults.push_back(it->mIndex);
//			++it;
//			if( it == mPositions.end())
//				break;
//		}
		
		for(int i = index; i < mPositions.size(); i++){
			Entry e = mPositions.get(i);
			if(toBinary(e.mDistance) < maxDistBinary){
				if(distance3DToleranceInULPs >= toBinary(Vector3f.distanceSquare(e.mPosition, pPosition)))
					poResults.add(e.mIndex);
			}else
				break;
		}
	}

	// ------------------------------------------------------------------------------------
	/** Compute a table that maps each vertex ID referring to a spatially close
	 *  enough position to the same output ID. Output IDs are assigned in ascending order
	 *  from 0...n.
	 * @param fill Will be filled with numPositions entries. 
	 * @param pRadius Maximal distance from the position a vertex may have to
	 *   be counted in.
	 *  @return Number of unique vertices (n).  */
	public int generateMappingTable(int[] fill, float pRadius){
//		fill.resize(mPositions.size(),UINT_MAX);
		if(fill.length < mPositions.size())
			throw new IllegalArgumentException();
		Arrays.fill(fill, -1);
		float dist, maxDist;

		int t=0;
		final float pSquared = pRadius*pRadius;
		for (int i = 0; i < mPositions.size();) {
			Entry e = mPositions.get(i);
			dist = Vector3f.dot(e.mPosition, mPlaneNormal);
			maxDist = dist + pRadius;

			fill[e.mIndex] = t;
			final Vector3f oldpos = e.mPosition;
			for (++i; i < mPositions.size() && e.mDistance < maxDist 
				&& Vector3f.distanceSquare(mPositions.get(i).mPosition, oldpos) < pSquared; ++i) 
			{
				fill[mPositions.get(i).mIndex] = t;
			}
			++t;
		}
		
		return t;
	}
	
	protected static class Entry implements Comparable<Entry>{
		public int mIndex; ///< The vertex referred by this entry
		public Vector3f mPosition; ///< Position
		public float mDistance; ///< Distance of this vertex to the sorting plane

		public Entry() { /** intentionally not initialized.*/ }
		public Entry(int pIndex, Vector3f pPosition, float pDistance){
			mIndex = pIndex;
			mPosition = pPosition;
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
