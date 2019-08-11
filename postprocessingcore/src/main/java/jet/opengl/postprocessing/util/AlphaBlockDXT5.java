package jet.opengl.postprocessing.util;

final class AlphaBlockDXT5 {

	private static final Object lock = new Object();
	private static final byte[] alpha_array = new byte[8];
	private static final byte[] index_array = new byte[16];
	public long u;
	
	public int load(byte[] buf, int offset){
		u = Numeric.getLong(buf, offset);
		return offset + 8;
	}
	
	/**
	 * Get the stub value. <p>
	 * index == 0 for alpha0. take 8 bits<br>
	 * index == 1 for alpha1. take 8 bits<p>
	 * 
	 * 2 <= index < 18 for correspond bits, per bits take 3 bits.
	 * @param index
	 * @return the correspond bits value.
	 */
	public int getBits(int index){
		if(index < 2){
			return (int) ((u >> (8 * index)) & 0xFF);
		}else{
			return (int) ((u >> (3 * (index - 2) + 16)) & 7);
		}
	}
	
	public void evaluatePalette(byte[] alpha){
		final int alpha0 = getBits(0);
		final int alpha1 = getBits(1);
		
		if (alpha0 > alpha1) {
	        evaluatePalette8(alpha);
	    }
	    else {
	        evaluatePalette6(alpha);
	    }
	}
	
	public void evaluatePalette8(byte[] alpha){
		// 8-alpha block:  derive the other six alphas.
	    // Bit code 000 = alpha0, 001 = alpha1, others are interpolated.
		
		final int alpha0 = getBits(0);
		final int alpha1 = getBits(1);
		
		alpha[0] = (byte) alpha0;
	    alpha[1] = (byte) alpha1;
	    alpha[2] = (byte) ((6 * alpha0 + 1 * alpha1) / 7);    // bit code 010
	    alpha[3] = (byte) ((5 * alpha0 + 2 * alpha1) / 7);    // bit code 011
	    alpha[4] = (byte) ((4 * alpha0 + 3 * alpha1) / 7);    // bit code 100
	    alpha[5] = (byte) ((3 * alpha0 + 4 * alpha1) / 7);    // bit code 101
	    alpha[6] = (byte) ((2 * alpha0 + 5 * alpha1) / 7);    // bit code 110
	    alpha[7] = (byte) ((1 * alpha0 + 6 * alpha1) / 7);    // bit code 111
	}
	
	public void evaluatePalette6(byte[] alpha){
		// 6-alpha block.
	    // Bit code 000 = alpha0, 001 = alpha1, others are interpolated.
		final int alpha0 = getBits(0);
		final int alpha1 = getBits(1);
		
		alpha[0] = (byte) alpha0;
	    alpha[1] = (byte) alpha1;
	    alpha[2] = (byte) ((4 * alpha0 + 1 * alpha1) / 5);    // Bit code 010
	    alpha[3] = (byte) ((3 * alpha0 + 2 * alpha1) / 5);    // Bit code 011
	    alpha[4] = (byte) ((2 * alpha0 + 3 * alpha1) / 5);    // Bit code 100
	    alpha[5] = (byte) ((1 * alpha0 + 4 * alpha1) / 5);    // Bit code 101
	    alpha[6] = 0;                             // Bit code 110
	    alpha[7] = -1;                            // Bit code 111
	}
	
	public void indices(byte[] index_array){
		index_array[0x0] = (byte) getBits(2 + 0x0);
		index_array[0x1] = (byte) getBits(2 + 0x1);
		index_array[0x2] = (byte) getBits(2 + 0x2);
		index_array[0x3] = (byte) getBits(2 + 0x3);
		index_array[0x4] = (byte) getBits(2 + 0x4);
		index_array[0x5] = (byte) getBits(2 + 0x5);
		index_array[0x6] = (byte) getBits(2 + 0x6);
		index_array[0x7] = (byte) getBits(2 + 0x7);
		index_array[0x8] = (byte) getBits(2 + 0x8);
		index_array[0x9] = (byte) getBits(2 + 0x9);
		index_array[0xA] = (byte) getBits(2 + 0xA);
		index_array[0xB] = (byte) getBits(2 + 0xB);
		index_array[0xC] = (byte) getBits(2 + 0xC);
		index_array[0xD] = (byte) getBits(2 + 0xD);
		index_array[0xE] = (byte) getBits(2 + 0xE);
		index_array[0xF] = (byte) getBits(2 + 0xF);
	}
	
	public int index(int index){
		int offset = (3 * index + 16);
	    return (int)((u >> offset) & 0x7);
	}
	
	public void setIndex(int index, int value){
		 int offset = (3 * index + 16);
		 long mask = 0x7l << offset;
		 u = (u & ~mask) | (Numeric.unsignedInt(value) << offset);
	}
	
	public void decodeBlock(ColorBlock block){
		synchronized (lock) {
			evaluatePalette(alpha_array);
			
			indices(index_array);
			
			for(int i = 0; i < 16; i++) {
				block.setAlpha(i, alpha_array[index_array[i]] & 0xFF);
			}	
		}
	}
	
	public static void main(String[] args) {
		String format = "index_array[0x%X] = getBits(2 + 0x%X);\n";
		for(int i = 0; i < 16; i++)
			System.out.printf(format, i, i);
	}
}
