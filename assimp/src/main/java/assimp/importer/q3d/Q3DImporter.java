package assimp.importer.q3d;

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
import assimp.common.Camera;
import assimp.common.DeadlyImportError;
import assimp.common.DefaultLogger;
import assimp.common.Face;
import assimp.common.ImporterDesc;
import assimp.common.IntPair;
import assimp.common.Light;
import assimp.common.LightSourceType;
import assimp.common.Material;
import assimp.common.MemoryUtil;
import assimp.common.Mesh;
import assimp.common.Node;
import assimp.common.Scene;
import assimp.common.ShadingMode;
import assimp.common.StreamReader;
import assimp.common.Texture;
import assimp.common.TextureType;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;

public class Q3DImporter extends BaseImporter{

	static final ImporterDesc desc = new ImporterDesc(
			"Quick3D Importer",
			"",
			"",
			"http://www.quick3d.com/",
			ImporterDesc.aiImporterFlags_SupportBinaryFlavour,
			0,
			0,
			0,
			0,
			"q3o q3s" 
		);
	
	@Override
	protected boolean canRead(String pFile, InputStream pIOHandler, boolean checkSig) throws IOException {
		String extension = getExtension(pFile);

		if (extension.equals("q3s") || extension.equals("q3o"))
			return true;
		else if (extension.length() == 0 || checkSig)	{
			if (pIOHandler == null)
				return true;
			String tokens[] = {"quick3Do","quick3Ds"};
			return searchFileHeaderForToken(pIOHandler,pFile,tokens);
		}
		return false;
	}

	@Override
	protected ImporterDesc getInfo() {return desc;}

	@Override
	protected void internReadFile(File pFile, Scene pScene) {
		final Vector3f faceNormal = new Vector3f();
		final Vector3f pV1 = new Vector3f();
		final Vector3f pV2 = new Vector3f();
		final Vector3f pV3 = new Vector3f();
		
		try(FileInputStream filein = new FileInputStream(pFile);
			BufferedInputStream in = new BufferedInputStream(filein);
				StreamReader stream = new StreamReader(in, true)){
			// The header is 22 bytes large
			if (stream.getRemainingSize() < 22)
				throw new DeadlyImportError("File is either empty or corrupt: " + pFile);

			// Check the file's signature
//			if (ASSIMP_strincmp( (const char*)stream.GetPtr(), "quick3Do", 8 ) &&
//				ASSIMP_strincmp( (const char*)stream.GetPtr(), "quick3Ds", 8 ))
			ByteBuffer ptr = stream.getPtr();
			if(!AssUtil.equals(ptr, "quick3Do", 0, 8) && AssUtil.equals(ptr, "quick3Ds", 0, 8))
			{
				byte[] bytes = new byte[8];
				ptr.get(bytes);
				throw new DeadlyImportError("Not a Quick3D file. Signature string is: " + 
					/*std::string((const char*)stream.GetPtr(),8)*/ new String(bytes));
			}

			// Print the file format version
			if(DefaultLogger.LOG_OUT){
				byte[] bytes = new byte[2];
				ptr.get(bytes);
				DefaultLogger.info("Quick3D File format version: " + new String(bytes)
					/*std::string(&((const char*)stream.GetPtr())[8],2)*/);
			}

			// ... an store it
//			char major = ((const char*)stream.GetPtr())[8];
//			char minor = ((const char*)stream.GetPtr())[9];
			byte major = ptr.get(8);
			byte minor = ptr.get(9);

			stream.incPtr(10);
			int numMeshes    = stream.getI4();
			int numMats      = stream.getI4();
			int numTextures  = stream.getI4();

//			std::vector<Material> materials;
//			materials.reserve(numMats);
			ArrayList<Q3DMaterial> materials = new ArrayList<>(numMats);

//			std::vector<Mesh> meshes;
//			meshes.reserve(numMeshes);
			ArrayList<Q3DMesh> meshes = new ArrayList<>(numMeshes);

			// Allocate the scene root node
			pScene.mRootNode = new Node();

			Vector3f fgColor = new Vector3f(0.6f,0.6f,0.6f);
			
			// Now read all file chunks
			outer : while (true)
			{
				if (stream.getRemainingSize() < 1)break;
				byte c = stream.getI1();
				switch (c)
				{
					// Meshes chunk
				case 'm':
					{
						for (int quak = 0; quak < numMeshes; ++quak)
						{
							Q3DMesh mesh;
							meshes.add(mesh = new Q3DMesh());
//							Mesh& mesh = meshes.back();

							// read all vertices
							int numVerts = stream.getI4();
							if (numVerts == 0)
								throw new DeadlyImportError("Quick3D: Found mesh with zero vertices");

//							std::vector<aiVector3D>& verts = mesh.verts;
//							verts.resize(numVerts);
							FloatBuffer verts = mesh.verts = MemoryUtil.createFloatBuffer(3 * numVerts, AssimpConfig.LOADING_USE_NATIVE_MEMORY);

							for (int i = 0; i < numVerts;++i)
							{
//								verts[i].x = stream.GetF4();
//								verts[i].y = stream.GetF4();
//								verts[i].z = stream.GetF4();
								verts.put(stream.getF4()).put(stream.getF4()).put(stream.getF4());
							}
							verts.flip();

							// read all faces
							numVerts = stream.getI4();
							if (numVerts == 0)
								throw new DeadlyImportError("Quick3D: Found mesh with zero faces");

//							std::vector<Face >& faces = mesh.faces;
//							faces.reserve(numVerts);
							ArrayList<Q3DFace> faces = mesh.faces = new ArrayList<>(numVerts);

							// number of indices
							for (int i = 0; i < numVerts;++i)
							{
//								Q3DFace face_back;
								faces.add(/*face_back =*/ new Q3DFace(stream.getI2()) );
//								if (face_back.indices.empty())
//									throw DeadlyImportError("Quick3D: Found face with zero indices");
							}

							// indices
							for (int i = 0; i < numVerts;++i)
							{
								Q3DFace vec = faces.get(i);
								int[] ele = vec.indices.elements();
								for (int a = 0; a < vec.indices.size();++a)
//									vec.indices[a] = stream.GetI4();
									ele[a] = stream.getI4();
							}

							// material indices
							for (int i = 0; i < numVerts;++i)
							{
//								faces[i].mat = stream.GetI4();
								faces.get(i).mat = stream.getI4();
							}

							// read all normals
							numVerts = stream.getI4();
//							std::vector<aiVector3D>& normals = mesh.normals;
//							normals.resize(numVerts);
							FloatBuffer normals = mesh.normals = MemoryUtil.createFloatBuffer(3 * numVerts, AssimpConfig.LOADING_USE_NATIVE_MEMORY);

							for (int i = 0; i < numVerts;++i)
							{
//								normals[i].x = stream.GetF4();
//								normals[i].y = stream.GetF4();
//								normals[i].z = stream.GetF4();
								normals.put(stream.getF4()).put(stream.getF4()).put(stream.getF4());
							}
							normals.flip();

							numVerts = stream.getI4();
							if (numTextures != 0 && numVerts!=0)
							{
								// read all texture coordinates
//								std::vector<aiVector3D>& uv = mesh.uv;
//								uv.resize(numVerts);
								FloatBuffer uv = mesh.uv = MemoryUtil.createFloatBuffer(2 * numVerts, AssimpConfig.LOADING_USE_NATIVE_MEMORY);

								for (int i = 0; i < numVerts;++i)
								{
//									uv[i].x = stream.GetF4();
//									uv[i].y = stream.GetF4();
									uv.put(stream.getF4()).put(stream.getF4());
								}
								uv.flip();

								// UV indices
								for (int i = 0; i < faces.size();++i)
								{
									Q3DFace vec = faces.get(i);
									int[] uvindices = vec.uvindices.elements();
									for (int a = 0; a < vec.indices.size();++a)
									{
										uvindices[a] = stream.getI4();
										if (i == 0 && a == 0)
											mesh.prevUVIdx = uvindices[a];
										else if (uvindices[a] != mesh.prevUVIdx)
											mesh.prevUVIdx = -1;
									}
								}
							}

							// we don't need the rest, but we need to get to the next chunk
							stream.incPtr(36);
							if (minor > '0' && major == '3')
								stream.incPtr(mesh.faces.size());
						}
						// stream.IncPtr(4); // unknown value here
					}
					break;

					// materials chunk 
				case 'c':
					ByteArrayList name_bytes = new ByteArrayList();
					for (int i = 0; i < numMats; ++i)
					{
						Q3DMaterial mat;
						materials.add(mat = new Q3DMaterial());
//						Material& mat = materials.back();

						// read the material name
//						while (( c = stream.getI1()))  // TODO
//							mat.name.data[mat.name.length++] = c;
						name_bytes.clear();
						while((c = stream.getI1()) != 0)
							name_bytes.add(c);
						
						mat.name = new String(name_bytes.elements(), 0, name_bytes.size());
						// add the terminal character
//						mat.name.data[mat.name.length] = '\0';

						// read the ambient color
						mat.ambient.x = stream.getF4();
						mat.ambient.y = stream.getF4();
						mat.ambient.z = stream.getF4();

						// read the diffuse color
						mat.diffuse.x = stream.getF4();
						mat.diffuse.y = stream.getF4();
						mat.diffuse.z = stream.getF4();

						// read the ambient color
						mat.specular.x = stream.getF4();
						mat.specular.y = stream.getF4();
						mat.specular.z = stream.getF4();

						// read the transparency
						mat.transparency = stream.getF4();

						// unknown value here
						// stream.IncPtr(4);
						// FIX: it could be the texture index ...
						mat.texIdx = stream.getI4();
					}

					break;

					// texture chunk
				case 't':

//					pScene.mNumTextures = numTextures;
					if (numTextures == 0)break;
					pScene.mTextures    = new Texture[numTextures/*pScene.mNumTextures*/];
					// to make sure we won't crash if we leave through an exception
//					::memset(pScene.mTextures,0,sizeof(void*)*pScene.mNumTextures);
					for (int i = 0; i < /*pScene.mNumTextures*/numTextures; ++i)
					{
						Texture tex = pScene.mTextures[i] = new Texture();

						// skip the texture name
						while (stream.getI1() != 0);

						// read texture width and height
						tex.mWidth  = stream.getI4();
						tex.mHeight = stream.getI4();

						if (tex.mWidth == 0 || tex.mHeight == 0)
							throw new DeadlyImportError("Quick3D: Invalid texture. Width or height is zero");

						int mul = tex.mWidth * tex.mHeight;
//						aiTexel* begin = tex->pcData = new aiTexel[mul];
//						aiTexel* const end = & begin [mul];
						tex.pcData = MemoryUtil.createIntBuffer(mul, AssimpConfig.MESH_USE_NATIVE_MEMORY);

//						for (;begin != end; ++begin)
						while(tex.pcData.remaining() > 0)
						{
//							begin->r = stream.GetI1();
//							begin->g = stream.GetI1();
//							begin->b = stream.GetI1();
//							begin->a = 0xff;
							int r = stream.getI1() & 0xFF;
							int g = stream.getI1() & 0xFF;
							int b = stream.getI1() & 0xFF;
							int a = 0xFF;
							tex.pcData.put(AssUtil.makefourcc(r, g, b, a));
						}
						tex.pcData.flip();
					}

					break;

					// scene chunk
				case 's':
					{
						// skip position and rotation
						stream.incPtr(12);

						for (int i = 0; i < 4;++i)
							for (int a = 0; a < 4;++a)
//								pScene.mRootNode.mTransformation[i][a] = stream.GetF4();
								pScene.mRootNode.mTransformation.set(a, i, stream.getF4());
						
						stream.incPtr(16);

						// now setup a single camera
//						pScene.mNumCameras = 1;
						pScene.mCameras = new Camera[1];
						Camera cam = pScene.mCameras[0] = new Camera();
						cam.mPosition.x = stream.getF4();
						cam.mPosition.y = stream.getF4();
						cam.mPosition.z = stream.getF4();
						cam.mName = ("Q3DCamera");

						// skip eye rotation for the moment
						stream.incPtr(12);

						// read the default material color
						fgColor .x = stream.getF4();
						fgColor .y = stream.getF4();
						fgColor .z = stream.getF4();

						// skip some unimportant properties
						stream.incPtr(29);

						// setup a single point light with no attenuation
//						pScene.mNumLights = 1;
						pScene.mLights = new Light[1];
						Light light = pScene.mLights[0] = new Light();
						light.mName = ("Q3DLight");
						light.mType = LightSourceType.aiLightSource_POINT;

						light.mAttenuationConstant  = 1;
						light.mAttenuationLinear    = 0;
						light.mAttenuationQuadratic = 0;

						light.mColorDiffuse.x = stream.getF4();
						light.mColorDiffuse.y = stream.getF4();
						light.mColorDiffuse.z = stream.getF4();

						light.mColorSpecular.set(light.mColorDiffuse);


						// We don't need the rest, but we need to know where this chunk ends.
						int temp = (stream.getI4() * stream.getI4());

						// skip the background file name
						while (stream.getI1() != '\0');

						// skip background texture data + the remaining fields 
						stream.incPtr(temp*3 + 20); // 4 bytes of unknown data here

						// TODO
						break outer;
					}

				default:
					throw new DeadlyImportError("Quick3D: Unknown chunk");
				};
			}
			
			// If we have no mesh loaded - break here
			if (meshes.isEmpty())
				throw new DeadlyImportError("Quick3D: No meshes loaded");

			// If we have no materials loaded - generate a default mat
			if (materials.isEmpty())
			{
				if(DefaultLogger.LOG_OUT)
					DefaultLogger.info("Quick3D: No material found, generating one");
				Q3DMaterial back = new Q3DMaterial();
				back.diffuse.set(fgColor);
				materials.add(/*Material()*/back);
//				materials.back().diffuse  = fgColor ;
			}

			// find out which materials we'll need
//			typedef std::pair<int, int> FaceIdx;
//			typedef std::vector< FaceIdx > FaceIdxArray;
//			FaceIdxArray* fidx = new FaceIdxArray[materials.size()];
			@SuppressWarnings("unchecked")
			ArrayList<IntPair>[] fidx = new ArrayList[materials.size()];
			AssUtil.initArray(fidx);
			int pScene_mNumMeshes = 0;
			int p = 0;
//			for (std::vector<Mesh>::iterator it = meshes.begin(), end = meshes.end();
//				 it != end; ++it,++p)
			for(int i = 0; i < meshes.size(); i++, ++p)
			{
				Q3DMesh it = meshes.get(i);
				int q = 0;
//				for (std::vector<Face>::iterator fit = (*it).faces.begin(), fend = (*it).faces.end();
//					 fit != fend; ++fit,++q)
				for( int j = 0; j < it.faces.size(); j++, ++q)
				{
					Q3DFace fit = it.faces.get(j);
					if (fit.mat >= materials.size())
					{
						if(DefaultLogger.LOG_OUT)
							DefaultLogger.warn("Quick3D: Material index overflow");
						fit.mat = 0;
					}
					if (fidx[fit.mat].isEmpty())++pScene_mNumMeshes;
					fidx[fit.mat].add(new IntPair(p,q) );
				}
			}
//			pScene.mNumMaterials = pScene.mNumMeshes;
			pScene.mMaterials = new Material[/*pScene.mNumMaterials*/pScene_mNumMeshes];
			pScene.mMeshes = new Mesh[/*pScene.mNumMaterials*/pScene_mNumMeshes];

			for (int i = 0, real = 0; i < materials.size(); ++i)
			{
				if (fidx[i].isEmpty())continue;

				// Allocate a mesh and a material
				Mesh mesh = pScene.mMeshes[real] = new Mesh();
				Material mat = new Material();
				pScene.mMaterials[real] = mat;

				mesh.mMaterialIndex = real;

				// Build the output material
				Q3DMaterial srcMat = materials.get(i);
				mat.addProperty(srcMat.diffuse,  Material.AI_MATKEY_COLOR_DIFFUSE,0,0);
				mat.addProperty(srcMat.specular, Material.AI_MATKEY_COLOR_SPECULAR,0,0);
				mat.addProperty(srcMat.ambient,  Material.AI_MATKEY_COLOR_AMBIENT,0,0);
			
				// NOTE: Ignore transparency for the moment - it seems
				// unclear how to interpret the data
//		#if 0
//				if (!(minor > '0' && major == '3'))
//					srcMat.transparency = 1.0f - srcMat.transparency;
//				mat.addProperty(&srcMat.transparency, 1, AI_MATKEY_OPACITY);
//		#endif

				// add shininess - Quick3D seems to use it ins its viewer
				srcMat.transparency = 16.f;
				mat.addProperty(srcMat.transparency, Material.AI_MATKEY_SHININESS,0,0);

				int m = ShadingMode.aiShadingMode_Phong.ordinal();
				mat.addProperty(m, Material.AI_MATKEY_SHADING_MODEL,0,0);

				if (!AssUtil.isEmpty(srcMat.name))
					mat.addProperty(srcMat.name,Material.AI_MATKEY_NAME,0,0);

				// Add a texture
				if (srcMat.texIdx < /*pScene.mNumTextures*/numTextures || real < /*pScene.mNumTextures*/numTextures)
				{
//					srcMat.name.data[0] = '*';
//					srcMat.name.length  = ASSIMP_itoa10(&srcMat.name.data[1],1000,
//						(srcMat.texIdx < pScene.mNumTextures ? srcMat.texIdx : real));
					srcMat.name = "*" + (srcMat.texIdx < numTextures ? srcMat.texIdx : real);
					mat.addProperty(srcMat.name,/*AI_MATKEY_TEXTURE_DIFFUSE(0)*/Material._AI_MATKEY_TEXTURE_BASE, 
							TextureType.aiTextureType_DIFFUSE.ordinal(), 0);
				}

//				mesh->mNumFaces = fidx[i].size();
				Face[] faces = mesh.mFaces = new Face[/*mesh->mNumFaces*/fidx[i].size()];
				
				// Now build the output mesh. First find out how many
				// vertices we'll need
//				for (FaceIdxArray::const_iterator it = fidx[i].begin(),end = fidx[i].end();
//					 it != end; ++it)
				for (IntPair it : fidx[i])
				{
					mesh.mNumVertices += meshes.get(it.first).faces.get(it.second).indices.size();
				}

//				aiVector3D* verts = mesh->mVertices = new aiVector3D[mesh->mNumVertices];
//				aiVector3D* norms = mesh->mNormals  = new aiVector3D[mesh->mNumVertices];
//				aiVector3D* uv;
				final boolean natived = AssimpConfig.MESH_USE_NATIVE_MEMORY;
				FloatBuffer verts = mesh.mVertices = MemoryUtil.createFloatBuffer(mesh.mNumVertices * 3, natived);
				FloatBuffer norms = mesh.mNormals  = MemoryUtil.createFloatBuffer(mesh.mNumVertices * 3, natived);
				FloatBuffer uv;
				if (real < /*pScene.mNumTextures*/numTextures)
				{
					uv = mesh.mTextureCoords[0] =  MemoryUtil.createFloatBuffer(mesh.mNumVertices * 3, natived);
					mesh.mNumUVComponents[0]    =  2;
				}
				else uv = null;

				// Build the final array
				int cnt = 0;
//				for (FaceIdxArray::const_iterator it = fidx[i].begin(),end = fidx[i].end();
//					 it != end; ++it, ++faces)
				for (int l = 0; l < fidx[i].size(); l++)
				{
					IntPair it = fidx[i].get(l);
					Q3DMesh qm   = meshes.get(it.first);
					Q3DFace face = qm.faces.get((it).second);
//					faces.mNumIndices = face.indices.size();
//					faces.mIndices = new int [faces->mNumIndices];
					Face f = faces[l] = Face.createInstance(face.indices.size());
					
					boolean fnOK = false;

//					for (int n = 0; n < faces->mNumIndices;++n, ++cnt, ++norms, ++verts)
					int[] face_indices = face.indices.elements();
					for (int n = 0; n < f.getNumIndices(); n++, cnt++)
					{
						if (face_indices[n] >= qm.verts.remaining()/3)
						{
							if(DefaultLogger.LOG_OUT)
								DefaultLogger.warn("Quick3D: Vertex index overflow");
							face_indices[n] = 0;
						}

						// copy vertices
//						*verts =  m.verts[ face.indices[n] ];
						int index = face_indices[n] * 3;
						verts.put(qm.verts.get(index++));
						verts.put(qm.verts.get(index++));
						verts.put(qm.verts.get(index++));

						if (face_indices[n] >= qm.normals.remaining()/3 && f.getNumIndices() >= 3)
						{
							// we have no normal here - assign the face normal
							if (!fnOK)
							{
//								const aiVector3D& pV1 =  m.verts[ face.indices[0] ];
//								const aiVector3D& pV2 =  m.verts[ face.indices[1] ];
//								const aiVector3D& pV3 =  m.verts[ face.indices.size() - 1 ];
								index = face_indices[0] * 3;
								pV1.x = qm.verts.get(index++);
								pV1.y = qm.verts.get(index++);
								pV1.z = qm.verts.get(index++);
								pV2.x = qm.verts.get(index++);
								pV2.y = qm.verts.get(index++);
								pV2.z = qm.verts.get(index++);
								
								index = (face.indices.size() - 1) * 3;
								pV3.x = qm.verts.get(index++);
								pV3.y = qm.verts.get(index++);
								pV3.z = qm.verts.get(index++);
								
//								faceNormal = (pV2 - pV1) ^ (pV3 - pV1).Normalize();
								Vector3f.sub(pV2, pV1, pV2);
								Vector3f.sub(pV3, pV1, pV3);
								Vector3f.cross(pV2, pV3, faceNormal);
								faceNormal.normalise();
								fnOK = true;
							}
//							*norms = faceNormal;
							faceNormal.store(norms);
						}
						else {
//							*norms =  m.normals[ face.indices[n] ];
							index = face_indices[n] * 3;
							norms.put(qm.normals.get(index++));
							norms.put(qm.normals.get(index++));
							norms.put(qm.normals.get(index++));
						}

						// copy texture coordinates
						if (uv != null && qm.uv != null)
						{
							if (qm.prevUVIdx != 0xffffffff && qm.uv.remaining()/3 >= qm.verts.remaining()/3) // workaround
							{
//								*uv = m.uv[face.indices[n]];
								index = face_indices[n] * 3;
								uv.put(qm.uv.get(index++));
								uv.put(1.f - qm.uv.get(index++));
								uv.put(qm.uv.get(index++));
							}
							else
							{
								if (face.uvindices.getInt(n) >= qm.uv.remaining()/3)
								{
									if(DefaultLogger.LOG_OUT)
									DefaultLogger.warn("Quick3D: Texture coordinate index overflow");
//									face.uvindices[n] = 0;
									face.uvindices.set(n, 0);
								}
//								*uv = m.uv[face.uvindices[n]];
								index = face.uvindices.getInt(n) * 3;
								uv.put(qm.uv.get(index++));
								uv.put(1.f - qm.uv.get(index++));
								uv.put(qm.uv.get(index++));
							}
//							uv->y = 1.f - uv->y;
//							++uv;
						}

						// setup the new vertex index
//						faces->mIndices[n] = cnt;
						f.set(n, cnt);
					}

				}
				++real;
			}

			// Delete our nice helper array
//			delete[] fidx;
			fidx = null;

			// Now we need to attach the meshes to the root node of the scene
//			pScene.mRootNode->mNumMeshes = pScene.mNumMeshes;
//			pScene.mRootNode->mMeshes = new int [pScene.mNumMeshes];
//			for (int i = 0; i < pScene.mNumMeshes;++i)
//				pScene.mRootNode->mMeshes[i] = i;
			pScene.mRootNode.mMeshes = new int[pScene_mNumMeshes];
			for(int i = 0; i < pScene_mNumMeshes; i++)
				pScene.mRootNode.mMeshes[i] = i;

			/*pScene.mRootNode->mTransformation *= aiMatrix4x4(
				1.f, 0.f, 0.f, 0.f,
			    0.f, -1.f,0.f, 0.f,
				0.f, 0.f, 1.f, 0.f,
				0.f, 0.f, 0.f, 1.f);*/

			// Add cameras and light sources to the scene root node
			int numChildren = /*pScene.mNumLights+pScene.mNumCameras*/ pScene.getNumLights() + pScene.getNumCameras();
			if (numChildren > 0)
			{
				pScene.mRootNode.mChildren = new Node [numChildren ];

				// the light source
				Node nd = pScene.mRootNode.mChildren[0] = new Node();
				nd.mParent = pScene.mRootNode;
				nd.mName = ("Q3DLight");
				nd.mTransformation.load(pScene.mRootNode.mTransformation);
				nd.mTransformation.invert();

				// camera
				nd = pScene.mRootNode.mChildren[1] = new Node();
				nd.mParent = pScene.mRootNode;
				nd.mName = ("Q3DCamera");
				nd.mTransformation.load(pScene.mRootNode.mChildren[0].mTransformation);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
