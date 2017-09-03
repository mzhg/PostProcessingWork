package assimp.importer.ac;

import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

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
import assimp.common.IntPair;
import assimp.common.Light;
import assimp.common.LightSourceType;
import assimp.common.Material;
import assimp.common.MemoryUtil;
import assimp.common.Mesh;
import assimp.common.Node;
import assimp.common.ParsingUtil;
import assimp.common.Scene;
import assimp.common.ShadingMode;
import assimp.common.Subdivider;
import assimp.common.TextureType;
import assimp.common.UVTransform;

/** AC3D (*.ac) importer class */
public class AC3DImporter extends BaseImporter{
	
	private static final ImporterDesc desc = new ImporterDesc(
		"AC3D Importer",
		"",
		"",
		"",
		ImporterDesc.aiImporterFlags_SupportTextFlavour,
		0,
		0,
		0,
		0,
		"ac acc ac3d"
	);

	// points to the next data line
	ParsingUtil buffer;

	// Configuration option: if enabled, up to two meshes
	// are generated per material: those faces who have 
	// their bf cull flags set are separated.
	boolean configSplitBFCull;

	// Configuration switch: subdivision surfaces are only
	// evaluated if the value is true.
	boolean configEvalSubdivision;

	// counts how many objects we have in the tree.
	// basing on this information we can find a
	// good estimate how many meshes we'll have in the final scene.
	int mNumMeshes;

	// current list of light sources
	List<Light> mLights;

	// name counters
	int lights, groups, polys, worlds;
		
	@Override
	protected boolean canRead(String pFile, InputStream pIOHandler, boolean checkSig) {
		String extension = getExtension(pFile);

		// fixme: are acc and ac3d *really* used? Some sources say they are
		if(extension.equals("ac") || extension.equals("ac3d") || extension.equals("acc")) {
			return true;
		}
		if (extension.length() == 0|| checkSig) {
//			int token = AI_MAKE_MAGIC("AC3D");
			byte[] magic = {(byte)'D', (byte)'3', (byte)'C', (byte)'A'};
			return checkMagicToken(new File(pFile),magic,0, 4);
		}
		return false;
	}

	@Override
	protected ImporterDesc getInfo() { return desc;}

	@Override
	protected void internReadFile(File pFile, Scene pScene) {
		ByteBuffer data = FileUtils.loadText(pFile, false, AssimpConfig.LOADING_USE_NATIVE_MEMORY);
		buffer = new ParsingUtil(data);
		
		mNumMeshes = 0;

		lights = polys = worlds = groups = 0;

//		if (::strncmp(buffer,"AC3D",4)) {
		if(buffer.strncmp("AC3D")){
			throw new DeadlyImportError("AC3D: No valid AC3D file, magic sequence not found");
		}

		// print the file format version to the console
		if(DefaultLogger.LOG_OUT){
			int version = AssUtil.hexDigitToDecimal( (char)buffer.get() );
//			char msg[3];
//			ASSIMP_itoa10(msg,3,version);
			DefaultLogger.info(("AC3D file format version: ") + version);
		}

		List<ACMaterial> materials = new ArrayList<ACMaterial>(5);
//		materials.reserve(5);

		List<ACObject> rootObjects = new ArrayList<ACObject>(5);
//		rootObjects.reserve(5);

		List<Light> lights = new ArrayList<Light>();
		mLights = lights;

		while (getNextLine())
		{
			if (buffer.tokenMatch("MATERIAL"))  // (TokenMatch(buffer,"MATERIAL",8))
			{
				ACMaterial mat;
				materials.add(mat = new ACMaterial());
//				Material& mat = materials.back();

				// manually parse the material ... sscanf would use the buldin atof ...
				// Format: (name) rgb %f %f %f  amb %f %f %f  emis %f %f %f  spec %f %f %f  shi %d  trans %f

//				AI_AC_SKIP_TO_NEXT_TOKEN();
				if(buffer.ac_skip_to_next_token())
					continue;
				
				if ('\"' == buffer.get())
				{
					String[] out = new String[1];
//					AI_AC_GET_STRING(mat.name);
//					AI_AC_SKIP_TO_NEXT_TOKEN();
					if(buffer.ac_get_string(out)){
						continue;
					}else{
						mat.name = out[0];
					}
					
					if(buffer.ac_skip_to_next_token())
						continue;
				}

//				AI_AC_CHECKED_LOAD_FLOAT_ARRAY("rgb",3,3,&mat.rgb);
//				AI_AC_CHECKED_LOAD_FLOAT_ARRAY("amb",3,3,&mat.amb);
//				AI_AC_CHECKED_LOAD_FLOAT_ARRAY("emis",4,3,&mat.emis);
//				AI_AC_CHECKED_LOAD_FLOAT_ARRAY("spec",4,3,&mat.spec);
//				AI_AC_CHECKED_LOAD_FLOAT_ARRAY("shi",3,1,&mat.shin);
//				AI_AC_CHECKED_LOAD_FLOAT_ARRAY("trans",5,1,&mat.trans);
				
				float[] fout3 = new float[3];
				if(!buffer.ac_checked_load_float_array("rgb", fout3)){
					mat.rgb.load(fout3, 0);
				}else
					continue;
				
				if(!buffer.ac_checked_load_float_array("amb", fout3)){
					mat.amb.load(fout3, 0);
				}else
					continue;
				
				if(!buffer.ac_checked_load_float_array("emis", fout3)){
					mat.emis.load(fout3, 0);
				}else
					continue;
				
				float[] fout1 = new float[1];
				if(!buffer.ac_checked_load_float_array("shi", fout1)){
					mat.shin= fout1[0];
				}else
					continue;
				
				if(!buffer.ac_checked_load_float_array("trans", fout1)){
					mat.trans = fout1[0];
				}else
					continue;
			}
			
			loadObjectSection(rootObjects);
		}

		if (rootObjects.isEmpty() || mNumMeshes == 0)
		{
			throw new DeadlyImportError("AC3D: No meshes have been loaded");
		}
		if (materials.isEmpty())
		{
			if(DefaultLogger.LOG_OUT)
				DefaultLogger.warn("AC3D: No material has been found");
			materials.add(new ACMaterial());
		}

		mNumMeshes += (mNumMeshes>>2) + 1;
		List<Mesh> meshes = new ArrayList<Mesh>(mNumMeshes);
//		meshes.reserve(mNumMeshes);

		List<Material> omaterials = new ArrayList<Material>(mNumMeshes);
//		materials.reserve(mNumMeshes);

		// generate a dummy root if there are multiple objects on the top layer
		ACObject root;
		if (1 == rootObjects.size())
			root = rootObjects.get(0);
		else
		{
			root = new ACObject();
		}

		// now convert the imported stuff to our output data structure
		pScene.mRootNode = convertObjectSection(root,meshes,omaterials,materials, null);
		if (1 != rootObjects.size())/*delete root*/ root=null;

//		if (!::strncmp( pScene->mRootNode->mName.data, "Node", 4))
		if(pScene.mRootNode.mName.equals("Node"))
			pScene.mRootNode.mName = ("<AC3DWorld>");

		// copy meshes
		if (meshes.isEmpty())
		{
			throw new DeadlyImportError("An unknown error occured during converting");
		}
//		pScene->mNumMeshes = (unsigned int)meshes.size();
//		pScene->mMeshes = new aiMesh*[pScene->mNumMeshes];
//		::memcpy(pScene->mMeshes,&meshes[0],pScene->mNumMeshes*sizeof(void*));
		pScene.mMeshes = meshes.toArray(new Mesh[meshes.size()]);

		// copy materials
//		pScene->mNumMaterials = (unsigned int)omaterials.size();
//		pScene->mMaterials = new aiMaterial*[pScene->mNumMaterials];
//		::memcpy(pScene->mMaterials,&omaterials[0],pScene->mNumMaterials*sizeof(void*));
		pScene.mMaterials = omaterials.toArray(new Material[omaterials.size()]);

		// copy lights
//		pScene->mNumLights = (unsigned int)lights.size();
//		if (lights.size())
//		{
//			pScene->mLights = new aiLight*[lights.size()];
//			::memcpy(pScene->mLights,&lights[0],lights.size()*sizeof(void*));
//		}
		if(lights.size() > 0)
			pScene.mLights = lights.toArray(new Light[lights.size()]);
	}
	
	// -------------------------------------------------------------------
	/** Get the next line from the file.
	 *  @return false if the end of the file was reached*/
	boolean getNextLine(){
		buffer.skipLine();
		return buffer.skipSpaces();
	}

	// -------------------------------------------------------------------
	/** Load the object section. This method is called recursively to
	 *  load subobjects, the method returns after a 'kids 0' was 
	 *  encountered.
	 *  @objects List of output objects*/
	void loadObjectSection(List<ACObject> objects){
		if (!buffer.tokenMatch("OBJECT"))
			return;
		String[] out = new String[1];
		buffer.skipSpaces();

		++mNumMeshes;

		ACObject obj;
		objects.add(obj = new ACObject());

		Light light = null;
//		if (!ASSIMP_strincmp(buffer,"light",5))
		if(!buffer.strncmp("light"))
		{
			// This is a light source. Add it to the list
			mLights.add(light = new Light());

			// Return a point light with no attenuation
			light.mType = LightSourceType.aiLightSource_POINT;
			light.mColorDiffuse.set(1.f,1.f,1.f);
			light.mColorSpecular.set(1.f,1.f,1.f);
			light.mAttenuationConstant = 1.f;

			// Generate a default name for both the light source and the node
			// FIXME - what's the right way to print a size_t? Is 'zu' universally available? stick with the safe version.
//			light.mName.length = ::sprintf(light.mName.data,"ACLight_%i",static_cast<unsigned int>(mLights.size())-1);
			light.mName = String.format("ACLight_%i", mLights.size() - 1);
			obj.name = light.mName; //std::string( light.mName.data );

			if(DefaultLogger.LOG_OUT)
				DefaultLogger.debug("AC3D: Light source encountered");
			obj.type = ACObject.Light;
		}
		else if(!buffer.strncmp("group"))    //(!ASSIMP_strincmp(buffer,"group",5))
		{
			obj.type = ACObject.Group;
		}
		else if (!buffer.strncmp("world"))  // (!ASSIMP_strincmp(buffer,"world",5))
		{
			obj.type = ACObject.World;
		}
		else obj.type = ACObject.Poly;
		while (getNextLine())
		{
//			if (TokenMatch(buffer,"kids",4))
			if(buffer.tokenMatch("kids"))
			{
//				buffer.skipSpaces();
				buffer.skipSpaces();
				int num = buffer.strtoul10(); //   strtoul10(buffer,&buffer);
				getNextLine();
				if (num != 0)
				{
					// load the children of this object recursively
//					obj.children.reserve(num);
					for (int i = 0; i < num; ++i)
						loadObjectSection(obj.children);
				}
				return;
			}
			else if(buffer.tokenMatch("name")) //(TokenMatch(buffer,"name",4))
			{
				buffer.skipSpaces();
//				AI_AC_GET_STRING(obj.name);
				if(!buffer.ac_get_string(out)){
					obj.name = out[0];
				}else{
					continue;
				}

				// If this is a light source, we'll also need to store
				// the name of the node in it.
				if (light != null)
				{
					light.mName=(obj.name);
				}
			}
			else if (buffer.tokenMatch("texture"))  //(TokenMatch(buffer,"texture",7))
			{
				buffer.skipSpaces();
//				AI_AC_GET_STRING(obj.texture);
				if(!buffer.ac_get_string(out)){
					obj.texture = out[0];
				}else{
					continue;
				}
			}
			else if (buffer.tokenMatch("texrep")) //(TokenMatch(buffer,"texrep",6))
			{
				buffer.skipSpaces();
//				AI_AC_CHECKED_LOAD_FLOAT_ARRAY("",0,2,&obj.texRepeat);
//				if (!obj.texRepeat.x || !obj.texRepeat.y)
//					obj.texRepeat = aiVector2D (1.f,1.f);
				float[] fout = new float[2];
				if(!buffer.ac_checked_load_float_array("", fout)){
					obj.texRepeat.load(fout, 0);
					if(obj.texRepeat.x == 0 || obj.texRepeat.y == 0)
						obj.texRepeat.set(1f,1f);
				}else
					continue;
			}
			else if (buffer.tokenMatch("texoff"))  //(TokenMatch(buffer,"texoff",6))
			{
				buffer.skipSpaces();
//				AI_AC_CHECKED_LOAD_FLOAT_ARRAY("",0,2,&obj.texOffset);
				float[] fout = new float[2];
				if(!buffer.ac_checked_load_float_array("", fout)){
					obj.texOffset.load(fout, 0);
				}else
					continue;
				
			}
			else if (buffer.tokenMatch("rot"))  //(TokenMatch(buffer,"rot",3))
			{
				buffer.skipSpaces();
//				AI_AC_CHECKED_LOAD_FLOAT_ARRAY("",0,9,&obj.rotation);
				float[] fout = new float[9];
				if(!buffer.ac_checked_load_float_array("", fout)){
					obj.rotation.load(fout, 0);
				}else
					continue;
			}
			else if (buffer.tokenMatch("loc"))  //(TokenMatch(buffer,"loc",3))
			{
				buffer.skipSpaces();
//				AI_AC_CHECKED_LOAD_FLOAT_ARRAY("",0,3,&obj.translation);
				float[] fout = new float[3];
				if(!buffer.ac_checked_load_float_array("", fout)){
					obj.translation.load(fout, 0);
				}else
					continue;
			}
			else if (buffer.tokenMatch("subdiv"))  //(TokenMatch(buffer,"subdiv",6))
			{
				buffer.skipSpaces();
				obj.subDiv = buffer.strtoul10(); //strtoul10(buffer,&buffer);
			}
			else if (buffer.tokenMatch("crease"))  //(TokenMatch(buffer,"crease",6))
			{
				buffer.skipSpaces();
				int pos = buffer.getCurrent();
				obj.crease = (float) buffer.fast_atoreal_move(true);
				buffer.setCurrent(pos);
			}
			else if (buffer.tokenMatch("numvert"))  //(TokenMatch(buffer,"numvert",7))
			{
				buffer.skipSpaces();

				int t = buffer.strtoul10();
//				obj.vertices.reserve(t);
				for (int i = 0; i < t;++i)
				{
					if (!getNextLine())
					{
						DefaultLogger.error("AC3D: Unexpected EOF: not all vertices have been parsed yet");
						break;
					}
					else if (!buffer.isNumeric())
					{
						DefaultLogger.error("AC3D: Unexpected token: not all vertices have been parsed yet");
//						--buffer; 
						buffer.deCre(); // make sure the line is processed a second time
						break;
					}
					Vector3f v;
					obj.vertices.add(v = new Vector3f());
//					aiVector3D& v = obj.vertices.back();
//					AI_AC_CHECKED_LOAD_FLOAT_ARRAY("",0,3,&v.x);
					float[] fout = new float[3];
					if(!buffer.ac_checked_load_float_array("", fout)){
						v.load(fout, 0);
					}else
						continue;
				}
			}
			else if (buffer.tokenMatch("numsurf"))  //(TokenMatch(buffer,"numsurf",7))
			{
				buffer.skipSpaces();
				boolean Q3DWorkAround = false;

				final int t = buffer.strtoul10();
//				obj.surfaces.reserve(t);
				for (int i = 0; i < t;++i)
				{
					getNextLine();
					if (!buffer.tokenMatch("SURF"))  //(!TokenMatch(buffer,"SURF",4))
					{
						// FIX: this can occur for some files - Quick 3D for 
						// example writes no surf chunks
						if (!Q3DWorkAround)
						{
							DefaultLogger.warn("AC3D: SURF token was expected");
							DefaultLogger.debug("Continuing with Quick3D Workaround enabled");
						}
//						--buffer; 
						buffer.deCre();  // make sure the line is processed a second time
						// break; --- see fix notes above

						Q3DWorkAround = true;
					}
					buffer.skipSpaces();
					Surface surf;
					obj.surfaces.add(surf = new Surface());
//					Surface& surf = obj.surfaces.back();
					int pos = buffer.getCurrent();
					surf.flags = buffer.strtoul_cppstyle();
					buffer.setCurrent(pos);
				
					while (true)
					{
						if (!getNextLine())
						{
							DefaultLogger.error("AC3D: Unexpected EOF: surface is incomplete");
							break;
						}
						if (buffer.tokenMatch("mat"))  //(TokenMatch(buffer,"mat",3))
						{
							buffer.skipSpaces();
							pos = buffer.getCurrent();
							surf.mat = buffer.strtoul10();
							buffer.setCurrent(pos);
						}
						else if (buffer.tokenMatch("refs"))  //(TokenMatch(buffer,"refs",4))
						{
							// --- see fix notes above
							if (Q3DWorkAround)
							{
								if (!surf.entries.isEmpty())
								{
//									buffer -= 6;
									buffer.setCurrent(buffer.getCurrent() - 6);
									break;
								}
							}

							buffer.skipSpaces();
							final int m = buffer.strtoul10();
//							surf.entries.reserve(m);

							obj.numRefs += m;

							for (int k = 0; k < m; ++k)
							{
								if(!getNextLine())
								{
									DefaultLogger.error("AC3D: Unexpected EOF: surface references are incomplete");
									break;
								}
								SurfaceEntry entry;
								surf.entries.add(entry = new SurfaceEntry());
//								Surface::SurfaceEntry& entry = surf.entries.back();

								entry.first = buffer.strtoul10();
								buffer.skipSpaces();
//								AI_AC_CHECKED_LOAD_FLOAT_ARRAY("",0,2,&entry.second);
								float[] fout = new float[2];
								if(!buffer.ac_checked_load_float_array("", fout)){
									entry.x = fout[0];
									entry.y = fout[1];
								}else
									continue;
							}
						}
						else 
						{

//							--buffer; // make sure the line is processed a second time
							buffer.deCre();
							break;
						}
					}
				}
			}
		}
		DefaultLogger.error("AC3D: Unexpected EOF: \'kids\' line was expected");
	}

	// -------------------------------------------------------------------
	/** Convert all objects into meshes and nodes.
	 *  @param object Current object to work on
	 *  @param meshes Pointer to the list of output meshes
	 *  @param outMaterials List of output materials
	 *  @param materials Material list
	 *  @param Scenegraph node for the object */
	Node convertObjectSection(ACObject object, List<Mesh> meshes, List<Material> outMaterials, List<ACMaterial> materials,Node parent){
		
		Node node = new Node();
		int node_num_meshes = 0;
		node.mParent = parent;
		if (object.vertices.size() > 0)
		{
			if (object.surfaces.isEmpty() || object.numRefs == 0)
			{
				/* " An object with 7 vertices (no surfaces, no materials defined). 
				     This is a good way of getting point data into AC3D. 
				     The Vertex->create convex-surface/object can be used on these
				     vertices to 'wrap' a 3d shape around them "
					 (http://www.opencity.info/html/ac3dfileformat.html)

					 therefore: if no surfaces are defined return point data only
				 */
				if(DefaultLogger.LOG_OUT)
					DefaultLogger.info("AC3D: No surfaces defined in object definition, a point list is returned");

				Mesh mesh;
				meshes.add(mesh = new Mesh());
//				aiMesh* mesh = meshes.back();

				int numFaces = mesh.mNumVertices = object.vertices.size();
				Face[] faces = mesh.mFaces = new Face[numFaces];
				mesh.mVertices = MemoryUtil.createFloatBuffer(mesh.mNumVertices * 3, AssimpConfig.MESH_USE_NATIVE_MEMORY);  //new aiVector3D[mesh->mNumVertices];
				for (int i = 0; i < mesh.mNumVertices;++i)
				{
					Vector3f v = object.vertices.get(i);
//					*verts = object.vertices[i];
					mesh.mVertices.put(3 * i, v.x);
					mesh.mVertices.put(3 * i + 1, v.y);
					mesh.mVertices.put(3 * i + 2, v.z);
//					faces->mNumIndices = 1;
//					faces->mIndices = new unsigned int[1];
//					faces->mIndices[0] = i;
					Face face = faces[i] = Face.createInstance(1);
					face.set(0, i);
				}

				// use the primary material in this case. this should be the
				// default material if all objects of the file contain points
				// and no faces.
				mesh.mMaterialIndex = 0;
				Material back;
				outMaterials.add(back = new Material());
				convertMaterial(object, materials.get(0), back);
			}
			else
			{
				// need to generate one or more meshes for this object.
				// find out how many different materials we have
//				typedef std::pair< unsigned int, unsigned int > IntPair;
//				typedef std::vector< IntPair > MatTable;
//				MatTable needMat(materials.size(),IntPair(0,0));
				IntPair[] needMat = new IntPair[materials.size()];
				AssUtil.initArray(needMat);

//				std::vector<Surface>::iterator it,end = object.surfaces.end();
//				std::vector<Surface::SurfaceEntry>::iterator it2,end2;
				int it, end = object.surfaces.size();
				int it2, end2;

				for (it = 0; it != end; ++it)
				{
					Surface surface = object.surfaces.get(it);
					int idx = surface.mat;
					if (idx >= needMat.length)
					{
						DefaultLogger.error("AC3D: material index is out of range");
						idx = 0;
					}
					if (surface.entries.isEmpty())
					{
						DefaultLogger.warn("AC3D: surface her zero vertex references");
					}

					// validate all vertex indices to make sure we won't crash here
					for (it2  = 0, end2 = surface.entries.size(); it2 != end2; ++it2)
					{
						SurfaceEntry entry = surface.entries.get(it2);
						if (entry.first >= object.vertices.size())
						{
							DefaultLogger.warn("AC3D: Invalid vertex reference");
							entry.first = 0;
						}
					}

					if (needMat[idx].first == 0)++node_num_meshes;

					switch (surface.flags & 0xf)
					{
						// closed line
					case 0x1:

						needMat[idx].first  += surface.entries.size();
						needMat[idx].second += surface.entries.size()<<1;
						break;

						// unclosed line
					case 0x2:

						needMat[idx].first  += surface.entries.size()-1;
						needMat[idx].second += (surface.entries.size()-1)<<1;
						break;

						// 0 == polygon, else unknown
					default:

						if ((surface.flags & 0xf)!=0)
						{
							DefaultLogger.warn("AC3D: The type flag of a surface is unknown");
							surface.flags &= ~(0xf);
						}

						// the number of faces increments by one, the number
						// of vertices by surface.numref.
						needMat[idx].first++;
						needMat[idx].second += surface.entries.size();
					};
				}
				node.mMeshes = new int[node_num_meshes];
				int pip = 0;
				int mat = 0;
				final int oldm = meshes.size();
//				for (MatTable::const_iterator cit = needMat.begin(), cend = needMat.end();
//					cit != cend; ++cit, ++mat)
				for(int k = 0; k < needMat.length; k++)
				{
					IntPair cit = needMat[k];
					if (cit.first == 0)continue;

					// allocate a new aiMesh object
					node.mMeshes[pip++] = meshes.size();
					Mesh mesh = new Mesh();
					meshes.add(mesh);

					mesh.mMaterialIndex = outMaterials.size();
					Material back;
					outMaterials.add(back = new Material());
					convertMaterial(object, materials.get(mat), back);

					// allocate storage for vertices and normals
//					mesh.mNumFaces = (*cit).first;
//					aiFace* faces = mesh->mFaces = new aiFace[mesh->mNumFaces];
					mesh.mFaces = new Face[cit.first];
					int faces = 0;
					mesh.mNumVertices = cit.second;
					FloatBuffer vertices = mesh.mVertices = MemoryUtil.createFloatBuffer(mesh.mNumVertices * 3, AssimpConfig.MESH_USE_NATIVE_MEMORY);  //new aiVector3D[mesh->mNumVertices];
					int cur = 0;
					int vertices_index = 0;

					// allocate UV coordinates, but only if the texture name for the
					// surface is not empty
//					aiVector3D* uv = NULL;
					FloatBuffer uv = null;
					int uv_index = 0;
					if(object.texture.length() > 0)
					{
						uv = mesh.mTextureCoords[0] = MemoryUtil.createFloatBuffer(mesh.mNumVertices * 3, AssimpConfig.MESH_USE_NATIVE_MEMORY);  //new aiVector3D[mesh->mNumVertices];
						mesh.mNumUVComponents[0] = 2;
					}

					for (it = 0; it != end; ++it)
					{
						Surface surface = object.surfaces.get(it);
						if (mat == surface.mat)
						{
							Surface src = surface;

							// closed polygon
							int type = surface.flags & 0xf; 
							if (type == 0)
							{
								Face face = mesh.mFaces[faces++];
								int numIndices;
								if((numIndices = src.entries.size()) > 0)
								{
//									face.mIndices = new unsigned int[numIndices];
									face = mesh.mFaces[faces - 1] = Face.createInstance(numIndices);
									for (int i = 0; i < numIndices;++i,++vertices_index)
									{
										SurfaceEntry entry = src.entries.get(i);
//										face.mIndices[i] = cur++;
										face.set(i, cur++);

										// copy vertex positions
//										*vertices = object.vertices[entry.first] + object.translation;
										Vector3f v = object.vertices.get(entry.first);
										Vector3f t = object.translation;
										vertices.put(3 * vertices_index + 0, v.x + t.x);
										vertices.put(3 * vertices_index + 1, v.y + t.y);
										vertices.put(3 * vertices_index + 2, v.z + t.z);

										// copy texture coordinates 
										if (uv != null)
										{
//											uv->x =  entry.second.x;
//											uv->y =  entry.second.y;
											uv.put(3 * uv_index + 0, entry.x);
											uv.put(3 * uv_index + 1, entry.y);
											++uv_index;
										}
									}
								}
							}
							else
							{
								
								it2  = 0/*surface.entries.begin()*/;
								SurfaceEntry entry = surface.entries.get(it2);
								
								// either a closed or an unclosed line
								int tmp = surface.entries.size();
								if (0x2 == type)--tmp;
								for (int m = 0; m < tmp;++m)
								{
//									aiFace& face = *faces++;
//
//									face.mNumIndices = 2;
//									face.mIndices = new unsigned int[2];
//									face.mIndices[0] = cur++;
//									face.mIndices[1] = cur++;
									Face face = mesh.mFaces[faces++] = Face.createInstance(2);
									face.set(0, cur++);
									face.set(1, cur++);

									// copy vertex positions
//									*vertices++ = object.vertices[(*it2).first];
									
									Vector3f v = object.vertices.get(entry.first);
									vertices.put(3 * vertices_index, v.x);
									vertices.put(3 * vertices_index +1, v.y);
									vertices.put(3 * vertices_index +2, v.z);
									vertices_index++;
									
									// copy texture coordinates 
									if (uv != null)
									{
//										uv->x =  (*it2).second.x;
//										uv->y =  (*it2).second.y;
										uv.put(3 * uv_index, entry.x);
										uv.put(3 * uv_index + 1, entry.y);
//										++uv;
										uv_index++;
									}


									if (0x1 == type && tmp-1 == m)
									{
										// if this is a closed line repeat its beginning now
										it2  = /*surface.entries.begin()*/ 0;
										entry = surface.entries.get(it2);
									}
									else {
										++it2;
										entry = surface.entries.get(it2);
									}

									// second point
//									*vertices++ = object.vertices[(*it2).first];
									v = object.vertices.get(entry.first);
									vertices.put(3 * vertices_index, v.x);
									vertices.put(3 * vertices_index +1, v.y);
									vertices.put(3 * vertices_index +2, v.z);
									vertices_index++;

									if (uv != null)
									{
//										uv->x =  (*it2).second.x;
//										uv->y =  (*it2).second.y;
										uv.put(3 * uv_index, entry.x);
										uv.put(3 * uv_index + 1, entry.y);
										++uv_index;
									}
								}
							}
						}
					}
				}

				// Now apply catmull clark subdivision if necessary. We split meshes into
				// materials which is not done by AC3D during smoothing, so we need to
				// collect all meshes using the same material group.
				if (object.subDiv != 0)	{
					if (configEvalSubdivision) {
//						boost::scoped_ptr<Subdivider> div(Subdivider::Create(Subdivider::CATMULL_CLARKE));
						Subdivider div = Subdivider.create(Subdivider.CATMULL_CLARKE);
						if(DefaultLogger.LOG_OUT)
							DefaultLogger.info("AC3D: Evaluating subdivision surface: "+object.name);

//						std::vector<aiMesh*> cpy(meshes.size()-oldm,NULL);
//						div->Subdivide(&meshes[oldm],cpy.size(),&cpy.front(),object.subDiv,true);
//						std::copy(cpy.begin(),cpy.end(),meshes.begin()+oldm);
						Mesh[] cpy = new Mesh[meshes.size() - oldm];
						Mesh[] smesh = new Mesh[cpy.length];
						for(int i = oldm; i < meshes.size(); i++)
							smesh[i - oldm] = meshes.get(i);
						div.subdivide(smesh, cpy.length, cpy, object.subDiv, true);
						for(int i = oldm; i < meshes.size(); i++)
							meshes.set(i, cpy[i-oldm]);
						// previous meshes are deleted vy Subdivide().
					}
					else {
						DefaultLogger.info("AC3D: Letting the subdivision surface untouched due to my configuration: "
							+object.name);
					}
				}
			}
		}

		if (object.name.length() > 0)
			node.mName = (object.name);
		else
		{
			// generate a name depending on the type of the node
			switch (object.type)
			{
			case ACObject.Group:
//				node->mName.length = ::sprintf(node->mName.data,"ACGroup_%i",groups++);
				node.mName = String.format("ACGroup_%i",groups++);
				break;
			case ACObject.Poly:
//				node->mName.length = ::sprintf(node->mName.data,"ACPoly_%i",polys++);
				node.mName = String.format("ACPoly_%i",polys++);
				break;
			case ACObject.Light:
//				node->mName.length = ::sprintf(node->mName.data,"ACLight_%i",lights++);
				node.mName = String.format("ACLight_%i",lights++);
				break;

				// there shouldn't be more than one world, but we don't care
			case ACObject.World: 
//				node->mName.length = ::sprintf(node->mName.data,"ACWorld_%i",worlds++);
				node.mName = String.format("ACWorld_%i",worlds++);
				break;
			}
		}


		// setup the local transformation matrix of the object
		// compute the transformation offset to the parent node
//		node->mTransformation = aiMatrix4x4 ( object.rotation );
		node.mTransformation.load(object.rotation);

		if (object.type == ACObject.Group || object.numRefs == 0)
		{
			node.mTransformation.m30 = object.translation.x;
			node.mTransformation.m31 = object.translation.y;
			node.mTransformation.m32 = object.translation.z;
		}

		// add children to the object
		if (object.children.size() > 0)
		{
//			node->mNumChildren = object.children.size();
			node.mChildren = new Node[object.children.size()/*node->mNumChildren*/];
			for (int i = 0; i < node.mChildren.length;++i)
			{
				node.mChildren[i] = convertObjectSection(object.children.get(i),meshes,outMaterials,materials,node);
			}
		}

		return node;
		
	}

	// -------------------------------------------------------------------
	/** Convert a material
	 *  @param object Current object
	 *  @param matSrc Source material description
	 *  @param matDest Destination material to be filled */
	void convertMaterial(ACObject object, ACMaterial matSrc, Material matDest){
		String s;

		if (matSrc.name.length() > 0)
		{
			s = (matSrc.name);
			matDest.addProperty(s, Material.AI_MATKEY_NAME, 0, 0);
		}
		if (object.texture.length() > 0)
		{
			s = (object.texture);
			matDest.addProperty(s,Material._AI_MATKEY_TEXTURE_BASE, TextureType.aiTextureType_DIFFUSE.ordinal(), 0);

			// UV transformation
			if (1.f != object.texRepeat.x || 1.f != object.texRepeat.y ||
				object.texOffset.x != 0f  || object.texOffset.y != 0f)
			{
				UVTransform transform = new UVTransform();
				transform.mScaling.set(object.texRepeat);
				transform.mTranslation.set(object.texOffset);
				matDest.addProperty(transform,Material._AI_MATKEY_UVTRANSFORM_BASE, TextureType.aiTextureType_DIFFUSE.ordinal(), 0);
			}
		}

		matDest.addProperty(matSrc.rgb,Material.AI_MATKEY_COLOR_DIFFUSE, 0, 0);
		matDest.addProperty(matSrc.amb,Material.AI_MATKEY_COLOR_AMBIENT, 0, 0);
		matDest.addProperty(matSrc.emis,Material.AI_MATKEY_COLOR_EMISSIVE, 0, 0);
		matDest.addProperty(matSrc.spec,Material.AI_MATKEY_COLOR_SPECULAR, 0, 0);

		int n;
		if (matSrc.shin != 0f)
		{
			n = ShadingMode.aiShadingMode_Phong.ordinal() + 1;
			matDest.addProperty(matSrc.shin,Material.AI_MATKEY_SHININESS, 0, 0);
		}
		else n = ShadingMode.aiShadingMode_Gouraud.ordinal() + 1;
		matDest.addProperty(n, Material.AI_MATKEY_SHADING_MODEL, 0, 0);

		float f = 1.f - matSrc.trans;
		matDest.addProperty(f,Material.AI_MATKEY_OPACITY, 0, 0);
	}
	
	@Override
	public void setupProperties(Importer pImp) {
		configSplitBFCull = pImp.getPropertyInteger(AssimpConfig.AI_CONFIG_IMPORT_AC_SEPARATE_BFCULL,1) != 0 ? true : false;
		configEvalSubdivision =  pImp.getPropertyInteger(AssimpConfig.AI_CONFIG_IMPORT_AC_EVAL_SUBDIVISION,1) !=0 ? true : false;
	}

}
