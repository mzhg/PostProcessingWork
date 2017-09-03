package assimp.importer.nff;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import assimp.common.AssUtil;
import assimp.common.AssimpConfig;
import assimp.common.BaseImporter;
import assimp.common.Camera;
import assimp.common.DeadlyImportError;
import assimp.common.DefaultLogger;
import assimp.common.Face;
import assimp.common.FileUtils;
import assimp.common.ImporterDesc;
import assimp.common.Light;
import assimp.common.LightSourceType;
import assimp.common.Material;
import assimp.common.MemoryUtil;
import assimp.common.Mesh;
import assimp.common.Node;
import assimp.common.Scene;
import assimp.common.ShadingMode;
import assimp.common.StandardShapes;
import assimp.common.TextureMapping;
import assimp.common.TextureType;

public class NFFImporter extends BaseImporter{

	static final ImporterDesc desc = new ImporterDesc(
		"Neutral File Format Importer",
		"",
		"",
		"",
		ImporterDesc.aiImporterFlags_SupportBinaryFlavour,
		0,
		0,
		0,
		0,
		"enff nff"
	);
	
	@Override
	protected boolean canRead(String pFile, InputStream pIOHandler, boolean checkSig) throws IOException {
		return simpleExtensionCheck(pFile,"nff","enff",null);
	}

	@Override
	protected ImporterDesc getInfo() { return desc;}

	@Override
	protected void internReadFile(File pFile, Scene pScene) {
		boolean natived = AssimpConfig.LOADING_USE_NATIVE_MEMORY;
		ByteBuffer buffer = FileUtils.loadText(pFile, true, natived);
		// mesh arrays - separate here to make the handling of the pointers below easier.
		ArrayList<MeshInfo> meshes = new ArrayList<MeshInfo>();
		ArrayList<MeshInfo> meshesWithNormals = new ArrayList<MeshInfo>();
		ArrayList<MeshInfo> meshesWithUVCoords = new ArrayList<MeshInfo>();
		ArrayList<MeshInfo> meshesLocked = new ArrayList<MeshInfo>();
		Vector3f vec3 = new Vector3f();
		
		byte[] cache = new byte[1024];
		String sz;
		
		// camera parameters
		Vector3f camPos = new Vector3f();
		Vector3f camUp = new Vector3f(0.f,1.f,0.f);
		Vector3f camLookAt = new Vector3f(0.f,0.f,1.f);
		float angle = 45.f;
		Vector2f resolution = new Vector2f();

		boolean hasCam = false;

		MeshInfo currentMeshWithNormals = null;
		MeshInfo currentMesh = null;
		MeshInfo currentMeshWithUVCoords = null;

		ShadingInfo s = new ShadingInfo(); // current material info

		// degree of tesselation
		int iTesselation = 4;

		// some temporary variables we need to parse the file
		int sphere				= 0,
			cylinder			= 0,
			cone				= 0,
			numNamed			= 0,
			dodecahedron		= 0,
			octahedron			= 0,
			tetrahedron			= 0,
			hexahedron			= 0;

		// lights imported from the file
		ArrayList<NFFLight> lights = new ArrayList<NFFLight>();
		// check whether this is the NFF2 file format
		if (tokenMatch(buffer,"nff",3))
		{
			final float qnan = Float.NaN;
			final Vector4f cQNAN = new Vector4f(qnan,0.f,0.f,1.f);
			final Vector3f vQNAN = new Vector3f(qnan,0.f,0.f);

			// another NFF file format ... just a raw parser has been implemented
			// no support for further details, I don't think it is worth the effort
			// http://ozviz.wasp.uwa.edu.au/~pbourke/dataformats/nff/nff2.html
			// http://www.netghost.narod.ru/gff/graphics/summary/sense8.htm

			// First of all: remove all comments from the file
//			CommentRemover::RemoveLineComments("//",&mBuffer2[0]);

			while ((sz = AssUtil.getNextLine(buffer,cache, true)) != null)
			{
//				SkipSpaces(line,&sz);
				if (tokenMatch(sz,"version",7))
				{
					if(DefaultLogger.LOG_OUT){
						DefaultLogger.info("NFF (Sense8) file format: " + sz.substring(8));
					}
				}
				else if (tokenMatch(sz,"viewpos",7))
				{
					ai_nff_parse_triple(sz, camPos);
					hasCam = true;
				}
				else if (tokenMatch(sz,"viewdir",7))
				{
					ai_nff_parse_triple(sz, camLookAt);
					hasCam = true;
				}
				// This starts a new object section
				else if (/*!IsSpaceOrNewLine(*sz) TODO*/ sz.length() > 0)
				{
					int subMeshIdx = 0;
					StringTokenizer tokens = new StringTokenizer(sz);
					// read the name of the object, skip all spaces
					// at the end of it.
//					const char* sz3 = sz;
//					while (!IsSpaceOrNewLine(*sz))++sz;
//					std::string objectName = std::string(sz3,(sz-sz3));
					String objectName = tokens.nextToken();
					final int objStart = meshes.size();

					// There could be a material table in a separate file
					ArrayList<ShadingInfo> materialTable = new ArrayList<ShadingInfo>();
					while (true)
					{
//						AI_NFF2_GET_NEXT_TOKEN();
						do{
							if((sz = AssUtil.getNextLine(buffer, cache, true)) == null){
								if(DefaultLogger.LOG_OUT)
									DefaultLogger.warn("NFF2: Unexpected EOF, can't read next token");
								break;
							}
						}while(sz.isEmpty());

						// material table - an external file
						if (tokenMatch(sz,"mtable",6))
						{
//							SkipSpaces(&sz);
//							sz3 = sz;
//							while (!IsSpaceOrNewLine(*sz))++sz;
							tokens = new StringTokenizer(sz);
							tokens.nextToken(); //  skip the word 'mtable'
//							const unsigned int diff = (sz-sz3);
							if (!tokens.hasMoreTokens()) {
								if(DefaultLogger.LOG_OUT)
									DefaultLogger.warn("NFF2: Found empty mtable token");
							}else 
							{
								// The material table has the file extension .mat.
								// If it is not there, we need to append it
								String path = /*std::string(sz3,diff)*/ tokens.nextToken();
//								if(-1 == path.find_last_of(".mat"))
//								{
//									path.append(".mat");
//								}
								if(!path.endsWith(".mat"))
									path += ".mat";

								// Now extract the working directory from the path to
								// this file and append the material library filename 
								// to it.
								int ss;
								if ((-1 == (ss = path.lastIndexOf('\\')) || ss == 0) &&
									(-1 == (ss = path.lastIndexOf('/'))  || ss == 0) )
								{
//									s = pFile.lastIndexOf('\\');
//									if (-1 == s)s = pFile.lastIndexOf('/');
//									if (-1 != s)
//									{
//										path = pFile.substr(0,s+1) + path;
//									}
									
									path = pFile.getParent() + path;
								}
								loadNFF2MaterialTable(materialTable,path);
							}
						}
						else break;
					}

					// read the numbr of vertices
					int num = /*::strtoul10(sz,&sz)*/ AssUtil.parseInt(tokens.nextToken());
					
					// temporary storage
					FloatBuffer  tempColors;
					FloatBuffer tempPositions,tempTextureCoords,tempNormals;

					boolean hasNormals = false,hasUVs = false,hasColor = false;
//					tempPositions.reserve      (num);
//					tempColors.reserve         (num);
//					tempNormals.reserve        (num);
//					tempTextureCoords.reserve  (num);
					tempPositions 	  = MemoryUtil.createFloatBuffer(num * 3, natived);
					tempColors    	  = MemoryUtil.createFloatBuffer(num * 4, natived);
					tempNormals  	  = MemoryUtil.createFloatBuffer(num * 3, natived);
					tempTextureCoords = MemoryUtil.createFloatBuffer(num * 3, natived);
					
					for (int i = 0; i < num; ++i)
					{
//						AI_NFF2_GET_NEXT_TOKEN();
						do{
							if((sz = AssUtil.getNextLine(buffer, cache, true)) == null){
								if(DefaultLogger.LOG_OUT)
									DefaultLogger.warn("NFF2: Unexpected EOF, can't read next token");
								break;
							}
						}while(sz.isEmpty());
						
//						aiVector3D v;
//						ai_nff_parse_triple(sz,v);
//						tempPositions.push_back(v);
//						v.store(tempPositions);
						tokens = new StringTokenizer(sz);
						tempPositions.put(AssUtil.parseFloat(tokens.nextToken()));
						tempPositions.put(AssUtil.parseFloat(tokens.nextToken()));
						tempPositions.put(AssUtil.parseFloat(tokens.nextToken()));

						// parse all other attributes in the line
						while (true)
						{
//							SkipSpaces(&sz);
//							if (IsLineEnd(*sz))break;
							
							if(!tokens.hasMoreTokens())
								break;
							
							String next = tokens.nextToken();

							// color definition
//							if (tokenMatch(sz,"0x",2))
							if(next.startsWith("0x"))
							{
								hasColor = true;
//								register unsigned int numIdx = ::strtoul16(sz,&sz);
								int numIdx = Integer.parseInt(next.substring(2), 16);
//								aiColor4D clr;
								float a = 1.f;

								// 0xRRGGBB
								float r = ((numIdx >> 16) & 0xff) / 255.f;
								float g = ((numIdx >> 8)  & 0xff) / 255.f;
								float b = ((numIdx)       & 0xff) / 255.f;
//								tempColors.push_back(clr);
								tempColors.put(r).put(g).put(b).put(a);
							}
							// normal vector
							else if (tokenMatch(next,"norm",4))
							{
								hasNormals = true;
//								AI_NFF_PARSE_TRIPLE(v);
//								tempNormals.push_back(v);
								tempNormals.put(AssUtil.parseFloat(tokens.nextToken()));  // x
								tempNormals.put(AssUtil.parseFloat(tokens.nextToken()));  // y
								tempNormals.put(AssUtil.parseFloat(tokens.nextToken()));  // z
							}
							// UV coordinate
							else if (tokenMatch(next,"uv",2))
							{
								hasUVs = true;
//								AI_NFF_PARSE_FLOAT(v.x);
//								AI_NFF_PARSE_FLOAT(v.y);
//								v.z = 0.f;
//								tempTextureCoords.push_back(v);
								tempTextureCoords.put(AssUtil.parseFloat(tokens.nextToken()));  // x
								tempTextureCoords.put(AssUtil.parseFloat(tokens.nextToken()));  // y
								tempTextureCoords.put(0);  // z
							}
						}
						
						// fill in dummies for all attributes that have not been set
						if (tempNormals.position() != tempPositions.position())
//							tempNormals.push_back(vQNAN);
							vQNAN.store(tempNormals);

						if (tempTextureCoords.position() != tempPositions.position())
//							tempTextureCoords.push_back(vQNAN);
							vQNAN.store(tempTextureCoords);

						if (tempColors.position()/4 != tempPositions.position()/3)
//							tempColors.push_back(cQNAN);
							cQNAN.store(tempColors);
					}
					
					tempPositions.flip();
					tempNormals.flip();
					tempTextureCoords.flip();
					tempColors.flip();

//					AI_NFF2_GET_NEXT_TOKEN();
					do{
						if((sz = AssUtil.getNextLine(buffer, cache, true)) == null){
							if(DefaultLogger.LOG_OUT)
								DefaultLogger.warn("NFF2: Unexpected EOF, can't read next token");
							break;
						}
					}while(sz.isEmpty());
					
					if (num == 0 )throw new DeadlyImportError("NFF2: There are zero vertices");
//					num = ::strtoul10(sz,&sz);
					num = AssUtil.parseInt(sz);

//					std::vector<unsigned int> tempIdx;
//					tempIdx.reserve(10);
					IntArrayList tempIdx = new IntArrayList();
					for (int i = 0; i < num; ++i)
					{
//						AI_NFF2_GET_NEXT_TOKEN();
						do{
							if((sz = AssUtil.getNextLine(buffer, cache, true)) == null){
								if(DefaultLogger.LOG_OUT)
									DefaultLogger.warn("NFF2: Unexpected EOF, can't read next token");
								break;
							}
						}while(sz.isEmpty());
//						SkipSpaces(line,&sz);
//						unsigned int numIdx = ::strtoul10(sz,&sz);
						tokens = new StringTokenizer(sz, " ");
						int numIdx = AssUtil.parseInt(tokens.nextToken());
						// read all faces indices
						if (numIdx > 0)
						{
							// mesh.faces.push_back(numIdx);
							// tempIdx.erase(tempIdx.begin(),tempIdx.end());
							tempIdx.size(numIdx);

							for (int a = 0; a < numIdx;++a)
							{
//								SkipSpaces(sz,&sz);
//								m = ::strtoul10(sz,&sz);
								int m = AssUtil.parseInt(tokens.nextToken());
								if (m >= tempPositions.remaining()/3)
								{
									DefaultLogger.error("NFF2: Vertex index overflow");
									m= 0;
								}
								// mesh.vertices.push_back (tempPositions[idx]);
//								tempIdx[a] = m;
								tempIdx.set(a, m);
							}
						}

						// build a temporary shader object for the face. 
						ShadingInfo shader = new ShadingInfo();
						int matIdx = 0;

						// white material color - we have vertex colors
						shader.color.set(1.f,1.f,1.f); 
						Vector4f c  = new Vector4f(1.f,1.f,1.f,1.f);
						while (true)
						{
//							SkipSpaces(sz,&sz);
//							if(IsLineEnd(*sz))break;
							if(!tokens.hasMoreTokens()) break;
							
							String next = tokens.nextToken();
							// per-polygon colors
//							if (tokenMatch(sz,"0x",2))
							if(next.startsWith("0x"))
							{
								hasColor = true;
//								const char* sz2 = sz;
//								numIdx = ::strtoul16(sz,&sz);
//								const unsigned int diff = (sz-sz2);
								String value16 = next.substring(2);
								numIdx = Integer.parseInt(value16, 16);
								final int diff = value16.length();
								// 0xRRGGBB
								if (diff > 3)
								{
									c.x = ((numIdx >> 16) & 0xff) / 255.f;
									c.y = ((numIdx >> 8)  & 0xff) / 255.f;
									c.z = ((numIdx)       & 0xff) / 255.f;
								}
								// 0xRGB
								else
								{
									c.x = ((numIdx >> 8) & 0xf) / 16.f;
									c.y = ((numIdx >> 4) & 0xf) / 16.f;
									c.z = ((numIdx)      & 0xf) / 16.f;
								}
							}
							// TODO - implement texture mapping here
//	#if 0
//							// mirror vertex texture coordinate?
//							else if (TokenMatch(sz,"mirror",6))
//							{
//							}
//							// texture coordinate scaling
//							else if (TokenMatch(sz,"scale",5))
//							{
//							}
//							// texture coordinate translation
//							else if (TokenMatch(sz,"trans",5))
//							{
//							}
//							// texture coordinate rotation angle
//							else if (TokenMatch(sz,"rot",3))
//							{
//							}
//	#endif

							// texture file name for this polygon + mapping information
							else if ('_' == next.charAt(0))
							{
								// get mapping information
								switch (next.charAt(1))
								{
								case 'v':
								case 'V':

									shader.shaded = false;
									break;

								case 't':
								case 'T':
								case 'u':
								case 'U':
									if(DefaultLogger.LOG_OUT)
										DefaultLogger.warn("Unsupported NFF2 texture attribute: trans");
								};
//								if (!sz[1] || '_' != sz[2])
								if (next.charAt(1) == 0 || '_' != next.charAt(2))
								{
									if(DefaultLogger.LOG_OUT)
										DefaultLogger.warn("NFF2: Expected underscore after texture attributes");
									continue;
								}
//								const char* sz2 = sz+3;
//								while (!IsSpaceOrNewLine( *sz ))++sz;
//								const unsigned int diff = (sz-sz2);
//								if (diff)shader.texFile = std::string(sz2,diff);
								shader.texFile = next.substring(3);
							}

							// Two-sided material?
							else if (tokenMatch(next,"both",4))
							{
								shader.twoSided = true;
							}

							// Material ID?
							else if (!materialTable.isEmpty() && tokenMatch(next,"matid",5))
							{
//								SkipSpaces(&sz);
//								matIdx = ::strtoul10(sz,&sz);
								matIdx = AssUtil.parseInt(tokens.nextToken());
								if (matIdx >= materialTable.size())
								{
									DefaultLogger.error("NFF2: Material index overflow.");
									matIdx = 0;
								}

								// now combine our current shader with the shader we
								// read from the material table.
								ShadingInfo mat = materialTable.get(matIdx);
//								shader.ambient   = mat.ambient;
//								shader.diffuse   = mat.diffuse;
//								shader.emissive  = mat.emissive;
//								shader.opacity   = mat.opacity;
//								shader.specular  = mat.specular;
//								shader.shininess = mat.shininess;
								shader.set(mat);
							}
							else{
//								SkipToken(sz);
							}
						}

						// search the list of all shaders we have for this object whether
						// there is an identical one. In this case, we append our mesh
						// data to it.
						MeshInfo mesh = null;
//						for (std::vector<MeshInfo>::iterator it = meshes.begin() + objStart, end = meshes.end();
//							 it != end; ++it)
						for(int k = objStart; k < meshes.size(); k++)
						{
							MeshInfo it = meshes.get(k);
							if (it.shader.equals(shader) && it.matIndex == matIdx)
							{
								// we have one, we can append our data to it
//								mesh = &(*it);
								mesh = it;
							}
						}
						if (mesh == null)
						{
							meshes.add(mesh = new MeshInfo((byte)MeshInfo.PatchType_Simple,false));
//							mesh = &meshes.back();
							mesh.matIndex = matIdx;

							// We need to add a new mesh to the list. We assign
							// an unique name to it to make sure the scene will
							// pass the validation step for the moment.
							// TODO: fix naming of objects in the scenegraph later
							if (objectName.length() > 0)
							{
//								::strcpy(mesh->name,objectName.c_str()); 
//								ASSIMP_itoa10(&mesh->name[objectName.length()],30,subMeshIdx++);
								objectName.getBytes(0, objectName.length(), mesh.name, 0);
								AssUtil.assimp_itoa10(mesh.name, objectName.length(), 30, subMeshIdx++);
							}

							// copy the shader to the mesh. 
							mesh.shader = shader;
						}

						// fill the mesh with data
						if (!tempIdx.isEmpty())
						{
							mesh.faces.add(tempIdx.size());
//							for (std::vector<unsigned int>::const_iterator it = tempIdx.begin(), end = tempIdx.end();
//								it != end;++it)
							for (int k = 0; k < tempIdx.size(); k++)
							{
								int m = tempIdx.getInt(k);

								// copy colors -vertex color specifications override polygon color specifications
								if (hasColor)
								{
									if(mesh.colors == null)
										mesh.colors = MemoryUtil.createFloatBuffer(tempColors.remaining(), natived);
									
//									const aiColor4D& clr = tempColors[m];
//									mesh->colors.push_back((is_qnan( clr.r ) ? c : clr));
									int index = 4 * m;
									float r = tempColors.get(index++);
									float g = tempColors.get(index++);
									float b = tempColors.get(index++);
									float a = tempColors.get(index++);
									if(r == r){
										mesh.colors.put(r).put(g).put(b).put(a);
									}else{
										c.store(mesh.colors);
									}
								}
								if(mesh.vertices == null)
									mesh.vertices = MemoryUtil.createFloatBuffer(tempPositions.remaining(), natived);
								
								// positions should always be there
//								mesh.vertices.push_back (tempPositions[m]);
								int index = 3 * m;
								mesh.vertices.put(tempPositions.get(index++));
								mesh.vertices.put(tempPositions.get(index++));
								mesh.vertices.put(tempPositions.get(index++));

								// copy normal vectors
								if (hasNormals){
									if(mesh.normals == null)
										mesh.normals = MemoryUtil.createFloatBuffer(tempPositions.remaining(), natived);
//									mesh->normals.push_back  (tempNormals[m]);
									index = 3 * m;
									mesh.normals.put(tempNormals.get(index++));
									mesh.normals.put(tempNormals.get(index++));
									mesh.normals.put(tempNormals.get(index++));
								}

								// copy texture coordinates
								if (hasUVs){
									if(mesh.uvs == null)
										mesh.uvs = MemoryUtil.createFloatBuffer(tempTextureCoords.remaining(), natived);
//									mesh.uvs.push_back      (tempTextureCoords[m]);
									index = 3 * m;
									mesh.uvs.put(tempTextureCoords.get(index++));
									mesh.uvs.put(tempTextureCoords.get(index++));
									mesh.uvs.put(tempTextureCoords.get(index++));
								}
							}
						}
					}
					if (num == 0)throw new DeadlyImportError("NFF2: There are zero faces");
				}
			}
//			camLookAt = camLookAt + camPos;
			Vector3f.add(camLookAt, camPos, camLookAt);
		}
		else // "Normal" Neutral file format that is quite more common
		{
//			while (GetNextLine(buffer,line))
			while((sz = AssUtil.getNextLine(buffer, cache, true)) != null)
			{
//				sz = line;
				if ('p' == sz.charAt(0) || tokenMatch(sz,"tpp",3))
				{
					MeshInfo out = null;

					// 'tpp' - texture polygon patch primitive
					if ('t' == /*line[0]*/ sz.charAt(0))
					{
						currentMeshWithUVCoords = null;
//						for (std::vector<MeshInfo>::iterator it = meshesWithUVCoords.begin(), end = meshesWithUVCoords.end();
//							it != end;++it)
						for (MeshInfo it : meshesWithUVCoords)
						{
							if (it.shader.equals(s))
							{
								currentMeshWithUVCoords = /*&(*it)*/ it;
								break;
							}
						}

						if (currentMeshWithUVCoords == null)
						{
							meshesWithUVCoords.add(currentMeshWithUVCoords = new MeshInfo((byte)MeshInfo.PatchType_UVAndNormals, false));
//							currentMeshWithUVCoords = &meshesWithUVCoords.back();
							currentMeshWithUVCoords.shader = s; // TODO  reference copy
						}
						out = currentMeshWithUVCoords;
						sz = sz.substring(3); // skip the 'tpp'
					}
					// 'pp' - polygon patch primitive
					else if ('p' == /*line[1]*/ sz.charAt(1))
					{
						currentMeshWithNormals = null;
//						for (std::vector<MeshInfo>::iterator it = meshesWithNormals.begin(), end = meshesWithNormals.end();
//							it != end;++it)
						for (MeshInfo it : meshesWithNormals)
						{
							if (it.shader.equals(s))
							{
								currentMeshWithNormals = /*&(*it)*/ it;
								break;
							}
						}

						if (currentMeshWithNormals == null)
						{
							meshesWithNormals.add(currentMeshWithNormals = new MeshInfo((byte)MeshInfo.PatchType_Normals, false));
//							currentMeshWithNormals = &meshesWithNormals.back();
							currentMeshWithNormals.shader = s;  // TODO reference copy
						}
//						sz = &line[2];
						sz = sz.substring(2);
						out = currentMeshWithNormals;
					}
					// 'p' - polygon primitive
					else
					{
						currentMesh = null;
//						for (std::vector<MeshInfo>::iterator it = meshes.begin(), end = meshes.end();it != end;++it)
						for (MeshInfo it : meshes)
						{
							if (it.shader.equals(s))
							{
								currentMesh = /*&(*it)*/ it;
								break;
							}
						}

						if (currentMesh == null)
						{
							meshes.add(currentMesh = new MeshInfo((byte)MeshInfo.PatchType_Simple, false));
//							currentMesh = &meshes.back();
							currentMesh.shader = s;  // TODO reference copy
						}
//						sz = &line[1];
						out = currentMesh;
					}
//					SkipSpaces(sz,&sz);
//					m = strtoul10(sz);
					StringTokenizer tokens = new StringTokenizer(sz);
					int m = AssUtil.parseInt(tokens.nextToken());

					// ---- flip the face order
//					out.vertices.resize(out.vertices.size()+m);
					out.vertices = MemoryUtil.enlarge(out.vertices, out.vertices.limit() + m *3);
					if (out != currentMesh)
					{
//						out.normals.resize(out.vertices.size());
						out.normals = MemoryUtil.enlarge(out.normals, out.vertices.limit());
					}
					if (out == currentMeshWithUVCoords)
					{
//						out.uvs.resize(out.vertices.size());
						out.uvs = MemoryUtil.enlarge(out.uvs, out.vertices.limit());
					}
					for (int n = 0; n < m;++n)
					{
//						if(!GetNextLine(buffer,line))
						if((sz = AssUtil.getNextLine(buffer, cache, true)) == null)
						{
							DefaultLogger.error("NFF: Unexpected EOF was encountered. Patch definition incomplete");
							continue;
						}

//						aiVector3D v; sz = &line[0];
//						AI_NFF_PARSE_TRIPLE(v);
//						out.vertices[out.vertices.size()-n-1] = v;
						tokens = new StringTokenizer(sz, " ");
						float x = AssUtil.parseFloat(tokens.nextToken());
						float y = AssUtil.parseFloat(tokens.nextToken());
						float z = AssUtil.parseFloat(tokens.nextToken());
						int index = out.vertices.limit() - 3 * (n + 1);
						out.vertices.put(index++, x);
						out.vertices.put(index++, y);
						out.vertices.put(index++, z);

						if (out != currentMesh)
						{
//							AI_NFF_PARSE_TRIPLE(v);
//							out.normals[out.vertices.size()-n-1] = v;
							x = AssUtil.parseFloat(tokens.nextToken());
							y = AssUtil.parseFloat(tokens.nextToken());
							z = AssUtil.parseFloat(tokens.nextToken());
							index = out.vertices.limit() - 3 * (n + 1);
							out.normals.put(index++, x);
							out.normals.put(index++, y);
							out.normals.put(index++, z);
						}
						if (out == currentMeshWithUVCoords)
						{
							// FIX: in one test file this wraps over multiple lines
//							SkipSpaces(&sz);
//							if (IsLineEnd(*sz))
//							{
//								GetNextLine(buffer,line);
//								sz = line;
//							}
							if(!tokens.hasMoreElements()){
								sz = AssUtil.getNextLine(buffer, cache, true);
								tokens = new StringTokenizer(sz);
							}
//							AI_NFF_PARSE_FLOAT(v.x);
							x = AssUtil.parseFloat(tokens.nextToken());
//							SkipSpaces(&sz);
//							if (IsLineEnd(*sz))
//							{
//								GetNextLine(buffer,line);
//								sz = line;
//							}
							if(!tokens.hasMoreElements()){
								sz = AssUtil.getNextLine(buffer, cache, true);
								tokens = new StringTokenizer(sz);
							}
//							AI_NFF_PARSE_FLOAT(v.y);
//							v.y = 1.f - v.y;
							y = 1.f - AssUtil.parseFloat(tokens.nextToken());
//							out.uvs[out.vertices.size()-n-1] = v;
							index = out.vertices.limit() - 3 * (n + 1);
							out.uvs.put(index++, x);
							out.uvs.put(index++, y);
						}
					}
					out.faces.add(m);
				}
				// 'f' - shading information block
				else if (tokenMatch(sz,"f",1))
				{
					// read the RGB colors
//					AI_NFF_PARSE_TRIPLE(s.color);

					// read the other properties
//					AI_NFF_PARSE_FLOAT(s.diffuse.r);
//					AI_NFF_PARSE_FLOAT(s.specular.r);
//					AI_NFF_PARSE_FLOAT(d); // skip shininess and transmittance
//					AI_NFF_PARSE_FLOAT(d);
//					AI_NFF_PARSE_FLOAT(s.refracti);
					StringTokenizer tokens = new StringTokenizer(sz.substring(1));  // skip the letter 'f' 
					s.color.x = AssUtil.parseFloat(tokens.nextToken());
					s.color.y = AssUtil.parseFloat(tokens.nextToken());
					s.color.z = AssUtil.parseFloat(tokens.nextToken());
					
					s.diffuse.x = AssUtil.parseFloat(tokens.nextToken());
					s.specular.x = AssUtil.parseFloat(tokens.nextToken());
					tokens.nextToken(); // skip shininess and transmittance
					tokens.nextToken(); 
					s.refracti = AssUtil.parseFloat(tokens.nextToken());

					// NFF2 uses full colors here so we need to use them too
					// although NFF uses simple scaling factors
					s.diffuse.y  = s.diffuse.z = s.diffuse.x;
					s.specular.y = s.specular.z = s.specular.x;

					// if the next one is NOT a number we assume it is a texture file name
					// this feature is used by some NFF files on the internet and it has
					// been implemented as it can be really useful
					String token = tokens.nextToken();
//					SkipSpaces(&sz);
					if (!AssUtil.isNumeric(token))
					{
						// TODO: Support full file names with spaces and quotation marks ...
//						const char* p = sz;
//						while (!IsSpaceOrNewLine( *sz ))++sz;
//
//						unsigned int diff = (sz-p);
//						if (diff)
//						{
//							s.texFile = std::string(p,diff);
//						}
						s.texFile = token;
					}
					else
					{
//						AI_NFF_PARSE_FLOAT(s.ambient); // optional
						s.ambient.x = AssUtil.parseFloat(tokens.nextToken());
					}
				}
				// 'shader' - other way to specify a texture
				else if (tokenMatch(sz,"shader",6))
				{
//					SkipSpaces(&sz);
//					const char* old = sz;
//					while (!IsSpaceOrNewLine(*sz))++sz;
//					s.texFile = std::string(old, (uintptr_t)sz - (uintptr_t)old);
					s.texFile = sz.substring(7).trim();
				}
				// 'l' - light source
				else if (tokenMatch(sz,"l",1))
				{
					NFFLight light;
					lights.add(light = new NFFLight());
//					Light& light = lights.back();

//					AI_NFF_PARSE_TRIPLE(light.position);
//					AI_NFF_PARSE_FLOAT (light.intensity);
//					AI_NFF_PARSE_TRIPLE(light.color);
					StringTokenizer tokens = new StringTokenizer(sz);
					light.position.x = AssUtil.parseFloat(tokens.nextToken());
					light.position.y = AssUtil.parseFloat(tokens.nextToken());
					light.position.z = AssUtil.parseFloat(tokens.nextToken());
					light.intensity  = AssUtil.parseFloat(tokens.nextToken());
					light.color.x    = AssUtil.parseFloat(tokens.nextToken());
					light.color.y    = AssUtil.parseFloat(tokens.nextToken());
					light.color.z    = AssUtil.parseFloat(tokens.nextToken());
				}
				// 's' - sphere
				else if (tokenMatch(sz,"s",1))
				{
					MeshInfo curr;
					meshesLocked.add(curr = new MeshInfo((byte)MeshInfo.PatchType_Simple,true));
//					MeshInfo& currentMesh = meshesLocked.back();
					curr.shader = s; // TODO reference copy
					curr.shader.mapping = TextureMapping.aiTextureMapping_SPHERE;

					ai_nff_parse_shape_information(sz.substring(1), curr);

					// we don't need scaling or translation here - we do it in the node's transform
//					StandardShapes::MakeSphere(iTesselation, currentMesh.vertices);
					List<Vector3f> positions = new ArrayList<Vector3f>();
					StandardShapes.makeSphere(iTesselation, positions);
					curr.vertices = MemoryUtil.createFloatBuffer(positions.size() * 3, natived);
					for(Vector3f v : positions)
						v.store(curr.vertices);
					curr.vertices.flip();
					curr.faces.size(positions.size()/*currentMesh.vertices.size()*//3);
					Arrays.fill(curr.faces.elements(), 0, curr.faces.size(), 3);

					// generate a name for the mesh
//					::sprintf(currentMesh.name,"sphere_%i",sphere++);
					String sss = String.format("sphere_%d", sphere++);
					sss.getBytes(0, sss.length(), curr.name, 0);
				}
				// 'dod' - dodecahedron
				else if (tokenMatch(sz,"dod",3))
				{
					MeshInfo curr;
					meshesLocked.add(curr = new MeshInfo((byte)MeshInfo.PatchType_Simple,true));
//					MeshInfo& currentMesh = meshesLocked.back();
					curr.shader = s;// TODO reference copy
					curr.shader.mapping = TextureMapping.aiTextureMapping_SPHERE;

					ai_nff_parse_shape_information(sz.substring(3), curr);

					// we don't need scaling or translation here - we do it in the node's transform
//					StandardShapes::MakeDodecahedron(currentMesh.vertices);
//					currentMesh.faces.resize(currentMesh.vertices.size()/3,3);
					List<Vector3f> positions = new ArrayList<Vector3f>();
					StandardShapes.makeDodecahedron(positions, false);
					curr.vertices = MemoryUtil.createFloatBuffer(positions.size() * 3, natived);
					for(Vector3f v : positions)
						v.store(curr.vertices);
					curr.vertices.flip();
					Arrays.fill(curr.faces.elements(), 0, curr.faces.size(), 3);
					// generate a name for the mesh
//					::sprintf(currentMesh.name,"dodecahedron_%i",dodecahedron++);
					String sss = String.format("dodecahedron_%d", dodecahedron++);
					sss.getBytes(0, sss.length(), curr.name, 0);
				}

				// 'oct' - octahedron
				else if (tokenMatch(sz,"oct",3))
				{
					MeshInfo curr;
					meshesLocked.add(curr = new MeshInfo((byte)MeshInfo.PatchType_Simple,true));
//					MeshInfo& currentMesh = meshesLocked.back();
					curr.shader = s;  // TODO reference copy
					curr.shader.mapping = TextureMapping.aiTextureMapping_SPHERE;

					ai_nff_parse_shape_information(sz.substring(3), curr);

					// we don't need scaling or translation here - we do it in the node's transform
//					StandardShapes::MakeOctahedron(currentMesh.vertices);
//					currentMesh.faces.resize(currentMesh.vertices.size()/3,3);
					List<Vector3f> positions = new ArrayList<Vector3f>();
					StandardShapes.makeOctahedron(positions);
					curr.vertices = MemoryUtil.createFloatBuffer(positions.size() * 3, natived);
					for(Vector3f v : positions)
						v.store(curr.vertices);
					curr.vertices.flip();
					Arrays.fill(curr.faces.elements(), 0, curr.faces.size(), 3);

					// generate a name for the mesh
//					::sprintf(currentMesh.name,"octahedron_%i",octahedron++);
					String sss = String.format("octahedron_%d", dodecahedron++);
					sss.getBytes(0, sss.length(), curr.name, 0);
				}

				// 'tet' - tetrahedron
				else if (tokenMatch(sz,"tet",3))
				{
					MeshInfo curr;
					meshesLocked.add(curr = new MeshInfo((byte)MeshInfo.PatchType_Simple,true));
					currentMesh.shader = s; // TODO reference copy
					currentMesh.shader.mapping = TextureMapping.aiTextureMapping_SPHERE;

					ai_nff_parse_shape_information(sz.substring(3), curr);

					// we don't need scaling or translation here - we do it in the node's transform
//					StandardShapes::MakeTetrahedron(currentMesh.vertices);
//					currentMesh.faces.resize(currentMesh.vertices.size()/3,3);
					List<Vector3f> positions = new ArrayList<Vector3f>();
					StandardShapes.makeTetrahedron(positions);
					curr.vertices = MemoryUtil.createFloatBuffer(positions.size() * 3, natived);
					for(Vector3f v : positions)
						v.store(curr.vertices);
					curr.vertices.flip();
					Arrays.fill(curr.faces.elements(), 0, curr.faces.size(), 3);

					// generate a name for the mesh
//					::sprintf(currentMesh.name,"tetrahedron_%i",tetrahedron++);
					String sss = String.format("tetrahedron_%d", dodecahedron++);
					sss.getBytes(0, sss.length(), curr.name, 0);
				}

				// 'hex' - hexahedron
				else if (tokenMatch(sz,"hex",3))
				{
					MeshInfo curr;
					meshesLocked.add(curr = new MeshInfo((byte)MeshInfo.PatchType_Simple,true));
					currentMesh.shader = s; // TODO reference copy
					currentMesh.shader.mapping = TextureMapping.aiTextureMapping_BOX;

					ai_nff_parse_shape_information(sz.substring(3), curr);

					// we don't need scaling or translation here - we do it in the node's transform
//					StandardShapes::MakeHexahedron(currentMesh.vertices);
//					currentMesh.faces.resize(currentMesh.vertices.size()/3,3);
					List<Vector3f> positions = new ArrayList<Vector3f>();
					StandardShapes.makeHexahedron(positions, false);
					curr.vertices = MemoryUtil.createFloatBuffer(positions.size() * 3, natived);
					for(Vector3f v : positions)
						v.store(curr.vertices);
					curr.vertices.flip();
					Arrays.fill(curr.faces.elements(), 0, curr.faces.size(), 3);

					// generate a name for the mesh
//					::sprintf(currentMesh.name,"hexahedron_%i",hexahedron++);
					String sss = String.format("hexahedron_%d", dodecahedron++);
					sss.getBytes(0, sss.length(), curr.name, 0);
				}
				// 'c' - cone
				else if (tokenMatch(sz,"c",1))
				{
					MeshInfo curr;
					meshesLocked.add(curr = new MeshInfo((byte)MeshInfo.PatchType_Simple,true));
					curr.shader = s; // TODO reference copy
					curr.shader.mapping = TextureMapping.aiTextureMapping_CYLINDER;


//					if(!GetNextLine(buffer,line))
					if((sz = AssUtil.getNextLine(buffer, cache, true)) == null)
					{
						DefaultLogger.error("NFF: Unexpected end of file (cone definition not complete)");
						break;
					}
//					sz = line;

					// read the two center points and the respective radii
					Vector3f center1 = new Vector3f(), center2 = new Vector3f(); float radius1, radius2;
//					AI_NFF_PARSE_TRIPLE(center1);
//					AI_NFF_PARSE_FLOAT(radius1);
					StringTokenizer tokens = new StringTokenizer(sz);
					center1.x = AssUtil.parseFloat(tokens.nextToken());
					center1.y = AssUtil.parseFloat(tokens.nextToken());
					center1.z = AssUtil.parseFloat(tokens.nextToken());
					radius1   = AssUtil.parseFloat(tokens.nextToken());

//					if(!GetNextLine(buffer,line))
					if((sz = AssUtil.getNextLine(buffer, cache, true)) == null)
					{
						DefaultLogger.error("NFF: Unexpected end of file (cone definition not complete)");
						break;
					}
//					sz = line;

//					AI_NFF_PARSE_TRIPLE(center2);
//					AI_NFF_PARSE_FLOAT(radius2);
					tokens = new StringTokenizer(sz);
					center2.x = AssUtil.parseFloat(tokens.nextToken());
					center2.y = AssUtil.parseFloat(tokens.nextToken());
					center2.z = AssUtil.parseFloat(tokens.nextToken());
					radius2   = AssUtil.parseFloat(tokens.nextToken());

					// compute the center point of the cone/cylinder -
					// it is its local transformation origin
//					currentMesh.dir    =  center2-center1;
//					currentMesh.center =  center1+currentMesh.dir/2.f;
					Vector3f.sub(center2, center1, curr.dir);
					Vector3f.linear(center1, curr.dir, 0.5f, curr.center);

					float f;
					if (( f = curr.dir.length()) < 10e-3f )
					{
						DefaultLogger.error("NFF: Cone height is close to zero");
						continue;
					}
//					currentMesh.dir /= f; // normalize
					curr.dir.scale(1.f/f); // normalize

					// generate the cone - it consists of simple triangles
//					StandardShapes::MakeCone(f, radius1, radius2,
//						integer_pow(4, iTesselation), curr.vertices);

					// MakeCone() returns tris
//					curr.faces.resize(curr.vertices.size()/3,3);
					List<Vector3f> positions = new ArrayList<Vector3f>();
					StandardShapes.makeCone(f, radius1, radius2, (int)Math.pow(4, iTesselation), positions, false);
					curr.vertices = MemoryUtil.createFloatBuffer(positions.size() * 3, natived);
					for(Vector3f v : positions)
						v.store(curr.vertices);
					curr.vertices.flip();
					Arrays.fill(curr.faces.elements(), 0, curr.faces.size(), 3);

					// generate a name for the mesh. 'cone' if it a cone,
					// 'cylinder' if it is a cylinder. Funny, isn't it?
					if (radius1 != radius2){
//						::sprintf(curr.name,"cone_%i",cone++);
						String sss = String.format("cone_%d", dodecahedron++);
						sss.getBytes(0, sss.length(), curr.name, 0);
					}
					else {
//						::sprintf(curr.name,"cylinder_%i",cylinder++);
						String sss = String.format("cylinder_%d", dodecahedron++);
						sss.getBytes(0, sss.length(), curr.name, 0);
					}
				}
				// 'tess' - tesselation
				else if (tokenMatch(sz,"tess",4))
				{
//					SkipSpaces(&sz);
//					iTesselation = strtoul10(sz);
					iTesselation = AssUtil.parseInt(sz.substring(5).trim());
				}
				// 'from' - camera position
				else if (tokenMatch(sz,"from",4))
				{
					ai_nff_parse_triple(sz, camPos);
					hasCam = true;
				}
				// 'at' - camera look-at vector
				else if (tokenMatch(sz,"at",2))
				{
					ai_nff_parse_triple(sz, camLookAt);
					hasCam = true;
				}
				// 'up' - camera up vector
				else if (tokenMatch(sz,"up",2))
				{
					ai_nff_parse_triple(sz, camUp);
					hasCam = true;
				}
				// 'angle' - (half?) camera field of view
				else if (tokenMatch(sz,"angle",5))
				{
//					AI_NFF_PARSE_FLOAT(angle);
					angle = AssUtil.parseFloat(sz.substring(6));
					hasCam = true;
				}
				// 'resolution' - used to compute the screen aspect
				else if (tokenMatch(sz,"resolution",10))
				{
//					AI_NFF_PARSE_FLOAT(resolution.x);
//					AI_NFF_PARSE_FLOAT(resolution.y);
					StringTokenizer tokens = new StringTokenizer(sz);
					tokens.nextToken();
					resolution.x = AssUtil.parseFloat(tokens.nextToken());
					resolution.y = AssUtil.parseFloat(tokens.nextToken());
					hasCam = true;
				}
				// 'pb' - bezier patch. Not supported yet
				else if (tokenMatch(sz,"pb",2))
				{
					DefaultLogger.error("NFF: Encountered unsupported ID: bezier patch");
				}
				// 'pn' - NURBS. Not supported yet
				else if (tokenMatch(sz,"pn",2) || tokenMatch(sz,"pnn",3))
				{
					DefaultLogger.error("NFF: Encountered unsupported ID: NURBS");
				}
				// '' - comment
				else if ('#' == sz.charAt(0))
				{
//					const char* sz;SkipSpaces(&line[1],&sz);
//					if (!IsLineEnd(*sz))DefaultLogger::get()->info(sz);
				}
			}
		}
		// --------------------------------------- Generate Scene ------------------------  ///
		// copy all arrays into one large
		meshes.ensureCapacity(meshes.size()+meshesLocked.size()+meshesWithNormals.size()+meshesWithUVCoords.size());
//		meshes.insert  (meshes.end(),meshesLocked.begin(),meshesLocked.end());
//		meshes.insert  (meshes.end(),meshesWithNormals.begin(),meshesWithNormals.end());
//		meshes.insert  (meshes.end(),meshesWithUVCoords.begin(),meshesWithUVCoords.end());
		meshes.addAll(meshesLocked);
		meshes.addAll(meshesWithNormals);
		meshes.addAll(meshesWithUVCoords);

		// now generate output meshes. first find out how many meshes we'll need
		int numMeshes = 0;
//		std::vector<MeshInfo>::const_iterator it = meshes.begin(), end = meshes.end();
//		for (;it != end;++it)
		for ( MeshInfo it : meshes)
		{
			if (!it.faces.isEmpty())
			{
				++numMeshes;
				if (it.name[0] != 0)++numNamed;
			}
		}

		// generate a dummy root node - assign all unnamed elements such
		// as polygons and polygon patches to the root node and generate
		// sub nodes for named objects such as spheres and cones.
		Node root = new Node();
		root.mName = ("<NFF_Root>");
		int numChildren = numNamed + (hasCam ? 1 : 0) + lights.size();
		int root_numMeshes = numMeshes-numNamed;

		Node[] ppcChildren = null;
		int[] pMeshes = null;
		if (root_numMeshes > 0)
			pMeshes = root.mMeshes = new int[/*root.mNumMeshes*/root_numMeshes];
		if (/*root.mNumChildren*/numChildren > 0)
			ppcChildren = root.mChildren = new Node[/*root.mNumChildren*/ numChildren];

		// generate the camera
		if (hasCam)
		{
			Node nd = ppcChildren[0] = new Node();
			nd.mName = ("<NFF_Camera>");
			nd.mParent = root;

			// allocate the camera in the scene
//			pScene->mNumCameras = 1;
			pScene.mCameras = new Camera[1];
			Camera c = pScene.mCameras[0] = new Camera();

			c.mName = nd.mName; // make sure the names are identical
			c.mHorizontalFOV = (float)Math.toRadians( angle );
//			c.mLookAt		= camLookAt - camPos;
			Vector3f.sub(camLookAt, camPos, c.mLookAt);
			c.mPosition.set(camPos);
			c.mUp.set(camUp);

			// If the resolution is not specified in the file, we
			// need to set 1.0 as aspect. 
			c.mAspect		= (resolution.y == 0 ? 0.f : resolution.x / resolution.y);
//			++ppcChildren;
		}

		// generate light sources
		if (!lights.isEmpty())
		{
//			pScene->mNumLights = lights.size();
			pScene.mLights = new Light[/*pScene->mNumLights*/lights.size()];
			for (int i = 0; i < pScene.mLights.length;++i/*,++ppcChildren*/)
			{
				NFFLight l = lights.get(i);

				Node nd = ppcChildren[i+1]  = new Node();
				nd.mParent = root;

//				nd.mName.length = ::sprintf(nd->mName.data,"<NFF_Light%i>",i);
				nd.mName = String.format("<NFF_Light%d>", i);

				// allocate the light in the scene data structure
				Light out = pScene.mLights[i] = new Light();
				out.mName = nd.mName; // make sure the names are identical
				out.mType = LightSourceType.aiLightSource_POINT;
//				out.mColorDiffuse = out.mColorSpecular = l.color * l.intensity;
				Vector3f.scale(l.color, l.intensity, out.mColorDiffuse);
				out.mColorSpecular.set(out.mColorDiffuse);
				out.mPosition.set(l.position);
			}
		}

		if (numMeshes == 0)throw new DeadlyImportError("NFF: No meshes loaded");
		pScene.mMeshes = new Mesh[numMeshes];
		pScene.mMaterials = new Material[/*pScene->mNumMaterials = pScene->mNumMeshes*/numMeshes];
		int mesh_index = 0;
//		for (it = meshes.begin(), m = 0; it != end;++it)
		for(int i = 0, m = 0; i < meshes.size(); i++)
		{
			MeshInfo it = meshes.get(i);
			if (it.faces.isEmpty())continue;

			MeshInfo src = it;
			Mesh mesh = pScene.mMeshes[m] = new Mesh();
			mesh.mNumVertices = src.vertices.limit()/3;
			int numFaces = src.faces.size();

			// Generate sub nodes for named meshes
			if (src.name[0] != 0)
			{
				Node node = ppcChildren[AssUtil.findfirstNull(ppcChildren)] = new Node();
				node.mParent = root;
//				node.mNumMeshes = 1;
				node.mMeshes = new int[1];
				node.mMeshes[0] = m;
				node.mName = new String(src.name).trim();

				// setup the transformation matrix of the node
//				aiMatrix4x4::FromToMatrix(aiVector3D(0.f,1.f,0.f),
//					src.dir,node.mTransformation);
				AssUtil.fromToMatrix(Vector3f.Y_AXIS, src.dir, node.mTransformation);

				Matrix4f mat = node.mTransformation;
				mat.m00 *= src.radius.x; mat.m01 *= src.radius.x; mat.m02 *= src.radius.x;
				mat.m10 *= src.radius.y; mat.m11 *= src.radius.y; mat.m12 *= src.radius.y;
				mat.m20 *= src.radius.z; mat.m21 *= src.radius.z; mat.m22 *= src.radius.z;
				mat.m30 = src.center.x;
				mat.m31 = src.center.y;
				mat.m32 = src.center.z;

//				++ppcChildren;
			}
			else // *pMeshes++ = m;
				pMeshes[mesh_index++] = m;

			// copy vertex positions
//			mesh->mVertices = new aiVector3D[mesh->mNumVertices];
//			::memcpy(mesh->mVertices,&src.vertices[0],
//				sizeof(aiVector3D)*mesh->mNumVertices);
			natived = AssimpConfig.MESH_USE_NATIVE_MEMORY;
			mesh.mVertices = MemoryUtil.refCopy(src.vertices, natived);

			// NFF2: there could be vertex colors
			if (!AssUtil.isEmpty(src.colors))
			{
				assert(src.colors.remaining()/4 == src.vertices.remaining()/3);

				// copy vertex colors
//				mesh->mColors[0] = new aiColor4D[mesh->mNumVertices];
//				::memcpy(mesh->mColors[0],&src.colors[0],
//					sizeof(aiColor4D)*mesh->mNumVertices);
				mesh.mColors[0] = MemoryUtil.refCopy(src.colors, natived);
			}

//			if (!src.normals.empty())
//			{
//				ai_assert(src.normals.size() == src.vertices.size());
//
//				// copy normal vectors
//				mesh->mNormals = new aiVector3D[mesh->mNumVertices];
//				::memcpy(mesh->mNormals,&src.normals[0],
//					sizeof(aiVector3D)*mesh->mNumVertices);
//			}
			mesh.mNormals = MemoryUtil.refCopy(src.normals, natived);

//			if (!src.uvs.empty())
//			{
//				ai_assert(src.uvs.size() == src.vertices.size());
//
//				// copy texture coordinates
//				mesh->mTextureCoords[0] = new aiVector3D[mesh->mNumVertices];
//				::memcpy(mesh->mTextureCoords[0],&src.uvs[0],
//					sizeof(aiVector3D)*mesh->mNumVertices);
//			}
			mesh.mTextureCoords[0] = MemoryUtil.refCopy(src.uvs, natived);

			// generate faces
			int p = 0;
			Face[] pFace = mesh.mFaces = new Face[numFaces];
//			for (std::vector<unsigned int>::const_iterator it2 = src.faces.begin(),
//				end2 = src.faces.end();
//				it2 != end2;++it2,++pFace)
			for (int ii = 0; ii < src.faces.size(); ii++)
			{
//				pFace->mIndices = new int [ pFace->mNumIndices = *it2 ];
				Face face = pFace[i] = Face.createInstance(src.faces.getInt(ii));
//				for (unsigned int o = 0; o < pFace->mNumIndices;++o)
//					pFace->mIndices[o] = p++;
				for(int o = 0; o < face.getNumIndices(); o++)
					face.set(o, p++);
			}

			// generate a material for the mesh
			Material pcMat =pScene.mMaterials[m] = new Material();

			mesh.mMaterialIndex = m++;
			pcMat.addProperty(Material.AI_DEFAULT_MATERIAL_NAME, Material.AI_MATKEY_NAME,0,0);

			// FIX: Ignore diffuse == 0 
//			aiColor3D c = src.shader.color * (src.shader.diffuse.r ?  src.shader.diffuse : aiColor3D(1.f,1.f,1.f));
			Vector3f c = vec3;
			if(src.shader.diffuse.x != 0)
				Vector3f.scale(src.shader.color, src.shader.diffuse, c);
			else
				c.set(src.shader.color);
			pcMat.addProperty(c,Material.AI_MATKEY_COLOR_DIFFUSE,0,0);
//			c = src.shader.color * src.shader.specular;
			Vector3f.scale(src.shader.color, src.shader.specular, c);
			pcMat.addProperty(c,Material.AI_MATKEY_COLOR_SPECULAR,0,0);

			// NFF2 - default values for NFF
			pcMat.addProperty(src.shader.ambient, Material.AI_MATKEY_COLOR_AMBIENT,0,0);
			pcMat.addProperty(src.shader.emissive,Material.AI_MATKEY_COLOR_EMISSIVE,0,0);
			pcMat.addProperty(src.shader.opacity, Material.AI_MATKEY_OPACITY,0,0);

			// setup the first texture layer, if existing
			if (src.shader.texFile.length() > 0)
			{
				pcMat.addProperty(src.shader.texFile, Material._AI_MATKEY_TEXTURE_BASE, TextureType.aiTextureType_DIFFUSE.ordinal(),0);

				if (TextureMapping.aiTextureMapping_UV != src.shader.mapping) {
					Vector3f v = c;
					v.set(0.f,-1.f,0.f);
					pcMat.addProperty(v, Material._AI_MATKEY_TEXMAP_AXIS_BASE, TextureType.aiTextureType_DIFFUSE.ordinal(),0);
					pcMat.addProperty(src.shader.mapping.ordinal(), Material._AI_MATKEY_MAPPING_BASE, TextureType.aiTextureType_DIFFUSE.ordinal(),0);
				}
			}

			// setup the name of the material
			if (src.shader.name.length() > 0)
			{
//				s.Set(src.shader.texFile);
				pcMat.addProperty(src.shader.texFile, Material.AI_MATKEY_NAME,0,0);
			}

			// setup some more material properties that are specific to NFF2
			int ii;
			if (src.shader.twoSided)
			{
				ii = 1;
				pcMat.addProperty(ii,Material.AI_MATKEY_TWOSIDED,0,0);
			}
			ii = (src.shader.shaded ?  ShadingMode.aiShadingMode_Gouraud.ordinal() : ShadingMode.aiShadingMode_NoShading.ordinal());
			if (src.shader.shininess != 0)
			{
				ii = ShadingMode.aiShadingMode_Phong.ordinal();
				pcMat.addProperty(src.shader.shininess,Material.AI_MATKEY_SHININESS,0,0);
			}
			pcMat.addProperty(ii,Material.AI_MATKEY_SHADING_MODEL,0,0);
		}
		pScene.mRootNode = root;
	}
	
	static void ai_nff_parse_triple(String line, Vector3f c){
		StringTokenizer tokens = new StringTokenizer(line, " \t");
		tokens.nextToken();  // skip the first token.
		
		if(tokens.hasMoreTokens()) c.x = AssUtil.parseFloat(tokens.nextToken());
		if(tokens.hasMoreTokens()) c.y = AssUtil.parseFloat(tokens.nextToken());
		if(tokens.hasMoreTokens()) c.z = AssUtil.parseFloat(tokens.nextToken());
	}
	
	static void ai_nff_parse_triple2(String line, Vector3f c){
		StringTokenizer tokens = new StringTokenizer(line, " \t");
//		tokens.nextToken();  // skip the first token.
		
		String[] _tokens = new String[4];
		int i;
		for(i = 0; i < 4 && tokens.hasMoreTokens(); i++)
			_tokens[i] = tokens.nextToken();
		
		if(i == 4){
			i = 1;  // skip the first
		}else{
			i = 0;
		}
		
		if(tokens.hasMoreTokens()) c.x = AssUtil.parseFloat(_tokens[i++]);
		if(tokens.hasMoreTokens()) c.y = AssUtil.parseFloat(_tokens[i++]);
		if(tokens.hasMoreTokens()) c.z = AssUtil.parseFloat(_tokens[i++]);
	}
	
	// -------------------------------------------------------------------
	/** Loads the material table for the NFF2 file format from an
	 *  external file.<p>
	 *
	 *  @param output Receives the list of output meshes
	 *  @param path Path to the file (abs. or rel.)
	*/
	void loadNFF2MaterialTable(List<ShadingInfo> output,String path){
		ByteBuffer buffer = FileUtils.loadText(new File(path), true, AssimpConfig.LOADING_USE_NATIVE_MEMORY);
		
		// The file should start with the magic sequence "mat"
		if (!tokenMatch(buffer,"mat",3))	{
			DefaultLogger.error("NFF2: Not a valid material library " + path + ".");
			return;
		}

		ShadingInfo curShader = null;
		byte[] cache = new byte[1024];
		String sz;
		while((sz = AssUtil.getNextLine(buffer, cache, true)) != null){
			
			// 'version' defines the version of the file format
			if (tokenMatch(sz,"version",7))
			{
				if(DefaultLogger.LOG_OUT)
					DefaultLogger.info("NFF (Sense8) material library file format: " + sz);
			}
			// 'matdef' starts a new material in the file
			else if (tokenMatch(sz,"matdef",6))
			{
				// add a new material to the list
				output.add( curShader = new ShadingInfo() );
//				curShader = & output.back();

				// parse the name of the material
			}
			else if (!tokenMatch(sz,"valid",5))
			{
				// check whether we have an active material at the moment
//				if (!IsLineEnd(*sz))
//				sz = sz.substring(5).trim();
				if (sz.length() > 0)
				{
					if (curShader == null)
					{
						DefaultLogger.error("NFF2 material library: Found element " + sz + "but there is no active material");
						continue;
					}
				}
				else continue;

				// now read the material property and determine its type
				if (tokenMatch(sz,"ambient",7))
				{
					ai_nff_parse_triple(sz, curShader.ambient);
				}
				else if (tokenMatch(sz,"diffuse",7) || tokenMatch(sz,"ambientdiffuse",14) /* correct? */)
				{
//					AI_NFF_PARSE_TRIPLE(c);
//					curShader->diffuse = curShader->ambient = c;
					ai_nff_parse_triple(sz, curShader.diffuse);
					curShader.ambient.set(curShader.diffuse);
				}
				else if (tokenMatch(sz,"specular",8))
				{
//					AI_NFF_PARSE_TRIPLE(c);
//					curShader->specular = c;
					ai_nff_parse_triple(sz, curShader.specular);
				}
				else if (tokenMatch(sz,"emission",8))
				{
//					AI_NFF_PARSE_TRIPLE(c);
//					curShader->emissive = c;
					ai_nff_parse_triple(sz, curShader.emissive);
				}
				else if (tokenMatch(sz,"shininess",9))
				{
//					AI_NFF_PARSE_FLOAT(curShader->shininess);
					int space = sz.indexOf(' ');
					curShader.shininess = AssUtil.parseFloat(sz.substring(space + 1));
				}
				else if (tokenMatch(sz,"opacity",7))
				{
//					AI_NFF_PARSE_FLOAT(curShader->opacity);
					int space = sz.indexOf(' ');
					curShader.opacity = AssUtil.parseFloat(sz.substring(space + 1));
				}
			}
		}
			
	}
	
	static boolean tokenMatch(String line, String str, int len){
//		return AssUtil.equals(buffer, str, 0, len);
		return line.startsWith(str);
	}
	
	static boolean tokenMatch(ByteBuffer buffer, String str, int len){
		return AssUtil.equals(buffer, str, 0, len);
	}
	
	static void ai_nff_parse_shape_information(String str, MeshInfo mesh){
		Vector3f center = mesh.center;
		Vector3f radius = mesh.radius;
		radius.set(1.f, Float.NaN, Float.NaN);
		
		StringTokenizer tokens = new StringTokenizer(str, " ");
		center.x = AssUtil.parseFloat(tokens.nextToken());
		center.y = AssUtil.parseFloat(tokens.nextToken());
		center.z = AssUtil.parseFloat(tokens.nextToken());
		
		radius.x = AssUtil.parseFloat(tokens.nextToken());
		radius.y = AssUtil.parseFloat(tokens.nextToken());
		radius.z = AssUtil.parseFloat(tokens.nextToken());
		
		if(radius.z != radius.z)
			radius.z = radius.x;
		
		if(radius.y != radius.y)
			radius.y = radius.x;
	}

}
