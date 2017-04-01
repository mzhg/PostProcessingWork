package jet.opengl.postprocessing.texture;

public class TextureDataDesc {

	public int format;
	public int type;
	/**
	 * The data source of the texture.<ul>
	 * <li> List: mipmap's data
	 * <li> long the GL_PIXEL_UNPACK_BUFFER offsets
	 * <li> byte[], short[] int[], float[], long[], double[]
	 * <li> ByteBuffer, ShortBuffer, IntBuffer, FloatBuffer, LongBuffer, DoubleBuffer.
	 * </ul>
	 */
	public Object data;
	
	public int imageSize;
	
	/**
	 * If true, then the values are interpreted with the bit orderings shows below:<p>
	 * <table>
  <tr>
    <th>Element Size</th>
    <th>Default Bit Ordering</th>
    <th>Modified Bit Ordering</th>
  </tr>
  <tr>
    <td>8 bit</td>
    <td>[7..0]</td>
    <td>[7..0]</td>
  </tr>
  <tr>
    <td>16 bit</td>
    <td>[15..0]</td>
    <td>[7..0][15..8]</td>
  </tr>
  <tr>
    <td>32 bit</td>
    <td>[31..0]</td>
    <td>[7..0][15..8][23..16][31..24]</td>
  </tr>
</table>
	 */
	public boolean unpackSwapBytes = false;
	public boolean unpackLSBFirst  = false;
	
	public int unpackRowLength = 0;
	public int unpackSkipRows = 0;
	public int unpackSkipPixels;
	public int unpackAlignment = 4;
	public int unpackImageHeight = 0;
	public int unpackSkipImages = 0;
	public int unpackCompressedBlockWidth = 0;
	public int unpackCompressedBlockHeight = 0;
	public int unpackCompressedBlockDepth = 0;
	public int unpackCompressedBlockSize = 0;
	
	public TextureDataDesc() {
	}
	
	public TextureDataDesc(int format, int type, Object data) {
		this.format = format;
		this.type = type;
		this.data = data;
	}
}
