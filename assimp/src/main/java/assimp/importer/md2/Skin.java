package assimp.importer.md2;

import java.nio.ByteBuffer;

/** Data structure for a MD2 skin */
final class Skin {
	static final int SIZE = 64;
	final byte[] name = new byte[64];
	
	void load(ByteBuffer buf){
		buf.get(name);
	}
}
