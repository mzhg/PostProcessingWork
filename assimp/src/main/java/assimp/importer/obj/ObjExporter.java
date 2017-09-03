package assimp.importer.obj;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import assimp.common.AssUtil;
import assimp.common.AssimpConfig;
import assimp.common.Material;
import assimp.common.Mesh;
import assimp.common.Node;
import assimp.common.Scene;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

/** Helper class to export a given scene to an OBJ file. */
public final class ObjExporter {
	
	String filename;
	Scene pScene;
	
	VecIndexMap vpMap = new VecIndexMap();
	VecIndexMap vnMap = new VecIndexMap();
	VecIndexMap vtMap = new VecIndexMap();
	
	final List<MeshInstace> meshes = new ArrayList<>();
	
	public ObjExporter(String filename, Scene pScene) {
		this.filename = filename;
		this.pScene = pScene;
		
		try(BufferedWriter mOutputMat = new BufferedWriter(new FileWriter(filename))){
			writeGeometryFile(mOutputMat);
			writeMaterialFile(mOutputMat);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	

	void writeHeader(Writer out/*std::ostringstream& out*/) throws IOException{
		out.write("# File produced by Open Asset Import Library (http://www.assimp.sf.net)\n");
		out.write("# (assimp v");
		out.write(Integer.toString(AssimpConfig.MAJOR_VERSION));
		out.write('.');
		out.write(Integer.toString(AssimpConfig.MINOR_VERSION));
		out.write('.');
		out.write("0");
		out.write( ")\n\n");
	}

	void writeMaterialFile(Writer out) throws IOException{
		writeHeader(out);

		Vector4f c = new Vector4f();
		for(int i = 0; i < pScene.getNumMaterials(); ++i) {
			Material mat = pScene.mMaterials[i];

			int illum = 1;
//			mOutputMat << "newmtl " << GetMaterialName(i)  << endl;
			out.write("newmtl ");
			out.write(getMaterialName(i));
			out.write('\n');

//			aiColor4D c;
//			if(AI_SUCCESS == mat.Get(AI_MATKEY_COLOR_DIFFUSE,c)) {
//				mOutputMat << "kd " << c.r << " " << c.g << " " << c.b << endl;
//			}
//			if(AI_SUCCESS == mat.Get(AI_MATKEY_COLOR_AMBIENT,c)) {
//				mOutputMat << "ka " << c.r << " " << c.g << " " << c.b << endl;
//			}
//			if(AI_SUCCESS == mat.Get(AI_MATKEY_COLOR_SPECULAR,c)) {
//				mOutputMat << "ks " << c.r << " " << c.g << " " << c.b << endl;
//			}
//			if(AI_SUCCESS == mat.Get(AI_MATKEY_COLOR_EMISSIVE,c)) {
//				mOutputMat << "ke " << c.r << " " << c.g << " " << c.b << endl;
//			}
			
			float o;
			if(AI_SUCCESS == mat.Get(AI_MATKEY_OPACITY,o)) {
				mOutputMat << "d " << o << endl;
			}

			if(AI_SUCCESS == mat.Get(AI_MATKEY_SHININESS,o) && o) {
				mOutputMat << "Ns " << o << endl;
				illum = 2;
			}

			mOutputMat << "illum " << illum << endl;

			aiString s;
			if(AI_SUCCESS == mat.Get(AI_MATKEY_TEXTURE_DIFFUSE(0),s)) {
				mOutputMat << "map_kd " << s.data << endl;
			}
			if(AI_SUCCESS == mat.Get(AI_MATKEY_TEXTURE_AMBIENT(0),s)) {
				mOutputMat << "map_ka " << s.data << endl;
			}
			if(AI_SUCCESS == mat.Get(AI_MATKEY_TEXTURE_SPECULAR(0),s)) {
				mOutputMat << "map_ks " << s.data << endl;
			}
			if(AI_SUCCESS == mat.Get(AI_MATKEY_TEXTURE_SHININESS(0),s)) {
				mOutputMat << "map_ns " << s.data << endl;
			}
			if(AI_SUCCESS == mat.Get(AI_MATKEY_TEXTURE_HEIGHT(0),s) || AI_SUCCESS == mat.Get(AI_MATKEY_TEXTURE_NORMALS(0),s)) {
				// implementations seem to vary here, so write both variants
				mOutputMat << "bump " << s.data << endl;
				mOutputMat << "map_bump " << s.data << endl;
			}

			mOutputMat << endl;
		}
	}
	
	void writeGeometryFile(Writer out){
		
	}

	String getMaterialName(int index){
		Material mat = pScene.mMaterials[index];
//		String s;
//		if(AI_SUCCESS == mat.Get(AI_MATKEY_NAME,s)) {
//			return std::string(s.data,s.length);
//		}
		
		String s = mat.getString(Material.AI_MATKEY_NAME, 0, 0);
		if(s != null)
			return s;

//		char number[ sizeof(unsigned int) * 3 + 1 ];
//		ASSIMP_itoa10(number,index);
//		return "$Material_" + std::string(number);
		
		byte[] number = new byte[4 * 3 + 1];
		int count = AssUtil.assimp_itoa10(number, 0, 13, index);
		return "$Material_" + new String(number, 0, count);
	}
	
	String getMaterialLibName(){
		// within the Obj file, we use just the relative file name with the path stripped
		String s = getMaterialLibFileName();
		int il = s.lastIndexOf("/\\");
		if (il != -1) {
			return s.substring(il + 1);
		}

		return s;
	}

	private String getMaterialLibFileName() {
		return filename + ".mtl";
	}


	void addMesh(String name, Mesh m, Matrix4f mat){
		
	}
	void addNode(Node nd, Matrix4f mParent){
		
	}
	
	private static final class Face{
		byte kind;
		int[] indices;
	}
	
	private static final class MeshInstace{
		String name;
		String matname;
		final List<Face> faces = new ArrayList<>();
	}
	
	private static final class VecIndexMap{
		int mNextIndex;
		Object2IntMap<Vector3f> vecMap = new Object2IntOpenHashMap<>();
	}
	
	
}
