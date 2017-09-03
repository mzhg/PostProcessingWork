package assimp.importer.md2;

import java.nio.ByteBuffer;

/** Data structure for a MD2 frame */
final class Frame {
	
	static final int SIZE = 3 * 4 + 3 * 4 + 16 + 4;

	final float[] scale = new float[3];
	final float[] translate = new float[3];
	final byte[] name = new byte[16];
	final Vertex vertices = new Vertex();
	
	void load(ByteBuffer buf){
		for(int i = 0; i < scale.length; i++)
			scale[i] = buf.getFloat();
		for(int i = 0; i < translate.length; i++)
			translate[i] = buf.getFloat();
		buf.get(name);
		vertices.load(buf);
	}
}
