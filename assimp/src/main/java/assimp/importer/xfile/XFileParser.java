package assimp.importer.xfile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.ZipInputStream;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import assimp.common.AssimpConfig;
import assimp.common.DeadlyImportError;
import assimp.common.DefaultLogger;
import assimp.common.FileUtils;
import assimp.common.MemoryUtil;
import assimp.common.Mesh;
import assimp.common.ParsingUtil;
import assimp.common.QuatKey;
import assimp.common.VectorKey;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class XFileParser {

	private static boolean DEBUG = false;
	// Magic identifier for MSZIP compressed data
	private static final int MSZIP_MAGIC = 0x4B43;
	private static final int MSZIP_BLOCK = 32786;
	
    int mMajorVersion, mMinorVersion; ///< version numbers
	boolean mIsBinaryFormat; ///< true if the file is in binary, false if it's in text form
	int mBinaryFloatSize; ///< float size in bytes, either 4 or 8
	// counter for number arrays in binary format
	int mBinaryNumCount;

//	const char* P;
//	const char* End;
	ByteBuffer pBuffer;
	ParsingUtil parsing;

	/// Line number when reading in text format
	int mLineNumber;

	/// Imported data
	XScene mScene;	
	
	/** Constructor. Creates a data structure out of the XFile given in the memory block. 
	 * @param pBuffer Null-terminated memory buffer containing the XFile
	 * @throws IOException 
	 */
	public XFileParser(ByteBuffer pBuffer) throws IOException{
		parsing = new ParsingUtil(pBuffer);
		if(parsing.strncmp("xof "))
			throw new DeadlyImportError("Header mismatch, file is not an XFile.");
		
		// read version. It comes in a four byte format such as "0302"
		mMajorVersion = ((parsing.get(4) - 48) & 0xFF) * 10 + (parsing.get(5) - 48) & 0xFF;
		mMinorVersion = ((parsing.get(6) - 48) & 0xFF) * 10 + (parsing.get(7) - 48) & 0xFF;
		
		if(DEBUG)
			System.out.println("Version: (" + mMajorVersion + ", " + mMinorVersion+ ")");
		
		parsing.setCurrent(8);
		
		boolean compressed = false;
		ByteBuffer uncompressed = null;

		// txt - pure ASCII text format
		if(!parsing.strncmp("txt "))
			mIsBinaryFormat = false;

		// bin - Binary format
		else if(!parsing.strncmp("bin "))
			mIsBinaryFormat = true;

		// tzip - Inflate compressed text format
		else if(!parsing.strncmp("tzip"))
		{
			mIsBinaryFormat = false;
			compressed = true;
		}
		// bzip - Inflate compressed binary format
		else if(!parsing.strncmp("bzip"))
		{
			mIsBinaryFormat = true;
			compressed = true;
		}else
		{
			String msg = parsing.getString(8, 12);
			throw new DeadlyImportError("Unsupported xfile format " + msg);
		}
		
		// float size
		mBinaryFloatSize = (0xFF & (parsing.get(12) - 48)) * 1000
			+ (0xFF & (parsing.get(13) - 48)) * 100
			+ (0xFF & (parsing.get(14) - 48)) * 10
			+ (0xFF & (parsing.get(15) - 48));
		
		if(DEBUG){
			System.out.println("Data Format: binary = " + mIsBinaryFormat + ", compressed = " + compressed);
			System.out.println("floatSize = " + mBinaryFloatSize);
		}
		
		if( mBinaryFloatSize != 32 && mBinaryFloatSize != 64)
//			ThrowException( boost::str( boost::format( )
//				% mBinaryFloatSize));
			throw new DeadlyImportError(String.format("Unknown float size %d specified in xfile header.", mBinaryFloatSize) );
		
		// The x format specifies size in bits, but we work in bytes
		mBinaryFloatSize /= 8;
		
		parsing.setCurrent(16);
		
		// If this is a compressed X file, apply the inflate algorithm to it
		if(compressed){
			/* ///////////////////////////////////////////////////////////////////////  
			 * COMPRESSED X FILE FORMAT
			 * ///////////////////////////////////////////////////////////////////////
			 *    [xhead]
			 *    2 major
			 *    2 minor
			 *    4 type    // bzip,tzip
			 *    [mszip_master_head]
			 *    4 unkn    // checksum?
			 *    2 unkn    // flags? (seems to be constant)
			 *    [mszip_head]
			 *    2 ofs     // offset to next section
			 *    2 magic   // 'CK'
			 *    ... ofs bytes of data
			 *    ... next mszip_head
			 *
			 *  http://www.kdedevelopers.org/node/3181 has been very helpful.
			 * ///////////////////////////////////////////////////////////////////////
			 */
			
			// skip unknown data (checksum, flags?)
			parsing.skip(6);
			
			// First find out how much storage we'll need. Count sections.
			int p1 = parsing.getCurrent();
			int end = pBuffer.limit();
			int est_out = 0;
			
			while(p1 + 3 < end){
				// read next offset
				int ofs = pBuffer.getShort(p1) & 0xFFFF;
				p1 += 2;
				
				if (ofs >= MSZIP_BLOCK)
					throw new DeadlyImportError("X: Invalid offset to next MSZIP compressed block");
				
				// check magic word
				int magic = pBuffer.getShort(p1) & 0xFFFF;
				p1 += 2;
				
				if (magic != MSZIP_MAGIC)
					throw new DeadlyImportError("X: Unsupported compressed format, expected MSZIP header");

				// and advance to the next offset
				p1 += ofs;
				est_out += MSZIP_BLOCK; // one decompressed block is 32786 in size
			}
			
			// Allocate storage and terminating zero and do the actual uncompressing
			uncompressed = MemoryUtil.createByteBuffer(est_out, AssimpConfig.LOADING_USE_NATIVE_MEMORY);
			byte[] buf = new byte[MSZIP_BLOCK];
			byte[] tmp = new byte[1024];
			while(parsing.getCurrent() + 3 < end){
				int ofs = pBuffer.getShort(parsing.getCurrent()) & 0xFFFF;
				parsing.skip(4);
				
				pBuffer.position(parsing.getCurrent());
				if(DEBUG){
					System.out.println("position = " + parsing.getCurrent() + ", remaining = " + pBuffer.remaining() + ", ofs = " + ofs);
				}
				ofs = Math.min(ofs, pBuffer.remaining());
				pBuffer.get(buf, 0, ofs);
				FileUtils.save("test_data.x", buf, 0, ofs);
				ByteArrayInputStream _in = new ByteArrayInputStream(buf, 0, ofs);
				CheckedInputStream checkedInput = new CheckedInputStream(_in, new CRC32());
				ZipInputStream in = new ZipInputStream(checkedInput);
				if(in.getNextEntry() != null){
					int len;
					while((len = in.read(tmp)) != -1){
						if(DEBUG)
							System.out.println("len = " + len);
						uncompressed.put(tmp, 0, len);
					}
				}
				parsing.skip(ofs);
			}
			
			uncompressed.flip();
			pBuffer = uncompressed;
			parsing = new ParsingUtil(pBuffer);
			
			// FIXME: we don't need the compressed data anymore, could release
			// it already for better memory usage. Consider breaking const-co.
			DefaultLogger.info("Successfully decompressed MSZIP-compressed file");
		}else{
			// start reading here
			readUntilEndOfLine();
		}
		
		this.pBuffer = pBuffer;
		mScene = new XScene();
		parseFile();
		
		// filter the imported hierarchy for some degenerated cases
		if( mScene.mRootNode != null) {
			filterHierarchy( mScene.mRootNode);
		}
	}

	/** Returns the temporary representation of the imported data */
	public XScene getImportedData() { return mScene; }
	
	void parseFile(){
		boolean running = true;
		while( running )
		{
			// read name of next object
			String objectName = getNextToken();
			if (objectName.length() == 0){
				if(DEBUG){
					System.out.println("Read a empty string at line: " + mLineNumber);
				}
				break;
			}else{
				if(DEBUG){
					System.out.println("Get a Token: " + objectName);
				}
			}

			// parse specific object
			if( objectName.equals("template"))
				parseDataObjectTemplate();
			else
			if( objectName.equals("Frame"))
				parseDataObjectFrame( null);
			else
			if( objectName.equals("Mesh"))
			{
				// some meshes have no frames at all
				XMesh mesh = new XMesh();
				parseDataObjectMesh( mesh);
				mScene.mGlobalMeshes.add( mesh);
			} else
			if( objectName.equals("AnimTicksPerSecond"))
				parseDataObjectAnimTicksPerSecond();
			else
			if( objectName.equals("AnimationSet"))
				parseDataObjectAnimationSet();
			else
			if( objectName.equals("Material"))
			{
				// Material outside of a mesh or node
				XMaterial material = new XMaterial(); 
				parseDataObjectMaterial( material);
				mScene.mGlobalMaterials.add( material);
			} else
			if( objectName.equals("}"))
			{
				// whatever?
				DefaultLogger.warn("} found in dataObject");
			} else
			{
				// unknown format
				DefaultLogger.warn("Unknown data object in animation of .x file");
				parseUnknownDataObject();
			}
		}
	}
	
	void parseDataObjectTemplate(){
		// parse a template data object. Currently not stored.
		/*String name = */readHeadOfDataObject( /*&name*/);

		// read GUID
		/*String guid = */getNextToken();

		// read and ignore data members
		boolean running = true;
		while ( running )
		{
			String s = getNextToken();

			if( s.endsWith("}"))
				break;

			if( s.length() == 0)
				throw new DeadlyImportError( "Unexpected end of file reached while parsing template definition");
		}
	}
	
	void parseDataObjectFrame( XNode pParent){
		// A coordinate frame, or "frame of reference." The Frame template
		// is open and can contain any object. The Direct3D extensions (D3DX)
		// mesh-loading functions recognize Mesh, FrameTransformMatrix, and
		// Frame template instances as child objects when loading a Frame
		// instance.
		String name = readHeadOfDataObject(/*&name*/);

		// create a named node and place it at its parent, if given
		XNode node = new XNode( pParent);
		node.mName = name;
		if( pParent != null)
		{
			pParent.mChildren.add( node);
		} else
		{
			// there might be multiple root nodes
			if( mScene.mRootNode != null)
			{
				// place a dummy root if not there
				if( !mScene.mRootNode.mName.equals("$dummy_root"))
				{
					XNode exroot = mScene.mRootNode;
					mScene.mRootNode = new XNode(null);
					mScene.mRootNode.mName = "$dummy_root";
					mScene.mRootNode.mChildren.add( exroot);
					exroot.mParent = mScene.mRootNode;
				}
				// put the new node as its child instead
				mScene.mRootNode.mChildren.add( node);
				node.mParent = mScene.mRootNode;
			} else
			{
				// it's the first node imported. place it as root
				mScene.mRootNode = node;
			}
		}

		// Now inside a frame.
		// read tokens until closing brace is reached.
		boolean running = true;
		while ( running )
		{
			String objectName = getNextToken();
			if (objectName.length() == 0)
				throw new DeadlyImportError( "Unexpected end of file reached while parsing frame");

			if( objectName.equals("}"))
				break; // frame finished
			else
			if( objectName.equals("Frame"))
				parseDataObjectFrame( node); // child frame
			else
			if( objectName.equals("FrameTransformMatrix"))
				parseDataObjectTransformationMatrix( node.mTrafoMatrix);
			else
			if( objectName.equals("Mesh"))
			{
				XMesh mesh = new XMesh();
				node.mMeshes.add( mesh);
				parseDataObjectMesh( mesh);
			} else
			{
				DefaultLogger.warn("Unknown data object in frame in x file");
				parseUnknownDataObject();
			}
		}
	}
	
	void readMatrix(Matrix4f pMatrix){
		// read its components
		pMatrix.m00 = ReadFloat(); pMatrix.m01 = ReadFloat();
		pMatrix.m02 = ReadFloat(); pMatrix.m03 = ReadFloat();
		pMatrix.m10 = ReadFloat(); pMatrix.m11 = ReadFloat();
		pMatrix.m12 = ReadFloat(); pMatrix.m13 = ReadFloat();
		pMatrix.m20 = ReadFloat(); pMatrix.m21 = ReadFloat();
		pMatrix.m22 = ReadFloat(); pMatrix.m23 = ReadFloat();
		pMatrix.m30 = ReadFloat(); pMatrix.m31 = ReadFloat();
		pMatrix.m32 = ReadFloat(); pMatrix.m33 = ReadFloat();
	}
	
	void parseDataObjectTransformationMatrix( Matrix4f pMatrix){
		// read header, we're not interested if it has a name
		readHeadOfDataObject();

		readMatrix(pMatrix);

		// trailing symbols
		CheckForSemicolon();
		CheckForClosingBrace();
	}
	
	void parseDataObjectMesh( XMesh pMesh){
		String name = readHeadOfDataObject( /*&name*/);
		final boolean natived = AssimpConfig.LOADING_USE_NATIVE_MEMORY;
		// read vertex count
		int numVertices = ReadInt();
//		pMesh.mPositions.resize( numVertices);
		pMesh.mPositions = MemoryUtil.createFloatBuffer(numVertices * 3, natived);

		// read vertices
		Vector3f vec = new Vector3f();
		for(int a = 0; a < numVertices; a++){
			/*pMesh->mPositions[a] = */ReadVector3(vec);
			vec.store(pMesh.mPositions);
		}
		pMesh.mPositions.flip();
		
		// read position faces
		int numPosFaces = ReadInt();
//		pMesh->mPosFaces.resize( numPosFaces);
		pMesh.mPosFaces = new ArrayList<XFace>(numPosFaces);
		for(int a = 0; a < numPosFaces; a++)
		{
			int numIndices = ReadInt();
			if( numIndices < 3)
//				ThrowException( boost::str( boost::format( "Invalid index count %1% for face %2%.") % numIndices % a));
				throw new DeadlyImportError(String.format("Invalid index count %d for face %d.", numIndices, a));

			// read indices
//			Face& face = pMesh->mPosFaces[a];
			XFace face = new XFace();
			int[] indices = new int[numIndices];
			for(int b = 0; b < numIndices; b++)
//				face.mIndices.push_back( ReadInt());
			{
				indices[b] = ReadInt();
			}
			face.mIndices = indices;
			pMesh.mPosFaces.add(face);
			TestForSeparator();
		}

		// here, other data objects may follow
		boolean running = true;
		while ( running )
		{
			String objectName = getNextToken();

			if( objectName.length() == 0)
				throw new DeadlyImportError( "Unexpected end of file while parsing mesh structure");
			else
			if( objectName.equals("}"))
				break; // mesh finished
			else
			if( objectName.equals("MeshNormals"))
				parseDataObjectMeshNormals( pMesh);
			else
			if( objectName.equals("MeshTextureCoords"))
				parseDataObjectMeshTextureCoords( pMesh);
			else
			if( objectName.equals("MeshVertexColors"))
				parseDataObjectMeshVertexColors( pMesh);
			else
			if( objectName.equals("MeshMaterialList"))
				parseDataObjectMeshMaterialList( pMesh);
			else
			if( objectName.equals("VertexDuplicationIndices"))
				parseUnknownDataObject(); // we'll ignore vertex duplication indices
			else
			if( objectName.equals("XSkinMeshHeader"))
				parseDataObjectSkinMeshHeader( pMesh);
			else
			if( objectName.equals("SkinWeights"))
				parseDataObjectSkinWeights( pMesh);
			else
			{
				DefaultLogger.warn("Unknown data object in mesh in x file");
				parseUnknownDataObject();
			}
		}
	}
	
	void parseDataObjectSkinWeights( XMesh pMesh){
		readHeadOfDataObject();

		String transformNodeName = getNextTokenAsString();

		XBone bone;
		pMesh.mBones.add( bone = new XBone());
//		Bone& bone = pMesh->mBones.back();
		bone.mName = transformNodeName;

		// read vertex weights
		int numWeights = ReadInt();
		bone.mWeights = new ArrayList<XBoneWeight>(numWeights);

		for( int a = 0; a < numWeights; a++)
		{
			XBoneWeight weight = new XBoneWeight();
			weight.mVertex = ReadInt();
			bone.mWeights.add( weight);
		}

		// read vertex weights
		for(int a = 0; a < numWeights; a++)
			bone.mWeights.get(a).mWeight = ReadFloat();

		// read matrix offset
//		bone.mOffsetMatrix.a1 = ReadFloat(); bone.mOffsetMatrix.b1 = ReadFloat();
//		bone.mOffsetMatrix.c1 = ReadFloat(); bone.mOffsetMatrix.d1 = ReadFloat();
//		bone.mOffsetMatrix.a2 = ReadFloat(); bone.mOffsetMatrix.b2 = ReadFloat();
//		bone.mOffsetMatrix.c2 = ReadFloat(); bone.mOffsetMatrix.d2 = ReadFloat();
//		bone.mOffsetMatrix.a3 = ReadFloat(); bone.mOffsetMatrix.b3 = ReadFloat();
//		bone.mOffsetMatrix.c3 = ReadFloat(); bone.mOffsetMatrix.d3 = ReadFloat();
//		bone.mOffsetMatrix.a4 = ReadFloat(); bone.mOffsetMatrix.b4 = ReadFloat();
//		bone.mOffsetMatrix.c4 = ReadFloat(); bone.mOffsetMatrix.d4 = ReadFloat();
		bone.mOffsetMatrix = new Matrix4f();
		readMatrix(bone.mOffsetMatrix);

		CheckForSemicolon();
		CheckForClosingBrace();
	}
	
	void parseDataObjectSkinMeshHeader( XMesh pMesh){
		readHeadOfDataObject();

		/*unsigned int maxSkinWeightsPerVertex =*/ ReadInt();
		/*unsigned int maxSkinWeightsPerFace =*/ ReadInt();
		/*unsigned int numBonesInMesh = */ReadInt();

		CheckForClosingBrace();
	}
	
	void parseDataObjectMeshNormals( XMesh pMesh){
		readHeadOfDataObject();
		final boolean natived = AssimpConfig.LOADING_USE_NATIVE_MEMORY;
		// read count
		int numNormals = ReadInt();
//		pMesh.mNormals.resize( numNormals);
		pMesh.mNormals = MemoryUtil.createFloatBuffer(numNormals * 3, natived);

		// read normal vectors
		Vector3f nor = new Vector3f();
		for(int a = 0; a < numNormals; a++){
//			pMesh->mNormals[a] = ReadVector3();
			ReadVector3(nor);
			nor.store(pMesh.mNormals);
		}
		pMesh.mNormals.flip();

		// read normal indices
		int numFaces = ReadInt();
		if( numFaces != pMesh.mPosFaces.size())
			throw new DeadlyImportError( "Normal face count does not match vertex face count.");

		pMesh.mNormFaces = new ArrayList<XFace>(numFaces);
		for( int a = 0; a < numFaces; a++)
		{
			int numIndices = ReadInt();
			XFace face;
			pMesh.mNormFaces.add( face = new XFace());
//			Face& face = pMesh->mNormFaces.back();
			int[] indices = new int[numIndices];
			for(int b = 0; b < numIndices; b++)
//				face.mIndices.push_back( ReadInt());
				indices[b] = ReadInt();
			face.mIndices = indices;

			TestForSeparator();
		}

		CheckForClosingBrace();
	}
	
	void parseDataObjectMeshTextureCoords( XMesh pMesh){
		final boolean natived = AssimpConfig.LOADING_USE_NATIVE_MEMORY;
		
		readHeadOfDataObject();
		if( pMesh.mNumTextures + 1 > Mesh.AI_MAX_NUMBER_OF_TEXTURECOORDS)
			throw new DeadlyImportError( "Too many sets of texture coordinates");

//		std::vector<aiVector2D>& coords = pMesh->mTexCoords[pMesh->mNumTextures++];
		int numCoords = ReadInt();

		if( numCoords != pMesh.mPositions.remaining()/3)
			throw new DeadlyImportError( "Texture coord count does not match vertex count");

//		coords.resize( numCoords);
		FloatBuffer coords = pMesh.mTexCoords[pMesh.mNumTextures++] = MemoryUtil.createFloatBuffer(numCoords * 2, natived);
		Vector2f vec = new Vector2f();
		for(int a = 0; a < numCoords; a++){
//			coords[a] = ReadVector2();
			ReadVector2(vec);
			vec.store(coords);
		}
		coords.flip();

		CheckForClosingBrace();
	}
	
	void parseDataObjectMeshVertexColors( XMesh pMesh){
		final boolean natived = AssimpConfig.LOADING_USE_NATIVE_MEMORY;
		
		readHeadOfDataObject();
		if( pMesh.mNumColorSets + 1 > Mesh.AI_MAX_NUMBER_OF_COLOR_SETS)
			throw new DeadlyImportError( "Too many colorsets");
//		std::vector<aiColor4D>& colors = pMesh->mColors[pMesh->mNumColorSets++];

		int numColors = ReadInt();
		if( numColors != pMesh.mPositions.remaining()/4)
			throw new DeadlyImportError( "Vertex color count does not match vertex count");

//		colors.resize( numColors, aiColor4D( 0, 0, 0, 1));
		FloatBuffer colors = pMesh.mColors[pMesh.mNumColorSets++] = MemoryUtil.createFloatBuffer(numColors * 4, natived);
		Vector4f color = new Vector4f();
		for( int a = 0; a < numColors; a++)
		{
			int index = ReadInt();
			if( index >= pMesh.mPositions.remaining()/4)
				throw new DeadlyImportError( "Vertex color index out of bounds");

			/*colors[index] = */ReadRGBA(color);
			colors.position(index * 4);
			color.store(colors);
			// HACK: (thom) Maxon Cinema XPort plugin puts a third separator here, kwxPort puts a comma.
			// Ignore gracefully.
			if( !mIsBinaryFormat)
			{
				findNextNoneWhiteSpace();
				int b = parsing.get();
				if( b == ';' || b == ',')
					parsing.inCre();
			}
		}
		colors.position(0);
		CheckForClosingBrace();
	}
	
	void parseDataObjectMeshMaterialList( XMesh pMesh){
		readHeadOfDataObject();

		// read material count
		/*unsigned int numMaterials =*/ ReadInt();
		// read non triangulated face material index count
		int numMatIndices = ReadInt();

		// some models have a material index count of 1... to be able to read them we
		// replicate this single material index on every face
		if( numMatIndices != pMesh.mPosFaces.size() && numMatIndices != 1)
			throw new DeadlyImportError( "Per-Face material index count does not match face count.");

		if(pMesh.mFaceMaterials == null)
			pMesh.mFaceMaterials = new IntArrayList(numMatIndices);
		// read per-face material indices
		for(int a = 0; a < numMatIndices; a++)
			pMesh.mFaceMaterials.add( ReadInt());

		// in version 03.02, the face indices end with two semicolons.
		// commented out version check, as version 03.03 exported from blender also has 2 semicolons
		if( !mIsBinaryFormat) // && MajorVersion == 3 && MinorVersion <= 2)
		{
//			if(P < End && *P == ';')
//				++P;
			
			if(parsing.hasNext() && parsing.get() == ';')
				parsing.inCre();
		}

		// if there was only a single material index, replicate it on all faces
		while( pMesh.mFaceMaterials.size() < pMesh.mPosFaces.size())
			pMesh.mFaceMaterials.add( pMesh.mFaceMaterials.getInt(0));

		// read following data objects
		boolean running = true;
		while ( running )
		{
			String objectName = getNextToken();
			if( objectName.length() == 0)
				throw new DeadlyImportError( "Unexpected end of file while parsing mesh material list.");
			else
			if( objectName.equals("}"))
				break; // material list finished
			else
			if( objectName.equals("{"))
			{
				// template materials 
				String matName = getNextToken();
				XMaterial material = new XMaterial();
				material.mIsReference = true;
				material.mName = matName;
				pMesh.mMaterials.add( material);

				CheckForClosingBrace(); // skip }
			} else
			if( objectName.equals("Material"))
			{
				XMaterial material;
				pMesh.mMaterials.add( material = new XMaterial());
				parseDataObjectMaterial( /*&pMesh->mMaterials.back()*/ material);
			} else
			if( objectName.equals(";"))
			{
				// ignore
			} else
			{
				DefaultLogger.warn("Unknown data object in material list in x file");
				parseUnknownDataObject();
			}
		}
	}
	
	void parseDataObjectMaterial( XMaterial pMaterial){
		String matName = readHeadOfDataObject( /*&matName*/);
		if( matName.isEmpty())
			matName = "material" + mLineNumber ; //boost::lexical_cast<std::string>( mLineNumber);
		pMaterial.mName = matName;
		pMaterial.mIsReference = false;

		// read material values
		ReadRGBA(pMaterial.mDiffuse); 
		pMaterial.mSpecularExponent = ReadFloat();
		ReadRGB(pMaterial.mSpecular); 
		ReadRGB(pMaterial.mEmissive); 

		// read other data objects
		boolean running = true;
		while ( running )
		{
			String objectName = getNextToken();
			if( objectName.length() == 0)
				throw new DeadlyImportError( "Unexpected end of file while parsing mesh material");
			else
			if( objectName.equals("}"))
				break; // material finished
			else
			if( objectName.equals("TextureFilename") || objectName.equals("TextureFileName"))
			{
				// some exporters write "TextureFileName" instead.
				String texname = parseDataObjectTextureFilename(/* texname*/);
				pMaterial.mTextures.add( new XTexEntry( texname));
			} else
			if( objectName.equals("NormalmapFilename") || objectName.equals("NormalmapFileName"))
			{
				// one exporter writes out the normal map in a separate filename tag
				String texname = parseDataObjectTextureFilename( /*texname*/);
				pMaterial.mTextures.add(new XTexEntry( texname, true));
			} else
			{
				DefaultLogger.warn("Unknown data object in material in x file");
				parseUnknownDataObject();
			}
		}
	}
	
	void parseDataObjectAnimTicksPerSecond(){
		readHeadOfDataObject();
		mScene.mAnimTicksPerSecond = ReadInt();
		CheckForClosingBrace();
	}
	
	void parseDataObjectAnimationSet(){
		String animName = readHeadOfDataObject( /*&animName*/);

		XAnimation anim = new XAnimation();
		mScene.mAnims.add( anim);
		anim.mName = animName;

		boolean running = true;
		while ( running )
		{
			String objectName = getNextToken();
			if( objectName.length() == 0)
				throw new DeadlyImportError( "Unexpected end of file while parsing animation set.");
			else
			if( objectName.equals("}"))
				break; // animation set finished
			else
			if( objectName.equals("Animation"))
				parseDataObjectAnimation( anim);
			else
			{
				DefaultLogger.warn("Unknown data object in animation set in x file");
				parseUnknownDataObject();
			}
		}
	}
	
	void parseDataObjectAnimation( XAnimation pAnim){
		readHeadOfDataObject();
		XAnimBone banim = new XAnimBone();
		pAnim.mAnims.add( banim);

		boolean running = true;
		while( running )
		{
			String objectName = getNextToken();

			if( objectName.length() == 0)
				throw new DeadlyImportError( "Unexpected end of file while parsing animation.");
			else
			if( objectName.equals("}"))
				break; // animation finished
			else
			if( objectName.equals("AnimationKey"))
				parseDataObjectAnimationKey( banim);
			else
			if( objectName.equals("AnimationOptions"))
				parseUnknownDataObject(); // not interested
			else
			if( objectName.equals("{"))
			{
				// read frame name
				banim.mBoneName = getNextToken();
				CheckForClosingBrace();
			} else
			{
				DefaultLogger.warn("Unknown data object in animation in x file");
				parseUnknownDataObject();
			}
		}
	}
	
	void parseDataObjectAnimationKey( XAnimBone pAnimBone){
		readHeadOfDataObject();

		// read key type
		int keyType = ReadInt();

		// read number of keys
		int numKeys = ReadInt();

		for(int a = 0; a < numKeys; a++)
		{
			// read time
			int time = ReadInt();

			// read keys
			switch( keyType)
			{
				case 0: // rotation quaternion
				{
					// read count
					if( ReadInt() != 4)
						throw new DeadlyImportError( "Invalid number of arguments for quaternion key in animation");

					QuatKey key = new QuatKey();
					key.mTime = time;
					key.mValue.w = ReadFloat();
					key.mValue.x = ReadFloat();
					key.mValue.y = ReadFloat();
					key.mValue.z = ReadFloat();
					pAnimBone.mRotKeys.add( key);

					CheckForSemicolon();
					break;
				}

				case 1: // scale vector
				case 2: // position vector
				{
					// read count
					if( ReadInt() != 3)
						throw new DeadlyImportError( "Invalid number of arguments for vector key in animation");

					VectorKey key = new VectorKey();
					key.mTime = time;
					ReadVector3(key.mValue);

					if( keyType == 2)
						pAnimBone.mPosKeys.add( key);
					else
						pAnimBone.mScaleKeys.add( key);

					break;
				}

				case 3: // combined transformation matrix
				case 4: // denoted both as 3 or as 4
				{
					// read count
					if( ReadInt() != 16)
						throw new DeadlyImportError( "Invalid number of arguments for matrix key in animation");

					// read matrix
					XMatrixKey key = new XMatrixKey();
					key.mTime = time;
//					key.mMatrix.a1 = ReadFloat(); key.mMatrix.b1 = ReadFloat();
//					key.mMatrix.c1 = ReadFloat(); key.mMatrix.d1 = ReadFloat();
//					key.mMatrix.a2 = ReadFloat(); key.mMatrix.b2 = ReadFloat();
//					key.mMatrix.c2 = ReadFloat(); key.mMatrix.d2 = ReadFloat();
//					key.mMatrix.a3 = ReadFloat(); key.mMatrix.b3 = ReadFloat();
//					key.mMatrix.c3 = ReadFloat(); key.mMatrix.d3 = ReadFloat();
//					key.mMatrix.a4 = ReadFloat(); key.mMatrix.b4 = ReadFloat();
//					key.mMatrix.c4 = ReadFloat(); key.mMatrix.d4 = ReadFloat();
					readMatrix(key.mMatrix);
					pAnimBone.mTrafoKeys.add( key);

					CheckForSemicolon();
					break;
				}

				default:
					throw new DeadlyImportError(String.format( "Unknown key type %1% in animation.", keyType));
			} // end switch

			// key separator
			CheckForSeparator();
		}

		CheckForClosingBrace();
	}
	
	String parseDataObjectTextureFilename( /*String pName*/){
		readHeadOfDataObject();
		String pName = getNextTokenAsString( /*pName*/);
		CheckForClosingBrace();

		// FIX: some files (e.g. AnimationTest.x) have "" as texture file name
		if (pName.length() == 0)
		{
			DefaultLogger.warn("Length of texture file name is zero. Skipping this texture.");
		}

		// some exporters write double backslash paths out. We simply replace them if we find them
//		while( pName.find( "\\\\") != std::string::npos)  TODO
//			pName.replace( pName.find( "\\\\"), 2, "\\");
//		int index = pName.indexOf("\\\\");
//		if(index >= 0){
//			StringBuilder sb = new StringBuilder();
//			int prev = 0;
//			while(true){
//				if(index > 0){
//					sb.append(pName.sub)
//				}
//			}
//		}
		return pName;
	}
	
	void parseUnknownDataObject(){
		// find opening delimiter
		boolean running = true;
		while( running )
		{
			String t = getNextToken();
			if( t.length() == 0)
				throw new DeadlyImportError( "Unexpected end of file while parsing unknown segment.");

			if( t.equals("{"))
				break;
		}

		int counter = 1;

		// parse until closing delimiter
		while( counter > 0)
		{
			String t = getNextToken();

			if( t.length() == 0)
				throw new DeadlyImportError( "Unexpected end of file while parsing unknown segment.");

			if( t.equals("{"))
				++counter;
			else
			if( t.equals("}"))
				--counter;
		}
	}

	//! places pointer to next begin of a token, and ignores comments
	void findNextNoneWhiteSpace(){
		if( mIsBinaryFormat)
			return;

		boolean running = true;
		while( running )
		{
			int b = parsing.get();
			while(parsing.hasNext() && /*isspace( (unsigned char) *P)*/ ParsingUtil.isSpace((byte)b))
			{
				if( b == '\n')
					mLineNumber++;
//				++P;
				parsing.inCre();
				b = parsing.get();
			}

//			if( P >= End)
			if(!parsing.hasNext())
				return;

			// check if this is a comment
			b = parsing.get();
			int c = parsing.get(parsing.getCurrent() + 1); // TODO mabe have problems.
			if( (b == '/' && c == '/') ||	b == '#')
				readUntilEndOfLine();
			else
				break;
		}
	}

	/** returns next parseable token. Returns empty string if no token there */
	String getNextToken(){
		String s = "";

		// process binary-formatted file
		if( mIsBinaryFormat)
		{
			// in binary mode it will only return NAME and STRING token
			// and (correctly) skip over other tokens.

//			if( End - P < 2) return s;
			if(parsing.remaining() < 2){
				if(DEBUG)
					System.out.println("There is not enough data!");
				return s;
			}
			int tok = readBinWord();
			if(DEBUG) System.out.println("tok = " + tok);
			int len;

			// standalone tokens
			switch( tok) 
			{
				case 1:
					// name token
					if(parsing.remaining() < 4) return s;
					len = ReadBinDWord();
					if(parsing.remaining() < len) return s;
//					s = std::string(P, len);
					s = parsing.getString(len);
//					P += len;
					parsing.skip(len);
					if(DEBUG)
						System.out.println("tok1 = " + s);
					return s;
				case 2:
					// string token
					if(parsing.remaining() < 4) return s;
					len = ReadBinDWord();
					if(parsing.remaining() < len) return s;
//					s = std::string(P, len);
					s = parsing.getString(len);
//					P += (len + 2);
					parsing.skip(len + 2);
					return s;
				case 3:
					// integer token
//					P += 4;
					parsing.skip(4);
					return "<integer>";
				case 5:
					// GUID token
//					P += 16;
					parsing.skip(16);
					return "<guid>";
				case 6:
					if(parsing.remaining() < 4) return s;
					len = ReadBinDWord();
//					P += (len * 4);
					parsing.skip(len * 4);
					return "<int_list>";
				case 7:
					if(parsing.remaining() < 4) return s;
					len = ReadBinDWord();
//					P += (len * mBinaryFloatSize);
					parsing.skip(len * mBinaryFloatSize);
					return "<flt_list>";
				case 0x0a:
					return "{";
				case 0x0b:
					return "}";
				case 0x0c:
					return "(";
				case 0x0d:
					return ")";
				case 0x0e:
					return "[";
				case 0x0f:
					return "]";
				case 0x10:
					return "<";
				case 0x11:
					return ">";
				case 0x12:
					return ".";
				case 0x13:
					return ",";
				case 0x14:
					return ";";
				case 0x1f:
					return "template";
				case 0x28:
					return "WORD";
				case 0x29:
					return "DWORD";
				case 0x2a:
					return "FLOAT";
				case 0x2b:
					return "DOUBLE";
				case 0x2c:
					return "CHAR";
				case 0x2d:
					return "UCHAR";
				case 0x2e:
					return "SWORD";
				case 0x2f:
					return "SDWORD";
				case 0x30:
					return "void";
				case 0x31:
					return "string";
				case 0x32:
					return "unicode";
				case 0x33:
					return "cstring";
				case 0x34:
					return "array";
			}
			
			return s;
		}
		// process text-formatted file
		else
		{
			findNextNoneWhiteSpace();
			if( /*P >= End*/ !parsing.hasNext()){
				if(DEBUG)
					System.out.println("There is no more data!");
				return s;
			}

			StringBuilder _s = new StringBuilder(s);
			while( /*(P < End)*/parsing.hasNext() && !/*isspace( (unsigned char) *P)*/ parsing.isSpace())
			{
				// either keep token delimiters when already holding a token, or return if first valid char
				int b = parsing.get();
				if( b == ';' || b == '}' || b == '{' || b == ',')
				{
					if(_s.length() == 0){
//						s.append( P++, 1);
						_s.append((char)b);
						parsing.inCre();
					}
					break; // stop for delimiter
				}
//				s.append( P++, 1);
				_s.append((char)b);
				parsing.inCre();
			}
			
			return _s.toString();
		}
	}

	//! reads header of dataobject including the opening brace.
	//! returns false if error happened, and writes name of object
	//! if there is one
	String readHeadOfDataObject( /*= NULL*/){
		String nameOrBrace = getNextToken();
		if(!nameOrBrace.equals("{"))
		{
//			if( poName)
//				*poName = nameOrBrace;

			if( !getNextToken().equals("{"))
				throw new DeadlyImportError( "Opening brace expected.");
			
			return nameOrBrace;
		}
		
		return "";  // TODO
	}

	//! checks for closing curly brace, throws exception if not there
	void CheckForClosingBrace(){
		if( !getNextToken().equals("}"))
			throw new DeadlyImportError( "Closing brace expected.");
	}

	//! checks for one following semicolon, throws exception if not there
	void CheckForSemicolon(){
		if( mIsBinaryFormat)
			return;

		if( !getNextToken().equals(";"))
			throw new DeadlyImportError( "Semicolon expected.");
	}

	//! checks for a separator char, either a ',' or a ';'
	void CheckForSeparator(){
		if( mIsBinaryFormat)
			return;

		String token = getNextToken();
		if(!token.equals(",") && !token.equals(";"))
			throw new DeadlyImportError( "Separator character (';' or ',') expected.");
	}

	/// tests and possibly consumes a separator char, but does nothing if there was no separator
	void TestForSeparator(){
		if( mIsBinaryFormat)
		    return;

		  findNextNoneWhiteSpace();
//		  if( P >= End)
		  if( !parsing.hasNext())
		    return;

		  // test and skip
		  int p = parsing.get();
		  if( p == ';' || p == ',')
		    parsing.inCre();
	}

	//! reads a x file style string
	String getNextTokenAsString(/*String poString*/){
		String poString = "";
		if( mIsBinaryFormat)
		{
			poString = getNextToken();
			return poString;
		}

		findNextNoneWhiteSpace();
		if( !parsing.hasNext())
			throw new DeadlyImportError( "Unexpected end of file while parsing string");

		int b = parsing.get();
		if( b != '"')
			throw new DeadlyImportError( "Expected quotation mark.");
//		++P;
		parsing.inCre();
		int end = parsing.getCurrent();
//		while( P < End && *P != '"')
//			poString.append( P++, 1);
		StringBuilder sb = new StringBuilder();  // TODO performs problems.
		while( parsing.hasNext() && ( b = parsing.get()) != '"'){
			sb.append((char)b);
			parsing.inCre();
		}

//		if( P >= End-1)  // only remain one character.
		if(parsing.remaining() < 2)
			throw new DeadlyImportError( "Unexpected end of file while parsing string");

		int c = parsing.get(parsing.getCurrent() + 1);
//		if( P[1] != ';' || P[0] != '"')
		if( c != ';' || b != '"')
			throw new DeadlyImportError( "Expected quotation mark and semicolon at the end of a string.");
//		P+=2;
		parsing.skip(2);
		return sb.toString();
	}

	void readUntilEndOfLine(){
		if( mIsBinaryFormat)
			return;

		while(parsing.hasNext())
		{
			int b = parsing.get();
			if( b == '\n' || b == '\r')
			{
//				++P; 
				parsing.inCre();
				mLineNumber++;
				return;
			}

//			++P;
			parsing.inCre();
		}
	}

	int readBinWord(){
//		ai_assert(End - P >= 2);
		assert parsing.remaining() >= 2;
		
//		const unsigned char* q = (const unsigned char*) P;
//		unsigned short tmp = q[0] | (q[1] << 8);
//		P += 2;
		if(DEBUG)
			System.out.println("current: " + parsing.getCurrent());
		int q = pBuffer.getShort(parsing.getCurrent()) & 0xFFFF;
		parsing.inCre(2);
		return q;
	}
	
	int ReadBinDWord(){
		assert parsing.remaining() >= 4;
		
		int q = pBuffer.getInt(parsing.getCurrent());
		parsing.inCre(4);
		return q;
	}
	
	int ReadInt(){
		if( mIsBinaryFormat)
		{
			if( mBinaryNumCount == 0 && parsing.remaining() >= 2)
			{
				int tmp = readBinWord(); // 0x06 or 0x03
				if( tmp == 0x06 && parsing.remaining() >= 4) // array of ints follows
					mBinaryNumCount = ReadBinDWord();
				else // single int follows
					mBinaryNumCount = 1; 
			}

			--mBinaryNumCount;
			if ( parsing.remaining() >= 4) {
				return ReadBinDWord();
			} else {
//				P = End;
				parsing.setCurrent(pBuffer.limit());
				return 0;
			}
		} else
		{
			findNextNoneWhiteSpace();

			// TODO: consider using strtol10 instead???

			// check preceeding minus sign
			boolean isNegative = false;
			if( parsing.get() == '-')
			{
				isNegative = true;
//				P++;
				parsing.inCre();
			}

			// at least one digit expected
//			if( !isdigit( *P))
			if(!Character.isDigit(parsing.get()))
				throw new DeadlyImportError( "Number expected.");

			// read digits
			int number = 0;
			while(parsing.hasNext())
			{
				int c = parsing.get();
//				if( !isdigit( *P))
				if(!Character.isDigit(c))
					break;
				number = number * 10 + (c - 48);
//				P++;
				parsing.inCre();
			}
			
			CheckForSeparator();
//			return isNegative ? ((unsigned int) -int( number)) : number;
			return isNegative ? -number : number;  // TODO In the origin C++ program, this method only return a unsigned integer.
		}
	}
	
	float ReadFloat(){
		if( mIsBinaryFormat)
		{
			if( mBinaryNumCount == 0 && parsing.remaining() >= 2)
			{
//				unsigned short tmp = ReadBinWord(); // 0x07 or 0x42
				int tmp = readBinWord(); // 0x07 or 0x42
				if( tmp == 0x07 && parsing.remaining() >= 4) // array of floats following
					mBinaryNumCount = ReadBinDWord();
				else // single float following
					mBinaryNumCount = 1; 
			}

			--mBinaryNumCount;
			if( mBinaryFloatSize == 8)
			{
				if( parsing.remaining() >= 8) {
//					float result = (float) (*(double*) P);
					float result = (float) pBuffer.getDouble(parsing.getCurrent());
//					P += 8;
					parsing.skip(8);
					return result;
				} else {
//					P = End;
					parsing.setCurrent(pBuffer.limit());
					return 0;
				}
			} else
			{
				if( parsing.remaining() >= 4) {
//					float result = *(float*) P;
//					P += 4;
					float result = pBuffer.getFloat(parsing.getCurrent());
					parsing.skip(4);
					return result;
				} else {
//					P = End;
					parsing.setCurrent(pBuffer.limit());
					return 0;
				}
			}
		}

		// text version
		findNextNoneWhiteSpace();
		// check for various special strings to allow reading files from faulty exporters
		// I mean you, Blender!
		// Reading is safe because of the terminating zero
		if( !parsing.strncmp("-1.#IND00") || !parsing.strncmp("1.#IND00"))
		{ 
//			P += 9;
			parsing.skip(9);
			CheckForSeparator();
			return 0.0f;
		} else
		if( !parsing.strncmp("1.#QNAN0"))
		{
//			P += 8;
			parsing.skip(8);
			CheckForSeparator();
			return 0.0f;
		}

		float result = 0.0f;
//		P = fast_atoreal_move<float>( P, result);
		result = (float) parsing.fast_atoreal_move(true);

		CheckForSeparator();

		return result;
	}
	
	void ReadVector2(Vector2f vec){
		vec.x = ReadFloat();
		vec.y = ReadFloat();
		TestForSeparator();
	}
	
	void ReadVector3(Vector3f vec){
		vec.x = ReadFloat();
		vec.y = ReadFloat();
		vec.z = ReadFloat();
		TestForSeparator();
	}
	
	void ReadRGB(Vector3f vec){
		ReadVector3(vec);
	}
	
	void ReadRGBA(Vector4f vec){
		vec.x = ReadFloat();
		vec.y = ReadFloat();
		vec.z = ReadFloat();
		vec.w = ReadFloat();
		TestForSeparator();
	}

	/* Throws an exception with a line number and the given text. */
//	void ThrowException( const std::string& pText);

	/** Filters the imported hierarchy for some degenerated cases that some exporters produce.
	 * @param pData The sub-hierarchy to filter
	 */
	void filterHierarchy( XNode pNode){
		// if the node has just a single unnamed child containing a mesh, remove
		// the anonymous node inbetween. The 3DSMax kwXport plugin seems to produce this
		// mess in some cases
		if( pNode.mChildren.size() == 1 && pNode.mMeshes.isEmpty() )
		{
			XNode child = pNode.mChildren.get(0);
			if( child.mName.length() == 0 && child.mMeshes.size() > 0)
			{
				// transfer its meshes to us
				for(int a = 0; a < child.mMeshes.size(); a++)
					pNode.mMeshes.add( child.mMeshes.get(a));
				child.mMeshes.clear();

				// transfer the transform as well
//				pNode.mTrafoMatrix = pNode->mTrafoMatrix * child->mTrafoMatrix;
				Matrix4f.mul(pNode.mTrafoMatrix, child.mTrafoMatrix, pNode.mTrafoMatrix);
				pNode.mTrafoMatrix.transpose();  // TODO ???

				// then kill it
//				delete child;
				pNode.mChildren.clear();
			}
		}

		// recurse
		for( int a = 0; a < pNode.mChildren.size(); a++)
			filterHierarchy( pNode.mChildren.get(a));
	}
}
