package assimp.importer.xfile;

/** Helper structure to represent a bone weight */
public class XBoneWeight {
	int mVertex;
	float mWeight;
	
	@Override
	public String toString() {
		return "XBoneWeight [mVertex=" + mVertex + ", mWeight=" + mWeight + "]";
	}
	
}
