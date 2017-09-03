package assimp.importer.hmp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.lwjgl.util.vector.Vector3f;

import assimp.common.AssUtil;
import assimp.common.AssimpConfig;
import assimp.common.BaseImporter;
import assimp.common.DeadlyImportError;
import assimp.common.DefaultLogger;
import assimp.common.Face;
import assimp.common.FileUtils;
import assimp.common.ImporterDesc;
import assimp.common.Material;
import assimp.common.MemoryUtil;
import assimp.common.Mesh;
import assimp.common.Node;
import assimp.common.Scene;
import assimp.common.ShadingMode;
import assimp.importer.md2.MD2;
import assimp.importer.mdl.MDLImporter;

public class HMPImporter extends BaseImporter{
	// to make it easier for us, we test the magic word against both "endianesses"
	static final int AI_HMP_MAGIC_NUMBER_BE_4 = AssUtil.makeMagic("HMP4");
	static final int AI_HMP_MAGIC_NUMBER_LE_4 = AssUtil.makeMagic("4PMH");

	static final int AI_HMP_MAGIC_NUMBER_BE_5 = AssUtil.makeMagic("HMP5");
	static final int AI_HMP_MAGIC_NUMBER_LE_5 = AssUtil.makeMagic("5PMH");

	static final int AI_HMP_MAGIC_NUMBER_BE_7 = AssUtil.makeMagic("HMP7");
	static final int AI_HMP_MAGIC_NUMBER_LE_7 = AssUtil.makeMagic("7PMH");
	
	static final ImporterDesc desc = new ImporterDesc(
		"3D GameStudio Heightmap (HMP) Importer",
		"",
		"",
		"",
		ImporterDesc.aiImporterFlags_SupportBinaryFlavour,
		0,
		0,
		0,
		0,
		"hmp" 
	);
	
	private Scene pScene;
	private ByteBuffer mBuffer;
	private final Header_HMP5 header_HMP5 = new Header_HMP5();
	private final Vertex_HMP5 vertex_HMP5 = new Vertex_HMP5();
	private final Vertex_HMP7 vertex_HMP7 = new Vertex_HMP7();
	private final Vector3f temp = new Vector3f();
	
	@Override
	protected boolean canRead(String pFile, InputStream pIOHandler, boolean checkSig) throws IOException {
		String extension = getExtension(pFile);
		if(extension.equals("hmp"))
			return true;
		
		// if check for extension is not enough, check for the magic tokens 
		if (extension.length() == 0 || checkSig) {
			byte[] tokens = new byte[12]; 
//			tokens[0] = AI_HMP_MAGIC_NUMBER_LE_4;
//			tokens[1] = AI_HMP_MAGIC_NUMBER_LE_5;
//			tokens[2] = AI_HMP_MAGIC_NUMBER_LE_7;
			AssUtil.getBytes(AI_HMP_MAGIC_NUMBER_LE_4, tokens, 0);
			AssUtil.getBytes(AI_HMP_MAGIC_NUMBER_LE_5, tokens, 4);
			AssUtil.getBytes(AI_HMP_MAGIC_NUMBER_LE_7, tokens, 8);
			// TODO
			return checkMagicToken(new File(pFile),tokens,3,0);
		}
		return false;
	}

	@Override
	protected ImporterDesc getInfo() { return desc;}

	@Override
	protected void internReadFile(File pFile, Scene pScene) {
		this.pScene = pScene; 
		ByteBuffer buffer = FileUtils.loadText(pFile, false, AssimpConfig.LOADING_USE_NATIVE_MEMORY);
		mBuffer = buffer;
		
		// Determine the file subtype and call the appropriate member function
		final int iMagic = buffer.getInt(0);
		
		// HMP4 format
		if (AI_HMP_MAGIC_NUMBER_LE_4 == iMagic ||
			AI_HMP_MAGIC_NUMBER_BE_4 == iMagic)
		{
			if(DefaultLogger.LOG_OUT)
				DefaultLogger.debug("HMP subtype: 3D GameStudio A4, magic word is HMP4");
			internReadFile_HMP4();
		}
		// HMP5 format
		else if (AI_HMP_MAGIC_NUMBER_LE_5 == iMagic ||
				 AI_HMP_MAGIC_NUMBER_BE_5 == iMagic)
		{
			if(DefaultLogger.LOG_OUT)
				DefaultLogger.debug("HMP subtype: 3D GameStudio A5, magic word is HMP5");
			internReadFile_HMP5();
		}
		// HMP7 format
		else if (AI_HMP_MAGIC_NUMBER_LE_7 == iMagic ||
				 AI_HMP_MAGIC_NUMBER_BE_7 == iMagic)
		{
			if(DefaultLogger.LOG_OUT)
				DefaultLogger.debug("HMP subtype: 3D GameStudio A7, magic word is HMP7");
			internReadFile_HMP7();
		}
		else
		{
			// Print the magic word to the logger
//			char szBuffer[5];
//			szBuffer[0] = ((char*)&iMagic)[0];
//			szBuffer[1] = ((char*)&iMagic)[1];
//			szBuffer[2] = ((char*)&iMagic)[2];
//			szBuffer[3] = ((char*)&iMagic)[3];
//			szBuffer[4] = '\0';
			byte[] szBuffer = new byte[4];
			AssUtil.getBytes(iMagic, szBuffer, 0);

			// We're definitely unable to load this file
			throw new DeadlyImportError( "Unknown HMP subformat " + pFile +
				". Magic word (" + new String(szBuffer) + ") is not known");
		}

		// Set the AI_SCENE_FLAGS_TERRAIN bit
		pScene.mFlags |= Scene.AI_SCENE_FLAGS_TERRAIN;
	}
	
	// -------------------------------------------------------------------
	/** Import a HMP4 file
	*/
	void internReadFile_HMP4( ){
		throw new DeadlyImportError("HMP4 is currently not supported");
	}

	// -------------------------------------------------------------------
	/** Import a HMP5 file
	*/
	void internReadFile_HMP5( ){
		// read the file header and skip everything to byte 84
		Header_HMP5 pcHeader = /*(const HMP::Header_HMP5*)*/ header_HMP5.load(mBuffer);
//		const unsigned char* szCurrent = (const unsigned char*)(mBuffer+84);
		mBuffer.position(84);
		validateHeader_HMP457();

		// generate an output mesh
//		pScene.mNumMeshes = 1;
//		pScene.mMeshes = new aiMesh*[1];
//		aiMesh* pcMesh = pScene.mMeshes[0] = new aiMesh();
		pScene.mMeshes = new Mesh[1];
		Mesh pcMesh = pScene.mMeshes[0] = new Mesh();

		pcMesh.mMaterialIndex = 0;
		pcMesh.mVertices = MemoryUtil.createFloatBuffer(3 * pcHeader.numverts, AssimpConfig.MESH_USE_NATIVE_MEMORY); /*new aiVector3D[pcHeader.numverts]*/;
		pcMesh.mNormals = MemoryUtil.createFloatBuffer(3 * pcHeader.numverts, AssimpConfig.MESH_USE_NATIVE_MEMORY); //new aiVector3D[pcHeader.numverts];

		final int height = (int) (pcHeader.numverts / pcHeader.fnumverts_x);
		final int width =  (int) pcHeader.fnumverts_x;

		// generate/load a material for the terrain
		createMaterial(/*szCurrent,&szCurrent*/);

		// goto offset 120, I don't know why ...
		// (fixme) is this the frame header? I assume yes since it starts with 2. 
//		szCurrent += 36;
		mBuffer.position(mBuffer.position() + 36);
//		sizeCheck(/*szCurrent*/mBuffer.position() + 4/*sizeof(const HMP::Vertex_HMP7)*/*height*width);

		// now load all vertices from the file
		FloatBuffer pcVertOut = pcMesh.mVertices;
		FloatBuffer pcNorOut = pcMesh.mNormals;
//		const HMP::Vertex_HMP5* src = (const HMP::Vertex_HMP5*) szCurrent;
		int old_pos = mBuffer.position();
		Vertex_HMP5 src = vertex_HMP5.load(mBuffer);
		Vector3f vOut = temp;
		for (int y = 0; y < height;++y)
		{
			for (int x = 0; x < width;++x)
			{
				float _x = x * pcHeader.ftrisize_x;
				float _y = y * pcHeader.ftrisize_y;
				float _z = (((float)(src.z & 0xFFFF) / 0xffff)-0.5f) * pcHeader.ftrisize_x * 8.0f; 
				pcVertOut.put(_x).put(_y).put(_z);
				
				MD2.lookupNormalIndex(src.normals162index & 0xFF, /**pcNorOut*/vOut );
//				++pcVertOut;++pcNorOut;++src;
				vOut.store(pcNorOut);
				src = vertex_HMP5.load(mBuffer);
			}
		}
		pcVertOut.flip();
		pcNorOut.flip();
		
		mBuffer.position(old_pos);
		// generate texture coordinates if necessary
		if (pcHeader.numskins > 0)
			generateTextureCoords(width,height);

		// now build a list of faces
		createOutputFaceList(width,height);	

		// there is no nodegraph in HMP files. Simply assign the one mesh
		// (no, not the one ring) to the root node
		pScene.mRootNode = new Node();
		pScene.mRootNode.mName = ("terrain_root");
//		pScene.mRootNode->mNumMeshes = 1;
		pScene.mRootNode.mMeshes = new int[1];
//		pScene.mRootNode->mMeshes[0] = 0;
	}

	// -------------------------------------------------------------------
	/** Import a HMP7 file
	*/
	void internReadFile_HMP7( ){
		// read the file header and skip everything to byte 84
		Header_HMP5 pcHeader = /*(const HMP::Header_HMP5*)*/header_HMP5.load(mBuffer) ;
//		const unsigned char* szCurrent = (const unsigned char*)(mBuffer+84);
		mBuffer.position(84);
		validateHeader_HMP457();

		// generate an output mesh
//		pScene->mNumMeshes = 1;
		pScene.mMeshes = new Mesh[1];
		Mesh pcMesh = pScene.mMeshes[0] = new Mesh();

		pcMesh.mMaterialIndex = 0;
		pcMesh.mVertices = MemoryUtil.createFloatBuffer(3 * pcHeader.numverts, AssimpConfig.MESH_USE_NATIVE_MEMORY); //new aiVector3D[pcHeader.numverts];
		pcMesh.mNormals = MemoryUtil.createFloatBuffer(3 * pcHeader.numverts, AssimpConfig.MESH_USE_NATIVE_MEMORY); // new aiVector3D[pcHeader.numverts];

		final int height = (int) (pcHeader.numverts / pcHeader.fnumverts_x);
		final int width = (int) pcHeader.fnumverts_x;

		// generate/load a material for the terrain
		createMaterial(/*szCurrent,&szCurrent*/);

		// goto offset 120, I don't know why ...
		// (fixme) is this the frame header? I assume yes since it starts with 2. 
//		szCurrent += 36;
		mBuffer.position(mBuffer.position() + 36);

//		sizeCheck(/*szCurrent*/mBuffer.position() + 4/*sizeof(const HMP::Vertex_HMP7)*/*height*width);

		// now load all vertices from the file
		FloatBuffer pcVertOut = pcMesh.mVertices;
		FloatBuffer pcNorOut = pcMesh.mNormals;
//		const HMP::Vertex_HMP7* src = (const HMP::Vertex_HMP7*) szCurrent;
		int old_pos = mBuffer.position();
		Vertex_HMP7 src = vertex_HMP7.load(mBuffer);
		Vector3f nor = temp;
		for (int y = 0; y < height;++y)
		{
			for (int x = 0; x < width;++x)
			{
				pcVertOut.put(x * pcHeader.ftrisize_x);
				pcVertOut.put(y * pcHeader.ftrisize_y);

				// FIXME: What exctly is the correct scaling factor to use?
				// possibly pcHeader.scale_origin[2] in combination with a
				// signed interpretation of src->z?
				pcVertOut.put((((float)(src.z & 0xffff) / 0xffff)-0.5f) * pcHeader.ftrisize_x * 8.0f); 

				nor.x = ((float)(src.normal_x & 0xff) / 0x80 ); // * pcHeader.scale_origin[0];
				nor.y = ((float)(src.normal_y & 0xff) / 0x80 ); // * pcHeader.scale_origin[1];
				nor.z = 1.0f;
//				pcNorOut->Normalize();
				nor.normalise();
				nor.store(pcNorOut);
				
//				++pcVertOut;++pcNorOut;++src;
				src = vertex_HMP7.load(mBuffer);
			}
		}
		
		pcVertOut.flip();
		pcNorOut.flip();
		mBuffer.position(old_pos);

		// generate texture coordinates if necessary
		if (pcHeader.numskins > 0)generateTextureCoords(width,height);

		// now build a list of faces
		createOutputFaceList(width,height);	

		// there is no nodegraph in HMP files. Simply assign the one mesh
		// (no, not the One Ring) to the root node
//		pScene->mRootNode = new aiNode();
//		pScene->mRootNode->mName.Set("terrain_root");
//		pScene->mRootNode->mNumMeshes = 1;
//		pScene->mRootNode->mMeshes = new int[1];
//		pScene->mRootNode->mMeshes[0] = 0;
		pScene.mRootNode = new Node();
		pScene.mRootNode.mName = ("terrain_root");
		pScene.mRootNode.mMeshes = new int[1];
	}

	// -------------------------------------------------------------------
	/** Validate a HMP 5,4,7 file header
	*/
	void validateHeader_HMP457( ){
//		Header_HMP5 pcHeader = (const HMP::Header_HMP5*)mBuffer;
		Header_HMP5 pcHeader = header_HMP5.load(mBuffer);

//		if (120 > iFileSize)
//		{
//			throw new DeadlyImportError("HMP file is too small (header size is 120 bytes, this file is smaller)");
//		}

		if (pcHeader.ftrisize_x == 0|| pcHeader.ftrisize_y == 0)
			throw new DeadlyImportError("Size of triangles in either  x or y direction is zero");
		
		if(pcHeader.fnumverts_x < 1.0f || (pcHeader.numverts/pcHeader.fnumverts_x) < 1.0f)
			throw new DeadlyImportError("Number of triangles in either x or y direction is zero");
		
		if(pcHeader.numframes == 0)
			throw new DeadlyImportError("There are no frames. At least one should be there");
	}

	// -------------------------------------------------------------------
	/** Try to load one material from the file, if this fails create
	 * a default material
	*/
	void createMaterial(){
		Mesh pcMesh = pScene.mMeshes[0];
//		const HMP::Header_HMP5* const pcHeader = (const HMP::Header_HMP5*)mBuffer;
		Header_HMP5 pcHeader = header_HMP5.load(mBuffer);

		// we don't need to generate texture coordinates if
		// we have no textures in the file ...
		if (pcHeader.numskins > 0)
		{
			pcMesh.mTextureCoords[0] = MemoryUtil.createFloatBuffer(pcHeader.numverts * 3, AssimpConfig.MESH_USE_NATIVE_MEMORY);
			pcMesh.mNumUVComponents[0] = 2;

			// now read the first skin and skip all others
			readFirstSkin(pcHeader.numskins/*,szCurrent,&szCurrent*/);
		}
		else
		{
			// generate a default material
			final int iMode = (int)ShadingMode.aiShadingMode_Gouraud.ordinal();
			Material pcHelper = new Material();
			pcHelper.addProperty(iMode, Material.AI_MATKEY_SHADING_MODEL, 0,0);

			Vector3f clr = new Vector3f();
			clr.x = clr.y = clr.z = 0.6f;
			pcHelper.addProperty(clr, Material.AI_MATKEY_COLOR_DIFFUSE, 0, 0);
			pcHelper.addProperty(clr, Material.AI_MATKEY_COLOR_SPECULAR, 0, 0);

			clr.x = clr.y = clr.z = 0.05f;
			pcHelper.addProperty(clr, Material.AI_MATKEY_COLOR_AMBIENT, 0, 0);

			pcHelper.addProperty(Material.AI_DEFAULT_MATERIAL_NAME,Material.AI_MATKEY_NAME, 0,0);

			// add the material to the scene
//			pScene->mNumMaterials = 1;
//			pScene->mMaterials = new aiMaterial*[1];
//			pScene->mMaterials[0] = pcHelper;
			pScene.mMaterials = new Material[]{pcHelper};
		}
//		*szCurrentOut = szCurrent;
	}
	
	Vector3f get(FloatBuffer buf, int index){
		index = index * 3;
		temp.x = buf.get(index++);
		temp.y = buf.get(index++);
		temp.z = buf.get(index++);
		return temp;
	}

	// -------------------------------------------------------------------
	/** Build a list of output faces and vertices. The function 
	 *  triangulates the height map read from the file
	 * \param width Width of the height field
	 * \param width Height of the height field
	*/
	void createOutputFaceList(int width,int height){
		Mesh pcMesh = pScene.mMeshes[0];

		// Allocate enough storage
		int numFaces = (width-1) * (height-1);
		pcMesh.mFaces = new Face[numFaces];

		pcMesh.mNumVertices   = numFaces*4;
		FloatBuffer pcVertices = MemoryUtil.createFloatBuffer(pcMesh.mNumVertices * 3, AssimpConfig.MESH_USE_NATIVE_MEMORY);
		FloatBuffer pcNormals  = MemoryUtil.createFloatBuffer(pcMesh.mNumVertices * 3, AssimpConfig.MESH_USE_NATIVE_MEMORY);

//		Face pcFaceOut(pcMesh.mFaces);
		int pcFaceOut = 0;
		FloatBuffer pcVertOut = pcVertices;
		FloatBuffer pcNorOut = pcNormals;

		FloatBuffer pcUVs = pcMesh.mTextureCoords[0] != null? MemoryUtil.createFloatBuffer(pcMesh.mNumVertices * 3, AssimpConfig.MESH_USE_NATIVE_MEMORY) : null;
		FloatBuffer pcUVOut = (pcUVs);

		// Build the terrain square
		int iCurrent = 0;
		for (int y = 0; y < height-1;++y)	{
			for (int x = 0; x < width-1;++x,++pcFaceOut)	{
//				pcFaceOut->mNumIndices = 4;
//				pcFaceOut->mIndices = new int[4];
				Face face = pcMesh.mFaces[pcFaceOut] = Face.createInstance(4);

//				*pcVertOut++ = pcMesh.mVertices[y*width+x];
//				*pcVertOut++ = pcMesh.mVertices[(y+1)*width+x];
//				*pcVertOut++ = pcMesh.mVertices[(y+1)*width+x+1];
//				*pcVertOut++ = pcMesh.mVertices[y*width+x+1];
				get(pcMesh.mVertices, y*width+x).store(pcVertOut);
				get(pcMesh.mVertices, (y+1)*width+x).store(pcVertOut);
				get(pcMesh.mVertices, (y+1)*width+x+1).store(pcVertOut);
				get(pcMesh.mVertices, y*width+x+1).store(pcVertOut);
				

//				*pcNorOut++ = pcMesh.mNormals[y*width+x];
//				*pcNorOut++ = pcMesh.mNormals[(y+1)*width+x];
//				*pcNorOut++ = pcMesh.mNormals[(y+1)*width+x+1];
//				*pcNorOut++ = pcMesh.mNormals[y*width+x+1];
				get(pcMesh.mNormals, y*width+x).store(pcNorOut);
				get(pcMesh.mNormals, (y+1)*width+x).store(pcNorOut);
				get(pcMesh.mNormals, (y+1)*width+x+1).store(pcNorOut);
				get(pcMesh.mNormals, y*width+x+1).store(pcNorOut);

				if (pcMesh.mTextureCoords[0] != null)
				{
//					*pcUVOut++ = pcMesh.mTextureCoords[0][y*width+x];
//					*pcUVOut++ = pcMesh.mTextureCoords[0][(y+1)*width+x];
//					*pcUVOut++ = pcMesh.mTextureCoords[0][(y+1)*width+x+1];
//					*pcUVOut++ = pcMesh.mTextureCoords[0][y*width+x+1];
					get(pcMesh.mTextureCoords[0], y*width+x).store(pcUVOut);
					get(pcMesh.mTextureCoords[0], (y+1)*width+x).store(pcUVOut);
					get(pcMesh.mTextureCoords[0], (y+1)*width+x+1).store(pcUVOut);
					get(pcMesh.mTextureCoords[0], y*width+x+1).store(pcUVOut);
				}
				
				for (int i = 0; i < 4;++i)
//					pcFaceOut->mIndices[i] = iCurrent++;
					face.set(i, iCurrent++);
			}
		}
//		delete[] pcMesh.mVertices;
		pcMesh.mVertices = pcVertices;

//		delete[] pcMesh.mNormals;
		pcMesh.mNormals = pcNormals;

		if (pcMesh.mTextureCoords[0] != null)
		{
//			delete[] pcMesh.mTextureCoords[0];
			pcMesh.mTextureCoords[0] = pcUVs;
		}
	}

	// -------------------------------------------------------------------
	/** Generate planar texture coordinates for a terrain
	 * @param width Width of the terrain, in vertices
	 * @param height Height of the terrain, in vertices
	*/
	void generateTextureCoords( int width, int height){
//		ai_assert(NULL != pScene->mMeshes && NULL != pScene->mMeshes[0] &&
//			      NULL != pScene->mMeshes[0]->mTextureCoords[0]);

		FloatBuffer uv = pScene.mMeshes[0].mTextureCoords[0];

		final float fY = (1.0f / height) + (1.0f / height) / (height-1);
		final float fX = (1.0f / width) + (1.0f / width) / (width-1);

		for (int y = 0; y < height;++y)	{
			for (int x = 0; x < width;++x/*,++uv*/)	{
//				uv->y = fY*y;
//				uv->x = fX*x;
//				uv->z = 0.0f;
				uv.put(fY*y).put(fX*x).put(0);
			}
		}
		uv.flip();
	}

	// -------------------------------------------------------------------
	/** Read the first skin from the file and skip all others ...
	 *  \param iNumSkins Number of skins in the file
	 *  \param szCursor Position of the first skin (offset 84)
	*/
	void readFirstSkin(int iNumSkins/*,  int char* szCursor,
		in char** szCursorOut*/){
		assert(0 != iNumSkins /*&& NULL != szCursor*/);

		// read the type of the skin ...
		// sometimes we need to skip 12 bytes here, I don't know why ...
//		uint32_t iType = *((uint32_t*)szCursor);szCursor += sizeof(uint32_t);
		int iType = mBuffer.getInt();
		if (0 == iType)
		{
//			szCursor += sizeof(uint32_t) * 2;
			mBuffer.getLong();  // skip the 8 bytes.
//			iType = *((uint32_t*)szCursor);szCursor += sizeof(uint32_t);
			iType = mBuffer.getInt();
			if (iType == 0)
				throw new DeadlyImportError("Unable to read HMP7 skin chunk");
			
		}
		// read width and height
//		uint32_t iWidth  = *((uint32_t*)szCursor); szCursor += sizeof(uint32_t);
//		uint32_t iHeight = *((uint32_t*)szCursor); szCursor += sizeof(uint32_t);
		int iWidth = mBuffer.getInt();
		int iHeight = mBuffer.getInt();

		// allocate an output material
		Material pcMat = new Material();

		// read the skin, this works exactly as for MDL7
		MDLImporter.parseSkinLump_3DGS_MDL7(mBuffer,/*szCursor,&szCursor,*/pScene,
			pcMat,iType,iWidth,iHeight);

		// now we need to skip any other skins ... 
		for (int i = 1; i< iNumSkins;++i)
		{
			iType   = mBuffer.getInt();//*((uint32_t*)szCursor);   szCursor += sizeof(uint32_t);
			iWidth  = mBuffer.getInt();//*((uint32_t*)szCursor);   szCursor += sizeof(uint32_t);
			iHeight = mBuffer.getInt();//*((uint32_t*)szCursor);   szCursor += sizeof(uint32_t);

			MDLImporter.skipSkinLump_3DGS_MDL7(mBuffer, /*szCursor,&szCursor,*/iType,iWidth,iHeight);
//			sizeCheck(szCursor);
		}

		// setup the material ...
//		pScene->mNumMaterials = 1;
//		pScene->mMaterials = new aiMaterial*[1];
//		pScene->mMaterials[0] = pcMat;
		pScene.mMaterials = new Material[]{pcMat};

//		*szCursorOut = szCursor;
	}

}
