package assimp.importer.xfile;

import java.util.List;

/** Helper structure to represent an animation set in a XFile */
public class XAnimation {

	String mName;
	List<XAnimBone> mAnims;
	
	@Override
	public String toString() {
		return "XAnimation [mName=" + mName + ", mAnims=" + mAnims + "]";
	}
}
