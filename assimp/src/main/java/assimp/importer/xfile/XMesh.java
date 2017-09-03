package assimp.importer.xfile;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import assimp.common.AssUtil;
import assimp.common.Mesh;
import it.unimi.dsi.fastutil.ints.IntList;

/** Helper structure to represent an XFile mesh */
public class XMesh {

	public FloatBuffer mPositions;  // list<vec3f>
	public List<XFace> mPosFaces;
	public FloatBuffer mNormals;   // list<vec3f>
	public List<XFace> mNormFaces;
	public int mNumTextures;
	public FloatBuffer[] mTexCoords = new FloatBuffer[Mesh.AI_MAX_NUMBER_OF_TEXTURECOORDS]; // list<vec2f>
	public int mNumColorSets;
	public FloatBuffer[] mColors = new FloatBuffer[Mesh.AI_MAX_NUMBER_OF_COLOR_SETS]; // list<vec3f>

	IntList mFaceMaterials;
	final List<XMaterial> mMaterials = new ArrayList<XMaterial>();

	final List<XBone> mBones = new ArrayList<XBone>();

	
	@Override
	public String toString() {
		
		return "XMesh [mPositions=" + AssUtil.toString(mPositions, 3) + ", \nmPosFaces=" + mPosFaces
				+ ", \nmNormals=" + AssUtil.toString(mNormals, 3) + ", \nmNormFaces=" + mNormFaces
				+ ", \nmNumTextures=" + mNumTextures + ", \nmTexCoords="
				+ AssUtil.toString(mTexCoords, 2) + ", \nmNumColorSets="
				+ mNumColorSets + ", \nmColors=" + AssUtil.toString(mColors, 4)
				+ ", \nmFaceMaterials=" + mFaceMaterials + ", \nmMaterials="
				+ mMaterials + ", \nmBones=" + mBones + "]";
	}
	
}
