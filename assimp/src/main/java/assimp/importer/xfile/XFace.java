package assimp.importer.xfile;

import java.util.Arrays;


/** Helper structure representing a XFile mesh face */
public class XFace {

	public int[] mIndices;

	@Override
	public String toString() {
		return "XFace [mIndices=" + Arrays.toString(mIndices) + "]";
	}
	
	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		
		XFace face = (XFace)obj;
		return Arrays.equals(face.mIndices, mIndices);
	}
}
