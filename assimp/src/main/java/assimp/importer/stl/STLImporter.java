package assimp.importer.stl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

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
import assimp.common.ParsingUtil;
import assimp.common.Scene;

public class STLImporter extends BaseImporter{
	
	static final ImporterDesc desc = new ImporterDesc(
		"Stereolithography (STL) Importer",
		"",
		"",
		"",
		ImporterDesc.aiImporterFlags_SupportTextFlavour | ImporterDesc.aiImporterFlags_SupportBinaryFlavour,
		0,
		0,
		0,
		0,
		"stl" 
	);

	/** Buffer to hold the loaded file */
	ByteBuffer mBuffer;

	/* Size of the file, in bytes */
//	unsigned int fileSize;
	/** The count of faces */
	int mNumFaces;

	/** Output scene */
	Scene pScene;

	/** Default vertex color */
	final Vector4f clrColorDefault = new Vector4f();
	
	@Override
	protected boolean canRead(String pFile, InputStream pIOHandler,boolean checkSig) throws IOException {
		String extension = getExtension(pFile);

		if (extension.equals("stl"))
			return true;
		else if (extension.length() == 0|| checkSig)	{
			if (pIOHandler == null)
				return true;
			String[] tokens = {"STL","solid"};
			return searchFileHeaderForToken(pIOHandler,pFile,tokens);
		}
		return false;
	}

	@Override
	protected ImporterDesc getInfo() { return desc;}

	@Override
	protected void internReadFile(File pFile, Scene pScene) {
		mBuffer = FileUtils.loadText(pFile, false, AssimpConfig.LOADING_USE_NATIVE_MEMORY);
		this.pScene = pScene;
		
		// the default vertex color is light gray.
//		clrColorDefault.r = clrColorDefault.g = clrColorDefault.b = clrColorDefault.a = 0.6f;
		clrColorDefault.set(0.6f,0.6f,0.6f,0.6f);

		// allocate one mesh
//		pScene.mNumMeshes = 1;
		pScene.mMeshes = new Mesh[1];
		Mesh pMesh = pScene.mMeshes[0] = new Mesh();
		pMesh.mMaterialIndex = 0;

		// allocate a single node
		pScene.mRootNode = new Node();
//		pScene.mRootNode->mNumMeshes = 1;
		pScene.mRootNode.mMeshes = new int[1];
//		pScene.mRootNode->mMeshes[0] = 0;

		boolean bMatClr = false;

		if (isBinarySTL(mBuffer/*, fileSize*/)) {
			bMatClr = loadBinaryFile();
		} else if (isAsciiSTL(mBuffer/*, fileSize*/)) {
			loadASCIIFile();
		} else {
			throw new DeadlyImportError( "Failed to determine STL storage representation for " + pFile + ".");
		}

		// now copy faces
		pMesh.mFaces = new Face[mNumFaces];
		for (int i = 0, p = 0; i < mNumFaces;++i)	{

			Face face = pMesh.mFaces[i] = Face.createInstance(3);
//			face.mIndices = new unsigned int[face.mNumIndices = 3];
			for (int o = 0; o < 3;++o,++p) {
//				face.mIndices[o] = p;
				face.set(o, p);
			}
		}

		// create a single default material, using a light gray diffuse color for consistency with
		// other geometric types (e.g., PLY).
		Material pcMat = new Material();
//		aiString s;
//		s.Set(AI_DEFAULT_MATERIAL_NAME);
		pcMat.addProperty(Material.AI_DEFAULT_MATERIAL_NAME, Material.AI_MATKEY_NAME,0,0);

//		Vector4f clrDiffuse = new Vector4f(0.6f,0.6f,0.6f,1.0f);
//		if (bMatClr) {
//			clrDiffuse = clrColorDefault;
//		}
		Vector4f clrDiffuse = clrColorDefault;
		if(!bMatClr)
			clrDiffuse.set(0.6f,0.6f,0.6f,1.0f);
		
		pcMat.addProperty(clrDiffuse,Material.AI_MATKEY_COLOR_DIFFUSE,0,0);
		pcMat.addProperty(clrDiffuse,Material.AI_MATKEY_COLOR_SPECULAR,0,0);
		clrDiffuse.set(0.05f,0.05f,0.05f,1.0f);
		pcMat.addProperty(clrDiffuse,Material.AI_MATKEY_COLOR_AMBIENT,0,0);

//		pScene.mNumMaterials = 1;
		pScene.mMaterials = new Material[]{pcMat};
//		pScene.mMaterials[0] = pcMat;
	}
	
	// Read an ASCII STL file
	void loadASCIIFile()
	{
		Vector3f vec3 = new Vector3f();
		final int fileSize = mBuffer.limit();
		Mesh pMesh = pScene.mMeshes[0];

//		const char* sz = mBuffer;
		ParsingUtil sz = new ParsingUtil(mBuffer);
//		SkipSpaces(&sz);
		sz.skipSpaces();
//		ai_assert(!IsLineEnd(sz));

//		sz += 5; // skip the "solid"
		sz.inCre(5);
//		SkipSpaces(&sz);
		sz.skipSpaces();
//		const char* szMe = sz;
		int szMe = sz.getCurrent();
		while (!ParsingUtil.isSpaceOrNewLine((byte)sz.get())) {
			sz.inCre();
		}

		// setup the name of the node
		if ((sz.getCurrent()-szMe) > 0)	{

//			pScene->mRootNode->mName.length = temp;
//			memcpy(pScene->mRootNode->mName.data,szMe,temp);
//			pScene->mRootNode->mName.data[temp] = '\0';
			pScene.mRootNode.mName = sz.getString(szMe, sz.getCurrent());
		}
		else pScene.mRootNode.mName =("<STL_ASCII>");

		// try to guess how many vertices we could have
		// assume we'll need 160 bytes for each face
		final boolean natived = AssimpConfig.MESH_USE_NATIVE_MEMORY;
		pMesh.mNumVertices = (mNumFaces = Math.max(1,fileSize / 160 )) * 3;
//		pMesh->mVertices = new aiVector3D[pMesh->mNumVertices];
//		pMesh->mNormals  = new aiVector3D[pMesh->mNumVertices];
		pMesh.mVertices  = MemoryUtil.createFloatBuffer(pMesh.mNumVertices * 3, natived);
		pMesh.mNormals   = MemoryUtil.createFloatBuffer(pMesh.mNumVertices * 3, natived);
		
		int curFace = 0, curVertex = 3;
		for ( ;; )
		{
			// go to the next token
			if(!sz.skipSpacesAndLineEnd())
			{
				// seems we're finished although there was no end marker
				if(DefaultLogger.LOG_OUT)
					DefaultLogger.warn("STL: unexpected EOF. \'endsolid\' keyword was expected");
				break;
			}
			// facet normal -0.13 -0.13 -0.98
			if (!sz.strncmp("facet") && ParsingUtil.isSpaceOrNewLine(/*(sz+5)*/(byte)sz.get(sz.getCurrent() + 5)))	{

				if (3 != curVertex) {
					if(DefaultLogger.LOG_OUT)
						DefaultLogger.warn("STL: A new facet begins but the old is not yet complete");
				}
				if (mNumFaces == curFace)	{
//					ai_assert(pMesh->mNumFaces != 0);

					// need to resize the arrays, our size estimate was wrong
					int iNeededSize = /*(unsigned int)(sz-mBuffer)*/sz.getCurrent() / mNumFaces;
					if (iNeededSize <= 160)iNeededSize >>= 1; // prevent endless looping
					int add = ((/*mBuffer+*/fileSize)-sz.getCurrent()) / iNeededSize;
					add += add >> 3; // add 12.5% as buffer
					iNeededSize = (mNumFaces + add)*3;
//					aiVector3D* pv = new aiVector3D[iNeededSize];
//					memcpy(pv,pMesh->mVertices,pMesh->mNumVertices*sizeof(aiVector3D));
//					delete[] pMesh->mVertices;
//					pMesh->mVertices = pv;
					FloatBuffer pv = MemoryUtil.createFloatBuffer(iNeededSize * 3, natived);
					pv.put(pMesh.mVertices).position(0);
					pMesh.mVertices = pv;
					
					pv = MemoryUtil.createFloatBuffer(iNeededSize * 3, natived);
					pv.put(pMesh.mNormals).position(0);
					pMesh.mNormals = pv;

					pMesh.mNumVertices = iNeededSize;
					mNumFaces += add;
				}
//				aiVector3D* vn = &pMesh->mNormals[curFace++*3];
				int normal_index = curFace++ * 3;

//				sz += 6;
				sz.inCre(6);
				curVertex = 0;
//				SkipSpaces(&sz);
				sz.skipSpaces();
				if (sz.strncmp("normal"))	{
					if(DefaultLogger.LOG_OUT)
						DefaultLogger.warn("STL: a facet normal vector was expected but not found");
				}
				else
				{
//					sz += 7;
					sz.inCre(7);
					sz.skipSpaces()/*SkipSpaces(&sz)*/;
					vec3.x = (float) sz.fast_atoreal_move(true)/*sz = fast_atoreal_move<float>(sz, (float&)vn->x )*/; 
					sz.skipSpaces()/*SkipSpaces(&sz)*/;
					vec3.y = (float) sz.fast_atoreal_move(true)/*sz = fast_atoreal_move<float>(sz, (float&)vn->y )*/; 
					sz.skipSpaces()/*SkipSpaces(&sz)*/;
					vec3.z = (float) sz.fast_atoreal_move(true)/*sz = fast_atoreal_move<float>(sz, (float&)vn->z )*/; 
//					*(vn+1) = *vn;
//					*(vn+2) = *vn;
					pMesh.mNormals.position(3 * normal_index);
					vec3.store(pMesh.mNormals);  // vn
					vec3.store(pMesh.mNormals);  // vn + 1
					vec3.store(pMesh.mNormals);  // vn + 2
					pMesh.mNormals.position(0);
				}
			}
			// vertex 1.50000 1.50000 0.00000
			else if (!sz.strncmp("vertex") && ParsingUtil.isSpaceOrNewLine(/**(sz+6)*/(byte)sz.get(sz.getCurrent() + 6)))
			{
				if (3 == curVertex)	{
					DefaultLogger.error("STL: a facet with more than 3 vertices has been found");
				}
				else
				{
//					sz += 7;
					sz.inCre(7);
					sz.skipSpaces()/*SkipSpaces(&sz)*/;
//					aiVector3D* vn = &pMesh->mVertices[(curFace-1)*3 + curVertex++];
					int index = ((curFace-1)*3 + curVertex++) * 3;
//					sz = fast_atoreal_move<float>(sz, (float&)vn->x ); 
//					SkipSpaces(&sz);
//					sz = fast_atoreal_move<float>(sz, (float&)vn->y ); 
//					SkipSpaces(&sz);
//					sz = fast_atoreal_move<float>(sz, (float&)vn->z ); 
					vec3.x = (float) sz.fast_atoreal_move(true); sz.skipSpaces();
					vec3.y = (float) sz.fast_atoreal_move(true); sz.skipSpaces();
					vec3.z = (float) sz.fast_atoreal_move(true); 
					
					pMesh.mVertices.put(index++, vec3.x);
					pMesh.mVertices.put(index++, vec3.y);
					pMesh.mVertices.put(index++, vec3.z);
				}
			}
			else if (!sz.strncmp("endsolid"/*,8*/))	{
				// finished!
				break;
			}
			// else skip the whole identifier
			else while (!ParsingUtil.isSpaceOrNewLine((byte)sz.get())) {
				sz.inCre();
			}
		}

		if (curFace == 0)	{
			mNumFaces = 0;
			throw new DeadlyImportError("STL: ASCII file is empty or invalid; no data loaded");
		}
		mNumFaces = curFace;
		pMesh.mNumVertices = curFace*3;
		// we are finished!
	}
	
	// ------------------------------------------------------------------------------------------------
	// Read a binary STL file
	boolean loadBinaryFile()
	{
		final int fileSize = mBuffer.limit();
		// skip the first 80 bytes
		if (fileSize < 84) {
			throw new DeadlyImportError("STL: file is too small for the header");
		}
		boolean bIsMaterialise = false;

		// search for an occurence of "COLOR=" in the header
//		const char* sz2 = (const char*)mBuffer;
//		const char* const szEnd = sz2+80;
		mBuffer.limit(80);
		while (/*sz2 < szEnd*/mBuffer.remaining() > 0)	{

			if ('C' == mBuffer.get()/**sz2++*/ && 'O' == mBuffer.get()/**sz2++*/ && 'L' == mBuffer.get()/**sz2++*/ &&
				'O' == mBuffer.get()/**sz2++*/ && 'R' == mBuffer.get()/**sz2++*/ && '=' == mBuffer.get()/**sz2++*/)	{

				// read the default vertex color for facets
				bIsMaterialise = true;
				if(DefaultLogger.LOG_OUT)
					DefaultLogger.info("STL: Taking code path for Materialise files");
				clrColorDefault.x = (mBuffer.get() & 0xFF)/**sz2++*/ / 255.0f;
				clrColorDefault.y = (mBuffer.get() & 0xFF)/**sz2++*/ / 255.0f;
				clrColorDefault.z = (mBuffer.get() & 0xFF)/**sz2++*/ / 255.0f;
				clrColorDefault.w = (mBuffer.get() & 0xFF)/**sz2++*/ / 255.0f;
				break;
			}
		}
//		const unsigned char* sz = (const unsigned char*)mBuffer + 80;
		mBuffer.position(80).limit(fileSize);

		// now read the number of facets
		Mesh pMesh = pScene.mMeshes[0];
		pScene.mRootNode.mName = ("<STL_BINARY>");

//		pMesh->mNumFaces = *((uint32_t*)sz);
//		sz += 4;
		mNumFaces = mBuffer.getInt();

		if (fileSize < 84 + mNumFaces*50) {
			throw new DeadlyImportError("STL: file is too small to hold all facets");
		}

		if (mNumFaces == 0) {
			throw new DeadlyImportError("STL: file is empty. There are no facets defined");
		}

		pMesh.mNumVertices = mNumFaces*3;

		final boolean natived = AssimpConfig.MESH_USE_NATIVE_MEMORY;
		FloatBuffer vp,vn;
		vp = pMesh.mVertices = MemoryUtil.createFloatBuffer(pMesh.mNumVertices * 3, natived);
		vn = pMesh.mNormals  = MemoryUtil.createFloatBuffer(pMesh.mNumVertices * 3, natived);
		final Vector3f vec3 = new Vector3f();
		final Vector4f vec4 = new Vector4f();
		for (int i = 0; i < mNumFaces;++i)	{

			// NOTE: Blender sometimes writes empty normals ... this is not
			// our fault ... the RemoveInvalidData helper step should fix that
//			*vn = *((aiVector3D*)sz);
//			sz += sizeof(aiVector3D);
//			*(vn+1) = *vn;
//			*(vn+2) = *vn;
//			vn += 3;
			vec3.x = mBuffer.getFloat();
			vec3.y = mBuffer.getFloat();
			vec3.z = mBuffer.getFloat();
			vec3.store(vn);
			vec3.store(vn);
			vec3.store(vn);

//			*vp++ = *((aiVector3D*)sz);
//			sz += sizeof(aiVector3D);
			vec3.x = mBuffer.getFloat();
			vec3.y = mBuffer.getFloat();
			vec3.z = mBuffer.getFloat();
			vec3.store(vp);

//			*vp++ = *((aiVector3D*)sz);
//			sz += sizeof(aiVector3D);
			vec3.x = mBuffer.getFloat();
			vec3.y = mBuffer.getFloat();
			vec3.z = mBuffer.getFloat();
			vec3.store(vp);

//			*vp++ = *((aiVector3D*)sz);
//			sz += sizeof(aiVector3D);
			vec3.x = mBuffer.getFloat();
			vec3.y = mBuffer.getFloat();
			vec3.z = mBuffer.getFloat();
			vec3.store(vp);

//			uint16_t color = *((uint16_t*)sz);
//			sz += 2;
			int color = mBuffer.getShort() & 0xFFFF;

			if ((color & (1 << 15)) !=0)
			{
				// seems we need to take the color
				if (pMesh.mColors[0] == null)
				{
//					pMesh->mColors[0] = new aiColor4D[pMesh->mNumVertices];
					pMesh.mColors[0] = MemoryUtil.createFloatBuffer(pMesh.mNumVertices * 4, natived);
					for (int k = 0; k <pMesh.mNumVertices;++k)
//						*pMesh->mColors[0]++ = this->clrColorDefault;
						clrColorDefault.store(pMesh.mColors[0]);
//					pMesh->mColors[0] -= pMesh->mNumVertices;
					pMesh.mColors[0].flip();

					if(DefaultLogger.LOG_OUT)
						DefaultLogger.info("STL: Mesh has vertex colors");
				}
//				aiColor4D* clr = &pMesh->mColors[0][i*3];
//				clr->a = 1.0f;
				Vector4f clr = vec4;
				clr.w = 1.0f;
				int clr_index = (i*3)*4;
				if (bIsMaterialise) // this is reversed
				{
					clr.x = (color & 0x31) / 31.0f;
					clr.y = ((color & (0x31<<5))>>5) / 31.0f;
					clr.z = ((color & (0x31<<10))>>10) / 31.0f;
				}
				else
				{
					clr.z = (color & 0x31) / 31.0f;
					clr.y = ((color & (0x31<<5))>>5) / 31.0f;
					clr.x = ((color & (0x31<<10))>>10) / 31.0f;
				}
				// assign the color to all vertices of the face
//				*(clr+1) = *clr;
//				*(clr+2) = *clr;
				pMesh.mColors[0].position(clr_index);
				clr.store(pMesh.mColors[0]);
				clr.store(pMesh.mColors[0]);
				clr.store(pMesh.mColors[0]);
				pMesh.mColors[0].position(0);
			}
		}
		if (bIsMaterialise && pMesh.mColors[0] == null)
		{
			// use the color as diffuse material color
			return true;
		}
		return false;
	}
	
	// A valid binary STL buffer should consist of the following elements, in order:
	// 1) 80 byte header
	// 2) 4 byte face count
	// 3) 50 bytes per face
	static boolean isBinarySTL(ByteBuffer buffer/*, unsigned int fileSize*/) {
		int fileSize = buffer.limit();
		if (fileSize < 84)
			return false;

//		const uint32_t faceCount = *reinterpret_cast<const uint32_t*>(buffer + 80);
//		const uint32_t expectedBinaryFileSize = faceCount * 50 + 84;
		final int faceCount = buffer.getInt(80);
		final int expectedBinaryFileSize = faceCount * 50 + 84;

		return expectedBinaryFileSize == fileSize;
	}

	// An ascii STL buffer will begin with "solid NAME", where NAME is optional.
	// Note: The "solid NAME" check is necessary, but not sufficient, to determine
	// if the buffer is ASCII; a binary header could also begin with "solid NAME".
	static boolean isAsciiSTL(ByteBuffer buffer/*, unsigned int fileSize*/) {
		if (isBinarySTL(buffer/*, fileSize*/))
			return false;
		final int fileSize = buffer.limit();
		final int src_pos = buffer.position();
//		const char* bufferEnd = buffer + fileSize;
//
//		if (!SkipSpaces(&buffer))
//			return false;
//
//		if (buffer + 5 >= bufferEnd)
//			return false;
		int cursor = src_pos;
		while(cursor < fileSize){
			byte b = buffer.get(cursor);
			if(b == ' ' || b == '\t')
				cursor++;
			else
				break;
		}
		
		if(cursor + 5 >= fileSize)
			return false;
		
		buffer.position(cursor);
		boolean r= AssUtil.equals(buffer, "solid", 0, 5);
		buffer.position(src_pos);
		return r;
//		return strncmp(buffer, "solid", 5) == 0;
	}

}
