package assimp.importer.ms3d;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntRBTreeMap;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import assimp.common.Animation;
import assimp.common.AssUtil;
import assimp.common.AssimpConfig;
import assimp.common.BaseImporter;
import assimp.common.Bone;
import assimp.common.DeadlyImportError;
import assimp.common.DefaultLogger;
import assimp.common.Face;
import assimp.common.FileUtils;
import assimp.common.ImporterDesc;
import assimp.common.Material;
import assimp.common.MemoryUtil;
import assimp.common.Mesh;
import assimp.common.Node;
import assimp.common.NodeAnim;
import assimp.common.QuatKey;
import assimp.common.Scene;
import assimp.common.ShadingMode;
import assimp.common.StreamReader;
import assimp.common.TextureType;
import assimp.common.VectorKey;
import assimp.common.VertexWeight;

/** Milkshape 3D importer implementation */
public class MS3DImporter extends BaseImporter{

	static final ImporterDesc desc = new ImporterDesc(
		"Milkshape 3D Importer",
		"",
		"",
		"http://chumbalum.swissquake.ch/",
		ImporterDesc.aiImporterFlags_SupportBinaryFlavour,
		0,
		0,
		0,
		0,
		"ms3d" 
	);
	
	private Scene mScene;
	
	@Override
	protected boolean canRead(String pFile, InputStream pIOHandler, boolean checkSig) throws IOException {
		// first call - simple extension check
		String extension = getExtension(pFile);
		if (extension.endsWith("ms3d")) {
			return true;
		}

		// second call - check for magic identifiers
		else if (extension.length() ==0 || checkSig)	{
			if (pIOHandler == null) {
				return true;
			}
			String tokens[] = {"MS3D000000"};
			return searchFileHeaderForToken(pIOHandler,pFile,tokens);
		}
		return false;
	}

	@Override
	protected ImporterDesc getInfo() { return desc;}

	// ------------------------------------------------------------------------------------------------
	static void readColor(StreamReader stream, Vector4f ambient)
	{
		// aiColor4D is packed on gcc, implicit binding to float& fails therefore.
//		stream >> (float&)ambient.r >> (float&)ambient.g >> (float&)ambient.b >> (float&)ambient.a;
		ambient.x = stream.getF4();
		ambient.y = stream.getF4();
		ambient.z = stream.getF4();
		ambient.w = stream.getF4();
	}

	// ------------------------------------------------------------------------------------------------
	static void readVector(StreamReader stream, Vector3f pos)
	{
		// See note in ReadColor()
//		stream >> (float&)pos.x >> (float&)pos.y >> (float&)pos.z;
		pos.x = stream.getF4();
		pos.y = stream.getF4();
		pos.z = stream.getF4();
	}

	// ------------------------------------------------------------------------------------------------
	static<T extends TempComment> void readComments(StreamReader stream, T[] outp)
	{
//		uint16_t cnt;
//		stream >> cnt;
		int cnt = stream.getI2() & 0xFFFF;

		for(int i = 0; i < cnt; ++i) {
//			uint32_t index, clength;
//			stream >> index >> clength;
			int index = stream.getI4();
			int clength = stream.getI4();

			if(index >= outp.length) {
				if(DefaultLogger.LOG_OUT)
					DefaultLogger.warn("MS3D: Invalid index in comment section");
			}
			else if (clength > stream.getRemainingSize()) {
				throw new DeadlyImportError("MS3D: Failure reading comment, length field is out of range");
			}
			else {
//				outp[index].comment = std::string(reinterpret_cast<char*>(stream.GetPtr()),clength);
				ByteBuffer buffer = stream.getPtr();
				byte[] bytes = new byte[clength];
				buffer.get(bytes);
				outp[index].comment = new String(bytes);
			}
			stream.incPtr(clength);
		}
	}
	
	// ------------------------------------------------------------------------------------------------
	void collectChildJoints(TempJoint[] joints, boolean[] hadit, Node nd, Matrix4f absTrafo)
	{
		int cnt = 0;
		for(int i = 0; i < joints.length; ++i) {
			if (!hadit[i] && /*!strcmp(joints[i].parentName,nd.mName.data)*/ AssUtil.equals(joints[i].parentName, 0, nd.mName)) {
				++cnt;
			}
		}

		nd.mChildren = new Node[/*nd.mNumChildren = */cnt];
		cnt = 0;
		Matrix4f temp_mat = new Matrix4f();
		for(int i = 0; i < joints.length; ++i) {
			TempJoint joint = joints[i];
			if (!hadit[i] && /*!strcmp(joints[i].parentName,nd.mName.data)*/ AssUtil.equals(joint.parentName, 0, nd.mName)) {
				Node ch = nd.mChildren[cnt++] = new Node(new String(joint.name));
				ch.mParent = nd;

//				ch.mTransformation = aiMatrix4x4::Translation(joints[i].position,aiMatrix4x4()=aiMatrix4x4())*
					// XXX actually, I don't *know* why we need the inverse here. Probably column vs. row order?
//					aiMatrix4x4().FromEulerAnglesXYZ(joints[i].rotation).Transpose();
				// TODO
				Matrix4f.rotationYawPitchRoll(joint.rotation.y, joint.rotation.x, joint.rotation.z, temp_mat);
				ch.mTransformation.m30 = joint.position.x;
				ch.mTransformation.m31 = joint.position.y;
				ch.mTransformation.m32 = joint.position.z;
				Matrix4f.mul(ch.mTransformation, temp_mat, ch.mTransformation);
//				const aiMatrix4x4 abs = absTrafo*ch->mTransformation;
				Matrix4f abs = Matrix4f.mul(absTrafo, ch.mTransformation, temp_mat);
				Matrix4f abs_invert = null;
				for(int a = 0; a < mScene.getNumMeshes(); ++a) {
					Mesh msh = mScene.mMeshes[a];
					for(int n = 0; n < msh.getNumBones(); ++n) {
						Bone bone = msh.mBones[n];

						if(bone.mName.equals(ch.mName)) {
//							bone->mOffsetMatrix = aiMatrix4x4(abs).Inverse();
							if(abs_invert == null){
								Matrix4f.invert(abs, bone.mOffsetMatrix);
								abs_invert = bone.mOffsetMatrix;
							}else{
								bone.mOffsetMatrix.load(abs_invert);
							}
						}
					}
				}
		
				hadit[i] = true;
				collectChildJoints(joints,hadit,ch,abs);
			}
		}
	}
	
	// ------------------------------------------------------------------------------------------------
	void collectChildJoints(TempJoint[] joints, Node nd)
	{
//		 std::vector<bool> hadit(joints.size(),false);
//		 aiMatrix4x4 trafo;
		boolean[] hadit = new boolean[joints.length];

		collectChildJoints(joints,hadit,nd,Matrix4f.IDENTITY);
	}
	
	@Override
	protected void internReadFile(File pFile, Scene pScene) {
		this.mScene = pScene;
		
		ByteBuffer buf = FileUtils.loadText(pFile, false, AssimpConfig.LOADING_USE_NATIVE_MEMORY);
		StreamReader stream = new StreamReader(buf, true);
		// CanRead() should have done this already
		byte[] head = new byte[10];
		int version;

		// 1 ------------ read into temporary data structures mirroring the original file

		stream.copyAndAdvance(head,0, 10);
//		stream >> version;
		version = stream.getI4();
		if (!AssUtil.equals(head,0, "MS3D000000")) {
			throw new DeadlyImportError("Not a MS3D file, magic string MS3D000000 not found: "+pFile.getAbsolutePath());
		}

		if (version != 4) {
			throw new DeadlyImportError("MS3D: Unsupported file format version, 4 was expected");
		}

//		uint16_t verts;
//		stream >> verts;
		int verts = stream.getI2() & 0xFFFF;

//		std::vector<TempVertex> vertices(verts);
		TempVertex[] vertices = new TempVertex[verts];
		for (int i = 0; i < verts; ++i) {
			TempVertex v = vertices[i] = new TempVertex();

			stream.incPtr(1);
			readVector(stream,v.pos);
			v.bone_id[0] = stream.getI1(); 
			v.ref_cnt = stream.getI1();

			v.bone_id[1] = v.bone_id[2] = v.bone_id[3] = -1;
			v.weights[1] = v.weights[2] = v.weights[3] = 0.f;
			v.weights[0] = 1.f;
		}

//		uint16_t tris;
//		stream >> tris;
		int tris = stream.getI2() & 0xFFFF;

//		std::vector<TempTriangle> triangles(tris);
		TempTriangle[] triangles = new TempTriangle[tris];
		for (int i = 0;i < tris; ++i) {
			TempTriangle t = triangles[i] = new TempTriangle();

			stream.incPtr(2);
			for (int j = 0; j < 3; ++j) {
				t.indices[j] = stream.getI2();
			}

			for (int j = 0; j < 3; ++j) {
				readVector(stream,t.normals[j]);
			}

			for (int j = 0; j < 3; ++j) {
//				stream >> (float&)(t.uv[i].x); // see note in ReadColor()
				t.uv[j].x = stream.getF4();
			}
			for (int j = 0; j < 3; ++j) {
//				stream >> (float&)(t.uv[i].y);
				t.uv[j].y = stream.getF4();
			}

			t.sg    = stream.getI1(); 
			t.group = stream.getI1(); 
		}

//		uint16_t grp;
//		stream >> grp;
		int grp = stream.getI2() & 0xFFFF;

		boolean need_default = false;
//		std::vector<TempGroup> groups(grp);
		TempGroup[] groups = new TempGroup[grp];
		for (int i = 0;i < grp; ++i) {
			TempGroup t = groups[i] = new TempGroup();

			stream.incPtr(1);
			stream.copyAndAdvance(t.name,0,32);

//			t.name[32] = '\0';
//			uint16_t num;
//			stream >> num;
			int num = stream.getI2() & 0xFFFF;

			t.triangles.size(num);
			int[] t_triangles = t.triangles.elements();
			for (int j = 0; j < num; ++j) {
//				t.triangles[i] = stream.GetI2(); 
				t_triangles[j] = stream.getI2();
			}
			t.mat = stream.getI1(); 
			if (t.mat == -1) {
				need_default = true;
			}
		}

//		uint16_t mat;
//		stream >> mat;
		int mat = stream.getI2() & 0xFFFF;

//		std::vector<TempMaterial> materials(mat);
		TempMaterial[] materials = new TempMaterial[mat];
		for (int i = 0;i < mat; ++i) {
			TempMaterial t = materials[i] = new TempMaterial();

			stream.copyAndAdvance(t.name,0,32);
//			t.name[32] = '\0';

			readColor(stream,t.ambient);
			readColor(stream,t.diffuse);
			readColor(stream,t.specular);
			readColor(stream,t.emissive);
//			stream >> t.shininess  >> t.transparency;
			t.shininess = stream.getF4();
			t.transparency = stream.getF4();

			stream.incPtr(1);

			stream.copyAndAdvance(t.texture,0,128);
//			t.texture[128] = '\0';

			stream.copyAndAdvance(t.alphamap,0,128);
//			t.alphamap[128] = '\0';
		}

		float animfps, currenttime;
		int totalframes;
//		stream >> animfps >> currenttime >> totalframes;
		animfps = stream.getF4();
		currenttime = stream.getF4();
		totalframes = stream.getI4();

//		uint16_t joint;
//		stream >> joint;
		int joint = stream.getI2() & 0xFFFF;

//		std::vector<TempJoint> joints(joint);
		TempJoint[] joints = new TempJoint[joint];
		for(int i = 0; i < joint; ++i) {
			TempJoint j = joints[i] = new TempJoint();

			stream.incPtr(1);
			stream.copyAndAdvance(j.name,0,32);
//			j.name[32] = '\0';

			stream.copyAndAdvance(j.parentName,0,32);
//			j.parentName[32] = '\0';

		//	DefaultLogger::get()->debug(j.name);
		//	DefaultLogger::get()->debug(j.parentName);

			readVector(stream,j.rotation);
			readVector(stream,j.position);

			int rot_size, pos_size;
			j.rotFrames.ensureCapacity(rot_size = stream.getI2() & 0xFFFF);
			j.posFrames.ensureCapacity(pos_size = stream.getI2() & 0xFFFF);

			for(int a = 0; a < rot_size/*j.rotFrames.size()*/; ++a) {
				TempKeyFrame kf = new TempKeyFrame()/*j.rotFrames.get(a)*/;
//				stream >> kf.time;
				kf.time = stream.getF4();
				readVector(stream,kf.value);
				j.rotFrames.add(kf);
			}
			for(int a = 0; a < pos_size/*j.posFrames.size()*/; ++a) {
				TempKeyFrame kf = new TempKeyFrame() /*j.posFrames[a]*/;
//				stream >> kf.time;
				kf.time = stream.getF4();
				readVector(stream,kf.value);
				j.posFrames.add(kf);
			}
		}

		if(stream.getRemainingSize() > 4) {
//			uint32_t subversion;
//			stream >> subversion;
			int subversion = stream.getI4();
			if (subversion == 1) {
				readComments(stream,groups);
				readComments(stream,materials);
				readComments(stream,joints);
				
				// model comment - print it for we have such a nice log.
				if (stream.getI4() != 0) {
					final int len = stream.getI4();
					if (len > stream.getRemainingSize()) {
						throw new DeadlyImportError("MS3D: Model comment is too long");
					}

//					String s = std::string(reinterpret_cast<char*>(stream.GetPtr()),len);
					if(DefaultLogger.LOG_OUT){
						byte[] bytes = new byte[len];
						stream.getPtr().get(bytes);
						DefaultLogger.debug("MS3D: Model comment: " + new String(bytes));
					}
				}

				if(stream.getRemainingSize() > 4 && AssUtil.inrange(subversion = stream.getI4(), 1, 3)/*inrange((stream >> subversion,subversion),1u,3u)*/) {
					for( int i = 0; i < verts; ++i) {
						TempVertex v = vertices[i];
						v.weights[3]=1.f;
						for(int n = 0; n < 3; v.weights[3]-=v.weights[n++]) {
							v.bone_id[n+1] = stream.getI1();
//							v.weights[n] = static_cast<float>(static_cast<unsigned int>(stream.GetI1()))/255.f;
							v.weights[n] = (stream.getI1() & 0xFF)/255.f;
						}
						stream.incPtr((subversion-1)<<2);
					}

					// even further extra data is not of interest for us, at least now now.
				}
			}
		}

		// 2 ------------ convert to proper aiXX data structures -----------------------------------

		if (need_default && materials.length > 0) {
			if(DefaultLogger.LOG_OUT)
				DefaultLogger.warn("MS3D: Found group with no material assigned, spawning default material");
			// if one of the groups has no material assigned, but there are other 
			// groups with materials, a default material needs to be added (
			// scenepreprocessor adds a default material only if nummat==0).
//			materials.add(new TempMaterial());
//			TempMaterial& m = materials.back();
			TempMaterial m = new TempMaterial();
//			strcpy(m.name,"<MS3D_DefaultMat>");
			String name = "<MS3D_DefaultMat>";
			name.getBytes(0, name.length(), m.name, 0);
			m.diffuse.set(0.6f,0.6f,0.6f,1.0f);
			m.transparency = 1.f;
			m.shininess = 0.f;

			// this is because these TempXXX struct's have no c'tors.
			m.texture[0] = m.alphamap[0] = '\0';

			for (int i = 0; i < groups.length; ++i) {
				TempGroup g = groups[i];
				if (g.mat == -1) {
					g.mat = /*materials.size()-1*/ materials.length;
				}
			}
			
			materials = java.util.Arrays.copyOf(materials, materials.length + 1);
			materials[materials.length - 1] = m;
		}

		// convert materials to our generic key-value dict-alike
		if (materials.length > 0) {
			pScene.mMaterials = new Material[materials.length];
			for (int i = 0; i < materials.length; ++i) {

				Material mo = new Material();
				pScene.mMaterials[/*pScene->mNumMaterials++*/i] = mo;

				TempMaterial mi = materials[i];

				String tmp;
				if (/*0[mi.alphamap]*/ mi.alphamap[0] != 0) {
//					tmp = aiString(mi.alphamap);
					tmp = AssUtil.toString(mi.alphamap);
					mo.addProperty(tmp, Material._AI_MATKEY_TEXTURE_BASE, TextureType.aiTextureType_OPACITY.ordinal(),0);
				}
				if (/*0[mi.texture]*/ mi.texture[0] != 0) {
//					tmp = aiString(mi.texture);
					tmp = AssUtil.toString(mi.texture);
					mo.addProperty(tmp,Material._AI_MATKEY_TEXTURE_BASE, TextureType.aiTextureType_DIFFUSE.ordinal(),0);
				}
				if (/*0[mi.name]*/ mi.name[0] != 0) {
//					tmp = aiString(mi.name);
					tmp = AssUtil.toString(mi.name);
					mo.addProperty(tmp, Material.AI_MATKEY_NAME,0,0);
				}

				mo.addProperty(mi.ambient,Material.AI_MATKEY_COLOR_AMBIENT,0,0);
				mo.addProperty(mi.diffuse,Material.AI_MATKEY_COLOR_DIFFUSE,0,0);
				mo.addProperty(mi.specular,Material.AI_MATKEY_COLOR_SPECULAR,0,0);
				mo.addProperty(mi.emissive,Material.AI_MATKEY_COLOR_EMISSIVE,0,0);

				mo.addProperty(mi.shininess,Material.AI_MATKEY_SHININESS,0,0);
				mo.addProperty(mi.transparency,Material.AI_MATKEY_OPACITY,0,0);

				final int sm = mi.shininess>0.f? ShadingMode.aiShadingMode_Phong.ordinal():ShadingMode.aiShadingMode_Gouraud.ordinal();
				mo.addProperty(sm,Material.AI_MATKEY_SHADING_MODEL,0,0);
			}
		}

		// convert groups to meshes
		if (groups.length == 0) {
			throw new DeadlyImportError("MS3D: Didn't get any group records, file is malformed");
		}

		Int2IntMap mybones = new Int2IntRBTreeMap(); // TODO Need This?
		pScene.mMeshes = new Mesh[/*pScene.mNumMeshes=static_cast<unsigned int>(groups.size())*/ groups.length];
		for (int j = 0; j <groups.length; ++j) {
		
			Mesh m = pScene.mMeshes[j] = new Mesh();
			TempGroup g = groups[j];

			if (materials.length > 0 && g.mat > materials.length) {
				throw new DeadlyImportError("MS3D: Encountered invalid material index, file is malformed");
			} // no error if no materials at all - scenepreprocessor adds one then

			m.mMaterialIndex  = g.mat;
			m.mPrimitiveTypes = Mesh.aiPrimitiveType_TRIANGLE; 

			m.mFaces = new Face[/*m.mNumFaces = */g.triangles.size()];
			m.mNumVertices = m.mFaces.length*3;

			// storage for vertices - verbose format, as requested by the postprocessing pipeline
			final boolean natived = AssimpConfig.MESH_USE_NATIVE_MEMORY;
//			m.mVertices = new aiVector3D[m.mNumVertices];
//			m.mNormals  = new aiVector3D[m.mNumVertices];
//			m.mTextureCoords[0] = new aiVector3D[m.mNumVertices];
			m.mVertices = MemoryUtil.createFloatBuffer(m.mNumVertices * 3, natived);
			m.mNormals  = MemoryUtil.createFloatBuffer(m.mNumVertices * 3, natived);
			m.mTextureCoords[0] = MemoryUtil.createFloatBuffer(m.mNumVertices * 3, natived);
			m.mNumUVComponents[0] = 2;

//			typedef std::map<unsigned int,unsigned int> BoneSet;
//			BoneSet mybones;
			mybones.clear();
			for (int i = 0,n = 0; i < m.mFaces.length; ++i) {
				Face f = m.mFaces[i] = Face.createInstance(3);
				if (g.triangles.getInt(i)>triangles.length) {
					throw new DeadlyImportError("MS3D: Encountered invalid triangle index, file is malformed");
				}

				TempTriangle t = triangles[g.triangles.getInt(i)];
//				f.mIndices = new unsigned int[f.mNumIndices=3];
				
				for (int k = 0; k < 3; ++k,++n) {
					if (t.indices[k]>vertices.length) {
						throw new DeadlyImportError("MS3D: Encountered invalid vertex index, file is malformed");
					}

					TempVertex v = vertices[t.indices[k]];
					for(int a = 0; a < 4; ++a) {
						if (v.bone_id[a] != -1) {
							if (v.bone_id[a] >= joints.length) {
								throw new DeadlyImportError("MS3D: Encountered invalid bone index, file is malformed");
							}
//							if (mybones.find(v.bone_id[a]) == mybones.end()) {
//								 mybones[v.bone_id[a]] = 1;
//							}
//							else ++mybones[v.bone_id[a]];
							mybones.put(v.bone_id[a], mybones.get(v.bone_id[a]) + 1);
						}
					}

					// collect vertex components
//					m.mVertices[n] = v.pos;
					int index = n * 3;
					m.mVertices.put(index++, v.pos.x);
					m.mVertices.put(index++, v.pos.y);
					m.mVertices.put(index++, v.pos.z);

//					m.mNormals[n] = t.normals[i];
					index -= 3;
					Vector3f nor = t.normals[k];
					m.mNormals.put(index++, nor.x);
					m.mNormals.put(index++, nor.y);
					m.mNormals.put(index++, nor.z);
					
//					m.mTextureCoords[0][n] = aiVector3D(t.uv[i].x,1.f-t.uv[i].y,0.0);
					index -= 3;
					m.mTextureCoords[0].put(index++, t.uv[k].x);
					m.mTextureCoords[0].put(index++, 1.f - t.uv[k].y);
//					f.mIndices[i] = n;
					f.set(k, n);
				}
			}

			// allocate storage for bones
			if(mybones.size() > 0) {
//				std::vector<unsigned int> bmap(joints.size());
				int[] bmap = new int[joints.length];
				m.mBones = new Bone[mybones.size()];
//				for(BoneSet::const_iterator it = mybones.begin(); it != mybones.end(); ++it) {
				int numBones = 0;
				for (Int2IntMap.Entry it : mybones.int2IntEntrySet()){
					Bone bn = m.mBones[numBones] = new Bone();
					TempJoint jnt = joints[it.getIntKey()]; 

					bn.mName = AssUtil.toString(jnt.name);
					bn.mWeights = new VertexWeight[it.getIntValue()];

//					bmap[(*it).first] = m.mNumBones++;
					bmap[it.getIntKey()] = numBones++;
				}

				// .. and collect bone weights
				for (int l = 0,n = 0; l < m.getNumFaces(); ++l) {
					TempTriangle t = triangles[g.triangles.getInt(l)];

					for (int i = 0; i < 3; ++i,++n) {
						TempVertex v = vertices[t.indices[i]];
						for(int a = 0; a < 4; ++a) {
							final int bone = v.bone_id[a];
							if(bone==-1){
								continue;
							}

							Bone outbone = m.mBones[bmap[bone]];
//							aiVertexWeight& outwght = outbone->mWeights[outbone->mNumWeights++];
							int index = AssUtil.findfirstNull(outbone.mWeights);
							VertexWeight outwght = outbone.mWeights[index] = new VertexWeight();

							outwght.mVertexId = n;
							outwght.mWeight = v.weights[a];
						}
					}
				}
			}
		}
		
		// ... add dummy nodes under a single root, each holding a reference to one
		// mesh. If we didn't do this, we'd lose the group name.
		Node rt = pScene.mRootNode = new Node("<MS3DRoot>");
		
		if(AssimpConfig.ASSIMP_BUILD_MS3D_ONE_NODE_PER_MESH){
			int count = joints.length > 0 ? 1 : 0 + pScene.getNumMeshes();
			rt.mChildren = new Node[count/*rt->mNumChildren=pScene->mNumMeshes+(joints.size()?1:0)*/];

			for (int i = 0; i < pScene.getNumMeshes(); ++i) {
				Node nd = rt.mChildren[i] = new Node();

				TempGroup g = groups[i];

				// we need to generate an unique name for all mesh nodes.
				// since we want to keep the group name, a prefix is
				// prepended.
//				nd.mName = aiString("<MS3DMesh>_");
//				nd.mName.Append(g.name);
				nd.mName = "<MS3DMesh>_" + AssUtil.toString(g.name);
				nd.mParent = rt;

				nd.mMeshes = new int[/*nd.mNumMeshes =*/ 1];
				nd.mMeshes[0] = i;
			}
		}else{
			if(pScene.mMeshes != null){
				rt.mMeshes = new int[pScene.mMeshes.length];
				for (int i = 0; i < pScene.mMeshes.length; ++i) {
					rt.mMeshes[/*rt->mNumMeshes++*/ i] = i;
				}
			}
		}
		
		if(joints.length > 0){
			Node jt;
			if(!AssimpConfig.ASSIMP_BUILD_MS3D_ONE_NODE_PER_MESH){
				rt.mChildren = new Node[1];
//				rt.mNumChildren = 1;

				jt = rt.mChildren[0] = new Node();
			}else{
				jt = rt.mChildren[pScene.getNumMeshes()] = new Node();
			}
			
			jt.mParent = rt;
			collectChildJoints(joints,jt);
			jt.mName = ("<MS3DJointRoot>");

			pScene.mAnimations = new Animation[/* pScene->mNumAnimations =*/ 1 ];
			Animation anim = pScene.mAnimations[0] = new Animation();

			anim.mName = ("<MS3DMasterAnim>");

			// carry the fps info to the user by scaling all times with it
			anim.mTicksPerSecond = animfps;
			
			// leave duration at its default, so ScenePreprocessor will fill an appropriate
			// value (the values taken from some MS3D files seem to be too unreliable
			// to pass the validation)
			// anim.mDuration = totalframes/animfps;

			anim.mChannels = new NodeAnim[joints.length];
			int numChannels = 0;
			Matrix4f mat1 = new Matrix4f();
			Matrix4f mat2 = new Matrix4f();
//			for(std::vector<TempJoint>::const_iterator it = joints.begin(); it != joints.end(); ++it) {
			for (TempJoint it : joints){
				if (it.rotFrames.isEmpty() && it.posFrames.isEmpty()) {
					continue;
				}

				NodeAnim nd = anim.mChannels[numChannels++] = new NodeAnim();
				nd.mNodeName = AssUtil.toString(it.name);

				if (it.rotFrames.size() > 0) {
					nd.mRotationKeys = new QuatKey[it.rotFrames.size()];
					int numRotationKeys = 0;
//					for(std::vector<TempKeyFrame>::const_iterator rot = (*it).rotFrames.begin(); rot != (*it).rotFrames.end(); ++rot) {
					for(TempKeyFrame rot : it.rotFrames){
						QuatKey q = nd.mRotationKeys[numRotationKeys++] = new QuatKey();

						q.mTime = rot.time*animfps;

						// XXX it seems our matrix&quaternion code has faults in its conversion routines --
						// aiQuaternion(x,y,z) seems to besomething different as quat(matrix.fromeuler(x,y,z)).
//						q.mValue = aiQuaternion(aiMatrix3x3(aiMatrix4x4().FromEulerAnglesXYZ((*rot).value)*
//							aiMatrix4x4().FromEulerAnglesXYZ((*it).rotation)).Transpose());
						Matrix4f.rotationYawPitchRoll(rot.value.y, rot.value.x, rot.value.z, mat1);
						Matrix4f.rotationYawPitchRoll(it.rotation.y, it.rotation.x, it.rotation.z, mat2);
						Matrix4f.mul(mat1, mat2, mat1);
						q.mValue.setFromMatrix(mat1);
					}
				}

				if (it.posFrames.size() > 0) {
					nd.mPositionKeys = new VectorKey[it.posFrames.size()];

					QuatKey[] qu = nd.mRotationKeys;
					int numPositionKeys = 0;
//					for(std::vector<TempKeyFrame>::const_iterator pos = (*it).posFrames.begin(); pos != (*it).posFrames.end(); ++pos,++qu) {
					for(TempKeyFrame pos : it.posFrames){
						VectorKey v = nd.mPositionKeys[numPositionKeys++] = new VectorKey();

						v.mTime = pos.time*animfps;
//						v.mValue = (*it).position + (*pos).value;
						Vector3f.add(it.position, pos.value, v.mValue);
					}
				}
			}
			// fixup to pass the validation if not a single animation channel is non-trivial
//			if (!anim.mNumChannels) {
//				anim.mChannels = NULL;
//			}
			
			if(numChannels == 0)
				anim.mChannels = null;
		}
	}

}
