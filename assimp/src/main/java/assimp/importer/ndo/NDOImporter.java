package assimp.importer.ndo;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.lwjgl.util.vector.Vector3f;

import assimp.common.AssUtil;
import assimp.common.AssimpConfig;
import assimp.common.BaseImporter;
import assimp.common.DeadlyImportError;
import assimp.common.DefaultLogger;
import assimp.common.Face;
import assimp.common.FileUtils;
import assimp.common.ImporterDesc;
import assimp.common.MemoryUtil;
import assimp.common.Mesh;
import assimp.common.Node;
import assimp.common.Scene;
import assimp.common.StreamReader;
/** Importer class to load meshes from Nendo.<p>
*
*  Basing on
*  <blender>/blender/release/scripts/nendo_import.py by Anthony D'Agostino. 
*/
public class NDOImporter extends BaseImporter{

	static final ImporterDesc desc = new ImporterDesc(
		"Nendo Mesh Importer",
		"",
		"",
		"http://www.izware.com/nendo/index.htm",
		ImporterDesc.aiImporterFlags_SupportBinaryFlavour,
		0,
		0,
		0,
		0,
		"ndo" 
	);
	
	@Override
	protected boolean canRead(String pFile, InputStream pIOHandler, boolean checkSig) throws IOException {
		// check file extension 
		String extension = getExtension(pFile);
		
		if( extension.equals("ndo"))
			return true;

		if ((checkSig || extension.length() == 0) && pIOHandler != null) {
			String tokens[] = {"nendo"};
			return searchFileHeaderForToken(pIOHandler,pFile,tokens,1,5,false);
		}
		return false;
	}

	@Override
	protected ImporterDesc getInfo() {return desc;}

	@Override
	protected void internReadFile(File pFile, Scene pScene) {
		boolean natived = AssimpConfig.LOADING_USE_NATIVE_MEMORY;
		ByteBuffer buffer = FileUtils.loadText(pFile, false, natived);
		@SuppressWarnings("resource") // NO need close
		StreamReader reader = new StreamReader(buffer, false);  // BE
		
		// first 9 bytes are nendo file format ("nendo 1.n")
		ByteBuffer head = reader.getPtr();
		reader.incPtr(9);

		if (!AssUtil.equals(head, "nendo ",0,6)) {
			throw new DeadlyImportError("Not a Nendo file; magic signature missing");
		}
		// check if this is a supported version. if not, continue, too -- users,
		// please don't complain if it doesn't work then ...
		head.position(6);
		int file_format = 12;
		if (AssUtil.equals(head,"1.0",0,3)) {
			file_format = 10;
			if(DefaultLogger.LOG_OUT)
				DefaultLogger.info("NDO file format is 1.0");
		}
		else if (AssUtil.equals(head,"1.1",0,3)) {
			file_format = 11;
			if(DefaultLogger.LOG_OUT)
				DefaultLogger.info("NDO file format is 1.1");
		}
		else if (AssUtil.equals(head, "1.2",0,3)) {
			file_format = 12;
			if(DefaultLogger.LOG_OUT)
				DefaultLogger.info("NDO file format is 1.2");
		}
		else {
			if(DefaultLogger.LOG_OUT){
				byte[] bytes = new byte[3];
				head.get(bytes);
				DefaultLogger.warn("Unrecognized nendo file format version, continuing happily ... :" + new String(bytes));
			}
		}

		reader.incPtr(2); /* skip flags */
		if (file_format >= 12) {
			reader.incPtr(2);
		}
		int temp = reader.getI1() & 0xFF;

//		std::vector<Object> objects(temp); /* buffer to store all the loaded objects in */
		NDOObject[] objects = new NDOObject[temp];
		AssUtil.initArray(objects);
		byte[] temp_bytes = new byte[1024];
		// read all objects
		for (int o = 0; o < objects.length; ++o) {
			
//			if (file_format < 12) {
				if (reader.getI1() == 0) {
					continue; /* skip over empty object */
				}
			//	reader.GetI2();
//			}
			NDOObject obj = objects[o];

			temp = file_format >= 12 ? reader.getI4() : reader.getI2() & 0xFFFF;
//			head = (const char*)reader.GetPtr();
			head = reader.getPtr();
			reader.incPtr(temp + 76); /* skip unknown stuff */

//			obj.name = std::string(head, temp);
			if(temp > temp_bytes.length)
				temp_bytes = new byte[temp];
			head.get(temp_bytes,0,temp);
			obj.name = new String(temp_bytes,0, temp);
			
			// read edge table
			temp = file_format >= 12 ? reader.getI4() : reader.getI2() & 0xFFFF;
			obj.edges.ensureCapacity(temp);
			for (int e = 0; e < temp; ++e) {
				Edge edge;
				obj.edges.add(edge = new Edge());
//				Edge& edge = obj.edges.back();

				for (int i = 0; i< 8; ++i) {
					edge.edge[i] = file_format >= 12 ? reader.getI4() : reader.getI2() & 0xFFFF;
				}
				edge.hard =  file_format >= 11 ? reader.getI1() & 0xFF  : 0;
				for (int i = 0; i< 8; ++i) {
					edge.color[i] = reader.getI1();
				}
			}

			// read face table
			temp = file_format >= 12 ? reader.getI4() : reader.getI2() & 0xFFFF;
			obj.faces.ensureCapacity(temp);
			for (int e = 0; e < temp; ++e) {
				NDOFace face;
				obj.faces.add(face = new NDOFace());
//				Face& face = obj.faces.back();

				face.elem = file_format >= 12 ? reader.getI4() : reader.getI2() & 0xFFFF;
			}

			// read vertex table
			temp = file_format >= 12 ? reader.getI4() : reader.getI2() & 0xFFFF;
			obj.vertices.ensureCapacity(temp);
			for (int e = 0; e < temp; ++e) {
				Vertex v;
				obj.vertices.add(v = new Vertex());
//				Vertex& v = obj.vertices.back();

				v.num = file_format >= 12 ? reader.getI4() : reader.getI2() & 0xFFFF;
				v.val.x = reader.getF4();
				v.val.y = reader.getF4();
				v.val.z = reader.getF4();
			}

			// read UVs
			temp = file_format >= 12 ? reader.getI4() : reader.getI2() & 0xFFFF;
			for (int e = 0; e < temp; ++e) {
				temp = file_format >= 12 ? reader.getI4() : reader.getI2() & 0xFFFF;
			}

			temp = file_format >= 12 ? reader.getI4() : reader.getI2() & 0xFFFF;
			for (int e = 0; e < temp; ++e) {
				temp = file_format >= 12 ? reader.getI4() : reader.getI2() & 0xFFFF;
			}

			if (reader.getI1() != 0) {
				final int x = reader.getI2() & 0xFFFF, y = reader.getI2() & 0xFFFF;
				temp = 0;
				while (temp < x*y)  {
					int repeat = reader.getI1() & 0xFF;
					reader.getI1();
					reader.getI1();
					reader.getI1();
					temp += repeat;
				}
			}
		}

		// construct a dummy node graph and add all named objects as child nodes
		Node root = pScene.mRootNode = new Node("$NDODummyRoot");
		Node[] cc = root.mChildren = new Node [ /*root->mNumChildren = static_cast<unsigned int>( objects.size())*/ objects.length];
		pScene.mMeshes = new Mesh [ /*root->mNumChildren*/cc.length];

//		std::vector<aiVector3D> vertices;
//		std::vector<unsigned int> indices;
		FloatArrayList vertices = FloatArrayList.wrap(AssUtil.EMPTY_FLOAT);
		IntArrayList indices = new IntArrayList();
		Int2IntMap face_table = new Int2IntOpenHashMap();
//		for_each(const Object& obj,objects) {
		int index = 0;
		int numMeshes = 0;
		for (NDOObject obj : objects){
			Node nd = cc[index++] = new Node(obj.name);
			nd.mParent = root;

			// translated from a python dict() - a vector might be sufficient as well
//			typedef std::map<unsigned int, unsigned int>  FaceTable;
//			FaceTable face_table;
			face_table.clear();

			int n = 0;
//			for_each(const Edge& edge, obj.edges) {
			for (Edge edge : obj.edges){
			
//				face_table[edge.edge[2]] = n;
//				face_table[edge.edge[3]] = n;
				face_table.put(edge.edge[2], n);
				face_table.put(edge.edge[3], n);

				++n;
			}

			Mesh mesh = new Mesh();
			Face[] faces = mesh.mFaces = new Face[/*mesh->mNumFaces=*/face_table.size()];

			vertices.clear();
			vertices.ensureCapacity((4 * face_table.size()) * 3); // arbitrarily choosen 
//			for_each(FaceTable::value_type& v, face_table) {
			int face_index = 0;
			for (Int2IntMap.Entry v : face_table.int2IntEntrySet()){
				indices.clear();

//				aiFace& f = *faces++;
			
				final int key = v.getIntKey();
				int cur_edge = v.getIntValue();
				while (true) {
					int next_edge, next_vert;
					Edge curr_edge = obj.edges.get(cur_edge);
					if (key == curr_edge.edge[3]) {
						next_edge = curr_edge.edge[5];
						next_vert = curr_edge.edge[1];
					}
					else {
						next_edge = curr_edge.edge[4];
						next_vert = curr_edge.edge[0];
					}
					indices.add( vertices.size()/3 );
//					vertices.add(obj.vertices[ next_vert ].val);
					Vector3f vec = obj.vertices.get(next_vert).val;
					vertices.add(vec.x);
					vertices.add(vec.y);
					vertices.add(vec.z);

					cur_edge = next_edge;
					if (cur_edge == v.getIntValue()) {
						break;
					}
				}
				
//				f.mIndices = new unsigned int[f.mNumIndices = indices.size()];
//				std::copy(indices.begin(),indices.end(),f.mIndices);
				Face f = faces[face_index++] = Face.createInstance(indices.size());
				for(int m = 0; m <f.getNumIndices(); m++)
					f.set(m, indices.getInt(m));
			}

//			mesh->mVertices = new aiVector3D[mesh->mNumVertices = vertices.size()];
//			std::copy(vertices.begin(),vertices.end(),mesh->mVertices);
			mesh.mNumVertices = vertices.size()/3;
			mesh.mVertices = MemoryUtil.createFloatBuffer(vertices.size(), AssimpConfig.MESH_USE_NATIVE_MEMORY);
			mesh.mVertices.put(vertices.elements(), 0, vertices.size()).flip();

			if (mesh.mNumVertices > 0) {
				pScene.mMeshes[numMeshes] = mesh;

				(nd.mMeshes = new int[/*nd->mNumMeshes=*/1])[0]=numMeshes++;
			}
		}
	}

}
