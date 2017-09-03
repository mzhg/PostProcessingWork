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

import java.util.HashMap;
import java.util.Map;

/** Helper class to evaluate subdivision surfaces. Different algorithms
 *  are provided for choice. */
public abstract class Subdivider {

	/** Enumerates all supported subvidision algorithms */
	public static final int CATMULL_CLARKE = 0x1;
	// ---------------------------------------------------------------
	/** Create a subdivider of a specific type
	 *
	 *  @param algo Algorithm to be used for subdivision
	 *  @return Subdivider instance. */
	public static Subdivider create (int algo){
		switch (algo)
		{
		case CATMULL_CLARKE:
			return new CatmullClarkSubdivider();
		};

		return null; // shouldn't happen
	}

	// ---------------------------------------------------------------
	/** Subdivide a mesh using the selected algorithm
	 *
	 *  @param mesh First mesh to be subdivided. Must be in verbose
	 *    format.
	 *  @param out Receives the output mesh, allocated by me.
	 *  @param num Number of subdivisions to perform.
	 *  @param discard_input If true is passed, the input mesh is
	 *    deleted after the subdivision is complete. This can 
	 *    improve performance because it allows the optimization
	 *    to reuse the existing mesh for intermediate results.
	 *  @pre out!=mesh*/
	public abstract void subdivide ( Mesh mesh,  Mesh out, int num, boolean discard_input);

	// ---------------------------------------------------------------
	/** Subdivide multiple meshes using the selected algorithm. This
	 *  avoids erroneous smoothing on objects consisting of multiple
	 *  per-material meshes. Usually, most 3d modellers smooth on a
	 *  per-object base, regardless the materials assigned to the
	 *  meshes.
	 *
	 *  @param smesh Array of meshes to be subdivided. Must be in
	 *    verbose format.
	 *  @param nmesh Number of meshes in smesh.
	 *  @param out Receives the output meshes. The array must be
	 *    sufficiently large (at least @c nmesh elements) and may not
	 *    overlap the input array. Output meshes map one-to-one to
	 *    their corresponding input meshes. The meshes are allocated 
	 *    by the function.
	 *  @param discard_input If true is passed, input meshes are
	 *    deleted after the subdivision is complete. This can 
	 *    improve performance because it allows the optimization
	 *    of reusing existing meshes for intermediate results.
	 *  @param num Number of subdivisions to perform.
	 *  @pre nmesh != 0, smesh and out may not overlap*/
	public abstract void subdivide (
		Mesh[] smesh, 
		int nmesh,
		Mesh[] out, 
		int num,
		boolean discard_input);
	
	/** Subdivider stub class to implement the Catmull-Clarke subdivision algorithm. The 
	 *  implementation is basing on recursive refinement. Directly evaluating the result is also
	 *  possible and much quicker, but it depends on lengthy matrix lookup tables. */
	static final class CatmullClarkSubdivider extends Subdivider{

		@Override
		public void subdivide(Mesh mesh, Mesh out, int num,boolean discard_input) {
			
		}
		
		// ---------------------------------------------------------------------------
		// Hashing function to derive an index into an #EdgeMap from two given
		// 'unsigned int' vertex coordinates (!!distinct coordinates - same 
		// vertex position == same index!!). 
		// NOTE - this leads to rare hash collisions if a) sizeof(unsigned int)>4
		// and (id[0]>2^32-1 or id[0]>2^32-1).
		// MAKE_EDGE_HASH() uses temporaries, so INIT_EDGE_HASH() needs to be put
		// at the head of every function which is about to use MAKE_EDGE_HASH().
		// Reason is that the hash is that hash construction needs to hold the
		// invariant id0<id1 to identify an edge - else two hashes would refer
		// to the same edge.
		// ---------------------------------------------------------------------------
		private static long makeEdgeHash(int id0, int id1){
			if(id0 < id1){
				int t = id0;
				id0 = id1;
				id1 = t;
			}
			
			return ((long)id0)^(((long)id1)<<32l);
		}

		@Override
		public void subdivide(Mesh[] smesh, int nmesh, Mesh[] out, int num, boolean discard_input) {
			if (num == 0) {
				// No subdivision at all. Need to copy all the meshes .. argh.
				if (discard_input) {
					for (int s = 0; s < nmesh; ++s) {
						out[s] = smesh[s];
						smesh[s] = null;
					}
				}
				else {
					for (int s = 0; s < nmesh; ++s) {
//						SceneCombiner::Copy(out+s,smesh[s]);
						out[s] = smesh[s].copy();
					}
				}
				return;
			}
			
			Mesh[] inmeshes = new Mesh[nmesh];
			Mesh[] outmeshes = new Mesh[nmesh];
			int[] maptbl = new int[nmesh];
			int count = 0;
			
			// Remove pure line and point meshes from the working set to reduce the 
			// number of edge cases the subdivider is forced to deal with. Line and 
			// point meshes are simply passed through.
			for (int s = 0; s < nmesh; ++s) {
				Mesh i = smesh[s];
				// FIX - mPrimitiveTypes might not yet be initialized
				if (i.mPrimitiveTypes != 0 && (i.mPrimitiveTypes & (Mesh.aiPrimitiveType_LINE|Mesh.aiPrimitiveType_POINT))==i.mPrimitiveTypes) {
					if(DefaultLogger.LOG_OUT)
						DefaultLogger.debug("Catmull-Clark Subdivider: Skipping pure line/point mesh");

					if (discard_input) {
						out[s] = i;
						smesh[s] = null;
					}
					else {
//						SceneCombiner::Copy(out+s,i);
						out[s] = i.copy();
					}
					continue;
				}

//				outmeshes.push_back(NULL);inmeshes.push_back(i);
//				maptbl.push_back(s);
				outmeshes[count] = null;
				inmeshes[count] = i;
				maptbl[count] = s;
				count++;
			}
			
			// Do the actual subdivision on the preallocated storage. InternSubdivide 
			// *always* assumes that enough storage is available, it does not bother
			// checking any ranges.
//			ai_assert(inmeshes.size()==outmeshes.size()&&inmeshes.size()==maptbl.size());
			if (count == 0) {
				if(DefaultLogger.LOG_OUT)
					DefaultLogger.warn("Catmull-Clark Subdivider: Pure point/line scene, I can't do anything");
				return;
			}
			internSubdivide(inmeshes,count,outmeshes,num);
			for (int i = 0; i < count; ++i) {
//				ai_assert(outmeshes[i]);
				out[maptbl[i]] = outmeshes[i];
			}

//			if (discard_input) {
//				for (size_t s = 0; s < nmesh; ++s) {
//					delete smesh[s];
//				}
//			}
		}
		
		// ------------------------------------------------------------------------------------------------
		// Note - this is an implementation of the standard (recursive) Cm-Cl algorithm without further 
		// optimizations (except we're using some nice LUTs). A description of the algorithm can be found
		// here: http://en.wikipedia.org/wiki/Catmull-Clark_subdivision_surface
		//
		// The code is mostly O(n), however parts are O(nlogn) which is therefore the algorithm's
		// expected total runtime complexity. The implementation is able to work in-place on the same
		// mesh arrays. Calling #InternSubdivide() directly is not encouraged. The code can operate
		// in-place unless 'smesh' and 'out' are equal (no strange overlaps or reorderings). 
		// Previous data is replaced/deleted then.
		// ------------------------------------------------------------------------------------------------
		void internSubdivide (Mesh[] smesh, int nmesh, Mesh[] out, int num){
			int eh_tmp0__, eh_tmp1__;
			
			int[] maptbl;
			SpatialSort spatial = new SpatialSort();
			
			// ---------------------------------------------------------------------
			// 0. Offset table to index all meshes continuously, generate a spatially
			// sorted representation of all vertices in all meshes.
			// ---------------------------------------------------------------------
//			List<IntPair> moffsets = new ArrayList<>(nmesh);
			IntPair[] moffsets = new IntPair[nmesh];
			
			int totfaces = 0, totvert = 0;
			for (int t = 0; t < nmesh; ++t) {
				Mesh mesh = smesh[t];

				spatial.append(mesh.mVertices,mesh.mNumVertices,/*sizeof(aiVector3D)*/12,false);
				moffsets[t] = new IntPair(totfaces,totvert);

				totfaces += mesh.getNumFaces();
				totvert  += mesh.mNumVertices;
			}

			spatial.finalise();
			maptbl =new int[spatial.size()];
			int num_unique = spatial.generateMappingTable(maptbl,ProcessHelper.computePositionEpsilon(smesh,nmesh));
			
			// ---------------------------------------------------------------------
			// 1. Compute the centroid point for all faces
			// ---------------------------------------------------------------------
			Vertex[] centroids = new Vertex[totfaces];
			int nfacesout = 0;
			Vertex tmp = new Vertex();
			for (int t = 0, n = 0; t < nmesh; ++t) {
				Mesh mesh = smesh[t];
				for (int i = 0; i < mesh.getNumFaces();++i,++n)
				{
					Face face = mesh.mFaces[i];
					Vertex c = centroids[n];
					if(c == null){
						c = centroids[n] = new Vertex();
					}

					for (int a = 0; a < face.getNumIndices();++a) {
//						c += Vertex(mesh,face.get(a));
						tmp.assign(mesh, face.get(a));
						Vertex.add(c, tmp, c);
					}

//					c /= static_cast<float>(face.mNumIndices);
					Vertex.div(c, face.getNumIndices(), c);
					nfacesout += face.getNumIndices();
				}
			}
			
			Map<Long, Edge> edges = new HashMap<Long, Subdivider.Edge>();

			// ---------------------------------------------------------------------
			// 2. Set each edge point to be the average of all neighbouring 
			// face points and original points. Every edge exists twice
			// if there is a neighboring face.
			// ---------------------------------------------------------------------
			for (int t = 0; t < nmesh; ++t) {
				Mesh mesh = smesh[t];

				for (int i = 0; i < mesh.getNumFaces();++i)	{
					Face face = mesh.mFaces[i];

					for (int p =0; p< face.getNumIndices(); ++p) {
						final int id[] = { 
							face.get(p), 
							face.get(p==face.getNumIndices()-1?0:p+1)
						};
						final  int mp[] = { 
							maptbl[moffsets[t].second+id[0]], // moffsets[t].second+id[0]
							maptbl[moffsets[t].second+id[1]]
						};

						Long key = makeEdgeHash(mp[0],mp[1]);
						Edge e = edges.get(key);
						if(e == null){
							edges.put(key, e = new Edge());
						}
						e.ref++;
						if (e.ref<=2) {
							if (e.ref==1) { // original points (end points) - add only once
//								e.edge_point = e.midpoint = Vertex(mesh,id[0])+Vertex(mesh,id[1]);
//								e.midpoint *= 0.5f;
								
								Vertex a = e.edge_point;
								Vertex b = e.midpoint;
								
								if(a == null){
									a = new Vertex(mesh, id[0]);
									b = new Vertex(mesh, id[1]);
								}else{
									a.assign(mesh, id[0]);
									b.assign(mesh, id[1]);
								}
								
								Vertex.add(a, b, a);
								Vertex.div(a, 2, b);
								
								if(e.edge_point == null){
									e.edge_point = a;
									e.midpoint = b;
								}
							}
//							e.edge_point += centroids[FLATTEN_FACE_IDX(t,i)];
							Vertex c = centroids[moffsets[t].first+i];
							if(c != null)
								Vertex.add(e.edge_point, c, e.edge_point);
						}
					}
				}
			}
			
			// ---------------------------------------------------------------------
			// 3. Normalize edge points
			// ---------------------------------------------------------------------
			{int bad_cnt = 0;
			for (Edge it : edges.values()) {
				if (it.ref < 2) {
//					ai_assert(it.ref);
					if(it.ref != 1){
						throw new AssertionError();
					}
					++bad_cnt;
				}
//				it.edge_point *= 1.f/((*it).second.ref+2.f);
				Vertex.mul(it.edge_point, 1.0f/(it.ref + 2.0f), it.edge_point);
			}

			if (bad_cnt!=0) {
				// Report the number of bad edges. bad edges are referenced by less than two
				// faces in the mesh. They occur at outer model boundaries in non-closed
				// shapes.
//				char tmp[512];
//				sprintf(tmp,"Catmull-Clark Subdivider: got %u bad edges touching only one face (totally %u edges). ",
//					bad_cnt,static_cast<unsigned int>(edges.size()));
//
//				DefaultLogger::get().debug(tmp);
				
				if(DefaultLogger.LOG_OUT){
					String print = String.format("Catmull-Clark Subdivider: got %d bad edges touching only one face (totally %d edges). ",
					bad_cnt,edges.size());
					DefaultLogger.debug(print);
				}
			}}
			
			// ---------------------------------------------------------------------
			// 4. Compute a vertex-face adjacency table. We can't reuse the code
			// from VertexTriangleAdjacency because we need the table for multiple
			// meshes and out vertex indices need to be mapped to distinct values 
			// first.
			// ---------------------------------------------------------------------
//			UIntVector faceadjac(nfacesout), cntadjfac(maptbl.size(),0), ofsadjvec(maptbl.size()+1,0); 
			int[] faceadjac = new int[nfacesout];
			int[] cntadjfac = new int[maptbl.length];
			int[] ofsadjvec = new int[maptbl.length + 1];
			{
			for (int t = 0; t < nmesh; ++t) {
				Mesh minp = smesh[t];
				for (int i = 0; i < minp.getNumFaces(); ++i) {
					
					Face f = minp.mFaces[i];
					for (int n = 0; n < f.getNumIndices(); ++n) {
						++cntadjfac[maptbl[moffsets[t].second+f.get(n)]];  
					}
				}
			}
			int cur = 0;
			for (int i = 0; i < cntadjfac.length; ++i) {
				ofsadjvec[i+1] = cur;
				cur += cntadjfac[i];
			}
			for (int t = 0; t < nmesh; ++t) {
				Mesh minp = smesh[t];
				for (int i = 0; i < minp.getNumFaces(); ++i) {
					Face f = minp.mFaces[i];
					for (int n = 0; n < f.getNumIndices(); ++n) {
						faceadjac[ofsadjvec[1+maptbl[moffsets[t].second + f.get(n)]]++] = (moffsets[t].first + i);
					}
				}
			}
			
			if(AssimpConfig.ASSIMP_BUILD_DEBUG){
				for (int t = 0; t < ofsadjvec.length-1; ++t) {
					for (int m = 0; m <  cntadjfac[t]; ++m) {
						final int fidx = faceadjac[ofsadjvec[t]+m];
						if(!(fidx < totfaces))
							throw new AssertionError("fidx >= totfaces");
						for (int n = 1; n < nmesh; ++n) {
							if (moffsets[n].first > fidx) {
								Mesh msh = smesh[--n];
								Face f = msh.mFaces[fidx-moffsets[n].first];
								
								boolean haveit = false;
								for (int i = 0; i < f.getNumIndices(); ++i) {
									if (maptbl[moffsets[n].second + f.get(i)]==t) {
										haveit = true; break;
									}
								}
								if(!haveit)
									throw new AssertionError("haveit is false");
								break;
							}
						}
					}
				}
			}
			
			}
			
			TouchedOVertex[] new_points = new TouchedOVertex[num_unique];
			// ---------------------------------------------------------------------
			// 5. Spawn a quad from each face point to the corresponding edge points
			// the original points being the fourth quad points.
			// ---------------------------------------------------------------------
			boolean natived = AssimpConfig.MESH_USE_NATIVE_MEMORY;
			for (int t = 0; t < nmesh; ++t) {
				Mesh minp = smesh[t];
				Mesh mout = out[t] = new Mesh();

				int out_numFaces = 0;
				for (int a  = 0; a < minp.getNumFaces(); ++a) {
					out_numFaces += minp.mFaces[a].getNumIndices();
				}

				// We need random access to the old face buffer, so reuse is not possible.
				mout.mFaces = new Face[out_numFaces];

				mout.mNumVertices = out_numFaces*4;
				mout.mVertices = MemoryUtil.createFloatBuffer(mout.mNumVertices * 3, natived);

				// quads only, keep material index
				mout.mPrimitiveTypes = Mesh.aiPrimitiveType_POLYGON;
				mout.mMaterialIndex = minp.mMaterialIndex;

				if (minp.hasNormals()) {
					mout.mNormals = MemoryUtil.createFloatBuffer(mout.mNumVertices * 3, natived);
				}

				if (minp.hasTangentsAndBitangents()) {
					mout.mTangents = MemoryUtil.createFloatBuffer(mout.mNumVertices * 3, natived);
					mout.mBitangents = MemoryUtil.createFloatBuffer(mout.mNumVertices * 3, natived);
				}

				for(int i = 0; minp.hasTextureCoords(i); ++i) {
					mout.mTextureCoords[i] = MemoryUtil.createFloatBuffer(mout.mNumVertices * 3, natived);
					mout.mNumUVComponents[i] = minp.mNumUVComponents[i];
				}

				for(int i = 0; minp.hasVertexColors(i); ++i) {
					mout.mColors[i] = MemoryUtil.createFloatBuffer(mout.mNumVertices * 4, natived);
				}

				mout.mNumVertices = out_numFaces<<2;
				for (int i = 0, v = 0, n = 0; i < minp.getNumFaces();++i)	{

					Face face = minp.mFaces[i];
					for (int a = 0; a < face.getNumIndices();++a)	{

						// Get a clean new face.
						Face faceOut = mout.mFaces[n++];
						if(faceOut == null){
							faceOut = mout.mFaces[n-1] = Face.createInstance(4);
						}
						
//						faceOut.mIndices = new int [faceOut.mNumIndices = 4];

						// Spawn a new quadrilateral (ccw winding) for this original point between:
						// a) face centroid
						centroids[moffsets[t].first + i].sortBack(mout,v); faceOut.set(0,v++);

						// b) adjacent edge on the left, seen from the centroid
						Edge e0 = edges.get(makeEdgeHash(maptbl[moffsets[t].second + face.get(a)],
							maptbl[moffsets[t].second + face.get(a==face.getNumIndices()-1?0:a+1)
							]));  // fixme: replace with mod face.mNumIndices? 

						// c) adjacent edge on the right, seen from the centroid
						Edge e1 = edges.get(makeEdgeHash(maptbl[moffsets[t].second + face.get(a)],
							maptbl[moffsets[t].second + face.get(a == 0 ?face.getNumIndices()-1:a-1)
							]));  // fixme: replace with mod face.mNumIndices? 

						e0.edge_point.sortBack(mout,v); faceOut.set(3, v++);
						e1.edge_point.sortBack(mout,v); faceOut.set(1, v++);

						// d= original point P with distinct index i
						// F := 0
						// R := 0
						// n := 0
						// for each face f containing i
						//    F := F+ centroid of f
						//    R := R+ midpoint of edge of f from i to i+1
						//    n := n+1
						//
						// (F+2R+(n-3)P)/n
						final int org = maptbl[moffsets[t].second + face.get(a)];
						TouchedOVertex ov = new_points[org];
						if(ov == null)
							ov = new_points[org] = new TouchedOVertex(false, null);

						if (!ov.first) {
							ov.first = true;

							int adj[] = faceadjac;
							int adj_index;
							int cnt;
//							GET_ADJACENT_FACES_AND_CNT(org,adj,cnt);
//							adj = faceadjac[ofsadjvec[org]];
							adj_index = ofsadjvec[org];
							cnt = cntadjfac[org];

							if (cnt < 3) {
//								ov.second =new Vertex(minp,face.get(a));
								if(ov.second != null)
									ov.second.assign(minp, face.get(a));
								else
									ov.second =new Vertex(minp,face.get(a));
							}
							else {

								Vertex F = new Vertex(),R = new Vertex();
								for (int o = 0; o < cnt; ++o) {
									if(!(adj[adj_index + o] < totfaces))
										throw new AssertionError("adj[adj_index + o] >= totfaces");
//									F += centroids[adj[o]];
									Vertex.add(F, centroids[adj[o]], F);

									// adj[0] is a global face index - search the face in the mesh list
									Mesh mp = null;
									int nidx;

									if (adj[o] < moffsets[0].first) {
										mp = smesh[nidx=0];
									}
									else {
										for (nidx = 1; nidx<= nmesh; ++nidx) {
											if (nidx == nmesh ||moffsets[nidx].first > adj[o]) {
												mp = smesh[--nidx];
												break;
											}
										}
									}

//									ai_assert(adj[o]-moffsets[nidx].first < mp.mNumFaces);
									if(!(adj[o]-moffsets[nidx].first < mp.getNumFaces())){
										throw new AssertionError();
									}
									Face f = mp.mFaces[adj[o]-moffsets[nidx].first];
									boolean haveit = false;

									// find our original point in the face
									for (int m = 0; m < f.getNumIndices(); ++m) {
										if (maptbl[moffsets[nidx].second + f.get(m)] == org) {

											// add *both* edges. this way, we can be sure that we add
											// *all* adjacent edges to R. In a closed shape, every
											// edge is added twice - so we simply leave out the
											// factor 2.f in the amove formula and get the right
											// result.

											Edge c0 = edges.get(makeEdgeHash(org,maptbl[moffsets[nidx].second + f.get(m ==0?f.getNumIndices()-1:m-1)]));
											// fixme: replace with mod face.mNumIndices? 

											Edge c1 = edges.get(makeEdgeHash(org,maptbl[moffsets[nidx].second + f.get(m==f.getNumIndices()-1?0:m+1)]));
											// fixme: replace with mod face.mNumIndices? 
//											R += c0.midpoint+c1.midpoint;
											Vertex.add(R, c0.midpoint, R);
											Vertex.add(R, c1.midpoint, R);

											haveit = true;
											break;
										}
									}

									// this invariant *must* hold if the vertex-to-face adjacency table is valid
									if(!haveit)
										throw new AssertionError();
								}

								final float div = cnt, divsq = 1.f/(div*div);
								Vertex ve = ov.second;
								if(ve == null){
									ve = new Vertex(minp,face.get(a));
								}
								
								Vertex.mul(R, divsq, R);
								Vertex.mul(F, divsq, F);
								Vertex.add(ve, R, ve);
								Vertex.add(ve, F, ve);
//								ov.second = new Vertex()*((div-3.f) / div) + R*divsq + F*divsq;
								ov.second = ve;
							}
						}
						ov.second.sortBack(mout,v); face.set(2, v ++);
					}
				}
			}

			// ---------------------------------------------------------------------
			// 7. Apply the next subdivision step. 
			// ---------------------------------------------------------------------
			if (num != 1) {
//				std::vector<aiMesh*> tmp(nmesh);
				Mesh[] mtmp = new Mesh[nmesh];
				internSubdivide (out,nmesh,mtmp,num-1);
				for (int i = 0; i < nmesh; ++i) {
					out[i] = mtmp[i];
				}
			}
		}
	}
	
	private static final class TouchedOVertex{
		boolean first;
		Vertex second;
		public TouchedOVertex(boolean first, Vertex second) {
			this.first = first;
			this.second = second;
		}
	}
	
	private static final class Edge{
		Vertex edge_point, midpoint;
		int ref;
		public Edge() {}
	}
}
