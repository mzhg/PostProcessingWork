package assimp.importer.off;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.StringTokenizer;

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
import assimp.common.Scene;

/** Importer class for the Object File Format (.off)
*/
public class OFFImporter extends BaseImporter{

	static final ImporterDesc desc = new ImporterDesc(
		"OFF Importer",
		"",
		"",
		"",
		ImporterDesc.aiImporterFlags_SupportBinaryFlavour,
		0,
		0,
		0,
		0,
		"off" 
	);
	
	@Override
	protected boolean canRead(String pFile, InputStream pIOHandler,boolean checkSig) throws IOException {
		String extension = getExtension(pFile);

		if (extension.equals("off"))
			return true;
		else if (extension.length() == 0 || checkSig)
		{
			if (pIOHandler == null)return true;
			String[] tokens = {"off"};
			return searchFileHeaderForToken(pIOHandler,pFile,tokens);
		}
		return false;
	}

	@Override
	protected ImporterDesc getInfo() { return desc;}

	@Override
	protected void internReadFile(File pFile, Scene pScene) {
		ByteBuffer buffer = FileUtils.loadText(pFile, false, AssimpConfig.LOADING_USE_NATIVE_MEMORY);
		byte[] cache = new byte[4096];
		String line = AssUtil.getNextLine(buffer, cache, true);
		if ('O' == line.charAt(0)) {
			line = AssUtil.getNextLine(buffer, cache, true); // skip the 'OFF' line
		}
		
		StringTokenizer tokenizer = new StringTokenizer(line, "\t ");
		final int numVertices = AssUtil.parseInt(tokenizer.nextToken());
		int numFaces    = AssUtil.parseInt(tokenizer.nextToken());
		
		pScene.mMeshes = new Mesh[ /*pScene.mNumMeshes = */1 ];
		Mesh mesh = pScene.mMeshes[0] = new Mesh();
		mesh.mFaces = new Face [/*mesh->mNumFaces =*/ numFaces];
		
//		std::vector<aiVector3D> tempPositions(numVertices);
		FloatBuffer tempPositions = MemoryUtil.createFloatBuffer(numVertices * 3, AssimpConfig.LOADING_USE_NATIVE_MEMORY);

		// now read all vertex lines
		for (int i = 0; i< numVertices;++i)
		{
			if((line = AssUtil.getNextLine(buffer, cache, true)) == null)
			{
				DefaultLogger.error("OFF: The number of verts in the header is incorrect");
				break;
			}
//			aiVector3D& v = tempPositions[i];
//
//			sz = line; SkipSpaces(&sz);
//			sz = fast_atoreal_move<float>(sz,(float&)v.x); SkipSpaces(&sz);
//			sz = fast_atoreal_move<float>(sz,(float&)v.y); SkipSpaces(&sz);
//			fast_atoreal_move<float>(sz,(float&)v.z);
			tokenizer = new StringTokenizer(line, "\t ");
			tempPositions.put(AssUtil.parseFloat(tokenizer.nextToken()));  // x
			tempPositions.put(AssUtil.parseFloat(tokenizer.nextToken()));  // y
			tempPositions.put(AssUtil.parseFloat(tokenizer.nextToken()));  // z
		}
		tempPositions.flip();
		
		// First find out how many vertices we'll need
//		const char* old = buffer;
		final int old = buffer.position();
		int face_index = 0;
		for (int i = 0; i< numFaces;++i)
		{
//			if(!GetNextLine(buffer,line))
			if((line = AssUtil.getNextLine(buffer, cache, true)) == null)
			{
				DefaultLogger.error("OFF: The number of faces in the header is incorrect");
				break;
			}
			if(line.length() == 0)
				continue;
			
//			sz = line;SkipSpaces(&sz);
			int space = line.indexOf(' ');
			int numIndices = AssUtil.parseInt(line.substring(0, space));
//			if(!(faces->mNumIndices = strtoul10(sz,&sz)) || faces->mNumIndices > 9)
			if(numIndices == 0 || numIndices > 9)
			{
				DefaultLogger.error("OFF: Faces with zero indices aren't allowed");
				--numFaces;
				continue;
			}
			mesh.mFaces[face_index] = Face.createInstance(numIndices);
			mesh.mNumVertices += numIndices;
			face_index++;
//			++faces;
		}

		if (mesh.mNumVertices == 0)
			throw new DeadlyImportError("OFF: There are no valid faces");

		// allocate storage for the output vertices
//		aiVector3D* verts = mesh->mVertices = new aiVector3D[mesh->mNumVertices];
		FloatBuffer verts = mesh.mVertices = MemoryUtil.createFloatBuffer(mesh.mNumVertices * 3, AssimpConfig.MESH_USE_NATIVE_MEMORY);

		// second: now parse all face indices
//		buffer = old;faces = mesh->mFaces;
		buffer.position(old);
		face_index = 0;
		for (int i = 0, p = 0; i< numFaces;)
		{
//			if(!GetNextLine(buffer,line))break;
			if((line = AssUtil.getNextLine(buffer, cache, true)) == null) break;

//			int idx;
//			sz = line;SkipSpaces(&sz);
//			if(!(idx = strtoul10(sz,&sz)) || idx > 9)
//				continue;
			tokenizer = new StringTokenizer(line, "\t ");
			int idx = AssUtil.parseInt(tokenizer.nextToken());
			if(idx == 0 || idx > 9)
				continue;

//			facesmIndices = new unsigned int [faces->mNumIndices];
			Face f = mesh.mFaces[face_index];
			for (int m = 0; m < f.getNumIndices();++m)
			{
//				SkipSpaces(&sz);
				idx = AssUtil.parseInt(tokenizer.nextToken());
				if (idx/*(idx = strtoul10(sz,&sz))*/ >= numVertices)
				{
					DefaultLogger.error("OFF: Vertex index is out of range");
					idx = numVertices-1;
				}
//				faces->mIndices[m] = p++;
				f.set(m, p++);
//				*verts++ = tempPositions[idx];
				int index = 3 * idx;
				verts.put(tempPositions.get(index++));
				verts.put(tempPositions.get(index++));
				verts.put(tempPositions.get(index++));
			}
			++i;
			++face_index;
		}
		
		verts.flip();
		
		// generate the output node graph
		pScene.mRootNode = new Node();
		pScene.mRootNode.mName = ("<OFFRoot>");
		pScene.mRootNode.mMeshes = new int [/*pScene.mRootNode.mNumMeshes =*/ 1];
		pScene.mRootNode.mMeshes[0] = 0;

		// generate a default material
		pScene.mMaterials = new Material[/*pScene.mNumMaterials =*/ 1];
		Material pcMat = new Material();

		Vector4f clr = new Vector4f(0.6f,0.6f,0.6f,1.0f);
		pcMat.addProperty(clr,Material.AI_MATKEY_COLOR_DIFFUSE,0,0);
		pScene.mMaterials[0] = pcMat;

		int twosided =1;
		pcMat.addProperty(twosided,Material.AI_MATKEY_TWOSIDED,0,0);
	}

}
