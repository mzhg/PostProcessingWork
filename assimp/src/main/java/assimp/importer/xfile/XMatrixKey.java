package assimp.importer.xfile;

import org.lwjgl.util.vector.Matrix4f;

public class XMatrixKey {

	float mTime;
	final Matrix4f mMatrix = new Matrix4f();
	
	@Override
	public String toString() {
		return "XMatrixKey [mTime=" + mTime + ", mMatrix=" + mMatrix + "]";
	}
}
