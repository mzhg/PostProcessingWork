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

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Postprocessing filter to split meshes with many bones into submeshes
 * so that each submesh has a certain max bone count.<p>
 *
 * Applied BEFORE the JoinVertices-Step occurs.<br>
 * Returns NON-UNIQUE vertices, splits by bone count.
*/
public class SplitByBoneCountProcess extends BaseProcess implements PostProcessSteps{
	
	/** Max bone count. Splitting occurs if a mesh has more than that number of bones. */
	public int mMaxBoneCount = AssimpConfig.AI_SBBC_DEFAULT_MAX_BONES;
	
	/** Per mesh index: Array of indices of the new submeshes. */
	public IntList[] mSubMeshIndices;
	
	/** Returns whether the processing step is present in the given flag.
	* @param pFlags The processing flags the importer was called with. A
 	*   bitwise combination of #aiPostProcessSteps.
	* @return true if the process is present in this flag fields, 
 	*   false if not.
	*/
	public boolean isActive(int pFlags){
		return (pFlags & aiProcess_SplitByBoneCount) != 0;
	}

	/** Called prior to ExecuteOnScene().<p>
	* The function is a request to the process to update its configuration
	* basing on the Importer's configuration property list.
	*/
	public void setupProperties(Importer pImp){
		mMaxBoneCount = pImp.getPropertyInteger(AssimpConfig.AI_CONFIG_PP_SBBC_MAX_BONES,AssimpConfig.AI_SBBC_DEFAULT_MAX_BONES);
	}
	
	/** Executes the post processing step on the given imported data.<br>
	* At the moment a process is not supposed to fail.
	* @param pScene The imported data to work at.
	*/
	public void execute(Scene pScene){
		if(DefaultLogger.LOG_OUT)
			DefaultLogger.debug("SplitByBoneCountProcess begin");

		// early out 
		boolean isNecessary = false;
		for( int a = 0; a < pScene.getNumMeshes(); ++a)
			if( pScene.mMeshes[a].getNumBones() > mMaxBoneCount )
				isNecessary = true;

		if( !isNecessary )
		{
			if(DefaultLogger.LOG_OUT)
				DefaultLogger.debug(String.format( "SplitByBoneCountProcess early-out: no meshes with more than %d bones.",mMaxBoneCount));
			return;
		}

		// we need to do something. Let's go.
//		mSubMeshIndices.clear();
//		mSubMeshIndices.resize( pScene->mNumMeshes);
		mSubMeshIndices = new IntList[pScene.getNumMeshes()];

		// build a new array of meshes for the scene
		List<Mesh> meshes = new ArrayList<Mesh>();

		for( int a = 0; a < pScene.getNumMeshes(); ++a)
		{
			Mesh srcMesh = pScene.mMeshes[a];

			List<Mesh> newMeshes = new ArrayList<Mesh>();
			splitMesh( pScene.mMeshes[a], newMeshes);

			// mesh was split
			if( !newMeshes.isEmpty() )
			{
				// store new meshes and indices of the new meshes
				for( int b = 0; b < newMeshes.size(); ++b)
				{
					mSubMeshIndices[a].add( meshes.size());
					meshes.add( newMeshes.get(b));
				}

				// and destroy the source mesh. It should be completely contained inside the new submeshes
//				delete srcMesh;
			}
			else
			{
				// Mesh is kept unchanged - store it's new place in the mesh array
				mSubMeshIndices[a].add( meshes.size());
				meshes.add( srcMesh);
			}
		}

		// rebuild the scene's mesh array
		int mNumMeshes = meshes.size();
//		delete [] pScene->mMeshes;
		pScene.mMeshes = new Mesh[mNumMeshes];
//		std::copy( meshes.begin(), meshes.end(), pScene->mMeshes);
		meshes.toArray(pScene.mMeshes);

		// recurse through all nodes and translate the node's mesh indices to fit the new mesh array
		updateNode( pScene.mRootNode);

		if(DefaultLogger.LOG_OUT)
			DefaultLogger.debug(String.format( "SplitByBoneCountProcess end: split %d meshes into %d submeshes.", mSubMeshIndices.length, meshes.size()));
	}

	/// Splits the given mesh by bone count.
	/// @param pMesh the Mesh to split. Is not changed at all, but might be superfluous in case it was split.
	/// @param poNewMeshes Array of submeshes created in the process. Empty if splitting was not necessary.
	public void splitMesh(Mesh pMesh, List<Mesh> poNewMeshes){
		// skip if not necessary
		if( pMesh.getNumBones() <= mMaxBoneCount )
			return;

		// necessary optimisation: build a list of all affecting bones for each vertex
		// TODO: (thom) maybe add a custom allocator here to avoid allocating tens of thousands of small arrays
//		typedef std::pair<size_t, float> BoneWeight;
//		std::vector< std::vector<BoneWeight> > vertexBones( pMesh->mNumVertices);
		@SuppressWarnings("unchecked")
		List<BoneWeight>[] vertexBones = new List[pMesh.mNumVertices];
		for( int a = 0; a < pMesh.getNumBones(); ++a)
		{
			Bone bone = pMesh.mBones[a];
			for( int b = 0; b < bone.getNumWeights(); ++b){
//				vertexBones[ bone->mWeights[b].mVertexId ].push_back( BoneWeight( a, bone->mWeights[b].mWeight));
				List<BoneWeight> list = vertexBones[bone.mWeights[b].mVertexId];
				if(list == null){
					vertexBones[bone.mWeights[b].mVertexId] = list = new ArrayList<>();
				}
				list.add(new BoneWeight( a, bone.mWeights[b].mWeight));
			}
		}

		int numFacesHandled = 0;
//		std::vector<bool> isFaceHandled( pMesh->mNumFaces, false);
		boolean[] isFaceHandled = new boolean[pMesh.getNumFaces()];
		while( numFacesHandled < pMesh.getNumFaces())
		{
			// which bones are used in the current submesh
			int numBones = 0;
//			std::vector<bool> isBoneUsed( pMesh->mNumBones, false);
			boolean[] isBoneUsed = new boolean[pMesh.getNumBones()];
			// indices of the faces which are going to go into this submesh
//			std::vector<size_t> subMeshFaces;
//			subMeshFaces.reserve( pMesh->mNumFaces);
			IntList subMeshFaces = new IntArrayList(pMesh.getNumFaces());
			// accumulated vertex count of all the faces in this submesh
			int numSubMeshVertices = 0;
			// a small local array of new bones for the current face. State of all used bones for that face
			// can only be updated AFTER the face is completely analysed. Thanks to imre for the fix.
			IntList newBonesAtCurrentFace = new IntArrayList();

			// add faces to the new submesh as long as all bones affecting the faces' vertices fit in the limit
			for( int a = 0; a < pMesh.getNumFaces(); ++a)
			{
				// skip if the face is already stored in a submesh
				if( isFaceHandled[a] )
					continue;

				Face face = pMesh.mFaces[a];
				// check every vertex if its bones would still fit into the current submesh
				for( int b = 0; b < face.getNumIndices(); ++b )
				{
//					const std::vector<BoneWeight>& vb = vertexBones[face.mIndices[b]];
					List<BoneWeight> vb = vertexBones[face.get(b)];
					for( int c = 0; c < vb.size(); ++c)
					{
						int boneIndex = vb.get(c).first;
						// if the bone is already used in this submesh, it's ok
						if( isBoneUsed[boneIndex] )
							continue;

						// if it's not used, yet, we would need to add it. Store its bone index
//						if( std::find( newBonesAtCurrentFace.begin(), newBonesAtCurrentFace.end(), boneIndex) == newBonesAtCurrentFace.end() )
//							newBonesAtCurrentFace.push_back( boneIndex);
						if(!newBonesAtCurrentFace.contains(boneIndex))
							newBonesAtCurrentFace.add(boneIndex);
					}
				}

				// leave out the face if the new bones required for this face don't fit the bone count limit anymore
				if( numBones + newBonesAtCurrentFace.size() > mMaxBoneCount )
					continue;

				// mark all new bones as necessary
				while( !newBonesAtCurrentFace.isEmpty() )
				{
					int newIndex = newBonesAtCurrentFace.removeInt(newBonesAtCurrentFace.size() - 1);
					//newBonesAtCurrentFace.pop_back(); // this also avoids the deallocation which comes with a clear()
					if( isBoneUsed[newIndex] ) 
						continue;

					isBoneUsed[newIndex] = true;
					numBones++;
				}

				// store the face index and the vertex count
				subMeshFaces.add( a);
				numSubMeshVertices += face.getNumIndices();

				// remember that this face is handled
				isFaceHandled[a] = true;
				numFacesHandled++;
			}

			// create a new mesh to hold this subset of the source mesh
			Mesh newMesh = new Mesh();
			if( pMesh.mName.length() > 0 )
				newMesh.mName =  String.format( "%s_sub%d", pMesh.mName, poNewMeshes.size());
			newMesh.mMaterialIndex = pMesh.mMaterialIndex;
			newMesh.mPrimitiveTypes = pMesh.mPrimitiveTypes;
			poNewMeshes.add( newMesh);

			// create all the arrays for this mesh if the old mesh contained them
			newMesh.mNumVertices = numSubMeshVertices;
//			newMesh.mNumFaces = subMeshFaces.size();
			newMesh.mVertices = MemoryUtil.createFloatBuffer(newMesh.mNumVertices * 3, AssimpConfig.MESH_USE_NATIVE_MEMORY);  //new aiVector3D[newMesh->mNumVertices];
			if( pMesh.hasNormals() )
				newMesh.mNormals = MemoryUtil.createFloatBuffer(newMesh.mNumVertices * 3, AssimpConfig.MESH_USE_NATIVE_MEMORY);  //new aiVector3D[newMesh->mNumVertices];
			if( pMesh.hasTangentsAndBitangents() )
			{
				newMesh.mTangents = MemoryUtil.createFloatBuffer(newMesh.mNumVertices * 3, AssimpConfig.MESH_USE_NATIVE_MEMORY);
				newMesh.mBitangents = MemoryUtil.createFloatBuffer(newMesh.mNumVertices * 3, AssimpConfig.MESH_USE_NATIVE_MEMORY);
			}
			for( int a = 0; a < Mesh.AI_MAX_NUMBER_OF_TEXTURECOORDS; ++a )
			{
				if( pMesh.hasTextureCoords( a) )
					newMesh.mTextureCoords[a] = MemoryUtil.createFloatBuffer(newMesh.mNumVertices * 3, AssimpConfig.MESH_USE_NATIVE_MEMORY);
				newMesh.mNumUVComponents[a] = pMesh.mNumUVComponents[a];
			}
			for( int a = 0; a < Mesh.AI_MAX_NUMBER_OF_COLOR_SETS; ++a )
			{
				if( pMesh.hasVertexColors( a) )
					newMesh.mColors[a] = MemoryUtil.createFloatBuffer(newMesh.mNumVertices * 4, AssimpConfig.MESH_USE_NATIVE_MEMORY);
			}

			// and copy over the data, generating faces with linear indices along the way
			newMesh.mFaces = new Face[subMeshFaces.size()];
			int nvi = 0; // next vertex index
			//std::vector<size_t> previousVertexIndices( numSubMeshVertices, std::numeric_limits<size_t>::max()); // per new vertex: its index in the source mesh
			int[] previousVertexIndices = new int[numSubMeshVertices];
			Arrays.fill(previousVertexIndices, -1);
			for( int a = 0; a < subMeshFaces.size(); ++a )
			{
				Face srcFace = pMesh.mFaces[subMeshFaces.getInt(a)];
				Face dstFace = newMesh.mFaces[a] = Face.createInstance(srcFace.getNumIndices());
//				dstFace.mNumIndices = srcFace.mNumIndices;
//				dstFace.mIndices = new unsigned int[dstFace.mNumIndices];

				// accumulate linearly all the vertices of the source face
				for( int b = 0; b < dstFace.getNumIndices(); ++b )
				{
					int srcIndex = srcFace.get(b);
//					dstFace.mIndices[b] = nvi;
					dstFace.set(b, nvi);
					previousVertexIndices[nvi] = srcIndex;

//					newMesh->mVertices[nvi] = pMesh->mVertices[srcIndex];
					newMesh.mVertices.put(nvi, pMesh.mVertices.get(srcIndex));
					if( pMesh.hasNormals() )
//						newMesh->mNormals[nvi] = pMesh->mNormals[srcIndex];
						newMesh.mNormals.put(nvi, pMesh.mNormals.get(srcIndex));
					if( pMesh.hasTangentsAndBitangents() )
					{
//						newMesh->mTangents[nvi] = pMesh->mTangents[srcIndex];
//						newMesh->mBitangents[nvi] = pMesh->mBitangents[srcIndex];
						newMesh.mTangents.put(nvi, pMesh.mTangents.get(srcIndex));
						newMesh.mBitangents.put(nvi, pMesh.mBitangents.get(srcIndex));
					}
					for( int c = 0; c < Mesh.AI_MAX_NUMBER_OF_TEXTURECOORDS; ++c )
					{
						if( pMesh.hasTextureCoords( c) )
//							newMesh.mTextureCoords[c][nvi] = pMesh->mTextureCoords[c][srcIndex];
							newMesh.mTextureCoords[c].put(nvi, pMesh.mTextureCoords[c].get(srcIndex));
					}
					for( int c = 0; c < Mesh.AI_MAX_NUMBER_OF_COLOR_SETS; ++c )
					{
						if( pMesh.hasVertexColors( c) )
//							newMesh->mColors[c][nvi] = pMesh->mColors[c][srcIndex];
							newMesh.mColors[c].put(nvi, pMesh.mColors[c].get(srcIndex));
					}

					nvi++;
				}
			}

//			ai_assert( nvi == numSubMeshVertices );
			if(!( nvi == numSubMeshVertices ))
				throw new AssertionError();

			// Create the bones for the new submesh: first create the bone array
//			newMesh.mNumBones = 0;
			newMesh.mBones = new Bone[numBones];
			int[] newMeshBoneWeightCount = new int[numBones];
			int newMesh_numBones = 0;

//			std::vector<size_t> mappedBoneIndex( pMesh->mNumBones, std::numeric_limits<size_t>::max());
			int[] mappedBoneIndex = new int[pMesh.getNumBones()];
			Arrays.fill(mappedBoneIndex, -1);
			for( int a = 0; a < pMesh.getNumBones(); ++a )
			{
				if( !isBoneUsed[a] )
					continue;

				// create the new bone
				Bone srcBone = pMesh.mBones[a];
				Bone dstBone = new Bone();
				mappedBoneIndex[a] = newMesh_numBones;
				newMesh.mBones[newMesh_numBones++] = dstBone;
				dstBone.mName = srcBone.mName;
				dstBone.mOffsetMatrix.load(srcBone.mOffsetMatrix);
//				dstBone.mNumWeights = 0;
			}

//			ai_assert( newMesh->mNumBones == numBones );
			if(!( newMesh_numBones == numBones ))
				throw new AssertionError();

			// iterate over all new vertices and count which bones affected its old vertex in the source mesh
			for( int a = 0; a < numSubMeshVertices; ++a )
			{
				int oldIndex = previousVertexIndices[a];
//				const std::vector<BoneWeight>& bonesOnThisVertex = vertexBones[oldIndex];
				List<BoneWeight> bonesOnThisVertex = vertexBones[oldIndex];

				if(bonesOnThisVertex != null){
					for( int b = 0; b < bonesOnThisVertex.size(); ++b )
					{
						int newBoneIndex = mappedBoneIndex[ bonesOnThisVertex.get(b).first ];
						if( newBoneIndex != -1)
//							newMesh.mBones[newBoneIndex]->mNumWeights++;
							newMeshBoneWeightCount[newBoneIndex] ++;
					}
				}
			}

			// allocate all bone weight arrays accordingly
			for( int a = 0; a < newMesh_numBones; ++a )
			{
				Bone bone = newMesh.mBones[a];
//				ai_assert( bone->mNumWeights > 0 );
				if(!(newMeshBoneWeightCount[a] > 0))
					throw new AssertionError();
				bone.mWeights = new VertexWeight[newMeshBoneWeightCount[a]];
				AssUtil.initArray(bone.mWeights);
//				bone->mNumWeights = 0; // for counting up in the next step
				newMeshBoneWeightCount[a] = 0;
			}

			// now copy all the bone vertex weights for all the vertices which made it into the new submesh
			for( int a = 0; a < numSubMeshVertices; ++a)
			{
				// find the source vertex for it in the source mesh
				int previousIndex = previousVertexIndices[a];
				// these bones were affecting it
				List<BoneWeight> bonesOnThisVertex = vertexBones[previousIndex];
				
				// all of the bones affecting it should be present in the new submesh, or else
				// the face it comprises shouldn't be present
				if(bonesOnThisVertex != null){
					for( int b = 0; b < bonesOnThisVertex.size(); ++b)
					{
						int newBoneIndex = mappedBoneIndex[ bonesOnThisVertex.get(b).first ];
//						ai_assert( newBoneIndex != std::numeric_limits<size_t>::max() );
						if(!(newBoneIndex != -1)){
							throw new AssertionError();
						}
						
						VertexWeight dstWeight = newMesh.mBones[newBoneIndex].mWeights[newMeshBoneWeightCount[newBoneIndex]]; // + newMesh->mBones[newBoneIndex]->mNumWeights;
//						newMesh.mBones[newBoneIndex]->mNumWeights++;
						newMeshBoneWeightCount[newBoneIndex]++;
	
						dstWeight.mVertexId = a;
						dstWeight.mWeight = bonesOnThisVertex.get(b).second;
					}
				}
			}

			// I have the strange feeling that this will break apart at some point in time...
		}
	}

	/// Recursively updates the node's mesh list to account for the changed mesh list
	public void updateNode(Node pNode){
		// rebuild the node's mesh index list
		if( pNode.getNumMeshes() > 0 )
		{
//			std::vector<size_t> newMeshList;
			IntList newMeshList = new IntArrayList();
			for( int a = 0; a < pNode.getNumMeshes(); ++a)
			{
				int srcIndex = pNode.mMeshes[a];
//				const std::vector<size_t>& replaceMeshes = mSubMeshIndices[srcIndex];
//				newMeshList.insert( newMeshList.end(), replaceMeshes.begin(), replaceMeshes.end());
				IntList replaceMeshes = mSubMeshIndices[srcIndex];
				if(replaceMeshes != null){
					newMeshList.addAll(replaceMeshes);
				}
			}

//			delete pNode->mMeshes;
//			pNode->mNumMeshes = newMeshList.size();
			pNode.mMeshes = new int[newMeshList.size()];
//			std::copy( newMeshList.begin(), newMeshList.end(), pNode->mMeshes);
			newMeshList.toIntArray(pNode.mMeshes);
		}

		// do that also recursively for all children
		for( int a = 0; a < pNode.getNumChildren(); ++a )
		{
			updateNode( pNode.mChildren[a]);
		}
	}
	
	private static final class BoneWeight{
		int first;
		float second;
		public BoneWeight(int first, float second) {
			this.first = first;
			this.second = second;
		}
	}
}
