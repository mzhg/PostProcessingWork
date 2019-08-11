package jet.opengl.postprocessing.util;

final class BlockDXT3 {

	public final AlphaBlockDXT3 alpha = new AlphaBlockDXT3();
	public final BlockDXT1 color = new BlockDXT1();
	
	public void decodeBlock(ColorBlock block){
		// Decode color.
	    color.decodeBlock(block);
	    
	    // Decode alpha.
	    alpha.decodeBlock(block);
	}

	public int load(byte[] buf, int offset) {
		offset = alpha.load(buf, offset);
		offset = color.load(buf, offset);
		return offset;
	}
}
