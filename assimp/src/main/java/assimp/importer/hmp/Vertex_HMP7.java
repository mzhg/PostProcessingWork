package assimp.importer.hmp;

import java.nio.ByteBuffer;

/** Data structure for a terrain vertex in a HMP7 file 
*/
final class Vertex_HMP7 {
	short z;
	byte normal_x, normal_y;
	
	Vertex_HMP7 load(ByteBuffer mBuffer) {
		z = mBuffer.getShort();
		normal_x = mBuffer.get();
		normal_y = mBuffer.get();
		return this;
	}
}
