package assimp.importer.xfile;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

/** Helper structure analogue to aiScene */
public class XScene {

	XNode mRootNode;
	final List<XMesh> mGlobalMeshes = new ArrayList<XMesh>(); // global meshes found outside of any frames
	final List<XMaterial> mGlobalMaterials = new ArrayList<XMaterial>(); // global materials found outside of any meshes.
	
	final List<XAnimation> mAnims = new ArrayList<XAnimation>();
	int mAnimTicksPerSecond;
	
	public XMesh getMesh(){
		for(XNode child : mRootNode.mChildren){
			if(child.mMeshes.size() > 0)
				return child.mMeshes.get(0);
		}
		
		return null;
	}
	
	@Override
	public String toString() {
		return "XScene [mRootNode=" + mRootNode + ", mGlobalMeshes="
				+ mGlobalMeshes + ", mGlobalMaterials=" + mGlobalMaterials
				+ ", mAnims=" + mAnims + ", mAnimTicksPerSecond="
				+ mAnimTicksPerSecond + "]";
	}
	
	public void printMeshes(){
		System.out.println("print RootNode.mMeshes: ");
		int count = 0;
		for(XMesh mesh : mRootNode.mMeshes){
			printMesh(count++, mesh);
		}
		
		System.out.println("print RootNode.mChildren: ");
		count = 0;
		for(XNode child : mRootNode.mChildren){
//			printMesh(count++, mesh);
			System.out.println("print RootNode.mChildren" + (count++) + ", mesh count = " + child.mMeshes.size());
			int k = 0;
			for(XMesh mesh : child.mMeshes){
				printMesh(k++, mesh);
			}
		}
		
		count = 0;
		System.out.println("print GlobalMeshes: ");
		for(XMesh mesh : mGlobalMeshes){
			printMesh(count++, mesh);
		}
		
		
	}
	
	private void printMesh(int index, XMesh mesh){
		System.out.print("positions" + index + ":");
		FloatBuffer buf = mesh.mPositions;
		for(int i = 0; i < buf.limit() - 1; i++){
			System.out.print(buf.get(i));
			System.out.print(',');
		}
		System.out.println(buf.get(buf.limit() - 1));
		
		System.out.print("normals" + index + ":");
		buf = mesh.mNormals;
		for(int i = 0; i < buf.limit() - 1; i++){
			System.out.print(buf.get(i));
			System.out.print(',');
		}
		System.out.println(buf.get(buf.limit() - 1));
	}
}
