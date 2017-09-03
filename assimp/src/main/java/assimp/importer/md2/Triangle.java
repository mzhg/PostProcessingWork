package assimp.importer.md2;

import java.nio.ByteBuffer;

/** Data structure for a MD2 triangle */
final class Triangle {
	static final int SIZEE = 12;
	
	short[] vertexIndices = new short[3];
	short[] textureIndices = new short[3];
	
	void load(ByteBuffer buf){
		for(int i = 0; i < 3; i++)
			vertexIndices[i] = buf.getShort();
		
		for(int i = 0; i < 3; i++)
			textureIndices[i] = buf.getShort();
	}
}
