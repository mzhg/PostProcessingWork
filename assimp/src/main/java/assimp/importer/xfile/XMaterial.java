package assimp.importer.xfile;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

/** Helper structure representing a XFile material */
public class XMaterial {
	String mName;
	boolean mIsReference; // if true, mName holds a name by which the actual material can be found in the material list
	final Vector4f mDiffuse = new Vector4f();
	float mSpecularExponent;
	final Vector3f mSpecular = new Vector3f();
	final Vector3f mEmissive = new Vector3f();
	final List<XTexEntry> mTextures = new ArrayList<XTexEntry>();

  	int sceneIndex = -1; ///< the index under which it was stored in the scene's material list

	@Override
	public String toString() {
		return "XMaterial [mName=" + mName + ", mIsReference=" + mIsReference
				+ ", mDiffuse=" + mDiffuse + ", mSpecularExponent="
				+ mSpecularExponent + ", mSpecular=" + mSpecular
				+ ", mEmissive=" + mEmissive + ", mTextures=" + mTextures
				+ ", sceneIndex=" + sceneIndex + "]";
	}
}
