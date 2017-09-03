package assimp.importer.xgl;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.zip.ZipInputStream;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.WritableVector2f;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import assimp.common.AssUtil;
import assimp.common.AssimpConfig;
import assimp.common.BaseImporter;
import assimp.common.DeadlyExportError;
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

/** XGL/ZGL importer.<p>
*
* Spec: http://vizstream.aveva.com/release/vsplatform/XGLSpec.htm
*/
public class XGLImporter extends BaseImporter{

	static final ImporterDesc desc = new ImporterDesc(
		"XGL Importer",
		"",
		"",
		"",
		ImporterDesc.aiImporterFlags_SupportTextFlavour,
		0,
		0,
		0,
		0,
		"xgl zgl" 
	);
	
	XmlPullParser reader;
	Scene scene;

	@Override
	protected boolean canRead(String pFile, InputStream pIOHandler,
			boolean checkSig) throws IOException {
		/* NOTE: A simple check for the file extension is not enough
		 * here. XGL and ZGL are ok, but xml is too generic
		 * and might be collada as well. So open the file and
		 * look for typical signal tokens.
		 */
		String extension = getExtension(pFile);

		if (extension.equals("xgl") || extension.equals("zgl")) {
			return true;
		}
		else if (extension.equals("xml") || checkSig) {
//			ai_assert(pIOHandler != NULL);

			String tokens[] = {"<world>","<World>","<WORLD>"};
			return searchFileHeaderForToken(pIOHandler,pFile,tokens);
		}
		return false;
	}

	@Override
	protected ImporterDesc getInfo() { return desc;}

	@Override
	protected void internReadFile(File pFile, Scene pScene) {
		scene = pScene;
		
		InputStream in = null;
		String filename = pFile.getName();
		try {
			if(getExtension(filename).equals("zgl")){ // compressed data.
				in = new ZipInputStream(new FileInputStream(pFile));
			}else{
				in = new FileInputStream(pFile);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		final TempScope scope = new TempScope();
		try {
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			reader = factory.newPullParser();
			reader.setInput(in, null);
			
			// parse the XML file
			int event = reader.getEventType();
			while(event != XmlPullParser.END_DOCUMENT){
				switch (event) {
				case XmlPullParser.START_DOCUMENT:
				case XmlPullParser.START_TAG:
					if(reader.getName().toLowerCase().equals("world")){
						readWorld(scope);
					}
					
					break;

				default:
					break;
				}
			}
			
			in.close();
		} catch (XmlPullParserException e) {
			throw new DeadlyImportError(e);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		List<Mesh> meshes = scope.meshes_linear;
		List<Material> materials = scope.materials_linear;
		if(AssUtil.isEmpty(meshes) || AssUtil.isEmpty(materials)) {
			throwException("failed to extract data from XGL file, no meshes loaded");
		}

		// copy meshes
//		scene->mNumMeshes = static_cast<unsigned int>(meshes.size());
//		scene->mMeshes = new aiMesh*[scene->mNumMeshes]();
//		std::copy(meshes.begin(),meshes.end(),scene->mMeshes);
		scene.mMeshes = AssUtil.toArray(meshes, Mesh.class);

		// copy materials
//		scene->mNumMaterials = static_cast<unsigned int>(materials.size());
//		scene->mMaterials = new aiMaterial*[scene->mNumMaterials]();
//		std::copy(materials.begin(),materials.end(),scene->mMaterials);
		scene.mMaterials = AssUtil.toArray(materials, Material.class);

		if (scope.light != null) {
//			scene->mNumLights = 1;
//			scene->mLights = new aiLight*[1];
//			scene->mLights[0] = scope.light;
			scene.mLights = new Light[]{scope.light};

			scope.light.mName = scene.mRootNode.mName;
		}
		
		scope.dismiss();
	}
	
	void throwException(String msg){
		throw new DeadlyExportError("XGL: " + msg);
	}
	
	// ------------------------------------------------------------------------------------------------
	boolean readElementUpToClosing(String closetag) throws XmlPullParserException, IOException
	{
//		while(reader->read()) {
//			if (reader->getNodeType() == EXN_ELEMENT) {
//				return true;
//			}
//			else if (reader->getNodeType() == EXN_ELEMENT_END && !ASSIMP_stricmp(reader->getNodeName(),closetag)) {
//				return false;
//			}
//		}
		int event;
		
		while((event = reader.next()) != XmlPullParser.END_DOCUMENT){
			switch (event) {
			case XmlPullParser.START_TAG: 
				return true;
			case XmlPullParser.END_TAG:
				if(reader.getName().toLowerCase().equals(closetag)){
					return false;
				}
			default:
				break;
			}
		}
		
		logError("unexpected EOF, expected closing <" + closetag + "> tag");
		return false;
	}
	
	// ------------------------------------------------------------------------------------------------
	boolean skipToText() throws XmlPullParserException, IOException
	{
//		while(reader->read()) {
//			if (reader->getNodeType() == EXN_TEXT) {
//				return true;
//			}
//			else if (reader->getNodeType() == EXN_ELEMENT || reader->getNodeType() == EXN_ELEMENT_END) {
//				ThrowException("expected text contents but found another element (or element end)");
//			}
//		}
		
		int event;
		while((event = reader.next()) != XmlPullParser.END_DOCUMENT){
			switch (event) {
			case XmlPullParser.TEXT: 
				return true;
			case XmlPullParser.START_TAG:
			case XmlPullParser.END_TAG:
				throwException("expected text contents but found another element (or element end)");
			default:
				break;
			}
		}
		return false;
	}
	
	// ------------------------------------------------------------------------------------------------
	String getElementName()
	{
//		const char* s  = reader->getNodeName();
//		size_t len = strlen(s);
//
//		std::string ret;
//		ret.resize(len);
//
//		std::transform(s,s+len,ret.begin(),::tolower);
		return reader.getName().toLowerCase();
	}
	
	// ------------------------------------------------------------------------------------------------
	void readWorld(TempScope scope) throws XmlPullParserException, IOException
	{
		while (readElementUpToClosing("world"))	{	
			String s = getElementName();
			// XXX right now we'd skip <lighting> if it comes after
			// <object> or <mesh>
			if (s.equals("lighting")) {
				readLighting(scope);
			}
			else if (s.equals("object") || s.equals("mesh") || s.equals("mat")) {
				break;
			}
		}

		
		Node nd = readObject(scope,true,"world");
		if(nd == null) {
			throwException("failure reading <world>");
		}
		if(AssUtil.isEmpty(nd.mName)) {
			nd.mName = ("WORLD");
		}

		scene.mRootNode = nd;
	}
	
	// ------------------------------------------------------------------------------------------------
	void readLighting(TempScope scope) throws XmlPullParserException, IOException
	{
		while (readElementUpToClosing("lighting"))	{
			String s = getElementName();
			if (s.equals("directionallight")) {
				scope.light = readDirectionalLight();
			}
			else if (s.equals("ambient")) {
				logWarn("ignoring <ambient> tag");
			}
			else if (s.equals("spheremap")) {
				logWarn("ignoring <spheremap> tag");
			}
		}
	}

	// ------------------------------------------------------------------------------------------------
	Light readDirectionalLight() throws XmlPullParserException, IOException
	{
//		ScopeGuard<aiLight> l(new aiLight());
		Light l = new Light();
		l.mType = LightSourceType.aiLightSource_DIRECTIONAL;

		while (readElementUpToClosing("directionallight"))	{	
			String s = getElementName();
			if (s.equals("direction")) {
//				l.mDirection = ReadVec3();
				readVec3(l.mDirection);
			}
			else if (s.equals("diffuse")) {
//				l->mColorDiffuse = ReadCol3();
				readCol3(l.mColorDiffuse);
			}
			else if (s.equals("specular")) {
//				l->mColorSpecular = ReadCol3();
				readCol3(l.mColorSpecular);
			}
		}
		return l;
	}
	
	// ------------------------------------------------------------------------------------------------
	Node readObject(TempScope scope, boolean skipFirst, String closetag) throws XmlPullParserException, IOException
	{
		Node nd = (new Node());
		ArrayList<Node> children = new ArrayList<Node>();
		IntArrayList meshes = null;

//		try {
		while (skipFirst || readElementUpToClosing(closetag))	{
			skipFirst = false;

			String s = getElementName();
			if (s.equals("mesh")) {
				final int prev = AssUtil.size(scope.meshes_linear);
				if(readMesh(scope)) {
					final int newc = AssUtil.size(scope.meshes_linear);
					if(newc - prev > 0)
						meshes = new IntArrayList(newc - prev);
					for(int i = 0; i < newc-prev; ++i) {
						meshes.add(/*static_cast<unsigned int>*/(i+prev));
					}
				}
			}
			else if (s.equals("mat")) {
				readMaterial(scope);
			}
			else if (s.equals("object")) {
				children.add(readObject(scope,false, "object"));
			}
			else if (s.equals("objectref")) {
				// XXX
			}
			else if (s.equals("meshref")) {
				final int id = /*static_cast<unsigned int>*/( readIndexFromText() );

//					std::multimap<unsigned int, aiMesh*>::iterator it = scope.meshes.find(id), end = scope.meshes.end();
//					if (it == end) {
//						ThrowException("<meshref> index out of range");
//					}
				
				if(AssUtil.isEmpty(scope.meshes))
					throwException("<meshref> is empty");
				
				List<Mesh> list = scope.meshes.get(id);
				if(AssUtil.isEmpty(list))
					throwException("<meshref> index out of range");
				
//					for(; it != end && (*it).first == id; ++it) {
				for(Mesh m : list){
					// ok, this is n^2 and should get optimized one day
//						aiMesh* const m = (*it).second;
					
//						int i = 0, mcount = (AssUtil.size(scope.meshes_linear));
//						for(; i < mcount; ++i) {
//							if (scope.meshes_linear[i] == m) {
//								meshes.push_back(i);
//								break;
//							}
//						}
//						
//						ai_assert(i < mcount);
					
					int index = scope.meshes_linear != null ? scope.meshes_linear.indexOf(m) : -1;
					if(index >=0){
						meshes.add(index);
					}
				}
			}
			else if (s.equals("transform")) {
				readTrafo(nd.mTransformation);
			}
		}

//		} catch(...) {
//			BOOST_FOREACH(aiNode* ch, children) {
//				delete ch;
//			}
//			throw;
//		}

		// link meshes to node
//		nd->mNumMeshes = static_cast<unsigned int>(meshes.size());
//		if (nd->mNumMeshes) {
//			nd->mMeshes = new unsigned int[nd->mNumMeshes]();
//			for(unsigned int i = 0; i < nd->mNumMeshes; ++i) {
//				nd->mMeshes[i] = meshes[i];
//			}
//		}
		nd.mMeshes = meshes != null ? meshes.toIntArray() : null;

		// link children to parent
//		nd->mNumChildren = static_cast<unsigned int>(children.size());
//		if (nd->mNumChildren) {
//			nd->mChildren = new aiNode*[nd->mNumChildren]();
//			for(unsigned int i = 0; i < nd->mNumChildren; ++i) {
//				nd->mChildren[i] = children[i];
//				children[i]->mParent = nd;
//			}
//		}
		
		nd.mChildren = AssUtil.toArray(children, Node.class);

		return nd/*.dismiss()*/;
	}
	
	// ------------------------------------------------------------------------------------------------
	void readTrafo(Matrix4f mat) throws XmlPullParserException, IOException
	{
		Vector3f forward = new Vector3f(), up = new Vector3f(), right = new Vector3f(), position = new Vector3f();
		float scale = 1.0f;

		while (readElementUpToClosing("transform"))	{	
			String s = getElementName();
			if (s.equals("forward")) {
				readVec3(forward);
			}
			else if (s.equals("up")) {
				readVec3(up);
			}
			else if (s.equals("position")) {
				readVec3(position);
			}
			if (s.equals("scale")) {
				scale = readFloat();
				if(scale < 0.f) {
					// this is wrong, but we can leave the value and pass it to the caller
					logError("found negative scaling in <transform>, ignoring");
				}
			}
		}

//		aiMatrix4x4 m;
		if(forward.lengthSquared() < 1e-4 || up.lengthSquared() < 1e-4) {
			logError("A direction vector in <transform> is zero, ignoring trafo");
//			return m;
			mat.setIdentity();
			return;
		}

//		forward.Normalize();
//		up.Normalize();
//
//		right = forward ^ up;
//		if (fabs(up * forward) > 1e-4) {
//			// this is definitely wrong - a degenerate coordinate space ruins everything
//			// so subtitute identity transform.
//			LogError("<forward> and <up> vectors in <transform> are skewing, ignoring trafo");
//			return m;
//		}
//
//		right *= scale;
//		up *= scale;
//		forward *= scale;
//
//		m.a1 = right.x;
//		m.b1 = right.y;
//		m.c1 = right.z;
//
//		m.a2 = up.x;
//		m.b2 = up.y;
//		m.c2 = up.z;
//
//		m.a3 = forward.x;
//		m.b3 = forward.y;
//		m.c3 = forward.z;
//
//		m.a4 = position.x;
//		m.b4 = position.y;
//		m.c4 = position.z;
		Matrix4f.lookAt(position, forward, up, mat);

//		return m;
	}
	
	// ------------------------------------------------------------------------------------------------
	Mesh toOutputMesh(TempMaterialMesh m)
	{
		final boolean natived = AssimpConfig.MESH_USE_NATIVE_MEMORY;
		Mesh mesh = new Mesh();
//		mesh->mNumVertices = static_cast<unsigned int>(m.positions.size());
//		mesh->mVertices = new aiVector3D[mesh->mNumVertices];
//		std::copy(m.positions.begin(),m.positions.end(),mesh->mVertices);
		mesh.mVertices = MemoryUtil.refCopy(m.positions, natived);
		mesh.mNumVertices = mesh.mVertices.remaining()/3;

//		if(m.normals.size()) {
//			mesh->mNormals = new aiVector3D[mesh->mNumVertices];
//			std::copy(m.normals.begin(),m.normals.end(),mesh->mNormals);
//		}
		mesh.mNormals = MemoryUtil.refCopy(m.normals, natived);

		if(m.uvs/*.size()*/ != null) {
			mesh.mNumUVComponents[0] = 2;
			mesh.mTextureCoords[0] = MemoryUtil.createFloatBuffer(mesh.mVertices.capacity(), natived);//new aiVector3D[mesh.mNumVertices];

			for(int i = 0; i < mesh.mNumVertices; ++i) {
//				mesh->mTextureCoords[0][i] = aiVector3D(m.uvs[i].x,m.uvs[i].y,0.f);
				MemoryUtil.arraycopy(m.uvs, 2 * i, mesh.mTextureCoords[0], 3 * i, 2);
			}
		}

//		mesh->mNumFaces =  static_cast<unsigned int>(m.vcounts.size());
//		mesh->mFaces = new aiFace[m.vcounts.size()];
		mesh.mFaces = new Face[m.vcounts.size()];

		int idx = 0;
		for(int i = 0; i < mesh.mFaces.length; ++i) {
			Face f = mesh.mFaces[i] = Face.createInstance(m.vcounts.getInt(i));
//			f.mNumIndices = m.vcounts[i];
//			f.mIndices = new unsigned int[f.mNumIndices];
			for(int c = 0; c < f.getNumIndices(); ++c) {
//				f.mIndices[c] = idx++;
				f.set(c, idx++);
			}
		}

//		ai_assert(idx == mesh->mNumVertices);

		mesh.mPrimitiveTypes = m.pflags;
		mesh.mMaterialIndex = m.matid;
		return mesh/*.dismiss()*/;
	}
	
	// ------------------------------------------------------------------------------------------------
	boolean readMesh(TempScope scope) throws XmlPullParserException, IOException
	{
		TempMesh t = new TempMesh();

//		std::map<unsigned int, TempMaterialMesh> bymat;
		Int2ObjectMap<TempMaterialMesh> bymat = new Int2ObjectOpenHashMap<TempMaterialMesh>();
		final int mesh_id = readIDAttr();
		Vector3f vec3 = new Vector3f();

		while (readElementUpToClosing("mesh"))	{	
			String s = getElementName();

			if (s.equals("mat")) {
				readMaterial(scope);
			}
			else if (s.equals("p")) {
				String _id = reader.getAttributeValue(null, "ID");
				if (_id == null) {
					logWarn("no ID attribute on <p>, ignoring");
				}
				else {
					int id = AssUtil.parseInt(_id); //reader->getAttributeValueAsInt("ID");
//					t.points[id] = ReadVec3();
					readVec3(vec3);
					t.points.put(id, t.float3.size()/3);
					t.float3.add(vec3.x);
					t.float3.add(vec3.y);
					t.float3.add(vec3.z);
				}
			}
			else if (s.equals("n")) {
				String _id = reader.getAttributeValue(null, "ID");
				if (/*!reader->getAttributeValue("ID")*/ _id == null) {
					logWarn("no ID attribute on <n>, ignoring");
				}
				else {
					int id = AssUtil.parseInt(_id)/*reader->getAttributeValueAsInt("ID")*/;
//					t.normals[id] = ReadVec3();
					readVec3(vec3);
					t.normals.put(id, t.float3.size()/3);
					t.float3.add(vec3.x);
					t.float3.add(vec3.y);
					t.float3.add(vec3.z);
				}
			}
			else if (s.equals("tc")) {
				String _id = reader.getAttributeValue(null, "ID");
				if (/*!reader->getAttributeValue("ID")*/_id == null) {
					logWarn("no ID attribute on <tc>, ignoring");
				}
				else {
					int id = AssUtil.parseInt(_id);//reader->getAttributeValueAsInt("ID");
//					t.uvs[id] = ReadVec2();
					readVec2(vec3);
					t.uvs.put(id, t.float2.size()/2);
					t.float2.add(vec3.x);
					t.float2.add(vec3.y);
				}
			}
			else if (s.equals("f") || s.equals("l") || s.equals("p")) {
				final int vcount = s.equals("f") ? 3 : (s.equals("l") ? 2 : 1);

				int mid = ~0;
				TempFace[] tf = new TempFace[3];
				AssUtil.initArray(tf);
				boolean[] has = {false, false, false};

				while (readElementUpToClosing(s))	{
					String ss = getElementName();
					if (ss.equals("fv1") || ss.equals("lv1") || ss.equals("pv1")) {
						readFaceVertex(t,tf[0]);
						has[0] = true;
					}
					else if (ss.equals("fv2") || ss.equals("lv2")) {
						readFaceVertex(t,tf[1]);
						has[1] = true;
					}
					else if (ss.equals("fv3")) {
						readFaceVertex(t,tf[2]);
						has[2] = true;
					}
					else if (ss.equals("mat")) {
						if (mid != ~0) {
							logWarn("only one material tag allowed per <f>");
						}
						mid = resolveMaterialRef(scope);
					}
					else if (ss.equals("matref")) {
						if (mid != ~0) {
							logWarn("only one material tag allowed per <f>");
						}
						mid = resolveMaterialRef(scope);
					}
				}

				if (mid == ~0) {
					throwException("missing material index");
				}

				boolean nor = false;
				boolean uv = false;
				for(int i = 0; i < vcount; ++i) {
					if (!has[i]) {
						throwException("missing face vertex data");
					}

					nor = nor || tf[i].has_normal();
					uv = uv || tf[i].has_uv();
				}			

				if (mid >= (1<<30)) {
					logWarn("material indices exhausted, this may cause errors in the output");
				}
				int meshId = mid | ((nor?1:0)<<31) | ((uv?1:0)<<30);

				TempMaterialMesh mesh = bymat.get(meshId);
				mesh.matid = mid;

				for(int i = 0; i < vcount; ++i) {
//					mesh.positions.push_back(tf[i].pos);
					tf[i].pos.store(mesh.positions);
					if(nor) {
//						mesh.normals.push_back(tf[i].normal);
						tf[i].normal.store(mesh.normals);
					}
					if(uv) {
//						mesh.uvs.push_back(tf[i].uv);
						tf[i].uv.store(mesh.uvs);
					}
					
					mesh.pflags |= 1 << (vcount-1);
				}

				mesh.vcounts.add(vcount);
			}		
		}

		// finally extract output meshes and add them to the scope 
//		typedef std::pair<unsigned int, TempMaterialMesh> pairt;
//		BOOST_FOREACH(const pairt& p, bymat) {
		for(TempMaterialMesh p : bymat.values()){
//			aiMesh* const m  = ToOutputMesh(p.second);
			Mesh m = toOutputMesh(p);
			scope.meshes_linear.add(m);

			// if this is a definition, keep it on the stack
			if(mesh_id != ~0) {
//				scope.meshes.insert(std::pair<unsigned int, aiMesh*>(mesh_id,m));
				List<Mesh> list = scope.meshes.get(mesh_id);
				if(list == null)
					scope.meshes.put(mesh_id, list = new LinkedList<Mesh>());
				list.add(m);
			}
		}

		// no id == not a reference, insert this mesh right *here*
		return mesh_id == ~0;
	}
	
	// ----------------------------------------------------------------------------------------------
	int resolveMaterialRef(TempScope scope) throws XmlPullParserException, IOException
	{
		String s = getElementName();
		if (s.equals("mat")) {
			readMaterial(scope);
			return scope.materials_linear.size()-1;
		}

		final int id = readIndexFromText();

//		std::map<unsigned int, aiMaterial*>::iterator it = scope.materials.find(id), end = scope.materials.end();
//		if (it == end) {
//			ThrowException("<matref> index out of range");
//		}
//		
//		// ok, this is n^2 and should get optimized one day
//		aiMaterial* const m = (*it).second;
//
//		unsigned int i = 0, mcount = static_cast<unsigned int>(scope.materials_linear.size());
//		for(; i < mcount; ++i) {
//			if (scope.materials_linear[i] == m) {
//				return i;
//			}
//		}
		
		Material m = scope.materials.get(id);
		if(m == null)
			throwException("<matref> index out of range");
		
		// ok, this is n^2 and should get optimized one day
		int i = scope.materials_linear.indexOf(m);
		if(i >= 0)return i;

	    assert(false);
		return 0;
	}
	
	// ------------------------------------------------------------------------------------------------
	void readMaterial(TempScope scope) throws XmlPullParserException, IOException
	{
		final Vector3f c = new Vector3f();
		final int mat_id = readIDAttr();

//		ScopeGuard<aiMaterial> mat(new aiMaterial());
		Material mat = new Material();
		while (readElementUpToClosing("mat"))  {
			String s = getElementName();
			if (s.equals("amb")) {
//				const aiColor3D c = ReadCol3();
				readCol3(c);
				mat.addProperty(c,Material.AI_MATKEY_COLOR_AMBIENT,0,0);
			}
			else if (s.equals("diff")) {
//				const aiColor3D c = ReadCol3();
				readCol3(c);
				mat.addProperty(c,Material.AI_MATKEY_COLOR_DIFFUSE,0,0);
			}
			else if (s.equals("spec")) {
//				const aiColor3D c = ReadCol3();
				readCol3(c);
				mat.addProperty(c,Material.AI_MATKEY_COLOR_SPECULAR,0,0);
			}
			else if (s.equals("emiss")) {
//				const aiColor3D c = ReadCol3();
				readCol3(c);
				mat.addProperty(c,Material.AI_MATKEY_COLOR_EMISSIVE,0,0);
			}
			else if (s == "alpha") {
				final float f = readFloat();
				mat.addProperty(f,Material.AI_MATKEY_OPACITY,0,0);
			}
			else if (s == "shine") {
				final float f = readFloat();
				mat.addProperty(f,Material.AI_MATKEY_SHININESS,0,0);
			}
		}

//		scope.materials[mat_id] = mat;
		scope.materials.put(mat_id, mat);
		scope.materials_linear.add(mat);
	}
	
	// ----------------------------------------------------------------------------------------------
	void readFaceVertex(TempMesh t, TempFace out) throws XmlPullParserException, IOException
	{
		final String end = getElementName();

		boolean havep = false;
		while (readElementUpToClosing(end/*.c_str()*/))  {
			String s = getElementName();
			if (s.equals("pref")) {
				final int id = readIndexFromText();
//				std::map<unsigned int, aiVector3D>::const_iterator it = t.points.find(id);
//				if (it == t.points.end()) {
//					ThrowException("point index out of range");
//				}
//
//				out.pos = (*it).second;
				t.getPoint(id, out.pos);
				havep = true;
			}
			else if (s.equals("nref")) {
				final int id = readIndexFromText();
//				std::map<unsigned int, aiVector3D>::const_iterator it = t.normals.find(id);
//				if (it == t.normals.end()) {
//					ThrowException("normal index out of range");
//				}
//
//				out.normal = (*it).second;
				t.getNormal(id, out.normal);
				out.has_normal = true;
			}
			else if (s.equals("tcref")) {
				final int id = readIndexFromText();
//				std::map<unsigned int, aiVector2D>::const_iterator it = t.uvs.find(id);
//				if (it == t.uvs.end()) {
//					ThrowException("uv index out of range");
//				}
//
//				out.uv = (*it).second;
				t.getUV(id, out.uv);
				out.has_uv = true;
			}
			else if (s.equals("p")) {
				 readVec3(out.pos);
			}
			else if (s.equals( "n")) {
				readVec3(out.normal);
			}
			else if (s.equals("tc")) {
				readVec2(out.uv);
			}
		}

		if (!havep) {
			throwException("missing <pref> in <fvN> element");
		}
	}
	
	// ------------------------------------------------------------------------------------------------
	int readIDAttr()
	{
		for(int i = 0, e = reader.getAttributeCount(); i < e; ++i) {
		
//			if(!ASSIMP_stricmp(reader->getAttributeName(i),"id")) {
			if(reader.getAttributeName(i).equalsIgnoreCase("id")){
				return AssUtil.parseInt(reader.getAttributeValue(i));
			}
		}	
		return ~0;
	}

	// ------------------------------------------------------------------------------------------------
	float readFloat() throws XmlPullParserException, IOException
	{
		if(!skipToText()) {
			logError("unexpected EOF reading float element contents");
			return 0.f;
		}
//		const char* s = reader->getNodeData(), *se;
//
//		if(!SkipSpaces(&s)) {
//			LogError("unexpected EOL, failed to parse float");
//			return 0.f;
//		}
//
//		float t;
//		se = fast_atoreal_move(s,t);
//
//		if (se == s) {
//			LogError("failed to read float text");
//			return 0.f;
//		}

		return AssUtil.parseFloat(reader.getText().trim()); //TODO performance issue.
	}
	
	void logError(String msg){
		DefaultLogger.error("XGL: " + msg);
	}
	
	void logWarn(String msg){
		DefaultLogger.warn("XGL: " + msg);
	}

	// ------------------------------------------------------------------------------------------------
	int readIndexFromText() throws XmlPullParserException, IOException
	{
		if(!skipToText()) {
			logError("unexpected EOF reading index element contents");
			return ~0;
		}
//		const char* s = reader->getNodeData(), *se;
//		if(!SkipSpaces(&s)) {
//			logError("unexpected EOL, failed to parse index element");
//			return ~0u;
//		}
//
//		const unsigned int t = strtoul10(s,&se);
//
//		if (se == s) {
//			LogError("failed to read index");
//			return ~0u;
//		}

		return AssUtil.parseInt(reader.getText().trim());  //TODO performance issue.
	}
	
	// ------------------------------------------------------------------------------------------------
	void readVec2(WritableVector2f vec) throws XmlPullParserException, IOException
	{
		if(!skipToText()) {
			logError("unexpected EOF reading vec2 contents");
			return;
		}
//		const char* s = reader->getNodeData();
//
//		for(int i = 0; i < 2; ++i) {
//			if(!SkipSpaces(&s)) {
//				LogError("unexpected EOL, failed to parse vec2");
//				return vec;
//			}
//			vec[i] = fast_atof(&s);
//
//			SkipSpaces(&s);
//			if (i != 1 && *s != ',') {
//				LogError("expected comma, failed to parse vec2");
//				return vec;
//			}
//			++s;
//		}
		
		StringTokenizer tokens = new StringTokenizer(reader.getText(), ", ");
		try {
			vec.setX(AssUtil.parseFloat(tokens.nextToken()));
			vec.setY(AssUtil.parseFloat(tokens.nextToken()));
		} catch (NoSuchElementException e) {
			logError("unexpected EOL, failed to parse vec2");
		}

	}

	// ------------------------------------------------------------------------------------------------
	void readVec3(Vector3f vec) throws XmlPullParserException, IOException
	{
//		aiVector3D vec;

		if(!skipToText()) {
			logError("unexpected EOF reading vec3 contents");
			return /*vec*/;
		}
//		const char* s = reader->getNodeData();
//
//		for(int i = 0; i < 3; ++i) {
//			if(!SkipSpaces(&s)) {
//				LogError("unexpected EOL, failed to parse vec3");
//				return vec;
//			}
//			vec[i] = fast_atof(&s);
//
//			SkipSpaces(&s);
//			if (i != 2 && *s != ',') {
//				LogError("expected comma, failed to parse vec3");
//				return vec;
//			}
//			++s;
//		}

		StringTokenizer tokens = new StringTokenizer(reader.getText(), ", ");
		try {
			vec.setX(AssUtil.parseFloat(tokens.nextToken()));
			vec.setY(AssUtil.parseFloat(tokens.nextToken()));
			vec.setZ(AssUtil.parseFloat(tokens.nextToken()));
		} catch (NoSuchElementException e) {
			logError("unexpected EOL, failed to parse vec3");
		}
		return /*vec*/;
	}

	// ------------------------------------------------------------------------------------------------
	void readCol3(Vector3f col) throws XmlPullParserException, IOException
	{
		readVec3(col);
		if (col.x < 0.f || col.x > 1.0f || col.y < 0.f || col.y > 1.0f || col.z < 0.f || col.z > 1.0f) {
			if(DefaultLogger.LOG_OUT)
				logWarn("color values out of range, ignoring");
		}
//		return aiColor3D(v.x,v.y,v.z);
	}
}
