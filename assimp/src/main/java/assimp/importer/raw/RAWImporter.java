package assimp.importer.raw;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
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
import assimp.common.ParsingUtil;
import assimp.common.Scene;
import assimp.common.TextureType;
/** Importer class for the PovRay RAW triangle format
*/
public class RAWImporter extends BaseImporter{

	static final ImporterDesc desc = new ImporterDesc(
			"Raw Importer",
			"",
			"",
			"",
			ImporterDesc.aiImporterFlags_SupportTextFlavour,
			0,
			0,
			0,
			0,
			"raw"
		);
	
	@Override
	protected boolean canRead(String pFile, InputStream pIOHandler,boolean checkSig) throws IOException {
		return simpleExtensionCheck(pFile,"raw",null,null);
	}

	@Override
	protected ImporterDesc getInfo() { return desc;}

	@Override
	protected void internReadFile(File pFile, Scene pScene) {
		ByteBuffer buffer = FileUtils.loadText(pFile, false, AssimpConfig.LOADING_USE_NATIVE_MEMORY);
		
		// list of groups loaded from the file
		ArrayList<GroupInformation> outGroups = new ArrayList<>();
		outGroups.add(new GroupInformation("<default>"));
		int curGroup = 0;
		float[] data = new float[12];
		String[] tokenArray = new String[13];
		// now read all lines
		byte[] cache = new byte[4096];
		String line;
		while((line = AssUtil.getNextLine(buffer, cache, true)) != null){
			if(line.length() == 0)
				continue;
			
			if(!Character.isDigit(line.charAt(0))){
				int sz2 = 0;
				while(ParsingUtil.isSpaceOrNewLine((byte)line.charAt(sz2))) ++sz2;
				final int length = sz2;
				
				// find an existing group with this name
				int count = 0;
				for(GroupInformation it : outGroups){
					if(length == it.name.length() && line.startsWith(it.name)){
						curGroup = count;
						sz2 = -1;
						break;
					}
					
					count++;
				}
				
				if(sz2 != -1){
					outGroups.add(new GroupInformation(line.substring(0, sz2)));
					curGroup = outGroups.size() - 1;
				}
			}else{
				StringTokenizer tokens = new StringTokenizer(line);
				
				int count = 0;
				
				while(tokens.hasMoreTokens()){
					tokenArray[count] = tokens.nextToken();
				}
				
				int len = (count == 12 || count ==9) ? count: count -1;
				for(int i = 0;i < len; i--)
					data[i] = AssUtil.parseFloat(tokenArray[i]);
				
				if (count != 12 && count != 9  && count != 13 && count != 10){
					DefaultLogger.error("A line may have either 9 or 12 floats and an optional texture");
					continue;
				}
				
				MeshInformation output = null;
				String sz;
				if(count == 10 || count == 13){
					sz = tokenArray[count - 1];
				}else if(count == 9){
					sz = "%default%";
				}else{
					sz = "";
				}
				
				// search in the list of meshes whether we have one with this texture
				for(MeshInformation it : outGroups.get(curGroup).meshes){
					if((sz.length() > 0 ? sz.equals(it.name) : true)){
						output = it;
						break;
					}
				}
				
				// if we don't have the mesh, create it
				if (output == null)
				{
					outGroups.get(curGroup).meshes.add(output = new MeshInformation(sz));
//					output = &((*curGroup).meshes.back());
				}
				if (12 == count)
				{
//					aiColor4D v(data[0],data[1],data[2],1.0f);
//					output->colors.push_back(v);
//					output->colors.push_back(v);
//					output->colors.push_back(v);

//					output->vertices.push_back(aiVector3D(data[3],data[4],data[5]));
//					output->vertices.push_back(aiVector3D(data[6],data[7],data[8]));
//					output->vertices.push_back(aiVector3D(data[9],data[10],data[11]));
					output.colors.put(data,0,3).put(1.0f);
					output.colors.put(data,0,3).put(1.0f);
					output.colors.put(data,0,3).put(1.0f);
					output.vertices.put(data, 3, 9);
				}
				else
				{
//					output->vertices.push_back(aiVector3D(data[0],data[1],data[2]));
//					output->vertices.push_back(aiVector3D(data[3],data[4],data[5]));
//					output->vertices.push_back(aiVector3D(data[6],data[7],data[8]));
					output.vertices.put(data, 0, 9);
				}
			}
		}
		
		pScene.mRootNode = new Node();
		pScene.mRootNode.mName = ("<RawRoot>");
		int numChildren = 0;
		int numMeshes = 0;
		// count the number of valid groups
		// (meshes can't be empty)
//		for (std::vector< GroupInformation >::iterator it = outGroups.begin(), end = outGroups.end();
//			it != end;++it)
		for(GroupInformation it : outGroups)
		{
			if (!it.meshes.isEmpty())
			{
				++numChildren; 
				numMeshes += it.meshes.size();
				
				for(MeshInformation mesh : it.meshes){
					mesh.colors.flip();
					mesh.vertices.flip();
				}
			}
		}

		if (numMeshes == 0)
		{
			throw new DeadlyImportError("RAW: No meshes loaded. The file seems to be corrupt or empty.");
		}

		pScene.mMeshes = new Mesh[numMeshes];
		Node[] cc = null;
		int cc_cursor = 0;
		if (1 == numChildren)
		{
//			cc = pScene.mRootNode;
			numChildren = 0;
		}
		else cc = pScene.mRootNode.mChildren = new Node[/*pScene.mRootNode.mN*/numChildren];

//		pScene.mNumMaterials = pScene.mNumMeshes;
		Material[] mats = pScene.mMaterials = new Material[numMeshes/*pScene.mNumMaterials*/];
		int mat_cursor = 0;
		Vector4f clr = new Vector4f(1, 1, 1, 1);
		int meshIdx = 0;
//		for (std::vector< GroupInformation >::iterator it = outGroups.begin(), end = outGroups.end();
//			it != end;++it)
		for(GroupInformation it : outGroups)
		{
			if (it.meshes.isEmpty())continue;
			
			Node node;
			if (/*pScene.mRootNode->mN*/numChildren > 0)
			{
				node = cc[cc_cursor] = new Node();
				node.mParent = pScene.mRootNode;
			}
			else node = pScene.mRootNode;++cc_cursor;
//			node->mName.Set((*it).name);
			node.mName = it.name;

			// add all meshes
//			node->mNumMeshes = (unsigned int)(*it).meshes.size();
//			unsigned int* pi = node->mMeshes = new unsigned int[ node->mNumMeshes ];
			node.mMeshes = new int[it.meshes.size()];
			int pi = 0;
//			for (std::vector< MeshInformation >::iterator it2 = (*it).meshes.begin(),
//				end2 = (*it).meshes.end(); it2 != end2; ++it2)
			for(MeshInformation it2 : it.meshes)
			{
//				ai_assert(!(*it2).vertices.empty());

				// allocate the mesh
				node.mMeshes[pi++] = meshIdx;
				Mesh mesh = pScene.mMeshes[meshIdx] = new Mesh();
				mesh.mMaterialIndex = meshIdx++;

				mesh.mPrimitiveTypes = Mesh.aiPrimitiveType_TRIANGLE;

				// allocate storage for the vertex components and copy them
				mesh.mNumVertices = it2.vertices.remaining()/3;
//				mesh.mVertices = new aiVector3D[ mesh->mNumVertices ];
//				::memcpy(mesh->mVertices,&(*it2).vertices[0],sizeof(aiVector3D)*mesh->mNumVertices);
				final boolean natived = AssimpConfig.MESH_USE_NATIVE_MEMORY;
				mesh.mVertices = MemoryUtil.refCopy(it2.vertices, natived);

				if (it2.colors.remaining() > 0)
				{
					assert(it2.colors.remaining()/4 == mesh.mNumVertices);

//					mesh->mColors[0] = new aiColor4D[ mesh->mNumVertices ];
//					::memcpy(mesh->mColors[0],&(*it2).colors[0],sizeof(aiColor4D)*mesh->mNumVertices);
					mesh.mColors[0] = MemoryUtil.refCopy(it2.colors, natived);
				}

				// generate triangles
				assert(0 == mesh.mNumVertices % 3);
				/*aiFace* fc = */mesh.mFaces = new Face[/* mesh.mNumFaces = */mesh.mNumVertices/3 ];
//				aiFace* const fcEnd = fc + mesh->mNumFaces;
				final int fcEnd = mesh.mFaces.length;
				int fc = 0;
				int n = 0;
				while (fc != fcEnd)
				{
//					aiFace& f = *fc++;
//					f.mIndices = new unsigned int[f.mNumIndices = 3];
//					for (unsigned int m = 0; m < 3;++m)
//						f.mIndices[m] = n++;
					Face f = mesh.mFaces[fc++] = Face.createInstance(3);
					for(int m = 0; m < 3; m++)
						f.set(m, n++);
				}

				// generate a material for the mesh
				Material mat = new Material();

				clr.set(1.0f,1.0f,1.0f,1.0f);
				if ("%default%".equals(it2.name)) // a gray default material
				{
					clr.x = clr.y = clr.z = 0.6f;
				}
				else if (it2.name.length() > 0) // a texture
				{
//					aiString s;
//					s.Set((*it2).name);
					mat.addProperty(it2.name, Material._AI_MATKEY_TEXTURE_BASE, TextureType.aiTextureType_DIFFUSE.ordinal(), 0);
				}
				mat.addProperty(clr,Material.AI_MATKEY_COLOR_DIFFUSE,0,0);
//				*mats++ = mat;
				mats[mat_cursor++] = mat;
			}
		}
	}

}
