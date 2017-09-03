package assimp.importer.hmp;

import java.nio.ByteBuffer;

/** Data structure for a terrain vertex in a HMP5 file 
*/
final class Vertex_HMP5 {

	short z;
	byte normals162index;
	byte pad;
	
	Vertex_HMP5 load(ByteBuffer buf){
		z = buf.getShort();
		normals162index = buf.get();
		pad = buf.get();
		return this;
	}
}
