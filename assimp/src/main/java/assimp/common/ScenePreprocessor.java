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

import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;

/** ScenePreprocessor: Preprocess a scene before any post-processing
 *  steps are executed.<p>
 *
 *  The step computes data that needn't necessarily be provided by the
 *  importer, such as aiMesh::mPrimitiveTypes.
*/
public class ScenePreprocessor {

	/** Scene we're currently working on */
	protected Scene scene;
	
	/** Default c'tpr. Use setScene() to assign a scene to the object.
	 */
	public ScenePreprocessor(){}
	
	/** Constructs the object and assigns a specific scene to it
	 */
	public ScenePreprocessor(Scene _scene){ scene = _scene;}
	

	// ----------------------------------------------------------------
	/** Assign a (new) scene to the object.<p>
	 *  
	 *  One 'SceneProcessor' can be used for multiple scenes.
	 *  Call ProcessScene to have the scene preprocessed.
	 *  @param sc Scene to be processed.
	 */
	public void setScene (Scene sc)	{
		scene = sc;
	}

	// ----------------------------------------------------------------
	/** Preprocess the current scene
	 */
	public void processScene (){
		// Process all meshes
		int numMeshes = scene.getNumMeshes();
		for (int i = 0; i < numMeshes;++i)
			processMesh(scene.mMeshes[i]);

		// - nothing to do for nodes for the moment
		// - nothing to do for textures for the moment
		// - nothing to do for lights for the moment
		// - nothing to do for cameras for the moment

		// Process all animations
		int numAnimations = scene.getNumAnimations();
		for (int i = 0; i < numAnimations;++i)
			processAnimation(scene.mAnimations[i]);

		// Generate a default material if none was specified
		if (scene.getNumMaterials() == 0 && numMeshes != 0)	{
			scene.mMaterials      = new Material[/*2*/ 1];
			Material helper;

			String name;

			scene.mMaterials[0] = helper = new Material();
			Vector3f clr = new Vector3f(0.6f,0.6f,0.6f);
			helper.addProperty(clr,"$clr.diffuse",0,0);

			// setup the default name to make this material identifiable
			name = Material.AI_DEFAULT_MATERIAL_NAME;
			helper.addProperty(name,"?mat.name",0,0);

			if(DefaultLogger.LOG_OUT)
				DefaultLogger.debug("ScenePreprocessor: Adding default material \'"  + Material.AI_DEFAULT_MATERIAL_NAME  + "\'");

			for (int i = 0; i < numMeshes/*scene.mNumMeshes*/;++i) {
				scene.mMeshes[i].mMaterialIndex = 0/*scene.mNumMaterials*/;
			}

//			scene.mNumMaterials++;
		}
	}
	
	// ----------------------------------------------------------------
	/** Preprocess an animation in the scene
	 *  @param anim Anim to be preprocessed.
	 */
	protected void processAnimation (Animation anim){
		double first = 10e10, last = -10e10;
		for (int i = 0; i < anim.getNumChannels();++i)	{
			NodeAnim channel = anim.mChannels[i];

			/*  If the exact duration of the animation is not given
			 *  compute it now.
			 */
			if (anim.mDuration == -1.)	{

				// Position keys
				for (int j = 0; j < channel.getNumPositionKeys();++j)	{
					VectorKey key = channel.mPositionKeys[j];
					first = Math.min (first, key.mTime);
					last  = Math.max (last,  key.mTime);
				}

				// Scaling keys
				for (int j = 0; j < channel.getNumScalingKeys();++j)	{
					VectorKey key = channel.mScalingKeys[j];
					first = Math.min (first, key.mTime);
					last  = Math.max (last,  key.mTime);
				}

				// Rotation keys
				for (int j = 0; j < channel.getNumRotationKeys();++j)	{
					QuatKey key = channel.mRotationKeys[j];
					first = Math.min (first, key.mTime);
					last  = Math.max (last,  key.mTime);
				}
			}

			/*  Check whether the animation channel has no rotation
			 *  or position tracks. In this case we generate a dummy
			 *  track from the information we have in the transformation
			 *  matrix of the corresponding node.
			 */
			if (channel.getNumRotationKeys() == 0 || channel.getNumPositionKeys() == 0 || channel.getNumScalingKeys() == 0)	{
				// Find the node that belongs to this animation
				Node node = scene.mRootNode.findNode(channel.mNodeName);
				if (node != null) // ValidateDS will complain later if 'node' is NULL
				{
					// Decompose the transformation matrix of the node
					Vector3f scaling = new Vector3f();
				    Vector3f position = new Vector3f();
					Quaternion rotation = new Quaternion();

//					node.mTransformation.decompose(scaling, rotation,position);
					AssUtil.decompose(node.mTransformation, scaling, rotation, position);

					// No rotation keys? Generate a dummy track
					if (/*!channel.mNumRotationKeys*/ channel.mRotationKeys == null)	{
//						channel.mNumRotationKeys = 1;
						channel.mRotationKeys = new QuatKey[]{new QuatKey()};
						QuatKey q = channel.mRotationKeys[0];

						q.mTime  = 0.f;
						q.mValue.set(rotation);

						if(DefaultLogger.LOG_OUT)
							DefaultLogger.debug("ScenePreprocessor: Dummy rotation track has been generated");
					}

					// No scaling keys? Generate a dummy track
					if (/*!channel.mNumScalingKeys*/ channel.mScalingKeys == null)	{
//						channel.mNumScalingKeys = 1;
						channel.mScalingKeys = new VectorKey[]{ new VectorKey()};
						VectorKey q = channel.mScalingKeys[0];

						q.mTime  = 0.f;
						q.mValue.set(scaling);

						if(DefaultLogger.LOG_OUT)
							DefaultLogger.debug("ScenePreprocessor: Dummy scaling track has been generated");
					}

					// No position keys? Generate a dummy track
					if (/*!channel.mNumPositionKeys*/ channel.mPositionKeys == null)	{
//						channel.mNumPositionKeys = 1;
						channel.mPositionKeys = new VectorKey[] {new VectorKey()};
						VectorKey q = channel.mPositionKeys[0];

						q.mTime  = 0.f;
						q.mValue.set(position);

						if(DefaultLogger.LOG_OUT)
							DefaultLogger.debug("ScenePreprocessor: Dummy position track has been generated");
					}
				}
			}
		}

		if (anim.mDuration == -1.)		{
			if(DefaultLogger.LOG_OUT)
				DefaultLogger.debug("ScenePreprocessor: Setting animation duration");
			anim.mDuration = last - Math.min( first, 0. );
		}
	}
	
	// ----------------------------------------------------------------
	/** Preprocess a mesh in the scene
	 *  @param mesh Mesh to be preprocessed.
	 */
	protected void processMesh (Mesh mesh){
		// If aiMesh::mNumUVComponents is *not* set assign the default value of 2
		for (int i = 0; i < Mesh.AI_MAX_NUMBER_OF_TEXTURECOORDS; ++i)	{
			if (mesh.mTextureCoords[i] == null)
				mesh.mNumUVComponents[i] = 0;

			else {
				if( mesh.mNumUVComponents[i] == 0)
					mesh.mNumUVComponents[i] = 2;

//				aiVector3D* p = mesh.mTextureCoords[i], *end = p+mesh.mNumVertices;
				int numVertices = mesh.mNumVertices;
				FloatBuffer p = mesh.mTextureCoords[i];

				// Ensure unsued components are zeroed. This will make 1D texture channels work
				// as if they were 2D channels .. just in case an application doesn't handle
				// this case
				if (2 == mesh.mNumUVComponents[i]) {
//					for (; p != end; ++p)
//						p.z = 0.f;
					
					for(int k=0; k < numVertices; k++)
						p.put(3 * k + 2, 0.0f);
				}
				else if (1 == mesh.mNumUVComponents[i]) {
//					for (; p != end; ++p)
//						p.z = p.y = 0.f;
					for(int k=0; k < numVertices; k++){
						p.put(3 * k + 1, 0.0f);  // y
						p.put(3 * k + 2, 0.0f);  // z
					}
				}
				else if (3 == mesh.mNumUVComponents[i]) {
					// Really 3D coordinates? Check whether the third coordinate is != 0 for at least one element
//					for (; p != end; ++p) {
//						if (p.z != 0)
//							break;
//					}
					int k;
					for(k=0; k < numVertices; k++){
						if(p.get(3 * k + 2) != 0)
							break;
					}
					
					if (k == numVertices) {
						if(DefaultLogger.LOG_OUT)
							DefaultLogger.warn("ScenePreprocessor: UVs are declared to be 3D but they're obviously not. Reverting to 2D.");
						mesh.mNumUVComponents[i] = 2;
					}
				}
			}
		}

		// If the information which primitive types are there in the
		// mesh is currently not available, compute it.
		if (mesh.mPrimitiveTypes == 0)	{
			for (int a = 0; a < mesh.getNumFaces(); ++a)	{
				Face face = mesh.mFaces[a];
				switch (face.getNumIndices())
				{
				case 3:
					mesh.mPrimitiveTypes |= Mesh.aiPrimitiveType_TRIANGLE;
					break;

				case 2:
					mesh.mPrimitiveTypes |= Mesh.aiPrimitiveType_LINE;
					break;

				case 1:
					mesh.mPrimitiveTypes |= Mesh.aiPrimitiveType_POINT;
					break;

				default:
					mesh.mPrimitiveTypes |= Mesh.aiPrimitiveType_POLYGON;
					break;
				}
			}
		}

		// If tangents and normals are given but no bitangents compute them
		if (mesh.mTangents!= null && mesh.mNormals!=null && mesh.mBitangents == null)	{
//			mesh.mBitangents = new aiVector3D[mesh.mNumVertices];
			mesh.mBitangents = MemoryUtil.createFloatBuffer(3 * mesh.mNumVertices, AssimpConfig.MESH_USE_NATIVE_MEMORY);
			Vector3f tan = new Vector3f();
			Vector3f nor = new Vector3f();
			Vector3f bitang = new Vector3f();
			for (int i = 0; i < mesh.mNumVertices;++i)	{
//				mesh.mBitangents[i] = mesh.mNormals[i] ^ mesh.mTangents[i];
				int index = 3 * i;
				tan.x = mesh.mTangents.get(index);
				tan.y = mesh.mTangents.get(index + 1);
				tan.z = mesh.mTangents.get(index + 2);
				
				nor.x = mesh.mNormals.get(index);
				nor.y = mesh.mNormals.get(index + 1);
				nor.z = mesh.mNormals.get(index + 2);
				
				Vector3f.cross(nor, tan, bitang);
				bitang.store(mesh.mBitangents);
			}
			
			mesh.mBitangents.flip();
		}
	}
}
