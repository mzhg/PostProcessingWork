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
package assimp.importer.blender;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.zip.ZipInputStream;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import assimp.common.AssUtil;
import assimp.common.AssimpConfig;
import assimp.common.BaseImporter;
import assimp.common.Camera;
import assimp.common.DeadlyImportError;
import assimp.common.DefaultLogger;
import assimp.common.Face;
import assimp.common.ImporterDesc;
import assimp.common.Light;
import assimp.common.LightSourceType;
import assimp.common.Material;
import assimp.common.MemoryUtil;
import assimp.common.Mesh;
import assimp.common.Node;
import assimp.common.Scene;
import assimp.common.StreamReader;
import assimp.common.Texture;
import assimp.common.TextureType;

/** Load blenders official binary format. The actual file structure (the `DNA` how they
 *  call it is outsourced to BlenderDNA.cpp/BlenderDNA.h. This class only performs the
 *  conversion from intermediate format to aiScene. */
public final class BlenderImporter extends BaseImporter{

	static final ImporterDesc blenderDesc = new ImporterDesc(
		"Blender 3D Importer \nhttp://www.blender3d.org",
		"",
		"",
		"No animation support yet",
		ImporterDesc.aiImporterFlags_SupportBinaryFlavour,
		0,
		0,
		2,
		50,
		"blend"
	);
	
	final BlenderModifierShowcase modifier_cache;
	
	public BlenderImporter() {
		modifier_cache = new BlenderModifierShowcase();
	}
	
	@Override
	protected boolean canRead(String pFile, InputStream pIOHandler, boolean checkSig) {
		final String extension = getExtension(pFile);
		if (extension.equals("blend")) {
			return true;
		}

		else if ((extension.length() == 0|| checkSig) && pIOHandler != null)	{
			// note: this won't catch compressed files
			String tokens[] = {"BLENDER"};
			try {
				return searchFileHeaderForToken(pIOHandler,pFile,tokens);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return false;
	}
	
	@Override
	public void getExtensionList(Set<String> app) {
		app.add("blend");
	}

	@Override
	protected ImporterDesc getInfo() { return blenderDesc;}

	@Override
	protected void internReadFile(File pFile, Scene pScene) {
		FileDatabase file  = new FileDatabase();
		try(BufferedInputStream stream = new BufferedInputStream(new FileInputStream(pFile))){
			byte[] magic = new byte[7];
			stream.read(magic);
			InputStream stream2 = null;
			if(!AssUtil.equals(magic, 0, "BLENDER")){
				if (magic[0] != 0x1f || (magic[1] & 0xFF) != 0x8b) {
					throw new DeadlyImportError("BLENDER magic bytes are missing, couldn't find GZIP header either");
				}

				if(DefaultLogger.LOG_OUT)
					DefaultLogger.debug("Found no BLENDER magic word but a GZIP header, might be a compressed file");
				if (magic[2] != 8) {
					throw new DeadlyImportError("Unsupported GZIP compression method");
				}
				
				//Re-create a zipStream.
				stream.close();
				stream2 = new BufferedInputStream(new ZipInputStream(new FileInputStream(pFile)));
				
				// .. and retry
				stream2.read(magic);
				if (!AssUtil.equals(magic,0,"BLENDER")) {
					stream2.close();
					throw new DeadlyImportError("Found no BLENDER magic word in decompressed GZIP file");
				}
			}
			
			if(stream2 == null)
				stream2 = stream;
			
			file.i64bit = (stream2.read() == '-');
			file.little = (stream2.read() == 'v');
			
			stream2.read(magic, 0, 3);
			if(DefaultLogger.LOG_OUT)
				DefaultLogger.info(AssUtil.makeString("Blender version is ",(char)magic[0],".",new String(magic, 1, 2),
			" (64bit: ",file.i64bit?"true":"false",
			", little endian: ",file.little?"true":"false",")"));
			
			parseBlendFile(file,stream2);
			stream2.close();
		}catch(Exception e){
			e.printStackTrace();
		}

		BLEScene scene = new BLEScene();
		extractScene(scene,file);

		convertBlendFile(pScene,scene,file);
	}
	
	// --------------------
	void parseBlendFile(FileDatabase out, InputStream stream) throws IOException{
		out.reader = new StreamReader(stream,out.little);

		DNAParser dna_reader = new DNAParser(out);
		DNA dna = null;

//		out.entries.reserve(128); 
		out.entries.ensureCapacity(128);
		{ // even small BLEND files tend to consist of many file blocks
			SectionParser parser = new SectionParser(out.reader,out.i64bit);

			// first parse the file in search for the DNA and insert all other sections into the database
			while (/*(parser.Next(),1)*/ true) {
				parser.next();
				FileBlockHead head = parser.getCurrent();

				if (head.id.equals("ENDB")) {
					break; // only valid end of the file
				}
				else if (head.id.equals("DNA1")) {
					dna_reader.parse();
					dna = dna_reader.getDNA();
					continue;
				}

				out.entries.add(head);
			}
		}
		if (dna == null) {
			throw new DeadlyImportError("SDNA not found");
		}

//		std::sort(out.entries.begin(),out.entries.end());
		Collections.sort(out.entries);
	}

	// --------------------
	void extractScene(BLEScene out, FileDatabase file){
		FileBlockHead block = null;
//		std::map<std::string,size_t>::const_iterator it = file.dna.indices.find("Scene");
//		if (it == file.dna.indices.end()) {
//			throw new DeadlyImportError("There is no `Scene` structure record");
//		}
		int index = file.dna.indices.getInt("Scene");
		if(index == -1){
			throw new DeadlyImportError("There is no `Scene` structure record");
		}

		Structure ss = file.dna.structures.get(index);

		// we need a scene somewhere to start with. 
//		for_each(const FileBlockHead& bl,file.entries) {
		for (FileBlockHead bl : file.entries){

			// Fix: using the DNA index is more reliable to locate scenes
			//if (bl.id == "SC") {

			if (bl.dna_index == /*(*it).second*/ index) {
				block = bl;
				break;
			}
		}

		if (block == null) {
			throw new DeadlyImportError("There is not a single `Scene` record to load");
		}

		file.reader.setCurrentPos(block.start);
		ss.convert(out,file);

//	#ifndef ASSIMP_BUILD_BLENDER_NO_STATS
		if(!AssimpConfig.ASSIMP_BUILD_BLENDER_NO_STATS){
		DefaultLogger.info(AssUtil.makeString(
			"(Stats) Fields read: "	,file.stats().fields_read,
			", pointers resolved: "	,file.stats().pointers_resolved,  
			", cache hits: "        ,file.stats().cache_hits,  
			", cached objects: "	,file.stats().cached_objects
		));
		}
//	#endif
	}

	// --------------------
	void convertBlendFile(Scene out, BLEScene in, FileDatabase file){
		ConversionData conv = new ConversionData(file);

		// FIXME it must be possible to take the hierarchy directly from
		// the file. This is terrible. Here, we're first looking for
		// all objects which don't have parent objects at all -
//		std::deque<const Object*> no_parents;
		ArrayList<BLEObject> no_parents = new ArrayList<BLEObject>();
//		for (boost::shared_ptr<Base> cur = boost::static_pointer_cast<Base> ( in.base.first ); cur; cur = cur.next) {
		for (Base cur = (Base)in.base.first; cur != null;  cur = cur.next){
			if (cur.object != null) {
				if(cur.object.parent == null) {
					no_parents.add(cur.object);
				}
				else conv.objects.add(cur.object);
			}
		}
//		for (boost::shared_ptr<Base> cur = in.basact; cur; cur = cur.next) {
		for (Base cur = in.basact; cur != null; cur = cur.next){
			if (cur.object != null) {
				if(cur.object.parent != null) {
					conv.objects.add(cur.object);
				}
			}
		}

		if (no_parents.isEmpty()) {
			throw new DeadlyImportError("Expected at least one object with no parent");
		}

		Node root = out.mRootNode = new Node("<BlenderRoot>");

//		root.mNumChildren = static_cast<unsigned int>(no_parents.size());
//		root.mChildren = new aiNode*[root.mNumChildren]();
		root.mChildren = new Node[no_parents.size()];
		for (int i = 0; i < root.mChildren.length; ++i) {
			root.mChildren[i] = convertNode(in, no_parents.get(i), conv, null);	
			root.mChildren[i].mParent = root;
		}

		buildMaterials(conv);

//		if (conv.meshes.size() > 0) {
//			out.mMeshes = new aiMesh*[out.mNumMeshes = static_cast<unsigned int>( conv.meshes.size() )];
//			std::copy(conv.meshes.begin(),conv.meshes.end(),out.mMeshes);
//			conv.meshes.dismiss();
//		}
		out.mMeshes = AssUtil.toArray(conv.meshes, Mesh.class);

//		if (conv.lights.size() > 0) {
//			out.mLights = new aiLight*[out.mNumLights = static_cast<unsigned int>( conv.lights.size() )];
//			std::copy(conv.lights.begin(),conv.lights.end(),out.mLights);
//			conv.lights.dismiss();
//		}
		out.mLights = AssUtil.toArray(conv.lights, Light.class);

//		if (conv.cameras.size() > 0) {
//			out.mCameras = new aiCamera*[out.mNumCameras = static_cast<unsigned int>( conv.cameras.size() )];
//			std::copy(conv.cameras.begin(),conv.cameras.end(),out.mCameras);
//			conv.cameras.dismiss();
//		}
		out.mCameras = AssUtil.toArray(conv.cameras, Camera.class);

//		if (conv.materials.size() > 0) {
//			out.mMaterials = new aiMaterial*[out.mNumMaterials = static_cast<unsigned int>( conv.materials.size() )];
//			std::copy(conv.materials.begin(),conv.materials.end(),out.mMaterials);
//			conv.materials.dismiss();
//		}
		out.mMaterials = AssUtil.toArray(conv.materials, Material.class);

//		if (conv.textures.size() > 0) {
//			out.mTextures = new aiTexture*[out.mNumTextures = static_cast<unsigned int>( conv.textures.size() )];
//			std::copy(conv.textures.begin(),conv.textures.end(),out.mTextures);
//			conv.textures.dismiss();
			
//		}
		out.mTextures = AssUtil.toArray(conv.textures, Texture.class);

		// acknowledge that the scene might come out incomplete
		// by Assimps definition of `complete`: blender scenes
		// can consist of thousands of cameras or lights with
		// not a single mesh between them.
		if (out.mMeshes == null) {
			out.mFlags |= Scene.AI_SCENE_FLAGS_INCOMPLETE;
		}
	}
	
	// --------------------
	Node convertNode(BLEScene in, BLEObject obj, ConversionData conv_data, Matrix4f parentTransform){
//		std::deque<const Object*> children;
		ArrayList<BLEObject> children = new ArrayList<BLEObject>();
//		for(std::set<const Object*>::iterator it = conv_data.objects.begin(); it != conv_data.objects.end() ;) {
		Iterator<BLEObject> it = conv_data.objects.iterator();
		while(it.hasNext()){
			BLEObject object = it.next();
			if (object.parent == obj) {
				children.add(object);
				it.remove();
			}
		}

//		ScopeGuard<aiNode> node(new aiNode(obj.id.name+2)); // skip over the name prefix 'OB'
		Node node = new Node(new String(obj.id.name, 2, obj.id.name.length - 2));
		if (obj.data != null) {
			switch (obj.type)
			{
			case BLEObject. Type_EMPTY:
				break; // do nothing


				// supported object types
			case BLEObject. Type_MESH: {
				final int old = conv_data.meshes.size();

				checkActualType(obj.data,"Mesh");
				convertMesh(in,obj,(BLEMesh)(obj.data),conv_data,conv_data.meshes);

				if (conv_data.meshes.size() > old) {
					node.mMeshes = new int[/*node.mNumMeshes = static_cast<unsigned int>*/(conv_data.meshes.size()-old)];
					for (int i = 0; i < node.mMeshes.length; ++i) {
						node.mMeshes[i] = i + old;
					}
				}}
				break;
			case BLEObject. Type_LAMP: {
				checkActualType(obj.data,"Lamp");
				Light mesh = convertLight(in,obj,(Lamp)(obj.data),conv_data);

				if (mesh != null) {
					conv_data.lights.add(mesh);
				}}
				break;
			case BLEObject. Type_CAMERA: {
				checkActualType(obj.data,"Camera");
				Camera mesh = convertCamera(in,obj,(BLECamera)(obj.data),conv_data);

				if (mesh != null) {
					conv_data.cameras.add(mesh);
				}}
				break;


				// unsupported object types / log, but do not break
			case BLEObject. Type_CURVE:
				notSupportedObjectType(obj,"Curve");
				break;
			case BLEObject. Type_SURF:
				notSupportedObjectType(obj,"Surface");
				break;
			case BLEObject. Type_FONT:
				notSupportedObjectType(obj,"Font");
				break;
			case BLEObject. Type_MBALL:
				notSupportedObjectType(obj,"MetaBall");
				break;
			case BLEObject. Type_WAVE:
				notSupportedObjectType(obj,"Wave");
				break;
			case BLEObject. Type_LATTICE:
				notSupportedObjectType(obj,"Lattice");
				break;

				// invalid or unknown type
			default:
				break;
			}
		}

//		for(unsigned int x = 0; x < 4; ++x) {
//			for(unsigned int y = 0; y < 4; ++y) {
//				node.mTransformation[y][x] = obj.obmat[x][y];
//			}
//		}
		
		node.mTransformation.load(obj.obmat);

//		aiMatrix4x4 m = parentTransform;
//		m = m.Inverse();
//
//		node.mTransformation = m*node.mTransformation;
		if(parentTransform != null){
			Matrix4f dest = node.mTransformation;
			Matrix4f.invert(parentTransform, dest);
			Matrix4f.mul(parentTransform, dest, dest);
		}
		
		if (children.size() > 0) {
//			node.mNumChildren = static_cast<unsigned int>(children.size());
//			aiNode** nd = node.mChildren = new aiNode*[node.mNumChildren]();
//			for_each (const Object* nobj,children) {
//				*nd = ConvertNode(in,nobj,conv_data,node.mTransformation * parentTransform);
//				(*nd++).mParent = node;
//			}
			
			int nd = 0;
			node.mChildren = new Node[children.size()];
			for(BLEObject nobj : children){
				Matrix4f transform;
				if(parentTransform == null)
					transform = node.mTransformation;
				else{
					transform = Matrix4f.mul(node.mTransformation, parentTransform, null);
				}
				
				node.mChildren[nd] = convertNode(in, nobj, conv_data, transform);
			}
		}

		// apply modifiers
		modifier_cache.applyModifiers(node,conv_data,in,obj);
		return node;
	}

	// --------------------
	void convertMesh(BLEScene in, BLEObject obj, BLEMesh mesh, ConversionData conv_data, ArrayList<Mesh> temp){
		BlenderBMeshConverter BMeshConverter = new BlenderBMeshConverter( mesh );
		if ( BMeshConverter.containsBMesh( ) )
		{
			mesh = BMeshConverter.triangulateBMesh( );
		}

//		typedef std::pair<const int,size_t> MyPair;
		if ((mesh.totface == 0 && mesh.totloop == 0) || mesh.totvert == 0) {
			return;
		}

		// some sanity checks
		if (mesh.totface > mesh.mface.size() ){
			throw new DeadlyImportError("Number of faces is larger than the corresponding array");
		}

		if (mesh.totvert > mesh.mvert.size()) {
			throw new DeadlyImportError("Number of vertices is larger than the corresponding array");
		}

		if (mesh.totloop > mesh.mloop.size()) {
			throw new DeadlyImportError("Number of vertices is larger than the corresponding array");
		}

		// collect per-submesh numbers
//		std::map<int,size_t> per_mat;
//		std::map<int,size_t> per_mat_verts;
		Int2IntOpenHashMap per_mat = new Int2IntOpenHashMap();
		Int2IntOpenHashMap per_mat_verts = new Int2IntOpenHashMap();
		for (int i = 0; i < mesh.totface; ++i) {

			MFace mf = mesh.mface.get(i);
//			per_mat[ mf.mat_nr ]++;
//			per_mat_verts[ mf.mat_nr ] += mf.v4?4:3;
			per_mat.put(mf.mat_nr, per_mat.get(mf.mat_nr) + 1);
			per_mat_verts.put(mf.mat_nr, per_mat_verts.get(mf.mat_nr) + (mf.v4 != 0 ?4:3));
		}

		for (int i = 0; i < mesh.totpoly; ++i) {
			MPoly mp = mesh.mpoly.get(i);
//			per_mat[ mp.mat_nr ]++;
//			per_mat_verts[ mp.mat_nr ] += mp.totloop;
			per_mat.put(mp.mat_nr, per_mat.get(mp.mat_nr) + 1);
			per_mat_verts.put(mp.mat_nr, per_mat_verts.get(mp.mat_nr) + mp.totloop);
		}

		// ... and allocate the corresponding meshes
		final int old = temp.size();
//		temp.reserve(temp.size() + per_mat.size());
		temp.ensureCapacity(temp.size() + per_mat.size());

//		std::map<size_t,size_t> mat_num_to_mesh_idx;
		Int2IntOpenHashMap mat_num_to_mesh_idx = new Int2IntOpenHashMap();
//		for_each(MyPair& it, per_mat) {
		for (Int2IntMap.Entry it : per_mat.int2IntEntrySet()){

//			mat_num_to_mesh_idx[it.getIntKey()] = temp.size();
			mat_num_to_mesh_idx.put(it.getIntKey(), temp.size());
			Mesh out;
			temp.add(out = new Mesh());

//			aiMesh* out = temp.back();
//			out.mVertices = new aiVector3D[per_mat_verts[it.getIntKey()]];
//			out.mNormals  = new aiVector3D[per_mat_verts[it.getIntKey()]];
			final int bufSize = per_mat_verts.get(it.getIntKey()) * 3;
			out.mVertices = MemoryUtil.createFloatBuffer(bufSize, AssimpConfig.MESH_USE_NATIVE_MEMORY);
			out.mNormals  = MemoryUtil.createFloatBuffer(bufSize, AssimpConfig.MESH_USE_NATIVE_MEMORY);

			//out.mNumFaces = 0
			//out.mNumVertices = 0
//			out.mFaces = new aiFace[it.second]();
			out.mFaces = new Face[it.getIntValue()];

			// all submeshes created from this mesh are named equally. this allows
			// curious users to recover the original adjacency.
//			out.mName = aiString(mesh.id.name+2);  
			out.mName = new String(mesh.id.name, 2, mesh.id.name.length - 2);
				// skip over the name prefix 'ME'

			// resolve the material reference and add this material to the set of
			// output materials. The (temporary) material index is the index 
			// of the material entry within the list of resolved materials.
			if (!mesh.mat.isEmpty()) {

				if (it.getIntKey() >= mesh.mat.size() ) {
					throw new DeadlyImportError("Material index is out of range");
				}

				BLEMaterial mat = mesh.mat.get(it.getIntKey());
//				const std::deque< boost::shared_ptr<Material> >::iterator has = std::find(
//						conv_data.materials_raw.begin(),
//						conv_data.materials_raw.end(),mat
//				);
				
				int has = conv_data.materials_raw.indexOf(mat);

				if (has >= 0/* != conv_data.materials_raw.end()*/) {
					out.mMaterialIndex = has/*static_cast<unsigned int>( std::distance(conv_data.materials_raw.begin(),has))*/;
				}
				else {
					out.mMaterialIndex = /*static_cast<unsigned int>*/( conv_data.materials_raw.size() );
					conv_data.materials_raw.add(mat);
				}
			}
			else out.mMaterialIndex = /*static_cast<unsigned int>*/( -1 );
		}

		for (int i = 0; i < mesh.totface; ++i) {

			MFace mf = mesh.mface.get(i);

			final Mesh out = temp.get(mat_num_to_mesh_idx.get(mf.mat_nr));
			int numFaces = AssUtil.findfirstNull(out.mFaces);
			Face f = out.mFaces[numFaces++] = Face.createInstance(mf.v4 != 0 ?4:3);

//			f.mIndices = new unsigned int[ f.mNumIndices = mf.v4?4:3 ];
//			aiVector3D* vo = out.mVertices + out.mNumVertices;
//			aiVector3D* vn = out.mNormals + out.mNumVertices;
			int v_offset = out.mNumVertices * 3;
			int n_offset = v_offset;
			FloatBuffer vo = out.mVertices;  /*vo.position(offset);*/
			FloatBuffer vn = out.mNormals;   /*vn.position(vn);*/

			// XXX we can't fold this easily, because we are restricted
			// to the member names from the BLEND file (v1,v2,v3,v4) 
			// which are assigned by the genblenddna.py script and
			// cannot be changed without breaking the entire
			// import process.

			if (mf.v1 >= mesh.totvert) {
				throw new DeadlyImportError("Vertex index v1 out of range");
			}
			MVert v = mesh.mvert.get(mf.v1);
//			vo.x = v.co[0];
//			vo.y = v.co[1];
//			vo.z = v.co[2];
			vo.put(v_offset++, v.co[0]);
			vo.put(v_offset++, v.co[1]);
			vo.put(v_offset++, v.co[2]);
//			vn.x = v.no[0];
//			vn.y = v.no[1];
//			vn.z = v.no[2];
			vn.put(n_offset++, v.no[0]);
			vn.put(n_offset++, v.no[1]);
			vn.put(n_offset++, v.no[2]);
//			f.mIndices[0] = out.mNumVertices++;
			f.set(0, out.mNumVertices++);
//			++vo;
//			++vn;

			//	if (f.mNumIndices >= 2) {
			if (mf.v2 >= mesh.totvert) {
				throw new DeadlyImportError("Vertex index v2 out of range");
			}
			v = mesh.mvert.get(mf.v2);
//			vo.x = v.co[0];
//			vo.y = v.co[1];
//			vo.z = v.co[2];
			vo.put(v_offset++, v.co[0]);
			vo.put(v_offset++, v.co[1]);
			vo.put(v_offset++, v.co[2]);
//			vn.x = v.no[0];
//			vn.y = v.no[1];
//			vn.z = v.no[2];
			vn.put(n_offset++, v.no[0]);
			vn.put(n_offset++, v.no[1]);
			vn.put(n_offset++, v.no[2]);
//			f.mIndices[1] = out.mNumVertices++;
			f.set(1, out.mNumVertices++);
//			++vo;
//			++vn;

			if (mf.v3 >= mesh.totvert) {
				throw new DeadlyImportError("Vertex index v3 out of range");
			}
			//	if (f.mNumIndices >= 3) {
//			v = &mesh.mvert[mf.v3];
			v = mesh.mvert.get(mf.v3);
//			vo.x = v.co[0];
//			vo.y = v.co[1];
//			vo.z = v.co[2];
			vo.put(v_offset++, v.co[0]);
			vo.put(v_offset++, v.co[1]);
			vo.put(v_offset++, v.co[2]);
//			vn.x = v.no[0];
//			vn.y = v.no[1];
//			vn.z = v.no[2];
			vn.put(n_offset++, v.no[0]);
			vn.put(n_offset++, v.no[1]);
			vn.put(n_offset++, v.no[2]);
			f.set(2, out.mNumVertices++);
//			++vo;
//			++vn;

			if (mf.v4 >= mesh.totvert) {
				throw new DeadlyImportError("Vertex index v4 out of range");
			}
			//	if (f.mNumIndices >= 4) {
			if (mf.v4 != 0) {
//				v = &mesh.mvert[mf.v4];
				v = mesh.mvert.get(mf.v4);
//				vo.x = v.co[0];
//				vo.y = v.co[1];
//				vo.z = v.co[2];
				vo.put(v_offset++, v.co[0]);
				vo.put(v_offset++, v.co[1]);
				vo.put(v_offset++, v.co[2]);
//				vn.x = v.no[0];
//				vn.y = v.no[1];
//				vn.z = v.no[2];
//				f.mIndices[3] = out.mNumVertices++;
				vn.put(n_offset++, v.no[0]);
				vn.put(n_offset++, v.no[1]);
				vn.put(n_offset++, v.no[2]);
				f.set(3, out.mNumVertices++);
//				++vo;
//				++vn;

				out.mPrimitiveTypes |= Mesh.aiPrimitiveType_POLYGON;
			}
			else out.mPrimitiveTypes |= Mesh.aiPrimitiveType_TRIANGLE;

			//	}
			//	}
			//	}
		}

		for (int i = 0; i < mesh.totpoly; ++i) {
			
			MPoly mf = mesh.mpoly.get(i);
			
			final Mesh out = temp.get(mat_num_to_mesh_idx.get(mf.mat_nr));
			int numFaces = AssUtil.findfirstNull(out.mFaces);
			Face f = out.mFaces[numFaces++] = Face.createInstance(mf.totloop);
			
//			f.mIndices = new unsigned int[ f.mNumIndices = mf.totloop ];
//			aiVector3D* vo = out.mVertices + out.mNumVertices;
//			aiVector3D* vn = out.mNormals + out.mNumVertices;
			int v_offset = out.mNumVertices * 3;
			int n_offset = v_offset;
			FloatBuffer vo = out.mVertices;  /*vo.position(offset);*/
			FloatBuffer vn = out.mNormals;   /*vn.position(vn);*/
			
			// XXX we can't fold this easily, because we are restricted
			// to the member names from the BLEND file (v1,v2,v3,v4)
			// which are assigned by the genblenddna.py script and
			// cannot be changed without breaking the entire
			// import process.
			for (int j = 0;j < mf.totloop; ++j)
			{
				MLoop loop = mesh.mloop.get(mf.loopstart + j);

				if (loop.v >= mesh.totvert) {
					throw new DeadlyImportError("Vertex index out of range");
				}

				MVert v = mesh.mvert.get(loop.v);
				
//				vo.x = v.co[0];
//				vo.y = v.co[1];
//				vo.z = v.co[2];
//				vn.x = v.no[0];
//				vn.y = v.no[1];
//				vn.z = v.no[2];
//				f.mIndices[j] = out.mNumVertices++;
				
				vo.put(v_offset++, v.co[0]);
				vo.put(v_offset++, v.co[1]);
				vo.put(v_offset++, v.co[2]);
				vn.put(n_offset++, v.no[0]);
				vn.put(n_offset++, v.no[1]);
				vn.put(n_offset++, v.no[2]);
				f.set(j, out.mNumVertices++);
//				++vo;
//				++vn;
				
			}
			if (mf.totloop == 3)
			{
				out.mPrimitiveTypes |= Mesh.aiPrimitiveType_TRIANGLE;
			}
			else
			{
				out.mPrimitiveTypes |= Mesh.aiPrimitiveType_POLYGON;
			}
		}
		
		Object2IntMap<Mesh> mesh_to_num_faces = new Object2IntOpenHashMap<Mesh>();
		// collect texture coordinates, they're stored in a separate per-face buffer
		if (mesh.mtface.size() > 0 || mesh.mloopuv.size() > 0) {
			if (mesh.totface > ( mesh.mtface.size())) {
				throw new DeadlyImportError("Number of UV faces is larger than the corresponding UV face array (#1)");
			}
			
//			for (std::vector<aiMesh*>::iterator it = temp.begin()+old; it != temp.end(); ++it) {
			for (int m = old; m < temp.size(); m ++ ){
				Mesh it = temp.get(m);
//				ai_assert((*it).mNumVertices && (*it).mNumFaces);

//				(*it).mTextureCoords[0] = new aiVector3D[(*it).mNumVertices];
//				(*it).mNumFaces = (*it).mNumVertices = 0;
				it.mTextureCoords[0] = MemoryUtil.createFloatBuffer(it.mNumVertices * 3,AssimpConfig.MESH_USE_NATIVE_MEMORY);
				it.mNumVertices = 0;
				mesh_to_num_faces.put(it, 0);
			}

			for (int i = 0; i < mesh.totface; ++i) {
				MTFace v = mesh.mtface.get(i);

				final Mesh out = temp.get(mat_num_to_mesh_idx.get(mesh.mface.get(i).mat_nr));
				int numFaces = mesh_to_num_faces.getInt(out);
				Face f = out.mFaces[numFaces++];
				mesh_to_num_faces.put(out, numFaces);
//				aiVector3D* vo = &out.mTextureCoords[0][out.mNumVertices];
//				for (unsigned int i = 0; i < f.mNumIndices; ++i,++vo,++out.mNumVertices) {
//					vo.x = v.uv[i][0];
//					vo.y = v.uv[i][1];
//				}
				
				FloatBuffer vo = out.mTextureCoords[0];
				int index = 3 * out.mNumVertices;
				for(int j = 0; j < f.getNumIndices(); ++j, ++out.mNumVertices){
					vo.put(index++, v.uv[j][0]);
					vo.put(index++, v.uv[j][1]);
					index++;
				}
			}
			
			for (int i = 0; i < mesh.totpoly; ++i) {
				MPoly v = mesh.mpoly.get(i);
				final Mesh out = temp.get(mat_num_to_mesh_idx.get(v.mat_nr) );
//				const aiFace& f = out.mFaces[out.mNumFaces++];
				int numFaces = mesh_to_num_faces.getInt(out);
				Face f = out.mFaces[numFaces++];
				mesh_to_num_faces.put(out, numFaces);
				
//				aiVector3D* vo = &out.mTextureCoords[0][out.mNumVertices];
//				for (unsigned int j = 0; j < f.mNumIndices; ++j,++vo,++out.mNumVertices) {
//					const MLoopUV& uv = mesh.mloopuv[v.loopstart + j];
//					vo.x = uv.uv[0];
//					vo.y = uv.uv[1];
//				}
				FloatBuffer vo = out.mTextureCoords[0];
				int index = 3 * out.mNumVertices;
				for(int j = 0; j < f.getNumIndices(); ++j, ++out.mNumVertices){
					MLoopUV uv = mesh.mloopuv.get(v.loopstart + j);
					vo.put(index++, uv.uv[0]);
					vo.put(index++, uv.uv[1]);
					index++;
				}
				
			}
		}

		// collect texture coordinates, old-style (marked as deprecated in current blender sources)
		if (mesh.tface.size() > 0) {
			if (mesh.totface > ( mesh.tface.size())) {
				throw new DeadlyImportError("Number of faces is larger than the corresponding UV face array (#2)");
			}
			
			mesh_to_num_faces.clear();
//			for (std::vector<aiMesh*>::iterator it = temp.begin()+old; it != temp.end(); ++it) {
			for (int m = old; m < temp.size(); m++){
				Mesh it = temp.get(m);
//				ai_assert((*it).mNumVertices && (*it).mNumFaces);

//				(*it).mTextureCoords[0] = new aiVector3D[(*it).mNumVertices];
//				(*it).mNumFaces = (*it).mNumVertices = 0;
				it.mTextureCoords[0] = MemoryUtil.createFloatBuffer(it.mNumVertices * 3,AssimpConfig.MESH_USE_NATIVE_MEMORY);
				it.mNumVertices = 0;
				mesh_to_num_faces.put(it, 0);
			}

			for (int i = 0; i < mesh.totface; ++i) {
				TFace v = mesh.tface.get(i);

				final Mesh out = temp.get(mat_num_to_mesh_idx.get( mesh.mface.get(i).mat_nr ) );
//				const aiFace& f = out.mFaces[out.mNumFaces++];
				int numFaces = mesh_to_num_faces.getInt(out);
				Face f = out.mFaces[numFaces++];
				mesh_to_num_faces.put(out, numFaces);
				
//				aiVector3D* vo = &out.mTextureCoords[0][out.mNumVertices];
//				for (unsigned int i = 0; i < f.mNumIndices; ++i,++vo,++out.mNumVertices) {
//					vo.x = v.uv[i][0];
//					vo.y = v.uv[i][1];
//				}
				
				FloatBuffer vo = out.mTextureCoords[0];
				int index = 3 * out.mNumVertices;
				for(int j = 0; j < f.getNumIndices(); ++j, ++out.mNumVertices){
					vo.put(index++, v.uv[j][0]);
					vo.put(index++, v.uv[j][1]);
					index++;
				}
			}
		}

		// collect vertex colors, stored separately as well
		if (mesh.mcol.size() > 0 || mesh.mloopcol.size() > 0) {
			if (mesh.totface > mesh.mcol.size()/4) {
				throw new DeadlyImportError("Number of faces is larger than the corresponding color face array");
			}
//			for (std::vector<aiMesh*>::iterator it = temp.begin()+old; it != temp.end(); ++it) {
//				ai_assert((*it).mNumVertices && (*it).mNumFaces);
//
//				(*it).mColors[0] = new aiColor4D[(*it).mNumVertices];
//				(*it).mNumFaces = (*it).mNumVertices = 0;
//			}
			
			mesh_to_num_faces.clear();
//			for (std::vector<aiMesh*>::iterator it = temp.begin()+old; it != temp.end(); ++it) {
			for (int m = old; m < temp.size(); m++){
				Mesh it = temp.get(m);
//				ai_assert((*it).mNumVertices && (*it).mNumFaces);

//				(*it).mTextureCoords[0] = new aiVector3D[(*it).mNumVertices];
//				(*it).mNumFaces = (*it).mNumVertices = 0;
				it.mColors[0] = MemoryUtil.createFloatBuffer(it.mNumVertices * 4,AssimpConfig.MESH_USE_NATIVE_MEMORY);
				it.mNumVertices = 0;
				mesh_to_num_faces.put(it, 0);
			}

			for (int i = 0; i < mesh.totface; ++i) {

				final Mesh out = temp.get(mat_num_to_mesh_idx.get(mesh.mface.get(i).mat_nr));
//				const aiFace& f = out.mFaces[out.mNumFaces++];
				int numFaces = mesh_to_num_faces.getInt(out);
				Face f = out.mFaces[numFaces++];
				mesh_to_num_faces.put(out, numFaces);
				
//				aiColor4D* vo = &out.mColors[0][out.mNumVertices];
//				for (unsigned int n = 0; n < f.mNumIndices; ++n, ++vo,++out.mNumVertices) {
//					const MCol* col = &mesh.mcol[(i<<2)+n];
//
//					vo.r = col.r;
//					vo.g = col.g;
//					vo.b = col.b;
//					vo.a = col.a;
//				}
				FloatBuffer vo = out.mColors[0];
				int index = 4 * out.mNumVertices;
				for(int j = 0; j < f.getNumIndices(); ++j, ++out.mNumVertices){
					MCol col = mesh.mcol.get((i<<2)+j);
					vo.put(index++, col.r);
					vo.put(index++, col.g);
					vo.put(index++, col.b);
					vo.put(index++, col.a);
				}
//				for (unsigned int n = f.mNumIndices; n < 4; ++n);
			}
			
			for (int i = 0; i < mesh.totpoly; ++i) {
				MPoly v = mesh.mpoly.get(i);
				final Mesh out = temp.get(mat_num_to_mesh_idx.get(v.mat_nr));
//				const aiFace& f = out.mFaces[out.mNumFaces++];
				int numFaces = mesh_to_num_faces.getInt(out);
				Face f = out.mFaces[numFaces++];
				mesh_to_num_faces.put(out, numFaces);
				
//				aiColor4D* vo = &out.mColors[0][out.mNumVertices];
//				for (unsigned int j = 0; j < f.mNumIndices; ++j,++vo,++out.mNumVertices) {
//					const MLoopCol& col = mesh.mloopcol[v.loopstart + j];
//					vo.r = col.r;
//					vo.g = col.g;
//					vo.b = col.b;
//					vo.a = col.a;
//				}
				
				FloatBuffer vo = out.mColors[0];
				int index = 4 * out.mNumVertices;
				for(int j = 0; j < f.getNumIndices(); ++j, ++out.mNumVertices){
					MLoopCol col = mesh.mloopcol.get(v.loopstart + j);
					vo.put(index++, col.r);
					vo.put(index++, col.g);
					vo.put(index++, col.b);
					vo.put(index++, col.a);
				}
			}

		}

		return;
	}

	// --------------------
	Light convertLight(BLEScene in, BLEObject obj, Lamp lamp, ConversionData conv_data){
		Light out = new Light();
//		out.mName = obj.id.name+2;
		out.mName = new String(obj.id.name, 2, obj.id.name.length - 2);

		switch (lamp.type)
		{
		    case Lamp.Type_Local:
		        out.mType = LightSourceType.aiLightSource_POINT;
		        break;
		    case Lamp.Type_Sun:
		        out.mType = LightSourceType.aiLightSource_DIRECTIONAL;

		        // blender orients directional lights as facing toward -z
		        out.mDirection.set(0.f, 0.f, -1.f);
		        break;
		    default:
		        break;
		}

//		out.mColorAmbient = aiColor3D(lamp.r, lamp.g, lamp.b) * lamp.energy;
//		out.mColorSpecular = aiColor3D(lamp.r, lamp.g, lamp.b) * lamp.energy;
//		out.mColorDiffuse = aiColor3D(lamp.r, lamp.g, lamp.b) * lamp.energy;
		Vector3f v = out.mColorAmbient;
		v.set(lamp.r, lamp.g, lamp.b);
		v.scale(lamp.energy);
		
		out.mColorSpecular.set(v);
		out.mColorDiffuse.set(v);
		return out;
	}

	// --------------------
	Camera convertCamera(BLEScene in, BLEObject obj, BLECamera mesh, ConversionData conv_data){
		Camera out = new Camera();
		out.mName = new String(obj.id.name, 2, obj.id.name.length - 2);
		out.mPosition.set(0.f, 0.f, 0.f);
		out.mUp.set(0.f, 1.f, 0.f);
		out.mLookAt.set(0.f, 0.f, -1.f);
		return out;
	}

	// --------------------
	void buildMaterials(ConversionData conv_data){
//		conv_data.materials.reserve(conv_data.materials_raw.size());
		conv_data.materials.ensureCapacity(conv_data.materials_raw.size());

		// add a default material if necessary
		int index = -1; //static_cast<unsigned int>( -1 );
//		for_each( aiMesh* mesh, conv_data.meshes.get() ) {
		for (Mesh mesh : conv_data.meshes) {
			if (mesh.mMaterialIndex == /*static_cast<unsigned int>*/( -1 )) {

				if (index == /*static_cast<unsigned int>*/( -1 )) {

					// ok, we need to add a dedicated default material for some poor material-less meshes
//					boost::shared_ptr<Material> p(new Material());
					BLEMaterial p = new BLEMaterial();
//					strcpy( p.id.name+2, AI_DEFAULT_MATERIAL_NAME );
					String source = Material.AI_DEFAULT_MATERIAL_NAME;
					source.getBytes(0, source.length(), p.id.name, 2);

					p.r = p.g = p.b = 0.6f;
					p.specr = p.specg = p.specb = 0.6f;
					p.ambr = p.ambg = p.ambb = 0.0f;
					p.mirr = p.mirg = p.mirb = 0.0f;
					p.emit = 0.f;
					p.alpha = 0.f;

					// XXX add more / or add default c'tor to Material

					index = /*static_cast<unsigned int>*/( conv_data.materials_raw.size() );
					conv_data.materials_raw.add(p);

//					LogInfo("Adding default material ...");
				}
				mesh.mMaterialIndex = index;
			}
		}

		final Vector3f col = new Vector3f();
//		for_each(boost::shared_ptr<Material> mat, conv_data.materials_raw) {
		for (BLEMaterial mat : conv_data.materials_raw) {

			// reset per material global counters
//			for (int i = 0; i < sizeof(conv_data.next_texture)/sizeof(conv_data.next_texture[0]);++i) {
//				conv_data.next_texture[i] = 0 ;
//			}
			Arrays.fill(conv_data.next_texture, 0);
		
			Material mout = new Material();
			conv_data.materials.add(mout);

			// set material name
			String name = new String(mat.id.name, 2, mat.id.name.length - 2); // skip over the name prefix 'MA'
			mout.addProperty(name,Material.AI_MATKEY_NAME, 0,0);

			// basic material colors
			col.set(mat.r,mat.g,mat.b);
			if (mat.r != 0 || mat.g != 0|| mat.b != 0) {
				
				// Usually, zero diffuse color means no diffuse color at all in the equation.
				// So we omit this member to express this intent.
				mout.addProperty(col,Material.AI_MATKEY_COLOR_DIFFUSE, 0, 0);

				if (mat.emit != 0) {
					col.set(mat.emit * mat.r, mat.emit * mat.g, mat.emit * mat.b) ;
					mout.addProperty(col, Material.AI_MATKEY_COLOR_EMISSIVE, 0, 0) ;
				}
			}

			col.set(mat.specr,mat.specg,mat.specb);
			mout.addProperty(col,Material.AI_MATKEY_COLOR_SPECULAR, 0, 0);

			// is hardness/shininess set?
			if( mat.har != 0) {
//				const float har = mat.har;
				mout.addProperty(mat.har,Material.AI_MATKEY_SHININESS, 0, 0);
			}

			col.set(mat.ambr,mat.ambg,mat.ambb);
			mout.addProperty(col,Material.AI_MATKEY_COLOR_AMBIENT, 0, 0);

			col.set(mat.mirr,mat.mirg,mat.mirb);
			mout.addProperty(col,Material.AI_MATKEY_COLOR_REFLECTIVE, 0,0);

			for(int i = 0; i < mat.mtex.length/*sizeof(mat.mtex) / sizeof(mat.mtex[0])*/; ++i) {
				if (mat.mtex[i] == null) {
					continue;
				}

				resolveTexture(mout,mat,mat.mtex[i],conv_data);
			}
		}
	}

	// --------------------
	void resolveTexture(Material out, BLEMaterial mat, MTex tex, ConversionData conv_data){
		Tex rtex = tex.tex;
		if(rtex == null || rtex.type == 0) {
			return;
		}
		
		// We can't support most of the texture types because they're mostly procedural.
		// These are substituted by a dummy texture.
		String dispnam = "";
		switch( rtex.type ) 
		{
				// these are listed in blender's UI
			case Tex.Type_CLOUDS		:  
			case Tex.Type_WOOD			:  
			case Tex.Type_MARBLE		:  
			case Tex.Type_MAGIC			: 
			case Tex.Type_BLEND			:  
			case Tex.Type_STUCCI		: 
			case Tex.Type_NOISE			: 
			case Tex.Type_PLUGIN		: 
			case Tex.Type_MUSGRAVE		:  
			case Tex.Type_VORONOI		:  
			case Tex.Type_DISTNOISE		:  
			case Tex.Type_ENVMAP		:  

				// these do no appear in the UI, why?
			case Tex.Type_POINTDENSITY	:  
			case Tex.Type_VOXELDATA	: 

				if(DefaultLogger.LOG_OUT)
					DefaultLogger.warn("Encountered a texture with an unsupported type: "+dispnam);
				addSentinelTexture(out, mat, tex, conv_data);
				break;

			case Tex.Type_IMAGE		:
				if (rtex.ima == null) {
					DefaultLogger.error("A texture claims to be an Image, but no image reference is given");
					break;
				}
				resolveImage(out, mat, tex, rtex.ima,conv_data);
				break;

			default:
//				ai_assert(false);
				assert false;
		};
	}

	// --------------------
	void resolveImage( Material out, BLEMaterial mat, MTex _tex, BLEImage img, ConversionData conv_data){
		String name;

		// check if the file contents are bundled with the BLEND file
		if (img.packedfile != null) {
//			name.data[0] = '*';
//			name.length = 1+ ASSIMP_itoa10(name.data+1,MAXLEN-1,conv_data.textures.size());
			name = '*' + Integer.toString(conv_data.textures.size());

			Texture tex;
			conv_data.textures.add(tex = new Texture());
//			aiTexture* tex = conv_data.textures.back();

			// usually 'img.name' will be the original file name of the embedded textures,
			// so we can extract the file extension from it.
//			const size_t nlen = strlen( img.name );
//			const char* s = img.name+nlen, *e = s;
//
//			while (s >= img.name && *s != '.')--s;
			int nlen = img.name.length - 1;
			while(img.name[nlen] == 0)
				nlen --;
			int s = nlen, e = s;
			while(s >= 0 && img.name[s] != '.') --s;

//			achFormatHint[0] = s+1>e ? '\0' : ::tolower( s[1] );
//			achFormatHint[1] = s+2>e ? '\0' : ::tolower( s[2] );
//			achFormatHint[2] = s+3>e ? '\0' : ::tolower( s[3] );
//			achFormatHint[3] = '\0';
			tex.achFormatHint = new String(img.name, s + 1, e - s);

			// tex.mHeight = 0;
			tex.mWidth = img.packedfile.size;
//			uint8_t* ch = new uint8_t[tex.mWidth];
			ByteBuffer ch = MemoryUtil.createByteBuffer(tex.mWidth, AssimpConfig.MESH_USE_NATIVE_MEMORY);

			conv_data.db.reader.setCurrentPos(/*static_cast<size_t>*/(int)( img.packedfile.data.val));
			conv_data.db.reader.copyAndAdvance(ch);

//			tex.pcData = reinterpret_cast<aiTexel*>(ch);
			tex.pcData = ch.asIntBuffer();

			if(DefaultLogger.LOG_OUT)
				DefaultLogger.info("Reading embedded texture, original file was "+ new String(img.name, 0, nlen + 1));
		}
		else {
			name = new String(img.name).trim();
		}

		TextureType texture_type = TextureType.aiTextureType_UNKNOWN;
		int map_type = _tex.mapto;

		if ((map_type & MTex.MapType_COL) != 0)
		    texture_type = TextureType.aiTextureType_DIFFUSE;
		else if ((map_type & MTex.MapType_NORM)!=0) {
		    if ((_tex.tex.imaflag & Tex.ImageFlags_NORMALMAP)!=0) {
		        texture_type = TextureType.aiTextureType_NORMALS;
		    }
		    else {
		        texture_type = TextureType.aiTextureType_HEIGHT;
		    }
		    out.addProperty(_tex.norfac,Material.AI_MATKEY_BUMPSCALING, 0, 0);
		}
		else if ((map_type & MTex.MapType_COLSPEC) !=0)
			texture_type = TextureType.aiTextureType_SPECULAR;
		else if ((map_type & MTex.MapType_COLMIR) !=0)
			texture_type = TextureType.aiTextureType_REFLECTION;
		//else if (map_type & MTex.MapType_REF)
		else if ((map_type & MTex.MapType_SPEC) !=0)
			texture_type = TextureType.aiTextureType_SHININESS;
		else if ((map_type & MTex.MapType_EMIT) !=0)
			texture_type = TextureType.aiTextureType_EMISSIVE;
		//else if (map_type & MTex.MapType_ALPHA)
		//else if (map_type & MTex.MapType_HAR)
		//else if (map_type & MTex.MapType_RAYMIRR)
		//else if (map_type & MTex.MapType_TRANSLU)
		else if ((map_type & MTex.MapType_AMB) !=0)
			texture_type = TextureType.aiTextureType_AMBIENT;
		else if ((map_type & MTex.MapType_DISPLACE) !=0)
			texture_type = TextureType.aiTextureType_DISPLACEMENT;
		//else if (map_type & MTex.MapType_WARP)

		out.addProperty(name,Material._AI_MATKEY_TEXTURE_BASE, texture_type.ordinal(),conv_data.next_texture[texture_type.ordinal()]++);
	}

	void addSentinelTexture( Material out, BLEMaterial mat, MTex tex, ConversionData conv_data){
//		String name;
//		name.length = sprintf(name.data, "Procedural,num=%i,type=%s",conv_data.sentinel_cnt++,
//			GetTextureTypeDisplayString(tex.tex.type)
//		);
		String name = String.format("Procedural,num=%i,type=%s", conv_data.sentinel_cnt++, Tex.getTextureTypeDisplayString(tex.tex.type));
		int index = TextureType.aiTextureType_DIFFUSE.ordinal();
		out.addProperty(name,Material._AI_MATKEY_TEXTURE_BASE, index,(
			conv_data.next_texture[index]++)
		);
	}

	// --------------------
	static void checkActualType(ElemBase dt, String check){
//		if (strcmp(dt.dna_type,check)) {
		if (!dt.dna_type.equals(check)){
			throw new DeadlyImportError(AssUtil.makeString(
				"Expected object at ",Integer.toHexString(dt.hashCode())," to be of type `",check, 
				"`, but it claims to be a `",dt.dna_type,"`instead"
			));
		}
	}

	// --------------------
	static void notSupportedObjectType(BLEObject obj, String type){
		if(DefaultLogger.LOG_OUT)
			DefaultLogger.warn(AssUtil.makeString("Object `",new String(obj.id.name),"` - type is unsupported: `",type, "`, skipping" ));
	}

}
