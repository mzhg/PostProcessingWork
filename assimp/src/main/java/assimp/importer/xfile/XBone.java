package assimp.importer.xfile;

import java.util.List;

import org.lwjgl.util.vector.Matrix4f;

/** Helper structure to represent a bone in a mesh */
public class XBone {

	String mName;
	List<XBoneWeight> mWeights;
	Matrix4f mOffsetMatrix;
	
	@Override
	public String toString() {
		return "XBone [mName=" + mName + ", mWeights=" + mWeights
				+ ", mOffsetMatrix=" + mOffsetMatrix + "]";
	}
	
}
