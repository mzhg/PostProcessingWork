/** Defines the BHV motion capturing loader class */

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
package assimp.importer.bvh;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import assimp.common.Animation;
import assimp.common.AssUtil;
import assimp.common.AssimpConfig;
import assimp.common.BaseImporter;
import assimp.common.DeadlyImportError;
import assimp.common.FileUtils;
import assimp.common.Importer;
import assimp.common.ImporterDesc;
import assimp.common.Node;
import assimp.common.NodeAnim;
import assimp.common.QuatKey;
import assimp.common.Scene;
import assimp.common.SkeletonMeshBuilder;
import assimp.common.VectorKey;

/** Loader class to read Motion Capturing data from a .bvh file. <p>
*
* This format only contains a hierarchy of joints and a series of keyframes for
* the hierarchy. It contains no actual mesh data, but we generate a dummy mesh
* inside the loader just to be able to see something.
*/
public final class BVHLoader extends BaseImporter{
	
	static final ImporterDesc desc = new ImporterDesc(
		"BVH Importer (MoCap)",
		"",
		"",
		"",
		ImporterDesc.aiImporterFlags_SupportTextFlavour,
		0,
		0,
		0,
		0,
		"bvh"
	);

	/** Filename, for a verbose error message */
	String mFileName;

	/** Buffer to hold the loaded file */
	ByteBuffer mBuffer;

	/** Next char to read from the buffer */
//	std::vector<char>::const_iterator mReader;
	int mReader;

	/** Current line, for error messages */
	int mLine;

	/** Collected list of nodes. Will be bones of the dummy mesh some day, addressed by their array index.
	* Also contain the motion data for the node's channels
	*/
	final List<BVHNode> mNodes = new ArrayList<BVHNode>();

	/** basic Animation parameters */
	float mAnimTickDuration;
	int mAnimNumFrames;

	boolean noSkeletonMesh;
	final StringBuilder mToken = new StringBuilder();
	final byte[] tmpBuf = new byte[64];
	final int[] out = new int[1];
	
	@Override
	protected boolean canRead(String pFile, InputStream pIOHandler, boolean checkSig) {
		// check file extension 
		String extension = getExtension(pFile);
		
		if( extension.equals("bvh"))
			return true;

		if ((extension.isEmpty() || checkSig) && pIOHandler != null) {
			String tokens[] = {"HIERARCHY"};
			try {
				return searchFileHeaderForToken(pIOHandler,pFile,tokens);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return false;
	}
	
	@Override
	public void setupProperties(Importer pImp) {
		noSkeletonMesh = pImp.getPropertyInteger(AssimpConfig.AI_CONFIG_IMPORT_NO_SKELETON_MESHES,0) != 0;
	}

	@Override
	protected ImporterDesc getInfo() {return desc;}

	@Override
	protected void internReadFile(File pFile, Scene pScene) {
		mBuffer = FileUtils.loadText(pFile, false, AssimpConfig.LOADING_USE_NATIVE_MEMORY);
		mReader = 0;
		mLine = 1;
		readStructure( pScene);

		if (!noSkeletonMesh) {
			// build a dummy mesh for the skeleton so that we see something at least
			new SkeletonMeshBuilder( pScene, null, false);
		}

		// construct an animation from all the motion data we read
		createAnimation( pScene);
	}
	
	/** Reads the file */
	void readStructure(Scene pScene){
		// first comes hierarchy
		String header = getNextToken();
		if( !header.equals("HIERARCHY"))
			throw new DeadlyImportError( "Expected header string \"HIERARCHY\".");
		readHierarchy( pScene);

		// then comes the motion data
		String motion = getNextToken();
		if(!motion.equals("MOTION"))
			throw new DeadlyImportError( "Expected beginning of motion data \"MOTION\".");
		readMotion( pScene);
	}

	/** Reads the hierarchy */
	void readHierarchy(Scene pScene){
		String root = getNextToken();
		if( !root.equals("ROOT"))
			throw new DeadlyImportError( "Expected root node \"ROOT\".");

		// Go read the hierarchy from here
		pScene.mRootNode = readNode();
	}

	/** Reads a node and recursively its childs and returns the created node. */
	Node readNode(){
		// first token is name
		String nodeName = getNextToken();
		if( nodeName.isEmpty() || nodeName.equals("{"))
			throw new DeadlyImportError(String.format( "Expected node name, but found \"%s\".", nodeName));

		// then an opening brace should follow
		String openBrace = getNextToken();
		if( openBrace != "{")
			throw new DeadlyImportError(String.format( "Expected opening brace \"{\", but found \"%s\".", openBrace));

		// Create a node
		Node node = new Node( nodeName);
//		std::vector<aiNode*> childNodes;
		List<Node> childNodes = new ArrayList<Node>();

		// and create an bone entry for it
		BVHNode internNode;
		mNodes.add(internNode = new BVHNode( node));
//		BVHNode& internNode = mNodes.back();

		// now read the node's contents
		while(true)
		{
			String token = getNextToken();

			// node offset to parent node
			if( token.equals("OFFSET"))
				readNodeOffset( node);
			else if( token.equals("CHANNELS"))
				readNodeChannels( internNode);
			else if( token.equals("JOINT"))
			{
				// child node follows
				Node child = readNode();
				child.mParent = node;
				childNodes.add( child);
			} 
			else if( token.equals("End"))
			{
				// The real symbol is "End Site". Second part comes in a separate token
				String siteToken = getNextToken();
				if( !siteToken.equals("Site"))
					throw new DeadlyImportError(String.format( "Expected \"End Site\" keyword, but found \"%s %s\".", token , siteToken));

				Node child = readEndSite( nodeName);
				child.mParent = node;
				childNodes.add( child);
			} 
			else if( token.equals("}"))
			{
				// we're done with that part of the hierarchy
				break;
			} else
			{
				// everything else is a parse error
				throw new DeadlyImportError(String.format( "Unknown keyword \"%s\".", token));
			}
		}

		// add the child nodes if there are any
		if( childNodes.size() > 0)
		{
//			node->mNumChildren = childNodes.size();
//			node->mChildren = new aiNode*[node->mNumChildren];
//			std::copy( childNodes.begin(), childNodes.end(), node->mChildren);
			node.mChildren = childNodes.toArray(new Node[childNodes.size()]);
		}

		// and return the sub-hierarchy we built here
		return node;
	}

	/** Reads an end node and returns the created node. */
	Node readEndSite(String pParentName){
		// check opening brace
		String openBrace = getNextToken();
		if( !openBrace.equals("{"))
			throw new DeadlyImportError(String.format( "Expected opening brace \"{\", but found \"%s\".", openBrace));

		// Create a node
		Node node = new Node( "EndSite_" + pParentName);

		// now read the node's contents. Only possible entry is "OFFSET"
		while(true)
		{
			String token = getNextToken();

			// end node's offset
			if( token.equals("OFFSET"))
			{
				readNodeOffset( node);
			} 
			else if( token.equals("}"))
			{
				// we're done with the end node
				break;
			} else
			{
				// everything else is a parse error
				throw new DeadlyImportError(String.format( "Unknown keyword \"%s\".", token));
			}
		}

		// and return the sub-hierarchy we built here
		return node;
	}

	/** Reads a node offset for the given node */
	void readNodeOffset(Node pNode){
		// Offset consists of three floats to read
		float x = getNextTokenAsFloat();
		float y = getNextTokenAsFloat();
		float z = getNextTokenAsFloat();

		// build a transformation matrix from it
//		pNode->mTransformation = aiMatrix4x4( 1.0f, 0.0f, 0.0f, offset.x, 0.0f, 1.0f, 0.0f, offset.y,
//			0.0f, 0.0f, 1.0f, offset.z, 0.0f, 0.0f, 0.0f, 1.0f);
		Matrix4f mat = pNode.mTransformation;
		mat.setRow(0, 1.0f, 0.0f, 0.0f, x);
		mat.setRow(1, 0.0f, 1.0f, 0.0f, y);
		mat.setRow(2, 0.0f, 0.0f, 1.0f, z);
		mat.setRow(3, 0.0f, 0.0f, 0.0f, 1);
	}

	/** Reads the animation channels into the given node */
	void readNodeChannels( BVHNode pNode){
		// number of channels. Use the float reader because we're lazy
		float numChannelsFloat = getNextTokenAsFloat();
		int numChannels = (int)numChannelsFloat;

		for(int a = 0; a < numChannels; a++)
		{
			String channelToken = getNextToken();

			if( channelToken.equals("Xposition"))
				pNode.mChannels.add( BVHNode.Channel_PositionX);
			else if( channelToken.equals("Yposition"))
				pNode.mChannels.add( BVHNode.Channel_PositionY);
			else if( channelToken.equals("Zposition"))
				pNode.mChannels.add( BVHNode.Channel_PositionZ);
			else if( channelToken.equals("Xrotation"))
				pNode.mChannels.add( BVHNode.Channel_RotationX);
			else if( channelToken.equals("Yrotation"))
				pNode.mChannels.add( BVHNode.Channel_RotationY);
			else if( channelToken.equals("Zrotation"))
				pNode.mChannels.add( BVHNode.Channel_RotationZ);
			else
				throw new DeadlyImportError(String.format( "Invalid channel specifier \"%s\".", channelToken));
		}
	}

	/** Reads the motion data */
	void readMotion(Scene pScene){
		// Read number of frames
		String tokenFrames = getNextToken();
		if( !tokenFrames.equals("Frames:"))
			throw new DeadlyImportError(String.format( "Expected frame count \"Frames:\", but found \"%s\".", tokenFrames));

		float numFramesFloat = getNextTokenAsFloat();
		mAnimNumFrames = (int) numFramesFloat;

		// Read frame duration
		String tokenDuration1 = getNextToken();
		String tokenDuration2 = getNextToken();
		if( !tokenDuration1.equals("Frame") || !tokenDuration2.equals("Time:"))
			throw new DeadlyImportError(String.format( "Expected frame duration \"Frame Time:\", but found \"%s %s\".", tokenDuration1 , tokenDuration2));

		mAnimTickDuration = getNextTokenAsFloat();

		// resize value vectors for each node
//		for( std::vector<Node>::iterator it = mNodes.begin(); it != mNodes.end(); ++it)
//			it->mChannelValues.reserve( it->mChannels.size() * mAnimNumFrames);

		// now read all the data and store it in the corresponding node's value vector
		for(int frame = 0; frame < mAnimNumFrames; ++frame)
		{
			// on each line read the values for all nodes
//			for( std::vector<Node>::iterator it = mNodes.begin(); it != mNodes.end(); ++it)
			for(BVHNode it : mNodes)
			{
				// get as many values as the node has channels
				for(int c = 0; c < it.mChannels.size(); ++c)
					it.mChannelValues.add( getNextTokenAsFloat());
			}

			// after one frame worth of values for all nodes there should be a newline, but we better don't rely on it
		}
	}

	/** Retrieves the next token */
	String getNextToken(){
		// skip any preceeding whitespace
		final int end = mBuffer.limit();
		int c;
		while( mReader != end)
		{
			c = mBuffer.get(mReader) & 0xFF;
//			if( !isspace( *mReader))
			if(!Character.isSpaceChar(c))
				break;

			// count lines
			if(c == '\n')
				mLine++;

			++mReader;
		}

		// collect all chars till the next whitespace. BVH is easy in respect to that.
		if(mToken.length() > 0)
			// clear the token.
			mToken.delete(0, mToken.length());
		while( mReader != end)
		{
			c = mBuffer.get(mReader) & 0xFF;
//			if( isspace( *mReader))
			if(Character.isSpaceChar(c))
				break;

//			token.push_back( *mReader);
			mToken.append((char)c);
			++mReader;

			// little extra logic to make sure braces are counted correctly
//			if( token == "{" || token == "}")
			if(AssUtil.equals(mToken, "{") || AssUtil.equals(mToken, "}"))
				break;
		}

		// empty token means end of file, which is just fine
		return mToken.toString();
	}

	/** Reads the next token as a float */
	@SuppressWarnings("deprecation")
	float getNextTokenAsFloat(){
		String token = getNextToken();
		if( token.isEmpty())
			throw new DeadlyImportError( "Unexpected end of file while trying to read a float");

		// check if the float is valid by testing if the atof() function consumed every char of the token
//		const char* ctoken = token.c_str();
//		float result = 0.0f;
//		ctoken = fast_atoreal_move<float>( ctoken, result);
//
//		if( ctoken != token.c_str() + token.length())
//			ThrowException( boost::str( boost::format( "Expected a floating point number, but found \"%s\".") % token));
		byte[] buf = null;
		if(token.length() > tmpBuf.length)
			buf = new byte[token.length()];
		else{
			buf = tmpBuf;
		}
		token.getBytes(0, token.length(), buf, 0);
		if(token.length() < buf.length)
			Arrays.fill(buf, token.length(), buf.length, (byte)0);
		float result = (float) AssUtil.fast_atoreal_move(buf, 0, out, true);
		if(out[0] != token.length())
			throw new DeadlyImportError(String.format("Expected a floating point number, but found \"%s\".",token));
		
		return result;
	}

	/** Constructs an animation for the motion data and stores it in the given scene */
	void createAnimation(Scene pScene){
		// create the animation
//		pScene->mNumAnimations = 1;
//		pScene->mAnimations = new aiAnimation*[1];
//		aiAnimation* anim = new aiAnimation;
//		pScene->mAnimations[0] = anim;
		Animation anim = new Animation();
		pScene.mAnimations = new Animation[]{anim};

		// put down the basic parameters
		anim.mName = "Motion";
		anim.mTicksPerSecond = 1.0f /( mAnimTickDuration);
		anim.mDuration = ( mAnimNumFrames - 1);

		// now generate the tracks for all nodes
//		anim->mNumChannels = mNodes.size();
		anim.mChannels = new NodeAnim[mNodes.size()];

		// FIX: set the array elements to NULL to ensure proper deletion if an exception is thrown
//		for (unsigned int i = 0; i < anim->mNumChannels;++i)
//			anim->mChannels[i] = NULL;

		for(int a = 0; a < anim.mChannels.length; a++)
		{
			BVHNode node = mNodes.get(a);
			String nodeName = node.mNode.mName;
			NodeAnim nodeAnim = new NodeAnim();
			anim.mChannels[a] = nodeAnim;
			nodeAnim.mNodeName = nodeName;

			// translational part, if given
			if( node.mChannels.size() == 6)
			{
//				nodeAnim->mNumPositionKeys = mAnimNumFrames;
				nodeAnim.mPositionKeys = new VectorKey[mAnimNumFrames];
//				aiVectorKey* poskey = nodeAnim->mPositionKeys;
				final float[] channelValues = node.mChannelValues.elements();
				final int[] channels = node.mChannels.elements();
				for(int fr = 0; fr < mAnimNumFrames; ++fr)
				{
					VectorKey poskey = nodeAnim.mPositionKeys[fr] = new VectorKey();
					poskey.mTime = fr;

					// Now compute all translations in the right order
					for(int channel = 0; channel < 3; ++channel)
					{
						switch( channels[channel])
						{	
						case BVHNode.Channel_PositionX: poskey.mValue.x = channelValues[fr * node.mChannels.size() + channel]; break;
						case BVHNode.Channel_PositionY: poskey.mValue.y = channelValues[fr * node.mChannels.size() + channel]; break;
						case BVHNode.Channel_PositionZ: poskey.mValue.z = channelValues[fr * node.mChannels.size() + channel]; break;
						default: throw new DeadlyImportError( "Unexpected animation channel setup at node " + nodeName );
						}
					}
//					++poskey;
				}
			} else
			{
				// if no translation part is given, put a default sequence
//				aiVector3D nodePos( node.mNode->mTransformation.a4, node.mNode->mTransformation.b4, node.mNode->mTransformation.c4);
//				nodeAnim->mNumPositionKeys = 1;
//				nodeAnim->mPositionKeys = new aiVectorKey[1];
//				nodeAnim->mPositionKeys[0].mTime = 0.0;
//				nodeAnim->mPositionKeys[0].mValue = nodePos;
				final Matrix4f trans = node.mNode.mTransformation;
				VectorKey key = new VectorKey();
				key.mValue.set(trans.m30, trans.m31, trans.m32);
			}

			// rotation part. Always present. First find value offsets
			{
				int rotOffset  = 0;
				if( node.mChannels.size() == 6)
				{
					// Offset all further calculations
					rotOffset = 3;
				} 

				// Then create the number of rotation keys
//				nodeAnim->mNumRotationKeys = mAnimNumFrames;
				nodeAnim.mRotationKeys = new QuatKey[mAnimNumFrames];
//				aiQuatKey* rotkey = nodeAnim->mRotationKeys;
				Matrix4f rotMatrix = new Matrix4f();
				final float[] channelValues = node.mChannelValues.elements();
				for(int fr = 0; fr < mAnimNumFrames; ++fr)
				{
					rotMatrix.setIdentity();
					QuatKey rotkey = nodeAnim.mRotationKeys[fr] = new QuatKey();
					for(int channel = 0; channel < 3; ++channel)
					{
						// translate ZXY euler angels into a quaternion
						final float angle = channelValues[fr * node.mChannels.size() + rotOffset + channel] * (float)( Math.PI) / 180.0f;

						// Compute rotation transformations in the right order
						switch (node.mChannels.get(rotOffset+channel)) 
						{
						case BVHNode.Channel_RotationX: rotMatrix.rotate(angle, Vector3f.X_AXIS); break;  //aiMatrix4x4::RotationX( angle, temp); rotMatrix *= aiMatrix3x3( temp); break;
						case BVHNode.Channel_RotationY: rotMatrix.rotate(angle, Vector3f.Y_AXIS); break;  //aiMatrix4x4::RotationY( angle, temp); rotMatrix *= aiMatrix3x3( temp);	break;
						case BVHNode.Channel_RotationZ: rotMatrix.rotate(angle, Vector3f.Z_AXIS); break;  //aiMatrix4x4::RotationZ( angle, temp); rotMatrix *= aiMatrix3x3( temp); break;
						default: throw new DeadlyImportError( "Unexpected animation channel setup at node " + nodeName );
						}
					}

					rotkey.mTime = fr;
//					rotkey->mValue = aiQuaternion( rotMatrix);
					rotkey.mValue.setFromMatrix(rotMatrix);
//					++rotkey;
				}
			}

			// scaling part. Always just a default track
			{
//				nodeAnim->mNumScalingKeys = 1;
//				nodeAnim->mScalingKeys = new aiVectorKey[1];
//				nodeAnim->mScalingKeys[0].mTime = 0.0;
//				nodeAnim->mScalingKeys[0].mValue.Set( 1.0f, 1.0f, 1.0f);
				nodeAnim.mScalingKeys = new VectorKey[]{new VectorKey(0f, new Vector3f(1, 1, 1))};
			}
		}
	}

}
