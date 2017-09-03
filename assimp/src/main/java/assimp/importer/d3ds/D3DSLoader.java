
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
package assimp.importer.d3ds;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;

import assimp.common.Animation;
import assimp.common.AssUtil;
import assimp.common.AssimpConfig;
import assimp.common.BaseImporter;
import assimp.common.Camera;
import assimp.common.DeadlyExportError;
import assimp.common.DeadlyImportError;
import assimp.common.DefaultLogger;
import assimp.common.Face;
import assimp.common.ImporterDesc;
import assimp.common.Light;
import assimp.common.LightSourceType;
import assimp.common.Material;
import assimp.common.MemoryUtil;
import assimp.common.Mesh;
import assimp.common.Node;
import assimp.common.NodeAnim;
import assimp.common.QuatKey;
import assimp.common.SGSpatialSort;
import assimp.common.Scene;
import assimp.common.ShadingMode;
import assimp.common.StreamReader;
import assimp.common.TextureMapMode;
import assimp.common.TextureType;
import assimp.common.VectorKey;

/** Importer class for 3D Studio r3 and r4 3DS files
 */
public class D3DSLoader extends BaseImporter{

	private static final ImporterDesc desc = new ImporterDesc(
			"Discreet 3DS Importer",
			"",
			"",
			"Limited animation support",
			ImporterDesc.aiImporterFlags_SupportBinaryFlavour,
			0,
			0,
			0,
			0,
			"3ds prj" 
	);
	
	/** Last touched node index */
	protected short mLastNodeIndex;

	/** Current node, root node */
	protected D3DSNode mCurrentNode, mRootNode;

	/** Scene under construction */
	protected DSScene mScene;

	/** Ambient base color of the scene */
	protected final Vector3f mClrAmbient = new Vector3f();

	/** Master scaling factor of the scene */
	protected float mMasterScale;

	/** Path to the background image of the scene */
	protected String mBackgroundImage;
	protected boolean bHasBG;

	/** true if PRJ file */
	protected boolean bIsPrj;
	
	// -------------------------------------------------------------------
	/** Returns whether the class can handle the format of the given file. 
	 * See BaseImporter::CanRead() for details.	
	 */
	public boolean canRead( String pFile,InputStream pIOHandler, boolean checkSig){
		String extension = getExtension(pFile);
		if(extension.equals("3ds") || extension.equals("prj") ) {
			return true;
		}
		if (extension.length() == 0 || checkSig) {
			byte[] token = new byte[4];
//			token[0] = 0x4d4d;
//			token[1] = 0x3dc2;
			AssUtil.getBytes((short)0x4d4d, token, 0);
			AssUtil.getBytes((short)0x3dc2, token, 2);
			//token[2] = 0x3daa;
			return checkMagicToken(new File(pFile),token,0,2);
		}
		return false;
	}
	
	// -------------------------------------------------------------------
	/** Return importer meta information.
	 * See #BaseImporter::GetInfo for the details
	 */
	protected ImporterDesc getInfo (){ return desc;}
	
	void computeNormalsWithSmoothingsGroups(D3DSMesh sMesh)
	{
		// First generate face normals
//		sMesh.mNormals.resize(sMesh.mPositions.size(),aiVector3D());
		Vector3f[] normals = new Vector3f[sMesh.mPositions.size()];
		AssUtil.initArray(normals);
		Vector3f pDelta1 = new Vector3f();
		Vector3f pDelta2 = new Vector3f();
		for(int a = 0; a < sMesh.mFaces.size(); a++)
		{
			D3DSFace face = sMesh.mFaces.get(a);

			Vector3f pV1 = sMesh.mPositions.get(face.mIndices[0]);
			Vector3f pV2 = sMesh.mPositions.get(face.mIndices[1]);
			Vector3f pV3 = sMesh.mPositions.get(face.mIndices[2]);

//			aiVector3D pDelta1 = *pV2 - *pV1;
//			aiVector3D pDelta2 = *pV3 - *pV1;
			Vector3f.sub(pV2, pV1, pDelta1);
			Vector3f.sub(pV3, pV1, pDelta2);
//			aiVector3D vNor = pDelta1 ^ pDelta2;
			Vector3f vNor = Vector3f.cross(pDelta1, pDelta2, null);

			for (int c = 0; c < 3;++c)
				normals[face.mIndices[c]] = vNor;
		}

		// calculate the position bounds so we have a reliable epsilon to check position differences against 
		
		Vector3f minVec = pDelta1;
		Vector3f maxVec = pDelta2;
		minVec.set( 1e10f, 1e10f, 1e10f);
		maxVec.set( -1e10f, -1e10f, -1e10f);
		for(int a = 0; a < sMesh.mPositions.size(); a++)
		{
			Vector3f position = sMesh.mPositions.get(a);
			minVec.x = Math.min( minVec.x, position.x);
			minVec.y = Math.min( minVec.y, position.y);
			minVec.z = Math.min( minVec.z, position.z);
			maxVec.x = Math.max( maxVec.x, position.x);
			maxVec.y = Math.max( maxVec.y, position.y);
			maxVec.z = Math.max( maxVec.z, position.z);
		}
		final float posEpsilon = Vector3f.distance(maxVec, minVec) * 1e-5f;
//		std::vector<aiVector3D> avNormals;
//		avNormals.resize(sMesh.mNormals.size());
		
		// now generate the spatial sort tree
		SGSpatialSort sSort = new SGSpatialSort();
//		for( typename std::vector<T>::iterator i =  sMesh.mFaces.begin();
//			i != sMesh.mFaces.end();++i)
//		{
//			for (int c = 0; c < 3;++c)
//				sSort.Add(sMesh.mPositions[(*i).mIndices[c]],(*i).mIndices[c],(*i).iSmoothGroup);
//		}
		for(D3DSFace i : sMesh.mFaces){
			for(int c = 0; c < 3; c++)
				sSort.add(sMesh.mPositions.get(i.mIndices[c]), i.mIndices[c], i.iSmoothGroup);
		}
		
		sSort.prepare();

//		std::vector<bool> vertexDone(sMesh.mPositions.size(),false);
		boolean[] vertexDone = new boolean[sMesh.mPositions.size()];
		final Vector3f vNormals = new Vector3f();
		IntList poResult = new IntArrayList();
//		for( typename std::vector<T>::iterator i =  sMesh.mFaces.begin();
//			i != sMesh.mFaces.end();++i)
		for(D3DSFace i : sMesh.mFaces)
		{
//			std::vector<int> poResult;
			for (int c = 0; c < 3;++c)
			{
				int idx = i.mIndices[c];
				if (vertexDone[idx])continue;

				sSort.findPositions(sMesh.mPositions.get(idx),i.iSmoothGroup,posEpsilon,poResult, false);

//				aiVector3D vNormals;
//				for (std::vector<int>::const_iterator
//					a =  poResult.begin();
//					a != poResult.end();++a)
				vNormals.set(0, 0, 0);
				for(int k = 0; k < poResult.size(); k ++)
				{
					int a = poResult.getInt(k);
//					vNormals += sMesh.mNormals[a];
					Vector3f nor = normals[a];
					vNormals.x += nor.x;
					vNormals.y += nor.y;
					vNormals.z += nor.z;
				}
				vNormals.normalise();

				// write back into all affected normals
//				for (std::vector<int>::const_iterator
//					a =  poResult.begin();
//					a != poResult.end();++a)
				for(int k = 0; k < poResult.size(); k++)
				{
					idx = poResult.getInt(k);
					normals[idx].set(vNormals);
					vertexDone[idx] = true;
				}
			}
		}
//		sMesh.mNormals = avNormals;
		sMesh.mNormals.clear();
		for(Vector3f v : normals)
			sMesh.mNormals.add(v);
	}

	// -------------------------------------------------------------------
	/** Imports the given file into the given scene structure. 
	 * See BaseImporter::InternReadFile() for details
	 */
	protected void internReadFile(File pFile, Scene pScene){
		try(BufferedInputStream in = new BufferedInputStream(new FileInputStream(pFile));
				StreamReader stream = new StreamReader(in, true)){  // TODO
			if(stream.getRemainingSize() < 16){
				throw new DeadlyExportError("3DS file is either empty or corrupt: " + pFile.getAbsolutePath());
			}
			
			// Allocate our temporary 3DS representation
			mScene = new DSScene();

			// Initialize members
			mLastNodeIndex             = -1;
			mCurrentNode               = new D3DSNode();
			mRootNode                  = mCurrentNode;
			mRootNode.mHierarchyPos    = -1;
			mRootNode.mHierarchyIndex  = -1;
			mRootNode.mParent          = null;
			mMasterScale               = 1.0f;
			mBackgroundImage           = "";
			bHasBG                     = false;
			bIsPrj                     = false;

			// Parse the file
			parseMainChunk(stream);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// Process all meshes in the file. First check whether all
		// face indices haev valid values. The generate our 
		// internal verbose representation. Finally compute normal
		// vectors from the smoothing groups we read from the
		// file.
		for (D3DSMesh i : mScene.mMeshes)	{
			checkIndices(i);
			makeUnique  (i);
			computeNormalsWithSmoothingsGroups(i);
		}

		// Replace all occurences of the default material with a
		// valid material. Generate it if no material containing
		// DEFAULT in its name has been found in the file
		replaceDefaultMaterial();

		// Convert the scene from our internal representation to an
		// aiScene object. This involves copying all meshes, lights
		// and cameras to the scene
		convertScene(pScene);

		// Generate the node graph for the scene. This is a little bit
		// tricky since we'll need to split some meshes into submeshes
		generateNodeGraph(pScene);

		// Now apply the master scaling factor to the scene
		applyMasterScale(pScene);
		
		mScene = null;
		System.gc();
	}
	
	// Find a node with a specific name in the import hierarchy
	D3DSNode findNode(D3DSNode root, String name)
	{
		if (root.mName.equals(name))
			return root;
//		for (std::vector<D3DS::Node*>::iterator it = root.mChildren.begin();it != root.mChildren.end(); ++it)	{
		for(D3DSNode it : root.mChildren){
			D3DSNode nd;
			if (( nd = findNode(it,name)) != null)
				return nd;
		}
		return null;
	}
	
	// Convert a 3DS texture to texture keys in an aiMaterial
	static void copyTexture(Material mat, D3DSTexture texture, TextureType type)
	{
		// Setup the texture name
		String tex = texture.mMapName;
		mat.addProperty(tex, Material._AI_MATKEY_TEXTURE_BASE, type.ordinal(),0);

		// Setup the texture blend factor
		if (!Float.isNaN(texture.mTextureBlend))
			mat.addProperty(texture.mTextureBlend, Material._AI_MATKEY_TEXBLEND_BASE, type.ordinal(),0);

		// Setup the texture mapping mode
		mat.addProperty(texture.mMapMode.ordinal(),Material._AI_MATKEY_MAPPINGMODE_U_BASE,type.ordinal(),0);
		mat.addProperty(texture.mMapMode.ordinal(),Material._AI_MATKEY_MAPPINGMODE_V_BASE,type.ordinal(),0);

		// Mirroring - double the scaling values 
		// FIXME: this is not really correct ...
		if (texture.mMapMode == TextureMapMode.aiTextureMapMode_Mirror)
		{
			texture.mScaleU *= 2.f;
			texture.mScaleV *= 2.f;
			texture.mOffsetU /= 2.f;
			texture.mOffsetV /= 2.f;
		}
		
		// Setup texture UV transformations
		float[] input = {texture.mOffsetU, texture.mOffsetV, texture.mScaleU, texture.mScaleV, texture.mRotation};
		mat.addProperty(input,Material._AI_MATKEY_UVTRANSFORM_BASE,type.ordinal(),0);
	}

	// -------------------------------------------------------------------
	/** Converts a temporary material to the outer representation 
	 */
	void convertMaterial(D3DSMaterial oldMat,Material mat){
		// NOTE: Pass the background image to the viewer by bypassing the
		// material system. This is an evil hack, never do it again!
		if (0 != mBackgroundImage.length() && bHasBG)
		{
			String tex = mBackgroundImage;
			mat.addProperty( tex, Material.AI_MATKEY_GLOBAL_BACKGROUND_IMAGE, 0, 0);

			// Be sure this is only done for the first material
			mBackgroundImage = ""; // std::string("");
		}

		// At first add the base ambient color of the scene to the material
		oldMat.mAmbient.x += mClrAmbient.x;
		oldMat.mAmbient.y += mClrAmbient.y;
		oldMat.mAmbient.z += mClrAmbient.z;

		String name=( oldMat.mName);
		mat.addProperty(name, Material.AI_MATKEY_NAME, 0, 0);

		// Material colors
		mat.addProperty( oldMat.mAmbient, Material.AI_MATKEY_COLOR_AMBIENT, 0, 0);
		mat.addProperty( oldMat.mDiffuse, Material.AI_MATKEY_COLOR_DIFFUSE, 0, 0);
		mat.addProperty( oldMat.mSpecular,Material.AI_MATKEY_COLOR_SPECULAR, 0, 0);
		mat.addProperty( oldMat.mEmissive, Material.AI_MATKEY_COLOR_EMISSIVE, 0,0);

		// Phong shininess and shininess strength
		if (D3DSHelper.Phong == oldMat.mShading || 
			D3DSHelper.Metal == oldMat.mShading)
		{
			if (oldMat.mSpecularExponent == 0 || oldMat.mShininessStrength == 0)
			{
				oldMat.mShading = D3DSHelper.Gouraud;
			}
			else
			{
				mat.addProperty(oldMat.mSpecularExponent, Material.AI_MATKEY_SHININESS, 0, 0);
				mat.addProperty(oldMat.mShininessStrength,Material.AI_MATKEY_SHININESS_STRENGTH, 0, 0);
			}
		}

		// Opacity
		mat.addProperty(oldMat.mTransparency,Material.AI_MATKEY_OPACITY, 0, 0);

		// Bump height scaling
		mat.addProperty(oldMat.mBumpHeight,Material.AI_MATKEY_BUMPSCALING,0, 0);

		// Two sided rendering?
		if (oldMat.mTwoSided)
		{
			int i = 1;
			mat.addProperty(i,Material.AI_MATKEY_TWOSIDED, 0, 0);
		}

		// Shading mode
		ShadingMode eShading = ShadingMode.aiShadingMode_NoShading;
		switch (oldMat.mShading)
		{
			case D3DSHelper.Flat:
				eShading = ShadingMode.aiShadingMode_Flat; break;

			// I don't know what "Wire" shading should be,
			// assume it is simple lambertian diffuse shading
			case D3DSHelper.Wire:
				{
					// Set the wireframe flag
					int iWire = 1;
					mat.addProperty(iWire, Material.AI_MATKEY_ENABLE_WIREFRAME, 0, 0);
				}

			case D3DSHelper.Gouraud:
				eShading = ShadingMode.aiShadingMode_Gouraud; break;

			// assume cook-torrance shading for metals.
			case D3DSHelper.Phong :
				eShading = ShadingMode.aiShadingMode_Phong; break;

			case D3DSHelper.Metal :
				eShading = ShadingMode.aiShadingMode_CookTorrance; break;

				// FIX to workaround a warning with GCC 4 who complained
				// about a missing case Blinn: here - Blinn isn't a valid
				// value in the 3DS Loader, it is just needed for ASE
			case D3DSHelper.Blinn :
				eShading = ShadingMode.aiShadingMode_Blinn; break;
		}
		mat.addProperty(eShading.ordinal(),Material.AI_MATKEY_SHADING_MODEL, 0, 0);

		// DIFFUSE texture
		if( oldMat.sTexDiffuse.mMapName.length() > 0)
			copyTexture(mat,oldMat.sTexDiffuse, TextureType.aiTextureType_DIFFUSE);

		// SPECULAR texture
		if( oldMat.sTexSpecular.mMapName.length() > 0)
			copyTexture(mat,oldMat.sTexSpecular, TextureType.aiTextureType_SPECULAR);

		// OPACITY texture
		if( oldMat.sTexOpacity.mMapName.length() > 0)
			copyTexture(mat,oldMat.sTexOpacity, TextureType.aiTextureType_OPACITY);

		// EMISSIVE texture
		if( oldMat.sTexEmissive.mMapName.length() > 0)
			copyTexture(mat,oldMat.sTexEmissive, TextureType.aiTextureType_EMISSIVE);

		// BUMP texture
		if( oldMat.sTexBump.mMapName.length() > 0)
			copyTexture(mat,oldMat.sTexBump, TextureType.aiTextureType_HEIGHT);

		// SHININESS texture
		if( oldMat.sTexShininess.mMapName.length() > 0)
			copyTexture(mat,oldMat.sTexShininess, TextureType.aiTextureType_SHININESS);

		// REFLECTION texture
		if( oldMat.sTexReflective.mMapName.length() > 0)
			copyTexture(mat,oldMat.sTexReflective, TextureType.aiTextureType_REFLECTION);

		// Store the name of the material itself, too
		if( oldMat.mName.length() > 0)	{
			String tex = oldMat.mName;
//			tex.Set( oldMat.mName);
			mat.addProperty(tex, Material.AI_MATKEY_NAME, 0, 0);
		}
	}

	// -------------------------------------------------------------------
	/** Read a chunk
	 *
	 *  @param pcOut Receives the current chunk
	 */
	void readChunk(StreamReader stream, Chunk pcOut){
		pcOut.flag = stream.getI2();
		pcOut.size = stream.getI4();
		
		if (pcOut.size - Chunk.SIZE > stream.getRemainingSize())
			throw new DeadlyImportError("Chunk is too large");
		
		if (pcOut.size - Chunk.SIZE > stream.getRemainingSizeToLimit())
			DefaultLogger.error("3DS: Chunk overflow");
	}

	// -------------------------------------------------------------------
	/** Parse a percentage chunk. mCurrent will point to the next
	* chunk behind afterwards. If no percentage chunk is found
	* QNAN is returned.
	*/
	float parsePercentageChunk(StreamReader stream){
		Chunk chunk = new Chunk();
		readChunk(stream, chunk);
		
		if (D3DSHelper.CHUNK_PERCENTF == chunk.flag)
			return stream.getF4();
		else if (D3DSHelper.CHUNK_PERCENTW == chunk.flag)
			return (float)(stream.getI2() & 0xFFFF) / (float)0xFFFF;
		return Float.NaN;
	}

	// -------------------------------------------------------------------
	/** Parse a color chunk. mCurrent will point to the next
	* chunk behind afterwards. If no color chunk is found
	* QNAN is returned in all members.
	*/
	void parseColorChunk(StreamReader stream, Vector3f out, boolean acceptPercent /*= true*/){
		// error return value
		final float qnan = Float.NaN;
//		static const aiColor3D clrError = aiColor3D(qnan,qnan,qnan);

		Chunk chunk = new Chunk();
		readChunk(stream, chunk);
		final int diff = chunk.size - Chunk.SIZE; // sizeof(D3DSHelper.Chunk);

		boolean bGamma = false;

		// Get the type of the chunk
		switch(chunk.flag)
		{
		case D3DSHelper.CHUNK_LINRGBF:
			bGamma = true;

		case D3DSHelper.CHUNK_RGBF:
			if (/*sizeof(float)*/4 * 3 > diff)	{
//				*out = clrError;
				out.set(qnan, qnan, qnan);
				return;
			}
			out.x = stream.getF4();
			out.y = stream.getF4();
			out.z = stream.getF4();
			break;

		case D3DSHelper.CHUNK_LINRGBB:
			bGamma = true;
		case D3DSHelper.CHUNK_RGBB:
			if (/*sizeof(char)*/1 * 3 > diff)	{
//				*out = clrError;
				out.set(qnan, qnan, qnan);
				return;
			}
			out.x = (float)(stream.getI1() & 0xFF) / 255.0f;
			out.y = (float)(stream.getI1() & 0xFF) / 255.0f;
			out.z = (float)(stream.getI1() & 0xFF) / 255.0f;
			break;

		// Percentage chunks are accepted, too.
		case D3DSHelper.CHUNK_PERCENTF:
			if (acceptPercent && 4 <= diff)	{
				out.x = out.y = out.z = stream.getF4();
				break;
			}
//			*out = clrError;
			out.set(qnan, qnan, qnan);
			return;

		case D3DSHelper.CHUNK_PERCENTW:
			if (acceptPercent && 1 <= diff)	{
				out.x = out.y = out.z = (float)(stream.getI1() & 0xFF) / 255.0f;
				break;
			}
//			*out = clrError;
			out.set(qnan, qnan, qnan);
			return;

		default:
			stream.incPtr(diff);
			// Skip unknown chunks, hope this won't cause any problems.
			parseColorChunk(stream, out,acceptPercent);
			return;
		};
	}


	// -------------------------------------------------------------------
	/** Skip a chunk in the file
	*/
	void skipChunk(StreamReader stream){
		Chunk psChunk = new Chunk();
		readChunk(stream, psChunk);
		stream.incPtr(psChunk.size - Chunk.SIZE);
	}

	// -------------------------------------------------------------------
	/** Generate the nodegraph
	*/
	void generateNodeGraph(Scene pcOut){
		pcOut.mRootNode = new Node();
		if (0 == mRootNode.mChildren.size())
		{
			//////////////////////////////////////////////////////////////////////////////
			// It seems the file is so messed up that it has not even a hierarchy.
			// generate a flat hiearachy which looks like this:
			//
			//                ROOT_NODE
			//                   |
			//   ----------------------------------------
			//   |       |       |            |         |  
			// MESH_0  MESH_1  MESH_2  ...  MESH_N    CAMERA_0 ....
			//
			if(DefaultLogger.LOG_OUT)
				DefaultLogger.warn("No hierarchy information has been found in the file. ");

			int numChildren = pcOut.getNumMeshes() + mScene.mCameras.size() + mScene.mLights.size();

			pcOut.mRootNode.mChildren = new Node [ numChildren ];
			pcOut.mRootNode.mName = ("<3DSDummyRoot>");

			// Build dummy nodes for all meshes
			int a = 0;
			for (int i = 0; i < pcOut.getNumMeshes();++i,++a)
			{
				Node pcNode = pcOut.mRootNode.mChildren[a] = new Node();
				pcNode.mParent = pcOut.mRootNode;
				pcNode.mMeshes = new int[1];
				pcNode.mMeshes[0] = i;
//				pcNode.mNumMeshes = 1;

				// Build a name for the node
//				pcNode.mName.length = sprintf(pcNode.mName.data,"3DSMesh_%i",i);
				pcNode.mName = String.format("3DSMesh_%i",i);
			}

			// Build dummy nodes for all cameras
			for (int i = 0; i < mScene.mCameras.size();++i,++a)
			{
				Node pcNode = pcOut.mRootNode.mChildren[a] = new Node();
				pcNode.mParent = pcOut.mRootNode;

				// Build a name for the node
				pcNode.mName = mScene.mCameras.get(i).mName;
			}

			// Build dummy nodes for all lights
			for (int i = 0; i < mScene.mLights.size();++i,++a)
			{
				Node pcNode = pcOut.mRootNode.mChildren[a] = new Node();
				pcNode.mParent = pcOut.mRootNode;

				// Build a name for the node
				pcNode.mName = mScene.mLights.get(i).mName;
			}
		}
		else
		{
			// First of all: find out how many scaling, rotation and translation
			// animation tracks we'll have afterwards
			int numChannel = countTracks(mRootNode,0);

			if (numChannel > 0)
			{
				// Allocate a primary animation channel
//				pcOut.mNumAnimations = 1;
				pcOut.mAnimations    = new Animation[1];
				Animation anim     = pcOut.mAnimations[0] = new Animation();

				anim.mName =("3DSMasterAnim");

				// Allocate enough storage for all node animation channels, 
				// but don't set the mNumChannels member - we'll use it to
				// index into the array
				anim.mChannels = new NodeAnim[numChannel];
			}

			addNodeToGraph(pcOut,  pcOut.mRootNode, mRootNode,null);
		}

		// We used the first and second vertex color set to store some temporary values so we need to cleanup here
		for (int a = 0; a < pcOut.getNumMeshes(); ++a)
		{
			pcOut.mMeshes[a].mColors[0] = null;
			pcOut.mMeshes[a].mColors[1] = null;
			pcOut.mMeshes[a].tag        = null;
		}

//		pcOut.mRootNode.mTransformation = aiMatrix4x4(
//			1.f,0.f,0.f,0.f,
//			0.f,0.f,1.f,0.f,
//			0.f,-1.f,0.f,0.f,
//			0.f,0.f,0.f,1.f) * pcOut.mRootNode.mTransformation;
		// TODO
		Matrix4f mat = new Matrix4f();
		mat.m21 = -1f;
		mat.m11 = 0;
		mat.m22 = 0;
		mat.m12 = 1;
		Matrix4f.mul(pcOut.mRootNode.mTransformation, mat, pcOut.mRootNode.mTransformation);

		// If the root node is unnamed name it "<3DSRoot>"
//		if (::strstr( pcOut.mRootNode.mName.data, "UNNAMED" ) ||
//			(pcOut.mRootNode.mName.data[0] == '$' && pcOut.mRootNode.mName.data[1] == '$') )
		if(pcOut.mRootNode.mName.contains("UNNAMED") || pcOut.mRootNode.mName.startsWith("$$"))
		{
			pcOut.mRootNode.mName = ("<3DSRoot>");
		}
	}
	
	// -------------------------------------------------------------------
	/** Parse a main top-level chunk in the file
	*/
	void parseMainChunk(StreamReader stream){
		while (true) {                                                       
			if (stream.getRemainingSizeToLimit() < Chunk.SIZE){ 
				return;                                                          
			}                                                                    
			Chunk chunk = new Chunk();                                            
			readChunk(stream, chunk);
		
			int chunkSize = chunk.size-Chunk.SIZE;	             
		    if(chunkSize <= 0)                                                   
		        continue;                                                        
			final int oldReadLimit = stream.getReadLimit();                     
			stream.setReadLimit(stream.getCurrentPos() + chunkSize);
			
			// get chunk type
			switch (chunk.flag)
			{
			
			case (short)D3DSHelper.CHUNK_PRJ:
				bIsPrj = true;
			case D3DSHelper.CHUNK_MAIN:
				parseEditorChunk(stream);
				break;
			};
			
			stream.skipToReadLimit();
			stream.setReadLimit(oldReadLimit);
			if (stream.getRemainingSizeToLimit() == 0)
				return;
		}
	}

	// -------------------------------------------------------------------
	/** Parse a top-level chunk in the file
	*/
	void parseChunk(StreamReader stream, String name){
		while (true) {                                                       
			if (stream.getRemainingSizeToLimit() < Chunk.SIZE){ 
				return;                                                          
			}                                                                    
			Chunk chunk = new Chunk();                                            
			readChunk(stream, chunk);
		
			int chunkSize = chunk.size-Chunk.SIZE;	             
		    if(chunkSize <= 0)                                                   
		        continue;                                                        
			final int oldReadLimit = stream.getReadLimit();                     
			stream.setReadLimit(stream.getCurrentPos() + chunkSize);
			
			// IMPLEMENTATION NOTE;
			// Cameras or lights define their transformation in their parent node and in the
			// corresponding light or camera chunks. However, we read and process the latter
			// to to be able to return valid cameras/lights even if no scenegraph is given.

			// get chunk type
			switch (chunk.flag)
			{
			case D3DSHelper.CHUNK_TRIMESH:
				{
				// this starts a new triangle mesh
				D3DSMesh m;
				mScene.mMeshes.add(m = new D3DSMesh());

				// Setup the name of the mesh
				m.mName = name;

				// Read mesh chunks
				parseMeshChunk(stream);
				}
				break;

			case D3DSHelper.CHUNK_LIGHT:	
				{
				// This starts a new light
				Light light = new Light();
				mScene.mLights.add(light);

//				light.mName.Set(std::string(name, num));
				light.mName = name;

				// First read the position of the light
				light.mPosition.x = stream.getF4();
				light.mPosition.y = stream.getF4();
				light.mPosition.z = stream.getF4();

				light.mColorDiffuse.set(1.f,1.f,1.f);

				// Now check for further subchunks
				if (!bIsPrj) /* fixme */
					parseLightChunk(stream);

				// The specular light color is identical the the diffuse light color. The ambient light color
				// is equal to the ambient base color of the whole scene.
				light.mColorSpecular.set(light.mColorDiffuse);
				light.mColorAmbient .set(mClrAmbient);

				if (light.mType == LightSourceType.aiLightSource_UNDEFINED)
				{
					// It must be a point light
					light.mType = LightSourceType.aiLightSource_POINT;
				}}
				break;

			case D3DSHelper.CHUNK_CAMERA:
				{
				// This starts a new camera
				Camera camera = new Camera();
				mScene.mCameras.add(camera);
				camera.mName = name;

				// First read the position of the camera
				camera.mPosition.x = stream.getF4();
				camera.mPosition.y = stream.getF4();
				camera.mPosition.z = stream.getF4();

				// Then the camera target
				camera.mLookAt.x = stream.getF4() - camera.mPosition.x;
				camera.mLookAt.y = stream.getF4() - camera.mPosition.y;
				camera.mLookAt.z = stream.getF4() - camera.mPosition.z;
				float len = camera.mLookAt.length();
				if (len < 1e-5f) {
					
					// There are some files with lookat == position. Don't know why or whether it's ok or not.
					DefaultLogger.error("3DS: Unable to read proper camera look-at vector");
					camera.mLookAt.set(0.f,1.f,0.f);

				}
				else /*camera.mLookAt /= len;*/
					camera.mLookAt.scale(1.0f/len);

				// And finally - the camera rotation angle, in counter clockwise direction 
				final float angle =  (float)Math.toRadians( stream.getF4() );
//				aiQuaternion quat(camera.mLookAt,angle);  TODO
//				camera.mUp = quat.GetMatrix() * aiVector3D(0.f,1.f,0.f);
				Quaternion quat = new Quaternion();
				quat.setFromAxisAngle(camera.mLookAt, angle);
				Matrix3f mat = new Matrix3f();
			    quat.toMatrix(mat);
			    camera.mUp.set(0.f,1.f,0.f);
			    Matrix3f.transform(mat, camera.mUp, camera.mUp);

				// Read the lense angle
				camera.mHorizontalFOV = (float)Math.toRadians ( stream.getF4() );
				if (camera.mHorizontalFOV < 0.001f)  {
					camera.mHorizontalFOV = (float)Math.toRadians(45.f);
				}

				// Now check for further subchunks 
				if (!bIsPrj) /* fixme */ {
					parseCameraChunk(stream);
				}}
				break;
			};
			
			stream.skipToReadLimit();
			stream.setReadLimit(oldReadLimit);
			if (stream.getRemainingSizeToLimit() == 0)
				return;
		}
	}

	// -------------------------------------------------------------------
	/** Parse a top-level editor chunk in the file
	*/
	void parseEditorChunk(StreamReader stream){
		while (true) {                                                       
			if (stream.getRemainingSizeToLimit() < Chunk.SIZE){ 
				return;                                                          
			}                                                                    
			Chunk chunk = new Chunk();                                            
			readChunk(stream, chunk);                                                
			int chunkSize = chunk.size-Chunk.SIZE;	             
		    if(chunkSize <= 0)                                                   
		        continue;                                                        
			final int oldReadLimit = stream.getReadLimit();                     
			stream.setReadLimit(stream.getCurrentPos() + chunkSize);
			// get chunk type
			switch (chunk.flag)
			{
			case D3DSHelper.CHUNK_OBJMESH:

				parseObjectChunk(stream);
				break;

			// NOTE: In several documentations in the internet this
			// chunk appears at different locations
			case (short) D3DSHelper.CHUNK_KEYFRAMER:

				parseKeyframeChunk(stream);
				break;

			case D3DSHelper.CHUNK_VERSION:
				{
				// print the version number
//				char buff[10];
//				ASSIMP_itoa10(buff,stream.getI2());
//				DefaultLogger::get().info(std::string("3DS file format version: ") + buff);
					DefaultLogger.info("3DS file format version: " + stream.getI2());
				}
				break;
			};
			
			stream.skipToReadLimit();                  
			stream.setReadLimit(oldReadLimit);         
			if (stream.getRemainingSizeToLimit() == 0) 
				return;                                 
		}
	}

	// -------------------------------------------------------------------
	/** Parse a top-level object chunk in the file
	*/
	void parseObjectChunk(StreamReader stream){
		while (true) {                                                       
			if (stream.getRemainingSizeToLimit() < Chunk.SIZE){ 
				return;                                                          
			}                                                                    
			Chunk chunk = new Chunk();                                            
			readChunk(stream, chunk);                                                
			int chunkSize = chunk.size-Chunk.SIZE;	             
		    if(chunkSize <= 0)                                                   
		        continue;                                                        
			final int oldReadLimit = stream.getReadLimit();                     
			stream.setReadLimit(stream.getCurrentPos() + chunkSize);
			// get chunk type
			// get chunk type
			switch (chunk.flag)
			{
			case D3DSHelper.CHUNK_OBJBLOCK:
				{
				int cnt = 0;
//				const char* sz = (const char*)stream.GetPtr();
				ByteBuffer sz = stream.getPtr();

				// Get the name of the geometry object
				while (stream.getI1() != 0)++cnt;
				byte[] name = new byte[cnt];
				sz.get(name);
				parseChunk(stream, new String(name));
				}
				break;

			case (short) D3DSHelper.CHUNK_MAT_MATERIAL:

				// Add a new material to the list
				mScene.mMaterials.add(new D3DSMaterial());
				parseMainChunk(stream);
				break;

			case D3DSHelper.CHUNK_AMBCOLOR:

				// This is the ambient base color of the scene.
				// We add it to the ambient color of all materials
				parseColorChunk(stream, mClrAmbient,true);
//				if (is_qnan(mClrAmbient.r))
				if(Float.isNaN(mClrAmbient.x))
				{
					// We failed to read the ambient base color.
					DefaultLogger.error("3DS: Failed to read ambient base color");
					mClrAmbient.x = mClrAmbient.y = mClrAmbient.z = 0.0f;
				}
				break;

			case D3DSHelper.CHUNK_BIT_MAP:
				{
				// Specifies the background image. The string should already be 
				// properly 0 terminated but we need to be sure
				int cnt = 0;
//				const char* sz = (const char*)stream.GetPtr();
				ByteBuffer sz = stream.getPtr();
				while (stream.getI1() != 0)++cnt;
				byte[] name = new byte[cnt];
				sz.get(name);
				mBackgroundImage = new String(name)/*std::string(sz,cnt)*/;
				}
				break;

			case D3DSHelper.CHUNK_BIT_MAP_EXISTS:
				bHasBG = true;
				break;

			case D3DSHelper.CHUNK_MASTER_SCALE:
				// Scene master scaling factor
				mMasterScale = stream.getF4();
				break;
			};
			
			stream.skipToReadLimit();                  
			stream.setReadLimit(oldReadLimit);         
			if (stream.getRemainingSizeToLimit() == 0) 
				return;                                 
		}
	}

	// -------------------------------------------------------------------
	/** Parse a material chunk in the file
	*/
	void parseMaterialChunk(StreamReader stream){
		while (true) {                                                       
			if (stream.getRemainingSizeToLimit() < Chunk.SIZE){ 
				return;                                                          
			}                                                                    
			Chunk chunk = new Chunk();                                            
			readChunk(stream, chunk);                                                
			int chunkSize = chunk.size-Chunk.SIZE;	             
		    if(chunkSize <= 0)                                                   
		        continue;                                                        
			final int oldReadLimit = stream.getReadLimit();                     
			stream.setReadLimit(stream.getCurrentPos() + chunkSize);
			
			switch (chunk.flag & 0xFFFF)
			{
			case D3DSHelper.CHUNK_MAT_MATNAME:

				{
				// The material name string is already zero-terminated, but we need to be sure ...
//				const char* sz = (const char*)stream.getPtr();
			    final ByteBuffer sz = stream.getPtr();
				int cnt = 0;
				while (stream.getI1() != 0)
					++cnt;

				if (cnt == 0)	{
					// This may not be, we use the default name instead
					DefaultLogger.error("3DS: Empty material name");
				}
				else {
					byte[] name = new byte[cnt];
					sz.get(name);
					AssUtil.back(mScene.mMaterials).mName = new String(name);
				}
				}
				break;

			case D3DSHelper.CHUNK_MAT_DIFFUSE:
				{
				// This is the diffuse material color
				Vector3f pc =AssUtil.back(mScene.mMaterials).mDiffuse;
				parseColorChunk(stream, pc, true);
				if (Float.isNaN(pc.x))	{
					// color chunk is invalid. Simply ignore it
					DefaultLogger.error("3DS: Unable to read DIFFUSE chunk");
//					pc.r = pc.g = pc.b = 1.0f;
					pc.set(1, 1, 1);
				}}
				break;

			case D3DSHelper.CHUNK_MAT_SPECULAR:
				{
				// This is the specular material color
				Vector3f pc = AssUtil.back(mScene.mMaterials).mSpecular;
				parseColorChunk(stream, pc, true);
				if (Float.isNaN(pc.x))	{
					// color chunk is invalid. Simply ignore it
					DefaultLogger.error("3DS: Unable to read SPECULAR chunk");
//					pc.r = pc.g = pc.b = 1.0f;
					pc.set(1, 1, 1);
				}}
				break;

			case D3DSHelper.CHUNK_MAT_AMBIENT:
				{
				// This is the ambient material color
				Vector3f pc = AssUtil.back(mScene.mMaterials).mAmbient;
				parseColorChunk(stream,pc, true);
				if (Float.isNaN(pc.x))	{
					// color chunk is invalid. Simply ignore it
					DefaultLogger.error("3DS: Unable to read AMBIENT chunk");
//					pc.r = pc.g = pc.b = 0.0f;
					pc.set(0,0, 0);
				}}
				break;

			case D3DSHelper.CHUNK_MAT_SELF_ILLUM:
				{
				// This is the emissive material color
				Vector3f pc = AssUtil.back(mScene.mMaterials).mEmissive;
				parseColorChunk(stream,pc, true);
				if (Float.isNaN(pc.x))	{
					// color chunk is invalid. Simply ignore it
					DefaultLogger.error("3DS: Unable to read EMISSIVE chunk");
//					pc.r = pc.g = pc.b = 0.0f;
					pc.set(0,0, 0);
				}}
				break;

			case D3DSHelper.CHUNK_MAT_TRANSPARENCY:
				{
				// This is the material's transparency
				float pcf = AssUtil.back(mScene.mMaterials).mTransparency;
				pcf = parsePercentageChunk(stream);

				// NOTE: transparency, not opacity
				if (Float.isNaN(pcf))
					pcf = 1.0f;
				else pcf = 1.0f - pcf * (float)0xFFFF / 100.0f;
				AssUtil.back(mScene.mMaterials).mTransparency = pcf;
				}
				break;

			case D3DSHelper.CHUNK_MAT_SHADING:
				// This is the material shading mode
				AssUtil.back(mScene.mMaterials).mShading = stream.getI2() & 0xFFFF;
				break;

			case D3DSHelper.CHUNK_MAT_TWO_SIDE:
				// This is the two-sided flag
				AssUtil.back(mScene.mMaterials).mTwoSided = true;
				break;

			case D3DSHelper.CHUNK_MAT_SHININESS:
				{ // This is the shininess of the material
				float pcf = AssUtil.back(mScene.mMaterials).mSpecularExponent;
				pcf = parsePercentageChunk(stream);
				if (Float.isNaN(pcf))
					pcf = 0.0f;
				else pcf *= (float)0xFFFF;
				AssUtil.back(mScene.mMaterials).mSpecularExponent = pcf;
				}
				break;

			case D3DSHelper.CHUNK_MAT_SHININESS_PERCENT:
				{ // This is the shininess strength of the material
				float pcf = AssUtil.back(mScene.mMaterials).mShininessStrength;
				pcf = parsePercentageChunk(stream);
				if (Float.isNaN(pcf))
					pcf = 0.0f;
				else pcf *= (float)0xffff / 100.0f;
				AssUtil.back(mScene.mMaterials).mShininessStrength=pcf;
				}
				break;

			case D3DSHelper.CHUNK_MAT_SELF_ILPCT:
				{ // This is the self illumination strength of the material
				float f = parsePercentageChunk(stream);
				if (Float.isNaN(f))
					f = 0.0f;
				else f *= (float)0xFFFF / 100.0f;
				AssUtil.back(mScene.mMaterials).mEmissive.set(f,f,f);
				}
				break;

			// Parse texture chunks
			case D3DSHelper.CHUNK_MAT_TEXTURE:
				// Diffuse texture
				parseTextureChunk(stream,AssUtil.back(mScene.mMaterials).sTexDiffuse);
				break;
			case D3DSHelper.CHUNK_MAT_BUMPMAP:
				// Height map
				parseTextureChunk(stream,AssUtil.back(mScene.mMaterials).sTexBump);
				break;
			case D3DSHelper.CHUNK_MAT_OPACMAP:
				// Opacity texture
				parseTextureChunk(stream,AssUtil.back(mScene.mMaterials).sTexOpacity);
				break;
			case D3DSHelper.CHUNK_MAT_MAT_SHINMAP:
				// Shininess map
				parseTextureChunk(stream,AssUtil.back(mScene.mMaterials).sTexShininess);
				break;
			case D3DSHelper.CHUNK_MAT_SPECMAP:
				// Specular map
				parseTextureChunk(stream,AssUtil.back(mScene.mMaterials).sTexSpecular);
				break;
			case D3DSHelper.CHUNK_MAT_SELFIMAP:
				// Self-illumination (emissive) map
				parseTextureChunk(stream,AssUtil.back(mScene.mMaterials).sTexEmissive);
				break;
			case D3DSHelper.CHUNK_MAT_REFLMAP:
				// Reflection map
				parseTextureChunk(stream,AssUtil.back(mScene.mMaterials).sTexReflective);
				break;
			};
			
			stream.skipToReadLimit();                  
			stream.setReadLimit(oldReadLimit);         
			if (stream.getRemainingSizeToLimit() == 0) 
				return;                                 
		}
	}

	// -------------------------------------------------------------------
	/** Parse a mesh chunk in the file
	*/
	void parseMeshChunk(StreamReader stream){
		while (true) {                                                       
			if (stream.getRemainingSizeToLimit() < Chunk.SIZE){ 
				return;                                                          
			}                                                                    
			Chunk chunk = new Chunk();                                            
			readChunk(stream, chunk);                                                
			int chunkSize = chunk.size-Chunk.SIZE;	             
		    if(chunkSize <= 0)                                                   
		        continue;                                                        
			final int oldReadLimit = stream.getReadLimit();                     
			stream.setReadLimit(stream.getCurrentPos() + chunkSize);
			
			// Get the mesh we're currently working on
			D3DSMesh mMesh = AssUtil.back(mScene.mMeshes);

			// get chunk type
			switch (chunk.flag)
			{
			case D3DSHelper.CHUNK_VERTLIST:
				{
				// This is the list of all vertices in the current mesh
				int num = stream.getI2() & 0xFFFF;
//				mMesh.mPositions.reserve(num);
				while (num-- > 0)	{
					Vector3f v = new Vector3f();
					v.x = stream.getF4();
					v.y = stream.getF4();
					v.z = stream.getF4();
					mMesh.mPositions.add(v);
				}}
				break;
			case D3DSHelper.CHUNK_TRMATRIX:
				{
				// This is the RLEATIVE transformation matrix of the current mesh. Vertices are
				// pretransformed by this matrix wonder.
				mMesh.mMat.m00 = stream.getF4();
				mMesh.mMat.m10 = stream.getF4();
				mMesh.mMat.m20 = stream.getF4();
				mMesh.mMat.m01 = stream.getF4();
				mMesh.mMat.m11 = stream.getF4();
				mMesh.mMat.m21 = stream.getF4();
				mMesh.mMat.m02 = stream.getF4();
				mMesh.mMat.m12 = stream.getF4();
				mMesh.mMat.m22 = stream.getF4();
				mMesh.mMat.m03 = stream.getF4();
				mMesh.mMat.m12 = stream.getF4();
				mMesh.mMat.m23 = stream.getF4();
				}
				break;

			case D3DSHelper.CHUNK_MAPLIST:
				{
				// This is the list of all UV coords in the current mesh
				int num = stream.getI2() & 0xFFFF;
//				mMesh.mTexCoords.reserve(num);
				while (num-- > 0)	{
					Vector3f v = new Vector3f();
					v.x = stream.getF4();
					v.y = stream.getF4();
					mMesh.mTexCoords.add(v);
				}}
				break;

			case D3DSHelper.CHUNK_FACELIST:
				{
				// This is the list of all faces in the current mesh
				int num = stream.getI2() & 0xFFFF;
//				mMesh.mFaces.reserve(num);
				while (num-- > 0)	{
					// 3DS faces are ALWAYS triangles
					D3DSFace sFace;
					mMesh.mFaces.add(sFace = new D3DSFace());
//					D3DS::Face& sFace = mMesh.mFaces.back();

					sFace.mIndices[0] = stream.getI2() & 0xFFFF;
					sFace.mIndices[1] = stream.getI2() & 0xFFFF;
					sFace.mIndices[2] = stream.getI2() & 0xFFFF;

					stream.incPtr(2); // skip edge visibility flag
				}

				// Resize the material array (0xcdcdcdcd marks the default material; so if a face is 
				// not referenced by a material, $$DEFAULT will be assigned to it)
				mMesh.mFaceMaterials.size(mMesh.mFaces.size());
				Arrays.fill(mMesh.mFaceMaterials.elements(), 0xcdcdcdcd);

				// Larger 3DS files could have multiple FACE chunks here
				chunkSize = stream.getRemainingSizeToLimit();
				if ( chunkSize > Chunk.SIZE)
					parseFaceChunk(stream);
				}
				break;
			};
			
			stream.skipToReadLimit();                  
			stream.setReadLimit(oldReadLimit);         
			if (stream.getRemainingSizeToLimit() == 0) 
				return;                                 
		}
	}

	// -------------------------------------------------------------------
	/** Parse a light chunk in the file
	*/
	void parseLightChunk(StreamReader stream){
		while (true) {                                                       
			if (stream.getRemainingSizeToLimit() < Chunk.SIZE){ 
				return;                                                          
			}                                                                    
			Chunk chunk = new Chunk();                                            
			readChunk(stream, chunk);                                                
			int chunkSize = chunk.size-Chunk.SIZE;	             
		    if(chunkSize <= 0)                                                   
		        continue;                                                        
			final int oldReadLimit = stream.getReadLimit();                     
			stream.setReadLimit(stream.getCurrentPos() + chunkSize);
			
			Light light = null;
			if(!mScene.mLights.isEmpty())
				light = mScene.mLights.get(mScene.mLights.size() - 1);

			// get chunk type
			switch (chunk.flag)
			{
			case D3DSHelper.CHUNK_DL_SPOTLIGHT:
				// Now we can be sure that the light is a spot light
				light.mType = LightSourceType.aiLightSource_SPOT;

				// We wouldn't need to normalize here, but we do it
				light.mDirection.x = stream.getF4() - light.mPosition.x;
				light.mDirection.y = stream.getF4() - light.mPosition.y;
				light.mDirection.z = stream.getF4() - light.mPosition.z;
				light.mDirection.normalise();

				// Now the hotspot and falloff angles - in degrees
				light.mAngleInnerCone = (float)Math.toRadians( stream.getF4() );

				// FIX: the falloff angle is just an offset
				light.mAngleOuterCone = light.mAngleInnerCone+(float)Math.toRadians( stream.getF4() );
				break; 

				// intensity multiplier
			case D3DSHelper.CHUNK_DL_MULTIPLIER:
//				light.mColorDiffuse = light.mColorDiffuse * stream.getF4();
				light.mColorDiffuse.scale(stream.getF4());
				break;

				// light color
			case D3DSHelper.CHUNK_RGBF:
			case D3DSHelper.CHUNK_LINRGBF:
				light.mColorDiffuse.x *= stream.getF4();
				light.mColorDiffuse.y *= stream.getF4();
				light.mColorDiffuse.z *= stream.getF4();
				break;

				// light attenuation
			case D3DSHelper.CHUNK_DL_ATTENUATE: 
				light.mAttenuationLinear = stream.getF4();
				break;
			};
			
			stream.skipToReadLimit();                  
			stream.setReadLimit(oldReadLimit);         
			if (stream.getRemainingSizeToLimit() == 0) 
				return;                                 
		}
	}

	// -------------------------------------------------------------------
	/** Parse a camera chunk in the file
	*/
	void parseCameraChunk(StreamReader stream){
		while (true) {                                                       
			if (stream.getRemainingSizeToLimit() < Chunk.SIZE){ 
				return;                                                          
			}                                                                    
			Chunk chunk = new Chunk();                                            
			readChunk(stream, chunk);                                                
			int chunkSize = chunk.size-Chunk.SIZE;	             
		    if(chunkSize <= 0)                                                   
		        continue;                                                        
			final int oldReadLimit = stream.getReadLimit();                     
			stream.setReadLimit(stream.getCurrentPos() + chunkSize);
			
			Camera camera = AssUtil.back(mScene.mCameras);

			// get chunk type
			switch (chunk.flag)
			{
				// near and far clip plane
			case D3DSHelper.CHUNK_CAM_RANGES:
				camera.mClipPlaneNear = stream.getF4();
				camera.mClipPlaneFar  = stream.getF4();
				break;
			}
			
			stream.skipToReadLimit();                  
			stream.setReadLimit(oldReadLimit);         
			if (stream.getRemainingSizeToLimit() == 0) 
				return;                                 
		}
	}

	// -------------------------------------------------------------------
	/** Parse a face list chunk in the file
	*/
	void parseFaceChunk(StreamReader stream){
		while (true) {                                                       
			if (stream.getRemainingSizeToLimit() < Chunk.SIZE){ 
				return;                                                          
			}                                                                    
			Chunk chunk = new Chunk();                                            
			readChunk(stream, chunk);                                                
			int chunkSize = chunk.size-Chunk.SIZE;	             
		    if(chunkSize <= 0)                                                   
		        continue;                                                        
			final int oldReadLimit = stream.getReadLimit();                     
			stream.setReadLimit(stream.getCurrentPos() + chunkSize);
			
			// Get the mesh we're currently working on
			D3DSMesh mMesh = AssUtil.back(mScene.mMeshes);

			// Get chunk type
			switch (chunk.flag)
			{
			case D3DSHelper.CHUNK_SMOOLIST:
				{
				// This is the list of smoothing groups - a bitfield for every face. 
				// Up to 32 smoothing groups assigned to a single face.
				int num = chunkSize/4, m = 0;
//				for (D3DSFace>::iterator i =  mMesh.mFaces.begin(); m != num;++i, ++m)	{
				for(int i = 0; m != num; ++i, ++m){
					// nth bit is set for nth smoothing group
					mMesh.mFaces.get(i).iSmoothGroup = stream.getI4();
				}}
				break;

			case D3DSHelper.CHUNK_FACEMAT:
				{
				// at fist an asciiz with the material name
//				const char* sz = (const char*)stream.getPtr();
				final ByteBuffer sz = stream.getPtr();
				int szCount = 0;
				while (stream.getI1() != 0)szCount++;

				// find the index of the material
				int idx = 0xcdcdcdcd, cnt = 0;
//				for (std::vector<D3DS::Material>::const_iterator i =  mScene.mMaterials.begin();i != mScene.mMaterials.end();++i,++cnt)	{
				for(int i = 0; i < mScene.mMaterials.size(); i++, ++cnt){
					// use case independent comparisons. hopefully it will work.
					String name = mScene.mMaterials.get(i).mName;
					if (!ASSIMP_stricmp(sz, szCount, name))	{
						idx = cnt;
						break;
					}
				}
				if (0xcdcdcdcd == idx)	{
					DefaultLogger.error("3DS: Unknown material: " + sz);
				}

				// Now continue and read all material indices
				cnt = stream.getI2() & 0xFFFF;
				for (int i = 0; i < cnt;++i)	{
					 int fidx = stream.getI2() & 0xFFFF;

					// check range
					if (fidx >= mMesh.mFaceMaterials.size())	{
						DefaultLogger.error("3DS: Invalid face index in face material list");
					}
					else mMesh.mFaceMaterials.set(fidx, idx);
				}}
				break;
			};
			
			stream.skipToReadLimit();                  
			stream.setReadLimit(oldReadLimit);         
			if (stream.getRemainingSizeToLimit() == 0) 
				return;                                 
		}
	}
	
	static boolean ASSIMP_stricmp(ByteBuffer str1, int count, String str2){
		if(count != str2.length())
			return true;
		
		for(int i = 0; i < count; i++){
			if(str1.get(i) != (byte)str2.charAt(i))
				return true;
		}
		
		return false;
	}

	// -------------------------------------------------------------------
	/** Parse a keyframe chunk in the file
	*/
	void parseKeyframeChunk(StreamReader stream){
		while (true) {                                                       
			if (stream.getRemainingSizeToLimit() < Chunk.SIZE){ 
				return;                                                          
			}                                                                    
			Chunk chunk = new Chunk();                                            
			readChunk(stream, chunk);                                                
			int chunkSize = chunk.size-Chunk.SIZE;	             
		    if(chunkSize <= 0)                                                   
		        continue;                                                        
			final int oldReadLimit = stream.getReadLimit();                     
			stream.setReadLimit(stream.getCurrentPos() + chunkSize);
			
			// get chunk type
			switch (chunk.flag)
			{
			case (short)D3DSHelper.CHUNK_TRACKCAMTGT:
			case (short)D3DSHelper.CHUNK_TRACKSPOTL:
			case (short)D3DSHelper.CHUNK_TRACKCAMERA:
			case (short)D3DSHelper.CHUNK_TRACKINFO:
			case (short)D3DSHelper.CHUNK_TRACKLIGHT:
			case (short)D3DSHelper.CHUNK_TRACKLIGTGT:

				// this starts a new mesh hierarchy chunk
				parseHierarchyChunk(stream,chunk.flag);
				break;
			};
			
			stream.skipToReadLimit();                  
			stream.setReadLimit(oldReadLimit);         
			if (stream.getRemainingSizeToLimit() == 0) 
				return;                                 
		}
	}

	// -------------------------------------------------------------------
	/** Parse a hierarchy chunk in the file
	*/
	void parseHierarchyChunk(StreamReader stream, short parent){
		while (true) {                                                       
			if (stream.getRemainingSizeToLimit() < Chunk.SIZE){ 
				return;                                                          
			}                                                                    
			Chunk chunk = new Chunk();                                            
			readChunk(stream, chunk);                                                
			int chunkSize = chunk.size-Chunk.SIZE;	             
		    if(chunkSize <= 0)                                                   
		        continue;                                                        
			final int oldReadLimit = stream.getReadLimit();                     
			stream.setReadLimit(stream.getCurrentPos() + chunkSize);
			
			// get chunk type
			switch (chunk.flag & 0xFFFF)
			{
			case D3DSHelper.CHUNK_TRACKOBJNAME:

				// This is the name of the object to which the track applies. The chunk also
				// defines the position of this object in the hierarchy.
				{

				// First of all: get the name of the object
				int cnt = 0;
//				const char* sz = (const char*)stream.GetPtr();
				final ByteBuffer sz = stream.getPtr();

				while (stream.getI1() != 0)++cnt;
//				std::string name = std::string(sz,cnt);
				byte[] _name = new byte[cnt];
				sz.get(_name);
				String name = new String(_name);

				// Now find out whether we have this node already (target animation channels 
				// are stored with a separate object ID)
				D3DSNode pcNode = findNode(mRootNode,name);
				int instanceNumber = 1;

				if ( pcNode != null)
				{
					// if the source is not a CHUNK_TRACKINFO block it wont be an object instance
					if (parent != D3DSHelper.CHUNK_TRACKINFO)
					{
						mCurrentNode = pcNode;
						break;
					}
					pcNode.mInstanceCount++;
					instanceNumber = pcNode.mInstanceCount;
				}
				pcNode = new D3DSNode();
				pcNode.mName = name;
				pcNode.mInstanceNumber = instanceNumber;

				// There are two unknown values which we can safely ignore
				stream.incPtr(4);

				// Now read the hierarchy position of the object
				short hierarchy = (short) (stream.getI2() &0xFFFF + 1);
				pcNode.mHierarchyPos   = hierarchy;
				pcNode.mHierarchyIndex = mLastNodeIndex;

				// And find a proper position in the graph for it
				if (mCurrentNode != null && mCurrentNode.mHierarchyPos == hierarchy)	{

					// add to the parent of the last touched node
					mCurrentNode.mParent.add(pcNode);
					mLastNodeIndex++;	
				}
				else if(hierarchy >= mLastNodeIndex)	{

					// place it at the current position in the hierarchy
					mCurrentNode.add(pcNode);
					mLastNodeIndex = hierarchy;
				}
				else	{
					// need to go back to the specified position in the hierarchy.
					inverseNodeSearch(pcNode,mCurrentNode);
					mLastNodeIndex++;	
				}
				// Make this node the current node
				mCurrentNode = pcNode;
				}
				break;

			case D3DSHelper.CHUNK_TRACKDUMMYOBJNAME:

				// This is the "real" name of a $$$DUMMY object
				{
//					const char* sz = (const char*) stream.GetPtr();
					ByteBuffer sz = stream.getPtr();
					int cnt =0;
					while (stream.getI1() != 0) cnt++;

					// If object name is DUMMY, take this one instead
					if (mCurrentNode.mName == "$$$DUMMY")	{
						//DefaultLogger::get().warn("3DS: Skipping dummy object name for non-dummy object");
						byte[] name = new byte[cnt];
						sz.get(name);
						mCurrentNode.mName = new String(name); //std::string(sz);
						break;
					}
				}
				break;

			case D3DSHelper.CHUNK_TRACKPIVOT:

				if ( D3DSHelper.CHUNK_TRACKINFO != parent) 
				{
					if(DefaultLogger.LOG_OUT)
						DefaultLogger.warn("3DS: Skipping pivot subchunk for non usual object");
					break;
				}

				// Pivot = origin of rotation and scaling
				mCurrentNode.vPivot.x = stream.getF4();
				mCurrentNode.vPivot.y = stream.getF4();
				mCurrentNode.vPivot.z = stream.getF4();
				break;


				// ////////////////////////////////////////////////////////////////////
				// POSITION KEYFRAME
			case D3DSHelper.CHUNK_TRACKPOS:
				{
				stream.incPtr(10);
				final int numFrames = stream.getI4();
				boolean sortKeys = false;

				// This could also be meant as the target position for
				// (targeted) lights and cameras
//				std::vector<aiVectorKey>* l;
				List<VectorKey> l;
				if ( D3DSHelper.CHUNK_TRACKCAMTGT == parent || D3DSHelper.CHUNK_TRACKLIGTGT == parent)	{
					l =  mCurrentNode.aTargetPositionKeys;
				}
				else l =  mCurrentNode.aPositionKeys;

//				l.reserve(numFrames);
				for (int i = 0; i < numFrames;++i)	{
					final int fidx = stream.getI4();

					// Setup a new position key
					VectorKey v = new VectorKey();
					v.mTime = (float)fidx;

					skipTCBInfo(stream);
					v.mValue.x = stream.getF4();
					v.mValue.y = stream.getF4();
					v.mValue.z = stream.getF4();

					// check whether we'll need to sort the keys
					if (!l.isEmpty() && v.mTime <= AssUtil.back(l).mTime)
						sortKeys = true;

					// Add the new keyframe to the list
					if(!l.contains(v))
						l.add(v);
				}

				// Sort all keys with ascending time values and remove duplicates?
				if (sortKeys)	{
//					std::stable_sort(l.begin(),l.end());
//					l.erase ( std::unique (l.begin(),l.end(),&KeyUniqueCompare<aiVectorKey>), l.end() );
					Collections.sort(l);
				}}

				break;

				// ////////////////////////////////////////////////////////////////////
				// CAMERA ROLL KEYFRAME
			case D3DSHelper.CHUNK_TRACKROLL:
				{
				// roll keys are accepted for cameras only
				if (parent != D3DSHelper.CHUNK_TRACKCAMERA)	{
					DefaultLogger.warn("3DS: Ignoring roll track for non-camera object");
					break;
				}
				boolean sortKeys = false;
//				std::vector<aiFloatKey>* l = &mCurrentNode.aCameraRollKeys;
				List<FloatKey> l = mCurrentNode.aCameraRollKeys;

				stream.incPtr(10);
				final int numFrames = stream.getI4();
//				l.reserve(numFrames);
				for (int i = 0; i < numFrames;++i)	{
					final int fidx = stream.getI4();

					// Setup a new position key
					FloatKey v = new FloatKey();
					v.mTime = (double)fidx;

					// This is just a single float 
					skipTCBInfo(stream);
					v.mValue = stream.getF4();

					// Check whether we'll need to sort the keys
					if (!l.isEmpty() && v.mTime <= AssUtil.back(l).mTime)
						sortKeys = true;

					// Add the new keyframe to the list
					if(!l.contains(v))
						l.add(v);
				}

				// Sort all keys with ascending time values and remove duplicates?
				if (sortKeys)	{
//					std::stable_sort(l.begin(),l.end());
//					l.erase ( std::unique (l.begin(),l.end(),&KeyUniqueCompare<aiFloatKey>), l.end() );
					
					Collections.sort(l);
				}}
				break;


				// ////////////////////////////////////////////////////////////////////
				// CAMERA FOV KEYFRAME
			case D3DSHelper.CHUNK_TRACKFOV:
				{
					DefaultLogger.error("3DS: Skipping FOV animation track. This is not supported");
				}
				break;


				// ////////////////////////////////////////////////////////////////////
				// ROTATION KEYFRAME
			case D3DSHelper.CHUNK_TRACKROTATE:
				{
				stream.incPtr(10);
				final int numFrames = stream.getI4();

				boolean sortKeys = false;
//				std::vector<aiQuatKey>* l = &mCurrentNode.aRotationKeys;
//				l.reserve(numFrames);
				List<QuatKey> l = mCurrentNode.aRotationKeys;
				final Vector3f axis =  new Vector3f();
				for (int i = 0; i < numFrames;++i)	{
					final int fidx = stream.getI4();
					skipTCBInfo(stream);

					QuatKey v = new QuatKey();
					v.mTime = (float)fidx;

					// The rotation keyframe is given as an axis-angle pair
					final float rad = stream.getF4();
					axis.x = stream.getF4();
					axis.y = stream.getF4();
					axis.z = stream.getF4();

//					if (!axis.x && !axis.y && !axis.z)
					if(axis.isZero())
						axis.y = 1.f;

					// Construct a rotation quaternion from the axis-angle pair
//					v.mValue = aiQuaternion(axis,rad);
					v.mValue.setFromAxisAngle(axis, rad);

					// Check whether we'll need to sort the keys
					if (!l.isEmpty() && v.mTime <= AssUtil.back(l).mTime)
						sortKeys = true;

					// add the new keyframe to the list
					if(!l.contains(v))
						l.add(v);
				}
				// Sort all keys with ascending time values and remove duplicates?
				if (sortKeys)	{
//					std::stable_sort(l.begin(),l.end());
//					l.erase ( std::unique (l.begin(),l.end(),&KeyUniqueCompare<aiQuatKey>), l.end() );
					Collections.sort(l);
				}}
				break;

				// ////////////////////////////////////////////////////////////////////
				// SCALING KEYFRAME
			case D3DSHelper.CHUNK_TRACKSCALE:
				{
				stream.incPtr(10);
				final int numFrames = stream.getI2();
				stream.incPtr(2);

				boolean sortKeys = false;
//				std::vector<aiVectorKey>* l = &mCurrentNode.aScalingKeys;
//				l.reserve(numFrames);
				List<VectorKey> l = mCurrentNode.aScalingKeys;
				for ( int i = 0; i < numFrames;++i)	{
					final int fidx = stream.getI4();
					skipTCBInfo(stream);

					// Setup a new key
					VectorKey v = new VectorKey();
					v.mTime = (float)fidx;

					// ... and read its value
					v.mValue.x = stream.getF4();
					v.mValue.y = stream.getF4();
					v.mValue.z = stream.getF4();

					// check whether we'll need to sort the keys
					if (!l.isEmpty() && v.mTime <= AssUtil.back(l).mTime)
						sortKeys = true;
					
					// Remove zero-scalings on singular axes - they've been reported to be there erroneously in some strange files
					if (v.mValue.x == 0) v.mValue.x = 1.f;
					if (v.mValue.y == 0) v.mValue.y = 1.f;
					if (v.mValue.z == 0) v.mValue.z = 1.f;

					if(!l.contains(v))
						l.add(v);
				}
				// Sort all keys with ascending time values and remove duplicates?
				if (sortKeys)	{
//					std::stable_sort(l.begin(),l.end());
//					l.erase ( std::unique (l.begin(),l.end(),&KeyUniqueCompare<aiVectorKey>), l.end() );
					Collections.sort(l);
				}}
				break;
			};
			
			stream.skipToReadLimit();                  
			stream.setReadLimit(oldReadLimit);         
			if (stream.getRemainingSizeToLimit() == 0) 
				return;                                 
		}
	}

	// -------------------------------------------------------------------
	/** Parse a texture chunk in the file
	*/
	void parseTextureChunk(StreamReader stream, D3DSTexture pcOut){
		while (true) {                                                       
			if (stream.getRemainingSizeToLimit() < Chunk.SIZE){ 
				return;                                                          
			}                                                                    
			Chunk chunk = new Chunk();                                            
			readChunk(stream, chunk);                                                
			int chunkSize = chunk.size-Chunk.SIZE;	             
		    if(chunkSize <= 0)                                                   
		        continue;                                                        
			final int oldReadLimit = stream.getReadLimit();                     
			stream.setReadLimit(stream.getCurrentPos() + chunkSize);
			
			// get chunk type
			switch (chunk.flag & 0xFFFF)
			{
			case D3DSHelper.CHUNK_MAPFILE:
				{
				// The material name string is already zero-terminated, but we need to be sure ...
//				const char* sz = (const char*)stream.GetPtr();
				ByteBuffer sz = stream.getPtr();
				int cnt = 0;
				while (stream.getI1() != 0)
					++cnt;
				byte[] name = new byte[cnt];
				sz.get(name);
				pcOut.mMapName =new String(name); //   std::string(sz,cnt);
				}
				break;


			case D3DSHelper.CHUNK_PERCENTF:
				// Manually parse the blend factor
				pcOut.mTextureBlend = stream.getF4();
				break;

			case D3DSHelper.CHUNK_PERCENTW:
				// Manually parse the blend factor
				pcOut.mTextureBlend = (float)(stream.getI2() & 0xFFFF) / 100.0f;
				break;

			case D3DSHelper.CHUNK_MAT_MAP_USCALE:
				// Texture coordinate scaling in the U direction
				pcOut.mScaleU = stream.getF4();
				if (0.0f == pcOut.mScaleU)
				{
					DefaultLogger.warn("Texture coordinate scaling in the x direction is zero. Assuming 1.");
					pcOut.mScaleU = 1.0f;
				}
				break;
			case D3DSHelper.CHUNK_MAT_MAP_VSCALE:
				// Texture coordinate scaling in the V direction
				pcOut.mScaleV = stream.getF4();
				if (0.0f == pcOut.mScaleV)
				{
					DefaultLogger.warn("Texture coordinate scaling in the y direction is zero. Assuming 1.");
					pcOut.mScaleV = 1.0f;
				}
				break;

			case D3DSHelper.CHUNK_MAT_MAP_UOFFSET:
				// Texture coordinate offset in the U direction
				pcOut.mOffsetU = -stream.getF4();
				break;

			case D3DSHelper.CHUNK_MAT_MAP_VOFFSET:
				// Texture coordinate offset in the V direction
				pcOut.mOffsetV = stream.getF4();
				break;

			case D3DSHelper.CHUNK_MAT_MAP_ANG:
				// Texture coordinate rotation, CCW in DEGREES
				pcOut.mRotation = -(float) Math.toRadians( stream.getF4() );
				break;

			case D3DSHelper.CHUNK_MAT_MAP_TILING:
				{
				final int iFlags = stream.getI2() & 0xFFFF;

				// Get the mapping mode (for both axes)
				if ((iFlags & 0x2) != 0)
					pcOut.mMapMode = TextureMapMode.aiTextureMapMode_Mirror;
				
				else if ((iFlags & 0x10) !=0)
					pcOut.mMapMode = TextureMapMode.aiTextureMapMode_Decal;
				
				// wrapping in all remaining cases
				else pcOut.mMapMode = TextureMapMode.aiTextureMapMode_Wrap;
				}
				break;
			};
			
			stream.skipToReadLimit();                  
			stream.setReadLimit(oldReadLimit);         
			if (stream.getRemainingSizeToLimit() == 0) 
				return;                                 
		}
	}

	// -------------------------------------------------------------------
	/**Split meshes by their materials and generate output aiMesh'es
	*/
	void convertMeshes(Scene pcOut){
//		std::vector<aiMesh*> avOutMeshes;
		List<Mesh> avOutMeshes = new ArrayList<Mesh>(mScene.mMeshes.size() * 2);
//		avOutMeshes.reserve(mScene.mMeshes.size() * 2);

		int iFaceCnt = 0,num = 0;
		String name;

		// we need to split all meshes by their materials
//		for (std::vector<D3DS::Mesh>::iterator i =  mScene.mMeshes.begin(); i != mScene.mMeshes.end();++i)	{
		IntArrayList[] aiSplit = new IntArrayList[mScene.mMaterials.size()];
		AssUtil.initArray(aiSplit);
		for(D3DSMesh i : mScene.mMeshes){
//			boost::scoped_array< std::vector<unsigned int> > aiSplit(new std::vector<unsigned int>[mScene.mMaterials.size()]);
			for(IntArrayList ial : aiSplit)
				ial.clear();

//			int name_length = AssUtil.assimp_itoa10(name.data,num++);
			name = Integer.toString(num++);

			int iNum = 0;
//			for (std::vector<unsigned int>::const_iterator a =  (*i).mFaceMaterials.begin();
//				a != (*i).mFaceMaterials.end();++a,++iNum)
			for(int k = 0; k < i.mFaceMaterials.size(); ++k,++iNum)
			{
				int a = i.mFaceMaterials.getInt(k);
				aiSplit[a].add(iNum);
			}
			// now generate submeshes
			for (int p = 0; p < mScene.mMaterials.size();++p)
			{
				if (aiSplit[p].isEmpty())	{
					continue;
				}
				Mesh meshOut = new Mesh();
				meshOut.mName = name;
				meshOut.mPrimitiveTypes = Mesh.aiPrimitiveType_TRIANGLE;

				// be sure to setup the correct material index
				meshOut.mMaterialIndex = p;

				// use the color data as temporary storage
//				meshOut.mColors[0] = (aiColor4D*)(&*i);  TODO 
				meshOut.tag = i;
				
				avOutMeshes.add(meshOut);

				// convert vertices
//				meshOut.mNumFaces = aiSplit[p].size();
				meshOut.mNumVertices = aiSplit[p].size()/*meshOut.mNumFaces*/*3;

				// allocate enough storage for faces
				meshOut.mFaces = new Face[aiSplit[p].size()/*meshOut.mNumFaces*/];
				iFaceCnt += meshOut.mFaces.length;

//				meshOut.mVertices = new aiVector3D[meshOut.mNumVertices];
//				meshOut.mNormals  = new aiVector3D[meshOut.mNumVertices];
				meshOut.mVertices = MemoryUtil.createFloatBuffer(meshOut.mNumVertices * 3, AssimpConfig.MESH_USE_NATIVE_MEMORY);
				meshOut.mNormals = MemoryUtil.createFloatBuffer(meshOut.mNumVertices * 3, AssimpConfig.MESH_USE_NATIVE_MEMORY);
				if (i.mTexCoords.size() > 0)
				{
					meshOut.mTextureCoords[0] = MemoryUtil.createFloatBuffer(meshOut.mNumVertices * 3, AssimpConfig.MESH_USE_NATIVE_MEMORY); //new aiVector3D[meshOut.mNumVertices];
				}
				for (int q = 0, base = 0; q < aiSplit[p].size();++q)
				{
					int index = aiSplit[p].getInt(q);
					Face face = meshOut.mFaces[q] = Face.createInstance(3);

//					face.mIndices = new unsigned int[3];
//					face.mNumIndices = 3;

					for (int a = 0; a < 3;++a,++base)
					{
						int idx = i.mFaces.get(index).mIndices[a];
//						meshOut.mVertices[base]  = (*i).mPositions[idx];
						int base0 = 3 * base + 0;
						int base1 = 3 * base + 1;
						int base2 = 3 * base + 2;
						Vector3f position = i.mPositions.get(idx);
						meshOut.mVertices.put(base0, position.x);
						meshOut.mVertices.put(base1, position.y);
						meshOut.mVertices.put(base2, position.z);
						
//						meshOut.mNormals [base]  = (*i).mNormals[idx];
						Vector3f normal = i.mNormals.get(idx);
						meshOut.mNormals.put(base0, normal.x);
						meshOut.mNormals.put(base1, normal.y);
						meshOut.mNormals.put(base2, normal.z);

						if (i.mTexCoords.size() > 0){
//							meshOut.mTextureCoords[0][base] = (*i).mTexCoords[idx];
							Vector3f texcoord = i.mTexCoords.get(idx);
							meshOut.mTextureCoords[0].put(base0, texcoord.x);
							meshOut.mTextureCoords[0].put(base1, texcoord.y);
							meshOut.mTextureCoords[0].put(base2, texcoord.z);
						}

//						face.mIndices[a] = base;
						face.set(a, base);
					}
				}
			}
		}

		// Copy them to the output array
//		pcOut.mNumMeshes = (unsigned int)avOutMeshes.size();
		pcOut.mMeshes = new Mesh[/*pcOut.mNumMeshes*/ avOutMeshes.size()];
//		for (unsigned int a = 0; a < pcOut.mNumMeshes;++a) {
//			pcOut.mMeshes[a] = avOutMeshes[a];
//		}
		avOutMeshes.toArray(pcOut.mMeshes);
		
		// We should have at least one face here
		if (iFaceCnt == 0) {
			throw new DeadlyImportError("No faces loaded. The mesh is empty");
		}
	}

	// -------------------------------------------------------------------
	/** Replace the default material in the scene
	*/
	void replaceDefaultMaterial(){
		// Try to find an existing material that matches the
		// typical default material setting:
		// - no textures
		// - diffuse color (in grey!)
		// NOTE: This is here to workaround the fact that some
		// exporters are writing a default material, too.
		int idx = 0xcdcdcdcd;
		for (int i = 0; i < mScene.mMaterials.size();++i)
		{
			D3DSMaterial material = mScene.mMaterials.get(i);
			String s = material.mName.toLowerCase();
//			for (std::string::iterator it = s.begin(); it != s.end(); ++it)
//				*it = ::tolower(*it);

			if (/*std::string::npos*/-1 == s.indexOf("default"))continue;

			if (material.mDiffuse.x != material.mDiffuse.y ||
				material.mDiffuse.x != material.mDiffuse.z)continue;

			if (material.sTexDiffuse.mMapName.length()   != 0	||
				material.sTexBump.mMapName.length()      != 0	|| 
				material.sTexOpacity.mMapName.length()   != 0	||
				material.sTexEmissive.mMapName.length()  != 0	||
				material.sTexSpecular.mMapName.length()  != 0	||
				material.sTexShininess.mMapName.length() != 0 )
			{
				continue;
			}
			idx = i;
		}
		if (0xcdcdcdcd == idx)idx = (int)mScene.mMaterials.size();

		// now iterate through all meshes and through all faces and
		// find all faces that are using the default material
		int cnt = 0;
//		for (std::vector<D3DS::Mesh>::iterator
//			i =  mScene.mMeshes.begin();
//			i != mScene.mMeshes.end();++i)
		for(D3DSMesh i : mScene.mMeshes)
		{
//			for (std::vector<int>::iterator
//				a =  (*i).mFaceMaterials.begin();
//				a != (*i).mFaceMaterials.end();++a)
			for(int k = 0; k < i.mFaceMaterials.size(); k++)
			{
				int a = i.mFaceMaterials.getInt(k);
				// NOTE: The additional check seems to be necessary,
				// some exporters seem to generate invalid data here
				if (0xcdcdcdcd == a)
				{
//					(*a) = idx;
					i.mFaceMaterials.set(k, idx);
					++cnt;
				}
				else if ( a >= mScene.mMaterials.size())
				{
//					(*a) = idx;
					i.mFaceMaterials.set(k, idx);
					if(DefaultLogger.LOG_OUT)
						DefaultLogger.warn("Material index overflow in 3DS file. Using default material");
					++cnt;
				}
			}
		}
		if (cnt > 0 && idx == mScene.mMaterials.size())
		{
			// We need to create our own default material
			D3DSMaterial sMat = new D3DSMaterial();
			sMat.mDiffuse.set(0.3f,0.3f,0.3f);
			sMat.mName = "%%%DEFAULT";
			mScene.mMaterials.add(sMat);

			if(DefaultLogger.LOG_OUT)
				DefaultLogger.info("3DS: Generating default material");
		}
	}

	// -------------------------------------------------------------------
	/** Convert the whole scene
	*/
	void convertScene(Scene pcOut){
		// Allocate enough storage for all output materials
//		pcOut->mNumMaterials = (unsigned int)mScene->mMaterials.size();
		pcOut.mMaterials    = new Material[mScene.mMaterials.size()/*pcOut->mNumMaterials*/];

		//  ... and convert the 3DS materials to aiMaterial's
		for (int i = 0; i < pcOut.getNumMaterials();++i)
		{
			Material pcNew = new Material();
			convertMaterial(mScene.mMaterials.get(i),pcNew);
			pcOut.mMaterials[i] = pcNew;
		}

		// Generate the output mesh list
		convertMeshes(pcOut);

		// Now copy all light sources to the output scene
		int numLights = mScene.mLights.size();
		if (numLights > 0)
		{
			pcOut.mLights = new Light[numLights];
//			::memcpy(pcOut->mLights,&mScene->mLights[0],sizeof(void*)*pcOut->mNumLights);
			mScene.mLights.toArray(pcOut.mLights);
		}

		// Now copy all cameras to the output scene
		int numCameras = mScene.mCameras.size();
		if (numCameras > 0)
		{
			pcOut.mCameras = new Camera[numCameras];
//			::memcpy(pcOut->mCameras,&mScene->mCameras[0],sizeof(void*)*pcOut->mNumCameras);
			mScene.mCameras.toArray(pcOut.mCameras);
		}
	}

	// -------------------------------------------------------------------
	/** generate unique vertices for a mesh
	*/
	void makeUnique(D3DSMesh sMesh){
		// TODO: really necessary? I don't think. Just a waste of memory and time
		// to do it now in a separate buffer. 

		// Allocate output storage
//		std::vector<aiVector3D> vNew  (sMesh.mFaces.size() * 3);
//		std::vector<aiVector3D> vNew2;
//		if (sMesh.mTexCoords.size())
//			vNew2.resize(sMesh.mFaces.size() * 3);
		Vector3f[] vNew = new Vector3f[sMesh.mFaces.size() * 3];
		Vector3f[] vNew2 = null;
		if(sMesh.mTexCoords.size() > 0)
			vNew2 = new Vector3f[sMesh.mFaces.size() * 3];

		for (int i = 0, base = 0; i < sMesh.mFaces.size();++i)
		{
			D3DSFace face = sMesh.mFaces.get(i);

			// Positions
			for (int a = 0; a < 3;++a,++base)
			{
				vNew[base] = sMesh.mPositions.get(face.mIndices[a]);
				if (sMesh.mTexCoords.size() > 0)
					vNew2[base] = sMesh.mTexCoords.get(face.mIndices[a]);

				face.mIndices[a] = base;
			}
		}
//		sMesh.mPositions = vNew;
//		sMesh.mTexCoords = vNew2;
		
		sMesh.mPositions.clear();
		for(Vector3f v : vNew)
			sMesh.mPositions.add(v);
		
		if(vNew2 != null){
			sMesh.mTexCoords.clear();
			for(Vector3f v : vNew2)
				sMesh.mTexCoords.add(v);
		}
	}

	// -------------------------------------------------------------------
	/** Add a node to the node graph
	*/
	void addNodeToGraph(Scene pcSOut,Node pcOut,D3DSNode pcIn,Matrix4f absTrafo){
//		std::vector<unsigned int> iArray;
//		iArray.reserve(3);
		IntArrayList iArray = new IntArrayList(3);
		Matrix4f abs = new Matrix4f();
		Vector3f tmp = new Vector3f();

		// Find all meshes with the same name as the node
		for (int a = 0; a < pcSOut.getNumMeshes();++a)
		{
//			D3DSMesh pcMesh = (const D3DS::Mesh*)pcSOut.mMeshes[a].mColors[0];
			D3DSMesh pcMesh = (D3DSMesh)pcSOut.mMeshes[a].tag;
//			ai_assert(NULL != pcMesh);

			if (pcIn.mName.equals(pcMesh.mName))
				iArray.add(a);
		}
		if (!iArray.isEmpty())
		{
			// The matrix should be identical for all meshes with the 
			// same name. It HAS to be identical for all meshes .....
//			D3DSMesh imesh = ((D3DSMesh)pcSOut.mMeshes[iArray[0]].mColors[0]);
			D3DSMesh imesh = (D3DSMesh) pcSOut.mMeshes[iArray.getInt(0)].tag;

			// Compute the inverse of the transformation matrix to move the
			// vertices back to their relative and local space
//			Matrix4f mInv = imesh.mMat, mInvTransposed = imesh.mMat;
//			mInv.Inverse();mInvTransposed.Transpose();
			Matrix4f mInv = Matrix4f.invert(imesh.mMat, null);
			Matrix4f mInvTransposed = Matrix4f.transpose(imesh.mMat, null);
			Vector3f pivot = pcIn.vPivot;

//			pcOut.mNumMeshes = (unsigned int)iArray.size();
			pcOut.mMeshes = new int[iArray.size()];
			for (int i = 0;i < iArray.size();++i)	{
				final int iIndex = iArray.getInt(i);
				final Mesh mesh = pcSOut.mMeshes[iIndex];

				if (mesh.mColors[1] == null)
				{
					// Transform the vertices back into their local space
					// fixme: consider computing normals after this, so we don't need to transform them
//					const aiVector3D* const pvEnd = mesh.mVertices + mesh.mNumVertices;
//					aiVector3D* pvCurrent = mesh.mVertices, *t2 = mesh.mNormals;
					final int pvEnd = mesh.mNumVertices;
					int pvCurrent = 0;

					for (; pvCurrent != pvEnd; ++pvCurrent/*, ++t2*/) {
//						*pvCurrent = mInv * (*pvCurrent);
//						*t2 = mInvTransposed * (*t2);
						tmp.x = mesh.mVertices.get(3 * pvCurrent);
						tmp.y = mesh.mVertices.get(3 * pvCurrent + 1);
						tmp.z = mesh.mVertices.get(3 * pvCurrent + 2);
						Matrix4f.transformVector(mInv, tmp, tmp);
						mesh.mVertices.put(3 * pvCurrent, tmp.x);
						mesh.mVertices.put(3 * pvCurrent + 1, tmp.y);
						mesh.mVertices.put(3 * pvCurrent + 2, tmp.z);
						
						tmp.x = mesh.mNormals.get(3 * pvCurrent);
						tmp.y = mesh.mNormals.get(3 * pvCurrent + 1);
						tmp.z = mesh.mNormals.get(3 * pvCurrent + 2);
						Matrix4f.transformVector(mInvTransposed, tmp, tmp);
						mesh.mNormals.put(3 * pvCurrent, tmp.x);
						mesh.mNormals.put(3 * pvCurrent + 1, tmp.y);
						mesh.mNormals.put(3 * pvCurrent + 2, tmp.z);
					}

					// Handle negative transformation matrix determinant . invert vertex x
					if (imesh.mMat.determinant() < 0.0f)
					{
						/* we *must* have normals */
//						for (pvCurrent = mesh.mVertices, t2 = mesh.mNormals; pvCurrent != pvEnd; ++pvCurrent, ++t2) {
						for(pvCurrent = 0; pvCurrent < pvEnd; pvCurrent++){
//							pvCurrent.x *= -1.f;
//							t2.x *= -1.f;
							int index = 3 * pvCurrent;
							mesh.mVertices.put(index, -mesh.mVertices.get(index));
							mesh.mNormals.put(index, -mesh.mNormals.get(index));
						}
						
						if(DefaultLogger.LOG_OUT)
							DefaultLogger.info("3DS: Flipping mesh X-Axis");
					}

					// Handle pivot point
					if (pivot.x != 0|| pivot.y!=0 || pivot.z!=0)
					{
//						for (pvCurrent = mesh.mVertices; pvCurrent != pvEnd; ++pvCurrent)	{
						for(pvCurrent = 0; pvCurrent < pvEnd; pvCurrent++){
							int index = 3 * pvCurrent;
							mesh.mVertices.put(index, mesh.mVertices.get(index) - pivot.x);
							mesh.mVertices.put(index + 1, mesh.mVertices.get(index + 1) - pivot.y);
							mesh.mVertices.put(index + 2, mesh.mVertices.get(index + 2) - pivot.z);
//							*pvCurrent -= pivot;
						}
					}

					mesh.mColors[1] = FloatBuffer.wrap(new float[]{});
				}
				else
					mesh.mColors[1] = FloatBuffer.wrap(new float[]{});

				// Setup the mesh index
				pcOut.mMeshes[i] = iIndex;
			}
		}

		// Setup the name of the node
		// First instance keeps its name otherwise something might break, all others will be postfixed with their instance number
		if (pcIn.mInstanceNumber > 1)
		{
//			char tmp[12];
//			ASSIMP_itoa10(tmp, pcIn.mInstanceNumber);
//			std::string tempStr = pcIn.mName + "_inst_";
//			tempStr += tmp;
//			pcOut.mName.Set(tempStr);
			pcOut.mName = pcIn.mName + "_inst_" + Integer.toString(pcIn.mInstanceNumber);
		}
		else
			pcOut.mName = pcIn.mName;

		// Now build the transformation matrix of the node
		// ROTATION
		if (pcIn.aRotationKeys.size() > 0){

			// FIX to get to Assimp's quaternion conventions
//			for (std::vector<aiQuatKey>::iterator it = pcIn.aRotationKeys.begin(); it != pcIn.aRotationKeys.end(); ++it) {
			for(QuatKey it : pcIn.aRotationKeys){
				it.mValue.w *= -1.f;
			}

//			pcOut.mTransformation = aiMatrix4x4( pcIn.aRotationKeys[0].mValue.GetMatrix() );
			pcIn.aRotationKeys.get(0).mValue.toMatrix(pcOut.mTransformation);
		}
		else if (pcIn.aCameraRollKeys.size() > 0) 
		{
//			aiMatrix4x4::RotationZ(AI_DEG_TO_RAD(- pcIn.aCameraRollKeys[0].mValue),
//				pcOut.mTransformation);
			pcOut.mTransformation.rotate((float)Math.toRadians(-pcIn.aCameraRollKeys.get(0).mValue), Vector3f.Z_AXIS);
		}

		// SCALING
		Matrix4f m = pcOut.mTransformation;
		if (pcIn.aScalingKeys.size() > 0)
		{
			Vector3f v = pcIn.aScalingKeys.get(0).mValue;
			m.m00 *= v.x; m.m01 *= v.x; m.m02 *= v.x;
			m.m10 *= v.y; m.m11 *= v.y; m.m12 *= v.y;
			m.m20 *= v.z; m.m21 *= v.z; m.m22 *= v.z;
		}

		// TRANSLATION
		if (pcIn.aPositionKeys.size() > 0)
		{
			Vector3f v = pcIn.aPositionKeys.get(0).mValue;
			m.m30 += v.x;
			m.m31 += v.y;
			m.m32 += v.z;
		}

		// Generate animation channels for the node
		if (pcIn.aPositionKeys.size()  > 1  || pcIn.aRotationKeys.size()   > 1 ||
			pcIn.aScalingKeys.size()   > 1  || pcIn.aCameraRollKeys.size() > 1 ||
			pcIn.aTargetPositionKeys.size() > 1)
		{
			Animation anim = pcSOut.mAnimations[0];
//			ai_assert(NULL != anim);

			if (pcIn.aCameraRollKeys.size() > 1)
			{
				if(DefaultLogger.LOG_OUT)
					DefaultLogger.debug("3DS: Converting camera roll track ...");

				// Camera roll keys - in fact they're just rotations
				// around the camera's z axis. The angles are given
				// in degrees (and they're clockwise).
//				pcIn.aRotationKeys.resize(pcIn.aCameraRollKeys.size());
				boolean needResize = pcIn.aRotationKeys.size() < pcIn.aCameraRollKeys.size();
				QuatKey[] rotationKeys = new QuatKey[pcIn.aCameraRollKeys.size()];
				pcIn.aRotationKeys.toArray(rotationKeys);
				if(needResize){
					for(int i = pcIn.aRotationKeys.size(); i < rotationKeys.length; i++)
						rotationKeys[i] = new QuatKey();
				}
				for (int i = 0; i < pcIn.aCameraRollKeys.size();++i)
				{
					QuatKey  q = rotationKeys[i];
					FloatKey f = pcIn.aCameraRollKeys.get(i);

					q.mTime  = (float) f.mTime;

					// FIX to get to Assimp quaternion conventions
//					q.mValue = aiQuaternion(0.f,0.f,AI_DEG_TO_RAD( /*-*/ f.mValue));
					AssUtil.aiQuaterniont(0.f,0.f,(float)Math.toRadians( /*-*/ f.mValue), q.mValue);
				}
				
				if(needResize){
					for(int i = pcIn.aRotationKeys.size(); i < rotationKeys.length; i++)
						pcIn.aRotationKeys.add(rotationKeys[i]);
				}
			}
			
			// Cameras or lights define their transformation in their parent node and in the
			// corresponding light or camera chunks. However, we read and process the latter
			// to to be able to return valid cameras/lights even if no scenegraph is given.
			for (int n = 0; n < pcSOut.getNumCameras();++n)	{
				if (pcSOut.mCameras[n].mName.equals(pcOut.mName)) {
					pcSOut.mCameras[n].mLookAt.set(0.f,0.f,1.f);
				}
			}
			for (int n = 0; n < pcSOut.getNumLights();++n)	{
				if (pcSOut.mLights[n].mName.equals(pcOut.mName)) {
					pcSOut.mLights[n].mDirection.set(0.f,0.f,1.f);
				}
			}

			// Allocate a new node anim and setup its name
//			NodeAnim nda = anim.mChannels[anim.mNumChannels++] = new aiNodeAnim();
			int numChannels = anim.getNumChannels();
			if(numChannels == 0){
				anim.mChannels = new NodeAnim[1];
			}else{
				anim.mChannels = Arrays.copyOf(anim.mChannels, numChannels + 1);
			}
			NodeAnim nda = anim.mChannels[numChannels] = new NodeAnim();
			nda.mNodeName = pcIn.mName;

			// POSITION keys
			if (pcIn.aPositionKeys.size()  > 0)
			{
//				nda.mNumPositionKeys = (unsigned int)pcIn.aPositionKeys.size();
				nda.mPositionKeys = new VectorKey[pcIn.aPositionKeys.size()];
//				::memcpy(nda.mPositionKeys,&pcIn.aPositionKeys[0],
//					sizeof(aiVectorKey)*nda.mNumPositionKeys);
				pcIn.aPositionKeys.toArray(nda.mPositionKeys);
			}

			// ROTATION keys
			if (pcIn.aRotationKeys.size()  > 0)
			{
//				nda.mNumRotationKeys = (unsigned int)pcIn.aRotationKeys.size();
				nda.mRotationKeys = new QuatKey[/*nda.mNumRotationKeys*/pcIn.aRotationKeys.size()];

				// Rotations are quaternion offsets
				Quaternion qua = new Quaternion();
				for (int n = 0; n < nda.getNumRotationKeys();++n)
				{
					QuatKey q = pcIn.aRotationKeys.get(n);

//					abs = (n ? abs * q.mValue : q.mValue);
					if(n > 0){
						Quaternion.mul(qua, q.mValue, qua);
					}else{
						qua.set(q.mValue);
					}
					qua.normalise();
					nda.mRotationKeys[n].mTime  = q.mTime;
					nda.mRotationKeys[n].mValue.set(qua);
				}
			}

			// SCALING keys
			if (pcIn.aScalingKeys.size()  > 0)
			{
//				nda.mNumScalingKeys = (unsigned int)pcIn.aScalingKeys.size();
				nda.mScalingKeys = new VectorKey[pcIn.aScalingKeys.size()/*nda.mNumScalingKeys*/];
//				::memcpy(nda.mScalingKeys,&pcIn.aScalingKeys[0],
//					sizeof(aiVectorKey)*nda.mNumScalingKeys);
				pcIn.aScalingKeys.toArray(nda.mScalingKeys);
			}
		}

		// Allocate storage for children 
//		pcOut.mNumChildren = (unsigned int)pcIn.mChildren.size();
		pcOut.mChildren = new Node[pcIn.mChildren.size()];

		// Recursively process all children
		final int size = pcIn.mChildren.size();
		for (int i = 0; i < size;++i)
		{
			pcOut.mChildren[i] = new Node();
			pcOut.mChildren[i].mParent = pcOut;
			addNodeToGraph(pcSOut,pcOut.mChildren[i],pcIn.mChildren.get(i),abs);
		}
	}

	// -------------------------------------------------------------------
	/** Search for a node in the graph.
	* Called recursively
	*/
	void inverseNodeSearch(D3DSNode pcNode,D3DSNode pcCurrent){
		if (pcCurrent == null)	{
			mRootNode.add(pcNode);
			return;
		}

		if (pcCurrent.mHierarchyPos == pcNode.mHierarchyPos)	{
			if(pcCurrent.mParent != null) {
				pcCurrent.mParent.add(pcNode);
			}
			else pcCurrent.add(pcNode);
			return;
		}
		
		inverseNodeSearch(pcNode,pcCurrent.mParent);
	}
	
	// Find out how many node animation channels we'll have finally
	int countTracks(D3DSNode node, int cnt)
	{
		//////////////////////////////////////////////////////////////////////////////
		// We will never generate more than one channel for a node, so
		// this is rather easy here.

		if (node.aPositionKeys.size()  > 1  || node.aRotationKeys.size()   > 1   ||
			node.aScalingKeys.size()   > 1  || node.aCameraRollKeys.size() > 1 ||
			node.aTargetPositionKeys.size()  > 1)
		{
			++cnt;

			// account for the additional channel for the camera/spotlight target position
			if (node.aTargetPositionKeys.size()  > 1)++cnt;
		}

		// Recursively process all children
		for (int i = 0; i < node.mChildren.size();++i)
			cnt = countTracks(node.mChildren.get(i),cnt);
		
		return cnt;
	}

	// -------------------------------------------------------------------
	/** Apply the master scaling factor to the mesh
	*/
	void applyMasterScale(Scene pScene){
		// There are some 3DS files with a zero scaling factor
		if (mMasterScale == 0)mMasterScale = 1.0f;
		else mMasterScale = 1.0f / mMasterScale;
		
		// Construct an uniform scaling matrix and multiply with it
		Matrix4f mat = pScene.mRootNode.mTransformation;
		mat.setIdentity();
		mat.m00 = mat.m11 = mat.m22 = mMasterScale;
	}

	// -------------------------------------------------------------------
	/** Clamp all indices in the file to a valid range
	*/
	void checkIndices(D3DSMesh sMesh){
//		for (std::vector< D3DS::Face >::iterator i =  sMesh.mFaces.begin(); i != sMesh.mFaces.end();++i)
		for(D3DSFace i : sMesh.mFaces)
		{
			// check whether all indices are in range
			for (int a = 0; a < 3;++a)
			{
				if (i.mIndices[a] >= sMesh.mPositions.size())
				{
					if(DefaultLogger.LOG_OUT)
						DefaultLogger.warn("3DS: Vertex index overflow)");
					i.mIndices[a] = sMesh.mPositions.size()-1;
				}
				if ( !sMesh.mTexCoords.isEmpty() && i.mIndices[a] >= sMesh.mTexCoords.size())
				{
					if(DefaultLogger.LOG_OUT)
					DefaultLogger.warn("3DS: Texture coordinate index overflow)");
					i.mIndices[a] = sMesh.mTexCoords.size()-1;
				}
			}
		}
	}

	// -------------------------------------------------------------------
	/** Skip the TCB info in a track key
	*/
	void skipTCBInfo(StreamReader stream){
		int flags = stream.getI2();

		if (flags == 0)	{
			// Currently we can't do anything with these values. They occur
			// quite rare, so it wouldn't be worth the effort implementing
			// them. 3DS ist not really suitable for complex animations,
			// so full support is not required.
			DefaultLogger.warn("3DS: Skipping TCB animation info");
		}

		if ((flags & D3DSHelper.KEY_USE_TENS)!=0) {
			stream.incPtr(4);
		}
		if ((flags & D3DSHelper.KEY_USE_BIAS)!=0) {
			stream.incPtr(4);
		}
		if ((flags & D3DSHelper.KEY_USE_CONT)!=0) {
			stream.incPtr(4);
		}
		if ((flags & D3DSHelper.KEY_USE_EASE_FROM)!=0) {
			stream.incPtr(4);
		}
		if ((flags & D3DSHelper.KEY_USE_EASE_TO)!=0) {
			stream.incPtr(4);
		}
	}
}
