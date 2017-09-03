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

/** The VertexTriangleAdjacency class computes a vertex-triangle
 *  adjacency map from a given index buffer.
 *
 *  @note Although it is called #VertexTriangleAdjacency, the current version does also
 *    support arbitrary polygons. */
public class VertexTriangleAdjacency {

	/** Offset table */
	public int[] mOffsetTable;

	/** Adjacency table */
	public int[] mAdjacencyTable;

	/** Table containing the number of referenced triangles per vertex */
	public int[] mLiveTriangles;

	/** Debug: Number of referenced vertices */
	public int iNumVertices;
	
	// ----------------------------------------------------------------------------
	/** Construction from an existing index buffer
	 *  @param pcFaces Index buffer
	 *  @param iNumFaces Number of faces in the buffer
	 *  @param iNumVertices Number of referenced vertices. This value
	 *    is computed automatically if 0 is specified.
	 *  @param bComputeNumTriangles If you want the class to compute
	 *    a list containing the number of referenced triangles per vertex
	 *    per vertex - pass true.  */
	public VertexTriangleAdjacency(Face[] pcFaces, int iNumFaces, int iNumVertices, boolean bComputeNumTriangles){
		// compute the number of referenced vertices if it wasn't specified by the caller
		if (iNumVertices == 0)	{
			for (Face pcFace : pcFaces)	{
//				ai_assert(3 == pcFace.mNumIndices);
				iNumVertices = Math.max(iNumVertices,pcFace.get(0));
				iNumVertices = Math.max(iNumVertices,pcFace.get(1));
				iNumVertices = Math.max(iNumVertices,pcFace.get(2));
			}
		}

		this.iNumVertices = iNumVertices;

		int[] pi;
		int pi_index;
		int offset_index = 1;  // index for moffsetTable Array.

		// allocate storage
		if (bComputeNumTriangles)	{
			pi = mLiveTriangles = new int[iNumVertices+1];
//			memset(mLiveTriangles,0,sizeof(unsigned int)*(iNumVertices+1));
			mOffsetTable = new int[iNumVertices+2]/*+1*/;
			pi_index = 0;
		}
		else {
			pi = mOffsetTable = new int[iNumVertices+2]/*+1*/;
			pi_index = 1;
//			memset(mOffsetTable,0,sizeof(unsigned int)*(iNumVertices+1));
			mLiveTriangles = null; // important, otherwise the d'tor would crash
		}

		// get a pointer to the end of the buffer
		int piEnd = pi_index+iNumVertices;
//		*piEnd++ = 0u;
		pi[piEnd] = 0;

		// first pass: compute the number of faces referencing each vertex
		for (Face pcFace : pcFaces)
		{
			pi[pcFace.get(0) + pi_index]++;	
			pi[pcFace.get(1) + pi_index]++;	
			pi[pcFace.get(2) + pi_index]++;	
		}

		// second pass: compute the final offset table
		int iSum = 0;
		int piCurOut = offset_index;
		for (int piCur = pi_index; piCur != piEnd;++piCur,++piCurOut)	{

			int iLastSum = iSum;
			iSum += pi[piCur]; 
			mOffsetTable[piCurOut] = iLastSum;
		}
		pi = this.mOffsetTable;

		// third pass: compute the final table
		this.mAdjacencyTable = new int[iSum];
		iSum = 0;
		for (int i = 0; i< pcFaces.length; ++i,++iSum)	{
			Face pcFace = pcFaces[i];
			int idx = pcFace.get(0);
			mAdjacencyTable[pi[idx + 1]++] = iSum;

			idx = pcFace.get(1);
			mAdjacencyTable[pi[idx + 1]++] = iSum;

			idx = pcFace.get(2);
			mAdjacencyTable[pi[idx + 1]++] = iSum;
		}
		// fourth pass: undo the offset computations made during the third pass
		// We could do this in a separate buffer, but this would be TIMES slower.
//		--mOffsetTable;
//		*mOffsetTable = 0u;
		mOffsetTable[0] = 0;
	}
	
	// ----------------------------------------------------------------------------
	/** @brief Get all triangles adjacent to a vertex
	 *  @param iVertIndex Index of the vertex
	 *  @return A pointer to the adjacency list. */
	public int getAdjacentTriangles(int iVertIndex){
//		ai_assert(iVertIndex < iNumVertices);
		return mAdjacencyTable[ mOffsetTable[iVertIndex]];
	}
	
	public void setAdjacentTriangles(int iVertIndex, int value){
//		ai_assert(iVertIndex < iNumVertices);
		mAdjacencyTable[ mOffsetTable[iVertIndex]] = value;
	}


	// ----------------------------------------------------------------------------
	/** @brief Get the number of triangles that are referenced by
	 *    a vertex. This function returns a reference that can be modified
	 *  @param iVertIndex Index of the vertex
	 *  @return Number of referenced triangles */
	public int getNumTrianglesPtr(int iVertIndex){
//		ai_assert(iVertIndex < iNumVertices && null != mLiveTriangles);
		return mLiveTriangles[iVertIndex];
	}
}
