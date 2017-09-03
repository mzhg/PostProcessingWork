package assimp.common;

import java.nio.FloatBuffer;

import org.lwjgl.util.vector.Vector3f;

public final class ProcessHelper {

	// -------------------------------------------------------------------------------
	public static String textureTypeToString(TextureType in){
		switch (in){
		case aiTextureType_NONE:
			return "n/a";
		case aiTextureType_DIFFUSE:
			return "Diffuse";
		case aiTextureType_SPECULAR:
			return "Specular";
		case aiTextureType_AMBIENT:
			return "Ambient";
		case aiTextureType_EMISSIVE:
			return "Emissive";
		case aiTextureType_OPACITY:
			return "Opacity";
		case aiTextureType_NORMALS:
			return "Normals";
		case aiTextureType_HEIGHT:
			return "Height";
		case aiTextureType_SHININESS:
			return "Shininess";
		case aiTextureType_DISPLACEMENT:
			return "Displacement";
		case aiTextureType_LIGHTMAP:
			return "Lightmap";
		case aiTextureType_REFLECTION:
			return "Reflection";
		case aiTextureType_UNKNOWN:
			return "Unknown";
		default:
			break;
		}
	    return  "BUG";          
	}
	
	static void min(Vector3f a, Vector3f b, Vector3f dest){
		boolean less = (a.x != b.x ? a.x < b.x : a.y != b.y ? a.y < b.y : a.z < b.z); 
		Vector3f d = less ? a : b;
		if(d != dest)
			dest.set(d);
	}
	
	static void max(Vector3f a, Vector3f b, Vector3f dest){
		boolean more = (a.x != b.x ? a.x > b.x : a.y != b.y ? a.y > b.y : a.z > b.z); 
		Vector3f d = more ? a : b;
		if(d != dest)
			dest.set(d);
	}
	
	static void minMaxChooser(Vector3f minVec, Vector3f maxVec){
		final float extram = 1e10f;
		minVec.set(extram, extram, extram);
		maxVec.set(-extram, -extram, -extram);
	}
	
	public static float computePositionEpsilon(Mesh[] meshes, int num)
	{
		final float epsilon = 1e-4f;
		
		// calculate the position bounds so we have a reliable epsilon to check position differences against 
		Vector3f maxVec = new Vector3f();
		Vector3f minVec = new Vector3f();
		Vector3f mi = new Vector3f();
		Vector3f ma = new Vector3f();
//		minMaxChooser<aiVector3D>()(minVec,maxVec);
		minMaxChooser(minVec, maxVec);

		for (int a = 0; a < num; ++a) {
			Mesh pMesh = meshes[a];
		    arrayBounds(pMesh.mVertices,pMesh.mNumVertices,mi,ma);

			min(minVec,mi, minVec);
			max(maxVec,ma, maxVec);
		}
		return Vector3f.distance(maxVec, minVec) * epsilon;
	}
	
	static void arrayBounds(FloatBuffer vertices, int numVertices, Vector3f mi, Vector3f ma){
		Vector3f vec = new Vector3f();
		
		minMaxChooser(mi, ma);
		int position = vertices.position();
		for(int i = 0; i < numVertices; i++){
			vec.load(vertices);
			
			min(mi, vec, mi);
			max(ma, vec, ma);
		}
		vertices.position(position);
	}
}
