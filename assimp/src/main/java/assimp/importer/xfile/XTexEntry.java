package assimp.importer.xfile;

/** Helper structure representing a texture filename inside a material and its potential source */
public class XTexEntry {

	String mName;
	boolean mIsNormalMap; // true if the texname was specified in a NormalmapFilename tag
	
	public XTexEntry() {
	}
	
	public XTexEntry(String name, boolean isNormalMap) {
		this.mName = name;
		this.mIsNormalMap = isNormalMap;
	}
	
	public XTexEntry(String name) {
		this.mName = name;
	}

	@Override
	public String toString() {
		return "XTexEntry [mName=" + mName + ", mIsNormalMap=" + mIsNormalMap
				+ "]";
	}
}
