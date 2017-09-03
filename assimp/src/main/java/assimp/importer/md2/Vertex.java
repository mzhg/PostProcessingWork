package assimp.importer.md2;

import java.nio.ByteBuffer;

/** Data structure for a MD2 vertex */
final class Vertex {

	byte[] vertex = new byte[3];
	byte lightNormalIndex;
	
	void load(ByteBuffer buf){
		buf.get(vertex);
		lightNormalIndex = buf.get();
	}
}
