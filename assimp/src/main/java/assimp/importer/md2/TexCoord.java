package assimp.importer.md2;

import java.nio.ByteBuffer;

/** Data structure for a MD2 texture coordinate */
final class TexCoord {
	final static int SIZE = 4;
	
	short s;
	short t;
	
	void load(ByteBuffer buf){
		s = buf.getShort();
		t = buf.getShort();
	}
}
