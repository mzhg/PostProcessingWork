package assimp.importer.md2;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.lwjgl.util.vector.Vector3f;

import assimp.common.AssUtil;
import assimp.common.AssimpConfig;
import assimp.common.BaseImporter;
import assimp.common.DeadlyImportError;
import assimp.common.DefaultLogger;
import assimp.common.Face;
import assimp.common.FileUtils;
import assimp.common.Importer;
import assimp.common.ImporterDesc;
import assimp.common.Material;
import assimp.common.MemoryUtil;
import assimp.common.Mesh;
import assimp.common.Node;
import assimp.common.Scene;
import assimp.common.ShadingMode;
import assimp.common.TextureType;

/** Importer class for MD2
*/
public class MD2Importer extends BaseImporter{

	static final int AI_MD2_MAGIC_NUMBER_BE = AssUtil.makeMagic("IDP2");
	static final int AI_MD2_MAGIC_NUMBER_LE = AssUtil.makeMagic("2PDI");
	
	static final int AI_MD2_VERSION = 15;
	static final int AI_MD2_MAXQPATH	=	64;
	static final int AI_MD2_MAX_FRAMES	=   512;
	static final int AI_MD2_MAX_SKINS	=   32;	
	static final int AI_MD2_MAX_VERTS	=   2048;	
	static final int AI_MD2_MAX_TRIANGLES = 4096;
	
	static final ImporterDesc desc = new ImporterDesc(
		"Quake II Mesh Importer",
		"",
		"",
		"",
		ImporterDesc.aiImporterFlags_SupportBinaryFlavour,
		0,
		0,
		0,
		0,
		"md2" 
	);
	
	/** Configuration option: frame to be loaded */
	int configFrameID;

	/** Header of the MD2 file */
	final MD2Header m_pcHeader = new MD2Header();

	/** Buffer to hold the loaded file */
	ByteBuffer mBuffer;

	/* Size of the file, in bytes */
//	int fileSize;
	
	@Override
	protected boolean canRead(String pFile, InputStream pIOHandler,boolean checkSig) throws IOException {
		String extension = getExtension(pFile);
		if (extension.equals("md2"))
			return true;

		// if check for extension is not enough, check for the magic tokens 
		if (extension.length() == 0 || checkSig) {
//			uint32_t tokens[1]; 
//			tokens[0] = AI_MD2_MAGIC_NUMBER_LE;
			byte[] tokens = new byte[4];
			AssUtil.getBytes(AI_MD2_MAGIC_NUMBER_LE, tokens,0);
			return checkMagicToken(new File(pFile),tokens,0, 4);
		}
		return false;
	}

	@Override
	protected ImporterDesc getInfo() { return desc;}
	
	@Override
	public void setupProperties(Importer pImp) {
		// The 
		// AI_CONFIG_IMPORT_MD2_KEYFRAME option overrides the
		// AI_CONFIG_IMPORT_GLOBAL_KEYFRAME option.
		configFrameID = pImp.getPropertyInteger(AssimpConfig.AI_CONFIG_IMPORT_MD2_KEYFRAME,-1);
		if(-1 == configFrameID){
			configFrameID = pImp.getPropertyInteger(AssimpConfig.AI_CONFIG_IMPORT_GLOBAL_KEYFRAME,0);
		}
	}
	
	// Validate the file header
	void validateHeader( )
	{
		// check magic number
		if (m_pcHeader.magic != AI_MD2_MAGIC_NUMBER_BE && m_pcHeader.magic != AI_MD2_MAGIC_NUMBER_LE)
		{
//			char szBuffer[5];
//			szBuffer[0] = ((char*)&m_pcHeader.magic)[0];
//			szBuffer[1] = ((char*)&m_pcHeader.magic)[1];
//			szBuffer[2] = ((char*)&m_pcHeader.magic)[2];
//			szBuffer[3] = ((char*)&m_pcHeader.magic)[3];
//			szBuffer[4] = '\0';
			byte[] szBuffer = new byte[4];
			AssUtil.getBytes(m_pcHeader.magic, szBuffer, 0);

			throw new DeadlyImportError("Invalid MD2 magic word: should be IDP2, the magic word found is " +  new String(szBuffer));
		}

		// check file format version
		if (m_pcHeader.version != 8 && DefaultLogger.LOG_OUT)
			DefaultLogger.warn( "Unsupported md2 file version. Continuing happily ...");

		// check some values whether they are valid
		if (0 == m_pcHeader.numFrames)
			throw new DeadlyImportError( "Invalid md2 file: NUM_FRAMES is 0");

		if (m_pcHeader.offsetEnd > mBuffer.limit())
			throw new DeadlyImportError( "Invalid md2 file: File is too small");
		
		int fileSize = mBuffer.limit();
		if (m_pcHeader.offsetSkins		+ m_pcHeader.numSkins * Skin.SIZE			>= fileSize ||
			m_pcHeader.offsetTexCoords	+ m_pcHeader.numTexCoords * TexCoord.SIZE	>= fileSize ||
			m_pcHeader.offsetTriangles	+ m_pcHeader.numTriangles * Triangle.SIZEE	>= fileSize ||
			m_pcHeader.offsetFrames		+ m_pcHeader.numFrames *   Frame.SIZE		>= fileSize ||
			m_pcHeader.offsetEnd			> fileSize)
		{
			throw new DeadlyImportError("Invalid MD2 header: some offsets are outside the file");
		}

		if (m_pcHeader.numSkins > AI_MD2_MAX_SKINS && DefaultLogger.LOG_OUT)
			DefaultLogger.warn("The model contains more skins than Quake 2 supports");
		if ( m_pcHeader.numFrames > AI_MD2_MAX_FRAMES && DefaultLogger.LOG_OUT)
			DefaultLogger.warn("The model contains more frames than Quake 2 supports");
		if (m_pcHeader.numVertices > AI_MD2_MAX_VERTS && DefaultLogger.LOG_OUT)
			DefaultLogger.warn("The model contains more vertices than Quake 2 supports");

		if (m_pcHeader.numFrames <= configFrameID )
			throw new DeadlyImportError("The requested frame is not existing the file");
	}

	@Override
	protected void internReadFile(File pFile, Scene pScene) {
		// TODO should load binary data here
		mBuffer = FileUtils.loadText(pFile, false, AssimpConfig.LOADING_USE_NATIVE_MEMORY);
		
		m_pcHeader.load(mBuffer);
		
		validateHeader();

		// there won't be more than one mesh inside the file
//		pScene.mNumMaterials = 1;
		pScene.mRootNode = new Node();
//		pScene.mRootNode->mNumMeshes = 1;
		pScene.mRootNode.mMeshes = new int[1];
		pScene.mRootNode.mMeshes[0] = 0;
		pScene.mMaterials = new Material[1];
		pScene.mMaterials[0] = new Material();
//		pScene.mNumMeshes = 1;
		pScene.mMeshes = new Mesh[1];

		Mesh pcMesh = pScene.mMeshes[0] = new Mesh();
		pcMesh.mPrimitiveTypes = Mesh.aiPrimitiveType_TRIANGLE;
		
		// navigate to the begin of the frame data
		Frame pcFrame = new Frame();
		mBuffer.position(m_pcHeader.offsetFrames + configFrameID * Frame.SIZE);
		pcFrame.load(mBuffer);
		
		// navigate to the begin of the triangle data
		Triangle[] pcTriangles = new Triangle[m_pcHeader.numTriangles];
		mBuffer.position(m_pcHeader.offsetTriangles);
		for(int i = 0; i < pcTriangles.length; i++){
			Triangle t = new Triangle();
			t.load(mBuffer);
			pcTriangles[i] = t;
		}
		
		// navigate to the begin of the tex coords data
		TexCoord[] pcTexCoords = new TexCoord[m_pcHeader.numTexCoords];
		mBuffer.position(m_pcHeader.offsetTexCoords);
		for(int i = 0; i < pcTexCoords.length; i++){
			TexCoord t = new TexCoord();
			t.load(mBuffer);
			pcTexCoords[i] = t;
		}
		
		// navigate to the begin of the vertex data
		Vertex[] pcVerts = new Vertex[m_pcHeader.numVertices];
		mBuffer.position(m_pcHeader.offsetFrames + configFrameID * Frame.SIZE - 4);
		for(int i = 0; i < pcVerts.length; i++){
			Vertex v = new Vertex();
			v.load(mBuffer);
			pcVerts[i] = v;
		}
		
//		pcMesh.mNumFaces = m_pcHeader.numTriangles;
		pcMesh.mFaces = new Face[m_pcHeader.numTriangles];

		// allocate output storage
		pcMesh.mNumVertices = pcMesh.mFaces.length*3;
//		pcMesh.mVertices = new Vector3D[pcMesh.mNumVertices];
//		pcMesh.mNormals = new Vector3D[pcMesh.mNumVertices];
		final boolean natived = AssimpConfig.MESH_USE_NATIVE_MEMORY;
		pcMesh.mVertices = MemoryUtil.createFloatBuffer(pcMesh.mNumVertices * 3, natived);
		pcMesh.mNormals  = MemoryUtil.createFloatBuffer(pcMesh.mNumVertices * 3, natived);

		// Not sure whether there are MD2 files without texture coordinates
		// NOTE: texture coordinates can be there without a texture,
		// but a texture can't be there without a valid UV channel
		Material pcHelper = pScene.mMaterials[0];
		final int iMode = ShadingMode.aiShadingMode_Gouraud.ordinal();
		pcHelper.addProperty(iMode, Material.AI_MATKEY_SHADING_MODEL,0,0);

		if (m_pcHeader.numTexCoords != 0 && m_pcHeader.numSkins != 0)
		{
			// navigate to the first texture associated with the mesh
//			const MD2::Skin* pcSkins = (const MD2::Skin*) ((unsigned char*)m_pcHeader + 
//				m_pcHeader.offsetSkins);
			Skin[] pcSkins = new Skin[m_pcHeader.numSkins];
			mBuffer.position(m_pcHeader.offsetSkins);
			for(int i = 0; i < pcSkins.length; i++){
				Skin s = new Skin();
				s.load(mBuffer);
				pcSkins[i] = s;
			}

			Vector3f clr = new Vector3f(1, 1, 1);
//			clr.b = clr.g = clr.r = 1.0f;
			pcHelper.addProperty(clr, Material.AI_MATKEY_COLOR_DIFFUSE,0,0);
			pcHelper.addProperty(clr, Material.AI_MATKEY_COLOR_SPECULAR,0,0);

//			clr.b = clr.g = clr.r = 0.05f;
			clr.set(0.05f, 0.05f, 0.05f);
			pcHelper.addProperty(clr, Material.AI_MATKEY_COLOR_AMBIENT,0,0);

			if (pcSkins[0].name[0] != 0)
			{
//				aiString szString;
//				const size_t iLen = ::strlen(pcSkins->name);
//				::memcpy(szString.data,pcSkins->name,iLen);
//				szString.data[iLen] = '\0';
//				szString.length = iLen;
				String szString = AssUtil.toString(pcSkins[0].name);
				pcHelper.addProperty(szString, Material._AI_MATKEY_TEXTURE_BASE, TextureType.aiTextureType_DIFFUSE.ordinal(),0);
			}
			else if(DefaultLogger.LOG_OUT){
				DefaultLogger.warn("Texture file name has zero length. It will be skipped.");
			}
		}
		else	{
			// apply a default material
			Vector3f clr = new Vector3f(0.6f,.6f,.6f);
//			clr.b = clr.g = clr.r = 0.6f;
			pcHelper.addProperty(clr, Material.AI_MATKEY_COLOR_DIFFUSE,0,0);
			pcHelper.addProperty(clr, Material.AI_MATKEY_COLOR_SPECULAR,0,0);

			clr.x = clr.y = clr.z = 0.05f;
			pcHelper.addProperty(clr, Material.AI_MATKEY_COLOR_AMBIENT,0,0);
			pcHelper.addProperty(Material.AI_DEFAULT_MATERIAL_NAME,Material.AI_MATKEY_NAME,0,0);

			// TODO: Try to guess the name of the texture file from the model file name
			pcHelper.addProperty("$texture_dummy.bmp",Material._AI_MATKEY_TEXTURE_BASE, TextureType.aiTextureType_DIFFUSE.ordinal(),0);
		}


		// now read all triangles of the first frame, apply scaling and translation
		int iCurrent = 0;

		float fDivisorU = 1.0f,fDivisorV = 1.0f;
		if (m_pcHeader.numTexCoords > 0)	{
			// allocate storage for texture coordinates, too
//			pcMesh.mTextureCoords[0] = new aiVector3D[pcMesh.mNumVertices];
			pcMesh.mTextureCoords[0] = MemoryUtil.createFloatBuffer(pcMesh.mNumVertices * 3, natived);
			pcMesh.mNumUVComponents[0] = 2;

			// check whether the skin width or height are zero (this would
			// cause a division through zero)
			if (m_pcHeader.skinWidth == 0)	{
				DefaultLogger.error("MD2: No valid skin width given");
			}
			else fDivisorU = (float)m_pcHeader.skinWidth;
			if (m_pcHeader.skinHeight == 0){
				DefaultLogger.error("MD2: No valid skin height given");
			}
			else fDivisorV = (float)m_pcHeader.skinHeight;
		}

		for (int i = 0; i < m_pcHeader.numTriangles;++i)	{
			// Allocate the face
			pScene.mMeshes[0].mFaces[i]/*.mIndices = new unsigned int[3]*/ = Face.createInstance(3);
//			pScene->mMeshes[0]->mFaces[i].mNumIndices = 3;

			// copy texture coordinates
			// check whether they are different from the previous value at this index.
			// In this case, create a full separate set of vertices/normals/texcoords
			Vector3f vec = new Vector3f();
			for (int c = 0; c < 3;++c,++iCurrent)	{

				// validate vertex indices
				int iIndex = pcTriangles[i].vertexIndices[c] & 0xFFFF;
				if (iIndex >= m_pcHeader.numVertices)	{
					DefaultLogger.error("MD2: Vertex index is outside the allowed range");
					iIndex = m_pcHeader.numVertices-1;
				}

				// read x,y, and z component of the vertex
//				aiVector3D& vec = pcMesh.mVertices[iCurrent];

				vec.x = (float)(pcVerts[iIndex].vertex[0] & 0xFF) * pcFrame.scale[0];
				vec.x += pcFrame.translate[0];

				vec.y = (float)(pcVerts[iIndex].vertex[1] & 0xFF) * pcFrame.scale[1];
				vec.y += pcFrame.translate[1];

				vec.z = (float)(pcVerts[iIndex].vertex[2] & 0xFF) * pcFrame.scale[2];
				vec.z += pcFrame.translate[2];
				int index = iCurrent * 3;
				pcMesh.mVertices.put(index++, vec.x);
				pcMesh.mVertices.put(index++, vec.z);  // swap y z
				pcMesh.mVertices.put(index++, vec.y);  // swap y z

				// read the normal vector from the precalculated normal table
//				aiVector3D& vNormal = pcMesh.mNormals[iCurrent];
				MD2.lookupNormalIndex(pcVerts[iIndex].lightNormalIndex & 0xFF,vec);
				index -= 3;
				pcMesh.mNormals.put(index++, vec.x);
				pcMesh.mNormals.put(index++, vec.z);  // swap y z
				pcMesh.mNormals.put(index++, vec.y);  // swap y z
				
				// flip z and y to become right-handed
//				std::swap((float&)vNormal.z,(float&)vNormal.y);
//				std::swap((float&)vec.z,(float&)vec.y);

				if (m_pcHeader.numTexCoords > 0)	{
					// validate texture coordinates
					iIndex = pcTriangles[i].textureIndices[c];
					if (iIndex >= m_pcHeader.numTexCoords)	{
						DefaultLogger.error("MD2: UV index is outside the allowed range");
						iIndex = m_pcHeader.numTexCoords-1;
					}

//					aiVector3D& pcOut = pcMesh.mTextureCoords[0][iCurrent];

					// the texture coordinates are absolute values but we
					// need relative values between 0 and 1
//					pcOut.x = pcTexCoords[iIndex].s / fDivisorU;
//					pcOut.y = 1.f-pcTexCoords[iIndex].t / fDivisorV;
					index = iCurrent * 3;
					pcMesh.mTextureCoords[0].put(index, pcTexCoords[iIndex].s / fDivisorU);
					pcMesh.mTextureCoords[0].put(index, 1.f-pcTexCoords[iIndex].t / fDivisorV);
				}
				pScene.mMeshes[0].mFaces[i].set(c, iCurrent);
			}
		}
	}

}
