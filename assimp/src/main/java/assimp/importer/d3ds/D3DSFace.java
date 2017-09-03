package assimp.importer.d3ds;

/** Helper structure representing a face with smoothing groups assigned */
public class D3DSFace {

	/** Indices. .3ds is using uint16. However, after
	 * an unique vrtex set has been generated,
	 * individual index values might exceed 2^16
	 */
	public int[] mIndices = new int[3];

	/** specifies to which smoothing group the face belongs to */
	public int iSmoothGroup;
	
	public D3DSFace() {
		mIndices[0] = -1;
		mIndices[1] = -1;
		mIndices[2] = -1;
	}
}
