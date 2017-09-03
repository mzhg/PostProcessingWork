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
import java.util.List;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

/** 
 * This little helper class constructs a dummy mesh for a given scene
 * the resembles the node hierarchy. This is useful for file formats
 * that don't carry any mesh data but only animation data.
 */
public class SkeletonMeshBuilder {

	/** space to assemble the mesh data: points */
	protected FloatBuffer mVertices;
	
	protected final List<TriangleFace> mFaces = new ArrayList<TriangleFace>();
	protected final List<Bone> mBones = new ArrayList<Bone>();
	protected boolean mKnobsOnly;
	
	/** The constructor processes the given scene and adds a mesh there. <p>
	 *
	 * Does nothing if the scene already has mesh data. 
	 * @param pScene The scene for which a skeleton mesh should be constructed.
	 * @param root The node to start with. NULL is the scene root
	 * @param bKnobsOnly Set this to true if you don't want the connectors
	 *   between the knobs representing the nodes.
	 */
	public SkeletonMeshBuilder(Scene pScene, Node root, boolean bKnobsOnly) {
		// nothing to do if there's mesh data already present at the scene
		if( pScene.getNumMeshes() > 0 || pScene.mRootNode == null)
			return;

		if (root == null)
			root = pScene.mRootNode;

		mKnobsOnly = bKnobsOnly;

		// allocate memory for vertices
		allocate(root);
		
		// build some faces around each node
		createGeometry( root );

		// create a mesh to hold all the generated faces
//		pScene->mNumMeshes = 1;
		pScene.mMeshes = new Mesh[1];
		pScene.mMeshes[0] = createMesh();
		// and install it at the root node
//		root->mNumMeshes = 1;
		root.mMeshes = new int[1];
		root.mMeshes[0] = 0;

		// create a dummy material for the mesh
//		pScene->mNumMaterials = 1;
		pScene.mMaterials = new Material[1];
		pScene.mMaterials[0] = createMaterial();
	}
	
	protected void allocate(Node root) {
		// TODO
	}

	public int getNumVertices() { return mVertices != null ? mVertices.remaining()/3 : 0;}
	public int getBufferLength() { return mVertices != null ? mVertices.remaining() : 0;}
	int getCapacity() { return mVertices != null ? mVertices.capacity() : 0;}

	// -------------------------------------------------------------------
	/** Recursively builds a simple mesh representation for the given node
	 * and also creates a joint for the node that affects this part of 
	 * the mesh.
	 * @param pNode The node to build geometry for.
	 */
	protected void createGeometry(Node pNode){
		// add a joint entry for the node. 
		int vertexStartIndex = getNumVertices();
		Vector3f vec = new Vector3f();
		Vector3f vec2 = new Vector3f();
		Vector3f childpos = new Vector3f();
		Vector3f front = new Vector3f();
		Vector3f side = new Vector3f();
		// now build the geometry. 
		if( pNode.getNumChildren() > 0 && !mKnobsOnly)
		{
			// Allocate memory for vertices. 
			int totalCapacity = getBufferLength() + pNode.getNumChildren() * 36;
			if(totalCapacity > getCapacity()){
				FloatBuffer newBuf = MemoryUtil.createFloatBuffer(totalCapacity, AssimpConfig.MESH_USE_NATIVE_MEMORY);
				if(mVertices != null){
					newBuf.put(newBuf);
				}
				mVertices = newBuf;
			}else{
				mVertices.position(mVertices.limit());
			}
			// If the node has children, we build little pointers to each of them
			for( int a = 0; a < pNode.getNumChildren(); a++)
			{
				// find a suitable coordinate system
				Matrix4f childTransform = pNode.mChildren[a].mTransformation;
				childpos.set( childTransform.m30, childTransform.m31, childTransform.m32);
				float distanceToChild = childpos.length();
				if( distanceToChild < 0.0001f)
					continue;
//				aiVector3D up = aiVector3D( childpos).Normalize();
				vec2.set(childpos).normalise();
				Vector3f up = vec2;
				Vector3f orth = vec;
				orth.set( 1.0f, 0.0f, 0.0f);
				if( Math.abs(Vector3f.dot(orth, up)) > 0.99f)
					orth.set( 0.0f, 1.0f, 0.0f);

//				aiVector3D front = (up ^ orth).Normalize();
//				aiVector3D side = (front ^ up).Normalize();
				Vector3f.cross(up, orth, front).normalise();
				Vector3f.cross(front, up, side).normalise();

				int localVertexStart = getNumVertices();
//				mVertices.push_back( -front * distanceToChild * 0.1f);
//				mVertices.push_back( childpos);
//				mVertices.push_back( -side * distanceToChild * 0.1f);
//				mVertices.push_back( -side * distanceToChild * 0.1f);
//				mVertices.push_back( childpos);
//				mVertices.push_back( front * distanceToChild * 0.1f);
//				mVertices.push_back( front * distanceToChild * 0.1f);
//				mVertices.push_back( childpos);
//				mVertices.push_back( side * distanceToChild * 0.1f);
//				mVertices.push_back( side * distanceToChild * 0.1f);
//				mVertices.push_back( childpos);
//				mVertices.push_back( -front * distanceToChild * 0.1f);
				
				Vector3f.scale(front, -distanceToChild * 0.1f, vec).store(mVertices);
				childpos.store(mVertices);
				Vector3f.scale(side, -distanceToChild * 0.1f, vec).store(mVertices);
				Vector3f.scale(side, -distanceToChild * 0.1f, vec).store(mVertices);
				childpos.store(mVertices);
				Vector3f.scale(front, distanceToChild * 0.1f, vec).store(mVertices);
				Vector3f.scale(front, distanceToChild * 0.1f, vec).store(mVertices);
				childpos.store(mVertices);
				Vector3f.scale(side, distanceToChild * 0.1f, vec).store(mVertices);
				Vector3f.scale(side, distanceToChild * 0.1f, vec).store(mVertices);
				childpos.store(mVertices);
				Vector3f.scale(front, -distanceToChild * 0.1f, vec).store(mVertices);

				mFaces.add(new TriangleFace( localVertexStart + 0, localVertexStart + 1, localVertexStart + 2));
				mFaces.add(new TriangleFace( localVertexStart + 3, localVertexStart + 4, localVertexStart + 5));
				mFaces.add(new TriangleFace( localVertexStart + 6, localVertexStart + 7, localVertexStart + 8));
				mFaces.add(new TriangleFace( localVertexStart + 9, localVertexStart + 10, localVertexStart + 11));
			}
			mVertices.flip();
		} 
		else
		{
			// if the node has no children, it's an end node. Put a little knob there instead
			//aiVector3D ownpos( pNode->mTransformation.a4, pNode->mTransformation.b4, pNode->mTransformation.c4);
			Vector3f ownpos = childpos;
			ownpos.set(pNode.mTransformation.m30, pNode.mTransformation.m31, pNode.mTransformation.m32);
			float sizeEstimate = ownpos.length() * 0.18f;
			
//			mVertices.push_back( aiVector3D( -sizeEstimate, 0.0f, 0.0f));  //1
//			mVertices.push_back( aiVector3D( 0.0f, sizeEstimate, 0.0f));   //2
//			mVertices.push_back( aiVector3D( 0.0f, 0.0f, -sizeEstimate));  //3
//			mVertices.push_back( aiVector3D( 0.0f, sizeEstimate, 0.0f));   //4
//			mVertices.push_back( aiVector3D( sizeEstimate, 0.0f, 0.0f));   //5
//			mVertices.push_back( aiVector3D( 0.0f, 0.0f, -sizeEstimate));  //6
//			mVertices.push_back( aiVector3D( sizeEstimate, 0.0f, 0.0f));   //7
//			mVertices.push_back( aiVector3D( 0.0f, -sizeEstimate, 0.0f));  //8
//			mVertices.push_back( aiVector3D( 0.0f, 0.0f, -sizeEstimate));  //9
//			mVertices.push_back( aiVector3D( 0.0f, -sizeEstimate, 0.0f));  //10
//			mVertices.push_back( aiVector3D( -sizeEstimate, 0.0f, 0.0f));  //11
//			mVertices.push_back( aiVector3D( 0.0f, 0.0f, -sizeEstimate));  //12
			vec.set( -sizeEstimate, 0.0f, 0.0f); vec.store(mVertices);     //1
			vec.set( 0.0f, sizeEstimate, 0.0f); vec.store(mVertices);      //2
			vec.set( 0.0f, 0.0f, -sizeEstimate); vec.store(mVertices);     //3
			vec.set( 0.0f, sizeEstimate, 0.0f); vec.store(mVertices);      //4
			vec.set( sizeEstimate, 0.0f, 0.0f); vec.store(mVertices);      //5
			vec.set( 0.0f, 0.0f, -sizeEstimate); vec.store(mVertices);     //6
			vec.set( sizeEstimate, 0.0f, 0.0f); vec.store(mVertices);      //7
			vec.set( 0.0f, -sizeEstimate, 0.0f); vec.store(mVertices);     //8
			vec.set( 0.0f, 0.0f, -sizeEstimate); vec.store(mVertices);     //9
			vec.set( 0.0f, -sizeEstimate, 0.0f); vec.store(mVertices);     //10
			vec.set( -sizeEstimate, 0.0f, 0.0f); vec.store(mVertices);     //11
			vec.set( 0.0f, 0.0f, -sizeEstimate); vec.store(mVertices);     //12

//			mVertices.push_back( aiVector3D( -sizeEstimate, 0.0f, 0.0f));  //1
//			mVertices.push_back( aiVector3D( 0.0f, 0.0f, sizeEstimate));   //2
//			mVertices.push_back( aiVector3D( 0.0f, sizeEstimate, 0.0f));   //3
//			mVertices.push_back( aiVector3D( 0.0f, sizeEstimate, 0.0f));   //4
//			mVertices.push_back( aiVector3D( 0.0f, 0.0f, sizeEstimate));   //5
//			mVertices.push_back( aiVector3D( sizeEstimate, 0.0f, 0.0f));   //6
//			mVertices.push_back( aiVector3D( sizeEstimate, 0.0f, 0.0f));   //7
//			mVertices.push_back( aiVector3D( 0.0f, 0.0f, sizeEstimate));   //8
//			mVertices.push_back( aiVector3D( 0.0f, -sizeEstimate, 0.0f));  //9
//			mVertices.push_back( aiVector3D( 0.0f, -sizeEstimate, 0.0f));  //10
//			mVertices.push_back( aiVector3D( 0.0f, 0.0f, sizeEstimate));   //11
//			mVertices.push_back( aiVector3D( -sizeEstimate, 0.0f, 0.0f));  //12
			vec.set( -sizeEstimate, 0.0f, 0.0f); vec.store(mVertices);     //1
			vec.set( 0.0f, 0.0f, sizeEstimate); vec.store(mVertices);      //2
			vec.set( 0.0f, sizeEstimate, 0.0f); vec.store(mVertices);     //3
			vec.set( 0.0f, sizeEstimate, 0.0f); vec.store(mVertices);      //4
			vec.set( 0.0f, 0.0f, sizeEstimate); vec.store(mVertices);      //5
			vec.set( sizeEstimate, 0.0f, 0.0f); vec.store(mVertices);     //6
			vec.set( sizeEstimate, 0.0f, 0.0f); vec.store(mVertices);      //7
			vec.set( 0.0f, 0.0f, sizeEstimate); vec.store(mVertices);     //8
			vec.set( 0.0f, -sizeEstimate, 0.0f); vec.store(mVertices);     //9
			vec.set( 0.0f, -sizeEstimate, 0.0f); vec.store(mVertices);     //10
			vec.set( 0.0f, 0.0f, sizeEstimate); vec.store(mVertices);     //11
			vec.set( -sizeEstimate, 0.0f, 0.0f); vec.store(mVertices);     //12

			mFaces.add(new TriangleFace( vertexStartIndex + 0, vertexStartIndex + 1, vertexStartIndex + 2));
			mFaces.add(new TriangleFace( vertexStartIndex + 3, vertexStartIndex + 4, vertexStartIndex + 5));
			mFaces.add(new TriangleFace( vertexStartIndex + 6, vertexStartIndex + 7, vertexStartIndex + 8));
			mFaces.add(new TriangleFace( vertexStartIndex + 9, vertexStartIndex + 10, vertexStartIndex + 11));
			mFaces.add(new TriangleFace( vertexStartIndex + 12, vertexStartIndex + 13, vertexStartIndex + 14));
			mFaces.add(new TriangleFace( vertexStartIndex + 15, vertexStartIndex + 16, vertexStartIndex + 17));
			mFaces.add(new TriangleFace( vertexStartIndex + 18, vertexStartIndex + 19, vertexStartIndex + 20));
			mFaces.add(new TriangleFace( vertexStartIndex + 21, vertexStartIndex + 22, vertexStartIndex + 23));
			mVertices.flip();
		}

		int numVertices = getNumVertices() - vertexStartIndex;
		if( numVertices > 0)
		{
			// create a bone affecting all the newly created vertices
			Bone bone = new Bone();
			mBones.add( bone);
			bone.mName = pNode.mName;

			// calculate the bone offset matrix by concatenating the inverse transformations of all parents
//			bone->mOffsetMatrix = aiMatrix4x4( pNode->mTransformation).Inverse();
			Matrix4f.invert(pNode.mTransformation, bone.mOffsetMatrix);
			Matrix4f tmpMat = new Matrix4f();
			for(Node parent = pNode.mParent; parent != null; parent = parent.mParent){
//				bone->mOffsetMatrix = aiMatrix4x4( parent->mTransformation).Inverse() * bone->mOffsetMatrix;
				Matrix4f.mul(Matrix4f.invert(parent.mTransformation, tmpMat), bone.mOffsetMatrix, bone.mOffsetMatrix); // TODO
			}

			// add all the vertices to the bone's influences
//			bone->mNumWeights = numVertices;
			bone.mWeights = new VertexWeight[numVertices];
			for(int a = 0; a < numVertices; a++)
				bone.mWeights[a] = new VertexWeight( vertexStartIndex + a, 1.0f);

			// HACK: (thom) transform all vertices to the bone's local space. Should be done before adding
			// them to the array, but I'm tired now and I'm annoyed.
			Matrix4f boneToMeshTransform = Matrix4f.invert(bone.mOffsetMatrix, tmpMat);/*aiMatrix4x4( bone->mOffsetMatrix).Inverse()*/;
			for(int a = vertexStartIndex; a < mVertices.remaining()/3; a++){
//				mVertices[a] = boneToMeshTransform * mVertices[a];
				int old_pos = mVertices.position();
				vec.load(mVertices);
				Matrix4f.transformVector(boneToMeshTransform, vec, vec);
				mVertices.position(old_pos);
				vec.store(mVertices);
			}
			mVertices.flip();
		}

		// and finally recurse into the children list
		for(int a = 0; a < pNode.getNumChildren(); a++)
			createGeometry( pNode.mChildren[a]);
	}

	// -------------------------------------------------------------------
	/** Creates the mesh from the internally accumulated stuff and returns it. 
	 */
	protected Mesh createMesh(){
		Mesh mesh = new Mesh();

		// add points
		mesh.mNumVertices = getNumVertices();
//		mesh.mVertices = new aiVector3D[mesh->mNumVertices];
//		std::copy( mVertices.begin(), mVertices.end(), mesh->mVertices);
		mesh.mVertices = mVertices;

		mesh.mNormals = MemoryUtil.createFloatBuffer(mesh.mNumVertices, AssimpConfig.MESH_USE_NATIVE_MEMORY);  //new aiVector3D[mesh->mNumVertices];
		Vector3f i0 = new Vector3f();
		Vector3f i1 = new Vector3f();
		Vector3f i2 = new Vector3f();
		// add faces
//		mesh->mNumFaces = mFaces.size();
//		mesh->mFaces = new aiFace[mesh->mNumFaces];
		mesh.mFaces = new Face[mFaces.size()];
		for(int a = 0; a < mesh.mFaces.length; a++)
		{
			Face inface = mFaces.get(a);
			mesh.mFaces[a] = inface;
//			aiFace& outface = mesh->mFaces[a];
//			outface.mNumIndices = 3;
//			outface.mIndices = new unsigned int[3];
//			outface.mIndices[0] = inface.mIndices[0];
//			outface.mIndices[1] = inface.mIndices[1];
//			outface.mIndices[2] = inface.mIndices[2];

			// Compute per-face normals ... we don't want the bones to be smoothed ... they're built to visualize
			// the skeleton, so it's good if there's a visual difference to the rest of the geometry
//			aiVector3D nor = ((mVertices[inface.mIndices[2]] - mVertices[inface.mIndices[0]]) ^ 
//				(mVertices[inface.mIndices[1]] - mVertices[inface.mIndices[0]]));
			load(i0, mVertices, inface.get(0));
			load(i1, mVertices, inface.get(1));
			load(i2, mVertices, inface.get(2));
			
			Vector3f _a = Vector3f.sub(i2, i0, i2);
			Vector3f _b = Vector3f.sub(i1, i0, i1);
			Vector3f nor = Vector3f.cross(_a, _b, i0);
			

			if (nor.length() < 1e-5f) /* ensure that FindInvalidData won't remove us ...*/
				nor.set(1.f,0.f,0.f);

			for (int n = 0; n < 3; ++n){
//				mesh->mNormals[inface.mIndices[n]] = nor;
				store(nor, mesh.mNormals, inface.get(n));
			}
		}

		// add the bones
//		mesh->mNumBones = mBones.size();
//		mesh->mBones = new aiBone*[mesh->mNumBones];
//		std::copy( mBones.begin(), mBones.end(), mesh->mBones);
		if(mBones.size() > 0)
			mesh.mBones = mBones.toArray(new Bone[mBones.size()]);

		// default
		mesh.mMaterialIndex = 0;

		return mesh;
	}
	
	private static void load(Vector3f v, FloatBuffer buf, int index){
		v.x = buf.get(3 * index);
		v.y = buf.get(3 * index + 1);
		v.z = buf.get(3 * index + 2);
	}
	
	private static void store(Vector3f v, FloatBuffer buf, int index){
		buf.put(3 * index, v.x);
		buf.put(3 * index + 1, v.y);
		buf.put(3 * index + 2, v.z);
	}

	// -------------------------------------------------------------------
	/** Creates a dummy material and returns it. */
	protected Material createMaterial(){
		Material matHelper = new Material();

		// Name
//		aiString matName( std::string( ""));
//		matHelper->AddProperty( &matName, AI_MATKEY_NAME);
		matHelper.addProperty("SkeletonMaterial", "?mat.name",0,0);

		// Prevent backface culling
		final int no_cull = 1;
		matHelper.addProperty(no_cull,"$mat.twosided",0,0);
		return matHelper;
	}
}
