package assimp.importer.unreal;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;

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
import assimp.common.Pair;
import assimp.common.ParsingUtil;
import assimp.common.Scene;
import assimp.common.StreamReader;
import assimp.common.TextureType;

public class UnrealImporter extends BaseImporter{
	
		/*
		0 = Normal one-sided
		1 = Normal two-sided
		2 = Translucent two-sided
		3 = Masked two-sided
		4 = Modulation blended two-sided
		8 = Placeholder triangle for weapon positioning (invisible)
		*/
//	enum MeshFlags {
	static final int
		MF_NORMAL_OS            = 0,
		MF_NORMAL_TS            = 1,
		MF_NORMAL_TRANS_TS      = 2,
		MF_NORMAL_MASKED_TS     = 3,
		MF_NORMAL_MOD_TS        = 4,
		MF_WEAPON_PLACEHOLDER   = 8;
//	};


	static final ImporterDesc desc = new ImporterDesc(
		"Unreal Mesh Importer",
		"",
		"",
		"",
		ImporterDesc.aiImporterFlags_SupportTextFlavour,
		0,
		0,
		0,
		0,
		"3d uc" 
	);
	
	//! frame to be loaded
	int configFrameID;

	//! process surface flags
	boolean configHandleFlags = true;
	@Override
	protected boolean canRead(String pFile, InputStream pIOHandler,boolean checkSig) throws IOException {
		return simpleExtensionCheck(pFile,"3d","uc",null);
	}

	@Override
	protected ImporterDesc getInfo() { return desc;}
	
	@Override
	public void setupProperties(Importer pImp) {
		// The 
		// AI_CONFIG_IMPORT_UNREAL_KEYFRAME option overrides the
		// AI_CONFIG_IMPORT_GLOBAL_KEYFRAME option.
		configFrameID = pImp.getPropertyInteger(AssimpConfig.AI_CONFIG_IMPORT_UNREAL_KEYFRAME,-1);
		if(/*static_cast<unsigned int>*/(-1) == configFrameID)	{
			configFrameID = pImp.getPropertyInteger(AssimpConfig.AI_CONFIG_IMPORT_GLOBAL_KEYFRAME,0);
		}

		// AI_CONFIG_IMPORT_UNREAL_HANDLE_FLAGS, default is true
		configHandleFlags = (0 != pImp.getPropertyInteger(AssimpConfig.AI_CONFIG_IMPORT_UNREAL_HANDLE_FLAGS,1));
	}

	@Override
	protected void internReadFile(File pFile, Scene pScene) {
		// For any of the 3 files being passed get the three correct paths
		// First of all, determine file extension
		String filename = pFile.getName();
		int pos = filename.lastIndexOf('.');
		String extension = pos == -1 ? filename : filename.substring(0, pos);
		String d_path,a_path,uc_path;
		
		// build proper paths
		d_path  = extension+"_d.3d";
		a_path  = extension+"_a.3d";
		uc_path = extension+".uc";

		if(DefaultLogger.LOG_OUT){
			DefaultLogger.debug("UNREAL: data file is " + d_path);
			DefaultLogger.debug("UNREAL: aniv file is " + a_path);
			DefaultLogger.debug("UNREAL: uc file is "   + uc_path);
		}
		
		int numTris = -1;
		int numVert = -1;
		Triangle[] triangles = null;
		
		try(BufferedInputStream in = new BufferedInputStream(new FileInputStream(d_path));
				StreamReader d_reader = new StreamReader(in, true)){
			numTris = d_reader.getI2() & 0xFFFF;
			numVert = d_reader.getI2() & 0xFFFF;
			d_reader.incPtr(44);
			if (numTris == 0|| numVert < 3)
				throw new DeadlyImportError("UNREAL: Invalid number of vertices/triangles");

			// maximum texture index
			int maxTexIdx = 0;

			// collect triangles
//			 triangles(numTris);
			triangles = new Triangle[numTris];
			AssUtil.initArray(triangles);
//			for (std::vector<Triangle>::iterator it = triangles.begin(), end = triangles.end();it != end; ++it)	{
			for(Triangle tri : triangles){
//				Triangle& tri = *it;

				for (int i = 0; i < 3;++i)	{

					tri.mVertex[i] = d_reader.getI2();
					if (tri.mVertex[i] >= numTris)	{
						if(DefaultLogger.LOG_OUT)
							DefaultLogger.warn("UNREAL: vertex index out of range");
						tri.mVertex[i] = 0;
					}
				}
				tri.mType = d_reader.getI1();

				// handle mesh flagss?
				if (configHandleFlags)
					tri.mType = MF_NORMAL_OS;
				else {
					// ignore MOD and MASKED for the moment, treat them as two-sided
					if (tri.mType == MF_NORMAL_MOD_TS || tri.mType == MF_NORMAL_MASKED_TS)
						tri.mType = MF_NORMAL_TS;
				}
				d_reader.incPtr(1);

				for ( int i = 0; i < 3;++i)
					for ( int i2 = 0; i2 < 2;++i2)
						tri.mTex[i][i2] = d_reader.getI1();

				tri.mTextureNum = d_reader.getI1();
				maxTexIdx = Math.max(maxTexIdx,tri.mTextureNum & 0xFF);
				d_reader.incPtr(1);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		FloatBuffer vertices = null;
		ArrayList<Pair<Integer, String>> textures = new ArrayList<Pair<Integer,String>>();
		
		try(BufferedInputStream in = new BufferedInputStream(new FileInputStream(a_path));
				StreamReader a_reader = new StreamReader(in, true)){
			// read number of frames
			final int numFrames = a_reader.getI2() & 0xFFFF;
			if (configFrameID >= numFrames)
				throw new DeadlyImportError("UNREAL: The requested frame does not exist");

			int st = a_reader.getI2() & 0xFFFF; 
			if (st != numVert*4)
				throw new DeadlyImportError("UNREAL: Unexpected aniv file length");

			// skip to our frame
			a_reader.incPtr(configFrameID *numVert*4);

			// collect vertices
//			std::vector<aiVector3D> vertices(numVert);
//			for (std::vector<aiVector3D>::iterator it = vertices.begin(), end = vertices.end(); it != end; ++it)	{
//				int32_t val = a_reader.GetI4();
//				DecompressVertex(*it,val);
//			}
			vertices = MemoryUtil.createFloatBuffer(numVert * 3, AssimpConfig.LOADING_USE_NATIVE_MEMORY);
			UNVertex v = new UNVertex();
			for(int i = 0; i < numVert; i++){
				v.decompress(a_reader.getI4());
				vertices.put(v.x);
				vertices.put(v.y);
				vertices.put(v.z);
			}
			vertices.flip();

			// list of textures. 
//			std::vector< std::pair<unsigned int, std::string> > textures; 
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// allocate the output scene
		Node nd = pScene.mRootNode = new Node();
		nd.mName =("<UnrealRoot>");
		
		File uc_file = new File(uc_path);
		if(uc_file.exists() && uc_file.canRead()){
			ByteBuffer _data = FileUtils.loadText(new File(uc_path), false, AssimpConfig.LOADING_USE_NATIVE_MEMORY);
			ParsingUtil data = new ParsingUtil(_data);
			ArrayList<Pair<String, String>> tempTextures = new ArrayList<Pair<String,String>>();
			
			// do a quick search in the UC file for some known, usually texture-related, tags
			for (;data.hasNext();data.inCre())	{
				if (data.tokenMatchI("#exec"))	{
					data.skipSpacesAndLineEnd();

					// #exec TEXTURE IMPORT [...] NAME=jjjjj [...] FILE=jjjj.pcx [...] 
					if (data.tokenMatchI("TEXTURE"))	{
						data.skipSpacesAndLineEnd();

						if (data.tokenMatchI("IMPORT"))	{
//							tempTextures.push_back(std::pair< std::string,std::string >());
//							std::pair< std::string,std::string >& me = tempTextures.back();
							Pair<String, String> me = new Pair<String, String>();
							tempTextures.add(me);
							for (;!ParsingUtil.isLineEnd((byte)data.get());data.inCre())	{
//								if (!::ASSIMP_strincmp(data,"NAME=",5))	{
								if(!data.strncmp("NAME=")){
//									const char *d = data+=5;
									data.setCurrent(data.getCurrent() + 5);
									int d = data.getCurrent();
									for (;!ParsingUtil.isSpaceOrNewLine((byte)data.get());data.inCre());
//									me.first = std::string(d,(size_t)(data-d)); 
									me.first = data.getString(d, data.getCurrent());
								}
								else if (!data.strncmp("FILE=")/*!::ASSIMP_strincmp(data,"FILE=",5)*/)	{
//									const char *d = data+=5;
									data.setCurrent(data.getCurrent() + 5);
									int d = data.getCurrent();
									for (;!ParsingUtil.isSpaceOrNewLine((byte)data.get());/*++*/data.inCre());
//									me.second = std::string(d,(size_t)(data-d)); 
									me.first = data.getString(d, data.getCurrent());
								}
							}
							if (me.first.length() == 0|| me.second.length() == 0)
//								tempTextures.pop_back();
								tempTextures.remove(tempTextures.size() - 1);
						}
					}
					// #exec MESHMAP SETTEXTURE MESHMAP=box NUM=1 TEXTURE=Jtex1
					// #exec MESHMAP SCALE MESHMAP=box X=0.1 Y=0.1 Z=0.2
					else if (data.tokenMatchI("MESHMAP"/*,7*/)) {
//						SkipSpacesAndLineEnd(&data);
						data.skipSpacesAndLineEnd();

						if (data.tokenMatchI("SETTEXTURE")) {
							Pair<Integer, String> me = new Pair<Integer, String>();
							textures.add(/*std::pair<unsigned int, std::string>()*/me);
//							std::pair<unsigned int, std::string>& me = textures.back();

							for (;!ParsingUtil.isLineEnd((byte)data.get());data.inCre())	{
								if (!/*::ASSIMP_strincmp(data,"NUM=",4)*/data.strncmp("NUM="))	{
//									data += 4;
									data.setCurrent(data.getCurrent() + 4);
									me.first = data.strtoul10(/*data,&data*/);
								}
								else if (!/*::ASSIMP_strincmp(data,"TEXTURE=",8)*/data.strncmp("TEXTURE="))	{
//									data += 8;
									data.setCurrent(data.getCurrent() + 8);
									int d = data.getCurrent();
//									for (;!IsSpaceOrNewLine(*data);++data);
									for(;!ParsingUtil.isSpaceOrNewLine((byte)data.get()); data.inCre());
//									me.second = std::string(d,(size_t)(data-d)); 
									me.second = data.getString(d, data.getCurrent());
						
									// try to find matching path names, doesn't care if we don't find them
//									for (std::vector< std::pair< std::string,std::string > >::const_iterator it = tempTextures.begin();
//										 it != tempTextures.end(); ++it)	{
									for(Pair<String, String> it : tempTextures){  // TODO Can instead by map.
										if (it.first.equals(me.second))	{
											me.second = it.second;
											break;
										}
									}
								}	
							}
						}
						else if (data.tokenMatchI("SCALE")) {

							for (;!ParsingUtil.isLineEnd((byte)data.get());data.inCre())	{
								int curr = data.getCurrent();
								if (data.get(curr) == 'X' && data.get(curr + 1) == '=')	{
									data.setCurrent(curr + 2);
//									data = fast_atoreal_move<float>(data+2,(float&)nd->mTransformation.a1);
									nd.mTransformation.m00 = (float) data.fast_atoreal_move(true);
								}
								else if (data.get(curr) == 'Y' && data.get(curr + 1) == '=')	{
									data.setCurrent(curr + 2);
//									data = fast_atoreal_move<float>(data+2,(float&)nd->mTransformation.b2);
									nd.mTransformation.m11 = (float) data.fast_atoreal_move(true);
								}
								else if (data.get(curr) == 'Z' && data.get(curr + 1) == '=')	{
									data.setCurrent(curr + 2);
//									data = fast_atoreal_move<float>(data+2,(float&)nd->mTransformation.c3);
									nd.mTransformation.m22 = (float) data.fast_atoreal_move(true);
								}
							}
						}
					}
				}
			}
		}else{
			DefaultLogger.error("Unable to open .uc file");
		}
		
//		std::vector<TempMat> materials;
//		materials.reserve(textures.size()*2+5);
		ArrayList<TempMat> materials = new ArrayList<TempMat>(textures.size() * 2 + 5);
		
		int numMeshes = 0;
		// find out how many output meshes and materials we'll have and build material indices
//		for (std::vector<Triangle>::iterator it = triangles.begin(), end = triangles.end();it != end; ++it)	{
		for (Triangle tri : triangles){
//			Triangle& tri = *it;
			TempMat mat = new TempMat(tri);
//			std::vector<TempMat>::iterator nt = std::find(materials.begin(),materials.end(),mat);
			int nt = materials.indexOf(mat);
			if (nt == -1/*nt == materials.end()*/) {
				// add material
				tri.matIndex = materials.size();
				mat.numFaces = 1;
				materials.add(mat);

				++numMeshes;
			}
			else {
				tri.matIndex = /*static_cast<unsigned int>*/(nt/*-materials.begin()*/);
//				++nt.numFaces;
				++materials.get(nt).numFaces;
			}
		}

		if (numMeshes == 0) {
			throw new DeadlyImportError("UNREAL: Unable to find valid mesh data");
		}

		// allocate meshes and bind them to the node graph
		pScene.mMeshes = new Mesh[numMeshes];
		pScene.mMaterials = new Material[numMeshes/*pScene->mNumMaterials = pScene->mNumMeshes*/];

//		nd->mNumMeshes  = pScene->mNumMeshes;
		final boolean natived = AssimpConfig.MESH_USE_NATIVE_MEMORY;
		nd.mMeshes = new int[numMeshes];
		for (int i = 0; i < numMeshes;++i) {
			Mesh m = pScene.mMeshes[i] =  new Mesh();
			m.mPrimitiveTypes = Mesh.aiPrimitiveType_TRIANGLE;

			final int num = materials.get(i).numFaces;
//			m->mFaces            = new aiFace     [num];
//			m->mVertices         = new aiVector3D [num*3];
//			m->mTextureCoords[0] = new aiVector3D [num*3];
			m.mFaces = new Face[num];
			m.mVertices 		= MemoryUtil.createFloatBuffer(num*3*3, natived);
			m.mTextureCoords[0] = MemoryUtil.createFloatBuffer(num*3*3, natived);

			nd.mMeshes[i] = i;

			// create materials, too
			Material mat = new Material();
			pScene.mMaterials[i] = mat;

			// all white by default - texture rulez
			Vector3f color = new Vector3f(1.f,1.f,1.f);

//			aiString s;
//			::sprintf(s.data,"mat%i_tx%i_",i,materials[i].tex);
			String s = String.format("mat%d_tx%d_", i,materials.get(i).tex);

			// set the two-sided flag
			if (materials.get(i).type == MF_NORMAL_TS) {
				final int twosided = 1;
				mat.addProperty(twosided,Material.AI_MATKEY_TWOSIDED,0,0);
//				::strcat(s.data,"ts_");
				s += "ts_";
			}
//			else ::strcat(s.data,"os_");
			else	s += "os_";

			// make TRANS faces 90% opaque that RemRedundantMaterials won't catch us
			if (materials.get(i).type == MF_NORMAL_TRANS_TS)	{
				final float opac = 0.9f;
				mat.addProperty(opac, Material.AI_MATKEY_OPACITY,0,0);
//				::strcat(s.data,"tran_");
				s+= "tran_";
			}
			else /*::strcat(s.data,"opaq_")*/ s+= "opaq_";

			// a special name for the weapon attachment point
			if (materials.get(i).type == MF_WEAPON_PLACEHOLDER)	{
//				s.length = ::sprintf(s.data,"$WeaponTag$");
				s = "$WeaponTag$"; // TODO
				color.set(0.f,0.f,0.f);
			}

			// set color and name
			mat.addProperty(color,Material.AI_MATKEY_COLOR_DIFFUSE,0,0);
//			s.length = ::strlen(s.data);
			mat.addProperty(s, Material.AI_MATKEY_NAME,0,0);

			// set texture, if any
			final int tex = materials.get(i).tex;
//			for (std::vector< std::pair< unsigned int, std::string > >::const_iterator it = textures.begin();it != textures.end();++it)	{
			for (Pair<Integer, String> it : textures){
				if (it.first == tex)	{
//					s.Set((*it).second);
					mat.addProperty(it.second,Material._AI_MATKEY_TEXTURE_BASE, TextureType.aiTextureType_DIFFUSE.ordinal(),0);
					break;
				}
			}
		}

		// fill them.
//		for (std::vector<Triangle>::iterator it = triangles.begin(), end = triangles.end();it != end; ++it)	{
		int numFaces = 0;
		for (Triangle tri : triangles){
//			Triangle& tri = *it;
//			std::vector<TempMat>::iterator nt = std::find(materials.begin(),materials.end(),mat);
			int nt = -1;
			for(int i = 0; i < materials.size(); i++){
				if(materials.get(i).type == tri.mType){
					nt = i;
					break;
				}
			}

			Mesh mesh = pScene.mMeshes[nt/*-materials.begin()*/];
			Face f    = mesh.mFaces[numFaces++] = Face.createInstance(3);
//			f.mIndices   = new unsigned int[f.mNumIndices = 3];
			
			for (int i = 0; i < 3;++i,mesh.mNumVertices++) {
//				f.mIndices[i] = mesh->mNumVertices;
				f.set(i, mesh.mNumVertices);

//				mesh->mVertices[mesh->mNumVertices] = vertices[ tri.mVertex[i] ];
				MemoryUtil.arraycopy(vertices, tri.mVertex[i] * 3, mesh.mVertices, mesh.mNumVertices * 3, 3);
//				mesh->mTextureCoords[0][mesh->mNumVertices] = aiVector3D( tri.mTex[i][0] / 255.f, 1.f - tri.mTex[i][1] / 255.f, 0.f);
				mesh.mTextureCoords[0].put((tri.mTex[i][0] &0xFF) / 255.f);
				mesh.mTextureCoords[0].put(1.0f - (tri.mTex[i][1] &0xFF) / 255.f);
				mesh.mTextureCoords[0].put(0);
			}
			mesh.mTextureCoords[0].flip();
		}

		// convert to RH
		MakeLeftHandedProcess hero;
		hero.Execute(pScene);

		FlipWindingOrderProcess flipper;
		flipper.Execute(pScene);
		
	}

}
