/*
Open Asset Import Library (assimp)
----------------------------------------------------------------------

Copyright (c) 2006-2012, assimp team
All rights reserved.

Redistribution and use of this software in source and binary forms, 
with or without modification, are permitted provided that the 
following conditions are met:

* Redistributions of source code must retain the above
  copyright notice, this list of conditions and the
  following disclaimer.

* Redistributions in binary form must reproduce the above
  copyright notice, this list of conditions and the
  following disclaimer in the documentation and/or other
  materials provided with the distribution.

* Neither the name of the assimp team, nor the names of its
  contributors may be used to endorse or promote products
  derived from this software without specific prior
  written permission of the assimp team.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT 
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE 
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

----------------------------------------------------------------------
*/
package assimp.importer.cob;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import org.lwjgl.util.vector.Vector3f;

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
import assimp.common.LineSplitter;
import assimp.common.Material;
import assimp.common.MemoryUtil;
import assimp.common.Mesh;
import assimp.common.Node;
import assimp.common.Scene;
import assimp.common.ShadingMode;
import assimp.common.StreamReader;
import assimp.common.TextureType;

/** Importer class to load TrueSpace files (cob,scn) up to v6. 
*
*  Currently relatively limited, loads only ASCII files and needs more test coverage. */
public final class COBImporter extends BaseImporter{

	static final float units[] = {
		1000.f,
		100.f,
		1.f,
		0.001f,
		1.f/0.0254f,
		1.f/0.3048f,
		1.f/0.9144f,
		1.f/1609.344f
	};	

	static final ImporterDesc desc = new ImporterDesc(
		"TrueSpace Object Importer",
		"",
		"",
		"little-endian files only",
		ImporterDesc.aiImporterFlags_SupportTextFlavour | ImporterDesc.aiImporterFlags_SupportBinaryFlavour,
		0,
		0,
		0,
		0,
		"cob scn"
	);
	
	float[] floatTrip = new float[3];
	int _cur;
	ChunkInfo _nfo;
	StreamReader _reader;
	
	int numMeshes;
	int numMaterials;
	int numLights;
	int numCameras;
	
	@Override
	protected boolean canRead(String pFile, InputStream pIOHandler, boolean checkSig) {
		String extension = getExtension(pFile);
		if (extension.equals("cob") || extension.equals("scn")) {
			return true;
		}

		else if ((extension.length() == 0|| checkSig) && pIOHandler != null)	{
			final String tokens[] = {"Caligary"};
			try {
				return searchFileHeaderForToken(pIOHandler,pFile,tokens);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	@Override
	protected ImporterDesc getInfo() { return desc; }

	@SuppressWarnings("resource")
	@Override
	protected void internReadFile(File pFile, Scene pScene) {
		ByteBuffer buf = FileUtils.loadText(pFile, true, AssimpConfig.LOADING_USE_NATIVE_MEMORY);
		StreamReader stream = new StreamReader(buf, true);
		
		// check header;
		byte[] head = new byte[32];
		stream.copyAndAdvance(head, 0, 32);
		if(!AssUtil.equals(head, 0, "Caligari ")){
			throw new DeadlyImportError("COB::Could not found magic id: `Caligari`");
		}
		
		if(DefaultLogger.LOG_OUT){
			DefaultLogger.info("File format tag: " + new String(head, 9, 6));
		}
		
		final boolean isAsciiFile = (head[15]=='A');
		if (head[16]!='L') {
			throw new DeadlyImportError("COB::File is big-endian, which is not supported");
		}
		
		COBScene scene = new COBScene();
		// load data into intermediate structures
		if(isAsciiFile)
			readAsciiFile(scene, stream);
		else
			readBinaryFile(scene, stream);
		
		if(scene.nodes.isEmpty()) {
			throw new DeadlyImportError("COB::No nodes loaded");
		}

		// sort faces by material indices
//		for_each(boost::shared_ptr< Node >& n,scene.nodes) {
		for(COBNode n : scene.nodes){
			if (n.type == COBNode.TYPE_MESH) {
				COBMesh mesh = (COBMesh)(n);
//				for_each(Face& f,mesh.faces) {
				for(COBFace f : mesh.faces){
//					mesh.temp_map[f.material].push_back(&f);
					ArrayDeque<COBFace> deque = mesh.temp_map.get(f.material);
					if(deque == null)
						mesh.temp_map.put(f.material, deque = new ArrayDeque<COBFace>());
					deque.add(f);
				}
			} 
		}

		// count meshes
//		for_each(boost::shared_ptr< Node >& n,scene.nodes) {
		numMeshes = 0;
		for (COBNode n: scene.nodes) {
			if (n.type == COBNode.TYPE_MESH) {
				COBMesh mesh = (COBMesh)n;
				if (mesh.vertex_positions != null && mesh.texture_coords != null) {
					numMeshes += mesh.temp_map.size();
				}
			} 
		}
		pScene.mMeshes = new Mesh[numMeshes];
		pScene.mMaterials = new Material[numMeshes];
		numMeshes = 0;

		int numLights = 0;
		int numCameras = 0;
//		for_each(boost::shared_ptr< Node >& n,scene.nodes) {
		for(COBNode n : scene.nodes){
			if (n.type == COBNode.TYPE_LIGHT) {
//				++pScene->mNumLights;
				++numLights;
			}
			else if (n.type == COBNode.TYPE_CAMERA) {
//				++pScene->mNumCameras;
				++numCameras;
			}
		}

		if (numLights > 0) {
			pScene.mLights  = new Light[numLights];
		}
		if (numCameras > 0) {
			pScene.mCameras = new Camera[numCameras];
		}
//		pScene->mNumLights = pScene->mNumCameras = 0;
		numLights = 0;
		numCameras = 0;

		// resolve parents by their IDs and build the output graph
//		boost::scoped_ptr<Node> root(new Group());
		COBNode root = new COBGroup();
		for(int n = 0; n < scene.nodes.size(); ++n) {
//		for (COBNode nn : scene.nodes) {
			COBNode nn = scene.nodes.get(n);
			if(nn.parent_id==0) {
				root.temp_children.add(nn);
			}

			for(int m = n; m < scene.nodes.size(); ++m) {
//			for(COBNode mm : scene.nodes){
				COBNode mm = scene.nodes.get(m);
				if (mm.parent_id == nn.id) {
					nn.temp_children.add(mm);
				}
			}
		}

		pScene.mRootNode = buildNodes(root,scene,pScene);
		try {
			stream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// ------------------------------------------------------------------------------------------------
	void convertTexture(COBTexture tex, Material out, TextureType type)
	{
//		aiString path( tex->path );
		out.addProperty(tex.path, Material._AI_MATKEY_TEXTURE_BASE,type.ordinal(),0);
		out.addProperty(tex.transform, Material._AI_MATKEY_UVTRANSFORM_BASE, type.ordinal(),0);
	}

	// Conversion to Assimp output format
	
	// -------------------------------------------------------------------
	/** @brief Read from an ascii scene/object file
	 *  @param out Receives output data.
	 *  @param stream Stream to read from. */
	void readAsciiFile(COBScene out, StreamReader stream){
		ChunkInfo ci = new ChunkInfo();
		for(LineSplitter splitter = new LineSplitter(stream);splitter.hasMore();splitter.next()) {

			// add all chunks to be recognized here. /else ../ omitted intentionally.
			if (splitter.match_start("PolH ")) {
				readChunkInfo_Ascii(ci,splitter);
				readPolH_Ascii(out,splitter,ci);
			}
			if (splitter.match_start("BitM ")) {
				readChunkInfo_Ascii(ci,splitter);
				readBitM_Ascii(out,splitter,ci);
			}
			if (splitter.match_start("Mat1 ")) {
				readChunkInfo_Ascii(ci,splitter);
				readMat1_Ascii(out,splitter,ci);
			}
			if (splitter.match_start("Grou ")) {
				readChunkInfo_Ascii(ci,splitter);
				readGrou_Ascii(out,splitter,ci);
			}
			if (splitter.match_start("Lght ")) {
				readChunkInfo_Ascii(ci,splitter);
				readLght_Ascii(out,splitter,ci);
			}
			if (splitter.match_start("Came ")) {
				readChunkInfo_Ascii(ci,splitter);
				readCame_Ascii(out,splitter,ci);
			}
			if (splitter.match_start("Bone ")) {
				readChunkInfo_Ascii(ci,splitter);
				readBone_Ascii(out,splitter,ci);
			}
			if (splitter.match_start("Chan ")) {
				readChunkInfo_Ascii(ci,splitter);
				readChan_Ascii(out,splitter,ci);
			}
			if (splitter.match_start("Unit ")) {
				readChunkInfo_Ascii(ci,splitter);
				readUnit_Ascii(out,splitter,ci);
			}
			if (splitter.match_start("END ")) {
				// we don't need this, but I guess there is a reason this
				// chunk has been implemented into COB for.
				return;
			}
		}
	}

	// -------------------------------------------------------------------
	/** Read from a binary scene/object file
	 *  @param out Receives output data.
	 *  @param stream Stream to read from.  */
	void readBinaryFile(COBScene out, StreamReader reader){
		while(true) {
			String type;
//			 type += reader -> GetI1()
//			,type += reader -> GetI1()
//			,type += reader -> GetI1()
//			,type += reader -> GetI1()
//			;
			byte[] bytes = new byte[4];
			bytes[0] = reader.getI1();
			bytes[1] = reader.getI1();
			bytes[2] = reader.getI1();
			bytes[3] = reader.getI1();
			type = new String(bytes);

			ChunkInfo nfo = new ChunkInfo();
			nfo.version  = reader.getI2()*10;
			nfo.version += reader.getI2();

			nfo.id = reader.getI4();
			nfo.parent_id = reader.getI4();
			nfo.size = reader.getI4();

			if (type.equals("PolH")) {
				readPolH_Binary(out,reader,nfo);
			}
			else if (type.equals("BitM")) {
				readBitM_Binary(out,reader,nfo);
			}
			else if (type.equals("Grou")) {
				readGrou_Binary(out,reader,nfo);
			}
			else if (type.equals("Lght")) {
				readLght_Binary(out,reader,nfo);
			}
			else if (type.equals("Came")) {
				readCame_Binary(out,reader,nfo);
			}
			else if (type.equals("Mat1")) {
				readMat1_Binary(out,reader,nfo);
			}
		/*	else if (type == "Bone") {
				ReadBone_Binary(out,*reader,nfo);
			}
			else if (type == "Chan") {
				ReadChan_Binary(out,*reader,nfo);
			}*/
			else if (type.equals("Unit")) {
				readUnit_Binary(out,reader,nfo);
			}
			else if (type.equals("OLay")) {
				// ignore layer index silently.
				if(nfo.size != /*static_cast<unsigned int>*/(-1) ) {
					reader.incPtr(nfo.size);
				}
				else{ 
					unsupportedChunk_Binary(reader,nfo,type);
					return;
				}
			}
			else if (type.equals("END ")) 
				return;
			else unsupportedChunk_Binary(reader,nfo,type);
		}
	}

	Node buildNodes(COBNode root,COBScene scin,Scene fill){
		Node nd = new Node();
		Vector3f vec3 = new Vector3f();
		int nd_mNumMeshes = 0;
		nd.mName = root.name;
		if(root.transform != null)
			nd.mTransformation.load(root.transform);
		else
			nd.mTransformation.setIdentity();

		// Note to everybody believing Voodoo is appropriate here:
		// I know polymorphism, run as fast as you can ;-)
		if (COBNode.TYPE_MESH == root.type) {
			COBMesh ndmesh = (COBMesh)(root);
			if (ndmesh.vertex_positions != null && ndmesh.texture_coords != null) {

//				typedef std::pair<unsigned int,Mesh::FaceRefList> Entry;
//				for_each(Entry& reflist,ndmesh.temp_map) {
				for(Int2ObjectMap.Entry<ArrayDeque<COBFace>> reflist : ndmesh.temp_map.int2ObjectEntrySet()){
					{	// create mesh
						int n = 0;
//						for_each(Face* f, reflist.second) {
						for (COBFace f : reflist.getValue()){
							n += f.indices.size();
						}
						if (n == 0) {
							continue;
						}
						Mesh outmesh = fill.mMeshes[numMeshes++] = new Mesh();
						++nd_mNumMeshes;

//						outmesh->mVertices = new aiVector3D[n];
//						outmesh->mTextureCoords[0] = new aiVector3D[n];
						outmesh.mVertices = MemoryUtil.createFloatBuffer(n * 3, AssimpConfig.MESH_USE_NATIVE_MEMORY);
						outmesh.mTextureCoords[0] = MemoryUtil.createFloatBuffer(n * 3, AssimpConfig.MESH_USE_NATIVE_MEMORY);

//						outmesh->mFaces = new aiFace[reflist.second.size()]();
						outmesh.mFaces = new Face[reflist.getValue().size()];
						int numFaces = 0;
						int numIndices = 0;
//						for_each(Face* f, reflist.second) {
						for (COBFace f : reflist.getValue()) {
							if (f.indices.isEmpty()) {
								continue;
							}

							Face fout = outmesh.mFaces[numFaces++] = Face.createInstance(f.indices.size());
//							fout.mIndices = new unsigned int[f->indices.size()];

//							for_each(VertexIndex& v, f->indices) {
							for (VertexIndex v : f.indices) {
								if (v.pos_idx >= ndmesh.getVertexCount()) {
									throw new DeadlyImportError("COB::Position index out of range");
								}
								if (v.uv_idx >= ndmesh.getTexCoordCount()) {
									throw new DeadlyImportError("COB::UV index out of range");
								}
//								outmesh->mVertices[outmesh->mNumVertices] = ndmesh.vertex_positions[ v.pos_idx ];
								MemoryUtil.arraycopy(ndmesh.vertex_positions, v.pos_idx * 3, outmesh.mVertices, outmesh.mNumVertices * 3, 3);
//								outmesh->mTextureCoords[0][outmesh->mNumVertices] = aiVector3D( 
//									ndmesh.texture_coords[ v.uv_idx ].x,
//									ndmesh.texture_coords[ v.uv_idx ].y,
//									0.f
//								);
								FloatBuffer buf = outmesh.mTextureCoords[0];
								int index = outmesh.mNumVertices * 3;
								buf.put(index++, ndmesh.texture_coords.get(2 * v.uv_idx));
								buf.put(index++, ndmesh.texture_coords.get(2 * v.uv_idx + 1));
								buf.put(index++, 0);

//								fout.mIndices[fout.mNumIndices++] = outmesh->mNumVertices++;
								fout.set(numIndices++, outmesh.mNumVertices++);
							}
						}
						outmesh.mMaterialIndex = numMaterials;
					}{	// create material
						COBMaterial min = null;
//						for_each(Material& m, scin.materials) {
						for (COBMaterial m : scin.materials) {
							if (m.parent_id == ndmesh.id && m.matnum == reflist.getIntKey()) {
								min = m;
								break;
							}
						}
//						boost::scoped_ptr<Material> defmat;
						if(min == null) {
							if(DefaultLogger.LOG_OUT)
							DefaultLogger.debug("Could not resolve material index " + reflist.getIntKey() +" - creating default material for this slot");
							min=new COBMaterial();
						}

						Material mat = new Material();
						fill.mMaterials[numMaterials++] = mat;

//						aiString s(format("#mat_")<<fill->mNumMeshes<<"_"<<min->matnum);
						String s = "#mat_" + numMaterials + "_" + min.matnum;
						mat.addProperty(s,Material.AI_MATKEY_NAME, 0, 0);

						if((ndmesh.draw_flags & COBMesh.WIRED)!= 0) {
							mat.addProperty(1, Material.AI_MATKEY_ENABLE_WIREFRAME,0,0);
						}

						{	int shader;
							switch(min.shader) 
							{
							case COBMaterial.FLAT:
								shader = ShadingMode.aiShadingMode_Gouraud.ordinal() + 1;
								break;

							case COBMaterial.PHONG:
								shader = ShadingMode.aiShadingMode_Phong.ordinal() + 1;
								break;

							case COBMaterial.METAL:
								shader = ShadingMode.aiShadingMode_CookTorrance.ordinal() + 1;
								break;

							default:
//								ai_assert(false); // shouldn't be here
								throw new AssertionError();
							}
							mat.addProperty(shader, Material.AI_MATKEY_SHADING_MODEL, 0, 0);
							if(shader != ShadingMode.aiShadingMode_Gouraud.ordinal() - 1) {
								mat.addProperty(min.exp, Material.AI_MATKEY_SHININESS, 0,0);
							}
						}

						mat.addProperty(min.ior,Material.AI_MATKEY_REFRACTI, 0,0);
						vec3.set(min.r, min.g, min.b);
						mat.addProperty(vec3,Material.AI_MATKEY_COLOR_DIFFUSE, 0,0);

//						aiColor3D c = aiColor3D(min->rgb)*min->ks;
						vec3.scale(min.ks);
						mat.addProperty(vec3,Material.AI_MATKEY_COLOR_SPECULAR, 0, 0);

//						c = aiColor3D(min->rgb)*min->ka;
						vec3.set(min.r * min.ka, min.g * min.ka, min.b * min.ka);
						mat.addProperty(vec3,Material.AI_MATKEY_COLOR_AMBIENT,0,0);

						// convert textures if some exist.
						if(min.tex_color != null) {
							convertTexture(min.tex_color,mat,TextureType.aiTextureType_DIFFUSE);
						}
						if(min.tex_env != null) {
							convertTexture(min.tex_env  ,mat,TextureType.aiTextureType_UNKNOWN);
						}
						if(min.tex_bump != null) {
							convertTexture(min.tex_bump ,mat,TextureType.aiTextureType_HEIGHT);
						}
					}
				}
			}
		}
		else if (COBNode.TYPE_LIGHT == root.type) {
			COBLight ndlight = (COBLight)(root);
			Light outlight = fill.mLights[numLights++] = new Light();
			
			outlight.mName =(ndlight.name);
			outlight.mColorDiffuse.x = outlight.mColorAmbient.x = outlight.mColorSpecular.x = ndlight.r;
			outlight.mColorDiffuse.y = outlight.mColorAmbient.y = outlight.mColorSpecular.y = ndlight.g;
			outlight.mColorDiffuse.z = outlight.mColorAmbient.z = outlight.mColorSpecular.z = ndlight.b;

			outlight.mAngleOuterCone = (float)Math.toRadians(ndlight.angle);
			outlight.mAngleInnerCone = (float)Math.toRadians(ndlight.inner_angle);

			// XXX
			outlight.mType = ndlight.ltype==COBLight.SPOT ? LightSourceType.aiLightSource_SPOT : LightSourceType.aiLightSource_DIRECTIONAL;
		}
		else if (COBNode.TYPE_CAMERA == root.type) {
			COBCamera ndcam = (COBCamera)(root);
			Camera outcam = fill.mCameras[numCameras++] = new Camera();

			outcam.mName = (ndcam.name);
		}

		// add meshes
		if (nd_mNumMeshes > 0) { // mMeshes must be NULL if count is 0
			nd.mMeshes = new int[nd_mNumMeshes];
			for(int i = 0; i < nd_mNumMeshes;++i) {
				nd.mMeshes[i] = numMeshes-i-1;
			}
		}

		// add children recursively
		nd.mChildren = new Node[root.temp_children.size()];
		int numChildren = 0;
//		for_each(Node* n, root.temp_children) {
		for (COBNode n : root.temp_children) {
			(nd.mChildren[numChildren++] = buildNodes(n,scin,fill)).mParent = nd;
		}

		return nd;
	}
		
	// ASCII file support
	void unsupportedChunk_Ascii(LineSplitter splitter, ChunkInfo nfo, String name){
		String error = ("Encountered unsupported chunk: ") +  name +
				" [version: " + nfo.version + ", size: " + nfo.size + "]";

			// we can recover if the chunk size was specified.
			if(nfo.size != (-1)) {
				DefaultLogger.error(error);

				// (HACK) - our current position in the stream is the beginning of the
				// head line of the next chunk. That's fine, but the caller is going
				// to call ++ on `splitter`, which we need to swallow to avoid 
				// missing the next line.
				splitter.get_stream().incPtr(nfo.size);
				splitter.swallow_next_increment();
			}
			else throw new DeadlyImportError("COB::" + error);
	}
	
	void readChunkInfo_Ascii(ChunkInfo out, LineSplitter splitter){
		final String[] all_tokens = new String[8];
		splitter.get_tokens(all_tokens);

		String version = all_tokens[1];
		out.version = (version.charAt(1)-'0')*100+(version.charAt(3)-'0')*10+(version.charAt(4)-'0');
		out.id	= Integer.parseInt(all_tokens[3]); //strtoul10(all_tokens[3]);
		out.parent_id = Integer.parseInt(all_tokens[5]); //strtoul10();
		out.size = Integer.parseInt(all_tokens[7]); //strtol10(all_tokens[7]);
	}
	
	void readBasicNodeInfo_Ascii(COBNode msh, LineSplitter splitter, ChunkInfo nfo){
		for(;splitter.hasMore();splitter.next()) {
			if (splitter.match_start("Name")) {
//				msh.name = std::string(splitter[1]);
				// TODO
				msh.name = splitter.get_token(1).replace(',', '_');

				// make nice names by merging the dupe count
//				std::replace(msh.name.begin(),msh.name.end(),
//					',','_');
			}
			else if (splitter.match_start("Transform")) {
				for(int y = 0; y < 4 /*&& ++splitter*/; ++y) {
					splitter.next();
//					const char* s = splitter->c_str();
					StringTokenizer tokenizer = new StringTokenizer(splitter.get().toString(), " \t");
					for(int x = 0; x < 4; ++x) {
//						SkipSpaces(&s);
//						msh.transform[y][x] = fast_atof(&s);
						msh.transform.set(y, x, Float.parseFloat(tokenizer.nextToken()), false);
					}
				}
				// we need the transform chunk, so we won't return until we have it.
				return;
			}
		}
	}
	/*template <typename T> */void readFloat3Tuple_Ascii(String in){
		StringTokenizer tokenizer = new StringTokenizer(in, " \t,");
		for(int i = 0; i < 3; i++){
			floatTrip[i] = Float.parseFloat(tokenizer.nextToken());
		}
	}
	
	static int strtoul10(String str){
		return Integer.parseInt(str);
	}
	
	static float fast_atof(String str){
		return Float.parseFloat(str);
	}
	
	// ------------------------------------------------------------------------------------------------
	void readGrou_Ascii(COBScene out, LineSplitter splitter, ChunkInfo nfo)
	{
		if(nfo.version > 1) {
			unsupportedChunk_Ascii(splitter,nfo,"Grou");
			return;
		}

		COBGroup msh;
		out.nodes.add(msh = new COBGroup());
//		Group& msh = (Group&)(*out.nodes.back().get());
		msh.set(nfo);

		readBasicNodeInfo_Ascii(msh,splitter,nfo);
		splitter.next();
	}

	void readPolH_Ascii(COBScene out, LineSplitter splitter, ChunkInfo nfo){
		if(nfo.version > 8) {
			unsupportedChunk_Ascii(splitter,nfo,"PolH");
			return;
		}

		COBMesh msh;
		out.nodes.add(msh = new COBMesh());
//		Mesh& msh = (Mesh&)(*out.nodes.back().get());
		msh.set(nfo);;

		readBasicNodeInfo_Ascii(msh, splitter.next(),nfo);

		// the chunk has a fixed order of components, but some are not interesting of us so
		// we're just looking for keywords in arbitrary order. The end of the chunk is
		// either the last `Face` or the `DrawFlags` attribute, depending on the format ver.
		for(;splitter.hasMore();splitter.next()) {
			if (splitter.match_start("World Vertices")) {
				final int cnt = strtoul10(splitter.get_token(2));
//				msh.vertex_positions.resize(cnt);
				msh.vertex_positions = MemoryUtil.createFloatBuffer(cnt * 3, AssimpConfig.MESH_USE_NATIVE_MEMORY);

				for(int cur = 0;cur < cnt /*&& ++splitter*/;++cur) {
					splitter.next();
//					const char* s = splitter->c_str();
					StringTokenizer token = new StringTokenizer(splitter.get().toString(), " \t");

//					aiVector3D& v = msh.vertex_positions[cur]; 
//
//					SkipSpaces(&s);
//					v.x = fast_atof(&s);
//					SkipSpaces(&s);
//					v.y = fast_atof(&s);
//					SkipSpaces(&s);
//					v.z = fast_atof(&s);
					
					int index = cur * 3;
					msh.vertex_positions.put(index++, fast_atof(token.nextToken()));
					msh.vertex_positions.put(index++, fast_atof(token.nextToken()));
					msh.vertex_positions.put(index++, fast_atof(token.nextToken()));
				}
			}
			else if (splitter.match_start("Texture Vertices")) {
				final int cnt = strtoul10(splitter.get_token(2));
//				msh.texture_coords.resize(cnt);
				msh.texture_coords = MemoryUtil.createFloatBuffer(cnt * 2, AssimpConfig.MESH_USE_NATIVE_MEMORY);

				for(int cur = 0;cur < cnt /*&& ++splitter*/;++cur) {
					splitter.next();
//					const char* s = splitter->c_str();
					StringTokenizer token = new StringTokenizer(splitter.get().toString(), " \t");

//					aiVector2D& v = msh.texture_coords[cur]; 
//
//					SkipSpaces(&s);
//					v.x = fast_atof(&s);
//					SkipSpaces(&s);
//					v.y = fast_atof(&s);
					
					int index = cur * 2;
					msh.texture_coords.put(index++, fast_atof(token.nextToken()));
					msh.texture_coords.put(index++, fast_atof(token.nextToken()));
				}
			}
			else if (splitter.match_start("Faces")) {
				final int cnt = strtoul10(splitter.get_token(1));
//				msh.faces.reserve(cnt);

				for(int cur = 0; cur < cnt /*&& ++splitter*/ ;++cur) {
					if (splitter.match_start("Hole")) {
						logWarn_Ascii(splitter,"Skipping unsupported `Hole` line");
						continue;
					}

					if (!splitter.match_start("Face")) {
						throw new DeadlyImportError("Expected Face line");
					}

					COBFace face;
					msh.faces.add(face = new COBFace());
//					Face& face = msh.faces.back();

//					face.indices.resize(strtoul10(splitter.get_token(2)));
					int size = strtoul10(splitter.get_token(2));
					for(int i = 0; i < size; i++){
						face.indices.add(new VertexIndex());
					}
					face.flags = strtoul10(splitter.get_token(4));
					face.material = strtoul10(splitter.get_token(6));

//					const char* s = (++splitter)->c_str();
//					StringTokenizer token = new StringTokenizer(splitter.get().toString(), " \t");
					StringBuilder seq = splitter.get();
					splitter.next();
					int curr = 0;
					for(int i = 0; i < face.indices.size(); ++i) {
//						if(!token.hasMoreTokens()){
//						if(!SkipSpaces(&s)) {
//							throw new DeadlyImportError("Expected EOL token in Face entry");
//						}
						
//						String t = token.nextToken();
//						int s = 0;
//						if ('<' != t.charAt(s++)) {
//							throw new DeadlyImportError("Expected < token in Face entry");
//						}
//						int dot = t.indexOf(',');
//						int end = t.
//						face.indices[i].pos_idx = strtoul10(s,&s);
//						if (',' != *s++) {
//							throw new DeadlyImportError("Expected , token in Face entry");
//						}
//						face.indices[i].uv_idx = strtoul10(s,&s);
//						if ('>' != *s++) {
//							throw new DeadlyImportError("Expected < token in Face entry");
//						}
						
						int first = seq.indexOf("<", curr);
						int second = seq.indexOf(",", first);
						int last = seq.indexOf(">", second);
						
						if(first < 0 || second  < 0 || last < 0)
							throw new DeadlyImportError("Expected < token in Face entry");
						VertexIndex vi = face.indices.get(i);
						vi.pos_idx = strtoul10(seq.substring(first + 1, second));
						vi.uv_idx  = strtoul10(seq.substring(second + 1, last));
						curr = last + 1;
					}
				}
				if (nfo.version <= 4) {
					break;
				}
			}
			else if (splitter.match_start("DrawFlags")) {
				msh.draw_flags = strtoul10(splitter.get_token(1));
				break;
			}
		}
	}
	void readBitM_Ascii(COBScene out, LineSplitter splitter, ChunkInfo nfo){
		if(nfo.version > 1) {
			unsupportedChunk_Ascii(splitter,nfo,"BitM");
			return;
		}
	/*
		"\nThumbNailHdrSize %ld"
		"\nThumbHeader: %02hx 02hx %02hx "
		"\nColorBufSize %ld"		
		"\nColorBufZipSize %ld"		
		"\nZippedThumbnail: %02hx 02hx %02hx "
	*/

		final int head = strtoul10(splitter.get_token(1)); splitter.next();
		if (head != /*sizeof(Bitmap::BitmapHeader)*/ 0 ) {
			logWarn_Ascii(splitter,"Unexpected ThumbNailHdrSize, skipping this chunk");
			return;
		}

		/*union {
			Bitmap::BitmapHeader data;
			char opaq[sizeof Bitmap::BitmapHeader()];
		};*/
//		ReadHexOctets(opaq,head,(++splitter)[1]);
	}
	
	void readMat1_Ascii(COBScene out, LineSplitter splitter, ChunkInfo nfo){
		if(nfo.version > 8) {
			unsupportedChunk_Ascii(splitter,nfo,"Mat1");
			return;
		}

		splitter.next();
		if (!splitter.match_start("mat# ")) {
			logWarn_Ascii(splitter,"Expected `mat#` line in `Mat1` chunk " + nfo.id);
			return;
		}

		COBMaterial mat;
		out.materials.add(mat  = new COBMaterial());
//		Material& mat = out.materials.back();
		mat.set(nfo);

		mat.matnum = Integer.parseInt(splitter.get_token(1)); //strtoul10(splitter[1]);
//		++splitter;
		splitter.next();

		if (!splitter.match_start("shader: ")) {
			logWarn_Ascii(splitter,
				"Expected `mat#` line in `Mat1` chunk " + nfo.id);
			return;
		}
//		std::string shader = std::string(splitter[1]);
		String shader = splitter.get_token(1);
		shader = shader.substring(0,shader.indexOf(" \t"));

		if (shader.equals("metal")) {
			mat.shader = COBMaterial.METAL;
		}
		else if (shader.equals("phong")) {
			mat.shader = COBMaterial.PHONG;
		}
		else if (!shader.equals("flat")) {
			logWarn_Ascii(splitter,
				"Unknown value for `shader` in `Mat1` chunk " + nfo.id);
		}

//		++splitter;
		splitter.next();
		if (!splitter.match_start("rgb ")) {
			logWarn_Ascii(splitter,/*format()<<*/
				"Expected `rgb` line in `Mat1` chunk " + nfo.id);
		}

//		const char* rgb = splitter[1];
		readFloat3Tuple_Ascii(/*mat.rgb,&rgb*/ splitter.get_token(1));
		mat.r = floatTrip[0];
		mat.g = floatTrip[1];
		mat.b = floatTrip[2];

//		++splitter;
		splitter.next();
		if (!splitter.match_start("alpha ")) {
			logWarn_Ascii(splitter,/*format()<<*/
				"Expected `alpha` line in `Mat1` chunk " + nfo.id);
		}

		final String[] tokens = new String[10];
		splitter.get_tokens(tokens);

		mat.alpha	= Float.parseFloat( tokens[1] );
		mat.ka		= Float.parseFloat( tokens[3] );
		mat.ks		= Float.parseFloat( tokens[5] );
		mat.exp		= Float.parseFloat( tokens[7] );
		mat.ior		= Float.parseFloat( tokens[9] );
	}
	void ReadGrou_Ascii(COBScene out, LineSplitter splitter, ChunkInfo nfo){
		if(nfo.version > 5) {
			unsupportedChunk_Ascii(splitter,nfo,"Bone");
			return;
		}
		
		COBBone msh;
		out.nodes.add(msh = new COBBone());
//		Bone& msh = (Bone&)(*out.nodes.back().get());
		msh.set(nfo);

		readBasicNodeInfo_Ascii(msh,splitter,nfo); splitter.next();

		// TODO
	}
	
	void readBone_Ascii(COBScene out, LineSplitter splitter, ChunkInfo nfo){
		if(nfo.version > 5) {
			unsupportedChunk_Ascii(splitter,nfo,"Bone");
			return;
		}

		COBBone msh;
		out.nodes.add(msh = new COBBone());
//		Bone& msh = (Bone&)(*out.nodes.back().get());
		msh.set(nfo);;

		readBasicNodeInfo_Ascii(msh,/*++splitter*/splitter,nfo); splitter.next();

		// TODO
	}
	
	void readCame_Ascii(COBScene out, LineSplitter splitter, ChunkInfo nfo){
		if(nfo.version > 2) {
			unsupportedChunk_Ascii(splitter,nfo,"Came");
			return;
		}

		COBCamera msh;
		out.nodes.add(msh = new COBCamera());
//		Camera& msh = (Camera&)(*out.nodes.back().get());
		msh.set(nfo);

		readBasicNodeInfo_Ascii(msh,splitter.next(),nfo);

		// skip the next line, we don't know this differenciation between a
		// standard camera and a panoramic camera.
//		++splitter;
		splitter.next();
	}
	void readLght_Ascii(COBScene out, LineSplitter splitter, ChunkInfo nfo){
		if(nfo.version > 8) {
			unsupportedChunk_Ascii(splitter,nfo,"Lght");
			return;
		}

		COBLight msh;
		out.nodes.add(msh = new COBLight());
//		Light& msh = (Light&)(*out.nodes.back().get());
		msh.set(nfo);;

		readBasicNodeInfo_Ascii(msh,splitter.next(),nfo);

		if (splitter.match_start("Infinite ")) {
			msh.ltype = COBLight.INFINITE;
		}
		else if (splitter.match_start("Local ")) {
			msh.ltype = COBLight.LOCAL;
		}
		else if (splitter.match_start("Spot ")) {
			msh.ltype = COBLight.SPOT;
		}
		else {
			if(DefaultLogger.LOG_OUT)
				logWarn_Ascii(splitter,/*format()<<*/
					"Unknown kind of light source in `Lght` chunk " + nfo.id + " : " + splitter.get());
			msh.ltype = COBLight.SPOT;
		}
		
//		++splitter;
		splitter.next();
		if (!splitter.match_start("color ")) {
			logWarn_Ascii(splitter,/*format()<<*/
				"Expected `color` line in `Lght` chunk " + nfo.id);
		}

//		const char* rgb = splitter[1];
		StringTokenizer tokenizer = new StringTokenizer(splitter.get().toString(), " \t,");
		tokenizer.nextToken();  // skip the first token
//		readFloat3Tuple_Ascii(/*msh.color ,&rgb*/ tokens[1]);
		msh.r = Float.parseFloat(tokenizer.nextToken());
		msh.g = Float.parseFloat(tokenizer.nextToken());
		msh.b = Float.parseFloat(tokenizer.nextToken());

//		SkipSpaces(&rgb);
//		if (strncmp(rgb,"cone angle",10)) {
		if(!tokenizer.nextToken().equals("cone") || !tokenizer.nextToken().equals("angle")){
			logWarn_Ascii(splitter,/*format()<<*/
				"Expected `cone angle` entity in `color` line in `Lght` chunk " + nfo.id);
		}
//		SkipSpaces(rgb+10,&rgb);
//		msh.angle = fast_atof(&rgb);
		msh.angle = Float.parseFloat(tokenizer.nextToken());

//		SkipSpaces(&rgb);
//		if (strncmp(rgb,"inner angle",11)) {
		if(!tokenizer.nextToken().equals("inner") || !tokenizer.nextToken().equals("angle")){
			logWarn_Ascii(splitter,/*format()<<*/
				"Expected `inner angle` entity in `color` line in `Lght` chunk " + nfo.id);
		}
//		SkipSpaces(rgb+11,&rgb);
//		msh.inner_angle = fast_atof(&rgb);
		msh.inner_angle = Float.parseFloat(tokenizer.nextToken());
	}
	
	void readUnit_Ascii(COBScene out, LineSplitter splitter, ChunkInfo nfo){
		if(nfo.version > 1) {
			unsupportedChunk_Ascii(splitter,nfo,"Unit");
			return;
		}
//		++splitter;
		splitter.next();
		if (!splitter.match_start("Units ")) {
			logWarn_Ascii(splitter,/*format()<<*/
				"Expected `Units` line in `Unit` chunk " + nfo.id);
			return;
		}

		// parent chunks preceede their childs, so we should have the
		// corresponding chunk already.
//		for_each(boost::shared_ptr< Node >& nd, out.nodes) {
		for (COBNode nd : out.nodes) {
			if (nd.id == nfo.parent_id) {
				final int t=Integer.parseInt(splitter.get_token(1));
			
//				nd.unit_scale = t>=/*sizeof(units)/sizeof(units[0])*/units.length?(
//						logWarn_Ascii(splitter,/*format()<<*/t + " is not a valid value for `Units` attribute in `Unit chunk` " + nfo.id)
//					,1.f):units[t];
					
				if(t >= units.length){
					logWarn_Ascii(splitter,/*format()<<*/t + " is not a valid value for `Units` attribute in `Unit chunk` " + nfo.id);
					nd.unit_scale = 1.f;
				}else{
					nd.unit_scale = units[t];
				}
				return;
			}
		}
		if(DefaultLogger.LOG_OUT)
		logWarn_Ascii(splitter,/*format()<<*/"`Unit` chunk " + nfo.id + " is a child of "
			 + nfo.parent_id + " which does not exist");
	}
	
	void readChan_Ascii(COBScene out, LineSplitter splitter, ChunkInfo nfo){
		if(nfo.version > 8) {
			unsupportedChunk_Ascii(splitter,nfo,"Chan");
		}
	}

	static void logWarn_Ascii  (LineSplitter splitter, String message){
		if(DefaultLogger.LOG_OUT)
			DefaultLogger.warn(message +" [at line " +  splitter.get_index() + "]");
	}
	
	static void logError_Ascii (LineSplitter splitter, String message){
		DefaultLogger.error(message +" [at line " +  splitter.get_index() + "]");
	}
	
	static void logInfo_Ascii  (LineSplitter splitter, String message){
		if(DefaultLogger.LOG_OUT)
			DefaultLogger.info(message +" [at line " +  splitter.get_index() + "]");
	}
	
	static void logDebug_Ascii (LineSplitter splitter, String message){
		if(DefaultLogger.LOG_OUT)
			DefaultLogger.debug(message +" [at line " +  splitter.get_index() + "]");
	}
	
	static void logWarn_Ascii  (String message){
		if(DefaultLogger.LOG_OUT)
			DefaultLogger.warn(message);
	}
	
	static void logError_Ascii ( String message){
		DefaultLogger.error(message);
	}
	
	static void logInfo_Ascii  (String message){
		if(DefaultLogger.LOG_OUT)
			DefaultLogger.info(message);
	}
	
	static void logDebug_Ascii (String message){
		if(DefaultLogger.LOG_OUT)
			DefaultLogger.debug(message);
	}


	// Binary file support

	void unsupportedChunk_Binary(StreamReader reader, ChunkInfo nfo, String name){
		final String error = ("Encountered unsupported chunk: ") +  name +
				" [version: " + nfo.version + ", size: " + nfo.size + "]";

			// we can recover if the chunk size was specified.
			if(nfo.size != /*static_cast<unsigned int>*/(-1)) {
				DefaultLogger.error(error);
				reader.incPtr(nfo.size);
			}
			else throw new DeadlyImportError("COB:: " + error);
	}
	
	String readString_Binary(StreamReader reader){
		int length = reader.getI2();
		byte[] bytes = new byte[length];
		for(int i = 0; i< length; i++){
			bytes[i] = reader.getI1();
		}
		
		return new String(bytes);
	}
	void readBasicNodeInfo_Binary(COBNode msh, StreamReader reader, ChunkInfo nfo){
		final int dupes = reader.getI2();
		msh.name = readString_Binary(reader);

		msh.name = msh.name + '_' + dupes;

		// skip local axes for the moment
		reader.incPtr(48);

		msh.transform.setIdentity();
		for( int y = 0; y < 3; ++y) {
			for( int x =0; x < 4; ++x) {
//				msh.transform[y][x] = reader.GetF4();
				msh.transform.set(y, x, reader.getF4(), false);
			}
		}
	}
	
	void begin_chunk_guard(ChunkInfo nfo, StreamReader reader){
		_nfo = nfo;
		_reader = reader;
		_cur = reader.getCurrentPos();
	}
	
	void end_chunk_guard(){
		// don't do anything if the size is not given
		if(_nfo.size != /*static_cast<unsigned int>*/(-1)) {
			_reader.incPtr(/*static_cast<int>*/(_nfo.size)-_reader.getCurrentPos()+_cur);
		}
		
		_nfo = null;
		_reader = null;
		_cur = 0;
	}

	void readPolH_Binary(COBScene out, StreamReader reader, ChunkInfo nfo){
		if(nfo.version > 8) {
			unsupportedChunk_Binary(reader,nfo,"PolH");
			return;
		}
//		const chunk_guard cn(nfo,reader);
		
		try{
			begin_chunk_guard(nfo, reader);
			
			COBMesh msh;
			out.nodes.add(msh = new COBMesh());
//			Mesh& msh = (Mesh&)(*out.nodes.back().get());
			msh.set(nfo);

			readBasicNodeInfo_Binary(msh,reader,nfo);

//			msh.vertex_positions.resize(reader.GetI4());
			int count = reader.getI4();
			msh.vertex_positions = MemoryUtil.createFloatBuffer(count * 3, AssimpConfig.MESH_USE_NATIVE_MEMORY);
//			for_each(aiVector3D& v,msh.vertex_positions) {
//				v.x = reader.GetF4();
//				v.y = reader.GetF4();
//				v.z = reader.GetF4();
//			}
			FloatBuffer buf = msh.vertex_positions;
			while(count-- > 0){
				buf.put(reader.getF4());
				buf.put(reader.getF4());
				buf.put(reader.getF4());
			}
			buf.flip();
			

//			msh.texture_coords.resize(reader.GetI4());
//			for_each(aiVector2D& v,msh.texture_coords) {
//				v.x = reader.GetF4();
//				v.y = reader.GetF4();
//			}
			count = reader.getI4();
			buf = msh.texture_coords = MemoryUtil.createFloatBuffer(count * 2, AssimpConfig.MESH_USE_NATIVE_MEMORY);
			while(count-- > 0){
				buf.put(reader.getF4());
				buf.put(reader.getF4());
			}
			buf.flip();
			final int numf = reader.getI4();
//			msh.faces.reserve(numf);
			for(int i = 0; i < numf; ++i) {
				// XXX backface culling flag is 0x10 in flags

				// hole?
				boolean hole;
				if ((hole = (reader.getI1() & 0x08) != 0)) {
					// XXX Basically this should just work fine - then triangulator
					// should output properly triangulated data even for polygons
					// with holes. Test data specific to COB is needed to confirm it.
					if (msh.faces.isEmpty()) {
						throw new DeadlyImportError(("A hole is the first entity in the `PolH` chunk with id ") + nfo.id);
					}	
				}
				else msh.faces.add(new COBFace());
//				Face& f = msh.faces.back();
				COBFace f = AssUtil.back(msh.faces);

				final int num = reader.getI2();
//				f.indices.reserve(f.indices.size() + num);

				if(!hole) {
					f.material = reader.getI2();
					f.flags = 0;
				}

				List<VertexIndex> tmpList = new ArrayList<VertexIndex>(num);
				for(int x = 0; x < num; ++x) {
					VertexIndex v;
					tmpList.add(v = new VertexIndex());

//					VertexIndex& v = f.indices.back();
					v.pos_idx = reader.getI4();
					v.uv_idx = reader.getI4();
				}

				if(hole) {
//					std::reverse(f.indices.rbegin(),f.indices.rbegin()+num);
					Collections.reverse(tmpList);
				}
				
				f.indices.addAll(tmpList);
				tmpList = null;
			}
			if (nfo.version>4) {
				msh.draw_flags = reader.getI4();	
			}
			
			if(nfo.version>5 && nfo.version<8)
				reader.getI4();
		}finally{
			end_chunk_guard();
		}
	}
	
	void readBitM_Binary(COBScene out, StreamReader reader, ChunkInfo nfo){
		if(nfo.version > 1) {
			unsupportedChunk_Binary(reader,nfo,"BitM");
			return;
		}

//		const chunk_guard cn(nfo,reader);

		try{
			begin_chunk_guard(nfo, reader);
			final int len = reader.getI4();
			reader.incPtr(len);

			reader.getI4();
			reader.incPtr(reader.getI4());
		}finally{
			end_chunk_guard();
		}
	}
	
	void readMat1_Binary(COBScene out, StreamReader reader, ChunkInfo nfo){
		if(nfo.version > 8) {
			unsupportedChunk_Binary(reader,nfo,"Mat1");
			return;
		}

//		const chunk_guard cn(nfo,reader);
		try{
			begin_chunk_guard(nfo, reader);
			
			COBMaterial mat;
			out.materials.add(mat = new COBMaterial());
//			Material& mat = out.materials.back();
			mat.set(nfo);

			mat.matnum = reader.getI2();
			switch(reader.getI1()) {
				case 'f':
					// TODO
					mat.type = Character.toString((char)COBMaterial.FLAT);
					break;
				case 'p':
					// TODO
					mat.type = Character.toString((char)COBMaterial.PHONG);
					break;
				case 'm':
					// TODO
					mat.type = Character.toString((char)COBMaterial.METAL);
					break;
				default:
					logError_Ascii(("Unrecognized shader type in `Mat1` chunk with id ") + nfo.id);
					// TODO
					mat.type = Character.toString((char)COBMaterial.FLAT);
			}

			switch(reader.getI1()) {
				case 'f':
					mat.autofacet = COBMaterial.FACETED;
					break;
				case 'a':
					mat.autofacet = COBMaterial.AUTOFACETED;
					break;
				case 's':
					mat.autofacet = COBMaterial.SMOOTH;
					break;
				default:
					logError_Ascii(("Unrecognized faceting mode in `Mat1` chunk with id ") + nfo.id);
					mat.autofacet = COBMaterial.FACETED;
			}
			mat.autofacet_angle = /*static_cast<float>*/(reader.getI1());

			mat.r = reader.getF4();
			mat.g = reader.getF4();
			mat.b = reader.getF4();

			mat.alpha = reader.getF4();
			mat.ka    = reader.getF4();
			mat.ks    = reader.getF4();
			mat.exp   = reader.getF4();
			mat.ior   = reader.getF4();

//			char id[2];
//			id[0] = reader.GetI1(),id[1] = reader.GetI1();
			byte id0 = reader.getI1();
			byte id1 = reader.getI1();

			if (id0 == 'e' && id1 == ':') {
//				mat.tex_env.reset(new Texture());
				mat.tex_env = new COBTexture();

				reader.getI1();
				mat.tex_env.path = readString_Binary(reader);

				// advance to next texture-id
				id0 = reader.getI1();
				id1 = reader.getI1();
			}

			if (id0 == 't' && id1 == ':') {
//				mat.tex_color.reset(new Texture());
				mat.tex_color = new COBTexture();

				reader.getI1();
				mat.tex_color.path =readString_Binary(reader);

				mat.tex_color.transform.mTranslation.x = reader.getF4();
				mat.tex_color.transform.mTranslation.y = reader.getF4();

				mat.tex_color.transform.mScaling.x = reader.getF4();
				mat.tex_color.transform.mScaling.y = reader.getF4();

				// advance to next texture-id
				id0 = reader.getI1();
				id1 = reader.getI1();
			}

			if (id0 == 'b' && id1 == ':') {
//				mat.tex_bump.reset(new Texture());
				mat.tex_bump = new COBTexture();

				reader.getI1();
				mat.tex_bump.path = readString_Binary(reader);

				mat.tex_bump.transform.mTranslation.x = reader.getF4();
				mat.tex_bump.transform.mTranslation.y = reader.getF4();

				mat.tex_bump.transform.mScaling.x = reader.getF4();
				mat.tex_bump.transform.mScaling.y = reader.getF4();

				// skip amplitude for I don't know its purpose.
				reader.getF4();
			}
			reader.incPtr(-2);
		}finally{
			end_chunk_guard();
		}
	}
	
	void readCame_Binary(COBScene out, StreamReader reader, ChunkInfo nfo){
		if(nfo.version > 2) {
			unsupportedChunk_Binary(reader,nfo,"Came");
			return;
		}

//		const chunk_guard cn(nfo,reader);
		try{
			begin_chunk_guard(nfo, reader);
			
			COBCamera msh;
			out.nodes.add(msh = new COBCamera());
//			Camera& msh = (Camera&)(*out.nodes.back().get());
			msh.set(nfo);

			readBasicNodeInfo_Binary(msh,reader,nfo);

			// the rest is not interesting for us, so we skip over it.
			if(nfo.version > 1) {
				if (reader.getI2()==512) {
					reader.incPtr(42);
				}
			}
		}finally{
			end_chunk_guard();
		}
	}
	
	void readLght_Binary(COBScene out, StreamReader reader, ChunkInfo nfo){
		if(nfo.version > 2) {
			unsupportedChunk_Binary(reader,nfo,"Lght");
			return;
		}

		try{
//			const chunk_guard cn(nfo,reader);
			begin_chunk_guard(nfo, reader);

			COBLight msh;
			out.nodes.add(msh = new COBLight());
//			Light& msh = (Light&)(*out.nodes.back().get());
			msh.set(nfo);;

			readBasicNodeInfo_Binary(msh,reader,nfo);
		}finally{
			end_chunk_guard();
		}
	}
	
	void readGrou_Binary(COBScene out, StreamReader reader, ChunkInfo nfo){
		if(nfo.version > 2) {
			unsupportedChunk_Binary(reader,nfo,"Grou");
			return;
		}

		try{
//			const chunk_guard cn(nfo,reader);
			begin_chunk_guard(nfo, reader);

			COBGroup msh;
			out.nodes.add(msh = new COBGroup());
//			Group& msh = (Group&)(*out.nodes.back().get());
			msh.set(nfo);

			readBasicNodeInfo_Binary(msh,reader,nfo);
		}finally{
			end_chunk_guard();
		}
	}
	
	void readUnit_Binary(COBScene out, StreamReader reader, ChunkInfo nfo){
		if(nfo.version > 1) {
			unsupportedChunk_Binary(reader,nfo,"Unit");
		}

		try{
//			 const chunk_guard cn(nfo,reader);
			begin_chunk_guard(nfo, reader);

			// parent chunks preceede their childs, so we should have the
			// corresponding chunk already.
//			for_each(boost::shared_ptr< Node >& nd, out.nodes) {
			for (COBNode nd : out.nodes) {
				if (nd.id == nfo.parent_id) {
					int t=reader.getI2();
//					nd->unit_scale = t>=sizeof(units)/sizeof(units[0])?(
//						LogWarn_Ascii(format()<<t<<" is not a valid value for `Units` attribute in `Unit chunk` "<<nfo.id)
//						,1.f):units[t];
					if(t >= units.length){
						logWarn_Ascii(t + " is not a valid value for `Units` attribute in `Unit chunk` " + nfo.id);
						nd.unit_scale =1.f;
					}else{
						nd.unit_scale = units[t];
					}
					
					return;
				}
			}
			
			logWarn_Ascii("`Unit` chunk " + nfo.id + " is a child of "
				+nfo.parent_id+" which does not exist");
		}finally{
			end_chunk_guard();
		}
	}
}
